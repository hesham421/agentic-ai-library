# Spring Boot Version Guide

Feature availability across Spring Boot versions. Use this when targeting a specific Boot version or deciding whether a feature is available.

## Feature Matrix

| Feature | Boot 3.2 | Boot 3.3 | Boot 3.4 | Boot 3.5 | Boot 4.0 |
|---------|----------|----------|----------|----------|----------|
| Virtual Threads | Yes (Java 21+) | Yes | Yes | Yes | Yes |
| RestClient | Yes | Yes | Yes | Yes | Yes |
| Docker Compose integration | Yes | Yes | Yes | Yes | Yes |
| `@ServiceConnection` | Yes | Yes | Yes | Yes | Yes |
| SSL/TLS Bundles | Yes (auto-reload) | Yes | Yes | Yes | Yes |
| CDS (Class Data Sharing) | — | Yes | Yes | Yes | Yes |
| Structured Logging | — | — | Yes | Yes | Yes |
| Global HTTP client timeouts | — | — | Yes | Yes | Yes |
| Actuator access control model | — | — | Yes | Yes | Yes |
| Bean background initialization | — | — | — | Yes | Yes |
| `spring-boot-starter-opentelemetry` | — | — | — | — | Yes |
| `@HttpExchange` auto-config | — | — | — | — | Yes |
| `RestTestClient` | — | — | — | — | Yes |
| Jackson 3 (default) | — | — | — | — | Yes |
| Kotlin Serialization support | — | — | — | — | Yes |
| API Versioning auto-config | — | — | — | — | Yes |
| `@Retryable` in Spring core | — | — | — | — | Yes |
| JSpecify null-safety | — | — | — | — | Yes |
| JUnit 6 | — | — | — | — | Yes |

## Minimum Requirements by Version

| Requirement | Boot 3.2 | Boot 3.3 | Boot 3.4 | Boot 3.5 | Boot 4.0 |
|-------------|----------|----------|----------|----------|----------|
| Java | 17+ | 17+ | 17+ | 17+ | 17+ |
| Jakarta EE | 9.1 | 9.1 | 9.1 | 9.1 | 11 |
| Spring Framework | 6.1 | 6.1 | 6.2 | 6.2 | 7.0 |
| Gradle | 7.5+ | 7.5+ | 7.6+ | 8.4+ | 8.14+ |

## SSL/TLS Bundles (Boot 3.1+, enhanced in 3.2+)

Named SSL bundles centralize trust material across web server, RestClient, Kafka, and RabbitMQ:

```yaml
spring:
  ssl:
    bundle:
      pem:
        my-service:
          keystore:
            certificate: classpath:my-cert.pem
            private-key: classpath:my-key.pem
          truststore:
            certificate: classpath:ca-cert.pem
          reload-on-update: true    # Boot 3.2+: auto-reload on file change
```

Apply to RestClient:

```java
RestClient client = builder
    .baseUrl("https://secure.example.com")
    .apply(ssl.fromBundle("my-service"))
    .build();
```

Apply to embedded server:

```yaml
server:
  ssl:
    bundle: my-service
```

## CDS — Class Data Sharing (Boot 3.3+)

Faster startup without GraalVM constraints. Extract CDS archive from a Boot jar:

```bash
java -Djarmode=tools -jar app.jar extract --destination app
cd app
java -XX:ArchiveClassesAtExit=app.jsa -jar app.jar    # Training run
java -XX:SharedArchiveFile=app.jsa -jar app.jar        # Production run
```

Typical improvement: 20–40% faster startup. Combine with virtual threads for best throughput-to-startup ratio.

## Global HTTP Client Configuration (Boot 3.4+)

Centralized timeouts for all auto-configured HTTP clients (RestClient, RestTemplate, WebClient):

```yaml
spring:
  http:
    clients:
      connect-timeout: 5s
      read-timeout: 30s
      redirects: dont-follow    # follow (default), dont-follow
```

Per-client overrides via builder customization still take precedence.
