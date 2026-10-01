# api-doc-generator

> **STATUS IN THIS REPO (aias backend) — does not run, does not apply as-is.**
> This folder is a partial copy of a tool written for an earlier, unrelated
> backend. Its own modules are missing — `extractors/__init__.py`,
> `openapi_extractor.py`, `dto_extractor.py`, `validation_extractor.py`,
> `security_extractor.py`, `response_model_extractor.py`, `sync.py`,
> `renderers/base.py` — so `generate.py` fails on import. Beyond that, several
> of its checks assume that backend's conventions, which aias does not have: a
> response envelope class, method-level authorization annotations and permission
> codes, a shared `common` source root. aias answers plain JSON, reports errors as
> RFC 9457 ProblemDetail with `{MOD}-{http}[-{SLUG}]` codes, and has no caller
> authentication (amendment A2). The parts that do carry over are the contract
> join (`extractors/contract_extractor.py`: stamping served endpoints with the
> `x-api-id` of the module's API document and reporting drift) and the v7
> discovery of the module's plan and API document through `scripts/gov-module.py`
> (`discovery.py` already passes `--track backend`).
> Until a complete copy is adapted to aias, `/generate-api-docs` stops and says
> so. The rest of this README documents the original tool and is kept as its
> reference, unedited.

A tool that generates frontend-ready API documentation directly from
an implemented Spring Boot module. In its original backend it worked
identically for every module that followed that platform's conventions (a
`GroupedOpenApi` bean per module, a shared `common` foundation), with no
generator change required to support a new one. It works under both layouts this platform has used: the
single consolidated POM it builds as today, and a multi-module Maven reactor. **The
implemented backend is the only source of truth** for what the docs say; the one
governance input is the module's declared contract (its API document,
`api-spec-<mod>.yaml`), read only to stamp each served endpoint with its
contract id and report declared-vs-served drift.

## Purpose

Frontend developers need accurate, up-to-date API documentation. Hand-written
docs drift from the real implementation the moment either side changes. This
tool regenerates documentation from the actual running/compiled backend every
time, so it can never be stale — if it's wrong, the backend is wrong (or
under-annotated), not the doc generator.

## Quick start

```bash
python3 generate.py --module SEC --function generate
python3 generate.py --module SEC --function update
python3 generate.py --module SEC --function review
python3 generate.py --module SEC --function check      # review + assertions; exit 2 on any FAIL
python3 -m unittest discover -s tests -t .             # the extractor tests, fixture Java under tests/fixtures/
```

That's it in normal use — see "Automatic discovery" below for what these two
flags actually resolve to and why nothing else is usually needed.

## Architecture

