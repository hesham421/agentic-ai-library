# TEST REPORT — DOC backend v1 — 2026-10-03

Test phase close-out per `.claude/commands/DOC/execute-backend-test.md` STEP 1/4/5/6 and `.claude/commands/orchestrate-module.md` STEP 4. **No JUnit** (user policy 2026-10-02): the evidence is the api-verify scripts (`governance/shared/backend/modules/*/test-api/`, run 2026-10-03, all five exit 0, `api_verify` v1 recorded PASS) and the E2E simulation run `20261003T092840Z` ([`E2E-SIMULATION-2026-10-03.json`](E2E-SIMULATION-2026-10-03.json)). An id counts as PASS only with green evidence tagged with that id; NOT-EXERCISABLE is never counted as passed.

## STEP 4 — Coverage cross-check (governed plan ↔ tests)

**Ratio: 25 / 87 required ids PASS (28.7 %)** — PASS 25 · FAIL 0 · GAP 50 · NOT-EXERCISABLE 12 · DEFERRED 0 · OUT-OF-TRACK 0.

- TC blocks of the P4 test plan: 77 — PASS 20 · FAIL 0 · GAP 45 · NOT-EXERCISABLE 12.
- AC ids listed in the built integration packages' `tests` (mapped to TCs through "Derived from"; an AC is PASS only when every TC derived from it is PASS): 10 — PASS 5 · GAP 5 · NOT-EXERCISABLE 0 · FAIL 0.
- Every id listed in the `tests` of the exec units, test units and built integration packages is a TC block of the test plan or one of the AC rows above (no id is dropped). No integration package is deferred (every `requires` is met), so no row is DEFERRED.

```
GOVERNED PLAN ↔ TESTS — DOC v1
```

