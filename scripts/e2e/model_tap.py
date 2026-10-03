#!/usr/bin/env python3
"""A RECORDING pass-through for the model provider endpoint (local E2E only, stdlib only).

The DOC groups point ``spring.ai.openai.base-url`` at http://127.0.0.1:7292/<same path> so that every model
call of the app (the comparison model and the document-reading model) is forwarded UNCHANGED to the real
provider and its request shape is recorded. This is how the runner observes, from outside the app, what a
TC asks to "capture": which model a call goes to, its messages and parts (the reading instruction, one
document per call, no tool), and the document content handed to the comparison model as data.

    python3 scripts/e2e/model_tap.py            # port 7292, upstream https://generativelanguage.googleapis.com

Record: ``logs/e2e-model-tap.jsonl`` (gitignored), one JSON object per call: time, path, model, status,
elapsed ms, ``tools`` present or not, and the messages with every inline media part reduced to its media
type and length. Request HEADERS are never recorded (the Authorization header carries the API key) and the
response body is not recorded. The documents are synthetic (scripts/e2e/synth.py).
"""
import json
import os
import sys
import threading
import time
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
PORT = int(os.environ.get("MODEL_TAP_PORT", "7292"))
UPSTREAM = os.environ.get("MODEL_TAP_UPSTREAM", "https://generativelanguage.googleapis.com")
RECORD = REPO / "logs" / "e2e-model-tap.jsonl"
FORWARDED = ("authorization", "content-type", "accept", "x-goog-api-key", "user-agent")
LOCK = threading.Lock()


def reduce_part(part):
    """A message part with any inline data replaced by its media type and size (never the bytes)."""
    if not isinstance(part, dict):
        return part
    out = {}
    for key, value in part.items():
        if key == "image_url" and isinstance(value, dict):
            url = str(value.get("url", ""))
            media = url[5:url.index(";")] if url.startswith("data:") and ";" in url else "url"
            out[key] = {"mediaType": media, "length": len(url)}
        elif key in ("file", "input_audio") and isinstance(value, dict):
            data = str(value.get("file_data") or value.get("data") or "")
            media = data[5:data.index(";")] if data.startswith("data:") and ";" in data else value.get("format")
            out[key] = {"mediaType": media, "length": len(data), "filename": value.get("filename")}
        else:
            out[key] = value
    return out


def reduce_body(raw):
    try:
        body = json.loads(raw.decode())
    except (ValueError, UnicodeDecodeError):
        return {"unparsed": len(raw)}
    messages = []
    for m in body.get("messages", []) or []:
        content = m.get("content")
        if isinstance(content, list):
            content = [reduce_part(p) for p in content]
        messages.append({"role": m.get("role"), "content": content})
    return {"model": body.get("model"), "messages": messages, "toolsPresent": "tools" in body,
            "tools": len(body.get("tools") or []), "keys": sorted(body)}


class Tap(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *args):
        pass

    def _forward(self, method):
        length = int(self.headers.get("Content-Length") or 0)
        raw = self.rfile.read(length) if length else b""
        headers = {k: v for k, v in self.headers.items() if k.lower() in FORWARDED}
        started = time.time()
        req = urllib.request.Request(UPSTREAM + self.path, data=raw if method != "GET" else None, method=method,
                                     headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=300) as r:
                status, resp_headers, body = r.status, r.headers, r.read()
        except urllib.error.HTTPError as e:
            status, resp_headers, body = e.code, e.headers, e.read() or b""
        except Exception as e:  # upstream unreachable: answer 502, record it
            status, resp_headers, body = 502, {}, json.dumps({"error": {"message": f"model tap: {e}"}}).encode()
        record = {"at": time.strftime("%Y-%m-%dT%H:%M:%S"), "method": method, "path": self.path,
                  "status": status, "elapsedMs": int((time.time() - started) * 1000)}
        if raw:
            record.update(reduce_body(raw))
        with LOCK:
            RECORD.parent.mkdir(exist_ok=True)
            with open(RECORD, "a") as f:
                f.write(json.dumps(record) + "\n")
        self.send_response(status)
        for key, value in (resp_headers.items() if hasattr(resp_headers, "items") else []):
            if key.lower() in ("content-type", "content-encoding"):
                self.send_header(key, value)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        self._forward("POST")

    def do_GET(self):
        self._forward("GET")


def main():
    server = ThreadingHTTPServer(("127.0.0.1", PORT), Tap)
    print(f"model tap on 127.0.0.1:{PORT} -> {UPSTREAM}; record {RECORD}", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    return 0


if __name__ == "__main__":
    sys.exit(main())
