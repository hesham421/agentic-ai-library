#!/usr/bin/env python3
"""
gov-module.py — a consumer's one reader and writer of the governance project repo.

Every path, file name and vocabulary comes from what the factory PUBLISHES in the
project repo — `platform/profile-summary.json → consumer` (schema_version 7) and
`platform/modules-registry.json` — so nothing here restates a factory fact. The
same file serves the backend and the frontend; the track is this repo's.

    scripts/gov-module.py pin                      the project's factory pin agrees with its contract
    scripts/gov-module.py scope                    the sparse-checkout patterns this track may see
    scripts/gov-module.py pathspec                 the git pathspecs this track may stage (its partition, not the factory's packages)
    scripts/gov-module.py modules                  every module: wave, delivered version, current version
    scripts/gov-module.py plan MOD [--json]        everything to build MOD: plans, every artifact, packages, tests, API document, features
    scripts/gov-module.py requires MOD             each integration package's `requires`, met or not
    scripts/gov-module.py state-init MOD           create or upgrade the execution state (every channel and row list)
    scripts/gov-module.py record MOD package UNIT --passed N --failed N
    scripts/gov-module.py record MOD api_verify --version N --result PASS|FAIL      (backend)
    scripts/gov-module.py record MOD feature FEAT-MOD-001
    scripts/gov-module.py record MOD <channel> --key K --resolution "OPEN — …" [--detail …]   (api_doc_gaps, blocked, deferred_xm)
    scripts/gov-module.py validate [MOD]           every execution-state file holds the contract (CI)
    scripts/gov-module.py delivery MOD             per-package acceptance and, for the frontend, the backend's api_verify

Options: --project DIR (default governance/shared) · --track backend|frontend (default: this repo's
name, else $GOV_TRACK). Exit 0 = OK, 1 = a refusal, 2 = usage, 3 = the project predates schema 7.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
from datetime import datetime, timezone
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent


class Refused(Exception):
    pass


class PreV7(Refused):
    """The project predates the contract this tool reads — said, exit 3, never guessed around."""


SUPPORTED_SCHEMA = 7          # the factory contract this tool reads (profile-summary → consumer.schema_version)


# ── the contract ────────────────────────────────────────────────────────────

class Project:
    def __init__(self, root: Path, track: str):
        self.root = root
        summary = root / "platform" / "profile-summary.json"
        if not summary.exists():
            raise Refused(f"{summary} not found — is the governance submodule checked out (scripts/governance pull)?")
        self.summary = json.loads(summary.read_text(encoding="utf-8"))
        self._c = self.summary.get("consumer")
        if track not in self.summary["tracks"]:
            raise Refused(f"track `{track}` is not one of {sorted(self.summary['tracks'])}")
        self.track = track
        self.t = self.summary["tracks"][track]

    @property
    def c(self) -> dict:
        if not self._c:
            raise PreV7("platform/profile-summary.json carries no `consumer` contract — the project was published by a "
                        "factory older than schema 7; the factory owner runs `gov.py upgrade-project` there")
        return self._c

    # files ---------------------------------------------------------------
    def pub(self, name: str, mod: str | None = None) -> Path:
        home = self.c["publications"][name]
        if mod:
            home = home.replace("{MOD}", mod.upper()).replace("{mod}", mod.lower())
        return self.root / home

    def json_pub(self, name: str, default=None):
        p = self.pub(name)
        return json.loads(p.read_text(encoding="utf-8")) if p.exists() else default

    def registry(self) -> dict:
        return (self.json_pub("modules-registry", {}) or {}).get("modules") or {}

    def module(self, mod: str) -> dict:
        m = self.registry().get(mod.upper())
        if not m:
            raise Refused(f"module {mod.upper()} is not in {self.c['publications']['modules-registry']}")
        return m

    def partition(self, mod: str) -> Path:
        return self.root / self.t["partition"].replace("{MOD}", mod.upper()).replace("{mod}", mod.lower())

    def state_file(self, mod: str) -> Path:
        return self.partition(mod) / self.c["feedback"]["file"]

    def state_files(self, mod: str) -> list[Path]:
        """The partition's own file, then one per version folder (feedback.versioned)."""
        base = self.partition(mod)
        out = [self.state_file(mod)]
        if self.c["feedback"].get("versioned") and base.is_dir():
            vf = self.t["delivery_version_folder"]
            rx = re.compile("^" + re.escape(vf).replace(re.escape("{version}"), r"(\d+)") + "$")
            out += [d / self.c["feedback"]["file"] for d in sorted(base.iterdir(), key=lambda d: d.name)
                    if d.is_dir() and rx.match(d.name)]
        return [p for p in out if p.exists()]

    def analysis_root(self, mod: str, version: int) -> Path:
        base = self.root / self.summary["paths"]["modules"] / mod.upper()
        return base if version <= 1 else base / self.t["delivery_version_folder"].replace("{version}", str(version))

    def artifact(self, mod: str, name: str, upto: int) -> Path | None:
        """The artifact as of version `upto`: the latest version folder ≤ upto holding it
        (a delta version holds only what changed)."""
        a = self.c["artifacts"][name]
        fname = a["file"].replace("{mod}", mod.lower()).replace("{MOD}", mod.upper()).replace("{profile}", self.summary["profile"])
        for v in range(upto, 0, -1):
            p = self.analysis_root(mod, v) / a["folder"] / fname
            if p.exists():
                return p
        return None

    def delivered(self, mod: str) -> dict:
        d = (self.module(mod).get("delivery") or {}).get(self.track)
        if not d:
            raise Refused(f"{mod.upper()} has no {self.track} delivery yet ({self.t['delivery_current']}) — "
                          f"the factory splits packages only after the module's review gate")
        return d

    def plan_artifact(self, kind: str) -> str:
        return next(n for n, a in self.c["artifacts"].items()
                    if a.get("track") == self.track and a.get("plan") == kind)


