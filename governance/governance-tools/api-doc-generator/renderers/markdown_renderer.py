"""
Markdown renderer: one index.md (module overview + shared sections) plus one
file per GROUP under endpoints/<group-slug>.md, containing every endpoint in
that group as its own "## {METHOD} {path}" section. That heading text is the
stable key sync.py diffs endpoint sections by — see sync.py's module
docstring. Every subsection is conditional on data actually being present —
nothing here invents content; absent data just means the subsection doesn't
get written.
"""

import json
import re
from dataclasses import replace
from typing import Optional

from models.api_doc_model import (
    ApiDocument,
    Endpoint,
    ErrorCode,
    FieldErrorSemantics,
    FieldSpec,
    PaginationConstraints,
    Parameter,
    ResponseEnvelope,
    StatusMapping,
    UniqueConstraint,
)
from renderers.base import Renderer


def _slugify(text: str) -> str:
    text = re.sub(r"[^A-Za-z0-9]+", "-", text.strip()).strip("-").lower()
    return text or "root"


def group_file_paths(groups: list[str]) -> dict[str, str]:
    """{group name: output path}, guaranteeing one distinct file per group.

    Two different @Tag values can slugify to the same filename ("Role Grants"
    and "Role-Grants" both give "role-grants"), and writing both to one path
    would silently drop a whole controller's documentation. Collisions are
    disambiguated by suffix in group order, which is deterministic for a given
    OpenAPI document, so the same backend always renders the same paths.

    This is the single definition of a group's path -- sync.py imports it
    rather than recomputing one, so the file a group renders to and the file
    sync diffs/deletes it as can never drift apart."""
    paths: dict[str, str] = {}
    used: set[str] = set()
    for group in groups:
        base = _slugify(group)
        slug, n = base, 1
        while slug in used:
            n += 1
            slug = f"{base}-{n}"
        used.add(slug)
        paths[group] = f"endpoints/{slug}.md"
    return paths


def _anchor_slug(method: str, path: str) -> str:
    """Reproduces the GitHub-flavored-markdown anchor a '## {method} {path}'
    heading resolves to: lowercase, strip everything but word chars/spaces/
    hyphens (this drops '/', '{', '}', backticks, etc. with NO replacement
    character, matching GitHub's own slugger — adjacent path segments run
    together, e.g. '/widgets/{id}' -> 'widgetsid'), then turn the remaining
    run of spaces into a single hyphen. Anchor rules differ from _slugify's
    filename rules (which do insert a hyphen for stripped punctuation), so
    this is intentionally a separate function, not a _slugify reuse."""
    heading = f"{method} {path}".lower()
    heading = re.sub(r"[^\w\- ]", "", heading)
    heading = re.sub(r"\s+", "-", heading.strip())
    return heading


def _flatten_fields(fields: list[FieldSpec], prefix: str = "") -> list[FieldSpec]:
    """Flattens FieldSpec.nested (populated by dto_extractor's recursive
    schema expansion) into dotted-path rows -- parent.child, or parent[].child
    for array-of-object fields -- so a nested DTO like a search filter or a
    field-error item renders as real rows instead of an opaque type name.
    Self-referential schemas (FieldSpec.recursive_ref) get one explanatory
    row instead of expanding forever."""
    flat: list[FieldSpec] = []
    for f in fields:
        row = replace(f, name=f"{prefix}{f.name}") if prefix else replace(f)
        if f.nested_schema_description:
            # The nested DTO's own class-level note. It qualifies every child
            # row that follows, so it belongs on the parent row rather than
            # being dropped when the object is flattened away.
            row.description = f"{row.description} — " if row.description else ""
            row.description += f.nested_schema_description
        flat.append(row)
        if f.recursive_ref:
            flat.append(FieldSpec(
                name=f"{prefix}{f.name}[]" if f.is_array else f"{prefix}{f.name}",
                type=f"same shape as `{f.recursive_ref}` (recursive)",
                description="Recursive — repeats this field's own structure.",
            ))
        elif f.nested:
            child_prefix = f"{prefix}{f.name}[]." if f.is_array else f"{prefix}{f.name}."
            flat.extend(_flatten_fields(f.nested, child_prefix))
    return flat


