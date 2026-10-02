package io.agenticai.integration.service;

import io.agenticai.integration.domain.CheckSnapshot;
import io.agenticai.integration.dto.RequiredDocumentTypesResponse;
import io.agenticai.integration.port.CheckRecordPort;
import io.agenticai.integration.port.VersionDocumentsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * API-INT-008 — the required document types of the version a Check runs on (CON-INT-008;
 * REQ-INT-064, ADR-INT-020 (5)). Load the Check ({@link CheckRecordPort#read}; unknown →
 * {@code RPT-404-CHECK-NOT-FOUND}, passed through), then the required document types of ITS service
 * code (DBF-INT-003) and version (DBF-INT-004) — never the current version — through
 * {@link VersionDocumentsPort}. Writes nothing; keeps nothing (REQ-INT-059).
 *
 * <p>No {@code @Transactional}: INT owns no table; each owner's read is its own.
 */
@Service("intRequiredDocumentTypesService")
public class RequiredDocumentTypesService {

    private static final Logger log = LoggerFactory.getLogger(RequiredDocumentTypesService.class);

    private final CheckRecordPort checkRecords;
    private final VersionDocumentsPort versionDocuments;

    public RequiredDocumentTypesService(CheckRecordPort checkRecords, VersionDocumentsPort versionDocuments) {
        this.checkRecords = Objects.requireNonNull(checkRecords, "checkRecords");
        this.versionDocuments = Objects.requireNonNull(versionDocuments, "versionDocuments");
    }

    /** The required document types of the version the Check {@code checkId} runs on. */
    public RequiredDocumentTypesResponse read(Long checkId) {
        Objects.requireNonNull(checkId, "checkId");
        CheckSnapshot check = checkRecords.read(checkId);
        List<String> types = versionDocuments.requiredDocumentTypes(check.serviceCode(), check.versionNumber());
        log.debug("INT read required document types checkId={} versionNumber={} returned={}",
                checkId, check.versionNumber(), types.size());
        return new RequiredDocumentTypesResponse(check.checkId(), check.serviceCode(), check.versionNumber(), types);
    }
}
