package io.agenticai.doc.domain;

import java.util.Locale;

/**
 * The one way DOC spells a size in an outcome detail, so that a TOO_LARGE detail always names
 * the measured size and the maximum file size in the same form (REQ-DOC-041, REQ-DOC-042,
 * AC-DOC-044): the exact byte count, and the megabyte figure beside it once the size reaches one
 * megabyte — {@code 26214400 bytes (25 MB)}. Framework-free.
 */
public final class FileSize {

    private static final long MEGABYTE = 1024L * 1024L;

    private FileSize() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /** {@code N bytes}, followed by {@code (M MB)} when {@code bytes} is at least one megabyte. */
    public static String describe(long bytes) {
        if (bytes < MEGABYTE) {
            return bytes + " bytes";
        }
        String megabytes = bytes % MEGABYTE == 0
                ? Long.toString(bytes / MEGABYTE)
                : String.format(Locale.ROOT, "%.1f", bytes / (double) MEGABYTE);
        return bytes + " bytes (" + megabytes + " MB)";
    }

    /**
     * The detail of a TOO_LARGE outcome (RULE-DOC-005, REQ-DOC-042): the subject, its measured
     * size and the maximum file size, both spelled by {@link #describe}.
     *
     * @param subject          what was measured — a location, "the content column", a file name
     * @param size             the measured size in bytes
     * @param maxFileSizeBytes {@code aias.check.max-file-size} in bytes
     */
    public static String tooLargeDetail(String subject, long size, long maxFileSizeBytes) {
        return subject + " is " + describe(size)
                + ", larger than the maximum file size of " + describe(maxFileSizeBytes)
                + "; its content was not read";
    }
}
