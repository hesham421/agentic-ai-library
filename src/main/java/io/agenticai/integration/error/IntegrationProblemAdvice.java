package io.agenticai.integration.error;

import io.agenticai.chk.contract.CheckNotAwaitingDocumentsException;
import io.agenticai.chk.contract.ConnectionNotActivatedException;
import io.agenticai.chk.contract.ServiceNotAvailableException;
import io.agenticai.chk.contract.StartIncompleteException;
import io.agenticai.doc.contract.DocumentTypeNotOfServiceException;
import io.agenticai.doc.contract.FetchModeNotManualException;
import io.agenticai.doc.contract.IncompleteUploadException;
import io.agenticai.doc.contract.ServiceVersionNotFoundException;
import io.agenticai.doc.contract.UploadLimitReachedException;
import io.agenticai.integration.config.IntegrationProperties;
import io.agenticai.rpt.contract.RequestKeysMissingException;
import io.agenticai.rpt.contract.RptRefusalException;
import io.agenticai.platform.web.Problems;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.unit.DataSize;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Locale;
import java.util.Map;

/**
 * The one exception-to-HTTP mapping of Host Integration (INT) — CORE R1, CU.1. Every failure
 * raised while an INT controller handles a request answers as an RFC 9457 {@link ProblemDetail}
 * ({@code application/problem+json}) with {@code type}, {@code title}, {@code status},
 * {@code detail} and the extension member {@code code}. No INT controller maps exceptions itself.
 *
 * <ul>
 *   <li><b>INT's own exceptions</b> ({@link IntegrationException}) → their catalog code, the status
 *       of the code, the catalog message.</li>
 *   <li><b>Pass-through</b> (REQ-INT-006, ADR-INT-003, ADR-INT-010, ADR-INT-025; the catalog's
 *       PASS-THROUGH rows): every typed refusal of the Check Engine, Document Access and the Report
 *       Store that INT's operations can receive answers with that refusal's OWN code, its status
 *       (the {@code {http}} segment of the code) and its message, unchanged. They are handled
 *       through the owners' {@code .contract} types only — INT never imports an owner's
 *       {@code .error} package; {@code code()} / {@code status()} are the inherited public
 *       accessors, invoked on the contract subclass. CHK and DOC publish no common contract base,
 *       so one handler per contract type; RPT publishes one ({@link RptRefusalException}), so one
 *       handler covers every Report Store refusal, plus one for {@link RequestKeysMissingException}
 *       (a {@code .contract} type outside that base). A Report Store failure of status 5xx
 *       ({@code RPT-500-REPORT-NOT-STORED}) is a failure, not a refusal: it answers {@code INT-500}
 *       (REQ-INT-008, AC-INT-012).</li>
 *   <li>{@link MaxUploadSizeExceededException} → {@code INT-413-UPLOAD-TOO-LARGE}, {@code {limit}}
 *       being the configured upload request limit (REQ-INT-014). It reaches this advice because
 *       multipart parsing is lazy ({@code spring.servlet.multipart.resolve-lazily=true}): the part
 *       is parsed while the INT handler's arguments are resolved, so the handler is known. When the
 *       first part read is a {@code @RequestPart} text part, Spring reaches the servlet container's
 *       size-limit failure through {@code getMultipartHeaders} and wraps it as a plain
 *       {@link MultipartException} ("Could not access multipart servlet request") instead; that
 *       exception is recognised by its cause and answered the same way
 *       ({@link #isUploadSizeLimit}).</li>
 *   <li>An unreadable body ({@link HttpMessageNotReadableException}), a missing multipart part
 *       ({@link MissingServletRequestPartException}), an unreadable multipart request
 *       ({@link MultipartException}) or a non-numeric {@code checkId}
 *       ({@link MethodArgumentTypeMismatchException}), an unsupported Content-Type
 *       ({@link HttpMediaTypeNotSupportedException}) or an unsatisfiable Accept header
 *       ({@link HttpMediaTypeNotAcceptableException}) → {@code INT-400-REQUEST-INVALID}
 *       (REQ-INT-007; the API document lists no 415/406 code for INT).</li>
 *   <li>Anything else → {@code INT-500}, logged at error level with the request path and the Check
 *       identifier; the body carries the catalog message and no stack trace (REQ-INT-008).</li>
 * </ul>
 *
 * <p>Scoped to {@code io.agenticai.integration}; each other module's advice is scoped to its own
 * package, so none overlap.
 */
@RestControllerAdvice(basePackages = "io.agenticai.integration")
public class IntegrationProblemAdvice {

    private static final Logger log = LoggerFactory.getLogger(IntegrationProblemAdvice.class);

    private static final String CHECK_ID = "checkId";

