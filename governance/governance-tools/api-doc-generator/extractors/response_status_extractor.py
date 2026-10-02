"""
BEST-EFFORT resolution of an endpoint's real success HTTP status.

springdoc declares `200` for every operation that carries no explicit
`@ApiResponse`, and no controller in this codebase declares one — so every
endpoint published as `200 — OK`, including the creates, which this platform
answers with `201` (the controller's shared response helper sets the status
from the `Status` the service returned: `Status.CREATED` -> 201). The mapping
itself is never invented here; it is the same shared `Status -> HttpStatus`
table error_mapping_extractor already reads from the platform's own source.

What this module adds is the missing link: WHICH `Status` a given endpoint's
service method actually returns. It is read structurally, never guessed:

  1. the controller method delegates to exactly one method on a class in the
     module's own source (the same one-delegate convention security_extractor
     relies on for permissions);
  2. that method's return type names a result wrapper class
     (`ServiceResult<T>`), whose own source declares static factories;
  3. each factory's default `Status` is read from the wrapper's own body
     (`success(T data)` -> `new ServiceResult<>(data, Status.SUCCESS)`), and a
     call site passing a `Status` explicitly overrides it;
  4. only when every `return` in the method agrees on one `Status` is a status
     published — a method with two different outcomes keeps the OpenAPI
     document's own answer rather than having one of them picked for it.

Nothing here is module-specific: no class name, no status and no HTTP code is
spelled as a literal.
"""

import re
from pathlib import Path
from typing import Optional

from extractors.business_error_extractor import CONSTRUCTOR, ClassInfo, MethodInfo, _calls_in, _paren_end

RESULT_TYPE_RE = re.compile(r"^([A-Z]\w*)")
STATUS_ARG_RE = re.compile(r"(?<![\w.])Status\s*\.\s*(\w+)")


def _result_type_name(return_type: str) -> Optional[str]:
    m = RESULT_TYPE_RE.match(return_type.strip())
    return m.group(1) if m else None


def factory_default_statuses(result_class: ClassInfo) -> dict[str, Optional[str]]:
    """{factory method name -> the Status its body hands the constructor}, for
    every static factory of the result wrapper. A factory that takes the
    Status as a parameter (its body names no Status constant) maps to None,
    which means "read it from the call site"."""
    defaults: dict[str, Optional[str]] = {}
    for name, overloads in result_class.methods.items():
        if name == CONSTRUCTOR:
            continue
        for mi in overloads:
            found = sorted(set(STATUS_ARG_RE.findall(mi.body)))
            if len(found) == 1:
                defaults.setdefault(name, found[0])
            else:
                defaults.setdefault(name, None)
    return defaults


def _returned_statuses(method: MethodInfo, result_type: str,
                       defaults: dict[str, Optional[str]]) -> set[Optional[str]]:
    """Every Status this method's `<ResultType>.<factory>(...)` returns can
    carry. An explicit Status among the call's own arguments wins over the
    factory's default, exactly as the factory overload does at runtime."""
    statuses: set[Optional[str]] = set()
    call_re = re.compile(rf"(?<![\w.]){re.escape(result_type)}\s*\.\s*(\w+)\s*\(")
    for m in call_re.finditer(method.body):
        factory = m.group(1)
        if factory not in defaults:
            continue
        open_index = m.end() - 1
        args = method.body[open_index + 1:_paren_end(method.body, open_index) - 1]
        explicit = STATUS_ARG_RE.findall(args)
        if len(set(explicit)) == 1:
            statuses.add(explicit[0])
        elif explicit:
            statuses.add(None)
        else:
            statuses.add(defaults[factory])
    return statuses


def resolve_success_status(classes: dict[str, ClassInfo], controller: str, method_name: str,
                           common_source_roots: list[Path]) -> Optional[tuple[str, str]]:
    """(Status constant, "Class.method" it was read at) for the endpoint whose
    controller method is `controller.method_name`, or None when it could not
    be resolved with certainty."""
    controller_class = classes.get(controller)
    if not controller_class or method_name not in controller_class.methods:
        return None
    overloads = controller_class.methods[method_name]
    if len(overloads) != 1:
        return None
    delegates = [(c, n) for c, n, _ in _calls_in(controller_class, overloads[0], classes)
                 if c != controller and n != CONSTRUCTOR]
    if len(set(delegates)) != 1:
        return None
    service_name, service_method = delegates[0]
    service = classes[service_name]
    service_overloads = service.methods.get(service_method, [])
    if len(service_overloads) != 1:
        return None
    service_mi = service_overloads[0]

    result_type = _result_type_name(service_mi.return_type)
    if not result_type:
        return None
    result_class = classes.get(result_type) or _find_class(result_type, common_source_roots)
    if result_class is None:
        return None

    defaults = factory_default_statuses(result_class)
    statuses = _returned_statuses(service_mi, result_type, defaults)
    if len(statuses) != 1:
        return None
    status = next(iter(statuses))
    if not status:
        return None
    return status, f"{service_name}.{service_method}"


_class_cache: dict[tuple[str, tuple[str, ...]], Optional[ClassInfo]] = {}


def _find_class(name: str, common_source_roots: list[Path]) -> Optional[ClassInfo]:
    """The shared result wrapper lives in the platform's common source, not in
    the module's own tree, so it is looked up there by file name. Cached: the
    same wrapper is asked for once per endpoint."""
    from extractors.business_error_extractor import _parse_class

    key = (name, tuple(str(p) for p in common_source_roots))
    if key in _class_cache:
        return _class_cache[key]
    found: Optional[ClassInfo] = None
    for root in common_source_roots:
        for path in sorted(root.rglob(f"{name}.java")):
            info = _parse_class(path)
            if info and info.name == name:
                found = info
                break
        if found:
            break
    _class_cache[key] = found
    return found


def attach_response_statuses(document, classes: dict[str, ClassInfo],
                             controller_of: dict[int, tuple[str, str]],
                             status_http: dict[str, str],
                             common_source_roots: list[Path],
                             http_status_label) -> None:
    """Rewrites each endpoint's success response status from the Status its
    service actually returns. Only a response the OpenAPI document declared as
    the generic success default is touched, and only when the resolved status
    maps to a different HTTP code — an operation that declares its own
    responses keeps them."""
    default_success = {"200", "default"}
    for ep in document.endpoints:
        located = controller_of.get(id(ep))
        if not located:
            continue
        resolved = resolve_success_status(classes, *located,
                                          common_source_roots=common_source_roots)
        if resolved is None:
            continue
        status, site = resolved
        http_constant = status_http.get(status)
        if not http_constant:
            continue
        label = http_status_label(http_constant)
        code = label.split()[0] if label and label.split()[0].isdigit() else None
        if not code:
            continue
        for resp in ep.responses:
            if resp.status_code not in default_success or resp.status_code == code:
                continue
            resp.status_code = code
            resp.description = http_constant.replace("_", " ").title()
            resp.status_source = f"`Status.{status}` returned by `{site}`"
