#!/usr/bin/env python3
"""A RECORDING wrapper around a stdio MCP server (local E2E only, stdlib only).

The DOC groups start the local Oracle MCP server through this wrapper
(``spring.ai.mcp.client.stdio.connections.local-oracle.command=python3`` and
``...args=scripts/e2e/mcp_tap.py,governance/mcp-servers/oracle/index.js``): every JSON-RPC line passes through
unchanged in both directions, and each ``tools/call`` request (tool name, the SQL text, the binds, maxRows) with
its answer (row count, or the error text) is appended to ``logs/e2e-mcp-tap.jsonl`` (gitignored). That is how
the runner "captures the call on the MCP query channel" (TC-DOC-022/039/041/050). The environment the app gives
the server (database credentials) is passed on and never recorded; row VALUES are not recorded.
"""
import json
import os
import subprocess
import sys
import threading
import time
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
RECORD = REPO / "logs" / "e2e-mcp-tap.jsonl"
LOCK = threading.Lock()
PENDING = {}


def record(entry):
    entry["at"] = time.strftime("%Y-%m-%dT%H:%M:%S")
    with LOCK:
        RECORD.parent.mkdir(exist_ok=True)
        with open(RECORD, "a") as f:
            f.write(json.dumps(entry) + "\n")


def watch_request(line):
    try:
        msg = json.loads(line)
    except ValueError:
        return
    if isinstance(msg, dict) and msg.get("method") == "tools/call":
        params = msg.get("params") or {}
        PENDING[msg.get("id")] = time.time()
        record({"dir": "call", "id": msg.get("id"), "tool": params.get("name"),
                "arguments": params.get("arguments")})


def watch_response(line):
    try:
        msg = json.loads(line)
    except ValueError:
        return
    if not isinstance(msg, dict) or msg.get("id") not in PENDING or "method" in msg:
        return
    started = PENDING.pop(msg.get("id"))
    result = msg.get("result") or {}
    texts = [c.get("text", "") for c in result.get("content", []) if isinstance(c, dict)]
    entry = {"dir": "answer", "id": msg.get("id"), "ms": int((time.time() - started) * 1000),
             "isError": bool(result.get("isError")) or "error" in msg}
    if entry["isError"]:
        entry["error"] = (json.dumps(msg.get("error")) if "error" in msg else " ".join(texts))[:500]
    else:
        rows = None
        for text in texts:
            try:
                data = json.loads(text)
            except ValueError:
                continue
            if isinstance(data, dict):
                for key in ("rows", "data", "result"):
                    if isinstance(data.get(key), list):
                        rows = len(data[key])
                        break
                if rows is None and isinstance(data.get("rowCount"), int):
                    rows = data["rowCount"]
            elif isinstance(data, list):
                rows = len(data)
        entry["rows"] = rows
    record(entry)


def main():
    child = subprocess.Popen(["node"] + sys.argv[1:], stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                             env=os.environ.copy(), bufsize=0)

    def upstream():
        for line in iter(sys.stdin.buffer.readline, b""):
            watch_request(line)
            child.stdin.write(line)
            child.stdin.flush()
        child.stdin.close()

    threading.Thread(target=upstream, daemon=True).start()
    for line in iter(child.stdout.readline, b""):
        sys.stdout.buffer.write(line)
        sys.stdout.buffer.flush()
        watch_response(line)
    return child.wait()


if __name__ == "__main__":
    sys.exit(main())
