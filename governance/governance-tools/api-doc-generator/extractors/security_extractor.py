"""
Best-effort permission discovery from Java source.

Global auth requirement (is this endpoint behind the security filter chain at
all, and which scheme) is already fully discoverable straight from the
OpenAPI JSON — see openapi_extractor._operation_auth — because Spring Security
applies globally and OpenApiConfig declares one Bearer scheme applied to every
operation. That part needs no source access and lives in openapi_extractor.

What is NOT visible in the OpenAPI JSON is the specific business permission
(e.g. "BRANCH_CREATE") required to call an endpoint, because this codebase
puts `@PreAuthorize` in two different places depending on the module:
  - most modules: on the *service* method
  - some modules: directly on the *controller* method

There is no springdoc customizer projecting this into the OpenAPI doc, so the
only way to discover it is to read the Java source: check the controller
method first, and if nothing is found there, resolve the service class the
controller method delegates to (by convention: `xxxService.methodName(...)`
inside the method body, matched back to a `private final X xxxService;`
field) and check the same-named method on that service class.

Both annotation spellings in this codebase are handled: the single-line
`@PreAuthorize("...")` and the multi-line string concatenation
`@PreAuthorize("hasAuthority(T(...)"\n    + ".PERM_X)")`, in any number of parts.

This is a heuristic over source text (regex-based), not a real Java parser.
The common case is one delegate call per controller method under the same
method name (e.g. `MasterLookupController.create` -> `MasterLookupService.
create`), tried first. Some controllers in this codebase legitimately use a
differently-named delegate instead — e.g. `MasterLookupController.
createDetail` calls `LookupDetailService.create`, not `.createDetail` — so a
same-name-only match would silently drop the permission for every one of
those endpoints. As a fallback, when exactly one call is made against one of
the controller's own `private final XxxService` fields (matched by type name
ending in "Service", the convention every service field in this codebase
follows), that call is treated as the delegate regardless of its method name.
If more than one such call exists (genuine ambiguity) or none does, it
returns nothing — never guesses.
"""

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

# Outcomes of one endpoint's permission lookup. FOUND is the only one that yields
# a rule; DECLARED_NONE means every method on the path was read and none carries
# an annotation (absent by design); the rest mean the lookup could not finish
# (could not extract) and must never be reported as "no permission".
FOUND = "FOUND"
DECLARED_NONE = "DECLARED_NONE"
CONTROLLER_NOT_MATCHED = "CONTROLLER_NOT_MATCHED"
DELEGATE_UNRESOLVED = "DELEGATE_UNRESOLVED"
SERVICE_SOURCE_MISSING = "SERVICE_SOURCE_MISSING"
SERVICE_METHOD_MISSING = "SERVICE_METHOD_MISSING"
NOT_EXTRACTED = {CONTROLLER_NOT_MATCHED, DELEGATE_UNRESOLVED, SERVICE_SOURCE_MISSING, SERVICE_METHOD_MISSING}


@dataclass
class AuthorizationRule:
    annotation: str            # "PreAuthorize" | "Secured"
    expression: str            # the joined string value, verbatim apart from the join
    constants: list[str] = field(default_factory=list)


@dataclass
class PermissionLookup:
    outcome: str
    constants: list[str] = field(default_factory=list)
    source_label: Optional[str] = None     # "controller" | "service:<ClassName>"
    expression: Optional[str] = None
    checked: list[str] = field(default_factory=list)   # "Class.method" read along the way, in order

    @property
    def found(self) -> bool:
        return self.outcome == FOUND


