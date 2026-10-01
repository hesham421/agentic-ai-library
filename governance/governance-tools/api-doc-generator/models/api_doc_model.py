"""
Data model for the API documentation generator.

Every field below is Optional (or an empty list/dict by default). Absence of
a value means "not discoverable from the implemented backend" — extractors
must never fabricate a value to fill a gap, and renderers must skip a
subsection entirely rather than print a placeholder for missing data.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Optional


@dataclass
class FieldSpec:
    name: str
    type: str
    required: bool = False
    description: Optional[str] = None
    example: Optional[str] = None
    # The example exactly as it appeared in the OpenAPI document (bool/int/
    # dict/... -- NOT stringified), so renderers can emit a real JSON literal
    # instead of Python's str() of it ("True" is not valid JSON). `example`
    # above stays the display string used in documentation tables.
    example_raw: Optional[Any] = None
    min_length: Optional[int] = None
    max_length: Optional[int] = None
    pattern: Optional[str] = None
    format: Optional[str] = None
    enum: list[str] = field(default_factory=list)
    is_array: bool = False
    item_type: Optional[str] = None
    # Populated when this field's own type is an object (or array-of-object)
    # schema with properties of its own — the field's row still renders as
    # before, but renderers may also expand this into a nested sub-table.
    nested: list["FieldSpec"] = field(default_factory=list)
    # Set instead of `nested` when the referenced schema is already being
    # expanded higher up the same chain (self-referential / recursive
    # schemas, e.g. a tree node's own child list) — names the schema so the
    # renderer can say "same shape as X" instead of expanding forever.
    recursive_ref: Optional[str] = None
    # The description of the schema this field's own type refers to (the
    # nested DTO's class-level @Schema(description = ...)). A DTO author's
    # note about the whole nested object -- "the field examples below describe
    # a single entry; the complete payload is on X" -- lives there and nowhere
    # else, and expanding the nested fields into flat rows used to drop it.
    nested_schema_description: Optional[str] = None


@dataclass
class Parameter:
    name: str
    location: str  # "path" | "query" | "header"
    type: str = "string"
    required: bool = False
    description: Optional[str] = None
    example: Optional[str] = None


@dataclass
class RequestBody:
    content_type: str = "application/json"
    schema_name: Optional[str] = None
    fields: list[FieldSpec] = field(default_factory=list)
    # The schema's OWN example -- a class-level @Schema(example = ...) on the
    # request DTO, which springdoc emits as the schema's "example". It is the
    # one payload the DTO's author wrote as a whole and validated against the
    # module's own rules, so it outranks an example synthesised field by field
    # (a per-field synthesis can only ever produce one entry of a collection,
    # which for a rule like "exactly one remainder target" is a payload the
    # server must reject). Absent = no class-level example was declared.
    example_raw: Optional[Any] = None
    # The schema's own description (class-level @Schema(description = ...)),
    # kept so a warning the DTO author wrote for the whole payload is not lost
    # when only field rows are rendered.
    description: Optional[str] = None


@dataclass
class ResponseVariant:
    status_code: str
    description: Optional[str] = None
    content_type: Optional[str] = None
    schema_name: Optional[str] = None
    fields: list[FieldSpec] = field(default_factory=list)
    is_array: bool = False
    is_page: bool = False
    # Set only when status_code was resolved from the service's own
    # Status -> HTTP mapping rather than taken from the OpenAPI document.
    # springdoc declares 200 for every operation that carries no explicit
    # @ApiResponse, so a service returning Status.CREATED publishes as 200
    # unless the real status is read from source. Names the constant and the
    # method it was read at, so the claim is checkable.
    status_source: Optional[str] = None


@dataclass
class Endpoint:
    method: str
    path: str
    group: Optional[str] = None          # OpenAPI tag, used to bucket endpoints
    operation_id: Optional[str] = None
    summary: Optional[str] = None
    description: Optional[str] = None
    path_params: list[Parameter] = field(default_factory=list)
    query_params: list[Parameter] = field(default_factory=list)
    header_params: list[Parameter] = field(default_factory=list)
    request_body: Optional[RequestBody] = None
    responses: list[ResponseVariant] = field(default_factory=list)
    requires_auth: Optional[bool] = None      # None = undetermined, else True/False
    security_schemes: list[str] = field(default_factory=list)
    # Populated only by security_extractor.py, only when --source is given.
    permission: list[str] = field(default_factory=list)
    permission_source: Optional[str] = None   # "controller" | "service:<ClassName>"
    # The raw @PreAuthorize/@Secured SpEL expression, kept verbatim when it
    # carries no SecurityPermissions-style constant to extract (e.g.
    # "isAuthenticated()", "hasRole('ADMIN')"). Never evaluated -- it is
    # rendered as-is so an endpoint with a real, non-constant authorization
    # rule stops looking unprotected in the docs.
    permission_expression: Optional[str] = None
    # Populated only by contract_extractor.attach_contract_ids(), only when
    # the module's backend execution plan (its API REGISTRY) was discovered.
    # This is the id — API-SEC-004, ... — every other governance artifact
    # names this endpoint by; without it the published docs are unjoinable to
    # the rest of the chain. Absent = this endpoint is in no API REGISTRY.
    api_id: Optional[str] = None
    # Populated only by error_mapping_extractor.attach_endpoint_error_codes(),
    # only when common source roots are available -- see PossibleError.
    possible_errors: list[PossibleError] = field(default_factory=list)
    # How the permission lookup ended (security_extractor outcome constants) and
    # every Class.method it read. Absent = no module source was available.
    permission_lookup: Optional[str] = None
    permission_checked: list[str] = field(default_factory=list)
    # Populated only by business_error_extractor: the module's own error codes
    # reachable from this endpoint's controller method, each with its throw site.
    business_errors: list[BusinessError] = field(default_factory=list)
    business_walk: Optional[BusinessErrorWalk] = None

    def slug(self) -> str:
        if self.operation_id:
            return self.operation_id
        cleaned = self.path.strip("/").replace("/", "-").replace("{", "").replace("}", "")
        return f"{self.method.lower()}-{cleaned}" if cleaned else self.method.lower()


@dataclass
class ContractEntry:
    """One row of a module's API REGISTRY (backend execution plan) — the
    declared contract, as opposed to what is actually served."""
    api_id: str
    method: str
    path: str
    operation: Optional[str] = None


@dataclass
class ContractDrift:
    """A declared-vs-served mismatch. kind is "unimplemented" (a registry id
    with no served endpoint — the id a consumer would resolve to a dead path)
    or "undeclared" (a served endpoint no registry id names)."""
    kind: str
    method: str
    path: str
    detail: str
    api_id: Optional[str] = None
    # For an undeclared endpoint: contract ids its controller's own Javadoc
    # names that no registry row carries. A signpost quoted from source, never
    # a registration -- see contract_extractor.claimed_ids.
    claimed_ids: list[str] = field(default_factory=list)
    claim_source: Optional[str] = None


@dataclass
class PossibleError:
    """One framework-level error this specific endpoint can structurally
    produce, attached only when both (a) a real per-endpoint fact (requires
    auth / has a found permission check / accepts a request body) and (b) a
    real, already-extracted framework error-code+HTTP-status pairing exist --
    never attached by convention or guesswork. See
    error_mapping_extractor.attach_endpoint_error_codes."""
    code: str
    http_status: Optional[str]
    reason: str


@dataclass
class ErrorCode:
    name: str
    value: str
    source_file: str
    # Populated only by error_mapping_extractor.py, only when a shared
    # common-source root was discovered/given. status is the business Status
    # enum constant found at this code's throw site (e.g. "CONFLICT");
    # http_status is that Status resolved through the shared Status->HttpStatus
    # table (e.g. "409 CONFLICT"). Either may be absent independently.
    status: Optional[str] = None
    http_status: Optional[str] = None
    # Populated only by business_error_extractor: every Class.method in the
    # module's source that names this code at a throw site, and how many
    # endpoints' walks reached one of them. 0 with sites = unbound.
    throw_sites: list[str] = field(default_factory=list)
    bound_endpoints: int = 0
    bound_via: Optional[str] = None   # where the binding is rendered when not in Business Responses
    # Populated only by message_bundle_extractor: {locale tag -> message}, read
    # from the application's own i18n bundles keyed by this code's VALUE (the
    # wire code doubles as the message key on this platform). "" is the base
    # bundle (the fallback every locale resolves through); "ar", "fr", ... are
    # its locale variants. A code with no bundle entry keeps an empty dict --
    # never a placeholder, never an invented message.
    messages: dict[str, str] = field(default_factory=dict)
    # The constant's own Javadoc, flattened to one paragraph: what the module's
    # author wrote about when this code is raised and when it stops being
    # raised. Absent = the constant carries no Javadoc.
    note: Optional[str] = None


@dataclass
class BusinessError:
    """One module error code reachable from an endpoint, read at a real throw
    site. kind is "thrown" (the LocalizedException's own code) or "detail" (a
    code carried in fieldErrors[] of a multi-error throw); status is None when
    a detail's throwing Status was not in the same method body."""
    code: str
    value: str
    throw_site: str
    kind: str = "thrown"
    status: Optional[str] = None
    http_status: Optional[str] = None


