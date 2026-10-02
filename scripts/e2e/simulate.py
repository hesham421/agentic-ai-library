#!/usr/bin/env python3
"""aias end-to-end SIMULATION — drives the real HTTP API of a running LOCAL app through the whole
product flow (registry -> start -> upload -> confirm -> pipeline -> report -> decision -> approval)
and asserts status, ``code`` and key body fields at every step. No JUnit, no direct DB access.

    python3 scripts/e2e/setup_fixtures.py          # once: synthetic fixtures + app restart
    python3 scripts/e2e/simulate.py                # every group
    python3 scripts/e2e/simulate.py --only refusals --only lifecycle
    python3 scripts/e2e/simulate.py --list

Safety: refuses any base URL that is not localhost; uses only the synthetic fixture services of the
``local`` profile and the local approval stub; writes nothing to the database except through the API.

Model quota: the free comparison model allows ~20 calls/day. Every Check that reaches RUNNING costs one
comparison call (``--max-model-checks``, default 8); a PNG upload costs one reading call. A Check that
ends FAILED/MODEL_UNAVAILABLE with a 429/quota detail marks the scenario SKIPPED-QUOTA and every later
model-dependent scenario is skipped without calling the model.
"""
import argparse
import datetime as dt
import http.client
import json
import os
import subprocess
import sys
import threading
import time
import traceback
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import synth  # noqa: E402
import setup_fixtures as fx  # noqa: E402

REPO = Path(__file__).resolve().parents[2]
STUB_LOG = REPO / "logs/approval-stub.log"
PROFILE = REPO / "src/main/resources/application-local.properties"
UNKNOWN_ID = 987654321
MAX_FILE_SIZE = 10 * 1024 * 1024        # aias.check.max-file-size default (10 MB)
REQUEST_LIMIT = 12 * 1024 * 1024        # aias.integration.upload.request-limit set by setup_fixtures.py
MAX_UPLOADS = 20                        # aias.check.max-uploads default
UPLOAD_WINDOW_MIN = 60                  # aias.check.upload-window of the local profile (PT60M)
MODEL_SPACING_S = 15                    # free tier ~5 requests/minute/model (provider retries count too)
RATE_COOLDOWN_S = 65                    # after a per-minute 429: wait for the minute window to pass
OVERLOAD_COOLDOWN_S = 30                # after a 503 "high demand"


# ============================================================================================ infra
class Skip(Exception):
    def __init__(self, status, reason):
        super().__init__(reason)
        self.status = status
        self.reason = reason


class Hard(Exception):
    """A failed precondition inside a scenario: later steps cannot run."""


class Resp:
    def __init__(self, method, url, status, headers, raw, elapsed):
        self.method, self.url, self.status, self.headers, self.raw, self.elapsed = (
            method, url, status, headers, raw, elapsed)
        try:
            self.json = json.loads(raw.decode()) if raw else None
        except (ValueError, UnicodeDecodeError):
            self.json = None

    @property
    def code(self):
        return self.json.get("code") if isinstance(self.json, dict) else None

    def short(self):
        body = self.raw[:300].decode(errors="replace") if self.raw else ""
        return f"{self.method} {self.url} -> {self.status} {body}"


class Client:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def request(self, method, path, body=None, headers=None, timeout=60):
        url = self.base + path
        data = body
        hdrs = dict(headers or {})
        if isinstance(body, (dict, list)):
            data = json.dumps(body).encode()
            hdrs.setdefault("Content-Type", "application/json")
        req = urllib.request.Request(url, data=data, method=method, headers=hdrs)
        started = time.time()
        try:
            with urllib.request.urlopen(req, timeout=timeout) as r:
                return Resp(method, path, r.status, dict(r.headers), r.read(), time.time() - started)
        except urllib.error.HTTPError as e:
            return Resp(method, path, e.code, dict(e.headers or {}), e.read() or b"", time.time() - started)

    def get(self, path, **kw):
        return self.request("GET", path, **kw)

    def post(self, path, body=None, **kw):
        return self.request("POST", path, body=body, **kw)

    def upload(self, check_id, document_type, file_name, content, content_type="application/pdf",
               omit_type=False):
        boundary = "e2e" + uuid.uuid4().hex
        parts = []
        if not omit_type:
            parts.append((f'--{boundary}\r\nContent-Disposition: form-data; name="documentType"\r\n\r\n'
                          f"{document_type}\r\n").encode())
        parts.append((f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{file_name}"\r\n'
                      f"Content-Type: {content_type}\r\n\r\n").encode() + content + b"\r\n")
        parts.append(f"--{boundary}--\r\n".encode())
        body = b"".join(parts)
        headers = {"Content-Type": f"multipart/form-data; boundary={boundary}"}
        path = f"/api/v1/checks/{check_id}/documents"
        if len(body) > 4 * 1024 * 1024:
            return self.streamed_post(path, body, headers)
        return self.post(path, body=body, headers=headers, timeout=120)

    def streamed_post(self, path, body, headers):
        """A large POST that tolerates the server answering (e.g. 413) and closing before the whole body
        is sent: the early response is still read and returned."""
        u = urllib.parse.urlparse(self.base)
        conn = http.client.HTTPConnection(u.hostname, u.port or 80, timeout=120)
        started = time.time()
        conn.putrequest("POST", path)
        for k, v in headers.items():
            conn.putheader(k, v)
        conn.putheader("Content-Length", str(len(body)))
        conn.endheaders()
        try:
            for i in range(0, len(body), 65536):
                conn.send(body[i:i + 65536])
        except (BrokenPipeError, ConnectionResetError):
            pass
        try:
            r = conn.getresponse()
            return Resp("POST", path, r.status, dict(r.getheaders()), r.read(), time.time() - started)
        finally:
            conn.close()


class Scenario:
    def __init__(self, group, name, tcs, model):
        self.group, self.name, self.tcs, self.model = group, name, tcs, model
        self.checks = []          # (ok, description, detail)
        self.status = None
        self.reason = ""
        self.checks_created = []
        self.duration = 0.0

    def expect(self, ok, description, detail=""):
        self.checks.append((bool(ok), description, "" if ok else str(detail)[:600]))
        return bool(ok)

    def require(self, ok, description, detail=""):
        if not self.expect(ok, description, detail):
            raise Hard(description)

    def status_code(self, resp, status, code=None, description=None, hard=False):
        """Asserts the HTTP status and (when given) the ProblemDetail code; ``code`` may be a set."""
        codes = {code} if isinstance(code, str) else (set(code) if code else None)
        ok = resp.status == status and (codes is None or resp.code in codes)
        if ok and status >= 400:
            ok = isinstance(resp.json, dict) and all(k in resp.json for k in ("type", "title", "status", "code"))
        label = description or f"{resp.method} {resp.url} -> {status}" + (f" {'/'.join(sorted(codes))}" if codes else "")
        (self.require if hard else self.expect)(ok, label, resp.short())
        return ok


class Ctx:
    def __init__(self, args):
        self.args = args
        self.http = Client(args.base_url)
        self.run_id = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        self.cache = {}
        self.comparison_calls = 0
        self.reading_calls = 0
        self.quota_hit = None
        self.last_model_start = 0.0
        self.cooldown_until = 0.0
        self.created = []         # surviving records: (checkId, service, requestNumber, what)
        self.scenario = None
        self.comparison_model = read_property("aias.check.comparison-model.model")
        self.mode_touched = False
        self.restarts = []        # app restarts made by the restart groups

    # ---- model budget -------------------------------------------------------------------------
    def model_gate(self, s, reading=False):
        """Called right before an action that makes the pipeline call the comparison model."""
        if self.quota_hit:
            raise Skip("SKIPPED-QUOTA", f"model quota already exhausted in this run ({self.quota_hit})")
        if self.comparison_calls >= self.args.max_model_checks:
            raise Skip("SKIPPED-BUDGET", f"comparison-call budget of {self.args.max_model_checks} reached")
        wait = max(MODEL_SPACING_S - (time.time() - self.last_model_start), self.cooldown_until - time.time())
        if wait > 0:
            time.sleep(wait)
        self.last_model_start = time.time()
        self.comparison_calls += 1
        if reading:
            self.reading_calls += 1

    def judge_model_failure(self, report):
        """FAILED/MODEL_UNAVAILABLE is an environment outcome, not an app defect: skip cleanly."""
        if report.get("status") != "FAILED" or report.get("failureReason") != "MODEL_UNAVAILABLE":
            return
        detail = (report.get("failureDetail") or "")
        low = detail.lower()
        where = f"check {report.get('checkId')} ended MODEL_UNAVAILABLE: {detail[:300]}"
        if "perminute" in low:
            # a per-MINUTE free-tier limit is transient, not an exhausted quota: cool down, retry once
            self.cooldown_until = time.time() + RATE_COOLDOWN_S
            raise Skip("SKIPPED-RATE", where)
        if "429" in low or "quota" in low or "resource_exhausted" in low or "too many requests" in low or "rate limit" in low:
            self.quota_hit = detail[:200]
            raise Skip("SKIPPED-QUOTA", where)
        self.cooldown_until = time.time() + OVERLOAD_COOLDOWN_S
        raise Skip("SKIPPED-MODEL", where)

    def track(self, check_id, service, request_number, what):
        self.created.append({"checkId": check_id, "serviceCode": service, "requestNumber": request_number,
                             "what": what})

    def request_number(self, label, slash_space=False):
        return f"E2E/{self.run_id} {label}" if slash_space else f"E2E-{self.run_id}-{label}"


def read_property(key):
    try:
        for line in PROFILE.read_text().splitlines():
            if line.startswith(key + "="):
                return line.split("=", 1)[1].strip()
    except OSError:
        pass
    return None


# ========================================================================================= helpers
EMPLOYEE = "E2E-EMP-0001"


def start(ctx, s, service, request_number, expected_status):
    r = ctx.http.post("/api/v1/checks", {"serviceCode": service, "requestNumber": request_number,
                                         "employeeId": EMPLOYEE})
    s.status_code(r, 202, description=f"start {service} -> 202", hard=True)
    body = r.json or {}
    check_id = body.get("checkId")
    s.require(isinstance(check_id, int), "start answers a numeric checkId", r.short())
    s.expect(body.get("status") == expected_status, f"start status {expected_status}", r.short())
    s.expect(str(body.get("checkUrl", "")).endswith(f"/{check_id}"), "start names the Check's read address (checkUrl)",
             r.short())
    s.expect(str(r.headers.get("Location", "")).endswith(f"/{check_id}"), "Location header names the Check's read",
             r.headers)
    ctx.track(check_id, service, request_number, f"started by '{s.name}'")
    s.checks_created.append(check_id)
    return check_id


def wait_end(ctx, s, check_id, timeout=200):
    deadline = time.time() + timeout
    report = None
    while time.time() < deadline:
        r = ctx.http.get(f"/api/v1/check-reports/{check_id}")
        if r.status == 200 and r.json and r.json.get("status") in ("COMPLETED", "FAILED"):
            return r.json
        report = r.json
        time.sleep(2)
    s.require(False, f"check {check_id} ended within {timeout}s", report)


def pdf_for(request_number, gpa, credits):
    return synth.make_pdf(synth.transcript_lines(request_number, gpa, credits))


def doc_of(report, document_type):
    return [d for d in report.get("documents", []) if d.get("documentType") == document_type]


def finding_of(report, condition):
    return next((f for f in report.get("findings", []) if f.get("condition") == condition), None)


