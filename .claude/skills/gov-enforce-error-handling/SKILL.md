---
name: gov-enforce-error-handling
description: "ERROR HANDLING ENFORCER — validates that aias runtime failures use the module's error type carrying a registered error code {MOD}-{http}[-{SLUG}] from the module's error-catalog, and reach callers as RFC 9457 ProblemDetail {type, title, status, detail, code}; rejects raw runtime exceptions, inline codes or messages, swallowed exceptions, and messages that leak document content, query results, prompts or secrets. Validation only. Use when reviewing any code that throws, catches, or maps exceptions."
---

# Skill: gov-enforce-error-handling

## Description

**ERROR HANDLING GOVERNANCE ENFORCER.** Hosts and the employee frontend must be able to branch
on aias failures. That only works if every runtime failure carries a **stable error code** —
`{MOD}-{http}[-{SLUG}]`, e.g. `CHK-404-CHECK-NOT-FOUND`, `{SLUG}` in SCREAMING-KEBAB — declared in
the module's `error-catalog` block and delivered as an RFC 9457 `ProblemDetail`. This skill
checks for that and reports violations. It does not modify code.

## When to Use

- After any code that throws, catches, wraps, or maps exceptions is written or changed
- When a ProblemDetail advice is added or changed
- As part of [`gov-enforce-library-contract`](../gov-enforce-library-contract/SKILL.md) and
  [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md)

## When NOT to Use

- For Spring AI's own retry settings (`spring.ai.retry.*`). See [`spring-ai`](../spring-ai/SKILL.md)
- For asserting exceptions in tests. See
  [`spring-testing-fundamentals`](../spring-testing-fundamentals/SKILL.md) (`assertThatThrownBy`)
  and [`spring-mvc-testing`](../spring-mvc-testing/SKILL.md) (ProblemDetail responses)
- For load-time outcomes that are recorded, not thrown (e.g. REG's `REG-LOAD-*` load reason
  codes written to the load result) — those are data, not HTTP errors

## Constraints

- MUST NOT generate or modify code
- MUST NOT create new error codes. A code that is needed but absent from the module's
  `error-catalog` block is reported, and recorded for the factory
  (`gov-module.py --track backend record <MOD> api_doc_gaps …`) — never invented
- MUST NOT invent the class names below. Until the module's CORE unit names them, report
  "error type not yet created by CORE" instead of flagging every throw

## Placeholders

