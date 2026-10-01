# /RPT/execute-backend-test

Run RPT's backend test packages, publish its api-docs and hold them to the
API document (`api-verify`).

> **Self-contained.** Needs `scripts/gov-module.py`,
> `governance/governance-tools/api-doc-generator`, the `api-verify` skill
> (`.claude/skills/api-verify/SKILL.md`) and this module's artifacts as
> `gov-module.py plan RPT --json` names them. Never TestSprite (retired).

> **Track.** This repo's folder name is not a track name, so every
> `scripts/gov-module.py` and `./scripts/governance` call in this command runs
> with `GOV_TRACK=backend` exported in the same shell (the tool's own
> `--track` / `GOV_TRACK` option). Without it the tool refuses:
> *"track `agentic-ai-library` is not one of ['backend', 'frontend']"*.

## Usage
/RPT/execute-backend-test

---

## STEP 0 — Contract, gate, assessment

### 0.1 — Load
```bash
export GOV_TRACK=backend
python3 scripts/gov-module.py --track backend pin              # exit 3 → STOP: "the factory owner runs gov.py upgrade-project"
python3 scripts/gov-module.py --track backend plan RPT --json
python3 scripts/gov-module.py --track backend delivery RPT
```
`plan.delivered_version` must be 1; if it differs, STOP and re-run
`/generate-module-setup RPT`.

The REQUIRED COVERAGE is every `TC-RPT-<seq>` block of `plan.test_plan`
(the P4 backend test plan) plus every id listed in the `tests` of
`plan.exec_units`, `plan.test_units` and the built `plan.integration` packages —
with its traces (`AC-*`, `XM-*`, `API-*`) and one-line scenario.

### 0.2 — Gate (MANDATORY)
Every unit of `plan.exec_units` is `accepted` in `delivery`. Otherwise:
```
══════════════════════════════════════════════════════
⛔ TEST GATE FAILED — RPT
══════════════════════════════════════════════════════
Waiting on : [unit: not run | FAILING], ...
══════════════════════════════════════════════════════
```
STOP.

### 0.3 — Assessment and confirmation (same pattern as execute-backend.md)

---

## STEP 1 — Test packages

For each `plan.test_units` entry in the Test Map below (skipping the integration
test phase's units — their tests belong to the integration packages): read its
`.md` (`file`), implement every `TC` block it holds as a test tagged with the TC
id (JUnit/integration test under `src/test/java`), run them, and record:
```bash
GOV_TRACK=backend python3 scripts/gov-module.py record RPT package <UNIT> --passed <N> --failed <N>
```
`N` counts the TC blocks of the unit's `.md` plus its manifest's `tests`.

## STEP 2 — Publish the api-docs (MANDATORY, every run, before api-verify)

Run `/generate-api-docs RPT` (review → generate|update → check). The docs land
in this track's partition `governance/shared/backend/modules/RPT/api-docs/`
(`index.md` first, then `**/*.md`), with contract ids from `plan.api_spec`.
Confirm `index.md` was written before STEP 3.

## STEP 3 — api-verify

Confirm the app is reachable (`/actuator/health` on the port in
`src/main/resources/application.properties`; unreachable → `ENVIRONMENT_FAILURE`, stop).
Invoke the `api-verify` skill for RPT. It reads the api-docs (STEP 2) and
`plan.api_spec`, writes its script and problems report under
`governance/shared/backend/modules/RPT/test-api/`, runs the script, and
records the result:
```bash
GOV_TRACK=backend python3 scripts/gov-module.py record RPT api_verify --version <plan.delivered_version> --result PASS|FAIL
```
`PASS` only when the script exits zero. The frontend's delivery of this version
stays OPEN until this row is PASS.

## STEP 4 — Coverage cross-check (MANDATORY)

```
GOVERNED PLAN ↔ TESTS — RPT v1
TC/AC id          │ traces       │ scenario │ test (package / api-verify ref) │ result
──────────────────┼──────────────┼──────────┼─────────────────────────────────┼────────
TC-RPT-001   │ AC-… API-…   │ …        │ API-SCENARIOS · test_slot       │ PASS
TC-RPT-0NN   │ XM-…         │ …        │ ✗ none                          │ GAP
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
`governance/project-artifacts/TEST-REPORT-RPT-backend-[YYYY-MM-DD].md` with the
STEP 4 table and ratio ABOVE the taxonomy. A contract mismatch the backend cannot
fix in code → `GOV_TRACK=backend python3 scripts/gov-module.py record RPT api_doc_gaps --key "<METHOD> <path>" --resolution "OPEN — …"`.
Report and STOP on any FAIL or GAP — this command never fixes source.

## STEP 6 — End of work
```bash
export GOV_TRACK=backend
python3 scripts/gov-module.py --track backend delivery RPT
python3 scripts/gov-module.py --track backend validate RPT
./scripts/governance push
```

---

## Test Map — RPT v1

In `backend-test/index.md` order.

1. `API-SCENARIOS` · SUB · 29 TC blocks · 0 listed tests
2. `MODEL-EVAL` · SUB · 1 TC blocks · 0 listed tests
3. `RULE-SCENARIOS` · SUB · 34 TC blocks · 0 listed tests

## Constraints (NON-NEGOTIABLE)
- NEVER run before the gate (0.2) passes
- NEVER run api-verify before this run's api-docs were regenerated and published
- NEVER call TestSprite
- NEVER modify application source — report, don't fix
- NEVER hand-edit a generated `test-api` script — regenerate it
- ALWAYS record every package row and the `api_verify` row through `gov-module.py record`
- ALWAYS emit the STEP 4 coverage table before calling the module tested