def manual_flow(ctx, s, service, label, uploads, slash_space=False, reading=False):
    """start -> AWAITING_DOCUMENTS -> required types -> uploads -> list -> confirm -> end.
    ``uploads`` is a list of (documentType, fileName, bytes|callable(requestNumber), contentType)."""
    request_number = ctx.request_number(label, slash_space)
    check_id = start(ctx, s, service, request_number, "AWAITING_DOCUMENTS")
    r = ctx.http.get(f"/api/v1/checks/{check_id}/required-document-types")
    s.status_code(r, 200, description="required document types read -> 200")
    s.expect(r.json and r.json.get("requiredDocumentTypes") == ["TRANSCRIPT"] and r.json.get("serviceCode") == service
             and r.json.get("versionNumber") == 1, "required types are the version's [TRANSCRIPT]", r.short())
    for document_type, file_name, content, content_type in uploads:
        data = content(request_number) if callable(content) else content
        r = ctx.http.upload(check_id, document_type, file_name, data, content_type)
        s.status_code(r, 201, description=f"upload {file_name} -> 201", hard=True)
        s.expect(r.json.get("documentType") == document_type and r.json.get("fileSize") == len(data)
                 and r.json.get("oversized") is False and r.json.get("fileName") == file_name,
                 "upload receipt echoes type, name, size, oversized=false", r.short())
    r = ctx.http.get(f"/api/v1/checks/{check_id}/documents")
    s.status_code(r, 200, description="list uploads -> 200")
    s.expect(isinstance(r.json, list) and len(r.json) == len(uploads)
             and all("content" not in d for d in r.json), f"{len(uploads)} upload(s) listed without content", r.short())
    ctx.model_gate(s, reading=reading)
    r = ctx.http.post(f"/api/v1/checks/{check_id}/upload-confirmation", {})
    s.status_code(r, 202, description="confirm -> 202", hard=True)
    s.expect(r.json.get("status") == "RUNNING" and r.json.get("checkId") == check_id, "confirm answers RUNNING",
             r.short())
    report = wait_end(ctx, s, check_id)
    ctx.judge_model_failure(report)
    s.require(report.get("status") == "COMPLETED", f"check {check_id} COMPLETED",
              {k: report.get(k) for k in ("status", "failureReason", "failureDetail")})
    return {"checkId": check_id, "requestNumber": request_number, "report": report}


def shared(ctx, s, key, factory):
    """A COMPLETED check shared by several scenarios (one model call, reused)."""
    if key not in ctx.cache:
        try:
            ctx.cache[key] = factory()
        except Skip as e:
            ctx.cache[key] = e
            raise
        except Hard as e:
            ctx.cache[key] = Skip("SKIPPED-PRECONDITION", f"shared check '{key}' could not be built: {e}")
            raise
    value = ctx.cache[key]
    if isinstance(value, Skip):
        raise Skip(value.status, value.reason)
    return value


def manual_pass(ctx, s):
    return shared(ctx, s, "manual_pass", lambda: manual_flow(
        ctx, s, "demo-manual", "PASS", [("TRANSCRIPT", "transcript-pass.pdf",
                                         lambda rn: pdf_for(rn, "3.62", "128"), "application/pdf")]))


def approval_pass(ctx, s):
    return shared(ctx, s, "approval_pass", lambda: manual_flow(
        ctx, s, "demo-approval", "APR1", [("TRANSCRIPT", "transcript-pass.pdf",
                                           lambda rn: pdf_for(rn, "3.71", "131"), "application/pdf")],
        slash_space=True))


def approval_png(ctx, s):
    return shared(ctx, s, "approval_png", lambda: manual_flow(
        ctx, s, "demo-approval", "PNG",
        [("TRANSCRIPT", "transcript.png",
          lambda rn: synth.make_png(synth.transcript_lines(rn, "3.55", "126")), "image/png")],
        reading=True))


def awaiting_check(ctx, s, service="demo-manual", label="WAIT"):
    """A fresh manual Check left in AWAITING_DOCUMENTS (no model call; it expires after the window)."""
    return start(ctx, s, service, ctx.request_number(f"{label}-{uuid.uuid4().hex[:6]}"), "AWAITING_DOCUMENTS")


def stub_lines():
    try:
        return STUB_LOG.read_text(errors="replace").splitlines()
    except OSError:
        return []


def stub_mode(mode, seconds=None):
    query = f"set={mode}" + (f"&seconds={seconds}" if seconds is not None else "")
    with urllib.request.urlopen(f"http://127.0.0.1:7290/__mode?{query}", timeout=5) as r:
        return json.loads(r.read().decode()).get("mode")


# ======================================================================================= scenarios
SCENARIOS = []


def scenario(group, name, tcs=(), model=False):
    def register(fn):
        SCENARIOS.append((Scenario(group, name, list(tcs), model), fn))
        return fn
    return register


FIXTURES = {"demo-manual": ("manual", False), "demo-approval": ("manual", True),
            "demo-path": ("path", False), "demo-blob": ("blob", False)}


# ------------------------------------------------------------------------------------- registry
@scenario("registry", "The four fixture services are listed with their fetch modes, without SQL or connection",
          ["TC-REG-014", "TC-REG-005", "TC-REG-001"])
def reg_list(ctx, s):
    r = ctx.http.get("/api/v1/services")
    s.status_code(r, 200, hard=True)
    by_code = {x["serviceCode"]: x for x in r.json}
    for code, (mode, approval) in FIXTURES.items():
        x = by_code.get(code)
        s.expect(x and x["available"] is True and x["fetchMode"] == mode and x["approvalEnabled"] is approval
                 and x["versionNumber"] >= 1, f"{code}: available, fetch {mode}, approval {approval}", x)
    s.expect(by_code.get("demo-path", {}).get("requiredDocumentTypes") == ["ID_CARD", "TRANSCRIPT"]
             or sorted(by_code.get("demo-path", {}).get("requiredDocumentTypes", [])) == ["ID_CARD", "TRANSCRIPT"],
             "demo-path requires TRANSCRIPT and ID_CARD", by_code.get("demo-path"))
    leaked = [k for x in r.json for k in x if k.lower() in ("sql", "sqltext", "connection", "connectionname",
                                                             "endpoint", "credentialreference", "queries")]
    s.expect(not leaked, "no SQL / connection detail in the list", leaked)


@scenario("registry", "The load report shows both connections ACTIVATED and every fixture package loaded",
          ["TC-REG-008", "TC-REG-051", "TC-REG-006"])
def reg_load(ctx, s):
    r = ctx.http.get("/api/v1/load-results")
    s.status_code(r, 200, hard=True)
    rows = r.json
    s.expect(len({x["loadRunAt"] for x in rows}) == 1, "rows of the latest load run only", {x["loadRunAt"] for x in rows})
    connections = {x["subjectName"]: x["outcome"] for x in rows if x["subjectKind"] == "CONNECTION"}
    for name in ("local-oracle", "local-jdbc"):
        s.expect(connections.get(name) in ("ACTIVATED", "UPDATED"), f"connection {name} ACTIVATED", connections)
    packages = {x["subjectName"]: x for x in rows if x["subjectKind"] == "SERVICE_PACKAGE"}
    for code in FIXTURES:
        x = packages.get(code)
        s.expect(x and x["outcome"] in ("REGISTERED", "UNCHANGED", "UPDATED") and x["serviceCode"] == code,
                 f"package {code} REGISTERED/UNCHANGED", x)
    s.expect(not [x for x in rows if x["outcome"] == "REJECTED"], "no REJECTED row",
             [x for x in rows if x["outcome"] == "REJECTED"])


@scenario("registry", "An unknown service code is 404 REG-404-SERVICE-NOT-FOUND", ["TC-REG-016"])
def reg_unknown(ctx, s):
    s.status_code(ctx.http.get("/api/v1/services/no-such-service-e2e"), 404, "REG-404-SERVICE-NOT-FOUND")


@scenario("registry", "A service code is read trimmed and case-insensitively", ["TC-REG-075", "TC-REG-073"])
def reg_case(ctx, s):
    r = ctx.http.get("/api/v1/services/DEMO-Manual")
    s.status_code(r, 200)
    s.expect(r.json and r.json.get("serviceCode") == "demo-manual", "upper/mixed case answers demo-manual", r.short())
    r = ctx.http.get("/api/v1/services/" + urllib.parse.quote("  Demo-Path "))
    s.status_code(r, 200)
    s.expect(r.json and r.json.get("serviceCode") == "demo-path", "space-padded code answers demo-path", r.short())


# ---------------------------------------------------------------------------------------- manual
@scenario("manual", "Manual happy path: upload a passing PDF, confirm, COMPLETED/COMPLIANT with a full report",
          ["TC-INT-026", "TC-INT-028", "TC-INT-030", "TC-INT-031", "TC-INT-034", "TC-INT-087", "TC-INT-089",
           "TC-INT-091", "TC-INT-093", "TC-CHK-075", "TC-CHK-076", "TC-CHK-027", "TC-CHK-071", "TC-CHK-073",
           "TC-DOC-040", "TC-DOC-042", "TC-RPT-028", "TC-RPT-033"], model=True)
def manual_happy(ctx, s):
    run = manual_pass(ctx, s)
    check_id, rn, report = run["checkId"], run["requestNumber"], run["report"]
    s.expect(report["overallStatus"] == "COMPLIANT", "overall COMPLIANT", report["overallStatus"])
    s.expect(report["serviceCode"] == "demo-manual" and report["versionNumber"] == 1 and report["fetchMode"] == "manual",
             "metadata: service, version 1, fetch manual", report)
    s.expect(report["requestNumber"] == rn and report["employeeId"] == EMPLOYEE, "host identifiers unchanged",
             (report["requestNumber"], report["employeeId"]))
    s.expect(report["comparisonModel"] and (not ctx.comparison_model or report["comparisonModel"] == ctx.comparison_model),
             f"metadata names the comparison model ({ctx.comparison_model})", report["comparisonModel"])
    s.expect(report["startedAt"] and report["runningSince"] and report["endedAt"], "times started/running/ended set",
             report)
    s.expect(len(report["findings"]) >= 2, "one finding per condition and per required type", report["findings"])
    tf = finding_of(report, "TRANSCRIPT")
    s.expect(tf and tf["outcome"] == "SATISFIED", "TRANSCRIPT finding SATISFIED", tf)
    s.expect(all(f["outcome"] == "SATISFIED" for f in report["findings"]), "every finding SATISFIED", report["findings"])
    docs = doc_of(report, "TRANSCRIPT")
    s.expect(len(docs) == 1 and docs[0]["readStatus"] == "READ" and docs[0]["sourceMode"] == "manual",
             "TRANSCRIPT document READ (manual)", docs)
    s.expect(report["unreadQueries"] == [], "request_echo query read (no unread query)", report["unreadQueries"])
    s.expect(report.get("decision") is None, "no decision yet", report.get("decision"))
    r = ctx.http.get(f"/api/v1/checks/{check_id}")
    s.status_code(r, 200, description="RPT read of the Check -> 200")
    s.expect(r.json and r.json.get("overallStatus") == report["overallStatus"]
             and len(r.json.get("findings", [])) == len(report["findings"]), "RPT read equals the INT relay", r.short())
    q = urllib.parse.urlencode({"serviceCode": "demo-manual", "requestNumber": rn})
    for path in (f"/api/v1/checks?{q}", f"/api/v1/check-reports?{q}"):
        r = ctx.http.get(path)
        s.status_code(r, 200, description=f"list of the request {path.split('?')[0]} -> 200")
        ids = [c["checkId"] for c in (r.json or {}).get("checks", [])]
        s.expect(check_id in ids and r.json.get("total", 0) >= 1, "the list of the request includes the Check", r.short())
    # second confirmation and a late upload after the Check ended (no model call)
    r = ctx.http.post(f"/api/v1/checks/{check_id}/upload-confirmation", {})
    s.status_code(r, 409, "CHK-409-CHECK-NOT-AWAITING-DOCUMENTS", "confirm twice -> 409 CHK-409 (TC-INT-004)")
    r = ctx.http.upload(check_id, "TRANSCRIPT", "late.pdf", pdf_for(rn, "3.0", "120"))
    s.status_code(r, 409, "INT-409-CHECK-NOT-AWAITING-DOCUMENTS", "upload after the end -> 409 INT-409 (TC-INT-007)")


