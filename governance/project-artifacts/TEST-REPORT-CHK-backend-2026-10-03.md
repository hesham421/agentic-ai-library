# TEST REPORT — CHK backend v1 — 2026-10-03

Test phase close-out per `.claude/commands/CHK/execute-backend-test.md` STEP 1/4/5/6 and `.claude/commands/orchestrate-module.md` STEP 4. **No JUnit** (user policy 2026-10-02): the evidence is the api-verify scripts (`governance/shared/backend/modules/*/test-api/`, run 2026-10-03, all five exit 0, `api_verify` v1 recorded PASS) and the E2E simulation run `20261003T092840Z` ([`E2E-SIMULATION-2026-10-03.json`](E2E-SIMULATION-2026-10-03.json)). An id counts as PASS only with green evidence tagged with that id; NOT-EXERCISABLE is never counted as passed.

## STEP 4 — Coverage cross-check (governed plan ↔ tests)

**Ratio: 34 / 118 required ids PASS (28.8 %)** — PASS 34 · FAIL 0 · GAP 73 · NOT-EXERCISABLE 11 · DEFERRED 0 · OUT-OF-TRACK 0.

- TC blocks of the P4 test plan: 100 — PASS 29 · FAIL 0 · GAP 62 · NOT-EXERCISABLE 9.
- AC ids listed in the built integration packages' `tests` (mapped to TCs through "Derived from"; an AC is PASS only when every TC derived from it is PASS): 18 — PASS 5 · GAP 11 · NOT-EXERCISABLE 2 · FAIL 0.
- Every id listed in the `tests` of the exec units, test units and built integration packages is a TC block of the test plan or one of the AC rows above (no id is dropped). No integration package is deferred (every `requires` is met), so no row is DEFERRED.

```
GOVERNED PLAN ↔ TESTS — CHK v1
```