AUTH_ANNOTATION_RE = re.compile(r"^@(PreAuthorize|Secured)\b")
_COMMENT_RE = re.compile(r"/\*.*?\*/|//[^\n]*", re.DOTALL)
_STRING_LITERAL_RE = re.compile(r'"((?:[^"\\]|\\.)*)"')
# The SpEL form is hasAuthority(T(<fully.qualified.Holder>).CONSTANT). The
# holder class name is a per-project choice (this platform uses
# PermissionConstants; an earlier one used SecurityPermissions), so it is
# matched structurally -- any T(...) type reference followed by a SCREAMING_CASE
# constant -- rather than by a hardcoded class name that silently yields zero
# permissions the moment the class is renamed.
PERMISSION_CONST_RE = re.compile(r"T\(\s*[\w.]+\s*\)\s*\.\s*([A-Z][A-Z0-9_]*)")
# The literal form, hasAuthority('PERM_X') / hasRole("ADMIN").
PERMISSION_LITERAL_RE = re.compile(r"has(?:Authority|Role)\(\s*['\"]([A-Za-z0-9_]+)['\"]\s*\)")
FIELD_DECL_TEMPLATE = r"private\s+final\s+(\w+)\s+{name}\s*;"
SERVICE_FIELD_DECL_RE = re.compile(r"private\s+final\s+(\w+Service)\s+(\w+)\s*;")

CLASS_REQUEST_MAPPING_RE = re.compile(r'@RequestMapping\(\s*"([^"]*)"\s*\)')
MAPPING_ANNOTATION_RE = re.compile(
    r'@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)(?:\(\s*"([^"]*)"\s*\))?'
)
MAPPING_VERB = {
    "GetMapping": "GET", "PostMapping": "POST", "PutMapping": "PUT",
    "PatchMapping": "PATCH", "DeleteMapping": "DELETE",
}
METHOD_NAME_AFTER_ANNOTATION_RE = re.compile(r"\b(?:public|private|protected)\b[^;{]*?\b(\w+)\s*\(")


def strip_comments(source: str) -> str:
    """Comments become spaces (newlines kept), so line indices survive and no
    regex below can match a keyword that only appears in prose. String
    literals stay: the annotation values live in them."""
    def blank(m: re.Match) -> str:
        return re.sub(r"[^\n]", " ", m.group(0))
    return _COMMENT_RE.sub(blank, source)


def _combine_path(base: str, sub: str) -> str:
    base = (base or "").rstrip("/")
    sub = sub or ""
    if sub and not sub.startswith("/"):
        sub = "/" + sub
    return (base + sub) or "/"


def find_controller_for_endpoint(source_root: Path, http_method: str, path: str) -> tuple[Optional[Path], Optional[str]]:
    """Matches an Endpoint back to its controller source file + Java method
    name by re-deriving each controller method's full route (class-level
    @RequestMapping + method-level @XxxMapping) and comparing verb+path —
    more reliable than guessing by method name, since method names like
    "create"/"search" repeat across every controller in this codebase."""
    for controller_file in sorted(source_root.rglob("*Controller.java")):
        text = strip_comments(controller_file.read_text(encoding="utf-8"))
        class_match = CLASS_REQUEST_MAPPING_RE.search(text)
        base_path = class_match.group(1) if class_match else ""

        for m in MAPPING_ANNOTATION_RE.finditer(text):
            verb = MAPPING_VERB[m.group(1)]
            if verb != http_method.upper():
                continue
            full_path = _combine_path(base_path, m.group(2) or "")
            if full_path != path:
                continue
            name_match = METHOD_NAME_AFTER_ANNOTATION_RE.search(text[m.end():])
            if name_match:
                return controller_file, name_match.group(1)

    return None, None


def _find_declaration_index(lines: list[str], method_name: str) -> Optional[int]:
    pattern = re.compile(rf"\b(public|private|protected)\b.*\b{re.escape(method_name)}\s*\(")
    for i, line in enumerate(lines):
        if pattern.search(line):
            return i
    return None


def _collect_annotations_above(lines: list[str], decl_index: int) -> list[str]:
    """Every annotation directly above the declaration, each returned as ONE
    string even when it spans several lines (a concatenated @PreAuthorize, a
    multi-line @Operation). ⚠ The previous version returned only the first
    line of a multi-line annotation, which is how every concatenated
    @PreAuthorize in four modules read as "no permission"."""
    block: list[str] = []
    depth = 0
    i = decl_index - 1
    while i >= 0:
        line = lines[i]
        stripped = line.strip()
        net = _paren_net(line)
        if depth == 0 and (not stripped or stripped.startswith(("*", "/**", "//")) or stripped.endswith("*/")):
            i -= 1
            continue
        if depth > 0 or net != 0 or stripped.startswith(("@", "+", ")", '"')):
            block.append(line)
            depth = max(depth - net, 0)
            i -= 1
            continue
        break
    return _split_annotations("\n".join(reversed(block)))


