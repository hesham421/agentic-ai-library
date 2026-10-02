# aias E2E simulation run 20261002T114403Z

Base URL `http://localhost:7271` · comparison model `gemini-3.6-flash` · model calls: 11 comparison, 1 reading

| Group | Passed | Failed | Skipped | Ambiguous | Error |
|---|---|---|---|---|---|
| registry | 4 | 0 | 0 | 0 | 0 |
| manual | 3 | 0 | 0 | 0 | 0 |
| image | 1 | 0 | 0 | 0 | 0 |
| path | 1 | 0 | 0 | 0 | 0 |
| blob | 1 | 0 | 0 | 0 | 0 |
| decisions | 2 | 0 | 0 | 0 | 0 |
| approval | 2 | 1 | 0 | 0 | 0 |
| refusals | 6 | 0 | 0 | 0 | 0 |
| lifecycle | 2 | 0 | 0 | 0 | 0 |
| interrupted | 1 | 0 | 0 | 0 | 0 |
| expiry | 1 | 0 | 0 | 0 | 0 |
| notpermitted | 2 | 0 | 0 | 0 | 0 |
| limits | 5 | 0 | 0 | 0 | 0 |
| withdrawn | 2 | 0 | 0 | 0 | 0 |
| connection | 3 | 0 | 0 | 0 | 0 |
| race | 0 | 0 | 0 | 1 | 0 |

## Scenarios

- **PASSED** [registry] The four fixture services are listed with their fetch modes, without SQL or connection (TC-REG-014, TC-REG-005, TC-REG-001)
- **PASSED** [registry] The load report shows both connections ACTIVATED and every fixture package loaded (TC-REG-008, TC-REG-051, TC-REG-006)
- **PASSED** [registry] An unknown service code is 404 REG-404-SERVICE-NOT-FOUND (TC-REG-016)
- **PASSED** [registry] A service code is read trimmed and case-insensitively (TC-REG-075, TC-REG-073)
- **PASSED** [manual] Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT with a full report (TC-INT-026, TC-INT-028, TC-INT-030, TC-INT-031, TC-INT-034, TC-INT-087, TC-INT-089, TC-INT-091, TC-INT-093, TC-CHK-075, TC-CHK-076, TC-CHK-027, TC-CHK-071, TC-CHK-073, TC-DOC-040, TC-DOC-042, TC-RPT-028, TC-RPT-033)
- **PASSED** [manual] Manual NOT_COMPLIANT: a failing transcript (GPA 2.10, 90 credits) (TC-CHK-024, TC-RPT-011)
- **PASSED** [manual] Missing document: confirm without an upload -> TRANSCRIPT MISSING, NOT_COMPLIANT (TC-CHK-009, TC-DOC-045, TC-INT-092, TC-RPT-014)
- **PASSED** [image] A PNG transcript (no text layer) is read by the document-reading model (TC-DOC-056, TC-DOC-060)
- **PASSED** [path] Path flow + traversal: TRANSCRIPT read under the storage root, ../outside.pdf refused unopened (TC-INT-025, TC-CHK-056, TC-DOC-005, TC-DOC-019, TC-DOC-034, TC-DOC-035, TC-CHK-010, TC-CHK-025, TC-DOC-012)
- **PASSED** [blob] Blob flow: the TRANSCRIPT BLOB is read over the jdbc connection local-jdbc (TC-DOC-037, TC-DOC-038, TC-CHK-059)
- **PASSED** [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) (TC-INT-014, TC-INT-035, TC-INT-036, TC-INT-003, TC-RPT-039, TC-RPT-041, TC-RPT-042, TC-RPT-047, TC-RPT-049)
- **PASSED** [decisions] Decisions refused on a non-completed and an unknown Check (TC-RPT-040, TC-RPT-045)
- **PASSED** [approval] Approval API: refusals before any call on an approval-enabled service (TC-INT-016, TC-INT-017)
- **FAILED** [approval] Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok -> executed, then 409 — stopped: check 115 ended within 200s (TC-INT-019, TC-INT-020, TC-INT-037, TC-INT-038, TC-INT-040, TC-INT-041, TC-INT-042, TC-INT-043, TC-INT-018, TC-INT-039)
  - FAIL check 115 ended within 200s: `{'checkId': 115, 'status': 'RUNNING', 'serviceCode': 'demo-approval', 'versionNumber': 1, 'fetchMode': 'manual', 'requestNumber': 'E2E/20261002T114403Z APR1', 'employeeId': 'E2E-EMP-0001', 'startedAt': '2026-10-02T15:47:53.971207+04:00', 'runningSince': '2026-10-02T15:47:54.059303+04:00', 'endedAt':`