```
generate.py              CLI entrypoint. Parses --module/--function (+ rare
                         explicit overrides), calls discovery.py, then calls
                         generator.run(). Owns no pipeline logic itself.
discovery.py             Resolves --module into a RepositoryContext by reading
                         real repository artifacts (pom.xml, springdoc group
                         config, application*.properties) -- never a per-module lookup table,
                         and never a WORKSPACE.md (no such file exists in
                         this repo). See "Automatic
                         discovery" below. RepositoryContext is the ONLY
                         object carrying repository-shaped knowledge
                         (locations, which module, which shared resources)
                         across the discovery -> pipeline boundary.
checks.py                `--function check`: the review report plus assertions
                         that each carry an expected side read from source and
                         print as a ratio (permissions found / endpoints against
                         @PreAuthorize in source, ...). Any FAIL exits 2. A
                         module that legitimately cannot pass one declares it in
                         waivers/<MOD>.json with check, reason, by, on -- an
                         invalid waiver waives nothing and is itself a FAIL.
generator.py             The pipeline: extract -> render -> sync.
                         build_document(context) is the only place that
                         RepositoryContext gets unpacked; run(context, mode)
                         and build_document() are plain functions, importable
                         by generate.py or any other caller (tests, a future
                         batch-all-modules script, ...). Every individual
                         extractor still takes its own narrow, explicit input
                         (a Path, a dict, ...), never the context object
                         itself — extractors have no dependency on
                         discovery.py and no way to perform repository
                         discovery themselves, by construction.
models/
  api_doc_model.py       Dataclasses shared by every extractor and renderer.
                         Every field is Optional — absence means "not
                         discoverable", never "invent something reasonable".
extractors/
  openapi_extractor.py   Loads the OpenAPI JSON (file or URL), raising
                         OpenApiLoadError naming the URL tried and the
                         server's own response body on failure. Walks paths/
                         operations into Endpoint objects: method, path,
                         summary/description, tag, path/query/header params,
                         auth requirement + scheme names. Also reads `info.version`.
  contract_extractor.py  Joins the module's API DOCUMENT (api-spec-<mod>.yaml,
                         OpenAPI 3.1 -- each operation's x-api-id, method and
                         path; read with a stdlib line reader, no YAML library)
                         to the served endpoints, stamping each one with its
                         contract id (API-SEC-004, ...) and reporting both
                         directions of declared-vs-served drift. A pre-v7
                         module with no API document falls back to the prose
                         API REGISTRY of its backend execution plan, and the
                         run says FALLBACK.
                         This is what makes the generated docs addressable by
                         the ids every other governance artifact -- SRS,
                         frontend execution plan, P4 test plan -- is written
                         in; without it, resolving an id to a path meant
                         opening a planning document, which states the path
                         that was PROPOSED, not the one that is served.
  dto_extractor.py       Resolves $ref schemas into field lists, recursively:
                         a field whose own type (or array-item type) is itself
                         an object schema with properties gets its own fields
                         attached to FieldSpec.nested (e.g. a search request's
                         `filters: array<ContractFilter>` expands into
                         ContractFilter's field/operator/value). A visited-
                         schema-name guard turns self-referential schemas
                         (e.g. a tree node's own `children`) into a
                         `FieldSpec.recursive_ref` marker instead of expanding
                         forever. Also understands three recurring response
                         shapes: the ApiResponse<T> envelope, Spring's Page<T>
                         serialization, and plain arrays.
  validation_extractor.py  Turns one resolved schema property into a
                         FieldSpec (required / minLength / maxLength /
                         pattern / format / enum) — these are Bean
                         Validation constraints springdoc already translated
                         into OpenAPI keywords; nothing here re-reads Java.
  security_extractor.py  BEST-EFFORT, only runs when a module source root is
                         available. Locates a controller method's real Java
                         source (by re-deriving its route from
                         @RequestMapping/@XxxMapping and matching verb+path —
                         not by guessing off the method name, since names like
                         "create"/"search" repeat across every controller),
                         then looks for @PreAuthorize/@Secured on that method
                         or on the service method it delegates to. Handles the
                         single-line and the multi-line concatenated spelling
                         (any number of "..." + "..." parts), @Secured arrays and
                         `value =`; constants are matched structurally, never by
                         a PERM_ prefix (CU spells them CONFIG_*). Every lookup
                         ends in a recorded outcome -- FOUND, DECLARED_NONE, or
                         one of the NOT_EXTRACTED reasons -- and the docs print
                         which, so "no permission" is never ambiguous.
  business_error_extractor.py  BEST-EFFORT, module source required. Indexes
                         the module's classes and walks each endpoint's call
                         graph controller -> service -> domain (depth 8, only
                         into classes in the module's own source), recording
                         every `(Status, XErrorCodes.Y)` throw site and every
                         ErrorDetail code reached. Overloads that differ in
                         what they throw are skipped as ambiguous rather than
                         picked. Also finds module advisors that catch
                         AccessDeniedException and re-raise a module code, so
                         the 403 row names the code the client really receives.
                         A `fieldErrors[]` detail is reached through a check
                         method that names no Status of its own; its Status is
                         taken from the aggregate throw that actually reaches
                         it on the same walk (`new LocalizedException(Status.X,
                         <collected list>)` / `withDetails(Status.X, ...)`),
                         falling back to the Status the code is registered
                         with. A detail neither reaches keeps no status rather
                         than being given one.
  constraint_extractor.py  Module source + Flyway migrations required. Reads
                         @Table(uniqueConstraints) off the entities and replays
                         the migrations in version order (ADD CONSTRAINT UNIQUE,
                         CREATE UNIQUE INDEX ... WHERE, inline UNIQUE, and the
                         DROPs that retire them), reporting only this module's
                         tables; a partial index's WHERE clause is quoted verbatim.
  exception_extractor.py  BEST-EFFORT, only runs when a module source root is
                         available. Finds every `*ErrorCodes.java` file under
                         the module's own source tree and lists its constants
                         as a module-level "Known Error Codes" appendix, each
                         with its own Javadoc SUMMARY paragraph (markup
                         removed, block tags and the discussion after the
                         first `<p>` dropped) as the consumer note -- that is
                         where a module states when a code is raised and what
                         makes it stop being raised.
  message_bundle_extractor.py  Reads the application's own i18n bundles
                         (spring.messages.basename) and gives each error code
                         the message the API actually answers with, keyed by
                         the code's own VALUE -- the wire code doubles as the
                         message key on this platform. One column per locale
                         that has content; a code with no bundle entry renders
                         as an em dash, never as an invented or borrowed
                         message.
  response_status_extractor.py  BEST-EFFORT, module source required. springdoc
                         declares `200` for every operation that carries no
                         `@ApiResponse`, and no controller here declares one,
                         so every create published as 200. Resolves the real
                         success status structurally: controller -> its single
                         delegate -> that method's `ServiceResult` factory
                         calls, with the factory's own default Status read
                         from the wrapper class's source and an explicit
                         `Status.X` at the call site overriding it, then
                         through the same shared Status -> HttpStatus table
                         error_mapping_extractor already reads. Two different
                         outcomes in one method resolve to nothing rather than
                         to whichever came first, and the published line names
                         the constant and method it was read at.
  error_mapping_extractor.py  BEST-EFFORT, only runs when shared/common
                         source roots are available. Also reads what each
                         GlobalExceptionHandler method passes to
                         FieldErrorItem.field(...), verbatim, so index.md can say
                         what `fieldErrors[].field` carries per failure kind
                         (it is an error code, not a field path, for a business
                         multi-error whose throw named no field). Reads the shared,
                         module-independent Status -> HttpStatus table from
                         wherever this platform currently keeps it — the
                         Status enum's own constructor today, an explicit
                         OperationCodeImpl table historically; both are
                         recognised — the framework-level error codes
                         (validation, not-found, forbidden, ...) with their
                         real HTTP status from GlobalExceptionHandler, and
                         (per module) the Status a module's own error code
                         was thrown with, wherever a throw site literally
                         names both together. Never infers a mapping from a
                         code's name or value.
  common_headers_extractor.py  BEST-EFFORT, only runs when shared/common
                         source roots are available. Finds headers applied
                         globally by shared servlet filters (e.g. a
                         correlation-ID header) — structurally invisible in
                         OpenAPI, since filters run outside the request-mapping
                         layer springdoc introspects.
  response_model_extractor.py  Detects the shared ApiResponse<T> envelope
                         shape once (springdoc names each generic
                         instantiation differently, e.g.
                         "ApiResponseLegalEntityResponse", so this matches by
                         key-set rather than a fixed schema name), walking
                         every declared property generically (not a fixed
                         key subset) so any envelope field is documented, and
                         reusing dto_extractor's recursive expansion for
                         nested fields like error.fieldErrors.
tests/                   unittest suite (`python3 -m unittest discover -s tests -t .`);
                         fixtures under tests/fixtures/ carry BOTH spellings of
                         every construct whose alternative once broke a build.
waivers/                 per-module check waivers, created only when a module
                         legitimately cannot pass an assertion (none today).
renderers/
  base.py                Renderer interface: render(ApiDocument) -> {path: text}.
                         A renderer only renders — it must never call an
                         extractor or infer missing data itself.
  markdown_renderer.py   index.md (module overview + shared sections) +
                         endpoints/<group-slug>.md per group (= controller,
                         one file per @Tag), with every endpoint inside it as
                         its own "## {METHOD} {path}" section. Flattens
                         FieldSpec.nested into dotted-path rows (parent.child,
                         parent[].child) recursively, so nested DTOs render as
                         real fields instead of an opaque type name.
sync.py                  Documentation synchronization layer. Runs strictly
                         AFTER rendering — compares the freshly rendered
                         {path: text} against what's already under the output
                         directory, classifies each endpoint as
                         added/removed/updated/unchanged by diffing each
                         group file's "## " endpoint sections (the same
                         mechanism it already uses for index.md's shared
                         sections), and decides what to write/delete.
                         Never calls an extractor, never formats markdown.
```

