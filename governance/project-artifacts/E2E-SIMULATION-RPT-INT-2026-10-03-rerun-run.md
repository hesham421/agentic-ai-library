# aias E2E simulation run 20261003T125219Z

Base URL `http://localhost:7271` · comparison model `gemini-3.6-flash` · model calls: 0 comparison, 0 reading

| Group | Passed | Failed | Skipped | Ambiguous | Not exercisable | Error |
|---|---|---|---|---|---|---|
| registry | 0 | 4 | 0 | 0 | 0 | 0 |
| refusals | 0 | 6 | 0 | 0 | 0 | 0 |
| rpt-store | 1 | 0 | 0 | 0 | 0 | 0 |
| rpt-purge | 0 | 1 | 0 | 0 | 0 | 0 |

## Scenarios

- **FAILED** [registry] The four fixture services are listed with their fetch modes, without SQL or connection — stopped: GET /api/v1/services -> 200 (TC-REG-014, TC-REG-005, TC-REG-001)
  - FAIL GET /api/v1/services -> 200: `GET /api/v1/services -> 500 `
- **FAILED** [registry] The load report shows both connections ACTIVATED and every fixture package loaded — stopped: GET /api/v1/load-results -> 200 (TC-REG-008, TC-REG-051, TC-REG-006)
  - FAIL GET /api/v1/load-results -> 200: `GET /api/v1/load-results -> 500 `
- **FAILED** [registry] An unknown service code is 404 REG-404-SERVICE-NOT-FOUND (TC-REG-016)
  - FAIL GET /api/v1/services/no-such-service-e2e -> 404 REG-404-SERVICE-NOT-FOUND: `GET /api/v1/services/no-such-service-e2e -> 500 `
- **FAILED** [registry] A service code is read trimmed and case-insensitively (TC-REG-075, TC-REG-073)
  - FAIL GET /api/v1/services/DEMO-Manual -> 200: `GET /api/v1/services/DEMO-Manual -> 500 `
  - FAIL upper/mixed case answers demo-manual: `GET /api/v1/services/DEMO-Manual -> 500 `
  - FAIL GET /api/v1/services/%20%20Demo-Path%20 -> 200: `GET /api/v1/services/%20%20Demo-Path%20 -> 500 `
  - FAIL space-padded code answers demo-path: `GET /api/v1/services/%20%20Demo-Path%20 -> 500 `
- **FAILED** [refusals] Start refusals: unknown service, incomplete start, unreadable body, unsupported Content-Type (TC-CHK-003, TC-CHK-001, TC-INT-001)
  - FAIL POST /api/v1/checks -> 422 CHK-422-SERVICE-NOT-AVAILABLE: `POST /api/v1/checks -> 500 `
  - FAIL start without employeeId -> 400 CHK-400-START-INCOMPLETE: `POST /api/v1/checks -> 500 `
  - FAIL start with a blank employeeId -> 400 CHK-400-START-INCOMPLETE: `POST /api/v1/checks -> 500 `
  - FAIL unreadable JSON -> 400 INT-400-REQUEST-INVALID: `POST /api/v1/checks -> 500 `
  - FAIL Content-Type text/plain -> 400 INT-400-REQUEST-INVALID: `POST /api/v1/checks -> 500 `
- **FAILED** [refusals] Upload refusals and limits on a waiting manual Check — stopped: start demo-manual -> 202 (TC-INT-002, TC-DOC-013, TC-DOC-014, TC-INT-033, TC-DOC-027, TC-INT-009, TC-INT-008, TC-INT-100, TC-DOC-015, TC-INT-010)
  - FAIL start demo-manual -> 202: `POST /api/v1/checks -> 500 `
- **FAILED** [refusals] Maximum uploads per Check: 20 accepted, the 21st refused — stopped: start demo-manual -> 202 (TC-INT-097, TC-DOC-071, TC-DOC-072)
  - FAIL start demo-manual -> 202: `POST /api/v1/checks -> 500 `
- **FAILED** [refusals] Confirmation of an unknown Check; reads of unknown Checks (TC-CHK-038, TC-INT-088, TC-INT-094, TC-RPT-030)
  - FAIL POST /api/v1/checks/987654321/upload-confirmation -> 404 CHK-404-CHECK-NOT-FOUND: `POST /api/v1/checks/987654321/upload-confirmation -> 500 `
  - FAIL GET /api/v1/check-reports/987654321 -> 404 RPT-404-CHECK-NOT-FOUND: `GET /api/v1/check-reports/987654321 -> 500 `
  - FAIL GET /api/v1/checks/987654321 -> 404 RPT-404-CHECK-NOT-FOUND: `GET /api/v1/checks/987654321 -> 500 `
  - FAIL GET /api/v1/checks/987654321/required-document-types -> 404 RPT-404-CHECK-NOT-FOUND: `GET /api/v1/checks/987654321/required-document-types -> 500 `
  - FAIL GET /api/v1/active-checks/987654321 -> 404 CHK-404-ACTIVE-CHECK-NOT-FOUND: `GET /api/v1/active-checks/987654321 -> 500 `
