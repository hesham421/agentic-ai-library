# /REG/execute-backend

Build REG's delivered backend packages — one unit at a time, each accepted
when its listed tests are green.

> **Track.** This repo's folder name is not a track name, so every
> `scripts/gov-module.py` and `./scripts/governance` call in this command runs
> with `GOV_TRACK=backend` exported in the same shell (the tool's own
> `--track` / `GOV_TRACK` option). Without it the tool refuses:
> *"track `agentic-ai-library` is not one of ['backend', 'frontend']"*.

## Usage
/REG/execute-backend [UNIT | PHASE | integration | features]

---

## STEP 0 — Load the contract and the resume point (MANDATORY)

```bash
export GOV_TRACK=backend
./scripts/governance pull
python3 scripts/gov-module.py --track backend pin            # exit 3 → STOP: "the factory owner runs gov.py upgrade-project"
python3 scripts/gov-module.py --track backend plan REG --json
python3 scripts/gov-module.py --track backend state-init REG
python3 scripts/gov-module.py --track backend delivery REG
```

- `plan.delivered_version` must be 1 (the version this command was generated
  for). If it differs, STOP: re-run `/generate-module-setup REG`.
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
ASSESSMENT — REG v1 / [scope]
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
   `P2/db-script-<mod>.md` among `governance/shared/analysis/modules/REG/` (v1)
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
GOV_TRACK=backend python3 scripts/gov-module.py record REG package <UNIT> --passed <N> --failed <N>
```
`N` counts the unit's listed tests. A `no_tests` unit records `--passed 0 --failed 0`
after a clean build. Any failure → the row says so; do not advance past a failing
unit without the user.

### Blocked items, gaps
- OQ-blocked task → skip it, mark the code `// TODO: OQ-[ID] — pending resolution`, and
  `GOV_TRACK=backend python3 scripts/gov-module.py record REG blocked --key OQ-[ID] --resolution "OPEN — <what is missing>"`.
- The plan or the API document is wrong or silent →
  `GOV_TRACK=backend python3 scripts/gov-module.py record REG api_doc_gaps --key "<METHOD> <path>" --resolution "OPEN — <what>" --detail "<what you did instead>"`.
- A resolution must START with one of OPEN, DEFERRED, PENDING, RESOLVED, CLOSED,
  IMPLEMENTED, HUMAN, ADR — the tool refuses anything else.

---

## STEP 2 — Integration packages (after every ordinary unit is accepted)

```bash
GOV_TRACK=backend python3 scripts/gov-module.py requires REG
```

For each `plan.integration[]` edge, in order:
- `requires_met` → read its package (`manifest` → `XM-…/XM-….md`: target,
  type, contract, the `do` steps such as a `ddl_patch`), read the target's
  published contract (`platform/contracts/contract-<target>.md`), build it through
  the established cross-module layer (no XM id in code), write and run its
  `tests`, then `record REG package <XM-ID> --passed N --failed N`.
- not met → do NOT build it:
  `GOV_TRACK=backend python3 scripts/gov-module.py record REG deferred_xm --key <XM-ID> --resolution "DEFERRED — <requires> not met"`.
  Move on; a deferred edge is built when a later run sees it met.

Integration packages are excluded from the module's own delivery
(`gov-module.py delivery` still prints their lines — read those as integration
status, not as module acceptance).

## STEP 3 — Feature prompts

For each `plan.features[]` entry with `done: false`: read the prompt (`file`) —
it is self-contained; the frozen plans carry a FEATURE LOG line naming it —
execute it with the same skill / test discipline, then
`GOV_TRACK=backend python3 scripts/gov-module.py record REG feature <FEAT-ID>`.

## STEP 4 — Session report and end of work

```bash
export GOV_TRACK=backend
python3 scripts/gov-module.py --track backend delivery REG
python3 scripts/gov-module.py --track backend validate REG
./scripts/governance push
```

Print: units accepted / failing, tests passed/failed, blocked, deferred XM,
gaps recorded, features executed, next unit.

---

## Unit Map — REG v1

Phases in `plan.phases` order; units within a phase in that phase folder's
`index.md` order.

**CORE** · `no_tests` — accepted on a clean build (`--passed 0 --failed 0`)
1. `CORE` · PHASE · MEDIUM · tasks: 11 · 0 tests

**DATA-DOM** · `no_tests` — accepted on a clean build (`--passed 0 --failed 0`)
2. `DATA-DOM-CONFIG` · SUB · HEAVY · tasks: 27 · 0 tests
3. `DATA-DOM-TRANSACTIONAL` · SUB · LIGHT · tasks: 2 · 0 tests

**PORTS**
4. `PORTS` · PHASE · LIGHT · tasks: 3 · 3 tests

**SVC-API**
5. `SVC-API` · PHASE · HEAVY · tasks: 15 (12 + 3 API) · 89 tests

**ALIGN-BE** · `no_tests` — accepted on a clean build (`--passed 0 --failed 0`)
6. `ALIGN-BE` · PHASE · MEDIUM · tasks: 8 · 0 tests

**CROSS-MOD** (integration phase — never built as an ordinary phase; see STEP 2)

**Integration packages**
- none — REG has no cross-module XM edge (STEP 2 is a no-op)

**Features**
- none — no feature prompts delivered (STEP 3 is a no-op)

---

## Constraints (NON-NEGOTIABLE)
- NEVER skip STEP 0 or build without confirmation after the assessment
- NEVER restate a governance path — read it from `gov-module.py plan REG --json`
- NEVER invent a field, column, route or error code — the API document, the
  db-script's `sql` fence / `dbf-matrix`, and the `error-catalog` block are ground truth
- NEVER build the integration phase as an ordinary phase, or an integration
  package whose `requires` is not met
- NEVER mark a unit done any way but `gov-module.py record … package`
- NEVER write under `…/packages/` or `analysis/` — only this track's partition, only through `gov-module.py`
- NEVER advance past a failing unit without the user's instruction
