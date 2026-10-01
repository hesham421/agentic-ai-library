---
name: build-create-entity
description: "Generates an aias JPA entity class from the module's db-script, plus its domain class (business behaviour) when the entity has rules that answer \"is this operation allowed?\". Build step 1 of a DATA-DOM unit — MUST be completed before any other backend artifact of that entity. Enforces identity NUMBER(19) PKs named {entity}Id, Oracle 19c type mapping, createdAt/updatedAt on transactional entities, hard delete (no soft-delete flag), host identifiers as plain strings, and domain classes built through static factories."
---

# Skill: build-create-entity

## Description
Generates a JPA entity class for the aias service's own Oracle 19c schema, following the
module's db-script exactly. This is **build step 1** of a DATA-DOM unit and MUST be completed
before the repository, DTOs, service, controller, port or adapter that use the entity.

## When to Use
- When a DATA-DOM unit (`DATA-DOM-CONFIG` / `DATA-DOM-TRANSACTIONAL`) introduces an entity
- BEFORE creating the repository, DTOs, service or controller that use it

## When NOT to Use
- When the entity already exists — edit the entity file directly
- When adding a single field to an existing entity (edit it; do not re-run this skill)
- When only DTOs, service, controller, port or adapter code change
- For host data — host tables are never entities of this service; they are read through the
  query port as plain values (domain-profile G3, G14)

---

## Variables (resolve ALL before generating)

Nothing below is hardcoded to a particular module or entity. Resolve every variable from the
module's own documents — the DATA-DOM unit's `ENT-*` block (BINDINGS, field table, DOMAIN
RULES, STATE MACHINE) and the db-script (its one ```` ```sql ```` fence and its
```` ```yaml name=dbf-matrix ```` block) — never guess, never carry a value over from another
module.