## Data flow

```
compiled Spring Boot app
        │  springdoc-openapi
        ▼
   OpenAPI JSON  ──────────────►  openapi_extractor  ──►  Endpoint list
        │                              │
        │                              ├─► dto_extractor (+ validation_extractor, recursive)
        │                              │      per operation's request/response
        │                              │
        │                         (needs module source root, optional)
        │                              ├─► security_extractor       → permissions
        │                              └─► exception_extractor      → module error codes
        │
        │                         (needs shared/common source roots, optional)
        │                              ├─► error_mapping_extractor  → error codes' Status/HTTP
        │                              │      status + framework-level codes + shared table
        │                              └─► common_headers_extractor → globally-applied headers
        │
        └─────────────────────────►  response_model_extractor  → shared envelope
                                     dto_extractor.find_page_envelope → pagination envelope

                    ApiDocument (fully populated)
                              │
                              ▼
                     MarkdownRenderer.render()
                              │
                              ▼
                    {relative_path: file_content}
                              │
                              ▼
                       sync.py (see "Execution modes")
```

Extraction and rendering are fully separated: extractors never format text,
renderers never call `json.load` or open a Java file. To add a new output
format later (OpenAPI YAML, Postman/Insomnia collection, frontend model
stubs), write a new `Renderer` subclass — no extractor changes needed.

