# TEST REPORT — RPT backend v1 — 2026-10-03

Test phase close-out per `.claude/commands/RPT/execute-backend-test.md` STEP 1/4/5/6 and `.claude/commands/orchestrate-module.md` STEP 4. **No JUnit** (user policy 2026-10-02): the evidence is the api-verify scripts (`governance/shared/backend/modules/*/test-api/`, run 2026-10-03, all five exit 0, `api_verify` v1 recorded PASS) and the E2E simulation run `20261003T092840Z` ([`E2E-SIMULATION-2026-10-03.json`](E2E-SIMULATION-2026-10-03.json)). An id counts as PASS only with green evidence tagged with that id; NOT-EXERCISABLE is never counted as passed.

## STEP 4 — Coverage cross-check (governed plan ↔ tests)

**Ratio: 15 / 64 required ids PASS (23.4 %)** — PASS 15 · FAIL 0 · GAP 48 · NOT-EXERCISABLE 1 · DEFERRED 0 · OUT-OF-TRACK 0.

- TC blocks of the P4 test plan: 64 — PASS 15 · FAIL 0 · GAP 48 · NOT-EXERCISABLE 1.
- Every id listed in the `tests` of the exec units, test units and built integration packages is a TC block of the test plan or one of the AC rows above (no id is dropped). No integration package is deferred (every `requires` is met), so no row is DEFERRED.

```
GOVERNED PLAN ↔ TESTS — RPT v1
```

