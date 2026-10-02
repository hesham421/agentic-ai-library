package io.agenticai.integration.domain;

import java.util.Arrays;
import java.util.Objects;

/**
 * One uploaded file as API-INT-002 received it: the content and its file name as text — never a
 * path INT would open (REQ-INT-015). The bytes live only for the request (REQ-INT-059). The
 * document type and file name are passed as received; Document Access refuses an incomplete upload
 * itself ({@code DOC-400-INCOMPLETE-UPLOAD}, passed through).
 *
 * @param checkId      DBF-INT-001 — the Check identifier
 * @param documentType the document type code, as received
 * @param fileName     the file name, as text
 * @param bytes        the file's content; the array is held by reference (no copy of a possibly
 *                     large upload), and must not be modified by the caller
 */
public record UploadCommand(Long checkId, String documentType, String fileName, byte[] bytes) {

    public UploadCommand {
        Objects.requireNonNull(checkId, "checkId");
    }

    /** Content-based equality: a record's default compares the array by reference. */
    @Override
    public boolean equals(Object other) {
        return other instanceof UploadCommand that
                && checkId.equals(that.checkId)
                && Objects.equals(documentType, that.documentType)
                && Objects.equals(fileName, that.fileName)
                && Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkId, documentType, fileName, Arrays.hashCode(bytes));
    }

    /** Never prints the content (guardrail: no document content in logs). */
    @Override
    public String toString() {
        return "UploadCommand[checkId=" + checkId + ", documentType=" + documentType
                + ", fileName=" + fileName + ", bytes=" + (bytes == null ? "null" : bytes.length + " bytes") + "]";
    }
}
