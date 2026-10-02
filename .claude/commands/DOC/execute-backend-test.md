# /DOC/execute-backend-test

Run DOC's backend test packages, publish its api-docs and hold them to the
API document (`api-verify`).

> **Self-contained.** Needs `scripts/gov-module.py`,
> `governance/governance-tools/api-doc-generator`, the `api-verify` skill
> (`.claude/skills/api-verify/SKILL.md`) and this module's artifacts as
> `gov-module.py plan DOC --json` names them, plus the api-verify script
> generator `governance/governance-tools/api-verify-generator/build.py`.
> Never TestSprite (retired). No JUnit — the test phase IS api-verify.

> **Track.** This repo's folder name is not a track name, so every
> `scripts/gov-module.py` and `./scripts/governance` call in this command runs
> with `GOV_TRACK=backend` exported in the same shell (the tool's own
> `--track` / `GOV_TRACK` option). Without it the tool refuses:
> *"track `agentic-ai-library` is not one of ['backend', 'frontend']"*.

## Usage
/DOC/execute-backend-test

---

## STEP 0 — Contract, gate, assessment

### 0.1 — Load
```bash
export GOV_TRACK=backend
python3 scripts/gov-module.py --track backend pin              # exit 3 → STOP: "the factory owner runs gov.py upgrade-project"
python3 scripts/gov-module.py --track backend plan DOC --json
python3 scripts/gov-module.py --track backend delivery DOC
```
`plan.delivered_version` must be 1; if it differs, STOP and re-run
`/generate-module-setup DOC`.

The REQUIRED COVERAGE is every `TC-DOC-<seq>` block of `plan.test_plan`
(the P4 backend test plan) plus every id listed in the `tests` of
`plan.exec_units`, `plan.test_units` and the built `plan.integration` packages —
with its traces (`AC-*`, `XM-*`, `API-*`) and one-line scenario.

### 0.2 — Gate (MANDATORY)
Every unit of `plan.exec_units` is `accepted` in `delivery`. Otherwise:
```
══════════════════════════════════════════════════════
⛔ TEST GATE FAILED — DOC
══════════════════════════════════════════════════════
Waiting on : [unit: not run | FAILING], ...
══════════════════════════════════════════════════════
```
STOP.

### 0.3 — Assessment and confirmation (same pattern as execute-backend.md)

---

## STEP 1 — Test packages (REQUIRED COVERAGE, exercised by api-verify)

No JUnit (user decision 2026-10-02): this repo keeps no `src/test/java` tests.
The `TC` blocks of each `plan.test_units` entry in the Test Map below (skipping
the integration test phase's units — their tests belong to the integration
packages) are the REQUIRED COVERAGE that the `api-verify` script (STEP 3)
exercises, each check tagged with its TC id. Read every unit's `.md` (`file`)
so each TC is mapped to a script check; `MODEL-EVAL` is the fixed known-result
request set run on every model change — synthetic or anonymised data only, G13.

After the STEP 3 run, record each test unit's `package` row from that run's
per-TC results:
```bash
GOV_TRACK=backend python3 scripts/gov-module.py record DOC package <UNIT> --passed <N> --failed <N>
```
`--passed` / `--failed` count the TCs the unit holds that the script exercised
and that passed / failed. A TC the script cannot exercise over HTTP is listed
as not-exercisable in the problems report (`test-api/`) — never counted as
passed, and it shows as a GAP in STEP 4.

## STEP 2 — Publish the api-docs (MANDATORY, every run, before api-verify)

Run `/generate-api-docs DOC` (review → generate|update → check). The docs land
in this track's partition `governance/shared/backend/modules/DOC/api-docs/`
(`index.md` first, then `**/*.md`), with contract ids from `plan.api_spec`.
Confirm `index.md` was written before STEP 3.

## STEP 3 — api-verify

