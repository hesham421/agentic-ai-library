# Spring AI Version Guide

## Version Matrix

| Spring AI | Spring Boot | Java | Status |
|-----------|-------------|------|--------|
| 1.0.x | 3.4.x | 17+ | Maintenance |
| 1.1.x | 3.4.x, 3.5.x | 17+ | Current stable |
| 2.0.0-M4 | 4.0.x | 17+ | Preview |

**BOM:** `org.springframework.ai:spring-ai-bom:1.1.x` — published to Maven Central (no custom repository needed for releases).

## Feature Availability by Version

| Feature | 1.0 | 1.1 |
|---------|-----|-----|
| ChatClient fluent API | Yes | Yes |
| Structured output (format instructions) | Yes | Yes |
| Native structured output (JSON mode) | Yes | Yes |
| @Tool annotation | Yes | Yes |
| Function bean tools | Yes | Yes |
| ToolContext | Yes | Yes |
| returnDirect on tools | Yes | Yes |
| BaseAdvisor (simplified custom advisors) | — | Yes |
| RetrievalAugmentationAdvisor (modular RAG) | — | Yes |
| Query transformers (rewrite, compression, translation) | — | Yes |
| MultiQueryExpander | — | Yes |
| TextToSpeechModel (renamed from SpeechModel) | — | Yes |
| ToolCallAdvisor | — | Yes |
| MessageWindowChatMemory | Yes | Yes |
| VectorStoreChatMemoryAdvisor | Yes | Yes |
| JDBC/Cassandra/Neo4j/MongoDB memory repositories | Yes | Yes |
| MCP client/server starters | Yes | Yes |
| @McpTool / @McpResource annotations | — | Yes |
| Observability (Micrometer) | Yes | Yes |
| Docker Model Runner support | — | Yes |

## Breaking Changes Timeline

### 1.0.0-M1 (Oct 2024)

Old `ChatClient` was split into two:
- `ChatModel` — low-level provider wrapper (what old `ChatClient` was)
- `ChatClient` — new fluent high-level API

All code using the old `ChatClient` must migrate to `ChatModel` or the new `ChatClient`.

### 1.0.0-M5 (Dec 2024)

- VectorStore constructors deprecated — use builders instead
- Package moves: `org.springframework.ai.vectorstore.{provider}` → `org.springframework.ai.{provider}.vectorstore`

### 1.0.0-M7 (Feb 2025)

**Artifact IDs completely restructured.** This is the most disruptive change.

| Old artifact | New artifact |
|-------------|-------------|
| `spring-ai-openai-spring-boot-starter` | `spring-ai-starter-model-openai` |
| `spring-ai-anthropic-spring-boot-starter` | `spring-ai-starter-model-anthropic` |
| `spring-ai-ollama-spring-boot-starter` | `spring-ai-starter-model-ollama` |
| `spring-ai-pgvector-store-spring-boot-starter` | `spring-ai-starter-vector-store-pgvector` |
| `spring-ai-spring-boot-autoconfigure` | Removed (per-provider auto-config now) |

**Pattern:** `spring-ai-starter-model-{provider}`, `spring-ai-starter-vector-store-{store}`, `spring-ai-starter-mcp-{type}`

### 1.0.0-RC1 (Mar 2025)

- Advisor API renamed: `AdvisedRequest` → `ChatClientRequest`, `AdvisedResponse` → `ChatClientResponse`, `CallAroundAdvisor` → `CallAdvisor`
- Observability properties renamed: `include-prompt` → `log-prompt`, `include-completion` → `log-completion`
- Chat memory artifact names changed

### 1.0.0-RC3 (Apr 2025)

- `ChatOptions`: `Float` → `Double` for temperature, top-p, frequency-penalty, presence-penalty
- Code using `0.7f` will not compile — change to `0.7`

### 1.0.0 GA (May 2025)

First stable release. API stabilized from RC3.

### 1.1.0-RC1 (Aug 2025)

- `SpeechModel` renamed to `TextToSpeechModel`
- `SpeechPrompt` renamed to `TextToSpeechPrompt`
- Speed parameter: `Float` → `Double`
- New `BaseAdvisor` interface (simplified custom advisor creation)
- `RetrievalAugmentationAdvisor` replaces internal RAG pipeline
- `ToolCallAdvisor` for advisor-controlled tool execution

### 1.1.0 GA (Sep 2025)

Stable release. API stabilized from RC1.

## Known Issues

**Spring Boot 3.4:** Must set `spring.http.client.factory=jdk` — the default HTTP client factory causes issues with some AI providers.

**Multiple models:** Having multiple model starters on the classpath without setting `spring.ai.model.chat=<provider>` causes auto-configuration failure. Always set the property or disable auto-configured ChatClient.

## Migrating from Pre-1.0 to 1.0+

1. Update artifact IDs to new naming convention (M7 change)
2. Replace `ChatClient` usage: use `ChatModel` for low-level or new `ChatClient` for fluent API
3. Replace `Float` with `Double` for all chat options
4. Update advisor classes to new names (`CallAroundAdvisor` → `CallAdvisor`)
5. Switch VectorStore construction from constructors to builders
6. Update observation property names (`include-` → `log-`)

## Migrating from 1.0 to 1.1

1. Rename `SpeechModel` → `TextToSpeechModel` if using TTS
2. Consider migrating custom advisors to `BaseAdvisor` (simpler API)
3. Consider migrating RAG to `RetrievalAugmentationAdvisor` (modular pipeline)
4. Update `Speed` parameters from `Float` to `Double` in TTS
