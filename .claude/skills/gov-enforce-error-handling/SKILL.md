---
name: gov-enforce-error-handling
description: "ERROR HANDLING ENFORCER — validates that library errors use one library exception type carrying a stable, registered error code and a category; rejects raw runtime exceptions for runtime failures, inline message strings, swallowed exceptions, and messages that leak prompts or secrets. Validation only. Use when reviewing any code that throws, catches, or maps exceptions."
---

# Skill: gov-enforce-error-handling

## Description

**ERROR HANDLING GOVERNANCE ENFORCER.** Products embed this library and must be able to branch
on, translate, and localize its failures. That only works if every runtime failure carries a
**stable error code** and a **category**, never just a message string. This skill checks for
that and reports violations. It does not modify code.

## When to Use

- After any code that throws, catches, wraps, or maps exceptions is written or changed
- When a new error code is introduced
- As part of [`gov-enforce-library-contract`](../gov-enforce-library-contract/SKILL.md)

## When NOT to Use

- For Spring AI's own retry and tool-error settings (`spring.ai.retry.*`,
  `spring.ai.tools.throw-exception-on-error`). See [`spring-ai`](../spring-ai/SKILL.md)
- For asserting exceptions in tests. See
  [`spring-testing-fundamentals`](../spring-testing-fundamentals/SKILL.md) (`assertThatThrownBy`)
- To design the exception hierarchy itself. That is an architecture decision. This skill
  validates usage once it exists

## Constraints

- MUST NOT generate or modify code
- MUST NOT create new error codes. Report missing ones
- MUST NOT invent the names below. Until the architecture names them, report
  "error type not yet decided" instead of flagging every throw

## Placeholders

| Placeholder | Meaning |
|-------------|---------|
| `<LibraryException>` | The library's single base runtime exception. Carries a category, an error code, and arguments |
| `<ErrorCategory>` | The fixed category enum (taxonomy below) |
| `<Module>ErrorCodes` | One constants class per module holding that module's error codes |

---

## Two Kinds of Failure

Keep these apart. Most mistakes come from mixing them.

| Kind | Example | Correct signal |
|------|---------|----------------|
| **API misuse** (caller bug, programming error) | `null` passed where an agent id is required | `Objects.requireNonNull`, `IllegalArgumentException`, `IllegalStateException`. Idiomatic Java. No error code needed |
| **Runtime failure** (the caller can react to it) | Agent not found, tool call rejected, permission denied, model unavailable | `<LibraryException>` with a category and a registered code |

---

## Error Category Taxonomy

| Situation | Category | HTTP (only in a web adapter) |
|-----------|----------|------------------------------|
| Referenced item does not exist | `NOT_FOUND` | 404 |
| Duplicate key or name | `ALREADY_EXISTS` | 409 |
| Blocked because something still references it | `CONFLICT` | 409 |
| Rule violation not tied to a referencing item (invalid state transition, cycle) | `RULE_VIOLATION` | 422 |
| Structural or input validation failure | `VALIDATION_ERROR` | 400 |
| Caller lacks permission | `FORBIDDEN` | 403 |
| A model, MCP server, vector store, or tool backend failed or timed out | `UPSTREAM_FAILURE` | 502 / 504 |

The HTTP column is applied **only** by a shared web exception handler. Library core code never
mentions HTTP status.

---

## Enforcement Checklist (24 checks)

### CHECK 1: Exception types (5)

```
[ ] E.1.1 — Every runtime failure is a <LibraryException> (or a subclass) with a category and code
[ ] E.1.2 — No "throw new RuntimeException(" or "throw new Exception(" anywhere
[ ] E.1.3 — IllegalArgumentException / IllegalStateException are used only for API misuse,
            never for a failure the caller is expected to handle
[ ] E.1.4 — No generic not-found exception type (e.g. a framework EntityNotFoundException) escapes
            the library. It is translated to NOT_FOUND
[ ] E.1.5 — Exceptions from Spring AI, MCP clients, vector stores, and HTTP clients are
            translated at the library boundary to UPSTREAM_FAILURE, keeping the original as cause
```

### CHECK 2: Category usage (4)

```
[ ] E.2.1 — Every not-found uses NOT_FOUND
[ ] E.2.2 — Every duplicate uses ALREADY_EXISTS
[ ] E.2.3 — Every "blocked by a referencing item" uses CONFLICT, not RULE_VIOLATION
[ ] E.2.4 — Every category maps to exactly one HTTP status in the shared handler (if one exists)
```

