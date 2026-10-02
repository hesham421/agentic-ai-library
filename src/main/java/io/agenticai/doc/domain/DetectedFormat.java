package io.agenticai.doc.domain;

import java.util.Objects;

/**
 * A format the {@link FormatDetector} recognised from a document's content signature
 * (REQ-DOC-024): the {@link DocumentFormat} that chooses the reader, and the media type of the
 * signature beside it — what the document-reading step hands to the model with an image or a
 * scanned PDF ({@code Media}, PORTS-MODEL).
 *
 * @param format    the recognised format
 * @param mediaType the IANA media type of the recognised signature, e.g. {@code application/pdf},
 *                  {@code image/jpeg}, {@code image/png}, {@code image/tiff}
 */
public record DetectedFormat(DocumentFormat format, String mediaType) {

    public DetectedFormat {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(mediaType, "mediaType");
    }
}
