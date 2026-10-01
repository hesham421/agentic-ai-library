"""
Reads the module's declared API contract and joins it to the implemented
endpoints, so every generated endpoint carries
the contract id (API-<MOD>-NNN) the rest of the governance chain — the SRS,
the frontend execution plan, the test manifest, the alignment report — is
written in terms of.

Why this exists
───────────────
Before this extractor the generated docs were correct but *anonymous*: they
named METHOD + path and nothing else. Every downstream artifact, and every
frontend developer, works from a contract id instead ("implement
API-SEC-004"), and the id appeared nowhere in the published api-docs. The
only place left to resolve an id into a path was a planning document — and a
planning document states the path that was *proposed*, not the one that was
implemented. That is how a frontend ends up calling
/api/v1/security/auth/forgot-password against a backend that serves
/api/v1/sec/auth/password-reset/complete: not a wrong path in the docs, but
a missing join between the id and the docs.

So the id is stamped onto the implemented endpoint here, and any id that is
planned but not implemented (or implemented but not planned) is reported as
contract drift rather than silently dropped — a stale contract id is exactly
the failure mode above, and it is only detectable by comparing the two sides.

Where the declared contract comes from
──────────────────────────────────────
1. The module's API DOCUMENT (factory schema 7): `api-spec-<mod>.yaml`, the
   OpenAPI 3.1 document the factory derives from the backend plan and
   publishes beside the delivered packages. Each operation's `x-api-id`,
   method and path is one declared entry. This is THE contract.
2. Only for a pre-v7 module with no API document: the prose API REGISTRY
   table of its backend execution plan (parse_api_registry). The caller says
   out loud that it fell back.

Like every other extractor: it finds real metadata or leaves the field empty.
The document / registry is read verbatim; no path is ever inferred from an id.
"""

import re
from pathlib import Path
from typing import Optional

from extractors import security_extractor
from models.api_doc_model import ApiDocument, ContractDrift, ContractEntry

# "**API REGISTRY**" heading, then a markdown table whose header row starts
# with an API column. Matched structurally (a row whose first cell is an
# API-<MOD>-NNN id) rather than by column index, so a plan that adds or
# reorders descriptive columns still parses as long as the row carries an id,
# an HTTP verb and a path.
_API_ID_RE = re.compile(r"^API-[A-Z0-9]+-\d+$")
_VERB_RE = re.compile(r"^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)$", re.IGNORECASE)
_PATH_RE = re.compile(r"^/\S*$")
# Both registry spellings this repo's plans use: the markdown-table modules
# (SEC, MDL, FIN) head it "**API REGISTRY**", the older box-drawing ones (CU,
# NOTIF, FILE) write a bare "API REGISTRY" line above a │-delimited table.
_REGISTRY_HEADING_RE = re.compile(r"^\s*\**API\s+REGISTRY\b", re.MULTILINE)
_DELIMITERS = ("|", "\u2502")


def normalize_path(path: str) -> str:
    """Path parameter *names* are the plan author's choice on one side and
    the Java method's parameter name on the other; only their positions are
    part of the contract. `/roles/{id}/modules/{moduleId}` and
    `/roles/{roleId}/modules/{id}` are the same endpoint, and must not be
    reported as drift."""
    collapsed = re.sub(r"\{[^}]*\}", "{}", path.strip())
    return "/" + collapsed.strip("/")


def _key(method: str, path: str) -> tuple[str, str]:
    return method.strip().upper(), normalize_path(path)


def _cells(line: str) -> list[str]:
    """Splits one table row into cells for either delimiter. A markdown row is
    fenced by a leading/trailing pipe; a box-drawing row is not fenced at
    all, so it is split on its own delimiter as-is."""
    stripped = line.strip()
    if "\u2502" in stripped:
        return [c.strip().strip("*").strip() for c in stripped.split("\u2502")]
    if not stripped.startswith("|"):
        return []
    return [c.strip().strip("*").strip() for c in stripped.strip("|").split("|")]


def parse_api_registry(text: str) -> list[ContractEntry]:
    """Parses the API REGISTRY table. Only rows inside (or after) the API
    REGISTRY heading are considered, and only until a row stops looking like
    a registry row — so the plan's other id tables (the DOC-phase contract
    summary, whose paths are context-path-relative and therefore NOT the
    served paths) are never mistaken for it."""
    start = _REGISTRY_HEADING_RE.search(text)
    if not start:
        return []

    entries: list[ContractEntry] = []
    seen_row = False
    for line in text[start.end():].splitlines():
        cells = _cells(line)
        if not cells or not _API_ID_RE.match(cells[0]):
            stripped = line.strip()
            if seen_row and stripped and not any(d in stripped for d in _DELIMITERS):
                break  # table ended
            continue
        verb = next((c for c in cells[1:] if _VERB_RE.match(c)), None)
        path = next((c for c in cells[1:] if _PATH_RE.match(c)), None)
        if not verb or not path:
            continue
        seen_row = True
        operation = cells[1] if not _VERB_RE.match(cells[1]) and not _PATH_RE.match(cells[1]) else None
        entries.append(ContractEntry(
            api_id=cells[0],
            method=verb.upper(),
            path=path,
            operation=operation or None,
        ))
    return entries