| Variable | Meaning | Example shape |
|----------|---------|---------------|
| `<module>` | Java sub-package of the module under the project base package | lowercase module code |
| `<MODULE_TABLE_PREFIX>` | Table prefix of this module, as the db-script uses it | `REG`, `CHK`, `DOC`, `RPT`, `INT` |
| `<Entity>` | PascalCase entity name from the `ENT-*` block | — |
| `<ENTITY_TABLE>` | Table name **as defined in the db-script** | UPPER_SNAKE_CASE, module prefix |
| `<ENTITY_PK_COLUMN>` | PK column **as defined in the db-script** (`dbf-matrix`: the identifier row's `column`) | e.g. `SERVICE_PACKAGE_ID` |
| `<entityId>` | PK property — pattern `{entity}Id` (`dbf-matrix`: the identifier row's `entity_field`) | e.g. `servicePackageId` |
| `<kind>` | `config` or `transactional` (the `ENT-*` block's `kind`) | — |
| `<base.package>` | Project base package | the `groupId` in `pom.xml` |

> If a variable cannot be resolved from the unit or the db-script, STOP and record a gap
> (`gov-module.py --track backend record <MOD> api_doc_gaps …`). Never invent a column, table
> or constraint name.

## Responsibilities

- Generate a JPA entity whose table, columns, types and constraints match the db-script
- Map the identity primary key
- Map the audit fields `createdAt` / `updatedAt` on transactional entities
- Map closed enums (fetch mode, overall status, document read status, …) to Java enums whose
  stored values match the db-script's CHECK constraint exactly
- Store host identifiers (service request number, employee identity) as plain `String`
  columns exactly as received — never as an association
- Generate the entity's domain class when the unit's DOMAIN RULES give an owner layer of
  `domain` — see "Domain Class" below. An entity with no such rule needs none

## Constraints

- MUST NOT generate repository, DTO, service, controller, port or adapter code
- MUST NOT modify other existing entity files
- MUST NOT assume a missing variable value — require it before generating
- MUST NOT add a soft-delete or active/inactive flag, or `activate()`/`deactivate()` helpers —
  aias deletes hard (profile `delete_semantics: hard`)
- MUST NOT add a column the db-script does not declare (no tenant column, no `createdBy` /
  `updatedBy` — there is no caller identity until authentication arrives, amendment A2)
- MUST NOT map an association to another module's entity — cross-module references are ids
  resolved through that module's published contract interface

## Output

- The JPA entity `<Entity>.java` under `src/main/java/<base/package>/<module>/`
- The domain class (when required) under the module's `domain` layer
- The sub-package layout of the module (where entities and domain classes sit) is fixed by the
  module's first DATA-DOM unit and followed by every later one. If no module has established
  it yet, propose it in the phase assessment rather than choosing silently

---

## Steps

### 1. Class-level setup
```java
@Entity
@Table(name = "<ENTITY_TABLE>",
    uniqueConstraints = {
        @UniqueConstraint(name = "<UQ_NAME_FROM_DDL>", columnNames = {"<COLUMN>"})
    },
    indexes = {
        @Index(name = "<INDEX_NAME_FROM_DDL>", columnList = "<COLUMN>")
    }
)
public class <Entity> {

    protected <Entity>() { } // JPA
```
No base class: aias has no shared audit superclass. Lombok is not on the classpath — write
accessors explicitly (or add Lombok only on an explicit decision recorded in the module's CORE
unit).

### 2. Primary key
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY) // NUMBER(19) GENERATED BY DEFAULT AS IDENTITY
@Column(name = "<ENTITY_PK_COLUMN>", nullable = false, updatable = false)
private Long <entityId>;
```

### 3. Business fields — type mapping (oracle19c → Java, per the CORE unit's R1 table)

| oracle19c | Java | Mapping |
|---|---|---|
| `NUMBER(19)` | `Long` | — |
| `NUMBER(10,0)` | `Integer` | — |
| `NUMBER(p,s)` | `BigDecimal` | `precision`, `scale` from the DDL |
| `VARCHAR2(n CHAR)` | `String` | `@Column(length = n)` |
| `NUMBER(1)` | `Boolean` (0/1) | the dialect's NUMBER(1) mapping, or `@Convert(converter = NumericBooleanConverter.class)` if the dialect does not map it |
| `TIMESTAMP WITH TIME ZONE` | `OffsetDateTime` | — |
| `CLOB` | `String` | `@Lob` |
| `CLOB CHECK (col IS JSON)` | `String` | `@Lob`; parsed by the layer the unit names, never by the entity |

```java
@Column(name = "<COLUMN_NAME>", length = <LENGTH>, nullable = false)
private String <fieldName>;
```
`nullable` follows the db-script's `NOT NULL` marker exactly.

### 4. Closed enums
```java
@Column(name = "<ENUM_COLUMN>", length = <LENGTH>, nullable = false)
@Enumerated(EnumType.STRING)               // stored values equal the constant names
private <EnumType> <fieldName>;
```
When the CHECK constraint's values are not valid constant names (e.g. `path | blob | manual`),
use an `AttributeConverter` that writes exactly those values. Service codes and document types
are open data from the service registry — `String`, never an enum (they are never hardcoded).

### 5. References
```java
// Inside the module, as the dbf-matrix names it: a plain id property
@Column(name = "<FK_COLUMN>", nullable = false)
private Long <parentEntityId>;

// Host identifiers: strings exactly as the host sent them, never a foreign key
@Column(name = "<REQUEST_NUMBER_COLUMN>", length = <LENGTH>, nullable = false)
private String <requestNumber>;
```
Use an `@ManyToOne(fetch = FetchType.LAZY)` association only when the unit's BINDINGS or the
`dbf-matrix` `entity_field` names the association rather than the id. Never across modules.

### 6. Audit fields (transactional entities only)
```java
@Column(name = "CREATED_AT", nullable = false, updatable = false)
private OffsetDateTime createdAt;

@Column(name = "UPDATED_AT", nullable = false)
private OffsetDateTime updatedAt;
```
Map them exactly as the db-script declares them, and set them the way the unit says (a database
`DEFAULT SYSTIMESTAMP` → map read-only with `insertable = false, updatable = false`; otherwise
`@PrePersist` / `@PreUpdate` or Hibernate's `@CreationTimestamp` / `@UpdateTimestamp`). They are
never set from request data. `config` entities carry them only if the db-script declares them.

### 7. Normalisation
Canonicalisation the plan requires (e.g. REG's `ServiceCodes.canonical(code)` — trim + lower
case) happens once, in the place the unit names, before any comparison or query. Do not add an
unrequested `toUpperCase()` / `toLowerCase()` to an entity.

### 8. State changes
A state column the db-script declares (e.g. REG's `available`, a check status) changes only
through an intention-revealing method on the entity (e.g. `withdraw(OffsetDateTime at)`) that
the domain class or service calls after the rule has been decided. No generic setter-driven
state flips from a service.

---

## Shared Layer Mandate

aias has no shared persistence base class. What every entity consumes instead:

| # | Requirement | Source |
|---|-------------|--------|
| SH.1 | Type mapping and enum lists | the module's CORE unit (R1) and the db-script |
| SH.2 | Error codes for rule violations | the `error-catalog` block (`{MOD}-{http}[-{SLUG}]`) and the module's error-code constants created by its CORE unit |
| SH.3 | Per-check limits (`aias.check.timeout`, `max-rows`, `max-file-size`, `max-uploads`) | platform configuration created by a CORE unit — never a column, never a constant on an entity |

**Rules:**
- NEVER create an audit base class or a soft-delete mechanism
- NEVER add a tenant, `createdBy` or `updatedBy` column the db-script does not declare
- NEVER store request data a module's plan forbids it to hold (e.g. REG holds no request
  number, query result or document content — REQ-REG-059)

> After creating the entity, run [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md).

---

## Domain Class (Business Rules)

The entity file above is **persistence mapping plus intention-revealing state methods**. Any
business rule the unit's DOMAIN RULES assign to `owner layer: domain` — invariants, allowed
state transitions, guard conditions — belongs in a domain class (profile
`domain_behaviour_placement: domain_classes`). A rule assigned to `owner layer: service` stays
in the service; one assigned to `owner layer: repository` is a database constraint whose
violation the repository layer translates.

### The Decision Test

Before writing any conditional, ask: *does this code decide whether an operation is permitted
or what state comes next?* If yes, and the unit gives `owner layer: domain` → it belongs in the
domain class, not in the entity's mapping, the service body or the controller.

### Rules (STRICT — mirrors `gov-enforce-backend-contract` LAYER 0)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.0.1 | A domain class exists for every entity with `owner layer: domain` rules | YES |
| A.0.2 | The domain class carries no Spring, JPA or Spring AI annotations | YES |
| A.0.3 | The domain class never reaches a repository, a port, the model or the network — all facts are passed in | YES |
| A.0.4 | Every rule violation throws the module's domain exception carrying the `error-catalog` code | YES |
| A.0.5 | The domain class is constructed only via static factories `create(...)` / `from(...)` | YES |
| A.0.6 | The domain class never imports another module — cross-module facts are resolved by the service through the published contract and passed in | YES |
| A.0.7 | One domain class per concept; a shared policy class only when a rule genuinely spans several entities | YES |

### Responsibilities

- Static factory `create(...)` — runs construction-time rules, returns a valid instance or
  throws the domain exception
- Static factory `from(<Entity> entity)` — a domain view over a persisted entity, to evaluate a
  rule before changing it
- Guard methods taking plain values (counts, statuses, already-fetched facts) and throwing on
  violation
- Division of labour on state transitions: the domain class **decides**; the entity's state
  method **executes**; the service calls the domain class first

### Violations (MUST NOT)

- ❌ A domain class annotated with `@Component` / `@Service` / `@Entity`
- ❌ A repository, port or `ChatModel` field or constructor parameter on a domain class
- ❌ A public constructor used from outside the class instead of `create()` / `from()`
- ❌ A `domain`-owned rule left inline in the service or the controller
- ❌ Skipping this section for an entity whose unit lists `owner layer: domain` rules

### Shape

```java
public final class <Entity>Domain {

    private final <Status> status;

    private <Entity>Domain(<Status> status) {
        this.status = status;
    }

    public static <Entity>Domain from(<Entity> entity) {
        return new <Entity>Domain(entity.getStatus());
    }

    // Decision only — the service calls the entity's state method after this returns
    public void assertCanRecordDecision() {
        if (status != <Status>.COMPLETED) {
            throw new <Module>DomainException(<Module>ErrorCodes.<CODE_FROM_CATALOG>, status);
        }
    }
}
```

> **Status choice** comes from the `error-catalog` row, not from taste: the HTTP part of the
> code (`{MOD}-409-…`, `{MOD}-422-…`) is fixed by the plan. See
> [`gov-enforce-error-handling`](../gov-enforce-error-handling/SKILL.md).

---

## Rules (STRICT)

| Rule ID | Rule | MUST |
|---------|------|------|
| A.1.1 | No base class; no audit superclass; no soft-delete flag or `activate()`/`deactivate()` | YES |
| A.1.2 | Table and every column name come verbatim from the db-script | YES |
| A.1.3 | PK is `Long <entity>Id`, `GenerationType.IDENTITY`, column from the db-script | YES |
| A.1.4 | Java types follow the oracle19c mapping (step 3) | YES |
| A.1.5 | `nullable` matches the db-script's `NOT NULL` exactly | YES |
| A.1.6 | `NUMBER(1)` maps to `Boolean`, through `NumericBooleanConverter` if the dialect needs it | YES |
| A.1.7 | Closed enums stored with exactly the CHECK constraint's values | YES |
| A.1.8 | Service codes and document types are `String`, never enums | YES |
| A.1.9 | Host identifiers are `String` columns, never associations or foreign keys | YES |
| A.1.10 | No association to another module's entity | YES |
| A.1.11 | Any association used is `fetch = FetchType.LAZY` | YES |
| A.1.12 | `@UniqueConstraint` and `@Index` declared inside `@Table`, names verbatim from the DDL | YES |
| A.1.13 | Transactional entities map `createdAt` / `updatedAt`, never set from request data | YES |
| A.1.14 | State columns change only through intention-revealing entity methods | YES |
| A.1.15 | No `createdBy` / `updatedBy` / tenant column unless the db-script declares it | YES |

> Constraint and index names (A.1.12) are **physical database objects** and must match the
> db-script's DDL exactly.

---

## Violations (MUST NOT)

- ❌ `GenerationType.SEQUENCE` / `AUTO`, or a sequence the db-script does not create
- ❌ A generic or invented PK column name instead of the db-script's
- ❌ A soft-delete / active flag, or `activate()` / `deactivate()` helpers
- ❌ An audit base class, `createdBy` / `updatedBy`, or a tenant column
- ❌ A `@ManyToOne` to a host table or to another module's entity
- ❌ A host request number or employee identity stored as anything but a `String`
- ❌ An enum for service codes or document types
- ❌ `fetch = FetchType.EAGER` on any relationship
- ❌ A business rule with `owner layer: domain` written into the entity's mapping or the service
- ❌ Request data on an entity whose module's plan forbids holding it
- ❌ Lowercase or camelCase table names
