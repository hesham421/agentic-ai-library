package io.agenticai.reg.domain;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * The containment guard of REQ-REG-016: service packages are read only from the configured
 * package directory ({@code aias.registry.package-directory}). A folder path is resolved against
 * that directory and normalised, and is accepted only when it lies inside it.
 *
 * <p>Framework-free and purely lexical: nothing is opened or stat-ed here. The resolution is
 * against the absolute, normalised root, so {@code ..} segments cannot escape it. Resolving
 * symbolic links (a real-path check) is the concern of the port that opens the files.
 */
public final class PackageDirectory {

    private final Path root;

    private PackageDirectory(Path root) {
        this.root = root;
    }

    /**
     * The guard of the configured directory.
     *
     * @param configured the value of {@code aias.registry.package-directory}
     */
    public static PackageDirectory of(Path configured) {
        Objects.requireNonNull(configured, "configured");
        return new PackageDirectory(configured.toAbsolutePath().normalize());
    }

    /** The absolute, normalised package directory. */
    public Path root() {
        return root;
    }

    /**
     * Resolves {@code folder} against the package directory and normalises it.
     *
     * @return the resolved path when it lies inside the package directory; empty when it is the
     *         directory itself or escapes it
     */
    public Optional<Path> resolveInside(Path folder) {
        Objects.requireNonNull(folder, "folder");
        Path resolved = root.resolve(folder).toAbsolutePath().normalize();
        return isInside(resolved) ? Optional.of(resolved) : Optional.empty();
    }

    /** Whether {@code folder}, resolved and normalised, lies inside the package directory. */
    public boolean contains(Path folder) {
        return resolveInside(folder).isPresent();
    }

    private boolean isInside(Path resolved) {
        return resolved.startsWith(root) && !resolved.equals(root);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PackageDirectory that && root.equals(that.root);
    }

    @Override
    public int hashCode() {
        return root.hashCode();
    }

    @Override
    public String toString() {
        return "PackageDirectory[" + root + "]";
    }
}
