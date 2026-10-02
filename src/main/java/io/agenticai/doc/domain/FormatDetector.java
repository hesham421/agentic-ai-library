package io.agenticai.doc.domain;

import io.agenticai.doc.domain.ReadOutcome.Read;
import io.agenticai.doc.domain.ReadOutcome.Unreadable;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The format of a document, decided from the signature of its content and from nothing else —
 * never from a file name or a column (REQ-DOC-024, ADR-DOC-004):
 *
 * <ul>
 *   <li>{@code %PDF} → {@link DocumentFormat#PDF}, {@code application/pdf}</li>
 *   <li>an OLE2 compound file ({@code D0 CF 11 E0 A1 B1 1A E1}) holding a {@code Workbook}
 *       stream → {@link DocumentFormat#XLS}, {@code application/vnd.ms-excel}</li>
 *   <li>a ZIP archive ({@code 50 4B 03 04}) holding {@code xl/workbook.xml} →
 *       {@link DocumentFormat#XLSX},
 *       {@code application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}</li>
 *   <li>JPEG {@code FF D8 FF}, PNG {@code 89 50 4E 47}, TIFF {@code 49 49 2A 00} /
 *       {@code 4D 4D 00 2A} → {@link DocumentFormat#IMAGE} with its image media type</li>
 *   <li>anything else → UNREADABLE / UNSUPPORTED_FORMAT (REQ-DOC-028)</li>
 * </ul>
 *
 * <p>A domain service: a plain class with no Spring annotation, no port and no I/O — it looks
 * only at the bytes it is given, which the caller has already read within the maximum file
 * size. The two container formats need their directory read to tell a workbook from any other
 * OLE2 or ZIP file (a {@code .doc} or a {@code .docx} share the outer signature): the ZIP
 * directory is walked with the JDK's {@link ZipInputStream}, the OLE2 directory with POI's
 * {@link POIFSFileSystem} over the in-memory bytes — the library the spreadsheet reader needs
 * anyway, used here as an in-memory parser, not as an adapter. A container whose directory
 * cannot be parsed is not a workbook DOC can name, so it is UNSUPPORTED_FORMAT with the parse
 * failure as detail; a damaged workbook that <em>is</em> recognised fails later in its reader as
 * READING_FAILED (REQ-DOC-029).
 */
public class FormatDetector {

    private static final byte[] PDF = {'%', 'P', 'D', 'F'};
    private static final byte[] OLE2 = {
            (byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1};
    private static final byte[] ZIP = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] TIFF_LITTLE_ENDIAN = {0x49, 0x49, 0x2A, 0x00};
    private static final byte[] TIFF_BIG_ENDIAN = {0x4D, 0x4D, 0x00, 0x2A};

    private static final String XLSX_WORKBOOK_ENTRY = "xl/workbook.xml";
    /** The BIFF8 and BIFF5 names of the workbook stream, as HSSF accepts them. */
    private static final String[] XLS_WORKBOOK_STREAMS = {"Workbook", "Book"};

    static final String MEDIA_PDF = "application/pdf";
    static final String MEDIA_XLS = "application/vnd.ms-excel";
    static final String MEDIA_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static final String MEDIA_JPEG = "image/jpeg";
    static final String MEDIA_PNG = "image/png";
    static final String MEDIA_TIFF = "image/tiff";

    /**
     * The format of {@code content}.
     *
     * @param content the document's bytes, as read
     * @return the recognised format with its media type, or UNREADABLE / UNSUPPORTED_FORMAT with a
     *         detail naming what the signature was
     */
    public ReadOutcome<DetectedFormat> detect(byte[] content) {
        Objects.requireNonNull(content, "content");
        if (content.length == 0) {
            return unsupported("the document is empty");
        }
        if (startsWith(content, PDF)) {
            return recognised(DocumentFormat.PDF, MEDIA_PDF);
        }
        if (startsWith(content, JPEG)) {
            return recognised(DocumentFormat.IMAGE, MEDIA_JPEG);
        }
        if (startsWith(content, PNG)) {
            return recognised(DocumentFormat.IMAGE, MEDIA_PNG);
        }
        if (startsWith(content, TIFF_LITTLE_ENDIAN) || startsWith(content, TIFF_BIG_ENDIAN)) {
            return recognised(DocumentFormat.IMAGE, MEDIA_TIFF);
        }
        if (startsWith(content, OLE2)) {
            return detectOle2(content);
        }
        if (startsWith(content, ZIP)) {
            return detectZip(content);
        }
        return unsupported("the content signature " + signatureOf(content)
                + " is none of PDF, .xls, .xlsx, JPEG, PNG or TIFF");
    }

    /** An OLE2 compound file is a workbook when its root directory holds the workbook stream. */
    private static ReadOutcome<DetectedFormat> detectOle2(byte[] content) {
        try (POIFSFileSystem compound = new POIFSFileSystem(new ByteArrayInputStream(content))) {
            DirectoryEntry root = compound.getRoot();
            for (String name : root.getEntryNames()) {
                for (String workbookStream : XLS_WORKBOOK_STREAMS) {
                    if (workbookStream.equalsIgnoreCase(name)) {
                        return recognised(DocumentFormat.XLS, MEDIA_XLS);
                    }
                }
            }
            return unsupported("the content is an OLE2 compound file without a Workbook stream");
        } catch (IOException | RuntimeException e) {
            return unsupported("the content is an OLE2 compound file whose directory could not be read: "
                    + e.getMessage());
        }
    }

    /** A ZIP archive is a workbook when it holds {@code xl/workbook.xml}; entry bodies are skipped. */
    private static ReadOutcome<DetectedFormat> detectZip(byte[] content) {
        try (ZipInputStream archive = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if (XLSX_WORKBOOK_ENTRY.equals(entry.getName())) {
                    return recognised(DocumentFormat.XLSX, MEDIA_XLSX);
                }
                archive.closeEntry();
            }
            return unsupported("the content is a ZIP archive without " + XLSX_WORKBOOK_ENTRY);
        } catch (IOException | RuntimeException e) {
            return unsupported("the content is a ZIP archive whose directory could not be read: "
                    + e.getMessage());
        }
    }

    private static ReadOutcome<DetectedFormat> recognised(DocumentFormat format, String mediaType) {
        return new Read<>(new DetectedFormat(format, mediaType));
    }

    private static ReadOutcome<DetectedFormat> unsupported(String detail) {
        return new Unreadable<>(UnreadableReason.UNSUPPORTED_FORMAT, detail);
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    /** The first bytes as hex — a signature, never document content of any meaning. */
    private static String signatureOf(byte[] content) {
        int length = Math.min(content.length, 8);
        StringBuilder hex = new StringBuilder(length * 3);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                hex.append(' ');
            }
            hex.append(String.format("%02X", content[i]));
        }
        return hex.toString();
    }
}
