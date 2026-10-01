# /orchestrate-module (BACKEND)

Master orchestration protocol for executing a BACKEND module's governance
execution pipeline in THIS repo — the aias (Request Verification Service)
backend. It lives at `.claude/commands/orchestrate-module.md` and runs from the
repo root, drives the module's own `execute-backend.md` per-phase command, and
adds a strict session/safety discipline on top of it. This command knows ONLY
the backend track — there is no frontend concept and no cross-repo reach into
the aias frontend repo (its own repository, not created yet). Because this
repo's directory name is not `backend`, every `gov-module.py` call passes
`--track backend` (and `GOV_TRACK=backend` is exported for
`./scripts/governance`). Every path below is relative to the repo root unless
said otherwise.

## Step 0 — the contract comes from one tool (never typed)

```bash
export GOV_TRACK=backend                      # scripts/governance calls gov-module.py without --track
./scripts/governance pull                     # start of work: the factory's latest, the pin check, the derived scope
python3 scripts/gov-module.py --track backend pin             # 0 = schema 7 · 3 = pre-v7 · 1 = refused
python3 scripts/gov-module.py --track backend modules         # live modules, wave, backend delivered version
python3 scripts/gov-module.py --track backend plan {MODULE} --json
```

- `pin` exit **3** → the project predates factory schema 7: say so and STOP —
  *"the factory owner runs `gov.py upgrade-project`"*. Exit **1** → refused; report and STOP.
- `scripts/gov-module.py` reads every path and vocabulary from what the factory
  publishes (`platform/profile-summary.json → consumer`, `platform/modules-registry.json`).
  This command and every dispatch it writes **read paths from `plan --json`**:
  `exec_plan` (P3.1), `test_plan` (P4), `exec_units` / `test_units` (each with
  `unit`, `kind`, `tests`, `acceptance: tests-green`, `manifest`, `file`),
  `integration` (per XM edge: `xm`, `target`, `requires`, `requires_met`, `tests`,
  `manifest`), `api_spec` (the OpenAPI 3.1 API document — the contract),
  `contract`, `state_file`, `features`, `phases`, `delivered_version`.
