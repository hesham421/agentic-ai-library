package io.agenticai.reg.service;

import io.agenticai.reg.domain.ParsedServiceDefinition;

import java.util.Objects;

/**
 * The outcome of validating one package folder (load run step 3): what the folder declared, for
 * its Load Result row and for RULE-REG-002, and the first rule it failed, if any.
 *
 * @param folderName      the folder's name — the row's subject
 * @param canonicalCode   the declared service code in canonical form (REQ-REG-064), or
 *                        {@code null} when the folder declared no readable code
 * @param declaredVersion the declared version number, or {@code null} when unreadable
 * @param knowledgeText   the service knowledge text; {@code null} when absent or unreadable
 * @param definitionText  the service definition text; {@code null} when absent or unreadable
 * @param definition      the parsed definition — present only when every rule passed
 * @param rejection       the first failed rule's load reason; {@code null} when accepted
 */
record FolderVerdict(String folderName,
                     String canonicalCode,
                     Integer declaredVersion,
                     String knowledgeText,
                     String definitionText,
                     ParsedServiceDefinition definition,
                     LoadReason rejection) {

    FolderVerdict {
        Objects.requireNonNull(folderName, "folderName");
    }

    boolean rejected() {
        return rejection != null;
    }

    FolderVerdict rejectedBy(LoadReason reason) {
        return new FolderVerdict(folderName, canonicalCode, declaredVersion, knowledgeText, definitionText,
                definition, Objects.requireNonNull(reason, "reason"));
    }

    FolderVerdict withDefinition(ParsedServiceDefinition parsed) {
        return new FolderVerdict(folderName, canonicalCode, declaredVersion, knowledgeText, definitionText,
                parsed, rejection);
    }
}