def _constraints_text(f: FieldSpec) -> str:
    parts = []
    if f.max_length is not None:
        parts.append(f"maxLength: {f.max_length}")
    if f.min_length not in (None, 0):
        parts.append(f"minLength: {f.min_length}")
    if f.pattern:
        parts.append(f"pattern: `{f.pattern}`")
    if f.enum:
        parts.append(f"enum: {', '.join(f.enum)}")
    return "; ".join(parts)


def _field_table(fields: list[FieldSpec]) -> str:
    fields = _flatten_fields(fields)
    if not fields:
        return ""
    has_examples = any(f.example for f in fields)
    header = ["Field", "Type", "Required", "Constraints", "Description"]
    if has_examples:
        header.append("Example")
    lines = ["| " + " | ".join(header) + " |", "|" + "|".join(["---"] * len(header)) + "|"]
    for f in fields:
        row = [f.name, f.type, "Yes" if f.required else "No", _constraints_text(f), f.description or ""]
        if has_examples:
            row.append(f.example or "")
        lines.append("| " + " | ".join(c.replace("\n", " ") for c in row) + " |")
    return "\n".join(lines)


def _param_table(params: list[Parameter]) -> str:
    if not params:
        return ""
    lines = [
        "| Name | Type | Required | Description |",
        "|---|---|---|---|",
    ]
    for p in params:
        lines.append(f"| {p.name} | {p.type} | {'Yes' if p.required else 'No'} | {p.description or ''} |")
    return "\n".join(lines)


def _build_example_value(fields: list[FieldSpec]) -> tuple[dict, bool]:
    """Builds a JSON object from whatever real, backend-provided examples
    exist (recursing into FieldSpec.nested), and reports whether every field
    actually contributed one. Never invents a value for a field that has
    none -- that field is simply left out of the object, and `complete` comes
    back False so the caller can say so instead of implying full coverage."""
    obj: dict = {}
    complete = True
    for f in fields:
        if f.recursive_ref:
            complete = False
            continue
        if f.nested:
            nested_obj, nested_complete = _build_example_value(f.nested)
            if not nested_obj:
                complete = False
                continue
            obj[f.name] = [nested_obj] if f.is_array else nested_obj
            complete = complete and nested_complete
            continue
        if f.example_raw is not None:
            # The OpenAPI document's own value, untouched -- a boolean stays a
            # boolean, a number stays a number. Only the display string went
            # through str()/json.dumps().
            obj[f.name] = f.example_raw
            continue
        if not f.example:
            complete = False
            continue
        try:
            obj[f.name] = json.loads(f.example)
        except (json.JSONDecodeError, TypeError):
            obj[f.name] = f.example
    return obj, complete


def _example_json(fields: list[FieldSpec]) -> str:
    obj, complete = _build_example_value(fields)
    if not obj:
        return ""
    code = "```json\n" + json.dumps(obj, indent=2, ensure_ascii=False) + "\n```"
    if complete:
        return code
    return "_(partial — only fields with a documented example are shown)_\n\n" + code


def _request_example(body) -> str:
    """The request DTO's OWN example when it declares one (a class-level
    `@Schema(example = ...)`), otherwise a synthesis from the field examples.

    The order matters and is not a preference: a per-field synthesis can only
    ever emit ONE entry of a collection, so for any rule of the form "the set
    must also contain X" it publishes a payload the server is required to
    reject. A whole-payload example is written and owned by the DTO's author
    against the module's real rules; it is the one that must win."""
    if body.example_raw is not None:
        value = body.example_raw
        if isinstance(value, str):
            try:
                value = json.loads(value)
            except (json.JSONDecodeError, TypeError):
                return "```json\n" + value + "\n```"
        return "```json\n" + json.dumps(value, indent=2, ensure_ascii=False) + "\n```"
    return _example_json(body.fields)


def _envelope_section(title: str, envelope: ResponseEnvelope | None) -> str:
    if not envelope:
        return ""
    parts = [f"## {title}", ""]
    if envelope.schema_name:
        parts.append(f"Schema: `{envelope.schema_name}`")
        parts.append("")
    table = _field_table(envelope.fields)
    if table:
        parts.append(table)
        parts.append("")
    return "\n".join(parts)


