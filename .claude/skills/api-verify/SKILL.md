---
name: api-verify
description: "API VERIFICATION (post-implementation). Holds an aias backend module's published api-docs to its API document (api-spec-<mod>.yaml, OpenAPI 3.1): generates one runnable script that exercises the real API in dependency order, plus a problems report, runs it, and records the api_verify row. Standalone, on demand, never a gate. Use after a backend module's endpoints are implemented and its api-docs are regenerated and published."
---
Every path below comes from the factory's v7 consumer contract through one tool —
read it, never type it. This repo's directory name is not `backend`, so every
call passes `--track backend`:

```bash
python3 scripts/gov-module.py --track backend pin                    # exit 3 = pre-v7 project → STOP ("the factory owner runs gov.py upgrade-project")
python3 scripts/gov-module.py --track backend plan <MOD> --json      # api_spec · api_docs · test_plan · exec_units/test_units/integration (tests) · delivered_version · state_file
```

`<PART>` below is this track's partition, `governance/shared/backend/modules/<MOD>/`
(the folder holding `plan.state_file`).


# Skill: api-verify

## Description

**API VERIFICATION.** The API document (`plan.api_spec`) is the contract; the api-docs this
repo publishes (`plan.api_docs`) are the delivered surface. This skill holds the second to the
first: it turns them into one runnable script that calls the real API end-to-end in dependency
order, runs it, writes a report of what it found, and records the result as data the factory and
the frontend read (`api_verify` row). It designs no test and invents no rule, message,
dependency, or data value of its own — anything not traceable to an input document is skipped
and named as skipped.

This skill is **translation, not derivation**, and it is **module-agnostic** — it runs
identically for any aias `<MOD>` (REG, DOC, CHK, RPT, INT). Nothing about a specific module is
hard-coded here; every fact that can change — base path, response shapes, error format, output
location — is read from the module's API document and the conventions in §2, never typed into a
generated script by hand.

## When to Use

- A backend module's endpoints are implemented and its api-docs were just regenerated and
  published (`/generate-api-docs <MOD>` → `<PART>api-docs/`)
- Post-implementation verification against the real running API — not a code review, not a
  substitute for `gov-enforce-backend-contract` / `gov-enforce-library-contract`
- On demand, invoked explicitly for a given `<MOD>` — never automatically, never as a gate

## When NOT to Use

- Before the module's api-docs exist or are stale — regenerate them first
  (`/generate-api-docs`); a stale doc produces a script that tests the wrong surface. While the
  api-doc-generator copy in this repo cannot run (see `/generate-api-docs`), there are no
  api-docs: STOP and say so
- Without an API document (`plan.api_spec` missing — a pre-v7 project): there is no contract to
  hold the docs to; STOP and say so
- As a substitute for `gov-enforce-backend-contract` / `gov-enforce-library-contract` — those
  review code and architecture; this skill exercises the live HTTP surface
- To design new test cases from scratch — the factory's P4 backend test plan
  (`plan.test_plan`) and the packages' `tests` define them; this skill only consumes what the
  API document, the test plan and the api-docs already established

## Responsibilities

- State the inputs read (API document, api-docs, test plan) at the start of every run
- Produce one script that exercises every documented operation — the `POST` creates (start a
  check, upload documents, confirm uploads, record a decision) and the `GET` reads — in
  dependency order, and reports failures — nothing else
- Produce a problems report bucketed into *contract mismatch* / *likely real bug* / *test
  assumption mismatch* / *infrastructure*

## Constraints

- MUST NOT invent a business rule, error code, dependency edge, or payload value not present
  in the API document, the api-docs or the test plan
- MUST NOT ask questions — an unresolvable ambiguity (a dependency cycle, an ambiguous
  payload) is an ADR-worthy blocker: state it and stop, do not guess
- MUST NOT change any line artifact, mint any governance ID, or render a gate verdict — this
  runs entirely outside the governed pipeline; its one write into governance is the
  `api_verify` row (stage J), through `gov-module.py --track backend record`
- MUST NOT call an update or delete endpoint — aias exposes none; records the run creates
  remain and are listed (§3-F)