- **Where this repo writes:** only `governance/shared/backend/modules/{MODULE}/`
  (`execution-state.json`, `api-docs/`, `test-api/`) — never `…/packages/` (the
  factory's), never `analysis/`. The state file is written only through
  `gov-module.py record`.

## Usage

```
/orchestrate-module [MODULE] [UNIT|PHASE?] [--auto]
```

- `MODULE` (required): a module listed by `gov-module.py modules`. If it is not
  listed, STOP and say so; do not guess a near-match. Also required:
  `.claude/commands/{MODULE}/execute-backend.md` (or `…/v{N}/` for
  `delivered_version` N ≥ 2) — generate it with `/generate-module-setup {MODULE}`
  if missing. If `plan` REFUSES with *"no backend delivery yet"*, the module has
  not passed its ONE `analysis` review gate (after P4) and the factory has split
  nothing — STOP.

  **VERSION.** The version to orchestrate is `plan.delivered_version` — from the
  registry, never from the highest `analysis/modules/{MODULE}/vN` folder (a delta
  `vN` holds only what changed). `plan` already resolves the delta packages
  (`…/packages/vN/…`). To drive an older frozen version, ask — never assume.
- `UNIT|PHASE` (optional): if omitted, resume from `gov-module.py delivery
  {MODULE}` — the first unit (in the order below) not `accepted`. This command
  ALWAYS resumes; it never restarts a module and never re-runs an accepted unit.
- `--auto` (optional): AUTONOMOUS mode. Run every remaining phase to completion
  back-to-back WITHOUT pausing for the per-phase confirmation gate. Autonomy
  never weakens safety: units still run strictly one at a time, the per-unit
  skill-compliance read + STEP 1.3 verification still happen, and the run STILL
  HALTS on any of — a unit error, a failing acceptance test, a skill-compliance
  report that's silent or non-compliant, or a spec gap that STEP 2 cannot resolve
  from the module's own governance artifacts. Every one of those stops is now
  reached through STEP 1.4 first: on an impasse the orchestrator runs the
  second-agent debate, and halts only after that debate fails to resolve it — the
  debate is an added attempt BEFORE the stop, never a replacement for it. `--auto`
  removes the human pause between clean phases; it removes no check and no stop.
  Without `--auto`, the per-phase gate applies (recommended for a module's first
  run or after any spec or skill change).

**Order of work — read, never typed.**
1. The ordinary exec phases, in `plan.phases[]` order, **skipping** the phase
   with `integration: true`. A phase's units are the `exec_units` whose
   `manifest` sits in that phase's `folder`, in the order of the folder's
   `index.md`. Never assume which phase is first or last — there is no fixed
   phase list, and the integration phase is not necessarily named or placed
   anywhere in particular.
2. The integration packages — one per `plan.integration[]` edge (STEP 3.5).
3. The feature prompts not `done` (STEP 3.6).
4. The test phase (STEP 4), ending in `api_verify`.

In this file a **sub** is one dispatchable unit of work: one `exec_units[]`
entry, one integration package, or one feature prompt.

## Portability — never hardcode an absolute path

Keep this file working unchanged on any machine, for any user, with the repo
checked out anywhere. **Never write a specific machine's absolute path into this
file, into a dispatched agent's prompt, or into any config this command
touches.** At the start of every run derive the backend repo root at runtime —
`git rev-parse --show-toplevel`, or by walking up from this file's location —
never a remembered path from an earlier session or machine. Every path a
dispatched agent needs is resolved fresh, this run, on this machine.

---

## Role of the orchestrating session (this session, running this command)

**The orchestrating session never writes code, never edits governance specs,
and never touches the module's source files directly.** Its only jobs are:

1. Read state and spec files to brief each dispatch (read-only).
2. Dispatch exactly ONE Claude Code agent session (via the `Agent` tool) per
   **sub** — one delivered unit (`plan.exec_units[]`), one integration package,
   or one feature prompt. Never per individual task line, never a whole phase in one shot,
   regardless of what `execute-backend.md`'s weight table says. That file's
   weight-based chunking ("all LIGHT/MEDIUM → whole phase in one pass") is
   **overridden** by this standing rule: **one dispatched session per sub,
   always** — a deliberate context-safety and auditability choice.
3. Run execution **strictly sequentially**: dispatch sub N, wait for its agent
   to fully finish and report, do a lightweight verification pass, THEN dispatch
   sub N+1. Never dispatch two subs in parallel, even when they look
   independent — a later sub in a phase routinely depends on an earlier sub's
   output (a shared repository method, a common DTO/mapper, a cross-entity
   lookup).
4. Gate every **phase** transition on the user's explicit go-ahead. Before
   dispatching the first sub of a phase, print a phase assessment and wait:
   ```
   ══════════════════════════════════════════════════════
   PHASE ASSESSMENT — {MODULE} / {PHASE}
   ══════════════════════════════════════════════════════
   Subs pending : [list, one line each]
   Plan         : N separate Claude Code sessions (one per sub), sequential
   ══════════════════════════════════════════════════════
   Proceed?
   ```
   Never advance to the next phase without the user's explicit confirmation in
   this conversation, even if every sub completed cleanly. **Exception —
   `--auto`:** still PRINT each phase assessment (so the run stays auditable in
   the transcript), but do not wait — proceed automatically, and when the phase
   closes cleanly, on to the next. `--auto` only removes the pause between CLEAN
   phases; it never removes a stop on a sub error, a failed/omitted validation,
   a non-compliant skill report, or an unresolved spec gap.
5. Resolve any spec gap a dispatched agent reports (STEP 2) **before** letting
   the phase be considered done. A phase is not sound — and this command does
   not move on — until every gap opened during it is resolved or explicitly
   escalated to the user, not just recorded.
6. Communicate with dispatched agents in **English** (full technical detail).
   Communicate with the user in **Arabic**, concisely, to help them decide —
   not a narration of tool calls.

---

## STEP 0 — Locate module & resume point

1. `python3 scripts/gov-module.py --track backend state-init {MODULE}` (adds any channel or row
   list the contract requires; keeps every row), then
   `python3 scripts/gov-module.py --track backend delivery {MODULE}` — per unit `accepted` /
   `FAILING` / `not run`. That is the progress record: a unit is done when its
   `package_results` row has `tests_failed: 0`. Read the open rows of
   `plan.state_file` too (`api_doc_gaps`, `blocked`, `deferred_xm`).
2. If a `UNIT|PHASE` argument was given, use it (still skipping accepted units).
   Otherwise start at the first unit, in the order of work above, that is not
   `accepted`.
3. Read this module's own `.claude/commands/{MODULE}/execute-backend.md` for its
   Unit Map, weights and module-specific constraints (those substantive rules
   still apply — this command overrides only the *session-granularity and
   phase-gating* behavior above, not which files never get touched or how errors
   route).
4. **Skill orientation — mandatory, once per run, before the first phase
   assessment.** The backend skills at `.claude/skills/<skill>/SKILL.md` are the
   single binding source of rules — split into `build-*` (generate code) and
   `gov-*` (validate it), plus `api-verify` and the upstream `spring-*` skills;
   the routing table is in `CLAUDE.md` ("Skill routing"). Read the ones whose
   declared scope covers this module's work, in full. Do not assume "matching real code precedent" covers this —
   precedent tells you what the codebase currently does, not whether that pattern
   is what the skills prescribe (precedent can itself be non-compliant; only
   reading the skill catches it). The architecture rules live INSIDE the skills:
   domain-class ownership of business rules in `build-create-entity` +
   `gov-enforce-backend-contract`; layering (controller, service, domain, port,
   adapter, repository), the ProblemDetail error contract and the
   `{MOD}-{http}[-{SLUG}]` code format in `gov-enforce-backend-contract` +
   `gov-enforce-error-handling`; the §12 guardrails (G1–G14) and in-process
   module boundaries in `gov-enforce-library-contract`. A skill outranks
   codebase precedent, never the reverse.
   This is a one-time read for the run — STEP 1.1 still requires identifying,
   per sub, exactly which skill files that sub's work triggers.
5. Print the phase assessment (format above) and wait for confirmation before
   dispatching anything. (In `--auto`, print it but do not wait — see the
   phase-gate rule.)

---

## STEP 1 — Per-sub dispatch protocol

For each not-yet-accepted sub in the confirmed phase, in the order of work
(Step 0):

### 1.1 — Orchestrator prep (read-only, in this session)

Before writing the dispatch prompt, read:
- **This command file itself, in full, again — before every single sub**, not
  just once at the start. Long multi-sub runs are exactly where instruction
  drift happens: a rule easy to follow at sub 1 silently stops applying by sub
  20 as your recall of "what this command says" degrades. This file on disk is
  authoritative over your memory of it; re-read it fresh every time.
- The unit's own `.md` (`plan.exec_units[].file`) and its manifest (`manifest`
  → `tests`, the acceptance set), the phase's `*-HEADER.md` beside it, and
  `_SECTIONS.md` at `plan.exec_packages` (plan content outside every phase).
  Plan facts are NAMED yaml blocks (```` ```yaml name=<block> ````) — in
  `_SECTIONS.md` or the unit's `.md`: `error-catalog` (codes, HTTP status,
  English messages), `totals`, `self-check`, `xm-register`. Point the agent at
  the named block, never at a prose table.
- **The API document** (`plan.api_spec`): every operation this unit builds, by
  `x-api-id` — method, path, parameters, request/response schemas, each error
  response's `x-error-codes` (ProblemDetail), `x-traces`. It is the contract the
  backend implements (no security scheme — amendment A2).
- **The db-script** as of the delivered version: the latest
  `P2/db-script-<mod>.md` among `governance/shared/analysis/modules/{MODULE}/`
  (v1) and its `vK/` folders with K ≤ `plan.delivered_version` (module-suffixed;
  `ls` rather than assuming a bare `db-script.md`). Its DDL is ONE ```` ```sql ````
  fence and its field matrix the ```` ```yaml name=dbf-matrix ```` block
  (`id, table, column, type, entity_field, traces`) — the authoritative source
  for table and column names, constraint names, and types. A spec
  block naming a field is a plan; the db-script is schema ground truth. Never
  invent a column name. Referred to below as **the db-script**.
- **The SRS** (`P1/srs-<mod>.md`, same version rule) for the business rules
  (RULE-IDs) and `AC-*` this sub must satisfy — authoritative for behavior.
- **The acceptance tests**: each id in the unit's `tests` — a `TC-*` is defined
  by its `<!-- TC:<id>:START -->` block in `plan.test_plan` (P4) or the test
  package unit holding it; an `AC-*` by its Given/When/Then in the SRS.
- **Skill compliance (mandatory — not satisfied by precedent-matching alone).**
  1. Cross-reference this sub's work type against the skills index from STEP 0.4
     and list, explicitly, every skill file this sub triggers before writing the
     dispatch — err toward more, not fewer. Backend triggers: ALWAYS
     `gov-enforce-backend-contract` and `gov-enforce-library-contract`; a
     new/modified entity → `build-create-entity` (plus its domain class for any
     business rule); repository, DTO, service, controller, port or adapter →
     `gov-enforce-backend-contract` (no `build-*` skill exists for those layers
     — the unit's spec is the generator); the model port / MCP query channel →
     `spring-ai` (its LOCAL PROJECT NOTE first); `aias.*` configuration
     properties → `spring-boot`; caching → `gov-enforce-caching-rules`; error
     handling → `gov-enforce-error-handling`; tests → the matching
     `spring-*-testing` skill.
  2. Read every listed skill file **in full** — not skimmed, not assumed from
     its name.
  3. Precedent-matching answers "what does the existing code look like"; this
     step answers "is that code — and the precedent you're about to copy —
     actually compliant." Both must be answered; neither substitutes for the
     other.
  4. If a skill's prescribed pattern conflicts with real precedent, do NOT
     silently pick a side — surface it to the user before/with the phase
     assessment. A consistent codebase-wide divergence from a skill is often the
     right call, but it's the user's to bless, not this orchestrator's to
     assume.
- **The real code precedent to mirror.** Before inventing any structure, find an
  already-accepted analog and copy its exact shape: an earlier accepted sub in
  this module (closest by entity shape — flat vs. self-referencing), or failing
  that an already-built sibling module's equivalent
  entity/repository/service/controller/mapper/DTO set. Real, consistent
  precedent in the checked-out code wins over an aspirational structure diagram
  when they disagree.
- Whether this sub needs something an **earlier sub in this phase** already built
  (a shared repository method, mapper, lookup, or cross-entity FK helper). If
  so, name the exact file/symbol to reuse and instruct the agent to import it,
  not duplicate it — and, if it doesn't exist yet, to ADD it there (additive
  only, one new method/export, nothing else in that file touched).

### 1.2 — Dispatch (Agent tool, one sub, `run_in_background: false`)

Write a fully self-contained English prompt — the dispatched agent has no memory
of this conversation. It MUST include:

- **Repo root**: the backend repo's absolute path (derived this run). It's a git
  repo, no worktree needed, work on the current checkout. The agent NEVER
  consults or touches a frontend repo — it does not exist for this work. Every
  `gov-module.py` call the agent makes passes `--track backend`.
- **What NOT to touch**: every other module/phase/sub's output files; any file
  from an earlier sub in this phase (except the one specific additive change
  named in 1.1, if any — name that file and say "the ONLY change allowed here is
  X"); any controller/route/config not this phase's spec explicitly requires
  wiring into; `scripts/gov-module.py` and anything under `governance/shared/`
  outside this track's partition.
- **The exact files to read first, in full** (the ones identified in 1.1:
  HEADER, sub spec, the db-script, SRS slice, the named skills, the precedent)
  — **each as the full repo-relative path `plan --json` gave you this run**
  (e.g. the unit's `file`, `manifest`, `plan.api_spec`, the db-script). The
  dispatched agent has none of this session's bindings, so a bare `packages/…`
  in its prompt is a path it has to guess at — and the tree it would guess (the
  repo root) has no governance in it at all.
- **The "don't build a competing implementation" check**: before writing code,
  confirm whether the thing this sub needs already exists (an entity/repository/
  service/controller for this resource). If it exists: integrate/modify it,
  never create a competing new one. If genuinely absent, flag it in the report
  and implement it as a minimal explicit addition.
- **Contract ground-truth rule (backend)**: the API document (`plan.api_spec`)
  is the contract the backend implements — paths, verbs, schemas, status codes,
  error codes; the db-script (schema), the SRS (rules) and the unit's own spec
  block complete it. The api-docs are generated FROM the code afterwards and
  held to the API document by `api-verify`. The agent NEVER invents a contract
  detail those sources don't give, and NEVER consults the frontend. If a needed
  detail is confirmed absent or contradictory across them, it records ONE gap
  (`gov-module.py record … api_doc_gaps`, below) and continues with everything
  else; it does NOT guess. Only the orchestrator (STEP 2) runs a gap to ground.
- **OQ-blocked items**: skip, note in the report, write
  `// TODO: OQ-[ID] — pending resolution` only if the spec explicitly names that
  OQ ID for that exact field/behavior — never invent one — and record it:
  `python3 scripts/gov-module.py --track backend record {MODULE} blocked --key OQ-[ID] --resolution "OPEN — <what is missing>"`.
- **XM-ID prohibition**: an XM-ID is a governance marker, not code — never write
  an XM-ID reference anywhere in the code. Cross-module dependencies (the
  integration packages) are implemented via the established service/interface
  layer, not by emitting an XM-ID; if one seems needed in code, stop and flag it.
- **No parallel/competing mechanism for an owned responsibility** — use the
  established controller / service / domain / port / adapter / repository
  layering and the published contract interfaces between modules (in-process,
  one deployable); no bypassing it with ad-hoc queries, no second HTTP/data path.
- **aias non-negotiables** — no caller authentication, permission model or
  authorization annotation (deferred by amendment A2); the §12 guardrails
  (domain-profile G1–G14) hold in every unit: the LLM gets no tool that runs SQL
  or triggers approval, host data only through the read-only query port with
  bound parameters, file paths validated inside the storage root, nothing
  skipped silently, document content passed as delimited data, the per-check
  limits (`aias.check.*`) applied, no state carried between checks.
- **Skill compliance instruction**: name the exact skill file(s) from 1.1 by
  path, instruct the agent to read each in full before writing code, and to
  check its work against that skill's own "Verify before finishing" / violations
  list. A dispatch that never names a skill file means that step was skipped.
- **Validation step**: compile/build the module (`mvn -q -DskipTests compile` at
  minimum) and run the validating `gov-enforce-*` skills that apply — report the
  real result, never claim success without running it.
- **Acceptance tests**: write one test per id in the unit's `tests` (named or
  tagged with the id), run them, and report passed/failed per id. A unit of a
  phase with `no_tests: true` has none — its acceptance is the clean build.
