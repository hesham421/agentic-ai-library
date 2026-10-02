
# RPT — Report Store: API-RPT-001 … API-RPT-003 (read-only; the result port and the Employee Decision are
# in-process, ADR-RPT-006 — a Check is created over HTTP only through Host Integration's API-INT-001).
# Stage-B order: registry read → start a Check (API-INT-001) → read it → poll to its end → read again →
# list the Checks of its request → decision agreement of its service.

EXTRA_ARGS = [
    (["--unknown-check-id"], {"type": int, "default": 998, "help": "an absent Check identifier (TC-RPT-030 test data)"}),
    (["--employee-id"], {"default": "E-3307", "help": "employeeId sent on a start (TC-INT-025/026 test data)"}),
]

LIST_REQUEST_NUMBER = "1001"   # TC-RPT-035 test data


def preflight(r: Runner, args, c: Contract):
    """Covers: A0 — an available service to start a Check of."""
    services, call = available_services(r)
    precheck_services(r, services, call, "the registry's services", "API-RPT-001/002 happy paths")
    chosen = (services or [None])[0]
    r.precondition("an available service", "API-RPT-001 / API-RPT-002 happy paths — a Check is created only "
                   "through API-INT-001", "API-REG-001 GET /api/v1/services", chosen is not None,
                   "the registry holds no service — package directory empty",
                   "read a Check (TC-RPT-027/028/031), list the Checks of its request (TC-RPT-033)")
    r.precondition(args.unknown_check_id, "TC-RPT-030 test data (no Check run 998)",
                   "asserted by TC-RPT-030 itself (API-RPT-001 is the read that would show it)", True, "", "TC-RPT-030")
    for api_id in ("API-RPT-001", "API-RPT-002", "API-RPT-003"):
        r.unexercisable(api_id, "RPT-500", "an unexpected server failure cannot be produced through documented "
                                           "endpoints or Dev/Test fixtures")
    return {"service": chosen, "unknown": args.unknown_check_id, "employee": args.employee_id,
            "timeout": args.check_timeout, "check": None}