Confirm the app is reachable (`GET /api/v1/services` — no Actuator on the classpath, `/actuator/health` is 404 — on the port in
`src/main/resources/application-local.properties`; unreachable → `ENVIRONMENT_FAILURE`, stop).
Invoke the `api-verify` skill for DOC. It reads the api-docs (STEP 2) and
`plan.api_spec`, writes its script and problems report under
`governance/shared/backend/modules/DOC/test-api/`, runs the script, and
records the result (then STEP 1's per-unit `package` rows):
```bash
GOV_TRACK=backend python3 scripts/gov-module.py record DOC api_verify --version <plan.delivered_version> --result PASS|FAIL
```
The script is built by the persisted generator — never hand-edited:
```bash
python3 governance/governance-tools/api-verify-generator/build.py DOC
# → governance/shared/backend/modules/DOC/test-api/test_doc_apis.py
```
Local run facts: the app runs with `--spring.profiles.active=local`, on the
`server.port` of `src/main/resources/application-local.properties`; point the
script and the health check at that port.

`PASS` only when the script exits zero. The frontend's delivery of this version
stays OPEN until this row is PASS.

## STEP 4 — Coverage cross-check (MANDATORY)

```
GOVERNED PLAN ↔ TESTS — DOC v1
TC/AC id          │ traces       │ scenario │ test (package / api-verify ref) │ result
──────────────────┼──────────────┼──────────┼─────────────────────────────────┼────────
TC-DOC-001   │ AC-… API-…   │ …        │ API-SCENARIOS · test_slot       │ PASS
TC-DOC-0NN   │ XM-…         │ …        │ ✗ none                          │ GAP
```
A required id with no green test is a GAP — listed, never dropped. An id of a
deferred integration package is DEFERRED, not a gap. Record the ratio.

## STEP 5 — Classify and report

| Code | Meaning |
|---|---|
| `TEST_STRUCTURE_FAILURE` | Broken test script itself — not an app bug |
| `DB_PRECONDITION` | Required seed/lookup/master data missing |
| `ENVIRONMENT_FAILURE` | Server or config unreachable/broken |
| `DEPENDENCY_FAILURE` | Skipped/failed because an upstream test failed |
| `MISSING_IMPLEMENTATION` | Endpoint or feature not built yet |
| `AUTH_FAILURE` | Login / session / token issue |
| `VALIDATION_FAILURE` | Backend rejected input that should have been valid |
| `SERVER_ERROR` | 5xx from backend |
| `CONTRACT_BREAK` | Response departs from the API document |
| `API_REGRESSION` | API behavior changed vs. expected |
| `DATA_INTEGRITY_ISSUE` | API step reported success but DB state is wrong |
| `BUSINESS_LOGIC_ISSUE` | A functional/business rule behaves incorrectly |

Every failure/skip gets exactly one code. Write
`governance/project-artifacts/TEST-REPORT-DOC-backend-[YYYY-MM-DD].md` with the
STEP 4 table and ratio ABOVE the taxonomy. A contract mismatch the backend cannot
fix in code → `GOV_TRACK=backend python3 scripts/gov-module.py record DOC api_doc_gaps --key "<METHOD> <path>" --resolution "OPEN — …"`.
Report and STOP on any FAIL or GAP — this command never fixes source.

## STEP 6 — End of work
```bash
export GOV_TRACK=backend
python3 scripts/gov-module.py --track backend delivery DOC
python3 scripts/gov-module.py --track backend validate DOC
./scripts/governance push
```

---

## Test Map — DOC v1

In `backend-test/index.md` order.

1. `API-SCENARIOS` · SUB · 27 TC blocks · 27 listed tests
2. `INT-XM` · PHASE · 4 TC blocks · 4 listed tests · **integration test phase — built with the integration packages (execute-backend STEP 2), not here**
3. `MODEL-EVAL` · SUB · 12 TC blocks · 12 listed tests
4. `RULE-SCENARIOS` · SUB · 34 TC blocks · 34 listed tests

## Constraints (NON-NEGOTIABLE)
- NEVER run before the gate (0.2) passes
- NEVER run api-verify before this run's api-docs were regenerated and published
- NEVER call TestSprite
- NEVER write JUnit / `src/test/java` tests — the TC blocks are exercised by the api-verify script
- NEVER count a not-exercisable TC as passed
- NEVER modify application source — report, don't fix
- NEVER hand-edit a generated `test-api` script — regenerate it
- ALWAYS record every package row and the `api_verify` row through `gov-module.py record`
- ALWAYS emit the STEP 4 coverage table before calling the module tested
