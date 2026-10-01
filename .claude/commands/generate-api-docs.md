# Generate Backend API Docs

```
Lives at   : .claude/commands/generate-api-docs.md (the aias backend repo), so it
             auto-loads as a Claude Code slash command
Runs       : governance/governance-tools/api-doc-generator/generate.py
Writes to  : governance/shared/backend/modules/<MOD>/api-docs/ — this track's
             partition, as the factory declares it
             (profile-summary.json → consumer.partitions.api-docs.path)
Contract   : the module's API document, `gov-module.py --track backend plan <MOD> --json → api_spec`
```

> **STATUS — the generator cannot run in this repo yet.** The copy under
> `governance/governance-tools/api-doc-generator/` is incomplete: `generate.py`
> fails on import (`security_extractor`, `sync`, the OpenAPI/DTO/validation
> extractors and `renderers/base.py` are missing), and several of its checks
> assume a different backend's conventions (a response envelope, method-level
> authorization annotations, a `common` source root). aias has none of those:
> responses are plain JSON, errors are ProblemDetail, and there is no caller
> authentication (amendment A2). Until a complete generator adapted to aias is in
> place, this command STOPS at Step 0 with that message — never hand-write
> api-docs in its place. The steps below describe the intended flow.

(Re)generates a module's API documentation from the **running backend**, so
the frontend and the `api-verify` skill read documentation that matches the
implementation instead of a stale hand-written copy. Each served endpoint is
stamped with its contract id (`x-api-id`) from the module's API document
(`api-spec-<mod>.yaml`, OpenAPI 3.1, published by the factory beside the
delivered packages), and every declared-vs-served mismatch is reported.

$ARGUMENTS = MODULE

The module code is the only input. Everything else — which springdoc group the
module is, the OpenAPI URL, the module's source root, the output directory, the
API document — is auto-discovered from the repository itself and from
`scripts/gov-module.py --track backend`. Do NOT pass override flags unless discovery actually fails
and names the flag to pass.

## Step 0 — the contract comes from one tool (never typed)

```bash
export GOV_TRACK=backend                          # scripts/governance calls gov-module.py without --track (the generator passes it itself)
./scripts/governance pull                         # start of work
python3 scripts/gov-module.py --track backend pin                 # 0 = schema 7 · 3 = pre-v7 · 1 = refused
python3 scripts/gov-module.py --track backend plan $MODULE --json # api_spec, api_docs, delivered_version, …
```

- `plan.api_spec` — the module's API document: **the contract**. The generator
  reads it itself (`discovery.find_api_spec` calls this same tool) and prints
  `Contract src: API document api-spec-<mod>.yaml`.
- Output: `governance/shared/backend/modules/$MODULE/api-docs/` (`index.md`, then
  `endpoints/*.md`) — the partition the factory declares
  (`consumer.partitions.api-docs.path`). It is never version-suffixed: derived
  from the running application, there is one current set per module.
