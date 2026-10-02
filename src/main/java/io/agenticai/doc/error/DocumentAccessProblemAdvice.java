package io.agenticai.doc.error;

import io.agenticai.platform.web.Problems;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * The one exception-to-HTTP mapping of Document Access (DOC): every failure raised by a DOC
 * controller answers as an RFC 9457 {@link ProblemDetail} ({@code application/problem+json}) with
 * {@code type}, {@code title}, {@code status}, {@code detail} and the extension member
 * {@code code}. No DOC controller maps exceptions itself. Traces: ADR-DOC-012 — the catalog's two
 * PLATFORM-STD rows of API-DOC-001.
 *
 * <p>A missing or non-numeric {@code checkId} query parameter reaches Spring MVC as a
 * {@link MissingServletRequestParameterException} or a {@link MethodArgumentTypeMismatchException};
 * both are the catalog's trigger of {@code DOC-400-CHECK-ID-REQUIRED}. Anything that is not a
 * {@link DocumentAccessException} is an unexpected failure: it answers {@code DOC-500} with the
 * catalog message, is logged at error level with the request path and the Check identifier the
 * request carried, and carries no stack trace in the body.
 *
 * <p>Scoped to {@code io.agenticai.doc}; REG's advice is scoped to {@code io.agenticai.reg}, so
 * the two never overlap.
 */
@RestControllerAdvice(basePackages = "io.agenticai.doc")
public class DocumentAccessProblemAdvice {

    private static final Logger log = LoggerFactory.getLogger(DocumentAccessProblemAdvice.class);

    private static final String CHECK_ID_PARAMETER = "checkId";

    @ExceptionHandler(DocumentAccessException.class)
    ProblemDetail handleDocumentAccess(DocumentAccessException ex, HttpServletRequest request) {
        if (ex.status() >= 500) {
            log.error("DOC failure code={} path={} checkId={}",
                    ex.code(), request.getRequestURI(), checkIdOf(request), ex);
        } else {
            log.debug("DOC refusal code={} path={} checkId={}",
                    ex.code(), request.getRequestURI(), checkIdOf(request));
        }
        return problem(ex);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return checkIdRequired(ex, ex.getParameterName(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return checkIdRequired(ex, ex.getName(), request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        DocumentAccessException failure =
                new DocumentAccessException(DocumentAccessErrorCodes.UNEXPECTED_FAILURE, ex);
        log.error("DOC unexpected failure code={} path={} checkId={}",
                failure.code(), request.getRequestURI(), checkIdOf(request), ex);
        return problem(failure);
    }

    private static ProblemDetail checkIdRequired(Exception cause, String parameter, HttpServletRequest request) {
        DocumentAccessException refusal =
                new DocumentAccessException(DocumentAccessErrorCodes.CHECK_ID_REQUIRED, cause);
        log.debug("DOC refusal code={} path={} parameter={}", refusal.code(), request.getRequestURI(), parameter);
        return problem(refusal);
    }

    private static String checkIdOf(HttpServletRequest request) {
        return request.getParameter(CHECK_ID_PARAMETER);
    }

    private static ProblemDetail problem(DocumentAccessException ex) {
        return Problems.of(ex.status(), ex.getMessage(), ex.code());
    }
}
