---
name: gov-enforce-caching-rules
description: "CACHING GOVERNANCE ENFORCER — validates @Cacheable/@CacheEvict and any other caching against an explicit approved register and eligibility criteria. Rejects caching of per-user, per-tenant, per-conversation or model-generated data in shared caches, search results, missing eviction, and cache-name drift. Validation only. Use whenever caching is added or proposed."
---

# Skill: gov-enforce-caching-rules

## Description

**CACHING GOVERNANCE ENFORCER.** A reusable library runs inside many Products and often serves
many tenants and users at once. A wrong cache here leaks data across them or serves stale
results everywhere. This skill decides whether something may be cached at all, then checks how
it is cached. It does not modify code.

## When to Use

- When `@Cacheable`, `@CacheEvict`, `@CachePut`, or a cache client appears in a change
- When anyone proposes caching something
- As part of [`gov-enforce-library-contract`](../gov-enforce-library-contract/SKILL.md)

## When NOT to Use

- When the change contains no caching at all
- For chat memory. Conversation history is **state**, not a cache. It belongs in a
  `ChatMemoryRepository` (see [`spring-ai`](../spring-ai/SKILL.md)) and is out of scope here
- For vector stores and embeddings indexes. Those are retrieval stores, not caches
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
| 1 | Size | Small and bounded. Does not grow with traffic or conversations |
| 2 | Change rate | Low, changed by configuration or an administrator |
| 3 | Scope | Identical for every user, tenant, and conversation that can read it |
| 4 | Determinism | The same key always yields the same correct value |
| 5 | Reuse | Read on most requests or by several components |
| 6 | Staleness cost | A stale value cannot cause a wrong action or a permission bypass |

> **If ANY criterion is false, the item is NOT cacheable.**

Typical candidates: resolved agent or tool **definitions** loaded from configuration, prompt
templates, model capability metadata.

### Never cacheable in a shared cache

- ❌ Per-user, per-tenant, per-session, or per-conversation data
- ❌ Model completions, embeddings of user input, or tool results, unless a recorded decision
  covers the key design, the TTL, and the tenant isolation
- ❌ Permission or approval **decisions** (they must be evaluated every time)
- ❌ Human-approval / workflow state
- ❌ Search or retrieval result sets, and any paginated method
- ❌ Anything a Product marks as transactional or regulated data

---

## Enforcement Checklist (28 checks)

### CHECK 1: Eligibility (6)

```
[ ] C.1.1 — The item is on the approved register
[ ] C.1.2 — It satisfies all six eligibility criteria
[ ] C.1.3 — It is not per-user / per-tenant / per-conversation data
[ ] C.1.4 — It is not a model completion, embedding, or tool result without a recorded decision
[ ] C.1.5 — It is not a permission decision or approval/workflow state
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
[ ] C.3.5 — Cached read order: @Cacheable → @Transactional(readOnly = true) → security annotation (if any)
[ ] C.3.6 — Cached write order: @CacheEvict → @Transactional → security annotation (if any)
```

### CHECK 4: Eviction completeness (5)

```
[ ] C.4.1 — Every method that creates the cached item evicts
[ ] C.4.2 — Every method that updates it evicts
[ ] C.4.3 — Every method that deletes or disables it evicts
[ ] C.4.4 — A configuration reload or refresh evicts
[ ] C.4.5 — Eviction sits on the same method that performs the write
```

### CHECK 5: Prohibited patterns (6)

```
[ ] C.5.1 — No direct cache-client calls in orchestration code (use the cache abstraction)
[ ] C.5.2 — No @CachePut without a paired eviction strategy
[ ] C.5.3 — No caching annotations on repositories, controllers, or decision classes
[ ] C.5.4 — The library never forces a cache provider. It uses Spring's cache abstraction, and
            the Product chooses the provider
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
| [`gov-enforce-library-contract`](../gov-enforce-library-contract/SKILL.md) | Full library contract |
| [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md) | Error handling |
| [`spring-ai`](../spring-ai/SKILL.md) | Chat memory, which is state and not a cache |
