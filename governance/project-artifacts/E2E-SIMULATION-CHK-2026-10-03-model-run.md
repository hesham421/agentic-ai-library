# aias E2E simulation run 20261003T140842Z

Base URL `http://localhost:7271` · comparison model `gemini-3.6-flash` · model calls: 0 comparison, 0 reading

| Group | Passed | Failed | Skipped | Ambiguous | Not exercisable | Error |
|---|---|---|---|---|---|---|
| chk-pipeline | 1 | 0 | 0 | 0 | 0 | 0 |

## Scenarios

- **PASSED** [chk-pipeline] The comparison model is taken from configuration (aias.check.comparison-model.model = 'e2e-cmp-model-b', tier FREE, data class SYNTHETIC): the call is sent to it and the report records it (TC-CHK-082)

## App restarts (restart groups)

- 2026-10-03T18:08:51 — mode [aias.registry.package-directory=local/e2e-chk/packages, aias.documents.storage-root=local/e2e-chk/root, spring.ai.openai.base-url=http://127.0.0.1:7294/v1beta/openai, aias.check.comparison-model.model=e2e-cmp-model-b, aias.check.timeout=PT45S, aias.check.upload-window=PT75S, aias.check.deadline-check-interval=PT2S, aias.check.max-rows=1000, spring.ai.mcp.client.stdio.connections.local-oracle.command=python3, spring.ai.mcp.client.stdio.connections.local-oracle.args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js, connections=['local-oracle', 'local-jdbc', 'main-db']] (by 'The comparison model is taken from configuration (aias.check.comparison-model.model = 'e2e-cmp-model-b', tier FREE, data class SYNTHETIC): the call is sent to it and the report records it')
- 2026-10-03T18:09:29 — mode [normal] (by '(end of run)')

## Surviving records (synthetic, kept until the retention purge)

| checkId | service | requestNumber | what |
|---|---|---|---|
| 960 | chk-m-t1003140842 | `E2E-20261003T140842Z-CFGM` | started by 'The comparison model is taken from configuration (aias.check.comparison-model.model = 'e2e-cmp-model-b', tier FREE, data class SYNTHETIC): the call is sent to it and the report records it' |
