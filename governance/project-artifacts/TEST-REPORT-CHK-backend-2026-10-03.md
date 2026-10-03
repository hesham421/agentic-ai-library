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
