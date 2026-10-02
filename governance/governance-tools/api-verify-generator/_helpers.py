
# ═════════════════════════════════════════════════════════════════════════════
# helpers — ids threaded from the producing call's own return value
# ═════════════════════════════════════════════════════════════════════════════
_OWNER_CONTRACTS: dict[str, Contract] = {}


def owner_contract(r: Runner, api_id: str) -> Contract:
    """The API document that owns an operation id (another module's, read through gov-module.py)."""
    owner = api_id.split("-")[1]
    if owner == MODULE:
        return r.c
    if owner not in _OWNER_CONTRACTS:
        root = find_repo_root()
        out = subprocess.run([sys.executable, str(root / "scripts" / "gov-module.py"), "--track", "backend",
                              "plan", owner, "--json"], cwd=str(root), capture_output=True, text=True)
        if out.returncode != 0:
            raise RuntimeError(f"gov-module.py plan {owner} refused: {out.stderr or out.stdout}")
        _OWNER_CONTRACTS[owner] = Contract(json.loads(out.stdout)["api_spec"])
    return _OWNER_CONTRACTS[owner]


def path_of(r: Runner, api_id: str, **params) -> str:
    path = owner_contract(r, api_id).op(api_id)["path"]
    for key, value in params.items():
        path = path.replace("{" + key + "}", urllib.parse.quote(str(value), safe=""))
    return path


def available_services(r: Runner):
    """API-REG-001 — the registry's documented list (externally-owned service codes)."""
    call = r.http.get(path_of(r, "API-REG-001"))
    if call.error or call.status != 200 or not isinstance(call.body, list):
        return None, call
    return [s for s in call.body if isinstance(s, dict) and s.get("available") is True], call


def precheck_services(r: Runner, services, call, want: str, blocks: str):
    if services is None:
        return r.precondition(want, "registry (REG)", "API-REG-001 GET /api/v1/services", False,
                              f"registry read failed: HTTP {call.status} {call.error or call.code}", blocks)
    return None


def absent_check(r: Runner, check_id, source: str, blocks: str) -> bool:
    """A test-plan 'unknown Check' value is verified absent through API-RPT-001 before it is used."""
    call = r.http.get(path_of(r, "API-RPT-001", checkId=check_id))
    ok = call.status == 404 and call.code == "RPT-404-CHECK-NOT-FOUND"
    r.precondition(check_id, source, "API-RPT-001 GET /api/v1/checks/{checkId} → 404", ok,
                   "absent" if ok else f"HTTP {call.status} code={call.code} — exists or the read failed", blocks)
    return ok


def start_check(r: Runner, service: dict, employee_id: str, label: str):
    """API-INT-001 — the documented create; the Check it creates is tracked as a surviving record."""
    request_number = f"{r.ns}-{label}"
    body = {"serviceCode": service["serviceCode"], "requestNumber": request_number, "employeeId": employee_id}
    call = r.http.post(path_of(r, "API-INT-001"), json_body=body)
    if call.status == 202 and isinstance(call.body, dict) and call.body.get("checkId") is not None:
        r.track("Check", call.body["checkId"], service["serviceCode"], request_number,
                f"API-INT-001 start ({label}, fetch mode {service.get('fetchMode')})")
    return call, request_number


def upload(r: Runner, check_id, document_type, content: bytes | None = None, file_name: str | None = None,
           service_code="", request_number=""):
    """API-INT-002 — one small synthetic file (G13); a created upload is tracked."""
    content = content if content is not None else f"SYNTHETIC api-verify document {r.ns}\n".encode()
    file_name = file_name or f"{r.ns}-synthetic.txt"
    call = r.http.post(path_of(r, "API-INT-002", checkId=check_id),
                       files={"file": (file_name, content, "text/plain")}, data={"documentType": document_type})
    if call.status == 201 and isinstance(call.body, dict) and call.body.get("uploadedDocumentId") is not None:
        r.track("Uploaded Document", call.body["uploadedDocumentId"], service_code, request_number,
                f"API-INT-002 upload of {document_type} to Check {check_id}")
    return call


def read_check(r: Runner, check_id):
    return r.http.get(path_of(r, "API-RPT-001", checkId=check_id))


def wait_until_ended(r: Runner, check_id, timeout):
    return poll_until(lambda: read_check(r, check_id), {"COMPLETED", "FAILED"}, timeout)


OVERFLOW_ID = "9" * 20   # larger than int64 — exploratory only (observation)