## Automatic discovery

`discovery.py` is what makes `--module SEC --function generate` sufficient on
its own. It reads real, versioned repository artifacts — not a per-module
lookup table — so a brand-new module works the moment it follows the same
conventions every current module already does, with zero generator changes:

1. **Which springdoc group is this module?** Scans the backend tree for
   `GroupedOpenApi.builder()...group("X").displayName("Y").packagesToScan("Z", ...)`
   bean declarations (wherever they live — today that's `erp-main`'s
   `OpenApiConfig.java`, but discovery doesn't assume a path) and matches
   `--module` against each bean's method name / group id / display name
   first, falling back to its scanned package(s) only if nothing more
   specific matched (and preferring the most specific match, so a combined
   "all modules" group never wins by accident).
2. **What's the OpenAPI URL?**
   `http://localhost:<port><context-path><api-docs-path>/<group-id>` — all
   three parts read from the backend's own `application*.properties`
   (`server.port`, `server.servlet.context-path`, `springdoc.api-docs.path`),
   because all three move independently and none has a safe assumed value. A
   `@Value("${server.port:XXXX}")` default in Java is honoured as a fallback.
   On this backend today that resolves to
   `http://localhost:7272/v3/api-docs/<group-id>`.
3. **Where's the module's own source?** Whichever `src/main/java` tree
   actually contains the matched group's controller package(s) — found by
   existence check, not by guessing a directory name. Scoped to the domain
   root (the parent of `controller/`), so a module's best-effort source
   extractors only ever see that module's own code even under the single
   consolidated POM, where every module shares one `src/main/java`.
4. **Where's its shared/common source?** Under a multi-module reactor: every
   *other* reactor module (from the root `pom.xml`'s `<modules>` list) that
   this module's own `pom.xml` declares as a `<dependency>` — the real,
   build-enforced dependency graph, not an assumption about a module's name
   (a module can depend on several; all are searched). Under the single
   consolidated POM this platform builds as today, shared code already lives
   in the same `src/main/java` tree, so that whole tree is the common root.
5. **Where does output go?** The api-docs partition the factory declares:
   `profile-summary.json → consumer.partitions.api-docs.path`
   (`backend/modules/<MOD>/api-docs`, inside `governance/shared/`). A pre-v7
   project declares none; the tool then infers it and refuses when two
   candidates exist (see `discovery.default_output_dir`).
6. **Where is the contract?** `scripts/gov-module.py plan <MOD> --json` →
   `api_spec` (the API document beside the delivered packages) and
   `exec_plan` (the backend plan as of the DELIVERED version — never the
   highest `v<N>/` folder: a delta version holds only what changed). When the
   project predates schema 7 or the module has no delivery yet, the summary's
   `consumer.publications.api-specs`, then the registry's version for the plan.

Every step degrades to "not found" rather than guessing, exactly like the
existing best-effort extractors already do — `generate.py` turns that into a
clear error naming the explicit override flag to use instead.

`discovery.resolve()` returns one `RepositoryContext` object (module,
resolved OpenAPI source, module source root, shared/common source roots,
output directory) — `generate.py` passes it straight to `generator.run()`
without unpacking it into separate arguments itself. Repository-shaped
knowledge lives in exactly one place this way: no extractor imports
`discovery.py`, walks up looking for a `pom.xml`, or otherwise knows the
repository is laid out as sibling checkouts at all — each one just receives
whatever specific `Path`(s) `generator.build_document()` hands it.

```bash
# Explicit overrides — only for what discovery genuinely can't resolve
# (server not running on its default port, an unusual checkout, a saved
# OpenAPI file instead of a live server, ...). Never required for normal use.
python3 generate.py --module SEC --function generate \
    --openapi ./openapi.json \
    --source ../../../src/main/java/com/erp/sec \
    --common-source ../../../src/main/java \
    --api-spec ../../shared/backend/modules/SEC/packages/api-spec-sec.yaml \
    --output ../../shared/backend/modules/SEC/api-docs/
# --execution-plan <plan.md> is read ONLY when no API document exists (pre-v7).
```

## Execution modes (`--function`)

The generator supports the full documentation lifecycle, not just first-time
generation, so it's suitable for continuous use during backend development.
`sync.py` is a dedicated layer that runs after rendering — it never touches
extractors or renderers, and they don't know it exists.

- **generate** — full write, as if no docs exist yet. Every generated file
  is stamped with a marker line
  (`<!-- AUTO-GENERATED by api-doc-generator — do not edit manually -->`).
- **update** — re-extracts from the current backend, renders in memory, then
  compares against what's already under the output directory. Writes only
  added/changed files, deletes endpoint files for endpoints that no longer
  exist in the backend, leaves unchanged files alone, and never touches a
  file that doesn't carry the marker (hand-written docs, or a generated file
  a human has since edited) — those are reported as conflicts instead of
  being clobbered.
- **review** — runs the same comparison as update but writes nothing. Use it
  to answer "what changed in the API docs?" (e.g. in a PR check) without
  modifying anything.
- **check** — review, then the assertions in `checks.py`, exit 2 on any FAIL:
  `deterministic` (two renders identical), `stale` (disk matches the render),
  `contract-drift` (no declared-vs-served row), `contract-ids` (an API document
  with no `x-api-id` operation fails; on the pre-v7 fallback, a registry
  heading with no parseable row fails), `permissions` (found/endpoints against
  @PreAuthorize/@Secured counted in source; any NOT_EXTRACTED outcome fails),
  `auth-determined` (OpenAPI security requirement present when a
  SecurityFilterChain is declared), `envelope`, `error-codes`, `status-table`,
  `business-errors` (bound/endpoints against throw sites in source),
  `field-error-semantics`, `unique-constraints`. Every line is a ratio with
  its expected side, never a bare count -- a `0/38 while 45 in source` is what
  a count of 0 hid for four modules. Waive per module in
  `waivers/<MOD>.json`: `{"waivers": [{"check": "...", "reason": "...", "by":
  "...", "on": "YYYY-MM-DD"}]}`; the verdict prints WAIVED with the reason.
  Never satisfy `check` by deleting or loosening an assertion.

`--function` is required and explicit (no auto-detection ambiguity for CI/CD
— a pipeline always says exactly what it wants). If you're generating output
for the very first time against an existing, hand-authored `api-docs/`
folder from before this tool tracked its own output with the marker line,
that first run will report every file as a conflict (unmanaged) — run
`--function generate` once to (re)stamp it, or move the old folder aside.

Rendering is a deterministic function of the current backend state -- every
mapping and directory listing the tool consumes is sorted, endpoints are
ordered by group, path and verb rather than by the order springdoc emitted
them, and `check` proves it by rendering twice -- so the comparison never
needs to diff DTOs/permissions/validations/error-codes individually — any such change already shows up as a content difference in
the affected endpoint's file (or in index.md's shared sections), so it's
picked up automatically.

