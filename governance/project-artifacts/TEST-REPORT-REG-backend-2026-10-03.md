# TEST REPORT — REG backend v1 — 2026-10-03

Test phase close-out per `.claude/commands/REG/execute-backend-test.md` STEP 1/4/5/6 and `.claude/commands/orchestrate-module.md` STEP 4. **No JUnit** (user policy 2026-10-02): the evidence is the api-verify scripts (`governance/shared/backend/modules/*/test-api/`, run 2026-10-03, all five exit 0, `api_verify` v1 recorded PASS) and the E2E simulation run `20261003T092840Z` ([`E2E-SIMULATION-2026-10-03.json`](E2E-SIMULATION-2026-10-03.json)). An id counts as PASS only with green evidence tagged with that id; NOT-EXERCISABLE is never counted as passed.

## STEP 4 — Coverage cross-check (governed plan ↔ tests)

**Ratio: 20 / 92 required ids PASS (21.7 %)** — PASS 20 · FAIL 0 · GAP 68 · NOT-EXERCISABLE 4 · DEFERRED 0 · OUT-OF-TRACK 7 (not in the denominator).

- TC blocks of the P4 test plan: 92 — PASS 20 · FAIL 0 · GAP 68 · NOT-EXERCISABLE 4.
- Every id listed in the `tests` of the exec units, test units and built integration packages is a TC block of the test plan or one of the AC rows above (no id is dropped). No integration package is deferred (every `requires` is met), so no row is DEFERRED.

```
GOVERNED PLAN ↔ TESTS — REG v1
```