- **Recording, precisely scoped — through the tool, never a hand edit of
  `execution-state.json`**:
  ```bash
  python3 scripts/gov-module.py --track backend record {MODULE} package <UNIT> --passed <N> --failed <N>
  ```
  If (and only if) a new gap was found, ONE row:
  ```bash
  python3 scripts/gov-module.py --track backend record {MODULE} api_doc_gaps --key "<METHOD> <path> or the field/contract in question" \
      --resolution "OPEN — <type: MISSING_IN_DOCS | NAMING_MISMATCH | MAPPING_GAP | ABSENT> pending spec clarification" \
      --detail "<phase/unit> · what was missing/wrong · what was done instead"
  ```
  A resolution must START with one of OPEN, DEFERRED, PENDING, RESOLVED, CLOSED,
  IMPLEMENTED, HUMAN, ADR — the tool refuses anything else ("blocked pending …"
  is not a resolution). Nothing else in the state file may be touched.
- **No `git commit`/`git push`** — leave changes in the working tree.
- **Required report-back format** (cap word count, keep it scannable): sub
  completed y/n; files created/changed (exact paths, plus an explicit "did NOT
  touch X" where ambiguity was possible); contract discrepancies vs. the spec
  and how resolved; any `api_doc_gaps` added; any OQ-blocked items; validation
  result; **skill compliance** (which skill file(s) checked, and for each:
  compliant or a named, justified deviation — not silence); and the exact scope
  of each `gov-module.py record` call it made (and its passed/failed counts).

### 1.3 — Orchestrator verification (after the agent reports, before the next sub)

Do this yourself, read-only, in this session:
- `git status --short` in the repo — confirm the changed-files list matches
  exactly what the agent claimed, nothing more.
- Re-run the validation command yourself if cheap (`mvn -q -DskipTests compile`),
  or at least read the agent's own run output critically.
- `python3 scripts/gov-module.py --track backend delivery {MODULE}` and
  `python3 scripts/gov-module.py --track backend validate {MODULE}` — confirm the unit's row is
  there with the claimed counts, nothing else was disturbed, and the file holds
  the contract.
- Confirm the agent's report actually names the skill file(s) it checked and
  states compliant-or-deviation-with-reason for each — a report silent on this
  means the skill step was skipped, not that no skill applied; do not accept the
  sub as done on such a report. Send it back instead.
- If anything is off, send a follow-up to the same agent (by `agentId`, via
  `SendMessage`) to fix it in place — keep every code change attributable to a
  dispatched session, never patch it yourself.

### 1.4 — Impasse: second-agent debate before any escalation

**Trigger** — any one of:
- a dispatched agent reports it is blocked, or cannot complete its sub cleanly;
- STEP 1.3 verification reveals a failure the same-agent follow-up could not fix;
- STEP 2's mechanical gap resolution reaches step 5 (ABSENT — unresolved from
  the db-script, the SRS, cross-module artifacts, or existing source).

