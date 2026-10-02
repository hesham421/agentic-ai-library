# ALIGN-BE — REG v1 — code alignment audit — 2026-10-01

Unit `ALIGN-BE` (phase ALIGN-BE, kind PHASE, `tests: []`, acceptance = a clean build). The factory ran the ALIGN rows on the PLAN (`ALIGN-BE.md`, `findings: 0, clean: true`); this report runs the same rows on the CODE the plan produced — `src/main/java/io/agenticai/reg/**` (79 classes) and `src/main/resources/messages.properties` — against `_SECTIONS.md`, `api-spec-reg.yaml`, `db-script-reg.md`, `contract-reg.md`, `SVC-API.md` and the three `gov-enforce-*` skills. No test was written or run (user instruction); acceptance is `mvn -q -DskipTests compile` + `test-compile`, both exit 0.

Audit subject at a glance: 6 entities · 4 converters · 9 domain classes · 5 port types · 2 adapters · 7 repositories (12 query methods) · 23 service classes · 2 DTOs · 1 controller · 11 contract types · 4 error classes · 1 properties record · 34-line message bundle.

## ALIGN table — code side

| row | backing check | verdict | evidence |
|---|---|---|---|
| TRACEABILITY | traces | PASS (after 7 Javadoc fixes) | 49/49 distinct `DBF-REG-*` ids in `entity/`; 3/3 `API-REG-*` in the controller; `CON-REG-003/005/006/007/009/011/012/013` in `contract/`, `CON-REG-007…013` in `service/`; 28/28 `RULE-REG-*`, 70/74 `REQ-REG-*`, 14 `ADR-REG-*` cited. Before the fix the three controller methods named no REQ (API-REG-001 → REQ-REG-013, API-REG-002 → REQ-REG-014/015 were absent from code) and `LoadRunTally`, `ServiceRegistryException`, `ServiceRegistryProblemAdvice` carried no governance id — fixed, Javadoc only (see Findings F0). |
| COVERED | orphans | PASS | REQ-REG-001…074: 72 cited in code after the fix; the 2 uncited are not code artifacts — REQ-REG-058 (the pilot package's numeric thresholds written as digits: content of the delivered package folder, SVC-API step 6) and REQ-REG-013/015 now cited. Every REQ the SVC-API header traces (35) is cited. |
| BINDING (§2A) | value-agreement | PASS | scripted diff of every `@Table`/`@Column`/`@UniqueConstraint`/`@Index` against the db-script `sql` fence and `dbf-matrix`: 49/49 DBF rows aligned (name, Java type, nullability, `length` = `VARCHAR2(n CHAR)`), 5/5 `UQ_*` and 6/6 `IDX_*` present with identical names and column lists, 0 extra columns. FKs are ids not associations (as the dbf-matrix names them); CHECK constraints live in the DDL (`ddl-auto=validate`, schema from the db-script) and are mirrored by the four converters' closed value sets. |
| MANIFEST (§4) | count-agrees | PASS | 6 `@Entity` classes = 6 tables; 3 `@GetMapping` = 3 operations; 5 QR methods (QR-REG-001…005) = `QR: 5`; 25 `REG-LOAD-*` constants = 25 table rows; `ServiceRegistryErrorCodes` = 2 catalog rows; 1 serve code `REG-SERVE-CONNECTION-NOT-ACTIVATED` (`ServeRefusalCodes`); `XM: 0`. |
| WRITERS | required-writer | PASS | every NOT NULL column is written by the load run (`ServicePackage.register/withdraw/makeAvailable`, `ServicePackageVersion.loaded`, `ServiceQuery.of`, `RequiredDocument.of`, `Connection.fromActivation/updateFromActivation`, `LoadResult.recorded`); `CREATED_AT`/`UPDATED_AT` are `insertable = false, updatable = false` — DB `DEFAULT SYSTIMESTAMP` as the manifest states. |
| QRC (§5) | orphans | PASS (1 LOW finding) | QR-REG-001 `ServicePackageRepository.findByAvailableTrueOrderByServiceCodeAsc` · QR-REG-002 `ServicePackageVersionRepository.findFirstByServicePackageIdOrderByVersionNumberDesc` · QR-REG-003 `RequiredDocumentRepository.findByServicePackageVersionIdOrderByDocumentTypeAsc` · QR-REG-004 `ServicePackageRepository.findByServiceCode` · QR-REG-005 `LoadResultRepository.findLatestRun` (JPQL `max(loadRunAt)`, `order by subjectKind, subjectName`). All 12 repository methods have a caller outside `repository/` (grep table in the audit log). The 7 non-QR methods are prescribed inline by the plan (RULE-REG-025 blob query, RULE-REG-023 guard, RULE-REG-005/017 existence, CON-REG-009/011/012 reads). F1: QR-REG-002's catalogued result shape excludes the two CLOBs; the entity read loads them. |
| API (R3) | code-format | PASS | `ServiceRegistryException.CODE_FORMAT = ^REG-(\d{3})(-SLUG)*$` is enforced at construction; both codes match; statuses 404/500 are in the platform set. |
| API DOCUMENT | api-spec-agree | PASS | `api-spec-reg.yaml` parses (OpenAPI 3.1.0, 3 operations). `ServiceRegistryController` (`@RequestMapping("/api/v1")`): `GET /services` ↔ `listServices` (API-REG-001), `GET /services/{serviceCode}` ↔ `readService` (`@PathVariable("serviceCode")`, API-REG-002), `GET /load-results` ↔ `readLoadReport` (API-REG-003); no other mapping, no POST/PUT/PATCH/DELETE. `ServiceSummary` = schema field for field (6 required; `fetchMode` = `FetchMode.storedValue()` ∈ `path|blob|manual`); `LoadResultResponse` = `LoadResult` schema (5 required non-null, `serviceCode`/`versionNumber`/`reason` nullable; `subjectKind` ∈ `SERVICE_PACKAGE|CONNECTION|PACKAGE_DIRECTORY`; `outcome` = the 7 stored values). Plain arrays/objects, no envelope. |
| ERROR RESPONSES | api-spec-errors | PASS | `ServiceRegistryErrorCodes` holds exactly `REG-404-SERVICE-NOT-FOUND` and `REG-500` (grep of `"REG-[0-9]{3}` in `src/main`: 2 literals, both in that class). API-REG-002 raises 404 through `ServiceNotAvailableException extends ServiceRegistryException` (`ServiceRegistryQueryService.getService`); 001/003 raise only `REG-500`. `ServiceRegistryProblemAdvice` (the one `@RestControllerAdvice`, `basePackages = io.agenticai.reg`) answers `ProblemDetail` with `type/title/status/detail` + `code`, status = the code's `{http}` segment; every other exception → `REG-500`, logged with the path, no stack trace in the body. `messages.properties` carries both catalog texts verbatim. |
| DOCUMENT VALID | api-spec-valid | PASS (parse only) | YAML parses; every `$ref` target exists; the factory's full 3.1 validation is not repeated here. |
| RULE INPUTS | data-source | PASS | 25 `REG-LOAD-*` codes: `LoadReasonCodes` set ≡ plan table ≡ `messages.properties` keys, and every English text is byte-identical to the SVC-API table (diff empty). Implementing sites — RULE-REG-001/005…012/018…022/024/026/027 `PackageValidator` (plan order 026, 024, 020, 001, 018, 012, 022, 027, 008, 019, 021, 006, 007, 005, 009, 010, 011 is the code order); RULE-REG-002 `DuplicateServiceCodes`; 003/004 `VersionRegistration`; 013/014/015/025/027 `ConnectionActivation`; 023 `PackageDirectoryGuard` + `RegistryLoadRun`; RULE-REG-016 `ServiceRegistryService.getCurrentServicePackage` (unknown or withdrawn) and `ServiceRegistryQueryService.getService` (unknown → 404); RULE-REG-017 `ServiceRegistryService` → `ServiceConnectionNotActivatedException` (`REG-SERVE-CONNECTION-NOT-ACTIVATED`); RULE-REG-028 `LoadResultText.fit` (200/100/1000, code-point safe, `…`) applied by `LoadResultRecorder` before every insert. Every rule reads named inputs (port values, `REG_CONNECTION`, `REG_SVC_PKG_VER`). |
| CROSS-MODULE | registry-agree | N-A (0 XM) | `grep -rn 'XM-' src/main` → 0. `io.agenticai` holds only `reg`. |
| INTEGRATION | xm-block-complete | N-A (0 XM) | as above. |
| FOREIGN IDS | xref-resolve | PASS | no `*-DOC-/-CHK-/-RPT-/-INT-` id in REG code; the non-REG ids cited (`G2/G3/G4/G9`, `AIAS-4`, `TC-REG-017`, `PF-7` via contract) resolve: `TC-REG-017` in `P4/backend-test-plan-reg.md` and `PORTS.package.json`, `PF-7` in `analysis/platform/project-registry.md`. |
| SECURITY (R5) | operation-resolves | PASS | no `@PreAuthorize/@Secured/@RolesAllowed`, no `springframework.security`, no filter, no `aias.check.*` in REG or `application.properties` (grep: none). Entity operations resolve to API-REG-001/002/003 or the in-process interface as the entity registry states. |
| DEMAND (SRS) | operation-resolves | N-A (0 SCR-REQ) | REG has no screen. |
| DECISIONS | refs-exist | PASS | ADR-REG-003/007/008/009/010/011/015/016/017/018/019/020/021/022 cited in code; `analysis/decisions/REG/` holds ADR-REG-001…022. |
| PATHS | paths-resolve | PASS | `messages.properties` on the classpath (`ResourceBundle "messages"`, no-fallback control → base bundle); `aias.registry.*` keys of `application.properties` (commented samples) = the fields of `ServiceRegistryProperties` (`package-directory`, `environment-name`, `load-lock-timeout`, `connections[].name/type/endpoint/query-tool/dialect/credential-reference/read-only/limited-to-views`); `@ConfigurationPropertiesScan` on `Application`; the state file's cited `analysis/modules/REG/_state/briefs/P0.md` and the skill's `analysis/domain/domain-profile.md` exist. |
| COVERAGE | (the report) | this file | `findings: 4` (0 BINDING/API/ERROR, 1 QRC efficiency, 1 dead port method, 1 domain-factory style, 1 process); 1 accepted deviation pending the OPEN gap row. |

## Findings

| # | location | what differs | severity | fixed? |
|---|---|---|---|---|
| F0 | `controller/ServiceRegistryController.java:41-69`, `service/LoadResultRecorder.java:16`, `service/LoadRunTally.java:3`, `error/ServiceRegistryException.java:7`, `error/ServiceRegistryProblemAdvice.java:11` | TRACEABILITY — the three controller methods named their API id but not the REQs the API registry traces (REQ-REG-013; REQ-REG-014/015; REQ-REG-008), and three foundation classes carried no governance id. | trivial | YES — Javadoc only: added the REQ/DBF/CON traces per method, `REQ-REG-061` to the recorder, `REQ-REG-007` to the tally, `ADR-REG-011` + the catalog's API ids to the two error classes. Recompiled clean. |
| F1 | `repository/ServicePackageVersionRepository.java:26` ← `service/ServiceRegistryQueryService.java:88` | QR-REG-002's catalogued result shape is a projection `VERSION_NUMBER, FETCH_MODE, APPROVAL_ENABLED` "(never SERVICE_KNOWLEDGE, SERVICE_DEFINITION)"; the derived query returns the full `ServicePackageVersion`, so API-REG-001/002 read both CLOBs per package. The DTO exposes neither (REQ-REG-030 holds); the cost is I/O only. The same method legitimately serves CON-REG-007, which needs the knowledge. | LOW (efficiency; plan result shape not honoured) | NO — a projection interface for the HTTP path is a code change the orchestrator decides. Not an API-document / db-script contradiction, so no `api_doc_gaps` row. |
| F2 | `port/PackageSource.java:49` / `adapter/FileSystemPackageSource.java:76`; `service/RegistryLoadRun.java` | `refusedEntries()` (symlink entries resolving outside the package directory, REQ-REG-016) is implemented and documented as "exposed so that nothing is skipped silently", but no class in `src/main` calls it — the load run neither logs nor reports a refused entry (TC-REG-017 prescribes no Load Result row, so only a log line is missing). Dead port method in production (A.2.9 / P.6 spirit; G.6 spirit). | LOW | NO — orchestrator decides: a `log.warn` per refused entry in `RegistryLoadRun.loadPackages`, or removal of the port method. |
| F3 | `domain/ServiceDefinitionParser.java:88` ← `service/PackageValidator.java:64` | A.0.5 — the domain class is instantiated with `new ServiceDefinitionParser()` (public no-arg constructor), not through a static factory. Stateless, so harmless; a style deviation from the layer contract. | LOW | NO — two-file change (private constructor + factory), left to the orchestrator. |
| F4 | `governance/shared/backend/modules/REG/execution-state.json` → `gov-module.py delivery REG` | Process, not code: PORTS and SVC-API show `not run` although their code is present and their `api_doc_gaps` rows were recorded on 2026-10-01; no `package_results` row exists for either. Their acceptance is `tests-green` with 3 and 88 listed tests, which this run did not execute (user instruction). | INFO | NO — the orchestrator records them once their tests are run. |

Accepted deviation (not a finding — already recorded as the OPEN `api_doc_gaps` row of SVC-API): the three in-process refusals `ServiceConnectionNotActivatedException` (`REG-SERVE-CONNECTION-NOT-ACTIVATED`, plan-sanctioned namespace), `VersionNotFoundException` and `ConnectionNotFoundException` extend `RuntimeException` directly and carry no catalog code (ADR-REG-011 (2)); the interim load refusals with no code (`ConnectionActivation.MISSING_VALUE_REASON` / `MISSING_ENVIRONMENT_REASON`, `PackageValidator` "malformed:/missing:/invalid:" under RULE-REG-012, `ItemSavepoints` persistence failure → `REG-500`) are the same row. None of them reaches HTTP as anything but `REG-500`.

Observations (no row affected): (i) `ServiceDefinitionParser.parse` puts `YAMLException.getMessage()` into the `Malformed` detail → load report `REASON`; SnakeYAML marks include a source snippet, so a line of `service.yaml` (possibly SQL text) can appear in API-REG-003's `reason` — the (e) case of the OPEN gap; (ii) the API document's `serviceCode` path parameter `maxLength: 100` is not enforced in code — an over-long code answers 404, which is the only documented refusal, so this is consistent; (iii) a folder present but unreadable (`unreadableFile` set) declares no code, so `ServiceWithdrawal` withdraws its stored service in the same run that records RULE-REG-026 — the plan's "no folder → withdraw" wording does not distinguish this case; (iv) `ServiceWithdrawal.apply` and `ConnectionActivation.activate` iterate `findAll()` with per-row `findById` re-reads inside savepoints — prescribed by `ItemSavepoints`' clear-after-rollback rule, not a lazy-loop.

## Skill compliance — whole module

### gov-enforce-backend-contract (80)

| Layer | Checks | Passed | Failed | N/A | Status |
|---|---|---|---|---|---|
| Domain | 7 | 5 | 1 (A.0.5 — F3) | 1 (A.0.4: load reasons are recorded values, not thrown — ADR-REG-011) | ❌ LOW |
| Entity | 15 | 13 | 0 | 2 (A.1.9, A.1.11: no host identifier, no association) | ✅ |
| Repository | 9 | 7 | 0 | 2 (A.2.6, A.2.7) | ✅ — A.2.8: the one native statement (`LOCK TABLE … WAIT n`) admits no bind; `n` is an integer derived from `aias.registry.load-lock-timeout` and clamped |
| DTO | 10 | 8 | 0 | 2 (A.3.2, A.3.3: no request DTO) | ✅ — A.3.10 by Javadoc, springdoc absent from `pom.xml` |
| Port/Adapter | 10 | 5 | 0 | 5 (A.4.3, A.4.5, A.4.6, A.4.7, A.4.8: no query/model adapter, no `aias.check.*` — ADR-REG-019) | ✅ — A.4.4 analog: real-path containment before any open; A.4.9/A.4.10: `IOException` → `DirectoryStatus` / `unreadableFile` |
| Service | 17 | 14 | 0 | 3 (A.5.10, A.5.11, A.5.17) | ✅ — A.5.5: the file system is read inside the one load-run transaction the unit prescribes; A.5.14 with the accepted deviation above |
| Controller | 12 | 11 | 0 | 1 (A.6.7: no body) | ✅ — A.6.10 by constant + Javadoc, springdoc absent |
| **TOTAL** | **80** | **63** | **1** | **16** | **APPROVED with 1 LOW style violation (F3)** |

CU.1 PASS (one advice) · CU.2 PASS · CU.3 N/A (ADR-REG-019) · CU.4 N/A (no other module) · CU.5 PASS (file system and properties behind `PackageSource` / `ActivationSource`) · CU.6 PASS (one class for `aias.registry`). No automatic rejection trigger fires.

### gov-enforce-library-contract (42)

| Group | Checks | Passed | Failed | N/A |
|---|---|---|---|---|
| G Guardrails | 10 | 6 (G.2 separate `ApprovalApiRegistry`; G.3 read-only declarations enforced, no host write path; G.4 `SqlTextRules` bound-parameter check, integer-only native statement; G.5 real-path containment; G.6 unreadable file/directory → recorded rows; G.9 per-run tally, stateless adapters) | 0 | 4 (G.1, G.7, G.8, G.10: REG runs no query, no model, owns no check limit) |
| D Decision logic | 6 | 5 | 0 | 1 (D.4: results are values — ADR-REG-011) |
| M Module boundaries | 6 | 4 | 0 | 2 (M.3 single module; M.5 no package asks for an ArchUnit test, none present) |
| S Orchestration | 8 | 8 | 0 | 0 |
| P Persistence | 6 | 4 | 0 | 2 (P.3, P.4) |
| W Web adapters | 6 | 5 | 0 | 1 (W.3) |
| **TOTAL** | **42** | **32** | **0** | **10** |

Cross-cutting: error handling COMPLIANT (deviation recorded) · caching N/A (no cache annotation in REG). Verdict: APPROVED. Note under G.6/P.6: F2 (`refusedEntries()` uncalled) — recorded as LOW, not as a G failure, because the plan's TC-REG-017 prescribes no Load Result for a refused entry.

### gov-enforce-error-handling (24)

| Check | Rules | Passed | Failed | N/A |
|---|---|---|---|---|
| Exception types | 5 | 4 | 1 — E.1.1 accepted deviation: 3 in-process refusals extend `RuntimeException` without a catalog code (ADR-REG-011; OPEN gap row — never invented, as the skill requires) | 0 |
| Code ↔ status | 4 | 3 | 0 | 1 (E.2.4) |
| Code registration | 5 | 5 | 0 | 0 |
| Messages | 5 | 4 | 0 | 1 (E.4.5) |
| Handling patterns | 5 | 5 | 0 | 0 |
| **TOTAL** | **24** | **21** | **1 (accepted)** | **2** |

Codes used but missing from the error-catalog: none in HTTP scope (`REG-SERVE-CONNECTION-NOT-ACTIVATED` is the plan's in-process namespace, outside the catalog by design). Verdict: COMPLIANT, pending the factory's answer to the OPEN `api_doc_gaps` row.

## Validation

- `mvn -q -DskipTests compile` → exit 0 (before and after the Javadoc edits)
- `mvn -q -DskipTests test-compile` → exit 0 (before and after)
- no test written, no test run

```yaml name=self-check
findings: 4
clean: false
fixed: 1          # F0 — Javadoc traces, 5 files
unfixed_low: 3    # F1 QR-REG-002 projection · F2 refusedEntries() uncalled · F3 parser factory
process_info: 1   # F4 PORTS / SVC-API not recorded
accepted_deviations: 1   # in-process refusals without catalog code — OPEN api_doc_gaps row
binding_mismatches: 0
api_document_mismatches: 0
error_response_mismatches: 0
```