- **FAILED** [refusals] Non-numeric identifiers are refused by each module's own code (TC-INT-005)
  - FAIL GET /api/v1/check-reports/abc -> 400 INT-400-REQUEST-INVALID: `GET /api/v1/check-reports/abc -> 500 `
  - FAIL GET /api/v1/checks/abc/documents -> 400 INT-400-REQUEST-INVALID: `GET /api/v1/checks/abc/documents -> 500 `
  - FAIL GET /api/v1/checks/abc/required-document-types -> 400 INT-400-REQUEST-INVALID: `GET /api/v1/checks/abc/required-document-types -> 500 `
  - FAIL POST /api/v1/checks/abc/upload-confirmation -> 400 INT-400-REQUEST-INVALID: `POST /api/v1/checks/abc/upload-confirmation -> 500 `
  - FAIL POST /api/v1/checks/abc/decision -> 400 INT-400-REQUEST-INVALID: `POST /api/v1/checks/abc/decision -> 500 `
  - FAIL POST /api/v1/checks/abc/documents -> 400 INT-400-REQUEST-INVALID: `POST /api/v1/checks/abc/documents -> 500 `
  - FAIL GET /api/v1/checks/abc -> 400 RPT-400-CHECK-ID-INVALID: `GET /api/v1/checks/abc -> 500 `
  - FAIL GET /api/v1/active-checks/abc -> 400 CHK-400-CHECK-ID-INVALID: `GET /api/v1/active-checks/abc -> 500 `
  - FAIL GET /api/v1/uploaded-documents?checkId=abc -> 400 DOC-400-CHECK-ID-REQUIRED: `GET /api/v1/uploaded-documents?checkId=abc -> 500 `
  - FAIL GET /api/v1/uploaded-documents -> 400 DOC-400-CHECK-ID-REQUIRED: `GET /api/v1/uploaded-documents -> 500 `
- **FAILED** [refusals] Lists of a request without their keys are refused (TC-RPT-035, TC-INT-090)
  - FAIL GET /api/v1/checks?serviceCode=demo-manual -> 400 RPT-400-REQUEST-KEYS-MISSING: `GET /api/v1/checks?serviceCode=demo-manual -> 500 `
  - FAIL GET /api/v1/checks?requestNumber=x -> 400 RPT-400-REQUEST-KEYS-MISSING: `GET /api/v1/checks?requestNumber=x -> 500 `
  - FAIL GET /api/v1/check-reports -> 400 RPT-400-REQUEST-KEYS-MISSING: `GET /api/v1/check-reports -> 500 `
  - FAIL GET /api/v1/check-reports?serviceCode=demo-manual -> 400 RPT-400-REQUEST-KEYS-MISSING: `GET /api/v1/check-reports?serviceCode=demo-manual -> 500 `
- **PASSED** [rpt-store] No retention period configured: the scheduled purge is skipped and logged, nothing deleted (TC-RPT-052)
- **FAILED** [rpt-purge] Purge failure logged at WARN with its Check run and cause, before the closing line (TC-RPT-064)
  - FAIL the cause is the database's message (the lock wait was cancelled): `-10-03T16:53:20.112+04:00  WARN 55083 --- [agentic-ai-library] [   scheduling-1] i.a.rpt.service.ReportPurgeService       : Report purge kept Check run 130: its deletion failed (Connection is closed).`

## App restarts (restart groups)

- 2026-10-03T16:52:27 — mode [spring.ai.openai.base-url=http://127.0.0.1:7293/v1beta/openai, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, aias.registry.package-directory=local/e2e-rpt/packages-v2, aias.reports.purge-schedule=*/10 * * * * *, connections=['local-oracle', 'local-jdbc']] (by 'No retention period configured: the scheduled purge is skipped and logged, nothing deleted')
- 2026-10-03T16:53:08 — mode [spring.ai.openai.base-url=http://127.0.0.1:7293/v1beta/openai, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, aias.registry.package-directory=local/e2e-rpt/packages-v3, aias.documents.storage-root=local/e2e-rpt/root, aias.check.timeout=PT60S, aias.check.deadline-check-interval=PT5S, aias.check.max-rows=500, aias.reports.retention-days=1, aias.reports.purge-schedule=*/10 * * * * *, spring.datasource.hikari.data-source-properties[oracle.jdbc.ReadTimeout]=5000, connections=['local-oracle', 'local-jdbc']] (by 'Purge failure logged at WARN with its Check run and cause, before the closing line')
- 2026-10-03T16:53:56 — mode [normal] (by '(end of run)')

## Surviving records (synthetic, kept until the retention purge)

| checkId | service | requestNumber | what |
|---|---|---|---|