def _raised_by(c: ErrorCode) -> str:
    if c.source_file.endswith("GlobalExceptionHandler.java"):
        return "shared handler (any endpoint)"
    if c.bound_endpoints:
        return f"{c.bound_endpoints} endpoint(s) — see their {c.bound_via or 'Business Responses'}"
    if c.throw_sites:
        return "unbound — thrown at " + ", ".join(c.throw_sites) + ", reached by no endpoint's call walk"
    return "no throw site naming it in module source"


def _field_error_semantics_section(rows: list[FieldErrorSemantics]) -> str:
    """One schema slot, several meanings: each row is the expression a
    handler passes to FieldErrorItem.field(...), quoted, with a mechanical
    reading of it. No row claims what a branch means beyond its own text."""
    if not rows:
        return ""
    lines = [
        "### What `error.fieldErrors[].field` carries",
        "",
        "Read from `GlobalExceptionHandler.java`: the expression each handler passes to "
        "`FieldErrorItem.field(...)`. The same slot is filled differently per handler, so a client "
        "must not assume it is always a request field path — a row whose fallback branch is not a "
        "field path can put something else there.",
        "",
        "| Handler | Exceptions | HTTP Status | Code | `field` holds | Source expression |",
        "|---|---|---|---|---|---|",
    ]
    for r in rows:
        lines.append(f"| {r.handler} | {', '.join(r.exceptions) or ''} | {r.http_status or ''} | "
                     f"{r.code or ''} | {r.field_meaning} | `{r.field_expression}` |")
    lines.append("")
    return "\n".join(lines)


def _unique_constraints_section(rows: list[UniqueConstraint]) -> str:
    if not rows:
        return ""
    has_note = any(r.note for r in rows)
    lines = [
        "## Uniqueness Invariants",
        "",
        "Enforced by the database on this module's own tables, read from each entity's "
        "`@Table(uniqueConstraints)` and from the Flyway migrations. Scope is the partial index's "
        "`WHERE` clause verbatim where one exists; a request that violates an invariant is refused.",
        "",
        "| Table | Entity | Columns (entity field) | Scope | Constraint | Source |" + (" Note |" if has_note else ""),
        "|---|---|---|---|---|---|" + ("---|" if has_note else ""),
    ]
    for r in rows:
        cols = ", ".join(f"{c} ({f})" if f else c for c, f in zip(r.columns, r.entity_fields or [""] * len(r.columns)))
        row = f"| {r.table} | {r.entity or ''} | {cols} | {r.scope} | {r.name} | {', '.join(r.sources)} |"
        if has_note:
            row += f" {r.note or ''} |"
        lines.append(row)
    lines.append("")
    return "\n".join(lines)


def _message_locales(codes: list[ErrorCode]) -> list[str]:
    """Locale tags that actually carry a message, base bundle ("") first. A
    locale with nothing in it gets no column — an empty column would read as
    "this API has no message for these codes"."""
    tags = {tag for c in codes for tag in c.messages}
    return ([""] if "" in tags else []) + sorted(t for t in tags if t)


def _message_header(tag: str) -> str:
    return "Message" if not tag else f"Message ({tag})"


def _cell(text: str) -> str:
    """One table cell: no newline may survive (it would end the row) and no
    bare pipe (it would start a new column)."""
    return text.replace("\n", " ").replace("|", "\\|")


