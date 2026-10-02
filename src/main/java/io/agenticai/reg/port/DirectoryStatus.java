package io.agenticai.reg.port;

/**
 * The state of the configured package directory as the {@link PackageSource} finds it
 * (REQ-REG-066, RULE-REG-023, ADR-REG-018). The load run guards on it before loading any
 * package: only {@link #READY} leads to {@link PackageSource#folders()}.
 */
public enum DirectoryStatus {

    /** The configured path does not exist or is not a directory. */
    MISSING,

    /** The directory exists but listing it failed (permission denied, I/O fault). */
    UNREADABLE,

    /** The directory can be listed but holds no package folder inside it. */
    EMPTY,

    /** The directory can be listed and holds at least one package folder. */
    READY
}
