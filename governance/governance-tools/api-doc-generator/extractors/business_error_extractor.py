"""Binds a module's business error codes to the endpoints that can raise them by walking controller -> service -> domain call bodies in the module's own source; a code it cannot reach with certainty stays unbound."""

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

from extractors.error_mapping_extractor import THROW_RE, code_argument_throw_re, http_status_label
from models.api_doc_model import AccessDeniedOverride, ApiDocument, BusinessError, BusinessErrorWalk, Endpoint

MAX_DEPTH = 8

BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.DOTALL)
LINE_COMMENT_RE = re.compile(r"//[^\n]*")
STRING_RE = re.compile(r'"(?:[^"\\\n]|\\.)*"')
PACKAGE_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.MULTILINE)
CLASS_DECL_RE = re.compile(r"\b(?:class|enum|record|interface)\s+(\w+)")
FIELD_DECL_RE = re.compile(r"(?:private|protected|public)\s+(?:static\s+)?(?:final\s+)?([\w.]+)(?:<[^;=()]*>)?\s+(\w+)\s*(?:=[^;]*)?;")
METHOD_HEAD_RE = re.compile(
    r"(?<![\w.])(?:(?:public|protected|private|static|final|synchronized|default)\s+)*"
    r"(?:<[^>]+>\s+)?([\w.]+(?:<[^(){};]*>)?(?:\[\])*)\s+(\w+)\s*\(")
METHOD_TAIL_RE = re.compile(r"\s*(?:throws\s+[\w.,\s]+?)?\s*\{")
LOCAL_DECL_TEMPLATE = r"(?<![\w.])(?:final\s+)?([A-Z]\w*)(?:<[^;=()]*>)?\s+{name}\s*(?:=|;|:)"
CALL_RE = re.compile(r"(?<![\w.])(\w+)\s*\.\s*(\w+)\s*\(|(?<![\w.])(\w+)\s*\(")
CHAINED_STATIC_RE = re.compile(r"(?<![\w.])([A-Z]\w*)\s*\.\s*(\w+)\s*\([^()]*\)\s*\.\s*(\w+)\s*\(")
METHOD_REF_RE = re.compile(r"(?<![\w.])(\w+)\s*::\s*(\w+)")
DETAIL_RE = re.compile(r"ErrorDetail\s*\.\s*(?:of|ofField)\s*\(\s*(?:[^,()]*,\s*)?\w+\.([A-Z][A-Z0-9_]*)")
WITH_DETAILS_STATUS_RE = re.compile(r"LocalizedException\s*\.\s*withDetails\s*\(\s*Status\.(\w+)")
# The multi-error constructor -- new LocalizedException(Status.X, <List<ErrorDetail>>).
# Its second argument is a collected list, NOT an error-code constant, which is
# exactly what tells it apart from the single-code form THROW_RE already reads.
# This is the Status every ErrorDetail gathered for that throw is answered with,
# and the ONLY place it is stated: a check method that merely RETURNS an
# ErrorDetail names no Status of its own, so without reading the aggregate throw
# its rows can only be published as "not determined".
AGGREGATE_THROW_RE = re.compile(
    r"new\s+LocalizedException\s*\(\s*Status\.(\w+)\s*,\s*(?!\s*\w+\s*\.\s*[A-Z][A-Z0-9_]*\s*[,)])")
CATCH_ACCESS_DENIED_RE = re.compile(r"catch\s*\(\s*(?:[\w.]+\s*\|\s*)*AccessDeniedException\b[^)]*\)\s*\{")
PACKAGE_PREFIX_LITERAL_RE = re.compile(r'"([a-z][\w]*(?:\.[\w]+)+\.)"')
JAVA_KEYWORDS = {
    "if", "for", "while", "switch", "catch", "return", "new", "super", "this", "synchronized",
    "try", "else", "do", "throw", "case", "instanceof", "assert",
}
MODIFIERS = {"public", "protected", "private", "static", "final", "synchronized", "default", "abstract"}
CONSTRUCTOR = "<init>"
NEW_RE = re.compile(r"\bnew\s+([A-Z]\w*)\s*\(")

