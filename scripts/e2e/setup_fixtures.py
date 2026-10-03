#!/usr/bin/env python3
"""Idempotent setup of the LOCAL, SYNTHETIC fixtures the end-to-end simulation needs.

Run from anywhere:  python3 scripts/e2e/setup_fixtures.py [--restart] [--no-restart]

What it does (each step only when something is missing or different):

1. ``local/storage-root/`` (the ``path`` storage root) with synthetic transcripts — a passing PDF,
   a failing PDF and a PNG (no text layer) — and ``local/outside.pdf``, a file OUTSIDE the root
   that the path-traversal scenario must never open.
2. Two more synthetic service packages in ``local/package-directory/``: ``demo-path`` (fetch
   ``path``, document source over the MCP connection ``local-oracle``) and ``demo-blob`` (fetch
   ``blob``, document source over the jdbc connection ``local-jdbc``, the BLOB built from a hex
   literal with ``TO_BLOB(HEXTORAW(...))`` — no host table). ``demo-manual`` / ``demo-approval``
   are kept as they are.
3. A marked block in the gitignored ``src/main/resources/application-local.properties``: the
   storage root, the second REG connection ``local-jdbc``, a 2 s approval timeout and a 12 MB upload
   request limit.
4. ``LOCAL_JDBC_CREDENTIAL`` (``username:password`` of the local throwaway LOAN_SYS schema) in the
   gitignored ``local/secrets.properties`` — the value is copied from the local datasource password
   and is never printed.
5. Restarts the app (stop PID -> ``mvn -q -DskipTests package`` -> start -> wait for
   ``GET /api/v1/services``) when anything above changed or ``--restart`` is given, and makes sure
   the approval stub is running (restarted when it lacks the ``delay`` mode).
6. Restores the NORMAL local mode: no ``local/e2e-override.properties``, ``demo-blob`` in the package
   directory, ``demo-conn`` / ``demo-noconn`` parked in ``local/e2e-parked/`` (they are moved in only by
   the ``connection`` group of ``simulate.py``).

Profile override (``--override KEY=VALUE``, repeatable; ``--clear-override``): writes / removes the
gitignored ``local/e2e-override.properties``, which the local profile imports when present (it takes
precedence over the profile), and restarts the app WITHOUT a rebuild. ``simulate.py`` uses the same
functions (``apply_mode``) for its restart groups and always restores the normal mode at the end.
Note: Spring does not merge indexed lists across property sources — an override that sets any
``aias.registry.connections[i]`` must list every connection (``connections_with``).

Local only: refuses to touch anything but this repository's ``local/`` folder, its gitignored local
profile, the local app on 127.0.0.1:7271 and the stub on 127.0.0.1:7290. Writes no database row.
"""
import argparse
import os
import signal
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import synth  # noqa: E402

REPO = Path(__file__).resolve().parents[2]
LOCAL = REPO / "local"
STORAGE_ROOT = LOCAL / "storage-root"
PACKAGES = LOCAL / "package-directory"
PROFILE = REPO / "src/main/resources/application-local.properties"
SECRETS = LOCAL / "secrets.properties"
LOGS = REPO / "logs"
APP_PID = LOGS / "aias-local.pid"
STUB_PID = LOGS / "approval-stub.pid"
APP_PORT = 7271
STUB_PORT = 7290
JAR = REPO / "target/agentic-ai-library-0.1.0-SNAPSHOT.jar"
OVERRIDE = LOCAL / "e2e-override.properties"
PARKED = LOCAL / "e2e-parked"
APP_LOG = LOGS / "aias-local.log"
# fixture package folders this script owns or knows; the normal mode parks exactly PARKED_BY_DEFAULT
KNOWN_PACKAGES = ("demo-manual", "demo-approval", "demo-path", "demo-blob", "demo-path-big", "demo-blob-big",
                  "demo-rows", "demo-conn", "demo-noconn")
PARKED_BY_DEFAULT = frozenset({"demo-conn", "demo-noconn"})

