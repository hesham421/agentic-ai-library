---
name: gov-enforce-library-contract
description: "GOVERNANCE ENFORCER — validates aias (Request Verification Service) code against the service contract: the raw idea's §12 guardrails (domain-profile G1–G14), framework-free decision logic in domain classes, in-process module boundaries, orchestration-only services, and thin adapters. Validation only; never generates code. Use after writing or reviewing any backend code, and before calling a unit complete. (The folder name is historical — the repo began as a library bootstrap.)"
---

# Skill: gov-enforce-library-contract

## Description

**GOVERNANCE ENFORCER.** Checks aias code against the service contract and reports every
violation. This skill is a gatekeeper: it does NOT generate or fix code, it VALIDATES and
REPORTS.

The contract has two sources: the governing rules of the domain profile
(`governance/shared/analysis/domain/domain-profile.md` §5, G1–G14 — the raw idea's §12
guardrails, scored by the profile's review checks AIAS-3 … AIAS-9) and the profile's
architecture conventions (one deployable, in-process module interfaces, layers controller /
service / domain / port / adapter / repository, domain behaviour in domain classes). The
layer-by-layer code shape is [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md)'s.

## When to Use

- After writing or changing any code under `src/main/java`
- When reviewing a pull request
- When anyone claims a unit or a module is "complete"

## When NOT to Use

- While generating code. This skill only validates
- For one cross-cutting concern alone. Use
  [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md) or
  [`gov-enforce-caching-rules`](../gov-enforce-caching-rules/SKILL.md)
- For Spring AI API idioms (`ChatModel`, `Prompt`, `ChatOptions`, structured output). Those live
  in [`spring-ai`](../spring-ai/SKILL.md) — read its LOCAL PROJECT NOTE first
- For configuration-property conventions. Use [`spring-boot`](../spring-boot/SKILL.md)
- For test code. Use the `spring-*-testing` skills

## Constraints

- MUST NOT generate or modify application code
- MUST NOT fix violations automatically. Report them
- MUST NOT skip a check. A check that cannot apply yet (the layer does not exist) is reported
  as `N/A`, never as `PASS`
- MUST NOT invent architecture. Where a rule names a type the module's packages have not
  named yet (shown as `<placeholder>`), report the decision as missing instead of guessing a name

## Output

A compliance report (format below) with PASS / VIOLATION / N/A for every check and a rule ID for
every failure.

---

## Enforcement Checklist

### G: Guardrails (10 checks)

The §12 guardrails are part of what the service does — they are NOT deferred with
authentication. This group is **unconditional** and is checked first.

```
[ ] G.1  — The LLM analyses only: no model call is given a tool that runs SQL, reads files, or
           calls the Approval API; queries run only through the query port, exactly as the
           service definition writes them (G1, G4, AIAS-3)
[ ] G.2  — The Approval API is invoked only from the employee-decision operation, only after the
           employee's action, and only where the service version enables it (G2, AIAS-4)
[ ] G.3  — Host data is read only through the read-only query channel (MCP) and BLOBs only over a
           read-only JDBC connection — no write path to a host exists (G3, G14)
[ ] G.4  — Query parameters are bound or strictly type-validated; no SQL is concatenated from
           free text, request data or model output (G4, AIAS-5)
[ ] G.5  — Every file path is resolved, normalised and validated inside the configured storage
           root before it is opened (G5, AIAS-5)
[ ] G.6  — Anything that could not be read appears in the report; a missing or unreadable
           required document prevents COMPLIANT — nothing is skipped silently (G6, AIAS-8)
[ ] G.7  — Document content reaches the model only as delimited data, never as instructions or
           system text (G7, AIAS-6)
[ ] G.8  — Every check applies its limits — aias.check.timeout (PT2M), aias.check.max-rows (100),
           aias.check.max-file-size (10MB), aias.check.max-uploads (20) — as platform configuration,
           never read from a service definition (G8, AIAS-7)
[ ] G.9  — No data is carried from one check to another: no cache, static field, conversation
           memory or session holds a query result, document content or model output (G9)
[ ] G.10 — The engine depends on Spring AI ChatModel only, with no provider-specific feature;
           document reading uses its own configurable model (G12, AIAS-9)
```

### D: Decision Logic (6 checks)

Applies to every class whose job is to answer "is this operation allowed?" or "what happens
next?" — the rules a unit assigns to `owner layer: domain`.

```
[ ] D.1 — Decision logic lives in a domain class, separate from orchestration code
[ ] D.2 — That class carries NO Spring, JPA, or Spring AI annotations
[ ] D.3 — It never reaches persistence, a port, the model, or the network. All facts it needs
          are passed in as plain arguments
[ ] D.4 — It signals a rule violation through the module's error type carrying an error-catalog
          code, not a raw exception (see gov-enforce-error-handling)
[ ] D.5 — It never imports another module. Facts from another module are resolved by the caller
          through that module's published contract and passed in
[ ] D.6 — No over-application: one domain class per concept. A shared policy class exists only
          when a rule genuinely spans several concepts
```

### M: Module Boundaries (6 checks)

REG, DOC, CHK, RPT and INT run in ONE deployable and reach each other in-process.

```
[ ] M.1 — A module is used by others only through its published contract interface
          (platform/contracts/contract-<mod>.md). Internal packages are never imported from
          outside the module
[ ] M.2 — No repository, entity, port or adapter bean is injected outside the module that owns it
[ ] M.3 — No circular dependency between modules (build order REG → DOC → CHK → RPT → INT)
[ ] M.4 — Contract types are interfaces, records, or enums. No entity or mutable implementation
          class crosses a module boundary; returned lists are unmodifiable
[ ] M.5 — A boundary a package asks to be enforced (e.g. RPT imports no org.springframework.ai,
          java.nio.file or query-port type) has its architecture test (ArchUnit), not convention alone
[ ] M.6 — No module calls another over HTTP; cross-module edges are the XM integration packages,
          built through the contract interfaces with no XM id in code
```

### S: Orchestration Services (8 checks)

Applies to Spring-managed classes that coordinate a capability.

```
[ ] S.1 — Constructor injection only (no field injection), dependencies final
[ ] S.2 — The method body is orchestration-only: load → delegate decision → act → return.
          Domain-owned rule conditions are delegated to the domain class (D.1), never inlined
[ ] S.3 — If a method writes to the service's schema, it is @Transactional
[ ] S.4 — If a method only reads, it is @Transactional(readOnly = true)
[ ] S.5 — No model, MCP, document-source or host call is made while a database transaction is
          held open, unless the unit prescribes it
[ ] S.6 — log.info for state changes, log.debug for reads; no document content, query results,
          prompts, completions or credentials at info level or above
[ ] S.7 — No caller authentication, permission check or authorization annotation — deferred by
          amendment A2
[ ] S.8 — Errors follow gov-enforce-error-handling; nothing is swallowed
```

### P: Persistence (6 checks)

```
[ ] P.1 — The service's own schema only: no entity maps a host table; host identifiers are
          strings, never foreign keys
[ ] P.2 — Hard delete only: rows leave through the operations the plans define (e.g. the
          retention purge); no soft-delete flag
[ ] P.3 — Associations are LAZY. Data needed together is loaded with JOIN FETCH or an
          entity graph, never by touching a lazy collection in a loop
[ ] P.4 — Counts use count queries, not collection.size()
[ ] P.5 — No module stores request data its plan forbids it to hold (e.g. REG: no request
          number, query result or document content)
[ ] P.6 — No dead repository methods. Every method has a caller
```

### W: Web Adapters (6 checks)

```
[ ] W.1 — Controllers inject only services. Never a repository, port, ChatModel, or client
[ ] W.2 — Controllers contain ZERO decision logic
[ ] W.3 — @Valid on every @RequestBody
[ ] W.4 — Every endpoint is an operation of the module's API document and is documented
          (@Operation or the project's chosen equivalent)
[ ] W.5 — Exception-to-HTTP mapping lives in one ProblemDetail advice, never per controller
[ ] W.6 — The surface is POST (create) and GET (read) only, under /api/v1/{resource}; nothing
          renders a server-side page (the frontend is a separate app, amendment A1)
```

**Total: 42 checks.**

---

## Automatic Rejection Triggers

Any one of these rejects the change, whatever the other results:

| Pattern | Reason |
|---------|--------|
| Any failed check in group G | Breaks a §12 guardrail |
| Decision logic annotated with `@Component`/`@Service`/`@Entity` | Must be a plain class (D.2) |
| Decision logic holding a repository, port, client, or `ChatModel` | Must not touch I/O (D.3) |
| An import of another module's internal package | Module boundary (M.1) |
| A repository, port or adapter injected in another module | Module boundary (M.2) |
| A domain-owned rule condition inlined in an orchestration method | Must delegate (S.2) |
| An authorization annotation or permission check | Deferred by amendment A2 (S.7) |
| Business logic in a controller | Thin adapter (W.2) |
| A controller injecting a repository, a port or a model | Layer violation (W.1) |
| Document content, query results, prompts, completions, or secrets logged at info level or above | Data exposure (S.6) |

---

## Deliberately Not Enforced

The contract this skill grew from also mandated choices that do not belong to aias: an audit
base class, sequence primary keys, a result envelope, an activate/deactivate lifecycle,
method-level authorization on every service method, a fixed CRUD file set, and a
"no domain concepts" boundary for a generic library. aias is a domain service with its own
vocabulary (Check, Service Package, Finding, Employee Decision …). Do **not** flag the absence
of any of those. Authentication returns with the security version (amendment A2); add its
checks here then.

---

## Violation Template

```
❌ VIOLATION DETECTED

Rule: [Rule ID] — [Rule description]
Location: [File:Line]
Found: [What was found]
Expected: [What should be there]
Severity: CRITICAL / HIGH / MEDIUM

Fix: [Exact correction]
```

## Report Format

```
## Service Contract Enforcement Report

### Scope: [Unit / PR]   ### Date: [Date]

| Group                  | Checks | Passed | Failed | N/A |
|------------------------|--------|--------|--------|-----|
| G  Guardrails          | 10     | ?      | ?      | ?   |
| D  Decision logic      | 6      | ?      | ?      | ?   |
| M  Module boundaries   | 6      | ?      | ?      | ?   |
| S  Orchestration       | 8      | ?      | ?      | ?   |
| P  Persistence         | 6      | ?      | ?      | ?   |
| W  Web adapters        | 6      | ?      | ?      | ?   |
| **TOTAL**              | **42** | **?**  | **?**  | **?** |

Cross-cutting: error handling [COMPLIANT / NON-COMPLIANT / N/A]
               caching        [COMPLIANT / NON-COMPLIANT / N/A]

### Violations
1. [Rule ID] — [Description] — [Location]

### Verdict: APPROVED / REJECTED
```

Any automatic rejection trigger, or any failed check in group G, means **REJECTED**.

---

## Related Skills

| Skill | Purpose |
|-------|---------|
| [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md) | Layer-by-layer code contract |
| [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md) | Error types, error codes, ProblemDetail |
| [`gov-enforce-caching-rules`](../gov-enforce-caching-rules/SKILL.md) | Caching eligibility and annotations |
| [`spring-ai`](../spring-ai/SKILL.md) | Spring AI usage (ChatModel only in aias) |
| [`spring-boot`](../spring-boot/SKILL.md) | Configuration properties |