| TC/AC id | traces | scenario | evidence (api-verify check / E2E scenario) | result |
|---|---|---|---|---|
| TC-RPT-003 | AC-RPT-003 | Incomplete Check run refused | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-004 | AC-RPT-004 | AWAITING_DOCUMENTS with fetch mode path refused | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-005 | AC-RPT-005 | Manual Check starting RUNNING refused | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-006 | AC-RPT-006 | Mark RUNNING from AWAITING_DOCUMENTS | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-007 | AC-RPT-007 | Mark RUNNING again keeps the first running time | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-008 | AC-RPT-008 | Ended Check cannot be marked RUNNING | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-009 | AC-RPT-009 | Check not RUNNING cannot be completed | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-010 | AC-RPT-010 | Unknown Check on the result port | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-012 | AC-RPT-012 | Report stored whole or not at all | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-062 | AC-RPT-062 | Database failure while storing a report leaves nothing stored | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-016 | AC-RPT-016 | Metadata disagreeing with the Check run refused | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-017 | AC-RPT-017 | COMPLIANT refused unless every finding is SATISFIED | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-018 | AC-RPT-018 | Code outside its closed list refused (port and CHECK constraints) | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-019 | AC-RPT-019 | Finding without evidence refused | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-020 | AC-RPT-020 | UNREADABLE document without a reason refused (port and CHECK constraint) | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-021 | AC-RPT-021 API-RPT-001 | Failed Check stored with its reason | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-063 | AC-RPT-063 API-RPT-001 | Check awaiting documents failed with its reason | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-023 | AC-RPT-023 | Ended report never changes | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-035 | AC-RPT-035 API-RPT-002 | Listing refused without a service code | api-verify RPT test_checks (8/8 checks)<br>E2E [refusals] Lists of a request without their keys are refused → PASSED | PASS |
| TC-RPT-039 | AC-RPT-039 | Second decision refused — the decision is final | E2E [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) → PASSED | PASS |
| TC-RPT-040 | AC-RPT-040 | Decision on a non-completed Check refused | E2E [decisions] Decisions refused on a non-completed and an unknown Check → PASSED | PASS |
| TC-RPT-041 | AC-RPT-041 | Decision code outside EMPLOYEE_DECISION refused | E2E [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) → PASSED | PASS |
| TC-RPT-042 | AC-RPT-042 | Decision without the deciding employee refused | E2E [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) → PASSED | PASS |
| TC-RPT-045 | AC-RPT-045 | Decision for an unknown Check refused | E2E [decisions] Decisions refused on a non-completed and an unknown Check → PASSED | PASS |
| TC-RPT-046 | AC-RPT-046 | Approval API flag on a rejection refused (service and CHECK constraint) | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-049 | AC-RPT-049 API-RPT-003 | Decision agreement refused without a service code | api-verify RPT test_decision_agreement (4/4 checks)<br>E2E [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) → PASSED | PASS |
| TC-RPT-050 | AC-RPT-050 | Report inside the retention period kept by the purge | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-051 | AC-RPT-051 API-RPT-001 | Expired Check run purged with all its records | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-052 | AC-RPT-052 | No retention period, no purge | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-053 | AC-RPT-053 | Unfinished Check never purged | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-054 | AC-RPT-054 | Purge outcome logged | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-059 | AC-RPT-059 | Incomplete failure refused | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-060 | AC-RPT-060 | Purge deletes each Check run whole | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-064 | AC-RPT-064 | Purge failure logged with its Check run and cause | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-001 | AC-RPT-001 | Check run created, identifier returned | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-002 | AC-RPT-002 API-RPT-001 | Host identifiers kept exactly as sent | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-011 | AC-RPT-011 API-RPT-001 | Completed report stored | E2E [manual] Manual NOT_COMPLIANT: a failing transcript (GPA 2.10, 90 credits) → PASSED | PASS |
| TC-RPT-013 | AC-RPT-013 API-RPT-001 | Report order kept | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-014 | AC-RPT-014 API-RPT-001 | Missing and unreadable documents kept with their reason | E2E [manual] Missing document: confirm without an upload -> TRANSCRIPT MISSING, NOT → PASSED | PASS |
| TC-RPT-015 | AC-RPT-015 API-RPT-001 | Unread service query kept | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-022 | AC-RPT-022 API-RPT-001 | No document content kept | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-024 | AC-RPT-024 | One Check read for the Check Engine | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-025 | AC-RPT-025 | Unfinished Checks listed oldest first | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-026 | AC-RPT-026 | No unfinished Check — empty list | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-027 | AC-RPT-027 API-RPT-001 | Running Check read without report | api-verify RPT test_checks (3/3 checks) | PASS |
| TC-RPT-028 | AC-RPT-028 API-RPT-001 | Completed Check read with its report | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-RPT-029 | AC-RPT-029 API-RPT-001 | Finding returned with its evidence | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-030 | AC-RPT-030 API-RPT-001 | Unknown Check read answers not found | api-verify RPT test_checks (2/2 checks)<br>E2E [refusals] Confirmation of an unknown Check; reads of unknown Checks → PASSED | PASS |
| TC-RPT-031 | AC-RPT-031 API-RPT-001 | Failed Check read with its reason | ✗ none | **GAP** — api-verify RPT test_checks: read an ended Check — blocked: the Check did not end within 120.0s (status AWAITING_DOCUMENTS) |
| TC-RPT-032 | AC-RPT-032 API-RPT-001 | Stored text returned as data | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-033 | AC-RPT-033 API-RPT-002 | Checks of a request listed newest first | api-verify RPT test_checks (6/6 checks)<br>E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-RPT-034 | AC-RPT-034 API-RPT-002 | Listing capped at 100 with the total | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-036 | AC-RPT-036 API-RPT-001 | Every Check its own record | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-037 | AC-RPT-037 API-RPT-002 | At most 100 Checks in one listing | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-038 | AC-RPT-038 API-RPT-001 | Employee Decision recorded | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-043 | AC-RPT-043 API-RPT-001 | Execution through the Approval API recorded | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-044 | AC-RPT-044 | The Report Store never approves | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-047 | AC-RPT-047 API-RPT-003 | Decision agreement counted per version | api-verify RPT test_decision_agreement (2/2 checks)<br>E2E [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) → PASSED | PASS |
| TC-RPT-048 | AC-RPT-048 API-RPT-003 | Decision agreement empty for a service with no decision | api-verify RPT test_decision_agreement (3/3 checks) | PASS |
| TC-RPT-055 | AC-RPT-055 | No access to host data | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-056 | AC-RPT-056 API-RPT-001 | No file opened | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-058 | AC-RPT-058 API-RPT-002 | Read filters bound as parameters | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-061 | AC-RPT-061 | Hand-over with an undeclared field cannot reach the store | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-RPT-057 | AC-RPT-057 | No model call | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |

## STEP 5 — Failures and skips, classified

| Code | Count | Why |
|---|---|---|
| `TEST_STRUCTURE_FAILURE` | 48 | no api-verify check or E2E scenario exercises the id (the test suites lack it — a test-side gap, not an app defect); or the case is reachable only by a thread-level / in-process test, which the no-JUnit policy excludes |
| `MISSING_IMPLEMENTATION` | 1 | MODEL-EVAL needs the `model-eval` profile runner with a known-result set, which was never delivered |

No FAIL: no api-verify check and no E2E scenario carrying an id of this module failed. No app defect was found; no source was changed.

