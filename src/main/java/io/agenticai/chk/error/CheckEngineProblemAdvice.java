package io.agenticai.chk.error;

import io.agenticai.platform.web.Problems;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * The one exception-to-HTTP mapping of the Check Engine (CHK): every failure raised by a CHK
 * controller answers as an RFC 9457 {@link ProblemDetail} ({@code application/problem+json}) with
 * {@code type}, {@code title}, {@code status}, {@code detail} and the extension member
 * {@code code}. No CHK controller maps exceptions itself. Traces: ADR-CHK-018 — the catalog's
 * three PLATFORM-STD rows of API-CHK-001.
 *
 * <p>A non-numeric {@code checkId} path variable reaches Spring MVC as a
 * {@link MethodArgumentTypeMismatchException}, the catalog's trigger of
 * {@code CHK-400-CHECK-ID-INVALID}. Anything that is not a {@link CheckEngineException} (or that
 * mismatch on {@code checkId}) is an unexpected failure: it answers {@code CHK-500} with the
 * catalog message, is logged at error level with the request path and the Check identifier the
 * request carried, and carries no stack trace in the body.
 *
 * <p>Scoped to {@code io.agenticai.chk}; REG's and DOC's advices are scoped to their own packages,
 * so the three never overlap.
 */
@RestControllerAdvice(basePackages = "io.agenticai.chk")
public class CheckEngineProblemAdvice {

    private static final Logger log = LoggerFactory.getLogger(CheckEngineProblemAdvice.class);

    private static final String CHECK_ID_PARAMETER = "checkId";

    @ExceptionHandler(CheckEngineException.class)
    ProblemDetail handleCheckEngine(CheckEngineException ex, HttpServletRequest request) {
        if (ex.status() >= 500) {
            log.error("CHK failure code={} path={} checkId={}",
                    ex.code(), request.getRequestURI(), checkIdOf(request), ex);
        } else {
            log.debug("CHK refusal code={} path={} checkId={}",
                    ex.code(), request.getRequestURI(), checkIdOf(request));
        }
        return problem(ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        if (!CHECK_ID_PARAMETER.equals(ex.getName())) {
            return handleUnexpected(ex, request);
        }
        CheckEngineException refusal =
                new CheckEngineException(CheckEngineErrorCodes.CHECK_ID_INVALID, ex);
        log.debug("CHK refusal code={} path={} parameter={}",
                refusal.code(), request.getRequestURI(), ex.getName());
        return problem(refusal);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        CheckEngineException failure =
                new CheckEngineException(CheckEngineErrorCodes.UNEXPECTED_FAILURE, ex);
        log.error("CHK unexpected failure code={} path={} checkId={}",
                failure.code(), request.getRequestURI(), checkIdOf(request), ex);
        return problem(failure);
    }

    /** The {@code checkId} path variable as the request carried it, or {@code null}. */
    private static String checkIdOf(HttpServletRequest request) {
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables instanceof Map<?, ?> map && map.get(CHECK_ID_PARAMETER) instanceof String checkId) {
            return checkId;
        }
        return null;
    }

    private static ProblemDetail problem(CheckEngineException ex) {
        return Problems.of(ex.status(), ex.getMessage(), ex.code());
    }
}
