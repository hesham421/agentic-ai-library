# aias E2E simulation run 20261003T130654Z

Base URL `http://localhost:7271` · comparison model `gemini-3.6-flash` · model calls: 1 comparison, 0 reading

| Group | Passed | Failed | Skipped | Ambiguous | Not exercisable | Error |
|---|---|---|---|---|---|---|
| decisions | 2 | 0 | 0 | 0 | 0 | 0 |

## Scenarios

- **PASSED** [decisions] Decisions on a COMPLETED demo-manual Check (no Approval API) (TC-INT-014, TC-INT-035, TC-INT-036, TC-INT-003, TC-RPT-039, TC-RPT-041, TC-RPT-042, TC-RPT-047, TC-RPT-049)
- **PASSED** [decisions] Decisions refused on a non-completed and an unknown Check (TC-RPT-040, TC-RPT-045)

## Surviving records (synthetic, kept until the retention purge)

| checkId | service | requestNumber | what |
|---|---|---|---|
| 868 | demo-manual | `E2E-20261003T130654Z-PASS` | started by 'Decisions on a COMPLETED demo-manual Check (no Approval API)' |
| 869 | demo-manual | `E2E-20261003T130654Z-DEC-WAIT-78192b` | started by 'Decisions refused on a non-completed and an unknown Check' |
