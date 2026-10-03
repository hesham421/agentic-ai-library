# aias E2E simulation run 20261003T130537Z

Base URL `http://localhost:7271` · comparison model `gemini-3.6-flash` · model calls: 1 comparison, 0 reading · quota hit: the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er

| Group | Passed | Failed | Skipped | Ambiguous | Not exercisable | Error |
|---|---|---|---|---|---|---|
| registry | 4 | 0 | 0 | 0 | 0 | 0 |
| decisions | 1 | 0 | 1 | 0 | 0 | 0 |
| refusals | 6 | 0 | 0 | 0 | 0 | 0 |

## Scenarios

- **PASSED** [registry] The four fixture services are listed with their fetch modes, without SQL or connection (TC-REG-014, TC-REG-005, TC-REG-001)
- **PASSED** [registry] The load report shows both connections ACTIVATED and every fixture package loaded (TC-REG-008, TC-REG-051, TC-REG-006)
- **PASSED** [registry] An unknown service code is 404 REG-404-SERVICE-NOT-FOUND (TC-REG-016)
- **PASSED** [registry] A service code is read trimmed and case-insensitively (TC-REG-075, TC-REG-073)
- **SKIPPED-QUOTA** [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) — check 862 ended MODEL_UNAVAILABLE: the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head (TC-INT-014, TC-INT-035, TC-INT-036, TC-INT-003, TC-RPT-039, TC-RPT-041, TC-RPT-042, TC-RPT-047, TC-RPT-049)
- **PASSED** [decisions] Decisions refused on a non-completed and an unknown Check (TC-RPT-040, TC-RPT-045)
- **PASSED** [refusals] Start refusals: unknown service, incomplete start, unreadable body, unsupported Content-Type (TC-CHK-003, TC-CHK-001, TC-INT-001)
- **PASSED** [refusals] Upload refusals and limits on a waiting manual Check (TC-INT-002, TC-DOC-013, TC-DOC-014, TC-INT-033, TC-DOC-027, TC-INT-009, TC-INT-008, TC-INT-100, TC-DOC-015, TC-INT-010)
- **PASSED** [refusals] Maximum uploads per Check: 20 accepted, the 21st refused (TC-INT-097, TC-DOC-071, TC-DOC-072)
- **PASSED** [refusals] Confirmation of an unknown Check; reads of unknown Checks (TC-CHK-038, TC-INT-088, TC-INT-094, TC-RPT-030)
- **PASSED** [refusals] Non-numeric identifiers are refused by each module's own code (TC-INT-005)
- **PASSED** [refusals] Lists of a request without their keys are refused (TC-RPT-035, TC-INT-090)

## Surviving records (synthetic, kept until the retention purge)

| checkId | service | requestNumber | what |
|---|---|---|---|
| 862 | demo-manual | `E2E-20261003T130537Z-PASS` | started by 'Decisions on a COMPLETED demo-manual Check (no Approval API)' |
| 863 | demo-manual | `E2E-20261003T130537Z-DEC-WAIT-a7e398` | started by 'Decisions refused on a non-completed and an unknown Check' |
| 864 | demo-manual | `E2E-20261003T130537Z-UPL-47af58` | started by 'Upload refusals and limits on a waiting manual Check' |
| 865 | demo-manual | `E2E-20261003T130537Z-MAXUP-1612f6` | started by 'Maximum uploads per Check: 20 accepted, the 21st refused' |
