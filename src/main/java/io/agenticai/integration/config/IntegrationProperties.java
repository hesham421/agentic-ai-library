package io.agenticai.integration.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

/**
 * Environment settings of Host Integration (INT), bound from {@code aias.integration.*} and
 * validated at start-up (ADR-INT-012, CORE R1). The only {@code @ConfigurationProperties} class of
 * this prefix (CU.6).
 *
 * <ul>
 *   <li>{@code aias.integration.approval.timeout} — the connect and read timeout of the host
 *       Approval API call, whole seconds, default {@code 10s} (REQ-INT-037); reaching it answers
 *       {@code INT-504-APPROVAL-API-TIMED-OUT}.</li>
 *   <li>{@code aias.integration.approval.base-address} — the host Approval API's base address in
 *       this environment; the version's definition supplies only the method and the path
 *       (ADR-INT-009). Optional: absent, an approval-path decision answers
 *       {@code INT-502-APPROVAL-API-FAILED} with {@code {status}} = "no answer — no address is
 *       configured" and records nothing.</li>
 *   <li>{@code aias.integration.upload.request-limit} — the largest upload request, default
 *       {@code 50MB} (REQ-INT-014); never below {@code aias.check.max-file-size} (checked by
 *       {@link IntegrationLimitsCheck}). {@code spring.servlet.multipart.max-request-size} and
 *       {@code max-file-size} are bound to it in {@code application.properties} by placeholder.</li>
 * </ul>
 *
 * @param approval the host Approval API settings; never {@code null}
 * @param upload   the upload settings; never {@code null}
 */
@Validated
@ConfigurationProperties(prefix = "aias.integration")
public record IntegrationProperties(@Valid Approval approval, @Valid Upload upload) {

    public IntegrationProperties {
        approval = approval == null ? new Approval(null, null) : approval;
        upload = upload == null ? new Upload(null) : upload;
    }

    /**
     * {@code aias.integration.approval.*}.
     *
     * @param timeout     connect + read timeout, whole seconds, greater than 0; default 10 seconds
     * @param baseAddress the host Approval API base address; {@code null} when not configured
     */
    public record Approval(@NotNull Duration timeout, URI baseAddress) {

        /** Default of {@code aias.integration.approval.timeout} (ADR-INT-012). */
        public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

        public Approval {
            timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
            if (timeout.isZero() || timeout.isNegative() || timeout.toNanosPart() != 0) {
                throw new IllegalArgumentException(
                        "aias.integration.approval.timeout must be a whole number of seconds greater than 0: "
                                + timeout);
            }
        }

        /** Whether a base address is configured for this environment. */
        public boolean hasBaseAddress() {
            return baseAddress != null;
        }
    }

    /**
     * {@code aias.integration.upload.*}.
     *
     * @param requestLimit the largest upload request; default 50 MB, greater than 0
     */
    public record Upload(@NotNull DataSize requestLimit) {

        /** Default of {@code aias.integration.upload.request-limit} (ADR-INT-012). */
        public static final DataSize DEFAULT_REQUEST_LIMIT = DataSize.ofMegabytes(50);

        public Upload {
            requestLimit = requestLimit == null ? DEFAULT_REQUEST_LIMIT : requestLimit;
            if (requestLimit.toBytes() <= 0) {
                throw new IllegalArgumentException(
                        "aias.integration.upload.request-limit must be greater than 0: " + requestLimit);
            }
        }
    }
}