@scenario("manual", "Manual NOT_COMPLIANT: a failing transcript (GPA 2.10, 90 credits)",
          ["TC-CHK-024", "TC-RPT-011"], model=True)
def manual_not_compliant(ctx, s):
    run = manual_flow(ctx, s, "demo-manual", "FAIL", [("TRANSCRIPT", "transcript-fail.pdf",
                                                        lambda rn: pdf_for(rn, "2.10", "90"), "application/pdf")])
    report = run["report"]
    s.expect(report["overallStatus"] == "NOT_COMPLIANT", "overall NOT_COMPLIANT", report["overallStatus"])
    s.expect(any(f["outcome"] == "NOT_SATISFIED" for f in report["findings"] if f["condition"] != "TRANSCRIPT"),
             "the knowledge condition is NOT_SATISFIED", report["findings"])
    tf = finding_of(report, "TRANSCRIPT")
    s.expect(tf and tf["outcome"] == "SATISFIED", "the document itself was read (TRANSCRIPT SATISFIED)", tf)


@scenario("manual", "Missing document: confirm without an upload -> TRANSCRIPT MISSING, NOT_COMPLIANT",
          ["TC-CHK-009", "TC-DOC-045", "TC-INT-092", "TC-RPT-014"], model=True)
def manual_missing(ctx, s):
    rn = ctx.request_number("MISSING")
    check_id = start(ctx, s, "demo-manual", rn, "AWAITING_DOCUMENTS")
    r = ctx.http.get(f"/api/v1/checks/{check_id}/documents")
    s.status_code(r, 200)
    s.expect(r.json == [], "no upload listed", r.short())
    ctx.model_gate(s)
    r = ctx.http.post(f"/api/v1/checks/{check_id}/upload-confirmation", {})
    s.status_code(r, 202, hard=True)
    report = wait_end(ctx, s, check_id)
    ctx.judge_model_failure(report)
    s.require(report["status"] == "COMPLETED", "COMPLETED", report)
    tf = finding_of(report, "TRANSCRIPT")
    s.expect(tf and tf["outcome"] == "NOT_SATISFIED" and tf["evidence"] == "MISSING",
             "TRANSCRIPT finding NOT_SATISFIED, evidence MISSING", tf)
    docs = doc_of(report, "TRANSCRIPT")
    s.expect(len(docs) == 1 and docs[0]["readStatus"] == "MISSING", "TRANSCRIPT document MISSING", docs)
    s.expect(report["overallStatus"] == "NOT_COMPLIANT", "overall NOT_COMPLIANT (RULE: any NOT_SATISFIED)",
             report["overallStatus"])


# ---------------------------------------------------------------------------------------- image
@scenario("image", "A PNG transcript (no text layer) is read by the document-reading model",
          ["TC-DOC-056", "TC-DOC-060"], model=True)
def image_png(ctx, s):
    run = approval_png(ctx, s)
    report = run["report"]
    docs = doc_of(report, "TRANSCRIPT")
    s.expect(len(docs) == 1 and docs[0]["readStatus"] == "READ", "PNG TRANSCRIPT READ through the reading model", docs)
    tf = finding_of(report, "TRANSCRIPT")
    s.expect(tf and tf["outcome"] == "SATISFIED", "TRANSCRIPT finding SATISFIED", tf)
    s.expect(report["overallStatus"] in ("COMPLIANT", "NEEDS_MANUAL_REVIEW", "NOT_COMPLIANT"),
             f"an Overall Status is given ({report['overallStatus']}; COMPLIANT expected if the model read the values)",
             report["overallStatus"])
    s.expect(report["overallStatus"] == "COMPLIANT", "overall COMPLIANT (synthetic values pass)",
             [(f["outcome"], f["note"][:200]) for f in report["findings"]])


# ------------------------------------------------------------------------------------------ path
@scenario("path", "Path flow + traversal: TRANSCRIPT read under the storage root, ../outside.pdf refused unopened",
          ["TC-INT-025", "TC-CHK-056", "TC-DOC-005", "TC-DOC-019", "TC-DOC-034", "TC-DOC-035", "TC-CHK-010",
           "TC-CHK-025", "TC-DOC-012"], model=True)
def path_flow(ctx, s):
    rn = ctx.request_number("PATH")
    ctx.model_gate(s)
    check_id = start(ctx, s, "demo-path", rn, "RUNNING")
    r = ctx.http.get(f"/api/v1/active-checks/{check_id}")
    if r.status == 200:
        s.expect(r.json.get("checkStatus") == "RUNNING", "active check RUNNING while the pipeline runs", r.short())
    else:
        s.status_code(r, 404, "CHK-404-ACTIVE-CHECK-NOT-FOUND", "active check already ended (fast pipeline)")
    r = ctx.http.upload(check_id, "TRANSCRIPT", "x.pdf", pdf_for(rn, "3.0", "120"))
    s.status_code(r, 409, {"INT-409-CHECK-NOT-AWAITING-DOCUMENTS"}, "upload on a path Check -> 409 INT-409")
    r = ctx.http.get(f"/api/v1/checks/{check_id}/required-document-types")
    s.expect(r.status == 200 and sorted(r.json.get("requiredDocumentTypes", [])) == ["ID_CARD", "TRANSCRIPT"],
             "required types TRANSCRIPT + ID_CARD", r.short())
    report = wait_end(ctx, s, check_id)
    ctx.judge_model_failure(report)
    s.require(report["status"] == "COMPLETED", "COMPLETED", report)
    t = doc_of(report, "TRANSCRIPT")
    s.expect(len(t) == 1 and t[0]["readStatus"] == "READ" and t[0]["sourceMode"] == "path",
             "TRANSCRIPT READ from local/storage-root/transcripts/pass.pdf", t)
    i = doc_of(report, "ID_CARD")
    s.expect(len(i) == 1 and i[0]["readStatus"] == "UNREADABLE" and i[0]["unreadableReason"] == "OUTSIDE_STORAGE_ROOT",
             "ID_CARD (../outside.pdf) UNREADABLE / OUTSIDE_STORAGE_ROOT", i)
    s.expect(i and "SYNTHETIC - OUTSIDE" not in json.dumps(report), "the outside file's content appears nowhere", "")
    s.expect((finding_of(report, "TRANSCRIPT") or {}).get("outcome") == "SATISFIED", "TRANSCRIPT finding SATISFIED",
             finding_of(report, "TRANSCRIPT"))
    s.expect((finding_of(report, "ID_CARD") or {}).get("outcome") == "UNDETERMINED", "ID_CARD finding UNDETERMINED",
             finding_of(report, "ID_CARD"))
    s.expect(report["overallStatus"] != "COMPLIANT", f"overall not COMPLIANT ({report['overallStatus']})",
             report["overallStatus"])
    s.expect(report["overallStatus"] == "NEEDS_MANUAL_REVIEW",
             "overall NEEDS_MANUAL_REVIEW (UNDETERMINED, no NOT_SATISFIED)", report["findings"])


# ------------------------------------------------------------------------------------------ blob
@scenario("blob", "Blob flow: the TRANSCRIPT BLOB is read over the jdbc connection local-jdbc",
          ["TC-DOC-037", "TC-DOC-038", "TC-CHK-059"], model=True)
def blob_flow(ctx, s):
    rn = ctx.request_number("BLOB")
    ctx.model_gate(s)
    check_id = start(ctx, s, "demo-blob", rn, "RUNNING")
    report = wait_end(ctx, s, check_id)
    ctx.judge_model_failure(report)
    s.require(report["status"] == "COMPLETED", "COMPLETED", report)
    t = doc_of(report, "TRANSCRIPT")
    s.expect(len(t) == 1 and t[0]["readStatus"] == "READ" and t[0]["sourceMode"] == "blob",
             "TRANSCRIPT READ from the BLOB column", t)
    s.expect(report["fetchMode"] == "blob" and report["unreadQueries"] == [], "fetch blob, request_echo read", report)
    s.expect(report["overallStatus"] == "COMPLIANT", "overall COMPLIANT", report["findings"])


# ------------------------------------------------------------------------------------- decisions
@scenario("decisions", "Decisions on a COMPLETED demo-manual Check (no Approval API)",
          ["TC-INT-014", "TC-INT-035", "TC-INT-036", "TC-INT-003", "TC-RPT-039", "TC-RPT-041", "TC-RPT-042",
           "TC-RPT-047", "TC-RPT-049"], model=True)
def decisions(ctx, s):
    run = manual_pass(ctx, s)
    check_id, rn, overall = run["checkId"], run["requestNumber"], run["report"]["overallStatus"]

    def agreement_count():
        r = ctx.http.get("/api/v1/decision-agreement?serviceCode=demo-manual")
        s.status_code(r, 200, description="decision agreement read -> 200")
        return sum(x["count"] for x in r.json or [] if x["versionNumber"] == 1 and x["overallStatus"] == overall
                   and x["employeeDecision"] == "APPROVED")

    before = agreement_count()
    r = ctx.http.post(f"/api/v1/checks/{check_id}/decision", {"employeeDecision": "APPROVED"})
    s.status_code(r, 400, "RPT-400-DECISION-INCOMPLETE", "decision without decidedBy -> 400 RPT-400-DECISION-INCOMPLETE")
    r = ctx.http.post(f"/api/v1/checks/{check_id}/decision", {"employeeDecision": "MAYBE", "decidedBy": EMPLOYEE})
    s.status_code(r, 400, "RPT-400-DECISION-INCOMPLETE", "decision code MAYBE -> 400 RPT-400-DECISION-INCOMPLETE")
    r = ctx.http.post(f"/api/v1/checks/{check_id}/decision", {"employeeDecision": "APPROVED", "decidedBy": EMPLOYEE})
    s.status_code(r, 201, description="APPROVED -> 201", hard=True)
    s.expect(r.json["approvalApiExecuted"] is False and r.json["employeeDecision"] == "APPROVED"
             and r.json["decidedBy"] == EMPLOYEE and r.json["checkId"] == check_id and r.json["decidedAt"],
             "recorded decision answered, approvalApiExecuted=false", r.short())
    r = ctx.http.post(f"/api/v1/checks/{check_id}/decision", {"employeeDecision": "REJECTED", "decidedBy": EMPLOYEE})
    s.status_code(r, 409, "RPT-409-DECISION-ALREADY-RECORDED", "second decision -> 409 RPT-409-DECISION-ALREADY-RECORDED")
    r = ctx.http.get(f"/api/v1/check-reports/{check_id}")
    d = (r.json or {}).get("decision") or {}
    s.expect(d.get("employeeDecision") == "APPROVED" and d.get("approvalApiExecuted") is False,
             "the report carries the first decision", r.short())
    r = ctx.http.get("/api/v1/checks?" + urllib.parse.urlencode({"serviceCode": "demo-manual", "requestNumber": rn}))
    row = next((c for c in (r.json or {}).get("checks", []) if c["checkId"] == check_id), {})
    s.expect(row.get("employeeDecision") == "APPROVED", "list of the request shows the decision", row)
    after = agreement_count()
    s.expect(after == before + 1, f"decision agreement (v1, {overall}, APPROVED) counted +1 ({before} -> {after})", "")
    r = ctx.http.get("/api/v1/decision-agreement")
    s.status_code(r, 400, "RPT-400-SERVICE-CODE-MISSING", "agreement without serviceCode -> 400")