BLOCK_START = "# --- E2E SIMULATION FIXTURE (scripts/e2e/setup_fixtures.py) - synthetic, local only ---"
BLOCK_END = "# --- END E2E SIMULATION FIXTURE ---"
PROFILE_BLOCK = f"""{BLOCK_START}
# path storage root (DOC GuardedFileSystemAdapter), relative to the app's working directory (repo root)
aias.documents.storage-root=local/storage-root
# second REG connection: jdbc, for the blob fixture demo-blob. The credential reference names a property
# of the Spring Environment holding username:password - it lives in local/secrets.properties (gitignored).
aias.registry.connections[1].name=local-jdbc
aias.registry.connections[1].type=jdbc
aias.registry.connections[1].endpoint=jdbc:oracle:thin:@localhost:1521/FREEPDB1
aias.registry.connections[1].dialect=oracle
aias.registry.connections[1].credential-reference=LOCAL_JDBC_CREDENTIAL
aias.registry.connections[1].read-only=true
aias.registry.connections[1].limited-to-views=false
# short approval timeout so the stub's slow mode produces INT-504 quickly
aias.integration.approval.timeout=PT2S
# upload request limit just above the 10 MB maximum file size (REQ-INT-014 needs limit >= max file size),
# so the 413 scenario needs only a ~13 MB request
aias.integration.upload.request-limit=12MB
# re-declares the profile's import (a later key wins) adding the OPTIONAL e2e override file: absent in the
# normal mode; written only by setup_fixtures.py --override / simulate.py restart groups (local/, gitignored)
spring.config.import=optional:file:./local/secrets.properties,optional:file:./local/e2e-override.properties
{BLOCK_END}
"""

# Synthetic transcript values. Host-file fixtures carry no request number: the knowledge of demo-path and
# demo-blob does not compare it.
PASS_LINES = synth.transcript_lines(None, "3.62", "128", "SYNTHETIC STUDENT PATH-0001")
FAIL_LINES = synth.transcript_lines(None, "2.10", "90", "SYNTHETIC STUDENT PATH-0002")
BLOB_LINES = synth.transcript_lines(None, "3.45", "124", "SYNTHETIC STUDENT BLOB-0001")

KNOWLEDGE_HOSTFILE = """# {code} - synthetic local fixture service

This is SYNTHETIC test knowledge for local development only. It describes no real service.

Condition: the TRANSCRIPT must state a grade point average of at least 3.00 (on a 4.00 scale)
and at least 120 completed credit hours.
"""

DEMO_PATH_YAML = """service: demo-path
version: 1
input: requestId
queries:
  request_echo:
    connection: local-oracle
    sql: SELECT :requestId AS REQUEST_NUMBER FROM DUAL
  document_source:
    connection: local-oracle
    sql: >-
      SELECT CAST('TRANSCRIPT' AS VARCHAR2(100)) AS DOC_TYPE,
      CAST('transcripts/pass.pdf' AS VARCHAR2(400)) AS FILE_PATH FROM DUAL WHERE :requestId IS NOT NULL
      UNION ALL
      SELECT CAST('ID_CARD' AS VARCHAR2(100)), CAST('../outside.pdf' AS VARCHAR2(400)) FROM DUAL
      WHERE :requestId IS NOT NULL
documents:
  fetch: path
  source: document_source
  type_column: DOC_TYPE
  path_column: FILE_PATH
  required:
    - TRANSCRIPT
    - ID_CARD
approval:
  enabled: false
"""


def demo_blob_yaml(pdf_bytes):
    hex_literal = pdf_bytes.hex().upper()
    if len(hex_literal) > 4000:
        raise SystemExit("the synthetic blob PDF is too large for a 4000-character SQL literal")
    return f"""service: demo-blob
version: 1
input: requestId
queries:
  request_echo:
    connection: local-oracle
    sql: SELECT :requestId AS REQUEST_NUMBER FROM DUAL
  document_source:
    connection: local-jdbc
    sql: >-
      SELECT CAST('TRANSCRIPT' AS VARCHAR2(100)) AS DOC_TYPE,
      TO_BLOB(HEXTORAW('{hex_literal}')) AS CONTENT FROM DUAL WHERE :requestId IS NOT NULL
documents:
  fetch: blob
  source: document_source
  type_column: DOC_TYPE
  content_column: CONTENT
  required:
    - TRANSCRIPT
approval:
  enabled: false
"""


BIG_LINES = PASS_LINES + [f"SYNTHETIC PADDING LINE {i:02d} - this file exists only to exceed a small maximum file size"
                         for i in range(40)]

KNOWLEDGE_ROWS = """# demo-rows - synthetic local fixture service

This is SYNTHETIC test knowledge for local development only. It describes no real service.

Condition: the uploaded TRANSCRIPT must state a grade point average of at least 3.00 (on a 4.00 scale)
and at least 120 completed credit hours.
"""