def _paren_net(line: str) -> int:
    masked = _STRING_LITERAL_RE.sub('""', line)
    return masked.count("(") - masked.count(")")


def _split_annotations(text: str) -> list[str]:
    """Splits a run of annotations into units by balanced parentheses, string
    literals honoured, so `@A("x(" + ")y")` is one unit and not two."""
    units: list[str] = []
    i = 0
    n = len(text)
    while i < n:
        at = text.find("@", i)
        if at < 0:
            break
        j = at + 1
        while j < n and (text[j].isalnum() or text[j] in "_."):
            j += 1
        k = j
        while k < n and text[k].isspace():
            k += 1
        if k < n and text[k] == "(":
            depth = 0
            in_string = False
            m = k
            while m < n:
                ch = text[m]
                if in_string:
                    if ch == "\\":
                        m += 1
                    elif ch == '"':
                        in_string = False
                elif ch == '"':
                    in_string = True
                elif ch == "(":
                    depth += 1
                elif ch == ")":
                    depth -= 1
                    if depth == 0:
                        m += 1
                        break
                m += 1
            units.append(text[at:m].strip())
            i = m
        else:
            units.append(text[at:j].strip())
            i = j
    return units


def parse_authorization_annotation(annotation: str) -> Optional[AuthorizationRule]:
    """`@PreAuthorize("a" + "b")`, `@PreAuthorize(value = "a")`, `@Secured({"A", "B"})`:
    the string literals are joined for PreAuthorize (Java concatenation) and
    listed for Secured (an authority array). A non-literal part (a constant
    spliced into the expression) is kept verbatim rather than dropped."""
    m = AUTH_ANNOTATION_RE.match(annotation)
    if not m:
        return None
    kind = m.group(1)
    open_paren = annotation.find("(", m.end())
    if open_paren < 0:
        return None
    inner = annotation[open_paren + 1:annotation.rfind(")")]
    literals = [lit.replace('\\"', '"') for lit in _STRING_LITERAL_RE.findall(inner)]
    if not literals:
        return None
    leftover = _STRING_LITERAL_RE.sub("", inner)
    leftover = re.sub(r"^\s*value\s*=", "", leftover)
    leftover = re.sub(r"[\s+{},]", "", leftover)
    if leftover:
        expression = re.sub(r"\s+", " ", inner.strip())
        return AuthorizationRule(kind, expression, extract_permission_constants(expression))
    if kind == "Secured":
        return AuthorizationRule(kind, ", ".join(literals), list(literals))
    expression = "".join(literals)
    return AuthorizationRule(kind, expression, extract_permission_constants(expression))


def _method_body_span(lines: list[str], decl_index: int) -> tuple[int, int]:
    """Returns (start, end) line indices covering the method body, found by
    brace-balance counting starting from the first '{' at/after decl_index."""
    depth = 0
    started = False
    start = decl_index
    for i in range(decl_index, len(lines)):
        for ch in lines[i]:
            if ch == "{":
                depth += 1
                started = True
            elif ch == "}":
                depth -= 1
        if started and depth <= 0:
            return start, i
    return start, len(lines) - 1


