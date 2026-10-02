package io.agenticai.doc.adapter;

import io.agenticai.platform.config.CheckLimitsProperties;
import io.agenticai.doc.config.DocumentAccessProperties;
import io.agenticai.doc.domain.FileSize;
import io.agenticai.doc.domain.ReadOutcome;
import io.agenticai.doc.domain.ReadOutcome.Read;
import io.agenticai.doc.domain.ReadOutcome.Unreadable;
import io.agenticai.doc.domain.UnreadableReason;
import io.agenticai.doc.port.HostFilePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * The {@link HostFilePort} over host file storage, {@code path} mode (REQ-DOC-005). The only
 * class of DOC that opens the file system, and it only ever reads: there is no write, move,
 * rename or delete here, nor anywhere in DOC (REQ-DOC-049, REQ-DOC-050; guardrail G3).
 *
 * <p>Every location goes through the same six steps, in this order (AIAS-5; guardrails G5, G8):
 * <ol>
 *   <li>a blank location → NOT_FOUND (REQ-DOC-007);</li>
 *   <li>no {@code aias.documents.storage-root} → OUTSIDE_STORAGE_ROOT, nothing opened
 *       (REQ-DOC-011) — a missing boundary closes access, never opens it;</li>
 *   <li>resolve: {@code root.resolve(location).normalize()}, so every {@code ..} segment is
 *       folded, then — when the file exists — {@code toRealPath()}, so every symbolic link is
 *       followed (REQ-DOC-008); a file that does not exist takes the real path of its nearest
 *       existing ancestor plus the missing tail, so it is compared on the same terms. The
 *       result must lie strictly inside the storage root's own real path, else
 *       OUTSIDE_STORAGE_ROOT and the file is never opened (REQ-DOC-009). An absolute location
 *       resolves to itself and so fails unless it lies inside the root (REQ-DOC-010);</li>
 *   <li>no file at the resolved location, or a directory → NOT_FOUND (REQ-DOC-007);</li>
 *   <li>{@link Files#size} before any read (REQ-DOC-041); larger than
 *       {@code aias.check.max-file-size} → TOO_LARGE naming both sizes, no byte read
 *       (REQ-DOC-042). A file of exactly the maximum is read;</li>
 *   <li>open with {@link StandardOpenOption#READ} only and read the measured size (REQ-DOC-049).</li>
 * </ol>
 *
 * <p>Failure translation (A.4.9, E.1.5): no file-system exception leaves this class. An
 * {@code IOException} or {@code SecurityException} at any step becomes a recorded
 * READING_FAILED outcome carrying the exception's message as detail; the storage root that
 * cannot itself be resolved proves nothing inside it, so it is OUTSIDE_STORAGE_ROOT, as in REG's
 * package source. Details name the location as the host gave it, never the file's content.
 *
 * <p>Stateless: the storage root and the limit are read once from configuration — never from a
 * service definition or a request (REQ-DOC-010, CU.3) — and every call resolves and reads its
 * file afresh; nothing is cached (guardrail G9).
 */
@Component
public class GuardedFileSystemAdapter implements HostFilePort {

    private static final Logger log = LoggerFactory.getLogger(GuardedFileSystemAdapter.class);

    /** The absolute, normalised storage root; {@code null} when the environment sets none. */
    private final Path storageRoot;
    private final long maxFileSizeBytes;

    public GuardedFileSystemAdapter(DocumentAccessProperties documents, CheckLimitsProperties limits) {
        Objects.requireNonNull(documents, "documents");
        Objects.requireNonNull(limits, "limits");
        this.storageRoot = documents.storageRoot() == null
                ? null
                : documents.storageRoot().toAbsolutePath().normalize();
        this.maxFileSizeBytes = limits.maxFileSize().toBytes();
    }

    @Override
    public ReadOutcome<byte[]> read(String location) {
        // 1. a blank location names no file (REQ-DOC-007)
        if (location == null || location.isBlank()) {
            return unreadable(UnreadableReason.NOT_FOUND, "the document has no path; nothing to read");
        }
        // 2. no storage root: every path document is closed, nothing is opened (REQ-DOC-011)
        if (storageRoot == null) {
            return unreadable(UnreadableReason.OUTSIDE_STORAGE_ROOT,
                    "no storage root is set (aias.documents.storage-root); \"" + location + "\" was not opened");
        }
        try {
            // 3. resolve and contain (REQ-DOC-008, REQ-DOC-009, REQ-DOC-010)
            Path realRoot;
            try {
                realRoot = storageRoot.toRealPath();
            } catch (IOException e) {
                log.debug("DOC host file: storage root cannot be resolved ({}); \"{}\" not opened",
                        describe(e), location, e);
                return unreadable(UnreadableReason.OUTSIDE_STORAGE_ROOT,
                        "the storage root cannot be resolved (" + describe(e) + "); \"" + location
                                + "\" was not opened");
            }
            Path resolved;
            try {
                resolved = storageRoot.resolve(location).normalize();
            } catch (InvalidPathException e) {
                return unreadable(UnreadableReason.NOT_FOUND,
                        "\"" + location + "\" is not a valid path: " + e.getReason());
            }
            Path real = realPathOf(resolved);
            log.debug("DOC host file: \"{}\" resolved to {} for the storage-root check", location, real);
            if (!real.startsWith(realRoot) || real.equals(realRoot)) {
                return unreadable(UnreadableReason.OUTSIDE_STORAGE_ROOT,
                        "\"" + location + "\" resolves outside the storage root; it was not opened");
            }
            // 4. the file must exist and be a file (REQ-DOC-007)
            if (Files.notExists(real)) {
                return unreadable(UnreadableReason.NOT_FOUND, "no file exists at \"" + location + "\"");
            }
            if (Files.isDirectory(real)) {
                return unreadable(UnreadableReason.NOT_FOUND,
                        "\"" + location + "\" is a directory, not a file");
            }
            // 5. size before any read (REQ-DOC-041, REQ-DOC-042)
            long size = Files.size(real);
            if (size > maxFileSizeBytes) {
                return unreadable(UnreadableReason.TOO_LARGE,
                        FileSize.tooLargeDetail("\"" + location + "\"", size, maxFileSizeBytes));
            }
            if (size > Integer.MAX_VALUE - 8) {
                return unreadable(UnreadableReason.READING_FAILED,
                        "\"" + location + "\" is " + FileSize.describe(size)
                                + ", more than one array can hold; its content was not read");
            }
            // 6. open for reading only, read the measured size (REQ-DOC-049)
            try (InputStream in = Files.newInputStream(real, StandardOpenOption.READ)) {
                byte[] bytes = in.readNBytes((int) size);
                log.debug("DOC host file: read {} of \"{}\"", FileSize.describe(bytes.length), location);
                return new Read<>(bytes);
            }
        } catch (IOException | SecurityException e) {
            log.debug("DOC host file: reading \"{}\" failed: {}", location, describe(e), e);
            return unreadable(UnreadableReason.READING_FAILED,
                    "reading \"" + location + "\" failed: " + describe(e));
        }
    }

    /**
     * The real path of {@code resolved} — symbolic links followed — when it exists. When it does
     * not, the real path of its nearest existing ancestor with the missing tail appended, so
     * that a missing file is still compared with the storage root's real path on equal terms
     * (a root reached through a linked directory would otherwise look outside itself) and a
     * missing file under a link that leaves the root is still refused.
     */
    private static Path realPathOf(Path resolved) throws IOException {
        Path existing = resolved;
        Path missingTail = null;
        while (existing != null && Files.notExists(existing)) {
            Path name = existing.getFileName();
            missingTail = missingTail == null ? name : name.resolve(missingTail);
            existing = existing.getParent();
        }
        if (existing == null) {
            return resolved;
        }
        Path real = existing.toRealPath();
        return missingTail == null ? real : real.resolve(missingTail);
    }

    private static ReadOutcome<byte[]> unreadable(UnreadableReason reason, String detail) {
        return new Unreadable<>(reason, detail);
    }

    /** The exception's type and message — a file-system exception's message is often only a path. */
    private static String describe(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank()
                ? e.getClass().getSimpleName()
                : e.getClass().getSimpleName() + ": " + message;
    }
}