DEMO_PATH_BIG_YAML = """service: demo-path-big
version: 1
input: requestId
queries:
  request_echo:
    connection: local-oracle
    sql: SELECT :requestId AS REQUEST_NUMBER FROM DUAL
  document_source:
    connection: local-oracle
    sql: >-
      SELECT CAST('TRANSCRIPT' AS VARCHAR2(100)) AS DOC_TYPE,
      CAST('transcripts/big.pdf' AS VARCHAR2(400)) AS FILE_PATH FROM DUAL WHERE :requestId IS NOT NULL
documents:
  fetch: path
  source: document_source
  type_column: DOC_TYPE
  path_column: FILE_PATH
  required:
    - TRANSCRIPT
approval:
  enabled: false
"""

# 2000 bytes ("%PDF" + '0' padding) built in SQL: larger than the 1 KB maximum of the limits mode; its
# content never matters because the size is measured before any byte is streamed (REQ-DOC-041/042)
DEMO_BLOB_BIG_YAML = """service: demo-blob-big
version: 1
input: requestId
queries:
  request_echo:
    connection: local-oracle
    sql: SELECT :requestId AS REQUEST_NUMBER FROM DUAL
  document_source:
    connection: local-jdbc
    sql: >-
      SELECT CAST('TRANSCRIPT' AS VARCHAR2(100)) AS DOC_TYPE,
      TO_BLOB(HEXTORAW(RPAD('25504446', 4000, '30'))) AS CONTENT FROM DUAL WHERE :requestId IS NOT NULL
documents:
  fetch: blob
  source: document_source
  type_column: DOC_TYPE
  content_column: CONTENT
  required:
    - TRANSCRIPT
approval:
  enabled: false
"""

DEMO_ROWS_YAML = """service: demo-rows
version: 1
input: requestId
queries:
  request_echo:
    connection: local-oracle
    sql: SELECT :requestId AS REQUEST_NUMBER FROM DUAL
  rows_probe:
    connection: local-oracle
    sql: SELECT LEVEL AS N FROM DUAL WHERE :requestId IS NOT NULL CONNECT BY LEVEL <= 2
documents:
  fetch: manual
  required:
    - TRANSCRIPT
approval:
  enabled: false
"""

# parked by default; moved into the package directory only by the connection group
DEMO_CONN_YAML = """service: demo-conn
version: 1
input: requestId
queries:
  request_echo:
    connection: local-oracle
    sql: SELECT :requestId AS REQUEST_NUMBER FROM DUAL
  extra_echo:
    connection: local-extra
    sql: SELECT :requestId AS REQUEST_NUMBER FROM DUAL
documents:
  fetch: manual
  required:
    - TRANSCRIPT
approval:
  enabled: false
"""

DEMO_NOCONN_YAML = """service: demo-noconn
version: 1
input: requestId
queries:
  ghost_echo:
    connection: ghost-db
    sql: SELECT :requestId AS REQUEST_NUMBER FROM DUAL
documents:
  fetch: manual
  required:
    - TRANSCRIPT
approval:
  enabled: false
"""


def package_home(name):
    """Where a fixture package folder lives now: the package directory, else the parking folder, else
    its normal-mode home."""
    if (PACKAGES / name).exists():
        return PACKAGES / name
    if (PARKED / name).exists():
        return PARKED / name
    return (PARKED if name in PARKED_BY_DEFAULT else PACKAGES) / name


def write_if_changed(path, data):
    """Writes bytes/str when different; returns True when the file changed."""
    raw = data.encode() if isinstance(data, str) else data
    if path.exists() and path.read_bytes() == raw:
        return False
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(raw)
    print(f"  wrote {path.relative_to(REPO)}")
    return True


