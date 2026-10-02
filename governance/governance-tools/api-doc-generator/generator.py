"""
generator.py — the API documentation pipeline: extract -> render -> sync.
════════════════════════════════════════════════════════════════════
Generates frontend-ready API documentation directly from an implemented
Spring Boot module. The ONLY source of truth is the compiled application's
OpenAPI document (springdoc-openapi) plus, optionally, its Java source (the
module's own, and any shared/common modules it depends on) for sections that
have no OpenAPI representation at all (permissions, error codes, error/HTTP
status mapping, globally-applied headers).

This module has no CLI of its own — it's imported by generate.py, which is
responsible for turning "--module ORG --function update" into a
discovery.RepositoryContext (via discovery.py's repository auto-discovery,
with explicit overrides only where discovery genuinely can't be
authoritative).

build_document() is the ONLY place a RepositoryContext gets unpacked. Every
individual extractor below still takes its own narrow, explicit input (a
Path, a dict, ...), never the context object itself — so no extractor module
depends on discovery.py, or needs to know it's running inside a "resolved
repository" at all. That keeps repository knowledge confined to one
boundary, exactly as it was before the context object existed; the context
just replaces what used to be several loose, independently-threaded
parameters (module, openapi_source, source_root, common_source_roots) with
one object discovery.py hands over as a unit.

Design principle, unchanged: every extractor either finds real metadata or
leaves the field empty. The renderer never prints a placeholder for missing
data — it omits the subsection. Nothing here is ever invented.
"""

import checks
import sync
from discovery import RepositoryContext
from extractors import (
    business_error_extractor,
    common_headers_extractor,
    constraint_extractor,
    contract_extractor,
    dto_extractor,
    error_mapping_extractor,
    exception_extractor,
    message_bundle_extractor,
    openapi_extractor,
    pagination_extractor,
    response_model_extractor,
    response_status_extractor,
    security_extractor,
)
from models.api_doc_model import ResponseEnvelope
from renderers.markdown_renderer import MarkdownRenderer


def build_document(context: RepositoryContext):
    openapi = openapi_extractor.load_openapi(context.openapi_source)

    document = openapi_extractor.build_document(openapi, context.module)
    conventions = context.conventions or {}
    code_classes = tuple(conventions.get("error_code_classes") or exception_extractor.DEFAULT_CODE_CLASSES)
    business_error_extractor.configure_code_classes(code_classes)
    if conventions.get("authentication") == "none" and not document.security_schemes:
        # aias adaptation: the application declares no caller authentication
        # (conventions.json), and the OpenAPI document no security requirement,
        # so "none required" is a fact here, not an undetermined one.
        for ep in document.endpoints:
            if ep.requires_auth is None:
                ep.requires_auth = False
    document.response_envelope = response_model_extractor.find_envelope(openapi)

    page_schema_name, page_fields = dto_extractor.find_page_envelope(openapi)
    if page_schema_name:
        document.pagination_envelope = ResponseEnvelope(schema_name=page_schema_name, fields=page_fields)

    source_root = context.source_root
    controller_of: dict[int, tuple[str, str]] = {}
    classes: dict = {}
    if source_root is not None:
        document.error_codes = exception_extractor.find_error_codes(source_root, code_classes)
        document.source_facts["auth_annotations"] = security_extractor.count_authorization_annotations(source_root)
        classes = business_error_extractor.index_module_source(source_root)
        document.access_denied_overrides = business_error_extractor.find_access_denied_overrides(classes)

        for ep in document.endpoints:
            controller_file, method_name = security_extractor.find_controller_for_endpoint(source_root, ep.method, ep.path)
            if not controller_file or not method_name:
                ep.permission_lookup = security_extractor.CONTROLLER_NOT_MATCHED
                continue
            controller_of[id(ep)] = (controller_file.stem, method_name)
            controller_source = controller_file.read_text(encoding="utf-8")
            lookup = security_extractor.resolve_permission(
                controller_source, method_name, source_root, controller_name=controller_file.stem
            )
            ep.permission = lookup.constants
            ep.permission_source = lookup.source_label
            ep.permission_expression = lookup.expression
            ep.permission_lookup = lookup.outcome
            ep.permission_checked = lookup.checked

    status_http: dict[str, str] = {}
    if context.common_source_roots:
        status_http = error_mapping_extractor.find_status_http_mapping(context.common_source_roots)
        document.error_codes, document.status_mappings = error_mapping_extractor.enrich_error_codes(
            document.error_codes, source_root, context.common_source_roots,
            code_status_pattern=conventions.get("code_carries_http_status"),
        )
        document.common_headers = common_headers_extractor.find_common_headers(context.common_source_roots)
        document.auth_entry_point = error_mapping_extractor.find_auth_entry_point(context.common_source_roots)
        error_mapping_extractor.attach_endpoint_error_codes(
            document, status_http,
            service_package_of=lambda name: classes[name].package if name in classes else None,
        )
        document.pagination_constraints = pagination_extractor.find_pagination_constraints(
            source_root, context.common_source_roots
        )
        document.field_error_semantics = error_mapping_extractor.find_field_error_semantics(context.common_source_roots)

    if context.message_bundles:
        # The message a caller actually receives for each code, keyed by the
        # code's own value. Runs after enrich_error_codes so the framework-level
        # codes it appends get their messages too.
        bundles = message_bundle_extractor.load_bundles(context.message_bundles,
                                                        context.message_basename or "messages")
        message_bundle_extractor.attach_messages(document.error_codes, bundles)

    if source_root is not None:
        business_error_extractor.attach_business_errors(document, classes, controller_of, status_http)
        response_status_extractor.attach_response_statuses(
            document, classes, controller_of, status_http, context.common_source_roots,
            error_mapping_extractor.http_status_label)
        document.unique_constraints = constraint_extractor.find_unique_constraints(source_root, context.migration_roots)
        document.source_facts["unique_declared"] = sum(
            c.raw.count("@UniqueConstraint") for c in classes.values() if "@Entity" in c.raw)

    # Last: the contract-id join. It needs the full endpoint list, and it is
    # what makes the generated docs addressable by the ids every other
    # governance artifact (SRS, frontend plan, test plan) is written in.
    # The module's API document is the contract; the prose API REGISTRY of the
    # plan is read only for a pre-v7 module that has no API document.
    if context.api_spec is not None:
        entries = contract_extractor.load_api_spec(context.api_spec)
        contract_extractor.attach_contract_ids(document, entries, context.api_spec.name, source_root,
                                               kind="api-spec")
    elif context.execution_plan is not None:
        entries = contract_extractor.load_api_registry(context.execution_plan)
        contract_extractor.attach_contract_ids(document, entries, context.execution_plan.name, source_root,
                                               kind="api-registry")

    return document


