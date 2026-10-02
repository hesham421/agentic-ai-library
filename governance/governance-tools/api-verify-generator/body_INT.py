
# INT — Host Integration: API-INT-001 … API-INT-008 (four writes, four relayed reads; owners' refusals keep
# their own codes, ADR-INT-003).
# Stage-B order: registry reads → start a Check (001) → its documents (002 upload, 007 list) →
# confirmation (003) → its report (005 read, 006 list; async — polled) → required document types (008) →
# its decision (004).

EXTRA_ARGS = [
    (["--employee-id"], {"default": "E-3307", "help": "employeeId sent on a start (TC-INT-025/026 test data)"}),
    (["--decided-by"], {"default": "E-1001", "help": "decidedBy sent on a decision (TC-INT-099 test data)"}),
    (["--upload-request-limit-mb"], {"type": int, "default": 50,
                                     "help": "aias.integration.upload.request-limit in MB (TC-INT-009: 50 MB)"}),
    (["--max-uploads"], {"type": int, "default": 20, "help": "aias.check.max-uploads (TC-INT-097: 20)"}),
]

OLD_SERVICE = "old-service"          # TC-INT-001 test data (an unknown or withdrawn service)
UNKNOWN_UPLOAD_CHECK = 99999         # TC-INT-008 test data
UNKNOWN_REPORT_CHECK = 99998         # TC-INT-088 test data
UNKNOWN_TYPES_CHECK = 99997          # TC-INT-094 test data
UPLOAD_DOC_TYPE = "TRANSCRIPT"       # TC-INT-008 test data (refused before any document-type check)
FOREIGN_DOC_TYPE = "PASSPORT"        # TC-INT-002 test data (a type the service does not require)
LIST_REQUEST_NUMBER = "1001"         # TC-INT-090 test data


def preflight(r: Runner, args, c: Contract):
    """Covers: A0 — services to start Checks of, the test plan's unknown Check ids, the unknown service code."""
    services, call = available_services(r)
    precheck_services(r, services, call, "the registry's services", "every happy path")
    services = services or []
    non_manual = [s for s in services if s.get("fetchMode") in ("path", "blob")]
    manual = [s for s in services if s.get("fetchMode") == "manual" and s.get("requiredDocumentTypes")]
    r.precondition("an available service with fetchMode path or blob", "TC-INT-025 (scholarship-request, path)",
                   "API-REG-001 GET /api/v1/services", bool(non_manual),
                   f"{len(services)} available services — package directory empty",
                   "start → RUNNING (TC-INT-025), upload to a running Check (TC-INT-007), confirmation refused "
                   "(TC-INT-004), report read / list (TC-INT-087/089), decision (TC-INT-035/003)")
    r.precondition("an available service with fetchMode manual and required document types",
                   "TC-INT-026 (manual-service, manual)", "API-REG-001 GET /api/v1/services", bool(manual),
                   f"{len(services)} available services — package directory empty",
                   "start → AWAITING_DOCUMENTS (TC-INT-026), uploads (TC-INT-031/091/092/002/009/097), confirmation "
                   "(TC-INT-034), required document types (TC-INT-093)")
    old = r.http.get(path_of(r, "API-REG-002", serviceCode=OLD_SERVICE))
    old_ok = (old.status == 404) or (old.status == 200 and isinstance(old.body, dict) and old.body.get("available") is False)
    r.precondition(OLD_SERVICE, "TC-INT-001 test data (an unknown or withdrawn service — error-catalog trigger)",
                   "API-REG-002 GET /api/v1/services/{serviceCode} → 404 or available=false", old_ok,
                   f"HTTP {old.status} — the service is available", "TC-INT-001")
    unknown = {}
    for cid, tc in ((UNKNOWN_UPLOAD_CHECK, "TC-INT-008"), (UNKNOWN_REPORT_CHECK, "TC-INT-088"),
                    (UNKNOWN_TYPES_CHECK, "TC-INT-094")):
        unknown[tc] = cid if absent_check(r, cid, f"{tc} test data (an unknown Check)", tc) else None
    r.precondition(UPLOAD_DOC_TYPE, "TC-INT-008 test data", "not checked against the registry: the refusal under test "
                   "(no such Check) comes before any document-type check — TC-INT-008 names only the absent Check", True,
                   "", "—")

    # not exercisable through documented endpoints / Dev/Test fixtures (stage C)
    for api_id in sorted(c.ops):
        r.unexercisable(api_id, "INT-500", "an unexpected server failure (TC-INT-006: a Report Store database outage) "
                                           "cannot be produced through documented endpoints", "TC-INT-006" if api_id == "API-INT-004" else "")
    r.unexercisable("API-INT-001", "CHK-422-CONNECTION-NOT-ACTIVATED", "needs a service package whose connection is not "
                    "activated — package and connection configuration, which the run never changes")
    r.unexercisable("API-INT-002", "DOC-404-SERVICE-VERSION-NOT-FOUND", "needs the Check's service package version to "
                    "vanish from the registry — not producible through documented endpoints")
    r.unexercisable("API-INT-002", "DOC-409-CHECK-ENDED", "needs Document Access to hold the Check as ended while the "
                    "Check Engine still reports AWAITING_DOCUMENTS (a race) — not producible deterministically", "TC-INT-096")
    r.unexercisable("API-INT-002", "DOC-422-FETCH-MODE-NOT-MANUAL", "a Check of a non-manual service never waits for "
                    "documents, so RULE-INT-001 (INT-409-CHECK-NOT-AWAITING-DOCUMENTS) answers before Document Access is reached")
    r.unexercisable("API-INT-004", "RPT-422-APPROVAL-FLAG-ON-REJECTION", "Host Integration never sends an executed "
                    "rejection (REQ-INT-027, the API document's own words)")
    stub = args.approval_stub_url
    for code, tc in (("INT-502-APPROVAL-API-FAILED", "TC-INT-019"), ("INT-504-APPROVAL-API-TIMED-OUT", "TC-INT-020")):
        r.unexercisable("API-INT-004", code, "needs an approval-enabled service package whose Approval API endpoint is a "
                        "Dev/Test stub" + ("" if stub else " — no --approval-stub-url given") + "; the run never changes a "
                        "package and never reaches a real host (§3-I)", tc)
    return {"path_svc": non_manual[0] if non_manual else None, "manual_svc": manual[0] if manual else None,
            "old_ok": old_ok, "unknown": unknown, "employee": args.employee_id, "decided_by": args.decided_by,
            "timeout": args.check_timeout, "limit_mb": args.upload_request_limit_mb, "max_uploads": args.max_uploads,
            "path": None, "manual": None}


