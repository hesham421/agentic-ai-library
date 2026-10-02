package io.agenticai.chk.domain;

/**
 * The facts of one Document Access outcome that RULE-CHK-004 needs — never the content.
 *
 * @param documentType the document type; {@code null} only for a host document whose type was NULL
 * @param readStatus   the CON-DOC-001 code: {@code READ}, {@code MISSING} or {@code UNREADABLE}
 * @param reason       the unreadable reason code, UNREADABLE only
 * @param detail       the unreadable detail, UNREADABLE only
 */
public record DocumentReading(String documentType, String readStatus, String reason, String detail) {
}