# aias adaptation: the throw form that names a code constant but no Status --
# new <ModuleException>(<Module>ErrorCodes.X, ...) / super(<Rejection>Codes.X, ...).
# Built from the configured code classes (configure_code_classes, called by the
# generator from conventions.json -> error_code_classes); the default is the
# original *ErrorCodes classes. Its status is resolved later from the code's
# registered HTTP status (read from the code's value when the project declares
# that convention).
CODE_ARG_THROW_RE = code_argument_throw_re(("*ErrorCodes.java",))


def configure_code_classes(class_globs) -> None:
    global CODE_ARG_THROW_RE
    CODE_ARG_THROW_RE = code_argument_throw_re(class_globs)


@dataclass
class MethodInfo:
    name: str
    return_type: str
    params: list[str]
    body: str
    body_start: int
    params_raw: str = ""


@dataclass
class ClassInfo:
    name: str
    package: str
    path: Path
    raw: str
    clean: str
    fields: dict[str, str] = field(default_factory=dict)
    methods: dict[str, list[MethodInfo]] = field(default_factory=dict)


def _mask(text: str) -> str:
    """Comments and string contents become spaces of equal length, so every
    index into the masked text is an index into the original."""
    def blank(m: re.Match) -> str:
        return re.sub(r"[^\n]", " ", m.group(0))
    masked = BLOCK_COMMENT_RE.sub(blank, text)
    masked = LINE_COMMENT_RE.sub(blank, masked)
    return STRING_RE.sub(lambda m: '"' + " " * (len(m.group(0)) - 2) + '"', masked)