    /** Detail keys of {@code INT-400-REQUEST-INVALID}'s {@code {detail}} placeholder (messages.properties). */
    static final String DETAIL_NOT_A_NUMBER = "INT-DETAIL-NOT-A-NUMBER";
    static final String DETAIL_VALUE_INVALID = "INT-DETAIL-VALUE-INVALID";
    static final String DETAIL_PART_MISSING = "INT-DETAIL-PART-MISSING";
    static final String DETAIL_BODY_UNREADABLE = "INT-DETAIL-BODY-UNREADABLE";
    static final String DETAIL_MULTIPART_UNREADABLE = "INT-DETAIL-MULTIPART-UNREADABLE";
    static final String DETAIL_CONTENT_TYPE_UNSUPPORTED = "INT-DETAIL-CONTENT-TYPE-UNSUPPORTED";
    static final String DETAIL_ACCEPT_UNSUPPORTED = "INT-DETAIL-ACCEPT-UNSUPPORTED";

    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;
    private static final long GB = MB * 1024L;

    private final IntegrationProperties properties;

    public IntegrationProblemAdvice(IntegrationProperties properties) {
        this.properties = properties;
    }

    // ---------------------------------------------------------------- INT's own exceptions

    @ExceptionHandler(IntegrationException.class)
    ProblemDetail handleIntegration(IntegrationException ex, HttpServletRequest request) {
        if (ex.status() >= 500) {
            log.error("INT failure code={} path={} checkId={}",
                    ex.code(), request.getRequestURI(), checkIdOf(request), ex);
        } else {
            log.debug("INT refusal code={} path={} checkId={}",
                    ex.code(), request.getRequestURI(), checkIdOf(request));
        }
        return problem(ex.code(), ex.status(), ex.getMessage());
    }

    // ---------------------------------------------------------------- pass-through: Check Engine