Example update-mode report:

```
Mode        : Update
Added       : 1
  + POST /widgets/{id}/archive
Removed     : 1
  - DELETE /widgets/{legacyId} (no longer present in the backend)
Updated     : 2
  ~ GET /widgets
  ~ POST /widgets
  (both in endpoints/widgets.md)
Unchanged   : 0
Shared docs : API Catalog (changed)
Files written: 4, deleted: 1
```

Endpoint changes are still reported one line per endpoint — grouping several
endpoints into one file on disk is a storage/sync-diffing detail, not a loss
of report granularity. When 2+ changes in the same status land in the same
group file, a `(both in ...)` / `(all N in ...)` note follows the list.

Output layout (one `api-docs/` per module, alongside its other governance
packages, not a shared `output/` under the tool itself):

```
backend/modules/ORG/api-docs/
├── index.md                          module overview, auth, common headers,
│                                      shared response/pagination envelopes,
│                                      known error codes (+ Status/HTTP status
│                                      where discoverable), API catalog
└── endpoints/
    └── <group-slug>.md               one file per group (= controller),
                                       every endpoint a "## {METHOD} {path}"
                                       section within it
```

## Supported Spring Boot features

- Any HTTP method/path springdoc exposes, grouped by `@Tag`.
- Path/query/header parameters (works whether pagination is a custom
  `SearchRequest` DTO body, a raw `Pageable` method parameter, or ad-hoc
  `@RequestParam`/`@RequestHeader`s — all fully visible in the OpenAPI JSON,
  no special-casing needed).
