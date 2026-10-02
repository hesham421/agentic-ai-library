"""
Best-effort discovery of error-code -> HTTP-status information that has no
OpenAPI representation at all (no controller anywhere declares
@ApiResponse/@ApiResponses, so springdoc never sees this).

Two distinct, shared, module-independent files carry this information across
every module of this ERP platform's backend architecture:

  - OperationCodeImpl's static Status -> HttpStatus table is the ONLY place
    that mapping exists (its own doc comment says so). It's shared and
    identical for every module, so it's read once from wherever the caller's
    discovered common-source roots point, never per module.

  - GlobalExceptionHandler's @ExceptionHandler methods hardcode the HTTP
    status for framework-level exceptions (validation, malformed JSON, method
    not allowed, ...) that every endpoint in every module can produce. These
    are read the same way, once, from the shared common-source roots.

A third, per-module piece -- which business Status a specific module's own
error code (e.g. a name-duplicate or cycle-detection code) was thrown with --
is NOT centralized; it only exists at each module's own throw sites
(`new BusinessException(Status.X, SomeErrorCodes.Y, ...)` /
`new LocalizedException(Status.X, SomeErrorCodes.Y, ...)` /
`LocalizedException.withDetails(Status.X, SomeErrorCodes.Y, ...)`). That part is
read from the module's own --source, one module at a time, and only recorded
when a throw site literally names both together -- never guessed from a
code's name or value.
"""

import re
from pathlib import Path
from typing import Optional

from models.api_doc_model import ApiDocument, AuthEntryPoint, ErrorCode, FieldErrorSemantics, PossibleError, StatusMapping

# Two shapes of the same shared table are recognised, because this platform
# has used both: an explicit statusMappings.put(...) table in a helper class,
# and -- currently -- the Status enum carrying its own HttpStatus in its
# constructor (NOT_FOUND(HttpStatus.NOT_FOUND), ...). Only matching the first
# meant the whole Status -> HTTP mapping silently came back empty, so every
# error code documented its business Status with a blank HTTP status beside it.
STATUS_MAPPING_RE = re.compile(r"statusMappings\.put\(\s*Status\.(\w+)\s*,\s*HttpStatus\.(\w+)\s*\)")
STATUS_ENUM_CONSTANT_RE = re.compile(r"^\s*([A-Z][A-Z0-9_]*)\s*\(\s*HttpStatus\.(\w+)\s*\)", re.MULTILINE)
STATUS_ENUM_DECL_RE = re.compile(r"\benum\s+Status\b")

# Framework handlers are matched in both the helper form (createError("X"))
# and the builder form this codebase uses (ApiError.builder().code("X")),
# paired with the HTTP status of the same @ExceptionHandler method -- written
# either as ResponseEntity.status(HttpStatus.X) or one of ResponseEntity's
# named shortcuts (badRequest()/notFound()/...).
CREATE_ERROR_CODE_RE = re.compile(r'(?:createError|\.code)\(\s*"([A-Z0-9_]+)"')
# The same argument written as a constant reference -- .code(CommonErrorCodes.
# VALIDATION_ERROR) -- which is how this codebase now spells it. Matching only
# the string-literal form above meant every framework code silently vanished
# from the docs the moment the handler was refactored to use the constants
# class, with no error and no empty section to notice: the codes were simply
# gone. The constant's literal value is then resolved from its own class, and
# only where that class is actually found; an unresolvable constant keeps its
# name as the documented code rather than being dropped or guessed at.
CREATE_ERROR_CODE_CONST_RE = re.compile(r'(?:createError|\.code)\(\s*(?:[A-Z]\w*\.)?([A-Z][A-Z0-9_]*)\s*[,)]')
_CONSTANT_DECL_RE = re.compile(r'\bstatic\s+final\s+String\s+([A-Z][A-Z0-9_]*)\s*=\s*"([^"]*)"')
RESPONSE_STATUS_RE = re.compile(r"ResponseEntity\.status\(\s*HttpStatus\.(\w+)\s*\)")
RESPONSE_SHORTCUT_RE = re.compile(r"ResponseEntity\.(badRequest|notFound|unprocessableEntity)\s*\(")
RESPONSE_SHORTCUT_STATUS = {
    "badRequest": "BAD_REQUEST",
    "notFound": "NOT_FOUND",
    "unprocessableEntity": "UNPROCESSABLE_ENTITY",
}

