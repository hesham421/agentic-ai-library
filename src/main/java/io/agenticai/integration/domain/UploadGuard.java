package io.agenticai.integration.domain;

import io.agenticai.integration.error.IntegrationErrorCodes;
import io.agenticai.integration.error.IntegrationException;

import java.util.Objects;

/**
 * RULE-INT-001 — uploads only while the Check waits for documents (REQ-INT-011): INT hands an
 * upload to Document Access only when the Check's status (DBF-INT-002) is
 * {@value #AWAITING_DOCUMENTS}; otherwise it refuses with INT's own code
 * {@code INT-409-CHECK-NOT-AWAITING-DOCUMENTS} and hands nothing over. Framework-free; built
 * through {@link #forCheck(CheckSnapshot)}.
 */
public final class UploadGuard {

    /**
     * The Check Engine's status code of a Check waiting for documents (CON-CHK-001). The owner's
     * enum is not published in its contract, so INT compares the code (ADR-INT-013).
     */
    static final String AWAITING_DOCUMENTS = "AWAITING_DOCUMENTS";

    private final CheckSnapshot check;

    private UploadGuard(CheckSnapshot check) {
        this.check = check;
    }

    /** The guard of an upload for {@code check}. */
    public static UploadGuard forCheck(CheckSnapshot check) {
        return new UploadGuard(Objects.requireNonNull(check, "check"));
    }

    /** Whether the Check accepts an upload. */
    public boolean allowsUpload() {
        return AWAITING_DOCUMENTS.equals(check.status());
    }

    /**
     * @throws IntegrationException {@code INT-409-CHECK-NOT-AWAITING-DOCUMENTS} when the Check is
     *                              not {@value #AWAITING_DOCUMENTS}
     */
    public void requireUploadAllowed() {
        if (!allowsUpload()) {
            throw new IntegrationException(IntegrationErrorCodes.CHECK_NOT_AWAITING_DOCUMENTS,
                    check.checkId(), check.status());
        }
    }
}
