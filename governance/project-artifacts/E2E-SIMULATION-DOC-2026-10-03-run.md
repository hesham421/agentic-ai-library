# aias E2E simulation run 20261003T105921Z

Base URL `http://localhost:7271` · comparison model `gemini-3.6-flash` · model calls: 9 comparison, 4 reading

| Group | Passed | Failed | Skipped | Ambiguous | Not exercisable | Error |
|---|---|---|---|---|---|---|
| doc-path | 20 | 2 | 0 | 0 | 0 | 0 |
| doc-blob | 1 | 1 | 0 | 0 | 0 | 0 |
| doc-manual | 5 | 0 | 0 | 0 | 0 | 0 |
| doc-noroot | 3 | 1 | 0 | 0 | 0 | 0 |
| doc-inprocess | 0 | 0 | 0 | 0 | 15 | 0 |

## Scenarios

- **PASSED** [doc-path] A path with no file is NOT_FOUND, its detail naming the path (TC-DOC-002)
- **PASSED** [doc-path] '..' segments are resolved before the storage-root check (TC-DOC-003)
- **PASSED** [doc-path] An absolute path outside the storage root is refused unopened (TC-DOC-004)
- **PASSED** [doc-path] A symbolic link pointing outside the storage root is refused unopened (TC-DOC-006)
- **PASSED** [doc-path] The storage root is taken only from the environment setting (TC-DOC-007)
- **PASSED** [doc-path] Unsupported formats by content signature: .docx, random bytes, a .pdf name without PDF content (TC-DOC-016)
- **PASSED** [doc-path] A password-protected PDF is READING_FAILED (TC-DOC-017)
- **PASSED** [doc-path] A damaged PDF and a damaged workbook are READING_FAILED (REQ-DOC-029)
- **PASSED** [doc-path] .xlsx read by table extraction: 1 table of 3 rows x 2 columns with the cell values (TC-DOC-043)
- **PASSED** [doc-path] .xls (BIFF8) read by table extraction (REQ-DOC-026)
- **PASSED** [doc-path] Exactly 10 MB is READ; 10 MB + 1 byte is TOO_LARGE naming both sizes (TC-DOC-026, TC-DOC-018, TC-DOC-025)
- **PASSED** [doc-path] Exactly the maximum rows (100) is accepted: 100 outcomes, one per row (TC-DOC-021, TC-DOC-044)
- **FAILED** [doc-path] Documents come only by the version's fetch mode (path); its version and its type column are used (TC-DOC-032, TC-DOC-033, TC-DOC-036, AC-DOC-002)
  - FAIL TC-DOC-036: row 14's type column ID_CARD: `{'position': 14, 'documentType': 'ID_CARD', 'sourceMode': 'path', 'readStatus': 'UNREADABLE', 'unreadableReason': 'READING_FAILED', 'detail': 'the document-reading call failed: BadRequestException: 400: [{"error":{"code":400,"message":"Invalid content part type: file","status":"INVALID_ARGUMENT"}}]'`
- **PASSED** [doc-path] Read content is handed to the Check Engine with its outcome (GPA 3.6) (TC-DOC-046)
- **PASSED** [doc-path] Host files are opened for reading only and stay unchanged after the Check (TC-DOC-048, TC-DOC-049)
- **PASSED** [doc-path] The document source query is sent exactly as written; the request number is one bound value (TC-DOC-050, AC-DOC-054)
- **PASSED** [doc-path] No host endpoint is called by the fetch, the Approval API included (TC-DOC-051)
- **PASSED** [doc-path] Instruction-like text inside a document stays content (TC-DOC-061)
- **FAILED** [doc-path] A PDF without a text layer is read in the document-reading step (TC-DOC-055)
  - FAIL id-scan.pdf: ID_CARD READ: `{'position': 14, 'documentType': 'ID_CARD', 'sourceMode': 'path', 'readStatus': 'UNREADABLE', 'unreadableReason': 'READING_FAILED', 'detail': 'the document-reading call failed: BadRequestException: 400: [{"error":{"code":400,"message":"Invalid content part type: file","status":"INVALID_ARGUMENT"}}]'`
  - FAIL log: no text layer (0 characters): ``