@scenario("decisions", "Decisions refused on a non-completed and an unknown Check", ["TC-RPT-040", "TC-RPT-045"])
def decisions_refused(ctx, s):
    check_id = awaiting_check(ctx, s, label="DEC-WAIT")
    r = ctx.http.post(f"/api/v1/checks/{check_id}/decision", {"employeeDecision": "REJECTED", "decidedBy": EMPLOYEE})
    s.status_code(r, 409, "RPT-409-CHECK-NOT-COMPLETED", "decision on AWAITING_DOCUMENTS -> 409 RPT-409-CHECK-NOT-COMPLETED")
    r = ctx.http.post(f"/api/v1/checks/{UNKNOWN_ID}/decision", {"employeeDecision": "REJECTED", "decidedBy": EMPLOYEE})
    s.status_code(r, 404, "RPT-404-CHECK-NOT-FOUND", "decision on an unknown Check -> 404")


# -------------------------------------------------------------------------------------- approval
@scenario("approval", "Approval API: refusals before any call on an approval-enabled service",
          ["TC-INT-016", "TC-INT-017"])
def approval_refusals(ctx, s):
    check_id = awaiting_check(ctx, s, "demo-approval", "APR-WAIT")
    before = len(stub_lines())
    r = ctx.http.post(f"/api/v1/checks/{check_id}/decision", {"employeeDecision": "APPROVED", "decidedBy": EMPLOYEE})
    s.status_code(r, 409, "RPT-409-CHECK-NOT-COMPLETED", "APPROVED on a non-completed Check -> 409, before any call")
    r = ctx.http.post(f"/api/v1/checks/{check_id}/decision", {"employeeDecision": "APPROVED"})
    s.status_code(r, 400, "RPT-400-DECISION-INCOMPLETE", "APPROVED without decidedBy -> 400, before any call")
    time.sleep(0.5)
    s.expect(len(stub_lines()) == before, "the stub received no call", stub_lines()[before:])


@scenario("approval", "Approval API: 500 -> INT-502, slow -> INT-504 (nothing recorded), ok -> executed, then 409",
          ["TC-INT-019", "TC-INT-020", "TC-INT-037", "TC-INT-038", "TC-INT-040", "TC-INT-041", "TC-INT-042",
           "TC-INT-043", "TC-INT-018", "TC-INT-039"], model=True)
def approval_api(ctx, s):
    run = approval_pass(ctx, s)
    check_id, rn = run["checkId"], run["requestNumber"]
    encoded = urllib.parse.quote(rn, safe="")
    expected_path = f"POST /requests/{encoded}/approve"
    body = {"employeeDecision": "APPROVED", "decidedBy": EMPLOYEE}
    try:
        # 500 -> 502, nothing recorded, exactly one call
        s.require(stub_mode("500") == "500", "stub mode 500")
        before = len(stub_lines())
        r = ctx.http.post(f"/api/v1/checks/{check_id}/decision", body)
        s.status_code(r, 502, "INT-502-APPROVAL-API-FAILED", "stub 500 -> 502 INT-502-APPROVAL-API-FAILED")
        time.sleep(0.5)
        new = [line for line in stub_lines()[before:] if "POST " in line]
        s.expect(len(new) == 1 and expected_path in new[0], f"exactly one call, never retried ({expected_path})", new)
        rep = ctx.http.get(f"/api/v1/check-reports/{check_id}").json or {}
        s.expect(rep.get("decision") is None, "no decision recorded after the failed call", rep.get("decision"))
        # slow -> 504 within the 2 s approval timeout, nothing recorded
        s.require(stub_mode("slow") == "slow", "stub mode slow")
        r = ctx.http.post(f"/api/v1/checks/{check_id}/decision", body, timeout=60)
        s.status_code(r, 504, "INT-504-APPROVAL-API-TIMED-OUT", "stub slow -> 504 INT-504-APPROVAL-API-TIMED-OUT")
        s.expect(r.elapsed < 8, f"answered after the approval timeout, not the stub delay ({r.elapsed:.1f}s)", r.elapsed)
        rep = ctx.http.get(f"/api/v1/check-reports/{check_id}").json or {}
        s.expect(rep.get("decision") is None, "no decision recorded after the timeout", rep.get("decision"))
        # ok -> 201 executed, the call carries the encoded request number and the JSON body
        s.require(stub_mode("ok") == "ok", "stub mode ok")
        before = len(stub_lines())
        r = ctx.http.post(f"/api/v1/checks/{check_id}/decision", body)
        s.status_code(r, 201, description="stub ok -> APPROVED 201", hard=True)
        s.expect(r.json["approvalApiExecuted"] is True and r.json["employeeDecision"] == "APPROVED",
                 "approvalApiExecuted=true", r.short())
        time.sleep(0.5)
        new = [line for line in stub_lines()[before:] if "POST " in line]
        s.expect(len(new) == 1 and expected_path in new[0], f"stub saw {expected_path}", new)
        if new and "body=" in new[0]:
            try:
                sent = json.loads(new[0].split("body=", 1)[1])
            except ValueError:
                sent = new[0]
            s.expect(sent == {"checkId": check_id, "decidedBy": EMPLOYEE}, "JSON body {checkId, decidedBy}", sent)
        # second decision -> 409, refused before any call
        before = len(stub_lines())
        r = ctx.http.post(f"/api/v1/checks/{check_id}/decision", body)
        s.status_code(r, 409, "RPT-409-DECISION-ALREADY-RECORDED", "second APPROVED -> 409 before any call")
        time.sleep(0.5)
        s.expect(len(stub_lines()) == before, "no stub call for the refused second decision", stub_lines()[before:])
        rep = ctx.http.get(f"/api/v1/check-reports/{check_id}").json or {}
        s.expect((rep.get("decision") or {}).get("approvalApiExecuted") is True, "report: decision executed", rep.get("decision"))
    finally:
        try:
            stub_mode("ok")
        except Exception:
            pass


@scenario("approval", "Approval API: a REJECTED decision never calls the Approval API", ["TC-INT-013"], model=True)
def approval_rejected(ctx, s):
    run = approval_png(ctx, s)
    check_id, rn = run["checkId"], run["requestNumber"]
    before = len(stub_lines())
    r = ctx.http.post(f"/api/v1/checks/{check_id}/decision", {"employeeDecision": "REJECTED", "decidedBy": EMPLOYEE})
    s.status_code(r, 201, description="REJECTED -> 201")
    s.expect(r.json and r.json.get("approvalApiExecuted") is False, "approvalApiExecuted=false", r.short())
    time.sleep(0.5)
    s.expect(len(stub_lines()) == before, "no stub call", stub_lines()[before:])


# -------------------------------------------------------------------------------------- refusals
@scenario("refusals", "Start refusals: unknown service, incomplete start, unreadable body, unsupported Content-Type",
          ["TC-CHK-003", "TC-CHK-001", "TC-INT-001"])
def refusals_start(ctx, s):
    rn = ctx.request_number("REFUSE")
    r = ctx.http.post("/api/v1/checks", {"serviceCode": "no-such-service-e2e", "requestNumber": rn, "employeeId": EMPLOYEE})
    s.status_code(r, 422, "CHK-422-SERVICE-NOT-AVAILABLE")
    s.expect("checkId" not in (r.json or {}), "no identifier returned", r.short())
    r = ctx.http.post("/api/v1/checks", {"serviceCode": "demo-manual", "requestNumber": rn})
    s.status_code(r, 400, "CHK-400-START-INCOMPLETE", "start without employeeId -> 400 CHK-400-START-INCOMPLETE")
    r = ctx.http.post("/api/v1/checks", {"serviceCode": "demo-manual", "requestNumber": rn, "employeeId": "   "})
    s.status_code(r, 400, "CHK-400-START-INCOMPLETE", "start with a blank employeeId -> 400 CHK-400-START-INCOMPLETE")
    r = ctx.http.post("/api/v1/checks", b"{not json", headers={"Content-Type": "application/json"})
    s.status_code(r, 400, "INT-400-REQUEST-INVALID", "unreadable JSON -> 400 INT-400-REQUEST-INVALID")
    r = ctx.http.post("/api/v1/checks", b"serviceCode=demo-manual", headers={"Content-Type": "text/plain"})
    s.status_code(r, 400, "INT-400-REQUEST-INVALID", "Content-Type text/plain -> 400 INT-400-REQUEST-INVALID")


@scenario("refusals", "Upload refusals and limits on a waiting manual Check",
          ["TC-INT-002", "TC-DOC-013", "TC-DOC-014", "TC-INT-033", "TC-DOC-027", "TC-INT-009", "TC-INT-008",
           "TC-INT-100", "TC-DOC-015", "TC-INT-010"])
def refusals_upload(ctx, s):
    check_id = awaiting_check(ctx, s, label="UPL")
    pdf = pdf_for("X", "3.0", "120")
    r = ctx.http.upload(check_id, "PASSPORT", "p.pdf", pdf)
    s.status_code(r, 422, "DOC-422-DOCUMENT-TYPE-NOT-OF-SERVICE", "type the version does not require -> 422")
    r = ctx.http.upload(check_id, "TRANSCRIPT", "empty.pdf", b"")
    s.status_code(r, 400, "DOC-400-INCOMPLETE-UPLOAD", "0-byte file -> 400 DOC-400-INCOMPLETE-UPLOAD")
    r = ctx.http.upload(check_id, None, "notype.pdf", pdf, omit_type=True)
    s.status_code(r, 400, {"INT-400-REQUEST-INVALID", "DOC-400-INCOMPLETE-UPLOAD"}, "no documentType part -> 400")
    r = ctx.http.upload(check_id, "TRANSCRIPT", "../../etc/passwd", pdf)
    s.status_code(r, 201, description="a path-shaped file name is accepted as text -> 201")
    s.expect(r.json and r.json.get("fileName") == "../../etc/passwd", "file name kept as text", r.short())
    r = ctx.http.upload(check_id, "TRANSCRIPT", "second.pdf", pdf)
    s.status_code(r, 201, description="a second upload of the same type -> 201")
    big = b"%PDF-1.4\n" + b"0" * (MAX_FILE_SIZE + 1024)
    r = ctx.http.upload(check_id, "TRANSCRIPT", "oversized.pdf", big)
    s.status_code(r, 201, description="file above the 10 MB maximum -> 201 with a notice")
    s.expect(r.json and r.json.get("oversized") is True and r.json.get("notice"), "oversized=true with notice", r.short())
    r = ctx.http.get(f"/api/v1/checks/{check_id}/documents")
    names = [d["fileName"] for d in r.json or []]
    s.expect(names == ["../../etc/passwd", "second.pdf", "oversized.pdf"], "both same-type uploads listed in order",
             names)
    r = ctx.http.get(f"/api/v1/uploaded-documents?checkId={check_id}")
    s.status_code(r, 200, description="DOC listing -> 200")
    s.expect([d["fileName"] for d in r.json or []] == names, "DOC listing equals the INT relay", r.short())
    try:
        r = ctx.http.upload(check_id, "TRANSCRIPT", "too-big.pdf", b"0" * (REQUEST_LIMIT + 1024 * 1024))
        s.status_code(r, 413, "INT-413-UPLOAD-TOO-LARGE", "request above the 12 MB request limit -> 413")
    except (ConnectionError, urllib.error.URLError) as e:
        s.expect(False, "request above the request limit -> 413 (connection dropped instead)", repr(e))
    r = ctx.http.upload(UNKNOWN_ID, "TRANSCRIPT", "x.pdf", pdf)
    s.status_code(r, 404, "RPT-404-CHECK-NOT-FOUND", "upload for an unknown Check -> 404")