def _error_codes_section(codes: list[ErrorCode], with_binding: bool) -> str:
    if not codes:
        return ""
    has_status = any(c.status or c.http_status for c in codes)
    locales = _message_locales(codes)
    has_note = any(c.note for c in codes)
    header = ["Code", "Value", "Source"]
    header += [_message_header(tag) for tag in locales]
    if has_status:
        header += ["Status", "HTTP Status"]
    if with_binding:
        header.append("Raised by")
    if has_note:
        header.append("Consumer note")
    lines = ["## Known Error Codes", ""]
    if locales:
        # The wire code doubles as the i18n message key on this platform, so
        # the text a caller actually receives for a code is knowable and is
        # published here rather than left for the integrator to trigger and
        # observe. A blank cell means that bundle has no entry for that code.
        lines += ["A code's value is also its i18n message key, so each row carries the message the API "
                  "actually answers with. `Message` is the base bundle (the fallback every locale resolves "
                  "through); a `Message (<tag>)` column is that locale's own bundle. An em dash means that "
                  "bundle has no entry for that code.", ""]
    if has_note:
        lines += ["\"Consumer note\" is the error-code constant's own Javadoc, verbatim apart from markup — "
                  "when the module's author documented what raises the code and what makes it stop being "
                  "raised, that is what it says.", ""]
    if with_binding:
        lines += ["\"Raised by\" counts the endpoints whose controller → service → domain call walk reaches a "
                  "throw site naming the code. An unbound code is thrown only from code no endpoint's walk "
                  "reaches (a cross-module entry point, a job, or a call past the walk's depth) — it is listed, "
                  "not attributed.", ""]
    lines += ["| " + " | ".join(header) + " |", "|" + "|".join(["---"] * len(header)) + "|"]
    for c in codes:
        row = [c.name, f"`{c.value}`", c.source_file]
        row += [c.messages.get(tag, "—") for tag in locales]
        if has_status:
            row += [c.status or "", c.http_status or ""]
        if with_binding:
            row.append(_raised_by(c))
        if has_note:
            row.append(c.note or "—")
        lines.append("| " + " | ".join(_cell(cell) for cell in row) + " |")
    lines.append("")
    return "\n".join(lines)


def _status_mappings_section(mappings: list[StatusMapping]) -> str:
    if not mappings:
        return ""
    lines = [
        "## Status -> HTTP Status Reference",
        "",
        "Shared, module-independent mapping every business error code's `Status` "
        "resolves through (see each error code's own Status column above, when known).",
        "",
    ]
    has_category = any(m.category for m in mappings)
    header = ["Status", "HTTP Status"] + (["Category"] if has_category else [])
    lines.append("| " + " | ".join(header) + " |")
    lines.append("|" + "|".join(["---"] * len(header)) + "|")
    for m in mappings:
        row = [m.name, m.http_status] + ([m.category or ""] if has_category else [])
        lines.append("| " + " | ".join(row) + " |")
    lines.append("")
    return "\n".join(lines)


def _pagination_constraints_section(c: PaginationConstraints | None) -> str:
    if not c:
        return ""
    rows = [
        ("Default page", c.default_page),
        ("Default size", c.default_size),
        ("Minimum size", c.min_size),
        ("Maximum size", c.max_size),
        ("Maximum page number", c.max_page_number),
    ]
    present = [(label, value) for label, value in rows if value is not None]
    if not present:
        return ""
    lines = ["## Pagination Constraints", ""]
    if c.source_file:
        lines.append(f"Source: `{c.source_file}`")
        lines.append("")
    lines += ["| Constraint | Value |", "|---|---|"]
    lines += [f"| {label} | {value} |" for label, value in present]
    lines.append("")
    return "\n".join(lines)


def _common_headers_section(headers: list[Parameter]) -> str:
    if not headers:
        return ""
    lines = ["## Common Headers", "", "Applied globally by shared middleware — not endpoint-specific.", ""]
    lines.append(_param_table(headers))
    lines.append("")
    return "\n".join(lines)


def _auth_section(ep: Endpoint) -> str:
    lines = ["**Authentication**", ""]
    if ep.requires_auth is None:
        lines.append("Not determined from the OpenAPI document.")
    elif ep.requires_auth:
        schemes = ", ".join(ep.security_schemes) if ep.security_schemes else "unspecified scheme"
        lines.append(f"Required ({schemes}).")
    else:
        lines.append("Not required.")
    source = f" (found on {ep.permission_source})" if ep.permission_source else ""
    if ep.permission:
        lines.append("")
        lines.append(f"**Required permission(s)**: {', '.join(ep.permission)}{source}")
    elif ep.permission_expression:
        # A real authorization rule that names no permission constant
        # (e.g. isAuthenticated()). Shown verbatim rather than dropped, so
        # the endpoint doesn't read as unprotected.
        lines.append("")
        lines.append(f"**Authorization rule**: `{ep.permission_expression}`{source}")
    elif ep.permission_lookup:
        lines.append("")
        lines.append(f"**Authorization**: {_permission_lookup_text(ep)}")
    lines.append("")
    return "\n".join(lines)


