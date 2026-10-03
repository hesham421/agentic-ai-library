# aias E2E simulation run 20261003T102414Z

Base URL `http://localhost:7271` · comparison model `gemini-3.6-flash` · model calls: 0 comparison, 0 reading

| Group | Passed | Failed | Skipped | Ambiguous | Not exercisable | Error |
|---|---|---|---|---|---|---|
| registry | 4 | 0 | 0 | 0 | 0 | 0 |
| reg-rules | 33 | 0 | 0 | 0 | 0 | 0 |
| reg-activation | 7 | 0 | 0 | 0 | 0 | 0 |
| reg-versions | 10 | 0 | 0 | 0 | 0 | 0 |
| reg-inprocess | 3 | 0 | 0 | 0 | 12 | 0 |

## Scenarios

- **PASSED** [registry] The four fixture services are listed with their fetch modes, without SQL or connection (TC-REG-014, TC-REG-005, TC-REG-001)
- **PASSED** [registry] The load report shows both connections ACTIVATED and every fixture package loaded (TC-REG-008, TC-REG-051, TC-REG-006)
- **PASSED** [registry] An unknown service code is 404 REG-404-SERVICE-NOT-FOUND (TC-REG-016)
- **PASSED** [registry] A service code is read trimmed and case-insensitively (TC-REG-075, TC-REG-073)
- **PASSED** [reg-rules] Folder with only a service definition is rejected (TC-REG-003)
- **PASSED** [reg-rules] Empty service knowledge is rejected (TC-REG-029)
- **PASSED** [reg-rules] A query using another parameter than the declared input is rejected (TC-REG-033)
- **PASSED** [reg-rules] A query using a substitution marker is rejected (TC-REG-034)
- **PASSED** [reg-rules] A query that is not a single SELECT is rejected (TC-REG-035)
- **PASSED** [reg-rules] A service definition declaring a check limit is rejected (TC-REG-036)
- **PASSED** [reg-rules] Two queries with one name are rejected (TC-REG-037)
- **PASSED** [reg-rules] An unknown fetch mode is rejected (TC-REG-040)
- **PASSED** [reg-rules] Fetch mode path without a path column is rejected (TC-REG-041)
- **PASSED** [reg-rules] Blob documents over an mcp connection are rejected (TC-REG-042)
- **PASSED** [reg-rules] A service definition declaring a file location is rejected (TC-REG-043)
- **PASSED** [reg-rules] Enabled approval without a definition is rejected (TC-REG-045)
- **PASSED** [reg-rules] A required document type declared twice is rejected (TC-REG-072)
- **PASSED** [reg-rules] Fetch mode path without a type column is rejected (TC-REG-083)
- **PASSED** [reg-rules] A document source naming an undeclared query is rejected (TC-REG-084)
- **PASSED** [reg-rules] A timeout in a service definition is rejected (TC-REG-086)
- **PASSED** [reg-rules] A maximum file size in a service definition is rejected (TC-REG-087)
- **PASSED** [reg-rules] An invalid service code is rejected (sub-cases a-g, one folder each) (TC-REG-076)
- **PASSED** [reg-rules] An over-length invalid service code is shortened on its Load Result row (TC-REG-097)
- **PASSED** [reg-rules] Two folders whose codes differ only in case are both rejected (TC-REG-074)
- **PASSED** [reg-rules] A failing pilot package is reported and never supplied (TC-REG-063)
- **PASSED** [reg-rules] A folder outside the package directory is never loaded (sibling dir, symlink inside) (TC-REG-017)
- **PASSED** [reg-rules] Activation refusals: unknown type, over-length name and endpoint; main-db ACTIVATED (TC-REG-054, TC-REG-094, TC-REG-095)
- **PASSED** [reg-rules] Failing and succeeding items in one load run each get their own outcome (TC-REG-085)
- **PASSED** [reg-rules] Earlier load results are removed at a new run (TC-REG-009)
- **PASSED** [reg-rules] One connection defined once and shared by two packages (TC-REG-048)
- **PASSED** [reg-rules] One fetch mode and the required document types recorded per version (TC-REG-038, TC-REG-039)
- **PASSED** [reg-rules] A folder whose name exceeds 200 characters is rejected; the run continues (TC-REG-093)
- **PASSED** [reg-rules] A 100-character service code with single hyphens is accepted (boundary pass) (TC-REG-099)
- **PASSED** [reg-rules] Fetch mode blob without a content column is rejected (TC-REG-082)
- **PASSED** [reg-rules] A package folder holding another file is rejected (TC-REG-064)
- **PASSED** [reg-rules] A valid folder is rejected as a duplicate even when its twin fails another rule (TC-REG-098)
- **PASSED** [reg-rules] Two instances starting together produce one complete load report (TC-REG-079)
- **PASSED** [reg-activation] Changed connection settings update the connection, not the versions (TC-REG-052)
- **PASSED** [reg-activation] An empty package directory withdraws nothing while services are available (TC-REG-078)
- **PASSED** [reg-activation] The limited-to-views declaration is recorded (TC-REG-058)
- **PASSED** [reg-activation] An over-length package directory path is shortened on its Load Result row (TC-REG-096)
- **PASSED** [reg-activation] A missing package directory withdraws nothing (TC-REG-077)
- **PASSED** [reg-activation] A connection name listed twice is refused (TC-REG-049)
- **PASSED** [reg-activation] A connection not declared read-only is refused (TC-REG-057)
- **PASSED** [reg-versions] Every version carries its version number (TC-REG-020)
- **PASSED** [reg-versions] A lower, unstored version is rejected (TC-REG-023)
- **PASSED** [reg-versions] The current version is supplied to a Check (TC-REG-025)
- **PASSED** [reg-versions] A present package folder with an unreadable file is rejected and the run continues (TC-REG-090)
- **PASSED** [reg-versions] A re-activation that turns a blob version's connection into mcp is refused (TC-REG-091)
- **PASSED** [reg-versions] Same version number with changed content is rejected (never edited in place) (TC-REG-022)
- **PASSED** [reg-versions] A higher version becomes current; the earlier stays stored (TC-REG-021)
- **PASSED** [reg-versions] Two folders declaring one service code are both rejected (TC-REG-004)
- **PASSED** [reg-versions] A package file that changes while it is read is rejected (TC-REG-081)
- **PASSED** [reg-versions] A removed connection is not re-registered as mcp while a blob version reads through it (TC-REG-092)
- **PASSED** [reg-inprocess] No package is supplied for an unknown service (TC-REG-013)
- **PASSED** [reg-inprocess] The registry exposes read operations only (TC-REG-018)
- **PASSED** [reg-inprocess] An instance that cannot take the load lock serves the stored registry (TC-REG-080)
- **NOT-EXERCISABLE** [reg-inprocess] Version stored as two separate parts — serviceKnowledge and serviceDefinition of a stored version are returned only in-process (getServicePackageVersion); no HTTP operation returns either text (API-REG-002 is a summary), and the only consumer (CHK's comparison prompt) needs a model call (TC-REG-002)
- **NOT-EXERCISABLE** [reg-inprocess] Any stored version resolves — API-INT-008 (the only public read of a pinned version) exposes versionNumber and requiredDocumentTypes only; serviceKnowledge, serviceDefinition and queries of version 2 are in-process only. Partial evidence asserted green in this scenario (TC-REG-026)
- **NOT-EXERCISABLE** [reg-inprocess] Only the queries of the service definition are supplied, unaltered — the supplied queries (sqlText, connectionName) are read in-process by CHK's pipeline, which runs only for a confirmed Check and ends in a comparison-model call; no API returns them (TC-REG-030)
- **NOT-EXERCISABLE** [reg-inprocess] An enabled approval API definition is recorded — the approvalApi text reaches only the Employee Decision path (ApprovalApiRegistry), exercised by an APPROVED decision on a COMPLETED Check — a comparison-model call; API-REG-002 exposes approvalEnabled only (TC-REG-044)
- **NOT-EXERCISABLE** [reg-inprocess] The approval API definition reaches only the Employee Decision path — the supplied package and getApprovalApi are in-process values; the Employee Decision path needs a COMPLETED Check (a comparison-model call) (TC-REG-046)
- **NOT-EXERCISABLE** [reg-inprocess] Disabled approval is reported to the Employee Decision path — getApprovalApi is reached only by a decision on a COMPLETED Check (a comparison-model call) (TC-REG-047)
- **NOT-EXERCISABLE** [reg-inprocess] A connection is supplied by name — getConnection's settings (type, endpoint, queryTool, dialect, credentialReference) are in-process only; no HTTP operation reads a Connection (TC-REG-050)
- **NOT-EXERCISABLE** [reg-inprocess] Only the credential reference is stored — needs a read of every stored REG_CONNECTION field; the runner works only through the HTTP API and no API returns a Connection (TC-REG-056)
- **NOT-EXERCISABLE** [reg-inprocess] The scholarship-request pilot package loads — MISSING_IMPLEMENTATION: the delivered pilot package folder scholarship-request (SVC-API step 6, REQ-REG-057/058) is not in this repository; authoring it needs the host's real policy text (POL-REG-014) and host query/columns, which must not be invented (TC-REG-059)
- **NOT-EXERCISABLE** [reg-inprocess] No request data is stored in the registry — needs a read of every stored field of the REG tables after 100 Checks; the runner works only through the HTTP API (no direct DB access) (TC-REG-061)
- **NOT-EXERCISABLE** [reg-inprocess] Supplied package content is read-only — immutability of the in-process value objects handed to a Check; nothing an HTTP client sends can attempt to change a supplied query (TC-REG-062)
- **NOT-EXERCISABLE** [reg-inprocess] A Check's pinned version is supplied after a newer version became current — fetchMode and the document-source fields of a pinned version are read only in-process by DOC's fetch inside a RUNNING Check (a comparison-model call), and start 2 (a restart) ends that Check INTERRUPTED before any fetch; API-INT-008 exposes versionNumber + requiredDocumentTypes only. Partial evidence asserted green in this scenario (TC-REG-089)

## App restarts (restart groups)

- 2026-10-03T14:24:21 — mode [aias.registry.package-directory=local/e2e-reg/rules-1, aias.documents.data-class=REAL, connections=['main-db', 'ftp-db', 'ccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc', 'blob-db']] (by 'Folder with only a service definition is rejected')
- 2026-10-03T14:24:57 — mode [aias.registry.package-directory=local/e2e-reg/rules-085, aias.documents.data-class=REAL, connections=['main-db', 'bad-db']] (by 'Failing and succeeding items in one load run each get their own outcome')
- 2026-10-03T14:25:33 — mode [aias.registry.package-directory=local/e2e-reg/rules-009, aias.documents.data-class=REAL, connections=['main-db']] (by 'Earlier load results are removed at a new run')
- 2026-10-03T14:26:10 — mode [aias.registry.package-directory=local/e2e-reg/rules-2b, aias.documents.data-class=REAL, connections=['main-db', 'docs-jdbc']] (by 'One connection defined once and shared by two packages')
- 2026-10-03T14:26:47 — mode [aias.registry.package-directory=local/e2e-reg/rules-2a, aias.documents.data-class=REAL, connections=['main-db']] (by 'A package folder holding another file is rejected')
- 2026-10-03T14:27:24 — two instances (7271 + 7272), dir local/e2e-reg/rules-079 (by 'Two instances starting together produce one complete load report')
- 2026-10-03T14:28:01 — mode [aias.registry.package-directory=local/e2e-reg/act-two, aias.documents.data-class=REAL, connections=['main-db']] (by 'Changed connection settings update the connection, not the versions')
- 2026-10-03T14:28:37 — mode [aias.registry.package-directory=local/e2e-reg/act-two, aias.documents.data-class=REAL, connections=['main-db']] (by 'Changed connection settings update the connection, not the versions')
- 2026-10-03T14:29:13 — mode [aias.registry.package-directory=local/e2e-reg/act-empty, aias.documents.data-class=REAL, connections=['main-db']] (by 'Changed connection settings update the connection, not the versions')
- 2026-10-03T14:29:48 — mode [aias.registry.package-directory=/srv/ddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd, aias.documents.data-class=REAL, connections=['main-db']] (by 'The limited-to-views declaration is recorded')
- 2026-10-03T14:30:24 — mode [aias.registry.package-directory=/srv/aias/packages, aias.documents.data-class=REAL, connections=['aux-db']] (by 'A missing package directory withdraws nothing')
- 2026-10-03T14:30:59 — mode [aias.registry.package-directory=/srv/aias/packages, aias.documents.data-class=REAL, connections=['main-db', 'main-db']] (by 'A connection name listed twice is refused')
- 2026-10-03T14:31:35 — mode [aias.registry.package-directory=/srv/aias/packages, aias.documents.data-class=REAL, connections=['main-db']] (by 'A connection not declared read-only is refused')
- 2026-10-03T14:32:11 — mode [aias.registry.package-directory=local/e2e-reg/versions-1, aias.documents.data-class=REAL, connections=['main-db', 'main-db-t1003102414']] (by 'Every version carries its version number')
- 2026-10-03T14:32:46 — mode [aias.registry.package-directory=local/e2e-reg/versions-2, aias.documents.data-class=REAL, connections=['main-db', 'main-db-t1003102414']] (by 'A lower, unstored version is rejected')
- 2026-10-03T14:33:22 — mode [aias.registry.package-directory=local/e2e-reg/versions-3, aias.documents.data-class=REAL, connections=['main-db', 'main-db-t1003102414']] (by 'A re-activation that turns a blob version's connection into mcp is refused')
- 2026-10-03T14:34:00 — mode [aias.registry.package-directory=local/e2e-reg/versions-4, aias.documents.data-class=REAL, connections=['main-db']] (by 'A package file that changes while it is read is rejected')
- 2026-10-03T14:34:35 — mode [aias.registry.package-directory=local/e2e-reg/versions-5, aias.documents.data-class=REAL, connections=['main-db', 'main-db-t1003102414']] (by 'A removed connection is not re-registered as mcp while a blob version reads through it')
- 2026-10-03T14:35:42 — mode [aias.registry.package-directory=local/e2e-reg/inprocess-080, aias.documents.data-class=REAL, aias.registry.load-lock-timeout=PT30S, connections=['main-db']] (by 'An instance that cannot take the load lock serves the stored registry')
- 2026-10-03T14:37:12 — mode [normal] (by '(end of run)')

## Surviving records (synthetic, kept until the retention purge)

| checkId | service | requestNumber | what |
|---|---|---|---|
| 210 | request-versions-t1003102414 | `E2E-20261003T102414Z-REG-V2` | started by 'Every version carries its version number' |
| 211 | request-versions-t1003102414 | `E2E-20261003T102414Z-REG-V3` | started by 'A lower, unstored version is rejected' |
| 212 | scholarship-request-t1003102414 | `E2E-20261003T102414Z-REG-LOCK` | started by 'An instance that cannot take the load lock serves the stored registry' |
