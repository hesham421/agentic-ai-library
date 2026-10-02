package io.agenticai.rpt.error;

import io.agenticai.platform.web.Problems;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Set;

/**
 * The one exception-to-HTTP mapping of the Report Store (RPT): every failure raised by an RPT
 * controller answers as an RFC 9457 {@link ProblemDetail} ({@code application/problem+json}) with
 * {@code type}, {@code title}, {@code status}, {@code detail} and the extension member
 * {@code code}. No RPT controller maps exceptions itself. Traces: ADR-RPT-013 — the catalog's five
 * rows of API-RPT-001 … API-RPT-003.
 *
 * <ul>
 *   <li>A {@link ReportStoreException} answers with its own code and status.</li>
 *   <li>A non-numeric {@code checkId} path variable reaches Spring MVC as a
 *       {@link MethodArgumentTypeMismatchException}, the catalog's trigger of
 *       {@code RPT-400-CHECK-ID-INVALID} (API-RPT-001).</li>
 *   <li>An ABSENT required query parameter reaches Spring MVC as a
 *       {@link MissingServletRequestParameterException}. The endpoint is told apart by the
 *       matched mapping pattern ({@link HandlerMapping#BEST_MATCHING_PATTERN_ATTRIBUTE}, set by the
 *       handler mapping before the arguments are resolved) and the parameter's name:
 *       {@code serviceCode} / {@code requestNumber} on {@value #CHECKS_PATH} →
 *       {@code RPT-400-REQUEST-KEYS-MISSING} (RULE-RPT-009, API-RPT-002); {@code serviceCode} on
 *       {@value #DECISION_AGREEMENT_PATH} → {@code RPT-400-SERVICE-CODE-MISSING} (RULE-RPT-015,
 *       API-RPT-003). A PRESENT but blank value is not a missing parameter to Spring MVC: the
 *       service refuses it with the same codes (both rules say "absent or blank").</li>
 *   <li>Anything else is an unexpected failure: it answers {@code RPT-500} with the catalog
 *       message, is logged at error level with the request path and the Check identifier the
 *       request carried, and carries no stack trace in the body.</li>
 * </ul>
 *
 * <p>Scoped to {@code io.agenticai.rpt}; the other modules' advices are scoped to their own
 * packages, so none overlap.
 */
@RestControllerAdvice(basePackages = "io.agenticai.rpt")
public class ReportStoreProblemAdvice {

    private static final Logger log = LoggerFactory.getLogger(ReportStoreProblemAdvice.class);

    private static final String CHECK_ID_PARAMETER = "checkId";
    private static final String SERVICE_CODE_PARAMETER = "serviceCode";
    private static final String REQUEST_NUMBER_PARAMETER = "requestNumber";

    /** Path of API-RPT-002, listing the Checks of a request (api-spec-rpt.yaml). */
    static final String CHECKS_PATH = "/api/v1/checks";

    /** Path of API-RPT-003, reading the decision agreement of a service (api-spec-rpt.yaml). */
    static final String DECISION_AGREEMENT_PATH = "/api/v1/decision-agreement";

    private static final Set<String> REQUEST_KEYS =
            Set.of(SERVICE_CODE_PARAMETER, REQUEST_NUMBER_PARAMETER);

    @ExceptionHandler(ReportStoreException.class)
    ProblemDetail handleReportStore(ReportStoreException ex, HttpServletRequest request) {
        if (ex.status() >= 500) {
            log.error("RPT failure code={} path={} checkId={}",
                    ex.code(), request.getRequestURI(), checkIdOf(request), ex);
        } else {
            log.debug("RPT refusal code={} path={} checkId={}",
                    ex.code(), request.getRequestURI(), checkIdOf(request));
        }
        return problem(ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        if (!CHECK_ID_PARAMETER.equals(ex.getName())) {
            return handleUnexpected(ex, request);
        }
        return refusal(ReportStoreErrorCodes.CHECK_ID_INVALID, ex, ex.getName(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        String parameter = ex.getParameterName();
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (CHECKS_PATH.equals(pattern) && REQUEST_KEYS.contains(parameter)) {
            return refusal(ReportStoreErrorCodes.REQUEST_KEYS_MISSING, ex, parameter, request);
        }
        if (DECISION_AGREEMENT_PATH.equals(pattern) && SERVICE_CODE_PARAMETER.equals(parameter)) {
            return refusal(ReportStoreErrorCodes.SERVICE_CODE_MISSING, ex, parameter, request);
        }
        return handleUnexpected(ex, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        ReportStoreException failure =
                new ReportStoreException(ReportStoreErrorCodes.UNEXPECTED_FAILURE, ex);
        log.error("RPT unexpected failure code={} path={} checkId={}",
                failure.code(), request.getRequestURI(), checkIdOf(request), ex);
        return problem(failure);
    }

    private static ProblemDetail refusal(
            String code, Exception cause, String parameter, HttpServletRequest request) {
        ReportStoreException refusal = new ReportStoreException(code, cause);
        log.debug("RPT refusal code={} path={} parameter={}",
                refusal.code(), request.getRequestURI(), parameter);
        return problem(refusal);
    }

    /** The {@code checkId} path variable as the request carried it, or {@code null}. */
    private static String checkIdOf(HttpServletRequest request) {
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables instanceof Map<?, ?> map && map.get(CHECK_ID_PARAMETER) instanceof String checkId) {
            return checkId;
        }
        return null;
    }

    private static ProblemDetail problem(ReportStoreException ex) {
        return Problems.of(ex.status(), ex.getMessage(), ex.code());
    }
}
