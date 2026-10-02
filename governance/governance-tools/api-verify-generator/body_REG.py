
# REG — Service Registry: API-REG-001 … API-REG-003 (read-only, ADR-REG-007).
# Stage-B order: registry reads only — list → read one (code threaded from the list) → load report.

EXTRA_ARGS = []

UNKNOWN_SERVICE = "unknown-service"   # TC-REG-016 test data


def preflight(r: Runner, args, c: Contract):
    """Covers: API-REG-001 (A0 — what the registry holds decides which reads can be asserted)."""
    services, call = available_services(r)
    precheck_services(r, services, call, "the registry's services", "test_services")
    listed = {s.get("serviceCode") for s in (call.body if isinstance(call.body, list) else []) if isinstance(s, dict)}
    r.precondition("at least one available service", "TC-REG-014 / TC-REG-015 (fixture packages, ADR-REG-007)",
                   "API-REG-001 GET /api/v1/services", bool(services),
                   "the registry holds no service — the package directory (aias.registry.package-directory) is empty",
                   "read one service (TC-REG-015, TC-REG-075)")
    unknown_ok = r.precondition(UNKNOWN_SERVICE, "TC-REG-016 test data", "absent from API-REG-001",
                                UNKNOWN_SERVICE not in listed, "held by the registry", "TC-REG-016")
    for api_id in ("API-REG-001", "API-REG-002", "API-REG-003"):
        r.unexercisable(api_id, "REG-500", "an unexpected server failure cannot be produced through documented "
                                           "endpoints or Dev/Test fixtures")
    return {"services": services or [], "unknown_ok": unknown_ok}


def test_services(r: Runner, ctx):
    """Covers: API-REG-001, API-REG-002 ; Negative: RULE-REG-016 / REG-404-SERVICE-NOT-FOUND / TC-REG-016"""
    call = r.http.get(path_of(r, "API-REG-001"))
    ok, body = r.expect_success("list services", "API-REG-001", call, "API-REG-001 ; TC-REG-014", checks=[
        ("every listed service is available (TC-REG-014)",
         isinstance(call.body, list) and all(isinstance(s, dict) and s.get("available") is True for s in call.body),
         "a listed row has available != true"),
        ("no row carries a property the API document does not declare (TC-REG-014)",
         isinstance(call.body, list) and all(set(s) <= r.c.top_fields(r.c.response_schema("API-REG-001", 200, "application/json"))
                                             for s in call.body if isinstance(s, dict)),
         "a row carries an undeclared property (SQL or connection field?)"),
    ])
    services = body if isinstance(body, list) else []
    if not services:
        r.block("TC-REG-014 expected row count (2 available, 1 withdrawn fixture services)", "API-REG-001 ; TC-REG-014",
                "the registry holds no service: the package directory is empty (Dev/Test fixture folders absent)")
        r.block("read one service — 200 with its current version (TC-REG-015)", "API-REG-002 ; TC-REG-015",
                "no service code to thread from API-REG-001 (registry empty)")
        r.block("read one service case-insensitively (TC-REG-075)", "API-REG-002 ; TC-REG-075",
                "no service code to thread from API-REG-001 (registry empty)")
    else:
        first = services[0]
        code = first["serviceCode"]
        call = r.http.get(path_of(r, "API-REG-002", serviceCode=code))
        r.expect_success("read one service", "API-REG-002", call, "API-REG-002 ; TC-REG-015", checks=[
            ("serviceCode is the threaded code", isinstance(call.body, dict) and call.body.get("serviceCode") == code,
             f"serviceCode={(call.body or {}).get('serviceCode') if isinstance(call.body, dict) else None}"),
            ("the read equals the listed summary", call.body == first, "read and list disagree"),
        ])
        call = r.http.get(path_of(r, "API-REG-002", serviceCode=code.upper()))
        r.expect_success("read one service with the code upper-cased (REQ-REG-064)", "API-REG-002", call,
                         "API-REG-002 ; TC-REG-075", checks=[
            ("canonical lower-case serviceCode", isinstance(call.body, dict) and call.body.get("serviceCode") == code,
             "serviceCode not canonical")])

    # stage C — RULE-REG-016 (error-catalog: the service code is not held by the registry)
    if ctx["unknown_ok"]:
        call = r.http.get(path_of(r, "API-REG-002", serviceCode=UNKNOWN_SERVICE))
        r.expect_problem("read an unknown service", "API-REG-002", call, 404, "REG-404-SERVICE-NOT-FOUND",
                         "API-REG-002 ; RULE-REG-016 / REG-404-SERVICE-NOT-FOUND / TC-REG-016")
    else:
        r.block("read an unknown service → 404", "API-REG-002 ; TC-REG-016", f"{UNKNOWN_SERVICE!r} is held by the registry")

    # stage E — serviceCode maxLength 100 (API document); trimmed matching (REQ-REG-064)
    r.observe("unknown service code of exactly 100 characters (at limit)", "API-REG-002 ; maxLength 100",
              r.http.get(path_of(r, "API-REG-002", serviceCode="a" * 100)), "at-limit unknown code")
    r.observe("service code of 101 characters (one over)", "API-REG-002 ; maxLength 100",
              r.http.get(path_of(r, "API-REG-002", serviceCode="a" * 101)),
              "the API document states no outcome for an over-length code")
    r.observe("unknown code with surrounding spaces", "API-REG-002 ; REQ-REG-064 (trimmed)",
              r.http.get(path_of(r, "API-REG-002", serviceCode=f" {UNKNOWN_SERVICE} ")), "trimmed, still unknown")


def test_load_results(r: Runner, ctx):
    """Covers: API-REG-003 ; TC-REG-008"""
    call = r.http.get(path_of(r, "API-REG-003"))
    ok, body = r.expect_success("read the load report", "API-REG-003", call, "API-REG-003 ; TC-REG-008")
    if isinstance(body, list) and not body:
        r.block("TC-REG-008 rows of the latest run (1 REGISTERED + 1 REJECTED)", "API-REG-003 ; TC-REG-008",
                "the latest load run saw no package folder (package directory empty) — fixture folders absent")
        r.observe("rows of the latest load run", "API-REG-003 ; REQ-REG-066", call,
                  "0 rows. REQ-REG-066 asks for a PACKAGE_DIRECTORY REJECTED row only while the registry holds an "
                  "available Service Package — it holds none, so an empty report is consistent")


SUITES = [test_services, test_load_results]
