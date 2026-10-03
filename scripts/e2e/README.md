# scripts/e2e — end-to-end simulation of the aias backend

A Python scenario runner (no JUnit) that drives the **real HTTP API of the running local app** through
the whole product flow and asserts the HTTP status, the ProblemDetail `code` and the key body fields at
every step:

registry → start a Check → upload → confirm → pipeline (service query over MCP, document fetch,
comparison model) → report → list of the request → Employee Decision → host Approval API (stub) →
end of the Check (Active Check gone, uploads deleted).

| File | What |
|---|---|
| `setup_fixtures.py` | Idempotent setup of the **synthetic** local fixtures, then app restart when anything changed |
| `simulate.py` | The scenario runner (stdlib only); `--list` prints every scenario with the P4 TC ids it realises |
| `synth.py` | Generators of synthetic documents: PDFs (text layer, exact size, password-protected, scanned, damaged), PNG, .xlsx, .xls (BIFF8), .docx |
| `model_tap.py` | DOC groups: recording pass-through on 127.0.0.1:7292 in front of the model endpoint (model, messages/parts, tools — never headers or responses) → `logs/e2e-model-tap.jsonl` |
| `mcp_tap.py` | DOC groups: recording wrapper of the Oracle MCP server (SQL text, binds, row count / error) → `logs/e2e-mcp-tap.jsonl` |
| `model_stub.py` | RPT / INT groups: a SCRIPTED local stand-in for the comparison model's provider on 127.0.0.1:7293 (OpenAI-compatible `/chat/completions`). A Check's document carries `E2ESTUB_<ID>`; the stub answers that script's findings, holds its answer (`hold`) or answers an error status (`logs/e2e-model-stub-scripts.json`, written by the runner). Records model / script / status only → `logs/e2e-model-stub.jsonl`. No free-tier quota is used |
| `hprof_holders.py` | TC-INT-023: attributes every copy of a marker found in a live-object heap dump to the chain of objects holding it |

## What it simulates