# ── commands ────────────────────────────────────────────────────────────────

def cmd_pin(p: Project, _a) -> int:
    """The project is pinned, by the schema this tool reads. A different schema is a
    refusal (advance only to a project this tooling understands); none at all is a
    pre-v7 project (exit 3)."""
    want = p.c["schema_version"]
    pin = p.json_pub("factory-pin")
    if not pin:
        raise PreV7(f"{p.c['publications']['factory-pin']} missing — the project was never pinned by its factory; "
                    f"the factory owner runs `gov.py upgrade-project`")
    if pin.get("schema_version") != want or want != SUPPORTED_SCHEMA:
        raise Refused(f"the project is pinned to factory schema {pin.get('schema_version')}, publishes contract {want}; "
                      f"this tool reads schema {SUPPORTED_SCHEMA} — update scripts/gov-module.py from the factory")
    print(f"pinned: factory {str(pin.get('commit'))[:12]} · schema {pin['schema_version']} · {pin.get('written_by')} {pin.get('written_at')}")
    return 0


def cmd_scope(p: Project, _a) -> int:
    print("\n".join(p.c["sparse"][p.track]))
    return 0


def cmd_pathspec(p: Project, _a) -> int:
    """What this track may stage in the project repo: its own partition, never the
    factory's delivery inside it (git pathspecs, one per line)."""
    part = p.t["partition"].replace("{MOD}", "*").replace("{mod}", "*")
    deliv = p.t["delivery"].replace("{MOD}", "*").replace("{mod}", "*")
    print(f":(glob){part}/**")
    print(f":(exclude,glob){deliv}/**")
    return 0


def cmd_modules(p: Project, _a) -> int:
    order = p.json_pub("build-order", {}) or {}
    wave = {m: i + 1 for i, w in enumerate(order.get("waves") or []) for m in w}
    for code, m in sorted(p.registry().items(), key=lambda kv: (wave.get(kv[0], 99), kv[0])):
        d = (m.get("delivery") or {}).get(p.track) or {}
        print(f"{code:6} wave {wave.get(code, '-')} · current v{m.get('current_version')} · "
              f"{p.track} delivered {('v' + str(d['version'])) if d else 'not yet'}")
    return 0


def _units(pkg_dir: Path, manifest_tpl: str) -> list[dict]:
    out = []
    for f in sorted(pkg_dir.rglob("*.json")) if pkg_dir.is_dir() else []:
        rx = "^" + re.escape(manifest_tpl).replace(re.escape("{unit}"), "(.+)") + "$"
        if re.match(rx, f.name):
            data = json.loads(f.read_text(encoding="utf-8"))
            out.append({**data, "manifest": str(f), "file": str(f.with_name(data["unit"] + ".md"))})
    return out


def _integration(p: Project, mod: str, d: dict) -> list[dict]:
    ip = p.c["integration_package"]
    root = Path(p.root / d["exec"]).parent / ip["path"].split("{")[0].rstrip("/")
    out = []
    for f in sorted(root.glob(f"*/{ip['manifest_file']}")) if root.is_dir() else []:
        data = json.loads(f.read_text(encoding="utf-8"))
        out.append({**data, "manifest": str(f), "requires_met": _requires_met(p, data)})
    return out