@dataclass
class BusinessErrorWalk:
    """What one endpoint's call walk covered, so an empty result is
    distinguishable from a walk that never left the controller."""
    visited: list[str] = field(default_factory=list)
    unresolved: list[str] = field(default_factory=list)
    depth_limit_hit: bool = False


@dataclass
class AccessDeniedOverride:
    """A module advisor that catches AccessDeniedException and re-raises a
    LocalizedException, so a 403 from its scope carries the module's own code
    rather than the shared handler's."""
    advisor: str
    status: str
    code: str
    value: str
    scope_prefix: Optional[str] = None


@dataclass
class AuthEntryPoint:
    """The AuthenticationEntryPoint SecurityConfig wires, and the error code
    its commence(...) writes: the 401 body an unauthenticated call receives,
    which never passes through GlobalExceptionHandler."""
    handler: str
    code: str
    value: str
    config: str


@dataclass
class FieldErrorSemantics:
    """One GlobalExceptionHandler method's contribution to fieldErrors[].field:
    the expression it passes to .field(...), verbatim, and a mechanical reading of it."""
    handler: str
    exceptions: list[str]
    field_expression: str
    field_meaning: str
    http_status: Optional[str] = None
    code: Optional[str] = None


@dataclass
class UniqueConstraint:
    """A uniqueness invariant the database enforces, read from the entity's
    @Table(uniqueConstraints) and/or the Flyway migrations. scope is "all rows"
    or the partial index's WHERE clause verbatim."""
    table: str
    name: str
    columns: list[str]
    scope: str = "all rows"
    entity: Optional[str] = None
    entity_fields: list[str] = field(default_factory=list)
    sources: list[str] = field(default_factory=list)
    note: Optional[str] = None