# Spring's own HttpStatus constant -> numeric code. A test asserting on a
# status needs the number, and the constant name alone never carries it.
# Only constants this platform's Status enum / GlobalExceptionHandler can
# actually produce are listed; an unlisted one renders as its bare name
# rather than a guessed number.
HTTP_STATUS_CODES = {
    "OK": 200, "CREATED": 201, "ACCEPTED": 202, "NO_CONTENT": 204,
    "BAD_REQUEST": 400, "UNAUTHORIZED": 401, "FORBIDDEN": 403, "NOT_FOUND": 404,
    "METHOD_NOT_ALLOWED": 405, "NOT_ACCEPTABLE": 406, "CONFLICT": 409, "GONE": 410,
    "PAYLOAD_TOO_LARGE": 413, "CONTENT_TOO_LARGE": 413, "URI_TOO_LONG": 414,
    "UNSUPPORTED_MEDIA_TYPE": 415, "UNPROCESSABLE_ENTITY": 422,
    "UNPROCESSABLE_CONTENT": 422, "TOO_MANY_REQUESTS": 429,
    "INTERNAL_SERVER_ERROR": 500, "NOT_IMPLEMENTED": 501, "BAD_GATEWAY": 502,
    "SERVICE_UNAVAILABLE": 503, "GATEWAY_TIMEOUT": 504,
}


def http_status_label(constant: Optional[str]) -> Optional[str]:
    """"CONFLICT" -> "409 CONFLICT". Left as the bare constant when its
    numeric code isn't known -- never guessed."""
    if not constant:
        return constant
    code = HTTP_STATUS_CODES.get(constant)
    return f"{code} {constant}" if code else constant
# Both throw forms carrying a (Status, ErrorCode) pair: the plain constructor, and
# LocalizedException.withDetails(Status, code, List<ErrorDetail>) -- the multi-error form
# used where one code is attributed to the request field(s) it concerns. Missing the second
# form silently blanks that code's Status/HTTP columns in index.md rather than failing.
THROW_RE = re.compile(
    r"(?:new\s+(?:BusinessException|LocalizedException)|LocalizedException\.withDetails)"
    r"\(\s*Status\.(\w+)\s*,\s*\w+\.(\w+)")


# aias adaptation: the module's exception is constructed with the code constant
# as its FIRST argument -- new ServiceRegistryException(ServiceRegistryErrorCodes.X, ...)
# or, in a subclass, super(CheckRejectionCodes.X, ...) -- and carries no Status:
# the HTTP status is the {http} segment of the code's value. Built from the
# configured code-class globs (conventions.json -> error_code_classes), so only a
# constant of a code class counts.
def code_argument_throw_re(class_globs) -> re.Pattern:
    suffixes = sorted({g.rsplit("/", 1)[-1].lstrip("*").removesuffix(".java") for g in class_globs})
    alternation = "|".join(re.escape(s) for s in suffixes if s)
    return re.compile(r"(?:\bnew\s+[A-Z]\w*|\bsuper)\s*\(\s*[A-Z]\w*(?:" + alternation
                      + r")\s*\.\s*([A-Z][A-Z0-9_]*)\b")


def http_status_from_value(value: str, pattern: Optional[str]) -> Optional[str]:
    """"REG-404-SERVICE-NOT-FOUND" -> "404 NOT_FOUND" when the project declares
    that a code value carries its HTTP status (group 1 of `pattern`). None when
    no pattern is declared or the value does not match -- never guessed."""
    if not pattern or not value:
        return None
    m = re.match(pattern, value)
    if not m:
        return None
    number = int(m.group(1))
    names = sorted(k for k, v in HTTP_STATUS_CODES.items() if v == number)
    preferred = {413: "CONTENT_TOO_LARGE", 422: "UNPROCESSABLE_ENTITY"}
    name = preferred.get(number) if preferred.get(number) in names else (names[0] if names else None)
    return f"{number} {name}" if name else str(number)