- MUST NOT write to the service's database or to any host system. "Write" here means a
  *direct* write — a SQL statement or file the script issues itself. State changes made by
  calling the service's own documented endpoints are not that: they are the thing being
  verified
- MUST NOT drive the host Approval API against a real host: a decision with an enabled
  Approval API is exercised only against a Dev/Test service package whose approval endpoint is
  a stub (§3-I)
- MUST NOT send real request data or real documents to the model provider — synthetic or
  anonymised requests and documents only (domain-profile G13)
- MUST NOT embed a literal credential or endpoint secret in a generated script — run
  arguments or clearly-marked placeholders only
- MUST NOT report a run as clean while records it created survive: every surviving id is
  listed in the report (§3-F), never summarised as "cleaned up"

## Output

- `<PART>test-api/test_<mod>_apis.py` (or the language the run targets)
- `<PART>test-api/<mod>_problems_report.md`
- one `api_verify` row in `plan.state_file`:
  `python3 scripts/gov-module.py --track backend record <MOD> api_verify --version <plan.delivered_version> --result PASS|FAIL`

---

## 1. Inputs

State them at the start of the run. There is one tier: the API document always exists for a
v7 module, so negatives are never self-derived and never skipped for want of a manifest.

| Input | Read from | Role |
|---|---|---|
| API document | `plan.api_spec` (`api-spec-<mod>.yaml`, OpenAPI 3.1) | **the contract** — every operation by `x-api-id`: method, path, parameters, request/response schemas, each error response (`application/problem+json`) with its catalog codes (`x-error-codes`), traces (`x-traces`). It declares no security scheme (amendment A2) |
| api-docs | `plan.api_docs` (`<PART>api-docs/`: `index.md`, then `**/*.md`) | **the delivered surface** — endpoints as the built backend documents them: verbs, paths, request/response field tables, examples, contract ids |
| test plan | `plan.test_plan` (P4) and the `tests` of `plan.exec_units` / `plan.integration` | the `TC-*` ids a negative or happy path is tagged with — the same id space as the packages' acceptance tests |
| error catalog | the `error-catalog` named block of the backend execution packages (`_SECTIONS.md`) | the trigger, the `RULE-*` and the English message behind each code |

Pair operations and documented endpoints by method + path. An operation with no delivered
counterpart, or a delivered endpoint with no operation, is listed in the report under
*contract mismatch*; the script does not invent a call for it.

Operations of one module may be served through another (INT relays the owners' refusals with
their own codes, ADR-INT-003). Pair by method + path all the same; the code asserted is the one
the API document lists for that operation.

## 2. Stack conventions (aias)

These are the project's facts; the API document states the per-operation detail. Do not
override them in the generated script; if a module's actual behaviour disagrees with them,
that is a contract mismatch (stage G), not a reason to change the script.

| Convention | Value |
|---|---|
| Base path | `/api/v1/{resource}` (no module segment) |
| Verbs | `POST` = create (start a check, upload documents, confirm uploads, record a decision) · `GET` = read. No `PUT`, `PATCH` or `DELETE` |
| Responses | plain JSON objects / arrays — no envelope, no paging wrapper unless the API document declares one |
| Errors | RFC 9457 `ProblemDetail` → `{type, title, status, detail, code}`; `code` = `{MOD}-{http}[-{SLUG}]` (e.g. `CHK-404-CHECK-NOT-FOUND`) |
| Statuses | 200, 201, 202, 400, 401, 403, 404, 409, 413, 415, 422, 500, 502, 504 — `202` = a check accepted and running asynchronously (poll its read until it ends) |
| Authentication | none — no login, no token, no permission (amendment A2) |
| Languages | messages in English; Arabic `PENDING` — assert codes and statuses, never message text |
| Target | Dev/Test only — the script refuses a base URL it cannot confirm as Dev/Test (run argument `--env dev|test`) |

## 3. Processing pipeline

**A0 — Preconditions (before any suite runs).** Every payload value that references something
the run does not itself create — a service code from the registry, a document type of a
service, a host request number the query channel must resolve — is *verified to exist* first,
through a documented read endpoint (e.g. `GET /api/v1/services/{serviceCode}`). A missing one is
reported once, up front, as a precondition failure naming the exact value and the document that
supplied it, and its dependent suites are marked blocked-on-precondition. Never let it surface
instead as a wall of downstream assertion failures: an example value in a governed document is a
claim about the environment, and a claim that is false is a finding about the document or the
Dev/Test data (service packages, host fixtures), not about the endpoint under test.