- `TEST_STRUCTURE_FAILURE` (48): TC-RPT-003 (GAP), TC-RPT-004 (GAP), TC-RPT-005 (GAP), TC-RPT-006 (GAP), TC-RPT-007 (GAP), TC-RPT-008 (GAP), TC-RPT-009 (GAP), TC-RPT-010 (GAP), TC-RPT-012 (GAP), TC-RPT-062 (GAP), TC-RPT-016 (GAP), TC-RPT-017 (GAP), TC-RPT-018 (GAP), TC-RPT-019 (GAP), TC-RPT-020 (GAP), TC-RPT-021 (GAP), TC-RPT-063 (GAP), TC-RPT-023 (GAP), TC-RPT-046 (GAP), TC-RPT-050 (GAP), TC-RPT-051 (GAP), TC-RPT-052 (GAP), TC-RPT-053 (GAP), TC-RPT-054 (GAP), TC-RPT-059 (GAP), TC-RPT-060 (GAP), TC-RPT-064 (GAP), TC-RPT-001 (GAP), TC-RPT-002 (GAP), TC-RPT-013 (GAP), TC-RPT-015 (GAP), TC-RPT-022 (GAP), TC-RPT-024 (GAP), TC-RPT-025 (GAP), TC-RPT-026 (GAP), TC-RPT-029 (GAP), TC-RPT-031 (GAP), TC-RPT-032 (GAP), TC-RPT-034 (GAP), TC-RPT-036 (GAP), TC-RPT-037 (GAP), TC-RPT-038 (GAP), TC-RPT-043 (GAP), TC-RPT-044 (GAP), TC-RPT-055 (GAP), TC-RPT-056 (GAP), TC-RPT-058 (GAP), TC-RPT-061 (GAP)
- `MISSING_IMPLEMENTATION` (1): TC-RPT-057 (NOT-EXERCISABLE)

## STEP 1 — Package rows

A unit's `package` row is recorded only when every id it lists has PASS evidence (then `--failed 0` → accepted). A unit with any GAP / NOT-EXERCISABLE id is **not recorded** — recording `passed=k failed=0` would falsely mark it accepted.

| Unit | kind | ids (source) | PASS | FAIL | row | not green |
|---|---|---|---|---|---|---|
| PORTS | exec_units | 32 (manifest) | 2 | 0 | **not accepted** — GAPs | TC-RPT-003, TC-RPT-004, TC-RPT-005, TC-RPT-006, TC-RPT-007, TC-RPT-008, TC-RPT-009, TC-RPT-010, TC-RPT-012, TC-RPT-062, TC-RPT-016, TC-RPT-017, TC-RPT-018, TC-RPT-019, TC-RPT-020, TC-RPT-021, TC-RPT-063, TC-RPT-023, TC-RPT-059, TC-RPT-001, TC-RPT-002, TC-RPT-013, TC-RPT-015, TC-RPT-024, TC-RPT-025 … (+5) |
| SVC-API | exec_units | 32 (manifest) | 13 | 0 | **not accepted** — GAPs | TC-RPT-046, TC-RPT-050, TC-RPT-051, TC-RPT-052, TC-RPT-053, TC-RPT-054, TC-RPT-060, TC-RPT-064, TC-RPT-022, TC-RPT-029, TC-RPT-031, TC-RPT-032, TC-RPT-034, TC-RPT-037, TC-RPT-038, TC-RPT-043, TC-RPT-044, TC-RPT-056, TC-RPT-058 |
| API-SCENARIOS | test_units | 29 (unit .md TC blocks) | 8 | 0 | **not accepted** — GAPs | TC-RPT-001, TC-RPT-002, TC-RPT-013, TC-RPT-015, TC-RPT-022, TC-RPT-024, TC-RPT-025, TC-RPT-026, TC-RPT-029, TC-RPT-031, TC-RPT-032, TC-RPT-034, TC-RPT-036, TC-RPT-037, TC-RPT-038, TC-RPT-043, TC-RPT-044, TC-RPT-055, TC-RPT-056, TC-RPT-058, TC-RPT-061 |
| MODEL-EVAL | test_units | 1 (unit .md TC blocks) | 0 | 0 | **not accepted** — GAPs | TC-RPT-057 |
| RULE-SCENARIOS | test_units | 34 (unit .md TC blocks) | 7 | 0 | **not accepted** — GAPs | TC-RPT-003, TC-RPT-004, TC-RPT-005, TC-RPT-006, TC-RPT-007, TC-RPT-008, TC-RPT-009, TC-RPT-010, TC-RPT-012, TC-RPT-062, TC-RPT-016, TC-RPT-017, TC-RPT-018, TC-RPT-019, TC-RPT-020, TC-RPT-021, TC-RPT-063, TC-RPT-023, TC-RPT-046, TC-RPT-050, TC-RPT-051, TC-RPT-052, TC-RPT-053, TC-RPT-054, TC-RPT-059 … (+2) |

ALIGN-BE / CORE / DATA-DOM* list no tests and were accepted (0/0 after a clean build) during execution; unchanged.

## Open gap rows of the module (`execution-state.json`)