- A request DTO's class-level `@Schema(example = ...)` is published verbatim
  as the Request Example. A field-by-field synthesis is the fallback for when
  no whole-payload example exists: it can only ever emit ONE entry of a
  collection, so for any rule of the form "the set must also contain X" it
  publishes a payload the server is required to reject.
- Request/response DTO fields with `@NotBlank`/`@NotNull` → required,
  `@Size` → min/maxLength, `@Pattern` → pattern, `@Schema(description,
  example)` → description/example — expanded recursively into nested fields
  when a field's own type is itself an object schema (search filter/sort
  contracts, field-error items, ...), with self-referential schemas rendered
  as an explicit "recursive" marker instead of expanding forever.
- `enum: [...]` values, when the schema actually has them (see Limitations).
- The shared `ApiResponse<T>` envelope (every declared field, not a fixed
  subset) and Spring `Page<T>` wrapper, each documented once rather than
  repeated on every endpoint.
- Global Bearer-auth requirement per operation, plus `info.version`.
- (best-effort, needs module source) Per-endpoint required permission,
  whether declared on the controller method or delegated to a service
  method.
- (best-effort, needs module source) Module-level known error codes from
  `*ErrorCodes.java`, each with how many endpoints' call walks reach a throw
  site naming it, and the per-endpoint "Business Responses" table those walks
  produce (HTTP status, wire code, constant, throw site).
- (best-effort, needs module source + migrations) The uniqueness invariants
  the database enforces on the module's tables, partial-index scope included.
- (needs shared source) What `error.fieldErrors[].field` carries per handler.
- (best-effort, needs shared/common source) Each error code's business
  `Status` and real HTTP status; the framework-level error codes and their
  HTTP status; headers applied globally by shared filters.
- (needs the i18n bundles) Each error code's ar/en (and any other locale's)
  message, resolved by the code's own value.
- (best-effort, needs module source) Each endpoint's real success HTTP status,
  derived from the `Status` its service returns rather than from springdoc's
  `200` default.

## Limitations

- **Per-endpoint business responses come from a source walk, not from
  annotations.** No controller declares `@ApiResponse`/`@ApiResponses`, so each
  endpoint's "Business Responses" table is what `business_error_extractor`
  reached by following real calls from the controller method. A code thrown
  only from code no endpoint reaches (a cross-module entry point, a job) stays
  in the module catalog marked `unbound`, with its throw sites, and is never
  attributed on a guess; an ambiguous overload is skipped and said so.
