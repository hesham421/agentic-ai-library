# aias E2E simulation run 20261003T115316Z

Base URL `http://localhost:7271` · comparison model `gemini-3.6-flash` · model calls: 1 comparison, 0 reading

| Group | Passed | Failed | Skipped | Ambiguous | Not exercisable | Error |
|---|---|---|---|---|---|---|
| registry | 4 | 0 | 0 | 0 | 0 | 0 |
| decisions | 2 | 0 | 0 | 0 | 0 | 0 |
| refusals | 6 | 0 | 0 | 0 | 0 | 0 |
| rpt-store | 18 | 1 | 0 | 0 | 0 | 0 |
| int-flow | 9 | 0 | 0 | 0 | 1 | 0 |
| rpt-purge | 5 | 1 | 0 | 0 | 0 | 0 |
| rpt-inprocess | 0 | 0 | 0 | 0 | 20 | 0 |
| int-inprocess | 0 | 0 | 0 | 0 | 1 | 0 |

## Scenarios

- **PASSED** [registry] The four fixture services are listed with their fetch modes, without SQL or connection (TC-REG-014, TC-REG-005, TC-REG-001)
- **PASSED** [registry] The load report shows both connections ACTIVATED and every fixture package loaded (TC-REG-008, TC-REG-051, TC-REG-006)
- **PASSED** [registry] An unknown service code is 404 REG-404-SERVICE-NOT-FOUND (TC-REG-016)
- **PASSED** [registry] A service code is read trimmed and case-insensitively (TC-REG-075, TC-REG-073)
- **PASSED** [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) (TC-INT-014, TC-INT-035, TC-INT-036, TC-INT-003, TC-RPT-039, TC-RPT-041, TC-RPT-042, TC-RPT-047, TC-RPT-049)
- **PASSED** [decisions] Decisions refused on a non-completed and an unknown Check (TC-RPT-040, TC-RPT-045)
- **PASSED** [refusals] Start refusals: unknown service, incomplete start, unreadable body, unsupported Content-Type (TC-CHK-003, TC-CHK-001, TC-INT-001)
- **PASSED** [refusals] Upload refusals and limits on a waiting manual Check (TC-INT-002, TC-DOC-013, TC-DOC-014, TC-INT-033, TC-DOC-027, TC-INT-009, TC-INT-008, TC-INT-100, TC-DOC-015, TC-INT-010)
- **PASSED** [refusals] Maximum uploads per Check: 20 accepted, the 21st refused (TC-INT-097, TC-DOC-071, TC-DOC-072)
- **PASSED** [refusals] Confirmation of an unknown Check; reads of unknown Checks (TC-CHK-038, TC-INT-088, TC-INT-094, TC-RPT-030)
- **PASSED** [refusals] Non-numeric identifiers are refused by each module's own code (TC-INT-005)
- **PASSED** [refusals] Lists of a request without their keys are refused (TC-RPT-035, TC-INT-090)
- **FAILED** [rpt-store] No retention period configured: the scheduled purge is skipped and logged, nothing deleted (TC-RPT-052)
  - FAIL an ended Check run older than 1 day exists (precondition): `[]`
