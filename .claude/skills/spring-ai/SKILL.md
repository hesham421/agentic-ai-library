---
name: spring-ai
description: Guide Spring AI usage including ChatClient, structured output, tool calling, RAG, advisors, chat memory, and MCP integration. Use when building AI-powered features with Spring Boot, integrating LLM providers, implementing retrieval-augmented generation, or configuring function/tool calling.
version: 0.2.0
tags: [ai, llm, rag, tool-calling, chatclient, structured-output, mcp]
triggers:
  - building AI-powered features with Spring Boot
  - integrating LLM providers (OpenAI, Anthropic, Ollama, etc.)
  - implementing retrieval-augmented generation (RAG)
  - configuring tool/function calling for LLMs
  - using ChatClient or ChatModel APIs
  - implementing chat memory or conversation history
  - setting up MCP servers or clients
  - structuring LLM output into Java objects
spring_versions:
  framework: ">=6.1"
  boot: ">=3.4"
  project: "Spring AI >=1.0"
depends_on: [spring-boot]
---

# Spring AI

> **[LOCAL PROJECT NOTE - added on install, not part of upstream]** This project — aias, the Request Verification Service — targets **Spring AI 2.0 on Spring Boot 4 / Java 21** (the profile; `pom.xml` currently pins Spring AI 2.0.1, Boot 4.1.1 and `java.version` 25). The version statements in this skill and in `references/version-guide.md` describe Spring AI 1.x on Boot 3.4 and are outdated for this repository. The official Spring AI 2.0 reference documentation (https://docs.spring.io/spring-ai/reference/) takes precedence; verify APIs against Spring AI 2.0 before relying on a 1.x example.
>
> **Where this skill does not apply to aias** (domain-profile G1, G7, G9, G12; scope §1): the engine depends on the provider-neutral **`ChatModel` / `Prompt` / `ChatOptions` API only** (plus `BeanOutputConverter` for structured output), with no provider-specific option class or feature — the "always use `ChatClient`" advice below does not override that. The model is given **no tools** (it never runs SQL, reads files or triggers approval), there is **no chat memory, no RAG / vector store and no multi-agent orchestration**, and document content is passed only as delimited data, never as instructions. The host query channel is an MCP *client* behind the query port, not a model tool. Use the sections on structured output, observability and retry; treat tool calling, advisors-with-memory, RAG and agentic patterns as out of scope.

**Pattern:** Process

Framework for integrating AI models into Spring Boot applications. Targets **Spring AI 1.0+** on **Boot 3.4+**, Java 17+.

**Mental model:** Spring AI mirrors Spring's HTTP client layering. `ChatModel` is the low-level provider wrapper (like `RestTemplate`). `ChatClient` is the fluent high-level API (like `RestClient`) — it adds advisors, tools, memory, and structured output. Always use `ChatClient` unless you need raw provider access.

**Do NOT Load** for general machine learning, data science, or non-LLM AI. For event-driven patterns use spring-events. For web endpoints exposing AI features use spring-web.

## When to Use

- Calling LLM providers (OpenAI, Anthropic, Ollama, Bedrock, etc.) from Spring Boot
- Converting LLM responses to typed Java objects (structured output)
- Registering tools/functions that LLMs can invoke
- Building RAG pipelines with vector stores and document ingestion
- Managing conversation history with chat memory
- Creating or consuming MCP servers
- Adding observability to AI calls

## Choosing Your Architecture

| Need | Add to ChatClient |
|------|-------------------|
| Just chat | Nothing — `ChatClient` alone is sufficient |
| Conversation history | `MessageChatMemoryAdvisor` + a `ChatMemoryRepository` |
| Domain knowledge (RAG) | `QuestionAnswerAdvisor` → graduate to `RetrievalAugmentationAdvisor` when retrieval quality drops |
| Model takes actions | Tools via `@Tool` or function beans |
| Multi-step reasoning | Agentic patterns (chain, routing, orchestrator-workers) — see `references/agentic-patterns.md` |
| Content safety | `SafeGuardAdvisor` |

**Before adding AI to your application, ask:**
- Can this be solved without an LLM? Simpler solutions (templates, rules) are faster, cheaper, and deterministic
- What happens when the model returns garbage? Always validate structured output and handle failures gracefully
- What is the cost per request? Monitor token usage via observations from day one

## ChatClient

The primary API. Spring Boot auto-configures a **prototype-scoped** `ChatClient.Builder` — each injection gets a fresh builder.

```java
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultSystem("You are a helpful assistant")
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
    .defaultTools(new DateTimeTools())
    .build();

String answer = chatClient.prompt()
    .user("What day is tomorrow?")
    .call()
    .content();
```

- **`call()`** — synchronous: `.content()`, `.entity(Class)`, `.chatResponse()`
- **`stream()`** — returns `Flux<String>` or `Flux<ChatResponse>`. Requires `spring-boot-starter-webflux` even in servlet apps. **Retry does not apply to streaming** — handle failures at the application level
- **Per-request options** — override model, temperature, etc.: `.options(ChatOptions.builder().temperature(0.9).model("gpt-4o").build())`
- **Multiple models** — disable auto-config with `spring.ai.chat.client.enabled=false`, create `ChatClient` instances manually with `@Qualifier` on each `ChatModel`
- **Runtime model switching** — use `chatModel.mutate()` to derive new instances with different base URLs, API keys, or options

## Prompt Templates

Externalize prompts to resource files instead of hardcoding strings:

```java
var template = new PromptTemplate(new ClassPathResource("/prompts/analyze.st"));
String rendered = template.render(Map.of("topic", "Spring Security", "depth", "expert"));
```

Uses StringTemplate syntax (`{variable}` delimiters). Use `SystemPromptTemplate` for system messages. Keep prompts in `src/main/resources/prompts/` — they are version-controlled, reviewable, and reusable.

## Structured Output

Convert LLM responses to Java types:

```java
record ActorFilms(String actor, List<String> movies) {}

ActorFilms result = chatClient.prompt()
    .user("List 5 films with Tom Hanks")
    .call()
    .entity(ActorFilms.class);
```

- **`entity(Class)`** — single object via `BeanOutputConverter` (JSON Schema DRAFT_2020_12)
- **`entity(ParameterizedTypeReference)`** — for generic types like `List<ActorFilms>`
- **Native structured output** — bypasses format instructions, uses the model's built-in JSON mode. More reliable. Enable with `AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT`. Supported by: OpenAI (GPT-4o+), Anthropic (Claude 3.5+), Vertex AI Gemini, Mistral AI

**Structured output is best-effort** — models are not guaranteed to return valid JSON. Always validate the result. Prefer native structured output when the provider supports it.

## Tool Calling

Four approaches, ranked by preference:

### 1. @Tool Annotation (Recommended)

```java
class DateTimeTools {
    @Tool(description = "Get current date and time in the user's timezone")
    String getCurrentDateTime() {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();
    }
}

chatClient.prompt("What time is it?").tools(new DateTimeTools()).call().content();
```

### 2. Function Bean — `@Bean @Description` returning `Function<I, O>`, reference via `.toolNames("beanName")`
### 3. FunctionToolCallback — programmatic `FunctionToolCallback.builder()` for dynamic registration
### 4. MethodToolCallback — reflective, for fine-grained control. Rarely needed

**Key rules:**
- **`returnDirect = true`** — tool result goes directly to the caller, bypasses model post-processing
- **`ToolContext`** — pass application context (tenant ID, user) not sent to the model
- **Method tools** do NOT support `Optional`, `CompletableFuture`, `Mono`, `Flux`, or functional types as parameters
- **Models never access APIs directly** — the application executes tool calls. Tool descriptions should be specific and hardened against prompt injection
- **Error handling** — by default, tool exceptions send error text back to the model. Set `spring.ai.tools.throw-exception-on-error=true` for fail-fast

## Advisors

Interceptors in the request/response pipeline (like servlet filters):

```
Request -> Advisor1(before) -> Advisor2(before) -> ... -> LLM
Response <- Advisor1(after) <- Advisor2(after) <- ... <- LLM
```

Ordering via `getOrder()` — lower values execute first on requests, last on responses (stack behavior).

**Built-in advisors:**

| Advisor | Purpose |
|---------|---------|
| `MessageChatMemoryAdvisor` | Adds conversation history as messages |
| `PromptChatMemoryAdvisor` | Appends history as text to system prompt |
| `VectorStoreChatMemoryAdvisor` | Semantic memory retrieval from vector store |
| `QuestionAnswerAdvisor` | Simple naive RAG |
| `RetrievalAugmentationAdvisor` | Modular RAG with composable stages |
| `SafeGuardAdvisor` | Content safety filtering |
| `SimpleLoggerAdvisor` | Debug logging of requests/responses |
| `ReReadingAdvisor` | RE2 technique for improved reasoning |
| `ToolCallAdvisor` | Advisor-controlled tool execution with observability |

For custom advisor creation and the RAG advisor pipeline, load `references/rag-advisors.md`.

## Chat Memory

`MessageWindowChatMemory` keeps the last N messages (preserves system messages):

```java
ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(10).build();

ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
    .build();

chatClient.prompt().user("My name is James")
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "session-123"))
    .call().content();
```

**Storage backends:** `InMemoryChatMemoryRepository` (default), JDBC (PostgreSQL, MySQL, etc.), Cassandra, Neo4j, MongoDB, CosmosDB.

**Pitfall:** `MessageWindowChatMemory` counts messages, not tokens. A few long messages can exceed context windows. Intermediate tool-call messages are NOT persisted to memory — conversation history may be incomplete.

## Retry and Resilience

Spring AI retries failed model calls automatically. Defaults are aggressive — tune for production:

```properties
spring.ai.retry.max-attempts=3              # Default: 10 (too many for production)
spring.ai.retry.backoff.initial-interval=2s
spring.ai.retry.backoff.multiplier=5
spring.ai.retry.backoff.max-interval=3m
spring.ai.retry.on-client-errors=false      # 4xx errors not retried by default
```

**Retry does NOT apply to streaming API.** No built-in circuit breaker — use Resilience4j if needed.

## Observability

Full Micrometer integration. Observations for chat calls, embedding calls, advisor execution, tool calls, and vector store operations.

```properties
spring.ai.chat.observations.log-prompt=true         # CAUTION: exposes prompts in traces
spring.ai.chat.observations.log-completion=true      # CAUTION: exposes responses in traces
spring.ai.tools.observations.include-content=false   # Tool args/results — keep false in prod
```

All default to `false` for security. Span attributes include model name, temperature, token counts, and finish reasons. Use `gen_ai.client.token.usage` counter for cost monitoring.

## Anti-Patterns

- **The Naked ChatModel** — using `ChatModel` directly instead of `ChatClient`. Loses advisors, memory, tools, and structured output. Use `ChatClient` for all application code
- **The Token Bomb** — no limit on conversation history size. `MessageWindowChatMemory` counts messages, not tokens. A few large messages silently exceed the context window. Monitor token usage via observations
- **The Trusting Tool** — tool descriptions that leak implementation details or accept unvalidated model input. Harden descriptions and validate arguments — the model's tool call is untrusted input
- **The Singleton Builder** — injecting `ChatClient` as a singleton when it holds mutable conversation state. Inject `ChatClient.Builder` (prototype-scoped) and build per-request
- **The Format Prayer** — relying on format instructions for structured output without validation. Models don't guarantee valid JSON. Use native structured output when available, always validate
- **The Silent Stream** — calling `stream()` without `spring-boot-starter-webflux` on the classpath. Fails at runtime with a confusing error
- **The Multi-Model Clash** — multiple model starters on classpath without `spring.ai.model.chat=<provider>`. Auto-configuration fails. Set the property or disable `spring.ai.chat.client.enabled`

## Resource Files

Load on demand for specific topics:

- **`references/rag-advisors.md`** — RAG pipeline architecture, vector store setup, document ETL, custom advisors, query transformers
- **`references/providers.md`** — Provider-specific configuration, model selection, MCP client/server setup, multimodal support, Docker Model Runner
- **`references/agentic-patterns.md`** — Chain, parallelization, routing, orchestrator-workers, evaluator-optimizer workflows
- **`references/version-guide.md`** — Version matrix, feature availability by version, breaking changes timeline, migration guides

## References

- [Spring AI Reference Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring AI API Documentation](https://docs.spring.io/spring-ai/docs/current/api/)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