@scenario("refusals", "Maximum uploads per Check: 20 accepted, the 21st refused",
          ["TC-INT-097", "TC-DOC-071", "TC-DOC-072"])
def refusals_max_uploads(ctx, s):
    check_id = awaiting_check(ctx, s, label="MAXUP")
    pdf = pdf_for("X", "3.0", "120")
    accepted = 0
    for i in range(MAX_UPLOADS):
        r = ctx.http.upload(check_id, "TRANSCRIPT", f"u{i + 1:02d}.pdf", pdf)
        accepted += r.status == 201
    s.expect(accepted == MAX_UPLOADS, f"{MAX_UPLOADS} uploads accepted", accepted)
    r = ctx.http.upload(check_id, "TRANSCRIPT", "u21.pdf", pdf)
    s.status_code(r, 422, "DOC-422-UPLOAD-LIMIT-REACHED", "upload 21 -> 422 DOC-422-UPLOAD-LIMIT-REACHED")


@scenario("refusals", "Confirmation of an unknown Check; reads of unknown Checks",
          ["TC-CHK-038", "TC-INT-088", "TC-INT-094", "TC-RPT-030"])
def refusals_unknown(ctx, s):
    s.status_code(ctx.http.post(f"/api/v1/checks/{UNKNOWN_ID}/upload-confirmation", {}), 404, "CHK-404-CHECK-NOT-FOUND")
    s.status_code(ctx.http.get(f"/api/v1/check-reports/{UNKNOWN_ID}"), 404, "RPT-404-CHECK-NOT-FOUND")
    s.status_code(ctx.http.get(f"/api/v1/checks/{UNKNOWN_ID}"), 404, "RPT-404-CHECK-NOT-FOUND")
    s.status_code(ctx.http.get(f"/api/v1/checks/{UNKNOWN_ID}/required-document-types"), 404, "RPT-404-CHECK-NOT-FOUND")
    s.status_code(ctx.http.get(f"/api/v1/active-checks/{UNKNOWN_ID}"), 404, "CHK-404-ACTIVE-CHECK-NOT-FOUND")


@scenario("refusals", "Non-numeric identifiers are refused by each module's own code", ["TC-INT-005"])
def refusals_ids(ctx, s):
    h = ctx.http
    for resp, status, code in [
        (h.get("/api/v1/check-reports/abc"), 400, "INT-400-REQUEST-INVALID"),
        (h.get("/api/v1/checks/abc/documents"), 400, "INT-400-REQUEST-INVALID"),
        (h.get("/api/v1/checks/abc/required-document-types"), 400, "INT-400-REQUEST-INVALID"),
        (h.post("/api/v1/checks/abc/upload-confirmation", {}), 400, "INT-400-REQUEST-INVALID"),
        (h.post("/api/v1/checks/abc/decision", {"employeeDecision": "REJECTED", "decidedBy": EMPLOYEE}), 400,
         "INT-400-REQUEST-INVALID"),
        (h.upload("abc", "TRANSCRIPT", "x.pdf", b"%PDF-1.4"), 400, "INT-400-REQUEST-INVALID"),
        (h.get("/api/v1/checks/abc"), 400, "RPT-400-CHECK-ID-INVALID"),
        (h.get("/api/v1/active-checks/abc"), 400, "CHK-400-CHECK-ID-INVALID"),
        (h.get("/api/v1/uploaded-documents?checkId=abc"), 400, "DOC-400-CHECK-ID-REQUIRED"),
        (h.get("/api/v1/uploaded-documents"), 400, "DOC-400-CHECK-ID-REQUIRED"),
    ]:
        s.status_code(resp, status, code)


@scenario("refusals", "Lists of a request without their keys are refused", ["TC-RPT-035", "TC-INT-090"])
def refusals_list_keys(ctx, s):
    for path in ("/api/v1/checks?serviceCode=demo-manual", "/api/v1/checks?requestNumber=x", "/api/v1/check-reports",
                 "/api/v1/check-reports?serviceCode=demo-manual"):
        s.status_code(ctx.http.get(path), 400, "RPT-400-REQUEST-KEYS-MISSING")


# ------------------------------------------------------------------------------------- lifecycle
@scenario("lifecycle", "Active Check of a waiting manual Check: status AWAITING_DOCUMENTS, upload-window deadline",
          ["TC-CHK-083", "TC-CHK-086"])
def lifecycle_active(ctx, s):
    started = dt.datetime.now(dt.timezone.utc)
    check_id = awaiting_check(ctx, s, label="LIFE")
    r = ctx.http.get(f"/api/v1/active-checks/{check_id}")
    s.status_code(r, 200, hard=True)
    s.expect(set(r.json) == {"checkId", "checkStatus", "deadlineAt"}, "only identifier, status and deadline", r.json)
    s.expect(r.json["checkStatus"] == "AWAITING_DOCUMENTS", "AWAITING_DOCUMENTS", r.json)
    deadline = dt.datetime.fromisoformat(r.json["deadlineAt"])
    delta = (deadline - started).total_seconds() / 60
    s.expect(UPLOAD_WINDOW_MIN - 1 <= delta <= UPLOAD_WINDOW_MIN + 1, f"deadline = start + {UPLOAD_WINDOW_MIN} min",
             f"{delta:.2f} min")


@scenario("lifecycle", "After the Check ends: no Active Check, and its uploads are deleted",
          ["TC-CHK-085", "TC-DOC-052", "TC-CHK-028"], model=True)
def lifecycle_end(ctx, s):
    run = manual_pass(ctx, s)
    check_id = run["checkId"]
    s.status_code(ctx.http.get(f"/api/v1/active-checks/{check_id}"), 404, "CHK-404-ACTIVE-CHECK-NOT-FOUND",
                  "active check of an ended Check -> 404")
    r = ctx.http.get(f"/api/v1/checks/{check_id}/documents")
    s.expect(r.status == 200 and r.json == [], "INT: uploads of the ended Check listed empty (REQ-DOC-054)", r.short())
    r = ctx.http.get(f"/api/v1/uploaded-documents?checkId={check_id}")
    s.expect(r.status == 200 and r.json == [], "DOC: uploaded documents deleted after the end", r.short())


# ================================================================================ restart groups
# These groups restart the local app in another MODE — a profile override (local/e2e-override.properties,
# imported by the local profile when present) and/or package folders parked out of the package directory —
# through setup_fixtures.apply_mode (no rebuild). The runner restores the normal mode at the end of every run.
EXPIRY_MODE = {"aias.check.upload-window": "PT1M", "aias.check.deadline-check-interval": "PT5S"}
MODEL_LOG_DEBUG = {  # debug lines that name model calls and host-file reads (no document content, no data)
    "logging.level.io.agenticai.chk.adapter.SpringAiComparisonAdapter": "DEBUG",
    "logging.level.io.agenticai.doc.adapter.SpringAiDocumentReadingAdapter": "DEBUG",
    "logging.level.io.agenticai.doc.service.DocumentReadStep": "DEBUG",
    "logging.level.io.agenticai.doc.adapter.GuardedFileSystemAdapter": "DEBUG",
}
NOT_PERMITTED_MODE = {"aias.documents.data-class": "REAL", **MODEL_LOG_DEBUG}
# limits: 1 KB maximum file size, 1 row per query, data class REAL with the reading model still tier FREE (so a
# scanned document is held back, REQ-DOC-058) and the comparison model declared APPROVED for this local test only
# (the data stays synthetic) so the Check COMPLETES and its document outcomes are observable in the report.
LIMITS_MODE = {"aias.check.max-file-size": "1KB", "aias.check.max-rows": "1", "aias.documents.data-class": "REAL",
               "aias.check.comparison-model.tier": "APPROVED", **MODEL_LOG_DEBUG}
EXTRA_CONNECTION = {"name": "local-extra", "type": "mcp", "endpoint": "mcp:local-oracle", "query-tool": "query",
                    "dialect": "oracle", "credential-reference": "LOCAL_ORACLE_CREDENTIAL", "read-only": "true",
                    "limited-to-views": "false"}
COMPARISON_CALL = "CHK comparison model: calling model"
READING_CALL = "DOC reading model: calling model"


def use_mode(ctx, s, override=None, parked=fx.PARKED_BY_DEFAULT, force=False):
    """Puts the app in the mode (restarting it only when something differs, or when ``force``)."""
    props = override or {}
    names = [v for k, v in props.items() if k.startswith("aias.registry.connections[") and k.endswith("].name")]
    label = ", ".join([f"{k}={v}" for k, v in props.items()
                       if not k.startswith(("logging.", "aias.registry.connections["))]
                      + ([f"connections={names}"] if names else [])) or "normal"
    extra = sorted(set(parked) - fx.PARKED_BY_DEFAULT)
    moved_in = sorted(fx.PARKED_BY_DEFAULT - set(parked))
    try:
        restarted = fx.apply_mode(override, parked, force_restart=force)
    except (SystemExit, subprocess.CalledProcessError) as e:
        raise Hard(f"the app could not be restarted in mode [{label}]: {e}")
    ctx.mode_touched = True
    if restarted:
        what = f"mode [{label}]" + (f", parked {extra}" if extra else "") + (f", moved in {moved_in}" if moved_in else "")
        ctx.restarts.append({"scenario": s.name, "mode": what, "at": dt.datetime.now().isoformat(timespec="seconds")})
        print(f"    (app restarted: {what})", flush=True)
    return restarted


def log_offset():
    try:
        return fx.APP_LOG.stat().st_size
    except OSError:
        return 0


def log_since(offset):
    try:
        with open(fx.APP_LOG, "rb") as f:
            f.seek(offset)
            return f.read().decode(errors="replace")
    except OSError:
        return ""


def wait_status(ctx, check_id, statuses, timeout):
    deadline = time.time() + timeout
    while True:
        r = ctx.http.get(f"/api/v1/check-reports/{check_id}")
        if (r.status == 200 and r.json and r.json.get("status") in statuses) or time.time() > deadline:
            return r
        time.sleep(2)


def uploads_gone(ctx, s, check_id, label):
    r = ctx.http.get(f"/api/v1/checks/{check_id}/documents")
    s.expect(r.status == 200 and r.json == [], f"INT: no upload listed after the {label} (REQ-DOC-054)", r.short())
    r = ctx.http.get(f"/api/v1/uploaded-documents?checkId={check_id}")
    s.expect(r.status == 200 and r.json == [], f"DOC: uploads deleted after the {label}", r.short())