| TC/AC id | traces | scenario | evidence (api-verify check / E2E scenario) | result |
|---|---|---|---|---|
| TC-REG-003 | AC-REG-003 API-REG-003 | Folder with only a service definition is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-004 | AC-REG-004 API-REG-003 API-REG-002 | Two folders declaring one service code are both rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-007 | AC-REG-007 API-REG-003 | A rejected folder is reported and loading continues | E2E [connection] A package whose query names an unregistered connection is REJECTED at  → PASSED | PASS |
| TC-REG-012 | AC-REG-012 | No package is supplied for a withdrawn service | E2E [withdrawn] A package folder moved out: the service is WITHDRAWN, unlisted, read a → PASSED | PASS |
| TC-REG-013 | AC-REG-013 | No package is supplied for an unknown service | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-016 | AC-REG-016 API-REG-002 | Read of an unknown service returns 404 | api-verify REG test_services (2/2 checks)<br>E2E [registry] An unknown service code is 404 REG-404-SERVICE-NOT-FOUND → PASSED | PASS |
| TC-REG-022 | AC-REG-022 API-REG-003 | Same version number with changed content is rejected (never edited in place) | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-023 | AC-REG-023 API-REG-003 API-REG-002 | A lower, unstored version is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-029 | AC-REG-029 API-REG-003 | Empty service knowledge is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-032 | AC-REG-032 API-REG-003 | A query naming an unregistered connection is rejected | E2E [connection] A package whose query names an unregistered connection is REJECTED at  → PASSED | PASS |
| TC-REG-033 | AC-REG-033 API-REG-003 | A query using another parameter than the declared input is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-034 | AC-REG-034 API-REG-003 | A query using a substitution marker is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-035 | AC-REG-035 API-REG-003 | A query that is not a single SELECT is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-036 | AC-REG-036 API-REG-003 | A service definition declaring a check limit is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-037 | AC-REG-037 API-REG-003 | Two queries with one name are rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-040 | AC-REG-040 API-REG-003 | An unknown fetch mode is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-041 | AC-REG-041 API-REG-003 | Fetch mode path without a path column is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-042 | AC-REG-042 API-REG-003 | Blob documents over an mcp connection are rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-043 | AC-REG-043 API-REG-003 | A service definition declaring a file location is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-045 | AC-REG-045 API-REG-003 | Enabled approval without a definition is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-049 | AC-REG-049 API-REG-003 | A connection name listed twice is refused | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-054 | AC-REG-054 API-REG-003 | An unknown connection type is refused | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-055 | AC-REG-055 | A version naming a removed connection is not supplied | E2E [connection] The connection removed from the activation config: REMOVED; start -> 4 → PASSED | PASS |
| TC-REG-057 | AC-REG-057 API-REG-003 | A connection not declared read-only is refused | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-063 | AC-REG-063 API-REG-003 | A failing pilot package is reported and never supplied | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-064 | AC-REG-064 API-REG-003 | A package folder holding another file is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-072 | AC-REG-065 API-REG-003 API-REG-002 | A required document type declared twice is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-074 | AC-REG-067 API-REG-003 | Two folders whose codes differ only in case are both rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-076 | AC-REG-069 API-REG-003 | An invalid service code is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-077 | AC-REG-070 API-REG-003 API-REG-001 | A missing package directory withdraws nothing | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-078 | AC-REG-071 API-REG-003 API-REG-001 | An empty package directory withdraws nothing while services are available | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-081 | AC-REG-074 API-REG-003 API-REG-002 | A package file that changes while it is read is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-082 | AC-REG-075 API-REG-003 | Fetch mode blob without a content column is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-083 | AC-REG-076 API-REG-003 | Fetch mode path without a type column is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-084 | AC-REG-077 API-REG-003 | A document source naming an undeclared query is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-085 | AC-REG-078 API-REG-003 API-REG-002 | Failing and succeeding items in one load run each get their own outcome | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-086 | AC-REG-079 API-REG-003 | A timeout in a service definition is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-087 | AC-REG-080 API-REG-003 | A maximum file size in a service definition is rejected | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-090 | AC-REG-083 API-REG-003 API-REG-002 | A present package folder with an unreadable file is rejected and the run continues | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-091 | AC-REG-084 API-REG-003 | A re-activation that turns a blob version's connection into mcp is refused | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-092 | AC-REG-085 API-REG-003 | A removed connection is not re-registered as mcp while a blob version reads through it | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-093 | AC-REG-086 API-REG-003 API-REG-002 | A folder whose name exceeds 200 characters is rejected; the run continues | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-094 | AC-REG-087 API-REG-003 | A connection name longer than 100 characters is refused | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-095 | AC-REG-088 API-REG-003 | A connection endpoint longer than 500 characters is refused | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-096 | AC-REG-089 API-REG-003 | An over-length package directory path is shortened on its Load Result row, never a load-run failure | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-097 | AC-REG-090 API-REG-003 | An over-length invalid service code is shortened on its Load Result row | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-098 | AC-REG-091 API-REG-003 API-REG-002 | A valid folder is rejected as a duplicate even when its twin fails another rule | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-099 | AC-REG-069 API-REG-003 API-REG-002 | A 100-character service code with single hyphens is accepted (boundary pass) | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-001 | AC-REG-001 API-REG-003 API-REG-002 | One Service Package per service code at first load | E2E [registry] The four fixture services are listed with their fetch modes, without S → PASSED | PASS |
| TC-REG-002 | AC-REG-002 API-REG-003 | Version stored as two separate parts | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-005 | AC-REG-005 API-REG-003 | Every folder of the package directory is loaded at start | E2E [registry] The four fixture services are listed with their fetch modes, without S → PASSED | PASS |
| TC-REG-006 | AC-REG-006 API-REG-003 API-REG-002 | A new service code is registered with its first version | E2E [registry] The load report shows both connections ACTIVATED and every fixture pac → PASSED | PASS |
| TC-REG-008 | AC-REG-008 API-REG-003 | The load report returns every row of the latest run | api-verify REG test_load_results (2/2 checks)<br>E2E [registry] The load report shows both connections ACTIVATED and every fixture pac → PASSED | PASS |
| TC-REG-009 | AC-REG-009 API-REG-003 | Earlier load results are removed at a new run | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-010 | AC-REG-010 API-REG-003 API-REG-001 API-REG-002 | A service whose folder is gone is withdrawn | E2E [withdrawn] A package folder moved out: the service is WITHDRAWN, unlisted, read a → PASSED<br>E2E [connection] Back to the normal registry: demo-conn's folder parked again -> WITHDR → PASSED | PASS |
| TC-REG-011 | AC-REG-011 API-REG-001 API-REG-003 | A returning service is available again | E2E [withdrawn] The folder put back: the service is available again with its stored ve → PASSED | PASS |
| TC-REG-014 | AC-REG-014 API-REG-001 | List of services returns available services only, without SQL or connection | api-verify REG test_services (4/4 checks)<br>E2E [registry] The four fixture services are listed with their fetch modes, without S → PASSED | PASS |
| TC-REG-015 | AC-REG-015 API-REG-002 | Read one service returns its current version summary | api-verify REG test_services (4/4 checks) | PASS |
| TC-REG-017 | AC-REG-017 API-REG-003 | A folder outside the package directory is never loaded | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-018 | AC-REG-018 API-REG-001 API-REG-002 API-REG-003 | The registry exposes read operations only | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-020 | AC-REG-020 API-REG-002 | Every version carries its version number | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-021 | AC-REG-021 API-REG-002 API-REG-003 | A higher version becomes current; the earlier stays stored | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-024 | AC-REG-024 API-REG-003 | An unchanged package registers no new version | E2E [withdrawn] The folder put back: the service is available again with its stored ve → PASSED | PASS |
| TC-REG-025 | AC-REG-025 | The current version is supplied to a Check | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-026 | AC-REG-026 | Any stored version resolves | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-027 | AC-REG-027 API-REG-003 | No stored version is deleted when a service is withdrawn | E2E [withdrawn] A package folder moved out: the service is WITHDRAWN, unlisted, read a → PASSED | PASS |
| TC-REG-030 | AC-REG-030 | Only the queries of the service definition are supplied, unaltered | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-038 | AC-REG-038 API-REG-002 | One fetch mode recorded per version | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-039 | AC-REG-039 API-REG-002 | Required document types recorded per version | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-044 | AC-REG-044 | An enabled approval API definition is recorded | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-046 | AC-REG-046 | The approval API definition reaches only the Employee Decision path | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-047 | AC-REG-047 | Disabled approval is reported to the Employee Decision path | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-048 | AC-REG-048 API-REG-003 | One connection defined once and shared by two packages | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-050 | AC-REG-050 | A connection is supplied by name | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-051 | AC-REG-051 API-REG-003 | Activation registers the environment's connections | E2E [registry] The load report shows both connections ACTIVATED and every fixture pac → PASSED<br>E2E [connection] A package whose query names an unregistered connection is REJECTED at  → PASSED | PASS |
| TC-REG-052 | AC-REG-052 API-REG-003 | Changed connection settings update the connection, not the versions | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-053 | AC-REG-053 API-REG-003 | A connection no longer listed is removed | E2E [connection] The connection removed from the activation config: REMOVED; start -> 4 → PASSED | PASS |
| TC-REG-056 | AC-REG-056 API-REG-001 | Only the credential reference is stored | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-058 | AC-REG-058 | The limited-to-views declaration is recorded | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-059 | AC-REG-059 API-REG-002 API-REG-003 | The scholarship-request pilot package loads | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-061 | AC-REG-061 API-REG-001 API-REG-002 API-REG-003 | No request data is stored in the registry | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-062 | AC-REG-062 | Supplied package content is read-only | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-073 | AC-REG-066 API-REG-003 API-REG-001 | A case and space variant of a stored service code is the same service | E2E [registry] A service code is read trimmed and case-insensitively → PASSED | PASS |
| TC-REG-075 | AC-REG-068 API-REG-002 | Read one service matches the code case-insensitively | api-verify REG test_services (3/3 checks)<br>E2E [registry] A service code is read trimmed and case-insensitively → PASSED | PASS |
| TC-REG-079 | AC-REG-072 API-REG-003 API-REG-001 | Two instances starting together produce one complete load report | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-080 | AC-REG-073 API-REG-003 | An instance that cannot take the load lock serves the stored registry | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-088 | AC-REG-081 API-REG-002 | Read of a withdrawn service reports available = false | E2E [withdrawn] A package folder moved out: the service is WITHDRAWN, unlisted, read a → PASSED | PASS |
| TC-REG-089 | AC-REG-082 | A Check's pinned version is supplied after a newer version became current | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-REG-019 | AC-REG-019 | Service knowledge supplied apart from queries and document references | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-REG-028 | AC-REG-028 | The whole service knowledge is supplied unaltered | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-REG-031 | AC-REG-031 API-REG-001 | Queries only through the query part; no SQL in the service knowledge | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-REG-060 | AC-REG-060 | Pilot numeric conditions are written as digits | ✗ none | **NOT-EXERCISABLE** — MODEL-EVAL needs the model-eval profile runner with a known-result set — never delivered |
| TC-REG-065 … TC-REG-071 | — | frontend track's TCs (test plan note: "TC-REG-065 … TC-REG-071 are the frontend track's") | — | OUT-OF-TRACK |

