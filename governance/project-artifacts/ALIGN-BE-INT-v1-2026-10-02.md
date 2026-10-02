# ALIGN-BE — INT v1 — code alignment audit — 2026-10-02

Unit `ALIGN-BE` (phase ALIGN-BE, kind PHASE, `tests: []`, acceptance = a clean build). The factory ran the ALIGN rows on the PLAN (`ALIGN-BE.md`, `findings: 0, clean: true`, `examined_nothing: C7.19, C7.20, C7.22, C7.23, C7.24, C7.28`). This report runs the same rows on the CODE the plan produced.

**Audit subject.** `src/main/java/io/agenticai/integration/**` (52 source files), plus:
- the INT block of `src/main/resources/messages.properties`;
- the `aias.integration.*` and `spring.servlet.multipart.*` keys of `application.properties`.

**Checked against:**
- `_SECTIONS.md`: registries, `totals`, the error-catalog (23 rows: 6 of INT's own and 17 PASS-THROUGH or RULE rows), RULE-INT-001…004;
- the unit files `CORE`, `DATA-DOM`, `PORTS`, `SVC-API-HEADER`, `SVC-API-COMMAND`, `SVC-API-QUERY` and `integration/XM-INT-001`;
- `api-spec-int.yaml`;
- `contract-int.md` (CON-INT-001…008) and the owners' contracts `contract-{reg,doc,chk,rpt}.md`;
- `analysis/decisions/INT/` (ADR-INT-001…029 on disk);
- the INT `execution-state.json` (read-only);
- the three `gov-enforce-*` skills.

No test was written or run (unit instruction). Acceptance is `mvn -q -DskipTests compile` + `test-compile`, and both exit 0.

**Audit subject at a glance:**
- 5 controllers and 7 services;
- 6 ports and 6 adapters (4 platform edges, plus the 2 XM-INT-001 adapters);
- 7 domain types: 5 records and 2 guards;
- 11 DTO records;
- 7 error classes: `IntegrationErrorCodes`, `IntegrationException` and its 2 subclasses, `IntegrationMessages`, `IntegrationTexts`, `IntegrationProblemAdvice`;
- 2 config classes and `package-info`;
- 14 INT message keys: 6 catalog keys, 7 `INT-DETAIL-*` keys and 1 `INT-LOG-*` key.

INT owns no table, entity, repository or QR (ADR-INT-015, ADR-INT-017 (2)).

**Expected, not findings.** The four OPEN `api_doc_gaps` rows below are known; they are listed again under "Accepted deviations".

## ALIGN table — code side

| row | backing check | verdict | evidence |
|---|---|---|---|
| TRACEABILITY | traces | PASS (after Javadoc fixes — F1) | Distinct ids cited in `integration/**`: **REQ-INT**: 54 by id, after F1 added REQ-INT-012 and REQ-INT-023. The others are cited through the ranges "REQ-INT-045 … REQ-INT-057" (`CheckReportController`) and "REQ-INT-021 … REQ-INT-039" (`DecisionController`). REQ-INT-041 is frontend-only and is in no API's traces. **DBF-INT**: 10/10. **RULE-INT**: 3/3 backend rules; RULE-INT-004 is the frontend's. **API-INT**: 8/8, as controller constants plus Javadoc. **CON-INT**: 8/8 (one `Honours` per operation). **ADR-INT**: 11 distinct. **No `XM-` string**: grep → 0. Before the fix, `error/IntegrationTexts.java` carried no governance id. |
| COVERED | orphans | PASS | Every REQ of the SVC-API traces is reached by a class, by id or by a cited range. The frontend-served REQs (040–043, 045–052, 055, 056, 066) sit on the reads API-INT-005…008, as `_SECTIONS.md` § Coverage states. |
| BINDING (§2A) | value-agreement | PASS | No column of INT's own. All 10 DBFs are read bindings carried by value:<br>• `CheckSnapshot`: `checkId` (001, `Long`), `status` (002), `serviceCode` (003), `versionNumber` (004, `Integer`), `requestNumber` (005), `employeeDecision` (007).<br>• `StartCheckCommand.employeeId` (006); `DecisionCommand.decidedBy` (008).<br>• `ApprovalDefinition.enabled` (009); `method`/`pathTemplate` parsed from 010.<br>The status and decision travel as String codes, not enums — OPEN gap row 2. |
| MANIFEST (§4) | count-agrees | PASS | **`DBF: 10`** = the 10 bindings above. **`API: 8`** = 8 mappings: 3 `@PostMapping` + 1 `@PostMapping`(decision) + 4 `@GetMapping`, across 5 controllers. **`QR: 0`** = no repository. **`XM: 1`** = 2 adapters of the one XM block (`RegApprovalDefinitionAdapter`, `RegVersionDocumentsAdapter`). **Error-catalog**: the 6 INT rows = the 6 `IntegrationErrorCodes` constants. |
| WRITERS | required-writer | PASS | INT writes no column. Every DBF's writer is the owner, as the manifest's "Writer" column states. `approvalApiExecuted` is set only in `DecisionService`: `true` on path 3, `false` on path 4 (REQ-INT-026). It is never read from the request: `DecisionRequest` has no such field. |
| QRC (§5) | orphans | N-A | QR: 0. |
| API (R3) | code-format | PASS | **Format**: `IntegrationException.CODE_FORMAT = ^INT-(\d{3})(?:-[A-Z0-9]+(?:-[A-Z0-9]+)*)?$`, and the status is the `{http}` segment. **INT codes**: all 6 match; statuses 400/409/413/500/502/504. **Pass-through codes**: they keep the owner's prefix and status (`ex.status()` of the owner's base type). |
| API DOCUMENT | api-spec-agree | PASS | **8 operations ↔ 8 controller methods**, field by field in the "API DOCUMENT detail" table below. **Method, path and success status** agree for each operation. **Location header**: only `startCheck` sends one (`ResponseEntity.accepted().location(...)`). **Path variables**: every `checkId` is `@PathVariable("checkId") Long` (int64). **Multipart parts**: `documentType` and `file`, as `@RequestPart` (required). **Query parameters**: `serviceCode` and `requestNumber` are bound `required = false`; deliberate, see accepted deviation (a). **DTO records = schemas**, field for field: `StartCheckRequest` 3; `StartedCheckResponse` 3; `UploadReceiptResponse` 6 (`notice` `NON_NULL`); `ConfirmedCheckResponse` 2; `DecisionRequest` 2; `RecordedDecisionResponse` 5; `UploadConfirmationRequest` 0; `CheckReportResponse` 18, with nested `Finding` 5, `Document` 6, `UnreadQuery` 3 and `Decision` 4 (null when absent); `ChecksOfRequestResponse {total, checks}` with `CheckItem` 6; `UploadedDocumentResponse` 6; `RequiredDocumentTypesResponse` 4. **Envelope**: none, plain JSON. |
| ERROR RESPONSES | api-spec-errors | PASS | **INT's own codes**: `IntegrationErrorCodes` = exactly the 6 rows (`INT-400-REQUEST-INVALID`, `INT-409-CHECK-NOT-AWAITING-DOCUMENTS`, `INT-413-UPLOAD-TOO-LARGE`, `INT-502-APPROVAL-API-FAILED`, `INT-504-APPROVAL-API-TIMED-OUT`, `INT-500`). The class is final, and its private constructor throws. **Inline codes**: grep of `"(INT\|RPT\|CHK\|DOC\|REG)-\d{3}"` outside that class → 0. **Texts**: a scripted diff of the 23 catalog English texts against `messages.properties` → 23/23 byte-identical; RPT's two-text code is keyed `CODE.SLUG`. **Pass-through**: every PASS-THROUGH or RULE row is reachable through `IntegrationProblemAdvice` with the owner's code, status and message unchanged — see the "PASS-THROUGH rows → handler" table. **Per operation (E.2.3)**: see the "E.2.3 — codes reachable per operation" table. |
| DOCUMENT VALID | api-spec-valid | PASS (parse only) | YAML parses and all `$ref`s resolve. The factory's 3.1 validation is not repeated. |
| RULE INPUTS | data-source | PASS | **RULE-INT-001** (`UploadGuard.requireUploadAllowed`, on DBF-INT-002 from `CheckRecordPort.read`) runs in `UploadService.upload` after the read and BEFORE `documentAccess.handOver`. On refusal nothing is handed over. **RULE-INT-002/003** (`ApprovalGuard.requireApprovalAllowed`) run in `DecisionService.approve`, inside the per-Check lock (`lock(checkId)` … `finally unlock`):<br>1. re-read the Check (`checkRecords.read`);<br>2. guard: code → decidedBy → status COMPLETED → undecided;<br>3. ONE `hostApproval.approve(...)`;<br>4. `handOverDecision(…, true)`.<br>**Nothing recorded on INT-502/504**: the adapter throws `ApprovalApiFailedException`/`ApprovalApiTimedOutException` before step 4, and nothing catches them. **Hand-over refused after a successful call**: WARN `INT-LOG-APPROVAL-NOT-RECORDED` (checkId, requestNumber) and rethrow unchanged (REQ-INT-039). **Path 4** (REJECTED, disabled version or unknown code): no definition or port call beyond the definition read, then `handOverDecision(…, false)`. |
| GUARDRAILS | G.2/A.5.11, G.9, REQ-INT-059/060 | PASS | **G.2 / A.5.11**: `HostApprovalPort` and `ApprovalDefinitionPort` are referenced only by their adapters and `DecisionService` (grep). `ApprovalApiRegistry` is imported only by `RegApprovalDefinitionAdapter` across all modules, apart from REG itself. `approve` is called once, at `DecisionService:132`. **One attempt**: no retry interceptor or template (grep `Retry` → 0); a plain `RestClient` on a JDK `HttpClient`; `Redirect.NEVER`; connect timeout = read timeout = `aias.integration.approval.timeout`. **URI**: `UriComponentsBuilder.fromUri(base).path(template).encode().buildAndExpand(requestNumber).toUri()`, never concatenated (the order differs from PORTS' text — OPEN gap row 3 (3)). **G.9**: the only mutable structure is `DecisionService.approvalLocks : ConcurrentHashMap<Long, CheckLock>`, holding the Check id → `ReentrantLock` and the holder count only. An entry is removed at 0 holders. No static non-final field; no cache annotation. **REQ-INT-059/060**: grep in `integration/**` for `DataSource`, `JdbcTemplate`, `McpQueryChannel`, `McpSyncClient`, `@Entity`, `JpaRepository`, `org.springframework.ai`, `java.nio.file`, `WebClient`, `RestTemplate` → 0 (`package-info` Javadoc only). |
| CONTRACT / CROSS-MODULE | registry-agree | PASS | **Imports**: other modules are imported only from `io.agenticai.{chk,doc,rpt,reg}.contract`, plus `io.agenticai.platform.config.CheckLimitsProperties` — 36 distinct imports, 0 from `.error`/`.service`/`.entity`/`.repository`; the FQN uses are also `.contract`. **Platform edges** (in-process interfaces, injected by type): `CheckEngine.startCheck/confirmUploads` (CON-CHK-004/005); `DocumentAccess.handOverUpload/listUploadedDocuments` (CON-DOC-003/006); `ReportStore.readCheck/listChecksOfRequest/recordDecision` (CON-RPT-003/004/006). **XM-INT-001 vs package text**: `RegApprovalDefinitionAdapter implements ApprovalDefinitionPort` wraps `ApprovalApiRegistry.getApprovalApi(serviceCode, int versionNumber)` (CON-REG-012) with the Check's own pair. `approvalEnabled` false → `disabled()`; otherwise `approvalApi` is split at its first whitespace into method + path. `VersionNotFoundException` → `INT-500`, cause kept. It is injected only into `DecisionService`. `RegVersionDocumentsAdapter implements VersionDocumentsPort` wraps `ServiceRegistry.getServicePackageVersion` (CON-REG-009) and returns `List.copyOf(requiredDocumentTypes)` in order; not-found → `INT-500`. It is injected only into `RequiredDocumentTypesService` and never receives the approval interface. `ServicePackageVersionView` carries no approval API definition, only the flag. **`requires`**: `REG:DELIVERED` → met (`gov-module.py requires INT`). Two additions beyond the package text are in OPEN gap row 3 (2): malformed-definition validation → INT-500, and a not-found log that carries service code/version rather than checkId. |
| FOREIGN IDS | xref-resolve | PASS | CON-CHK-001, CON-RPT-002, CON-REG-002/009/012 cited in code resolve to headings of the owners' contracts. |
| SECURITY (R5) | operation-resolves | PASS | Grep `PreAuthorize\|Secured\|RolesAllowed\|springframework.security` → 0. No security filter. Endpoints are open per the SRS (raw-idea A2, REQ-INT-004). |
| DEMAND (SRS) | operation-resolves | PASS | 7 screen-demand rows → API-INT-001…008, all built. |
| PATHS / DECISIONS | paths-resolve / refs-exist | PASS | **Messages**: every key the code uses is present in `messages.properties` (6 catalog + 7 `INT-DETAIL-*` + `INT-LOG-APPROVAL-NOT-RECORDED`), 0 missing. **Configuration**: `aias.integration.approval.timeout`, `approval.base-address` and `upload.request-limit` are documented (commented) in `application.properties`, with one binder, `IntegrationProperties` (CU.6). `spring.servlet.multipart.max-request-size`/`max-file-size` = `${aias.integration.upload.request-limit:50MB}`; `resolve-lazily=true`. `IntegrationLimitsCheck` fails start-up when the request limit is below `aias.check.max-file-size` (defaults 50 MB ≥ 10 MB). **ADRs**: the 11 cited ADR-INT ids (003, 009–013, 015, 017, 020, 023, 025) all exist in `analysis/decisions/INT/`. |
| COVERAGE | (the report) | this file | `findings: 5` — 1 fixed (Javadoc), 2 LOW open, 2 INFO. 0 API DOCUMENT / ERROR RESPONSES / RULE INPUTS / GUARDRAIL mismatches. |

### API DOCUMENT detail — operation ↔ controller method

| operationId (API) | api-spec | controller method | success |
|---|---|---|---|
| startCheck (001) | POST /api/v1/checks, JSON `StartCheckRequest` | `CheckIntakeController.start(@Valid @RequestBody StartCheckRequest)` | 202 + `Location: /api/v1/checks/{checkId}` + `StartedCheckResponse` (`checkUrl` = same address) |
| uploadDocument (002) | POST /api/v1/checks/{checkId}/documents, multipart `documentType` + `file` | `CheckUploadController.upload(@PathVariable("checkId") Long, @RequestPart("documentType") String, @RequestPart("file") MultipartFile)` | `@ResponseStatus(CREATED)` 201 |
| confirmUploads (003) | POST /api/v1/checks/{checkId}/upload-confirmation, JSON `{}` | `CheckUploadController.confirm(Long, @Valid @RequestBody UploadConfirmationRequest)` | 202 |
| recordEmployeeDecision (004) | POST /api/v1/checks/{checkId}/decision, JSON `DecisionRequest` | `DecisionController.decide(Long, @Valid @RequestBody DecisionRequest)` | 201 |
| readCheckReport (005) | GET /api/v1/check-reports/{checkId} | `CheckReportController.read(Long)` | 200 |
| listCheckReports (006) | GET /api/v1/check-reports?serviceCode&requestNumber | `CheckReportController.list(@RequestParam(name="serviceCode", required=false), …requestNumber…)` | 200 |
| listUploadedDocumentsOfCheck (007) | GET /api/v1/checks/{checkId}/documents | `CheckUploadController.list(Long)` → `List<UploadedDocumentResponse>` | 200 |
| readRequiredDocumentTypes (008) | GET /api/v1/checks/{checkId}/required-document-types | `RequiredDocumentTypesController.read(Long)` | 200 |

### PASS-THROUGH rows → handler (ERROR RESPONSES)

Every handler answers `problem(ex.code(), ex.status(), ex.getMessage())`, so the owner's code, status and text go out unchanged.

| catalog row | raised by | `IntegrationProblemAdvice` handler |
|---|---|---|
| CHK-400-START-INCOMPLETE | `chk.contract.StartIncompleteException` | `handleStartIncomplete` |
| CHK-422-SERVICE-NOT-AVAILABLE | `chk.contract.ServiceNotAvailableException` | `handleServiceNotAvailable` |
| CHK-422-CONNECTION-NOT-ACTIVATED | `chk.contract.ConnectionNotActivatedException` | `handleConnectionNotActivated` |
| CHK-404-CHECK-NOT-FOUND | `chk.contract.CheckNotFoundException` | `handleCheckEngineCheckNotFound` |
| CHK-409-CHECK-NOT-AWAITING-DOCUMENTS | `chk.contract.CheckNotAwaitingDocumentsException` | `handleCheckNotAwaitingDocuments` |
| DOC-400-INCOMPLETE-UPLOAD | `doc.contract.IncompleteUploadException` | `handleIncompleteUpload` |
| DOC-404-SERVICE-VERSION-NOT-FOUND | `doc.contract.ServiceVersionNotFoundException` | `handleServiceVersionNotFound` |
| DOC-422-FETCH-MODE-NOT-MANUAL | `doc.contract.FetchModeNotManualException` | `handleFetchModeNotManual` |
| DOC-422-DOCUMENT-TYPE-NOT-OF-SERVICE | `doc.contract.DocumentTypeNotOfServiceException` | `handleDocumentTypeNotOfService` |
| DOC-409-CHECK-ENDED | `doc.contract.CheckEndedException` | `handleDocumentCheckEnded` |
| DOC-422-UPLOAD-LIMIT-REACHED | `doc.contract.UploadLimitReachedException` | `handleUploadLimitReached` |
| RPT-404-CHECK-NOT-FOUND | `rpt.contract.CheckNotFoundException extends RptRefusalException` | `handleReportStoreRefusal` (status < 500) |
| RPT-400-DECISION-INCOMPLETE (RULE-INT-002) | `rpt.contract.DecisionIncompleteException`, thrown by `ApprovalGuard` or by RPT | `handleReportStoreRefusal` |
| RPT-409-CHECK-NOT-COMPLETED (RULE-INT-003) | `rpt.contract.CheckNotCompletedException`, thrown by `ApprovalGuard` or by RPT | `handleReportStoreRefusal` |
| RPT-409-DECISION-ALREADY-RECORDED (RULE-INT-003) | `rpt.contract.DecisionAlreadyRecordedException`, thrown by `ApprovalGuard` or by RPT | `handleReportStoreRefusal` |
| RPT-422-APPROVAL-FLAG-ON-REJECTION | `rpt.contract.ApprovalFlagOnRejectionException` | `handleReportStoreRefusal` |
| RPT-400-REQUEST-KEYS-MISSING | `rpt.contract.RequestKeysMissingException` (outside the RptRefusal base) | `handleRequestKeysMissing` |

Exceptions that map to INT-500 by design:

| exception | answer | why |
|---|---|---|
| `RPT-500-REPORT-NOT-STORED` | `INT-500` | `handleReportStoreRefusal` sends status ≥ 500 to `unexpected` (REQ-INT-008) |
| `DOC-400-CHECK-ID-REQUIRED`, REG `VersionNotFoundException` | `INT-500` | the catch-all handler, or the adapters' `INT-500` (ADR-INT-023 (2), XM-INT-001) |

### E.2.3 — codes reachable per operation

| operation | INT's own (source) | pass-through | ⊆ x-error-codes |
|---|---|---|---|
| 001 | 400 (unreadable JSON → `HttpMessageNotReadableException`), 500 | CHK-400, CHK-422 ×2 | yes |
| 002 | 400 (part missing / multipart unreadable / non-numeric id / `getBytes` IOException), 409 (`UploadGuard`), 413 (`MaxUploadSizeExceededException`), 500 | RPT-404, DOC-400/404/409/422 ×3 | yes |
| 003 | 400 (id, body), 500 | CHK-404, CHK-409 | yes |
| 004 | 400, 502/504 (`HttpHostApprovalAdapter`), 500 | RPT-404/400/409 ×2/422 | yes |
| 005 | 400, 500 | RPT-404 | yes |
| 006 | 500 | RPT-400-REQUEST-KEYS-MISSING | yes |
| 007 | 400, 500 | — | yes |
| 008 | 400, 500 | RPT-404 | yes |

The one exception: an unsupported `Content-Type` falls to the catch-all as INT-500 (OPEN gap row 4 (3)).

## Findings

| # | location | what differs | severity | fixed? |
|---|---|---|---|---|
| F1 | `error/IntegrationTexts.java:3-7`; `service/UploadService.java:27-28`; `dto/DecisionRequest.java:17` | TRACEABILITY — `IntegrationTexts` carried no governance id. REQ-INT-012 (an upload for an unknown Check is refused) is implemented in `UploadService` step 2 but was cited by no class by id. REQ-INT-023 (decidedBy exactly as the host sent it) was cited only through a range. | trivial | YES — Javadoc only. `IntegrationTexts` before: `… (the advice's detail values, the SVC-API log lines): … by the same mechanism — …`. After: `… (the advice's detail values, the SVC-API log lines such as the REQ-INT-039 WARN): … by the same mechanism (ADR-INT-017) — …`. `UploadService` step 2 before: `(unknown → RPT-404-CHECK-NOT-FOUND, passed through);`. After: `(… passed through — REQ-INT-012);`. `DecisionRequest @param decidedBy` before: `… exactly as the host sent it, string ≤ 100, required`. After: `… exactly as the host sent it (REQ-INT-023), string ≤ 100, required`. Recompiled clean. |
| F2 | `domain/ApprovalGuard.java:3-5` | A.0.6 / D.5 — a domain class imports another module: it throws `rpt.contract.DecisionIncompleteException`, `CheckNotCompletedException` and `DecisionAlreadyRecordedException` directly. ADR-INT-010 requires the Report Store's OWN codes and texts, and this is the plan-named owner (`ApprovalGuard` → RPT-400/409/409), so the behaviour is as planned. The skills' shape would be a framework-free refusal value from the guard, mapped to the RPT contract exception by `DecisionService`. | LOW | NO — moving the throw is a code change, not a mechanical alignment. |
| F3 | `controller/CheckUploadController.java:79-80` vs catalog row DOC-400-INCOMPLETE-UPLOAD | A request with NO `documentType` (or no `file`) part fails parameter binding (`MissingServletRequestPartException`) and answers `INT-400-REQUEST-INVALID`. The catalog's DOC-400 trigger reads "an upload without a document type or with an empty file", so only an EMPTY `documentType` or an empty file reaches Document Access and answers DOC-400. Both codes are listed under the operation's 400, so E.2.3 holds. The plan does not say which code an ABSENT part gets. | LOW | NO — plan-silent; candidate for the factory. Not recorded: execution-state is read-only for this unit. |
| F4 | `adapter/RegApprovalDefinitionAdapter.java:67`, `RegVersionDocumentsAdapter.java:46` | XM-INT-001 says REG's not-found → INT-500 "logged with the Check identifier". The adapters log service code + version (they do not receive the checkId); the advice's INT-500 ERROR line carries `checkId` from the path. The intent is met across two log lines. | INFO | NO. |
| F5 | `gov-module.py delivery INT` | Process, not code: PORTS, SVC-API-COMMAND, SVC-API-QUERY and XM-INT-001 show `not run` although their code is present and compiles. Their acceptance is `tests-green` (INT-XM / API-SCENARIOS / RULE-SCENARIOS), which this unit does not execute. | INFO | NO — record them once their tests run. |

Accepted deviations (not findings — each is an OPEN or RESOLVED `api_doc_gaps` row already in the state file):
- **(a)** Row `GET /api/v1/check-reports` (RESOLVED): `serviceCode`/`requestNumber` are bound `required = false`, so an absent key reaches RPT and answers `RPT-400-REQUEST-KEYS-MISSING`. The document declares the parameters `required: true`.
- **(b)** Row `POST …/documents` (OPEN): CHK and RPT publish no status or decision enum in `.contract`. INT compares the code strings `AWAITING_DOCUMENTS`, `COMPLETED`, `APPROVED` and `REJECTED` (`UploadGuard`, `ApprovalGuard`, `DecisionService`).
- **(c)** Row `POST …/decision` (OPEN): three points:
  - the `INT-DETAIL-APPROVAL-UNREACHABLE` text;
  - a malformed enabled definition → INT-500;
  - `encode().buildAndExpand()` instead of PORTS' `buildAndExpand().encode()` — required by AC-INT-035 (`2026/77 A` → `2026%2F77%20A`).
- **(d)** Row `POST /api/v1/checks` (OPEN): three points:
  - no bean-validation on the request DTOs, so the owners' codes come first;
  - `additionalProperties: false` is not enforced on `UploadConfirmationRequest`;
  - an unsupported Content-Type → INT-500.

Observations (no row affected):
- **(i)** `CheckSnapshot.versionNumber` is an `Integer` and is unboxed into the `int` parameters of `definitionOf`, `handOver` and `requiredDocumentTypes`. It would throw an NPE only if RPT returned a null version, which its NOT NULL column excludes.
- **(ii)** `HttpHostApprovalAdapter` catches `ResourceAccessException` only. Any other `RestClientException` (e.g. a body conversion failure) reaches the catch-all as INT-500. That is still "nothing recorded", and still a single attempt.
- **(iii)** The approval lock is per JVM (ADR-INT-017 (6)), and RPT's conditional UPDATE remains the final guard.

## START-UP READINESS (deployable-wide, `src/main/java/io/agenticai/**`)

Method: a static scan of all 380 sources. It covers stereotype annotations, Spring Data interfaces, `@Bean` methods, `@ConfigurationProperties`, `@RestControllerAdvice` arguments, request mappings and constructor or `@Autowired` injection points. Each injection type was resolved against the classes that implement it or the `@Bean` methods that return it, with ambiguous simple names checked by FQN. It is not a context start: tests and the application run were not allowed, and an Oracle database is required to start anyway.

**1. Bean-name collisions — 0. Nothing to fix.** 99 beans:
- 84 stereotype classes;
- 13 Spring Data repositories;
- 2 qualified `@Bean ChatModel`s: `comparisonModel` (conditional, CHK) and `documentReadingModel` (DOC).

The repositories are `activeCheckRepository`, `checkDocumentRepository`, `checkRunRepository`, `connectionRepository`, `endedCheckRepository`, `findingRepository`, `loadResultRepository`, `requiredDocumentRepository`, `servicePackageRepository`, `servicePackageVersionRepository`, `serviceQueryRepository`, `unreadQueryRepository` and `uploadedDocumentRepository`. Every effective name is unique.

Two simple class names repeat across modules, and both already resolve to distinct bean names:
- `CheckReportController`: RPT → `checkReportController`, INT → `intCheckReportController` (explicit);
- `RegConnectionAdapter`: CHK → `chkRegConnectionAdapter` (explicit), DOC → `regConnectionAdapter`.

All 17 INT beans already carry explicit `int*` names: 5 controllers, 7 services and 5 adapters including the HTTP adapter. The remaining INT beans use default names that cannot collide: `integrationProblemAdvice`, `integrationLimitsCheck` and the scanned `IntegrationProperties`.

**JPA (adjacent check).** The 13 `@Entity` simple names are unique, so there is no Hibernate entity-name clash. The two `autoApply` `FetchModeConverter`s convert different enums (`reg.domain.FetchMode` and `rpt.domain.FetchMode`).

**2. `@ConfigurationProperties` prefixes (CU.6) — 0 duplicates.**

| prefix | binder |
|---|---|
| `aias.registry` | `reg.config.ServiceRegistryProperties` |
| `aias.documents` | `doc.config.DocumentAccessProperties` |
| `aias.check` | `platform.config.CheckLimitsProperties` |
| `aias.check.comparison-model` | `chk.config.ComparisonModelProperties` |
| `aias.reports` | `rpt.config.ReportStoreProperties` |
| `aias.integration` | `integration.config.IntegrationProperties` |

`aias.check` and `aias.check.comparison-model` nest, but they are distinct keys, not a duplicate. Registration is through `@ConfigurationPropertiesScan`, whose bean names are `<prefix>-<FQCN>` and so unique.

**3. `@RestControllerAdvice` scopes — 5 advices, each `basePackages` = its own module, no overlap.**
- `ServiceRegistryProblemAdvice` → `io.agenticai.reg`
- `DocumentAccessProblemAdvice` → `io.agenticai.doc`
- `CheckEngineProblemAdvice` → `io.agenticai.chk`
- `ReportStoreProblemAdvice` → `io.agenticai.rpt`
- `IntegrationProblemAdvice` → `io.agenticai.integration`

None is a package prefix of another, and none is global. There are no `basePackageClasses`, `assignableTypes` or `annotations` selectors.

**4. Route uniqueness — 16 (method, path) pairs, 0 duplicates.**

| method | path | controller |
|---|---|---|
| GET | /api/v1/services | REG ServiceRegistryController |
| GET | /api/v1/services/{} | REG ServiceRegistryController |
| GET | /api/v1/load-results | REG ServiceRegistryController |
| GET | /api/v1/uploaded-documents | DOC UploadedDocumentController |
| GET | /api/v1/active-checks/{} | CHK ActiveCheckController |
| GET | /api/v1/checks | RPT CheckReportController |
| GET | /api/v1/checks/{} | RPT CheckReportController |
| GET | /api/v1/decision-agreement | RPT CheckReportController |
| POST | /api/v1/checks | INT CheckIntakeController |
| POST | /api/v1/checks/{}/documents | INT CheckUploadController |
| GET | /api/v1/checks/{}/documents | INT CheckUploadController |
| POST | /api/v1/checks/{}/upload-confirmation | INT CheckUploadController |
| POST | /api/v1/checks/{}/decision | INT DecisionController |
| GET | /api/v1/checks/{}/required-document-types | INT RequiredDocumentTypesController |
| GET | /api/v1/check-reports | INT CheckReportController |
| GET | /api/v1/check-reports/{} | INT CheckReportController |

`/api/v1/checks` is shared by RPT (GET) and INT (POST) on different methods. The `{checkId}` sub-paths are distinct literals, so no pattern overlaps.

**5. Required beans with no implementation — 0 start-up blockers.** 165 injection points scanned.

Every application-typed injection resolves to exactly one bean. The one simple-name ambiguity, `ConnectionLookup`, is two different interfaces (`chk.port` / `doc.port`), each with one adapter.

The remaining injection points are framework-provided:
- `PlatformTransactionManager` (data-jpa auto-configuration) — `RegistryLoadRun`, `ReportPurgeService`, `StartupRecoveryService`, `CheckEndingService`;
- the shared `EntityManager` registered by Spring Data JPA — `RegistryLoadRun`, `LoadLock`, `JdbcSavepoints`;
- `Environment` — `EnvironmentDataClass`, `JdbcDocumentSourceAdapter`, both `@Bean` methods;
- `List<DocumentSourceQueryPort>` (a collection, 2 adapters).

The optional or conditional dependencies all go through `ObjectProvider`, so they are not blockers:
- `McpQueryChannel` (`platform.mcp`) has **no implementation** in `src/main/java`. It is injected only as `ObjectProvider<McpQueryChannel>` (`chk.adapter.McpServiceQueryAdapter`, `doc.adapter.McpDocumentSourceAdapter`), so it is fine at start-up.
- `ChatModel` is injected only as `@Qualifier`'d `ObjectProvider` (`SpringAiComparisonAdapter`, `SpringAiDocumentReadingAdapter`). `spring.ai.model.chat=none` disables the unqualified OpenAI auto-configured model, so no key is needed at start-up.

INT's own start-up check is `IntegrationLimitsCheck`: request-limit 50 MB ≥ `aias.check.max-file-size` 10 MB by default, so it passes.

**Environment prerequisites (not code defects):**
- an Oracle `DataSource`;
- `spring.jpa.open-in-view=false` is already set, so no connection is held across the Approval API call.

## Skill compliance — INT

### gov-enforce-backend-contract (80)

| Layer | Checks | Passed | Failed | N/A | Status |
|---|---|---|---|---|---|
| Domain | 7 | 6 (A.0.1 `UploadGuard`/`ApprovalGuard` exist; A.0.2/A.0.3 no framework, no port or network; A.0.4 `UploadGuard` → INT-409, `ApprovalGuard` → RPT codes per ADR-INT-010; A.0.5 static `forCheck`/`forDecision`; A.0.7) | 1 (A.0.6 — F2) | 0 | ❌ LOW |
| Entity | 15 | 0 | 0 | 15 (no entity — ADR-INT-007/013/015) | N/A |
| Repository | 9 | 0 | 0 | 9 (no repository — ADR-INT-017 (2)) | N/A |
| DTO | 10 | 9 (A.3.3 `approvalApiExecuted` not accepted; A.3.5 no envelope; A.3.6 `OffsetDateTime`; A.3.7 `List.copyOf`; A.3.8 one `of(...)` per record; A.3.10 by Javadoc, no springdoc) | 0 | 1 (A.3.2 — constraints deliberately absent, gap row (d)) | ✅ |
| Port/Adapter | 10 | 5 (A.4.1; A.4.2 each owner interface / HTTP touched only by its adapter; A.4.8 approval timeout applied; A.4.9 HTTP and REG not-found translated, owners' refusals deliberately relayed; A.4.6 the approval adapter is no model tool) | 0 | 5 (A.4.3, A.4.4, A.4.5, A.4.7, A.4.10 — INT runs no query, opens no path, calls no model) | ✅ |
| Service | 17 | 12 (A.5.1; A.5.2; A.5.5 no `@Transactional` across the host call, OSIV off; A.5.6; A.5.7 guards delegated; A.5.9; A.5.10; A.5.11; A.5.12 ids-only lock map; A.5.13; A.5.14; A.5.15 info for writes / debug for reads, ids and codes only, never bytes or decidedBy) | 0 | 5 (A.5.3/A.5.4 INT owns no table; A.5.8 no service-owned rule; A.5.16 exact keys only; A.5.17 async work is CHK's) | ✅ |
| Controller | 12 | 12 (A.6.3 8/8 operations; A.6.5 202/201/202/201/200×4; A.6.7 `@Valid` on all 3 `@RequestBody`s, multipart limits bound; A.6.9 `@PathVariable("checkId")`; A.6.10 constants + Javadoc; A.6.12 zero logic) | 0 | 0 | ✅ |
| **TOTAL** | **80** | **44** | **1** | **35** | **APPROVED with 1 LOW violation (F2)** |

CU checks:
- CU.1 PASS — one advice, `basePackages = "io.agenticai.integration"`.
- CU.2 PASS.
- CU.3 PASS — INT reads `aias.check.max-file-size` only for the start-up comparison.
- CU.4 PASS — `.contract` only.
- CU.5 PASS — the Approval API only through `HostApprovalPort`.
- CU.6 PASS — `aias.integration` → `IntegrationProperties` only.

No automatic rejection trigger fires.

### gov-enforce-library-contract (42)

| Group | Checks | Passed | Failed | N/A |
|---|---|---|---|---|
| G Guardrails | 10 | 2 (G.2 approval only from `DecisionService.approve`, after RULE-INT-002/003, once; G.9 no Check data in statics or caches) | 0 | 8 (G.1, G.3–G.8, G.10: INT calls no model, runs no host query, opens no file — REQ-INT-015/060) |
| D Decision logic | 6 | 5 | 1 (D.5 — F2) | 0 |
| M Module boundaries | 6 | 5 (M.1/M.2 `.contract` only; M.3 INT is tier 4 and nothing imports INT; M.4 contract types are records/interfaces/exceptions — owners' contract exceptions extend their `.error` base, the CHK/DOC/RPT pattern; M.6 no HTTP between modules) | 0 | 1 (M.5: ArchUnit not run, boundary held by grep) |
| S Orchestration | 8 | 6 | 0 | 2 (S.3/S.4: no table) |
| P Persistence | 6 | 2 (P.1 no entity; P.5 holds nothing — REQ-INT-059) | 0 | 4 (P.2, P.3, P.4, P.6) |
| W Web adapters | 6 | 6 | 0 | 0 |
| **TOTAL** | **42** | **26** | **1** | **15** |

Cross-cutting: error handling COMPLIANT; caching N/A (no cache annotation). Verdict: APPROVED with 1 LOW (F2).

### gov-enforce-error-handling (24)

| Check | Rules | Passed | Failed | N/A |
|---|---|---|---|---|
| Exception types | 5 | 5 (E.1.1 INT failures are `IntegrationException` with catalog codes, owners' refusals relayed; E.1.2 0 `throw new RuntimeException/Exception`; E.1.3 `IllegalArgument/StateException` only for configuration binding, the start-up limit, a disabled definition passed to `approve`, and placeholder arity; E.1.4; E.1.5 `ResourceAccessException`/timeout → INT-502/504, `IOException` → INT-400, REG `VersionNotFoundException` → INT-500, causes kept) | 0 | 0 |
| Code ↔ status | 4 | 4 (E.2.3 per-operation table, gap row (d) noted; E.2.4 17 relayed rows unchanged) | 0 | 0 |
| Code registration | 5 | 5 | 0 | 0 |
| Messages | 5 | 4 (E.4.1 named placeholders; E.4.3 `UploadCommand.toString` hides bytes, no body or decidedBy logged; E.4.4 Arabic PENDING ADR-INT-017) | 0 | 1 (E.4.5: no test in this unit) |
| Handling patterns | 5 | 4 (E.5.2; E.5.3 the 5 catches translate or log-and-rethrow (REQ-INT-039); E.5.4; E.5.5 RULE-INT-001 before the handover, RULE-INT-002/003 before the Approval API call) | 0 | 1 (E.5.1: INT does no lookup of its own — not-found is the owner's) |
| **TOTAL** | **24** | **22** | **0** | **2** |

Codes used but missing from the error-catalog: none. Verdict: COMPLIANT.

## Validation

- `mvn -q -DskipTests compile` → exit 0 (after F1; JDK 25 at `JAVA_HOME`)
- `mvn -q -DskipTests test-compile` → exit 0
- no test written, no test run

```yaml name=self-check
findings: 5
clean: false
fixed: 1                  # F1 — Javadoc traces only (IntegrationTexts, UploadService REQ-INT-012, DecisionRequest REQ-INT-023)
unfixed_low: 2            # F2 ApprovalGuard imports rpt.contract (ADR-INT-010) · F3 absent multipart part → INT-400 vs DOC-400 wording
info: 2                   # F4 not-found log split across adapter + advice · F5 PORTS/SVC-API/XM-INT-001 not recorded
accepted_deviations: 4    # the four existing api_doc_gaps rows (1 RESOLVED, 3 OPEN)
api_document_mismatches: 0
error_response_mismatches: 0
rule_input_mismatches: 0
guardrail_violations: 0
startup_bean_name_collisions: 0
startup_bean_names_fixed: 0
startup_cfgprops_duplicates: 0
startup_advice_overlaps: 0
startup_route_duplicates: 0
startup_blockers: 0
api_doc_gaps_added: 0
```
