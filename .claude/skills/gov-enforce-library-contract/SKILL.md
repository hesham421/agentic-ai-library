---
name: gov-enforce-library-contract
description: "GOVERNANCE ENFORCER — validates library code against the Reusable Agentic AI Library contract: the Product-domain boundary, framework-free decision logic, module boundaries, orchestration-only services, and thin adapters. Validation only; never generates code. Use after writing or reviewing any library code, and before calling a feature complete."
---

# Skill: gov-enforce-library-contract

## Description

**GOVERNANCE ENFORCER.** Checks library code against this project's implementation contract and
reports every violation. This skill is a gatekeeper: it does NOT generate or fix code, it
VALIDATES and REPORTS.

The contract has two sources: the architecture principle in the root [`README.md`](../../../README.md)
(the library holds no Product domain), and the general layering rules adapted from an earlier
backend governance pack (see [`../README.md`](../README.md#local-skills)).

## When to Use

- After writing or changing any code under `src/main/java`
- When reviewing a pull request
- When anyone claims a library capability is "complete"

## When NOT to Use

- While generating code. This skill only validates
- For one cross-cutting concern alone. Use
  [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md) or
  [`gov-enforce-caching-rules`](../gov-enforce-caching-rules/SKILL.md)
- For Spring AI API usage (`ChatClient`, advisors, tools, memory, structured output). Those rules
  and anti-patterns live in [`spring-ai`](../spring-ai/SKILL.md). Do not restate them here
- For configuration-property and auto-configuration conventions. Use
  [`spring-boot`](../spring-boot/SKILL.md)
- For test code. Use the `spring-*-testing` skills

## Constraints

- MUST NOT generate or modify application code
- MUST NOT fix violations automatically. Report them
- MUST NOT skip a check. A check that cannot apply yet (the layer does not exist) is reported
  as `N/A`, never as `PASS`
- MUST NOT invent architecture. Where a rule names a type the architecture has not decided yet
  (shown as `<placeholder>`), report the decision as missing instead of guessing a name

## Output

A compliance report (format below) with PASS / VIOLATION / N/A for every check and a rule ID for
every failure.

---

## Enforcement Checklist

### B: Product-Domain Boundary (6 checks)

The library is domain-agnostic. This group is **unconditional** and is checked first.

```
[ ] B.1 — No type, package, field, constant, prompt, or test fixture names a Product domain
          concept (e.g. Employee, Invoice, Student, Order, Customer, Account, Loan)
[ ] B.2 — Product data enters only generically: as external data, tool schemas, configuration,
          or SPI implementations the Product supplies. The library never models it
[ ] B.3 — No Product business rule is encoded in the library (no "an invoice may not…")
[ ] B.4 — Behaviour that varies per Product is driven by configuration or an SPI, not by
          branching on a Product identifier
[ ] B.5 — Sample or demo code that needs a domain lives outside the library module and is
          clearly marked as an example
[ ] B.6 — Library concepts use the generic vocabulary from README.md (Agent, Tool, Knowledge,
          Memory, Context, Model, MCP, Permission, Execution)
```

### D: Decision Logic (6 checks)

Applies to every class whose job is to answer "is this operation allowed?" or "what happens
next?" (state transitions, guard rules, cycle prevention, policy evaluation).

```
[ ] D.1 — Decision logic lives in a plain class, separate from orchestration code
[ ] D.2 — That class carries NO Spring, JPA, or Spring AI annotations
[ ] D.3 — It never reaches persistence, a model, a tool, or the network. All facts it needs
          are passed in as plain arguments
[ ] D.4 — It signals a rule violation through the library's error type, not a raw exception
          (see gov-enforce-error-handling)
[ ] D.5 — It never imports another module's internals. Facts from another module are resolved
          by the caller and passed in
[ ] D.6 — No over-application: one decision class per concept. A shared policy class exists
          only when a rule genuinely spans several concepts
```

### M: Module Boundaries (6 checks)

```
[ ] M.1 — A module is used by others only through its public API package. Internal packages
          are never imported from outside the module
[ ] M.2 — No repository, store, or client bean is injected outside the module that owns it
[ ] M.3 — No circular dependency between modules
[ ] M.4 — Public API types are interfaces, records, or enums. No internal entity or mutable
          implementation class crosses a module boundary
[ ] M.5 — A boundary that matters is enforced by an automated architecture test (e.g.
          ArchUnit), not by convention alone
[ ] M.6 — Auto-configuration classes only wire beans. They hold no capability logic
```

### S: Orchestration Services (8 checks)

Applies to Spring-managed classes that coordinate a capability.

```
[ ] S.1 — Constructor injection only (no field injection), dependencies final
[ ] S.2 — The method body is orchestration-only: load → delegate decision → act → return.
          Rule conditions are delegated to the decision class (D.1), never inlined
[ ] S.3 — If a method writes to a transactional store, it is @Transactional
[ ] S.4 — If a method only reads a transactional store, it is @Transactional(readOnly = true)
[ ] S.5 — No remote model or tool call is made while a database transaction is held open,
          unless the reason is documented next to the code
[ ] S.6 — log.info for state changes, log.debug for reads; no prompts, completions, tool
          arguments, or secrets at info level or above
[ ] S.7 — Any caller-supplied sort or filter field is validated against an explicit allow-list
[ ] S.8 — Errors follow gov-enforce-error-handling; nothing is swallowed
```

### P: Persistence (6 checks, N/A until the library persists data)

```
[ ] P.1 — Existence checks use existsBy<Field>() rather than loading the row
[ ] P.2 — An update-time uniqueness check (existsBy<Field>AndIdNot) exists only for mutable fields
[ ] P.3 — Associations are LAZY. Data needed together is loaded with JOIN FETCH or an
          entity graph, never by touching a lazy collection in a loop
[ ] P.4 — Counts use count queries, not collection.size()
[ ] P.5 — Read-only multi-table reads use projections
[ ] P.6 — No dead repository methods. Every method has a caller
```

### W: Web Adapters (6 checks, N/A until the library exposes HTTP endpoints)

```
[ ] W.1 — Controllers inject only services. Never a repository, store, ChatModel, or client
[ ] W.2 — Controllers contain ZERO decision logic
[ ] W.3 — @Valid on every @RequestBody
[ ] W.4 — Every endpoint is documented (@Operation or the project's chosen equivalent)
[ ] W.5 — Exception-to-HTTP mapping lives in one shared handler, never per controller
[ ] W.6 — Endpoints are opt-in for the Product (conditional on a property or starter), so
          embedding the library never exposes an endpoint by accident
```

**Total: 38 checks.**

---

## Automatic Rejection Triggers

Any one of these rejects the change, whatever the other results:

| Pattern | Reason |
|---------|--------|
| A Product domain concept in library code (B.1–B.3) | Breaks the library's core boundary |
| Decision logic annotated with `@Component`/`@Service`/`@Entity` | Must be a plain class (D.2) |
| Decision logic holding a repository, client, or `ChatModel` | Must not touch I/O (D.3) |
| An import of another module's internal package | Module boundary (M.1) |
| A repository or store injected in another module | Module boundary (M.2) |
| A rule condition inlined in an orchestration method | Must delegate (S.2) |
| Business logic in a controller | Thin adapter (W.2) |
| A controller injecting a repository or a model | Layer violation (W.1) |
| Prompts, completions, or secrets logged at info level or above | Data exposure (S.6) |

---

## Deliberately Not Enforced

The source governance pack also mandated ERP-specific choices: an `AuditableEntity` base class,
`SEQUENCE` primary keys with fixed naming, `@SuperBuilder`, a `ServiceResult<T>` envelope, an
activate/deactivate lifecycle, `@PreAuthorize` on every method, and a fixed CRUD file set. Those
belong to a Product backend, not to this library. Do **not** flag their absence. If the library
architecture later adopts one, add it here as a new check.

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
## Library Contract Enforcement Report

### Scope: [Capability / PR]   ### Date: [Date]

| Group                  | Checks | Passed | Failed | N/A |
|------------------------|--------|--------|--------|-----|
| B  Domain boundary     | 6      | ?      | ?      | ?   |
| D  Decision logic      | 6      | ?      | ?      | ?   |
| M  Module boundaries   | 6      | ?      | ?      | ?   |
| S  Orchestration       | 8      | ?      | ?      | ?   |
| P  Persistence         | 6      | ?      | ?      | ?   |
| W  Web adapters        | 6      | ?      | ?      | ?   |
| **TOTAL**              | **38** | **?**  | **?**  | **?** |

Cross-cutting: error handling [COMPLIANT / NON-COMPLIANT / N/A]
               caching        [COMPLIANT / NON-COMPLIANT / N/A]

### Violations
1. [Rule ID] — [Description] — [Location]

### Verdict: APPROVED / REJECTED
```

Any automatic rejection trigger, or any failed check in group B, means **REJECTED**.

---

## Related Skills

| Skill | Purpose |
|-------|---------|
| [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md) | Error types, error codes, messages |
| [`gov-enforce-caching-rules`](../gov-enforce-caching-rules/SKILL.md) | Caching eligibility and annotations |
| [`spring-ai`](../spring-ai/SKILL.md) | Spring AI usage and its anti-patterns |
| [`spring-boot`](../spring-boot/SKILL.md) | Configuration properties and auto-configuration |
