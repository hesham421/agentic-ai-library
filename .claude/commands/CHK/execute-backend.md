# /CHK/execute-backend

Build CHK's delivered backend packages — one unit at a time, each accepted
when its listed tests are green.

> **Track.** This repo's folder name is not a track name, so every
> `scripts/gov-module.py` and `./scripts/governance` call in this command runs
> with `GOV_TRACK=backend` exported in the same shell (the tool's own
> `--track` / `GOV_TRACK` option). Without it the tool refuses:
> *"track `agentic-ai-library` is not one of ['backend', 'frontend']"*.

## Usage
/CHK/execute-backend [UNIT | PHASE | integration | features]

---

## STEP 0 — Load the contract and the resume point (MANDATORY)

```bash
export GOV_TRACK=backend
./scripts/governance pull
python3 scripts/gov-module.py pin            # exit 3 → STOP: "the factory owner runs gov.py upgrade-project"
python3 scripts/gov-module.py plan CHK --json
python3 scripts/gov-module.py state-init CHK
python3 scripts/gov-module.py delivery CHK
```

- `plan.delivered_version` must be 1 (the version this command was generated
  for). If it differs, STOP: re-run `/generate-module-setup CHK`.
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
ASSESSMENT — CHK v1 / [scope]
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
  ```` ```yaml name=error-catalog ```` (error codes, status, message keys),
  `permission-matrix`, `bootstrap` (seed data), `totals`, `self-check`. Read the
  named block, never a prose table.

### 1.1 — Read
1. The unit's `.md` (`plan.exec_units[unit].file`) completely.
2. Its manifest (`plan.exec_units[unit].manifest`) → `tests`: the acceptance set.
3. For every `API-*` block the unit builds: its operation in `plan.api_spec`
   (by `x-api-id`) — method, path, parameters, schemas, every error response
   and its `x-error-codes`, `x-paginated`. The API document is the contract;
   implement to it.
4. The db-script as of the delivered version — the latest
   `P2/db-script-<mod>.md` among `governance/shared/analysis/modules/CHK/` (v1)
   and its `vK/` folders with K ≤ `plan.delivered_version` (a delta folder holds
   only what changed; read-only): its DDL is ONE
   ```` ```sql ```` fence, its field matrix the ```` ```yaml name=dbf-matrix ````
   block (`id, table, column, type, entity_field, traces`). Never invent a column.
5. The SRS for the RULE/AC ids the unit traces.
6. Each listed test's definition: the `<!-- TC:<id>:START -->` block in
   `plan.test_plan` (or the test package unit holding it); an `AC-*` id → its
   Given/When/Then in the SRS.

### 1.2 — Build
1. Match each task to the skills in `.claude/skills/` (`build-*` to generate,
   `gov-*` to validate); read each skill in full before writing.
2. Implement the tasks in order.
3. Write the unit's acceptance tests — one test per listed id, named or tagged
   with it — and run them (`mvn -q test -Dtest=…`; at minimum
   `mvn -q -DskipTests compile` for a `no_tests` unit).
4. Run the validating skill (`gov-validate-backend-feature`).

### 1.3 — Record (the only way a unit is done)
```bash
GOV_TRACK=backend python3 scripts/gov-module.py record CHK package <UNIT> --passed <N> --failed <N>
```
`N` counts the unit's listed tests. A `no_tests` unit records `--passed 0 --failed 0`
after a clean build. Any failure → the row says so; do not advance past a failing
unit without the user.

### Blocked items, gaps
- OQ-blocked task → skip it, mark the code `// TODO: OQ-[ID] — pending resolution`, and
  `GOV_TRACK=backend python3 scripts/gov-module.py record CHK blocked --key OQ-[ID] --resolution "OPEN — <what is missing>"`.
- The plan or the API document is wrong or silent →
  `GOV_TRACK=backend python3 scripts/gov-module.py record CHK api_doc_gaps --key "<METHOD> <path>" --resolution "OPEN — <what>" --detail "<what you did instead>"`.
- A resolution must START with one of OPEN, DEFERRED, PENDING, RESOLVED, CLOSED,
  IMPLEMENTED, HUMAN, ADR — the tool refuses anything else.

---

## STEP 2 — Integration packages (after every ordinary unit is accepted)

```bash
GOV_TRACK=backend python3 scripts/gov-module.py requires CHK
```

For each `plan.integration[]` edge, in order:
- `requires_met` → read its package (`manifest` → `XM-…/XM-….md`: target,
  type, contract, the `do` steps such as a `ddl_patch`), read the target's
  published contract (`platform/contracts/contract-<target>.md`), build it through
  the established cross-module layer (no XM id in code), write and run its
  `tests`, then `record CHK package <XM-ID> --passed N --failed N`.