| TC/AC id | traces | scenario | evidence (api-verify check / E2E scenario) | result |
|---|---|---|---|---|
| TC-DOC-001 | AC-DOC-003 | Unresolvable service package version refused before any document is fetched | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-002 | AC-DOC-007 | `path` document with no file at its path reported NOT_FOUND | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-003 | AC-DOC-008 | `..` segments resolved before the storage-root check | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-004 | AC-DOC-009 | Absolute path outside the storage root refused unopened | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-005 | AC-DOC-010 | Relative traversal out of the storage root refused unopened | E2E [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outs → PASSED | PASS |
| TC-DOC-006 | AC-DOC-011 | Symbolic link pointing outside the storage root refused unopened | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-007 | AC-DOC-012 | Storage root taken only from the environment setting | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-008 | AC-DOC-013 | No storage root set closes every `path` document | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-009 | AC-DOC-016 | `blob` query naming a non-jdbc connection refused without running | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-010 | AC-DOC-017 | Empty BLOB content column reported NOT_FOUND | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-011 | AC-DOC-020 | `manual` fetch reads only the Check's own uploads | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-012 | AC-DOC-022 API-DOC-001 | Upload refused for a service whose fetch mode is not `manual` | E2E [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outs → PASSED | PASS |
| TC-DOC-013 | AC-DOC-023 API-DOC-001 | Upload refused for a document type outside the version's list | E2E [refusals] Upload refusals and limits on a waiting manual Check → PASSED | PASS |
| TC-DOC-014 | AC-DOC-024 API-DOC-001 | Upload of a 0-byte file refused | E2E [refusals] Upload refusals and limits on a waiting manual Check → PASSED | PASS |
| TC-DOC-015 | AC-DOC-025 API-DOC-001 | A second upload never changes the first | E2E [refusals] Upload refusals and limits on a waiting manual Check → PASSED | PASS |
| TC-DOC-016 | AC-DOC-030 | Unsupported format reported UNSUPPORTED_FORMAT | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-017 | AC-DOC-031 | Password-protected PDF reported READING_FAILED | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-018 | AC-DOC-038 | TOO_LARGE outcome names the file size and the maximum file size | E2E [limits] max-file-size 1 KB: a 4.5 KB path document is UNREADABLE / TOO_LARGE w → PASSED<br>E2E [limits] max-file-size 1 KB: a 2000-byte BLOB is UNREADABLE / TOO_LARGE, measur → PASSED | PASS |
| TC-DOC-019 | AC-DOC-039 | One document outside the storage root does not stop the others | E2E [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outs → PASSED | PASS |
| TC-DOC-020 | AC-DOC-041 | Document source query over the maximum rows fails every required type | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-021 | AC-DOC-041 | Document source query at exactly the maximum rows is accepted (boundary) | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-022 | AC-DOC-042 | MCP error on the document source query fails every required type | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-023 | AC-DOC-043 | Check timeout reached during reading marks unread documents OUT_OF_TIME | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-024 | AC-DOC-044 | Oversized BLOB measured before any byte is read | E2E [limits] max-file-size 1 KB: a 2000-byte BLOB is UNREADABLE / TOO_LARGE, measur → PASSED | PASS |
| TC-DOC-025 | AC-DOC-045 | Oversized `path` document reported TOO_LARGE | E2E [limits] max-file-size 1 KB: a 4.5 KB path document is UNREADABLE / TOO_LARGE w → PASSED | PASS |
| TC-DOC-026 | AC-DOC-045 | `path` document of exactly the maximum file size is read (boundary) | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-027 | AC-DOC-046 API-DOC-001 | Oversized upload kept without content, with the RULE-DOC-005 notice | E2E [refusals] Upload refusals and limits on a waiting manual Check → PASSED | PASS |
| TC-DOC-028 | AC-DOC-046 API-DOC-001 | Upload of exactly the maximum file size keeps its content (boundary) | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-029 | AC-DOC-047 | Oversized upload reported TOO_LARGE at fetch | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-030 | AC-DOC-055 | Connection not declared read-only refused without running the query | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-031 | AC-DOC-059 | Another Check's upload never supplied | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-070 | AC-DOC-065 API-DOC-001 | Upload refused for a Check already ended | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-071 | AC-DOC-067 API-DOC-001 | Upload refused once the Check holds the maximum uploads | E2E [refusals] Maximum uploads per Check: 20 accepted, the 21st refused → PASSED | PASS |
| TC-DOC-072 | AC-DOC-068 API-DOC-001 | Upload accepted one below the maximum uploads (boundary) | E2E [refusals] Maximum uploads per Check: 20 accepted, the 21st refused → PASSED | PASS |
| TC-DOC-032 | AC-DOC-001 | Documents obtained only by the version's fetch mode (`path`) | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-033 | AC-DOC-002 | Document settings read from the version the Check names | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-034 | AC-DOC-004 | `path` document source query sent once through the MCP channel with the request number bound | E2E [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outs → PASSED | PASS |
| TC-DOC-035 | AC-DOC-005 | `path` document read at the returned path | E2E [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outs → PASSED | PASS |
| TC-DOC-036 | AC-DOC-006 | Document type taken from the type column | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-037 | AC-DOC-014 | `blob` document source query run over the read-only jdbc connection, not MCP | E2E [blob] Blob flow: the TRANSCRIPT BLOB is read over the jdbc connection local- → PASSED | PASS |
| TC-DOC-038 | AC-DOC-015 | `blob` content read from the content column | E2E [blob] Blob flow: the TRANSCRIPT BLOB is read over the jdbc connection local- → PASSED | PASS |
| TC-DOC-039 | AC-DOC-018 | BLOB content never through the MCP query channel | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-040 | AC-DOC-019 API-DOC-001 | Upload handed over by INT creates an Uploaded Document | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-DOC-041 | AC-DOC-021 | `manual` mode touches no host document | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-042 | AC-DOC-027 | PDF with a text layer read by text extraction | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-DOC-043 | AC-DOC-028 | `.xlsx` read by table extraction | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-044 | AC-DOC-036 | Exactly one outcome per document | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-045 | AC-DOC-037 | MISSING outcome for a required type with no document | E2E [manual] Missing document: confirm without an upload -> TRANSCRIPT MISSING, NOT → PASSED | PASS |
| TC-DOC-046 | AC-DOC-040 | Read content handed to the Check Engine with its outcome | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-047 | AC-DOC-048 | Content handed over only as data, with no instruction field | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-048 | AC-DOC-052 | Host file opened for reading only | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-049 | AC-DOC-053 | Host documents unchanged after a Check | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-050 | AC-DOC-054 | Document source query sent exactly as written; request number bound, never concatenated | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-051 | AC-DOC-056 | No host endpoint called, the Approval API included | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-052 | AC-DOC-057 API-DOC-001 | Uploads deleted when their Check ends | E2E [lifecycle] After the Check ends: no Active Check, and its uploads are deleted → PASSED<br>E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRE → PASSED | PASS |
| TC-DOC-053 | AC-DOC-058 | No fetched content kept between Checks | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-073 | AC-DOC-063 | End of a Check recorded as an Ended Check | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-074 | AC-DOC-064 | Repeated end of a Check keeps one Ended Check and raises no error | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-075 | AC-DOC-066 API-DOC-001 | Late upload of an ended Check swept at the next end of a Check | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-076 | AC-DOC-069 API-DOC-001 | Uploaded Documents of a Check listed in upload order without content | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-077 | AC-DOC-070 | Listing the Uploaded Documents of a Check with none returns an empty list | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-054 | AC-DOC-026 | Format detected from the content signature, not the file name | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-DOC-055 | AC-DOC-029 | PDF without a text layer read in the document-reading step | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-DOC-056 | AC-DOC-032 | Document-reading model called through its own configuration | E2E [image] A PNG transcript (no text layer) is read by the document-reading model → PASSED | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-DOC-057 | AC-DOC-033 | Document-reading model replaced by configuration alone | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-DOC-058 | AC-DOC-034 | Provider-neutral model access | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-DOC-059 | AC-DOC-035 | No document-reading model configured | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-DOC-060 | AC-DOC-049 | Fixed reading instruction and the document only | E2E [image] A PNG transcript (no text layer) is read by the document-reading model → PASSED | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-DOC-061 | AC-DOC-050 | Instruction-like text inside a document stays content | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-DOC-062 | AC-DOC-051 | No tool given to the document-reading model | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-DOC-063 | AC-DOC-060 | One document per model call | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-DOC-064 | AC-DOC-061 | Free-tier model receives no document when the data class is REAL | E2E [notpermitted] FREE reading model on REAL data: a scanned (PNG) upload is never sent; → PASSED<br>E2E [limits] REAL data, FREE reading model: the PNG is UNREADABLE / MODEL_NOT_PERMI → PASSED | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-DOC-065 | AC-DOC-062 | Document held back from a free-tier model reported MODEL_NOT_PERMITTED | E2E [limits] REAL data, FREE reading model: the PNG is UNREADABLE / MODEL_NOT_PERMI → PASSED | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-DOC-066 | XM-DOC-001 | REG version read fails — DOC returns its defined not-found result | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-067 | XM-DOC-002 | REG read yields no document source query — every required type SOURCE_QUERY_FAILED | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-068 | XM-DOC-003 | REG read yields an empty required-type set — defined result, no MISSING outcome | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-DOC-069 | XM-DOC-004 | REG connection read fails — every required type SOURCE_QUERY_FAILED | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| AC-DOC-002 | AC-DOC-002 | AC — via TC-DOC-033 | via TC-DOC-033 | **GAP** — derived TC(s) not green: TC-DOC-033 |
| AC-DOC-003 | AC-DOC-003 | AC — via TC-DOC-001 | via TC-DOC-001 | **GAP** — derived TC(s) not green: TC-DOC-001 |
| AC-DOC-004 | AC-DOC-004 | AC — via TC-DOC-034 | via TC-DOC-034 | PASS |
| AC-DOC-014 | AC-DOC-014 | AC — via TC-DOC-037 | via TC-DOC-037 | PASS |
| AC-DOC-016 | AC-DOC-016 | AC — via TC-DOC-009 | via TC-DOC-009 | **GAP** — derived TC(s) not green: TC-DOC-009 |
| AC-DOC-022 | AC-DOC-022 | AC — via TC-DOC-012 | via TC-DOC-012 | PASS |
| AC-DOC-023 | AC-DOC-023 | AC — via TC-DOC-013 | via TC-DOC-013 | PASS |
| AC-DOC-037 | AC-DOC-037 | AC — via TC-DOC-045 | via TC-DOC-045 | PASS |
| AC-DOC-054 | AC-DOC-054 | AC — via TC-DOC-050 | via TC-DOC-050 | **GAP** — derived TC(s) not green: TC-DOC-050 |
| AC-DOC-055 | AC-DOC-055 | AC — via TC-DOC-030 | via TC-DOC-030 | **GAP** — derived TC(s) not green: TC-DOC-030 |

## STEP 5 — Failures and skips, classified

| Code | Count | Why |
|---|---|---|
| `TEST_STRUCTURE_FAILURE` | 50 | no api-verify check or E2E scenario exercises the id (the test suites lack it — a test-side gap, not an app defect); or the case is reachable only by a thread-level / in-process test, which the no-JUnit policy excludes |
| `MISSING_IMPLEMENTATION` | 12 | MODEL-EVAL needs the `model-eval` profile runner with a known-result set, which was never delivered |

No FAIL: no api-verify check and no E2E scenario carrying an id of this module failed. No app defect was found; no source was changed.

- `TEST_STRUCTURE_FAILURE` (50): TC-DOC-001 (GAP), TC-DOC-002 (GAP), TC-DOC-003 (GAP), TC-DOC-004 (GAP), TC-DOC-006 (GAP), TC-DOC-007 (GAP), TC-DOC-008 (GAP), TC-DOC-009 (GAP), TC-DOC-010 (GAP), TC-DOC-011 (GAP), TC-DOC-016 (GAP), TC-DOC-017 (GAP), TC-DOC-020 (GAP), TC-DOC-021 (GAP), TC-DOC-022 (GAP), TC-DOC-023 (GAP), TC-DOC-026 (GAP), TC-DOC-028 (GAP), TC-DOC-029 (GAP), TC-DOC-030 (GAP), TC-DOC-031 (GAP), TC-DOC-070 (GAP), TC-DOC-032 (GAP), TC-DOC-033 (GAP), TC-DOC-036 (GAP), TC-DOC-039 (GAP), TC-DOC-041 (GAP), TC-DOC-043 (GAP), TC-DOC-044 (GAP), TC-DOC-046 (GAP), TC-DOC-047 (GAP), TC-DOC-048 (GAP), TC-DOC-049 (GAP), TC-DOC-050 (GAP), TC-DOC-051 (GAP), TC-DOC-053 (GAP), TC-DOC-073 (GAP), TC-DOC-074 (GAP), TC-DOC-075 (GAP), TC-DOC-076 (GAP), TC-DOC-077 (GAP), TC-DOC-066 (GAP), TC-DOC-067 (GAP), TC-DOC-068 (GAP), TC-DOC-069 (GAP), AC-DOC-002 (GAP), AC-DOC-003 (GAP), AC-DOC-016 (GAP), AC-DOC-054 (GAP), AC-DOC-055 (GAP)
- `MISSING_IMPLEMENTATION` (12): TC-DOC-054 (NOT-EXERCISABLE), TC-DOC-055 (NOT-EXERCISABLE), TC-DOC-056 (NOT-EXERCISABLE), TC-DOC-057 (NOT-EXERCISABLE), TC-DOC-058 (NOT-EXERCISABLE), TC-DOC-059 (NOT-EXERCISABLE), TC-DOC-060 (NOT-EXERCISABLE), TC-DOC-061 (NOT-EXERCISABLE), TC-DOC-062 (NOT-EXERCISABLE), TC-DOC-063 (NOT-EXERCISABLE), TC-DOC-064 (NOT-EXERCISABLE), TC-DOC-065 (NOT-EXERCISABLE)

## STEP 1 — Package rows

A unit's `package` row is recorded only when every id it lists has PASS evidence (then `--failed 0` → accepted). A unit with any GAP / NOT-EXERCISABLE id is **not recorded** — recording `passed=k failed=0` would falsely mark it accepted.

| Unit | kind | ids (source) | PASS | FAIL | row | not green |
|---|---|---|---|---|---|---|
| PORTS-DOCUMENT | exec_units | 20 (manifest) | 6 | 0 | **not accepted** — GAPs | TC-DOC-002, TC-DOC-003, TC-DOC-004, TC-DOC-006, TC-DOC-007, TC-DOC-008, TC-DOC-010, TC-DOC-016, TC-DOC-017, TC-DOC-026, TC-DOC-043, TC-DOC-048, TC-DOC-049, TC-DOC-054 |
| PORTS-MODEL | exec_units | 12 (manifest) | 0 | 0 | **not accepted** — GAPs | TC-DOC-047, TC-DOC-055, TC-DOC-056, TC-DOC-057, TC-DOC-058, TC-DOC-059, TC-DOC-060, TC-DOC-061, TC-DOC-062, TC-DOC-063, TC-DOC-064, TC-DOC-065 |
| PORTS-QUERY | exec_units | 10 (manifest) | 3 | 0 | **not accepted** — GAPs | TC-DOC-009, TC-DOC-020, TC-DOC-021, TC-DOC-022, TC-DOC-030, TC-DOC-039, TC-DOC-050 |
| SVC-API | exec_units | 31 (manifest) | 11 | 0 | **not accepted** — GAPs | TC-DOC-001, TC-DOC-011, TC-DOC-023, TC-DOC-028, TC-DOC-029, TC-DOC-031, TC-DOC-070, TC-DOC-032, TC-DOC-033, TC-DOC-036, TC-DOC-041, TC-DOC-044, TC-DOC-046, TC-DOC-051, TC-DOC-053, TC-DOC-073, TC-DOC-074, TC-DOC-075, TC-DOC-076, TC-DOC-077 |
| API-SCENARIOS | test_units | 27 (manifest) | 8 | 0 | **not accepted** — GAPs | TC-DOC-032, TC-DOC-033, TC-DOC-036, TC-DOC-039, TC-DOC-041, TC-DOC-043, TC-DOC-044, TC-DOC-046, TC-DOC-047, TC-DOC-048, TC-DOC-049, TC-DOC-050, TC-DOC-051, TC-DOC-053, TC-DOC-073, TC-DOC-074, TC-DOC-075, TC-DOC-076, TC-DOC-077 |
| INT-XM | test_units | 4 (manifest) | 0 | 0 | **not accepted** — GAPs | TC-DOC-066, TC-DOC-067, TC-DOC-068, TC-DOC-069 |
| MODEL-EVAL | test_units | 12 (manifest) | 0 | 0 | **not accepted** — GAPs | TC-DOC-054, TC-DOC-055, TC-DOC-056, TC-DOC-057, TC-DOC-058, TC-DOC-059, TC-DOC-060, TC-DOC-061, TC-DOC-062, TC-DOC-063, TC-DOC-064, TC-DOC-065 |
| RULE-SCENARIOS | test_units | 34 (manifest) | 12 | 0 | **not accepted** — GAPs | TC-DOC-001, TC-DOC-002, TC-DOC-003, TC-DOC-004, TC-DOC-006, TC-DOC-007, TC-DOC-008, TC-DOC-009, TC-DOC-010, TC-DOC-011, TC-DOC-016, TC-DOC-017, TC-DOC-020, TC-DOC-021, TC-DOC-022, TC-DOC-023, TC-DOC-026, TC-DOC-028, TC-DOC-029, TC-DOC-030, TC-DOC-031, TC-DOC-070 |
| XM-DOC-001 | integration | 4 (manifest) | 1 | 0 | **not accepted** — GAPs | AC-DOC-002, AC-DOC-003, TC-DOC-066 |
| XM-DOC-002 | integration | 4 (manifest) | 2 | 0 | **not accepted** — GAPs | AC-DOC-054, TC-DOC-067 |
| XM-DOC-003 | integration | 3 (manifest) | 2 | 0 | **not accepted** — GAPs | TC-DOC-068 |
| XM-DOC-004 | integration | 4 (manifest) | 1 | 0 | **not accepted** — GAPs | AC-DOC-016, AC-DOC-055, TC-DOC-069 |

ALIGN-BE / CORE / DATA-DOM* list no tests and were accepted (0/0 after a clean build) during execution; unchanged.

## Open gap rows of the module (`execution-state.json`)

- `api_doc_gaps` · **aias.documents.reading-model: provider dependency and connection settings (endpoint, credential) not stated** — OPEN — MISSING_IN_DOCS pending spec clarification
- `api_doc_gaps` · **DocumentAccess.fetchDocuments (CON-DOC-004): outcome of a document source row whose type column is NULL** — OPEN — MISSING_IN_DOCS pending spec clarification
- `api_doc_gaps` · **GET /api/v1/uploaded-documents — unsupported Accept header** — OPEN — MISSING_IN_DOCS pending spec clarification
- `api_doc_gaps` · **document source query: platform MCP query channel bean, credential secret store, and readOnly on CON-REG-011 not stated** — OPEN — MISSING_IN_DOCS pending spec clarification · interim implemented: SpringAiMcpQueryChannel over the Spring AI MCP client; endpoint convention `mcp:<client>`; query-tool args {sql, binds, maxRows} taken from the local Oracle MCP server; factory to confirm the protocol

## Notes

- Evidence is taken from the tags the api-verify checks and the E2E scenarios carry. The orchestrate STEP 4.4 second-agent coverage debate and the STEP 4.5 fixing agent were not run in this pass (scope: close the test phase honestly with the available evidence); the GAP set above is the input for them.
- Model used for the comparison in the counted E2E run: `gemini-3.6-flash` (Gemini free tier, model calls: comparison 11, reading 1). The first attempt of the day (run on `gemini-3.8-flash`) hit that model's daily quota after 4 calls; it is kept as history (`E2E-SIMULATION-2026-10-03-quota-run.json`) and not counted.