def _brace_end(text: str, open_index: int) -> int:
    depth = 0
    for i in range(open_index, len(text)):
        ch = text[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return i + 1
    return len(text)


def _paren_end(text: str, open_index: int) -> int:
    depth = 0
    for i in range(open_index, len(text)):
        ch = text[i]
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return i + 1
    return len(text)


def _arg_count(text: str, open_index: int) -> int:
    end = _paren_end(text, open_index)
    inner = text[open_index + 1:end - 1]
    if not inner.strip():
        return 0
    depth = 0
    count = 1
    for ch in inner:
        if ch in "([{<":
            depth += 1
        elif ch in ")]}>":
            depth -= 1
        elif ch == "," and depth == 0:
            count += 1
    return count


def _param_types(params: str) -> list[str]:
    if not params.strip():
        return []
    types = []
    depth = 0
    current = ""
    for ch in params:
        if ch in "<([":
            depth += 1
        elif ch in ">)]":
            depth -= 1
        if ch == "," and depth == 0:
            types.append(current.strip())
            current = ""
        else:
            current += ch
    types.append(current.strip())
    cleaned = []
    for t in types:
        t = re.sub(r"@[\w.]+(?:\([^)]*\))?", "", t).strip()
        cleaned.append(t.split()[0] if t.split() else t)
    return cleaned


def _parse_class(path: Path) -> Optional[ClassInfo]:
    raw = path.read_text(encoding="utf-8", errors="ignore")
    clean = _mask(raw)
    decl = CLASS_DECL_RE.search(clean)
    if not decl:
        return None
    pkg = PACKAGE_RE.search(clean)
    info = ClassInfo(name=decl.group(1), package=pkg.group(1) if pkg else "", path=path, raw=raw, clean=clean)
    for m in FIELD_DECL_RE.finditer(clean):
        info.fields.setdefault(m.group(2), m.group(1).split(".")[-1])
    for m in METHOD_HEAD_RE.finditer(clean):
        return_type, name = m.group(1), m.group(2)
        if return_type in MODIFIERS and name == info.name:
            name, return_type = CONSTRUCTOR, info.name
        elif name in JAVA_KEYWORDS or return_type.split("<")[0] in JAVA_KEYWORDS | MODIFIERS | {"else"}:
            continue
        params_end = _paren_end(clean, m.end() - 1)
        tail = METHOD_TAIL_RE.match(clean, params_end)
        if not tail:
            continue
        params = clean[m.end():params_end - 1]
        open_index = tail.end() - 1
        body = clean[open_index:_brace_end(clean, open_index)]
        info.methods.setdefault(name, []).append(
            MethodInfo(name, return_type, _param_types(params), body, open_index, params))
    return info


def index_module_source(source_root: Path) -> dict[str, ClassInfo]:
    classes: dict[str, ClassInfo] = {}
    for path in sorted(source_root.rglob("*.java")):
        info = _parse_class(path)
        if info:
            classes.setdefault(info.name, info)
    return classes


def aggregate_detail_status(body: str) -> Optional[str]:
    """The Status a method answers a collected ErrorDetail list with, when its
    body states exactly one. Two different aggregate statuses in one body
    resolve to nothing rather than to whichever came first."""
    statuses = set(WITH_DETAILS_STATUS_RE.findall(body)) | set(AGGREGATE_THROW_RE.findall(body))
    return next(iter(statuses)) if len(statuses) == 1 else None


def _throws_in(body: str, site: str, inherited_detail_status: Optional[str] = None) -> list[BusinessError]:
    """`inherited_detail_status` is the aggregate Status of a caller already on
    this walk: a domain check that RETURNS an ErrorDetail is reached only
    through the method that throws the collected list, so that caller's Status
    is this detail's Status. Read from a real throw in a really-walked caller,
    never assumed."""
    found: list[BusinessError] = []
    for m in THROW_RE.finditer(body):
        status, code = m.groups()
        found.append(BusinessError(code=code, value=code, throw_site=site, kind="thrown", status=status))
    for m in CODE_ARG_THROW_RE.finditer(body):
        found.append(BusinessError(code=m.group(1), value=m.group(1), throw_site=site, kind="thrown", status=None))
    status = aggregate_detail_status(body) or inherited_detail_status
    for m in DETAIL_RE.finditer(body):
        found.append(BusinessError(code=m.group(1), value=m.group(1), throw_site=site, kind="detail", status=status))
    return found


def _throw_signature(methods: list[MethodInfo]) -> set[tuple[str, Optional[str], str]]:
    sig = set()
    for mi in methods:
        for be in _throws_in(mi.body, ""):
            sig.add((be.code, be.status, be.kind))
    return sig


def _pick_overload(methods: list[MethodInfo], argc: Optional[int]) -> tuple[Optional[MethodInfo], bool]:
    """(method, ambiguous). Overloads are told apart by arity; when arity
    cannot settle it and the candidates differ in what they throw, nothing is picked."""
    if argc is not None:
        exact = [m for m in methods if len(m.params) == argc
                 or (m.params and m.params[-1].endswith("...") and argc >= len(m.params) - 1)]
        if exact:
            methods = exact
    if len(methods) == 1:
        return methods[0], False
    if len({frozenset(_throw_signature([m])) for m in methods}) == 1:
        return methods[0], False
    return None, True


def _receiver_class(receiver: str, cls: ClassInfo, method: MethodInfo, classes: dict[str, ClassInfo]) -> Optional[str]:
    if receiver == "this":
        return cls.name
    if receiver in classes:
        return receiver
    field_type = cls.fields.get(receiver)
    if field_type in classes:
        return field_type
    local = re.search(LOCAL_DECL_TEMPLATE.format(name=re.escape(receiver)), method.params_raw + " ; " + method.body)
    if local and local.group(1) in classes:
        return local.group(1)
    return None


def _calls_in(cls: ClassInfo, method: MethodInfo, classes: dict[str, ClassInfo]) -> list[tuple[str, str, Optional[int]]]:
    """(class, method, argc) for every call in the body that resolves to a
    class in this module's own source. Anything else -- repositories, mappers,
    shared code, unknown receivers -- is not followed."""
    body = method.body
    calls: list[tuple[str, str, Optional[int]]] = []
    for m in CALL_RE.finditer(body):
        if m.group(3):
            name = m.group(3)
            if name in JAVA_KEYWORDS or name not in cls.methods:
                continue
            calls.append((cls.name, name, _arg_count(body, m.end() - 1)))
            continue
        receiver, name = m.group(1), m.group(2)
        target = _receiver_class(receiver, cls, method, classes)
        if target and name in classes[target].methods:
            calls.append((target, name, _arg_count(body, m.end() - 1)))
    for m in CHAINED_STATIC_RE.finditer(body):
        holder, factory, name = m.groups()
        if holder not in classes:
            continue
        factories = classes[holder].methods.get(factory, [])
        if factories and all(f.return_type.split("<")[0] == holder for f in factories) and name in classes[holder].methods:
            calls.append((holder, name, _arg_count(body, m.end() - 1)))
    for m in METHOD_REF_RE.finditer(body):
        receiver, name = m.groups()
        target = _receiver_class(receiver, cls, method, classes)
        if target and name in classes[target].methods:
            calls.append((target, name, None))
    for m in NEW_RE.finditer(body):
        target = m.group(1)
        if target in classes and CONSTRUCTOR in classes[target].methods:
            calls.append((target, CONSTRUCTOR, _arg_count(body, m.end() - 1)))
    return calls


def site_label(cls_name: str, method_name: str) -> str:
    return f"new {cls_name}()" if method_name == CONSTRUCTOR else f"{cls_name}.{method_name}"


def walk_endpoint(classes: dict[str, ClassInfo], controller: str, method_name: str,
                  max_depth: int = MAX_DEPTH) -> tuple[list[BusinessError], BusinessErrorWalk]:
    walk = BusinessErrorWalk()
    errors: list[BusinessError] = []
    seen: set[tuple[str, str, int]] = set()
    queue: list[tuple[str, str, Optional[int], int, Optional[str]]] = [(controller, method_name, None, 0, None)]
    while queue:
        cls_name, name, argc, depth, detail_status = queue.pop(0)
        cls = classes.get(cls_name)
        if not cls or name not in cls.methods:
            continue
        method, ambiguous = _pick_overload(cls.methods[name], argc)
        if ambiguous:
            walk.unresolved.append(f"{cls_name}.{name} (overloads differ)")
            continue
        key = (cls_name, name, method.body_start)
        if key in seen:
            continue
        seen.add(key)
        site = site_label(cls_name, name)
        walk.visited.append(site)
        detail_status = aggregate_detail_status(method.body) or detail_status
        errors.extend(_throws_in(method.body, site, detail_status))
        if depth >= max_depth:
            if _calls_in(cls, method, classes):
                walk.depth_limit_hit = True
            continue
        for target_cls, target_name, target_argc in _calls_in(cls, method, classes):
            queue.append((target_cls, target_name, target_argc, depth + 1, detail_status))
    return errors, walk


def find_access_denied_overrides(classes: dict[str, ClassInfo]) -> list[AccessDeniedOverride]:
    overrides: list[AccessDeniedOverride] = []
    for name in sorted(classes):
        cls = classes[name]
        for m in CATCH_ACCESS_DENIED_RE.finditer(cls.clean):
            block = cls.clean[m.end() - 1:_brace_end(cls.clean, m.end() - 1)]
            throw = THROW_RE.search(block)
            if not throw:
                continue
            prefixes = sorted(set(PACKAGE_PREFIX_LITERAL_RE.findall(cls.raw)))
            overrides.append(AccessDeniedOverride(
                advisor=name, status=throw.group(1), code=throw.group(2), value=throw.group(2),
                scope_prefix=prefixes[0] if len(prefixes) == 1 else None))
    return overrides


def count_throw_sites(classes: dict[str, ClassInfo]) -> dict[str, list[str]]:
    """Code constant -> every Class.method naming it at a throw site or as an
    ErrorDetail, across the whole module source. The expected side of the
    walk's coverage ratio."""
    sites: dict[str, list[str]] = {}
    for name in sorted(classes):
        cls = classes[name]
        for method_name in sorted(cls.methods):
            for mi in cls.methods[method_name]:
                for be in _throws_in(mi.body, site_label(name, method_name)):
                    if be.throw_site not in sites.setdefault(be.code, []):
                        sites[be.code].append(be.throw_site)
    return sites


def _status_rank(http_status: Optional[str]) -> int:
    if not http_status:
        return 999
    head = http_status.split()[0]
    return int(head) if head.isdigit() else 998


def attach_business_errors(document: ApiDocument, classes: dict[str, ClassInfo],
                           controller_of: dict[int, tuple[str, str]],
                           status_http: dict[str, str]) -> None:
    """controller_of maps id(endpoint) -> (ControllerClass, method). Resolves
    each bound code's wire value and HTTP status through the tables the document
    already carries, and records per-code binding counts on document.error_codes."""
    values = {c.name: c.value for c in document.error_codes}
    registered_status = {c.name: c.status for c in document.error_codes if c.status}
    registered_http = {c.name: c.http_status for c in document.error_codes if c.http_from_value and c.http_status}
    sites = count_throw_sites(classes)
    bound_counts: dict[str, int] = {}

    for ep in document.endpoints:
        located = controller_of.get(id(ep))
        if not located:
            continue
        errors, walk = walk_endpoint(classes, *located)
        ep.business_walk = walk
        unique: dict[tuple[str, Optional[str], str, str], BusinessError] = {}
        for be in errors:
            be.value = values.get(be.code, be.code)
            if not be.status:
                # Last resort: the Status this very code is REGISTERED with in
                # the module catalog (read by error_mapping_extractor from a
                # throw site that names both). A detail-kind row reached through
                # a path that states no Status of its own still answers with the
                # code's own status -- printing "not determined" for a code whose
                # status the same run already published two sections above is a
                # gap in this generator, not in the application.
                be.status = registered_status.get(be.code)
            if be.status:
                be.http_status = http_status_label(status_http.get(be.status))
            elif be.code in registered_http:
                # aias: no Status at the throw site; the code's value carries it.
                be.http_status = registered_http[be.code]
            unique.setdefault((be.code, be.status, be.throw_site, be.kind), be)
        ep.business_errors = sorted(unique.values(), key=lambda b: (_status_rank(b.http_status), b.code, b.throw_site, b.kind))
        for code in {b.code for b in ep.business_errors}:
            bound_counts[code] = bound_counts.get(code, 0) + 1

    via_codes = {o.code: o.advisor for o in document.access_denied_overrides}
    if document.auth_entry_point:
        via_codes[document.auth_entry_point.code] = document.auth_entry_point.handler
    for code in document.error_codes:
        code.throw_sites = sites.get(code.name, [])
        code.bound_endpoints = bound_counts.get(code.name, 0)
        if code.name in via_codes:
            via = sum(1 for ep in document.endpoints if any(pe.code.startswith(code.name + " ") or pe.code == code.name
                                                            for pe in ep.possible_errors))
            code.bound_endpoints = via
            code.bound_via = f"Other Possible Responses, via {via_codes[code.name]}"

    document.source_facts["throw_sites"] = sum(len(v) for v in sites.values())
    document.source_facts["endpoints_with_business_errors"] = sum(1 for ep in document.endpoints if ep.business_errors)
    document.source_facts["endpoints_walked_past_controller"] = sum(
        1 for ep in document.endpoints if ep.business_walk and len(ep.business_walk.visited) > 1)
