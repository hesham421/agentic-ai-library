# aias E2E simulation run 20261003T140738Z

Base URL `http://localhost:7271` · comparison model `gemini-3.6-flash` · model calls: 1 comparison, 0 reading · quota hit: the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er

| Group | Passed | Failed | Skipped | Ambiguous | Not exercisable | Error |
|---|---|---|---|---|---|---|
| lifecycle | 1 | 0 | 1 | 0 | 0 | 0 |
| limits | 0 | 0 | 5 | 0 | 0 | 0 |

## Scenarios

- **PASSED** [lifecycle] Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upload-window deadline (TC-CHK-083, TC-CHK-086)
- **SKIPPED-QUOTA** [lifecycle] After the Check ends: no Active Check, and its uploads are deleted — check 957 ended MODEL_UNAVAILABLE: the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head (TC-CHK-085, TC-DOC-052, TC-CHK-028)
- **SKIPPED-QUOTA** [limits] max-file-size 1 KB: a 4.5 KB path document is UNREADABLE / TOO_LARGE without being read — model quota already exhausted in this run (the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er) (TC-DOC-025, TC-DOC-018)
- **SKIPPED-QUOTA** [limits] max-file-size 1 KB: a 2000-byte BLOB is UNREADABLE / TOO_LARGE, measured before any byte is read — model quota already exhausted in this run (the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er) (TC-DOC-024, TC-DOC-018)
- **SKIPPED-QUOTA** [limits] REAL data, FREE reading model: the PNG is UNREADABLE / MODEL_NOT_PERMITTED, the PDF beside it is READ — model quota already exhausted in this run (the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er) (TC-DOC-065, TC-DOC-064)
- **SKIPPED-QUOTA** [limits] max-rows 1: a query answering 2 rows is recorded unread ('more than 1 rows') -> NEEDS_MANUAL_REVIEW — model quota already exhausted in this run (the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er) (TC-CHK-031, TC-CHK-032)
- **SKIPPED-QUOTA** [limits] Two simultaneous APPROVED decisions: one 201 (one Approval API call), one 409 after the lock — model quota already exhausted in this run (the comparison model call failed: RateLimitException: 429: [{"error":{"code":429,"message":"You exceeded your current quota, please check your plan and billing details. For more information on this er) (TC-INT-098)

## App restarts (restart groups)

- 2026-10-03T18:07:50 — mode [aias.check.max-file-size=1KB, aias.check.max-rows=1, aias.documents.data-class=REAL, aias.check.comparison-model.tier=APPROVED] (by 'max-file-size 1 KB: a 4.5 KB path document is UNREADABLE / TOO_LARGE without being read')
- 2026-10-03T18:08:27 — mode [normal] (by '(end of run)')

## Surviving records (synthetic, kept until the retention purge)

| checkId | service | requestNumber | what |
|---|---|---|---|
| 956 | demo-manual | `E2E-20261003T140738Z-LIFE-b8f589` | started by 'Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upload-window deadline' |
| 957 | demo-manual | `E2E-20261003T140738Z-PASS` | started by 'After the Check ends: no Active Check, and its uploads are deleted' |
| 958 | demo-approval | `E2E-20261003T140738Z-LIMITS-APR` | started by 'REAL data, FREE reading model: the PNG is UNREADABLE / MODEL_NOT_PERMITTED, the PDF beside it is READ' |
| 959 | demo-rows | `E2E-20261003T140738Z-ROWS` | started by 'max-rows 1: a query answering 2 rows is recorded unread ('more than 1 rows') -> NEEDS_MANUAL_REVIEW' |