- **PASSED** [rpt-store] Version 2 of SVC-A: a NEEDS_MANUAL_REVIEW Check decided APPROVED (TC-RPT-047 precondition)
- **PASSED** [rpt-store] No unfinished Check at a start: the unfinished-Check list is empty (start-up recovery: 0) (TC-RPT-026)
- **PASSED** [rpt-store] Path Check of version 3 created RUNNING and read back; the start answers before the report (40 s pipeline) (TC-RPT-001, TC-INT-027)
- **PASSED** [rpt-store] Host identifiers kept exactly as received (slash, leading/trailing spaces, case) (TC-RPT-002)
- **PASSED** [rpt-store] Confirmation marks the Check RUNNING with its running time; it then completes COMPLIANT (TC-RPT-006)
- **PASSED** [rpt-store] Employee Decision APPROVED by E-3307 recorded; Overall Status and findings unchanged (TC-RPT-038)
- **PASSED** [rpt-store] Report contents: finding with its evidence and note, stored text returned as data, order kept (TC-RPT-029, TC-RPT-032, TC-RPT-013)
- **PASSED** [rpt-store] Every Check its own record: a new Check of request 1001 beside a COMPLETED, APPROVED one (TC-RPT-036)
- **PASSED** [rpt-store] Decision agreement counted per version (4/1/2 on version 3, 1 on version 2, undecided not counted) (TC-RPT-047)
- **PASSED** [rpt-store] Unread service query request_details ('more than 500 rows') kept; NEEDS_MANUAL_REVIEW (TC-RPT-015)
- **PASSED** [rpt-store] No document content kept: a READ document entry and the RPT_CHECK_DOCUMENT columns (TC-RPT-022)
- **PASSED** [rpt-store] No file opened: a stored detail naming a path is returned as text (the path is a FIFO) (TC-RPT-056)
- **PASSED** [rpt-store] Listing capped at 100 with the total: 130 Checks of request 1002, 101 of request 1003 (TC-RPT-034, TC-RPT-037)
- **PASSED** [rpt-store] Read filters bound as parameters: requestNumber 1001' OR '1'='1 lists nothing (TC-RPT-058)
- **PASSED** [rpt-store] Failed Check read with its reason: the provider answers 503 -> MODEL_UNAVAILABLE (TC-RPT-031)
- **PASSED** [rpt-store] A RUNNING Check past its timeout is stored FAILED / TIMED_OUT with its detail and end time (TC-RPT-021)
- **PASSED** [rpt-store] A Check awaiting documents ends FAILED / UPLOAD_WINDOW_EXPIRED, never RUNNING (TC-RPT-063)
- **PASSED** [rpt-store] Unfinished Checks listed oldest first: a restart ends AWAITING (older) then RUNNING (newer); a COMPLETED one is untouched (TC-RPT-025)
- **PASSED** [int-flow] An employee unknown to any directory is accepted; no server-rendered report page (TC-INT-029, TC-INT-022)
- **PASSED** [int-flow] Uploads alone never continue the Check: TRANSCRIPT + ID_CARD uploaded, no confirmation (TC-INT-011)
- **PASSED** [int-flow] Neither an upload nor a confirmation records a decision (TC-INT-012)
- **PASSED** [int-flow] The uploaded-documents read relays Document Access's listing unchanged (300 KB + 60 MB) (TC-INT-095)
- **PASSED** [int-flow] APPROVED through the Approval API: recorded as executed; the only host connection is that call (TC-RPT-043, TC-INT-024)
- **PASSED** [int-flow] A refusal after an executed approval: REJECTED recorded while the Approval API holds; the APPROVED answers 409 and is logged (TC-INT-021)
- **NOT-EXERCISABLE** [int-flow] The decision path answers in the standard form when the approval definition cannot be read — REG never deletes a stored version and every Check is pinned to one, so the approval definition read (CON-REG-012) never answers not-found. Partial evidence (an unusable stored definition 'approve-it' -> the same INT-500 path) asserted green in this scenario (TC-INT-044)
- **PASSED** [int-flow] Host Integration keeps nothing after answering (tables, multipart storage, live heap) (TC-INT-023)
- **PASSED** [int-flow] An unexpected failure (the database paused) is answered INT-500 without internals (TC-INT-006)
- **PASSED** [int-flow] A COMPLETED compliant Check of an approval-enabled version, left undecided: no Approval API call; the Report Store never approves (TC-INT-015, TC-RPT-044)
- **PASSED** [rpt-purge] Purge with retention 1 day: an expired Check run is deleted with all its records (404) (TC-RPT-051)
- **PASSED** [rpt-purge] A Check run inside the retention period is kept by the purge (TC-RPT-050)
- **PASSED** [rpt-purge] Purge outcome logged: 'Report purge deleted N Check runs ended before <cut-off>' (TC-RPT-054)
- **PASSED** [rpt-purge] A failing deletion keeps that Check run whole; the others are deleted and counted (TC-RPT-060)
- **FAILED** [rpt-purge] Purge failure logged at WARN with its Check run and cause, before the closing line (TC-RPT-064)
  - FAIL the cause is the database's message (the lock wait was cancelled): `-10-03T16:09:15.096+04:00  WARN 28650 --- [agentic-ai-library] [   scheduling-1] i.a.rpt.service.ReportPurgeService       : Report purge kept Check run 124: its deletion failed (Connection is closed).`