- This repo writes there and in `execution-state.json` / `test-api/` beside it —
  never in `…/packages/` (the factory's) and never in `analysis/`.
- **Pre-v7 project** (`pin` exit 3): the generator still documents the running
  app, but it has no API document; it then FALLS BACK to the prose API REGISTRY
  of the backend execution plan for contract ids and says `FALLBACK` on its
  `Contract src:` / `Contract ids:` lines. Report that fallback; the factory owner
  runs `gov.py upgrade-project`.

## Preconditions

**Module validation:** the module must be listed by
`python3 scripts/gov-module.py --track backend modules` (on a pre-v7 project, which that tool
refuses: `jq -r '.modules | keys[]' governance/shared/platform/modules-registry.json`)
— never invent a code.

**Backend running:** the Spring Boot app must be up and `/v3/api-docs/<group>`
must answer for this module (springdoc is not on the classpath yet — the module's
packages decide whether and how it is added). The generator reads the real port from
`src/main/resources/application.properties` — never assume `8080`.

## Your Task

### STEP 1 — Review first, always

```bash
python3 governance/governance-tools/api-doc-generator/generate.py \
    --module $MODULE --function review
```

`review` writes nothing. Read its report before changing any file, and report:
endpoints added/removed/updated/unchanged, which shared `index.md` sections
changed, and any conflicts.

### STEP 2 — Then write, picking the mode from what review reported

- **Conflicts reported** (`unmanaged file already exists`) → STOP and show the
  list. Those are hand-written or hand-edited files; the tool refuses to
  clobber them. Ask before overwriting anything.
- **No `api-docs/` yet, or every file reported as added/unmanaged** →
  `--function generate` (full write, stamps every file with the
  AUTO-GENERATED marker).
- **Docs exist and carry the marker** → `--function update` (writes only what
  changed, deletes endpoint files for endpoints the backend no longer has,
  leaves everything else alone).

### STEP 3 — Run the guard, and file every FAIL with an owner

```bash
python3 governance/governance-tools/api-doc-generator/generate.py \
    --module $MODULE --function check
```

`check` prints everything `review` prints, then one line per assertion, each
with its **expected side read from source** and a ratio — never a bare count —
and exits non-zero on any FAIL. Report the block verbatim, then for each FAIL
name the owner and the item you filed. A FAIL is a correct result when the
problem is real; never satisfy it by weakening an assertion or by hand-editing
a generated file. A module that legitimately cannot pass one assertion declares
that once, with a reason, in
`governance/governance-tools/api-doc-generator/waivers/$MODULE.json`
(see the tool's README) — that is the only sanctioned way to silence one.

What each FAIL means, and who can own the fix. Every row lists causes on
**both** sides, because the generator and the backend have each produced every
one of these symptoms at least once — a symptom is a question, not a verdict:

| FAIL | Generator-side cause to rule out | Backend/factory-side cause to rule out | File the item with |
|---|---|---|---|
| `permissions` / `auth-determined` | — | not applicable to aias: there is no caller authentication and no permission model (amendment A2), so a 0/N here is the correct result | waive per module in `waivers/$MODULE.json` with reason "caller authentication deferred — raw-idea A2" |
| `envelope` | — | not applicable to aias: responses are plain JSON objects/arrays, errors RFC 9457 ProblemDetail — there is no envelope to find | waive per module with that reason |
| `status-table` / `error-codes` — 0 read while declared | the generator does not read this module's error-code catalog (`{MOD}-{http}[-{SLUG}]` codes carried in ProblemDetail `code`) | the catalog class was renamed or moved | generator owner first |
| `business-errors` — 0/N bound while source throws | the walk cannot resolve this module's delegate shape (`business_error_extractor`) | services throw only from code the controllers never reach | generator owner first; the `unbound` list names the codes to explain |
| `contract-ids` — API document has no `x-api-id` operation | the line reader in `contract_extractor.parse_api_spec` misses the document's layout | the factory published a document without ids | factory owner |
| `contract-drift` — any row | none: both sides are read verbatim | an endpoint the API document does not declare, or a declared operation nothing serves — the drift table quotes the id a controller Javadoc **claims**, which is a signpost, not a registration | backend owner when the served surface departs from the API document (implement to it); otherwise `gov-module.py record $MODULE api_doc_gaps --key "<METHOD> <path>" --resolution "OPEN — …"` for the factory |
| `stale` | — | the backend changed after the last regeneration | run `--function update`, then STEP 4 |
| `deterministic` | a new extractor iterates a mapping or directory listing unsorted | — | generator owner |

Then give the endpoint count, group count, error-code count, output path and
the per-endpoint change list. If a section is EMPTY and `check` did not fail
on it, say which assertion covered it and why it passed (a legitimately absent
feature is stated as such in the docs themselves — e.g. an endpoint's
authorization line stating that none is declared, which is correct for aias).

### STEP 4 — Publish them (they are NOT published until you do)

`governance/shared` is a **submodule**: a separate repository mounted here.
Files the generator wrote there are untracked in *that* repository and invisible
to everyone else — the factory's `fetch-inputs`, `api-verify` and the frontend
all read the pushed commit, not your working tree. Regenerating and stopping
looks like success and delivers nothing.

```bash
python3 scripts/gov-module.py --track backend validate $MODULE
./scripts/governance push "api-docs($MODULE): regenerated from the running app"
```

`push` gets the submodule off a detached HEAD, stages **only this track's
partition** (`gov-module.py pathspec` — never the factory's `packages/`),
commits, pushes, and commits this repo's pointer. Use it instead of raw `git`:

- **Detached HEAD.** `git submodule update` checks out a *commit*, so a commit
  made there is referenced by nothing and a plain `git push` publishes nothing.
  The script checks out the branch first and says so.
- **Pointer not bumped.** Pushing the submodule without committing the pointer
  here leaves this repo claiming the previous api-docs. The script commits it.

## Constraints (NON-NEGOTIABLE)

- **NEVER hand-edit a generated file under `api-docs/`.** It is regenerated
  output; a manual edit is destroyed on the next run AND turns the file into a
  conflict the tool then refuses to manage. If the docs are wrong, either the
  backend is wrong or under-annotated, or the generator missed something — the
  STEP 3 table says how to tell the two apart; fix the right one and regenerate.
- **NEVER pass `--openapi` / `--source` / `--common-source` / `--output` to
  "make it work."** If discovery fails it names the exact flag and why; report
  that instead of working around it.
- **NEVER invent content for a section the generator left out.** Absent means
  "not discoverable from the implemented backend" — that is information.
- **NEVER fall back to a saved or older OpenAPI JSON.** If the generator
  reports HTTP 500 from `/v3/api-docs/<group>`, that is a backend/springdoc
  fault, not a documentation fault: report it with the response body and stop.
  Docs that look current but aren't are worse than no docs.

## Notes

- Output always lands in this track's own partition —
  `governance/shared/backend/modules/[MODULE]/api-docs/`
  (`index.md` + `endpoints/<group-slug>.md`), read from
  `profile-summary.json → consumer.partitions.api-docs.path`, so the command
  works from any working directory. The generator resolves the layout itself and **refuses**
  rather than guessing when two candidate directories exist — if it says so,
  report that; do not pick one for it.
- `review` is safe to run any time, including in CI, to answer "have the API
  docs drifted from the backend?" without touching a file. `check` is the CI
  gate: the same, plus a non-zero exit on drift, staleness, non-determinism or
  a whole-module silent-empty section (STEP 3).
- Consumers of this output: the frontend repo and the governance factory, both
  of which read **this same single copy** through their own `governance/shared`
  submodule (the factory merges the folder `index.md` first, then `**/*.md`),
  and the `api-verify` skill, which holds these api-docs to the API document
  (`plan.api_spec`) and records `gov-module.py record $MODULE api_verify
  --version <delivered_version> --result PASS|FAIL`.