**An impasse is never escalated to the user on one agent's word.** Before any
escalation, put a SECOND agent on it and let the two converge. This is the same
philosophy as STEP 4's coverage debate and STEP 5's fixing agent: a second,
independently-reasoning agent grounded in the analysis files, not a human
interrupt, is the first response to a stall.

1. **Dispatch a SECOND agent** (`Agent` tool, read-heavy — it analyses, it does
   not write code) briefed with ALL of this module's analysis files:
   - the PRD (`P0_5/prd-<mod>.md`) and the SRS (`P1/srs-<mod>.md` — `RULE-ID`s /
     `AC-*`), under `governance/shared/analysis/modules/{MODULE}/` (latest
     version folder ≤ `plan.delivered_version`). The PRD is stage P0_5, NOT P1;
     `P0/` holds business-policies and the module registry;
   - the db-script (its ```` ```sql ```` fence and `dbf-matrix` block);
   - `plan.exec_plan`, `plan.test_plan`, `plan.api_spec`, this sub's own unit
     `.md` and manifest, the phase's `*-HEADER.md`, and `_SECTIONS.md`;
   - the exact skill files this sub triggers, from `.claude/skills/` (the list
     built in 1.1);
   - PLUS the first agent's FULL account of what it attempted, what it observed,
     and precisely where it stalled.

   Its job: independently reason the impasse against those artifacts and propose
   one concrete, grounded resolution — traceable to a real artifact line, not an
   opinion. It reads only `.claude/skills/` and the artifacts `plan --json` names
   for this module; it needs nothing else.

2. **Let the two converge.** The orchestrator relays between them — a follow-up
   to the first agent by `agentId` via `SendMessage` carrying the reviewer's
   proposal, or a dispatch of the reviewer with the first agent's report as its
   input — until they agree on ONE concrete solution traceable to a real
   artifact. **Bounded, never open-ended**: if an exchange produces no new
   artifact-grounded evidence, the debate has not converged — stop it and move
   to step 5 rather than looping.

3. **Same invention ban as everywhere else.** The debate's job is to FIND the
   grounded answer, never to fabricate one. No agreed "solution" may invent a
   contract, column, route, endpoint, error code, or business rule. If the
   artifacts genuinely do not contain it, that is an ABSENT case — step 5 — not
   something the two agents may settle between themselves.

4. **Apply the agreed solution ONLY via a dispatched agent session** — the
   orchestrator still never edits code itself — then verify it per STEP 1.3
   before continuing to the next sub.

5. **Escalate to the USER only if** the debate could not converge AND the missing
   piece is genuinely human-only: a business decision stated in no artifact, an
   external credential/value, a real-world fact absent from every source. State
   exactly what is needed and why it is unreachable from the artifacts, in Arabic
   per the communication rule, and wait. In `--auto`, THIS is the HALT point —
   the debate runs first; the halt applies only when the debate also cannot
   ground the answer.

---

## STEP 2 — Spec-gap resolution (same repo only — never reaches into frontend)

Trigger: a dispatched agent's report includes a new `api_doc_gaps` entry, or you
notice one still open from a prior sub. A gap must be run to ground before the
current phase is considered safe to build on — do not carry an unresolved gap
into the next phase.

Resolution stays entirely inside THIS backend repo — the backend is the source
of truth for its own contracts; there is nothing to ask the frontend. Work the
gap in this order:

1. **The db-script** (its `dbf-matrix` block and ```` ```sql ```` fence) — is the field/column actually
   defined, under a different real name or suffix? If so it's a NAMING_MISMATCH:
   correct this side's understanding (spec/comment wording) and proceed.
