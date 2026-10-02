# ALIGN-BE — RPT v1 — code alignment audit — 2026-10-02

Unit `ALIGN-BE` (phase ALIGN-BE, kind PHASE, `tests: []`, acceptance = a clean build). The factory ran the ALIGN rows on the PLAN (`ALIGN-BE.md`, `findings: 0, clean: true`, `examined_nothing: C7.19, C7.20, C7.23, C7.24, C7.26, C7.27, C7.28, C7.5, C7.5b`). This report runs the same rows on the CODE the plan produced: `src/main/java/io/agenticai/rpt/**` (80 classes; the CORE, DATA-DOM, PORTS `CheckResultStore` and SVC-API units), the RPT block of `src/main/resources/messages.properties` and the `aias.reports.*` keys of `application.properties`. It checks them against `_SECTIONS.md` (registries, DB manifest 46 DBF, QR-RPT-001…007, totals, error-catalog 5 rows, RULE-RPT-001…015), the unit files `CORE`, `DATA-DOM`, `PORTS` and `SVC-API`, `api-spec-rpt.yaml`, `db-script-rpt.md` (the `sql` fence and the `dbf-matrix`), `contract-rpt.md` (CON-RPT-001…006), `contract-chk.md` (CON-CHK-006…011, which RPT implements), `analysis/decisions/RPT/` and the three `gov-enforce-*` skills. No test was written or run (unit instruction). Acceptance is `mvn -q -DskipTests compile` + `test-compile`; both exit 0.

Audit subject at a glance:
- 4 entities and 8 converters.
- 17 domain types: 8 closed enums, `RefusalReason`, `RuleRefusal`, `CheckRunOpening`, `CheckStatusTransition`, `SubmittedReport`, `CheckFailure`, `DecisionRecording`, `ReportLimits`, `Texts`.
- 5 repositories and 3 projection records.
- 8 service-package classes.
- 1 controller.
- 28 contract types: `ReportStore`, 9 records, 16 typed exceptions plus `RptRefusalException`, and `ReportRejectionCodes`.
- 5 error classes, 1 config record and 1 adapter.
- 28 RPT message keys.

Expected, not findings:
- The ArchUnit boundary test that CORE R1 asks for is deferred. Tests are disallowed this run; the boundary is held by grep (0 hits) and is already in gap row 1.
- The three earlier OPEN `api_doc_gaps` rows of `execution-state.json` are known. They are the accepted deviations below.
- PORTS and SVC-API have no `package_results` row (see F6).

## ALIGN table — code side