- not met → do NOT build it:
  `GOV_TRACK=backend python3 scripts/gov-module.py record CHK deferred_xm --key <XM-ID> --resolution "DEFERRED — <requires> not met"`.
  Move on; a deferred edge is built when a later run sees it met.

Integration packages are excluded from the module's own delivery
(`gov-module.py delivery` still prints their lines — read those as integration
status, not as module acceptance).

## STEP 3 — Feature prompts

For each `plan.features[]` entry with `done: false`: read the prompt (`file`) —
it is self-contained; the frozen plans carry a FEATURE LOG line naming it —
execute it with the same skill / test discipline, then
`GOV_TRACK=backend python3 scripts/gov-module.py record CHK feature <FEAT-ID>`.

## STEP 4 — Session report and end of work

```bash
export GOV_TRACK=backend
python3 scripts/gov-module.py delivery CHK
python3 scripts/gov-module.py validate CHK
./scripts/governance push
```

Print: units accepted / failing, tests passed/failed, blocked, deferred XM,
gaps recorded, features executed, next unit.

---

## Unit Map — CHK v1

Phases in `plan.phases` order; units within a phase in that phase folder's
`index.md` order.

**CORE** · `no_tests` — accepted on a clean build (`--passed 0 --failed 0`)
1. `CORE` · PHASE · MEDIUM · tasks: 9 · 0 tests

**DATA-DOM** · `no_tests` — accepted on a clean build (`--passed 0 --failed 0`)
2. `DATA-DOM` · PHASE · LIGHT · tasks: 4 · 0 tests

**PORTS**
3. `PORTS-DOCUMENT` · SUB · LIGHT · tasks: 2 · 4 tests
4. `PORTS-MODEL` · SUB · MEDIUM · tasks: 5 · 15 tests
5. `PORTS-QUERY` · SUB · MEDIUM · tasks: 5 · 9 tests

**SVC-API**
6. `SVC-API` · PHASE · HEAVY · tasks: 8 ops/API · 22 steps · 67 tests

**ALIGN-BE** · `no_tests` — accepted on a clean build (`--passed 0 --failed 0`)
7. `ALIGN-BE` · PHASE · MEDIUM · tasks: 5 · 0 tests

**CROSS-MOD** (integration phase — never built as an ordinary phase; see STEP 2)

**Integration packages**
- `XM-CHK-001` → REG · ENT-REG-001 · requires `REG:DELIVERED` · MET · 4 tests (AC-CHK-005, AC-CHK-006, AC-CHK-008, TC-CHK-096) · LIGHT
- `XM-CHK-002` → REG · ENT-REG-002 · requires `REG:DELIVERED` · MET · 9 tests (AC-CHK-007, AC-CHK-008, AC-CHK-009, AC-CHK-016, AC-CHK-028, AC-CHK-035, AC-CHK-058, AC-CHK-059, TC-CHK-097) · LIGHT
- `XM-CHK-003` → REG · ENT-REG-003 · requires `REG:DELIVERED` · MET · 4 tests (AC-CHK-012, AC-CHK-013, AC-CHK-016, TC-CHK-098) · LIGHT
- `XM-CHK-004` → REG · ENT-REG-004 · requires `REG:DELIVERED` · MET · 5 tests (AC-CHK-020, AC-CHK-021, AC-CHK-022, AC-CHK-023, TC-CHK-099) · LIGHT
- `XM-CHK-005` → REG · ENT-REG-005 · requires `REG:DELIVERED` · MET · 4 tests (AC-CHK-007, AC-CHK-014, AC-CHK-015, TC-CHK-100) · LIGHT
- Integration test unit `INT-XM` (test phase `INT-XM`) is built with these packages, not on its own.

**Features**
- none — no feature prompts delivered (STEP 3 is a no-op)

---

## Constraints (NON-NEGOTIABLE)
- NEVER skip STEP 0 or build without confirmation after the assessment
- NEVER restate a governance path — read it from `gov-module.py plan CHK --json`
- NEVER invent a field, column, route or error code — the API document, the
  db-script's `sql` fence / `dbf-matrix`, and the `error-catalog` block are ground truth
- NEVER build the integration phase as an ordinary phase, or an integration
  package whose `requires` is not met
- NEVER mark a unit done any way but `gov-module.py record … package`
- NEVER write under `…/packages/` or `analysis/` — only this track's partition, only through `gov-module.py`
- NEVER advance past a failing unit without the user's instruction
