---
name: gov-enforce-caching-rules
description: "CACHING GOVERNANCE ENFORCER — validates @Cacheable/@CacheEvict and any other caching in aias against an explicit approved register and eligibility criteria. Rejects caching of per-check data (query results, document content, model output — guardrail G9), per-request or per-employee data, search results, missing eviction, and cache-name drift. Validation only. Use whenever caching is added or proposed."
---

# Skill: gov-enforce-caching-rules

## Description

**CACHING GOVERNANCE ENFORCER.** aias verifies one request per check and must carry nothing
from one check to another (domain-profile G9). Every delivered package says the same in its
CORE unit: no cache holds a query result, document content or model output. A wrong cache here
leaks one request's data into another check or serves a stale service configuration. This skill
decides whether something may be cached at all, then checks how it is cached. It does not modify
code.

## When to Use

- When `@Cacheable`, `@CacheEvict`, `@CachePut`, or a cache client appears in a change
- When anyone proposes caching something
- As part of [`gov-enforce-library-contract`](../gov-enforce-library-contract/SKILL.md)

## When NOT to Use

- When the change contains no caching at all
- For chat memory, vector stores or embeddings indexes — aias has none (conversation memory and
  RAG are out of scope, domain-profile §1); proposing one is a scope question, not a caching one
- For the JPA second-level cache or database query caches

## Constraints

- MUST NOT modify code
- MUST NOT approve caching for anything absent from the approved register
- MUST NOT add a register row. That needs an explicit, recorded design decision
- MUST NOT allow caching annotations on repositories, controllers, or decision classes

---

## Eligibility Gate (FIRST CHECK)

### Approved register

Exhaustive. Anything not listed here is not cacheable.

| Cached item | Approved cache name | Key | Justification / decision reference |
|-------------|---------------------|-----|------------------------------------|
| *(empty: no caching has been approved yet)* | | | |

### Eligibility criteria (ALL must hold)

| # | Criterion | Threshold |
|---|-----------|-----------|
| 1 | Size | Small and bounded. Does not grow with traffic or with the number of checks |
| 2 | Change rate | Low, changed by configuration or an administrator |
| 3 | Scope | Identical for every check, request and employee that can read it |
| 4 | Determinism | The same key always yields the same correct value |
| 5 | Reuse | Read on most requests or by several components |
| 6 | Staleness cost | A stale value cannot cause a wrong verdict, a check on the wrong service version, or an approval |

> **If ANY criterion is false, the item is NOT cacheable.**

Typical candidates: registry configuration read on every check — but note that a running check
reads its pinned service package version (REG, ADR-REG-020), so a cache must key on the version
and must never stand in for the registry's current-version read.

### Never cacheable in a shared cache

- ❌ Anything belonging to one check: query results, fetched or uploaded documents, read
  content, model output, findings (G9)
- ❌ Request or employee data (request numbers, employee identities, decisions)
- ❌ Model completions of any kind
- ❌ Employee decisions and Approval API results (they are evaluated and recorded every time)
- ❌ Check status or report reads (they change while a check runs)
- ❌ Search or list result sets, and any paginated method

---

## Enforcement Checklist (28 checks)

### CHECK 1: Eligibility (6)

```
[ ] C.1.1 — The item is on the approved register
[ ] C.1.2 — It satisfies all six eligibility criteria
[ ] C.1.3 — It is not per-check, per-request or per-employee data (G9)
[ ] C.1.4 — It is not a model completion, query result or document content
[ ] C.1.5 — It is not an employee decision, Approval API result or check status
[ ] C.1.6 — @Cacheable is not on a search, retrieval, or paginated method
```

### CHECK 2: Naming and keys (5)

```
[ ] C.2.1 — The cache name is descriptive camelCase, never "cache1", "data", "temp"
[ ] C.2.2 — The cache name matches the register exactly
[ ] C.2.3 — No second name is used for the same data
[ ] C.2.4 — @Cacheable and @CacheEvict use the same cacheNames value
[ ] C.2.5 — The key matches the register and contains every input that changes the value
```

### CHECK 3: Placement (6)

```
[ ] C.3.1 — @Cacheable appears only on orchestration-layer read methods
[ ] C.3.2 — @CacheEvict appears only on orchestration-layer write methods
[ ] C.3.3 — No @Cacheable on a write method, no @CacheEvict on a read method
[ ] C.3.4 — @CacheEvict uses allEntries = true, unless a recorded decision allows keyed eviction
[ ] C.3.5 — Cached read order: @Cacheable → @Transactional(readOnly = true)
[ ] C.3.6 — Cached write order: @CacheEvict → @Transactional
```

### CHECK 4: Eviction completeness (5)

```
[ ] C.4.1 — Every method that creates the cached item evicts
[ ] C.4.2 — Every method that updates it evicts
[ ] C.4.3 — Every method that deletes or withdraws it evicts
[ ] C.4.4 — A registry load run (start-up reload of service packages and connections) evicts
[ ] C.4.5 — Eviction sits on the same method that performs the write
```

### CHECK 5: Prohibited patterns (6)

```
[ ] C.5.1 — No direct cache-client calls in orchestration code (use the cache abstraction)
[ ] C.5.2 — No @CachePut without a paired eviction strategy
[ ] C.5.3 — No caching annotations on repositories, controllers, or decision classes
[ ] C.5.4 — Caching uses Spring's cache abstraction; the provider is chosen by an explicit
            decision recorded in a CORE unit, never introduced ad hoc
[ ] C.5.5 — Caching can be disabled by configuration without code changes
[ ] C.5.6 — No unbounded in-memory cache (every cache has a size or TTL bound)
```

---

## Annotation Order

```java
// Cached read: approved items only
@Cacheable(cacheNames = "<CACHE_NAME>", key = "#id")
@Transactional(readOnly = true)          // only if a transactional store is read
public <Definition> getDefinition(String id) { }

// Cached write: approved items only
@CacheEvict(cacheNames = "<CACHE_NAME>", allEntries = true)
@Transactional
public void updateDefinition(String id, ...) { }

// Default: anything not on the register carries zero caching annotations
```

---

## Report Format

```
## Caching Governance Report

### Item: [Name]   ### On register: YES / NO   ### Cache name: [name] / N/A

| Check        | Rules  | Passed | Failed |
|--------------|--------|--------|--------|
| Eligibility  | 6      | ?      | ?      |
| Naming/keys  | 5      | ?      | ?      |
| Placement    | 6      | ?      | ?      |
| Eviction     | 5      | ?      | ?      |
| Prohibited   | 6      | ?      | ?      |
| **TOTAL**    | **28** | **?**  | **?**  |

### Verdict: COMPLIANT / NON-COMPLIANT
```

## Related Skills

| Skill | Purpose |
|-------|---------|
| [`gov-enforce-library-contract`](../gov-enforce-library-contract/SKILL.md) | The aias service contract (G.9: no state between checks) |
| [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md) | Layer contract |
| [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md) | Error handling |