2. **The SRS and the API document** — does a RULE-ID, or the operation in
   `plan.api_spec`, specify the behavior the spec left implicit? If so it's
   MISSING_IN_DOCS resolved from there; document it.
3. **A cross-module dependency** — if the gap is a contract owned by ANOTHER
   backend module, read that module's published contract
   (`platform/contracts/contract-<target>.md`), its API document
   (`gov-module.py plan <TARGET> --json` → `api_spec`) and its published
   `api-docs/` to confirm the real contract. A read inside `governance/shared`,
   never a cross-repo one.
4. **Existing backend source** — an already-built sibling entity's
   entity/mapper/service may already implement the analogous field correctly;
   mirror it (record it as resolved via source).
5. **Genuinely absent / contradictory** — if none of the above resolves it, it's
   ABSENT: do NOT guess and do NOT invent a contract. **First run the STEP 1.4
   second-agent debate on it** — a second agent, briefed with ALL of this
   module's analysis files plus what steps 1–4 already checked, reasons the gap
   independently; if the debate grounds an answer in a real artifact, apply it
   via a dispatched agent session and record that as the resolution. Only if the
   debate ALSO fails to ground it, escalate to the USER with the exact gap and
   what you checked, and leave the `api_doc_gaps` entry open with `resolution`
   describing the state. In `--auto`, that post-debate escalation is a HALT.