- **PASSED** [rpt-purge] No access to host data: RPT reads, a decision and purge runs send 0 host queries (TC-RPT-055)
- **NOT-EXERCISABLE** [rpt-inprocess] Incomplete Check run refused — CHK refuses a start without employeeId (400 CHK-400-START-INCOMPLETE, refusals group) before it calls createCheckRun; no public path hands RPT a blank value (TC-RPT-003)
- **NOT-EXERCISABLE** [rpt-inprocess] AWAITING_DOCUMENTS with fetch mode path refused — CHK derives the initial status from the version's fetch mode (path -> RUNNING); step 2 is a direct INSERT into the service schema (TC-RPT-004)
- **NOT-EXERCISABLE** [rpt-inprocess] Manual Check starting RUNNING refused — CHK always starts a manual Check AWAITING_DOCUMENTS (TC-RPT-005)
- **NOT-EXERCISABLE** [rpt-inprocess] Mark RUNNING again keeps the first running time — CHK calls markRunning once per Check (at the confirmation); a second confirmation is refused by CHK (409 CHK-409) before RPT (TC-RPT-007)
- **NOT-EXERCISABLE** [rpt-inprocess] Ended Check cannot be marked RUNNING — a confirmation of an ended Check is refused by CHK (409 CHK-409-CHECK-NOT-AWAITING-DOCUMENTS) before markRunning (TC-RPT-008)
- **NOT-EXERCISABLE** [rpt-inprocess] Check not RUNNING cannot be completed — CHK completes only the Check its pipeline runs (TC-RPT-009)
- **NOT-EXERCISABLE** [rpt-inprocess] Unknown Check on the result port — CHK fails only Checks it created; no public path names Check 999 (TC-RPT-010)
- **NOT-EXERCISABLE** [rpt-inprocess] Report stored whole or not at all — the outcome `PASSED` cannot reach RPT: CHK's structured output admits SATISFIED / NOT_SATISFIED / UNDETERMINED only (MODEL_OUTPUT_INVALID before RPT) (TC-RPT-012)
- **NOT-EXERCISABLE** [rpt-inprocess] Database failure while storing a report leaves nothing stored — needs a fault injected on the INSERT of the second Finding — not producible through the API (TC-RPT-062)
- **NOT-EXERCISABLE** [rpt-inprocess] Metadata disagreeing with the Check run refused — CHK builds the metadata from the stored run (TC-RPT-016)
- **NOT-EXERCISABLE** [rpt-inprocess] COMPLIANT refused unless every finding is SATISFIED — CHK's Overall Status rule never hands COMPLIANT with a NOT_SATISFIED finding (a scripted NOT_SATISFIED gives NOT_COMPLIANT, TC-RPT-029) (TC-RPT-017)
- **NOT-EXERCISABLE** [rpt-inprocess] Code outside its closed list refused — CHK passes only its own closed codes; steps 2-3 are direct writes to the service schema (TC-RPT-018)
- **NOT-EXERCISABLE** [rpt-inprocess] Finding without evidence refused — CHK records a finding without evidence as UNDETERMINED with evidence 'NONE' (REQ-CHK-040) before RPT (TC-RPT-019)
- **NOT-EXERCISABLE** [rpt-inprocess] UNREADABLE document without a reason refused — DOC always gives an UNREADABLE outcome its reason; step 2 is a direct INSERT (TC-RPT-020)
- **NOT-EXERCISABLE** [rpt-inprocess] Ended report never changes — CHK completes a Check once; no public path calls completeCheck again (TC-RPT-023)
- **NOT-EXERCISABLE** [rpt-inprocess] Incomplete failure refused — CHK always gives a failure its detail (TC-RPT-059)
- **NOT-EXERCISABLE** [rpt-inprocess] One Check read for the Check Engine — getCheck's result is handed to CHK in-process only; API-RPT-001 is a different operation (its fields are asserted by TC-RPT-001) (TC-RPT-024)
- **NOT-EXERCISABLE** [rpt-inprocess] Approval API flag on a rejection refused — INT hands approvalApiExecuted=false for every REJECTED decision (TC-INT-013); step 2 is a direct UPDATE (TC-RPT-046)
- **NOT-EXERCISABLE** [rpt-inprocess] Unfinished Check never purged — needs a Check unfinished for longer than the retention period: the shortest period is 1 whole day and every restart (a purge-mode change) ends unfinished Checks INTERRUPTED; the rows cannot be aged without direct DB writes (TC-RPT-053)
- **NOT-EXERCISABLE** [rpt-inprocess] Hand-over with an undeclared field cannot reach the store — structural (reflection + architecture rule over the value types) — an in-process test the no-JUnit policy excludes; no API reaches it (TC-RPT-061)
- **NOT-EXERCISABLE** [int-inprocess] The upload carries the Check's own service version — making version 2 current needs a load run, which happens only at an instance start, and every start ends all unfinished Checks INTERRUPTED (REQ-CHK-055, a global recovery): no Check can await uploads on version 1 while version 2 is current (TC-INT-032)

## App restarts (restart groups)