def load_api_registry(plan_file: Optional[Path]) -> list[ContractEntry]:
    if plan_file is None or not plan_file.exists():
        return []
    return parse_api_registry(plan_file.read_text(encoding="utf-8", errors="ignore"))


# ── the API document (OpenAPI 3.1, factory schema 7) ─────────────────────────
# Read with a minimal, indentation-tracking line reader rather than a YAML
# library: this tool takes no third-party dependency, and the three facts it
# needs (path key, method key, x-api-id scalar) are block-style keys in every
# document the factory emits. Anything else in the document is ignored.

_HTTP_METHODS = {"get", "post", "put", "patch", "delete", "head", "options", "trace"}
_KEY_RE = re.compile(r"^(?P<indent> *)(?P<key>'[^']*'|\"[^\"]*\"|[^\s:#][^:#]*?)\s*:(?:\s+(?P<value>.*?))?\s*$")


def _unquote(text: str) -> str:
    text = text.strip()
    if len(text) >= 2 and text[0] == text[-1] and text[0] in "'\"":
        return text[1:-1]
    return text


def parse_api_spec(text: str) -> list[ContractEntry]:
    """Every operation under the document's top-level `paths:` that carries an
    `x-api-id`, as (id, METHOD, path, summary). An operation without one is
    not a contract entry and is skipped -- the factory emits one per API
    block, so a missing id is the document's defect, not something to guess."""
    entries: list[ContractEntry] = []
    in_paths = False
    path_indent = method_indent = None
    path = method = summary = api_id = None

    def flush():
        if path and method and api_id:
            entries.append(ContractEntry(api_id=api_id, method=method.upper(), path=path,
                                         operation=summary or None))

    for raw in text.splitlines():
        if not raw.strip() or raw.lstrip().startswith("#"):
            continue
        m = _KEY_RE.match(raw)
        indent = len(raw) - len(raw.lstrip(" "))
        if indent == 0:
            if in_paths:
                flush()
                path = method = summary = api_id = None
            in_paths = bool(m) and m.group("key") == "paths" and not m.group("value")
            continue
        if not in_paths or not m:
            continue
        key, value = _unquote(m.group("key")), m.group("value")
        if path_indent is None:
            path_indent = indent
        if indent == path_indent:
            flush()
            path, method, summary, api_id = (key if key.startswith("/") else None), None, None, None
            method_indent = None
            continue
        if path is None or indent < path_indent:
            continue
        if method_indent is None and key.lower() in _HTTP_METHODS and not value:
            method_indent = indent
        if indent == method_indent:
            flush()
            method, summary, api_id = (key if key.lower() in _HTTP_METHODS else None), None, None
            continue
        if method and method_indent is not None and indent > method_indent and value:
            if key == "x-api-id":
                api_id = _unquote(value)
            elif key == "summary" and summary is None:
                summary = _unquote(value)
    if in_paths:
        flush()
    return entries


def load_api_spec(spec_file: Optional[Path]) -> list[ContractEntry]:
    if spec_file is None or not spec_file.exists():
        return []
    return parse_api_spec(spec_file.read_text(encoding="utf-8", errors="ignore"))


def _segments(path: str) -> list[str]:
    return [s for s in normalize_path(path).split("/") if s]


def _suffix_match(by_key: dict, method: str, path: str):
    """The document's path may be relative to a server base (`/v1/slots`)
    while the served path carries the application's prefix
    (`/api/v1/apt/slots` is NOT that, `/api/v1/slots` is): a declared entry
    matches when its segments are the served path's trailing segments, and
    only when exactly one declared entry does."""
    served = _segments(path)
    hits = [entry for (m, p), entry in by_key.items()
            if m == method.strip().upper() and _segments(p) and served[-len(_segments(p)):] == _segments(p)]
    return hits[0] if len(hits) == 1 else None