Whatever the outcome, re-record the gap under the SAME key with a factual
resolution — `RESOLVED — <what was found, where>` when settled in code,
`HUMAN — <the decision needed>` when escalated, `ADR — <id>` when a decision
record settles it (`gov-module.py record {MODULE} api_doc_gaps --key "<same key>"
--resolution "…"` replaces that row; the factory reads it with `gov.py feedback`
and answers open ones). Never delete a row; sweep for any stale code comment
that still describes the field as an open gap. Only once every gap opened in the
current phase reads resolved (or is explicitly escalated) may the phase be
presented as done.

---

## STEP 3 — Phase closure and hand-back to the user

When the last sub in a phase completes and every gap is resolved:
1. Own final sweep: `git status --short`, a full compile/validation run,
   `gov-module.py delivery {MODULE}` (every unit of the phase `accepted`),
   `gov-module.py validate {MODULE}`, and the `api_doc_gaps` / `blocked`
   resolutions.
2. Report to the user in Arabic, concisely: what got built, any corrections
   applied, any gaps found-and-fixed (with the real root cause, not just
   "resolved"), any non-blocking issues worth flagging for later — help them
   decide, don't just narrate.
3. Print the next phase's assessment and wait for explicit confirmation before
   dispatching anything in it. (In `--auto`, print it and proceed automatically
   — unless this phase closed on a halt condition, in which case wait regardless
   of `--auto`.)

---

## STEP 3.5 — Integration packages (after the last ordinary phase closes)

The profile's integration phase (`plan.phases[]` with `integration: true`) is
NOT built as an ordinary phase. Each XM edge is ONE integration package:

```bash
python3 scripts/gov-module.py --track backend requires {MODULE}
```

For each `plan.integration[]` edge, in order:
- **`requires_met: true`** → dispatch it like any sub (STEP 1.1–1.4): the agent
  reads the package (`manifest` → `XM-…/XM-….md`: target, type, contract, its
  `do` steps such as a `ddl_patch`), the target's published contract
  (`plan.contract` of the target / `platform/contracts/contract-<target>.md`), and
  builds it through the established cross-module layer; writes and runs the
  package's `tests`; records
  `python3 scripts/gov-module.py --track backend record {MODULE} package <XM-ID> --passed N --failed N`.
