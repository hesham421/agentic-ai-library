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
| `synth.py` | Generators of synthetic transcripts: a PDF with a text layer, a PNG with bitmap-font text (no text layer) |

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

↻ = restart group. Completed Checks are shared between groups (one model call each), so the whole run costs
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

Exit code 0 when no scenario FAILED or ERRORed. Scenario statuses: `PASSED`, `FAILED`, `ERROR` (runner or
transport failure), `SKIPPED-QUOTA`, `SKIPPED-MODEL`, `SKIPPED-RATE`, `SKIPPED-BUDGET`, `SKIPPED-PRECONDITION`, `AMBIGUOUS`.

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