def _find_delegate(source: str, lines: list[str], decl_index: int, method_name: str) -> tuple[Optional[str], Optional[str]]:
    """Returns (service_class_name, service_method_name). Tries the same-name
    call first, then falls back to the single call made against a known
    `XxxService`-typed field, whatever its method name — see module
    docstring for why the fallback is needed."""
    start, end = _method_body_span(lines, decl_index)
    body = "\n".join(lines[start:end + 1])

    same_name_match = re.search(rf"\b(\w+)\.{re.escape(method_name)}\s*\(", body)
    if same_name_match:
        var_name = same_name_match.group(1)
        field_match = re.search(FIELD_DECL_TEMPLATE.format(name=re.escape(var_name)), source)
        if field_match:
            return field_match.group(1), method_name

    service_fields = SERVICE_FIELD_DECL_RE.findall(source)
    candidates = []
    for field_type, field_name in service_fields:
        for m in re.finditer(rf"\b{re.escape(field_name)}\.(\w+)\s*\(", body):
            candidates.append((field_type, m.group(1)))
    if len(candidates) == 1:
        return candidates[0]
    return None, None


def find_authorization_rule(source: str, method_name: str) -> Optional[AuthorizationRule]:
    lines = strip_comments(source).splitlines()
    decl_index = _find_declaration_index(lines, method_name)
    if decl_index is None:
        return None
    for annotation in _collect_annotations_above(lines, decl_index):
        rule = parse_authorization_annotation(annotation)
        if rule:
            return rule
    return None


def find_preauthorize(source: str, method_name: str) -> Optional[str]:
    rule = find_authorization_rule(source, method_name)
    return rule.expression if rule else None


def has_method(source: str, method_name: str) -> bool:
    return _find_declaration_index(strip_comments(source).splitlines(), method_name) is not None


def find_delegate(source: str, method_name: str) -> tuple[Optional[str], Optional[str]]:
    source = strip_comments(source)
    lines = source.splitlines()
    decl_index = _find_declaration_index(lines, method_name)
    if decl_index is None:
        return None, None
    return _find_delegate(source, lines, decl_index, method_name)


def extract_permission_constants(spel_expression: str) -> list[str]:
    found = PERMISSION_CONST_RE.findall(spel_expression)
    found += [c for c in PERMISSION_LITERAL_RE.findall(spel_expression) if c not in found]
    return found


def resolve_permission(
    controller_source: str, method_name: str, source_root: Path,
    controller_name: str = "controller",
) -> PermissionLookup:
    """Controller method first, then the service method it delegates to. Every
    method read is recorded in `checked`, so DECLARED_NONE names exactly what
    was inspected and a NOT_EXTRACTED outcome names where the walk stopped."""
    checked = [f"{controller_name}.{method_name}"]
    rule = find_authorization_rule(controller_source, method_name)
    if rule:
        return PermissionLookup(FOUND, rule.constants, "controller", rule.expression, checked)

    service_class, service_method = find_delegate(controller_source, method_name)
    if not service_class or not service_method:
        return PermissionLookup(DELEGATE_UNRESOLVED, checked=checked)

    matches = sorted(source_root.rglob(f"{service_class}.java"))
    if not matches:
        return PermissionLookup(SERVICE_SOURCE_MISSING, checked=checked + [f"{service_class}.{service_method}"])

    service_source = matches[0].read_text(encoding="utf-8")
    checked.append(f"{service_class}.{service_method}")
    if not has_method(service_source, service_method):
        return PermissionLookup(SERVICE_METHOD_MISSING, checked=checked)
    rule = find_authorization_rule(service_source, service_method)
    if not rule:
        return PermissionLookup(DECLARED_NONE, checked=checked)
    return PermissionLookup(FOUND, rule.constants, f"service:{service_class}", rule.expression, checked)


_AUTH_ANNOTATION_COUNT_RE = re.compile(r"@(?:PreAuthorize|Secured)\s*\(")


def count_authorization_annotations(source_root: Path) -> int:
    """How many @PreAuthorize/@Secured the module's source carries, comments
    excluded. The expected side of the check mode's permission ratio: not a
    denominator (a service method need not be an endpoint), an existence signal."""
    total = 0
    for path in sorted(source_root.rglob("*.java")):
        text = _COMMENT_RE.sub("", path.read_text(encoding="utf-8", errors="ignore"))
        text = _STRING_LITERAL_RE.sub('""', text)
        total += len(_AUTH_ANNOTATION_COUNT_RE.findall(text))
    return total