@dataclass
class StatusMapping:
    """One row of the shared, module-independent Status/exception -> HTTP
    status table (see error_mapping_extractor.py). Not tied to any module's
    own error codes — this is the platform-wide framework table."""
    name: str
    http_status: str
    category: Optional[str] = None
    source_file: Optional[str] = None


@dataclass
class PaginationConstraints:
    """Request-side pagination limits/defaults -- distinct from
    ResponseEnvelope (which describes the shape of a paginated *response*).
    Populated only by pagination_extractor.py, only when discoverable and
    unambiguously attributable to the module being documented."""
    default_page: Optional[int] = None
    default_size: Optional[int] = None
    min_size: Optional[int] = None
    max_size: Optional[int] = None
    max_page_number: Optional[int] = None
    source_file: Optional[str] = None


@dataclass
class ResponseEnvelope:
    schema_name: str
    fields: list[FieldSpec] = field(default_factory=list)


@dataclass
class SecurityScheme:
    name: str
    type: Optional[str] = None
    scheme: Optional[str] = None
    bearer_format: Optional[str] = None
    description: Optional[str] = None


@dataclass
class ApiDocument:
    module: str
    title: Optional[str] = None
    description: Optional[str] = None
    version: Optional[str] = None
    servers: list[str] = field(default_factory=list)
    security_schemes: list[SecurityScheme] = field(default_factory=list)
    endpoints: list[Endpoint] = field(default_factory=list)
    error_codes: list[ErrorCode] = field(default_factory=list)
    response_envelope: Optional[ResponseEnvelope] = None
    pagination_envelope: Optional[ResponseEnvelope] = None
    pagination_constraints: Optional[PaginationConstraints] = None
    # Populated only by error_mapping_extractor.py, only when a shared
    # common-source root was discovered/given (see 3.6 in the enhancement report).
    status_mappings: list[StatusMapping] = field(default_factory=list)
    # Populated only by common_headers_extractor.py, only when a shared
    # common-source root was discovered/given (see 3.12).
    common_headers: list[Parameter] = field(default_factory=list)
    # Populated only by contract_extractor.py, only when the module's backend
    # execution plan was discovered. contract_source names the plan file the
    # ids were joined from; contract_drift is every declared-vs-served
    # mismatch found while joining.
    contract_source: Optional[str] = None
    # "api-spec" (the module's OpenAPI API document, factory schema 7) or
    # "api-registry" (the pre-v7 prose API REGISTRY fallback).
    contract_kind: Optional[str] = None
    contract_drift: list[ContractDrift] = field(default_factory=list)
    # Populated only by error_mapping_extractor.find_field_error_semantics().
    field_error_semantics: list[FieldErrorSemantics] = field(default_factory=list)
    # Populated only by business_error_extractor, module source required.
    access_denied_overrides: list[AccessDeniedOverride] = field(default_factory=list)
    # Populated only by error_mapping_extractor.find_auth_entry_point(), shared source required.
    auth_entry_point: Optional[AuthEntryPoint] = None
    # Populated only by constraint_extractor, module source and migrations required.
    unique_constraints: list[UniqueConstraint] = field(default_factory=list)
    # Expected-side counts read from source for the check mode's ratios
    # (annotations, throw sites, ...). Never rendered.
    source_facts: dict[str, int] = field(default_factory=dict)

    def groups(self) -> list[str]:
        seen: list[str] = []
        for ep in self.endpoints:
            g = ep.group or "Ungrouped"
            if g not in seen:
                seen.append(g)
        return seen
