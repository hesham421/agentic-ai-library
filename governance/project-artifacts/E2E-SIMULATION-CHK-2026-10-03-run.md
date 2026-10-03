# aias E2E simulation run 20261003T135010Z

Base URL `http://localhost:7271` · comparison model `gemini-3.6-flash` · model calls: 1 comparison, 0 reading · quota hit: the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er

| Group | Passed | Failed | Skipped | Ambiguous | Not exercisable | Error |
|---|---|---|---|---|---|---|
| registry | 4 | 0 | 0 | 0 | 0 | 0 |
| refusals | 6 | 0 | 0 | 0 | 0 | 0 |
| lifecycle | 1 | 0 | 1 | 0 | 0 | 0 |
| expiry | 1 | 0 | 0 | 0 | 0 | 0 |
| limits | 0 | 0 | 5 | 0 | 0 | 0 |
| connection | 3 | 0 | 0 | 0 | 0 | 0 |
| chk-pipeline | 18 | 0 | 1 | 0 | 0 | 0 |
| chk-endings | 6 | 0 | 0 | 0 | 0 | 0 |
| chk-gate | 3 | 0 | 0 | 0 | 0 | 0 |
| chk-eval | 8 | 0 | 0 | 0 | 0 | 0 |
| chk-inprocess | 0 | 0 | 0 | 0 | 6 | 0 |

## Scenarios