def _no_service(r, items, which):
    for item, covers in items:
        r.block(item, covers, f"no available {which} service in the registry (package directory empty)")


def test_checks(r: Runner, ctx):
    """Covers: API-INT-001 ; Negative: PLATFORM-STD / INT-400-REQUEST-INVALID ; PASS-THROUGH /
    CHK-400-START-INCOMPLETE ; PASS-THROUGH / CHK-422-SERVICE-NOT-AVAILABLE / TC-INT-001"""
    p = path_of(r, "API-INT-001")
    for key, svc, label, want, tc in (("path", ctx["path_svc"], "int-path", "RUNNING", "TC-INT-025"),
                                      ("manual", ctx["manual_svc"], "int-manual", "AWAITING_DOCUMENTS", "TC-INT-026")):
        if svc is None:
            _no_service(r, [(f"start a Check → 202 {want}", f"API-INT-001 ; {tc} / TC-INT-030")], key if key == "manual" else "path/blob")
            continue
        call, rn = start_check(r, svc, ctx["employee"], label)
        loc = call.headers.get("Location", "")
        ok, body = r.expect_success(f"start a Check of a {svc.get('fetchMode')} service", "API-INT-001", call,
                                    f"API-INT-001 ; {tc} / TC-INT-030", checks=[
            (f"status {want}", isinstance(call.body, dict) and call.body.get("status") == want, call.safe_body()),
            ("Location and checkUrl name the Check's read", isinstance(call.body, dict) and
             str(call.body.get("checkId")) in loc and str(call.body.get("checkId")) in str(call.body.get("checkUrl")),
             f"Location={loc!r} checkUrl={(call.body or {}).get('checkUrl') if isinstance(call.body, dict) else None}"),
        ])
        if isinstance(call.body, dict) and call.status == 202:
            ctx[key] = {"id": call.body["checkId"], "svc": svc, "rn": rn, "status": call.body.get("status")}

    # stage C
    r.expect_problem("start with an unreadable body", "API-INT-001",
                     r.http.post(p, raw='{"serviceCode": ', headers={"Content-Type": "application/json"}), 400,
                     "INT-400-REQUEST-INVALID", "API-INT-001 ; PLATFORM-STD / INT-400-REQUEST-INVALID")
    r.untraced_negative("API-INT-001", "INT-400-REQUEST-INVALID")
    r.expect_problem("start without a service code", "API-INT-001",
                     r.http.post(p, json_body={"requestNumber": f"{r.ns}-incomplete", "employeeId": ctx["employee"]}), 400,
                     "CHK-400-START-INCOMPLETE", "API-INT-001 ; PASS-THROUGH / CHK-400-START-INCOMPLETE")
    r.untraced_negative("API-INT-001", "CHK-400-START-INCOMPLETE")
    if ctx["old_ok"]:
        rn = f"{r.ns}-old"
        call = r.http.post(p, json_body={"serviceCode": OLD_SERVICE, "requestNumber": rn, "employeeId": ctx["employee"]})
        if call.status == 202 and isinstance(call.body, dict):
            r.track("Check", call.body.get("checkId"), OLD_SERVICE, rn, "API-INT-001 start that should have been refused")
        r.expect_problem("start for an unknown / withdrawn service", "API-INT-001", call, 422,
                         "CHK-422-SERVICE-NOT-AVAILABLE", "API-INT-001 ; PASS-THROUGH / CHK-422-SERVICE-NOT-AVAILABLE / TC-INT-001")
        lst = r.http.get(path_of(r, "API-RPT-002"), params={"serviceCode": OLD_SERVICE, "requestNumber": rn})
        r._add("start for an unknown / withdrawn service — no Check was created (TC-INT-001 step 2)",
               "API-INT-001 ; TC-INT-001", lst.status == 200 and isinstance(lst.body, dict) and lst.body.get("total") == 0,
               "API-RPT-002 total 0", lst, "likely real bug", "DATA_INTEGRITY_ISSUE",
               actual=None if (lst.status == 200 and isinstance(lst.body, dict) and lst.body.get("total") == 0) else lst.safe_body())
    else:
        r.block("start for an unknown / withdrawn service → 422", "API-INT-001 ; TC-INT-001", f"{OLD_SERVICE!r} is available")

    # stage E (refused starts only — nothing is created)
    r.observe("start with a text/plain body", "API-INT-001 ; requestBody application/json",
              r.http.post(p, raw="serviceCode=x", headers={"Content-Type": "text/plain"}),
              "no documented code for a wrong Content-Type (open gap 'POST /api/v1/checks')")
    r.observe("start with a 101-character request number (unknown service)", "API-INT-001 ; maxLength 100",
              r.http.post(p, json_body={"serviceCode": OLD_SERVICE, "requestNumber": f"{r.ns}-" + "x" * 101,
                                        "employeeId": ctx["employee"]}),
              "no INT code for an over-length value (open gap 'POST /api/v1/checks')")
    r.observe("start with a blank employee id (unknown service)", "API-INT-001 ; CHK-400-START-INCOMPLETE vs CHK-422",
              r.http.post(p, json_body={"serviceCode": OLD_SERVICE, "requestNumber": f"{r.ns}-blank", "employeeId": " "}),
              "two documented refusals apply; their order is not documented")