| TC/AC id | traces | scenario | evidence (api-verify check / E2E scenario) | result |
|---|---|---|---|---|
| TC-CHK-001 | AC-CHK-004 | Start with a blank employee identity is refused and creates nothing | E2E [refusals] Start refusals: unknown service, incomplete start, unreadable body, un → PASSED | PASS |
| TC-CHK-002 | AC-CHK-005 | Start for a service whose available flag is false is refused | E2E [withdrawn] A package folder moved out: the service is WITHDRAWN, unlisted, read a → PASSED | PASS |
| TC-CHK-003 | AC-CHK-006 | Start for an unknown service code is refused and returns no identifier | E2E [refusals] Start refusals: unknown service, incomplete start, unreadable body, un → PASSED | PASS |
| TC-CHK-004 | AC-CHK-007 | Start for a service whose connection is not activated is refused | E2E [connection] The connection removed from the activation config: REMOVED; start -> 4 → PASSED | PASS |
| TC-CHK-005 | AC-CHK-011 | An unexpected error in a pipeline step ends the Check FAILED with INTERNAL_ERROR | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-006 | AC-CHK-013 | A request number carrying SQL text is sent only as the bound value | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-007 | AC-CHK-015 | A query over a connection that is not read-only is not run and is recorded as not read | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-008 | AC-CHK-016 | The document source query is never sent by the Check Engine | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-009 | AC-CHK-022 | A missing required document gives NOT_SATISFIED with evidence MISSING | E2E [manual] Missing document: confirm without an upload -> TRANSCRIPT MISSING, NOT → PASSED | PASS |
| TC-CHK-010 | AC-CHK-023 | An unreadable required document gives UNDETERMINED with its reason as evidence | E2E [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outs → PASSED | PASS |
| TC-CHK-011 | AC-CHK-024 | A Document Access failure ends the Check FAILED with INTERNAL_ERROR and the failure text | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-012 | AC-CHK-026 | A model outcome contradicting the recomputed comparison is replaced by the computation | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-013 | AC-CHK-027 | A value found that is not in the Check's data gives UNDETERMINED | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-014 | AC-CHK-028 | A limit the service knowledge does not state gives UNDETERMINED with the RULE-CHK-006 note | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-015 | AC-CHK-029 | A value that cannot be read as a number gives UNDETERMINED | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-016 | AC-CHK-030 | Evidence absent from the query results and documents gives UNDETERMINED | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-017 | AC-CHK-031 | The comparison model is called with no tool and no function definition | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-018 | AC-CHK-032 | Query text in the model output is never executed | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-019 | AC-CHK-033 | A COMPLIANT Check of an approval-enabled service calls no approval API | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-020 | AC-CHK-034 | A completed Check hands over no Employee Decision | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-021 | AC-CHK-037 | Instruction-like document content stays data and does not change the outcome | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-022 | AC-CHK-040 | Model output outside the fixed structure ends the Check FAILED with MODEL_OUTPUT_INVALID | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-023 | AC-CHK-041 | A model finding with empty evidence gives UNDETERMINED | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-024 | AC-CHK-042 | Any NOT_SATISFIED finding gives NOT_COMPLIANT, even beside UNDETERMINED | E2E [manual] Manual NOT_COMPLIANT: a failing transcript (GPA 2.10, 90 credits) → PASSED | PASS |
| TC-CHK-025 | AC-CHK-043 | An UNDETERMINED finding without any NOT_SATISFIED gives NEEDS_MANUAL_REVIEW | E2E [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outs → PASSED | PASS |
| TC-CHK-026 | AC-CHK-044 | An unread service query gives NEEDS_MANUAL_REVIEW although every finding is SATISFIED | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-027 | AC-CHK-045 | Every finding SATISFIED and every query read gives COMPLIANT | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-CHK-028 | AC-CHK-048 | After completion the Check Engine's schema keeps no report data and no Active Check | E2E [lifecycle] After the Check ends: no Active Check, and its uploads are deleted → PASSED<br>E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRE → PASSED | PASS |
| TC-CHK-029 | AC-CHK-050 | A rejected complete call ends the Check FAILED with INTERNAL_ERROR | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-030 | AC-CHK-051 | A failing service query is recorded as not read and the Check continues | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-031 | AC-CHK-052 | A query returning more than the maximum rows is recorded as not read and passes no rows | E2E [limits] max-rows 1: a query answering 2 rows is recorded unread ('more than 1  → PASSED | PASS |
| TC-CHK-032 | AC-CHK-052 | Boundary — a query returning exactly the maximum rows is read in full | E2E [limits] max-rows 1: a query answering 2 rows is recorded unread ('more than 1  → PASSED | PASS |
| TC-CHK-033 | AC-CHK-053 | A Check RUNNING past the timeout ends FAILED with TIMED_OUT and the late answer is discarded | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-034 | AC-CHK-054 | The timeout counts from RUNNING, not from the wait for uploads | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-035 | AC-CHK-055 | An unreachable comparison model ends the Check FAILED with MODEL_UNAVAILABLE | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-036 | AC-CHK-056 | A FAILED Check carries one reason, a detail, an end time and no Overall Status | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-037 | AC-CHK-057 | Unfinished Checks of an earlier run are ended INTERRUPTED at start-up and notified | E2E [interrupted] A waiting Check is ended INTERRUPTED by a restart; its Active Check an → PASSED | PASS |
| TC-CHK-038 | AC-CHK-060 | Confirming the uploads of an unknown Check is refused | E2E [refusals] Confirmation of an unknown Check; reads of unknown Checks → PASSED | PASS |
| TC-CHK-039 | AC-CHK-061 | Confirming the uploads of a FAILED Check is refused and leaves it FAILED | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-040 | AC-CHK-062 | An elapsed upload window ends the Check FAILED with UPLOAD_WINDOW_EXPIRED and notifies Document Access | E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRE → PASSED | PASS |
| TC-CHK-041 | AC-CHK-062 | Boundary — an upload window not yet elapsed does not end the Check | E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRE → PASSED | PASS |
| TC-CHK-042 | AC-CHK-064 | A Check FAILED with MODEL_OUTPUT_INVALID sends exactly one end-of-Check notice | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-043 | AC-CHK-063 AC-CHK-064 | The end-of-Check notice is sent once on every ending path | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-044 | AC-CHK-065 | A failing end-of-Check notice is retried once, logged, and the ending is kept | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-045 | AC-CHK-068 | A Check's working data is discarded when it ends | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-046 | AC-CHK-075 | Nothing is sent to a free-tier comparison model on REAL data | E2E [notpermitted] FREE comparison model on REAL data: a path Check ends FAILED / MODEL_N → PASSED | PASS |
| TC-CHK-047 | AC-CHK-076 | A Check on a free-tier model with REAL data ends FAILED with MODEL_NOT_PERMITTED | E2E [notpermitted] FREE comparison model on REAL data: a path Check ends FAILED / MODEL_N → PASSED<br>E2E [notpermitted] FREE reading model on REAL data: a scanned (PNG) upload is never sent; → PASSED | PASS |
| TC-CHK-048 | AC-CHK-077 | An undeclared data class counts as REAL and blocks the free-tier model | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-049 | AC-CHK-078 | An undeclared comparison model tier counts as FREE | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-050 | AC-CHK-082 | A Check that already ended is not ended a second time by its timeout | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-051 | AC-CHK-083 API-CHK-001 | The deadline check ends only the Active Checks whose deadline has passed, by status | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-052 | AC-CHK-084 | Start-up removes every Active Check left by an earlier run | E2E [interrupted] A waiting Check is ended INTERRUPTED by a restart; its Active Check an → PASSED | PASS |
| TC-CHK-053 | AC-CHK-036 | Instruction text and a forged data delimiter in a query result stay inside the data part | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-054 | AC-CHK-001 | Starting a Check returns its identifier before any service query is sent | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-055 | AC-CHK-002 | The request number and the employee identity are handed over exactly as sent | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-056 | AC-CHK-003 | A `path` Check is marked RUNNING and runs in the background | E2E [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outs → PASSED | PASS |
| TC-CHK-057 | AC-CHK-008 | The current version is loaded and recorded with the new Check run | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-058 | AC-CHK-009 | A `manual` Check keeps its start version when a newer one becomes current | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-059 | AC-CHK-010 | Two services of different fetch modes run the same six steps | E2E [blob] Blob flow: the TRANSCRIPT BLOB is read over the jdbc connection local- → PASSED | PASS |
| TC-CHK-060 | AC-CHK-012 | Only the version's non-source queries are sent, with their SQL text unaltered | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-061 | AC-CHK-014 | A service query goes only through the MCP query channel of its read-only connection | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-062 | AC-CHK-017 | Each query call carries the maximum rows and the time left before the timeout | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-063 | AC-CHK-018 | Documents are fetched once from Document Access with the Check's identifiers | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-064 | AC-CHK-019 | The Check Engine opens no file and no JDBC connection itself | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-065 | AC-CHK-020 | Each required document type gives exactly one finding | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-066 | AC-CHK-021 | One READ outcome satisfies a required document type | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-067 | AC-CHK-025 | The model states value, location, comparison and limit of an explicit condition | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-068 | AC-CHK-035 | The whole service knowledge is the only service instruction of the model input | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-069 | AC-CHK-036 | Query results and document content appear only between the data delimiters | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-070 | AC-CHK-038 | The model call carries the fixed output schema and its output is validated | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-071 | AC-CHK-039 | The report has one finding per condition and per required document type | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-CHK-072 | AC-CHK-046 | A decided Overall Status is handed to the result port with the Check COMPLETED | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-073 | AC-CHK-047 | The report metadata names version, fetch mode, model, times and employee | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-CHK-074 | AC-CHK-049 | Document outcomes are handed over without their content | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-075 | AC-CHK-058 | A `manual` Check waits in AWAITING_DOCUMENTS and runs no query | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-CHK-076 | AC-CHK-059 | Confirming the uploads runs the pipeline on the recorded version | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-CHK-077 | AC-CHK-063 | A COMPLIANT Check sends exactly one end-of-Check notice | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-078 | AC-CHK-066 | Two concurrent Checks share no data | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-079 | AC-CHK-067 | Each model call carries no earlier conversation | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-080 | AC-CHK-069 | A second Check of the same request starts independently | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-081 | AC-CHK-070 | Switching the provider by configuration needs no source change | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-082 | AC-CHK-071 | The comparison model is taken from configuration and recorded | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-083 | AC-CHK-079 API-CHK-001 | The Active Check of a new `manual` Check carries its upload-window deadline | api-verify CHK test_active_checks (4/4 checks)<br>E2E [lifecycle] Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upl → PASSED<br>E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRE → PASSED | PASS |
| TC-CHK-084 | AC-CHK-080 API-CHK-001 | Confirmation moves the Active Check to RUNNING with the timeout deadline | api-verify CHK test_active_checks (3/3 checks) | PASS |
| TC-CHK-085 | AC-CHK-081 API-CHK-001 | A completed Check has no Active Check any more | api-verify CHK test_active_checks (4/4 checks)<br>E2E [lifecycle] After the Check ends: no Active Check, and its uploads are deleted → PASSED | PASS |
| TC-CHK-086 | AC-CHK-085 API-CHK-001 | The Active Check holds only identifier, status and deadline | E2E [lifecycle] Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upl → PASSED | PASS |
| TC-CHK-087 | AC-CHK-072 | The known-result set has every Overall Status, each request synthetic | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-CHK-088 | AC-CHK-073 | The model-evaluation run reports expected and reached status per request | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-CHK-089 | AC-CHK-074 | A request reaching another status than expected fails the run and is named | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-CHK-090 | AC-CHK-072 AC-CHK-073 AC-CHK-025 AC-CHK-045 | Known-result request SYN-001 reaches COMPLIANT | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-CHK-091 | AC-CHK-072 AC-CHK-073 AC-CHK-026 AC-CHK-042 | Known-result request SYN-002 reaches NOT_COMPLIANT | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-CHK-092 | AC-CHK-072 AC-CHK-073 AC-CHK-022 AC-CHK-042 | Known-result request SYN-003 reaches NOT_COMPLIANT | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-CHK-093 | AC-CHK-072 AC-CHK-073 AC-CHK-023 AC-CHK-043 | Known-result request SYN-004 reaches NEEDS_MANUAL_REVIEW | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-CHK-094 | AC-CHK-072 AC-CHK-073 AC-CHK-052 AC-CHK-044 | Known-result request SYN-005 reaches NEEDS_MANUAL_REVIEW | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-CHK-095 | AC-CHK-072 AC-CHK-073 AC-CHK-037 AC-CHK-042 | Known-result request SYN-006 reaches NOT_COMPLIANT | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-CHK-096 | XM-CHK-001 | XM-CHK-001 degraded — REG has no package for the code: start refused with a defined rejection | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-097 | XM-CHK-002 | XM-CHK-002 degraded — the recorded version cannot be resolved on resume: FAILED with INTERNAL_ERROR | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-098 | XM-CHK-003 | XM-CHK-003 degraded — the version carries only its document source query: the Check still ends once | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-099 | XM-CHK-004 | XM-CHK-004 degraded — the version lists no required document type: no document finding, defined report | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-CHK-100 | XM-CHK-005 | XM-CHK-005 degraded — REG returns no connection settings: start refused as not activated | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| AC-CHK-005 | AC-CHK-005 | AC — via TC-CHK-002 | via TC-CHK-002 | PASS |
| AC-CHK-006 | AC-CHK-006 | AC — via TC-CHK-003 | via TC-CHK-003 | PASS |
| AC-CHK-007 | AC-CHK-007 | AC — via TC-CHK-004 | via TC-CHK-004 | PASS |
| AC-CHK-008 | AC-CHK-008 | AC — via TC-CHK-057 | via TC-CHK-057 | **GAP** — derived TC(s) not green: TC-CHK-057 |
| AC-CHK-009 | AC-CHK-009 | AC — via TC-CHK-058 | via TC-CHK-058 | **GAP** — derived TC(s) not green: TC-CHK-058 |
| AC-CHK-012 | AC-CHK-012 | AC — via TC-CHK-060 | via TC-CHK-060 | **GAP** — derived TC(s) not green: TC-CHK-060 |
| AC-CHK-013 | AC-CHK-013 | AC — via TC-CHK-006 | via TC-CHK-006 | **GAP** — derived TC(s) not green: TC-CHK-006 |
| AC-CHK-014 | AC-CHK-014 | AC — via TC-CHK-061 | via TC-CHK-061 | **GAP** — derived TC(s) not green: TC-CHK-061 |
| AC-CHK-015 | AC-CHK-015 | AC — via TC-CHK-007 | via TC-CHK-007 | **GAP** — derived TC(s) not green: TC-CHK-007 |
| AC-CHK-016 | AC-CHK-016 | AC — via TC-CHK-008 | via TC-CHK-008 | **GAP** — derived TC(s) not green: TC-CHK-008 |
| AC-CHK-020 | AC-CHK-020 | AC — via TC-CHK-065 | via TC-CHK-065 | **GAP** — derived TC(s) not green: TC-CHK-065 |
| AC-CHK-021 | AC-CHK-021 | AC — via TC-CHK-066 | via TC-CHK-066 | **GAP** — derived TC(s) not green: TC-CHK-066 |
| AC-CHK-022 | AC-CHK-022 | AC — via TC-CHK-009, TC-CHK-092 | via TC-CHK-009, TC-CHK-092 | **NOT-EXERCISABLE** — derived TC(s) not exercisable: TC-CHK-092 |
| AC-CHK-023 | AC-CHK-023 | AC — via TC-CHK-010, TC-CHK-093 | via TC-CHK-010, TC-CHK-093 | **NOT-EXERCISABLE** — derived TC(s) not exercisable: TC-CHK-093 |
| AC-CHK-028 | AC-CHK-028 | AC — via TC-CHK-014 | via TC-CHK-014 | **GAP** — derived TC(s) not green: TC-CHK-014 |
| AC-CHK-035 | AC-CHK-035 | AC — via TC-CHK-068 | via TC-CHK-068 | **GAP** — derived TC(s) not green: TC-CHK-068 |
| AC-CHK-058 | AC-CHK-058 | AC — via TC-CHK-075 | via TC-CHK-075 | PASS |
| AC-CHK-059 | AC-CHK-059 | AC — via TC-CHK-076 | via TC-CHK-076 | PASS |

## STEP 5 — Failures and skips, classified

| Code | Count | Why |
|---|---|---|
| `TEST_STRUCTURE_FAILURE` | 75 | no api-verify check or E2E scenario exercises the id (the test suites lack it — a test-side gap, not an app defect); or the case is reachable only by a thread-level / in-process test, which the no-JUnit policy excludes |
| `MISSING_IMPLEMENTATION` | 9 | MODEL-EVAL needs the `model-eval` profile runner with a known-result set, which was never delivered |

No FAIL: no api-verify check and no E2E scenario carrying an id of this module failed. No app defect was found; no source was changed.

- `TEST_STRUCTURE_FAILURE` (75): TC-CHK-005 (GAP), TC-CHK-006 (GAP), TC-CHK-007 (GAP), TC-CHK-008 (GAP), TC-CHK-011 (GAP), TC-CHK-012 (GAP), TC-CHK-013 (GAP), TC-CHK-014 (GAP), TC-CHK-015 (GAP), TC-CHK-016 (GAP), TC-CHK-017 (GAP), TC-CHK-018 (GAP), TC-CHK-019 (GAP), TC-CHK-020 (GAP), TC-CHK-021 (GAP), TC-CHK-022 (GAP), TC-CHK-023 (GAP), TC-CHK-026 (GAP), TC-CHK-029 (GAP), TC-CHK-030 (GAP), TC-CHK-033 (GAP), TC-CHK-034 (GAP), TC-CHK-035 (GAP), TC-CHK-036 (GAP), TC-CHK-039 (GAP), TC-CHK-042 (GAP), TC-CHK-043 (GAP), TC-CHK-044 (GAP), TC-CHK-045 (GAP), TC-CHK-048 (GAP), TC-CHK-049 (GAP), TC-CHK-050 (GAP), TC-CHK-051 (GAP), TC-CHK-053 (GAP), TC-CHK-054 (GAP), TC-CHK-055 (GAP), TC-CHK-057 (GAP), TC-CHK-058 (GAP), TC-CHK-060 (GAP), TC-CHK-061 (GAP), TC-CHK-062 (GAP), TC-CHK-063 (GAP), TC-CHK-064 (GAP), TC-CHK-065 (GAP), TC-CHK-066 (GAP), TC-CHK-067 (GAP), TC-CHK-068 (GAP), TC-CHK-069 (GAP), TC-CHK-070 (GAP), TC-CHK-072 (GAP), TC-CHK-074 (GAP), TC-CHK-077 (GAP), TC-CHK-078 (GAP), TC-CHK-079 (GAP), TC-CHK-080 (GAP), TC-CHK-081 (GAP), TC-CHK-082 (GAP), TC-CHK-096 (GAP), TC-CHK-097 (GAP), TC-CHK-098 (GAP), TC-CHK-099 (GAP), TC-CHK-100 (GAP), AC-CHK-008 (GAP), AC-CHK-009 (GAP), AC-CHK-012 (GAP), AC-CHK-013 (GAP), AC-CHK-014 (GAP), AC-CHK-015 (GAP), AC-CHK-016 (GAP), AC-CHK-020 (GAP), AC-CHK-021 (GAP), AC-CHK-022 (NOT-EXERCISABLE), AC-CHK-023 (NOT-EXERCISABLE), AC-CHK-028 (GAP), AC-CHK-035 (GAP)
- `MISSING_IMPLEMENTATION` (9): TC-CHK-087 (NOT-EXERCISABLE), TC-CHK-088 (NOT-EXERCISABLE), TC-CHK-089 (NOT-EXERCISABLE), TC-CHK-090 (NOT-EXERCISABLE), TC-CHK-091 (NOT-EXERCISABLE), TC-CHK-092 (NOT-EXERCISABLE), TC-CHK-093 (NOT-EXERCISABLE), TC-CHK-094 (NOT-EXERCISABLE), TC-CHK-095 (NOT-EXERCISABLE)

## STEP 1 — Package rows

A unit's `package` row is recorded only when every id it lists has PASS evidence (then `--failed 0` → accepted). A unit with any GAP / NOT-EXERCISABLE id is **not recorded** — recording `passed=k failed=0` would falsely mark it accepted.

| Unit | kind | ids (source) | PASS | FAIL | row | not green |
|---|---|---|---|---|---|---|
| PORTS-DOCUMENT | exec_units | 4 (manifest) | 0 | 0 | **not accepted** — GAPs | TC-CHK-011, TC-CHK-044, TC-CHK-063, TC-CHK-064 |
| PORTS-MODEL | exec_units | 15 (manifest) | 1 | 0 | **not accepted** — GAPs | TC-CHK-017, TC-CHK-018, TC-CHK-021, TC-CHK-022, TC-CHK-035, TC-CHK-048, TC-CHK-049, TC-CHK-053, TC-CHK-068, TC-CHK-069, TC-CHK-070, TC-CHK-079, TC-CHK-081, TC-CHK-082 |
| PORTS-QUERY | exec_units | 9 (manifest) | 2 | 0 | **not accepted** — GAPs | TC-CHK-006, TC-CHK-007, TC-CHK-008, TC-CHK-030, TC-CHK-060, TC-CHK-061, TC-CHK-062 |
| SVC-API | exec_units | 67 (manifest) | 26 | 0 | **not accepted** — GAPs | TC-CHK-005, TC-CHK-012, TC-CHK-013, TC-CHK-014, TC-CHK-015, TC-CHK-016, TC-CHK-019, TC-CHK-020, TC-CHK-023, TC-CHK-026, TC-CHK-029, TC-CHK-033, TC-CHK-034, TC-CHK-036, TC-CHK-039, TC-CHK-042, TC-CHK-043, TC-CHK-045, TC-CHK-050, TC-CHK-051, TC-CHK-054, TC-CHK-055, TC-CHK-057, TC-CHK-058, TC-CHK-065 … (+16) |
| API-SCENARIOS | test_units | 33 (manifest) | 10 | 0 | **not accepted** — GAPs | TC-CHK-054, TC-CHK-055, TC-CHK-057, TC-CHK-058, TC-CHK-060, TC-CHK-061, TC-CHK-062, TC-CHK-063, TC-CHK-064, TC-CHK-065, TC-CHK-066, TC-CHK-067, TC-CHK-068, TC-CHK-069, TC-CHK-070, TC-CHK-072, TC-CHK-074, TC-CHK-077, TC-CHK-078, TC-CHK-079, TC-CHK-080, TC-CHK-081, TC-CHK-082 |
| INT-XM | test_units | 5 (manifest) | 0 | 0 | **not accepted** — GAPs | TC-CHK-096, TC-CHK-097, TC-CHK-098, TC-CHK-099, TC-CHK-100 |
| MODEL-EVAL | test_units | 9 (manifest) | 0 | 0 | **not accepted** — GAPs | TC-CHK-087, TC-CHK-088, TC-CHK-089, TC-CHK-090, TC-CHK-091, TC-CHK-092, TC-CHK-093, TC-CHK-094, TC-CHK-095 |
| RULE-SCENARIOS | test_units | 53 (manifest) | 19 | 0 | **not accepted** — GAPs | TC-CHK-005, TC-CHK-006, TC-CHK-007, TC-CHK-008, TC-CHK-011, TC-CHK-012, TC-CHK-013, TC-CHK-014, TC-CHK-015, TC-CHK-016, TC-CHK-017, TC-CHK-018, TC-CHK-019, TC-CHK-020, TC-CHK-021, TC-CHK-022, TC-CHK-023, TC-CHK-026, TC-CHK-029, TC-CHK-030, TC-CHK-033, TC-CHK-034, TC-CHK-035, TC-CHK-036, TC-CHK-039 … (+9) |
| XM-CHK-001 | integration | 4 (manifest) | 2 | 0 | **not accepted** — GAPs | AC-CHK-008, TC-CHK-096 |
| XM-CHK-002 | integration | 9 (manifest) | 3 | 0 | **not accepted** — GAPs | AC-CHK-008, AC-CHK-009, AC-CHK-016, AC-CHK-028, AC-CHK-035, TC-CHK-097 |
| XM-CHK-003 | integration | 4 (manifest) | 0 | 0 | **not accepted** — GAPs | AC-CHK-012, AC-CHK-013, AC-CHK-016, TC-CHK-098 |
| XM-CHK-004 | integration | 5 (manifest) | 0 | 0 | **not accepted** — GAPs | AC-CHK-020, AC-CHK-021, AC-CHK-022, AC-CHK-023, TC-CHK-099 |
| XM-CHK-005 | integration | 4 (manifest) | 1 | 0 | **not accepted** — GAPs | AC-CHK-014, AC-CHK-015, TC-CHK-100 |

ALIGN-BE / CORE / DATA-DOM* list no tests and were accepted (0/0 after a clean build) during execution; unchanged.

## Open gap rows of the module (`execution-state.json`)

- `api_doc_gaps` · **aias.check.upload-window default** — OPEN — default value pending spec clarification
- `api_doc_gaps` · **ComparisonModelPort.compare (PORTS-MODEL): deadline bound and timed-out outcome** — OPEN — MISSING_IN_DOCS pending spec clarification
- `api_doc_gaps` · **CheckEngine / pipeline (SVC-API): unstated texts, formats and failure codes** — OPEN — MISSING_IN_DOCS pending spec clarification
- `api_doc_gaps` · **GET /api/v1/active-checks/{checkId} — unsupported Accept header** — OPEN — MISSING_IN_DOCS pending spec clarification
- `api_doc_gaps` · **service queries: platform MCP query channel, query-tool argument protocol, and readOnly on CON-REG-011 not stated** — OPEN — MISSING_IN_DOCS pending spec clarification · interim implemented: SpringAiMcpQueryChannel over the Spring AI MCP client; endpoint convention `mcp:<client>`; query-tool args {sql, binds, maxRows} taken from the local Oracle MCP server; factory to confirm the protocol

## Notes

- Evidence is taken from the tags the api-verify checks and the E2E scenarios carry. The orchestrate STEP 4.4 second-agent coverage debate and the STEP 4.5 fixing agent were not run in this pass (scope: close the test phase honestly with the available evidence); the GAP set above is the input for them.
- Model used for the comparison in the counted E2E run: `gemini-3.6-flash` (Gemini free tier, model calls: comparison 11, reading 1). The first attempt of the day (run on `gemini-3.8-flash`) hit that model's daily quota after 4 calls; it is kept as history (`E2E-SIMULATION-2026-10-03-quota-run.json`) and not counted.

## Update — 2026-10-03 (CHK gap closure, E2E run `20261003T135010Z` + `-model` run)

The 73 GAP ids above were taken up by new E2E groups `chk-pipeline`, `chk-endings`, `chk-gate`, `chk-eval`, `chk-inprocess` (and TC-CHK-100 added to the `connection` group). The history above is kept unchanged. No JUnit.

Runs (`scripts/e2e/simulate.py`):

- [`E2E-SIMULATION-CHK-2026-10-03.json`](E2E-SIMULATION-CHK-2026-10-03.json) / [`-run.md`](E2E-SIMULATION-CHK-2026-10-03-run.md) — run `20261003T135010Z`: the five CHK groups plus regressions `registry` 4/4, `refusals` 6/6, `expiry` 1/1, `connection` 3/3 (incl. TC-CHK-100), `lifecycle` 1 PASSED + 1 SKIPPED-QUOTA, `limits` 5 SKIPPED-QUOTA. CHK groups: 35 PASSED · 0 FAILED · 1 SKIPPED-QUOTA (TC-CHK-081) · 6 NOT-EXERCISABLE.
- [`-model.json`](E2E-SIMULATION-CHK-2026-10-03-model.json) — TC-CHK-082 split into its own stub-only scenario after the quota skip: PASSED 18/18.
- [`-regression.json`](E2E-SIMULATION-CHK-2026-10-03-regression.json) — `lifecycle` / `limits` retried on `gemini-3.7-flash` and `gemini-3.5-flash` (via `AIAS_CHECK_COMPARISONMODEL_MODEL`): daily quota 429 again on the first call. Those groups' earlier PASS evidence (TC-CHK-028/031/032/085) stands from run `20261003T092840Z`; their regression after the MCP-channel fix below could not be re-run on a real model today.

**Method.** Isolated fixtures under `local/e2e-chk/` (10 synthetic packages with run-tagged codes, storage root), override mode C1: `aias.check.timeout=PT45S`, `upload-window=PT75S`, `deadline-check-interval=PT2S`, `max-rows=1000`, an extra jdbc connection `main-db`, comparison model `e2e-cmp-model-b`. The comparison provider is the scripted `scripts/e2e/model_stub.py` (new: `raw` answers for free text); a second `model_tap.py` (port 7294, new `MODEL_TAP_RECORD`) between app and stub records the request the app sends (two messages, keys, tools); `mcp_tap.py` records SQL / binds / maxRows; DEBUG logs of `io.agenticai.chk` and DOC's fetch give step order, query names, "ms left"; DOC's INFO "DOC check ended checkId=" counts end-of-Check notices. Modes C2a (`aias.documents.data-class=` empty) and C2b (`comparison-model.tier=` empty, data class REAL) for the free-tier gate. MODEL-EVAL: the app started with profiles `local,model-eval` and `aias.check.model-eval.directory` pointing at a SYNTHETIC known-result set written by the runner in `KnownResultRequest`'s documented format (SYN-001…006, and SYN-901 for the mismatch run), against the stub. Scaled values (stated in each scenario): timeout 45 s instead of 300 s (TC-CHK-033/034/062/084), upload window 75 s (TC-CHK-051). The normal mode was restored at the end of every run.

**Model quota.** Probe at the start: `gemini-3.6-flash` and `gemini-3.7-flash` 200 (2 calls). The counted run's first real call (lifecycle) got a daily 429 on `gemini-3.6-flash`; retries on `gemini-3.7-flash` and `gemini-3.5-flash` (after a 200 probe, plus a 429 probe of `gemini-3.8-flash`) were also 429 on their first call. Real comparison calls made by the app: 3, all 429; 0 completed. Everything else was proven through stub + tap: the request shape (messages, schema, tools, model id) is the app's own whichever provider answers.

**Defect fixed (TC-CHK-078, REQ-CHK-063 precondition)** — `src/main/java/io/agenticai/platform/mcp/SpringAiMcpQueryChannel.java:81-88, 179-188` (platform MCP query channel behind CHK's `McpServiceQueryAdapter`, also used by DOC). Two concurrent Checks sent their service queries on the same MCP stdio client at the same time; the client's transport accepts one outbound message at a time, and one call failed with `RuntimeException: Failed to enqueue message`. That Check then recorded `request_details` as not read and ended NEEDS_MANUAL_REVIEW instead of COMPLIANT. Calls of one client are now serialised by a per-client `ReentrantLock` (taken interruptibly inside the deadline-bounded call). App stopped, `mvn -DskipTests package`, restarted; TC-CHK-078 rerun green, and all chk groups green in the counted run.

**Not app defects, recorded as observations.** (1) TC-CHK-030: Oracle 23 words ORA-00942 as `table or view "LOAN_SYS"."E2E_CHK_NO_SUCH_TABLE" does not exist`; the detail carries the host's text unchanged (asserted), not the TC's older wording. (2) TC-CHK-054: `startCheck` returns to INT 2 ms before the pipeline sends its first query (INT log vs MCP send log); the HTTP answer reaches the client ~90 ms later, after that send. The ordering is a race by design (the pipeline is submitted, then the id returned); the in-process order was asserted.

**Ratio: 110 / 118 required ids PASS (93.2 %)** — PASS 110 · FAIL 0 · GAP 1 (TC-CHK-081, quota) · NOT-EXERCISABLE 7 · AMBIGUOUS 0. Before: 34 PASS · 73 GAP · 11 NOT-EXERCISABLE. TC blocks: 93 / 100 PASS; ACs: 17 / 18 PASS (AC-CHK-009 NOT-EXERCISABLE via TC-CHK-058).

| TC/AC id | scenario | evidence | result |
|---|---|---|---|
| TC-CHK-001 | Start with a blank employee identity is refused and creates nothing | E2E [refusals] Start refusals: unknown service, incomplete start, unreadable body, un → PASSED<br>E2E [refusals] Start refusals: unknown service, incomplete start, unreadable body, unsupported  → PASSED (run `20261003T135010Z`) | PASS |
| TC-CHK-002 | Start for a service whose available flag is false is refused | E2E [withdrawn] A package folder moved out: the service is WITHDRAWN, unlisted, read a → PASSED | PASS |
| TC-CHK-003 | Start for an unknown service code is refused and returns no identifier | E2E [refusals] Start refusals: unknown service, incomplete start, unreadable body, un → PASSED<br>E2E [refusals] Start refusals: unknown service, incomplete start, unreadable body, unsupported  → PASSED (run `20261003T135010Z`) | PASS |
| TC-CHK-004 | Start for a service whose connection is not activated is refused | E2E [connection] The connection removed from the activation config: REMOVED; start -> 4 → PASSED<br>E2E [connection] The connection removed from the activation config: REMOVED; start -> 422 CHK-422 → PASSED (run `20261003T135010Z`) | PASS |
| TC-CHK-005 | An unexpected error in a pipeline step ends the Check FAILED with INTERNAL_ERROR | E2E [chk-inprocess] An unexpected error in a deterministic step ends the Check FAILED / INTERNAL_ERR → NOT-EXERCISABLE: needs a fault injected into the deterministic step: no request data, document or model output reaching it through the API raises an unexpected error there (every shape is handled — absent evidence, unparsable values, unk | **NOT-EXERCISABLE** |
| TC-CHK-006 | A request number carrying SQL text is sent only as the bound value | E2E [chk-pipeline] A request number carrying SQL text is only the bound value; a query with no bind → PASSED (10 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-007 | A query over a connection that is not read-only is not run and is recorded as not read | E2E [chk-pipeline] A query over a connection that is not a read-only MCP connection (main-db, type  → PASSED (8 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-008 | The document source query is never sent by the Check Engine | E2E [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's  → PASSED (33 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-009 | A missing required document gives NOT_SATISFIED with evidence MISSING | E2E [manual] Missing document: confirm without an upload -> TRANSCRIPT MISSING, NOT → PASSED | PASS |
| TC-CHK-010 | An unreadable required document gives UNDETERMINED with its reason as evidence | E2E [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outs → PASSED | PASS |
| TC-CHK-011 | A Document Access failure ends the Check FAILED with INTERNAL_ERROR and the failure text | E2E [chk-inprocess] A Document Access failure ends the Check FAILED / INTERNAL_ERROR with the failur → NOT-EXERCISABLE: Document Access raises only for an unresolvable service version ("service package version not found"); a Check always fetches the version it was started on and REG keeps every version, every other DOC failure is a docume | **NOT-EXERCISABLE** |
| TC-CHK-012 | A model outcome contradicting the recomputed comparison is replaced by the computation | E2E [chk-pipeline] A model SATISFIED contradicting the recomputed comparison (2.8 >= 3.0) is replac → PASSED (7 checks, run `20261003T135010Z`)<br>E2E [chk-pipeline] Deterministic checks part 2 (ADR-CHK-003), one finding each: value not in the da → PASSED (15 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-013 | A value found that is not in the Check's data gives UNDETERMINED | E2E [chk-pipeline] Deterministic checks part 2 (ADR-CHK-003), one finding each: value not in the da → PASSED (15 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-014 | A limit the service knowledge does not state gives UNDETERMINED with the RULE-CHK-006 note | E2E [chk-pipeline] Deterministic checks part 2 (ADR-CHK-003), one finding each: value not in the da → PASSED (15 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-015 | A value that cannot be read as a number gives UNDETERMINED | E2E [chk-pipeline] Deterministic checks part 2 (ADR-CHK-003), one finding each: value not in the da → PASSED (15 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-016 | Evidence absent from the query results and documents gives UNDETERMINED | E2E [chk-pipeline] Deterministic checks part 2 (ADR-CHK-003), one finding each: value not in the da → PASSED (15 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-017 | The comparison model is called with no tool and no function definition | E2E [chk-pipeline] The model call: 2 messages, 0 tools, the fixed output schema (condition, outcome → PASSED (19 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-018 | Query text in the model output is never executed | E2E [chk-pipeline] A COMPLIANT Check of an approval-enabled service: 0 approval calls; a model note → PASSED (9 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-019 | A COMPLIANT Check of an approval-enabled service calls no approval API | E2E [chk-pipeline] A COMPLIANT Check of an approval-enabled service: 0 approval calls; a model note → PASSED (9 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-020 | A completed Check hands over no Employee Decision | E2E [chk-pipeline] A COMPLIANT Check: no Employee Decision handed over, document outcomes without c → PASSED (9 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-021 | Instruction-like document content stays data and does not change the outcome | E2E [chk-pipeline] Instruction text in the transcript and a forged </check-data> in a query result  → PASSED (9 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-022 | Model output outside the fixed structure ends the Check FAILED with MODEL_OUTPUT_INVALID | E2E [chk-endings] Model output outside the fixed structure (free text) -> FAILED / MODEL_OUTPUT_IN → PASSED (10 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-023 | A model finding with empty evidence gives UNDETERMINED | E2E [chk-pipeline] Deterministic checks part 2 (ADR-CHK-003), one finding each: value not in the da → PASSED (15 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-024 | Any NOT_SATISFIED finding gives NOT_COMPLIANT, even beside UNDETERMINED | E2E [manual] Manual NOT_COMPLIANT: a failing transcript (GPA 2.10, 90 credits) → PASSED<br>E2E [chk-pipeline] Overall Status precedence: 1 NOT_SATISFIED + 1 UNDETERMINED + 3 SATISFIED -> NOT → PASSED (run `20261003T135010Z`) | PASS |
| TC-CHK-025 | An UNDETERMINED finding without any NOT_SATISFIED gives NEEDS_MANUAL_REVIEW | E2E [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outs → PASSED<br>E2E [chk-pipeline] Overall Status: 1 UNDETERMINED + 4 SATISFIED, every query read -> NEEDS_MANUAL_R → PASSED (run `20261003T135010Z`) | PASS |
| TC-CHK-026 | An unread service query gives NEEDS_MANUAL_REVIEW although every finding is SATISFIED | E2E [chk-pipeline] A failing service query (ORA-00942) is recorded as not read, the Check continues → PASSED (9 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-027 | Every finding SATISFIED and every query read gives COMPLIANT | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-CHK-028 | After completion the Check Engine's schema keeps no report data and no Active Check | E2E [lifecycle] After the Check ends: no Active Check, and its uploads are deleted → PASSED<br>E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRE → PASSED<br>E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRED; Active  → PASSED (run `20261003T135010Z`) | PASS |
| TC-CHK-029 | A rejected complete call ends the Check FAILED with INTERNAL_ERROR | E2E [chk-endings] The result port rejects the complete call (a finding with a blank condition) ->  → PASSED (9 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-030 | A failing service query is recorded as not read and the Check continues | E2E [chk-pipeline] A failing service query (ORA-00942) is recorded as not read, the Check continues → PASSED (9 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-031 | A query returning more than the maximum rows is recorded as not read and passes no rows | E2E [limits] max-rows 1: a query answering 2 rows is recorded unread ('more than 1  → PASSED | PASS |
| TC-CHK-032 | Boundary — a query returning exactly the maximum rows is read in full | E2E [limits] max-rows 1: a query answering 2 rows is recorded unread ('more than 1  → PASSED | PASS |
| TC-CHK-033 | A Check RUNNING past the timeout ends FAILED with TIMED_OUT and the late answer is discarded | E2E [chk-endings] A Check RUNNING past its timeout (the model holds its answer 60 s, timeout 45 s) → PASSED (11 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-034 | The timeout counts from RUNNING, not from the wait for uploads | E2E [chk-endings] Timeout counted from RUNNING: a Check waiting 50 s (> the 45 s timeout) is confi → PASSED (20 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-035 | An unreachable comparison model ends the Check FAILED with MODEL_UNAVAILABLE | E2E [chk-endings] The comparison model provider answers HTTP 503 -> FAILED / MODEL_UNAVAILABLE, no → PASSED (9 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-036 | A FAILED Check carries one reason, a detail, an end time and no Overall Status | E2E [chk-endings] A Check RUNNING past its timeout (the model holds its answer 60 s, timeout 45 s) → PASSED (11 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-037 | Unfinished Checks of an earlier run are ended INTERRUPTED at start-up and notified | E2E [interrupted] A waiting Check is ended INTERRUPTED by a restart; its Active Check an → PASSED | PASS |
| TC-CHK-038 | Confirming the uploads of an unknown Check is refused | E2E [refusals] Confirmation of an unknown Check; reads of unknown Checks → PASSED<br>E2E [refusals] Confirmation of an unknown Check; reads of unknown Checks → PASSED (run `20261003T135010Z`)<br>E2E [chk-endings] Timeout counted from RUNNING: a Check waiting 50 s (> the 45 s timeout) is confi → PASSED (run `20261003T135010Z`) | PASS |
| TC-CHK-039 | Confirming the uploads of a FAILED Check is refused and leaves it FAILED | E2E [chk-endings] Timeout counted from RUNNING: a Check waiting 50 s (> the 45 s timeout) is confi → PASSED (20 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-040 | An elapsed upload window ends the Check FAILED with UPLOAD_WINDOW_EXPIRED and notifies Document Access | E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRE → PASSED<br>E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRED; Active  → PASSED (run `20261003T135010Z`) | PASS |
| TC-CHK-041 | Boundary — an upload window not yet elapsed does not end the Check | E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRE → PASSED<br>E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRED; Active  → PASSED (run `20261003T135010Z`) | PASS |
| TC-CHK-042 | A Check FAILED with MODEL_OUTPUT_INVALID sends exactly one end-of-Check notice | E2E [chk-endings] Model output outside the fixed structure (free text) -> FAILED / MODEL_OUTPUT_IN → PASSED (10 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-043 | The end-of-Check notice is sent once on every ending path | E2E [chk-gate] The end-of-Check notice is sent exactly once on every ending path (COMPLETED and → PASSED (1 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-044 | A failing end-of-Check notice is retried once, logged, and the ending is kept | E2E [chk-inprocess] A failing end-of-Check notice is retried once, logged, and the ending is kept → NOT-EXERCISABLE: needs Document Access's endCheck to fail twice while the ending's own database transaction succeeds — both use the same database; not producible through the API | **NOT-EXERCISABLE** |
| TC-CHK-045 | A Check's working data is discarded when it ends | E2E [chk-pipeline] A completed Check's working data is discarded: its query-result, document and mo → PASSED (10 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-046 | Nothing is sent to a free-tier comparison model on REAL data | E2E [notpermitted] FREE comparison model on REAL data: a path Check ends FAILED / MODEL_N → PASSED | PASS |
| TC-CHK-047 | A Check on a free-tier model with REAL data ends FAILED with MODEL_NOT_PERMITTED | E2E [notpermitted] FREE comparison model on REAL data: a path Check ends FAILED / MODEL_N → PASSED<br>E2E [notpermitted] FREE reading model on REAL data: a scanned (PNG) upload is never sent; → PASSED | PASS |
| TC-CHK-048 | An undeclared data class counts as REAL and blocks the free-tier model | E2E [chk-gate] Data class declared without a value counts as REAL: the FREE model receives 0 ca → PASSED (27 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-049 | An undeclared comparison model tier counts as FREE | E2E [chk-gate] Comparison model tier declared without a value counts as FREE: with data class R → PASSED (20 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-050 | A Check that already ended is not ended a second time by its timeout | E2E [chk-endings] A Check that completes just before its deadline is not ended a second time by th → PASSED (8 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-051 | The deadline check ends only the Active Checks whose deadline has passed, by status | E2E [chk-endings] Timeout counted from RUNNING: a Check waiting 50 s (> the 45 s timeout) is confi → PASSED (20 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-052 | Start-up removes every Active Check left by an earlier run | E2E [interrupted] A waiting Check is ended INTERRUPTED by a restart; its Active Check an → PASSED | PASS |
| TC-CHK-053 | Instruction text and a forged data delimiter in a query result stay inside the data part | E2E [chk-pipeline] Instruction text in the transcript and a forged </check-data> in a query result  → PASSED (9 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-054 | Starting a Check returns its identifier before any service query is sent | E2E [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's  → PASSED (33 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-055 | The request number and the employee identity are handed over exactly as sent | E2E [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's  → PASSED (33 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-056 | A `path` Check is marked RUNNING and runs in the background | E2E [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outs → PASSED | PASS |
| TC-CHK-057 | The current version is loaded and recorded with the new Check run | E2E [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's  → PASSED (33 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-058 | A `manual` Check keeps its start version when a newer one becomes current | E2E [chk-inprocess] A manual Check keeps its start version when a newer one becomes current → NOT-EXERCISABLE: a newer version becomes current only through a load run at an instance start, and every start ends all unfinished Checks INTERRUPTED (REQ-CHK-055, global): no Check can await uploads on version 3 while version 4 is curre | **NOT-EXERCISABLE** |
| TC-CHK-059 | Two services of different fetch modes run the same six steps | E2E [blob] Blob flow: the TRANSCRIPT BLOB is read over the jdbc connection local- → PASSED | PASS |
| TC-CHK-060 | Only the version's non-source queries are sent, with their SQL text unaltered | E2E [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's  → PASSED (33 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-061 | A service query goes only through the MCP query channel of its read-only connection | E2E [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's  → PASSED (33 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-062 | Each query call carries the maximum rows and the time left before the timeout | E2E [chk-pipeline] Each query call carries the row limit maxRows + 1 and only the Check's time left → PASSED (8 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-063 | Documents are fetched once from Document Access with the Check's identifiers | E2E [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's  → PASSED (33 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-064 | The Check Engine opens no file and no JDBC connection itself | E2E [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's  → PASSED (33 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-065 | Each required document type gives exactly one finding | E2E [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's  → PASSED (33 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-066 | One READ outcome satisfies a required document type | E2E [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's  → PASSED (33 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-067 | The model states value, location, comparison and limit of an explicit condition | E2E [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's  → PASSED (33 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-068 | The whole service knowledge is the only service instruction of the model input | E2E [chk-pipeline] The model call: 2 messages, 0 tools, the fixed output schema (condition, outcome → PASSED (19 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-069 | Query results and document content appear only between the data delimiters | E2E [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's  → PASSED (33 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-070 | The model call carries the fixed output schema and its output is validated | E2E [chk-pipeline] The model call: 2 messages, 0 tools, the fixed output schema (condition, outcome → PASSED (19 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-071 | The report has one finding per condition and per required document type | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-CHK-072 | A decided Overall Status is handed to the result port with the Check COMPLETED | E2E [chk-pipeline] Overall Status precedence: 1 NOT_SATISFIED + 1 UNDETERMINED + 3 SATISFIED -> NOT → PASSED (8 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-073 | The report metadata names version, fetch mode, model, times and employee | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-CHK-074 | Document outcomes are handed over without their content | E2E [chk-pipeline] A COMPLIANT Check: no Employee Decision handed over, document outcomes without c → PASSED (9 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-075 | A `manual` Check waits in AWAITING_DOCUMENTS and runs no query | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-CHK-076 | Confirming the uploads runs the pipeline on the recorded version | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-CHK-077 | A COMPLIANT Check sends exactly one end-of-Check notice | E2E [chk-pipeline] A COMPLIANT Check: no Employee Decision handed over, document outcomes without c → PASSED (9 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-078 | Two concurrent Checks share no data | E2E [chk-pipeline] Two concurrent Checks share no data; a second Check of the same request starts o → PASSED (18 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-079 | Each model call carries no earlier conversation | E2E [chk-pipeline] The model call: 2 messages, 0 tools, the fixed output schema (condition, outcome → PASSED (19 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-080 | A second Check of the same request starts independently | E2E [chk-pipeline] Two concurrent Checks share no data; a second Check of the same request starts o → PASSED (18 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-081 | Switching the provider by configuration needs no source change | E2E [chk-pipeline] Provider switched by configuration only: Gemini (provider A, real call) then the → SKIPPED-QUOTA: model quota already exhausted in this run (the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more | **GAP** — SKIPPED-QUOTA: the one real-provider Check found every free Gemini model's daily quota spent (429); provider B (stub) half green, see TC-CHK-082 |
| TC-CHK-082 | The comparison model is taken from configuration and recorded | E2E [chk-pipeline] The comparison model is taken from configuration (aias.check.comparison-model.mo → PASSED (18 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-083 | The Active Check of a new `manual` Check carries its upload-window deadline | api-verify CHK test_active_checks (4/4 checks)<br>E2E [lifecycle] Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upl → PASSED<br>E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRE → PASSED<br>E2E [lifecycle] Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upload-window → PASSED (run `20261003T135010Z`)<br>E2E [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRED; Active  → PASSED (run `20261003T135010Z`) | PASS |
| TC-CHK-084 | Confirmation moves the Active Check to RUNNING with the timeout deadline | api-verify CHK test_active_checks (3/3 checks) | PASS |
| TC-CHK-085 | A completed Check has no Active Check any more | api-verify CHK test_active_checks (4/4 checks)<br>E2E [lifecycle] After the Check ends: no Active Check, and its uploads are deleted → PASSED | PASS |
| TC-CHK-086 | The Active Check holds only identifier, status and deadline | E2E [lifecycle] Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upl → PASSED<br>E2E [lifecycle] Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upload-window → PASSED (run `20261003T135010Z`) | PASS |
| TC-CHK-087 | The known-result set has every Overall Status, each request synthetic | E2E [chk-inprocess] The known-result set has every Overall Status, each request synthetic → NOT-EXERCISABLE: no known-result set is delivered with the service (model-eval/known-result-set/ is absent); the chk-eval group supplies a synthetic one to exercise the runner, which proves the runner, not the delivered set | **NOT-EXERCISABLE** |
| TC-CHK-088 | The model-evaluation run reports expected and reached status per request | E2E [chk-eval] Model-evaluation run (profile model-eval) over a SYNTHETIC known-result set of 6 → PASSED (3 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-089 | A request reaching another status than expected fails the run and is named | E2E [chk-eval] A known-result request reaching another status than expected fails the run and i → PASSED (2 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-090 | Known-result request SYN-001 reaches COMPLIANT | E2E [chk-eval] Known-result request SYN-001 reaches COMPLIANT → PASSED (1 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-091 | Known-result request SYN-002 reaches NOT_COMPLIANT | E2E [chk-eval] Known-result request SYN-002 reaches NOT_COMPLIANT → PASSED (1 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-092 | Known-result request SYN-003 reaches NOT_COMPLIANT | E2E [chk-eval] Known-result request SYN-003 reaches NOT_COMPLIANT → PASSED (1 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-093 | Known-result request SYN-004 reaches NEEDS_MANUAL_REVIEW | E2E [chk-eval] Known-result request SYN-004 reaches NEEDS_MANUAL_REVIEW → PASSED (1 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-094 | Known-result request SYN-005 reaches NEEDS_MANUAL_REVIEW | E2E [chk-eval] Known-result request SYN-005 reaches NEEDS_MANUAL_REVIEW → PASSED (2 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-095 | Known-result request SYN-006 reaches NOT_COMPLIANT | E2E [chk-eval] Known-result request SYN-006 reaches NOT_COMPLIANT → PASSED (2 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-096 | XM-CHK-001 degraded — REG has no package for the code: start refused with a defined rejection | E2E [chk-pipeline] XM-CHK-001 degraded: no package of 'scholarship-request' loaded -> 422 CHK-422-S → PASSED (3 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-097 | XM-CHK-002 degraded — the recorded version cannot be resolved on resume: FAILED with INTERNAL_ERROR | E2E [chk-inprocess] XM-CHK-002 degraded: the recorded version cannot be resolved on resume → NOT-EXERCISABLE: REG never deletes a stored version and a waiting Check does not survive a restart (REQ-CHK-055), so getServicePackageVersion never answers not-found for a Check's recorded version | **NOT-EXERCISABLE** |
| TC-CHK-098 | XM-CHK-003 degraded — the version carries only its document source query: the Check still ends once | E2E [chk-pipeline] XM-CHK-003 degraded: the version's only query is its document source query -> 0  → PASSED (6 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-099 | XM-CHK-004 degraded — the version lists no required document type: no document finding, defined report | E2E [chk-pipeline] XM-CHK-004 degraded: the version lists no required document type -> 0 required-d → PASSED (5 checks, run `20261003T135010Z`) | PASS |
| TC-CHK-100 | XM-CHK-005 degraded — REG returns no connection settings: start refused as not activated | E2E [connection] The connection removed from the activation config: REMOVED; start -> 422 CHK-422 → PASSED (9 checks, run `20261003T135010Z`) | PASS |
| AC-CHK-005 | AC — via TC-CHK-002 | via TC-CHK-002 | PASS |
| AC-CHK-006 | AC — via TC-CHK-003 | via TC-CHK-003 | PASS |
| AC-CHK-007 | AC — via TC-CHK-004 | via TC-CHK-004 | PASS |
| AC-CHK-008 | AC — via TC-CHK-057 | via TC-CHK-057 | PASS |
| AC-CHK-009 | AC — via TC-CHK-058 | via TC-CHK-058 | **NOT-EXERCISABLE** — derived TC(s) not exercisable: TC-CHK-058 |
| AC-CHK-012 | AC — via TC-CHK-060 | via TC-CHK-060 | PASS |
| AC-CHK-013 | AC — via TC-CHK-006 | via TC-CHK-006 | PASS |
| AC-CHK-014 | AC — via TC-CHK-061 | via TC-CHK-061 | PASS |
| AC-CHK-015 | AC — via TC-CHK-007 | via TC-CHK-007 | PASS |
| AC-CHK-016 | AC — via TC-CHK-008 | via TC-CHK-008 | PASS |
| AC-CHK-020 | AC — via TC-CHK-065 | via TC-CHK-065 | PASS |
| AC-CHK-021 | AC — via TC-CHK-066 | via TC-CHK-066 | PASS |
| AC-CHK-022 | AC — via TC-CHK-009, TC-CHK-092 | via TC-CHK-009, TC-CHK-092 | PASS |
| AC-CHK-023 | AC — via TC-CHK-010, TC-CHK-093 | via TC-CHK-010, TC-CHK-093 | PASS |
| AC-CHK-028 | AC — via TC-CHK-014 | via TC-CHK-014 | PASS |
| AC-CHK-035 | AC — via TC-CHK-068 | via TC-CHK-068 | PASS |
| AC-CHK-058 | AC — via TC-CHK-075 | via TC-CHK-075 | PASS |
| AC-CHK-059 | AC — via TC-CHK-076 | via TC-CHK-076 | PASS |

NOT-EXERCISABLE (reason in the `chk-inprocess` scenarios): TC-CHK-005 (needs a fault injected into the deterministic step), TC-CHK-011 (DOC raises only for an unresolvable version, which a running Check can never have), TC-CHK-044 (DOC's endCheck failing twice while the ending's own DB transaction succeeds), TC-CHK-058 and TC-CHK-097 (a newer or vanished version needs a restart, which ends every waiting Check INTERRUPTED — same as TC-INT-032), TC-CHK-087 (no known-result set is delivered with the service; the test-supplied synthetic set proves the runner only), AC-CHK-009 (via TC-CHK-058). TC-CHK-007 is realised with `main-db` of type jdbc: REG refuses a not-read-only connection at activation (reg-activation), so CHK can only receive the non-mcp variant of RULE-CHK-002; the TC's message and outcomes are asserted literally.

### Package rows (this update)

Recorded (`--failed 0`, every listed id PASS): PORTS-QUERY 9/0, XM-CHK-001 4/0, XM-CHK-003 4/0, XM-CHK-004 5/0, XM-CHK-005 4/0 → accepted.

Withheld: PORTS-DOCUMENT (TC-CHK-011, TC-CHK-044), PORTS-MODEL (TC-CHK-081), SVC-API (TC-CHK-005, TC-CHK-058, TC-CHK-087), API-SCENARIOS (TC-CHK-058, TC-CHK-081), INT-XM (TC-CHK-097), MODEL-EVAL (TC-CHK-087), RULE-SCENARIOS (TC-CHK-005, TC-CHK-011, TC-CHK-044), XM-CHK-002 (AC-CHK-009, TC-CHK-097).

`validate CHK`: 0 problems. `delivery CHK`: CHK backend v1 OPEN (8): 8 units accepted (ALIGN-BE, CORE, DATA-DOM, PORTS-QUERY, XM-CHK-001/003/004/005), 8 not accepted.
