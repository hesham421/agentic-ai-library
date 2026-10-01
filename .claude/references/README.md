# Reference Repositories

External repositories that inform the design of the Reusable Agentic AI Library. They are
**references only**: none is copied into this project, and none is a dependency. Do not copy their
source code. Read them, then design for this library's domain-agnostic, configuration-driven goals.

Information below was gathered on 2026-10-01.

| Classification | Repository |
|----------------|------------|
| Primary technical reference | `spring-projects/spring-ai` |
| Agentic AI reference | `spring-ai-community/spring-ai-agent-utils` |
| Learning / architecture references | `microsoft/Spring-AI-for-Beginners`, `omonuj/spring-ai-agents-rag` |
| Skill sources (see [`../skills/README.md`](../skills/README.md)) | `rynr/spring-skills`, `spring-ai-community/spring-testing-skills` |

---

## 1. spring-projects/spring-ai: Primary Technical Reference

- **URL:** https://github.com/spring-projects/spring-ai
- **Docs:** https://docs.spring.io/spring-ai/reference/ (current release: 2.0.1)
- **License:** Apache-2.0
- **Versions:** Release 2.0.1 is built against Spring Boot 4.1.1 with a Java 17 baseline. `main`
  is 2.1.0-SNAPSHOT on Boot 4.2.0-SNAPSHOT, so read the docs or the `v2.0.1` tag, not `main`.
- **Purpose:** The authoritative source for every Spring AI API this library builds on. When the
  Skills or other references disagree with it, this repository and its docs win.

### Relevant Areas

Docs paths are relative to `https://docs.spring.io/spring-ai/reference/`.

| Area | Source location | Docs |
|------|-----------------|------|
| ChatClient | `spring-ai-client-chat/.../chat/client/ChatClient.java` | `api/chatclient.html` |
| Advisors | `spring-ai-client-chat/.../chat/client/advisor/api/` | `api/advisors.html` |
| Tool calling | `spring-ai-model/.../tool/`, `ToolCallingManager`; `spring-ai-tool-search-tool` | `api/tools.html` |
| Structured output | `spring-ai-model/.../converter/BeanOutputConverter` | `api/structured-output.html` |
| RAG | `spring-ai-rag/` (`RetrievalAugmentationAdvisor`), `advisors/spring-ai-vector-store-advisor/` (`QuestionAnswerAdvisor`) | `api/retrieval-augmented-generation.html` |
| Vector stores | `spring-ai-vector-store/` (`VectorStore`), `vector-stores/*` | `api/vectordbs.html` |
| Chat memory | `spring-ai-model/.../chat/memory/`, `memory-repositories/*` | `api/chat-memory.html` |
| MCP | `mcp/common`, `mcp/mcp-annotations`, `auto-configurations/mcp/`, `spring-ai-starter-mcp-*` | `api/mcp/mcp-overview.html` |
| Model abstraction | `spring-ai-model/.../chat/model/ChatModel`, `models/*` | `api/chatmodel.html` |
| Observability | `spring-ai-model/.../chat/observation/` | `observability/index.html` |
| Evaluation | `spring-ai-commons/.../evaluation/Evaluator`, `RelevancyEvaluator`, `FactCheckingEvaluator` | `api/testing.html` |
| Agentic patterns | docs only | `api/effective-agents.html` |

---

## 2. spring-ai-community/spring-ai-agent-utils: Agentic AI Reference

- **URL:** https://github.com/spring-ai-community/spring-ai-agent-utils
- **License:** Apache-2.0
- **Versions:** Latest release 0.12.0 (`org.springaicommunity:spring-ai-agent-utils`), on Spring
  AI 2.0.1 / Java 17. It is pre-1.0, so expect API churn.
- **Purpose:** Agent-level building blocks on top of Spring AI. These are Claude Code-style
  capabilities rebuilt as Spring AI tools. It is the closest existing match to this library's future
  scope. **It is not a dependency.** Adopting it is a later architecture decision.

### Relevant Areas

| Area | Location |
|------|----------|
| Skills (loading `SKILL.md`-style instructions at runtime) | `SkillsTool`; `docs/` |
| Sub-agents and task orchestration, multi-model routing | `TaskTools`, `spring-ai-agent-utils-common` (subagent SPI), `spring-ai-agent-utils-a2a` (remote A2A subagents) |
| Planning / todo tracking | `TodoWriteTool` |
| Memory | `AutoMemoryTools`, `AutoMemoryToolsAdvisor` |
| Human interaction (HITL) | `AskUserQuestionTool` |
| Tool-based agents | File system, shell (with Docker sandbox in `exec-backends/`), grep/glob, web fetch/search tools |
| Agent composition | `advisors/`, `tools/task/`; runnable demos under `examples/` |
| MCP | Used only by some examples via the Spring AI MCP client starter. No first-class MCP features. |

---

## 3. microsoft/Spring-AI-for-Beginners: Learning Reference

- **URL:** https://github.com/microsoft/Spring-AI-for-Beginners
- **License:** MIT
- **Versions:** Spring AI 2.0.1, Spring Boot 4.0.6, **Java 25**. This independently confirms that
  Java 25 + Spring AI 2.0.x works.
- **Purpose:** A structured course with idiomatic, current Spring AI 2.0 examples. It is useful for
  onboarding and for sanity-checking API usage.
- **Caveat:** The examples are tied to Azure OpenAI / Microsoft Foundry, through the OpenAI
  starter. It contains no Claude Code Skills.

### Relevant Areas

| Lesson | Topic |
|--------|-------|
| `01-introduction` | Spring AI basics, `ChatClient` |
| `02-prompt-engineering` | Prompt engineering, reasoning effort |
| `03-rag` | RAG with `SimpleVectorStore` and the vector-store advisor |
| `04-tools` | Tool calling |
| `05-mcp` | MCP client (WebFlux) and MCP server (WebMVC) |
| `06-agents` | Agentic workflows: chain, parallelization, routing, orchestrator-workers, evaluator-optimizer |

---

## 4. omonuj/spring-ai-agents-rag: Architecture Reference

- **URL:** https://github.com/omonuj/spring-ai-agents-rag
- **License:** **None declared.** Reuse rights are unclear. Study only; never copy code.
- **Versions:** Spring AI **1.1.0**, Spring Boot **3.5.8**, Java 25. That is one major version
  behind this project, so APIs and starter names may differ from 2.0.x.
- **Purpose:** Shows how many concerns combine in a single application: agents, RAG, tools, MCP,
  memory, HITL, and observability across multiple providers. Use it to study integration seams,
  not as a template. It is a single-commit demo application with no tests and hard-coded
  configuration.

### Relevant Areas

| Area | Location |
|------|----------|
| Agent with plan/step model, chain workflow, human approval checkpoints | `src/.../aiagent/` (`ApprovalRequest`, `PendingApproval`) |
| Tools | `src/.../aiagent/tools/{diagram,rag,web,posture}` |
| Advanced RAG: query transformers, reranking, citations, ingestion | `src/.../rag/` |
| MCP client plus a separate MCP server | `src/.../mcp/`, `posture-service/` |
| JDBC chat memory | `src/.../chat/memory/` |
| Custom advisors (sanitizing, validation, error wrapping) | `src/.../` advisors |
| Multi-provider setup: OpenAI, Vertex Gemini, Hugging Face, Ollama, Docker Model Runner | `application*.yml`, `ollama/`, `huggingface/` |
| Observability stack: Prometheus, Grafana, Loki, Tempo, Jaeger | `docker/` |

**Anti-reference:** Its domain-specific "security posture review" agent is the kind of
Product logic this library must *not* contain.
