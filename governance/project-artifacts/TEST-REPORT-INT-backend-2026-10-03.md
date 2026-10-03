# TEST REPORT — INT backend v1 — 2026-10-03

Test phase close-out per `.claude/commands/INT/execute-backend-test.md` STEP 1/4/5/6 and `.claude/commands/orchestrate-module.md` STEP 4. **No JUnit** (user policy 2026-10-02): the evidence is the api-verify scripts (`governance/shared/backend/modules/*/test-api/`, run 2026-10-03, all five exit 0, `api_verify` v1 recorded PASS) and the E2E simulation run `20261003T092840Z` ([`E2E-SIMULATION-2026-10-03.json`](E2E-SIMULATION-2026-10-03.json)). An id counts as PASS only with green evidence tagged with that id; NOT-EXERCISABLE is never counted as passed.

## STEP 4 — Coverage cross-check (governed plan ↔ tests)

**Ratio: 48 / 62 required ids PASS (77.4 %)** — PASS 48 · FAIL 0 · GAP 12 · NOT-EXERCISABLE 2 · DEFERRED 0 · OUT-OF-TRACK 0.

- TC blocks of the P4 test plan: 58 — PASS 44 · FAIL 0 · GAP 12 · NOT-EXERCISABLE 2.
- AC ids listed in the built integration packages' `tests` (mapped to TCs through "Derived from"; an AC is PASS only when every TC derived from it is PASS): 4 — PASS 4 · GAP 0 · NOT-EXERCISABLE 0 · FAIL 0.
- Every id listed in the `tests` of the exec units, test units and built integration packages is a TC block of the test plan or one of the AC rows above (no id is dropped). No integration package is deferred (every `requires` is met), so no row is DEFERRED.

```
GOVERNED PLAN ↔ TESTS — INT v1
```