- `api_doc_gaps` · **RPT CORE — boundary guard + retention binding** — OPEN — pending spec clarification: (1) CORE R1 asks for an ArchUnit test failing the build if an RPT class imports org.springframework.ai, java.nio.file or the query-port types; not written this run (tests disallowed by the user) — the boundary is held by convention + grep (0 hits) until tests are allowed (gov-enforce-library-contract M.5 open). (2) aias.reports.retention-days binds as Integer per R1: a non-integer value (e.g. 'abc', '1.5') fails start-up at binding instead of the REQ-RPT-044 'not a whole number of days → purge skipped' path; the plan does not say which wins. (3) R1 says JPA maps the carried enums EnumType.STRING, but FETCH_MODE/SOURCE_MODE store path\|blob\|manual — DATA-DOM must use an AttributeConverter for FetchMode (build-create-entity step 4).
- `api_doc_gaps` · **RPT DATA-DOM — completeCheck / failCheck domain rules (RULE-RPT-005, 007, 008, 010) + enum mapping** — OPEN — pending spec clarification: (1) RULE-RPT-007 applied to unread queries (ENT-RPT-004) and RULE-RPT-008's 'document type required' state no message/code — SVC-API's FindingIncompleteException text names a finding; domain returns UNREAD_QUERY_INCOMPLETE / DOCUMENT_INCOMPLETE {position, field} for SVC-API to map; (2) RULE-RPT-005 owner is a 'CheckReport value object' but CheckReport is API-RPT-001's response — built as rpt.domain.SubmittedReport; (3) RULE-RPT-010 needs endedAt >= STARTED_AT but SVC-API failCheck reads no row before its conditional UPDATE — CheckFailure.refusal(startedAt) skips the comparison when startedAt is null, CHK_RPT_CHECK_RUN_ENDED_AT backstops; (4) CORE R1 says EnumType.STRING, FetchMode cannot — every RPT closed list is mapped through an AttributeConverter writing storedValue() (same values)
- `api_doc_gaps` · **RPT PORTS+SVC-API — result port adapter, in-process refusal table, API-RPT-001/002/003** — OPEN — pending spec clarification: (1) DATA-DOM message gaps carried into SVC-API: an unread query without name/detail (RefusalReason UNREAD_QUERY_INCOMPLETE, RULE-RPT-007) is raised as FindingIncompleteException RPT-422-FINDING-INCOMPLETE and a document outcome without a documentType (DOCUMENT_INCOMPLETE, RULE-RPT-008) as DocumentReasonMismatchException RPT-422-DOCUMENT-REASON-MISMATCH, both with the table's own generic text 'The report of Check {checkId} was not stored.' (key RPT-500-REPORT-NOT-STORED) since the plan states no message for either — no text invented; the factory should supply both messages. (2) CON-CHK-008 lets ReportDocumentOutcome.documentType be null for a host document whose type was NULL, but RULE-RPT-008 + DOCUMENT_TYPE NOT NULL refuse it, so such a report is refused and CHK fails the Check INTERNAL_ERROR. (3) getCheck: SVC-API says none -> RPT-404-CHECK-NOT-FOUND, the declared CheckResultPort returns Optional — implemented as an empty Optional. (4) RULE-RPT-006 is applied where CHK's String codes are mapped (CheckResultStore, fromStored), so an unknown code is refused before RULE-RPT-001 (createCheckRun) / RULE-RPT-010 (failCheck) presence checks when both apply; blank codes count as absent. (5) ADR-RPT-012(3) says a write failure rolls CHK's transaction back whole (Check stays RUNNING), while SVC-API completeCheck step 4 and CHK's CheckEndingService roll back to a savepoint and fail INTERNAL_ERROR; RPT declares ReportNotStoredException rollbackFor and leaves the savepoint to CHK. (6) API-RPT-002/003 query params maxLength 100 have no error code: not validated (an over-long value matches nothing). (7) Purge kept/deleted log lines (REQ-RPT-054, REQ-RPT-046) appended as message keys RPT-LOG-PURGE-KEPT / RPT-LOG-PURGE-DELETED. (8) M.5 ArchUnit boundary test still deferred (tests not allowed this run).
- `api_doc_gaps` · **completeCheck write failure: ADR-RPT-012(3) whole-transaction rollback vs REQ-CHK-048 failCheck in the same transaction** — OPEN — MAPPING_GAP pending spec clarification
- `api_doc_gaps` · **GET /api/v1/checks — unsupported Accept header** — OPEN — MISSING_IN_DOCS pending spec clarification

## Notes

- Evidence is taken from the tags the api-verify checks and the E2E scenarios carry. The orchestrate STEP 4.4 second-agent coverage debate and the STEP 4.5 fixing agent were not run in this pass (scope: close the test phase honestly with the available evidence); the GAP set above is the input for them.
- Model used for the comparison in the counted E2E run: `gemini-3.6-flash` (Gemini free tier, model calls: comparison 11, reading 1). The first attempt of the day (run on `gemini-3.8-flash`) hit that model's daily quota after 4 calls; it is kept as history (`E2E-SIMULATION-2026-10-03-quota-run.json`) and not counted.
