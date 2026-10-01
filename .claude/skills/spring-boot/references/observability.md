# Observability in Spring Boot

Deep-dive reference for configuring observability with Micrometer and OpenTelemetry. Load when working on metrics, tracing, or log correlation.

## Stack Overview

```
Application Code
    ↓ @Observed, @Timed, @Counted
Micrometer Observation API
    ↓
OpenTelemetry SDK (traces, metrics, logs)
    ↓ OTLP export
Collector (Grafana Agent, OTel Collector, Datadog Agent)
```

## Setup

### Boot 4.0

```gradle
implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

The `starter-opentelemetry` brings OTLP exporters and auto-configures the OpenTelemetry SDK.

### Boot 3.2–3.5

```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-tracing-bridge-otel'
implementation 'io.opentelemetry:opentelemetry-exporter-otlp'
```

## Configuration

```yaml
management:
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
    metrics:
      export:
        endpoint: http://otel-collector:4318/v1/metrics
        step: 30s
  tracing:
    sampling:
      probability: 1.0    # 1.0 for dev, 0.1–0.5 for production
  observations:
    annotations:
      enabled: true        # Enables @Observed, @Timed, @Counted
  metrics:
    tags:
      application: ${spring.application.name}
```

### Log Correlation

Trace and span IDs are automatically injected into MDC when tracing is active. With structured logging (Boot 3.4+):

```yaml
logging:
  structured:
    format:
      console: ecs
```

This includes `trace.id` and `span.id` in every log line automatically.

For Boot 3.2–3.3, add the pattern manually:

```yaml
logging:
  pattern:
    console: "%d{HH:mm:ss.SSS} %-5level [%thread] [%X{traceId}/%X{spanId}] %logger{36} - %msg%n"
```

## Custom Observations

### Using Annotations

```java
@Service
class OrderService {

    @Observed(name = "order.process",
              contextualName = "process-order",
              lowCardinalityKeyValues = {"order.type", "standard"})
    public Order process(OrderRequest request) {
        // Automatically traced and metered
    }
}
```

### Programmatic Observation

Use when you need dynamic key-values or span events:

```java
@Service
class PaymentService {
    private final ObservationRegistry registry;

    Payment charge(PaymentRequest request) {
        return Observation.createNotStarted("payment.charge", registry)
            .lowCardinalityKeyValue("payment.method", request.method().name())
            .observe(() -> doCharge(request));
    }
}
```

### Key-Value Annotations (Boot 4.0)

```java
@Observed(name = "order.process")
public Order process(@ObservationKeyValue("order.region") String region,
                     OrderRequest request) {
    // region is added as a low-cardinality key-value
}
```

## Auto-Instrumented Components

Spring Boot auto-instruments these without any code changes:

| Component | Observation Name |
|-----------|-----------------|
| HTTP server (MVC/WebFlux) | `http.server.requests` |
| HTTP client (RestClient/WebClient) | `http.client.requests` |
| JDBC (via DataSource proxy) | `jdbc.query` |
| Kafka consumer/producer | `spring.kafka.listener` / `spring.kafka.template` |
| RabbitMQ | `spring.rabbit.listener` / `spring.rabbit.template` |
| Scheduled tasks | `spring.scheduling` |
| Spring Data repositories | `spring.data.repository` |

## Context Propagation

Trace context propagates automatically across:
- HTTP (via `traceparent` / `tracestate` W3C headers)
- Kafka (via record headers)
- RabbitMQ (via message headers)
- `@Async` / `TaskExecutor` (via `ContextPropagatingTaskDecorator`)

For custom async boundaries, wrap with `ContextSnapshot`:

```java
ContextSnapshot snapshot = ContextSnapshot.captureAll();
executor.submit(snapshot.wrap(() -> {
    // Trace context is available here
}));
```

## Actuator Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true    # Kubernetes liveness/readiness
```

- `/actuator/health` — liveness/readiness probes (enabled by default in Boot 4.0)
- `/actuator/prometheus` — Prometheus scrape endpoint
- `/actuator/metrics/{name}` — individual metric lookup

## Production Checklist

- [ ] Set sampling probability < 1.0 (e.g., 0.1 for 10%)
- [ ] Use low-cardinality key-values only (never user IDs, request bodies)
- [ ] Verify OTLP endpoint is reachable from the deployment environment
- [ ] Configure `management.metrics.tags.application` for service identification
- [ ] Enable health probes for Kubernetes readiness/liveness
- [ ] Set `management.endpoint.health.show-details: when_authorized` (never `always` in production)
