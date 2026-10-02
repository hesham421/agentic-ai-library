package io.agenticai.integration.config;

import io.agenticai.platform.config.CheckLimitsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * The start-up check of ADR-INT-012 point 2 and REQ-INT-014: the upload request limit
 * ({@code aias.integration.upload.request-limit}) must not be below the platform's maximum file
 * size ({@code aias.check.max-file-size}), so a file above that size still reaches Document Access
 * and is reported UNREADABLE / TOO_LARGE instead of being cut off by the transport. A lower limit
 * fails start-up with the reason.
 */
@Configuration(proxyBeanMethods = false)
public class IntegrationLimitsCheck {

    public IntegrationLimitsCheck(IntegrationProperties integration, CheckLimitsProperties checkLimits) {
        DataSize requestLimit = integration.upload().requestLimit();
        DataSize maxFileSize = checkLimits.maxFileSize();
        if (requestLimit.toBytes() < maxFileSize.toBytes()) {
            throw new IllegalStateException(
                    "aias.integration.upload.request-limit (" + requestLimit
                            + ") must not be below aias.check.max-file-size (" + maxFileSize
                            + "): a file above the maximum file size must still reach Document Access"
                            + " (REQ-INT-014, ADR-INT-012)");
        }
    }
}