| Placeholder | Meaning |
|-------------|---------|
| `<Module>Exception` | The module's runtime exception (or small hierarchy). Carries the error code and the message arguments; its HTTP status is the `{http}` part of the code |
| `<Module>ErrorCodes` | One constants class per module holding exactly the codes of its `error-catalog` block |
| `<Module>ProblemAdvice` | The single `@RestControllerAdvice` the plan names (e.g. INT's `IntegrationProblemAdvice`) that turns exceptions into ProblemDetail |

---

## Two Kinds of Failure

Keep these apart. Most mistakes come from mixing them.

| Kind | Example | Correct signal |
|------|---------|----------------|
| **API misuse** (programming error inside the service) | `null` passed where a check id is required by an internal method | `Objects.requireNonNull`, `IllegalArgumentException`, `IllegalStateException`. Idiomatic Java. No error code needed |
| **Runtime failure** (the caller can react to it) | Check not found, service not available, upload too large, Approval API timed out | `<Module>Exception` with a registered `{MOD}-{http}[-{SLUG}]` code |

---

## The Error Contract

| Element | Value |
|---------|-------|
| Envelope | `ProblemDetail` (RFC 9457), media type `application/problem+json` |
| Fields | `type`, `title`, `status`, `detail`, plus the extension member `code` |
| `code` | `{MOD}-{http}[-{SLUG}]` — the module prefix of the module that raised it, the HTTP status, an optional SCREAMING-KEBAB slug. `INT-500` (no slug) is the unexpected-failure code |
| `status` | equals the `{http}` part of `code` |
| `detail` | the catalog's English message with its arguments filled in; Arabic texts are `PENDING` (per-module ADR) |
| Statuses | 200, 201, 202, 400, 401, 403, 404, 409, 413, 415, 422, 500, 502, 504 — 401/403 stay unused until caller authentication arrives (amendment A2) |
| Pass-through | a refusal raised by another module keeps its own code, status and message unchanged when relayed (e.g. INT relaying `CHK-422-SERVICE-NOT-AVAILABLE` — ADR-INT-003) |
| Unexpected | any other exception → the module's `{MOD}-500`, logged with the request path and the Check identifier; the answer carries no stack trace |

The HTTP status is part of the code and fixed by the catalog — code never picks a status of its
own.

---

## Enforcement Checklist (24 checks)

### CHECK 1: Exception types (5)

```
[ ] E.1.1 — Every runtime failure is a <Module>Exception (or a subclass) carrying a catalog code
[ ] E.1.2 — No "throw new RuntimeException(" or "throw new Exception(" anywhere
[ ] E.1.3 — IllegalArgumentException / IllegalStateException are used only for API misuse,
            never for a failure the caller is expected to handle
[ ] E.1.4 — No framework not-found exception (e.g. EntityNotFoundException) escapes a module.
            It is translated to the module's {MOD}-404-… code
[ ] E.1.5 — Exceptions from Spring AI, the MCP client, JDBC, the file system and HTTP clients are
            translated inside the adapter to the module's documented code (or recorded outcome),
            keeping the original as cause
```

### CHECK 2: Code ↔ status (4)

```
[ ] E.2.1 — Every code used matches the format {MOD}-{http}[-{SLUG}] and exists in the module's
            error-catalog block
[ ] E.2.2 — The ProblemDetail status always equals the {http} part of the code
[ ] E.2.3 — Each operation raises only the codes its API document lists under x-error-codes
[ ] E.2.4 — A relayed refusal from another module keeps its own code, status and message
```

### CHECK 3: Error-code registration (5)

```
[ ] E.3.1 — Codes are declared as static final String constants in <Module>ErrorCodes
[ ] E.3.2 — No inline code or message string at the throw site
[ ] E.3.3 — <Module>ErrorCodes holds exactly the error-catalog rows of its module — none missing,
            none extra
[ ] E.3.4 — <Module>ErrorCodes is final with a private constructor
[ ] E.3.5 — That constructor throws, so the class cannot be instantiated reflectively
```

### CHECK 4: Messages (5)

```
[ ] E.4.1 — Messages take the catalog's named parameters ({checkId}, {serviceCode}, …) for the
            arguments passed with the code
[ ] E.4.2 — Messages are the catalog's English text, human-readable, not stack traces or
            internal identifiers
[ ] E.4.3 — Messages and exception text never embed document content, query results, prompts,
            completions, connection settings or credentials
[ ] E.4.4 — Arabic texts are left PENDING as the catalog says — never machine-invented
[ ] E.4.5 — Clients branch on `code`, never on `detail`; tests assert code and status, not text
```

### CHECK 5: Handling patterns (5)

```
[ ] E.5.1 — Every lookup uses .orElseThrow(() -> new <Module>Exception(<Module>ErrorCodes.<CODE>, id))
[ ] E.5.2 — Exactly one ProblemDetail advice per boundary the plan names; no per-controller handler
[ ] E.5.3 — No catch block swallows an exception (empty catch, or catch-and-log-only on a path
            that must fail); an unreadable item inside a check is recorded in the report instead (G6)
[ ] E.5.4 — Wrapping always keeps the original exception as the cause
[ ] E.5.5 — Rule checks run before mutation (and before any Approval API call), so a failure
            leaves state unchanged
```

---

## Automatic Rejection Patterns

| Pattern | Reason |
|---------|--------|
| `throw new RuntimeException(` or `throw new Exception(` | Unstructured. The caller cannot branch on it |
| `catch (Exception e) { }` or catch-and-ignore | Swallowed failure |
| An inline literal code or message at a runtime-failure throw site | Must use a registered code |
| A code not in the module's error-catalog, or in another format | Invented contract |
| An error response that is not ProblemDetail, or lacks `code` | Error contract broken |
| Document content, a query result, a prompt, a completion or a secret in an exception message | Data exposure through logs and responses |
| A Spring AI / MCP / JDBC / file-system exception escaping its adapter untranslated | Leaks implementation details |
| A per-controller `@ExceptionHandler` or a second advice for the same boundary | One advice only |
| A stack trace in a response body | Unexpected failures answer `{MOD}-500` only |

---

## Canonical Patterns

```java
// ✅ Runtime failure: the caller gets a code it can branch on
CheckRun run = checkRuns.findById(checkId)
    .orElseThrow(() -> new <Module>Exception(<Module>ErrorCodes.CHECK_NOT_FOUND, checkId));

// ✅ API misuse: plain Java precondition, no code needed
Objects.requireNonNull(checkId, "checkId");

// ❌ Rejected: no code, wrong envelope
throw new RuntimeException("Check not found: " + checkId);
```

```java
public final class <Module>ErrorCodes {

    private <Module>ErrorCodes() {
        throw new UnsupportedOperationException("Constants class, do not instantiate");
    }

    // exactly the rows of the module's error-catalog block
    public static final String CHECK_NOT_FOUND = "<MOD>-404-CHECK-NOT-FOUND";
}
```

```java
// ✅ The one advice: exception → ProblemDetail with `code`
@ExceptionHandler(<Module>Exception.class)
ProblemDetail handle(<Module>Exception ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
    pd.setProperty("code", ex.code());
    return pd;
}
```

---

## Report Format

```
## Error Handling Governance Report

### Scope: [Module / Unit / PR]   ### Date: [Date]

| Check             | Rules  | Passed | Failed | N/A |
|-------------------|--------|--------|--------|-----|
| Exception types   | 5      | ?      | ?      | ?   |
| Code ↔ status     | 4      | ?      | ?      | ?   |
| Code registration | 5      | ?      | ?      | ?   |
| Messages          | 5      | ?      | ?      | ?   |
| Handling patterns | 5      | ?      | ?      | ?   |
| **TOTAL**         | **24** | **?**  | **?**  | **?** |

### Codes used but missing from the error-catalog
### Verdict: COMPLIANT / NON-COMPLIANT
```

## Related Skills

| Skill | Purpose |
|-------|---------|
| [`gov-enforce-library-contract`](../gov-enforce-library-contract/SKILL.md) | The aias service contract |
| [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md) | Layer contract |
| [`gov-enforce-caching-rules`](../gov-enforce-caching-rules/SKILL.md) | Caching rules |
| [`spring-mvc-testing`](../spring-mvc-testing/SKILL.md) | Testing `@RestControllerAdvice` and ProblemDetail responses |
