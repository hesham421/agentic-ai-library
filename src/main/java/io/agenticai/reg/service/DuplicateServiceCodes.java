package io.agenticai.reg.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RULE-REG-002 across folders (REQ-REG-004, REQ-REG-064, ADR-REG-017): a service code declared, in
 * canonical form, by two or more folders rejects every one of them. Counted over every folder
 * that declared a readable code, whether or not it failed a per-folder rule; a folder that
 * already failed keeps its own first reason, its valid twin is rejected under RULE-REG-002.
 * Pure.
 */
final class DuplicateServiceCodes {

    private DuplicateServiceCodes() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    static List<FolderVerdict> apply(List<FolderVerdict> verdicts) {
        Map<String, Integer> declaredBy = new HashMap<>();
        for (FolderVerdict verdict : verdicts) {
            if (verdict.canonicalCode() != null && !verdict.canonicalCode().isEmpty()) {
                declaredBy.merge(verdict.canonicalCode(), 1, Integer::sum);
            }
        }
        List<FolderVerdict> result = new ArrayList<>(verdicts.size());
        for (FolderVerdict verdict : verdicts) {
            String code = verdict.canonicalCode();
            boolean duplicated = code != null && declaredBy.getOrDefault(code, 0) > 1;
            if (duplicated && !verdict.rejected()) {
                result.add(verdict.rejectedBy(LoadReason.of(LoadReasonCodes.DUPLICATE_SERVICE_CODE, code)));
            } else {
                result.add(verdict);
            }
        }
        return List.copyOf(result);
    }
}