- **PASSED** [approval] Approval API: a REJECTED decision never calls the Approval API (TC-INT-013)
- **PASSED** [refusals] Start refusals: unknown service, incomplete start, unreadable body, unsupported Content-Type (TC-CHK-003, TC-CHK-001, TC-INT-001)
- **PASSED** [refusals] Upload refusals and limits on a waiting manual Check (TC-INT-002, TC-DOC-013, TC-DOC-014, TC-INT-033, TC-DOC-027, TC-INT-009, TC-INT-008, TC-INT-100, TC-DOC-015, TC-INT-010)
- **PASSED** [refusals] Maximum uploads per Check: 20 accepted, the 21st refused (TC-INT-097, TC-DOC-071, TC-DOC-072)
- **PASSED** [refusals] Confirmation of an unknown Check; reads of unknown Checks (TC-CHK-038, TC-INT-088, TC-INT-094, TC-RPT-030)
- **PASSED** [refusals] Non-numeric identifiers are refused by each module's own code (TC-INT-005)
- **PASSED** [refusals] Lists of a request without their keys are refused (TC-RPT-035, TC-INT-090)
- **PASSED** [lifecycle] Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upload-window deadline (TC-CHK-083, TC-CHK-086)
- **PASSED** [lifecycle] After the Check ends: no Active Check, and its uploads are deleted (TC-CHK-085, TC-DOC-052, TC-CHK-028)
- **PASSED** [interrupted] A waiting Check is ended INTERRUPTED by a restart; its Active Check and uploads are gone (TC-CHK-037, TC-CHK-052)
- **PASSED** [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRED; Active Check and uploads gone (TC-CHK-040, TC-CHK-041, TC-CHK-083, TC-CHK-028, TC-DOC-052)
- **PASSED** [notpermitted] FREE comparison model on REAL data: a path Check ends FAILED / MODEL_NOT_PERMITTED, nothing sent (TC-CHK-046, TC-CHK-047)
- **PASSED** [notpermitted] FREE reading model on REAL data: a scanned (PNG) upload is never sent; the Check ends MODEL_NOT_PERMITTED (TC-DOC-064, TC-CHK-047)
- **PASSED** [limits] max-file-size 1 KB: a 4.5 KB path document is UNREADABLE / TOO_LARGE without being read (TC-DOC-025, TC-DOC-018)
- **PASSED** [limits] max-file-size 1 KB: a 2000-byte BLOB is UNREADABLE / TOO_LARGE, measured before any byte is read (TC-DOC-024, TC-DOC-018)
- **PASSED** [limits] REAL data, FREE reading model: the PNG is UNREADABLE / MODEL_NOT_PERMITTED, the PDF beside it is READ (TC-DOC-065, TC-DOC-064)
- **PASSED** [limits] max-rows 1: a query answering 2 rows is recorded unread ('more than 1 rows') -> NEEDS_MANUAL_REVIEW (TC-CHK-031, TC-CHK-032)
- **PASSED** [limits] Two simultaneous APPROVED decisions: one 201 (one Approval API call), one 409 after the lock (TC-INT-098)
- **PASSED** [withdrawn] A package folder moved out: the service is WITHDRAWN, unlisted, read available=false, start refused (TC-REG-010, TC-REG-088, TC-REG-012, TC-REG-027, TC-CHK-002, TC-INT-001)
- **PASSED** [withdrawn] The folder put back: the service is available again with its stored version (TC-REG-011, TC-REG-024)
- **PASSED** [connection] A package whose query names an unregistered connection is REJECTED at load (ghost-db) (TC-REG-032, TC-REG-007, TC-REG-051)
- **PASSED** [connection] The connection removed from the activation config: REMOVED; start -> 422 CHK-422-CONNECTION-NOT-ACTIVATED (TC-REG-053, TC-REG-055, TC-CHK-004)
- **PASSED** [connection] Back to the normal registry: demo-conn's folder parked again -> WITHDRAWN (TC-REG-010)
- **AMBIGUOUS** [race] DOC-409-CHECK-ENDED through INT's upload (the race of ADR-INT-025) — NOT-DETERMINISTIC, not attempted: DOC answers DOC-409-CHECK-ENDED only when INT's status read (RPT, AWAITING_DOCUMENTS) precedes the Check's ending AND DOC's Ended-Check marker is committed before DOC's handover step 1a. Every ending path (CheckEndingService, start-up recovery) commits the RPT status first and records DOC's marker after that commit, and the multipart body is parsed before UploadService reads the status (resolve-lazily, argument resolution), so nothing an HTTP client controls can hold a request between INT's read and DOC's check. Only a thread-level test (a test double pausing between the two) reproduces it — out of scope for this API-only runner (TC-INT-096)

## App restarts (restart groups)

- 2026-10-02T16:03:17 — mode [normal] (by 'A waiting Check is ended INTERRUPTED by a restart; its Active Check and uploads are gone')
- 2026-10-02T16:03:53 — mode [aias.check.upload-window=PT1M, aias.check.deadline-check-interval=PT5S] (by 'Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRED; Active Check and uploads gone')
- 2026-10-02T16:05:33 — mode [aias.documents.data-class=REAL] (by 'FREE comparison model on REAL data: a path Check ends FAILED / MODEL_NOT_PERMITTED, nothing sent')
- 2026-10-02T16:06:14 — mode [aias.check.max-file-size=1KB, aias.check.max-rows=1, aias.documents.data-class=REAL, aias.check.comparison-model.tier=APPROVED] (by 'max-file-size 1 KB: a 4.5 KB path document is UNREADABLE / TOO_LARGE without being read')
- 2026-10-02T16:08:03 — mode [normal], parked ['demo-blob'] (by 'A package folder moved out: the service is WITHDRAWN, unlisted, read available=false, start refused')
- 2026-10-02T16:08:38 — mode [normal] (by 'The folder put back: the service is available again with its stored version')
- 2026-10-02T16:09:14 — mode [connections=['local-oracle', 'local-jdbc', 'local-extra']], moved in ['demo-conn', 'demo-noconn'] (by 'A package whose query names an unregistered connection is REJECTED at load (ghost-db)')
- 2026-10-02T16:09:51 — mode [normal], moved in ['demo-conn'] (by 'The connection removed from the activation config: REMOVED; start -> 422 CHK-422-CONNECTION-NOT-ACTIVATED')
- 2026-10-02T16:10:26 — mode [normal] (by 'Back to the normal registry: demo-conn's folder parked again -> WITHDRAWN')

## Surviving records (synthetic, kept until the retention purge)

| checkId | service | requestNumber | what |
|---|---|---|---|
| 107 | demo-manual | `E2E-20261002T114403Z-PASS` | started by 'Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT with a full report' |
| 108 | demo-manual | `E2E-20261002T114403Z-FAIL` | started by 'Manual NOT_COMPLIANT: a failing transcript (GPA 2.10, 90 credits)' |
| 109 | demo-manual | `E2E-20261002T114403Z-MISSING` | started by 'Missing document: confirm without an upload -> TRANSCRIPT MISSING, NOT_COMPLIANT' |
| 110 | demo-approval | `E2E-20261002T114403Z-PNG` | started by 'A PNG transcript (no text layer) is read by the document-reading model' |
| 111 | demo-path | `E2E-20261002T114403Z-PATH` | started by 'Path flow + traversal: TRANSCRIPT read under the storage root, ../outside.pdf refused unopened' |
| 112 | demo-blob | `E2E-20261002T114403Z-BLOB` | started by 'Blob flow: the TRANSCRIPT BLOB is read over the jdbc connection local-jdbc' |
| 113 | demo-manual | `E2E-20261002T114403Z-DEC-WAIT-7f3950` | started by 'Decisions refused on a non-completed and an unknown Check' |
| 114 | demo-approval | `E2E-20261002T114403Z-APR-WAIT-b41e32` | started by 'Approval API: refusals before any call on an approval-enabled service' |
| 115 | demo-approval | `E2E/20261002T114403Z APR1` | started by 'Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok -> executed, then 409' |
| 116 | demo-manual | `E2E-20261002T114403Z-UPL-d823ae` | started by 'Upload refusals and limits on a waiting manual Check' |
| 117 | demo-manual | `E2E-20261002T114403Z-MAXUP-86acf6` | started by 'Maximum uploads per Check: 20 accepted, the 21st refused' |
| 118 | demo-manual | `E2E-20261002T114403Z-LIFE-4494ec` | started by 'Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upload-window deadline' |
| 119 | demo-manual | `E2E-20261002T114403Z-INTR-012f8b` | started by 'A waiting Check is ended INTERRUPTED by a restart; its Active Check and uploads are gone' |
| 120 | demo-manual | `E2E-20261002T114403Z-EXP-9aa3c7` | started by 'Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRED; Active Check and uploads gone' |
| 121 | demo-path | `E2E-20261002T114403Z-NOPERM-PATH` | started by 'FREE comparison model on REAL data: a path Check ends FAILED / MODEL_NOT_PERMITTED, nothing sent' |
| 122 | demo-manual | `E2E-20261002T114403Z-NOPERM-PNG` | started by 'FREE reading model on REAL data: a scanned (PNG) upload is never sent; the Check ends MODEL_NOT_PERMITTED' |
| 123 | demo-path-big | `E2E-20261002T114403Z-BIG-PATH` | started by 'max-file-size 1 KB: a 4.5 KB path document is UNREADABLE / TOO_LARGE without being read' |
| 124 | demo-blob-big | `E2E-20261002T114403Z-BIG-BLOB` | started by 'max-file-size 1 KB: a 2000-byte BLOB is UNREADABLE / TOO_LARGE, measured before any byte is read' |
| 125 | demo-approval | `E2E-20261002T114403Z-LIMITS-APR` | started by 'REAL data, FREE reading model: the PNG is UNREADABLE / MODEL_NOT_PERMITTED, the PDF beside it is READ' |
| 126 | demo-rows | `E2E-20261002T114403Z-ROWS` | started by 'max-rows 1: a query answering 2 rows is recorded unread ('more than 1 rows') -> NEEDS_MANUAL_REVIEW' |
| 127 | demo-conn | `E2E-20261002T114403Z-CONN-OK` | started by 'A package whose query names an unregistered connection is REJECTED at load (ghost-db)' |