| row | backing check | verdict | evidence |
|---|---|---|---|
| TRACEABILITY | traces | PASS (after Javadoc fixes — F1) | Distinct ids cited in `rpt/**`: 46/46 `DBF-RPT-*` (each entity field names its DBF), 15/15 `RULE-RPT-*`, 6/6 `CON-RPT-*`, 6/6 `CON-CHK-006…011` (+ 001…003 for the carried lists), 3/3 `API-RPT-*` (controller constants `CheckReportController.java` + Javadoc), 7/7 `QR-RPT-*`, 4/4 `ENT-RPT-*`, 12 `ADR-RPT-*`. Before the fix `repository/UnfinishedCheckRow.java` and `domain/RuleRefusal.java` carried no governance id, and REQ-RPT-036 / 047 / 048 / 049 were cited by no class. |
| COVERED | orphans | PASS | REQ-RPT-001…054: 52 by id after F1; REQ-RPT-029 through the cited range "REQ-RPT-028 … REQ-RPT-031" (`CheckReportController`, API-RPT-002 Javadoc). Every REQ of the SVC-API `Covers` line is reached by a class. |
| BINDING (§2A) | value-agreement | PASS | Tables `RPT_CHECK_RUN`, `RPT_FINDING`, `RPT_CHECK_DOCUMENT`, `RPT_UNREAD_QUERY` verbatim; 20 + 9 + 10 + 7 = 46 `@Column` names = the 46 `dbf-matrix` rows and the `sql` fence, 0 extra. `length` = VARCHAR2 widths (100/10/100/100/30/30/200/30/30/100; 100/10/30/30; 100). `nullable = false` exactly on the NOT NULL columns. PK `{entity}Id` `Long` `GenerationType.IDENTITY` ×4. The six CLOBs (`FAILURE_DETAIL`, `CONDITION_TEXT`, `EVIDENCE`, `NOTE`, both `DETAIL`) carry `@Lob`. `NUMBER(10)` → `Integer`, `NUMBER(1)` → `Boolean` (no converter, same as REG/DOC — the oracle19c dialect stores 0/1), `TIMESTAMP WITH TIME ZONE` → `OffsetDateTime`. `@UniqueConstraint` `UQ_RPT_FINDING_RUN_POS` / `UQ_RPT_CHECK_DOCUMENT_RUN_POS` / `UQ_RPT_UNREAD_QUERY_RUN_POS` (`CHECK_RUN_ID, POSITION`) and `@Index` `IDX_RPT_CHECK_RUN_SVC_REQ (SERVICE_CODE, REQUEST_NUMBER, STARTED_AT)`, `IDX_RPT_CHECK_RUN_CHECK_STATUS`, `IDX_RPT_CHECK_RUN_ENDED_AT` verbatim. Converters (8, `autoApply`) write `storedValue()`: `CheckStatus` {AWAITING_DOCUMENTS, RUNNING, COMPLETED, FAILED}, `OverallStatus` {COMPLIANT, NOT_COMPLIANT, NEEDS_MANUAL_REVIEW}, `CheckFailureReason` (7), `FindingOutcome` (3), `FetchMode` {path, blob, manual} (lower case — the reason for converters, gap rows 1/2), `DocumentReadStatus` (3), `UnreadableReason` (8), `EmployeeDecision` {APPROVED, REJECTED} — each exactly its BLOCK 5c `IN (…)` list. A stored value outside the list fails the read with the constraint's name. `CREATED_AT`/`UPDATED_AT` `insertable = false, updatable = false` → `DEFAULT SYSTIMESTAMP`. The FKs and the CHECK shapes stay database-only (no association — ADR-RPT-011). |
| MANIFEST (§4) | count-agrees | PASS | `DBF: 46` = 46 mapped columns of 4 `@Entity`. `API: 3` = 3 `@GetMapping`. `QR: 7` = `findById`/`findForUpdate` (001), `findOfCheckRun` ×3 (002–004), `findNewestOfRequest` (005), `countOfRequest` (006), `findDecisionAgreement` (007). `XM: 0` = no import of another module except `chk.contract` in the adapter. The error-catalog's 5 rows = the 5 `ReportStoreErrorCodes` constants; the SVC-API rejection table's 16 rows = the 16 `ReportRejectionCodes` constants. |
| WRITERS | required-writer | PASS | No HTTP writer (ADR-RPT-006). The DBF-002…008 `CheckRun.created` writers sit behind `createCheckRun`. The native conditional UPDATEs: `markRunning` (DBF-007, 009), `complete` (007, 010…012), `fail` (007, 010, 013, 014) and `recordDecision` (015…018, `DECIDED_AT = SYSTIMESTAMP`). `Finding`/`CheckDocument`/`UnreadQuery.recorded` take positions from 1 in the received order. Identities come from the identity clause; the audit columns come from the DB default, and every UPDATE sets `UPDATED_AT = SYSTIMESTAMP`. |
| QRC (§5) + inline ops | orphans | PASS | QR-RPT-001 `findById` ← `CheckReportQueryService.read` / `CheckRunQueryService.findCheck`. With `FOR UPDATE` it is `findForUpdate` (`PESSIMISTIC_WRITE`) ← `completeCheck` step 1. QR-RPT-002…004 `findOfCheckRun … ORDER BY position` ← `read` (COMPLETED only). QR-RPT-005 `findNewestOfRequest … ORDER BY startedAt DESC, checkRunId DESC` + `Limit.of(100)`. QR-RPT-006 `countOfRequest`. QR-RPT-007 `findDecisionAgreement` (GROUP BY / ORDER BY as written, `employeeDecision IS NOT NULL`). Inline ops, one caller each: `markRunning`, `complete`, `fail`, `recordDecision`, `findUnfinished`, `findEndedBefore`, `deleteEnded`. Every value is a bound parameter. Reads carry `@Transactional(readOnly = true)`. QR-002…004 select the entity rather than the projection; the mapped result is the same. |
| API (R3) | code-format | PASS | `ReportStoreException.CODE_FORMAT = ^RPT-(\d{3})(?:-[A-Z0-9]+(?:-[A-Z0-9]+)*)?$`; the status is the `{http}` segment. All 5 catalog codes and 16 in-process codes match. Statuses 400/404/409/422/500 are platform statuses. `RPT-LOG-*` are message keys only. |
| API DOCUMENT | api-spec-agree | PASS | `api-spec-rpt.yaml`: 3 operations ↔ 3 controller methods. `readCheck` `GET /api/v1/checks/{checkId}` ↔ `@GetMapping("/checks/{checkId}") readCheck(@PathVariable("checkId") Long)`. `listChecksOfRequest` `GET /api/v1/checks` ↔ `@RequestParam("serviceCode")`, `@RequestParam("requestNumber")`. `readDecisionAgreement` `GET /api/v1/decision-agreement` ↔ `@RequestParam("serviceCode")`. No POST/PUT/PATCH/DELETE; 200, plain JSON, no envelope. Response records = schemas field for field: `CheckReport` (18 fields, 11 required non-null, lists copied unmodifiable, `decision` nullable); `FindingView` 5; `DocumentView` 6 (`unreadableReason`, `detail` nullable); `UnreadQueryView` 3; `DecisionView` 4; `CheckSummary` 6 (3 required); `ChecksOfRequest {total, checks}`; `AgreementRow` 4. Codes are carried as their stored values (`path` lower case). The `maxLength: 100` of the query parameters is not validated (gap row 3 (6)). |
| ERROR RESPONSES | api-spec-errors | PASS | `ReportStoreErrorCodes` = exactly `RPT-400-CHECK-ID-INVALID`, `RPT-404-CHECK-NOT-FOUND`, `RPT-400-REQUEST-KEYS-MISSING`, `RPT-400-SERVICE-CODE-MISSING`, `RPT-500` (= the 5 rows = the 5 distinct `x-error-codes`). `ReportRejectionCodes` = the 16 SVC-API rows; `CHECK_NOT_FOUND` shares the catalog code by design. Grep of `"RPT-\d{3}…"` literals in `src/main/java` outside the two constants classes → 0. Scripted diff of the plan texts against `messages.properties`: 25/25 texts byte-identical (5 catalog + 20 in-process; multi-text codes keyed `CODE.SLUG`). The three purge log lines are also verbatim. Keys used in code vs keys present → 0 missing. `ReportStoreProblemAdvice` (one advice, `basePackages = "io.agenticai.rpt"`) answers `ProblemDetail {type, title, status, detail}` + `code`: a type mismatch on `checkId` → 400 `RPT-400-CHECK-ID-INVALID`; a missing parameter by matched pattern → `RPT-400-REQUEST-KEYS-MISSING` / `RPT-400-SERVICE-CODE-MISSING`; a blank parameter → the same codes from the service; `ReportStoreException` → its own code/status; anything else → `RPT-500`, logged without a body stack trace. Over HTTP only `CheckNotFoundException` (404) can come out of the read path. |
| DOCUMENT VALID | api-spec-valid | PASS (parse only) | YAML parses, all `$ref`s resolve; the factory's 3.1 validation is not repeated. |
| RULE INPUTS | data-source | PASS | Owner sites:<br>RULE-RPT-001/002/006 (creation) `CheckRunOpening` ← `createCheckRun` (values received).<br>RULE-RPT-003 `CheckStatusTransition`: a 0-row conditional UPDATE triggers a re-read; in `completeCheck` it runs on the `FOR UPDATE` row.<br>RULE-RPT-004/005/007/008 `SubmittedReport.refusalAgainst(StoredRun)`, in plan order 006 → 004 → 007 (findings, then unread queries) → 008 → 005, against the locked row.<br>RULE-RPT-006 on carried codes in `CheckResultStore.code(…)`.<br>RULE-RPT-010 `CheckFailure` (no row read, so endedAt ≥ startedAt is backstopped by `CHK_RPT_CHECK_RUN_ENDED_AT` — gap row 2 (3)).<br>RULE-RPT-011…014 `DecisionRecording` (values received; 0 rows → re-read → 011 before 012).<br>RULE-RPT-009 / 015 in `CheckReportQueryService.listOfRequest` / `DecisionAgreementQueryService.read` and the advice.<br>**Conditional UPDATEs** verbatim: `markRunning WHERE CHECK_STATUS IN ('AWAITING_DOCUMENTS','RUNNING')`, `RUNNING_SINCE = COALESCE(RUNNING_SINCE, :runningSince)`; `complete … AND CHECK_STATUS = 'RUNNING'`; `fail … IN ('AWAITING_DOCUMENTS','RUNNING')`; `recordDecision … AND CHECK_STATUS = 'COMPLETED' AND EMPLOYEE_DECISION IS NULL`; purge `DELETE … IN ('COMPLETED','FAILED') AND ENDED_AT < :cutOff`.<br>**E.5.5**: in every write method the domain refusal is raised before the first INSERT/UPDATE (`createCheckRun` before `saveAndFlush`; `completeCheck` lock → validate → write; `failCheck` and `recordDecision` before the UPDATE). |
| CONTRACT | signatures | PASS | `CheckResultStore implements CheckResultPort`, matching CON-CHK-006…011 argument for argument:<br>`createCheckRun(String serviceCode, int versionNumber, String fetchMode, String requestNumber, String employeeId, String status, OffsetDateTime startedAt) → Long`<br>`markRunning(Long, OffsetDateTime)`<br>`completeCheck(Long, String overallStatus, List<ReportFinding>, List<ReportDocumentOutcome>, List<ReportUnreadQuery>, ReportMetadata)`<br>`failCheck(Long, String failureReason, String detail, OffsetDateTime endedAt)`<br>`getCheck(Long) → Optional<CheckRun>` (not found = empty — gap row 3 (3))<br>`listUnfinishedChecks() → List<UnfinishedCheck>`<br>The records are copied component by component into RPT's own `SubmittedReport` records, with no map, byte[] or untyped component (REQ-RPT-053). `rpt.contract.ReportStore` matches CON-RPT-003…006: `readCheck`, `listChecksOfRequest`, `readDecisionAgreement`, `recordDecision(Long, String, String, Boolean) → RecordedDecision {checkId, employeeDecision, decidedBy, decidedAt, approvalApiExecuted}`. `io.agenticai.chk.contract` is imported only by `adapter/CheckResultStore.java` (7 types). No other module imports `io.agenticai.rpt` yet. `rpt.contract` imports `rpt.error.ReportStoreException`/`ReportStoreTexts` (F3 — the CHK/REG/DOC pattern). |
| GUARDRAILS | REQ-RPT-047–049, P.2, G.9 | PASS | Grep in `rpt/**` for `org.springframework.ai`, `java.nio.file`, `java.io.File`, `DataSource`, `RestClient`, `WebClient`, `RestTemplate`, `HttpClient`, `McpQueryChannel`, `ServiceQueryPort` → 0. No host access, no outbound HTTP, no Approval API (REQ-RPT-037). **P.2**: hard delete only — native `DELETE` per id + `ON DELETE CASCADE`; no soft-delete or active flag; 3 `@Immutable` child entities. **G.9**: no static non-final field; every `static final` is a logger, key, `EnumSet` or `Pattern`; no cache annotation; no state between Checks. |
| TRANSACTIONS | ADR-RPT-012 | PASS on RPT's side; cross-module finding X1 recorded | Port methods `Propagation.REQUIRED`, `noRollbackFor = RptRefusalException`, `rollbackFor = ReportNotStoredException`, on the adapter and on `CheckRunCommandService`. `ReportNotStoredException extends RptRefusalException`; Spring's closest-rule depth makes `rollbackFor` win (F4). Reads `readOnly = true`. `recordDecision` `REQUIRED` in INT's transaction. Purge: one `REQUIRES_NEW` `TransactionTemplate` per id, and a failure keeps that run and continues. ADR-RPT-012(3) as written contradicts CHK's REQ-CHK-048 savepoint fallback — analysed below as **X1** (new OPEN gap row). The deployed stack has no savepoint support at all — **X2**. |
| TRACEABILITY of decisions (DECISIONS) | refs-exist | PASS | ADR-RPT-001, 003, 004, 006, 008, 009, 010, 011, 012, 013, 018, 020 cited in code; `analysis/decisions/RPT/` holds ADR-RPT-001…020. |
| SECURITY (R5) | operation-resolves | PASS | Grep `PreAuthorize|Secured|RolesAllowed|springframework.security` in `rpt/**` → 0 (caller authentication deferred, raw-idea A2). ENT-RPT-001…004 operations resolve as the entity registry states. |
| CROSS-MODULE / INTEGRATION | registry-agree | N-A (0 XM) | RPT consumes no entity; its one cross-module edge is the CHK port it implements. |
| FOREIGN IDS | xref-resolve | PASS | `CON-CHK-001…003, 006…011` → headings of `contract-chk.md`; `CON-REG-*`/`CON-DOC-*` are cited only in Javadoc of carried types and resolve. |
| DEMAND (SRS) | operation-resolves | N-A (0 SCR-REQ) | RPT has no screen. |
| PATHS | paths-resolve | PASS | `messages.properties` on the classpath: every RPT key the code uses is present (5 catalog, 16 rejection codes / 20 texts, 3 `RPT-LOG-*`), 0 missing. `aias.reports.retention-days` / `purge-schedule` documented (commented) in `application.properties`; one binder, `ReportStoreProperties`; the cron default `0 0 2 * * *` sits in both the record and `@Scheduled`. No `@EnableScheduling` in RPT (CHK's single one). |
| COVERAGE | (the report) | this file | `findings: 6` (2 fixed Javadoc — F1a/F1b; 4 LOW/INFO open — F2…F5; 1 process INFO — F6) + 2 cross-module findings (X1 recorded as a gap row, X2 reported). 0 BINDING / API DOCUMENT / ERROR RESPONSES mismatches. |

## Findings

| # | location | what differs | severity | fixed? |
|---|---|---|---|---|
| F1a | `adapter/CheckResultStore.java:38`; `entity/CheckRun.java:140` | TRACEABILITY — REQ-RPT-047/048/049 (no host query, no document, no model) and REQ-RPT-036 (the Approval API flag) were cited by no class, though they were implemented structurally. | trivial | YES — Javadoc only. Before: `… reads RPT tables and RPT never calls the Check Engine.` After: `… never calls the Check Engine. It is RPT's only adapter: RPT runs no host query, fetches no document and calls no model (REQ-RPT-047, REQ-RPT-048, REQ-RPT-049).` · DBF-RPT-018 Javadoc before: `1 when executed through the Approval API, 0 otherwise; present …`. After: `… 0 otherwise (REQ-RPT-036); present …`. |
| F1b | `repository/UnfinishedCheckRow.java:9`; `domain/RuleRefusal.java:12` | TRACEABILITY — two classes carried no governance id. | trivial | YES — Javadoc only. `UnfinishedCheckRow` before: `(SVC-API inline operation). Module-internal.` After: `(SVC-API inline operation; CON-CHK-011, REQ-RPT-022). Module-internal.` · `RuleRefusal` after: `… no framework. SVC-API maps it to its in-process rejection table (ADR-RPT-012, ADR-RPT-013).` Recompiled clean. |
| F2 | `entity/CheckRun.java` `markRunning`, `complete`, `fail`, `recordDecision` | A.1.14 — the entity's intention-revealing transitions have no caller. Every status and decision change is a native conditional UPDATE, as SVC-API prescribes (`UPDATED_AT = SYSTIMESTAMP`, guarded `WHERE`). Behaviour is correct; the four methods are dead code. This is the same pattern as CHK's F4. | LOW | NO — open LOW; removing them is a code change, not a mechanical alignment. |
| F3 | `contract/RptRefusalException.java:3-4` | M.4 — `rpt.contract` imports `rpt.error.ReportStoreException` and `ReportStoreTexts`, so INT will receive subtypes of a non-contract class. This is the same pattern as `chk.contract` → `chk.error` (CHK F2, accepted), REG and DOC. It does not contradict contract-rpt.md, which names no package. | LOW | NO — the cross-module pattern; orchestrator to accept as for CHK. |
| F4 | `contract/ReportNotStoredException.java:15` | A database failure (REQ-RPT-009) is modelled as a subclass of `RptRefusalException`, which PORTS defines as the "refusal … declared `noRollbackFor`". It rolls back only because Spring picks the closest rule (`rollbackFor = ReportNotStoredException`, depth 0, beats `noRollbackFor = RptRefusalException`, depth 1). The behaviour is as planned, but it depends on that resolution rule, and a catch of `RptRefusalException` (by INT or CHK) would also catch a database failure. | LOW | NO — design note; an `extends ReportStoreException` re-parent is a code change. |
| F5 | `service/CheckRunCommandService.java` `createCheckRun`, `markRunning`, `failCheck`, `recordDecision` | E.1.5 — only `completeCheck` translates a database failure (into `ReportNotStoredException`). In the other four writes a Spring `DataAccessException` leaves RPT untranslated, to CHK and INT. The plan prescribes translation only for completeCheck step 4; CON-CHK-006/009 say just "not stored / not found", and the callers treat any `RuntimeException`. | LOW | NO — plan-silent; candidate for the factory. |
| F6 | `governance/shared/backend/modules/RPT/execution-state.json` → `delivery RPT` | Process, not code: PORTS and SVC-API show `not run` although their code is present. Their acceptance is `tests-green` (32 + 32 TCs), not executed by this unit. | INFO | NO — record them once their tests run. |

## Cross-module findings

### X1 — completeCheck write failure: ADR-RPT-012(3) vs REQ-CHK-048 (recorded: OPEN gap row)

Read on both sides: `rpt/adapter/CheckResultStore.java`, `rpt/service/CheckRunCommandService.java:120-160`, `chk/service/CheckEndingService.java:85-130`, `chk/service/CheckPipeline.java:84-99,149`, `chk/service/PipelineRunner.java:52-60`, `chk/service/DeadlineCheckService.java:58-80`, and the spring-orm 7.0.9 bytecode of `JpaTransactionManager$JpaTransactionObject` (`setRollbackOnly` → `EntityTransaction.setRollbackOnly()` + `ConnectionHolder.setRollbackOnly()`; `isRollbackOnly` → `EntityTransaction.getRollbackOnly()`; `rollbackToSavepoint` → the savepoint manager + `EntityManagerHolder.resetRollbackOnly()` only).

- **Plan (RPT)**: ADR-RPT-012(3), PORTS and SVC-API completeCheck step 4 say a write failure → `ReportNotStoredException` (RPT-500-REPORT-NOT-STORED) rolls back the caller's WHOLE transaction. The report and CHK's Active Check DELETE are undone together, and the Check stays RUNNING until its deadline.
- **Plan (CHK)**: CON-CHK-008 / REQ-CHK-048 / ADR-CHK-015 say that on "not stored" CHK rolls back to a savepoint taken just before `completeCheck` and calls `failCheck(INTERNAL_ERROR)` in the SAME transaction.
- **Mechanism**: RPT's two proxies join (`REQUIRED`). Rollback is declared (`rollbackFor`, and the default rule applies to any `RuntimeException` anyway), so Spring marks the participating transaction rollback-only through `JpaTransactionObject.setRollbackOnly`, which sets the JPA `EntityTransaction` rollback-only. Hibernate also marks it itself when a flush throws a `PersistenceException`. CHK's `rollbackToSavepoint` clears only the `EntityManagerHolder` flag. The `EntityTransaction` stays rollback-only, so the ending's commit becomes `UnexpectedRollbackException`, and CHK's `failCheck(INTERNAL_ERROR)` and its DELETE roll back with the report.
- **Runtime outcome (as built)**: the exception leaves `CheckEndingService.end` → `CheckPipeline.run` catches it (it surfaces from `ending.complete` inside `runSteps`). `PipelineFailure.of` defaults to INTERNAL_ERROR with the CHK-500 text → `ending.fail` runs in a NEW `REQUIRES_NEW` transaction. The Active Check row is back, so the DELETE hits 1 row, and RPT `failCheck` moves the still-RUNNING run to **FAILED / INTERNAL_ERROR**. The end-of-Check notice is sent after that commit. Only if that second ending also fails (e.g. the database is still unavailable) does `PipelineRunner` log it, and the deadline check ends the Check **FAILED / TIMED_OUT** (REQ-CHK-080).
- **Correction to the brief**: the brief expected "stays RUNNING → TIMED_OUT". The code shows TIMED_OUT is only the second fallback.
- **In every path**: the outcome is recorded, never silent, and nothing of the report remains (REQ-RPT-009 holds). The plan statements contradict each other, so the decision is the factory's.
- **Gap row recorded**: `api_doc_gaps` key `completeCheck write failure: ADR-RPT-012(3) whole-transaction rollback vs REQ-CHK-048 failCheck in the same transaction`, resolution `OPEN — MAPPING_GAP pending spec clarification`, detail = both plan statements, the mechanism, the current behaviour and the never-silent outcome.

### X2 — no savepoint support under JpaTransactionManager + HibernateJpaDialect (CHK / REG; reported, not recorded)

- **Mechanism**: `JpaTransactionObject.createSavepoint()` → `getSavepointManager()` returns `EntityManagerHolder.getSavepointManager()`. That is set only when the dialect's transaction data `instanceof SavepointManager` (`JpaTransactionObject.setTransactionData`). `HibernateJpaDialect.beginTransaction` always returns `HibernateJpaDialect$SessionTransactionData`, which implements no interface (spring-orm 7.0.9, the version Boot 4.1.1 pins; also 6.2.10). With `nestedTransactionAllowed = true` (`reg/service/LoadRunTransactionConfiguration`), `createSavepoint()` therefore throws `NestedTransactionNotSupportedException("JpaDialect does not support savepoints - check your JPA provider's capabilities")`. ADR-RPT-012 itself rejected NESTED for this reason.
- **Consequence**: `CheckEndingService.handOverReport:104` throws before `completeCheck` is called. The ending rolls back, and the pipeline catch ends the Check FAILED / INTERNAL_ERROR. As built, **no Check can end COMPLETED**. `reg/service/ItemSavepoints.run:54` would fail its first item in the same way, giving REG-500 at the load run.
- **Confidence**: static (source + bytecode); not executed, because tests are disallowed and the Oracle Free container did not start in the last surefire run. It is outside RPT's code and was not recorded in CHK's/REG's partition by this unit. It is for the orchestrator to raise for CHK (REQ-CHK-048) and REG (ADR-REG-015).

Accepted deviations (not findings — each is an OPEN `api_doc_gaps` row already recorded):
- (a) Gap row 1 — the ArchUnit guard is deferred; `retention-days` binds as Integer; FetchMode needs a converter.
- (b) Gap row 2 — domain refusals are values (`RuleRefusal`); RULE-RPT-005's owner is `SubmittedReport`; `failCheck` has no end ≥ start comparison; converters are used for every closed list.
- (c) Gap row 3 — no messages for the unread-query and document-type incompleteness; the null `documentType` is refused; `getCheck` → `Optional`; RULE-RPT-006 is applied at mapping, before RULE-RPT-001/010; savepoint wording; `maxLength` is not validated; purge log keys; ArchUnit.

Observations (no row affected):
- (i) The `@Modifying(clearAutomatically = true)` UPDATEs clear the persistence context shared with CHK's ending transaction. CHK flushes before `completeCheck`, so nothing is lost.
- (ii) `CheckResultStore.completeCheck` `requireNonNull` on the lists throws an NPE, which is not a refusal and would mark CHK's transaction as in X1. CHK always passes lists.
- (iii) The purge catches `DataAccessException | TransactionException` only; any other `RuntimeException` would end that night's run early. The next run resumes.
- (iv) `ReportStoreService.recordDecision` / `CheckRunCommandService.recordDecision` declare `rollbackFor = ReportNotStoredException`, which they never throw. This is harmless.
- (v) Boolean `APPROVAL_API_EXECUTED` relies on the dialect's 0/1 mapping (oracle19c target; same as REG/DOC). An Oracle 23 test container maps `Boolean` to native BOOLEAN, which `ddl-auto=validate` may reject — a test-environment concern shared with REG/DOC.

## Skill compliance — whole module

### gov-enforce-backend-contract (80)

| Layer | Checks | Passed | Failed | N/A | Status |
|---|---|---|---|---|---|
| Domain | 7 | 6 | 0 | 1 (A.0.4: domain classes return `RuleRefusal` values, SVC-API raises the typed exception — plan design, gap row 2) | ✅ — A.0.5 static `create`/`from`; A.0.2/A.0.3/A.0.6 `rpt/domain` imports only `java.*` and itself |
| Entity | 15 | 11 | 1 (A.1.14 — F2) | 3 (A.1.10 no cross-module column; A.1.11 no association; A.1.15 nothing extra) | ❌ LOW |
| Repository | 9 | 7 | 0 | 2 (A.2.4 no existence check; A.2.6 no association) | ✅ — A.2.7 count query (QR-006); A.2.8 every native statement binds its values; A.2.9 every method has a caller |
| DTO | 10 | 8 | 0 | 2 (A.3.2, A.3.3: no request DTO) | ✅ — A.3.7 unmodifiable lists; A.3.10 by Javadoc (no springdoc) |
| Port/Adapter | 10 | 2 (A.4.2 the adapter is the only class touching `chk.contract`; A.4.9 refusals and database failure in RPT types for `completeCheck`) | 0 | 8 (A.4.1, A.4.3–A.4.8, A.4.10: RPT has no outbound port — REQ-RPT-047…049) | ✅ — F5 noted |
| Service | 17 | 15 | 0 | 2 (A.5.11 RPT never calls the Approval API; A.5.17 no async work) | ✅ — A.5.7 every domain-owned rule delegated; A.5.12 no state; A.5.15 info logs carry ids/codes/counts only |
| Controller | 12 | 11 | 0 | 1 (A.6.7: no body) | ✅ — A.6.9 `@PathVariable("checkId")`; A.6.10 constants + Javadoc |
| **TOTAL** | **80** | **60** | **1** | **19** | **APPROVED with 1 LOW violation (F2)** |

CU.1 PASS (one advice, `basePackages = "io.agenticai.rpt"`) · CU.2 PASS · CU.3 N/A (RPT applies no `aias.check.*` limit; listing cap = constant `ReportLimits.LIST_LIMIT = 100`, ADR-RPT-008) · CU.4 PASS (CHK only through `chk.contract`) · CU.5 PASS (no external system) · CU.6 PASS (`aias.reports` → `ReportStoreProperties` only). No automatic rejection trigger fires.

### gov-enforce-library-contract (42)

| Group | Checks | Passed | Failed | N/A |
|---|---|---|---|---|
| G Guardrails | 10 | 3 (G.2 no Approval path; G.6 unread queries / MISSING / UNREADABLE stored and block COMPLIANT — RULE-RPT-005; G.9 no Check data in statics or caches) | 0 | 7 (G.1, G.3, G.4, G.5, G.7, G.8, G.10: RPT calls no model, runs no host query, opens no file — REQ-RPT-047…049) |
| D Decision logic | 6 | 5 | 0 | 1 (D.4: outcomes are values — plan design) |
| M Module boundaries | 6 | 5 (M.4 noted: F3) | 0 | 1 (M.5: ArchUnit test deferred, held by grep — gap row 1) |
| S Orchestration | 8 | 8 | 0 | 0 |
| P Persistence | 6 | 5 (P.2 hard delete + cascade) | 0 | 1 (P.3: no association) |
| W Web adapters | 6 | 5 | 0 | 1 (W.3: no body) |
| **TOTAL** | **42** | **31** | **0** | **11** |

Cross-cutting: error handling COMPLIANT · caching N/A (no cache annotation in RPT). Verdict: APPROVED.

### gov-enforce-error-handling (24)

| Check | Rules | Passed | Failed | N/A |
|---|---|---|---|---|
| Exception types | 5 | 4 — E.1.1 HTTP failures are `ReportStoreException` (catalog codes), in-process refusals are its `RptRefusalException` subclasses; E.1.2 0 `throw new RuntimeException/Exception`; E.1.3 the 20 `IllegalState/ArgumentException` sites are invariants (converter integrity, "locked RUNNING not updated", placeholder arity, cron at binding); E.1.4 no framework not-found escapes | 0 | 0 — E.1.5 PARTIAL (F5, LOW: translated only where the plan prescribes) |
| Code ↔ status | 4 | 3 | 0 | 1 (E.2.4: RPT relays no other module's refusal) |
| Code registration | 5 | 5 | 0 | 0 |
| Messages | 5 | 4 (E.4.3: messages carry ids, codes, field names, positions — never evidence, condition or detail texts; purge WARN carries the database message only, REQ-RPT-054) | 0 | 1 (E.4.5: no test in this unit) |
| Handling patterns | 5 | 5 — E.5.1 `orElseThrow(() -> new CheckNotFoundException(checkId))`; E.5.3 the two catches translate (completeCheck) or are the prescribed log-and-continue (purge, REQ-RPT-052/054); E.5.4 cause kept; E.5.5 every rule before the first write | 0 | 0 |
| **TOTAL** | **24** | **21** | **0** | **2** (+ E.1.5 partial, F5) |

Codes used but missing from the error-catalog: none in HTTP scope. The 16 in-process codes are the SVC-API table, outside the catalog by ADR-RPT-013 point 2. Verdict: COMPLIANT (F5 LOW noted).

## Validation

- `mvn -q -DskipTests compile` → exit 0 (after F1a/F1b; JDK 25 at `JAVA_HOME`)
- `mvn -q -DskipTests test-compile` → exit 0
- no test written, no test run

```yaml name=self-check
findings: 6
clean: false
fixed: 2               # F1a, F1b — Javadoc traces only (CheckResultStore, CheckRun DBF-RPT-018, UnfinishedCheckRow, RuleRefusal)
unfixed_low: 4         # F2 dead CheckRun transitions · F3 rpt.contract → rpt.error (CHK/REG/DOC pattern) · F4 ReportNotStoredException under RptRefusalException · F5 DataAccessException untranslated outside completeCheck
process_info: 1        # F6 PORTS / SVC-API not recorded
cross_module: 2        # X1 recorded as OPEN api_doc_gaps row · X2 savepoints unsupported (CHK/REG) reported, not recorded
accepted_deviations: 3 # the three earlier OPEN api_doc_gaps rows
binding_mismatches: 0
api_document_mismatches: 0
error_response_mismatches: 0
api_doc_gaps_added: 1
```
