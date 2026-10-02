# Generate Backend Module Setup

```
Lives at   : .claude/commands/generate-module-setup.md (the aias backend repo), so it
             auto-loads as a Claude Code slash command
Reads      : scripts/gov-module.py --track backend (the factory's v7 consumer contract) — nothing else
Writes     : .claude/commands/[MODULE]/execute-backend.md
             .claude/commands/[MODULE]/execute-backend-test.md
             the module's execution-state.json (via `gov-module.py state-init`)
```

## Precondition — start of work, and the contract this repo reads

This repo's directory name is not `backend`, so `gov-module.py` cannot infer the
track: every call passes `--track backend`, and `GOV_TRACK=backend` is exported
for `./scripts/governance` (which calls the tool without the flag). The same
holds for every command this file generates.

```bash
export GOV_TRACK=backend
./scripts/governance pull                 # takes the factory's latest, checks the pin, applies the derived scope
python3 scripts/gov-module.py --track backend pin         # 0 = pinned at schema 7 · 3 = pre-v7 · 1 = refused
```

- `governance/shared/platform/profile-summary.json` missing → `git submodule update --init governance/shared`, then start over.
- `pin` exit **3** → the project predates factory schema 7. Say exactly that and
  STOP: *"the factory owner runs `gov.py upgrade-project`"*. Nothing below can be
  read from a pre-v7 project, and nothing here guesses around it.
- `pin` exit **1** → refused (the project is pinned to a schema this repo's tool
  does not read). Report its message and STOP.

## Step 0 — every factory fact comes from one tool (mechanical, never typed)

`scripts/gov-module.py` reads every path and vocabulary from what the factory
publishes in the project repo (`platform/profile-summary.json → consumer`,
`platform/modules-registry.json`). This command and everything it generates
**call it instead of restating a path**. A path, a phase list or a version
typed here goes stale the next time the factory publishes.

```bash
python3 scripts/gov-module.py --track backend modules                  # every module: wave, current version, backend delivered version
python3 scripts/gov-module.py --track backend plan $MODULE             # human summary
python3 scripts/gov-module.py --track backend plan $MODULE --json      # the object everything below reads
```