def find_status_http_mapping(common_source_roots: list[Path]) -> dict[str, str]:
    return find_status_http_mapping_with_source(common_source_roots)[0]


def find_status_http_mapping_with_source(
    common_source_roots: list[Path],
) -> tuple[dict[str, str], Optional[str]]:
    """(Status constant name -> HttpStatus constant name, the file it was read
    from). The source file is returned rather than assumed, since the table
    has lived in more than one place on this platform."""
    for root in common_source_roots:
        for path in sorted(root.rglob("OperationCodeImpl.java")):
            text = path.read_text(encoding="utf-8")
            mapping = {m.group(1): m.group(2) for m in STATUS_MAPPING_RE.finditer(text)}
            if mapping:
                return mapping, path.name

    for root in common_source_roots:
        for path in sorted(root.rglob("Status.java")):
            text = path.read_text(encoding="utf-8")
            if not STATUS_ENUM_DECL_RE.search(text):
                continue
            mapping = {m.group(1): m.group(2) for m in STATUS_ENUM_CONSTANT_RE.finditer(text)}
            if mapping:
                return mapping, path.name
    return {}, None


def find_error_code_constants(common_source_roots: list[Path]) -> dict[str, str]:
    """Constant name -> its literal value, for every *ErrorCodes class under
    the shared roots. Read from the declarations themselves, so a constant
    whose value differs from its name (SEC-401-INVALID-CREDENTIALS vs
    SEC_401_INVALID_CREDENTIALS) documents the value actually sent on the
    wire."""
    constants: dict[str, str] = {}
    for root in common_source_roots:
        for path in sorted(root.rglob("*ErrorCodes.java")):
            text = path.read_text(encoding="utf-8", errors="ignore")
            for m in _CONSTANT_DECL_RE.finditer(text):
                constants.setdefault(m.group(1), m.group(2))
    return constants


def _split_handler_methods(text: str) -> list[str]:
    parts = re.split(r"(?=@ExceptionHandler)", text)
    return parts[1:]


def find_framework_error_codes(common_source_roots: list[Path]) -> list[ErrorCode]:
    """Framework-level error codes (apply to any endpoint, any module) with
    their real HTTP status, parsed from the shared GlobalExceptionHandler."""
    codes: list[ErrorCode] = []
    seen: set[str] = set()
    constants = find_error_code_constants(common_source_roots)
    for root in common_source_roots:
        for path in sorted(root.rglob("GlobalExceptionHandler.java")):
            text = path.read_text(encoding="utf-8")
            try:
                rel = str(path.relative_to(root))
            except ValueError:
                rel = path.name
            for chunk in _split_handler_methods(text):
                code_m = CREATE_ERROR_CODE_RE.search(chunk) or CREATE_ERROR_CODE_CONST_RE.search(chunk)
                if not code_m:
                    continue
                status_m = RESPONSE_STATUS_RE.search(chunk)
                if status_m:
                    http = status_m.group(1)
                else:
                    shortcut_m = RESPONSE_SHORTCUT_RE.search(chunk)
                    if not shortcut_m:
                        # e.g. the LocalizedException handler, whose status is
                        # ex.getStatus().getHttpStatus() -- per-throw-site, not
                        # a fixed framework status. Nothing to record here.
                        continue
                    http = RESPONSE_SHORTCUT_STATUS[shortcut_m.group(1)]
                name = code_m.group(1)
                if name in seen:
                    continue
                seen.add(name)
                codes.append(ErrorCode(name=name, value=constants.get(name, name),
                                       source_file=rel, http_status=http))
    return codes