def setup_files():
    changed = False
    changed |= write_if_changed(STORAGE_ROOT / "README.txt",
                                "SYNTHETIC local storage root for the e2e simulation (scripts/e2e). No real data.\n")
    changed |= write_if_changed(STORAGE_ROOT / "transcripts/pass.pdf", synth.make_pdf(PASS_LINES))
    changed |= write_if_changed(STORAGE_ROOT / "transcripts/fail.pdf", synth.make_pdf(FAIL_LINES))
    changed |= write_if_changed(STORAGE_ROOT / "transcripts/transcript.png", synth.make_png(PASS_LINES))
    # OUTSIDE the storage root: must never be opened (path traversal scenario)
    changed |= write_if_changed(LOCAL / "outside.pdf",
                                synth.make_pdf(["SYNTHETIC - OUTSIDE THE STORAGE ROOT - MUST NEVER BE READ"]))
    # packages: the registry reloads only at start, so a package change needs a restart
    pkg_changed = False
    PARKED.mkdir(parents=True, exist_ok=True)
    pkg_changed |= write_if_changed(package_home("demo-path") / "knowledge.md", KNOWLEDGE_HOSTFILE.format(code="demo-path"))
    pkg_changed |= write_if_changed(package_home("demo-path") / "service.yaml", DEMO_PATH_YAML)
    pkg_changed |= write_if_changed(package_home("demo-blob") / "knowledge.md", KNOWLEDGE_HOSTFILE.format(code="demo-blob"))
    pkg_changed |= write_if_changed(package_home("demo-blob") / "service.yaml", demo_blob_yaml(synth.make_pdf(BLOB_LINES)))
    changed |= write_if_changed(STORAGE_ROOT / "transcripts/big.pdf", synth.make_pdf(BIG_LINES))
    for name, yaml in (("demo-path-big", DEMO_PATH_BIG_YAML), ("demo-blob-big", DEMO_BLOB_BIG_YAML),
                       ("demo-rows", DEMO_ROWS_YAML), ("demo-conn", DEMO_CONN_YAML),
                       ("demo-noconn", DEMO_NOCONN_YAML)):
        home = package_home(name)
        knowledge = (KNOWLEDGE_HOSTFILE.format(code=name) if name.endswith("-big")
                     else KNOWLEDGE_ROWS.replace("demo-rows", name))
        changed_here = write_if_changed(home / "knowledge.md", knowledge)
        changed_here |= write_if_changed(home / "service.yaml", yaml)
        if home.parent == PACKAGES:
            pkg_changed |= changed_here
    for kept in ("demo-manual", "demo-approval"):
        if not (package_home(kept) / "service.yaml").exists():
            print(f"  WARNING: {kept} is missing from {PACKAGES.relative_to(REPO)} (see local/README.md)")
    return changed, pkg_changed


def read_property(path, key):
    if not path.exists():
        return None
    for line in path.read_text().splitlines():
        if line.startswith(key + "="):
            return line.split("=", 1)[1]
    return None


def setup_profile():
    """Inserts or refreshes the marked block; returns True when the profile changed (needs a rebuild)."""
    text = PROFILE.read_text()
    if BLOCK_START in text:
        start = text.index(BLOCK_START)
        end = text.index(BLOCK_END, start) + len(BLOCK_END) + 1
        new_text = text[:start] + PROFILE_BLOCK + text[end:]
    else:
        new_text = text.rstrip("\n") + "\n\n" + PROFILE_BLOCK
    if new_text == text:
        return False
    PROFILE.write_text(new_text)
    print(f"  updated {PROFILE.relative_to(REPO)} (e2e block)")
    return True


def setup_secret():
    """LOCAL_JDBC_CREDENTIAL=LOAN_SYS:<local password>; the value is never printed."""
    user = read_property(PROFILE, "spring.datasource.username")
    password = read_property(PROFILE, "spring.datasource.password")
    if not user or not password:
        raise SystemExit("the local profile has no datasource username/password to build LOCAL_JDBC_CREDENTIAL")
    wanted = f"LOCAL_JDBC_CREDENTIAL={user}:{password}"
    lines = SECRETS.read_text().splitlines() if SECRETS.exists() else ["# Local secrets - gitignored (local/)."]
    kept = [line for line in lines if not line.startswith(("LOCAL_JDBC_CREDENTIAL=", "# e2e simulation (scripts/e2e)"))]
    if wanted in lines and len([l for l in lines if l.startswith("LOCAL_JDBC_CREDENTIAL=")]) == 1:
        return False
    kept.append("# e2e simulation (scripts/e2e): credential of the REG jdbc connection local-jdbc (local throwaway DB)")
    kept.append(wanted)
    SECRETS.write_text("\n".join(kept) + "\n")
    print(f"  updated {SECRETS.relative_to(REPO)} (LOCAL_JDBC_CREDENTIAL, value not shown)")
    return True