`plan --json` carries (names are the tool's — use them as given):

| Field | What it is |
|---|---|
| `delivered_version` | the version the factory delivered to this track — from the registry, **never** the highest `analysis/modules/MOD/vN` folder (a delta `vN` holds only what changed) |
| `current_version` | the module's analysis version (may be ahead of delivery) |
| `exec_plan` / `test_plan` | the backend execution plan (P3.1) and the backend test plan (P4) as of the delivered version |
| `exec_packages` / `test_packages` | the delivered package folders (`…/packages/vN/…` for a delta version — already resolved) |
| `exec_units` / `test_units` | one per `{unit}.package.json`: `unit`, `kind` (PHASE \| SUB), `tests` (TC/AC ids), `acceptance` (`tests-green`), `manifest`, `file` (the unit's `.md`) |
| `integration` | one per cross-module XM edge: `xm`, `target`, `requires` (e.g. `PAT:DELIVERED`), `requires_met`, `tests`, `acceptance`, `manifest` |
| `api_spec` | the module's API document (OpenAPI 3.1, `x-api-id` / `x-traces` / `x-error-codes`; no security scheme — caller authentication is deferred, amendment A2) — **the API contract** |
| `contract` | the module's published contract (`platform/contracts/contract-<mod>.md`) |
| `state_file` | this track's `execution-state.json` |
| `features` | `analysis/modules/MOD/prompts/FEAT-MOD-NNN.md`: `id`, `file`, `done` |
| `phases` | the profile's ordered exec phases: `key`, `folder`, `integration`, `no_tests` |
| `api_docs` | this track's published api-docs folder, `""` until first published |

If `plan` REFUSES with *"no backend delivery yet"*, the module has not passed its
review gate — the factory splits packages only after the ONE `analysis` gate
(after P4). Say so and STOP; there is nothing to set up.

**Where this repo may write.** Only under its partition
`governance/shared/backend/modules/<MODULE>/` — `execution-state.json`
(`plan.state_file`), `api-docs/`, `test-api/`. Never `…/packages/` (the factory's
delivery inside that partition), never `analysis/`. `./scripts/governance push`
stages only this track's pathspec (`gov-module.py pathspec`), so a write outside
it is not published — but it is still a write into someone else's tree; do not
make it.

---

## Input

```
$ARGUMENTS = MODULE
```

If missing, ask — do not guess. The module must be listed by
`python3 scripts/gov-module.py --track backend modules`; if it is not, STOP with "unknown module".

## Your Task

Generate two commands and initialise the execution state:

1. `.claude/commands/[MODULE]/execute-backend.md` — builds the delivered packages
   unit by unit, then the integration packages, then the feature prompts.
2. `.claude/commands/[MODULE]/execute-backend-test.md` — the test packages,
   api-docs publication and `api-verify`, ending in the `api_verify` row.
3. `python3 scripts/gov-module.py --track backend state-init [MODULE]` — creates or upgrades
   `plan.state_file` with every channel and row list. Never hand-write that file's
   skeleton; never overwrite rows already recorded.

**Version folder for the generated commands.** `delivered_version` 1 →
`.claude/commands/[MODULE]/`. `delivered_version` N ≥ 2 →
`.claude/commands/[MODULE]/v{N}/`, so the commands of an earlier delivery stay as
history. Never derive N from the analysis folders.

Each module gets its own `.claude/commands/[MODULE]/` folder — never write to the
flat `.claude/commands/execute-backend.md` (it collides with every other module).

**Generated commands call `gov-module.py` at run time.** They carry the unit
order and weights baked in below (that is what generation is for), but every
path they read — a unit's `.md`, its manifest, the plans, the API document, the
state file — they read from `plan --json` when they run. A generated command is
run standalone by a session that never read this file, so it must name the tool
and the field, never a variable of this file and never a bare `packages/…` path.

---

## Step 1 — Read the delivery

```bash
python3 scripts/gov-module.py --track backend plan $MODULE --json > /tmp/plan-$MODULE.json   # or hold it in the session
python3 scripts/gov-module.py --track backend requires $MODULE
python3 scripts/gov-module.py --track backend delivery $MODULE                             # what is already accepted
```

From `plan`:

- **Exec phases, in order** = `plan.phases[].key`, in the order given — the
  profile's own order (for aias today CORE → DATA-DOM → PORTS → SVC-API →
  ALIGN-BE → CROSS-MOD). Never type the list into a generated command, never
  sort by folder name, never assume which phase is last.
- **The integration phase** = the phase with `integration: true`. It is **not**
  built as an ordinary phase: each XM edge in `plan.integration[]` is ONE
  integration package. Its tests are the package's `tests`.
- **Units of a phase** = the `exec_units` whose `manifest` sits in that phase's
  `folder` under `exec_packages` (e.g. `…/backend-execution/PORTS/PORTS-QUERY.package.json`
  → phase `PORTS`, SUB `PORTS-QUERY`). A `PHASE` unit is the whole phase; `SUB` units split it.
  Within a phase keep the order of the phase folder's `index.md`; ignore
  `index.md`, `*-HEADER.md` (phase context, read once) and `.gitkeep`.
- `exec_packages/_SECTIONS.md` is plan content outside every phase — context,
  not a unit. Plan facts are NAMED yaml blocks (```` ```yaml name=<block> ````),
  wherever the split put them: `_SECTIONS.md` (e.g. `totals`, `error-catalog`,
  `self-check`) or a unit's own `.md` (e.g. `xm-register`). aias plans carry no
  permission or seed-data block — there is no permission model (amendment A2).
- **Test units** = `plan.test_units` (for aias: `RULE-SCENARIOS`,
  `API-SCENARIOS`, `MODEL-EVAL`). A test unit that belongs to the profile's
  integration **test** phase (`INT-XM`) (the test phase with `integration: true` —
  `jq -r '.tracks.backend.plans.test.phases[] | select(.integration) | .key' governance/shared/platform/profile-summary.json`)
  is built with the integration packages, not on its own.
- **Phases with `no_tests: true`** carry no tests: their units are accepted on a
  clean build (`--passed 0 --failed 0`).
- **Features** = `plan.features[]` not `done`.

A unit on disk that maps to no phase of `plan.phases`, or a phase declared there
with no unit on disk, is reported to the user — never folded in silently, never
fabricated.

### Weight classification

Read each unit's `.md` (`file`) and count its blocks (`API-*`, task blocks) and
its listed `tests`:

| Weight | Criteria |
|--------|----------|
| LIGHT  | < 5 tasks, single layer |
| MEDIUM | 5–10 tasks, 1–2 layers |
| HEAVY  | > 10 tasks, multi-layer (entity/domain + repository + port/adapter + service + controller) |
| XL     | Full feature in one unit |

Record weight, task count and test count for every unit.

---

## Step 2 — Initialise the execution state

```bash
python3 scripts/gov-module.py --track backend state-init $MODULE
python3 scripts/gov-module.py --track backend validate $MODULE
```

`state-init` adds whatever the contract requires and is missing — the channels
`api_doc_gaps`, `blocked`, `deferred_xm`, the `features_done` list, the
`package_results` and `api_verify` row lists — and keeps every row already
there. The state file is **written only through `gov-module.py record`**; a
hand edit is how a resolution word the factory cannot read gets in.

Progress is not a hand-kept `phases[].status`: a unit is done when
`package_results` holds its row with `tests_failed: 0`. `gov-module.py delivery
MOD` is the progress report.

---

## Step 3 — Generate `execute-backend.md`

Write to `.claude/commands/[MODULE]/execute-backend.md` (or `…/v{N}/`), filling
the bracketed parts from Step 1:

````markdown
# /[MODULE]/execute-backend

Build [MODULE]'s delivered backend packages — one unit at a time, each accepted
when its listed tests are green.

## Usage
/[MODULE]/execute-backend [UNIT | PHASE | integration | features]

---

## STEP 0 — Load the contract and the resume point (MANDATORY)

```bash
export GOV_TRACK=backend                     # scripts/governance calls gov-module.py without --track
./scripts/governance pull
python3 scripts/gov-module.py --track backend pin            # exit 3 → STOP: "the factory owner runs gov.py upgrade-project"
python3 scripts/gov-module.py --track backend plan [MODULE] --json
python3 scripts/gov-module.py --track backend state-init [MODULE]
python3 scripts/gov-module.py --track backend delivery [MODULE]
```

- `plan.delivered_version` must be [N] (the version this command was generated
  for). If it differs, STOP: re-run `/generate-module-setup [MODULE]`.
- Resume = the first unit of the Unit Map below that `delivery` does not show
  `accepted`. Never re-run an accepted unit unless asked.

## STEP 0.1 — Assessment, then confirmation

| Pending units in the requested scope | Action |
|---|---|
| All LIGHT/MEDIUM | one pass |
| Any HEAVY | chunk — one unit (or a few LIGHT ones) per pass |
| Any XL | that unit alone is one pass |

```
══════════════════════════════════════════════════════
ASSESSMENT — [MODULE] v[N] / [scope]
══════════════════════════════════════════════════════
Units pending : [unit · weight · tasks · tests]
Plan          : [one pass / chunks]
══════════════════════════════════════════════════════
Proceed? [waits for confirmation]
```

---

## STEP 1 — Per unit (in Unit Map order)

### 1.0 — Context, once per phase
- The phase's `*-HEADER.md` beside the unit's `file` (phase-level context).
- `_SECTIONS.md` at `plan.exec_packages` — plan content outside every phase.
  Plan facts are NAMED yaml blocks, in `_SECTIONS.md` or in the unit's own `.md`:
  ```` ```yaml name=error-catalog ```` (`{MOD}-{http}[-{SLUG}]` codes, HTTP
  status, English messages; Arabic `PENDING`), `totals`, `self-check`,
  `xm-register`. Read the named block, never a prose table.

### 1.1 — Read
1. The unit's `.md` (`plan.exec_units[unit].file`) completely.
2. Its manifest (`plan.exec_units[unit].manifest`) → `tests`: the acceptance set.
3. For every `API-*` block the unit builds: its operation in `plan.api_spec`
   (by `x-api-id`) — method, path, parameters, schemas, every error response
   (`application/problem+json` ProblemDetail) and its `x-error-codes`. The API
   document is the contract; implement to it.
4. The db-script as of the delivered version — the latest
   `P2/db-script-<mod>.md` among `governance/shared/analysis/modules/[MODULE]/` (v1)
   and its `vK/` folders with K ≤ `plan.delivered_version` (a delta folder holds
   only what changed; read-only): its DDL is ONE
   ```` ```sql ```` fence, its field matrix the ```` ```yaml name=dbf-matrix ````
   block (`id, table, column, type, entity_field, traces`). Never invent a column.
5. The SRS for the RULE/AC ids the unit traces.
6. Each listed test's definition: the `<!-- TC:<id>:START -->` block in
   `plan.test_plan` (or the test package unit holding it); an `AC-*` id → its
   Given/When/Then in the SRS.

### 1.2 — Build
1. Match each task to the skill routing table in `CLAUDE.md` (`build-*` to
   generate, `gov-*` to validate, `spring-*` for framework idioms); read each
   skill in full before writing.
2. Implement the tasks in order — layers controller, service, domain, port,
   adapter, repository; domain behaviour in domain classes; other modules only
   through their published contract interfaces (in-process).
3. Write the unit's acceptance tests — one test per listed id, named or tagged
   with it — and run them (`mvn -q test -Dtest=…`; at minimum
   `mvn -q -DskipTests compile` for a `no_tests` unit).
4. Run the validating skills (`gov-enforce-backend-contract`, then
   `gov-enforce-library-contract` for the §12 guardrails and boundaries).

### 1.3 — Record (the only way a unit is done)
```bash
python3 scripts/gov-module.py --track backend record [MODULE] package <UNIT> --passed <N> --failed <N>
```
`N` counts the unit's listed tests. A `no_tests` unit records `--passed 0 --failed 0`
after a clean build. Any failure → the row says so; do not advance past a failing
unit without the user.

### Blocked items, gaps
- OQ-blocked task → skip it, mark the code `// TODO: OQ-[ID] — pending resolution`, and
  `python3 scripts/gov-module.py --track backend record [MODULE] blocked --key OQ-[ID] --resolution "OPEN — <what is missing>"`.
- The plan or the API document is wrong or silent →
  `python3 scripts/gov-module.py --track backend record [MODULE] api_doc_gaps --key "<METHOD> <path>" --resolution "OPEN — <what>" --detail "<what you did instead>"`.
- A resolution must START with one of OPEN, DEFERRED, PENDING, RESOLVED, CLOSED,
  IMPLEMENTED, HUMAN, ADR — the tool refuses anything else.

---

## STEP 2 — Integration packages (after every ordinary unit is accepted)

```bash
python3 scripts/gov-module.py --track backend requires [MODULE]
```

For each `plan.integration[]` edge, in order:
- `requires_met` → read its package (`manifest` → `XM-…/XM-….md`: target,
  type, contract, the `do` steps such as a `ddl_patch`), read the target's
  published contract (`platform/contracts/contract-<target>.md`), build it through
  the established cross-module layer (no XM id in code), write and run its
  `tests`, then `record [MODULE] package <XM-ID> --passed N --failed N`.
- not met → do NOT build it:
  `python3 scripts/gov-module.py --track backend record [MODULE] deferred_xm --key <XM-ID> --resolution "DEFERRED — <requires> not met"`.
  Move on; a deferred edge is built when a later run sees it met.

Integration packages are excluded from the module's own delivery
(`gov-module.py delivery` still prints their lines — read those as integration
status, not as module acceptance).

## STEP 3 — Feature prompts

For each `plan.features[]` entry with `done: false`: read the prompt (`file`) —
it is self-contained; the frozen plans carry a FEATURE LOG line naming it —
execute it with the same skill / test discipline, then
`python3 scripts/gov-module.py --track backend record [MODULE] feature <FEAT-ID>`.

## STEP 4 — Session report and end of work

```bash
python3 scripts/gov-module.py --track backend delivery [MODULE]
python3 scripts/gov-module.py --track backend validate [MODULE]
./scripts/governance push
```

Print: units accepted / failing, tests passed/failed, blocked, deferred XM,
gaps recorded, features executed, next unit.

---

## Unit Map — [MODULE] v[N]
[phase (plan.phases order) → units in order: kind · weight · tasks · tests]
[integration: XM id → target · requires · met?]
[features: FEAT id · done?]

---

## Constraints (NON-NEGOTIABLE)
- NEVER skip STEP 0 or build without confirmation after the assessment
- NEVER restate a governance path — read it from `gov-module.py plan [MODULE] --json`
- NEVER invent a field, column, route or error code — the API document, the
  db-script's `sql` fence / `dbf-matrix`, and the `error-catalog` block are ground truth
- NEVER add caller authentication, a permission model or authorization
  annotations — deferred by amendment A2
- NEVER weaken a §12 guardrail (domain-profile G1–G14) to make a test pass
- NEVER build the integration phase as an ordinary phase, or an integration
  package whose `requires` is not met
- NEVER mark a unit done any way but `gov-module.py record … package`
- NEVER write under `…/packages/` or `analysis/` — only this track's partition, only through `gov-module.py`
- NEVER advance past a failing unit without the user's instruction
````

---

## Step 4 — Generate `execute-backend-test.md`

`api-verify` is module-scoped: it reads this module's published api-docs and its
API document, and nothing else. The generated command regenerates the api-docs
first, publishes them, then verifies.

````markdown
# /[MODULE]/execute-backend-test

Run [MODULE]'s backend test packages, publish its api-docs and hold them to the
API document (`api-verify`).

> **Self-contained.** Needs `scripts/gov-module.py` (always `--track backend`),
> `governance/governance-tools/api-doc-generator`, the `api-verify` skill
> (`.claude/skills/api-verify/SKILL.md`) and this module's artifacts as
> `gov-module.py --track backend plan [MODULE] --json` names them, plus the
> api-verify script generator `governance/governance-tools/api-verify-generator/build.py`.
> Never TestSprite (not part of aias). No JUnit — the test phase IS api-verify.

## Usage
/[MODULE]/execute-backend-test

---

## STEP 0 — Contract, gate, assessment

### 0.1 — Load
```bash
python3 scripts/gov-module.py --track backend pin              # exit 3 → STOP: "the factory owner runs gov.py upgrade-project"
python3 scripts/gov-module.py --track backend plan [MODULE] --json
python3 scripts/gov-module.py --track backend delivery [MODULE]
```
The REQUIRED COVERAGE is every `TC-[MODULE]-<seq>` block of `plan.test_plan`
(the P4 backend test plan) plus every id listed in the `tests` of
`plan.exec_units`, `plan.test_units` and the built `plan.integration` packages —
with its traces (`AC-*`, `XM-*`, `API-*`) and one-line scenario.

### 0.2 — Gate (MANDATORY)
Every unit of `plan.exec_units` is `accepted` in `delivery`. Otherwise:
```
══════════════════════════════════════════════════════
⛔ TEST GATE FAILED — [MODULE]
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
python3 scripts/gov-module.py --track backend record [MODULE] package <UNIT> --passed <N> --failed <N>
```
`--passed` / `--failed` count the TCs the unit holds that the script exercised
and that passed / failed. A TC the script cannot exercise over HTTP is listed
as not-exercisable in the problems report (`test-api/`) — never counted as
passed, and it shows as a GAP in STEP 4.

## STEP 2 — Publish the api-docs (MANDATORY, every run, before api-verify)

Run `/generate-api-docs [MODULE]` (review → generate|update → check). The docs land
in this track's partition `governance/shared/backend/modules/[MODULE]/api-docs/`
(`index.md` first, then `**/*.md`), with contract ids from `plan.api_spec`.
Confirm `index.md` was written before STEP 3.

## STEP 3 — api-verify

Confirm the app is reachable (`/actuator/health` if Actuator is on the classpath,
otherwise a documented read such as `GET /api/v1/services`, on the port in
`src/main/resources/application-local.properties`; unreachable → `ENVIRONMENT_FAILURE`, stop).
Invoke the `api-verify` skill for [MODULE]. It reads the api-docs (STEP 2) and
`plan.api_spec`, writes its script and problems report under
`governance/shared/backend/modules/[MODULE]/test-api/`, runs the script, and
records the result (then STEP 1's per-unit `package` rows):
```bash
python3 scripts/gov-module.py --track backend record [MODULE] api_verify --version <plan.delivered_version> --result PASS|FAIL
```
`PASS` only when the script exits zero. The frontend's delivery of this version
stays OPEN until this row is PASS.

The script is built by the persisted generator — never hand-edited:
```bash
python3 governance/governance-tools/api-verify-generator/build.py [MODULE]
# → governance/shared/backend/modules/[MODULE]/test-api/test_<module-lowercase>_apis.py
```
Local run facts: the app runs with `--spring.profiles.active=local`, on the
`server.port` of `src/main/resources/application-local.properties`; point the
script and the health check at that port.

## STEP 4 — Coverage cross-check (MANDATORY)

```
GOVERNED PLAN ↔ TESTS — [MODULE] v[N]
TC/AC id          │ traces       │ scenario │ test (package / api-verify ref) │ result
──────────────────┼──────────────┼──────────┼─────────────────────────────────┼────────
TC-[MODULE]-001   │ AC-… API-…   │ …        │ API-SCENARIOS · test_start      │ PASS
TC-[MODULE]-0NN   │ XM-…         │ …        │ ✗ none                          │ GAP
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
| `VALIDATION_FAILURE` | Backend rejected input that should have been valid |
| `SERVER_ERROR` | 5xx from backend |
| `CONTRACT_BREAK` | Response departs from the API document |
| `API_REGRESSION` | API behavior changed vs. expected |
| `DATA_INTEGRITY_ISSUE` | API step reported success but DB state is wrong |
| `BUSINESS_LOGIC_ISSUE` | A functional/business rule behaves incorrectly |

Every failure/skip gets exactly one code. Write
`governance/project-artifacts/TEST-REPORT-[MODULE]-backend-[YYYY-MM-DD].md` (create the folder if absent) with the
STEP 4 table and ratio ABOVE the taxonomy. A contract mismatch the backend cannot
fix in code → `record [MODULE] api_doc_gaps --key "<METHOD> <path>" --resolution "OPEN — …"`.
Report and STOP on any FAIL or GAP — this command never fixes source.

## STEP 6 — End of work
```bash
python3 scripts/gov-module.py --track backend delivery [MODULE]
python3 scripts/gov-module.py --track backend validate [MODULE]
./scripts/governance push
```

---

## Test Map — [MODULE] v[N]
[test units in order: kind · TC count · listed tests; integration-test-phase units marked]

## Constraints (NON-NEGOTIABLE)
- NEVER run before the gate (0.2) passes
- NEVER run api-verify before this run's api-docs were regenerated and published
- NEVER call TestSprite (not part of aias)
- NEVER write JUnit / `src/test/java` tests — the TC blocks are exercised by the api-verify script
- NEVER count a not-exercisable TC as passed
- NEVER modify application source — report, don't fix
- NEVER hand-edit a generated `test-api` script — regenerate it
- ALWAYS record every package row and the `api_verify` row through `gov-module.py record`
- ALWAYS emit the STEP 4 coverage table before calling the module tested
````

---

## Step 5 — Verify and report

```bash
python3 scripts/gov-module.py --track backend validate $MODULE
```

```
══════════════════════════════════════════════════════
BACKEND MODULE SETUP COMPLETE: [MODULE] v[N]
══════════════════════════════════════════════════════
execution-state.json      ✓  plan.state_file (state-init)
execute-backend.md        ✓  .claude/commands/[MODULE]/[vN/]
execute-backend-test.md   ✓  .claude/commands/[MODULE]/[vN/]

Phases (plan.phases)  : [keys in order, integration phase marked]
Exec units            : [count]  ·  test units: [count]
Integration packages  : [XM id → requires · met / deferred]
Features to execute   : [FEAT ids / none]
Weight map:
  [PHASE] / [UNIT]  → [WEIGHT]  ([N] tasks, [M] tests)

To start:   /[MODULE]/execute-backend
To verify:  /[MODULE]/execute-backend-test   (after every exec unit is accepted)
══════════════════════════════════════════════════════
```

---

## Constraints (this command itself — NON-NEGOTIABLE)

- NEVER run without MODULE specified, or on a pre-v7 project (`pin` exit 3)
- NEVER invent a phase, unit or path — every one comes from `gov-module.py plan`
- NEVER pick a version from the analysis folders — `plan.delivered_version` only
- NEVER reach into the frontend repo; this command has no frontend track
- NEVER write a machine's absolute path into a generated command or the state file