- **PASSED** [doc-path] Document-reading model: its own configuration, fixed instruction plus the document only, no tool, one document per call (TC-DOC-056, TC-DOC-060, TC-DOC-062, TC-DOC-063)
- **PASSED** [doc-path] A document source query over the maximum rows fails every required type, nothing opened (TC-DOC-020)
- **PASSED** [doc-path] An error of the MCP channel on the document source query fails every required type — retried once after: check 215 ended MODEL_UNAVAILABLE: the comparison model call failed: InternalServerException: 503: [{"error":{"code":503,"message":"This model is currently expe (TC-DOC-022)
- **FAILED** [doc-blob] A NULL (or empty) BLOB content column is NOT_FOUND; the other rows are read (TC-DOC-010)
  - FAIL 4 blob outcomes: `[{'position': 1, 'documentType': 'ID_CARD', 'sourceMode': 'blob', 'readStatus': 'UNREADABLE', 'unreadableReason': 'SOURCE_QUERY_FAILED', 'detail': 'the document source query failed: 99999/SQLException: ORA-22849: Type BLOB is not supported for this function or operator.\n\nhttps://docs.oracle.com/er`
  - FAIL TRANSCRIPT BLOB READ: `{'position': 1, 'documentType': 'ID_CARD', 'sourceMode': 'blob', 'readStatus': 'UNREADABLE', 'unreadableReason': 'SOURCE_QUERY_FAILED', 'detail': 'the document source query failed: 99999/SQLException: ORA-22849: Type BLOB is not supported for this function or operator.\n\nhttps://docs.oracle.com/err`
  - FAIL NULL content column: ID_CARD UNREADABLE / NOT_FOUND: `{'position': 2, 'documentType': 'TRANSCRIPT', 'sourceMode': 'blob', 'readStatus': 'UNREADABLE', 'unreadableReason': 'SOURCE_QUERY_FAILED', 'detail': 'the document source query failed: 99999/SQLException: ORA-22849: Type BLOB is not supported for this function or operator.\n\nhttps://docs.oracle.com/`
  - FAIL EMPTY_BLOB(): ID_CARD UNREADABLE / NOT_FOUND: `{}`
  - FAIL random bytes: UNREADABLE / UNSUPPORTED_FORMAT: `{}`
- **PASSED** [doc-blob] BLOB content never goes through the MCP query channel (TC-DOC-039)
- **PASSED** [doc-manual] manual fetch reads only the Check's own uploads; another Check's upload is never supplied (TC-DOC-011, TC-DOC-031)
- **PASSED** [doc-manual] manual mode touches no host document (TC-DOC-041)
- **PASSED** [doc-manual] Upload of exactly the maximum file size keeps its content (read at fetch) (TC-DOC-028)
- **PASSED** [doc-manual] An oversized upload is reported TOO_LARGE at fetch (TC-DOC-029, TC-DOC-027)
- **PASSED** [doc-manual] Format from the content signature, not the file name: scan.pdf holding a PNG goes to the model as an image (TC-DOC-054)
- **PASSED** [doc-noroot] No storage root set closes every path document (TC-DOC-008)
- **PASSED** [doc-noroot] No fetched content is kept between Checks (TC-DOC-053)
- **PASSED** [doc-noroot] No document-reading model configured: the PNG is READING_FAILED, the text PDF READ (TC-DOC-059)
- **FAILED** [doc-noroot] Uploaded Documents of a Check listed in upload order without content; none -> empty list (TC-DOC-076, TC-DOC-077)
  - FAIL each item: uploadedDocumentId, documentType, fileName, fileSize, oversized, uploadedAt — no content member: `[['createdAt', 'documentType', 'fileName', 'fileSize', 'oversized', 'uploadedDocumentId'], ['createdAt', 'documentType', 'fileName', 'fileSize', 'oversized', 'uploadedDocumentId']]`
- **NOT-EXERCISABLE** [doc-inprocess] Unresolvable service package version refused before any document is fetched — a Check is always pinned to the version REG supplied at its start and REG never deletes a stored version (TC-REG-027): no public operation makes fetchDocuments name an unstored version (TC-DOC-001)
- **NOT-EXERCISABLE** [doc-inprocess] blob query naming a non-jdbc connection refused without running — unreachable through the registry: REG rejects a blob document source over an mcp connection at load (TC-REG-042) and keeps a blob version's connection of type jdbc (RULE-REG-025, TC-REG-092), so no stored blob version reaches DOC over a non-jdbc connection (TC-DOC-009)
- **NOT-EXERCISABLE** [doc-inprocess] Check timeout reached during reading marks unread documents OUT_OF_TIME — needs an in-process reading-model stub that answers after the deadline, and the outcomes are not observable: the pipeline's next deadline check ends the Check FAILED / TIMED_OUT, whose report keeps no document outcome (TC-DOC-023)
- **NOT-EXERCISABLE** [doc-inprocess] Connection not declared read-only refused without running the query — REG refuses to activate a connection not declared read-only (TC-REG-057), and REG's getCurrentServicePackage refuses a Check whose query names an unregistered connection (RULE-REG-017 -> 422 CHK-422-CONNECTION-NOT-ACTIVATED, TC-REG-057's E2E) before DOC runs (TC-DOC-030)
- **NOT-EXERCISABLE** [doc-inprocess] Content handed over only as data, with no instruction field — the members of DocumentOutcome are an in-process type no API returns; the report omits content. Partial evidence (TC-DOC-046/061): the content reaches the comparison only inside its data block (TC-DOC-047)
- **NOT-EXERCISABLE** [doc-inprocess] Document-reading model replaced by configuration alone — needs a further mode (a third reading model) with one more reading and one more comparison call, beyond this run's model budget (8 comparison / 4 reading calls) (TC-DOC-057)
- **NOT-EXERCISABLE** [doc-inprocess] Provider-neutral model access — only one provider (the OpenAI-compatible Gemini endpoint) is configured locally; a second provider is not available (TC-DOC-058)
- **NOT-EXERCISABLE** [doc-inprocess] REG version read fails — DOC returns its defined not-found result — as TC-DOC-001: every Check is pinned to a stored version and INT hands over uploads with the Check's own pinned version, which REG always resolves (TC-DOC-066)
- **NOT-EXERCISABLE** [doc-inprocess] REG read yields no document source query — REG rejects at load a path/blob version whose document source names no declared query (RULE-REG-009, TC-REG-084), so getServicePackageVersion never yields one (TC-DOC-067)
- **NOT-EXERCISABLE** [doc-inprocess] REG read yields an empty required-type set — no MISSING outcome — the precondition (an Uploaded Document of a type, on a version whose required-type set is empty) cannot be created: RULE-DOC-002 refuses every handover for such a version. Partial evidence asserted green in this scenario (TC-DOC-068)
- **NOT-EXERCISABLE** [doc-inprocess] REG connection read fails — every required type SOURCE_QUERY_FAILED — REG's getCurrentServicePackage refuses the start of a Check whose any query (the document source query included) names an unregistered connection (RULE-REG-017 -> 422 CHK-422-CONNECTION-NOT-ACTIVATED, TC-REG-092's E2E): DOC never runs for it (TC-DOC-069)
- **NOT-EXERCISABLE** [doc-inprocess] Upload refused for a Check already ended — INT refuses an upload for a Check that no longer awaits documents (INT-409) before DOC's handover runs; DOC's RULE-DOC-009 is reachable only in the race of TC-INT-096 (AMBIGUOUS). Partial evidence asserted green in this scenario (TC-DOC-070)
- **NOT-EXERCISABLE** [doc-inprocess] End of a Check recorded as an Ended Check — step 3 expects DOC's CheckEndedException (DOC-409-CHECK-ENDED), which INT pre-empts with INT-409; step 2 reads DOC_ENDED_CHECK, which no API returns. Partial evidence asserted green (TC-DOC-073)
- **NOT-EXERCISABLE** [doc-inprocess] Repeated end of a Check keeps one Ended Check and raises no error — no public operation ends a Check twice: CHK sends one end notice per ending (a second only after a failure of the first) and the count of DOC_ENDED_CHECK rows is not returned by any API (TC-DOC-074)
- **NOT-EXERCISABLE** [doc-inprocess] Late upload of an ended Check swept at the next end of a Check — its precondition is a row inserted directly into DOC_UPLOADED_DOC (the race of ADR-DOC-015); the runner makes no direct database writes and the race is not reproducible through the API (TC-DOC-075)

## App restarts (restart groups)

- 2026-10-03T14:59:28 — mode [aias.registry.package-directory=local/e2e-doc/packages, aias.documents.storage-root=local/e2e-doc/root, aias.check.max-rows=100, aias.check.timeout=PT5M, aias.check.comparison-model.model=gemini-3.7-flash, aias.documents.reading-model.tier=APPROVED, aias.documents.reading-model.instruction=Transcribe the document text exactly., spring.ai.openai.base-url=http://127.0.0.1:7292/v1beta/openai, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, connections=['local-oracle', 'local-jdbc']] (by 'A path with no file is NOT_FOUND, its detail naming the path')
- 2026-10-03T15:02:36 — mode [aias.registry.package-directory=local/e2e-doc/packages, aias.documents.storage-root=, aias.check.max-rows=100, aias.check.timeout=PT5M, aias.check.comparison-model.model=gemini-3.7-flash, aias.documents.reading-model.tier=APPROVED, aias.documents.reading-model.instruction=Transcribe the document text exactly., spring.ai.openai.base-url=http://127.0.0.1:7292/v1beta/openai, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, aias.documents.reading-model.provider=, aias.check.max-file-size=1MB, connections=['local-oracle', 'local-jdbc']] (by 'No storage root set closes every path document')
- 2026-10-03T15:03:47 — mode [normal] (by '(end of run)')

## Surviving records (synthetic, kept until the retention purge)

| checkId | service | requestNumber | what |
|---|---|---|---|
| 213 | doc-path-t1003105921 | `E2E-t1003105921-1001' OR '1'='1` | started by 'A path with no file is NOT_FOUND, its detail naming the path' |
| 214 | doc-path-t1003105921 | `E2E-t1003105921-ROWS101` | started by 'A document source query over the maximum rows fails every required type, nothing opened' |
| 215 | doc-path-t1003105921 | `E2E-t1003105921-MCPERR` | started by 'An error of the MCP channel on the document source query fails every required type' |
| 216 | doc-path-t1003105921 | `E2E-t1003105921-MCPERR` | started by 'An error of the MCP channel on the document source query fails every required type' |
| 217 | doc-blob-t1003105921 | `E2E-t1003105921-BLOB` | started by 'A NULL (or empty) BLOB content column is NOT_FOUND; the other rows are read' |
| 218 | doc-manual-t1003105921 | `E2E-t1003105921-502` | started by 'manual fetch reads only the Check's own uploads; another Check's upload is never supplied' |
| 219 | doc-manual-t1003105921 | `E2E-t1003105921-501` | started by 'manual fetch reads only the Check's own uploads; another Check's upload is never supplied' |
| 220 | doc-path-t1003105921 | `E2E-t1003105921-2002` | started by 'No storage root set closes every path document' |
| 221 | doc-manual-t1003105921 | `E2E-t1003105921-NOMODEL` | started by 'No document-reading model configured: the PNG is READING_FAILED, the text PDF READ' |
| 222 | doc-manual-t1003105921 | `E2E-t1003105921-L501` | started by 'Uploaded Documents of a Check listed in upload order without content; none -> empty list' |
| 223 | doc-manual-t1003105921 | `E2E-t1003105921-L502` | started by 'Uploaded Documents of a Check listed in upload order without content; none -> empty list' |
| 224 | doc-manual-t1003105921 | `E2E-t1003105921-L503` | started by 'Uploaded Documents of a Check listed in upload order without content; none -> empty list' |
| 225 | doc-noreq-t1003105921 | `E2E-t1003105921-NOREQ` | started by 'REG read yields an empty required-type set — no MISSING outcome' |
