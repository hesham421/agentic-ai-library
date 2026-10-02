package io.agenticai.reg.error;

import io.agenticai.platform.web.Problems;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The one exception-to-HTTP mapping of the Service Registry (REG): every failure raised by a REG
 * controller answers as an RFC 9457 {@link ProblemDetail} ({@code application/problem+json}) with
 * {@code type}, {@code title}, {@code status}, {@code detail} and the extension member
 * {@code code}. No REG controller maps exceptions itself. Traces: ADR-REG-011; {@code REG-500} is the
 * PLATFORM-STD answer of API-REG-001 … API-REG-003 and {@code REG-404-SERVICE-NOT-FOUND} that of
 * API-REG-002 (RULE-REG-016).
 *
 * <p>Anything that is not a {@link ServiceRegistryException} is an unexpected failure: it answers
 * {@code REG-500} with the catalog message, is logged at error level with the request path, and
 * carries no stack trace in the body.
 */
@RestControllerAdvice(basePackages = "io.agenticai.reg")
public class ServiceRegistryProblemAdvice {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistryProblemAdvice.class);

    @ExceptionHandler(ServiceRegistryException.class)
    ProblemDetail handleServiceRegistry(ServiceRegistryException ex, HttpServletRequest request) {
        if (ex.status() >= 500) {
            log.error("REG failure code={} path={}", ex.code(), request.getRequestURI(), ex);
        } else {
            log.debug("REG refusal code={} path={}", ex.code(), request.getRequestURI());
        }
        return problem(ex);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        ServiceRegistryException failure =
                new ServiceRegistryException(ServiceRegistryErrorCodes.UNEXPECTED_FAILURE, ex);
        log.error("REG unexpected failure code={} path={}", failure.code(), request.getRequestURI(), ex);
        return problem(failure);
    }

    private static ProblemDetail problem(ServiceRegistryException ex) {
        return Problems.of(ex.status(), ex.getMessage(), ex.code());
    }
}