- **PASSED** [registry] The four fixture services are listed with their fetch modes, without SQL or connection (TC-REG-014, TC-REG-005, TC-REG-001)
- **PASSED** [registry] The load report shows both connections ACTIVATED and every fixture package loaded (TC-REG-008, TC-REG-051, TC-REG-006)
- **PASSED** [registry] An unknown service code is 404 REG-404-SERVICE-NOT-FOUND (TC-REG-016)
- **PASSED** [registry] A service code is read trimmed and case-insensitively (TC-REG-075, TC-REG-073)
- **PASSED** [refusals] Start refusals: unknown service, incomplete start, unreadable body, unsupported Content-Type (TC-CHK-003, TC-CHK-001, TC-INT-001)
- **PASSED** [refusals] Upload refusals and limits on a waiting manual Check (TC-INT-002, TC-DOC-013, TC-DOC-014, TC-INT-033, TC-DOC-027, TC-INT-009, TC-INT-008, TC-INT-100, TC-DOC-015, TC-INT-010)
- **PASSED** [refusals] Maximum uploads per Check: 20 accepted, the 21st refused (TC-INT-097, TC-DOC-071, TC-DOC-072)
- **PASSED** [refusals] Confirmation of an unknown Check; reads of unknown Checks (TC-CHK-038, TC-INT-088, TC-INT-094, TC-RPT-030)
- **PASSED** [refusals] Non-numeric identifiers are refused by each module's own code (TC-INT-005)
- **PASSED** [refusals] Lists of a request without their keys are refused (TC-RPT-035, TC-INT-090)
- **PASSED** [lifecycle] Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upload-window deadline (TC-CHK-083, TC-CHK-086)
- **SKIPPED-QUOTA** [lifecycle] After the Check ends: no Active Check, and its uploads are deleted — check 916 ended MODEL_UNAVAILABLE: the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head (TC-CHK-085, TC-DOC-052, TC-CHK-028)
- **PASSED** [expiry] Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRED; Active Check and uploads gone (TC-CHK-040, TC-CHK-041, TC-CHK-083, TC-CHK-028, TC-DOC-052)
- **SKIPPED-QUOTA** [limits] max-file-size 1 KB: a 4.5 KB path document is UNREADABLE / TOO_LARGE without being read — model quota already exhausted in this run (the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er) (TC-DOC-025, TC-DOC-018)
- **SKIPPED-QUOTA** [limits] max-file-size 1 KB: a 2000-byte BLOB is UNREADABLE / TOO_LARGE, measured before any byte is read — model quota already exhausted in this run (the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er) (TC-DOC-024, TC-DOC-018)
- **SKIPPED-QUOTA** [limits] REAL data, FREE reading model: the PNG is UNREADABLE / MODEL_NOT_PERMITTED, the PDF beside it is READ — model quota already exhausted in this run (the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er) (TC-DOC-065, TC-DOC-064)
- **SKIPPED-QUOTA** [limits] max-rows 1: a query answering 2 rows is recorded unread ('more than 1 rows') -> NEEDS_MANUAL_REVIEW — model quota already exhausted in this run (the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er) (TC-CHK-031, TC-CHK-032)
- **SKIPPED-QUOTA** [limits] Two simultaneous APPROVED decisions: one 201 (one Approval API call), one 409 after the lock — model quota already exhausted in this run (the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er) (TC-INT-098)
- **PASSED** [connection] A package whose query names an unregistered connection is REJECTED at load (ghost-db) (TC-REG-032, TC-REG-007, TC-REG-051)
- **PASSED** [connection] The connection removed from the activation config: REMOVED; start -> 422 CHK-422-CONNECTION-NOT-ACTIVATED (TC-REG-053, TC-REG-055, TC-CHK-004, TC-CHK-100)
- **PASSED** [connection] Back to the normal registry: demo-conn's folder parked again -> WITHDRAWN (TC-REG-010)
- **SKIPPED-QUOTA** [chk-pipeline] Provider switched by configuration only: Gemini (provider A, real call) then the local OpenAI-compatible endpoint (provider B, stub) — the configured model is called and recorded — model quota already exhausted in this run (the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er) (TC-CHK-081, TC-CHK-082)
- **PASSED** [chk-pipeline] Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's steps in order, 1 query (stored text, bound value) over the MCP tool, documents fetched once, 2 READ contents and the row only inside the data delimiters, one finding per required type (TC-CHK-054, TC-CHK-055, TC-CHK-057, TC-CHK-060, TC-CHK-061, TC-CHK-063, TC-CHK-064, TC-CHK-065, TC-CHK-066, TC-CHK-069, TC-CHK-008, TC-CHK-067)
- **PASSED** [chk-pipeline] A request number carrying SQL text is only the bound value; a query with no bind parameter is never sent (RULE-CHK-003) (TC-CHK-006)
- **PASSED** [chk-pipeline] A query over a connection that is not a read-only MCP connection (main-db, type jdbc) is not run and is recorded as not read (TC-CHK-007)
- **PASSED** [chk-pipeline] A failing service query (ORA-00942) is recorded as not read, the Check continues; every finding SATISFIED + an unread query -> NEEDS_MANUAL_REVIEW (TC-CHK-030, TC-CHK-026)
- **PASSED** [chk-pipeline] A model SATISFIED contradicting the recomputed comparison (2.8 >= 3.0) is replaced: NOT_SATISFIED, NOT_COMPLIANT (TC-CHK-012)
- **PASSED** [chk-pipeline] Deterministic checks part 2 (ADR-CHK-003), one finding each: value not in the data, limit not in the knowledge (RULE-CHK-006), value not a number, evidence absent, empty evidence; every comparison operator recomputed for numbers and dates (TC-CHK-013, TC-CHK-014, TC-CHK-015, TC-CHK-016, TC-CHK-023, TC-CHK-012)
- **PASSED** [chk-pipeline] Overall Status precedence: 1 NOT_SATISFIED + 1 UNDETERMINED + 3 SATISFIED -> NOT_COMPLIANT, handed over COMPLETED with 5 findings and 2 document outcomes (TC-CHK-024, TC-CHK-072)
- **PASSED** [chk-pipeline] Overall Status: 1 UNDETERMINED + 4 SATISFIED, every query read -> NEEDS_MANUAL_REVIEW (TC-CHK-025)
- **PASSED** [chk-pipeline] A COMPLIANT Check: no Employee Decision handed over, document outcomes without content (3 000-character transcript), exactly one end-of-Check notice after the complete call (TC-CHK-020, TC-CHK-074, TC-CHK-077)
- **PASSED** [chk-pipeline] A COMPLIANT Check of an approval-enabled service: 0 approval calls; a model note carrying an UPDATE statement is never executed and is stored only as finding text (TC-CHK-019, TC-CHK-018)
- **PASSED** [chk-pipeline] Instruction text in the transcript and a forged </check-data> in a query result stay data: escaped inside their blocks, absent from the instruction part; GPA 2.8 stays NOT_SATISFIED (TC-CHK-021, TC-CHK-053)
- **PASSED** [chk-pipeline] The model call: 2 messages, 0 tools, the fixed output schema (condition, outcome, evidence, note) validated; the 4 200-character knowledge whole as the only service instruction; no earlier conversation of the same request (TC-CHK-017, TC-CHK-070, TC-CHK-068, TC-CHK-079)
- **PASSED** [chk-pipeline] Two concurrent Checks share no data; a second Check of the same request starts on its own while the first is RUNNING (TC-CHK-078, TC-CHK-080)
- **PASSED** [chk-pipeline] Each query call carries the row limit maxRows + 1 and only the Check's time left (slow first query, then request_details) (TC-CHK-062)
- **PASSED** [chk-pipeline] XM-CHK-001 degraded: no package of 'scholarship-request' loaded -> 422 CHK-422-SERVICE-NOT-AVAILABLE, no Check run, no Active Check (TC-CHK-096)
- **PASSED** [chk-pipeline] XM-CHK-003 degraded: the version's only query is its document source query -> 0 service queries, documents fetched, exactly one ending, Active Check deleted (TC-CHK-098)
- **PASSED** [chk-pipeline] XM-CHK-004 degraded: the version lists no required document type -> 0 required-document findings, the model's findings verified, one ending (TC-CHK-099)
- **PASSED** [chk-pipeline] A completed Check's working data is discarded: its query-result, document and model-output markers are held by no live Check Engine object (heap dump) (TC-CHK-045)
- **PASSED** [chk-endings] Model output outside the fixed structure (free text) -> FAILED / MODEL_OUTPUT_INVALID, no Overall Status, exactly one end-of-Check notice after the fail call (TC-CHK-022, TC-CHK-042)
- **PASSED** [chk-endings] The comparison model provider answers HTTP 503 -> FAILED / MODEL_UNAVAILABLE, no Overall Status (TC-CHK-035)
- **PASSED** [chk-endings] The result port rejects the complete call (a finding with a blank condition) -> FAILED / INTERNAL_ERROR, one end-of-Check notice (TC-CHK-029)
- **PASSED** [chk-endings] A Check RUNNING past its timeout (the model holds its answer 60 s, timeout 45 s) ends FAILED / TIMED_OUT with one reason, a detail and an end time; the late answer is discarded (TC-CHK-033, TC-CHK-036)
- **PASSED** [chk-endings] Timeout counted from RUNNING: a Check waiting 50 s (> the 45 s timeout) is confirmed and runs on; the deadline check ends only the waiting Check past its window; confirmation refusals (TC-CHK-034, TC-CHK-051, TC-CHK-039, TC-CHK-038)
- **PASSED** [chk-endings] A Check that completes just before its deadline is not ended a second time by the timeout path (the DELETE guard) (TC-CHK-050)
- **PASSED** [chk-gate] Data class declared without a value counts as REAL: the FREE model receives 0 calls, the Check fails MODEL_NOT_PERMITTED; a restart ends a waiting Check INTERRUPTED with one notice (TC-CHK-048)
- **PASSED** [chk-gate] Comparison model tier declared without a value counts as FREE: with data class REAL the model receives 0 calls and the Check fails MODEL_NOT_PERMITTED (TC-CHK-049)
- **PASSED** [chk-gate] The end-of-Check notice is sent exactly once on every ending path (COMPLETED and the 7 failure reasons) (TC-CHK-043)
- **PASSED** [chk-eval] Model-evaluation run (profile model-eval) over a SYNTHETIC known-result set of 6 requests (test-supplied under local/e2e-chk/model-eval/set, runner's documented record format) against the scripted stub: the report lists request, expected and reached status; the run PASSED (TC-CHK-088)
- **PASSED** [chk-eval] Known-result request SYN-001 reaches COMPLIANT (TC-CHK-090)
- **PASSED** [chk-eval] Known-result request SYN-002 reaches NOT_COMPLIANT (TC-CHK-091)
- **PASSED** [chk-eval] Known-result request SYN-003 reaches NOT_COMPLIANT (TC-CHK-092)
- **PASSED** [chk-eval] Known-result request SYN-004 reaches NEEDS_MANUAL_REVIEW (TC-CHK-093)
- **PASSED** [chk-eval] Known-result request SYN-005 reaches NEEDS_MANUAL_REVIEW (TC-CHK-094)
- **PASSED** [chk-eval] Known-result request SYN-006 reaches NOT_COMPLIANT (TC-CHK-095)
- **PASSED** [chk-eval] A known-result request reaching another status than expected fails the run and is named (SYN-901 expected NOT_COMPLIANT, reached COMPLIANT under the scripted double) (TC-CHK-089)
- **NOT-EXERCISABLE** [chk-inprocess] An unexpected error in a deterministic step ends the Check FAILED / INTERNAL_ERROR — needs a fault injected into the deterministic step: no request data, document or model output reaching it through the API raises an unexpected error there (every shape is handled — absent evidence, unparsable values, unknown operators, blank texts). The INTERNAL_ERROR ending itself is evidenced by TC-CHK-029 (chk-endings) (TC-CHK-005)
- **NOT-EXERCISABLE** [chk-inprocess] A Document Access failure ends the Check FAILED / INTERNAL_ERROR with the failure text — Document Access raises only for an unresolvable service version ("service package version not found"); a Check always fetches the version it was started on and REG keeps every version, every other DOC failure is a document outcome (UNREADABLE), never an exception — no public path (TC-CHK-011)
- **NOT-EXERCISABLE** [chk-inprocess] A failing end-of-Check notice is retried once, logged, and the ending is kept — needs Document Access's endCheck to fail twice while the ending's own database transaction succeeds — both use the same database; not producible through the API (TC-CHK-044)
- **NOT-EXERCISABLE** [chk-inprocess] A manual Check keeps its start version when a newer one becomes current — a newer version becomes current only through a load run at an instance start, and every start ends all unfinished Checks INTERRUPTED (REQ-CHK-055, global): no Check can await uploads on version 3 while version 4 is current (same reason as TC-INT-032) (TC-CHK-058)
- **NOT-EXERCISABLE** [chk-inprocess] XM-CHK-002 degraded: the recorded version cannot be resolved on resume — REG never deletes a stored version and a waiting Check does not survive a restart (REQ-CHK-055), so getServicePackageVersion never answers not-found for a Check's recorded version (TC-CHK-097)
- **NOT-EXERCISABLE** [chk-inprocess] The known-result set has every Overall Status, each request synthetic — no known-result set is delivered with the service (model-eval/known-result-set/ is absent); the chk-eval group supplies a synthetic one to exercise the runner, which proves the runner, not the delivered set (TC-CHK-087)

## App restarts (restart groups)

- 2026-10-03T17:50:24 — mode [aias.check.upload-window=PT1M, aias.check.deadline-check-interval=PT5S] (by 'Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRED; Active Check and uploads gone')
- 2026-10-03T17:52:01 — mode [aias.check.max-file-size=1KB, aias.check.max-rows=1, aias.documents.data-class=REAL, aias.check.comparison-model.tier=APPROVED] (by 'max-file-size 1 KB: a 4.5 KB path document is UNREADABLE / TOO_LARGE without being read')
- 2026-10-03T17:52:38 — mode [connections=['local-oracle', 'local-jdbc', 'local-extra']], moved in ['demo-conn', 'demo-noconn'] (by 'A package whose query names an unregistered connection is REJECTED at load (ghost-db)')
- 2026-10-03T17:53:14 — mode [normal], moved in ['demo-conn'] (by 'The connection removed from the activation config: REMOVED; start -> 422 CHK-422-CONNECTION-NOT-ACTIVATED')
- 2026-10-03T17:53:50 — mode [normal] (by 'Back to the normal registry: demo-conn's folder parked again -> WITHDRAWN')
- 2026-10-03T17:54:30 — mode [aias.registry.package-directory=local/e2e-chk/packages, aias.documents.storage-root=local/e2e-chk/root, spring.ai.openai.base-url=http://127.0.0.1:7294/v1beta/openai, aias.check.comparison-model.model=e2e-cmp-model-b, aias.check.timeout=PT45S, aias.check.upload-window=PT75S, aias.check.deadline-check-interval=PT2S, aias.check.max-rows=1000, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, connections=['local-oracle', 'local-jdbc', 'main-db']] (by 'Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's steps in order, 1 query (stored text, bound value) over the MCP tool, documents fetched once, 2 READ contents and the row only inside the data delimiters, one finding per required type')
- 2026-10-03T18:03:39 — mode [aias.registry.package-directory=local/e2e-chk/packages, aias.documents.storage-root=local/e2e-chk/root, spring.ai.openai.base-url=http://127.0.0.1:7294/v1beta/openai, aias.check.comparison-model.model=e2e-cmp-model-b, aias.check.timeout=PT45S, aias.check.upload-window=PT75S, aias.check.deadline-check-interval=PT2S, aias.check.max-rows=1000, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, aias.documents.data-class=, connections=['local-oracle', 'local-jdbc', 'main-db']] (by 'Data class declared without a value counts as REAL: the FREE model receives 0 calls, the Check fails MODEL_NOT_PERMITTED; a restart ends a waiting Check INTERRUPTED with one notice')
- 2026-10-03T18:04:20 — mode [aias.registry.package-directory=local/e2e-chk/packages, aias.documents.storage-root=local/e2e-chk/root, spring.ai.openai.base-url=http://127.0.0.1:7294/v1beta/openai, aias.check.comparison-model.model=e2e-cmp-model-b, aias.check.timeout=PT45S, aias.check.upload-window=PT75S, aias.check.deadline-check-interval=PT2S, aias.check.max-rows=1000, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, aias.check.comparison-model.tier=, aias.documents.data-class=REAL, connections=['local-oracle', 'local-jdbc', 'main-db']] (by 'Comparison model tier declared without a value counts as FREE: with data class REAL the model receives 0 calls and the Check fails MODEL_NOT_PERMITTED')
- 2026-10-03T18:04:58 — profiles local,model-eval (set) (by 'Model-evaluation run (profile model-eval) over a SYNTHETIC known-result set of 6 requests (test-supplied under local/e2e-chk/model-eval/set, runner's documented record format) against the scripted stub: the report lists request, expected and reached status; the run PASSED')
- 2026-10-03T18:05:34 — profiles local,model-eval (mismatch) (by 'A known-result request reaching another status than expected fails the run and is named (SYN-901 expected NOT_COMPLIANT, reached COMPLIANT under the scripted double)')
- 2026-10-03T18:06:11 — mode [normal] (by '(end of run)')

## Surviving records (synthetic, kept until the retention purge)

| checkId | service | requestNumber | what |
|---|---|---|---|
| 913 | demo-manual | `E2E-20261003T135010Z-UPL-5ed1df` | started by 'Upload refusals and limits on a waiting manual Check' |
| 914 | demo-manual | `E2E-20261003T135010Z-MAXUP-c56fb6` | started by 'Maximum uploads per Check: 20 accepted, the 21st refused' |
| 915 | demo-manual | `E2E-20261003T135010Z-LIFE-cbbedb` | started by 'Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upload-window deadline' |
| 916 | demo-manual | `E2E-20261003T135010Z-PASS` | started by 'After the Check ends: no Active Check, and its uploads are deleted' |
| 917 | demo-manual | `E2E-20261003T135010Z-EXP-d8e481` | started by 'Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRED; Active Check and uploads gone' |
| 918 | demo-approval | `E2E-20261003T135010Z-LIMITS-APR` | started by 'REAL data, FREE reading model: the PNG is UNREADABLE / MODEL_NOT_PERMITTED, the PDF beside it is READ' |
| 919 | demo-rows | `E2E-20261003T135010Z-ROWS` | started by 'max-rows 1: a query answering 2 rows is recorded unread ('more than 1 rows') -> NEEDS_MANUAL_REVIEW' |
| 920 | demo-conn | `E2E-20261003T135010Z-CONN-OK` | started by 'A package whose query names an unregistered connection is REJECTED at load (ghost-db)' |
| 921 | chk-p-t1003135010 | `00-1001/A` | started by 'Path Check '00-1001/A' by ' e.2041 ': answered before its query, the pipeline's steps in order, 1 query (stored text, bound value) over the MCP tool, documents fetched once, 2 READ contents and the row only inside the data delimiters, one finding per required type' |
| 922 | chk-nobind-t1003135010 | `1001' OR '1'='1` | started by 'A request number carrying SQL text is only the bound value; a query with no bind parameter is never sent (RULE-CHK-003)' |
| 923 | chk-nomcp-t1003135010 | `E2E-20261003T135010Z-NOMCP` | started by 'A query over a connection that is not a read-only MCP connection (main-db, type jdbc) is not run and is recorded as not read' |
| 924 | chk-qerr-t1003135010 | `E2E-20261003T135010Z-QERR` | started by 'A failing service query (ORA-00942) is recorded as not read, the Check continues; every finding SATISFIED + an unread query -> NEEDS_MANUAL_REVIEW' |
| 925 | chk-m-t1003135010 | `E2E-20261003T135010Z-G28` | started by 'A model SATISFIED contradicting the recomputed comparison (2.8 >= 3.0) is replaced: NOT_SATISFIED, NOT_COMPLIANT' |
| 926 | chk-m-t1003135010 | `E2E-20261003T135010Z-G28VER` | started by 'Deterministic checks part 2 (ADR-CHK-003), one finding each: value not in the data, limit not in the knowledge (RULE-CHK-006), value not a number, evidence absent, empty evidence; every comparison operator recomputed for numbers and dates' |
| 927 | chk-m-t1003135010 | `E2E-20261003T135010Z-PREC1` | started by 'Overall Status precedence: 1 NOT_SATISFIED + 1 UNDETERMINED + 3 SATISFIED -> NOT_COMPLIANT, handed over COMPLETED with 5 findings and 2 document outcomes' |
| 928 | chk-m-t1003135010 | `E2E-20261003T135010Z-PREC2` | started by 'Overall Status: 1 UNDETERMINED + 4 SATISFIED, every query read -> NEEDS_MANUAL_REVIEW' |
| 929 | chk-m-t1003135010 | `E2E-20261003T135010Z-COMPL` | started by 'A COMPLIANT Check: no Employee Decision handed over, document outcomes without content (3 000-character transcript), exactly one end-of-Check notice after the complete call' |
| 930 | chk-apr-t1003135010 | `E2E-20261003T135010Z-APR` | started by 'A COMPLIANT Check of an approval-enabled service: 0 approval calls; a model note carrying an UPDATE statement is never executed and is stored only as finding text' |
| 931 | chk-m-t1003135010 | `E2E-20261003T135010Z-G28INJ` | started by 'Instruction text in the transcript and a forged </check-data> in a query result stay data: escaped inside their blocks, absent from the instruction part; GPA 2.8 stays NOT_SATISFIED' |
| 932 | chk-long-t1003135010 | `E2E-20261003T135010Z-CONV` | started by 'The model call: 2 messages, 0 tools, the fixed output schema (condition, outcome, evidence, note) validated; the 4 200-character knowledge whole as the only service instruction; no earlier conversation of the same request' |
| 933 | chk-long-t1003135010 | `E2E-20261003T135010Z-CONV` | started by 'The model call: 2 messages, 0 tools, the fixed output schema (condition, outcome, evidence, note) validated; the 4 200-character knowledge whole as the only service instruction; no earlier conversation of the same request' |
| 934 | chk-m-t1003135010 | `E2E-20261003T135010Z-R1001` | started by 'Two concurrent Checks share no data; a second Check of the same request starts on its own while the first is RUNNING' |
| 935 | chk-m-t1003135010 | `E2E-20261003T135010Z-R1002` | started by 'Two concurrent Checks share no data; a second Check of the same request starts on its own while the first is RUNNING' |
| 936 | chk-p-t1003135010 | `1001` | started by 'Two concurrent Checks share no data; a second Check of the same request starts on its own while the first is RUNNING' |
| 937 | chk-p-t1003135010 | `1001` | started by 'Two concurrent Checks share no data; a second Check of the same request starts on its own while the first is RUNNING' |
| 938 | chk-slow-t1003135010 | `E2E-20261003T135010Z-SLOW` | started by 'Each query call carries the row limit maxRows + 1 and only the Check's time left (slow first query, then request_details)' |
| 939 | chk-src-t1003135010 | `E2E-20261003T135010Z-SRC` | started by 'XM-CHK-003 degraded: the version's only query is its document source query -> 0 service queries, documents fetched, exactly one ending, Active Check deleted' |
| 940 | chk-noreq-t1003135010 | `E2E-20261003T135010Z-NOREQ` | started by 'XM-CHK-004 degraded: the version lists no required document type -> 0 required-document findings, the model's findings verified, one ending' |
| 941 | chk-m-t1003135010 | `E2E-20261003T135010Z-MRK3EB7FF794` | started by 'A completed Check's working data is discarded: its query-result, document and model-output markers are held by no live Check Engine object (heap dump)' |
| 942 | chk-m-t1003135010 | `E2E-20261003T135010Z-INVAL` | started by 'Model output outside the fixed structure (free text) -> FAILED / MODEL_OUTPUT_INVALID, no Overall Status, exactly one end-of-Check notice after the fail call' |
| 943 | chk-m-t1003135010 | `E2E-20261003T135010Z-U503` | started by 'The comparison model provider answers HTTP 503 -> FAILED / MODEL_UNAVAILABLE, no Overall Status' |
| 944 | chk-m-t1003135010 | `E2E-20261003T135010Z-REJ` | started by 'The result port rejects the complete call (a finding with a blank condition) -> FAILED / INTERNAL_ERROR, one end-of-Check notice' |
| 945 | chk-m-t1003135010 | `E2E-20261003T135010Z-TOUT` | started by 'A Check RUNNING past its timeout (the model holds its answer 60 s, timeout 45 s) ends FAILED / TIMED_OUT with one reason, a detail and an end time; the late answer is discarded' |
| 946 | chk-m-t1003135010 | `E2E-20261003T135010Z-WAITA` | started by 'Timeout counted from RUNNING: a Check waiting 50 s (> the 45 s timeout) is confirmed and runs on; the deadline check ends only the waiting Check past its window; confirmation refusals' |
| 947 | chk-m-t1003135010 | `E2E-20261003T135010Z-WAITB` | started by 'Timeout counted from RUNNING: a Check waiting 50 s (> the 45 s timeout) is confirmed and runs on; the deadline check ends only the waiting Check past its window; confirmation refusals' |
| 948 | chk-m-t1003135010 | `E2E-20261003T135010Z-EDGE` | started by 'A Check that completes just before its deadline is not ended a second time by the timeout path (the DELETE guard)' |
| 949 | chk-m-t1003135010 | `E2E-20261003T135010Z-INTR` | started by 'Data class declared without a value counts as REAL: the FREE model receives 0 calls, the Check fails MODEL_NOT_PERMITTED; a restart ends a waiting Check INTERRUPTED with one notice' |
| 950 | chk-m-t1003135010 | `E2E-20261003T135010Z-DCLS` | started by 'Data class declared without a value counts as REAL: the FREE model receives 0 calls, the Check fails MODEL_NOT_PERMITTED; a restart ends a waiting Check INTERRUPTED with one notice' |
| 951 | chk-m-t1003135010 | `E2E-20261003T135010Z-TIER` | started by 'Comparison model tier declared without a value counts as FREE: with data class REAL the model receives 0 calls and the Check fails MODEL_NOT_PERMITTED' |
