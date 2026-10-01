# TestSprite Governance — Backend

> **RETIRED — history only.** TestSprite is no longer this repo's backend test
> mechanism. Module tests are the delivered packages' acceptance tests
> (`{unit}.package.json → tests`, recorded with `scripts/gov-module.py record MOD
> package UNIT --passed N --failed N`), and API verification is the `api-verify`
> skill (the published api-docs held to the module's API document, recorded as
> the `api_verify` row). The paths below are obsolete: `governance/modules/<MOD>/…`
> no longer exists, and the v7 partition `governance/shared/backend/modules/<MOD>/`
> holds only `execution-state.json`, `api-docs/` and `test-api/` — no
> `testsprite/` archive. Do not start new TestSprite runs or archives from this
> document.

This document is the single source of truth for how TestSprite (the MCP-based
AI test generator, wired in `backend/.mcp.json` as the `TestSprite` server)
is used against this repository, and how its output is organized. Read this
before running any TestSprite tool, and before touching anything under
`testsprite_tests/` or `governance/modules/<MOD>/testsprite/`.

---

## 1. Mechanism — how TestSprite actually works here

TestSprite runs as an MCP server (`npx @testsprite/testsprite-mcp@latest`,
`API_KEY` from `TESTSPRITE_API_KEY`) and drives a fixed pipeline, always
against `projectPath = backend` (this repo root) and
`localEndpoint = http://localhost:7272/actuator/health` (the running Spring
Boot app — start it first, see `backend/CLAUDE.md`'s "Running Locally"):

1. **Bootstrap** (`testsprite_bootstrap_tests`) — records `type: backend`,
   `scope: codebase`, the local endpoint, and backend auth
   (`admin`/`admin`, basic token) into a session file under
   `~/.testsprite/mcp/session-*.json`. Writes `testsprite_tests/tmp/config.json`
   (gitignored — pure session cache, never commit it).
2. **Code summary** (`testsprite_generate_code_summary`) — scans the Spring
   Boot source and writes `testsprite_tests/tmp/code_summary.yaml`
   (gitignored).
3. **Standardized PRD** (`testsprite_generate_standardized_prd`) — writes
   `testsprite_tests/standard_prd.json`, a full description of every
   endpoint TestSprite discovered, across every module at once. This is
   **not per-module** — one PRD covers the whole backend.
4. **Backend test plan** (`testsprite_generate_backend_test_plan`) — writes
   `testsprite_tests/testsprite_backend_test_plan.json`, a flat list
   `TC001..TCNNN`, again spanning every module in one file, renumbered from
   `TC001` **every time it runs**.
5. **Generate + execute** (`testsprite_generate_code_and_execute`) — for
   each `TCnnn` in the plan, writes a standalone Python script
   `testsprite_tests/TCnnn_<slug>.py` (plain `requests` calls against
   `localhost:7272`, no pytest fixtures, no shared setup — each file is
   fully self-contained and ends with a bare call to its own `test_*()`
   function) and executes it, then writes
   `testsprite_tests/testsprite-mcp-test-report.md` / `.html`.

**The problem this document exists to prevent:** step 4 restarts numbering
at `TC001` on every run, and step 5 never deletes a previous run's
`TCnnn_*.py` files before writing new ones — it only adds/overwrites. Run
TestSprite twice without a governance step in between and `testsprite_tests/`
accumulates multiple unrelated files all named `TC001_*`, `TC002_*`, etc.,
from different runs, with no way to tell from the filename alone which ones
still match the current `testsprite_backend_test_plan.json`. This exact
situation was found and cleaned up on 2026-08-29 (see §6 below) — the rules
in §2–§4 exist so it doesn't recur.

---

## 2. Where things live

| Content | Location | Lifetime |
|---|---|---|
| TestSprite's own working directory (session cache, PRD, plan, generated `.py`, report — all freshly (re)written by every run) | `backend/testsprite_tests/` (repo root — this path is `projectPath`-derived and is **not configurable**; TestSprite always writes here) | Ephemeral — treat as scratch. Never the durable copy. |
| `testsprite_tests/tmp/` | same folder | Gitignored, session-only. Never archive or commit it. |
| **Durable, module-organized test archive** (the currently-valid generated `.py` files, one copy each, sorted by owning module) | `backend/governance/modules/<MOD>/testsprite/tests/` | Permanent, git-tracked. Overwritten only when a fresh run regenerates that module's tests (see §3). |
| **Full run bundle archive** (`standard_prd.json` + `testsprite_backend_test_plan.json` + `testsprite-mcp-test-report.md`/`.html` from one specific run, kept intact together) | `backend/governance/testsprite/runs/<YYYY-MM-DD>-backend/` | Permanent, one dated folder per run, never overwritten. |
| Ready-to-use prompts to drive TestSprite | `backend/governance/testsprite/prompts/` | Permanent. |
| This document | `backend/governance/testsprite/TESTSPRITE-GOVERNANCE.md` | Permanent. |

`governance/modules/<MOD>/testsprite/` is a **different, unrelated** folder
from the legacy `governance/modules/<MOD>/test-api/` that already exists for
ORG / FILESVC / NOTIFICATION / SECURITY — `test-api/` is a hand-written
pytest suite from the old MODE-5 API-test generation pass, not TestSprite
output. Do not merge the two or move files between them.

---

## 3. Module classification rule

`testsprite_backend_test_plan.json` and the generated `.py` files are not
module-aware — TestSprite treats the backend as one flat surface. Assign
each generated `TCnnn_*.py` to a module by the endpoint(s) it calls. The
authority for which module owns what is `governance/modules-registry.json`
plus that module's own `P0` business-policies and module-registry docs:

> **Rebuilt 2026-09-05**, amended 2026-09-12 after MDL shipped and was registered.
> The route prefixes below are the ones the controllers actually declare — verified
> against `@RequestMapping` and the live `/v3/api-docs/<group>` documents, not assumed
> from the module code (they differ: CU serves `/api/v1/common`, SEC serves
> `/api/v1/sec`). Note the registry itself is not a complete module list — `SEC` is
> fully built and live but still has no `modules-registry.json` entry. Re-derive this
> table any time a module is added/renamed or a controller's base path changes; don't
> assume it stays accurate across a major restructuring.

| Endpoint prefix / subject | Module |
|---|---|
| `/api/v1/sec/*` (auth, users, roles, sessions, registry, menu, audit-log, dashboard, signup-requests) | `SEC` |
| `/api/v1/common/configurations*` | `CU` |
| `/api/v1/files*` (documents, categories, lookups) | `FILE` |
| `/api/v1/mdl*` (lookup types, lookup values, lookup consumption) | `MDL` |
| `/api/v1/notifications*` (dispatch, channels, templates, logs, lookups) | `NOTIF` |

If a generated test genuinely spans two modules (rare — most TestSprite
scenarios hit one resource), file it under the module that owns the
resource being asserted on, not the module used only for setup (e.g. a test
that logs in via `SECURITY` to then test `/api/org/branches` is `ORG`).

If a future module is added to `modules-registry.json`, add its prefix here
in the same pass — don't leave newly-generated tests unclassifiable.

---

## 4. Standing rule for every future run (read before running TestSprite)

**Before starting a new run** (before calling
`testsprite_bootstrap_tests`/generation again):

1. Check `backend/testsprite_tests/` for leftover `.py` files, `standard_prd.json`,
   `testsprite_backend_test_plan.json`, or report files from a previous run
   that were never archived. If any exist, archive them first, exactly as
   in §5 below (classify each `.py` by §3, `git mv` it into its module's
   `testsprite/tests/`, and move the PRD/plan/report trio into a new
   `governance/testsprite/runs/<today>-backend/`) — never let a new run
   start writing into a folder that still holds a stale run's output.
2. Only then run the new pipeline (see
   `governance/testsprite/prompts/start-tests.md` for the exact prompt).

**After a run finishes** (code generated + executed + report written):

1. Read `testsprite_tests/testsprite_backend_test_plan.json` — this is the
   authoritative list of what the CURRENT run actually covers.
2. For every `TCnnn` in that plan, classify it by §3 and `git mv` its
   `.py` file from `testsprite_tests/` into
   `governance/modules/<MOD>/testsprite/tests/TCnnn_<slug>.py`. If a file
   with the same `TCnnn_<slug>.py` name already exists there from a prior
   run, overwrite it (that module's test coverage moved forward — don't
   keep both).
3. `git mv` `standard_prd.json`, `testsprite_backend_test_plan.json`,
   `testsprite-mcp-test-report.md`, `testsprite-mcp-test-report.html` as a
   set into a new `governance/testsprite/runs/<YYYY-MM-DD>-backend/`
   folder (one dated folder per run — never reuse or overwrite an existing
   date folder for a different run on the same day; append `-2`, `-3`, ...
   if a second run genuinely happens the same day).
4. Confirm `testsprite_tests/` now contains nothing but the gitignored
   `tmp/` folder before considering the run "closed out".

**Never**:
- Hand-edit a `.py` file already archived under
  `governance/modules/<MOD>/testsprite/tests/` to change what scenario it
  tests, or to make a genuinely new flow pass — that's what regenerating
  through TestSprite is for. The one sanctioned exception — keeping an
  existing test's assertions/payloads in sync after a real code change so
  it keeps passing — is §5, not a loophole to rewrite scenarios by hand.
- Leave a run's PRD/plan/report sitting at the root of `testsprite_tests/`
  once the run is closed out — it belongs in a dated `runs/` folder.
- Invent a different folder shape than §2 — if this structure doesn't fit a
  new situation, that's a structural question for the human, per
  `backend/CLAUDE.md`'s STRUCTURAL LAW, not something to improvise around.

---

## 5. Keeping archived tests in sync with code changes

The whole point of §2's durable archive is that any test in it can be
re-run at any time (`governance/testsprite/prompts/rerun-tests.md`) and
still mean something. A backend change that quietly breaks an archived
test's assumptions — without anyone touching the test — defeats that.

**Before finishing any backend code change**, check whether it touches
something an archived test exercises:

1. Identify the module the change belongs to (per §3's endpoint table).
2. Search that module's `governance/modules/<MOD>/testsprite/tests/*.py`
   for the endpoint path, request/response field name, status code, or
   error code you're changing (`grep -rl` for the path or field is enough
   — these are small, flat, self-contained scripts).
3. If nothing matches, there's nothing to do.
4. If a test matches and your change alters what it asserts — a renamed
   or restructured request/response field, a changed status code or
   error-code string, a new required field, a moved/renamed endpoint, a
   changed auth requirement — **update that test file's payload/assertions
   in the same change**, minimally, to match the new contract. This is a
   direct edit to the archived `.py` file — allowed specifically for this
   case, unlike the general "never hand-edit" rule in §4.
5. Re-run the updated test (via `rerun-tests.md`, scoped to that file or
   module) against the changed code before considering the code change
   done. A patched-but-never-executed test is not verified.
6. If the change is big enough that patching would mean rewriting the
   scenario's actual flow (not just updated values) — e.g. the endpoint's
   purpose changed, a multi-step flow gained/lost a step — prefer
   regenerating that scenario through TestSprite (`start-tests.md`) over
   hand-authoring a new flow. Say so instead of forcing a hand patch.

Never ship a backend change that leaves an archived test asserting on a
contract that no longer exists — either fix the test in the same change or
flag it explicitly as a known, intentional break for the human to resolve.
Silently leaving it to fail on the next re-run is not acceptable.

---

## 6. 2026-08-29 — one-time cleanup (event log)

On this date, `backend/testsprite_tests/` held the combined, unarchived
output of at least two separate runs (files dated 2026-08-25 and
2026-08-26), all flattened together with colliding `TC001..TC010` names.
Cleanup performed:

- The 12 files matching the **current**
  `testsprite_backend_test_plan.json` (the 2026-08-26 run) were classified
  per §3 and moved to their module's `governance/modules/<MOD>/testsprite/tests/`.
- That run's `standard_prd.json`, `testsprite_backend_test_plan.json`, and
  both report files were moved to
  `governance/testsprite/runs/2026-08-26-backend/`.
- The remaining 25 `.py` files (2026-08-25) did not match any entry in the
  current test plan — that run's own PRD/plan/report had already been
  overwritten by the 2026-08-26 run before this cleanup, so there is no
  matching bundle to reconstruct for them. They were moved as-is, unclassified,
  to `governance/testsprite/runs/archived-superseded-2026-08-25/tests/` for
  history only. They are not part of the module test archive and should not
  be treated as current coverage for any module.

This is the incident that produced §4's standing rule — the goal is that
this cleanup is never needed again.

---

## 7. Related

- Ready prompts: `governance/testsprite/prompts/start-tests.md` (fresh
  run), `governance/testsprite/prompts/rerun-tests.md` (re-execute already
  generated tests, no regeneration), and
  `governance/testsprite/prompts/fix-bugs.md` (diagnose → fix → re-run
  loop for failures reported by either of the above, per §5's sync rule).
- Module ownership authority: `governance/modules-registry.json` plus each
  module's own `P0` docs.
- Routing table cross-reference: `governance/GOVERNANCE-RULES.md`'s
  "Governance Content Map".