def pid_alive(pid_file):
    try:
        pid = int(pid_file.read_text().strip())
        os.kill(pid, 0)
        return pid
    except (OSError, ValueError):
        return None


def http_ok(url, timeout=3):
    try:
        with urllib.request.urlopen(url, timeout=timeout) as response:
            return 200 <= response.status < 300
    except Exception:
        return False


def stub_supports_delay():
    try:
        with urllib.request.urlopen(f"http://127.0.0.1:{STUB_PORT}/__mode?set=delay", timeout=3) as response:
            import json
            return json.loads(response.read().decode()).get("mode") == "delay"
    except Exception:
        return False


def ensure_stub():
    pid = pid_alive(STUB_PID)
    if pid and http_ok(f"http://127.0.0.1:{STUB_PORT}/__mode"):
        if stub_supports_delay():
            http_ok(f"http://127.0.0.1:{STUB_PORT}/__mode?set=ok&seconds=1")
            print("  approval stub running (mode reset to ok)")
            return
        print(f"  approval stub pid {pid} lacks the delay mode: restarting it")
        os.kill(pid, signal.SIGTERM)
        time.sleep(1)
    LOGS.mkdir(exist_ok=True)
    log = open(LOGS / "approval-stub.log", "a")
    env = dict(os.environ, STUB_PORT=str(STUB_PORT), STUB_DELAY="10")
    proc = subprocess.Popen([sys.executable, str(LOCAL / "approval-stub.py")], cwd=REPO, stdout=log,
                            stderr=subprocess.STDOUT, env=env, start_new_session=True)
    STUB_PID.write_text(str(proc.pid))
    for _ in range(20):
        if http_ok(f"http://127.0.0.1:{STUB_PORT}/__mode"):
            print(f"  approval stub started (pid {proc.pid})")
            return
        time.sleep(0.5)
    raise SystemExit("the approval stub did not start")


def restart_app(build=True, profiles="local"):
    pid = pid_alive(APP_PID)
    if pid:
        print(f"  stopping app pid {pid}")
        os.kill(pid, signal.SIGTERM)
        for _ in range(60):
            try:
                os.kill(pid, 0)
                time.sleep(0.5)
            except OSError:
                break
        else:
            os.kill(pid, signal.SIGKILL)
    if build or not JAR.exists():
        print("  building (mvn -q -DskipTests package)")
        subprocess.run(["mvn", "-q", "-DskipTests", "package"], cwd=REPO, check=True)
    LOGS.mkdir(exist_ok=True)
    current = APP_LOG
    archive = os.environ.get("E2E_LOG_ARCHIVE")
    if archive and current.exists():   # simulate.py keeps every app log of its run (restart groups)
        Path(archive).mkdir(parents=True, exist_ok=True)
        (Path(archive) / time.strftime("aias-local-until-%H%M%S.log")).write_bytes(current.read_bytes())
    if current.exists():
        current.replace(LOGS / "aias-local-prev.log")
    log = open(current, "w")
    proc = subprocess.Popen(["java", "-jar", str(JAR.relative_to(REPO)), f"--spring.profiles.active={profiles}",
                             f"--server.port={APP_PORT}"], cwd=REPO, stdout=log, stderr=subprocess.STDOUT,
                            start_new_session=True)
    APP_PID.write_text(str(proc.pid))
    print(f"  started app pid {proc.pid}; waiting for GET /api/v1/services")
    for _ in range(180):
        if proc.poll() is not None:
            raise SystemExit(f"the app exited during start-up (code {proc.returncode}); see logs/aias-local.log")
        if http_ok(f"http://127.0.0.1:{APP_PORT}/api/v1/services"):
            print("  app healthy")
            return
        time.sleep(1)
    raise SystemExit("the app did not become healthy within 180 s; see logs/aias-local.log")


# ------------------------------------------------------------------------------------- modes
def read_override():
    """The current override as {key: value}; {} when the file is absent (the normal mode)."""
    if not OVERRIDE.exists():
        return {}
    props = {}
    for line in OVERRIDE.read_text().splitlines():
        if line and not line.startswith("#") and "=" in line:
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