def test_check_documents(r: Runner, ctx):
    """Covers: API-INT-002, API-INT-007 ; Negative: PLATFORM-STD / INT-400-REQUEST-INVALID ;
    PASS-THROUGH / RPT-404-CHECK-NOT-FOUND / TC-INT-008 ; RULE-INT-001 / INT-409-CHECK-NOT-AWAITING-DOCUMENTS / TC-INT-007 ;
    PASS-THROUGH / DOC-422-DOCUMENT-TYPE-NOT-OF-SERVICE / TC-INT-002 ; PASS-THROUGH / DOC-400-INCOMPLETE-UPLOAD ;
    PLATFORM-STD / INT-413-UPLOAD-TOO-LARGE / TC-INT-009 ; PASS-THROUGH / DOC-422-UPLOAD-LIMIT-REACHED / TC-INT-097"""
    m = ctx["manual"]
    if m is None:
        _no_service(r, [("list the uploads of a fresh Check → []", "API-INT-007 ; TC-INT-092"),
                        ("upload a required document → 201", "API-INT-002 ; TC-INT-031"),
                        ("list after the upload → one entry", "API-INT-007 ; TC-INT-091"),
                        ("upload a type the service does not require → 422", "API-INT-002 ; TC-INT-002"),
                        ("upload an empty file → 400 DOC-400-INCOMPLETE-UPLOAD", "API-INT-002"),
                        ("upload above the request limit → 413", "API-INT-002 ; TC-INT-009"),
                        ("upload above the maximum uploads → 422", "API-INT-002 ; TC-INT-097")], "manual")
    else:
        cid, svc, rn = m["id"], m["svc"], m["rn"]
        lp = path_of(r, "API-INT-007", checkId=cid)
        c0 = r.http.get(lp)
        r.expect_success("list the uploads of a fresh Check", "API-INT-007", c0, "API-INT-007 ; TC-INT-092",
                         checks=[("empty", c0.body == [], c0.safe_body())])
        dtype = svc["requiredDocumentTypes"][0]
        up = upload(r, cid, dtype, service_code=svc["serviceCode"], request_number=rn)
        r.expect_success("upload a required document", "API-INT-002", up, "API-INT-002 ; TC-INT-031", checks=[
            ("documentType echoed, not oversized", isinstance(up.body, dict) and up.body.get("documentType") == dtype
             and up.body.get("oversized") is False, up.safe_body())])
        c1 = r.http.get(lp)
        rows = c1.body if isinstance(c1.body, list) else []
        r.expect_success("list after the upload", "API-INT-007", c1, "API-INT-007 ; TC-INT-091", checks=[
            ("one entry, the threaded upload", len(rows) == 1 and isinstance(up.body, dict)
             and rows[0].get("uploadedDocumentId") == up.body.get("uploadedDocumentId"), c1.safe_body())])
        if FOREIGN_DOC_TYPE in (svc.get("requiredDocumentTypes") or []):
            r.block("upload a type the service does not require", "API-INT-002 ; TC-INT-002",
                    f"{FOREIGN_DOC_TYPE!r} is a required type of {svc['serviceCode']}")
        else:
            r.expect_problem("upload a type the service does not require", "API-INT-002",
                             upload(r, cid, FOREIGN_DOC_TYPE, service_code=svc["serviceCode"], request_number=rn), 422,
                             "DOC-422-DOCUMENT-TYPE-NOT-OF-SERVICE",
                             "API-INT-002 ; PASS-THROUGH / DOC-422-DOCUMENT-TYPE-NOT-OF-SERVICE / TC-INT-002")
        r.expect_problem("upload an empty file", "API-INT-002",
                         upload(r, cid, dtype, content=b"", service_code=svc["serviceCode"], request_number=rn), 400,
                         "DOC-400-INCOMPLETE-UPLOAD", "API-INT-002 ; PASS-THROUGH / DOC-400-INCOMPLETE-UPLOAD")
        r.untraced_negative("API-INT-002", "DOC-400-INCOMPLETE-UPLOAD")
        big = b"\0" * ((ctx["limit_mb"] + 10) * 1024 * 1024)      # TC-INT-009: 60 MB against a 50 MB limit
        r.expect_problem("upload above the request limit", "API-INT-002",
                         upload(r, cid, dtype, content=big, service_code=svc["serviceCode"], request_number=rn), 413,
                         "INT-413-UPLOAD-TOO-LARGE", "API-INT-002 ; PLATFORM-STD / INT-413-UPLOAD-TOO-LARGE / TC-INT-009")
        del big
        held = len(r.http.get(lp).body or [])
        for _ in range(max(0, ctx["max_uploads"] - held)):
            if upload(r, cid, dtype, service_code=svc["serviceCode"], request_number=rn).status != 201:
                break
        r.expect_problem("upload above the maximum uploads per Check", "API-INT-002",
                         upload(r, cid, dtype, service_code=svc["serviceCode"], request_number=rn), 422,
                         "DOC-422-UPLOAD-LIMIT-REACHED", "API-INT-002 ; PASS-THROUGH / DOC-422-UPLOAD-LIMIT-REACHED / TC-INT-097")
        r.observe("upload without a documentType part", "API-INT-002 ; DOC-400-INCOMPLETE-UPLOAD vs INT-400",
                  r.http.post(path_of(r, "API-INT-002", checkId=cid),
                              files={"file": (f"{r.ns}-synthetic.txt", b"SYNTHETIC", "text/plain")}),
                  "two documented refusals could apply (a missing part / a missing document type)")
    pc = ctx["path"]
    if pc is None:
        _no_service(r, [("upload for a running Check → 409", "API-INT-002 ; TC-INT-007")], "path/blob")
    else:
        r.expect_problem("upload for a Check not waiting for documents", "API-INT-002",
                         upload(r, pc["id"], UPLOAD_DOC_TYPE, service_code=pc["svc"]["serviceCode"], request_number=pc["rn"]),
                         409, "INT-409-CHECK-NOT-AWAITING-DOCUMENTS",
                         "API-INT-002 ; RULE-INT-001 / INT-409-CHECK-NOT-AWAITING-DOCUMENTS / TC-INT-007")

    # stage C — no Check needed
    r.expect_problem("upload with a non-numeric checkId", "API-INT-002", upload(r, "abc", UPLOAD_DOC_TYPE), 400,
                     "INT-400-REQUEST-INVALID", "API-INT-002 ; PLATFORM-STD / INT-400-REQUEST-INVALID")
    r.untraced_negative("API-INT-002", "INT-400-REQUEST-INVALID")
    unk = ctx["unknown"]["TC-INT-008"]
    if unk is not None:
        r.expect_problem("upload for an unknown Check", "API-INT-002", upload(r, unk, UPLOAD_DOC_TYPE), 404,
                         "RPT-404-CHECK-NOT-FOUND", "API-INT-002 ; PASS-THROUGH / RPT-404-CHECK-NOT-FOUND / TC-INT-008")
    else:
        r.block("upload for an unknown Check → 404", "API-INT-002 ; TC-INT-008", f"Check {UNKNOWN_UPLOAD_CHECK} exists")
    r.expect_problem("list uploads with a non-numeric checkId", "API-INT-007",
                     r.http.get(path_of(r, "API-INT-007", checkId="abc")), 400, "INT-400-REQUEST-INVALID",
                     "API-INT-007 ; PLATFORM-STD / INT-400-REQUEST-INVALID")
    r.untraced_negative("API-INT-007", "INT-400-REQUEST-INVALID")
    if unk is not None:
        r.observe("list uploads of an unknown Check", "API-INT-007 ; unknown checkId",
                  r.http.get(path_of(r, "API-INT-007", checkId=unk)), "the API document declares no 404 for this read")
        r.observe("upload for an unknown Check without a file part", "API-INT-002 ; INT-400 vs RPT-404",
                  r.http.post(path_of(r, "API-INT-002", checkId=unk), data={"documentType": UPLOAD_DOC_TYPE}),
                  "an unreadable multipart part and an unknown Check — order not documented")


