# aias E2E simulation run 20261003T130306Z

Base URL `http://localhost:7271` · comparison model `gemini-3.6-flash` · model calls: 0 comparison, 0 reading

| Group | Passed | Failed | Skipped | Ambiguous | Not exercisable | Error |
|---|---|---|---|---|---|---|
| rpt-purge | 1 | 0 | 0 | 0 | 0 | 0 |

## Scenarios

- **PASSED** [rpt-purge] Purge failure logged at WARN with its Check run and cause, before the closing line (TC-RPT-064)

## App restarts (restart groups)

- 2026-10-03T17:04:35 — mode [spring.ai.openai.base-url=http://127.0.0.1:7293/v1beta/openai, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, aias.registry.package-directory=local/e2e-rpt/packages-v3, aias.documents.storage-root=local/e2e-rpt/root, aias.check.timeout=PT60S, aias.check.deadline-check-interval=PT5S, aias.check.max-rows=500, aias.reports.retention-days=1, aias.reports.purge-schedule=*/10 * * * * *, spring.datasource.hikari.data-source-properties[oracle.jdbc.ReadTimeout]=5000, connections=['local-oracle', 'local-jdbc']] (by 'Purge failure logged at WARN with its Check run and cause, before the closing line')
- 2026-10-03T17:05:26 — mode [normal] (by '(end of run)')

## Surviving records (synthetic, kept until the retention purge)

| checkId | service | requestNumber | what |
|---|---|---|---|