def test_checks(r: Runner, ctx):
    """Covers: API-RPT-001, API-RPT-002 ; Negative: PLATFORM-STD / RPT-400-CHECK-ID-INVALID ;
    PLATFORM-STD / RPT-404-CHECK-NOT-FOUND / TC-RPT-030 ; RULE-RPT-009 / RPT-400-REQUEST-KEYS-MISSING / TC-RPT-035"""
    svc = ctx["service"]
    if svc is None:
        for item, tc in (("read a running Check — no report yet", "API-RPT-001 ; TC-RPT-027"),
                         ("read an ended Check — report or failure reason", "API-RPT-001 ; TC-RPT-028 / TC-RPT-031"),
                         ("list the Checks of a request — the created Check", "API-RPT-002 ; TC-RPT-033")):
            r.block(item, tc, "no available service in the registry (package directory empty) — no Check can be "
                              "started through API-INT-001")
    else:
        call, rn = start_check(r, svc, ctx["employee"], "rpt")
        if call.status != 202 or not isinstance(call.body, dict):
            r.block("read a Check", "API-RPT-001", f"DEPENDENCY_FAILURE — setup API-INT-001 answered HTTP {call.status} "
                                                   f"code={call.code}")
        else:
            cid = call.body["checkId"]
            ctx["check"] = (cid, svc["serviceCode"], rn)
            c0 = read_check(r, cid)
            r.expect_success("read the new Check", "API-RPT-001", c0, "API-RPT-001 ; TC-RPT-027", checks=[
                ("identifiers exactly as sent", isinstance(c0.body, dict) and c0.body.get("checkId") == cid
                 and c0.body.get("serviceCode") == svc["serviceCode"] and c0.body.get("requestNumber") == rn
                 and c0.body.get("employeeId") == ctx["employee"], c0.safe_body())])
            last, ended = wait_until_ended(r, cid, ctx["timeout"])
            if not ended:
                r.block("read an ended Check", "API-RPT-001 ; TC-RPT-028 / TC-RPT-031",
                        f"the Check did not end within {ctx['timeout']}s (status "
                        f"{(last.body or {}).get('status') if isinstance(last.body, dict) else last.error})")
            else:
                b = last.body
                r.expect_success("read the ended Check", "API-RPT-001", last, "API-RPT-001 ; TC-RPT-028 / TC-RPT-031", checks=[
                    ("COMPLETED carries an Overall Status; FAILED a failure reason",
                     (b.get("status") == "COMPLETED" and b.get("overallStatus") is not None) or
                     (b.get("status") == "FAILED" and b.get("failureReason") is not None),
                     f"status={b.get('status')} overallStatus={b.get('overallStatus')} failureReason={b.get('failureReason')}")])
            lst = r.http.get(path_of(r, "API-RPT-002"), params={"serviceCode": svc["serviceCode"], "requestNumber": rn})
            r.expect_success("list the Checks of the run's request", "API-RPT-002", lst, "API-RPT-002 ; TC-RPT-033", checks=[
                ("total 1 and the created Check listed", isinstance(lst.body, dict) and lst.body.get("total") == 1
                 and [x.get("checkId") for x in lst.body.get("checks", [])] == [cid], lst.safe_body())])

    # a request no Check was made for: the run's own namespace
    keys = {"serviceCode": f"{r.ns}-SVC-A", "requestNumber": f"{r.ns}-REQ"}   # placeholders ⟨SVC-A⟩ (TC-RPT-033)
    lst = r.http.get(path_of(r, "API-RPT-002"), params=keys)
    r.expect_success("list the Checks of a request with none", "API-RPT-002", lst, "API-RPT-002 ; TC-RPT-033 (shape)", checks=[
        ("total 0, no Check", isinstance(lst.body, dict) and lst.body.get("total") == 0 and lst.body.get("checks") == [],
         lst.safe_body())])

    # stage C
    r.expect_problem("checkId not a number", "API-RPT-001", r.http.get(path_of(r, "API-RPT-001", checkId="abc")), 400,
                     "RPT-400-CHECK-ID-INVALID", "API-RPT-001 ; PLATFORM-STD / RPT-400-CHECK-ID-INVALID")
    r.untraced_negative("API-RPT-001", "RPT-400-CHECK-ID-INVALID")
    r.expect_problem("unknown Check", "API-RPT-001", read_check(r, ctx["unknown"]), 404, "RPT-404-CHECK-NOT-FOUND",
                     "API-RPT-001 ; PLATFORM-STD / RPT-404-CHECK-NOT-FOUND / TC-RPT-030")
    lp = path_of(r, "API-RPT-002")
    tag = "API-RPT-002 ; RULE-RPT-009 / RPT-400-REQUEST-KEYS-MISSING / TC-RPT-035"
    r.expect_problem("list without a service code", "API-RPT-002",
                     r.http.get(lp, params={"requestNumber": LIST_REQUEST_NUMBER}), 400, "RPT-400-REQUEST-KEYS-MISSING", tag)
    r.expect_problem("list without a request number", "API-RPT-002",
                     r.http.get(lp, params={"serviceCode": keys["serviceCode"]}), 400, "RPT-400-REQUEST-KEYS-MISSING", tag)
    r.expect_problem("list with a blank service code", "API-RPT-002",
                     r.http.get(lp, params={"serviceCode": " ", "requestNumber": LIST_REQUEST_NUMBER}), 400,
                     "RPT-400-REQUEST-KEYS-MISSING", tag)
    r.expect_problem("list with a blank request number", "API-RPT-002",
                     r.http.get(lp, params={"serviceCode": keys["serviceCode"], "requestNumber": " "}), 400,
                     "RPT-400-REQUEST-KEYS-MISSING", tag)

    # stage E
    r.observe("checkId larger than int64", "API-RPT-001 ; format int64",
              read_check(r, OVERFLOW_ID), "no documented outcome for an out-of-range number")
    r.observe("service code of 101 characters", "API-RPT-002 ; maxLength 100",
              r.http.get(lp, params={"serviceCode": "a" * 101, "requestNumber": keys["requestNumber"]}),
              "the API document states no outcome for an over-length key")


def test_decision_agreement(r: Runner, ctx):
    """Covers: API-RPT-003 ; Negative: RULE-RPT-015 / RPT-400-SERVICE-CODE-MISSING / TC-RPT-049"""
    p = path_of(r, "API-RPT-003")
    none = r.http.get(p, params={"serviceCode": f"{r.ns}-SVC-B"})          # placeholder ⟨SVC-B⟩ (TC-RPT-048)
    r.expect_success("agreement of a service with no decision", "API-RPT-003", none, "API-RPT-003 ; TC-RPT-048", checks=[
        ("empty array", none.body == [], none.safe_body())])
    if ctx["service"] is not None:
        real = r.http.get(p, params={"serviceCode": ctx["service"]["serviceCode"]})
        r.expect_success("agreement of a registered service", "API-RPT-003", real, "API-RPT-003 ; TC-RPT-047 (shape)")
    else:
        r.block("agreement counted per version for a decided service", "API-RPT-003 ; TC-RPT-047",
                "no service and no decided Check — a decision is recorded only through API-INT-004 on a COMPLETED Check")
    tag = "API-RPT-003 ; RULE-RPT-015 / RPT-400-SERVICE-CODE-MISSING / TC-RPT-049"
    r.expect_problem("agreement without a service code", "API-RPT-003", r.http.get(p), 400, "RPT-400-SERVICE-CODE-MISSING", tag)
    r.expect_problem("agreement with a blank service code", "API-RPT-003", r.http.get(p, params={"serviceCode": " "}), 400,
                     "RPT-400-SERVICE-CODE-MISSING", tag)
    r.observe("service code of 101 characters", "API-RPT-003 ; maxLength 100",
              r.http.get(p, params={"serviceCode": "a" * 101}), "the API document states no outcome for an over-length code")


SUITES = [test_checks, test_decision_agreement]
