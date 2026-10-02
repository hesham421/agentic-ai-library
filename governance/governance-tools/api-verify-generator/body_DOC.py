
# DOC — Document Access: API-DOC-001 (read-only; the upload handover is in-process, ADR-DOC-011,
# reached over HTTP only through Host Integration's API-INT-002).
# Stage-B order: registry read (a manual service) → start a Check (API-INT-001) → upload (API-INT-002)
# → list its Uploaded Documents (API-DOC-001).

EXTRA_ARGS = [
    (["--unknown-check-id"], {"type": int, "default": 99999,
                              "help": "an absent Check identifier (TC-INT-008 test data); verified absent first"}),
    (["--employee-id"], {"default": "E-3307", "help": "employeeId sent on a start (TC-INT-025/026 test data)"}),
]


def preflight(r: Runner, args, c: Contract):
    """Covers: A0 — a manual-fetch service (uploads exist only for a manual Check) and the unknown checkId."""
    services, call = available_services(r)
    precheck_services(r, services, call, "the registry's services", "test_uploaded_documents happy path")
    manual = [s for s in (services or []) if s.get("fetchMode") == "manual" and s.get("requiredDocumentTypes")]
    r.precondition("an available service with fetchMode manual and required document types",
                   "API-DOC-001 happy path — a Check with uploads is created through API-INT-001 + API-INT-002",
                   "API-REG-001 GET /api/v1/services", bool(manual),
                   f"{len(services or [])} available services, none manual — package directory empty",
                   "list the Uploaded Documents of a Check (200)")
    unknown_ok = absent_check(r, args.unknown_check_id, "TC-INT-008 test data (an unknown Check)",
                              "unknown-checkId observation")
    r.unexercisable("API-DOC-001", "DOC-500", "an unexpected server failure cannot be produced through documented "
                                              "endpoints or Dev/Test fixtures")
    return {"manual": manual[0] if manual else None, "unknown": args.unknown_check_id if unknown_ok else None,
            "employee": args.employee_id}


def test_uploaded_documents(r: Runner, ctx):
    """Covers: API-DOC-001 ; Negative: PLATFORM-STD / DOC-400-CHECK-ID-REQUIRED"""
    p = path_of(r, "API-DOC-001")
    svc = ctx["manual"]
    if svc is None:
        for item in ("list the Uploaded Documents of a fresh Check — empty array",
                     "list the Uploaded Documents after one upload — one entry, upload order"):
            r.block(item, "API-DOC-001", "no available manual-fetch service in the registry (package directory "
                                         "empty) — no Check with uploads can be created through API-INT-001/002")
    else:
        call, rn = start_check(r, svc, ctx["employee"], "doc")
        if call.status != 202 or not isinstance(call.body, dict):
            r.block("list the Uploaded Documents of a Check", "API-DOC-001",
                    f"DEPENDENCY_FAILURE — setup API-INT-001 answered HTTP {call.status} code={call.code}")
        else:
            cid = call.body["checkId"]
            c0 = r.http.get(p, params={"checkId": cid})
            r.expect_success("list the Uploaded Documents of a fresh Check", "API-DOC-001", c0, checks=[
                ("empty when none", c0.body == [], c0.safe_body())])
            dtype = svc["requiredDocumentTypes"][0]
            up = upload(r, cid, dtype, service_code=svc["serviceCode"], request_number=rn)
            if up.status != 201:
                r.block("list the Uploaded Documents after one upload", "API-DOC-001",
                        f"DEPENDENCY_FAILURE — setup API-INT-002 answered HTTP {up.status} code={up.code}")
            else:
                c1 = r.http.get(p, params={"checkId": cid})
                rows = c1.body if isinstance(c1.body, list) else []
                r.expect_success("list the Uploaded Documents after one upload", "API-DOC-001", c1, checks=[
                    ("exactly one entry", len(rows) == 1, f"{len(rows)} entries"),
                    ("the entry is the threaded upload", bool(rows) and rows[0].get("uploadedDocumentId") == up.body.get("uploadedDocumentId")
                     and rows[0].get("documentType") == dtype, c1.safe_body()),
                ])

    # stage C — error-catalog PLATFORM-STD: the checkId query parameter is missing or not a number
    r.expect_problem("checkId missing", "API-DOC-001", r.http.get(p), 400, "DOC-400-CHECK-ID-REQUIRED",
                     "API-DOC-001 ; PLATFORM-STD / DOC-400-CHECK-ID-REQUIRED")
    r.expect_problem("checkId not a number", "API-DOC-001", r.http.get(p, params={"checkId": "abc"}), 400,
                     "DOC-400-CHECK-ID-REQUIRED", "API-DOC-001 ; PLATFORM-STD / DOC-400-CHECK-ID-REQUIRED")
    r.untraced_negative("API-DOC-001", "DOC-400-CHECK-ID-REQUIRED")

    # stage E
    if ctx["unknown"] is not None:
        r.observe("unknown checkId", "API-DOC-001 ; unknown checkId", r.http.get(p, params={"checkId": ctx["unknown"]}),
                  "the API document declares no 404 for this read")
    r.observe("checkId larger than int64", "API-DOC-001 ; format int64", r.http.get(p, params={"checkId": OVERFLOW_ID}),
              "no documented outcome for an out-of-range number")
    r.observe("checkId present but empty", "API-DOC-001 ; required", r.http.get(p, params={"checkId": ""}),
              "blank value — 'missing' or 'not a number' either way")


SUITES = [test_uploaded_documents]