def failed_report(s, report, reason):
    s.expect(report.get("status") == "FAILED" and report.get("failureReason") == reason, f"FAILED / {reason}",
             {k: report.get(k) for k in ("status", "failureReason", "failureDetail")})
    s.expect(report.get("overallStatus") is None and report.get("findings") == [],
             "no Overall Status and no finding", {k: report.get(k) for k in ("overallStatus", "findings")})


def load_rows(ctx, s):
    r = ctx.http.get("/api/v1/load-results")
    s.status_code(r, 200, description="load report -> 200", hard=True)
    return r.json


def load_row(rows, kind, name):
    return next((x for x in rows if x["subjectKind"] == kind and x["subjectName"] == name), None)


def service_codes(ctx):
    r = ctx.http.get("/api/v1/services")
    return [x["serviceCode"] for x in r.json or []] if r.status == 200 else None


# ------------------------------------------------------------------------------------ interrupted
@scenario("interrupted", "A waiting Check is ended INTERRUPTED by a restart; its Active Check and uploads are gone",
          ["TC-CHK-037", "TC-CHK-052"])
def interrupted(ctx, s):
    use_mode(ctx, s)
    check_id = awaiting_check(ctx, s, label="INTR")
    r = ctx.http.upload(check_id, "TRANSCRIPT", "intr.pdf", pdf_for("X", "3.0", "120"))
    s.status_code(r, 201, description="upload while waiting -> 201", hard=True)
    r = ctx.http.get(f"/api/v1/active-checks/{check_id}")
    s.expect(r.status == 200 and r.json.get("checkStatus") == "AWAITING_DOCUMENTS", "Active Check before the restart",
             r.short())
    use_mode(ctx, s, force=True)
    log = log_since(0)
    s.expect("unfinished Check(s) ended INTERRUPTED" in log and "CHK start-up recovery: 0 unfinished" not in log,
             "the start-up recovery ended at least one unfinished Check INTERRUPTED (log)",
             [line for line in log.splitlines() if "start-up recovery" in line][:2])
    r = ctx.http.get(f"/api/v1/check-reports/{check_id}")
    s.status_code(r, 200, description="report after the restart -> 200", hard=True)
    failed_report(s, r.json, "INTERRUPTED")
    s.expect("restart" in (r.json.get("failureDetail") or ""), "detail: interrupted by a restart of the service",
             r.json.get("failureDetail"))
    s.status_code(ctx.http.get(f"/api/v1/active-checks/{check_id}"), 404, "CHK-404-ACTIVE-CHECK-NOT-FOUND",
                  "Active Check deleted by the recovery (REQ-CHK-081) -> 404")
    uploads_gone(ctx, s, check_id, "restart")
    r = ctx.http.upload(check_id, "TRANSCRIPT", "late.pdf", pdf_for("X", "3.0", "120"))
    s.status_code(r, 409, "INT-409-CHECK-NOT-AWAITING-DOCUMENTS", "upload after the interruption -> 409 INT-409")


# ----------------------------------------------------------------------------------------- expiry
@scenario("expiry", "Upload window PT1M: a waiting Check ends FAILED / UPLOAD_WINDOW_EXPIRED; Active Check and uploads gone",
          ["TC-CHK-040", "TC-CHK-041", "TC-CHK-083", "TC-CHK-028", "TC-DOC-052"])
def expiry(ctx, s):
    use_mode(ctx, s, EXPIRY_MODE)
    started = dt.datetime.now(dt.timezone.utc)
    check_id = awaiting_check(ctx, s, label="EXP")
    r = ctx.http.get(f"/api/v1/active-checks/{check_id}")
    s.status_code(r, 200, hard=True)
    delta = (dt.datetime.fromisoformat(r.json["deadlineAt"]) - started).total_seconds()
    s.expect(55 <= delta <= 65, f"deadline = start + 1 min ({delta:.1f} s)", r.json)
    r = ctx.http.upload(check_id, "TRANSCRIPT", "exp.pdf", pdf_for("X", "3.0", "120"))
    s.status_code(r, 201, description="upload inside the window -> 201", hard=True)
    time.sleep(max(0.0, 30 - (dt.datetime.now(dt.timezone.utc) - started).total_seconds()))
    r = ctx.http.get(f"/api/v1/active-checks/{check_id}")
    s.expect(r.status == 200 and r.json.get("checkStatus") == "AWAITING_DOCUMENTS",
             "boundary: 30 s into the window the Check still waits (TC-CHK-041)", r.short())
    r = wait_status(ctx, check_id, ("FAILED", "COMPLETED"), 120)
    s.require(r.status == 200 and r.json.get("status") == "FAILED", "the Check ended within 120 s", r.short())
    report = r.json
    failed_report(s, report, "UPLOAD_WINDOW_EXPIRED")
    ended = (dt.datetime.fromisoformat(report["endedAt"]) - dt.datetime.fromisoformat(report["startedAt"])).total_seconds()
    s.expect(60 <= ended <= 60 + 5 + 10, f"ended {ended:.1f} s after the start (window 60 s + check interval 5 s)",
             ended)
    s.status_code(ctx.http.get(f"/api/v1/active-checks/{check_id}"), 404, "CHK-404-ACTIVE-CHECK-NOT-FOUND",
                  "Active Check gone after the expiry -> 404")
    uploads_gone(ctx, s, check_id, "expiry")
    r = ctx.http.upload(check_id, "TRANSCRIPT", "late.pdf", pdf_for("X", "3.0", "120"))
    s.status_code(r, 409, "INT-409-CHECK-NOT-AWAITING-DOCUMENTS", "upload after the expiry -> 409 INT-409")
    r = ctx.http.post(f"/api/v1/checks/{check_id}/upload-confirmation", {})
    s.status_code(r, 409, "CHK-409-CHECK-NOT-AWAITING-DOCUMENTS", "confirmation after the expiry -> 409 CHK-409")


# ----------------------------------------------------------------------------------- notpermitted
@scenario("notpermitted", "FREE comparison model on REAL data: a path Check ends FAILED / MODEL_NOT_PERMITTED, nothing sent",
          ["TC-CHK-046", "TC-CHK-047"])
def not_permitted_path(ctx, s):
    use_mode(ctx, s, NOT_PERMITTED_MODE)
    offset = log_offset()
    check_id = start(ctx, s, "demo-path", ctx.request_number("NOPERM-PATH"), "RUNNING")
    report = wait_end(ctx, s, check_id)
    failed_report(s, report, "MODEL_NOT_PERMITTED")
    s.expect("nothing was sent" in (report.get("failureDetail") or ""), "detail: nothing was sent to the model",
             report.get("failureDetail"))
    s.status_code(ctx.http.get(f"/api/v1/active-checks/{check_id}"), 404, "CHK-404-ACTIVE-CHECK-NOT-FOUND")
    time.sleep(0.5)
    log = log_since(offset)
    s.expect("tier FREE with data class REAL; nothing sent" in log, "log: the comparison gate refused the call", "")
    s.expect(COMPARISON_CALL not in log and READING_CALL not in log, "log: no call reached any model",
             [line[-200:] for line in log.splitlines() if "calling model" in line])


@scenario("notpermitted", "FREE reading model on REAL data: a scanned (PNG) upload is never sent; the Check ends MODEL_NOT_PERMITTED",
          ["TC-DOC-064", "TC-CHK-047"])
def not_permitted_png(ctx, s):
    use_mode(ctx, s, NOT_PERMITTED_MODE)
    rn = ctx.request_number("NOPERM-PNG")
    check_id = start(ctx, s, "demo-manual", rn, "AWAITING_DOCUMENTS")
    png = synth.make_png(synth.transcript_lines(rn, "3.55", "126"))
    r = ctx.http.upload(check_id, "TRANSCRIPT", "scan.png", png, "image/png")
    s.status_code(r, 201, description="PNG upload -> 201", hard=True)
    offset = log_offset()
    r = ctx.http.post(f"/api/v1/checks/{check_id}/upload-confirmation", {})
    s.status_code(r, 202, description="confirm -> 202", hard=True)
    report = wait_end(ctx, s, check_id)
    failed_report(s, report, "MODEL_NOT_PERMITTED")
    time.sleep(0.5)
    log = log_since(offset)
    s.expect("DOC reading documentType=TRANSCRIPT" in log, "log: the reading step handled the PNG", "")
    s.expect(READING_CALL not in log, "log: the PNG was NOT sent to the FREE reading model (REQ-DOC-058)",
             [line[-200:] for line in log.splitlines() if READING_CALL in line])
    s.expect(COMPARISON_CALL not in log, "log: nothing sent to the comparison model", "")
    s.expect(report.get("documents") == [],
             "a FAILED report keeps no document outcome (the UNREADABLE/MODEL_NOT_PERMITTED outcome is asserted in the "
             "limits group, where the Check completes)", report.get("documents"))


# ----------------------------------------------------------------------------------------- limits
def hostfile_too_large(ctx, s, service, label, source_mode, subject):
    use_mode(ctx, s, LIMITS_MODE)
    offset = log_offset()
    ctx.model_gate(s)
    check_id = start(ctx, s, service, ctx.request_number(label), "RUNNING")
    report = wait_end(ctx, s, check_id)
    ctx.judge_model_failure(report)
    s.require(report["status"] == "COMPLETED", "COMPLETED",
              {k: report.get(k) for k in ("status", "failureReason", "failureDetail")})
    t = doc_of(report, "TRANSCRIPT")
    s.expect(len(t) == 1 and t[0]["readStatus"] == "UNREADABLE" and t[0]["unreadableReason"] == "TOO_LARGE"
             and t[0]["sourceMode"] == source_mode, f"TRANSCRIPT UNREADABLE / TOO_LARGE ({source_mode})", t)
    detail = t[0].get("detail", "") if t else ""
    s.expect(detail.startswith(subject) and "larger than the maximum file size of 1024 bytes" in detail
             and "its content was not read" in detail, "detail names the size and the 1 KB maximum, content not read",
             detail)
    tf = finding_of(report, "TRANSCRIPT")
    s.expect(tf and tf["outcome"] == "UNDETERMINED", "TRANSCRIPT finding UNDETERMINED", tf)
    s.expect(report["overallStatus"] != "COMPLIANT", f"overall not COMPLIANT ({report['overallStatus']})",
             report["overallStatus"])
    return report, log_since(offset)


@scenario("limits", "max-file-size 1 KB: a 4.5 KB path document is UNREADABLE / TOO_LARGE without being read",
          ["TC-DOC-025", "TC-DOC-018"], model=True)
def limits_path_too_large(ctx, s):
    _, log = hostfile_too_large(ctx, s, "demo-path-big", "BIG-PATH", "path", '"transcripts/big.pdf" is')
    s.expect('DOC host file: read ' not in log, "log: the file was measured, never read", 
             [line[-160:] for line in log.splitlines() if "DOC host file: read " in line])


@scenario("limits", "max-file-size 1 KB: a 2000-byte BLOB is UNREADABLE / TOO_LARGE, measured before any byte is read",
          ["TC-DOC-024", "TC-DOC-018"], model=True)
def limits_blob_too_large(ctx, s):
    hostfile_too_large(ctx, s, "demo-blob-big", "BIG-BLOB", "blob", "the content column is 2000 bytes")


def tiny_png(request_number):
    return synth.make_png(["SYNTHETIC TRANSCRIPT", request_number, "GPA 3.66 CREDITS 130"], scale=1, margin=4)


