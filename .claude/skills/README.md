# Claude Code Skills

Project-scoped [Claude Code Skills](https://docs.claude.com/en/docs/claude-code/skills) that guide
development of the aias backend — the Request Verification Service. Claude Code discovers every
`<skill>/SKILL.md` in this directory automatically. Which skill to use for which work is the
routing table in [`../../CLAUDE.md`](../../CLAUDE.md) ("Skill routing").

Upstream skills were selected for **quality, not quantity**. Each was inspected in full before
installation (see [Security review](#security-review)).

## Installed Skills

| Skill | Source repository | Upstream path | Upstream commit |
|-------|-------------------|---------------|-----------------|
| `spring-ai` | [rynr/spring-skills](https://github.com/rynr/spring-skills) | `skills/spring-ai/` | `fe9d704` (2026-04-02) |
| `spring-boot` | [rynr/spring-skills](https://github.com/rynr/spring-skills) | `skills/spring-boot/` | `fe9d704` (2026-04-02) |
| `spring-testing-fundamentals` | [spring-ai-community/spring-testing-skills](https://github.com/spring-ai-community/spring-testing-skills) | `skills/spring-testing-fundamentals/` | `7e7b331` (2026-04-24) |
| `spring-mvc-testing` | [spring-ai-community/spring-testing-skills](https://github.com/spring-ai-community/spring-testing-skills) | `skills/spring-mvc-testing/` | `7e7b331` (2026-04-24) |
| `spring-jpa-testing` | [spring-ai-community/spring-testing-skills](https://github.com/spring-ai-community/spring-testing-skills) | `skills/spring-jpa-testing/` | `7e7b331` (2026-04-24) |

Licenses: `rynr/spring-skills` is MIT (declared in its README; the repo has no LICENSE file).
`spring-ai-community/spring-testing-skills` is Apache-2.0 (copied as
[`LICENSE-spring-testing-skills`](LICENSE-spring-testing-skills)).

### spring-ai

- **Source:** rynr/spring-skills
- **Purpose:** Spring AI development guidance: `ChatClient`, advisors, tool calling, structured
  output, RAG, chat memory, MCP, observability, retry, plus reference files on agentic workflow
  patterns, providers/MCP, and RAG/advisor pipelines.
- **Why selected:** Spring AI 2.0 is the model layer of the Check Engine and of document reading.
- **Local modification:** One clearly marked `LOCAL PROJECT NOTE` below the title. It states the
  aias baseline (Spring AI 2.0 / Boot 4 / Java 21 per the profile; `pom.xml` currently pins Java
  25) and which parts of the skill do **not** apply to aias: the engine uses `ChatModel` /
  `Prompt` / `ChatOptions` only with no provider-specific feature, the model gets no tools, and
  there is no chat memory, RAG or multi-agent orchestration (domain-profile G1, G7, G9, G12). No
  other upstream text was changed.

### spring-boot

- **Source:** rynr/spring-skills
- **Purpose:** Boot 4.0 conventions: type-safe `@ConfigurationProperties`, auto-configuration
  troubleshooting, observability (OpenTelemetry), Boot 4 starter renames and migration notes.
- **Why selected:** The `spring-ai` skill declares `depends_on: [spring-boot]`, and aias is
  configured through `aias.*` `@ConfigurationProperties` (registry, connections, per-check limits).
- **Known gap:** It references `references/logging-pre34.md`, which does not exist upstream. That
  file only matters for Boot ≤ 3.3, so it is irrelevant here.

### spring-testing-fundamentals

- **Source:** spring-ai-community/spring-testing-skills
- **Purpose:** AssertJ/BDDMockito idioms, `@MockitoBean`/`@MockitoSpyBean` (Boot 4), context caching,
  testing pyramid, Boot 3 → 4 test annotation migration.
- **Why selected:** The default testing skill for all code, written for Boot 4 / Framework 7.
  Domain classes and services are tested with plain unit tests and mocked ports (including a
  mocked `ChatModel`) for the `RULE-SCENARIOS` test unit.

### spring-mvc-testing

- **Source:** spring-ai-community/spring-testing-skills
- **Purpose:** `@WebMvcTest`, `MockMvcTester` (Boot 4), `RestTestClient`, validation and
  `@RestControllerAdvice` testing.
- **Why selected:** The `API-SCENARIOS` test unit exercises the REST API and its ProblemDetail
  error responses.

### spring-jpa-testing

- **Source:** spring-ai-community/spring-testing-skills
- **Purpose:** `@DataJpaTest`, Testcontainers with `@ServiceConnection`, transactional test
  pitfalls, Hibernate 6/7.
- **Why selected:** The service's own Oracle 19c schema (identity PKs, `NUMBER(1)` booleans,
  CHECK constraints, the registry load lock) needs real-database tests.

## Local Skills

Written for this project, not pulled from upstream. They use a lane prefix: `build-*` skills
**generate** code, `gov-*` skills **validate only** and never generate or modify code.
`api-verify` exercises the running API. Upstream skills keep their upstream names (`spring-*`).

| Skill | Use when |
|-------|----------|
| [`build-create-entity`](build-create-entity/SKILL.md) | A DATA-DOM unit introduces an entity: JPA entity from the db-script (identity `{entity}Id` PK, oracle19c types, `createdAt`/`updatedAt`, hard delete, host identifiers as strings) plus its domain class |
| [`gov-enforce-backend-contract`](gov-enforce-backend-contract/SKILL.md) | After any backend code: 80 checks across Domain, Entity, Repository, DTO, Port/Adapter, Service, Controller |
| [`gov-enforce-library-contract`](gov-enforce-library-contract/SKILL.md) | After any backend code: the aias service contract — 42 checks; the §12 guardrails group (G1–G14) is unconditional. The folder name is historical |
| [`gov-enforce-error-handling`](gov-enforce-error-handling/SKILL.md) | Code that throws, catches, or maps exceptions: 24 checks on `{MOD}-{http}[-{SLUG}]` codes, the error-catalog and RFC 9457 ProblemDetail |
| [`gov-enforce-caching-rules`](gov-enforce-caching-rules/SKILL.md) | Any caching added or proposed: 28 checks against an approved register (empty today); nothing per-check is ever cached (G9) |
| [`api-verify`](api-verify/SKILL.md) | After a module's endpoints are built and its api-docs are published: holds the api-docs to the API document and records the `api_verify` row |

**No `build-*` skill exists yet** for repositories, DTOs, services, controllers, ports or
adapters. For those layers the delivered unit's package spec is the generator and
`gov-enforce-backend-contract` is the gate.

**Provenance:** the `build-*`, `gov-*` and `api-verify` skills were adapted on 2026-10-01 from an
earlier backend governance pack and then rewritten for aias. Everything that belonged to that
other project was removed — its audit base class, result envelope, sequence keys,
activate/deactivate lifecycle, soft delete, method-level authorization, bilingual message
exceptions and module codes. What remains is aias's own contract: the profile's layers and
conventions, the ProblemDetail error contract, hard delete, and the §12 guardrails. Security
rules are deliberately absent: caller authentication is deferred by amendment A2 and returns
with the security version.

**No overlap with upstream skills:** the local skills do not restate Spring AI, Spring Boot, or
testing guidance. They point to `spring-ai`, `spring-boot`, and the `spring-*-testing` skills
instead.

All local skills are **Markdown only**: no scripts, hooks, or executables.

## Considered and Not Installed

| Candidate | Source | Reason |
|-----------|--------|--------|
| `spring-testing` | rynr/spring-skills | Duplicates the three spring-ai-community testing skills. It targets Boot 3.2 and adds Spock guidance this project will not use. |
| `spring-security-testing` | spring-ai-community/spring-testing-skills | Caller authentication is deferred (amendment A2). Reconsider with the security version. |
| `spring-webflux-testing`, `spring-websocket-testing` | spring-ai-community/spring-testing-skills | The service is servlet-based REST with polling; no reactive or WebSocket surface. |
| `spring-framework`, `spring-web`, `spring-data-jpa`, `spring-security`, `spring-modulith`, `spring-events`, `spring-flyway`, `spring-kafka`, `spring-graphql`, `spring-versions` | rynr/spring-skills | Not needed yet. Reconsider `spring-data-jpa` / `spring-flyway` if the DATA-DOM work or a schema-migration decision calls for them. |
| `spring-project-analyzer` agent, plugin/marketplace manifests | rynr/spring-skills | It is a Claude Code plugin agent, not a Skill, and is not needed. |
| `ai-tutor`, `pdf` example skills | spring-ai-community/spring-ai-agent-utils (`examples/`) | Demo resources for that project's examples, unrelated to this service. `ai-tutor` also ships Python scripts. |

**Gap:** No AI-specific *testing* Skill exists in the source repositories. For the `MODEL-EVAL`
test unit (the fixed known-result request set run on every model change, domain-profile G12),
use the Spring AI "Model Evaluation" docs, described in
[`../references/README.md`](../references/README.md).

## Security Review

The following was done before installing any upstream Skill:

1. Both source repositories were shallow-cloned into a temporary directory outside the project. No
   upstream build, wrapper (`mvnw`), or CI script was executed.
2. Every installed `SKILL.md` and reference file was read. All installed Skills are **Markdown
   only**: no scripts, executables, or hooks.
3. The installed files were scanned for shell commands, `curl`/`wget`/`npx`, agent-directed
   instructions, and prompt-injection phrasing. Hits were limited to documentation links and
   illustrative config snippets (e.g. an MCP `npx` example in `spring-ai/references/providers.md`,
   which is documentation, not an instruction to run anything).
4. Installed files are byte-identical to upstream at the commits above, apart from the one
   documented `LOCAL PROJECT NOTE` in `spring-ai/SKILL.md`.

## Updating

Re-run the review above before pulling a newer upstream revision. Diff against the recorded commit,
read the changes, and update this table.
