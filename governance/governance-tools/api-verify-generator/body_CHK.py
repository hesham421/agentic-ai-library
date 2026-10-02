
# CHK — Check Engine: API-CHK-001 (read-only; start / confirm are in-process, ADR-CHK-017, reached over
# HTTP through Host Integration's API-INT-001 / API-INT-003).
# Stage-B order: registry read → start a Check (API-INT-001) → read its Active Check → [manual: confirm
# (API-INT-003) → read again] → poll the Check's read (API-RPT-001) to its end → Active Check gone.

EXTRA_ARGS = [
    (["--unknown-check-id"], {"type": int, "default": 99999,
                              "help": "an absent Check identifier (TC-INT-008 test data); verified absent first"}),
    (["--employee-id"], {"default": "E-3307", "help": "employeeId sent on a start (TC-INT-025/026 test data)"}),
]


def preflight(r: Runner, args, c: Contract):
    """Covers: A0 — an available service to start a Check of (TC-CHK-083 uses a manual one), the unknown checkId."""
    services, call = available_services(r)
    precheck_services(r, services, call, "the registry's services", "test_active_checks happy path")
    manual = [s for s in (services or []) if s.get("fetchMode") == "manual"]
    chosen = manual[0] if manual else ((services or [None])[0])
    r.precondition("an available service (manual preferred — TC-CHK-083)", "API-CHK-001 happy path — an Active "
                   "Check exists only for a Check started through API-INT-001", "API-REG-001 GET /api/v1/services",
                   chosen is not None, "the registry holds no service — package directory empty",
                   "read an Active Check (TC-CHK-083, TC-CHK-084, TC-CHK-085 ended half)")
    unknown_ok = absent_check(r, args.unknown_check_id, "TC-INT-008 test data (an unknown Check)",
                              "CHK-404-ACTIVE-CHECK-NOT-FOUND (does-not-exist half)")
    r.unexercisable("API-CHK-001", "CHK-500", "an unexpected server failure cannot be produced through documented "
                                              "endpoints or Dev/Test fixtures")
    return {"service": chosen, "unknown": args.unknown_check_id if unknown_ok else None,
            "employee": args.employee_id, "timeout": args.check_timeout}


def test_active_checks(r: Runner, ctx):
    """Covers: API-CHK-001 ; Negative: PLATFORM-STD / CHK-400-CHECK-ID-INVALID ;
    PLATFORM-STD / CHK-404-ACTIVE-CHECK-NOT-FOUND / TC-CHK-085"""
    svc = ctx["service"]
    if svc is None:
        for item, tc in (("Active Check of a new Check — 200 with status and deadline", "TC-CHK-083"),
                         ("Active Check after confirmation — RUNNING", "TC-CHK-084"),
                         ("ended Check has no Active Check — 404", "TC-CHK-085")):
            r.block(item, f"API-CHK-001 ; {tc}", "no available service in the registry (package directory empty) — "
                                                 "no Check can be started through API-INT-001")
    else:
        call, rn = start_check(r, svc, ctx["employee"], "chk")
        if call.status != 202 or not isinstance(call.body, dict):
            r.block("read an Active Check", "API-CHK-001", f"DEPENDENCY_FAILURE — setup API-INT-001 answered "
                                                           f"HTTP {call.status} code={call.code}")
        else:
            cid, started_status = call.body["checkId"], call.body.get("status")
            a = r.http.get(path_of(r, "API-CHK-001", checkId=cid))
            if a.status == 404 and started_status == "RUNNING":
                r.observe("Active Check right after a RUNNING start", "API-CHK-001", a,
                          "the Check may already have ended (asynchronous pipeline)")
            else:
                r.expect_success("Active Check of the new Check", "API-CHK-001", a, "API-CHK-001 ; TC-CHK-083", checks=[
                    ("checkId is the threaded id", isinstance(a.body, dict) and a.body.get("checkId") == cid, a.safe_body()),
                    ("holds only identifier, status and deadline (TC-CHK-086)",
                     isinstance(a.body, dict) and set(a.body) <= {"checkId", "checkStatus", "deadlineAt"}, a.safe_body()),
                ])
            if started_status == "AWAITING_DOCUMENTS":
                conf = r.http.post(path_of(r, "API-INT-003", checkId=cid), json_body={})
                if conf.status != 202:
                    r.block("Active Check after confirmation — RUNNING", "API-CHK-001 ; TC-CHK-084",
                            f"DEPENDENCY_FAILURE — setup API-INT-003 answered HTTP {conf.status} code={conf.code}")
                else:
                    a2 = r.http.get(path_of(r, "API-CHK-001", checkId=cid))
                    if a2.status == 404:
                        r.observe("Active Check right after confirmation", "API-CHK-001 ; TC-CHK-084", a2,
                                  "the Check may already have ended (asynchronous pipeline)")
                    else:
                        r.expect_success("Active Check after confirmation", "API-CHK-001", a2, "API-CHK-001 ; TC-CHK-084",
                                         checks=[("checkStatus RUNNING", isinstance(a2.body, dict) and a2.body.get("checkStatus") == "RUNNING",
                                                  a2.safe_body())])
            last, ended = wait_until_ended(r, cid, ctx["timeout"])
            if not ended:
                r.block("ended Check has no Active Check — 404", "API-CHK-001 ; TC-CHK-085",
                        f"the Check did not reach COMPLETED/FAILED within {ctx['timeout']}s (last: HTTP {last.status} "
                        f"{(last.body or {}).get('status') if isinstance(last.body, dict) else last.error})")
            else:
                r.expect_problem("ended Check has no Active Check", "API-CHK-001",
                                 r.http.get(path_of(r, "API-CHK-001", checkId=cid)), 404, "CHK-404-ACTIVE-CHECK-NOT-FOUND",
                                 "API-CHK-001 ; PLATFORM-STD / CHK-404-ACTIVE-CHECK-NOT-FOUND / TC-CHK-085")

    # stage C
    r.expect_problem("checkId not a number", "API-CHK-001", r.http.get(path_of(r, "API-CHK-001", checkId="abc")), 400,
                     "CHK-400-CHECK-ID-INVALID", "API-CHK-001 ; PLATFORM-STD / CHK-400-CHECK-ID-INVALID")
    r.untraced_negative("API-CHK-001", "CHK-400-CHECK-ID-INVALID")
    if ctx["unknown"] is not None:
        r.expect_problem("unknown Check has no Active Check", "API-CHK-001",
                         r.http.get(path_of(r, "API-CHK-001", checkId=ctx["unknown"])), 404, "CHK-404-ACTIVE-CHECK-NOT-FOUND",
                         "API-CHK-001 ; PLATFORM-STD / CHK-404-ACTIVE-CHECK-NOT-FOUND / TC-CHK-085 (does-not-exist half)")
    else:
        r.block("unknown Check → 404", "API-CHK-001 ; CHK-404-ACTIVE-CHECK-NOT-FOUND", "the unknown checkId exists")

    # stage E
    r.observe("checkId larger than int64", "API-CHK-001 ; format int64",
              r.http.get(path_of(r, "API-CHK-001", checkId=OVERFLOW_ID)), "no documented outcome for an out-of-range number")
    r.observe("negative checkId", "API-CHK-001 ; format int64", r.http.get(path_of(r, "API-CHK-001", checkId="-1")),
              "a number, but never a Check identifier")


SUITES = [test_active_checks]