- **The Authentication line is only as good as the OpenAPI `security` data.**
  Until 2026-09-23 `OpenApiConfig` declared no `SecurityScheme`, so every
  endpoint read "Not determined from the OpenAPI document" and `check` failed
  `auth-determined` on every module. `OpenApiConfig.erpOpenApi()` now declares
  the bearer scheme globally and the four public auth endpoints opt out with
  `@SecurityRequirements`; if a future backend drops either, `check` fails
  again rather than the docs going quiet. Required permissions come from
  source and are unaffected either way.
- **Permission and error/status discovery are source-text heuristics, not a
  real Java parser.** They rely on conventions actually observed across this
  codebase's modules (one delegate call per controller method under the same
  method name; `throw new (BusinessException|LocalizedException)(Status.X,
  SomeErrorCodes.Y, ...)` at the point of use). They return nothing for
  anything they can't confidently resolve — never guess — and, since
  2026-09-23, say so in the output: an endpoint prints whether its permission
  lookup found nothing to declare or could not finish, and `check` fails on
  the latter. Both spellings of every construct that has broken extraction
  once are pinned by `tests/` against fixture Java. The class names
  they depend on (the permission-constants holder, the Status table's home)
  are matched structurally rather than hardcoded, so a rename degrades into
  partial output instead of silently emptying a whole section.
- **Contract ids come from the API document.** Schema 7 publishes
  `api-spec-<mod>.yaml` per module; every operation carries `x-api-id`. A
  served path may carry a base prefix the document's paths do not
  (`/api/v1/slots` vs `/v1/slots`): a unique trailing-segment match counts.
- **Pre-v7 fallback only: a machine-readable API REGISTRY.** With no API
  document, the registry table is read from the module's
  `backend-execution-plan*.md` as of its registry version (backend delivery,
  else `current_version`; the latest folder ≤ that version holding a plan,
  never simply the highest `v<N>/`), in either spelling in
  use here: a markdown table under `**API REGISTRY**` (SEC, MDL, FIN) or a
  box-drawing (`│`) table under a bare `API REGISTRY` line (CU). NOTIF and
  FILE write their registry as compressed prose — several entries per line,
  `CRUD` in place of a verb — which carries no unambiguous verb+path pair, so
  those modules document without contract ids rather than have one guessed.
  Giving those plans a table is the whole fix; nothing here changes.
- **Requires `<api-docs-path>/<group>` to actually work as a path segment**, not a
  query string — springdoc groups aren't filterable via `?group=x`.

## Extension points

- **New output format**: add a `Renderer` subclass in `renderers/` (e.g.
  `renderers/openapi_yaml.py`, `renderers/postman.py`). It receives the same
  fully-populated `ApiDocument` — no extractor changes needed.
- **New extractor**: add a module under `extractors/` and call it from
  `generator.build_document()`, writing onto the existing
  `ApiDocument`/`Endpoint` dataclasses (add new Optional fields to
  `models/api_doc_model.py` as needed — never repurpose an existing field for
  a different meaning). Give it narrow, explicit parameters (a `Path`, a
  `dict`, ...), never `RepositoryContext` itself — `build_document()` is the
  one place that unpacks the context; keeping extractors one level removed
  from it is what keeps them free of any repository-layout assumptions.
- **New discovered input** (e.g. a future shared cross-module OpenAPI
  source): add a field to `discovery.RepositoryContext` and resolve it in
  `discovery.resolve()`. No other file needs to change its own repository
  logic — `generator.build_document()` just reads the new field.
- **Smarter permission/error resolution**: if this codebase ever adopts a
  `MethodSecurityExpressionHandler` introspection endpoint, or a real Java
  parser (e.g. javalang), `security_extractor.py`/`error_mapping_extractor.py`
  are the files that would need to change.
- **New shared index.md section** (e.g. Sorting, Search conventions): add it
  to `markdown_renderer._index_markdown`. `sync.py`'s section diff works off
  whatever `## ` headings actually appear, so review/update reports pick up
  the new section automatically — no `sync.py` change needed.
- **A new shared/cross-module contract worth surfacing** (e.g. a generic
  lookup/reference-data endpoint used by every module): would need a second,
  explicit OpenAPI input, since — unlike shared *source* — there is no
  build-enforced dependency graph linking one module's API to another's;
  see the enhancement report's discussion of why that one case stays an
  explicit override rather than auto-discovered.