def _permission_lookup_text(ep: Endpoint) -> str:
    """Absent by design reads differently from could-not-extract, and each
    names what was read, so a reader can verify the claim in source."""
    checked = " and ".join(f"`{c}`" for c in ep.permission_checked)
    if ep.permission_lookup == "DECLARED_NONE":
        return f"no `@PreAuthorize`/`@Secured` declared — checked {checked}."
    if ep.permission_lookup == "CONTROLLER_NOT_MATCHED":
        return "not extracted — no controller method matching this route was found in the module source."
    if ep.permission_lookup == "DELEGATE_UNRESOLVED":
        return f"not extracted — {checked} delegates to no single resolvable service call."
    if ep.permission_lookup == "SERVICE_SOURCE_MISSING":
        return f"not extracted — the service {checked} delegates to has no source file under the module."
    if ep.permission_lookup == "SERVICE_METHOD_MISSING":
        return f"not extracted — the delegate method was not found; checked {checked}."
    return f"not extracted ({ep.permission_lookup})."


def _endpoint_markdown(ep: Endpoint) -> str:
    """Renders one endpoint as a single '## {METHOD} {path}' section — that
    heading is the stable key sync.py splits group files on (see
    sync._split_sections / sync.compare), so every subsection below it uses
    '###' or lower. Never emit a bare '## ' line anywhere else in this
    function — sync.py would mistake it for a second endpoint boundary."""
    parts = [f"## {ep.method} {ep.path}", ""]
    if ep.summary:
        parts.append(f"**{ep.summary}**")
        parts.append("")
    if ep.description:
        parts.append(ep.description)
        parts.append("")
    if ep.api_id:
        # The id every other governance artifact names this endpoint by. It
        # is printed first, and before the operation id, because resolving
        # "implement API-SEC-004" against a planning document instead of this
        # file is what produces a call to a path the backend never served.
        parts.append(f"Contract ID: `{ep.api_id}`")
        parts.append("")
    if ep.operation_id:
        parts.append(f"Operation ID: `{ep.operation_id}`")
        parts.append("")

    parts.append(_auth_section(ep))

    path_table = _param_table(ep.path_params)
    if path_table:
        parts += ["### Path Parameters", "", path_table, ""]

    query_table = _param_table(ep.query_params)
    if query_table:
        parts += ["### Query Parameters", "", query_table, ""]

    header_table = _param_table(ep.header_params)
    if header_table:
        parts += ["### Headers", "", header_table, ""]

    if ep.request_body:
        parts.append("### Request Body")
        parts.append("")
        if ep.request_body.schema_name:
            parts.append(f"Schema: `{ep.request_body.schema_name}` ({ep.request_body.content_type})")
            parts.append("")
        table = _field_table(ep.request_body.fields)
        if table:
            parts.append(table)
            parts.append("")
        example = _request_example(ep.request_body)
        if example:
            parts.append("**Request Example**")
            parts.append("")
            parts.append(example)
            parts.append("")

    for resp in ep.responses:
        parts.append(f"### Response `{resp.status_code}`" + (f" — {resp.description}" if resp.description else ""))
        parts.append("")
        if resp.status_source:
            # springdoc declares 200 for an operation with no @ApiResponse, so
            # a status that differs from it was read from source; say where.
            parts.append(f"Status read from source: {resp.status_source}.")
            parts.append("")
        if resp.schema_name:
            shape = resp.schema_name
            if resp.is_array:
                shape = f"array of {shape}"
            elif resp.is_page:
                shape = f"paginated list of {shape} (see Pagination Envelope in index.md)"
            parts.append(f"Shape: `{shape}`")
            parts.append("")
        table = _field_table(resp.fields)
        if table:
            parts.append(table)
            parts.append("")
        example = _example_json(resp.fields)
        if example:
            parts.append("**Response Example**")
            parts.append("")
            parts.append(example)
            parts.append("")

    parts += _business_responses(ep)

    if ep.possible_errors:
        parts.append("### Other Possible Responses")
        parts.append("")
        parts.append("Structurally guaranteed by this endpoint's own shape (auth requirement, "
                      "permission check, request body) combined with the shared framework's "
                      "exception handling — not specific business errors.")
        parts.append("")
        parts.append("| HTTP Status | Code | Why |")
        parts.append("|---|---|---|")
        for perr in ep.possible_errors:
            parts.append(f"| {perr.http_status} | {perr.code} | {perr.reason} |")
        parts.append("")

    return "\n".join(parts).rstrip() + "\n"