- **`requires_met: false`** → do NOT build it. Record and move on:
  ```bash
  python3 scripts/gov-module.py --track backend record {MODULE} deferred_xm --key <XM-ID> --resolution "DEFERRED — <requires> not met"
  ```
  A deferred edge is picked up by a later run that sees it met.

Integration packages are excluded from the module's own delivery; a deferred one
never holds the module open. (`gov-module.py delivery` still prints their lines —
read those as integration status, not as module acceptance.)

## STEP 3.6 — Feature prompts

For each `plan.features[]` entry with `done: false`, in order: dispatch ONE
agent with the prompt (`file`) — it is a self-contained execution prompt; the
frozen plans carry a FEATURE LOG line naming it — under the same skill,
test and verification discipline (STEP 1.2–1.4). When it is green:
`python3 scripts/gov-module.py --track backend record {MODULE} feature <FEAT-ID>`.

---

## STEP 4 — Test phase (a real gated phase, entered in the same run)

Trigger: every ordinary exec unit is `accepted`, STEP 3.5 / 3.6 are done (built
or deferred), and every gap opened so far is resolved (never
escalated-and-still-open).

> **The test phase is a gated phase.** It is entered behind the explicit human
> "Proceed?" gate, then runs test → coverage debate → fix as one continued
> flow (steps 3–5), halting only on failure or on human-only input it cannot
> reach. Code review (`debate-review`) and anything after it stay SEPARATE,
> manually-run steps — named at the end, never dispatched from here.

1. **Read the test phase.** `plan.test_units` (the P4 backend test packages —
   each with its `.md` and `tests`) and `plan.test_plan` (the P4 backend test
   plan, whose `TC-*` blocks are the REQUIRED COVERAGE together with every id in
   the units' and built integration packages' `tests`). Assume no fixed shape or
   unit name.

2. **Print the SAME phase-assessment block** used for every execution phase, and
   wait for explicit confirmation (respect `--auto` identically: print but do
   not wait; still halt on any failure):
   ```
   ══════════════════════════════════════════════════════
   PHASE ASSESSMENT — [MODULE] / TEST
   ══════════════════════════════════════════════════════
   Test units   : [list each test unit + its TC count]
   Gated by     : every exec unit accepted (gov-module.py delivery)
   Plan         : run /[MODULE]/execute-backend-test → api-verify → debate coverage
                  (2nd agent) → fix agreed fail/blocked → close
   ══════════════════════════════════════════════════════
   Proceed?
   ```

3. **On confirmation, dispatch `/[MODULE]/execute-backend-test`** as this run's
   next phase, using the SAME one-dispatch / wait-for-report / verify discipline
   as a sub (STEP 1.2 dispatch, STEP 1.3 verification). That command builds and
   runs the test units (recording a `package` row per unit), then — BEFORE any
   API verification — regenerates and publishes this module's api-docs into
   `governance/shared/backend/modules/[MODULE]/api-docs/` via
   `/generate-api-docs` (never verify against a possibly-stale copy), then
   invokes the `api-verify` skill (`.claude/skills/api-verify/SKILL.md` — the
   delivered api-docs held to `plan.api_spec`; TestSprite is not part of aias,
   never dispatch it), records
   `python3 scripts/gov-module.py --track backend record [MODULE] api_verify --version <plan.delivered_version> --result PASS|FAIL`
   (PASS only when the script exits zero — the frontend's delivery of this
   version stays OPEN until this row is PASS), and emits the governed-plan ↔
   tests coverage table. Read that report and that table.

4. **Coverage decision — adopt a second agent to debate it.** Do not accept the
   run's verdict as final on its own. Dispatch a SECOND agent (read-only,
   separate from the one that produced the report) whose only job is to review
   this module's ANALYSIS files against the coverage table and challenge the
   verdict:
   - It reads the analysis sources — the PRD (`P0_5/`), the SRS (`P1/`, `AC-*`),
     `plan.exec_plan`, `plan.test_plan`, `plan.api_spec`, and the integration
     packages (their `XM-*` and the `tests` they carry).
   - For every claimed GAP it asks: is this genuinely uncovered, or already
     exercised by another test under a different id? For every claimed PASS it
     asks: does the `AC-*`/`XM-*` behind that `TC-*` actually get verified, or
     only touched superficially? An id of a deferred integration package is
     DEFERRED, not a gap.
   - The two agents exchange until they converge on ONE agreed set of real
     failures / real coverage gaps / blocked items. Only that agreed set is
     written to the report as the verdict — a disputed "gap" that the debate
     resolves as already-covered is struck; a "pass" the debate finds hollow
     becomes a gap. This is the correct-decision step, not a rubber stamp.

