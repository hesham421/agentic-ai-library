package io.agenticai.doc.adapter;

import io.agenticai.doc.domain.UnreadableReason;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Adapter-side helper of {@link SpringAiDocumentReadingAdapter}: renders the pages of a scanned
 * PDF (a PDF whose text layer is blank, REQ-DOC-027) to PNG images, so that the document reaches
 * the document-reading model as image media. OpenAI-compatible providers — Gemini's among them —
 * refuse a PDF sent as an OpenAI {@code file} content part, while every vision model accepts
 * images; rendering keeps the call provider-neutral (REQ-DOC-032) and leaves the
 * {@link io.agenticai.doc.port.DocumentReadingModelPort} signature unchanged.
 *
 * <p>Bounds (G8): at most {@link #MAX_PAGES} pages — a PDF with more is refused whole, never
 * truncated (G6) — and the rendered images together at most the given byte budget
 * ({@code aias.check.max-file-size}). Every page is rendered in grayscale at {@link #DPI} DPI and
 * encoded through a {@link MemoryCacheImageOutputStream}: nothing is written to disk (ImageIO's
 * file cache is bypassed), and nothing is kept after the call (G9). Rendering stops between pages
 * once the Check's deadline has passed or the thread is interrupted.
 *
 * <p>A refusal is a {@link Refused} carrying the outcome's reason and detail: too many pages or
 * bytes, or an interruption → READING_FAILED (REQ-DOC-029 — the document itself is within the
 * maximum file size, so TOO_LARGE, which REQ-DOC-042 gives a document refused unread, does not
 * apply); the deadline passed → OUT_OF_TIME (REQ-DOC-040). A PDFBox or ImageIO failure
 * propagates as is, for the adapter to translate into READING_FAILED. Package-private and
 * stateless.
 */
final class PdfPageRenderer {

    /**
     * The most pages of one scanned PDF sent to the reading model in its single call
     * (REQ-DOC-057). A documented constant, not configuration: the plan names no
     * {@code aias.documents.reading-model.*} key for it (recorded in DOC's reading-model gap row).
     */
    static final int MAX_PAGES = 10;

    /** Render resolution: legible for OCR / vision models at a moderate image size. */
    static final float DPI = 150f;

    private static final String PNG = "png";

    /** A rendering refused: the document's outcome reason, and its detail as the message. */
    static final class Refused extends Exception {
        private final UnreadableReason reason;

        Refused(UnreadableReason reason, String detail) {
            super(detail, null, false, false);
            this.reason = Objects.requireNonNull(reason, "reason");
        }

        UnreadableReason reason() {
            return reason;
        }
    }

    /**
     * The PNG image of every page, in page order.
     *
     * @param pdf           the PDF's bytes
     * @param maxTotalBytes the byte budget of all rendered images together
     * @param deadline      the Check's deadline (REQ-DOC-040)
     */
    List<byte[]> render(byte[] pdf, long maxTotalBytes, Instant deadline) throws IOException, Refused {
        Objects.requireNonNull(pdf, "pdf");
        Objects.requireNonNull(deadline, "deadline");
        try (PDDocument document = Loader.loadPDF(pdf)) {
            int pages = document.getNumberOfPages();
            if (pages == 0) {
                throw new Refused(UnreadableReason.READING_FAILED, "the scanned PDF has no page to read");
            }
            if (pages > MAX_PAGES) {
                throw new Refused(UnreadableReason.READING_FAILED, "the scanned PDF has " + pages + " pages; the document-reading step reads at most "
                        + MAX_PAGES + " pages of a PDF without a text layer");
            }
            PDFRenderer renderer = new PDFRenderer(document);
            List<byte[]> images = new ArrayList<>(pages);
            long total = 0;
            for (int page = 0; page < pages; page++) {
                stopIfOutOfTime(deadline);
                byte[] png = encode(renderer.renderImageWithDPI(page, DPI, ImageType.GRAY));
                total += png.length;
                if (total > maxTotalBytes) {
                    throw new Refused(UnreadableReason.READING_FAILED, "the page images rendered from the scanned PDF exceed the maximum file size of "
                            + maxTotalBytes + " byte(s) (aias.check.max-file-size) at page " + (page + 1)
                            + " of " + pages);
                }
                images.add(png);
            }
            return images;
        }
    }

    private static void stopIfOutOfTime(Instant deadline) throws Refused {
        if (Thread.currentThread().isInterrupted()) {
            throw new Refused(UnreadableReason.READING_FAILED, "the rendering of the scanned PDF was interrupted");
        }
        if (!Instant.now().isBefore(deadline)) {
            throw new Refused(UnreadableReason.OUT_OF_TIME, "the Check's timeout was reached while the scanned PDF was being rendered");
        }
    }

    /** PNG bytes, encoded in memory only (no ImageIO file cache). */
    private static byte[] encode(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(PNG);
        if (!writers.hasNext()) {
            throw new IOException("no PNG image writer is available");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(out);
            writer.write(new IIOImage(image, null, null));
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
