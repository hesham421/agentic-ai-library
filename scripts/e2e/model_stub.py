#!/usr/bin/env python3
"""A SCRIPTED stand-in for the comparison model's provider endpoint (local E2E only, stdlib only).

The RPT / INT groups of ``simulate.py`` point ``spring.ai.openai.base-url`` at http://127.0.0.1:7293/<path>.
The app's comparison adapter calls it exactly as it calls the real OpenAI-compatible provider; the stub
answers like that provider. It is a local test double of the EXTERNAL provider — the same role as
``local/approval-stub.py`` for the host Approval API — so that the Report Store's and Host Integration's
TCs, which need COMPLETED Checks with exact findings (or a provider that is slow / failing), cost no call
of the free-tier model quota and do not depend on a model reading synthetic values correctly. Everything
downstream of the provider (CHK's verification of every finding against the Check's own data, the Overall
Status rule, RPT's storage, INT) runs unchanged.

Script selection: the request's data part is searched for ``E2ESTUB_<ID>`` (the runner puts it in the text of
an uploaded / host document); the script ``<ID>`` is read from ``logs/e2e-model-stub-scripts.json`` (written
by the runner) on every call:

    {"<ID>": {"hold": 40, "status": 200, "findings": [{"condition": ..., "outcome": ..., "evidence": ...,
              "note": ...}], "error": "provider answered 503"}}

``hold`` delays the answer (seconds), ``status`` != 200 answers that HTTP status with ``error`` as the
provider's message. No marker → ``{"findings": []}`` at once.

Record: ``logs/e2e-model-stub.jsonl`` — time, path, model, script id, status, hold, tools present. No header
(the app sends its configured key) and no message content is recorded.
"""
import json
import os
import re
import sys
import threading
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
PORT = int(os.environ.get("MODEL_STUB_PORT", "7293"))
SCRIPTS = REPO / "logs" / "e2e-model-stub-scripts.json"
RECORD = REPO / "logs" / "e2e-model-stub.jsonl"
MARKER = re.compile(r"E2ESTUB_([A-Z0-9]+)")
LOCK = threading.Lock()


def scripts():
    try:
        return json.loads(SCRIPTS.read_text())
    except (OSError, ValueError):
        return {}


def data_text(body):
    out = []
    for m in body.get("messages", []) or []:
        if m.get("role") == "system":
            continue                      # the engine framing + service knowledge: never a script source
        content = m.get("content")
        if isinstance(content, str):
            out.append(content)
        elif isinstance(content, list):
            out += [p.get("text", "") for p in content if isinstance(p, dict)]
    return "\n".join(out)


class Stub(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *args):
        pass

    def _send(self, status, payload):
        raw = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def do_POST(self):
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length) if length else b""
        try:
            body = json.loads(raw.decode() or "{}")
        except ValueError:
            body = {}
        found = MARKER.search(data_text(body))
        sid = found.group(1) if found else None
        script = scripts().get(sid, {}) if sid else {}
        hold = float(script.get("hold", 0))
        status = int(script.get("status", 200))
        record = {"at": time.strftime("%Y-%m-%dT%H:%M:%S"), "path": self.path, "model": body.get("model"),
                  "script": sid, "status": status, "hold": hold, "toolsPresent": "tools" in body}
        with LOCK:
            RECORD.parent.mkdir(exist_ok=True)
            with open(RECORD, "a") as f:
                f.write(json.dumps(record) + "\n")
        if hold:
            time.sleep(hold)
        if not self.path.rstrip("/").endswith("/chat/completions"):
            self._send(404, {"error": {"message": "model stub: only /chat/completions", "code": 404}})
            return
        if status != 200:
            self._send(status, {"error": {"message": script.get("error", f"provider answered {status}"),
                                          "code": status, "status": "UNAVAILABLE"}})
            return
        answer = json.dumps({"findings": script.get("findings", [])})
        self._send(200, {
            "id": "chatcmpl-e2e-" + uuid.uuid4().hex[:12], "object": "chat.completion", "created": int(time.time()),
            "model": body.get("model") or "e2e-stub",
            "choices": [{"index": 0, "finish_reason": "stop", "logprobs": None,
                         "message": {"role": "assistant", "content": answer, "refusal": None}}],
            "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2}})

    def do_GET(self):
        self._send(200, {"object": "list", "data": []})


def main():
    server = ThreadingHTTPServer(("127.0.0.1", PORT), Stub)
    print(f"model stub on 127.0.0.1:{PORT}; scripts {SCRIPTS}; record {RECORD}", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    return 0


if __name__ == "__main__":
    sys.exit(main())
