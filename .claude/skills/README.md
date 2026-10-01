# Claude Code Skills

Project-scoped [Claude Code Skills](https://docs.claude.com/en/docs/claude-code/skills) that guide
development of the Reusable Agentic AI Library. Claude Code discovers every `<skill>/SKILL.md` in
this directory automatically.

Skills were selected for **quality, not quantity**. Each was inspected in full before installation
(see [Security review](#security-review)).

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
- **Why selected:** Spring AI is the foundation of this library. The anti-patterns section
  (naked `ChatModel`, unbounded memory, trusting tool input) maps directly to library design concerns.
- **Local modification:** One clearly marked `LOCAL PROJECT NOTE` was inserted below the title.
  Upstream targets Spring AI 1.x / Boot 3.4, and its `references/version-guide.md` lists 2.0 as a
  milestone preview. The note states that this project's baseline (Spring AI 2.0.1 / Boot 4.1.1 /
  Java 25) and the official Spring AI 2.0 docs take precedence. No other upstream text was changed.

### spring-boot

- **Source:** rynr/spring-skills
- **Purpose:** Boot 4.0 conventions: type-safe `@ConfigurationProperties`, auto-configuration
  troubleshooting, observability (OpenTelemetry), Boot 4 starter renames and migration notes.
- **Why selected:** The `spring-ai` skill declares `depends_on: [spring-boot]`. A reusable,
  configuration-driven library will be delivered through auto-configuration and
  `@ConfigurationProperties`, which this skill covers.
- **Known gap:** It references `references/logging-pre34.md`, which does not exist upstream. That
  file only matters for Boot ≤ 3.3, so it is irrelevant here.

### spring-testing-fundamentals

- **Source:** spring-ai-community/spring-testing-skills
- **Purpose:** AssertJ/BDDMockito idioms, `@MockitoBean`/`@MockitoSpyBean` (Boot 4), context caching,
  testing pyramid, Boot 3 → 4 test annotation migration.
- **Why selected:** It is the default testing skill for all code, written for Boot 4 / Framework 7.
  The library's core (agent runtime, tool and advisor abstractions) will mostly be tested with plain
  unit tests and mocked `ChatModel`s.

### spring-mvc-testing

- **Source:** spring-ai-community/spring-testing-skills
- **Purpose:** `@WebMvcTest`, `MockMvcTester` (Boot 4), `RestTestClient`, validation and
  `@RestControllerAdvice` testing.
- **Why selected:** Future HTTP-facing features (API tools, MCP server over WebMVC, sample
  Product integrations) need correct Boot 4 MVC test idioms.

### spring-jpa-testing

- **Source:** spring-ai-community/spring-testing-skills
- **Purpose:** `@DataJpaTest`, Testcontainers with `@ServiceConnection`, transactional test
  pitfalls, Hibernate 6/7.
- **Why selected:** It covers the requested database, integration, and Testcontainers testing. JDBC
  chat memory, pgvector, and checkpoint persistence will need real-database tests.

## Considered and Not Installed

| Candidate | Source | Reason |
|-----------|--------|--------|
| `spring-testing` | rynr/spring-skills | Duplicates the three spring-ai-community testing skills. It targets Boot 3.2 and adds Spock guidance this project will not use. |
| `spring-security-testing`, `spring-webflux-testing`, `spring-websocket-testing` | spring-ai-community/spring-testing-skills | Security, reactive, and WebSocket are out of scope for now. Reconsider when streaming or permissions are implemented. |
| `spring-framework`, `spring-web`, `spring-data-jpa`, `spring-security`, `spring-modulith`, `spring-events`, `spring-flyway`, `spring-kafka`, `spring-graphql`, `spring-versions` | rynr/spring-skills | Not needed yet. They are scoped to features the library does not have. |
| `spring-project-analyzer` agent, plugin/marketplace manifests | rynr/spring-skills | It is a Claude Code plugin agent, not a Skill, and is not needed. |
| `ai-tutor`, `pdf` example skills | spring-ai-community/spring-ai-agent-utils (`examples/`) | Demo resources for that project's examples, unrelated to developing this library. `ai-tutor` also ships Python scripts. |

**Gap:** No AI/agent-specific *testing* Skill exists in the source repositories. Until one is
available, use the Spring AI "Model Evaluation" docs (`RelevancyEvaluator`,
`FactCheckingEvaluator`), described in [`../references/README.md`](../references/README.md).

## Security Review

The following was done before installing any Skill:

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