def find_business_status_associations(source_root: Path) -> dict[str, str]:
    """Best-effort: error-code constant name -> Status constant name, parsed
    from throw sites in the module's own source. Only records a pairing that
    is literally spelled out at a throw site -- never inferred from the
    code's own name or value."""
    associations: dict[str, str] = {}
    for path in sorted(source_root.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        for m in THROW_RE.finditer(text):
            status_name, const_name = m.groups()
            associations.setdefault(const_name, status_name)
    return associations


def enrich_error_codes(
    module_error_codes: list[ErrorCode],
    source_root: Optional[Path],
    common_source_roots: list[Path],
    code_status_pattern: Optional[str] = None,
) -> tuple[list[ErrorCode], list[StatusMapping]]:
    """Fills in .status/.http_status on the module's own error codes where a
    throw site names both, appends the shared framework-level codes, and
    returns the shared Status->HttpStatus table for rendering once. Every
    piece here is best-effort and silently omitted when not found -- never
    fabricated. No-ops entirely if no common_source_roots were discovered."""
    status_http, table_source = find_status_http_mapping_with_source(common_source_roots)
    status_mappings = [
        StatusMapping(name=name, http_status=http_status_label(http), source_file=table_source)
        for name, http in sorted(status_http.items())
    ]

    if source_root is not None:
        associations = find_business_status_associations(source_root)
        for code in module_error_codes:
            status_name = associations.get(code.name)
            if not status_name:
                continue
            code.status = status_name
            http = status_http.get(status_name)
            if http:
                code.http_status = http_status_label(http)

    for code in module_error_codes:
        if not code.http_status:
            derived = http_status_from_value(code.value, code_status_pattern)
            if derived:
                code.http_status = derived
                code.http_from_value = True

    framework_codes = find_framework_error_codes(common_source_roots)
    for code in framework_codes:
        code.http_status = http_status_label(code.http_status)
    return module_error_codes + framework_codes, status_mappings


ENTRY_POINT_WIRING_RE = re.compile(r"\.authenticationEntryPoint\(\s*(\w+)\s*\)")
COMMENCE_HEAD_RE = re.compile(r"\bcommence\s*\([^)]*\)\s*(?:throws\s+[\w.,\s]+)?\{")
UNAUTHORIZED_THEN_CODE_RE = re.compile(
    r"(?:SC_UNAUTHORIZED|HttpStatus\.UNAUTHORIZED|\b401\b)[\s\S]{0,200}?\b(?:\w*ErrorCodes)\.([A-Z][A-Z0-9_]*)")


def find_auth_entry_point(common_source_roots: list[Path]) -> Optional[AuthEntryPoint]:
    """The class SecurityConfig wires as authenticationEntryPoint, resolved
    through its field type, and the error-code constant its commence(...)
    writes next to a 401. An inline entry point (new HttpStatusEntryPoint(...))
    resolves to nothing, and nothing is claimed."""
    constants = find_error_code_constants(common_source_roots)
    for root in common_source_roots:
        for config in sorted(root.rglob("*.java")):
            text = config.read_text(encoding="utf-8", errors="ignore")
            if "SecurityFilterChain" not in text:
                continue
            wiring = ENTRY_POINT_WIRING_RE.search(text)
            if not wiring:
                continue
            field_m = re.search(r"private\s+final\s+(\w+)\s+" + re.escape(wiring.group(1)) + r"\s*;", text)
            if not field_m:
                continue
            handler_name = field_m.group(1)
            for handler in sorted(root.rglob(f"{handler_name}.java")):
                body_text = handler.read_text(encoding="utf-8", errors="ignore")
                head = COMMENCE_HEAD_RE.search(body_text)
                if not head:
                    continue
                body = body_text[head.end() - 1:_balanced_block_end(body_text, head.end() - 1)]
                code_m = UNAUTHORIZED_THEN_CODE_RE.search(body)
                if code_m:
                    return AuthEntryPoint(handler=handler_name, code=code_m.group(1),
                                          value=constants.get(code_m.group(1), code_m.group(1)), config=config.stem)
    return None


def _balanced_block_end(text: str, open_index: int) -> int:
    depth = 0
    for i in range(open_index, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return i + 1
    return len(text)


def _access_denied_override(ep, overrides, service_package_of):
    """The advisor whose pointcut covers the class carrying this endpoint's
    rule, or None. A scope that could not be read is never assumed to match."""
    if not overrides or not ep.permission_source:
        return None
    if ep.permission_source.startswith("service:") and service_package_of:
        package = service_package_of(ep.permission_source.split(":", 1)[1]) or ""
        target = package + "."
    else:
        target = None
    for o in overrides:
        if o.scope_prefix and target and target.startswith(o.scope_prefix):
            return o
    return None


def attach_endpoint_error_codes(document: ApiDocument, status_http: Optional[dict[str, str]] = None,
                                service_package_of=None) -> None:
    """Attaches framework-level errors to individual endpoints, but only
    where BOTH halves of the claim are independently verified real facts:
    (a) a per-endpoint property already established by another extractor
    (requires_auth from the OpenAPI security requirement, permission from a
    real @PreAuthorize/@Secured found by security_extractor, request_body
    from the OpenAPI request body), and (b) a framework error code this run
    actually found in GlobalExceptionHandler.

    Codes are looked up by the HTTP STATUS the handler really returns, not by
    a hardcoded code name: a code's name is a project naming choice that
    changes (this platform returns ACCESS_DENIED, not "FORBIDDEN", and has no
    "INVALID_JSON" at all), whereas "the handler that answers 403" is the
    fact being asserted. If no handler for that status was found, nothing is
    claimed -- as before, silence rather than a stale assertion.

    Deliberately narrow: three rules, each unconditionally true of the
    Spring MVC/Security stack this platform runs on, never a guess about
    business logic:
      - requires_auth       -> the 401 handler, when one exists
      - permission found    -> the 403 handler (a real @PreAuthorize/@Secured
                                expression was found for this exact endpoint;
                                AccessDeniedException is handled globally)
      - has a request body  -> the 400 handler (any @RequestBody is
                                deserialized by Jackson before the controller
                                method runs, unconditionally; the framework
                                catches malformed JSON globally)
    """
    values = {c.name: c.value for c in document.error_codes}
    by_status: dict[str, ErrorCode] = {}
    for code in document.error_codes:
        if not code.http_status or code.status or code.http_from_value:
            # code.status set => a module business code enriched from its own
            # throw site, not a framework handler. Only framework-level codes
            # (which carry an HTTP status and no business Status) qualify.
            continue
        constant = code.http_status.split()[-1]
        by_status.setdefault(constant, code)

    def _attach(ep, status_constant: str, reason: str) -> None:
        code = by_status.get(status_constant)
        if not code:
            return
        ep.possible_errors.append(PossibleError(code=code.name, http_status=code.http_status, reason=reason))

    entry = document.auth_entry_point
    for ep in document.endpoints:
        if ep.requires_auth and entry:
            ep.possible_errors.append(PossibleError(
                code=f"{entry.code} (`{entry.value}`)", http_status=http_status_label("UNAUTHORIZED"),
                reason=f"Endpoint requires authentication; an unauthenticated call never reaches the controller "
                       f"— {entry.handler}.commence, wired as {entry.config}'s authenticationEntryPoint, "
                       f"writes this code."))
        elif ep.requires_auth:
            _attach(
                ep, "UNAUTHORIZED",
                "Endpoint requires authentication (global security requirement); "
                "an unauthenticated call is rejected before the controller method runs.",
            )
        if ep.permission or ep.permission_expression:
            override = _access_denied_override(ep, document.access_denied_overrides, service_package_of)
            unscoped = [o for o in document.access_denied_overrides if not o.scope_prefix]
            if override:
                http = http_status_label((status_http or {}).get(override.status)) or override.status
                ep.possible_errors.append(PossibleError(
                    code=f"{override.code} (`{values.get(override.code, override.code)}`)", http_status=http,
                    reason=f"An authorization check was found for this endpoint (@PreAuthorize/@Secured); "
                           f"{override.advisor} wraps `{override.scope_prefix}*`, catches AccessDeniedException "
                           f"and re-raises it as this module code, so the shared ACCESS_DENIED handler is not reached."))
            elif unscoped:
                names = ", ".join(f"{o.advisor} → {o.code}" for o in unscoped)
                _attach(
                    ep, "FORBIDDEN",
                    f"An authorization check was found for this endpoint (@PreAuthorize/@Secured). "
                    f"GlobalExceptionHandler maps AccessDeniedException to this status, but {names} may "
                    f"re-raise it as a module code first — its scope could not be read from source.",
                )
            else:
                _attach(
                    ep, "FORBIDDEN",
                    "An authorization check was found for this endpoint "
                    "(@PreAuthorize/@Secured); GlobalExceptionHandler maps AccessDeniedException to this status.",
                )
        if ep.request_body is not None:
            _attach(
                ep, "BAD_REQUEST",
                "Endpoint accepts a JSON request body; GlobalExceptionHandler maps a malformed or "
                "invalid body (HttpMessageNotReadableException / MethodArgumentNotValidException) to this status.",
            )


EXCEPTION_HANDLER_ARGS_RE = re.compile(r"@ExceptionHandler\s*(?:\(([^)]*)\))?")
EXCEPTION_CLASS_RE = re.compile(r"([\w.]+)\.class")
HANDLER_NAME_RE = re.compile(r"\b(?:public|protected|private)\b[^;{(]*?\b(\w+)\s*\(")
FIELD_CALL_RE = re.compile(r"\.field\s*\(")
TERNARY_NULL_RE = re.compile(r"^(.*?)\s*(!=|==)\s*null\s*\?\s*(.*?)\s*:\s*(.*)$", re.DOTALL)
PER_THROW_STATUS_RE = re.compile(r"getHttpStatus\(\)")


def _balanced_argument(text: str, open_index: int) -> str:
    depth = 0
    for i in range(open_index, len(text)):
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
            if depth == 0:
                return text[open_index + 1:i]
    return text[open_index + 1:]


def _field_meaning(expression: str) -> str:
    """A mechanical reading of the expression: a null-test ternary is spelled
    out as its two branches; anything else is quoted as-is. Nothing is inferred
    about what the branches mean."""
    m = TERNARY_NULL_RE.match(expression)
    if m:
        tested, op, when_true, when_false = (g.strip() for g in m.groups())
        non_null, fallback = (when_true, when_false) if op == "!=" else (when_false, when_true)
        return f"`{non_null}` when `{tested}` is non-null, otherwise `{fallback}`"
    return f"the value of `{expression}`"


def find_field_error_semantics(common_source_roots: list[Path]) -> list[FieldErrorSemantics]:
    """What each GlobalExceptionHandler method passes to FieldErrorItem.field(...),
    verbatim, with the handler's exceptions, HTTP status and error code. The
    same schema slot carries different things per handler, and only the
    handler source says which."""
    rows: list[FieldErrorSemantics] = []
    constants = find_error_code_constants(common_source_roots)
    for root in common_source_roots:
        for path in sorted(root.rglob("GlobalExceptionHandler.java")):
            text = path.read_text(encoding="utf-8")
            for chunk in _split_handler_methods(text):
                field_call = FIELD_CALL_RE.search(chunk)
                if not field_call:
                    continue
                args_m = EXCEPTION_HANDLER_ARGS_RE.match(chunk)
                exceptions = [e.split(".")[-1] for e in EXCEPTION_CLASS_RE.findall(args_m.group(1) or "")] if args_m else []
                name_m = HANDLER_NAME_RE.search(chunk)
                expression = re.sub(r"\s+", " ", _balanced_argument(chunk, field_call.end() - 1).strip())
                code_m = CREATE_ERROR_CODE_RE.search(chunk) or CREATE_ERROR_CODE_CONST_RE.search(chunk)
                code = constants.get(code_m.group(1), code_m.group(1)) if code_m else None
                status_m = RESPONSE_STATUS_RE.search(chunk)
                shortcut_m = RESPONSE_SHORTCUT_RE.search(chunk)
                if status_m:
                    http = http_status_label(status_m.group(1))
                elif shortcut_m:
                    http = http_status_label(RESPONSE_SHORTCUT_STATUS[shortcut_m.group(1)])
                elif PER_THROW_STATUS_RE.search(chunk):
                    http = "the thrown Status's HTTP status"
                    code = code or "the thrown error code"
                else:
                    http = None
                rows.append(FieldErrorSemantics(
                    handler=name_m.group(1) if name_m else "?", exceptions=exceptions,
                    field_expression=expression, field_meaning=_field_meaning(expression),
                    http_status=http, code=code))
    return rows
