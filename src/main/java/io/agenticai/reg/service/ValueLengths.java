package io.agenticai.reg.service;

import java.util.Optional;

/**
 * RULE-REG-027 (REQ-REG-073, ADR-REG-022): the declared column lengths every text value a package
 * folder or the activation configuration supplies is checked against before it is stored. The
 * limits are the db-script's {@code VARCHAR2(n CHAR)} lengths; characters are counted as Unicode
 * code points, as the columns are. Pure: nothing is stored or decided beyond the comparison.
 */
final class ValueLengths {

    static final int FOLDER_NAME = 200;          // REG_LOAD_RESULT.SUBJECT_NAME, the folder's row
    static final int INPUT_NAME = 100;           // REG_SVC_PKG_VER.INPUT_NAME
    static final int QUERY_NAME = 100;           // REG_SVC_QUERY.QUERY_NAME
    static final int QUERY_CONNECTION_NAME = 100; // REG_SVC_QUERY.CONNECTION_NAME
    static final int DOCUMENT_SOURCE_QUERY = 100; // REG_SVC_PKG_VER.DOCUMENT_SOURCE_QUERY_NAME
    static final int DOCUMENT_COLUMN = 128;      // DOCUMENT_TYPE_COLUMN / PATH_COLUMN / CONTENT_COLUMN
    static final int DOCUMENT_TYPE = 100;        // REG_REQ_DOC.DOCUMENT_TYPE
    static final int APPROVAL_API = 500;         // REG_SVC_PKG_VER.APPROVAL_API
    static final int CONNECTION_NAME = 100;      // REG_CONNECTION.CONNECTION_NAME
    static final int ENDPOINT = 500;             // REG_CONNECTION.ENDPOINT
    static final int QUERY_TOOL = 100;           // REG_CONNECTION.QUERY_TOOL
    static final int DIALECT = 30;               // REG_CONNECTION.DIALECT
    static final int CREDENTIAL_REFERENCE = 200; // REG_CONNECTION.CREDENTIAL_REFERENCE
    static final int ENVIRONMENT_NAME = 50;      // REG_CONNECTION.ENVIRONMENT_NAME

    private ValueLengths() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /** The length of {@code value} in Unicode code points; 0 for {@code null}. */
    static int length(String value) {
        return value == null ? 0 : value.codePointCount(0, value.length());
    }

    /**
     * The RULE-REG-027 reason when {@code value} is longer than {@code limit}; empty when it fits
     * or is absent.
     *
     * @param field   the field name as the message names it ("folder name", "endpoint", …)
     * @param subject the subject the message names — the folder name, connection name,
     *                environment name or service code
     */
    static Optional<LoadReason> tooLong(String field, String subject, String value, int limit) {
        int length = length(value);
        if (length <= limit) {
            return Optional.empty();
        }
        return Optional.of(LoadReason.of(LoadReasonCodes.VALUE_TOO_LONG, field, subject, length, limit));
    }
}