## STEP 5 — Failures and skips, classified

| Code | Count | Why |
|---|---|---|
| `TEST_STRUCTURE_FAILURE` | 68 | no api-verify check or E2E scenario exercises the id (the test suites lack it — a test-side gap, not an app defect); or the case is reachable only by a thread-level / in-process test, which the no-JUnit policy excludes |
| `MISSING_IMPLEMENTATION` | 4 | MODEL-EVAL needs the `model-eval` profile runner with a known-result set, which was never delivered |

No FAIL: no api-verify check and no E2E scenario carrying an id of this module failed. No app defect was found; no source was changed.

- `TEST_STRUCTURE_FAILURE` (68): TC-REG-003 (GAP), TC-REG-004 (GAP), TC-REG-013 (GAP), TC-REG-022 (GAP), TC-REG-023 (GAP), TC-REG-029 (GAP), TC-REG-033 (GAP), TC-REG-034 (GAP), TC-REG-035 (GAP), TC-REG-036 (GAP), TC-REG-037 (GAP), TC-REG-040 (GAP), TC-REG-041 (GAP), TC-REG-042 (GAP), TC-REG-043 (GAP), TC-REG-045 (GAP), TC-REG-049 (GAP), TC-REG-054 (GAP), TC-REG-057 (GAP), TC-REG-063 (GAP), TC-REG-064 (GAP), TC-REG-072 (GAP), TC-REG-074 (GAP), TC-REG-076 (GAP), TC-REG-077 (GAP), TC-REG-078 (GAP), TC-REG-081 (GAP), TC-REG-082 (GAP), TC-REG-083 (GAP), TC-REG-084 (GAP), TC-REG-085 (GAP), TC-REG-086 (GAP), TC-REG-087 (GAP), TC-REG-090 (GAP), TC-REG-091 (GAP), TC-REG-092 (GAP), TC-REG-093 (GAP), TC-REG-094 (GAP), TC-REG-095 (GAP), TC-REG-096 (GAP), TC-REG-097 (GAP), TC-REG-098 (GAP), TC-REG-099 (GAP), TC-REG-002 (GAP), TC-REG-009 (GAP), TC-REG-017 (GAP), TC-REG-018 (GAP), TC-REG-020 (GAP), TC-REG-021 (GAP), TC-REG-025 (GAP), TC-REG-026 (GAP), TC-REG-030 (GAP), TC-REG-038 (GAP), TC-REG-039 (GAP), TC-REG-044 (GAP), TC-REG-046 (GAP), TC-REG-047 (GAP), TC-REG-048 (GAP), TC-REG-050 (GAP), TC-REG-052 (GAP), TC-REG-056 (GAP), TC-REG-058 (GAP), TC-REG-059 (GAP), TC-REG-061 (GAP), TC-REG-062 (GAP), TC-REG-079 (GAP), TC-REG-080 (GAP), TC-REG-089 (GAP)
- `MISSING_IMPLEMENTATION` (4): TC-REG-019 (NOT-EXERCISABLE), TC-REG-028 (NOT-EXERCISABLE), TC-REG-031 (NOT-EXERCISABLE), TC-REG-060 (NOT-EXERCISABLE)

## STEP 1 — Package rows

A unit's `package` row is recorded only when every id it lists has PASS evidence (then `--failed 0` → accepted). A unit with any GAP / NOT-EXERCISABLE id is **not recorded** — recording `passed=k failed=0` would falsely mark it accepted.

