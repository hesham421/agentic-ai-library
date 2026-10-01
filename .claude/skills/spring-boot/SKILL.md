---
name: spring-boot
description: Guide Spring Boot project setup, configuration, profiles, observability, and auto-configuration. Use when starting new Spring Boot projects, structuring configuration, working with profiles, troubleshooting auto-configuration, enabling virtual threads, or configuring observability.
version: 0.2.0
tags: [core, configuration, profiles, auto-configuration, observability, virtual-threads]
triggers:
  - setting up a new Spring Boot project
  - configuring application properties or YAML
  - working with Spring profiles
  - understanding or customizing auto-configuration
  - enabling virtual threads or observability
  - choosing between RestClient, WebClient, or HTTP exchange interfaces
  - configuring dev services (Docker Compose, Testcontainers)
  - troubleshooting bean creation or property resolution
spring_versions:
  boot: ">=3.2"
  latest_targeted: "4.0"
  framework: "7.0"
depends_on: [spring-framework]
---

# Spring Boot

Core conventions for Spring Boot application setup, configuration, and runtime. Targets **Boot 4.0** (Spring Framework 7, Jakarta EE 11, Java 17+). Most guidance applies to Boot 3.2+ — version-specific features are annotated.

**Do NOT Load** for framework-specific topics (security, data, web, testing) — use the dedicated skill instead.

## When to Use

- Starting a new Spring Boot project or evaluating project structure
- Configuring properties, profiles, or environment-specific overrides
- Debugging auto-configuration issues ("why isn't my bean created?")
- Enabling virtual threads, observability, or structured logging
- Setting up local dev services (Docker Compose, Testcontainers)
- Choosing HTTP client strategy or embedded server

## Configuration

### Type-Safe Configuration Properties

Always bind configuration to typed records. Never scatter `@Value` across services.

```java
@ConfigurationProperties(prefix = "app.order")
record OrderProperties(Duration timeout, int maxRetries, URI serviceUrl) {}
```

Enable scanning in the root application class with `@ConfigurationPropertiesScan`.

### Profile Strategy

| Profile | Purpose | Activated by |
|---------|---------|-------------|
| (none) | Production-ready defaults | Default |
| `local` | Local dev (Docker Compose, debug logging) | `--spring.profiles.active=local` |
| `test` | Test overrides | `@ActiveProfiles("test")` |

- **2–3 profiles maximum.** Per-deployment variation belongs in environment variables, not profiles
- **Never use profiles for feature flags** — use configuration properties with `@ConditionalOnProperty`
- **Default config must be production-ready** — profiles only override what is genuinely different

### Debugging Property Resolution

When a property isn't taking effect, check in this order:
1. **Typo or wrong prefix** — compare against `@ConfigurationProperties` prefix and relaxed binding rules
2. **Profile not active** — verify `spring.profiles.active` in startup logs
3. **Override precedence** — env vars and CLI args override YAML. Run with `--debug` to see property sources
4. **Missing `@ConfigurationPropertiesScan`** — properties class exists but is never bound

## Virtual Threads

**Boot 3.2+ / Java 21+:** Enable with a single property:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

This auto-applies to Tomcat, Jetty, Kafka listeners, RabbitMQ, task execution, and scheduling. No thread pool tuning needed.

**Gotcha:** For daemon-thread-only apps (e.g., only `@Scheduled` tasks), also set `spring.main.keep-alive=true` — otherwise the JVM exits immediately since virtual threads are daemon threads.

## HTTP Clients

| Client | Use When |
|--------|----------|
| `RestClient` | Synchronous HTTP calls (replaces `RestTemplate`) |
| `WebClient` | Reactive / non-blocking HTTP |
| `@HttpExchange` interfaces | Declarative HTTP APIs (**Boot 4.0+**: auto-configured via `@ImportHttpServices`) |

```java
@Service
class OrderClient {
    private final RestClient rest;

    OrderClient(RestClient.Builder builder) {
        this.rest = builder.baseUrl("https://orders.example.com").build();
    }
}
```

> **Boot 3.x:** `RestTemplate` still works but is deprecated. Migrate to `RestClient` — the API is similar.

## Observability

**Boot 4.0:** Use `spring-boot-starter-opentelemetry` for traces, metrics, and log correlation.

> **Boot 3.2–3.5:** Use `spring-boot-starter-actuator` with `micrometer-tracing-bridge-otel` and `opentelemetry-exporter-otlp` manually.

```yaml
management:
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
    metrics:
      export:
        endpoint: http://otel-collector:4318/v1/metrics
  observations:
    annotations:
      enabled: true    # Enables @Observed, @Timed, @Counted
```

