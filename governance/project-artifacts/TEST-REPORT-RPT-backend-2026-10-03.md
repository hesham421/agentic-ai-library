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


## Update — 2026-10-03 (RPT gap closure, E2E runs `20261003T115316Z` … `20261003T130654Z`)

The 48 GAP ids above were taken up by new E2E groups (`rpt-store`, `rpt-purge`, `rpt-inprocess`). The history above is kept unchanged.

Runs (all `scripts/e2e/simulate.py`, groups `rpt-store`, `int-flow`, `rpt-purge`, `rpt-inprocess`, `int-inprocess` plus regressions):

- [`E2E-SIMULATION-RPT-INT-2026-10-03.json`](E2E-SIMULATION-RPT-INT-2026-10-03.json) / [`-run.md`](E2E-SIMULATION-RPT-INT-2026-10-03-run.md) — run `20261003T115316Z`, all new groups + `registry`/`refusals`/`decisions`: 44 PASSED · 2 FAILED (TC-RPT-052 precondition, TC-RPT-064) · 22 NOT-EXERCISABLE · 1 real comparison call.
- [`-rerun.json`](E2E-SIMULATION-RPT-INT-2026-10-03-rerun.json) — run `20261003T125219Z`: TC-RPT-052 PASSED (precondition fixed in the runner); TC-RPT-064 FAILED again (see the defect below). Its `registry`/`refusals` 500s are environmental: the jar had been rebuilt under the running process (`NoClassDefFoundError: ch/qos/logback/...` in `logs/e2e-20261003T125219Z/`), not an app defect.
- [`-rerun2.json`](E2E-SIMULATION-RPT-INT-2026-10-03-rerun2.json) — run `20261003T130306Z`, after the fix: TC-RPT-064 PASSED (11/11).
- [`-regression.json`](E2E-SIMULATION-RPT-INT-2026-10-03-regression.json) (`20261003T130537Z`: `registry` 4/4, `refusals` 6/6, `decisions` 1 PASSED + 1 SKIPPED-QUOTA on `gemini-3.6-flash`) and [`-regression-decisions.json`](E2E-SIMULATION-RPT-INT-2026-10-03-regression-decisions.json) (`20261003T130654Z`: `decisions` 2/2, the app's comparison model switched to `gemini-3.5-flash` through the `AIAS_CHECK_COMPARISONMODEL_MODEL` environment variable; the runner's header still prints the profile's model).

Method: the comparison model's provider is replaced by the scripted local stub `scripts/e2e/model_stub.py` (127.0.0.1:7293, override `spring.ai.openai.base-url`). A Check's document carries `E2ESTUB_<ID>`, and the stub answers that script's findings, holds its answer or returns an error status. So COMPLETED / NEEDS_MANUAL_REVIEW / NOT_COMPLIANT reports, MODEL_UNAVAILABLE (503) and TIMED_OUT (held answer) need **no free-tier quota**. One COMPLETED Check is reused across scenarios (decisions on different Checks, agreement counts, listing). The FAILED paths come from the upload-window expiry, a restart (INTERRUPTED), a 503 and a held answer. The listing cap uses 130 / 101 cheap waiting Checks of one request. Fixtures are isolated under `local/e2e-rpt/` and `local/e2e-int/`, overrides go in `local/e2e-override.properties`, and the normal mode is restored at the end of every run. There are no direct DB writes: the runner reads counts and the data dictionary through read-only SQL. Two purge-only steps go further, and both are disclosed here. (1) `rpt-purge` uses Check runs that genuinely ended more than 1 day ago (from the 2026-10-02 runs); retention `1` day and purge cron `*/10 * * * * *` delete them, which is the purge's designed effect on the local schema. (2) A second Oracle session holds `SELECT … FOR UPDATE` row locks on one Finding / one run row, then ROLLBACK, under a 5 s JDBC read timeout; this is the TC's "deletion made to fail" fault without writing a row. TC-INT-006 pauses `erp-oracle` (`docker pause`, unpaused in a `finally`) because its precondition is a database outage.

Model quota (probe 2026-10-03 ~17:00 +04, one 5-token call each): `gemini-3.5-flash`, `gemini-3.5-flash-lite`, `gemini-3.6-flash`, `gemini-3.7-flash`, `gemini-3.8-flash` all answered 200. Minutes later `gemini-3.6-flash` and `gemini-3.7-flash` answered daily-quota 429 (limit 20; each probe was probably the model's last request). Real comparison calls of this pass: 4 (main run 1, regression 1 → 429, retry on 3.7 1 → 429, retry on 3.5 1 → COMPLETED), within the 6-call target.

**Defect fixed (TC-RPT-064, REQ-RPT-054)** — `src/main/java/io/agenticai/rpt/service/ReportPurgeService.java:87-106`. When the deletion of a Check run failed on a broken connection (the lock wait cut by the read timeout), the rollback failed too. The template then threw the *rollback's* exception, so the WARN line named "Connection is closed" instead of the deletion's own cause. The deletion's `DataAccessException` is now kept apart (an `AtomicReference` inside the callback) and logged in preference. After the fix: `WARN … Report purge kept Check run 130: its deletion failed (Read timed out).` before the closing line. The runner's cause check was also widened to accept "timed out" and reject "Connection is closed". `mvn -DskipTests package`, app restarted, TC-RPT-064 rerun green, `registry`/`refusals`/`decisions` regressions green.

**Ratio: 43 / 64 required ids PASS (67.2 %)** — PASS 43 · FAIL 0 · GAP 0 · NOT-EXERCISABLE 21 · DEFERRED 0 · OUT-OF-TRACK 0. Before: 15 PASS · 48 GAP · 1 NOT-EXERCISABLE. AMBIGUOUS 0 · SKIPPED-QUOTA 0 (the only quota skip was a regression retried green on another model).

| TC id | scenario | evidence | result |
|---|---|---|---|
| TC-RPT-003 | Incomplete Check run refused | CHK refuses a start without employeeId (400 CHK-400-START-INCOMPLETE, refusals group) before it calls createCheckRun; no public path hands RPT a blank value | **NOT-EXERCISABLE** |
| TC-RPT-004 | AWAITING_DOCUMENTS with fetch mode path refused | CHK derives the initial status from the version's fetch mode (path -> RUNNING); step 2 is a direct INSERT into the service schema | **NOT-EXERCISABLE** |
| TC-RPT-005 | Manual Check starting RUNNING refused | CHK always starts a manual Check AWAITING_DOCUMENTS | **NOT-EXERCISABLE** |
| TC-RPT-006 | Mark RUNNING from AWAITING_DOCUMENTS | E2E [rpt-store] Confirmation marks the Check RUNNING with its running time; it then completes COMPLIANT → PASSED (7 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-007 | Mark RUNNING again keeps the first running time | CHK calls markRunning once per Check (at the confirmation); a second confirmation is refused by CHK (409 CHK-409) before RPT | **NOT-EXERCISABLE** |
| TC-RPT-008 | Ended Check cannot be marked RUNNING | a confirmation of an ended Check is refused by CHK (409 CHK-409-CHECK-NOT-AWAITING-DOCUMENTS) before markRunning | **NOT-EXERCISABLE** |
| TC-RPT-009 | Check not RUNNING cannot be completed | CHK completes only the Check its pipeline runs | **NOT-EXERCISABLE** |
| TC-RPT-010 | Unknown Check on the result port | CHK fails only Checks it created; no public path names Check 999 | **NOT-EXERCISABLE** |
| TC-RPT-012 | Report stored whole or not at all | the outcome `PASSED` cannot reach RPT: CHK's structured output admits SATISFIED / NOT_SATISFIED / UNDETERMINED only (MODEL_OUTPUT_INVALID before RPT) | **NOT-EXERCISABLE** |
| TC-RPT-062 | Database failure while storing a report leaves nothing stored | needs a fault injected on the INSERT of the second Finding — not producible through the API | **NOT-EXERCISABLE** |
| TC-RPT-016 | Metadata disagreeing with the Check run refused | CHK builds the metadata from the stored run | **NOT-EXERCISABLE** |
| TC-RPT-017 | COMPLIANT refused unless every finding is SATISFIED | CHK's Overall Status rule never hands COMPLIANT with a NOT_SATISFIED finding (a scripted NOT_SATISFIED gives NOT_COMPLIANT, TC-RPT-029) | **NOT-EXERCISABLE** |
| TC-RPT-018 | Code outside its closed list refused (port and CHECK constraints) | CHK passes only its own closed codes; steps 2-3 are direct writes to the service schema | **NOT-EXERCISABLE** |
| TC-RPT-019 | Finding without evidence refused | CHK records a finding without evidence as UNDETERMINED with evidence 'NONE' (REQ-CHK-040) before RPT | **NOT-EXERCISABLE** |
| TC-RPT-020 | UNREADABLE document without a reason refused (port and CHECK constraint) | DOC always gives an UNREADABLE outcome its reason; step 2 is a direct INSERT | **NOT-EXERCISABLE** |
| TC-RPT-021 | Failed Check stored with its reason | E2E [rpt-store] A RUNNING Check past its timeout is stored FAILED / TIMED_OUT with its detail and end time → PASSED (7 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-063 | Check awaiting documents failed with its reason | E2E [rpt-store] A Check awaiting documents ends FAILED / UPLOAD_WINDOW_EXPIRED, never RUNNING → PASSED (1 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-023 | Ended report never changes | CHK completes a Check once; no public path calls completeCheck again | **NOT-EXERCISABLE** |
| TC-RPT-046 | Approval API flag on a rejection refused (service and CHECK constraint) | INT hands approvalApiExecuted=false for every REJECTED decision (TC-INT-013); step 2 is a direct UPDATE | **NOT-EXERCISABLE** |
| TC-RPT-050 | Report inside the retention period kept by the purge | E2E [rpt-purge] A Check run inside the retention period is kept by the purge → PASSED (1 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-051 | Expired Check run purged with all its records | E2E [rpt-purge] Purge with retention 1 day: an expired Check run is deleted with all its records (404) → PASSED (10 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-052 | No retention period, no purge | E2E [rpt-store] No retention period configured: the scheduled purge is skipped and logged, nothing deleted → PASSED (5 checks, run `20261003T125219Z`) | PASS |
| TC-RPT-053 | Unfinished Check never purged | needs a Check unfinished for longer than the retention period: the shortest period is 1 whole day and every restart (a purge-mode change) ends unfinished Checks INTERRUPTED; the rows cannot be aged without direct DB writes | **NOT-EXERCISABLE** |
| TC-RPT-054 | Purge outcome logged | E2E [rpt-purge] Purge outcome logged: 'Report purge deleted N Check runs ended before <cut-off>' → PASSED (4 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-059 | Incomplete failure refused | CHK always gives a failure its detail | **NOT-EXERCISABLE** |
| TC-RPT-060 | Purge deletes each Check run whole | E2E [rpt-purge] A failing deletion keeps that Check run whole; the others are deleted and counted → PASSED (3 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-064 | Purge failure logged with its Check run and cause | E2E [rpt-purge] Purge failure logged at WARN with its Check run and cause, before the closing line → PASSED (11 checks, run `20261003T130306Z`) | PASS |
| TC-RPT-001 | Check run created, identifier returned | E2E [rpt-store] Path Check of version 3 created RUNNING and read back; the start answers before the report (40 s pipeline) → PASSED (7 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-002 | Host identifiers kept exactly as sent | E2E [rpt-store] Host identifiers kept exactly as received (slash, leading/trailing spaces, case) → PASSED (3 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-013 | Report order kept | E2E [rpt-store] Report contents: finding with its evidence and note, stored text returned as data, order kept → PASSED (11 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-015 | Unread service query kept | E2E [rpt-store] Unread service query request_details ('more than 500 rows') kept; NEEDS_MANUAL_REVIEW → PASSED (6 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-022 | No document content kept | E2E [rpt-store] No document content kept: a READ document entry and the RPT_CHECK_DOCUMENT columns → PASSED (4 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-024 | One Check read for the Check Engine | getCheck's result is handed to CHK in-process only; API-RPT-001 is a different operation (its fields are asserted by TC-RPT-001) | **NOT-EXERCISABLE** |
| TC-RPT-025 | Unfinished Checks listed oldest first | E2E [rpt-store] Unfinished Checks listed oldest first: a restart ends AWAITING (older) then RUNNING (newer); a COMPLETED one is untouched → PASSED (16 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-026 | No unfinished Check — empty list | E2E [rpt-store] No unfinished Check at a start: the unfinished-Check list is empty (start-up recovery: 0) → PASSED (8 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-029 | Finding returned with its evidence | E2E [rpt-store] Report contents: finding with its evidence and note, stored text returned as data, order kept → PASSED (11 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-031 | Failed Check read with its reason | E2E [rpt-store] Failed Check read with its reason: the provider answers 503 -> MODEL_UNAVAILABLE → PASSED (5 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-032 | Stored text returned as data | E2E [rpt-store] Report contents: finding with its evidence and note, stored text returned as data, order kept → PASSED (11 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-034 | Listing capped at 100 with the total | E2E [rpt-store] Listing capped at 100 with the total: 130 Checks of request 1002, 101 of request 1003 → PASSED (8 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-036 | Every Check its own record | E2E [rpt-store] Every Check its own record: a new Check of request 1001 beside a COMPLETED, APPROVED one → PASSED (10 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-037 | At most 100 Checks in one listing | E2E [rpt-store] Listing capped at 100 with the total: 130 Checks of request 1002, 101 of request 1003 → PASSED (8 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-038 | Employee Decision recorded | E2E [rpt-store] Employee Decision APPROVED by E-3307 recorded; Overall Status and findings unchanged → PASSED (3 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-043 | Execution through the Approval API recorded | E2E [int-flow] APPROVED through the Approval API: recorded as executed; the only host connection is that call → PASSED (16 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-044 | The Report Store never approves | E2E [int-flow] A COMPLETED compliant Check of an approval-enabled version, left undecided: no Approval API call; the Report Store never approves → PASSED (3 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-055 | No access to host data | E2E [rpt-purge] No access to host data: RPT reads, a decision and purge runs send 0 host queries → PASSED (6 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-056 | No file opened | E2E [rpt-store] No file opened: a stored detail naming a path is returned as text (the path is a FIFO) → PASSED (3 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-058 | Read filters bound as parameters | E2E [rpt-store] Read filters bound as parameters: requestNumber 1001' OR '1'='1 lists nothing → PASSED (3 checks, run `20261003T115316Z`) | PASS |
| TC-RPT-061 | Hand-over with an undeclared field cannot reach the store | structural (reflection + architecture rule over the value types) — an in-process test the no-JUnit policy excludes; no API reaches it | **NOT-EXERCISABLE** |
| TC-RPT-057 | No model call | unchanged — MODEL-EVAL needs the `model-eval` profile runner with a known-result set, never delivered | **NOT-EXERCISABLE** |

Purge observations without DB writes:
- TC-RPT-052: purge cron every 10 s with no retention period. The "skipped" line is logged, and an expired run is still read back.
- TC-RPT-050: a run ended minutes earlier is kept under a 1-day retention.

TC-RPT-053 (an unfinished Check older than the retention period) is NOT-EXERCISABLE. Ageing a row needs a DB write, the shortest retention is 1 whole day, and every restart (a purge-mode change) ends unfinished Checks INTERRUPTED.

### Package rows (this update)

No RPT row recorded: every RPT unit still lists at least one NOT-EXERCISABLE id.

| Unit | withheld — blocking ids |
|---|---|
| PORTS | TC-RPT-003, -004, -005, -007, -008, -009, -010, -012, -016, -017, -018, -019, -020, -023, -024, -057, -059, -061, -062 |
| SVC-API | TC-RPT-046, TC-RPT-053 |
| API-SCENARIOS | TC-RPT-024, TC-RPT-061 |
| MODEL-EVAL | TC-RPT-057 |
| RULE-SCENARIOS | TC-RPT-003, -004, -005, -007, -008, -009, -010, -012, -016, -017, -018, -019, -020, -023, -046, -053, -059, -062 |