| Group | Model calls | Scenarios |
|---|---|---|
| `registry` | 0 | the four fixture services and their fetch modes; load report (connections ACTIVATED, packages loaded); unknown code 404; trimmed / case-insensitive read |
| `manual` | 3 | happy path to COMPLETED/COMPLIANT with a full report and the list of the request; NOT_COMPLIANT transcript; confirmation with no upload → TRANSCRIPT MISSING |
| `image` | 1 + 1 reading | PNG transcript read by the document-reading model |
| `path` | 1 | `path` service starts RUNNING; TRANSCRIPT read under the storage root, `../outside.pdf` refused OUTSIDE_STORAGE_ROOT in the same Check |
| `blob` | 1 | `blob` service: TRANSCRIPT read from a BLOB column over the jdbc connection |
| `decisions` | reuses `manual` | incomplete / unknown-code / first / second decision, decision agreement, decision on non-completed and unknown Checks |
| `approval` | 1 (+ reuses `image`) | stub 500 → INT-502, slow → INT-504 (nothing recorded), ok → executed with the encoded request number, second → 409; REJECTED never calls the stub; refusals before any call |
| `refusals` | 0 | start refusals; upload refusals, oversized file, 413, max uploads (21st), unknown Checks, non-numeric ids per module, list keys missing |
| `lifecycle` | reuses `manual` | Active Check while waiting (upload-window deadline) vs 404 after the end; uploads deleted at the end |
| `interrupted` ↻ | 0 | a waiting Check with an upload, then a restart → FAILED / INTERRUPTED, Active Check gone, uploads deleted |
| `expiry` ↻ | 0 | mode `upload-window=PT1M`, `deadline-check-interval=PT5S`: deadline = start + 1 min, still waiting at 30 s, then FAILED / UPLOAD_WINDOW_EXPIRED, Active Check 404, uploads deleted, late upload / confirmation 409 |
| `notpermitted` ↻ | 0 | mode `data-class=REAL` (both models FREE): a path Check → FAILED / MODEL_NOT_PERMITTED with no model call (log); a PNG upload is never sent to the reading model (log) |
| `limits` ↻ | 4 | mode `max-file-size=1KB`, `max-rows=1`, `data-class=REAL`, comparison tier `APPROVED` (local test only; the data is synthetic), reading tier FREE: path and BLOB documents UNREADABLE / TOO_LARGE unread; a PNG UNREADABLE / MODEL_NOT_PERMITTED beside a READ PDF; a 2-row query unread "more than 1 rows" → NEEDS_MANUAL_REVIEW; two simultaneous APPROVED decisions → one 201 + one 409, exactly one Approval API call |
| `withdrawn` ↻ | 0 | `demo-blob` folder parked → load WITHDRAWN, unlisted, read `available:false`, start 422 `CHK-422-SERVICE-NOT-AVAILABLE`; folder back → available again (UNCHANGED, version kept) |
| `connection` ↻ | 0 | `demo-noconn` (query over `ghost-db`) REJECTED at load; `demo-conn` registered while `local-extra` is activated, then the connection is dropped → REMOVED, folder REJECTED, service kept, start 422 `CHK-422-CONNECTION-NOT-ACTIVATED`; back to normal → `demo-conn` WITHDRAWN |
| `race` | 0 | `DOC-409-CHECK-ENDED` through INT (TC-INT-096): reported AMBIGUOUS — not reproducible deterministically through the API (reason in the result) |
| `reg-rules` ↻ | 0 | REG load-run rules (RULE-REG-001…028) in an ISOLATED package directory `local/e2e-reg/<batch>`: one load run judges ~30 invalid folders (one Load Result row each, the TC's exact English reason), activation refusals (unknown type, over-length name / endpoint), symlink outside the directory; dedicated runs for the whole-run TCs (TC-REG-085 4 rows, TC-REG-009 3 rows after 4); valid / boundary folders; two instances started together on 7271 + 7272 (TC-REG-079) |
| `reg-activation` ↻ | 0 | connection activation over successive starts: endpoint change UPDATED then ACTIVATED (stored = new value), limited-to-views flip, empty / missing / 250-char package directory (nothing withdrawn, shortened subject), duplicate and not-read-only `main-db` (a Check start proves 0 registered: 422 `CHK-422-CONNECTION-NOT-ACTIVATED`) |
| `reg-versions` ↻ | 0 | version progression: v2→v3→v4 (a manual Check pinned to each version read through INT API-INT-008), edited-in-place / older version / duplicate code rejected, unreadable file (mode 000), file changing during the read (a FIFO whose mtime moves while it is read), blob connection turned mcp / removed then relisted mcp (RULE-REG-025) |
| `reg-inprocess` ↻ | 0 | in-process interface through public APIs: unknown service (REG-404 text + CHK-422), write verbs refused, load lock held by another Oracle session (sqlplus in the `erp-oracle` container) → the stored registry is served; the TCs no public API can observe are reported `NOT-EXERCISABLE` with the reason |

| `doc-path` ↻ | 3 + 3 reading | DOC `path` fetch in an ISOLATED package directory / storage root `local/e2e-doc/` (mode `m1`): one data-dependent document source query (one bound `:requestId`) gives request A exactly 100 rows (21 cases: NOT_FOUND incl. empty location and a directory, `..` resolved, absolute / symlink / other-directory paths OUTSIDE_STORAGE_ROOT, .docx / random bytes / `.pdf` name without PDF UNSUPPORTED_FORMAT, password-protected / damaged PDF and damaged workbook READING_FAILED, .xlsx and .xls tables, scanned PDF and 2 images to the reading model, exactly 10 MB READ / +1 byte TOO_LARGE, instruction-like text; 79 fillers), request C 101 rows (over max-rows), request D an MCP error |
| `doc-blob` ↻ | 1 | DOC `blob` fetch over `local-jdbc`: BLOB READ, NULL content column NOT_FOUND, random bytes UNSUPPORTED_FORMAT; no BLOB through the MCP channel |
| `doc-manual` ↻ | 2 + 1 reading | per-Check isolation of uploads (Checks 501 / 502), manual mode touches no host document, upload of exactly 10 MB kept and READ, oversized upload TOO_LARGE at fetch, `scan.pdf` holding a PNG read as an image |
| `doc-noroot` ↻ | 2 | mode `noroot` (no storage root, no reading model, 1 MB): every `path` document OUTSIDE_STORAGE_ROOT, nothing kept between Checks, PNG READING_FAILED without a reading model, upload listing in order without content |
| `doc-inprocess` | 0 | DOC TCs no public API reaches, reported `NOT-EXERCISABLE` with the reason (partial evidence asserted where cheap) |
| `rpt-store` ↻ | 0 (stub) | RPT through the Check lifecycle and API-RPT-001/002/003 in modes `R1` (SVC-A version 2, purge every 10 s without a retention period) and `R2` (version 3, timeout PT60S, upload window PT2M, max-rows 500): purge skipped (TC-RPT-052), 0 unfinished at a start, a path Check RUNNING read back (40 s pipeline, TC-INT-027), identifiers byte-identical, RUNNING with its running time, decision recorded, finding / evidence / note / `<script>` text / order, own record per Check, decision agreement over two versions (11 Checks), unread query "more than 500 rows", no content column (data dictionary), a FIFO path detail never opened, 130 / 101 Checks → 100 + total, bound filters, 503 → MODEL_UNAVAILABLE, TIMED_OUT, UPLOAD_WINDOW_EXPIRED, unfinished Checks ended oldest first after an abrupt stop (SIGKILL) |
| `int-flow` ↻ | 0 (stub) | INT in mode `R4` (local/e2e-int/packages; 70 MB request limit, 10 s approval timeout, short pool timeouts): unknown employee, no `/view` page, uploads alone / no decision, the uploaded-documents relay (300 KB + 60 MB, DOC's list called once), executed approval (1 stub call, 0 MCP), REJECTED while the Approval API holds → APPROVED 409 + WARN, unusable approval definition → INT-500 (partial, TC-INT-044), nothing kept (tables, multipart temp files, live heap dump attributed), database paused 1.5 s+ (`docker pause erp-oracle`, unpaused in a `finally`) → INT-500, an undecided compliant Check never approved |
| `rpt-purge` ↻ | 0 | mode `R3`: retention 1 day, purge every 10 s, JDBC socket read timeout 5 s; another Oracle session locks a Finding of run A and the row of run B (`SELECT … FOR UPDATE`, rolled back after 150 s): A and B kept (WARN before the closing count), every other run ended before the cut-off deleted (404, 0 rows — read-only counts), a recent run kept, A deleted whole after the release, 0 MCP queries. It deletes the local runs older than 1 day — the purge's designed effect |
| `rpt-inprocess` / `int-inprocess` | 0 | RPT / INT TCs no public path reaches, reported `NOT-EXERCISABLE` with the reason |
↻ = restart group. The REG groups make **0 model calls**: they never confirm a Check, start only `manual` Checks
(or ones expected to be refused 422), and their modes also set `aias.documents.data-class=REAL` (FREE models) as
defence in depth. A service code a REG TC stores carries the run's tag (`scholarship-request-t1003095850`) because
the local registry keeps every version forever; codes a TC expects rejected keep the TC's literal value. Batches of a
REG group run in a fixed order (each one's preconditions are what the earlier ones stored); `--only <reg group>`
replays whatever earlier batch a scenario needs. Status `NOT-EXERCISABLE` (reason in the result) never counts as passed. Completed Checks are shared between groups (one model call each), so the whole run costs
**11 comparison calls and 1 reading call** (base groups 7 + `limits` 4). `--only <group>` builds whatever shared
Check it needs.

### Restart groups and modes

A restart group puts the local app in a **mode** through `setup_fixtures.apply_mode` and restarts it (no
rebuild, ~20–40 s): a profile override in `local/e2e-override.properties` (gitignored; the local profile imports
it when present and it takes precedence) and/or fixture package folders parked in `local/e2e-parked/`. A base
group first returns to the normal mode, and the runner **always restores the normal mode** at the end of a run
(no override file, `demo-conn` / `demo-noconn` parked, every other fixture in the package directory). Any restart
ends every waiting Check of the earlier run `FAILED / INTERRUPTED` (REQ-CHK-055) — that is the designed recovery,
not a defect. Spring does not merge indexed lists across property sources, so an override that adds a REG
connection lists every connection (`setup_fixtures.connections_with`).

By hand: `python3 scripts/e2e/setup_fixtures.py --override aias.check.upload-window=PT1M --override
aias.check.deadline-check-interval=PT5S` (restart in that mode) and `python3 scripts/e2e/setup_fixtures.py
--clear-override` (back to normal).

## Setup and run

The `local` profile must already work (see `local/README.md`): Oracle Free `LOAN_SYS`, the Oracle MCP
server (`npm install` in `governance/mcp-servers/oracle`), `local/secrets.properties` with the model key,
JDK 25 on `JAVA_HOME`.

```bash
python3 scripts/e2e/setup_fixtures.py            # fixtures + restart if needed (--restart / --no-restart)
python3 scripts/e2e/simulate.py                  # all groups -> logs/e2e-simulation-<date>.json / -run.md
python3 scripts/e2e/simulate.py --only refusals --only registry   # no model call
python3 scripts/e2e/simulate.py --out governance/project-artifacts/E2E-SIMULATION-<date>
```

`--match <text>` runs only the scenarios whose name contains the text. The DOC modes override the comparison model (`E2E_DOC_COMPARISON_MODEL`, default `gemini-3.7-flash`).

Exit code 0 when no scenario FAILED or ERRORed. Scenario statuses: `PASSED`, `FAILED`, `ERROR` (runner or
transport failure), `SKIPPED-QUOTA`, `SKIPPED-MODEL`, `SKIPPED-RATE`, `SKIPPED-BUDGET`, `SKIPPED-PRECONDITION`, `AMBIGUOUS`, `NOT-EXERCISABLE`.

What `setup_fixtures.py` creates (all gitignored):

- `local/storage-root/transcripts/{pass.pdf, fail.pdf, transcript.png}` and `local/outside.pdf` (outside the root);
- `local/package-directory/demo-path/` (fetch `path`; the document source query over `local-oracle` returns
  `TRANSCRIPT → transcripts/pass.pdf` and `ID_CARD → ../outside.pdf`) and `demo-blob/` (fetch `blob`; the
  source query over `local-jdbc` returns `TO_BLOB(HEXTORAW('<hex of a synthetic PDF>'))` from `DUAL` — no
  host table);
- a marked block in `src/main/resources/application-local.properties`: `aias.documents.storage-root`,
  REG connection `local-jdbc` (jdbc, read-only, credential reference `LOCAL_JDBC_CREDENTIAL`),
  `aias.integration.approval.timeout=PT2S`, `aias.integration.upload.request-limit=12MB`;
- `LOCAL_JDBC_CREDENTIAL=<user>:<password>` in `local/secrets.properties`, copied from the local datasource
  settings and never printed;
- in the same marked block, `spring.config.import` re-declared to add the optional `local/e2e-override.properties`;
- restart-group fixtures: `local/storage-root/transcripts/big.pdf` (4.5 KB); packages `demo-path-big` (path →
  `big.pdf`), `demo-blob-big` (a 2000-byte BLOB built by `TO_BLOB(HEXTORAW(RPAD('25504446', 4000, '30')))`),
  `demo-rows` (manual; query `rows_probe` returns 2 rows by `CONNECT BY LEVEL <= 2`), and — parked in
  `local/e2e-parked/` — `demo-conn` (a query over the connection `local-extra`, which only the `connection` group
  activates) and `demo-noconn` (a query over the never-defined `ghost-db`);
- the approval stub is restarted when it lacks the `delay` mode (`/__mode?set=delay&seconds=1`: 200 after a
  short delay, used by the concurrent-approvals scenario).

## Model quota (free tier)

The local comparison and reading models are Gemini free tier (about 20 requests/day/model, 5/minute). The
runner spaces model-triggering actions 13 s apart and stops at `--max-model-checks` (default 12). When a
Check ends `FAILED / MODEL_UNAVAILABLE` with a per-DAY 429/quota detail, that scenario is `SKIPPED-QUOTA` and every
later model-dependent scenario is skipped without calling the model. A per-MINUTE 429 (`...PerMinute...`) gives
`SKIPPED-RATE` and a 65 s cool-down; any other `MODEL_UNAVAILABLE` detail (e.g. 503 overloaded) gives
`SKIPPED-MODEL` and a 30 s cool-down. A model scenario ending in either is retried once. Neither is an app defect. Overall-status assertions depend on
the model reading synthetic values correctly; a model misjudgement shows as a FAILED check whose detail
quotes the findings.

## Long runs

A full run takes about 25 minutes (9 app restarts). On macOS the runner holds a `caffeinate -i -s` assertion: a
system sleep freezes the app and the runner mid-Check, and the runner's wall-clock waits then fail. That assertion
does not stop a lid-closed sleep on battery, so keep the machine on AC power and awake. Every app log of a run is kept
in `logs/e2e-<runId>/`, because each restart replaces `logs/aias-local.log`.

## Safety

- **Local only**: `simulate.py` refuses any base URL whose host is not `localhost` / `127.0.0.1` / `::1`;
  the Approval API is only the local stub on port 7290 (`local/approval-stub.py`), whose mode the runner
  switches and always resets to `ok`.
- **Synthetic data only**: every transcript is generated by `synth.py` and says it is synthetic; no real
  person, request or document is used. The `limits` mode declares the comparison model tier `APPROVED` and the
  data class `REAL` only to make the reading model's FREE×REAL refusal observable in a COMPLETED report; the
  documents are still synthetic.
- **No direct DB writes**: the runner works only through the HTTP API. The Checks it creates stay in the
  local schema until the retention purge; each run lists them under "Surviving records". Waiting Checks it
  leaves behind end `FAILED / UPLOAD_WINDOW_EXPIRED` after the 60-minute window, without a model call — or
  `FAILED / INTERRUPTED` at the next restart of the app, whichever comes first.