def test_upload_confirmation(r: Runner, ctx):
    """Covers: API-INT-003 ; Negative: PLATFORM-STD / INT-400-REQUEST-INVALID / TC-INT-005 ;
    PASS-THROUGH / CHK-404-CHECK-NOT-FOUND ; PASS-THROUGH / CHK-409-CHECK-NOT-AWAITING-DOCUMENTS / TC-INT-004"""
    m = ctx["manual"]
    if m is None:
        _no_service(r, [("confirm the uploads → 202 RUNNING", "API-INT-003 ; TC-INT-034"),
                        ("confirm twice → 409", "API-INT-003 ; TC-INT-004")], "manual")
    else:
        p = path_of(r, "API-INT-003", checkId=m["id"])
        call = r.http.post(p, json_body={})
        r.expect_success("confirm the uploads", "API-INT-003", call, "API-INT-003 ; TC-INT-034", checks=[
            ("status RUNNING, the threaded checkId", isinstance(call.body, dict) and call.body.get("status") == "RUNNING"
             and call.body.get("checkId") == m["id"], call.safe_body())])
        r.expect_problem("confirm a Check no longer waiting", "API-INT-003", r.http.post(p, json_body={}), 409,
                         "CHK-409-CHECK-NOT-AWAITING-DOCUMENTS",
                         "API-INT-003 ; PASS-THROUGH / CHK-409-CHECK-NOT-AWAITING-DOCUMENTS / TC-INT-004")
    pc = ctx["path"]
    if pc is None:
        _no_service(r, [("confirm a running Check → 409", "API-INT-003 ; TC-INT-004")], "path/blob")
    else:
        r.expect_problem("confirm a running Check", "API-INT-003",
                         r.http.post(path_of(r, "API-INT-003", checkId=pc["id"]), json_body={}), 409,
                         "CHK-409-CHECK-NOT-AWAITING-DOCUMENTS",
                         "API-INT-003 ; PASS-THROUGH / CHK-409-CHECK-NOT-AWAITING-DOCUMENTS / TC-INT-004")

    # stage C
    r.expect_problem("confirm with a non-numeric checkId", "API-INT-003",
                     r.http.post(path_of(r, "API-INT-003", checkId="abc"), json_body={}), 400, "INT-400-REQUEST-INVALID",
                     "API-INT-003 ; PLATFORM-STD / INT-400-REQUEST-INVALID / TC-INT-005")
    unk = ctx["unknown"]["TC-INT-008"]
    if unk is not None:
        r.expect_problem("confirm an unknown Check", "API-INT-003",
                         r.http.post(path_of(r, "API-INT-003", checkId=unk), json_body={}), 404, "CHK-404-CHECK-NOT-FOUND",
                         "API-INT-003 ; PASS-THROUGH / CHK-404-CHECK-NOT-FOUND")
        r.untraced_negative("API-INT-003", "CHK-404-CHECK-NOT-FOUND")
        r.observe("confirm an unknown Check with an undeclared body property", "API-INT-003 ; additionalProperties false",
                  r.http.post(path_of(r, "API-INT-003", checkId=unk), json_body={"unexpected": 1}),
                  "the API document declares an empty object with no additional properties")
        r.observe("confirm an unknown Check without a body", "API-INT-003 ; requestBody required",
                  r.http.post(path_of(r, "API-INT-003", checkId=unk)), "INT-400 (unreadable body) vs CHK-404 — order not documented")