**A — Inventory.** Parse every operation of the API document (`x-api-id`, method, path, the
resource from the path, operation type from the verb, request/response schemas, error responses
with their codes) and every documented endpoint of the api-docs (verb, path, request field table
— name, type, required, constraints, example — response shape). Pair them (§1).

**B — Dependency order.** Infer edges from identifier fields threaded between operations (a
`checkId` returned by a start and required by an upload, a decision or a report read) and from
the api-docs' descriptions, then sort topologically; roots first (registry reads, then start a
check, then its documents, then its report, then its decision). An asynchronous step (`202`) is
followed by polling its documented read until the documented end state or the per-check timeout
(`aias.check.timeout`) — never a fixed sleep. A cycle or an unresolvable edge → stop and report
it as a blocker, do not guess.

**C — Negative mapping.** For every error response of every operation (`responses.<status>`
with `x-error-codes`): set up the precondition the catalog row describes (the `RULE-*` the
`error-catalog` block cites for that code — cite it, do not restate it), call the endpoint that
must be blocked, assert the HTTP status and the ProblemDetail `code` (never the governance RULE
id), and tag the function with the code and the `TC-*` of the test plan that names the same
operation and code. No such TC → tag the code only and list the operation+code as *untraced
negative* in the report. A code whose precondition cannot be produced through documented
endpoints and Dev/Test fixtures (e.g. `INT-504-APPROVAL-API-TIMED-OUT` without a stub) is listed
as *not exercisable* with the reason — never faked.

**D — Assembly.** One fixed structure (§4), one `test_<resource>()` per resource in stage-B
order, ids threaded as parameters from the producing call's own return value — never
hard-coded.

**E — Exploratory scenarios (bounded).** Only for fields with `required = yes` or a stated
constraint (length, numeric range, file size against `aias.check.max-file-size`, upload count
against `aias.check.max-uploads`): at-limit / one-over, omission, type mismatch, an undocumented
enum value, idempotency where documented (a second decision on the same check), an unknown
`checkId`. **Assert vs observe:** an outcome governed by a RULE + code is stage C's; any other
expected status is undocumented → executed as an *observation* (recorded, never pass/fail,
listed apart in the report). No documented source value → skip the case.

**F — Surviving records.** aias exposes no update or delete operation, so every check, upload
and decision the run creates remains in the Dev/Test schema until the service's own retention
purge removes it. Every created id is tracked per resource, and the report carries a
`Surviving records` section that **lists** them (id, service code, request number used, what
was created) — never a sentence like "cleaned up". Request numbers and other free values the
run sends carry the run's own namespace (e.g. a `RUN-<timestamp>` prefix) so residue is always
attributable. The script never deletes rows itself; a purge is a human's decision, suggested in
the report, never executed.