| Unit | kind | ids (source) | PASS | FAIL | row | not green |
|---|---|---|---|---|---|---|
| PORTS | exec_units | 3 (manifest) | 2 | 0 | **not accepted** — GAPs | TC-REG-017 |
| SVC-API | exec_units | 89 (manifest) | 18 | 0 | **not accepted** — GAPs | TC-REG-003, TC-REG-004, TC-REG-013, TC-REG-022, TC-REG-023, TC-REG-029, TC-REG-033, TC-REG-034, TC-REG-035, TC-REG-036, TC-REG-037, TC-REG-040, TC-REG-041, TC-REG-042, TC-REG-043, TC-REG-045, TC-REG-049, TC-REG-054, TC-REG-057, TC-REG-063, TC-REG-064, TC-REG-072, TC-REG-074, TC-REG-076, TC-REG-077 … (+46) |
| API-SCENARIOS | test_units | 40 (unit .md TC blocks) | 15 | 0 | **not accepted** — GAPs | TC-REG-002, TC-REG-009, TC-REG-017, TC-REG-018, TC-REG-020, TC-REG-021, TC-REG-025, TC-REG-026, TC-REG-030, TC-REG-038, TC-REG-039, TC-REG-044, TC-REG-046, TC-REG-047, TC-REG-048, TC-REG-050, TC-REG-052, TC-REG-056, TC-REG-058, TC-REG-059, TC-REG-061, TC-REG-062, TC-REG-079, TC-REG-080, TC-REG-089 |
| MODEL-EVAL | test_units | 4 (unit .md TC blocks) | 0 | 0 | **not accepted** — GAPs | TC-REG-019, TC-REG-028, TC-REG-031, TC-REG-060 |
| RULE-SCENARIOS | test_units | 48 (unit .md TC blocks) | 5 | 0 | **not accepted** — GAPs | TC-REG-003, TC-REG-004, TC-REG-013, TC-REG-022, TC-REG-023, TC-REG-029, TC-REG-033, TC-REG-034, TC-REG-035, TC-REG-036, TC-REG-037, TC-REG-040, TC-REG-041, TC-REG-042, TC-REG-043, TC-REG-045, TC-REG-049, TC-REG-054, TC-REG-057, TC-REG-063, TC-REG-064, TC-REG-072, TC-REG-074, TC-REG-076, TC-REG-077 … (+18) |

ALIGN-BE / CORE / DATA-DOM* list no tests and were accepted (0/0 after a clean build) during execution; unchanged.

## Open gap rows of the module (`execution-state.json`)

- `api_doc_gaps` · **load run: malformed service.yaml, a definition missing version/input/query sql or connection, an activation entry missing a required value, and an unexpected persistence failure of one item have no load reason code; in-process VersionNotFoundException / ConnectionNotFoundException have no stated code** — OPEN — MISSING_IN_DOCS pending spec clarification
- `api_doc_gaps` · **GET /api/v1/services — unsupported Accept header** — OPEN — MISSING_IN_DOCS pending spec clarification

## Notes

- Evidence is taken from the tags the api-verify checks and the E2E scenarios carry. The orchestrate STEP 4.4 second-agent coverage debate and the STEP 4.5 fixing agent were not run in this pass (scope: close the test phase honestly with the available evidence); the GAP set above is the input for them.
- Model used for the comparison in the counted E2E run: `gemini-3.6-flash` (Gemini free tier, model calls: comparison 11, reading 1). The first attempt of the day (run on `gemini-3.8-flash`) hit that model's daily quota after 4 calls; it is kept as history (`E2E-SIMULATION-2026-10-03-quota-run.json`) and not counted.

---

## Update — 2026-10-03 (REG gap closure, E2E run `20261003T102414Z`)

The 68 GAP ids above were taken up by four new E2E groups in `scripts/e2e/simulate.py` (`reg-rules`, `reg-activation`, `reg-versions`, `reg-inprocess`; 65 scenarios), run together with a `registry` regression: [`E2E-SIMULATION-REG-2026-10-03.json`](E2E-SIMULATION-REG-2026-10-03.json) / [`-run.md`](E2E-SIMULATION-REG-2026-10-03-run.md) — **57 PASSED · 0 FAILED · 12 NOT-EXERCISABLE · 0 model calls**, 20 app restarts (one load run each, plus the end-of-run restore; TC-REG-079 starts a second instance on port 7272 beside the app). A further `registry` regression in the restored normal mode passed 4/4 ([`E2E-SIMULATION-REG-2026-10-03-registry-regression.json`](E2E-SIMULATION-REG-2026-10-03-registry-regression.json)). The history above is kept unchanged.

How the TC blocks are realised (method):