def test_check_reports(r: Runner, ctx):
    """Covers: API-INT-005, API-INT-006 ; Negative: PLATFORM-STD / INT-400-REQUEST-INVALID ;
    PASS-THROUGH / RPT-404-CHECK-NOT-FOUND / TC-INT-088 ; PASS-THROUGH / RPT-400-REQUEST-KEYS-MISSING / TC-INT-090"""
    lp = path_of(r, "API-INT-006")
    for key in ("path", "manual"):
        ch = ctx[key]
        if ch is None:
            _no_service(r, [(f"read the {key} Check's report once ended", "API-INT-005 ; TC-INT-087"),
                            (f"list the Checks of the {key} Check's request", "API-INT-006 ; TC-INT-089")],
                        "manual" if key == "manual" else "path/blob")
            continue
        last, ended = poll_until(lambda: r.http.get(path_of(r, "API-INT-005", checkId=ch["id"])),
                                 {"COMPLETED", "FAILED"}, ctx["timeout"])
        ch["ended_status"] = (last.body or {}).get("status") if isinstance(last.body, dict) else None
        r.expect_success(f"read the {key} Check's report", "API-INT-005", last, "API-INT-005 ; TC-INT-087", checks=[
            ("identifiers exactly as sent", isinstance(last.body, dict) and last.body.get("checkId") == ch["id"]
             and last.body.get("requestNumber") == ch["rn"] and last.body.get("serviceCode") == ch["svc"]["serviceCode"],
             last.safe_body()),
            (f"ended within {ctx['timeout']}s (async, polled)", ended, f"status {ch['ended_status']}"),
        ])
        lst = r.http.get(lp, params={"serviceCode": ch["svc"]["serviceCode"], "requestNumber": ch["rn"]})
        r.expect_success(f"list the Checks of the {key} Check's request", "API-INT-006", lst, "API-INT-006 ; TC-INT-089", checks=[
            ("total 1, the created Check", isinstance(lst.body, dict) and lst.body.get("total") == 1
             and [x.get("checkId") for x in lst.body.get("checks", [])] == [ch["id"]], lst.safe_body())])
    keys = {"serviceCode": f"{r.ns}-SVC-A", "requestNumber": f"{r.ns}-REQ"}      # marked placeholders
    none = r.http.get(lp, params=keys)
    r.expect_success("list the Checks of a request with none", "API-INT-006", none, "API-INT-006 ; TC-INT-089 (shape)", checks=[
        ("total 0, no Check", isinstance(none.body, dict) and none.body.get("total") == 0 and none.body.get("checks") == [],
         none.safe_body())])

    # stage C
    r.expect_problem("read a report with a non-numeric checkId", "API-INT-005",
                     r.http.get(path_of(r, "API-INT-005", checkId="abc")), 400, "INT-400-REQUEST-INVALID",
                     "API-INT-005 ; PLATFORM-STD / INT-400-REQUEST-INVALID")
    r.untraced_negative("API-INT-005", "INT-400-REQUEST-INVALID")
    unk = ctx["unknown"]["TC-INT-088"]
    if unk is not None:
        r.expect_problem("read the report of an unknown Check", "API-INT-005",
                         r.http.get(path_of(r, "API-INT-005", checkId=unk)), 404, "RPT-404-CHECK-NOT-FOUND",
                         "API-INT-005 ; PASS-THROUGH / RPT-404-CHECK-NOT-FOUND / TC-INT-088")
    else:
        r.block("read the report of an unknown Check → 404", "API-INT-005 ; TC-INT-088", f"Check {UNKNOWN_REPORT_CHECK} exists")
    tag = "API-INT-006 ; PASS-THROUGH / RPT-400-REQUEST-KEYS-MISSING / TC-INT-090"
    r.expect_problem("list without a service code", "API-INT-006", r.http.get(lp, params={"requestNumber": LIST_REQUEST_NUMBER}),
                     400, "RPT-400-REQUEST-KEYS-MISSING", tag)
    r.expect_problem("list without a request number", "API-INT-006", r.http.get(lp, params={"serviceCode": keys["serviceCode"]}),
                     400, "RPT-400-REQUEST-KEYS-MISSING", tag)
    r.expect_problem("list with a blank service code", "API-INT-006",
                     r.http.get(lp, params={"serviceCode": " ", "requestNumber": LIST_REQUEST_NUMBER}), 400,
                     "RPT-400-REQUEST-KEYS-MISSING", tag)
    r.observe("read a report with a checkId larger than int64", "API-INT-005 ; format int64",
              r.http.get(path_of(r, "API-INT-005", checkId=OVERFLOW_ID)), "no documented outcome for an out-of-range number")