def _business_responses(ep: Endpoint) -> list[str]:
    """Rows are throw sites the controller's call walk reached, each cited.
    An empty walk says what it walked, so 'none' never masquerades as 'not looked'."""
    walk = ep.business_walk
    if walk is None:
        return []
    lines = ["### Business Responses", ""]
    walked = ", ".join(f"`{v}`" for v in walk.visited)
    if not ep.business_errors:
        if len(walk.visited) <= 1:
            lines.append(f"Not determined — the call walk did not resolve a delegate beyond {walked}.")
        else:
            lines.append(f"None reached — walked {walked} and found no throw site naming a module error code.")
    else:
        lines.append(f"Raised by this endpoint's own rules. Each row cites the throw site it was read from "
                     f"(walked {walked}).")
        lines.append("")
        lines.append("| HTTP Status | Code | Constant | Raised at |")
        lines.append("|---|---|---|---|")
        for be in ep.business_errors:
            where = be.throw_site + (" — as a `fieldErrors[]` entry" if be.kind == "detail" else "")
            http = be.http_status or (be.status or "not determined")
            lines.append(f"| {http} | `{be.value}` | {be.code} | {where} |")
    notes = []
    if walk.unresolved:
        notes.append("skipped as ambiguous: " + ", ".join(f"`{u}`" for u in walk.unresolved))
    if walk.depth_limit_hit:
        notes.append("calls beyond the walk's depth limit were not followed")
    if notes:
        lines.append("")
        lines.append("Not listed — " + "; ".join(notes) + ".")
    lines.append("")
    return lines


def _catalog_table(endpoints: list[Endpoint], group: str, group_file: str, with_api_ids: bool) -> str:
    """The API column is the lookup that was missing: a consumer holding a
    contract id can find the served path here instead of in a planning
    document. The column is omitted entirely when no endpoint has an id --
    an empty column would read as "these endpoints have no contract"."""
    if with_api_ids:
        lines = ["| API | Method | Path | Summary | Doc |", "|---|---|---|---|---|"]
    else:
        lines = ["| Method | Path | Summary | Doc |", "|---|---|---|---|"]
    for ep in endpoints:
        if (ep.group or "Ungrouped") != group:
            continue
        link = f"{group_file}#{_anchor_slug(ep.method, ep.path)}"
        row = f"| {ep.method} | `{ep.path}` | {ep.summary or ''} | [{ep.slug()}]({link}) |"
        lines.append(f"| {ep.api_id or '—'} {row}" if with_api_ids else row)
    return "\n".join(lines)


def _group_markdown(group: str, endpoints: list[Endpoint]) -> str:
    """One file per group (= controller, since each controller declares one
    @Tag). Every endpoint becomes a '## {METHOD} {path}' section within this
    single H1 — see _endpoint_markdown's docstring for why nothing inside an
    endpoint's own markdown may use '## '. Endpoints are emitted in the order
    `document.endpoints` provides them, which openapi_extractor sorts."""
    parts = [f"# {group}", ""]
    if len(endpoints) > 1:
        parts.append("**Endpoints in this file:**")
        parts.append("")
        for ep in endpoints:
            tag = f"`{ep.api_id}` — " if ep.api_id else ""
            parts.append(f"- {tag}[{ep.method} {ep.path}](#{_anchor_slug(ep.method, ep.path)})")
        parts.append("")
    for ep in endpoints:
        parts.append(_endpoint_markdown(ep))
    return "\n".join(parts).rstrip() + "\n"