def write_override(props):
    """Writes (or, for an empty/None ``props``, removes) local/e2e-override.properties; True when changed."""
    props = dict(props or {})
    if not props:
        if OVERRIDE.exists():
            OVERRIDE.unlink()
            print(f"  removed {OVERRIDE.relative_to(REPO)} (normal mode)")
            return True
        return False
    text = ("# E2E OVERRIDE (scripts/e2e) - local only, gitignored; imported by the local profile when present.\n"
            "# Remove it (python3 scripts/e2e/setup_fixtures.py --clear-override) to return to the normal mode.\n"
            + "".join(f"{k}={v}\n" for k, v in props.items()))
    if OVERRIDE.exists() and OVERRIDE.read_text() == text:
        return False
    OVERRIDE.write_text(text)
    print(f"  wrote {OVERRIDE.relative_to(REPO)}: {', '.join(f'{k}={v}' for k, v in props.items())}")
    return True


def connections_with(extra):
    """The profile's aias.registry.connections[i].* entries plus ``extra`` (a list of {field: value}) —
    an override must list EVERY connection, since Spring does not merge indexed lists across sources."""
    props = {}
    count = 0
    for line in PROFILE.read_text().splitlines():
        if line.startswith("aias.registry.connections[") and "=" in line:
            key, value = line.split("=", 1)
            props[key] = value
            count = max(count, int(key.split("[", 1)[1].split("]", 1)[0]) + 1)
    for offset, entry in enumerate(extra):
        for field, value in entry.items():
            props[f"aias.registry.connections[{count + offset}].{field}"] = value
    return props


def set_parked(parked):
    """Moves the known fixture package folders so that exactly ``parked`` are outside the package
    directory (in local/e2e-parked/); True when anything moved."""
    PARKED.mkdir(parents=True, exist_ok=True)
    moved = False
    for name in KNOWN_PACKAGES:
        inside, outside = PACKAGES / name, PARKED / name
        if name in parked and inside.exists():
            if outside.exists():
                raise SystemExit(f"{name} exists both in {PACKAGES} and {PARKED}; remove one")
            inside.rename(outside)
            print(f"  parked {name} (out of the package directory)")
            moved = True
        elif name not in parked and outside.exists():
            if inside.exists():
                raise SystemExit(f"{name} exists both in {PACKAGES} and {PARKED}; remove one")
            outside.rename(inside)
            print(f"  restored {name} into the package directory")
            moved = True
    return moved


def apply_mode(override=None, parked=PARKED_BY_DEFAULT, force_restart=False):
    """Puts the local app in a mode — an override (None/{} = normal) and a set of parked packages —
    restarting it (no rebuild) when anything changed, when ``force_restart`` or when it is down.
    Returns True when the app was restarted."""
    changed = write_override(override)
    changed |= set_parked(set(parked))
    if changed or force_restart or not http_ok(f"http://127.0.0.1:{APP_PORT}/api/v1/services"):
        restart_app(build=False)
        return True
    return False


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--restart", action="store_true", help="restart the app even when nothing changed")
    parser.add_argument("--no-restart", action="store_true", help="never restart the app")
    parser.add_argument("--override", action="append", metavar="KEY=VALUE",
                        help="write local/e2e-override.properties with these keys and restart without a rebuild")
    parser.add_argument("--clear-override", action="store_true",
                        help="remove local/e2e-override.properties (normal mode) and restart")
    args = parser.parse_args()
    if not PROFILE.exists():
        raise SystemExit(f"{PROFILE} is missing: copy application-local.properties.example first")
    print("e2e fixtures (synthetic, local only)")
    files_changed, packages_changed = setup_files()
    profile_changed = setup_profile()
    secret_changed = setup_secret()
    ensure_stub()
    if args.override:
        props = dict(item.split("=", 1) for item in args.override)
        if profile_changed:
            restart_app(build=True)
        apply_mode(props, PARKED_BY_DEFAULT, force_restart=packages_changed or secret_changed or args.restart)
        print("done (override mode; return with --clear-override)")
        return
    mode_changed = write_override(None) | set_parked(PARKED_BY_DEFAULT)
    need_restart = (packages_changed or profile_changed or secret_changed or mode_changed or args.clear_override
                    or not http_ok(f"http://127.0.0.1:{APP_PORT}/api/v1/services"))
    if args.restart or (need_restart and not args.no_restart):
        restart_app(build=profile_changed or args.restart)
    else:
        print("  app left running (no package/profile/secret change)" if not need_restart
              else "  restart needed but --no-restart given")
    print("done" + (" (host files changed)" if files_changed else ""))


if __name__ == "__main__":
    main()