- **Preconditions** are fixture package folders in an isolated package directory `local/e2e-reg/<batch>` (override `aias.registry.package-directory`) and override activation entries; **start** = one load run (an app restart); **observations** = API-REG-001/002/003. The in-process interface is observed through the public API of its users: a CHK start (getCurrentServicePackage / RULE-REG-016/017 → `CHK-422-*`) and INT API-INT-008 (getServicePackageVersion of the Check's pinned version). REG-404's `detail` carries the same catalog text as the in-process `ServiceNotAvailableException` (RULE-REG-016).
- **Expected reasons** are compared character for character with the TC's English text.
- **Batching**: independent invalid folders are judged in one load run; a TC is asserted on the rows of its own subjects. TCs whose expectation is the whole run (TC-REG-009, -079, -085) have a run of their own with the exact row count. TC-REG-076's sub-cases ("each run separately") are judged in one run, one folder each.
- **Repeatability**: the local registry keeps every version forever, so a service code a TC **stores** carries the run tag (e.g. `scholarship-request-t1003102414`). The reason is the TC template filled with that code. Codes a TC expects rejected keep their literal value. TC-REG-078 uses an existing empty directory `local/e2e-reg/act-empty` in place of `/srv/aias/packages`, which cannot be created here.
- **Black-box proofs of stored values**: an unchanged re-declaration answers `ACTIVATED` / `UNCHANGED`, which proves the stored value. This covers TC-REG-022 (contentHash h1 kept: original content → UNCHANGED), TC-REG-052 (endpoint e2 → ACTIVATED at the next start), TC-REG-058 (only limited-to-views flipped → UPDATED) and TC-REG-091 (jdbc endpoint relisted → ACTIVATED).
- **Environment realisations**: TC-REG-081 uses a FIFO as `service.yaml`; the definition is written only when the app opens it, so its mtime moves during the read (in place of the TC's PackageSource stub). TC-REG-090 uses file mode 000. TC-REG-080 holds `LOCK TABLE REG_LOAD_RESULT IN EXCLUSIVE MODE` from a sqlplus session in the local Oracle container for 120 s instead of 60 s, because the app restart uses ~35 s of the hold before the instance's 30 s wait begins. TC-REG-017 uses a sibling folder plus a symbolic link inside the directory.
- **Model safety**: no REG scenario confirms a Check or starts a non-manual one that could run. Every REG mode also sets `aias.documents.data-class=REAL` with FREE models as defence in depth. Model calls: 0.

**Ratio: 76 / 92 required ids PASS (82.6 %)** — PASS 76 · FAIL 0 · GAP 0 · NOT-EXERCISABLE 16 · DEFERRED 0 · OUT-OF-TRACK 7 (TC-REG-065 … TC-REG-071, not in the denominator). Before: 20 PASS · 68 GAP · 4 NOT-EXERCISABLE.

| TC id | scenario | evidence | result |
|---|---|---|---|
| TC-REG-001 | One Service Package per service code at first load | E2E [registry] The four fixture services are listed with their fetch modes, without S → PASSED (unchanged; registry regression re-run 20261003T102414Z green) | PASS |
| TC-REG-002 | Version stored as two separate parts | serviceKnowledge and serviceDefinition of a stored version are returned only in-process (getServicePackageVersion); no HTTP operation returns either text (API-REG-002 is a summary), and the only consumer (CHK's comparison prompt) needs a model call | **NOT-EXERCISABLE** |
| TC-REG-003 | Folder with only a service definition is rejected | E2E [reg-rules] Folder with only a service definition is rejected → PASSED (3/3 checks) | PASS |
| TC-REG-004 | Two folders declaring one service code are both rejected | E2E [reg-versions] Two folders declaring one service code are both rejected → PASSED (2/2 checks) | PASS |
| TC-REG-005 | Every folder of the package directory is loaded at start | E2E [registry] The four fixture services are listed with their fetch modes, without S → PASSED (unchanged; registry regression re-run 20261003T102414Z green) | PASS |
| TC-REG-006 | A new service code is registered with its first version | E2E [registry] The load report shows both connections ACTIVATED and every fixture pac → PASSED (unchanged; registry regression re-run 20261003T102414Z green) | PASS |
| TC-REG-007 | A rejected folder is reported and loading continues | E2E [connection] A package whose query names an unregistered connection is REJECTED at  → PASSED | PASS |
| TC-REG-008 | The load report returns every row of the latest run | api-verify REG test_load_results (2/2 checks)<br>E2E [registry] The load report shows both connections ACTIVATED and every fixture pac → PASSED (unchanged; registry regression re-run 20261003T102414Z green) | PASS |
| TC-REG-009 | Earlier load results are removed at a new run | E2E [reg-rules] Earlier load results are removed at a new run → PASSED (6/6 checks) | PASS |
| TC-REG-010 | A service whose folder is gone is withdrawn | E2E [withdrawn] A package folder moved out: the service is WITHDRAWN, unlisted, read a → PASSED<br>E2E [connection] Back to the normal registry: demo-conn's folder parked again -> WITHDR → PASSED | PASS |
| TC-REG-011 | A returning service is available again | E2E [withdrawn] The folder put back: the service is available again with its stored ve → PASSED | PASS |
| TC-REG-012 | No package is supplied for a withdrawn service | E2E [withdrawn] A package folder moved out: the service is WITHDRAWN, unlisted, read a → PASSED | PASS |
| TC-REG-013 | No package is supplied for an unknown service | E2E [reg-inprocess] No package is supplied for an unknown service → PASSED (4/4 checks) | PASS |
| TC-REG-014 | List of services returns available services only, without SQL or connection | api-verify REG test_services (4/4 checks)<br>E2E [registry] The four fixture services are listed with their fetch modes, without S → PASSED (unchanged; registry regression re-run 20261003T102414Z green) | PASS |
| TC-REG-015 | Read one service returns its current version summary | api-verify REG test_services (4/4 checks) | PASS |
| TC-REG-016 | Read of an unknown service returns 404 | api-verify REG test_services (2/2 checks)<br>E2E [registry] An unknown service code is 404 REG-404-SERVICE-NOT-FOUND → PASSED (unchanged; registry regression re-run 20261003T102414Z green) | PASS |
| TC-REG-017 | A folder outside the package directory is never loaded | E2E [reg-rules] A folder outside the package directory is never loaded (sibling dir, symlink inside) → PASSED (4/4 checks) | PASS |
| TC-REG-018 | The registry exposes read operations only | E2E [reg-inprocess] The registry exposes read operations only → PASSED (14/14 checks) | PASS |
| TC-REG-019 | Service knowledge supplied apart from queries and document references | ✗ none | **NOT-EXERCISABLE** |
| TC-REG-020 | Every version carries its version number | E2E [reg-versions] Every version carries its version number → PASSED (8/8 checks) | PASS |
| TC-REG-021 | A higher version becomes current; the earlier stays stored | E2E [reg-versions] A higher version becomes current; the earlier stays stored → PASSED (3/3 checks) | PASS |
| TC-REG-022 | Same version number with changed content is rejected (never edited in place) | E2E [reg-versions] Same version number with changed content is rejected (never edited in place) → PASSED (3/3 checks) | PASS |
| TC-REG-023 | A lower, unstored version is rejected | E2E [reg-versions] A lower, unstored version is rejected → PASSED (8/8 checks) | PASS |
| TC-REG-024 | An unchanged package registers no new version | E2E [withdrawn] The folder put back: the service is available again with its stored ve → PASSED | PASS |
| TC-REG-025 | The current version is supplied to a Check | E2E [reg-versions] The current version is supplied to a Check → PASSED (2/2 checks) | PASS |
| TC-REG-026 | Any stored version resolves | API-INT-008 (the only public read of a pinned version) exposes versionNumber and requiredDocumentTypes only; serviceKnowledge, serviceDefinition and queries of version 2 are in-process only — partial evidence green (partial: the pinned version 2 resolves (versionNumber 2, its required types) after version 3 became current) | **NOT-EXERCISABLE** |
| TC-REG-027 | No stored version is deleted when a service is withdrawn | E2E [withdrawn] A package folder moved out: the service is WITHDRAWN, unlisted, read a → PASSED | PASS |
| TC-REG-028 | The whole service knowledge is supplied unaltered | ✗ none | **NOT-EXERCISABLE** |
| TC-REG-029 | Empty service knowledge is rejected | E2E [reg-rules] Empty service knowledge is rejected → PASSED (1/1 checks) | PASS |
| TC-REG-030 | Only the queries of the service definition are supplied, unaltered | the supplied queries (sqlText, connectionName) are read in-process by CHK's pipeline, which runs only for a confirmed Check and ends in a comparison-model call; no API returns them | **NOT-EXERCISABLE** |
| TC-REG-031 | Queries only through the query part; no SQL in the service knowledge | ✗ none | **NOT-EXERCISABLE** |
| TC-REG-032 | A query naming an unregistered connection is rejected | E2E [connection] A package whose query names an unregistered connection is REJECTED at  → PASSED | PASS |
| TC-REG-033 | A query using another parameter than the declared input is rejected | E2E [reg-rules] A query using another parameter than the declared input is rejected → PASSED (1/1 checks) | PASS |
| TC-REG-034 | A query using a substitution marker is rejected | E2E [reg-rules] A query using a substitution marker is rejected → PASSED (1/1 checks) | PASS |
| TC-REG-035 | A query that is not a single SELECT is rejected | E2E [reg-rules] A query that is not a single SELECT is rejected → PASSED (1/1 checks) | PASS |
| TC-REG-036 | A service definition declaring a check limit is rejected | E2E [reg-rules] A service definition declaring a check limit is rejected → PASSED (1/1 checks) | PASS |
| TC-REG-037 | Two queries with one name are rejected | E2E [reg-rules] Two queries with one name are rejected → PASSED (1/1 checks) | PASS |
| TC-REG-038 | One fetch mode recorded per version | E2E [reg-rules] One fetch mode and the required document types recorded per version → PASSED (3/3 checks) | PASS |
| TC-REG-039 | Required document types recorded per version | E2E [reg-rules] One fetch mode and the required document types recorded per version → PASSED (3/3 checks) | PASS |
| TC-REG-040 | An unknown fetch mode is rejected | E2E [reg-rules] An unknown fetch mode is rejected → PASSED (1/1 checks) | PASS |
| TC-REG-041 | Fetch mode path without a path column is rejected | E2E [reg-rules] Fetch mode path without a path column is rejected → PASSED (1/1 checks) | PASS |
| TC-REG-042 | Blob documents over an mcp connection are rejected | E2E [reg-rules] Blob documents over an mcp connection are rejected → PASSED (1/1 checks) | PASS |
| TC-REG-043 | A service definition declaring a file location is rejected | E2E [reg-rules] A service definition declaring a file location is rejected → PASSED (2/2 checks) | PASS |
| TC-REG-044 | An enabled approval API definition is recorded | the approvalApi text reaches only the Employee Decision path (ApprovalApiRegistry), exercised by an APPROVED decision on a COMPLETED Check — a comparison-model call; API-REG-002 exposes approvalEnabled only | **NOT-EXERCISABLE** |
| TC-REG-045 | Enabled approval without a definition is rejected | E2E [reg-rules] Enabled approval without a definition is rejected → PASSED (1/1 checks) | PASS |
| TC-REG-046 | The approval API definition reaches only the Employee Decision path | the supplied package and getApprovalApi are in-process values; the Employee Decision path needs a COMPLETED Check (a comparison-model call) | **NOT-EXERCISABLE** |
| TC-REG-047 | Disabled approval is reported to the Employee Decision path | getApprovalApi is reached only by a decision on a COMPLETED Check (a comparison-model call) | **NOT-EXERCISABLE** |
| TC-REG-048 | One connection defined once and shared by two packages | E2E [reg-rules] One connection defined once and shared by two packages → PASSED (7/7 checks) | PASS |
| TC-REG-049 | A connection name listed twice is refused | E2E [reg-activation] A connection name listed twice is refused → PASSED (5/5 checks) | PASS |
| TC-REG-050 | A connection is supplied by name | getConnection's settings (type, endpoint, queryTool, dialect, credentialReference) are in-process only; no HTTP operation reads a Connection | **NOT-EXERCISABLE** |
| TC-REG-051 | Activation registers the environment's connections | E2E [registry] The load report shows both connections ACTIVATED and every fixture pac → PASSED<br>E2E [connection] A package whose query names an unregistered connection is REJECTED at  → PASSED (unchanged; registry regression re-run 20261003T102414Z green) | PASS |
| TC-REG-052 | Changed connection settings update the connection, not the versions | E2E [reg-activation] Changed connection settings update the connection, not the versions → PASSED (10/10 checks) | PASS |
| TC-REG-053 | A connection no longer listed is removed | E2E [connection] The connection removed from the activation config: REMOVED; start -> 4 → PASSED | PASS |
| TC-REG-054 | An unknown connection type is refused | E2E [reg-rules] Activation refusals: unknown type, over-length name and endpoint; main-db ACTIVATED → PASSED (7/7 checks) | PASS |
| TC-REG-055 | A version naming a removed connection is not supplied | E2E [connection] The connection removed from the activation config: REMOVED; start -> 4 → PASSED | PASS |
| TC-REG-056 | Only the credential reference is stored | needs a read of every stored REG_CONNECTION field; the runner works only through the HTTP API and no API returns a Connection | **NOT-EXERCISABLE** |
| TC-REG-057 | A connection not declared read-only is refused | E2E [reg-activation] A connection not declared read-only is refused → PASSED (5/5 checks) | PASS |
| TC-REG-058 | The limited-to-views declaration is recorded | E2E [reg-activation] The limited-to-views declaration is recorded → PASSED (3/3 checks) | PASS |
| TC-REG-059 | The scholarship-request pilot package loads | MISSING_IMPLEMENTATION: the delivered pilot package folder scholarship-request (SVC-API step 6, REQ-REG-057/058) is not in this repository; authoring it needs the host's real policy text (POL-REG-014) and host query/columns, which must not be invented | **NOT-EXERCISABLE** |
| TC-REG-060 | Pilot numeric conditions are written as digits | ✗ none | **NOT-EXERCISABLE** |
| TC-REG-061 | No request data is stored in the registry | needs a read of every stored field of the REG tables after 100 Checks; the runner works only through the HTTP API (no direct DB access) | **NOT-EXERCISABLE** |
| TC-REG-062 | Supplied package content is read-only | immutability of the in-process value objects handed to a Check; nothing an HTTP client sends can attempt to change a supplied query | **NOT-EXERCISABLE** |
| TC-REG-063 | A failing pilot package is reported and never supplied | E2E [reg-rules] A failing pilot package is reported and never supplied → PASSED (5/5 checks) | PASS |
| TC-REG-064 | A package folder holding another file is rejected | E2E [reg-rules] A package folder holding another file is rejected → PASSED (3/3 checks) | PASS |
| TC-REG-072 | A required document type declared twice is rejected | E2E [reg-rules] A required document type declared twice is rejected → PASSED (2/2 checks) | PASS |
| TC-REG-073 | A case and space variant of a stored service code is the same service | E2E [registry] A service code is read trimmed and case-insensitively → PASSED (unchanged; registry regression re-run 20261003T102414Z green) | PASS |
| TC-REG-074 | Two folders whose codes differ only in case are both rejected | E2E [reg-rules] Two folders whose codes differ only in case are both rejected → PASSED (2/2 checks) | PASS |
| TC-REG-075 | Read one service matches the code case-insensitively | api-verify REG test_services (3/3 checks)<br>E2E [registry] A service code is read trimmed and case-insensitively → PASSED (unchanged; registry regression re-run 20261003T102414Z green) | PASS |
| TC-REG-076 | An invalid service code is rejected | E2E [reg-rules] An invalid service code is rejected (sub-cases a-g, one folder each) → PASSED (14/14 checks) | PASS |
| TC-REG-077 | A missing package directory withdraws nothing | E2E [reg-activation] A missing package directory withdraws nothing → PASSED (7/7 checks) | PASS |
| TC-REG-078 | An empty package directory withdraws nothing while services are available | E2E [reg-activation] An empty package directory withdraws nothing while services are available → PASSED (4/4 checks) | PASS |
| TC-REG-079 | Two instances starting together produce one complete load report | E2E [reg-rules] Two instances starting together produce one complete load report → PASSED (9/9 checks) | PASS |
| TC-REG-080 | An instance that cannot take the load lock serves the stored registry | E2E [reg-inprocess] An instance that cannot take the load lock serves the stored registry → PASSED (15/15 checks) | PASS |
| TC-REG-081 | A package file that changes while it is read is rejected | E2E [reg-versions] A package file that changes while it is read is rejected → PASSED (3/3 checks) | PASS |
| TC-REG-082 | Fetch mode blob without a content column is rejected | E2E [reg-rules] Fetch mode blob without a content column is rejected → PASSED (2/2 checks) | PASS |
| TC-REG-083 | Fetch mode path without a type column is rejected | E2E [reg-rules] Fetch mode path without a type column is rejected → PASSED (1/1 checks) | PASS |
| TC-REG-084 | A document source naming an undeclared query is rejected | E2E [reg-rules] A document source naming an undeclared query is rejected → PASSED (1/1 checks) | PASS |
| TC-REG-085 | Failing and succeeding items in one load run each get their own outcome | E2E [reg-rules] Failing and succeeding items in one load run each get their own outcome → PASSED (8/8 checks) | PASS |
| TC-REG-086 | A timeout in a service definition is rejected | E2E [reg-rules] A timeout in a service definition is rejected → PASSED (1/1 checks) | PASS |
| TC-REG-087 | A maximum file size in a service definition is rejected | E2E [reg-rules] A maximum file size in a service definition is rejected → PASSED (1/1 checks) | PASS |
| TC-REG-088 | Read of a withdrawn service reports available = false | E2E [withdrawn] A package folder moved out: the service is WITHDRAWN, unlisted, read a → PASSED | PASS |
| TC-REG-089 | A Check's pinned version is supplied after a newer version became current | fetchMode and the document-source fields of a pinned version are read only in-process by DOC's fetch inside a RUNNING Check (a comparison-model call), and start 2 (a restart) ends that Check INTERRUPTED before any fetch; API-INT-008 exposes versionNumber + requiredDocumentTypes only — partial evidence green (partial: the pinned version 2 resolves (versionNumber 2, its required types) after version 3 became current) | **NOT-EXERCISABLE** |
| TC-REG-090 | A present package folder with an unreadable file is rejected and the run continues | E2E [reg-versions] A present package folder with an unreadable file is rejected and the run continues → PASSED (4/4 checks) | PASS |
| TC-REG-091 | A re-activation that turns a blob version's connection into mcp is refused | E2E [reg-versions] A re-activation that turns a blob version's connection into mcp is refused → PASSED (7/7 checks) | PASS |
| TC-REG-092 | A removed connection is not re-registered as mcp while a blob version reads through it | E2E [reg-versions] A removed connection is not re-registered as mcp while a blob version reads through it → PASSED (7/7 checks) | PASS |
| TC-REG-093 | A folder whose name exceeds 200 characters is rejected; the run continues | E2E [reg-rules] A folder whose name exceeds 200 characters is rejected; the run continues → PASSED (6/6 checks) | PASS |
| TC-REG-094 | A connection name longer than 100 characters is refused | E2E [reg-rules] Activation refusals: unknown type, over-length name and endpoint; main-db ACTIVATED → PASSED (7/7 checks) | PASS |
| TC-REG-095 | A connection endpoint longer than 500 characters is refused | E2E [reg-rules] Activation refusals: unknown type, over-length name and endpoint; main-db ACTIVATED → PASSED (7/7 checks) | PASS |
| TC-REG-096 | An over-length package directory path is shortened on its Load Result row, never a load-run failure | E2E [reg-activation] An over-length package directory path is shortened on its Load Result row → PASSED (4/4 checks) | PASS |
| TC-REG-097 | An over-length invalid service code is shortened on its Load Result row | E2E [reg-rules] An over-length invalid service code is shortened on its Load Result row → PASSED (4/4 checks) | PASS |
| TC-REG-098 | A valid folder is rejected as a duplicate even when its twin fails another rule | E2E [reg-rules] A valid folder is rejected as a duplicate even when its twin fails another rule → PASSED (4/4 checks) | PASS |
| TC-REG-099 | A 100-character service code with single hyphens is accepted (boundary pass) | E2E [reg-rules] A 100-character service code with single hyphens is accepted (boundary pass) → PASSED (4/4 checks) | PASS |
| TC-REG-065 … TC-REG-071 | frontend track's TCs | — | OUT-OF-TRACK |

### NOT-EXERCISABLE (never counted as passed)

- **MISSING_IMPLEMENTATION (5)**: TC-REG-019, -028, -031, -060: MODEL-EVAL needs the `model-eval` profile runner with a known-result set, which was never delivered (unchanged). TC-REG-059: the delivered pilot package folder `scholarship-request` (SVC-API step 6, REQ-REG-057/058) does not exist in this repository. Authoring it would need the host's real policy text (POL-REG-014) and host query columns, which must not be invented.
- **In-process only, no public observation (11)**: TC-REG-002, -026, -030, -044, -046, -047, -050, -056, -061, -062, -089. The reason for each is in the table and in the run JSON. Partial evidence is green for TC-REG-026 / -089: a Check pinned to version 2 still resolves version 2's document types through API-INT-008 after version 3 became current. A full check needs a thread-level / in-process test (excluded by the no-JUnit policy), a direct DB read, or a model-completed Check.

### App defects

None found. Every TC block was met by the running app. Three intermediate runner failures were test-side: the required-type order (the API document fixes no order, and the TC lists two values, not a sequence), batch ordering, and the lock hold time. They were fixed in the runner, and no REG source changed.

### Package rows (STEP 1, re-evaluated)

| Unit | ids | PASS | row | not green |
|---|---|---|---|---|
| PORTS | 3 | 3 | **recorded** `--passed 3 --failed 0` | — |
| SVC-API | 89 | 73 | withheld | TC-REG-002, TC-REG-026, TC-REG-030, TC-REG-044, TC-REG-046, TC-REG-047, TC-REG-050, TC-REG-056, TC-REG-059, TC-REG-061, TC-REG-062, TC-REG-089, TC-REG-019, TC-REG-028, TC-REG-031, TC-REG-060 |
| API-SCENARIOS | 40 | 28 | withheld | TC-REG-002, TC-REG-026, TC-REG-030, TC-REG-044, TC-REG-046, TC-REG-047, TC-REG-050, TC-REG-056, TC-REG-059, TC-REG-061, TC-REG-062, TC-REG-089 |
| MODEL-EVAL | 4 | 0 | withheld | TC-REG-019, TC-REG-028, TC-REG-031, TC-REG-060 |
| RULE-SCENARIOS | 48 | 48 | **recorded** `--passed 48 --failed 0` | — |

ALIGN-BE / CORE / DATA-DOM* (no tests) stay accepted. `api_doc_gaps` rows unchanged (still OPEN).
