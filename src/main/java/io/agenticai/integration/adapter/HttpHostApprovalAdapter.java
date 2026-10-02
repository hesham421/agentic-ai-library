package io.agenticai.integration.adapter;

import io.agenticai.integration.config.IntegrationProperties;
import io.agenticai.integration.domain.ApprovalDefinition;
import io.agenticai.integration.error.ApprovalApiFailedException;
import io.agenticai.integration.error.ApprovalApiTimedOutException;
import io.agenticai.integration.error.IntegrationTexts;
import io.agenticai.integration.port.HostApprovalPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Objects;

/**
 * {@link HostApprovalPort} over HTTP — the only network call of INT (ADR-INT-009,
 * ADR-INT-017 (5)). A plain Spring bean: never registered as a model tool, injected only into the
 * Employee Decision service (REQ-INT-029; AIAS-3, AIAS-4).
 *
 * <ol>
 *   <li><b>URI</b> (REQ-INT-031): {@code aias.integration.approval.base-address} + the definition's
 *       path template, its single {@code {…}} placeholder expanded with the request number through
 *       {@link UriComponentsBuilder} in template-encoding mode ({@code encode()} on the builder,
 *       then {@code buildAndExpand}): the template is encoded, and the request number is encoded
 *       strictly as ONE value — reserved characters included, so {@code 2026/77 A} becomes
 *       {@code 2026%2F77%20A} (AC-INT-035). Never string concatenation. (Encoding the components
 *       after expansion would leave a {@code /} of the request number unencoded and split it into
 *       two path segments.)</li>
 *   <li><b>Call</b> (REQ-INT-032): the definition's method with the JSON body
 *       {@code {"checkId": <checkId>, "decidedBy": "<decidedBy>"}} through a {@link RestClient} on a
 *       {@link JdkClientHttpRequestFactory} whose connect timeout and read timeout are both
 *       {@code aias.integration.approval.timeout}; HTTP/1.1, no redirect following.</li>
 *   <li><b>Outcome</b>: any 2xx → success; any other status, a connection failure or no
 *       configured base address → {@link ApprovalApiFailedException} ({@code INT-502}, REQ-INT-036);
 *       a connect or read timeout → {@link ApprovalApiTimedOutException} ({@code INT-504},
 *       REQ-INT-037). The HTTP client's exceptions are translated here, kept as the cause
 *       (E.1.5).</li>
 *   <li><b>Exactly one attempt</b> (REQ-INT-033): no retry interceptor, no retry template. Every
 *       outcome is logged at INFO with the Check identifier, the request number and the status —
 *       never the body or the deciding employee.</li>
 * </ol>
 */
@Component("intHttpHostApprovalAdapter")
public class HttpHostApprovalAdapter implements HostApprovalPort {

    private static final Logger log = LoggerFactory.getLogger(HttpHostApprovalAdapter.class);

    /** Detail keys of {@code INT-502}'s {@code {status}} when there is no HTTP status (messages.properties). */
    static final String DETAIL_NO_ADDRESS = "INT-DETAIL-NO-APPROVAL-ADDRESS";
    static final String DETAIL_UNREACHABLE = "INT-DETAIL-APPROVAL-UNREACHABLE";

    private final URI baseAddress;
    private final Duration timeout;
    private final RestClient restClient;

    public HttpHostApprovalAdapter(IntegrationProperties properties) {
        IntegrationProperties.Approval approval =
                Objects.requireNonNull(properties, "properties").approval();
        this.baseAddress = approval.baseAddress();
        this.timeout = approval.timeout();
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public void approve(ApprovalDefinition definition, String requestNumber, Long checkId, String decidedBy) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(requestNumber, "requestNumber");
        Objects.requireNonNull(checkId, "checkId");
        if (!definition.enabled() || definition.method() == null || definition.pathTemplate() == null) {
            throw new IllegalArgumentException("The Approval API is called only with an enabled definition");
        }

        if (baseAddress == null) {
            String status = IntegrationTexts.english(DETAIL_NO_ADDRESS);
            log.info("Approval API not called: checkId={} requestNumber={} status={}", checkId, requestNumber, status);
            throw new ApprovalApiFailedException(status, null);
        }

        URI uri = UriComponentsBuilder.fromUri(baseAddress)
                .path(definition.pathTemplate())
                .encode()
                .buildAndExpand(requestNumber)
                .toUri();

        int status;
        try {
            status = restClient.method(HttpMethod.valueOf(definition.method()))
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ApprovalCallBody(checkId, decidedBy))
                    .exchange((request, response) -> response.getStatusCode().value());
        } catch (ResourceAccessException failure) {
            if (isTimeout(failure)) {
                log.info("Approval API timed out: checkId={} requestNumber={} status=no answer within {}s",
                        checkId, requestNumber, timeout.toSeconds());
                throw new ApprovalApiTimedOutException(timeout.toSeconds(), failure);
            }
            String unreachable = IntegrationTexts.english(DETAIL_UNREACHABLE);
            log.info("Approval API failed: checkId={} requestNumber={} status={}", checkId, requestNumber, unreachable);
            throw new ApprovalApiFailedException(unreachable, failure);
        }

        if (status < 200 || status > 299) {
            log.info("Approval API failed: checkId={} requestNumber={} status={}", checkId, requestNumber, status);
            throw new ApprovalApiFailedException(String.valueOf(status), null);
        }
        log.info("Approval API executed: checkId={} requestNumber={} status={}", checkId, requestNumber, status);
    }

    /** Whether the I/O failure is a connect or read timeout, anywhere in its cause chain. */
    private static boolean isTimeout(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    /** The JSON body of the call (REQ-INT-032): the Check identifier and the deciding employee. */
    public record ApprovalCallBody(Long checkId, String decidedBy) {
    }
}