def limits_approval(ctx, s):
    """demo-approval Check in the limits mode: a small PNG and a PDF transcript (both under 1 KB)."""
    return shared(ctx, s, "limits_approval", lambda: manual_flow(
        ctx, s, "demo-approval", "LIMITS-APR",
        [("TRANSCRIPT", "scan.png", tiny_png, "image/png"),
         ("TRANSCRIPT", "transcript.pdf", lambda rn: pdf_for(rn, "3.66", "130"), "application/pdf")]))


@scenario("limits", "REAL data, FREE reading model: the PNG is UNREADABLE / MODEL_NOT_PERMITTED, the PDF beside it is READ",
          ["TC-DOC-065", "TC-DOC-064"], model=True)
def limits_reading_not_permitted(ctx, s):
    use_mode(ctx, s, LIMITS_MODE)
    offset = log_offset()
    run = limits_approval(ctx, s)
    report = run["report"]
    docs = doc_of(report, "TRANSCRIPT")
    png = [d for d in docs if d["readStatus"] == "UNREADABLE"]
    pdf = [d for d in docs if d["readStatus"] == "READ"]
    s.expect(len(docs) == 2, "two TRANSCRIPT outcomes (one per upload)", docs)
    s.expect(len(png) == 1 and png[0]["unreadableReason"] == "MODEL_NOT_PERMITTED"
             and "not sent" in (png[0].get("detail") or ""), "the PNG: UNREADABLE / MODEL_NOT_PERMITTED, not sent", png)
    s.expect(len(pdf) == 1 and pdf[0]["sourceMode"] == "manual", "the PDF: READ by text extraction", pdf)
    s.expect(READING_CALL not in log_since(offset), "log: nothing sent to the reading model", "")
    s.expect(report["comparisonModel"], "the comparison still ran (APPROVED tier)", report["comparisonModel"])


@scenario("limits", "max-rows 1: a query answering 2 rows is recorded unread ('more than 1 rows') -> NEEDS_MANUAL_REVIEW",
          ["TC-CHK-031", "TC-CHK-032"], model=True)
def limits_max_rows(ctx, s):
    use_mode(ctx, s, LIMITS_MODE)
    run = manual_flow(ctx, s, "demo-rows", "ROWS", [("TRANSCRIPT", "transcript.pdf",
                                                      lambda rn: pdf_for(rn, "3.62", "128"), "application/pdf")])
    report = run["report"]
    unread = {u["queryName"]: u["detail"] for u in report["unreadQueries"]}
    s.expect(unread.get("rows_probe") == "more than 1 rows", "rows_probe unread: 'more than 1 rows'", report["unreadQueries"])
    s.expect("request_echo" not in unread, "request_echo (1 row, exactly the maximum) read in full (TC-CHK-032)", unread)
    s.expect(doc_of(report, "TRANSCRIPT") and doc_of(report, "TRANSCRIPT")[0]["readStatus"] == "READ",
             "TRANSCRIPT READ", report["documents"])
    s.expect(report["overallStatus"] == "NEEDS_MANUAL_REVIEW", "overall NEEDS_MANUAL_REVIEW (an unread query)",
             [(f["condition"], f["outcome"]) for f in report["findings"]])


@scenario("limits", "Two simultaneous APPROVED decisions: one 201 (one Approval API call), one 409 after the lock",
          ["TC-INT-098"], model=True)
def concurrent_approvals(ctx, s):
    use_mode(ctx, s, LIMITS_MODE)
    run = limits_approval(ctx, s)
    check_id = run["checkId"]
    results = {}

    def send(label, decided_by, delay):
        time.sleep(delay)
        sent = time.time()
        r = ctx.http.post(f"/api/v1/checks/{check_id}/decision",
                          {"employeeDecision": "APPROVED", "decidedBy": decided_by})
        results[label] = (r, sent, time.time(), decided_by)

    try:
        s.require(stub_mode("delay", 1.0) == "delay", "stub mode delay (1 s, below the 2 s approval timeout)")
        before = len(stub_lines())
        threads = [threading.Thread(target=send, args=("A", "E2E-EMP-0001", 0.0)),
                   threading.Thread(target=send, args=("B", "E2E-EMP-0002", 0.3))]
        for t in threads:
            t.start()
        for t in threads:
            t.join(30)
        s.require(set(results) == {"A", "B"}, "both requests answered", list(results))
        statuses = sorted(r.status for r, *_ in results.values())
        s.expect(statuses == [201, 409], f"exactly one 201 and one 409 ({statuses})",
                 {k: v[0].short() for k, v in results.items()})
        winner = next((v for v in results.values() if v[0].status == 201), None)
        loser = next((v for v in results.values() if v[0].status == 409), None)
        if loser:
            s.status_code(loser[0], 409, "RPT-409-DECISION-ALREADY-RECORDED", "the second -> 409 RPT-409-DECISION-ALREADY-RECORDED")
        if winner and loser:
            s.expect(winner[0].json.get("approvalApiExecuted") is True and winner[0].json.get("decidedBy") == winner[3],
                     "the first: approvalApiExecuted=true", winner[0].short())
            s.expect(loser[2] >= winner[2] - 0.05 and loser[2] - loser[1] >= 0.4,
                     f"the second was answered only after the first's Approval API call "
                     f"(waited {loser[2] - loser[1]:.2f} s on the per-Check lock)", (winner[2], loser[1], loser[2]))
        time.sleep(0.5)
        calls = [line for line in stub_lines()[before:] if "POST " in line]
        s.expect(len(calls) == 1, "the stub received exactly ONE call", calls)
        rep = ctx.http.get(f"/api/v1/check-reports/{check_id}").json or {}
        d = rep.get("decision") or {}
        s.expect(winner and d.get("decidedBy") == winner[3] and d.get("approvalApiExecuted") is True,
                 "the report keeps the first decision only", d)
    finally:
        try:
            stub_mode("ok")
        except Exception:
            pass


# -------------------------------------------------------------------------------------- withdrawn
@scenario("withdrawn", "A package folder moved out: the service is WITHDRAWN, unlisted, read available=false, start refused",
          ["TC-REG-010", "TC-REG-088", "TC-REG-012", "TC-REG-027", "TC-CHK-002", "TC-INT-001"])
def withdrawn(ctx, s):
    use_mode(ctx, s, None, fx.PARKED_BY_DEFAULT | {"demo-blob"})
    row = load_row(load_rows(ctx, s), "SERVICE_PACKAGE", "demo-blob")
    s.expect(row and row["outcome"] == "WITHDRAWN" and row["serviceCode"] == "demo-blob" and row["versionNumber"] == 1,
             "load report: demo-blob WITHDRAWN (version 1 kept)", row)
    codes = service_codes(ctx)
    s.expect(codes is not None and "demo-blob" not in codes and "demo-manual" in codes,
             "GET /services no longer lists demo-blob", codes)
    r = ctx.http.get("/api/v1/services/demo-blob")
    s.status_code(r, 200, description="read by code -> 200")
    s.expect(r.json and r.json.get("available") is False and r.json.get("versionNumber") == 1,
             "read by code: available=false, version kept", r.short())
    r = ctx.http.post("/api/v1/checks", {"serviceCode": "demo-blob", "requestNumber": ctx.request_number("WDRN"),
                                         "employeeId": EMPLOYEE})
    s.status_code(r, 422, "CHK-422-SERVICE-NOT-AVAILABLE", "start for the withdrawn service -> 422 CHK-422-SERVICE-NOT-AVAILABLE")
    s.expect("checkId" not in (r.json or {}), "no Check created", r.short())


@scenario("withdrawn", "The folder put back: the service is available again with its stored version",
          ["TC-REG-011", "TC-REG-024"])
def withdrawn_restored(ctx, s):
    use_mode(ctx, s, None, fx.PARKED_BY_DEFAULT | {"demo-blob"})
    use_mode(ctx, s, None)
    row = load_row(load_rows(ctx, s), "SERVICE_PACKAGE", "demo-blob")
    s.expect(row and row["outcome"] == "UNCHANGED", "load report: demo-blob UNCHANGED (no new version)", row)
    codes = service_codes(ctx)
    s.expect(codes is not None and "demo-blob" in codes, "GET /services lists demo-blob again", codes)
    r = ctx.http.get("/api/v1/services/demo-blob")
    s.expect(r.status == 200 and r.json.get("available") is True and r.json.get("versionNumber") == 1
             and r.json.get("fetchMode") == "blob", "read by code: available=true, version 1, fetch blob", r.short())


# ------------------------------------------------------------------------------------- connection
@scenario("connection", "A package whose query names an unregistered connection is REJECTED at load (ghost-db)",
          ["TC-REG-032", "TC-REG-007", "TC-REG-051"])
def connection_unregistered(ctx, s):
    use_mode(ctx, s, fx.connections_with([EXTRA_CONNECTION]), fx.PARKED_BY_DEFAULT - {"demo-conn", "demo-noconn"})
    rows = load_rows(ctx, s)
    for name in ("local-oracle", "local-jdbc", "local-extra"):
        x = load_row(rows, "CONNECTION", name)
        s.expect(x and x["outcome"] in ("ACTIVATED", "UPDATED"), f"connection {name} ACTIVATED", x)
    x = load_row(rows, "SERVICE_PACKAGE", "demo-noconn")
    s.expect(x and x["outcome"] == "REJECTED" and 'connection "ghost-db"' in (x.get("reason") or ""),
             "demo-noconn REJECTED: its query uses the connection \"ghost-db\" (RULE-REG-005)", x)
    s.status_code(ctx.http.get("/api/v1/services/demo-noconn"), 404, "REG-404-SERVICE-NOT-FOUND",
                  "the rejected package was never registered -> 404")
    x = load_row(rows, "SERVICE_PACKAGE", "demo-conn")
    s.expect(x and x["outcome"] in ("REGISTERED", "UNCHANGED"), "demo-conn (query over local-extra) loaded", x)
    s.expect(len([r for r in rows if r["outcome"] == "REJECTED"]) == 1, "the load continued: one REJECTED row only",
             [r for r in rows if r["outcome"] == "REJECTED"])
    r = ctx.http.get("/api/v1/services/demo-conn")
    s.expect(r.status == 200 and r.json.get("available") is True, "demo-conn available", r.short())
    check_id = start(ctx, s, "demo-conn", ctx.request_number("CONN-OK"), "AWAITING_DOCUMENTS")
    s.expect(isinstance(check_id, int), "a Check of demo-conn starts while local-extra is activated", check_id)


@scenario("connection", "The connection removed from the activation config: REMOVED; start -> 422 CHK-422-CONNECTION-NOT-ACTIVATED",
          ["TC-REG-053", "TC-REG-055", "TC-CHK-004"])