def _contract_line(document) -> str:
    """Contract-id coverage is reported on every run, in every mode: an
    unstamped endpoint or a drifting registry id is precisely what makes a
    consumer fall back to a planning document for a path."""
    if not document.contract_source:
        return "Contract ids: none — no API document and no API REGISTRY found for this module"
    stamped = sum(1 for ep in document.endpoints if ep.api_id)
    drift = len(document.contract_drift)
    suffix = f", {drift} DRIFT (see index.md)" if drift else ""
    fallback = " (FALLBACK: pre-v7 prose API REGISTRY)" if document.contract_kind == "api-registry" else ""
    return f"Contract ids: {stamped}/{len(document.endpoints)} from {document.contract_source}{fallback}{suffix}"


def run(context: RepositoryContext, mode: str) -> str:
    """Runs the full pipeline and returns the human-readable report text.
    Raises on load failure — callers decide how to surface it (CLI exit code,
    etc.)."""
    document = build_document(context)
    files = MarkdownRenderer().render(document)

    lines = [f"Module      : {context.module}"]

    if mode == "generate":
        sync.write_all(context.output, files)
        lines.append("Mode        : Generate")
        lines.append(f"Endpoints   : {len(document.endpoints)}")
        lines.append(f"Groups      : {len(document.groups())}")
        lines.append(f"Error codes : {len(document.error_codes)}")
        lines.append(_contract_line(document))
        lines.append(f"Output      : {context.output}")
        return "\n".join(lines)

    lines.append(_contract_line(document))

    existing = sync.read_existing(context.output)
    report = sync.compare(existing, files, document.endpoints, mode=mode)

    if mode == "update":
        written, deleted = sync.apply(context.output, existing, files, report)
        lines.append(sync.format_report(report))
        lines.append(f"Files written: {len(written)}, deleted: {len(deleted)}")
        lines.append(f"Output      : {context.output}")
    else:  # review — no filesystem writes
        lines.append(sync.format_report(report))

    return "\n".join(lines)


def check(context: RepositoryContext) -> tuple[str, int]:
    """review's report, then every assertion in checks.py; exit status 2 on
    any FAIL so a pipeline stops on drift, staleness, non-determinism or a
    silently empty section instead of printing a count nobody reads."""
    document = build_document(context)
    files = MarkdownRenderer().render(document)
    files_again = MarkdownRenderer().render(build_document(context))
    existing = sync.read_existing(context.output)
    report = sync.compare(existing, files, document.endpoints, mode="check")
    results = checks.run_checks(context, document, files, files_again, report, existing)
    lines = [f"Module      : {context.module}", _contract_line(document), sync.format_report(report), ""]
    lines.append(checks.format_checks(context.module, results, len(document.endpoints), context.output))
    return "\n".join(lines), (2 if checks.failed(results) else 0)