**G — Problems report.** `<mod>_problems_report.md` lists only failures, bucketed: *contract
mismatch* (the delivered surface departs from the API document — the backend's to fix, or a
gap recorded for the factory: `gov-module.py --track backend record <MOD> api_doc_gaps --key
"<METHOD> <path>" --resolution "OPEN — …"`), *likely real bug* (a documented rejection did not
happen, a report shows `COMPLIANT` with a missing or unreadable required document, or an
unexplained status), *test assumption mismatch* (rejected, but with a status the API document
did not state — the document is the factory's to fix, through a gap, not the backend),
*infrastructure* (connection / timeout / model provider unavailable — rerun). Ambiguous →
*likely real bug*.

**H — Log correlation.** Log excerpts around a failing call are attached as *approximate*
unless a trace id or the Check identifier ties them to the call. Logs never reach the report
with document content, query results or model output in them.

**I — External dependencies.** A check reaches the host query channel (MCP), the document
source and the model provider. The run uses only Dev/Test connections and service packages
whose fixtures are synthetic. A decision on a service with an enabled Approval API is exercised
only when that package's approval endpoint is a Dev/Test stub named in the run arguments;
otherwise the decision suites for such services are listed as *not exercisable* with the
reason. The run never points the service at a real host and never changes a service package or
a connection — those are configuration, not API.

**J — The result reaches the factory as data.** After the script runs, record one row:

```bash
python3 scripts/gov-module.py --track backend record <MOD> api_verify --version <plan.delivered_version> --result PASS   # only when the script exits zero
python3 scripts/gov-module.py --track backend record <MOD> api_verify --version <plan.delivered_version> --result FAIL   # otherwise
```

The frontend's delivery of that version stays OPEN until this row is PASS. Then
`python3 scripts/gov-module.py --track backend validate <MOD>` and `./scripts/governance push`
(with `GOV_TRACK=backend` exported) publish the row and the `test-api/` output.

## 4. Script structure

```
# Intended for Dev/Test environments only.
config      : BASE_URL (argument or localhost default) · --env dev|test (required) · run namespace
              · optional approval-stub URL
client      : thin HTTP wrapper (get/post, multipart for uploads) that parses JSON and ProblemDetail
results     : TestResult / TestSuite records · run() for asserted calls · run_observation() for
              stage-E observations (separate bucket, never in totals)
helpers     : extract_id · problem_code(response) · poll_until(read, end_states, timeout)
preflight() : stage A0 — assert the Dev/Test target, then every externally-owned referenced value
              exists; a miss blocks its dependent suites with a precondition failure, never a cascade
per resource: test_<resource>(threaded ids…) → create (POST) → read (GET ok) → read (unknown id →
              not found) → [stage C negatives] → [stage E observations] ; appends created ids
report      : Markdown (+ optional HTML) with pass/fail suites, an "observations" section, the
              problems buckets, the "not exercisable" list and the "Surviving records" list →
              <mod>_problems_report.md ; never persists document content or model output
main()      : preflight() → suites in stage-B order, all inside try/finally (the report is written
              in finally); exit non-zero on any asserted failure (observations never affect it)
```

Every test function carries a traceability comment: `Covers: API-… ; Negative: RULE-… / <code> / TC-…`.

Payload rules: use the api-docs' `example` values verbatim; a required field without an example
gets a clearly marked placeholder of the right type; enum values only as seen in the docs;
ids threaded, never literal; uploaded files are small synthetic fixtures.

## 5. Generation gate (silent; only failures are reported)

```
[ ] every operation of the API document has a test function, or is listed as not yet served
    (contract mismatch) or not exercisable (with its reason)
[ ] every resource in the api-docs has a test_<resource>() function
[ ] main() order = stage-B order — no forward id reference; async steps polled, never slept
[ ] every payload value comes from an example or a marked placeholder
[ ] negatives only where the API document states status + code — none self-derived; tagged
    with the test plan's TC where one names that operation and code
[ ] exploratory scenarios only on required/constrained fields; undocumented outcomes use
    run_observation()
[ ] no PUT / PATCH / DELETE call anywhere; no direct database or host write
[ ] runtime error codes asserted from the ProblemDetail `code` in the {MOD}-{http}[-{SLUG}] format
[ ] no invented credentials, service codes, document types or enum values; traceability
    comment on every function
[ ] every externally-owned value a payload references is precondition-checked by preflight()
[ ] the report writer emits a Surviving records list from the tracked-id set — not a
    hardcoded "cleaned up" string
[ ] the Approval API is reached only through a Dev/Test stub given as a run argument
[ ] only synthetic or anonymised requests and documents (G13)
```

## 6. Boundaries

| Consumes (read-only) | Produces | Never |
|---|---|---|
| the API document, the api-docs, the P4 test plan, the `error-catalog` block, run arguments | `test_<mod>_apis.py`, `<mod>_problems_report.md` under `<PART>test-api/`, one `api_verify` row (stage J) | a governance ID of any kind, a change to any line artifact, a gate verdict, a direct write to the database or a host system, a call to a real host Approval API |

## Related Skills

| Skill | Purpose |
|---|---|
| [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md) | The layer contract this skill's target code was already required to pass |
| [`gov-enforce-library-contract`](../gov-enforce-library-contract/SKILL.md) | The aias service contract — §12 guardrails and module boundaries |
| [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md) | The ProblemDetail error contract this skill asserts against |