def _contract_section(doc: ApiDocument) -> Optional[str]:
    """States where the contract ids came from and, when the declared and the
    served contract disagree, lists every mismatch. Drift is published rather
    than only logged: an id that resolves to nothing served is the one fact a
    consumer cannot discover from anywhere else, and is exactly what sends
    them back to a stale planning document for a path."""
    if not doc.contract_source:
        return None
    stamped = [ep.api_id for ep in doc.endpoints if ep.api_id]
    # The illustrative id is taken from this document's own first stamped
    # endpoint, never written as a literal: a literal would print one module's
    # id (API-SEC-004) into every other module's index, which is the same
    # class of stale cross-module reference this whole section exists to stop.
    source_name = "API document" if doc.contract_kind == "api-spec" else "API REGISTRY"
    example = f"`{stamped[0]}`, ..." if stamped else f"an {source_name} id"
    lines = [
        "## Contract Traceability",
        "",
        f"Contract ids joined from `{doc.contract_source}` ({source_name}): "
        f"**{len(stamped)} of {len(doc.endpoints)}** served endpoints carry one.",
        "",
        f"Resolve a contract id ({example}) to a path **here** — the API column of the "
        "catalog below, and the `Contract ID` line of each endpoint. A planning document states "
        "the path that was proposed, not the one that is served.",
        "",
    ]
    if doc.contract_drift:
        lines += [
            "### Drift — declared vs served",
            "",
            "| Kind | API | Method | Path | What this means |",
            "|---|---|---|---|---|",
        ]
        for d in doc.contract_drift:
            api = d.api_id or "—"
            detail = d.detail
            if d.claimed_ids:
                ids = ", ".join(f"`{i}`" for i in d.claimed_ids)
                verb = "source claims" if len(d.claimed_ids) == 1 else "source mentions"
                api = f"— ({verb} {ids}, {d.claim_source}" + (", ambiguous)" if len(d.claimed_ids) > 1 else ")")
                detail += f" — the id quoted is a source comment, not a registered contract; registering it is the {source_name}'s to do"
            lines.append(f"| {d.kind} | {api} | {d.method} | `{d.path}` | {detail} |")
        lines.append("")
    return "\n".join(lines)


def _index_markdown(doc: ApiDocument) -> str:
    group_files = group_file_paths(doc.groups())
    parts = [f"# {doc.module} API Documentation", ""]
    if doc.title:
        parts.append(f"_{doc.title}_")
        parts.append("")
    if doc.description:
        parts.append(doc.description)
        parts.append("")
    if doc.version:
        parts.append(f"API version: `{doc.version}`")
        parts.append("")

    if doc.servers:
        parts.append("## Servers")
        parts.append("")
        for s in doc.servers:
            parts.append(f"- {s}")
        parts.append("")

    if doc.security_schemes:
        parts.append("## Authentication")
        parts.append("")
        for s in doc.security_schemes:
            desc = f" — {s.description}" if s.description else ""
            kind = s.scheme or s.type or ""
            fmt = f" ({s.bearer_format})" if s.bearer_format else ""
            parts.append(f"- **{s.name}**: {kind}{fmt}{desc}")
        parts.append("")

    parts.append(_common_headers_section(doc.common_headers))
    parts.append(_envelope_section("Common Response Envelope", doc.response_envelope))
    parts.append(_field_error_semantics_section(doc.field_error_semantics))
    parts.append(_envelope_section("Pagination Envelope", doc.pagination_envelope))
    parts.append(_pagination_constraints_section(doc.pagination_constraints))
    parts.append(_unique_constraints_section(doc.unique_constraints))
    parts.append(_error_codes_section(doc.error_codes, with_binding=any(ep.business_walk for ep in doc.endpoints)))
    parts.append(_status_mappings_section(doc.status_mappings))

    parts.append(_contract_section(doc))

    with_api_ids = any(ep.api_id for ep in doc.endpoints)
    parts.append("## API Catalog")
    parts.append("")
    for group in doc.groups():
        parts.append(f"### {group}")
        parts.append("")
        parts.append(_catalog_table(doc.endpoints, group, group_files[group], with_api_ids))
        parts.append("")

    return "\n".join(p for p in parts if p is not None).strip() + "\n"


class MarkdownRenderer(Renderer):

    def render(self, document: ApiDocument) -> dict[str, str]:
        files: dict[str, str] = {"index.md": _index_markdown(document)}
        group_files = group_file_paths(document.groups())
        for group, group_file in group_files.items():
            group_endpoints = [ep for ep in document.endpoints
                                if (ep.group or "Ungrouped") == group]
            files[group_file] = _group_markdown(group, group_endpoints)
        return files