def _requires_met(p: Project, pkg: dict) -> bool:
    """`requires` is `TARGET:STATE` — met when the edge's state in the published
    dependency graph is that state."""
    req = str(pkg.get("requires") or "")
    if ":" not in req:
        return True
    _target, state = req.split(":", 1)
    graph = p.json_pub("dependency-graph", {}) or {}
    edge = next((e for e in graph.get("edges") or [] if e.get("id") == pkg.get("xm")), None)
    return bool(edge) and str(edge.get("state", "")).upper() == state.strip().upper()


def build_plan(p: Project, mod: str) -> dict:
    mod = mod.upper()
    m = p.module(mod)
    d = p.delivered(mod)
    v = int(d["version"])
    pk = p.c["package"]
    exec_name, test_name = p.plan_artifact("exec"), p.plan_artifact("test")
    state = _load(p.state_file(mod))
    done = set(state.get(p.c["feature"]["done_key"]) or [])
    prompts_dir = p.root / p.summary["paths"]["modules"] / mod / p.c["feature"]["prompt_dir"]
    features = [{"id": f.stem, "file": str(f), "done": f.stem in done} for f in sorted(prompts_dir.glob("*.md"))] if prompts_dir.is_dir() else []
    plan = {
        "module": mod, "track": p.track, "delivered_version": v, "current_version": m.get("current_version"),
        "exec_plan": str(p.artifact(mod, exec_name, v) or ""), "test_plan": str(p.artifact(mod, test_name, v) or ""),
        "exec_packages": str(p.root / d["exec"]), "test_packages": str(p.root / d["test"]),
        "exec_units": _units(p.root / d["exec"], pk["manifest_file"]),
        "test_units": _units(p.root / d["test"], pk["manifest_file"]),
        "integration": _integration(p, mod, d) if any(ph.get("integration") for ph in p.t["plans"]["exec"]["phases"]) else [],
        # every analysis artifact this track may read, as of the delivered version: the
        # design references (ui-ux spec, flow diagram), the db-script, the SRS, the contract …
        "artifacts": {n: str(path) for n, a in p.c["artifacts"].items()
                      if a.get("track") in (None, p.track) and (path := p.artifact(mod, n, v))},
        "api_spec": str(p.pub("api-specs", mod)),
        "contract": str(p.pub("contracts", mod)),
        "state_file": str(p.state_file(mod)),
        "features": features,
        "acceptance": pk["acceptance"],
        "phases": [{"key": ph["key"], "folder": ph["folder"], "integration": ph.get("integration"), "no_tests": ph.get("no_tests")}
                   for ph in p.t["plans"]["exec"]["phases"]],
    }
    docs = (p.c["partitions"].get("api-docs") or {}).get("path")
    api_docs = p.root / docs.replace("{MOD}", mod).replace("{mod}", mod.lower()) if docs else None
    plan["api_docs"] = str(api_docs) if api_docs and api_docs.exists() else ""
    return plan


def cmd_plan(p: Project, a) -> int:
    plan = build_plan(p, a.module)
    if a.json:
        print(json.dumps(plan, indent=2, ensure_ascii=False))
        return 0
    print(f"{plan['module']} · {p.track} · delivered v{plan['delivered_version']} (current analysis v{plan['current_version']})")
    for k in ("exec_plan", "test_plan", "api_spec", "contract", "state_file", "api_docs"):
        print(f"  {k:11} {plan[k] or '—'}")
    print(f"  phases      {' → '.join(ph['key'] + ('*' if ph['integration'] else '') for ph in plan['phases'])}   (* integration)")
    for kind in ("exec_units", "test_units"):
        print(f"  {kind}:")
        for u in plan[kind]:
            print(f"    {u['unit']:28} {u['kind']:5} tests {len(u['tests']):3}  {u['acceptance']}")
    for i in plan["integration"]:
        print(f"  integration {i['xm']} → {i['target']} · requires {i['requires']} · {'MET' if i['requires_met'] else 'NOT MET — defer'} · tests {len(i['tests'])}")
    for f in plan["features"]:
        print(f"  feature     {f['id']} {'done' if f['done'] else 'TO EXECUTE'}  {f['file']}")
    return 0


