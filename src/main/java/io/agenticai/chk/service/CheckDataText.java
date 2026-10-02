package io.agenticai.chk.service;

import io.agenticai.chk.port.DocumentContent;
import io.agenticai.chk.port.DocumentOutcome;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The Check's own data as the texts the evidence of a finding is searched in (REQ-CHK-026,
 * REQ-CHK-029; ADR-CHK-003): each read query's rows as JSON — the form the comparison model
 * received them in — plus every non-null cell value on its own, and each READ document's text, or
 * its sheets as JSON plus every cell on its own. Nothing else of any other Check (REQ-CHK-063).
 * The texts are working data of the Check and are dropped with it (G9). Plain Java, no state.
 */
final class CheckDataText {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private CheckDataText() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    static List<String> of(Map<String, List<Map<String, Object>>> queryResults, List<DocumentOutcome> readDocuments) {
        Objects.requireNonNull(queryResults, "queryResults");
        Objects.requireNonNull(readDocuments, "readDocuments");
        List<String> texts = new ArrayList<>();
        queryResults.values().forEach(rows -> {
            texts.add(json(rows));
            rows.forEach(row -> row.values().forEach(value -> {
                if (value != null) {
                    texts.add(String.valueOf(value));
                }
            }));
        });
        for (DocumentOutcome document : readDocuments) {
            switch (document.content()) {
                case DocumentContent.Text text -> texts.add(text.text());
                case DocumentContent.Tables tables -> tables.tables().forEach(table -> {
                    Map<String, Object> sheet = new LinkedHashMap<>();
                    sheet.put("sheetName", table.sheetName());
                    sheet.put("rows", table.rows());
                    texts.add(json(List.of(sheet)));
                    table.rows().forEach(texts::addAll);
                });
                case null -> { }
            }
        }
        return texts;
    }

    private static String json(List<?> value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("the Check's data could not be written as JSON ("
                    + e.getClass().getSimpleName() + ")", e);
        }
    }
}