JAVADOC_RE = re.compile(r"/\*\*(.*?)\*/", re.DOTALL)
CLASS_JAVADOC_RE = re.compile(r"/\*\*(.*?)\*/\s*(?:@[^\n]*\n\s*)*(?:public\s+)?(?:final\s+)?class\b", re.DOTALL)


def _method_javadoc(text: str, method_name: str) -> str:
    lines = text.splitlines()
    decl = security_extractor._find_declaration_index(lines, method_name)
    if decl is None:
        return ""
    i = decl - 1
    depth = 0
    while i >= 0:
        stripped = lines[i].strip()
        depth += lines[i].count(")") - lines[i].count("(")
        if stripped.endswith("*/"):
            start = i
            while start >= 0 and "/**" not in lines[start]:
                start -= 1
            return "\n".join(lines[max(start, 0):i + 1])
        if depth > 0 or stripped.startswith(("@", "+", '"')) or not stripped or stripped.startswith(")"):
            i -= 1
            continue
        break
    return ""


def claimed_ids(source_root: Optional[Path], method: str, path: str,
                registered: set[str], id_prefix: str) -> tuple[list[str], Optional[str]]:
    """Ids of this module's shape that the served endpoint's controller
    Javadoc names and no registry row carries: first the method's own Javadoc,
    else the class Javadoc. Several candidates are all listed -- the reader
    decides, the tool does not. A signpost, never a registration."""
    if source_root is None:
        return [], None
    controller_file, method_name = security_extractor.find_controller_for_endpoint(source_root, method, path)
    if not controller_file or not method_name:
        return [], None
    text = controller_file.read_text(encoding="utf-8", errors="ignore")
    id_re = re.compile(rf"\b{re.escape(id_prefix)}\d+\b")
    ids = [i for i in dict.fromkeys(id_re.findall(_method_javadoc(text, method_name))) if i not in registered]
    if ids:
        return ids, f"{controller_file.stem}.{method_name} Javadoc"
    class_doc = CLASS_JAVADOC_RE.search(text)
    ids = [i for i in dict.fromkeys(id_re.findall(class_doc.group(1) if class_doc else "")) if i not in registered]
    return ids, (f"{controller_file.stem} class Javadoc" if ids else None)


def _id_prefix(entries: list[ContractEntry]) -> str:
    prefixes = {e.api_id.rsplit("-", 1)[0] + "-" for e in entries}
    return prefixes.pop() if len(prefixes) == 1 else "API-"


# What each contract source is called in drift details and the index.
SOURCE_NAMES = {"api-spec": "API document", "api-registry": "API REGISTRY"}


def attach_contract_ids(document: ApiDocument, entries: list[ContractEntry], source_label: str,
                        source_root: Optional[Path] = None, kind: str = "api-registry") -> None:
    """Stamps each implemented endpoint with its contract id and records both
    directions of drift on the document. Mutates in place, like the other
    enrich/attach extractors. `kind` is "api-spec" (the module's OpenAPI
    document -- served paths may carry a base prefix the document's paths do
    not, so a unique trailing-segment match also counts) or "api-registry"
    (the pre-v7 prose table, exact paths only)."""
    if not entries:
        return

    name = SOURCE_NAMES.get(kind, kind)
    document.contract_source = source_label
    document.contract_kind = kind
    by_key: dict[tuple[str, str], ContractEntry] = {}
    for entry in entries:
        by_key.setdefault(_key(entry.method, entry.path), entry)
    registered = {e.api_id for e in entries}
    prefix = _id_prefix(entries)

    matched: set[tuple[str, str]] = set()
    for ep in document.endpoints:
        key = _key(ep.method, ep.path)
        entry = by_key.get(key)
        if entry is None and kind == "api-spec":
            entry = _suffix_match(by_key, ep.method, ep.path)
            if entry is not None:
                key = _key(entry.method, entry.path)
                if key in matched:
                    entry = None
        if entry is None:
            ids, claim_source = claimed_ids(source_root, ep.method, ep.path, registered, prefix)
            document.contract_drift.append(ContractDrift(
                kind="undeclared",
                method=ep.method,
                path=ep.path,
                detail=f"implemented but absent from the {name} — it has no contract id "
                       "any other artifact can refer to",
                claimed_ids=ids,
                claim_source=claim_source,
            ))
            continue
        ep.api_id = entry.api_id
        matched.add(key)

    for entry in entries:
        key = _key(entry.method, entry.path)
        if key in matched:
            continue
        document.contract_drift.append(ContractDrift(
            kind="unimplemented",
            api_id=entry.api_id,
            method=entry.method,
            path=entry.path,
            detail=f"declared in the {name} but no such endpoint is served — a consumer "
                   "resolving this id from a planning document would call a path that does not exist",
        ))
