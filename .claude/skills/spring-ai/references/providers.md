# Providers, MCP, and Multimodal

## Provider Configuration

Each provider has its own starter. Naming convention (since 1.0.0-M7):
- Chat/embedding: `spring-ai-starter-model-{provider}`
- Vector store: `spring-ai-starter-vector-store-{store}`
- MCP: `spring-ai-starter-mcp-{type}`

### Common Providers

**OpenAI:**
```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=gpt-4o
spring.ai.openai.chat.options.temperature=0.7
```

**Anthropic:**
```properties
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.options.model=claude-sonnet-4-20250514
```

**Ollama (local):**
```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3
```

**Azure OpenAI:**
```properties
spring.ai.azure.openai.api-key=${AZURE_OPENAI_API_KEY}
spring.ai.azure.openai.endpoint=${AZURE_OPENAI_ENDPOINT}
spring.ai.azure.openai.chat.options.deployment-name=gpt-4o
```

**Docker Model Runner (local, Spring AI 1.1+):**
```properties
spring.ai.openai.base-url=http://localhost:12434/engines
spring.ai.openai.api-key=any-value
spring.ai.openai.chat.options.model=ai/gemma3:4B-F16
spring.ai.openai.embedding.enabled=false    # DMR does not support embeddings
```

Requires Docker Desktop for Mac 4.40+ with `docker desktop enable model-runner --tcp 12434`. Models sourced from Docker Hub `ai/` namespace. Supports tool calling and streaming. Testcontainers support via `DockerModelRunnerContainer`.

### Multiple Models

When multiple model starters are on the classpath:

```properties
# Select default provider
spring.ai.model.chat=openai
spring.ai.model.embedding=openai

# Or disable auto-configured ChatClient and wire manually
spring.ai.chat.client.enabled=false
```

Manual multi-model setup:

```java
@Configuration
class AiConfig {
    @Bean
    ChatClient openAiClient(@Qualifier("openAiChatModel") ChatModel model) {
        return ChatClient.builder(model).build();
    }

    @Bean
    ChatClient anthropicClient(@Qualifier("anthropicChatModel") ChatModel model) {
        return ChatClient.builder(model).build();
    }
}
```

### Runtime Model Switching

Use `mutate()` to derive new model instances without creating new beans:

```java
OpenAiChatModel groqModel = openAiChatModel.mutate()
    .openAiApi(openAiApi.mutate()
        .baseUrl("https://api.groq.com/openai")
        .apiKey(System.getenv("GROQ_API_KEY"))
        .build())
    .defaultOptions(OpenAiChatOptions.builder()
        .model("llama3-70b-8192").build())
    .build();
```

Any OpenAI-compatible API (Groq, Together, Fireworks) works with the OpenAI starter via `mutate()`.

## Model Selection Guide

| Use Case | Recommended | Why |
|----------|-------------|-----|
| General-purpose | OpenAI GPT-4o, Anthropic Claude Sonnet | Best balance of quality/cost |
| Code generation | Anthropic Claude Sonnet, OpenAI GPT-4o | Strong at structured output |
| Local/private | Ollama or Docker Model Runner | No data leaves your network |
| High throughput | OpenAI GPT-4o-mini, Anthropic Claude Haiku | Low cost, fast |
| Multimodal | OpenAI GPT-4o, Anthropic Claude, Gemini | Image + text input |
| Embedding | OpenAI text-embedding-3-small | Good quality/cost ratio |

## MCP Integration

Spring AI has first-class Model Context Protocol support for both client and server roles.

### MCP Client

Consumes tools, resources, and prompts from external MCP servers:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```

```properties
spring.ai.mcp.client.stdio.servers.brave-search.command=npx
spring.ai.mcp.client.stdio.servers.brave-search.args=-y,@anthropic/mcp-server-brave-search
spring.ai.mcp.client.stdio.servers.brave-search.env.BRAVE_API_KEY=${BRAVE_API_KEY}
```

MCP tools are automatically available as Spring AI tools via `SyncMcpToolCallbackProvider`.

### MCP Server

Expose Spring AI tools as MCP endpoints:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

```java
@McpTool(description = "Get weather for a city")
record WeatherTool() {
    @McpToolCall
    WeatherResponse getWeather(@McpToolArg(description = "City name") String city) {
        return weatherService.getWeather(city);
    }
}
```

Supports STDIO, SSE, and Streamable-HTTP transports.

## Multimodal Support

Input-only via `Media` on `UserMessage`:

```java
String response = ChatClient.create(chatModel).prompt()
    .user(u -> u.text("Describe this image")
        .media(MimeTypeUtils.IMAGE_PNG, new ClassPathResource("/photo.png")))
    .call().content();
```

**Supported models:** Anthropic Claude 3+, OpenAI GPT-4o, Azure OpenAI GPT-4o, Vertex AI Gemini, Ollama (LLaVA, Llama 3.2), Mistral AI Pixtral.

**Limitation:** Media is input-only (`UserMessage`). Responses are always text. For image generation use `ImageModel` (DALL-E, Stability AI). For audio use `TextToSpeechModel` (OpenAI, ElevenLabs) and `TranscriptionModel` (OpenAI Whisper).

## Image Generation

```java
ImageResponse response = imageModel.call(
    new ImagePrompt("A cat wearing a top hat",
        ImageOptions.builder()
            .model("dall-e-3")
            .width(1024).height(1024)
            .build()));

String imageUrl = response.getResult().getOutput().getUrl();
```

## Testing Strategies

Spring AI does not provide mock model utilities. Approaches:

- **Unit tests** — mock `ChatModel` with Mockito. Return fixed `ChatResponse` objects
- **Integration tests** — use real APIs (budget for costs) or Ollama via Testcontainers
- **Evaluation** — `RelevancyEvaluator` and `FactCheckingEvaluator` for LLM-as-a-judge patterns
- **Vector store tests** — use `SimpleVectorStore` (in-memory) for development, Testcontainers for integration

```java
RelevancyEvaluator evaluator = new RelevancyEvaluator(ChatClient.builder(chatModel));
EvaluationRequest req = new EvaluationRequest(question, retrievedContext, aiResponse);
assertThat(evaluator.evaluate(req).isPass()).isTrue();
```

## Known Issues and Migration Notes

| Version | Breaking Change |
|---------|----------------|
| 1.0.0-M7 | Starter artifacts completely renamed (old names no longer exist) |
| 1.0.0-RC1 | Advisor API renamed: `AdvisedRequest` -> `ChatClientRequest`, `CallAroundAdvisor` -> `CallAdvisor` |
| 1.0.0-RC3 | `ChatOptions` changed from `Float` to `Double` (temperature, penalties) |
| 1.1.0-RC1 | `SpeechModel` renamed to `TextToSpeechModel` |

**Spring Boot 3.4 bug:** Must set `spring.http.client.factory=jdk` — the default HTTP client factory causes issues with some providers.
