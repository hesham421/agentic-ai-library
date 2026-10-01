# Dev Services — Docker Compose & Testcontainers

Patterns for local development with auto-configured service connections. Load when setting up local development infrastructure.

## Docker Compose Integration (Boot 3.1+)

### Setup

```gradle
developmentOnly 'org.springframework.boot:spring-boot-docker-compose'
```

Spring Boot auto-detects `compose.yml` in the project root and manages container lifecycle.

### compose.yml

```yaml
services:
  postgres:
    image: postgres:17
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: myapp
      POSTGRES_USER: myapp
      POSTGRES_PASSWORD: myapp
  redis:
    image: redis:7
    ports: ["6379:6379"]
```

### Configuration

```yaml
# application-local.yml
spring:
  docker:
    compose:
      lifecycle-management: start_only    # Don't stop containers on app shutdown
      skip:
        in-tests: true                    # Don't start Docker Compose during tests
```

Spring auto-configures `spring.datasource.*`, `spring.data.redis.*`, etc. via `@ServiceConnection`. No manual connection properties needed for local dev.

### Supported Services

Docker Compose auto-configuration works with: PostgreSQL, MySQL, MariaDB, MongoDB, Redis, Elasticsearch, Kafka, RabbitMQ, Cassandra, Neo4j, Zipkin, and more. The image name determines the service type.

## Testcontainers at Dev Time (Boot 3.1+)

Alternative to Docker Compose — containers managed by the JVM with programmatic control.

### Setup

```gradle
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:postgresql'
```

### TestApplication Launcher

Create a test-scoped application launcher:

```java
// src/test/java/com/example/app/TestAppApplication.java
@TestConfiguration(proxyBeanMethods = false)
class DevServices {

    @Bean
    @ServiceConnection
    @RestartScope    // Survives DevTools restarts
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17");
    }

    @Bean
    @ServiceConnection
    @RestartScope
    GenericContainer<?> redis() {
        return new GenericContainer<>("redis:7").withExposedPorts(6379);
    }
}

public class TestAppApplication {
    public static void main(String[] args) {
        SpringApplication.from(AppApplication::main)
            .with(DevServices.class)
            .run(args);
    }
}
```

Run with:
```bash
./gradlew bootTestRun    # Gradle
./mvnw spring-boot:test-run    # Maven
```

### Parallel Container Startup

```yaml
spring:
  testcontainers:
    beans:
      startup: parallel    # Start all containers concurrently
```

### @ServiceConnection vs @DynamicPropertySource

**Prefer `@ServiceConnection`** — it auto-configures the right connection properties based on the container type:

```java
// Good — auto-configures spring.datasource.*, spring.flyway.*, etc.
@Bean
@ServiceConnection
PostgreSQLContainer<?> postgres() {
    return new PostgreSQLContainer<>("postgres:17");
}

// Avoid — manual property wiring, fragile, misses derived properties
@DynamicPropertySource
static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}
```

`@DynamicPropertySource` is still useful for non-standard or custom services where no `@ServiceConnection` support exists.

## DevTools Integration

### Setup

```gradle
developmentOnly 'org.springframework.boot:spring-boot-devtools'
```

### Key Settings

```yaml
spring:
  devtools:
    restart:
      enabled: true
      additional-paths: src/main/java    # Watch additional paths
    livereload:
      enabled: true    # Boot 4.0: disabled by default, opt-in
```

### @RestartScope

Mark Testcontainers beans with `@RestartScope` so containers survive DevTools restarts:

```java
@Bean
@ServiceConnection
@RestartScope    // Container persists across DevTools restarts
PostgreSQLContainer<?> postgres() { ... }
```

Without `@RestartScope`, each restart creates a new container — slow and wasteful.

## Decision: Docker Compose vs Testcontainers

| Aspect | Docker Compose | Testcontainers |
|--------|---------------|----------------|
| Configuration | YAML file | Java code |
| Lifecycle | Separate from JVM | Managed by JVM |
| Shared across services | Yes (shared compose file) | No (per-project) |
| Programmatic control | Limited | Full (wait strategies, init scripts) |
| CI compatibility | Needs Docker Compose CLI | Needs Docker daemon only |
| **Recommendation** | Teams with shared infra | Single-developer, testing-focused |

Both approaches use `@ServiceConnection` for auto-configuration — switching between them requires minimal code changes.