def test_required_document_types(r: Runner, ctx):
    """Covers: API-INT-008 ; Negative: PLATFORM-STD / INT-400-REQUEST-INVALID ;
    PASS-THROUGH / RPT-404-CHECK-NOT-FOUND / TC-INT-094"""
    found = False
    for key in ("manual", "path"):
        ch = ctx[key]
        if ch is None:
            continue
        found = True
        call = r.http.get(path_of(r, "API-INT-008", checkId=ch["id"]))
        r.expect_success(f"required document types of the {key} Check", "API-INT-008", call, "API-INT-008 ; TC-INT-093", checks=[
            ("those of the Check's own version", isinstance(call.body, dict) and call.body.get("checkId") == ch["id"]
             and call.body.get("serviceCode") == ch["svc"]["serviceCode"]
             and call.body.get("versionNumber") == ch["svc"].get("versionNumber")
             and sorted(call.body.get("requiredDocumentTypes") or []) == sorted(ch["svc"].get("requiredDocumentTypes") or []),
             call.safe_body())])
    if not found:
        _no_service(r, [("required document types of a Check's version", "API-INT-008 ; TC-INT-093")], "manual or path/blob")
    r.expect_problem("required document types with a non-numeric checkId", "API-INT-008",
                     r.http.get(path_of(r, "API-INT-008", checkId="abc")), 400, "INT-400-REQUEST-INVALID",
                     "API-INT-008 ; PLATFORM-STD / INT-400-REQUEST-INVALID")
    r.untraced_negative("API-INT-008", "INT-400-REQUEST-INVALID")
    unk = ctx["unknown"]["TC-INT-094"]
    if unk is not None:
        r.expect_problem("required document types of an unknown Check", "API-INT-008",
                         r.http.get(path_of(r, "API-INT-008", checkId=unk)), 404, "RPT-404-CHECK-NOT-FOUND",
                         "API-INT-008 ; PASS-THROUGH / RPT-404-CHECK-NOT-FOUND / TC-INT-094")
    else:
        r.block("required document types of an unknown Check → 404", "API-INT-008 ; TC-INT-094",
                f"Check {UNKNOWN_TYPES_CHECK} exists")


