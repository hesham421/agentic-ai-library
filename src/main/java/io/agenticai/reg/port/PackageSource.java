package io.agenticai.reg.port;

import java.util.List;

/**
 * Inbound configuration port of REG: the service packages of the configured package directory
 * ({@code aias.registry.package-directory}, ADR-REG-007). The adapter behind it is the only
 * class of REG that touches the file system; the load run reads through this interface only.
 *
 * <p>The port reports what it finds and decides nothing: a folder's completeness, its content
 * and its stability are judged by the load run under the module's rules.
 *
 * <p>The two file names below are the only place REG spells them; the SRS names the files by
 * role ("the service knowledge file", "the service definition file") and the ubiquitous
 * language of the analysis names them {@code knowledge.md} and {@code service.yaml}.
 */
public interface PackageSource {

    /** The service knowledge file of a package folder. */
    String KNOWLEDGE_FILE = "knowledge.md";

    /** The service definition file of a package folder. */
    String DEFINITION_FILE = "service.yaml";

    /**
     * The state of the package directory right now (REQ-REG-066, RULE-REG-023): the load run
     * calls this first and reads {@link #folders()} only when it is {@link DirectoryStatus#READY}.
     * {@link DirectoryStatus#EMPTY} means no package folder lies inside the directory — an
     * entry refused by {@link #refusedEntries()} is not a package folder.
     */
    DirectoryStatus status();

    /**
     * One entry per package folder inside the directory, in folder-name order, each read in
     * full (REQ-REG-005). Meaningful only when {@link #status()} is {@link DirectoryStatus#READY};
     * otherwise the list is empty. Never throws for a folder or a file that cannot be read —
     * that folder is returned with its {@link PackageFolder#unreadableFile()} set
     * (REQ-REG-071). The list is unmodifiable.
     */
    List<PackageFolder> folders();

    /**
     * The names of the directory entries that look like folders but were refused and never
     * read, because their real path does not lie inside the package directory's real path
     * (a symbolic link pointing outside it, REQ-REG-016) or could not be resolved. Exposed so
     * that nothing is skipped silently; the load run records no Load Result for them
     * (TC-REG-017). Unmodifiable; empty when nothing was refused.
     */
    List<String> refusedEntries();
}
