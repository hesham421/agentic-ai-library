# TestSprite Governance — aias Backend

> **NOT ADOPTED for aias — kept as a note only.** TestSprite is not this repo's
> backend test mechanism, and it is not wired here (there is no `.mcp.json`
> entry, no `testsprite_tests/` folder and no `prompts/` folder). Module tests
> are the delivered packages' acceptance tests (`{unit}.package.json → tests`,
> recorded with `python3 scripts/gov-module.py --track backend record MOD
> package UNIT --passed N --failed N`) and the test units `RULE-SCENARIOS`,
> `API-SCENARIOS`, `MODEL-EVAL` and `INT-XM`; API verification is the
> `api-verify` skill (the published api-docs held to the module's API document,
> recorded as the `api_verify` row). The v7 partition
> `governance/shared/backend/modules/<MOD>/` holds only `execution-state.json`,
> `api-docs/` and `test-api/` — no `testsprite/` archive. Do not start
> TestSprite runs or archives from this document.

This document came over with tooling copied from an earlier, unrelated backend,
where TestSprite (the MCP-based AI test generator) had been used and then
retired. What follows keeps the parts that would still matter if adopting it
for aias were ever proposed — the mechanism, the failure mode it causes, and the
rules that prevent it — rewritten for this repo. Adopting it requires an
explicit human decision (see `CLAUDE.md` → STRUCTURAL LAW).

---

## 1. Mechanism — how TestSprite works

TestSprite runs as an MCP server (`npx @testsprite/testsprite-mcp@latest`, an
`API_KEY` from the environment) and drives a fixed pipeline against
`projectPath` = this repo root and a `localEndpoint` on the running Spring Boot
app:

1. **Bootstrap** (`testsprite_bootstrap_tests`) — records `type: backend`,
   `scope: codebase`, the local endpoint and any backend auth into a session
   file under `~/.testsprite/mcp/`. aias has no caller authentication
   (amendment A2), so no credentials would be given. Writes
   `testsprite_tests/tmp/config.json` (session cache, never committed).
2. **Code summary** (`testsprite_generate_code_summary`) — scans the source and
   writes `testsprite_tests/tmp/code_summary.yaml`.
3. **Standardized PRD** (`testsprite_generate_standardized_prd`) — writes
   `testsprite_tests/standard_prd.json`, describing every endpoint it
   discovered across every module at once — **not per-module**.
4. **Backend test plan** (`testsprite_generate_backend_test_plan`) — writes
   `testsprite_tests/testsprite_backend_test_plan.json`, a flat list
   `TC001..TCNNN` spanning every module, renumbered from `TC001` **every time
   it runs**.
5. **Generate + execute** (`testsprite_generate_code_and_execute`) — writes one
   self-contained Python script per `TCnnn` and executes it, then writes a
   report.

**The problem this causes:** step 4 restarts numbering at `TC001` on every
run, and step 5 never deletes a previous run's scripts. Two runs without a
governance step in between leave unrelated files with colliding names and no
way to tell which still match the current plan. Its `TCnnn` ids also collide
in form with the factory's `TC-<MOD>-<seq>` ids while meaning something else.

**Why it does not fit aias as it stands:** it designs its own scenarios,
outside the factory's test plan, so its results cannot be recorded against the
governed `TC-*` ids; it would send request data and documents through a third
party (domain-profile G13 allows only synthetic or anonymised data with a
free-tier provider); and a check reaches the host query channel, the document
source and the model — dependencies a generic generator does not set up.

---

## 2. Where things would live (if ever adopted)

| Content | Location | Lifetime |
|---|---|---|
| TestSprite's working directory (session cache, PRD, plan, generated scripts, report) | `testsprite_tests/` at the repo root (fixed by `projectPath`) | Scratch — never the durable copy |
| `testsprite_tests/tmp/` | same folder | Session-only; gitignore it before the first run |
| Durable run bundle (PRD + plan + report of one run, kept together) | `governance/testsprite/runs/<YYYY-MM-DD>-backend/` | Permanent, one dated folder per run |
| This document | `governance/testsprite/TESTSPRITE-GOVERNANCE.md` | Permanent |

Nothing TestSprite writes goes into `governance/shared/` — the backend
partition there is defined by the factory (`execution-state.json`, `api-docs/`,
`test-api/`) and `./scripts/governance push` publishes only that.

---

## 3. Module classification rule

TestSprite's output is not module-aware. A generated test would be assigned to
an aias module by the operation it asserts on — its `x-api-id` in the module's
API document (`python3 scripts/gov-module.py --track backend plan <MOD> --json`
→ `api_spec`), not by path prefix: `/api/v1/checks/…` is served by both INT
(starts, uploads, decisions) and RPT (reads). Today's operations:

| Module | Operations |
|---|---|
| `REG` | `GET /api/v1/services`, `GET /api/v1/services/{serviceCode}`, `GET /api/v1/load-results` |
| `DOC` | `GET /api/v1/uploaded-documents` |
| `CHK` | `GET /api/v1/active-checks/{checkId}` |
| `RPT` | `GET /api/v1/checks/{checkId}`, `GET /api/v1/checks`, `GET /api/v1/decision-agreement` |
| `INT` | `POST /api/v1/checks`, `POST /api/v1/checks/{checkId}/documents`, `GET /api/v1/checks/{checkId}/documents`, `POST /api/v1/checks/{checkId}/upload-confirmation`, `POST /api/v1/checks/{checkId}/decision`, `GET /api/v1/check-reports/{checkId}`, `GET /api/v1/check-reports`, `GET /api/v1/checks/{checkId}/required-document-types` |

Re-derive this table from the API documents whenever a module's delivered
version changes; it is a snapshot, not a contract.

---

## 4. Standing rules (if ever adopted)

**Before a run:** archive any leftover output of a previous run first (the
PRD/plan/report trio into a new dated `runs/` folder); never let a new run write
into a folder that still holds a stale run's output. Use only a Dev/Test
deployment with synthetic service packages and host fixtures; never a real host
and never the host Approval API.

**After a run:** read the plan (the authoritative list of what the run
covered), move the PRD/plan/report trio into `governance/testsprite/runs/<YYYY-MM-DD>-backend/`
(append `-2`, `-3`, … for a second run the same day), and confirm
`testsprite_tests/` holds only `tmp/`.

**Never:**
- record a TestSprite result as a package row or as the `api_verify` row — those
  come only from the governed tests and the `api-verify` skill;
- hand-edit a generated script to change the scenario it tests;
- invent a folder shape other than §2 — a structural question goes to the human,
  per `CLAUDE.md`'s STRUCTURAL LAW.

---

## 5. Keeping archived tests in sync with code changes

If archived TestSprite scripts ever exist, a backend change that alters what one
asserts — a request/response field, a status, a ProblemDetail `code`, a path —
updates that script's payload/assertions in the same change, minimally, and
re-runs it. A change big enough to alter the scenario's flow regenerates it
instead of hand-authoring a new one. Never leave an archived test asserting on a
contract that no longer exists.

---

## 6. History

TestSprite was used and retired in the earlier backend this note came from;
its run archives and incident log stayed there. aias has never run it.

---

## 7. Related

- API verification: `.claude/skills/api-verify/SKILL.md`.
- Test units and acceptance: `python3 scripts/gov-module.py --track backend plan <MOD>`
  and `delivery <MOD>`.
- Module ownership: `governance/shared/platform/modules-registry.json`, each
  module's API document and its `P0` docs.
- Repository rules: `CLAUDE.md` ("Where to Find Governance", STRUCTURAL LAW).