5. **After tests: continue with a fixing agent on the agreed fail/blocked set.**
   If the agreed verdict (step 4) is clean, skip to step 6. Otherwise do NOT
   just halt — hand the report's agreed FAIL + BLOCKED + GAP items to a fixing
   agent that continues the work:
   - It fixes application code for real failures and fills real coverage gaps,
     following the binding skills in `.claude/skills/` (a skill outranks
     codebase precedent), then re-runs the affected tests via
     `/[MODULE]/execute-backend-test` (re-recording the affected `package` rows
     and the `api_verify` row) until the agreed set is closed.
   - It derives everything it can from what it can reach — the analysis files,
     the code, the skills, `plan --json`, the state file. It works autonomously
     on all of that without pausing.
   - **It stops and asks the human ONLY for information it genuinely cannot
     reach** — a business decision not stated in any analysis file, an external
     credential/value, a real-world fact absent from every artifact. It never
     stops to ask for anything derivable from the sources above. When it does
     stop, it reports in Arabic per the communication rule, states exactly what
     human input it needs and why it is unreachable, and waits.
   - Any item that cannot be closed without that human input is recorded
     (`record {MODULE} blocked --key <id> --resolution "HUMAN — <decision needed>"`)
     and the run halts there rather than presenting the module as done.

6. **Only when the agreed set is fully closed** (`gov-module.py delivery
   {MODULE}` shows every exec and test unit accepted and the `api_verify` row is
   PASS for `delivered_version`), run the end of work and print the closing
   banner naming the remaining SEPARATE, manually-run steps — do NOT dispatch them:
   ```bash
   python3 scripts/gov-module.py --track backend validate {MODULE}
   ./scripts/governance push          # stages only this track's partition
   ```
   ```
   ══════════════════════════════════════════════════════
   MODULE COMPLETE — [MODULE] v[delivered_version]
   ══════════════════════════════════════════════════════
   Implementation : every exec unit accepted (plan.phases order)
   Integration    : [XM built / deferred]
   Features       : [FEAT executed / none]
   Tests          : every governed id covered & passing (coverage table in report)
   api-verify     : PASS (recorded)

   Next steps (run separately, when ready — NOT auto-run from here):
     Code review : debate-review --local
   ══════════════════════════════════════════════════════
   ```

---

## Constraints (non-negotiable, every sub, every phase)

- NEVER treat this as anything but a BACKEND command — no frontend concept, no
  cross-repo reach into a frontend repo, ever. The one sanctioned cross-repo
  artifact in the ecosystem (the published module registry) is the tooling's
  concern, not this orchestrator's.
- NEVER skip the phase-assessment gate, and never advance a phase without the
  user's explicit instruction — the ONLY exception is `--auto`, which
  auto-advances between CLEAN phases (still printing each assessment) and still
  HALTS on any sub error, validation failure, silent/non-compliant skill report,
  or unresolved gap — in each case after the STEP 1.4 second-agent debate fails
  to resolve it, never before running that debate — and never skips the per-sub
  skill read or STEP 1.3 verification.
- NEVER dispatch more than one sub's agent at a time, even for LIGHT subs, even
  when they look independent.
- NEVER escalate an execution-time impasse to the user before running the STEP
  1.4 second-agent debate on it; and never let that debate run unbounded — it
  converges on an artifact-grounded solution, or it ends and the impasse
  escalates. The debate weakens no stop condition: it is an extra attempt before
  the halt, and the halt still stands when it fails.
- NEVER invent a route path, entity/field/column name, endpoint, or error code
  — trace every value to a real spec block, the db-script, or SRS entry; raise
  a gap or an OQ instead of guessing.
- NEVER add caller authentication or a permission model (amendment A2), and
  never weaken a §12 guardrail to make a sub pass.
- NEVER redesign an entity/repository/service/controller that already exists.
- NEVER write an XM-ID reference in code.
- NEVER dispatch a sub without first identifying (against the skills index from
  STEP 0.4) and reading, in full, every skill file that sub's work triggers.
  Matching real code precedent is necessary but not sufficient — precedent can
  itself be non-compliant, and only an actual skill read catches that. This
  discipline was learned the hard way on a frontend-side run of this same
  ecosystem (2026-08-29: 30 subs dispatched with zero skill files read,
  discovered only when the user asked — `create-forms`' RHF requirement and
  `enforce-permissions`' Layer-3 `can()` check were both silently violated
  module-wide, on an earlier project). The incident was frontend, the discipline is identical here —
  don't wait for a backend repeat to take it seriously.
- ALWAYS record every sub through `gov-module.py record` (its `package` row, or
  `feature`, or `deferred_xm`), scoped exactly as described — never let two subs'
  rows land in one dispatch, and never hand-edit `execution-state.json`.
- NEVER build an integration package whose `requires` is not met, and never
  build the integration phase as an ordinary phase.
- NEVER read a path the tool did not give you this run — `plan --json` is the
  only source of governance paths and of the delivered version.
- ALWAYS keep every code change attributable to a dispatched Claude Code
  session; this orchestrating session reads, briefs, verifies, and reports — it
  does not edit source itself.
- ALWAYS re-read this command file in full immediately before every sub's STEP
  1.1 prep — never rely on memory of it from earlier in the same conversation,
  no matter how many subs deep the run is.