def cmd_requires(p: Project, a) -> int:
    plan = build_plan(p, a.module)
    rc = 0
    for i in plan["integration"]:
        print(f"{i['xm']}: requires {i['requires']} → {'met' if i['requires_met'] else 'not met'}")
        rc = rc or (0 if i["requires_met"] else 1)
    return rc


# ── the execution state ─────────────────────────────────────────────────────

def _load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}


def _save(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def _row_lists(p: Project) -> list[str]:
    fb = p.c["feedback"]
    lists = [k for k, v in fb["channels"].items() if v.get("writer", p.track) != "factory"]
    lists.append(p.c["feature"]["done_key"])
    for k, spec in fb["delivery"].items():
        if spec.get("written_by", p.track) == p.track:
            lists.append(spec["key"])
    return lists


def cmd_state_init(p: Project, a) -> int:
    mod = a.module.upper()
    path = p.state_file(mod)
    data = _load(path)
    data.setdefault("module", mod)
    added = [k for k in _row_lists(p) if k not in data]
    for k in added:
        data[k] = []
    _save(path, data)
    print(f"{path}: {'added ' + ', '.join(added) if added else 'already complete'}")
    return 0


def _first_word(text: str) -> str:
    m = re.match(r"\W*([A-Za-z]+)", text or "")
    return m.group(1).upper() if m else ""


def _vocabulary(p: Project) -> set[str]:
    st = p.c["feedback"]["status"]
    return {w.upper() for k in ("open", "closed", "human") for w in st[k]}


def cmd_record(p: Project, a) -> int:
    mod = a.module.upper()
    path = p.state_file(mod)
    data = _load(path)
    fb, now = p.c["feedback"], datetime.now(timezone.utc).isoformat(timespec="seconds")
    if a.kind == "package":
        spec = fb["delivery"]["package_results"]
        name, passed, failed = spec["fields"]
        rows = [r for r in data.get(spec["key"]) or [] if r.get(name) != a.name]
        rows.append({name: a.name, passed: int(a.passed), failed: int(a.failed), "recorded_at": now})
        data[spec["key"]] = rows
    elif a.kind in fb["delivery"] and "written_by" in fb["delivery"][a.kind]:     # the api-verify run's row
        spec = fb["delivery"][a.kind]
        if spec.get("written_by") and spec["written_by"] != p.track:
            raise Refused(f"api_verify is written by the {spec['written_by']} track")
        ver, res = spec["fields"]
        rows = [r for r in data.get(spec["key"]) or [] if str(r.get(ver)) != str(a.version)]
        rows.append({ver: int(a.version), res: a.result.upper(), "recorded_at": now})
        data[spec["key"]] = rows
    elif a.kind == "feature":
        key = p.c["feature"]["done_key"]
        data[key] = sorted(set(data.get(key) or []) | {a.name})
    else:
        channel = a.kind
        if channel not in fb["channels"] or fb["channels"][channel].get("writer") == "factory":
            raise Refused(f"`{channel}` is not a channel this track writes — one of "
                          f"{sorted(k for k, v in fb['channels'].items() if v.get('writer') != 'factory')}")
        cspec = fb["channels"][channel]
        if not a.key or not a.resolution:
            raise Refused(f"record {channel} needs --key and --resolution")
        if _first_word(a.resolution) not in _vocabulary(p):
            raise Refused(f"resolution must start with one of {sorted(_vocabulary(p))} — got `{a.resolution}`")
        rows = [r for r in data.get(channel) or [] if str(r.get(cspec["key"])) != a.key]
        rows.append({cspec["key"]: a.key, fb["status_field"]: a.resolution, "detail": a.detail or "", "recorded_at": now})
        data[channel] = rows
    _save(path, data)
    print(f"{path}: recorded {a.kind} {a.name or a.key or a.version}")
    return 0


def validate_state(p: Project, path: Path) -> list[str]:
    errs = []
    try:
        data = _load(path)
    except ValueError as e:
        return [f"{path}: not JSON ({e})"]
    fb, vocab = p.c["feedback"], _vocabulary(p)
    for ch, spec in fb["channels"].items():
        if spec.get("writer") == "factory":
            continue
        rows = data.get(ch)
        if rows is None:
            continue
        if not isinstance(rows, list):
            errs.append(f"{path}: `{ch}` is not a list")
            continue
        for i, r in enumerate(rows):
            if not isinstance(r, dict):
                errs.append(f"{path}: {ch}[{i}] is not an object")
                continue
            if not (r.get(spec["key"]) or r.get("id")):
                errs.append(f"{path}: {ch}[{i}] has no `{spec['key']}`")
            w = _first_word(str(r.get(fb["status_field"], "")))
            if w not in vocab:
                errs.append(f"{path}: {ch}[{i}] `{fb['status_field']}` starts with `{w or '∅'}` — not one of {sorted(vocab)}")
    for k, spec in fb["delivery"].items():
        for i, r in enumerate(data.get(spec["key"]) or []):
            missing = [f for f in spec["fields"] if f not in r]
            if missing:
                errs.append(f"{path}: {spec['key']}[{i}] lacks {missing}")
    if not isinstance(data.get(p.c["feature"]["done_key"], []), list):
        errs.append(f"{path}: `{p.c['feature']['done_key']}` is not a list")
    return errs


def cmd_validate(p: Project, a) -> int:
    mods = [a.module.upper()] if a.module else sorted(p.registry())
    errs, seen = [], 0
    for m in mods:
        for f in p.state_files(m):
            seen += 1
            errs += validate_state(p, f)
    for e in errs:
        print(e, file=sys.stderr)
    print(f"validate: {seen} execution-state file(s), {len(errs)} problem(s)")
    return 1 if errs else 0


def cmd_delivery(p: Project, a) -> int:
    plan = build_plan(p, a.module)
    fb = p.c["feedback"]["delivery"]
    pr = fb["package_results"]
    name, passed, failed = pr["fields"]
    state = _load(p.state_file(a.module.upper()))
    results = {r.get(name): r for r in state.get(pr["key"]) or []}
    open_ = []
    for u in plan["exec_units"] + plan["test_units"] + plan["integration"]:
        unit = u.get("unit") or u.get("xm")
        r = results.get(unit)
        ok = bool(r) and int(r.get(failed, 1)) == 0
        print(f"  {unit:30} {'accepted' if ok else ('FAILING' if r else 'not run')}")
        if not ok:
            open_.append(unit)
    av = fb.get("api_verify") or {}
    if av and av.get("closes") == p.track:
        writer = Project(p.root, av["written_by"])
        rows = _load(writer.state_file(a.module.upper())).get(av["key"]) or []
        ver, res = av["fields"]
        ok = any(str(r.get(res, "")).upper() == av["passing"] and int(r.get(ver, 0)) == plan["delivered_version"] for r in rows)
        print(f"  api-verify ({av['written_by']}) v{plan['delivered_version']}: {'PASS' if ok else 'missing or failing — delivery stays OPEN'}")
        if not ok:
            open_.append("api_verify")
    print(f"{plan['module']} {p.track} v{plan['delivered_version']}: {'CLOSED' if not open_ else 'OPEN (' + str(len(open_)) + ')'}")
    return 0 if not open_ else 1


# ── main ────────────────────────────────────────────────────────────────────

def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(prog="gov-module.py", description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--project", default=os.environ.get("GOV_PROJECT", str(REPO / "governance" / "shared")))
    ap.add_argument("--track", default=os.environ.get("GOV_TRACK", REPO.name))
    sub = ap.add_subparsers(dest="cmd", required=True)
    for c in ("pin", "scope", "pathspec", "modules"):
        sub.add_parser(c)
    for c in ("plan", "requires", "state-init", "delivery"):
        s = sub.add_parser(c)
        s.add_argument("module")
        if c == "plan":
            s.add_argument("--json", action="store_true")
    s = sub.add_parser("validate"); s.add_argument("module", nargs="?")
    s = sub.add_parser("record")
    s.add_argument("module")
    s.add_argument("kind", help="package | api_verify | feature | a feedback channel (contract: consumer.feedback.channels)")
    s.add_argument("name", nargs="?", default=None)
    s.add_argument("--passed", type=int, default=0); s.add_argument("--failed", type=int, default=0)
    s.add_argument("--version", type=int); s.add_argument("--result")
    s.add_argument("--key"); s.add_argument("--resolution"); s.add_argument("--detail")
    a = ap.parse_args(argv)
    try:
        p = Project(Path(a.project).resolve(), a.track)
        return {"pin": cmd_pin, "scope": cmd_scope, "pathspec": cmd_pathspec, "modules": cmd_modules, "plan": cmd_plan, "requires": cmd_requires,
                "state-init": cmd_state_init, "record": cmd_record, "validate": cmd_validate,
                "delivery": cmd_delivery}[a.cmd](p, a)
    except PreV7 as e:
        print(f"PRE-V7: {e}", file=sys.stderr)
        return 3
    except Refused as e:
        print(f"REFUSED: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
