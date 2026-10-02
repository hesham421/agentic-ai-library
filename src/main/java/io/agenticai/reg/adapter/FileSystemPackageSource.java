package io.agenticai.reg.adapter;

import io.agenticai.reg.config.ServiceRegistryProperties;
import io.agenticai.reg.domain.PackageDirectory;
import io.agenticai.reg.port.DirectoryStatus;
import io.agenticai.reg.port.PackageFolder;
import io.agenticai.reg.port.PackageSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The {@link PackageSource} over the configured package directory (ADR-REG-007). The only class
 * of REG that opens the file system; it only ever reads — nothing is written, moved or deleted.
 *
 * <p>Containment (REQ-REG-016): the configured directory and every entry that looks like a
 * folder are resolved with {@link Path#toRealPath} — symbolic links followed — and an entry
 * whose real path is not strictly inside the directory's real path is refused and never
 * opened; it is reported through {@link #refusedEntries()}. {@link PackageDirectory} supplies
 * the lexical root first; the real-path check is this adapter's.
 *
 * <p>Failure translation (REQ-REG-071, RULE-REG-026, RULE-REG-023): an {@code IOException} on a
 * file inside a folder becomes that folder's {@link PackageFolder#unreadableFile()} and the
 * next folder is read; only a failure to resolve or list the directory itself is
 * {@link DirectoryStatus#UNREADABLE}. No file-system exception leaves this class.
 *
 * <p>Stable read (REQ-REG-069, RULE-REG-024): size and last-modified time of each file are
 * taken before and after its text is read; {@link PackageFolder#stable()} is their equality.
 *
 * <p>Non-directory entries directly under the package directory are ignored: a package is a
 * folder, and the directory's own content is not the load run's to judge. Folders are
 * returned in name order so every load run sees the same sequence. Stateless: every call
 * reads the directory afresh (G9).
 */
@Component
public class FileSystemPackageSource implements PackageSource {

    private final Path configuredDirectory;

    public FileSystemPackageSource(ServiceRegistryProperties properties) {
        Objects.requireNonNull(properties, "properties");
        this.configuredDirectory = PackageDirectory.of(properties.packageDirectory()).root();
    }

    @Override
    public DirectoryStatus status() {
        return scan().status();
    }

    @Override
    public List<PackageFolder> folders() {
        Scan scan = scan();
        if (scan.status() != DirectoryStatus.READY) {
            return List.of();
        }
        List<PackageFolder> folders = new ArrayList<>(scan.accepted().size());
        for (Path folder : scan.accepted()) {
            folders.add(readFolder(folder));
        }
        return List.copyOf(folders);
    }

    @Override
    public List<String> refusedEntries() {
        return scan().refused();
    }

    /** One listing of the package directory: its status, the accepted folders, the refused names. */
    private Scan scan() {
        Path realRoot;
        try {
            realRoot = configuredDirectory.toRealPath();
        } catch (NoSuchFileException e) {
            return Scan.of(DirectoryStatus.MISSING);
        } catch (IOException e) {
            return Scan.of(DirectoryStatus.UNREADABLE);
        }
        if (!Files.isDirectory(realRoot)) {
            return Scan.of(DirectoryStatus.MISSING);
        }

        List<Path> accepted = new ArrayList<>();
        List<String> refused = new ArrayList<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(realRoot)) {
            for (Path entry : entries) {
                if (!Files.isDirectory(entry)) {
                    continue; // a file directly under the package directory is not a package
                }
                if (isStrictlyInside(entry, realRoot)) {
                    accepted.add(entry);
                } else {
                    refused.add(entry.getFileName().toString());
                }
            }
        } catch (NotDirectoryException | NoSuchFileException e) {
            return Scan.of(DirectoryStatus.MISSING);
        } catch (IOException e) {
            return Scan.of(DirectoryStatus.UNREADABLE);
        }
        accepted.sort(Comparator.comparing(path -> path.getFileName().toString()));
        refused.sort(Comparator.naturalOrder());
        DirectoryStatus status = accepted.isEmpty() ? DirectoryStatus.EMPTY : DirectoryStatus.READY;
        return new Scan(status, List.copyOf(accepted), List.copyOf(refused));
    }

    /**
     * Whether {@code entry}'s real path lies strictly inside {@code realRoot}. An entry whose
     * real path cannot be resolved cannot be proven inside, so it is refused.
     */
    private static boolean isStrictlyInside(Path entry, Path realRoot) {
        try {
            Path real = entry.toRealPath();
            return real.startsWith(realRoot) && !real.equals(realRoot);
        } catch (IOException e) {
            return false;
        }
    }

    /** Reads one accepted folder; never throws — an I/O failure becomes {@code unreadableFile}. */
    private static PackageFolder readFolder(Path folder) {
        String folderName = folder.getFileName().toString();
        List<String> otherNames = new ArrayList<>();
        Path knowledgeFile = null;
        Path definitionFile = null;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(folder)) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                if (KNOWLEDGE_FILE.equals(name)) {
                    knowledgeFile = entry;
                } else if (DEFINITION_FILE.equals(name)) {
                    definitionFile = entry;
                } else {
                    otherNames.add(name);
                }
            }
        } catch (IOException e) {
            // the folder itself cannot be listed: its own name marks the unreadable item
            return new PackageFolder(folderName, null, null, List.of(), true, folderName);
        }
        otherNames.sort(Comparator.naturalOrder());

        FileRead knowledge = knowledgeFile == null ? FileRead.ABSENT : readStably(knowledgeFile);
        if (knowledge.failed()) {
            return new PackageFolder(folderName, null, null, otherNames, true, KNOWLEDGE_FILE);
        }
        FileRead definition = definitionFile == null ? FileRead.ABSENT : readStably(definitionFile);
        if (definition.failed()) {
            return new PackageFolder(folderName, knowledge.text(), null, otherNames, true, DEFINITION_FILE);
        }
        boolean stable = knowledge.stable() && definition.stable();
        return new PackageFolder(folderName, knowledge.text(), definition.text(), otherNames, stable, null);
    }

    /**
     * Reads {@code file} as UTF-8 text with its size and last-modified time taken before and
     * after the read (RULE-REG-024). Opened for reading only.
     */
    private static FileRead readStably(Path file) {
        try {
            Snapshot before = Snapshot.of(file);
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Snapshot after = Snapshot.of(file);
            return new FileRead(text, before.equals(after), false);
        } catch (IOException e) {
            return new FileRead(null, false, true);
        }
    }

    private record Scan(DirectoryStatus status, List<Path> accepted, List<String> refused) {

        static Scan of(DirectoryStatus status) {
            return new Scan(status, List.of(), List.of());
        }
    }

    private record FileRead(String text, boolean stable, boolean failed) {

        static final FileRead ABSENT = new FileRead(null, true, false);
    }

    private record Snapshot(long size, FileTime lastModified) {

        static Snapshot of(Path file) throws IOException {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            return new Snapshot(attributes.size(), attributes.lastModifiedTime());
        }
    }
}