- 2026-10-03T15:53:42 — mode [spring.ai.openai.base-url=http://127.0.0.1:7293/v1beta/openai, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, aias.registry.package-directory=local/e2e-rpt/packages-v2, aias.reports.purge-schedule=*/10 * * * * *, connections=['local-oracle', 'local-jdbc']] (by 'No retention period configured: the scheduled purge is skipped and logged, nothing deleted')
- 2026-10-03T15:54:27 — mode [spring.ai.openai.base-url=http://127.0.0.1:7293/v1beta/openai, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, aias.registry.package-directory=local/e2e-rpt/packages-v3, aias.documents.storage-root=local/e2e-rpt/root, aias.check.timeout=PT60S, aias.check.deadline-check-interval=PT5S, aias.check.max-rows=500, aias.check.upload-window=PT2M, connections=['local-oracle', 'local-jdbc']] (by 'No unfinished Check at a start: the unfinished-Check list is empty (start-up recovery: 0)')
- 2026-10-03T15:56:49 — mode [spring.ai.openai.base-url=http://127.0.0.1:7293/v1beta/openai, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, aias.registry.package-directory=local/e2e-int/packages, aias.integration.approval.timeout=PT10S, aias.integration.upload.request-limit=70MB, spring.datasource.hikari.connection-timeout=2500, spring.datasource.hikari.validation-timeout=1000, connections=['local-oracle', 'local-jdbc']] (by 'Unfinished Checks listed oldest first: a restart ends AWAITING (older) then RUNNING (newer); a COMPLETED one is untouched')
- 2026-10-03T16:09:09 — mode [spring.ai.openai.base-url=http://127.0.0.1:7293/v1beta/openai, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, aias.registry.package-directory=local/e2e-rpt/packages-v3, aias.documents.storage-root=local/e2e-rpt/root, aias.check.timeout=PT60S, aias.check.deadline-check-interval=PT5S, aias.check.max-rows=500, aias.reports.retention-days=1, aias.reports.purge-schedule=*/10 * * * * *, spring.datasource.hikari.data-source-properties[oracle.jdbc.ReadTimeout]=5000, connections=['local-oracle', 'local-jdbc']] (by 'Purge with retention 1 day: an expired Check run is deleted with all its records (404)')
- 2026-10-03T16:11:59 — mode [normal] (by '(end of run)')

## Surviving records (synthetic, kept until the retention purge)

| checkId | service | requestNumber | what |
|---|---|---|---|
| 510 | demo-manual | `E2E-20261003T115316Z-PASS` | started by 'Decisions on a COMPLETED demo-manual Check (no Approval API)' |
| 511 | demo-manual | `E2E-20261003T115316Z-DEC-WAIT-ed3202` | started by 'Decisions refused on a non-completed and an unknown Check' |
| 512 | demo-manual | `E2E-20261003T115316Z-UPL-67495b` | started by 'Upload refusals and limits on a waiting manual Check' |
| 513 | demo-manual | `E2E-20261003T115316Z-MAXUP-2daf36` | started by 'Maximum uploads per Check: 20 accepted, the 21st refused' |
| 514 | rpt-a-t1003115316 | `E2E-20261003T115316Z-V2NMR` | started by 'Version 2 of SVC-A: a NEEDS_MANUAL_REVIEW Check decided APPROVED (TC-RPT-047 precondition)' |
| 515 | rpt-b-t1003115316 | `E2E-20261003T115316Z-EXP63` | started by 'No unfinished Check at a start: the unfinished-Check list is empty (start-up recovery: 0)' |
| 516 | rpt-p-t1003115316 | `1001` | started by 'Path Check of version 3 created RUNNING and read back; the start answers before the report (40 s pipeline)' |
| 517 | rpt-b-t1003115316 | `00-1001/A` | started by 'Host identifiers kept exactly as received (slash, leading/trailing spaces, case)' |
| 518 | rpt-a-t1003115316 | `E2E-20261003T115316Z-A1` | started by 'Confirmation marks the Check RUNNING with its running time; it then completes COMPLIANT' |
| 519 | rpt-a-t1003115316 | `E2E-20261003T115316Z-A6` | started by 'Report contents: finding with its evidence and note, stored text returned as data, order kept' |
| 520 | rpt-a-t1003115316 | `1001` | started by 'Every Check its own record: a new Check of request 1001 beside a COMPLETED, APPROVED one' |
| 521 | rpt-a-t1003115316 | `1001` | started by 'Every Check its own record: a new Check of request 1001 beside a COMPLETED, APPROVED one' |
| 522 | rpt-a-t1003115316 | `E2E-20261003T115316Z-A3` | started by 'Decision agreement counted per version (4/1/2 on version 3, 1 on version 2, undecided not counted)' |
| 523 | rpt-a-t1003115316 | `E2E-20261003T115316Z-A4` | started by 'Decision agreement counted per version (4/1/2 on version 3, 1 on version 2, undecided not counted)' |
| 524 | rpt-a-t1003115316 | `E2E-20261003T115316Z-A5` | started by 'Decision agreement counted per version (4/1/2 on version 3, 1 on version 2, undecided not counted)' |
| 525 | rpt-a-t1003115316 | `E2E-20261003T115316Z-A7` | started by 'Decision agreement counted per version (4/1/2 on version 3, 1 on version 2, undecided not counted)' |
| 526 | rpt-a-t1003115316 | `E2E-20261003T115316Z-U2` | started by 'Decision agreement counted per version (4/1/2 on version 3, 1 on version 2, undecided not counted)' |
| 527 | rpt-a-t1003115316 | `E2E-20261003T115316Z-U3` | started by 'Decision agreement counted per version (4/1/2 on version 3, 1 on version 2, undecided not counted)' |
| 528 | rpt-q-t1003115316 | `E2E-20261003T115316Z-Q` | started by 'Unread service query request_details ('more than 500 rows') kept; NEEDS_MANUAL_REVIEW' |
| 529..658 | rpt-b-t1003115316 | `1002` | 130 Checks started by 'Listing capped at 100 with the total: 130 Checks of request 1002, 101 of request 1003' (AWAITING, expire after 2 min) |
| 659..759 | rpt-b-t1003115316 | `1003` | 101 Checks started by 'Listing capped at 100 with the total: 130 Checks of request 1002, 101 of request 1003' (AWAITING, expire after 2 min) |
| 760 | rpt-b-t1003115316 | `E2E-20261003T115316Z-U503` | started by 'Failed Check read with its reason: the provider answers 503 -> MODEL_UNAVAILABLE' |
| 761 | rpt-b-t1003115316 | `E2E-20261003T115316Z-T120` | started by 'A RUNNING Check past its timeout is stored FAILED / TIMED_OUT with its detail and end time' |
| 762 | rpt-b-t1003115316 | `E2E-20261003T115316Z-UNF-OLD` | started by 'Unfinished Checks listed oldest first: a restart ends AWAITING (older) then RUNNING (newer); a COMPLETED one is untouched' |
| 763 | rpt-b-t1003115316 | `E2E-20261003T115316Z-UNF-NEW` | started by 'Unfinished Checks listed oldest first: a restart ends AWAITING (older) then RUNNING (newer); a COMPLETED one is untouched' |
| 764 | int-plain-t1003115316 | `E2E-20261003T115316Z-X999` | started by 'An employee unknown to any directory is accepted; no server-rendered report page' |
| 765 | int-m2-t1003115316 | `E2E-20261003T115316Z-I011` | started by 'Uploads alone never continue the Check: TRANSCRIPT + ID_CARD uploaded, no confirmation' |
| 766 | int-m2-t1003115316 | `E2E-20261003T115316Z-I012` | started by 'Neither an upload nor a confirmation records a decision' |
| 767 | int-m2-t1003115316 | `E2E-20261003T115316Z-I095` | started by 'The uploaded-documents read relays Document Access's listing unchanged (300 KB + 60 MB)' |
| 768 | approve-service-t1003115316 | `E2E-20261003T115316Z-IDLE` | started by 'APPROVED through the Approval API: recorded as executed; the only host connection is that call' |
| 769 | approve-service-t1003115316 | `E2E-20261003T115316Z-EXEC` | started by 'APPROVED through the Approval API: recorded as executed; the only host connection is that call' |
| 770 | approve-service-t1003115316 | `R-643` | started by 'A refusal after an executed approval: REJECTED recorded while the Approval API holds; the APPROVED answers 409 and is logged' |
| 771 | int-mal-t1003115316 | `E2E-20261003T115316Z-MAL` | started by 'The decision path answers in the standard form when the approval definition cannot be read' |
| 772 | int-plain-t1003115316 | `E2E-20261003T115316Z-I719` | started by 'Host Integration keeps nothing after answering (tables, multipart storage, live heap)' |
| 773 | int-plain-t1003115316 | `E2E-20261003T115316Z-I718` | started by 'Host Integration keeps nothing after answering (tables, multipart storage, live heap)' |
| 774 | int-plain-t1003115316 | `E2E-20261003T115316Z-I006` | started by 'An unexpected failure (the database paused) is answered INT-500 without internals' |
