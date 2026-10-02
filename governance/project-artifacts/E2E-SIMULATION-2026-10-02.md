# E2E simulation of the aias backend — 2026-10-02

**Result (final full run `20261002T114403Z`): 36 of 38 scenarios PASSED (392 of 393 assertions), 1 FAILED
because of the environment (system sleep, see below — the scenario then PASSED 30/30 when rerun alone), 1
AMBIGUOUS (not deterministically reproducible, by design of the runner). No app defect was found and no app code
was changed.**

This is the second pass of the day. The first pass (run `20261002T100954Z`, 23/23 PASSED) listed open items; this pass
closes them with seven new groups that restart the app in other **modes** (profile override
`local/e2e-override.properties`, fixture package folders parked out of the package directory), confirms the
60-minute upload-window expiry live, and records one contract observation for the factory.

The suite drives the real HTTP API of the running local app (profile `local`, port 7271, Oracle `LOAN_SYS`, MCP
connection `local-oracle`, jdbc connection `local-jdbc`, Gemini free tier, approval stub on 7290). It has no JUnit
tests: `scripts/e2e/simulate.py` is the runner and `scripts/e2e/setup_fixtures.py` sets up the fixtures and modes.
See `scripts/e2e/README.md`.

- Raw results: [`E2E-SIMULATION-2026-10-02.json`](E2E-SIMULATION-2026-10-02.json) (every assertion, the app restarts)
  and [`E2E-SIMULATION-2026-10-02-run.md`](E2E-SIMULATION-2026-10-02-run.md) (the runner's summary); the approval
  group rerun: [`E2E-SIMULATION-2026-10-02-approval-rerun.json`](E2E-SIMULATION-2026-10-02-approval-rerun.json).
- Model calls in the final run: **11 comparison** (`gemini-3.6-flash`) and **1 reading** (`gemini-3.5-flash-lite`).
  The new groups made **4** of them (all in `limits`); every other new scenario needs no model.
- All data is synthetic. The runner only used the HTTP API and wrote nothing to the database directly.

## Upload-window expiry (REQ-CHK-060) — closed

**The real 60-minute window, live.** The six Checks the first pass left waiting were never restarted. Each ended
`FAILED / UPLOAD_WINDOW_EXPIRED` from the scheduled deadline check, on its deadline plus at most the 30 s default
interval, with no model call. The Active Check returns 404, and the INT and DOC upload lists are empty (DOC's
end-of-Check notice deleted 3 uploads of 47 and 20 of 48).

| checkId | started | ended | outcome |
|---|---|---|---|
| 35 | 14:09:36 | 15:09:47 | FAILED / UPLOAD_WINDOW_EXPIRED |
| 44 | 14:11:20 | 15:11:48 | FAILED / UPLOAD_WINDOW_EXPIRED |
| 45 | 14:11:20 | 15:11:48 | FAILED / UPLOAD_WINDOW_EXPIRED |
| 47 | 14:11:40 | 15:11:48 | FAILED / UPLOAD_WINDOW_EXPIRED (3 uploads deleted) |
| 48 | 14:11:41 | 15:11:48 | FAILED / UPLOAD_WINDOW_EXPIRED (20 uploads deleted) |
| 49 | 14:11:41 | 15:11:48 | FAILED / UPLOAD_WINDOW_EXPIRED |

The brief expected these six to expire after a restart with the short window. By design they would not: a restart
ends every unfinished Check `FAILED / INTERRUPTED` first (start-up recovery, REQ-CHK-055 / REQ-CHK-081). A
Check's deadline is also fixed at its start (`deadlineAt = startedAt + upload-window`), so a shorter window never
shortens an existing Check. The first restart was therefore held until 15:12, after they had expired, to observe
the real window.

**The short window (group `expiry`).** In mode `upload-window=PT1M` and `deadline-check-interval=PT5S`, the deadline
is start + 60.0 s and the Check is still waiting at 30 s (TC-CHK-041). It ends `FAILED / UPLOAD_WINDOW_EXPIRED`
60.9 s after the start. After that the Active Check returns 404, the upload is deleted (INT and DOC), and a late
upload and a late confirmation each return 409.

## Results per group (final full run)

The groups of the first pass (registry, manual, image, path, blob, decisions, approval, refusals, lifecycle) are
unchanged and were rerun. Their results match the first pass, except for the one approval scenario described under
"Environment problems".

| Group | Passed | Failed | Ambiguous | Model calls | What was proven (new groups) |
|---|---|---|---|---|---|
| registry · manual · image · path · blob · decisions · refusals · lifecycle | 20 | 0 | 0 | 7 + 1 reading | as in the first pass (below) |
| approval | 2 | 1 | 0 | 1 (+ reuses image) | the 500 / slow / ok scenario FAILED only because the machine slept during it; rerun alone (run `20261002T121130Z`) it PASSED 30/30 |
| `interrupted` | 1 | 0 | 0 | 0 | A waiting Check with one upload, then a restart: `FAILED / INTERRUPTED` ("interrupted by a restart of the service"); the recovery log line counts it; the Active Check returns 404; the upload is deleted (INT and DOC); a late upload returns 409 INT-409 (TC-CHK-037, TC-CHK-052) |
| `expiry` | 1 | 0 | 0 | 0 | see above (TC-CHK-040, TC-CHK-041, TC-CHK-083, TC-CHK-028, TC-DOC-052) |
| `notpermitted` | 2 | 0 | 0 | 0 | Mode `data-class=REAL`, both models FREE. A path Check ends `FAILED / MODEL_NOT_PERMITTED` ("nothing was sent"); the app log shows the comparison gate's refusal and **no** "calling model" line (TC-CHK-046, TC-CHK-047). A PNG upload: the reading step handles it and the reading model is never called; the Check ends MODEL_NOT_PERMITTED. A FAILED report keeps no document outcomes, so the PNG's outcome is asserted in `limits` (TC-DOC-064) |
| `limits` | 5 | 0 | 0 | 4 | Mode `max-file-size=1KB`, `max-rows=1`, `data-class=REAL`, comparison tier `APPROVED` (local test only; the data is synthetic), reading tier FREE. **Path:** `transcripts/big.pdf` is UNREADABLE / TOO_LARGE ("is 4531 bytes, larger than the maximum file size of 1024 bytes; its content was not read"), and the log has no "read" line for it (TC-DOC-025, TC-DOC-018). **Blob:** the 2000-byte content column is TOO_LARGE, measured before streaming (TC-DOC-024). **Scanned document:** on a `demo-approval` Check, the PNG is UNREADABLE / MODEL_NOT_PERMITTED ("not sent") and the PDF beside it is READ, with no reading-model call (TC-DOC-065, REQ-DOC-058). **max-rows:** `rows_probe` (2 rows) is unread with "more than 1 rows"; `request_echo` (exactly 1 row) is read; NEEDS_MANUAL_REVIEW (TC-CHK-031, TC-CHK-032). **Concurrent approvals** on that COMPLETED approval-enabled Check (stub `delay` 1 s): exactly one 201 (`approvalApiExecuted=true`) and one 409 `RPT-409-DECISION-ALREADY-RECORDED`; the second request waited 0.83 s on the per-Check lock; the stub received exactly ONE call; the report keeps the first decision (TC-INT-098) |
| `withdrawn` | 2 | 0 | 0 | 0 | `demo-blob` folder parked → load report WITHDRAWN (version 1 kept); not in `GET /services`; read by code gives `available:false`; start → 422 `CHK-422-SERVICE-NOT-AVAILABLE` with no Check created (TC-REG-010, TC-REG-088, TC-REG-012, TC-REG-027, TC-CHK-002, TC-INT-001). Folder put back → UNCHANGED, listed and available again, version 1, fetch blob (TC-REG-011, TC-REG-024) |
| `connection` | 3 | 0 | 0 | 0 | With the extra connection `local-extra` activated: `demo-noconn` (query over `ghost-db`) is REJECTED at load (RULE-REG-005), never registered (404), and the load continues; `demo-conn` (query over `local-extra`) loads and starts (TC-REG-032, TC-REG-007, TC-REG-051). With the connection dropped from the activation config: `local-extra` REMOVED; `demo-conn`'s folder REJECTED but the service stays available (a rejected folder withdraws nothing); start → 422 `CHK-422-CONNECTION-NOT-ACTIVATED` naming `local-extra` (TC-REG-053, TC-REG-055, TC-CHK-004). Back to normal → `demo-conn` WITHDRAWN |
| `race` | 0 | 0 | 1 | 0 | `DOC-409-CHECK-ENDED` through INT (TC-INT-096): **AMBIGUOUS / NOT-DETERMINISTIC**. Every ending path commits the RPT status first and records DOC's Ended-Check marker after that commit, and the multipart body is parsed before `UploadService` reads the status. Nothing an HTTP client controls can hold a request between INT's status read and DOC's step 1a, so only a thread-level test can reproduce it. Not faked |
| **Total** | **36** | **1** | **1** | **11 + 1** | |

### First-pass groups (unchanged scenarios, results as on the first pass)

| Group | Passed | Failed | Skipped | Model calls | What was proven |
|---|---|---|---|---|---|
| registry | 4 | 0 | 0 | 0 | Lists 4 fixture services (manual / manual+approval / path / blob) with no SQL or connection detail. Load report: `local-oracle` and `local-jdbc` ACTIVATED; `demo-path` and `demo-blob` REGISTERED; the others UNCHANGED; nothing REJECTED. Unknown code returns 404 `REG-404-SERVICE-NOT-FOUND`. `DEMO-Manual` and `"  Demo-Path "` are read trimmed and case-insensitively |
| manual | 3 | 0 | 0 | 3 | Happy path: AWAITING_DOCUMENTS → required types `[TRANSCRIPT]` → PDF upload 201 → listed without content → confirm 202 RUNNING → COMPLETED/COMPLIANT. The report has findings, TRANSCRIPT READ, version 1, model id, the host ids unchanged and no unread query. The RPT read and both lists of the request include the Check. A second confirmation returns 409 `CHK-409-CHECK-NOT-AWAITING-DOCUMENTS` and a late upload returns 409 `INT-409-CHECK-NOT-AWAITING-DOCUMENTS`. A failing transcript gives NOT_COMPLIANT. Confirming with no upload gives TRANSCRIPT NOT_SATISFIED with evidence `MISSING`, the document MISSING and NOT_COMPLIANT |
| image | 1 | 0 | 0 | 1 + 1 reading | The PNG (no text layer) is READ by the reading model and the result is COMPLIANT |
| path | 1 | 0 | 0 | 1 | The start answers RUNNING directly and an upload is refused with 409 `INT-409-CHECK-NOT-AWAITING-DOCUMENTS`. TRANSCRIPT is READ from `local/storage-root/transcripts/pass.pdf`. ID_CARD (`../outside.pdf`) is UNREADABLE / `OUTSIDE_STORAGE_ROOT` ("resolves outside the storage root; it was not opened"), so its finding is UNDETERMINED and the result NEEDS_MANUAL_REVIEW. The outside file's content appears nowhere |
| blob | 1 | 0 | 0 | 1 | TRANSCRIPT is READ (sourceMode `blob`) from `TO_BLOB(HEXTORAW(...))` over the jdbc connection and the result is COMPLIANT |
| decisions | 2 | 0 | 0 | reuses manual | No `decidedBy` and code `MAYBE` each return 400 `RPT-400-DECISION-INCOMPLETE`. APPROVED returns 201 with `approvalApiExecuted=false`. A second decision returns 409 `RPT-409-DECISION-ALREADY-RECORDED`. The report and the list show the decision. The decision agreement (v1, COMPLIANT, APPROVED) goes up by 1, and with no serviceCode it returns 400. A decision on an AWAITING Check returns 409 `RPT-409-CHECK-NOT-COMPLETED` and on an unknown Check 404 |
| approval | 3 | 0 | 0 | 1 (+ reuses image) | Stub returns 500 → 502 `INT-502-APPROVAL-API-FAILED`, one call, nothing recorded. Stub is slow → 504 `INT-504-APPROVAL-API-TIMED-OUT` after about 2 s, nothing recorded. Stub returns ok → 201 `approvalApiExecuted=true`; the stub saw `POST /requests/E2E%2F20261002T100954Z%20APR1/approve` with body `{"checkId":46,"decidedBy":"E2E-EMP-0001"}`. A second APPROVED returns 409 with no stub call. REJECTED makes no stub call. On a non-completed Check, APPROVED gets 409 and a missing `decidedBy` gets 400, both before any call |
| refusals | 6 | 0 | 0 | 0 | Start: 422 `CHK-422-SERVICE-NOT-AVAILABLE`; missing or blank employeeId → 400 `CHK-400-START-INCOMPLETE`; bad JSON or `text/plain` → 400 `INT-400-REQUEST-INVALID`. Uploads: wrong type → 422 `DOC-422-DOCUMENT-TYPE-NOT-OF-SERVICE`; empty file → 400 `DOC-400-INCOMPLETE-UPLOAD`; no documentType part → 400; a path-shaped file name is kept as text; a 10 MB+ file → 201 `oversized=true` with a notice; 13 MB request → 413 `INT-413-UPLOAD-TOO-LARGE`; unknown Check → 404 `RPT-404-CHECK-NOT-FOUND`; 20 uploads accepted and the 21st → 422 `DOC-422-UPLOAD-LIMIT-REACHED`. Unknown ids → 404 on confirm, report, RPT read, required types and active-check. Non-numeric ids → each module's own 400 code (INT ×6, RPT, CHK, DOC ×2). Lists without keys → 400 `RPT-400-REQUEST-KEYS-MISSING` (RPT and INT) |
| lifecycle | 2 | 0 | 0 | reuses manual | The Active Check of a waiting Check has exactly `{checkId, checkStatus, deadlineAt}`, AWAITING_DOCUMENTS, with deadline = start + 60 min. After the end, active-check returns 404 `CHK-404-ACTIVE-CHECK-NOT-FOUND` and the uploads are deleted (INT and DOC lists are empty, REQ-DOC-054) |
| **Total** | **23** | **0** | **0** | **7 + 1** | |

TC ids realised by the first-pass groups (from the P4 backend test plans), as tagged per scenario in the JSON:
REG 014/005/001/008/051/006/016/075/073 ·
INT 001/002/003/004/005/007/008/009/010/013/014/016/017/018/019/020/025/026/028/030/031/033/034/035/036/037/038/039/040/041/042/043/087/088/089/090/091/092/093/094/097/100 ·
CHK 001/003/009/010/024/025/027/028/038/056/059/071/073/075/076/083/085/086 ·
DOC 005/012/013/014/015/019/027/034/035/037/038/040/042/045/052/056/060/071/072 ·
RPT 011/014/028/030/033/035/039/040/041/042/045/047/049.

Added by the new groups: REG 007/010/011/012/024/027/032/053/055/088 · INT 098 (and 001 again, for a withdrawn service) ·
CHK 002/004/028/031/032/037/040/041/046/047/052 · DOC 018/024/025/052/064/065. TC-INT-096 is AMBIGUOUS.

## App defects found and fixed

**None in either pass.** Every new scenario matched the requirements on its first run, and no app code was changed.

## Environment problems and runner fixes (second pass)

- **System sleep (the one FAILED scenario).** In the final run, approval Check 115 was confirmed at 15:47:54. The
  machine then entered system sleep from 15:48:08 to 16:03:08 (`pmset -g log`: "Entering Sleep state due to
  'Maintenance Sleep'", "DarkWake ... 16:03:08"). The app and the runner were frozen together. On wake the runner's
  wall-clock wait (200 s) had already passed, so it failed the step. The next group restarted the app 8 s later, and
  Check 115 ended `FAILED / INTERRUPTED` before the app's deadline check could run, so it was never TIMED_OUT. This is
  not an app defect: run alone (`20261002T121130Z`) the same scenario PASSED 30/30 in 34 s. **Runner fix:** the run
  now holds a `caffeinate -i -s` assertion. This works on AC power; with the lid closed on battery, macOS still sleeps.
  A later full-run attempt was cut short by the same kind of sleep (16:33–17:05), so the run above stays the final
  one. The runner also keeps every app log of a run (`logs/e2e-<runId>/`), because each restart replaces
  `logs/aias-local.log` (the log of the 15:48 window was lost that way).
- **Per-minute vs per-day quota (runner defect, fixed).** The runner treated any 429 as an exhausted daily quota and
  skipped every later model scenario. A free-tier 429 can be per minute (`...PerMinutePerProjectPerModel`, 5 RPM),
  which is transient. `judge_model_failure` now gives that case `SKIPPED-RATE` with a 65 s cool-down. A 503 "high
  demand" gives `SKIPPED-MODEL` with a 30 s cool-down. A model scenario that ends in either is retried once. Only
  `...PerDay...` stops model scenarios for the rest of the run. Model spacing went from 13 s to 15 s, because the
  provider client's own retries of a 503 also count against the per-minute limit.
- **Free-tier daily quotas.** `gemini-3.5-flash` hit its 20/day limit early in this pass, and `gemini-3.7-flash` hit
  its limit after a series of 503 "high demand" answers. The local profile's comparison model was moved between free
  models, and the final run used `gemini-3.6-flash`. At the end the local profile names `gemini-3.8-flash`, whose
  quota resets daily (07:00 UTC). These are environment limits, not app outcomes: each one surfaced correctly as
  `FAILED / MODEL_UNAVAILABLE` with the provider's detail.

First-pass runner fixes (unchanged): the 413 scenario streams large bodies and reads the early response, and the
quota detector no longer matches `rate` inside `generate`.

## Fixture approach

- **Blob: hex literal, no table.** The `demo-blob` source query is
  `SELECT CAST('TRANSCRIPT' AS VARCHAR2(100)) AS DOC_TYPE, TO_BLOB(HEXTORAW('<1538 hex chars>')) AS CONTENT FROM DUAL WHERE :requestId IS NOT NULL`.
  It holds a 769-byte synthetic PDF. Oracle accepted it and ojdbc `getBlob` read the temporary LOB.
  No `AIAS_E2E_HOST_DOCS` table was created.
- **Path:** one Check covers both the normal read and the traversal attempt. The source query returns
  two rows: `TRANSCRIPT → transcripts/pass.pdf` and `ID_CARD → ../outside.pdf`. The string literals use
  `CAST(... AS VARCHAR2)` because a `UNION ALL` of CHAR literals would pad them with blanks.
- **Local profile changes** (gitignored, in a marked block):
  - storage root `local/storage-root`;
  - jdbc connection `local-jdbc`;
  - `aias.integration.approval.timeout=PT2S`;
  - `aias.integration.upload.request-limit=12MB` (it must be ≥ the 10 MB max file size, which `IntegrationLimitsCheck` enforces).

  `LOCAL_JDBC_CREDENTIAL` is in `local/secrets.properties`. In the same block, `spring.config.import` adds the
  optional `local/e2e-override.properties`.
- **Second pass:** a 4.5 KB `transcripts/big.pdf`; packages `demo-path-big`, `demo-blob-big` (2000 bytes from
  `TO_BLOB(HEXTORAW(RPAD('25504446', 4000, '30')))`), `demo-rows` (`SELECT LEVEL AS N FROM DUAL WHERE :requestId IS NOT
  NULL CONNECT BY LEVEL <= 2`); parked in `local/e2e-parked/`: `demo-conn` (query over `local-extra`) and
  `demo-noconn` (query over `ghost-db`). Spring does not merge an indexed list across property sources, so the
  `connection` mode's override lists all three connections. The approval stub gained a `delay` mode (200 after
  `seconds`, default 1).

## Contract observation recorded for the factory

`DOC-422-FETCH-MODE-NOT-MANUAL` is listed under `uploadDocument` 422 in `api-spec-int.yaml`, but it cannot be
reached through INT. The reasons:
- `UploadService.upload` reads the Check, and `UploadGuard.requireUploadAllowed` refuses every status except
  AWAITING_DOCUMENTS with `INT-409-CHECK-NOT-AWAITING-DOCUMENTS` before Document Access is called.
- `CheckStartService` gives AWAITING_DOCUMENTS only to a `manual` version; a path or blob Check starts RUNNING.
- INT hands over the Check's own service code and version, and a stored version's fetch mode never changes.

The path group confirms it live: an upload on a path Check answers INT-409. This is recorded as INT `api_doc_gaps`
row `POST /api/v1/checks/{checkId}/documents — DOC-422-FETCH-MODE-NOT-MANUAL unreachable` with resolution
`OPEN — MAPPING_GAP pending spec clarification`. That key held no earlier row; the endpoint's existing OPEN row
about the owners' status codes is unchanged. No code change.

## Still open / not exercised

| Item | Why | Reference |
|---|---|---|
| Upload reaching DOC after the Check ended (`DOC-409-CHECK-ENDED`) | AMBIGUOUS / NOT-DETERMINISTIC through the API (see `race`); needs a thread-level test | TC-INT-096 |
| A RUNNING Check ended TIMED_OUT by the deadline check | not run on purpose: it would spend a model call on a Check made to hang. The ending order is covered by the plan's own tests | REQ-CHK-051 |
| Model nondeterminism | the model splits the single knowledge "Condition" into 1 to 3 findings from run to run, so the runner never asserts an exact finding count. The COMPLIANT / NOT_COMPLIANT assertions depend on the model reading the synthetic values correctly | TC-CHK-071 |
| Approval executed at the host after a timeout | in slow mode the stub still answers 200 after 30 s, so the host may have approved although aias recorded nothing. This is the designed behaviour (nothing recorded on a timeout); a real host needs to be idempotent | REQ-INT-037 |

## Surviving records (synthetic, kept until the retention purge)

The final run's Checks are listed with their creating scenario in
[`E2E-SIMULATION-2026-10-02-run.md`](E2E-SIMULATION-2026-10-02-run.md) (checkIds 107–127). No Check of the final run
is left waiting: the restart groups ended the waiting ones `FAILED / INTERRUPTED`. The first pass's Checks 38–49 are as
reported then, except that 35, 44, 45, 47, 48 and 49 have now ended `FAILED / UPLOAD_WINDOW_EXPIRED` (table above).

At the end the app runs in the **normal** local mode: no override file, `demo-conn` / `demo-noconn` parked, the
comparison model `gemini-3.8-flash`. The pid is in `logs/aias-local.pid`. The approval stub runs in mode `ok`.
