# Spring Boot 4.0 Migration Guide

Migration reference for upgrading from Spring Boot 3.x to 4.0. Read this when planning or executing a Boot 4.0 migration.

## Migration Strategy

1. **Upgrade to Boot 3.5.x first** — fix all deprecation warnings
2. Add `spring-boot-properties-migrator` to detect renamed properties
3. Use `spring-boot-starter-classic` as a transitional bridge
4. Migrate in phases: starters → Jackson → testing → nullness

## Starter Renames

| Boot 3.x | Boot 4.0 |
|----------|----------|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-web-services` | `spring-boot-starter-webservices` |
| `spring-boot-starter-aop` | `spring-boot-starter-aspectj` |
| (included in data starters) | `spring-boot-starter-flyway` (now separate) |
| (included in data starters) | `spring-boot-starter-liquibase` (now separate) |

**Transitional aid:** `spring-boot-starter-classic` and `spring-boot-starter-test-classic` provide the old starter names temporarily.

## Jackson 3 Migration

Jackson 3 is the default in Boot 4.0. This is the highest-impact change for most applications.

### Group ID and Package Changes

```
com.fasterxml.jackson → tools.jackson (except jackson-annotations)
```

### Annotation Renames

| Boot 3.x | Boot 4.0 |
|----------|----------|
| `@JsonComponent` | `@JacksonComponent` |
| `@JsonMixin` | `@JacksonMixin` |
| `JsonObjectSerializer` | `ObjectValueSerializer` |
| `JsonValueDeserializer` | `ObjectValueDeserializer` |
| `Jackson2ObjectMapperBuilderCustomizer` | `JsonMapperBuilderCustomizer` |

### Property Changes

```yaml
# Boot 3.x
spring.jackson.read.unknown-properties: fail
spring.jackson.write.dates-as-timestamps: false

# Boot 4.0
spring.jackson.json.read.unknown-properties: fail
spring.jackson.json.write.dates-as-timestamps: false
```

### Backward Compatibility

For incremental migration, use the deprecated Jackson 2 bridge:

```gradle
implementation 'org.springframework.boot:spring-boot-jackson2'
```

```yaml
spring.jackson.use-jackson2-defaults: true
```

## Testing Changes

### Mock Bean Migration

| Boot 3.x | Boot 4.0 |
|----------|----------|
| `@MockBean` | `@MockitoBean` |
| `@SpyBean` | `@MockitoSpyBean` |

**Critical:** `@MockitoBean` / `@MockitoSpyBean` can only be placed on **test classes**, not on `@Configuration` classes.

### Explicit Test Auto-Configuration

`@SpringBootTest` no longer auto-provides MockMVC, WebClient, or TestRestTemplate. Add explicitly:

```java
@SpringBootTest
@AutoConfigureMockMvc                  // Required for MockMvc injection
class OrderControllerTest { }

@SpringBootTest
@AutoConfigureRestTestClient           // New: RestTestClient (non-reactive)
class OrderApiTest { }
```

### Test Starters

Framework-specific test starters are now separate:
- `spring-boot-starter-security-test` — for `@WithMockUser`
- `spring-boot-starter-webmvc-test` — for MockMvc slice tests

## Removed Technologies

| Removed | Alternative |
|---------|------------|
| Undertow | Tomcat or Jetty |
| Spock | JUnit 6 |
| Embedded launch scripts | Use container entrypoint or systemd |
| Spring Session Hazelcast | Spring Session Redis or JDBC |

## Property Renames

| Boot 3.x | Boot 4.0 |
|----------|----------|
| `management.tracing.enabled` | `management.tracing.export.enabled` |
| `spring.dao.exceptiontranslation.enabled` | `spring.persistence.exceptiontranslation.enabled` |
| `spring.session.redis.*` | `spring.session.data.redis.*` |
| `spring.data.mongodb.*` | `spring.mongodb.*` |
| `management.health.mongo.*` | `management.health.mongodb.*` |

## Nullness Migration

Spring Framework 7 migrated from JSR 305 to JSpecify:

```java
// Boot 3.x
import org.springframework.lang.Nullable;

// Boot 4.0
import org.jspecify.annotations.Nullable;
```

## Class Relocations

| Boot 3.x | Boot 4.0 |
|----------|----------|
| `o.s.b.env.EnvironmentPostProcessor` | `o.s.b.EnvironmentPostProcessor` |
| `ConditionalOnEnabledTracing` | `ConditionalOnEnabledTracingExport` |
| `HttpMessageConverters` | `ClientHttpMessageConvertersCustomizer` / `ServerHttpMessageConvertersCustomizer` |

## Build Changes

- **Maven:** Optional dependencies no longer included in uber jars. Enable with `<includeOptional>true</includeOptional>`
- **Gradle:** Minimum 8.14 (Gradle 9 also supported)
- **Hibernate:** Annotation processor renamed `hibernate-jpamodelgen` → `hibernate-processor`
- **Spring Retry:** `@Retryable` moved into Spring Framework 7 core. Remove the separate Spring Retry dependency.