def connection_removed(ctx, s):
    r = ctx.http.get("/api/v1/services/demo-conn")
    if not (r.status == 200 and r.json.get("available") is True):
        # demo-conn must be stored and available first (run alone, or after an earlier run withdrew it)
        use_mode(ctx, s, fx.connections_with([EXTRA_CONNECTION]), fx.PARKED_BY_DEFAULT - {"demo-conn"})
    use_mode(ctx, s, None, fx.PARKED_BY_DEFAULT - {"demo-conn"})
    rows = load_rows(ctx, s)
    x = load_row(rows, "CONNECTION", "local-extra")
    s.expect(x and x["outcome"] == "REMOVED", "load report: connection local-extra REMOVED", x)
    x = load_row(rows, "SERVICE_PACKAGE", "demo-conn")
    s.expect(x and x["outcome"] == "REJECTED" and 'connection "local-extra"' in (x.get("reason") or ""),
             "demo-conn's folder REJECTED this load (RULE-REG-005) — its stored version stays", x)
    r = ctx.http.get("/api/v1/services/demo-conn")
    s.expect(r.status == 200 and r.json.get("available") is True,
             "demo-conn keeps its previous state (available) — a rejected folder withdraws nothing", r.short())
    r = ctx.http.post("/api/v1/checks", {"serviceCode": "demo-conn", "requestNumber": ctx.request_number("CONN-GONE"),
                                         "employeeId": EMPLOYEE})
    s.status_code(r, 422, "CHK-422-CONNECTION-NOT-ACTIVATED",
                  "start -> 422 CHK-422-CONNECTION-NOT-ACTIVATED (RULE-REG-017, REQ-CHK-006)")
    s.expect("local-extra" in ((r.json or {}).get("detail") or ""), "the refusal names the connection", r.short())
    s.expect("checkId" not in (r.json or {}), "no Check created", r.short())


@scenario("connection", "Back to the normal registry: demo-conn's folder parked again -> WITHDRAWN", ["TC-REG-010"])
def connection_restored(ctx, s):
    use_mode(ctx, s, None)
    x = load_row(load_rows(ctx, s), "SERVICE_PACKAGE", "demo-conn")
    if x is None:
        # demo-conn was already withdrawn by an earlier run and no folder declares it: nothing to report
        r = ctx.http.get("/api/v1/services/demo-conn")
        s.expect(r.status == 404 or (r.status == 200 and r.json.get("available") is False),
                 "demo-conn unknown or withdrawn", r.short())
    else:
        s.expect(x["outcome"] == "WITHDRAWN", "load report: demo-conn WITHDRAWN", x)
    codes = service_codes(ctx)
    s.expect(codes is not None and "demo-conn" not in codes and "demo-noconn" not in codes,
             "neither connection fixture is listed", codes)


# ------------------------------------------------------------------------------------------- race
@scenario("race", "DOC-409-CHECK-ENDED through INT's upload (the race of ADR-INT-025)", ["TC-INT-096"])
def doc_check_ended_race(ctx, s):
    raise Skip("AMBIGUOUS", "NOT-DETERMINISTIC, not attempted: DOC answers DOC-409-CHECK-ENDED only when INT's status "
               "read (RPT, AWAITING_DOCUMENTS) precedes the Check's ending AND DOC's Ended-Check marker is committed "
               "before DOC's handover step 1a. Every ending path (CheckEndingService, start-up recovery) commits the "
               "RPT status first and records DOC's marker after that commit, and the multipart body is parsed "
               "before UploadService reads the status (resolve-lazily, argument resolution), so nothing an HTTP "
               "client controls can hold a request between INT's read and DOC's check. Only a thread-level test "
               "(a test double pausing between the two) reproduces it — out of scope for this API-only runner")


BASE_GROUPS = ["registry", "manual", "image", "path", "blob", "decisions", "approval", "refusals", "lifecycle"]
RESTART_GROUPS = ["interrupted", "expiry", "notpermitted", "limits", "withdrawn", "connection", "race"]
GROUPS = BASE_GROUPS + RESTART_GROUPS


# =========================================================================================== runner
TRANSIENT = ("SKIPPED-RATE", "SKIPPED-MODEL")


def run(ctx, selected):
    results = []
    for sc, fn in SCENARIOS:
        if selected and sc.group not in selected:
            continue
        s = run_one(ctx, sc, fn)
        if s.status in TRANSIENT and sc.model and not ctx.quota_hit:
            # a 503 / per-minute 429 is transient: drop the cached transient skips, wait, retry once
            first = s.reason
            for key, value in list(ctx.cache.items()):
                if isinstance(value, Skip) and value.status in TRANSIENT:
                    del ctx.cache[key]
            print(f"    retrying once after a transient model failure", flush=True)
            s = run_one(ctx, sc, fn)
            s.reason = (s.reason + " · " if s.reason else "") + f"retried once after: {first[:160]}"
        results.append(s)
    return results


def run_one(ctx, sc, fn):
    """Runs one scenario and returns its result."""
    s = Scenario(sc.group, sc.name, sc.tcs, sc.model)
    t0 = time.time()
    print(f"[{s.group}] {s.name} ...", flush=True)
    try:
        if s.group in BASE_GROUPS and ctx.mode_touched:
            use_mode(ctx, s)          # back to the normal mode (no restart when already there)
        fn(ctx, s)
        s.status = "PASSED" if all(ok for ok, _, _ in s.checks) else "FAILED"
    except Skip as e:
        s.status, s.reason = e.status, e.reason
        if any(not ok for ok, _, _ in s.checks):
            s.status = "FAILED"
            s.reason = "failed before the skip: " + e.reason
    except Hard as e:
        s.status, s.reason = "FAILED", f"stopped: {e}"
    except Exception as e:  # runner defect or transport failure
        s.status, s.reason = "ERROR", f"{type(e).__name__}: {e}"
        traceback.print_exc()
    s.duration = time.time() - t0
    failed = [c for c in s.checks if not c[0]]
    print(f"    -> {s.status} ({len(s.checks) - len(failed)}/{len(s.checks)} checks, {s.duration:.1f}s)"
          + (f" {s.reason}" if s.reason else ""), flush=True)
    for _, description, detail in failed:
        print(f"       FAIL {description}: {detail[:300]}", flush=True)
    return s


def write_outputs(ctx, results, prefix):
    groups = {}
    for s in results:
        g = groups.setdefault(s.group, {"passed": 0, "failed": 0, "skipped": 0, "ambiguous": 0, "error": 0})
        key = {"PASSED": "passed", "FAILED": "failed", "ERROR": "error", "AMBIGUOUS": "ambiguous"}.get(s.status, "skipped")
        g[key] += 1
    data = {
        "runId": ctx.run_id, "baseUrl": ctx.args.base_url, "date": dt.date.today().isoformat(),
        "comparisonModel": ctx.comparison_model,
        "modelCalls": {"comparison": ctx.comparison_calls, "reading": ctx.reading_calls},
        "quotaHit": ctx.quota_hit, "groups": groups,
        "scenarios": [{"group": s.group, "name": s.name, "tcs": s.tcs, "modelDependent": s.model,
                       "status": s.status, "reason": s.reason, "durationSec": round(s.duration, 1),
                       "checksCreated": s.checks_created,
                       "checks": [{"ok": ok, "check": d, "detail": det} for ok, d, det in s.checks]}
                      for s in results],
        "restarts": ctx.restarts,
        "survivingRecords": ctx.created,
    }
    Path(prefix + ".json").parent.mkdir(parents=True, exist_ok=True)
    Path(prefix + ".json").write_text(json.dumps(data, indent=2) + "\n")
    lines = [f"# aias E2E simulation run {ctx.run_id}", "",
             f"Base URL `{ctx.args.base_url}` · comparison model `{ctx.comparison_model}` · model calls: "
             f"{ctx.comparison_calls} comparison, {ctx.reading_calls} reading"
             + (f" · quota hit: {ctx.quota_hit}" if ctx.quota_hit else ""), "",
             "| Group | Passed | Failed | Skipped | Ambiguous | Error |", "|---|---|---|---|---|---|"]
    for g, c in groups.items():
        lines.append(f"| {g} | {c['passed']} | {c['failed']} | {c['skipped']} | {c['ambiguous']} | {c['error']} |")
    lines += ["", "## Scenarios", ""]
    for s in results:
        lines.append(f"- **{s.status}** [{s.group}] {s.name}" + (f" — {s.reason}" if s.reason else "")
                     + (f" ({', '.join(s.tcs)})" if s.tcs else ""))
        for ok, d, det in s.checks:
            if not ok:
                lines.append(f"  - FAIL {d}: `{det[:300]}`")
    if ctx.restarts:
        lines += ["", "## App restarts (restart groups)", ""]
        lines += [f"- {r['at']} — {r['mode']} (by '{r['scenario']}')" for r in ctx.restarts]
    lines += ["", "## Surviving records (synthetic, kept until the retention purge)", "",
              "| checkId | service | requestNumber | what |", "|---|---|---|---|"]
    lines += [f"| {r['checkId']} | {r['serviceCode']} | `{r['requestNumber']}` | {r['what']} |" for r in ctx.created]
    Path(prefix + "-run.md").write_text("\n".join(lines) + "\n")
    return data


def main():
    parser = argparse.ArgumentParser(description="aias end-to-end simulation (local only)")
    parser.add_argument("--base-url", default="http://localhost:7271")
    parser.add_argument("--only", action="append", choices=GROUPS, help="run only this group (repeatable)")
    parser.add_argument("--max-model-checks", type=int, default=14, help="comparison-call budget (default 14: 11 + retries)")
    parser.add_argument("--out", default=str(REPO / "logs" / f"e2e-simulation-{dt.date.today().isoformat()}"),
                        help="output prefix: writes <prefix>.json and <prefix>-run.md")
    parser.add_argument("--list", action="store_true", help="list the scenarios and exit")
    args = parser.parse_args()
    if args.list:
        for sc, _ in SCENARIOS:
            print(f"{sc.group:12} {'[model] ' if sc.model else '        '}{sc.name}  {' '.join(sc.tcs)}")
        return 0
    host = urllib.parse.urlparse(args.base_url).hostname
    if host not in ("localhost", "127.0.0.1", "::1"):
        print(f"refused: {args.base_url} is not a local Dev/Test address", file=sys.stderr)
        return 2
    ctx = Ctx(args)
    # keep every app log of this run (each restart replaces logs/aias-local.log) and keep the machine awake:
    # a system sleep freezes the app and the runner mid-Check (seen 2026-10-02: a 15-minute sleep left a Check
    # RUNNING and failed the runner's wall-clock wait)
    os.environ["E2E_LOG_ARCHIVE"] = str(REPO / "logs" / f"e2e-{ctx.run_id}")
    if sys.platform == "darwin":
        try:
            subprocess.Popen(["caffeinate", "-i", "-s", "-w", str(os.getpid())])
        except OSError:
            print("warning: caffeinate not available; keep the machine awake during the run", file=sys.stderr)
    ctx.mode_touched = fx.read_override() != {} or any(
        (fx.PARKED / name).exists() != (name in fx.PARKED_BY_DEFAULT) for name in fx.KNOWN_PACKAGES
        if (fx.PARKED / name).exists() or (fx.PACKAGES / name).exists())
    try:
        results = run(ctx, set(args.only or []))
    finally:
        if ctx.mode_touched:
            print("restoring the normal local mode (no override, default parked packages)", flush=True)
            if fx.apply_mode(None, fx.PARKED_BY_DEFAULT):
                ctx.restarts.append({"scenario": "(end of run)", "mode": "mode [normal]",
                                     "at": dt.datetime.now().isoformat(timespec="seconds")})
    data = write_outputs(ctx, results, args.out)
    print(json.dumps({"groups": data["groups"], "modelCalls": data["modelCalls"], "quotaHit": data["quotaHit"]}))
    print(f"results: {args.out}.json, {args.out}-run.md")
    return 0 if all(s.status not in ("FAILED", "ERROR") for s in results) else 1


if __name__ == "__main__":
    sys.exit(main())