| TC/AC id | traces | scenario | evidence (api-verify check / E2E scenario) | result |
|---|---|---|---|---|
| TC-INT-001 | AC-INT-007 API-INT-001 | A start for a withdrawn service passes the Check Engine's refusal | api-verify INT test_checks (3/3 checks)<br>E2E [refusals] Start refusals: unknown service, incomplete start, unreadable body, un → PASSED<br>E2E [withdrawn] A package folder moved out: the service is WITHDRAWN, unlisted, read a → PASSED | PASS |
| TC-INT-002 | AC-INT-008 API-INT-002 | An upload of a type the service does not require passes Document Access's refusal | api-verify INT test_check_documents (2/2 checks)<br>E2E [refusals] Upload refusals and limits on a waiting manual Check → PASSED | PASS |
| TC-INT-003 | AC-INT-009 API-INT-004 | A second decision on a decided Check passes the Report Store's refusal | api-verify INT test_decision (4/4 checks)<br>E2E [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) → PASSED | PASS |
| TC-INT-004 | AC-INT-010 API-INT-003 | A confirmation of a running Check passes the Check Engine's refusal | api-verify INT test_upload_confirmation (4/4 checks) | PASS |
| TC-INT-005 | AC-INT-011 API-INT-003 | A non-numeric Check identifier is refused before any module is called | api-verify INT test_upload_confirmation (2/2 checks)<br>E2E [refusals] Non-numeric identifiers are refused by each module's own code → PASSED | PASS |
| TC-INT-006 | AC-INT-012 API-INT-004 | An unexpected failure is answered without internals | ✗ none | **NOT-EXERCISABLE** — api-verify INT API-INT-004 `INT-500`: an unexpected server failure (TC-INT-006: a Report Store database outage) cannot be produced through documented endpoints |
| TC-INT-007 | AC-INT-015 API-INT-002 | An upload for a running Check is refused and Document Access receives nothing | api-verify INT test_check_documents (2/2 checks) | PASS |
| TC-INT-008 | AC-INT-016 API-INT-002 | An upload for an unknown Check passes the Report Store's not-found refusal | api-verify INT test_check_documents (2/2 checks)<br>E2E [refusals] Upload refusals and limits on a waiting manual Check → PASSED | PASS |
| TC-INT-009 | AC-INT-018 API-INT-002 | An upload request above the request limit is refused | api-verify INT test_check_documents (2/2 checks)<br>E2E [refusals] Upload refusals and limits on a waiting manual Check → PASSED | PASS |
| TC-INT-010 | AC-INT-019 API-INT-002 | A file name shaped like a path is passed as text and opens no file | E2E [refusals] Upload refusals and limits on a waiting manual Check → PASSED | PASS |
| TC-INT-011 | AC-INT-023 API-INT-002 | Uploads alone never continue the Check | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-INT-012 | AC-INT-028 API-INT-002 API-INT-003 | Neither an upload nor a confirmation records a decision | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-INT-013 | AC-INT-031 API-INT-004 | A rejection never calls the Approval API | api-verify INT test_decision (6/6 checks)<br>E2E [approval] Approval API: a REJECTED decision never calls the Approval API → PASSED | PASS |
| TC-INT-014 | AC-INT-032 API-INT-004 | No call where the version does not enable the Approval API | E2E [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) → PASSED | PASS |
| TC-INT-015 | AC-INT-033 API-INT-001 | A completed compliant Check without a decision triggers no approval | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-INT-016 | AC-INT-038 API-INT-004 | An approval without a deciding employee is refused before any call | api-verify INT test_decision (4/4 checks)<br>E2E [approval] Approval API: refusals before any call on an approval-enabled service → PASSED | PASS |
| TC-INT-017 | AC-INT-039 API-INT-004 | An approval on a running Check is refused before any call | E2E [approval] Approval API: refusals before any call on an approval-enabled service → PASSED | PASS |
| TC-INT-018 | AC-INT-040 API-INT-004 | An approval on an already decided Check is refused before any call | E2E [approval] Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok - → PASSED | PASS |
| TC-INT-019 | AC-INT-041 API-INT-004 | A failed approval records nothing | E2E [approval] Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok - → PASSED | PASS |
| TC-INT-020 | AC-INT-042 API-INT-004 | A timed-out approval records nothing | E2E [approval] Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok - → PASSED | PASS |
| TC-INT-021 | AC-INT-044 API-INT-004 | A refusal after an executed approval is answered and logged | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-INT-022 | AC-INT-064 | No server-rendered report page exists | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-INT-023 | AC-INT-065 API-INT-002 API-INT-004 | Host Integration keeps nothing after answering | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-INT-024 | AC-INT-066 API-INT-004 | The only host connection is the Approval API call | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-INT-088 | AC-INT-068 API-INT-005 | An unknown Check is refused on the report read | api-verify INT test_check_reports (2/2 checks)<br>E2E [refusals] Confirmation of an unknown Check; reads of unknown Checks → PASSED | PASS |
| TC-INT-090 | AC-INT-070 API-INT-006 | A list of Checks without a service code passes the Report Store's refusal | api-verify INT test_check_reports (6/6 checks)<br>E2E [refusals] Lists of a request without their keys are refused → PASSED | PASS |
| TC-INT-094 | AC-INT-074 API-INT-008 | An unknown Check is refused on the required-document-types read | api-verify INT test_required_document_types (2/2 checks)<br>E2E [refusals] Confirmation of an unknown Check; reads of unknown Checks → PASSED | PASS |
| TC-INT-096 | AC-INT-075 API-INT-002 | An upload reaching Document Access after the Check ended passes Document Access's ended-Check refusal | E2E [race] DOC-409-CHECK-ENDED through INT's upload (the race of ADR-INT-025) → AMBIGUOUS | **NOT-EXERCISABLE** — api-verify INT API-INT-002 `DOC-409-CHECK-ENDED`: needs Document Access to hold the Check as ended while the Check Engine still reports AWAITING_DOCUMENTS (a race) — not producible deterministically; E2E race: NOT-DETERMINISTIC, not attempted: DOC answers DOC- |
| TC-INT-097 | AC-INT-076 API-INT-002 | An upload above the maximum uploads per Check passes Document Access's upload-limit refusal | api-verify INT test_check_documents (2/2 checks)<br>E2E [refusals] Maximum uploads per Check: 20 accepted, the 21st refused → PASSED | PASS |
| TC-INT-098 | AC-INT-040 API-INT-004 | Two simultaneous APPROVED decisions on one approval-enabled Check are serialised by the per-Check lock | E2E [limits] Two simultaneous APPROVED decisions: one 201 (one Approval API call),  → PASSED | PASS |
| TC-INT-099 | AC-INT-079 API-INT-004 | A decision code outside APPROVED and REJECTED is refused before any call | api-verify INT test_decision (4/4 checks) | PASS |
| TC-INT-025 | AC-INT-001 API-INT-001 | Start a Check of a path service | api-verify INT test_checks (4/4 checks)<br>E2E [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outs → PASSED | PASS |
| TC-INT-026 | AC-INT-002 API-INT-001 | Start a Check of a manual service | api-verify INT test_checks (4/4 checks)<br>E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-INT-027 | AC-INT-003 API-INT-001 | The start is answered before the report exists | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-INT-028 | AC-INT-004 API-INT-001 | Host identifiers are handed on unchanged | E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-INT-029 | AC-INT-005 API-INT-001 | An employee unknown to any directory is accepted | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-INT-030 | AC-INT-006 API-INT-001 | The accepted start names the address of the Check's read | api-verify INT test_checks (8/8 checks)<br>E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-INT-031 | AC-INT-013 API-INT-002 | An upload is handed to Document Access | api-verify INT test_check_documents (3/3 checks)<br>E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-INT-032 | AC-INT-014 API-INT-002 | The upload carries the Check's own service version | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-INT-033 | AC-INT-017 API-INT-002 | An oversized file is accepted with Document Access's notice | E2E [refusals] Upload refusals and limits on a waiting manual Check → PASSED | PASS |
| TC-INT-034 | AC-INT-022 API-INT-003 | Confirmed uploads continue the Check | api-verify INT test_upload_confirmation (3/3 checks)<br>E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-INT-035 | AC-INT-025 API-INT-004 | A rejection without the Approval API is recorded | api-verify INT test_decision (6/6 checks)<br>E2E [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) → PASSED | PASS |
| TC-INT-036 | AC-INT-026 API-INT-004 | The recorded decision is answered | api-verify INT test_decision (6/6 checks)<br>E2E [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) → PASSED | PASS |
| TC-INT-037 | AC-INT-029 API-INT-004 | The Approval API is called before the Report Store | E2E [approval] Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok - → PASSED | PASS |
| TC-INT-038 | AC-INT-030 API-INT-004 | An executed approval is recorded as executed | E2E [approval] Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok - → PASSED | PASS |
| TC-INT-039 | AC-INT-034 API-INT-004 | The approval definition of the Check's own version is used | E2E [approval] Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok - → PASSED | PASS |
| TC-INT-040 | AC-INT-035 API-INT-004 | The request number is placed in the path as one encoded value | E2E [approval] Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok - → PASSED | PASS |
| TC-INT-041 | AC-INT-036 API-INT-004 | The call carries the Check and the deciding employee | E2E [approval] Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok - → PASSED | PASS |
| TC-INT-042 | AC-INT-037 API-INT-004 | One call, never retried | E2E [approval] Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok - → PASSED | PASS |
| TC-INT-043 | AC-INT-043 API-INT-004 | A retry after a failed approval is a new decision request | E2E [approval] Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok - → PASSED | PASS |
| TC-INT-087 | AC-INT-067 API-INT-005 | A Check and its report are read through Host Integration | api-verify INT test_check_reports (8/8 checks)<br>E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-INT-089 | AC-INT-069 API-INT-006 | The Checks of a request are listed through Host Integration | api-verify INT test_check_reports (9/9 checks)<br>E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-INT-091 | AC-INT-071 API-INT-007 | The uploaded documents are listed without content | api-verify INT test_check_documents (3/3 checks)<br>E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-INT-092 | AC-INT-072 API-INT-007 | A Check without uploads lists none | api-verify INT test_check_documents (3/3 checks)<br>E2E [manual] Missing document: confirm without an upload -> TRANSCRIPT MISSING, NOT → PASSED | PASS |
| TC-INT-095 | AC-INT-071 API-INT-007 | The uploaded-documents read relays Document Access's listing operation unchanged | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| TC-INT-093 | AC-INT-073 API-INT-008 | The required document types are those of the Check's own version | api-verify INT test_required_document_types (6/6 checks)<br>E2E [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT  → PASSED | PASS |
| TC-INT-100 | AC-INT-077 API-INT-002 API-INT-007 | A second upload of the same document type is handed over separately and both are listed | E2E [refusals] Upload refusals and limits on a waiting manual Check → PASSED | PASS |
| TC-INT-044 | XM-INT-001 API-INT-004 | The decision path answers in the standard form when the approval definition cannot be read | ✗ none | **GAP** — no api-verify check or E2E scenario exercises it |
| AC-INT-029 | AC-INT-029 | AC — via TC-INT-037 | via TC-INT-037 | PASS |
| AC-INT-032 | AC-INT-032 | AC — via TC-INT-014 | via TC-INT-014 | PASS |
| AC-INT-034 | AC-INT-034 | AC — via TC-INT-039 | via TC-INT-039 | PASS |
| AC-INT-073 | AC-INT-073 | AC — via TC-INT-093 | via TC-INT-093 | PASS |

## STEP 5 — Failures and skips, classified

| Code | Count | Why |
|---|---|---|
| `TEST_STRUCTURE_FAILURE` | 13 | no api-verify check or E2E scenario exercises the id (the test suites lack it — a test-side gap, not an app defect); or the case is reachable only by a thread-level / in-process test, which the no-JUnit policy excludes |
| `ENVIRONMENT_FAILURE` | 1 | the case needs an environment fault (a Report Store / database outage) that neither the api-verify script nor the local E2E setup can produce through the API |

No FAIL: no api-verify check and no E2E scenario carrying an id of this module failed. No app defect was found; no source was changed.

- `TEST_STRUCTURE_FAILURE` (13): TC-INT-011 (GAP), TC-INT-012 (GAP), TC-INT-015 (GAP), TC-INT-021 (GAP), TC-INT-022 (GAP), TC-INT-023 (GAP), TC-INT-024 (GAP), TC-INT-096 (NOT-EXERCISABLE), TC-INT-027 (GAP), TC-INT-029 (GAP), TC-INT-032 (GAP), TC-INT-095 (GAP), TC-INT-044 (GAP)
- `ENVIRONMENT_FAILURE` (1): TC-INT-006 (NOT-EXERCISABLE)

## STEP 1 — Package rows

A unit's `package` row is recorded only when every id it lists has PASS evidence (then `--failed 0` → accepted). A unit with any GAP / NOT-EXERCISABLE id is **not recorded** — recording `passed=k failed=0` would falsely mark it accepted.

| Unit | kind | ids (source) | PASS | FAIL | row | not green |
|---|---|---|---|---|---|---|
| PORTS | exec_units | 6 (manifest) | 5 | 0 | **not accepted** — GAPs | TC-INT-095 |
| SVC-API-COMMAND | exec_units | 43 (manifest) | 31 | 0 | **not accepted** — GAPs | TC-INT-006, TC-INT-011, TC-INT-012, TC-INT-015, TC-INT-021, TC-INT-022, TC-INT-023, TC-INT-024, TC-INT-096, TC-INT-027, TC-INT-029, TC-INT-032 |
| SVC-API-QUERY | exec_units | 8 (manifest) | 8 | 0 | recorded `--passed 8 --failed 0` | — |
| API-SCENARIOS | test_units | 26 (manifest) | 22 | 0 | **not accepted** — GAPs | TC-INT-027, TC-INT-029, TC-INT-032, TC-INT-095 |
| INT-XM | test_units | 1 (manifest) | 0 | 0 | **not accepted** — GAPs | TC-INT-044 |
| RULE-SCENARIOS | test_units | 31 (manifest) | 22 | 0 | **not accepted** — GAPs | TC-INT-006, TC-INT-011, TC-INT-012, TC-INT-015, TC-INT-021, TC-INT-022, TC-INT-023, TC-INT-024, TC-INT-096 |
| XM-INT-001 | integration | 5 (manifest) | 4 | 0 | **not accepted** — GAPs | TC-INT-044 |

ALIGN-BE / CORE / DATA-DOM* list no tests and were accepted (0/0 after a clean build) during execution; unchanged.

## Open gap rows of the module (`execution-state.json`)

- `api_doc_gaps` · **POST /api/v1/checks/{checkId}/documents** — OPEN — CheckStatus/EmployeeDecision named by CORE R1 / ADR-INT-013 are not published in chk/rpt .contract, so INT compares the owners' codes AWAITING_DOCUMENTS (RULE-INT-001) and COMPLETED/APPROVED/REJECTED (RULE-INT-002/003, decision endpoint) as strings — pending spec clarification
- `api_doc_gaps` · **POST /api/v1/checks/{checkId}/decision** — OPEN — PORTS/error-catalog silent on the INT-502 {status} value for an unreachable host and on a malformed Approval API definition; PORTS' prescribed buildAndExpand(...).encode() contradicts AC-INT-035
- `api_doc_gaps` · **POST /api/v1/checks** — OPEN — api-spec-int.yaml request schemas declare required/maxLength/enum/additionalProperties that the SVC-API-COMMAND unit assigns to the owners; no INT code for an over-length value; unsupported Content-Type now answers INT-400-REQUEST-INVALID; a 415 code is not in the catalog — pending spec clarification
- `api_doc_gaps` · **POST /api/v1/checks/{checkId}/documents — DOC-422-FETCH-MODE-NOT-MANUAL unreachable** — OPEN — MAPPING_GAP pending spec clarification

## Notes

- Evidence is taken from the tags the api-verify checks and the E2E scenarios carry. The orchestrate STEP 4.4 second-agent coverage debate and the STEP 4.5 fixing agent were not run in this pass (scope: close the test phase honestly with the available evidence); the GAP set above is the input for them.
- Model used for the comparison in the counted E2E run: `gemini-3.6-flash` (Gemini free tier, model calls: comparison 11, reading 1). The first attempt of the day (run on `gemini-3.8-flash`) hit that model's daily quota after 4 calls; it is kept as history (`E2E-SIMULATION-2026-10-03-quota-run.json`) and not counted.