### CHECK 3: Error-code registration (5)

```
[ ] E.3.1 — Codes are declared as static final String constants in <Module>ErrorCodes
[ ] E.3.2 — No inline code or message string at the throw site
[ ] E.3.3 — One code format per module: either <CONCEPT>_<SCENARIO> or a numbered form, never mixed
[ ] E.3.4 — <Module>ErrorCodes is final with a private constructor
[ ] E.3.5 — That constructor throws, so the class cannot be instantiated reflectively
```

### CHECK 4: Messages (5)

```
[ ] E.4.1 — Messages take parameters ({0}, {1}) for the arguments passed with the code
[ ] E.4.2 — Messages are human-readable, not stack traces or internal identifiers
[ ] E.4.3 — Messages and exception text never embed prompts, completions, tool arguments,
            retrieved documents, or credentials
[ ] E.4.4 — If the library ships message bundles, every registered code exists in every bundle
[ ] E.4.5 — Products can override any message (bundles are overridable, not hardcoded)
```

### CHECK 5: Handling patterns (5)

```
[ ] E.5.1 — Every lookup uses .orElseThrow(() -> new <LibraryException>(NOT_FOUND, CODE, id))
[ ] E.5.2 — Every uniqueness check throws ALREADY_EXISTS
[ ] E.5.3 — No catch block swallows an exception (empty catch, or catch-and-log-only on a path
            that must fail)
[ ] E.5.4 — Wrapping always keeps the original exception as the cause
[ ] E.5.5 — Rule checks run before mutation, so a failure leaves state unchanged
```

---

## Automatic Rejection Patterns

| Pattern | Reason |
|---------|--------|
| `throw new RuntimeException(` or `throw new Exception(` | Unstructured. The caller cannot branch on it |
| `catch (Exception e) { }` or catch-and-ignore | Swallowed failure |
| An inline literal message at a runtime-failure throw site | Must use a registered code |
| A prompt, completion, or secret placed in an exception message | Data exposure through logs and responses |
| A Spring AI / MCP / vector-store exception escaping the library untranslated | Leaks implementation details. Products cannot handle it uniformly |
| A per-module `@RestControllerAdvice` | One shared handler only |

---

## Canonical Patterns

```java
// ✅ Runtime failure: the caller gets a code it can branch on and localize
Agent agent = agentRegistry.find(agentId)
    .orElseThrow(() -> new <LibraryException>(
        <ErrorCategory>.NOT_FOUND, <Module>ErrorCodes.AGENT_NOT_FOUND, agentId));

// ✅ API misuse: plain Java precondition, no code needed
Objects.requireNonNull(agentId, "agentId");

// ❌ Rejected: no code, no category
throw new RuntimeException("Agent not found: " + agentId);
```

```java
public final class <Module>ErrorCodes {

    private <Module>ErrorCodes() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    public static final String <CONCEPT>_NOT_FOUND      = "<CONCEPT>_NOT_FOUND";
    public static final String <CONCEPT>_ALREADY_EXISTS = "<CONCEPT>_ALREADY_EXISTS";
}
```

---

## Report Format

```
## Error Handling Governance Report

### Scope: [Module / PR]   ### Date: [Date]

| Check             | Rules  | Passed | Failed | N/A |
|-------------------|--------|--------|--------|-----|
| Exception types   | 5      | ?      | ?      | ?   |
| Category usage    | 4      | ?      | ?      | ?   |
| Code registration | 5      | ?      | ?      | ?   |
| Messages          | 5      | ?      | ?      | ?   |
| Handling patterns | 5      | ?      | ?      | ?   |
| **TOTAL**         | **24** | **?**  | **?**  | **?** |

### Missing error codes
### Verdict: COMPLIANT / NON-COMPLIANT
```

## Related Skills

| Skill | Purpose |
|-------|---------|
| [`gov-enforce-library-contract`](../gov-enforce-library-contract/SKILL.md) | Full library contract |
| [`gov-enforce-caching-rules`](../gov-enforce-caching-rules/SKILL.md) | Caching rules |
| [`spring-ai`](../spring-ai/SKILL.md) | Tool-error and retry behaviour in Spring AI |
