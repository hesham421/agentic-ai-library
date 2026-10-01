# RAG and Advisors

## RAG Architecture

Two approaches, from simple to modular:

### QuestionAnswerAdvisor (Simple)

Naive RAG — embeds the query, retrieves similar documents, augments the prompt:

```java
var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
    .searchRequest(SearchRequest.builder()
        .similarityThreshold(0.8d)
        .topK(6)
        .build())
    .build();

chatClient.prompt()
    .user("What is our refund policy?")
    .advisors(qaAdvisor)
    .call().content();
```

### RetrievalAugmentationAdvisor (Modular)

Composable pipeline with pre-retrieval transformers, retrievers, and post-retrieval augmenters:

```java
Advisor rag = RetrievalAugmentationAdvisor.builder()
    .queryTransformers(RewriteQueryTransformer.builder()
        .chatClientBuilder(chatClientBuilder.build().mutate()).build())
    .documentRetriever(VectorStoreDocumentRetriever.builder()
        .similarityThreshold(0.50)
        .vectorStore(vectorStore)
        .build())
    .queryAugmenter(ContextualQueryAugmenter.builder()
        .allowEmptyContext(true)
        .build())
    .build();
```

### Pre-Retrieval Query Transformers

| Transformer | Purpose |
|------------|---------|
| `CompressionQueryTransformer` | Resolves pronouns using conversation history |
| `RewriteQueryTransformer` | Rephrases query for better retrieval |
| `TranslationQueryTransformer` | Translates to target language before retrieval |
| `MultiQueryExpander` | Generates N semantically diverse queries |

Each transformer requires a `ChatClientBuilder` — it uses an LLM to transform the query.

## Vector Stores

23+ implementations sharing a common abstraction. Key ones:

| Store | Starter | Notes |
|-------|---------|-------|
| PGvector | `spring-ai-starter-vector-store-pgvector` | PostgreSQL extension, good default choice |
| Qdrant | `spring-ai-starter-vector-store-qdrant` | Purpose-built, high performance |
| Redis | `spring-ai-starter-vector-store-redis` | Good if already using Redis |
| Elasticsearch | `spring-ai-starter-vector-store-elasticsearch` | Full-text + vector hybrid search |
| Chroma | `spring-ai-starter-vector-store-chroma` | Simple, good for dev/testing |
| SimpleVectorStore | (core) | In-memory, for development and testing only |

### Schema Initialization

Schema is NOT created automatically. Opt in explicitly:

```properties
spring.ai.vectorstore.initialize-schema=true
```

Without this, the application starts but vector store operations fail at runtime. For production, manage schemas via Flyway/Liquibase instead of auto-initialization.

### Portable Filter Expressions

All vector stores support a common filter DSL:

```java
SearchRequest.builder()
    .query("refund policy")
    .filterExpression("genre == 'legal' && year >= 2024")
    .topK(5)
    .build();
```

Works across all vector store implementations — translated to provider-native filters at runtime.

## Document ETL Pipeline

### Readers

| Reader | Formats |
|--------|---------|
| `PagePdfDocumentReader` | PDF (page-level chunks) |
| `ParagraphPdfDocumentReader` | PDF (paragraph-level chunks) |
| `TikaDocumentReader` | DOCX, PPTX, HTML, and 1000+ formats via Apache Tika |
| `JsonReader` | JSON documents |
| `TextReader` | Plain text |
| `MarkdownDocumentReader` | Markdown |
| `JsoupDocumentReader` | HTML with CSS selectors |

### Transformers

```java
var splitter = new TokenTextSplitter(
    800,    // default chunk size (tokens, CL100K_BASE encoding)
    350,    // min chunk size
    5,      // overlap between chunks
    10000,  // max characters per document
    true    // keep separator
);
```

**Metadata enrichers** (use an LLM to add metadata):
- `KeywordMetadataEnricher` — extracts keywords from each chunk
- `SummaryMetadataEnricher` — generates summaries for current, previous, and next chunks

### Ingestion Pattern

```java
var reader = new PagePdfDocumentReader(pdfResource);
var splitter = new TokenTextSplitter();

List<Document> documents = reader.read();
List<Document> chunks = splitter.apply(documents);
vectorStore.add(chunks);
```

**Pitfall:** `TokenCountBatchingStrategy` defaults to 8191 tokens (OpenAI standard) with 10% reserve. A single document chunk exceeding this limit throws an exception. Configure batch size for your embedding model.

## Custom Advisors

Implement `BaseAdvisor` for simple before/after hooks:

```java
public class TenantContextAdvisor implements BaseAdvisor {

    @Override
    public int getOrder() { return 0; }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String tenantId = TenantContext.current().getId();
        var systemMessage = new SystemMessage("You are assisting tenant: " + tenantId);
        return ChatClientRequest.from(request)
            .messages(m -> m.add(0, systemMessage))
            .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }
}
```

For sync-only advisors implement `CallAdvisor`. For streaming implement `StreamAdvisor`. `BaseAdvisor` covers both.

## Advisor Ordering

Advisors execute in `getOrder()` order on requests (lower first) and reverse on responses (stack behavior):

```
Order 0: TenantAdvisor
Order 100: MemoryAdvisor
Order 200: RAG Advisor
Order 300: SafeGuardAdvisor

Request:  Tenant -> Memory -> RAG -> SafeGuard -> LLM
Response: SafeGuard -> RAG -> Memory -> Tenant -> caller
```

Place memory advisors before RAG advisors — memory adds context that RAG query transformers can use.

## Key Guidelines

- **Start with `QuestionAnswerAdvisor`** — switch to `RetrievalAugmentationAdvisor` when you need query rewriting or custom retrieval
- **Configure chunk size for your domain** — 800 tokens is a reasonable default, but legal documents need larger chunks, chat logs need smaller
- **Use filter expressions** — don't retrieve everything and filter in code. Push filters to the vector store
- **Test with `SimpleVectorStore`** — no infrastructure needed for development. Switch to a real store for integration tests
- **Monitor retrieval quality** — use `RelevancyEvaluator` to measure if retrieved documents actually answer the question
