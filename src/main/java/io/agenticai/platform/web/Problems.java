package io.agenticai.platform.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/**
 * Builds the RFC 9457 {@link ProblemDetail} every module's ProblemAdvice answers with:
 * {@code type}, {@code title}, {@code status}, {@code detail} and the extension member
 * {@code code}. Module-neutral platform piece; each advice keeps its own exception mappings and
 * only delegates the body's construction here.
 *
 * <p><b>Why {@code type} is set explicitly.</b> Since Spring Framework 7, {@code ProblemDetail}'s
 * {@code type} field is {@code null} unless set (6.x defaulted it to {@code about:blank}), and
 * Spring's {@code ProblemDetailJacksonMixin} carries {@code @JsonInclude(NON_EMPTY)}, so an unset
 * type is omitted from the JSON. The API documents require {@code type}; RFC 9457 §4.2.1 defines
 * {@code about:blank} as the value when no problem-specific type URI exists — no type URI is
 * invented here.
 */
public final class Problems {

    /** RFC 9457 §4.2.1: the problem has no semantics beyond the HTTP status code. */
    public static final URI ABOUT_BLANK = URI.create("about:blank");

    private Problems() {
    }

    /**
     * A ProblemDetail for {@code status} with the given {@code detail} (the catalog message), the
     * reason phrase as {@code title}, {@code about:blank} as {@code type}, and {@code code}.
     */
    public static ProblemDetail of(int status, String detail, String code) {
        return of(HttpStatus.valueOf(status), detail, code);
    }

    /** See {@link #of(int, String, String)}. */
    public static ProblemDetail of(HttpStatus status, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(ABOUT_BLANK);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return problem;
    }
}
