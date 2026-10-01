# Reusable Agentic AI Library

A domain-agnostic, reusable Agentic AI foundation built on [Spring AI](https://spring.io/projects/spring-ai),
designed to be embedded into multiple independent Products.

> **Status: Bootstrap phase.** This repository contains an empty Spring Boot application, the
> technology baseline, and curated Claude Code Skills and references. No library capability
> has been implemented yet. The architecture will be designed before any implementation.

## Purpose

Many Products need the same AI capabilities: running agents, calling models, managing prompts and
context, remembering conversations, retrieving knowledge, calling tools and MCP servers, asking
humans for approval, and being observable and evaluable. This library provides those capabilities
**once**, in a reusable and configuration-driven way, so each Product only supplies its own domain.

```text
                         Reusable Agentic AI Library
                                      │
                   ┌──────────────────┼──────────────────┐
                   │                  │                  │
               Product A          Product B          Product C
                   │                  │                  │
               Own Domain          Own Domain          Own Domain
               Own APIs            Own APIs            Own APIs
```

## Architecture Principle

```text
Product
   ↓
Reusable Agentic AI Library
   ↓
Spring AI
   ↓
LLM / MCP / Vector Store / External Systems
```

- **Products** own their domain logic, data, APIs, and business rules.
- **The library** provides reusable AI capabilities, configured by each Product.
- **Spring AI** provides the model, tool, advisor, RAG, memory, and MCP abstractions the library builds on.

## Important Boundary

The library must **not** contain Product-specific business logic (HR, Finance, Education, CRM, ERP,
Sales, Government, or any other domain):

```text
❌ Employee
❌ Invoice
❌ Student
❌ Order
```

Domain data enters the library only generically, as external Product data or as tool schemas. The
library works with generic concepts:

```text
✓ Agent
✓ Tool
✓ Knowledge
✓ Memory
✓ Context
✓ Model
✓ MCP
✓ Permission
✓ Execution
```

## Future Scope (not yet implemented)

Agent Runtime · Agent Configuration · LLM Abstraction · Prompt Management · Context Management ·
Memory · Knowledge / RAG · Tool Calling · API Tools · MCP · Permissions · Human Approval ·
Observability · Agent Evaluation · Agentic Orchestration

## Technology Baseline

| Component | Version | Notes |
|-----------|---------|-------|
| Java | 25 (LTS) | Enforced by `maven-enforcer-plugin` |
| Spring Boot | 4.1.1 | Latest stable at bootstrap (2026-10-01) |
| Spring AI | 2.0.1 | BOM imported for version alignment. No Spring AI module is on the classpath yet. |
| Maven | 3.9+ | Enforced |

### Compatibility

These were verified against official sources rather than assumed:

- **Spring AI 2.0.1 ↔ Spring Boot 4.1.1:** The Spring AI `v2.0.1` build POM declares
  `spring-boot.version` **4.1.1**, so this is the exact pairing Spring AI is built and tested on.
  (Spring AI 1.x does **not** support Boot 4. Boot 4 requires Spring AI 2.x.)
- **Spring Boot 4.1 ↔ Java 25:** The Spring Boot 4.1 system requirements state Java 17+,
  *"compatible with versions up to and including Java 26"*.
- **Spring AI 2.0.1 ↔ Java 25:** Spring AI's baseline is Java 17. A throwaway probe compiled and
  ran a `ChatClient` call through `spring-ai-client-chat` 2.0.1 on Boot 4.1.1 / Java 25
  successfully. It was not committed, to keep the bootstrap dependency-free.

No compatibility issue was found. Known, harmless build-time notices on Java 25:

- `sun.misc.Unsafe::staticFieldBase` warning, emitted by Maven 3.9.10's own Guice, not by this
  project. Upgrading Maven to the latest 3.9.x / 4.x removes it.
- `Sharing is only supported for boot loader classes…` CDS notice in tests, a side effect of
  attaching Mockito as a Java agent. The agent is attached explicitly because JDK 21+ deprecates
  dynamic agent self-attachment.

## Getting Started

Requires a **JDK 25** (e.g. [Eclipse Temurin 25](https://adoptium.net/)) with `JAVA_HOME` pointing
to it.

```bash
java -version      # must report 25
mvn -version       # must report Java version: 25
mvn clean test
```

PowerShell, for a single session, if JDK 25 is not your default:

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Repository Layout

```text
.
├── .claude/
│   ├── skills/          # Curated Claude Code Skills (see .claude/skills/README.md)
│   └── references/      # Reference repositories, documentation only (see .claude/references/README.md)
├── src/
│   ├── main/java/io/agenticai/Application.java
│   ├── main/resources/application.properties
│   └── test/java/io/agenticai/ApplicationTests.java
├── pom.xml
├── README.md
├── .gitignore
└── .editorconfig
```

## Development with Claude Code

- **Skills** in [`.claude/skills/`](.claude/skills/README.md) give Claude Code current Spring AI,
  Spring Boot 4, and Spring testing guidance. They were selected and security-reviewed for this
  project. Three local `gov-*` skills validate library code against the Product-domain boundary,
  error-handling, and caching rules. They review code; they never generate it.
- **References** in [`.claude/references/`](.claude/references/README.md) list the external
  repositories to study (official Spring AI first). Nothing from them is copied into this project.
- When sources disagree, the official Spring AI 2.0 documentation and this project's `pom.xml` win.

## Next Phase

Design the library architecture: module layout (e.g. core / auto-configuration / starter),
public SPI, configuration model, and boundaries. The design comes before any capability is
implemented. The current single-module application and the `spring-boot-maven-plugin` (fat-jar
repackaging) are bootstrap scaffolding and are expected to change once the library module
structure is decided.
