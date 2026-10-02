package io.agenticai.reg.service;

/**
 * The load reason codes of the Service Registry — exactly the rows of the SVC-API load reason
 * table ({@code REG-LOAD-{SLUG}}, ADR-REG-011). A load reason is the {@code REASON} of a REJECTED
 * Load Result, written to the load report; it is never an HTTP error, so none of these is in
 * {@code ServiceRegistryErrorCodes} (that class holds the error catalog only). The English text
 * of each code lives in {@code messages.properties} under the code ({@link LoadReasonMessages}).
 *
 * <p>RULE-REG-016 and RULE-REG-017 are refused at serve time, RULE-REG-028 only shortens text:
 * none of the three has a load reason code.
 */
final class LoadReasonCodes {

    private LoadReasonCodes() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    /** RULE-REG-001. */
    static final String INCOMPLETE_PACKAGE = "REG-LOAD-INCOMPLETE-PACKAGE";
    /** RULE-REG-002. */
    static final String DUPLICATE_SERVICE_CODE = "REG-LOAD-DUPLICATE-SERVICE-CODE";
    /** RULE-REG-003. */
    static final String VERSION_EDITED_IN_PLACE = "REG-LOAD-VERSION-EDITED-IN-PLACE";
    /** RULE-REG-004. */
    static final String VERSION_OLDER_THAN_CURRENT = "REG-LOAD-VERSION-OLDER-THAN-CURRENT";
    /** RULE-REG-005. */
    static final String CONNECTION_NOT_ACTIVATED = "REG-LOAD-CONNECTION-NOT-ACTIVATED";
    /** RULE-REG-006. */
    static final String UNBOUND_PARAMETER = "REG-LOAD-UNBOUND-PARAMETER";
    /** RULE-REG-007. */
    static final String NOT_SINGLE_SELECT = "REG-LOAD-NOT-SINGLE-SELECT";
    /** RULE-REG-008. */
    static final String UNKNOWN_FETCH_MODE = "REG-LOAD-UNKNOWN-FETCH-MODE";
    /** RULE-REG-009. */
    static final String DOCUMENT_SOURCE_INCOMPLETE = "REG-LOAD-DOCUMENT-SOURCE-INCOMPLETE";
    /** RULE-REG-010. */
    static final String BLOB_NOT_JDBC = "REG-LOAD-BLOB-NOT-JDBC";
    /** RULE-REG-011. */
    static final String APPROVAL_API_UNDEFINED = "REG-LOAD-APPROVAL-API-UNDEFINED";
    /** RULE-REG-012. */
    static final String ELEMENT_NOT_ALLOWED = "REG-LOAD-ELEMENT-NOT-ALLOWED";
    /** RULE-REG-013. */
    static final String DUPLICATE_CONNECTION_NAME = "REG-LOAD-DUPLICATE-CONNECTION-NAME";
    /** RULE-REG-014. */
    static final String UNKNOWN_CONNECTION_TYPE = "REG-LOAD-UNKNOWN-CONNECTION-TYPE";
    /** RULE-REG-015. */
    static final String CONNECTION_NOT_READ_ONLY = "REG-LOAD-CONNECTION-NOT-READ-ONLY";
    /** RULE-REG-018. */
    static final String EMPTY_SERVICE_KNOWLEDGE = "REG-LOAD-EMPTY-SERVICE-KNOWLEDGE";
    /** RULE-REG-019. */
    static final String DUPLICATE_QUERY_NAME = "REG-LOAD-DUPLICATE-QUERY-NAME";
    /** RULE-REG-020. */
    static final String FOREIGN_FILE_IN_PACKAGE = "REG-LOAD-FOREIGN-FILE-IN-PACKAGE";
    /** RULE-REG-021. */
    static final String DUPLICATE_DOCUMENT_TYPE = "REG-LOAD-DUPLICATE-DOCUMENT-TYPE";
    /** RULE-REG-022. */
    static final String INVALID_SERVICE_CODE = "REG-LOAD-INVALID-SERVICE-CODE";
    /** RULE-REG-023. */
    static final String PACKAGE_DIRECTORY_UNAVAILABLE = "REG-LOAD-PACKAGE-DIRECTORY-UNAVAILABLE";
    /** RULE-REG-024. */
    static final String PACKAGE_CHANGED_DURING_READ = "REG-LOAD-PACKAGE-CHANGED-DURING-READ";
    /** RULE-REG-025. */
    static final String CONNECTION_TYPE_BREAKS_BLOB = "REG-LOAD-CONNECTION-TYPE-BREAKS-BLOB";
    /** RULE-REG-026. */
    static final String PACKAGE_FILE_UNREADABLE = "REG-LOAD-PACKAGE-FILE-UNREADABLE";
    /** RULE-REG-027. */
    static final String VALUE_TOO_LONG = "REG-LOAD-VALUE-TOO-LONG";
}