def test_decision(r: Runner, ctx):
    """Covers: API-INT-004 ; Negative: PLATFORM-STD / INT-400-REQUEST-INVALID ; PASS-THROUGH / RPT-404-CHECK-NOT-FOUND ;
    RULE-INT-002 / RPT-400-DECISION-INCOMPLETE / TC-INT-016, TC-INT-099 ; RULE-INT-003 / RPT-409-CHECK-NOT-COMPLETED /
    TC-INT-017 ; RULE-INT-003 / RPT-409-DECISION-ALREADY-RECORDED / TC-INT-003"""
    rejected = {"employeeDecision": "REJECTED", "decidedBy": ctx["decided_by"]}
    decided_any = False
    for key in ("path", "manual"):
        ch = ctx[key]
        if ch is None:
            continue
        p = path_of(r, "API-INT-004", checkId=ch["id"])
        if ch.get("ended_status") != "COMPLETED":
            r.expect_problem(f"decision on the {key} Check that is {ch.get('ended_status')}", "API-INT-004",
                             r.http.post(p, json_body=rejected), 409, "RPT-409-CHECK-NOT-COMPLETED",
                             "API-INT-004 ; RULE-INT-003 / RPT-409-CHECK-NOT-COMPLETED / TC-INT-017")
            continue
        if ch["svc"].get("approvalEnabled"):
            r.block(f"APPROVED decision on the {key} Check", "API-INT-004 ; TC-INT-037/038",
                    "approval-enabled service — exercised only against a Dev/Test Approval API stub (§3-I); REJECTED only")
        tag16 = "API-INT-004 ; RULE-INT-002 / RPT-400-DECISION-INCOMPLETE / TC-INT-016"
        r.expect_problem("decision without a deciding employee", "API-INT-004",
                         r.http.post(p, json_body={"employeeDecision": "REJECTED"}), 400, "RPT-400-DECISION-INCOMPLETE", tag16)
        r.expect_problem("decision code outside APPROVED / REJECTED", "API-INT-004",
                         r.http.post(p, json_body={"employeeDecision": "MAYBE", "decidedBy": ctx["decided_by"]}), 400,
                         "RPT-400-DECISION-INCOMPLETE", "API-INT-004 ; RULE-INT-002 / RPT-400-DECISION-INCOMPLETE / TC-INT-099")
        call = r.http.post(p, json_body=rejected)
        if call.status == 201:
            r.track("Employee Decision", ch["id"], ch["svc"]["serviceCode"], ch["rn"], "API-INT-004 REJECTED decision")
        r.expect_success(f"record a REJECTED decision on the {key} Check", "API-INT-004", call,
                         "API-INT-004 ; TC-INT-035 / TC-INT-013 / TC-INT-036", checks=[
            ("REJECTED, decidedBy as sent, Approval API not executed", isinstance(call.body, dict)
             and call.body.get("employeeDecision") == "REJECTED" and call.body.get("decidedBy") == ctx["decided_by"]
             and call.body.get("approvalApiExecuted") is False and call.body.get("checkId") == ch["id"], call.safe_body())])
        r.expect_problem("a second decision on the decided Check", "API-INT-004", r.http.post(p, json_body=rejected), 409,
                         "RPT-409-DECISION-ALREADY-RECORDED",
                         "API-INT-004 ; RULE-INT-003 / RPT-409-DECISION-ALREADY-RECORDED / TC-INT-003")
        decided_any = True
    if not decided_any:
        r.block("record a decision on a COMPLETED Check (201) and refuse a second (409)", "API-INT-004 ; TC-INT-035 / TC-INT-003",
                "no Check reached COMPLETED — no service in the registry, and no comparison model configured")
        r.block("decision refusals on a COMPLETED Check (TC-INT-016, TC-INT-099)", "API-INT-004 ; RPT-400-DECISION-INCOMPLETE",
                "no COMPLETED, undecided Check — the refusal's precondition (error-catalog RULE-INT-002) cannot be set up")
    if ctx["path"] is None and ctx["manual"] is None:
        r.block("decision on a Check that is not COMPLETED → 409", "API-INT-004 ; TC-INT-017", "no Check can be started")

    # stage C
    r.expect_problem("decision with a non-numeric checkId", "API-INT-004",
                     r.http.post(path_of(r, "API-INT-004", checkId="abc"), json_body=rejected), 400, "INT-400-REQUEST-INVALID",
                     "API-INT-004 ; PLATFORM-STD / INT-400-REQUEST-INVALID")
    r.untraced_negative("API-INT-004", "INT-400-REQUEST-INVALID")
    unk = ctx["unknown"]["TC-INT-008"]
    if unk is not None:
        r.expect_problem("decision on an unknown Check", "API-INT-004",
                         r.http.post(path_of(r, "API-INT-004", checkId=unk), json_body=rejected), 404, "RPT-404-CHECK-NOT-FOUND",
                         "API-INT-004 ; PASS-THROUGH / RPT-404-CHECK-NOT-FOUND")
        r.untraced_negative("API-INT-004", "RPT-404-CHECK-NOT-FOUND")
        r.observe("decision on an unknown Check with an unreadable body", "API-INT-004 ; INT-400 vs RPT-404",
                  r.http.post(path_of(r, "API-INT-004", checkId=unk), raw="{", headers={"Content-Type": "application/json"}),
                  "unreadable body (INT-400) and unknown Check (RPT-404) — order not documented")
        r.observe("decision code outside APPROVED / REJECTED on an unknown Check", "API-INT-004 ; RPT-400 vs RPT-404",
                  r.http.post(path_of(r, "API-INT-004", checkId=unk), json_body={"employeeDecision": "MAYBE",
                                                                              "decidedBy": ctx["decided_by"]}),
                  "TC-INT-099's precondition is a COMPLETED Check; on an unknown Check the order is not documented")
        r.observe("APPROVED decision on an unknown Check", "API-INT-004 ; RPT-404 before any approval call",
                  r.http.post(path_of(r, "API-INT-004", checkId=unk), json_body={"employeeDecision": "APPROVED",
                                                                              "decidedBy": ctx["decided_by"]}),
                  "no Check, so no Approval API definition — nothing is called or recorded")


SUITES = [test_checks, test_check_documents, test_upload_confirmation, test_check_reports,
          test_required_document_types, test_decision]