    @ExceptionHandler(StartIncompleteException.class)
    ProblemDetail handleStartIncomplete(StartIncompleteException ex, HttpServletRequest request) {
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    @ExceptionHandler(ServiceNotAvailableException.class)
    ProblemDetail handleServiceNotAvailable(ServiceNotAvailableException ex, HttpServletRequest request) {
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    @ExceptionHandler(ConnectionNotActivatedException.class)
    ProblemDetail handleConnectionNotActivated(ConnectionNotActivatedException ex, HttpServletRequest request) {
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    @ExceptionHandler(io.agenticai.chk.contract.CheckNotFoundException.class)
    ProblemDetail handleCheckEngineCheckNotFound(
            io.agenticai.chk.contract.CheckNotFoundException ex, HttpServletRequest request) {
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    @ExceptionHandler(CheckNotAwaitingDocumentsException.class)
    ProblemDetail handleCheckNotAwaitingDocuments(
            CheckNotAwaitingDocumentsException ex, HttpServletRequest request) {
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    // ---------------------------------------------------------------- pass-through: Document Access

    @ExceptionHandler(IncompleteUploadException.class)
    ProblemDetail handleIncompleteUpload(IncompleteUploadException ex, HttpServletRequest request) {
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    @ExceptionHandler(ServiceVersionNotFoundException.class)
    ProblemDetail handleServiceVersionNotFound(ServiceVersionNotFoundException ex, HttpServletRequest request) {
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    @ExceptionHandler(FetchModeNotManualException.class)
    ProblemDetail handleFetchModeNotManual(FetchModeNotManualException ex, HttpServletRequest request) {
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    @ExceptionHandler(DocumentTypeNotOfServiceException.class)
    ProblemDetail handleDocumentTypeNotOfService(
            DocumentTypeNotOfServiceException ex, HttpServletRequest request) {
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    @ExceptionHandler(io.agenticai.doc.contract.CheckEndedException.class)
    ProblemDetail handleDocumentCheckEnded(
            io.agenticai.doc.contract.CheckEndedException ex, HttpServletRequest request) {
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    @ExceptionHandler(UploadLimitReachedException.class)
    ProblemDetail handleUploadLimitReached(UploadLimitReachedException ex, HttpServletRequest request) {
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    // ---------------------------------------------------------------- pass-through: Report Store

    @ExceptionHandler(RptRefusalException.class)
    ProblemDetail handleReportStoreRefusal(RptRefusalException ex, HttpServletRequest request) {
        if (ex.status() >= 500) {
            return unexpected(ex, request);
        }
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    /**
     * {@code RPT-400-REQUEST-KEYS-MISSING} of {@code listChecksOfRequest} (API-INT-006's
     * PASS-THROUGH row): published by RPT as a {@code .contract} type that is not an
     * {@link RptRefusalException}, so it has its own handler.
     */
    @ExceptionHandler(RequestKeysMissingException.class)
    ProblemDetail handleRequestKeysMissing(RequestKeysMissingException ex, HttpServletRequest request) {
        return passThrough(ex.code(), ex.status(), ex.getMessage(), request);
    }

    // ---------------------------------------------------------------- transport

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleUploadTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        IntegrationException refusal = new IntegrationException(
                IntegrationErrorCodes.UPLOAD_TOO_LARGE, ex, readable(properties.upload().requestLimit()));
        log.debug("INT refusal code={} path={} checkId={}",
                refusal.code(), request.getRequestURI(), checkIdOf(request));
        return problem(refusal.code(), refusal.status(), refusal.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ProblemDetail handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        return requestInvalid(ex, IntegrationTexts.english(DETAIL_PART_MISSING, ex.getRequestPartName()), request);
    }

    @ExceptionHandler(MultipartException.class)
    ProblemDetail handleUnreadableMultipart(MultipartException ex, HttpServletRequest request) {
        if (isUploadSizeLimit(ex)) {
            // the container refused the upload's size, reached outside Spring's own size mapping (REQ-INT-014)
            return handleUploadTooLarge(new MaxUploadSizeExceededException(-1L, ex), request);
        }
        return requestInvalid(ex, IntegrationTexts.english(DETAIL_MULTIPART_UNREADABLE), request);
    }

    /**
     * Whether a {@link MultipartException} was caused by the servlet container's multipart size
     * limit — the same test Spring applies when it parses the request itself
     * ({@code StandardMultipartHttpServletRequest.handleParseFailure}): a cause whose text names an
     * exceeded size or limit. Container-neutral: no container class is referenced.
     */
    static boolean isUploadSizeLimit(MultipartException ex) {
        for (Throwable cause = ex.getCause(); cause != null && cause != cause.getCause(); cause = cause.getCause()) {
            if (cause instanceof MaxUploadSizeExceededException) {
                return true;
            }
            String text = cause.toString().toLowerCase(Locale.ROOT);
            if (text.contains("exceed") && (text.contains("size") || text.contains("limit"))) {
                return true;
            }
        }
        return false;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return requestInvalid(ex, IntegrationTexts.english(DETAIL_BODY_UNREADABLE), request);
    }

    /**
     * A body whose Content-Type the operation does not consume. The API document lists no 415 code
     * for INT; {@code INT-400-REQUEST-INVALID} is its documented "the request cannot be read" row
     * (REQ-INT-007), so the refusal answers with it (open api_doc_gaps row 'POST /api/v1/checks').
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail handleUnsupportedContentType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return requestInvalid(ex, IntegrationTexts.english(DETAIL_CONTENT_TYPE_UNSUPPORTED), request);
    }

    /** An Accept header no INT representation satisfies — same reasoning as the Content-Type refusal. */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ProblemDetail handleNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        return requestInvalid(ex, IntegrationTexts.english(DETAIL_ACCEPT_UNSUPPORTED), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        Class<?> required = ex.getRequiredType();
        boolean numeric = required != null
                && (Number.class.isAssignableFrom(required)
                    || required == long.class || required == int.class);
        String detail = IntegrationTexts.english(
                numeric ? DETAIL_NOT_A_NUMBER : DETAIL_VALUE_INVALID, ex.getName());
        return requestInvalid(ex, detail, request);
    }

    // ---------------------------------------------------------------- anything else

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        return unexpected(ex, request);
    }

    // ---------------------------------------------------------------- helpers

    private ProblemDetail requestInvalid(Exception cause, String detail, HttpServletRequest request) {
        IntegrationException refusal =
                new IntegrationException(IntegrationErrorCodes.REQUEST_INVALID, cause, detail);
        log.debug("INT refusal code={} path={} checkId={} detail={}",
                refusal.code(), request.getRequestURI(), checkIdOf(request), detail);
        return problem(refusal.code(), refusal.status(), refusal.getMessage());
    }

    private static ProblemDetail passThrough(String code, int status, String message, HttpServletRequest request) {
        log.debug("INT pass-through code={} path={} checkId={}", code, request.getRequestURI(), checkIdOf(request));
        return problem(code, status, message);
    }

    private static ProblemDetail unexpected(Exception ex, HttpServletRequest request) {
        IntegrationException failure = new IntegrationException(IntegrationErrorCodes.UNEXPECTED_FAILURE, ex);
        log.error("INT unexpected failure code={} path={} checkId={}",
                failure.code(), request.getRequestURI(), checkIdOf(request), ex);
        return problem(failure.code(), failure.status(), failure.getMessage());
    }

    /** The Check identifier the request carried: the {@code checkId} path variable, else the query parameter. */
    private static String checkIdOf(HttpServletRequest request) {
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables instanceof Map<?, ?> map && map.get(CHECK_ID) != null) {
            return String.valueOf(map.get(CHECK_ID));
        }
        return request.getParameter(CHECK_ID);
    }

    /** The limit as the catalog's {@code {limit}} reads it, e.g. "50 MB" (AC-INT-018). */
    static String readable(DataSize size) {
        long bytes = size.toBytes();
        if (bytes % GB == 0) {
            return (bytes / GB) + " GB";
        }
        if (bytes % MB == 0) {
            return (bytes / MB) + " MB";
        }
        if (bytes % KB == 0) {
            return (bytes / KB) + " KB";
        }
        return bytes + " bytes";
    }

    private static ProblemDetail problem(String code, int statusCode, String message) {
        return Problems.of(statusCode, message, code);
    }
}