Spring auto-instruments HTTP server/client, JDBC, Kafka, and RabbitMQ. Use `@Observed` for custom business operations — avoid raw Micrometer APIs.

## Structured Logging

**Boot 3.4+:** Built-in JSON logging — no custom Logback encoder needed:

```yaml
logging:
  structured:
    format:
      console: ecs    # Also: gelf, logstash
```

> **Boot 3.2–3.3:** Requires a manual Logstash Logback encoder. See `references/logging-pre34.md`.

Disable console logging entirely with `logging.console.enabled=false` (**Boot 4.0+**).

## Dev Services

### Docker Compose (Boot 3.1+)

```gradle
developmentOnly 'org.springframework.boot:spring-boot-docker-compose'
```

Spring auto-detects `compose.yml` and configures connections via `@ServiceConnection`. No manual `spring.datasource.*` needed for local dev.

### Testcontainers at Dev Time (Boot 3.1+)

```java
@TestConfiguration(proxyBeanMethods = false)
class DevServices {
    @Bean
    @ServiceConnection
    @RestartScope
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17");
    }
}
```

Run with `spring-boot:test-run` (Maven) or `bootTestRun` (Gradle). Use `@ServiceConnection` instead of `@DynamicPropertySource` — it auto-configures the connection.

## Auto-Configuration Troubleshooting

When a bean isn't created, follow this path:
1. **Run with `--debug`** — read the conditions evaluation report
2. **Check the negative match reason** — missing class? missing property? wrong profile?
3. **Verify package scanning** — `@SpringBootApplication` scans from its package down. Beans in sibling/parent packages are invisible
4. **Check `@Conditional*` annotations** — `@ConditionalOnProperty`, `@ConditionalOnClass`, `@ConditionalOnMissingBean` are the usual suspects

Only exclude auto-configuration when you have a specific reason **and** a replacement:

```java
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
```

## Key Decisions

### Embedded Server

| Server | Use Case |
|--------|----------|
| Tomcat (default) | Standard servlet-based apps |
| Jetty | Alternative servlet container |
| Netty | Reactive (WebFlux), non-blocking |

> **Boot 4.0:** Undertow was removed (lacks Servlet 6.1 support). Migrate to Tomcat or Jetty.

### Startup Optimization

| Approach | Tradeoff | When |
|----------|----------|------|
| Virtual threads | No build impact, runtime concurrency | Java 21+, most apps |
| CDS (Class Data Sharing) | Moderate startup improvement, standard JVM | Boot 3.3+, `java -Djarmode=tools extract` |
| GraalVM native image | Fastest startup, longest build, reflection constraints | Serverless, CLI tools |

### Starters (Boot 4.0)

Boot 4.0 renamed several starters:
- `spring-boot-starter-web` → **`spring-boot-starter-webmvc`**
- `spring-boot-starter-aop` → **`spring-boot-starter-aspectj`**
- Flyway/Liquibase now require dedicated starters: **`spring-boot-starter-flyway`**

Use `spring-boot-starter-classic` as a transitional bridge during migration.

## Anti-Patterns

- **The Scattered Value** — `@Value` annotations spread across services instead of `@ConfigurationProperties` records. Makes configuration undiscoverable and unvalidatable
- **The Profile Explosion** — profiles for `dev`, `staging`, `qa`, `uat`, `preprod`, `prod`. Use env vars for per-deployment values; profiles for fundamentally different behavior
- **The Silent Override** — `spring.main.allow-bean-definition-overriding=true` hides wiring errors. Fix the duplicate bean instead
- **The Eager Excluder** — excluding auto-configuration to "simplify" without understanding why it activated. Run `--debug` first
- **The Orphaned Scanner** — `@ComponentScan` with custom base packages bypassing root-package convention. Causes subtle scanning gaps
- **The Version Pinner** — overriding managed dependency versions without cause. One override can break transitive alignment across the entire BOM

## Resource Files

Load on demand for specific topics:

- **`references/version-guide.md`** — Feature-to-version matrix, Boot 3.2 through 4.0
- **`references/migration-4x.md`** — Boot 4.0 migration: starter renames, Jackson 3, testing changes, removed features
- **`references/observability.md`** — Full Micrometer/OTLP configuration, custom observations, context propagation
- **`references/dev-services.md`** — Docker Compose and Testcontainers patterns, service connections, DevTools integration

## References

- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/reference/)
- [Common Application Properties](https://docs.spring.io/spring-boot/appendix/application-properties/)
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Auto-configuration Classes](https://docs.spring.io/spring-boot/specification/auto-configuration.html)
