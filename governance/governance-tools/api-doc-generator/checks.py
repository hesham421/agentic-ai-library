"""check mode: everything review reports plus assertions that each carry an expected side read from source, printed as ratios, failing the run; a waiver is explicit, per module, in waivers/<MOD>.json, with a reason."""

import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

from extractors import contract_extractor, security_extractor
from models.api_doc_model import ApiDocument

WAIVERS_DIR = Path(__file__).resolve().parent / "waivers"
CHECK_NAMES = (
    "deterministic", "stale", "contract-drift", "contract-ids", "permissions", "auth-determined",
    "envelope", "error-codes", "status-table", "business-errors", "field-error-semantics", "unique-constraints",
)
PASS, FAIL, WAIVED, NA, INFO = "PASS", "FAIL", "WAIVED", "N/A", "INFO"


@dataclass
class CheckResult:
    name: str
    verdict: str
    detail: str
    waiver: Optional[str] = None


@dataclass
class Waiver:
    check: str
    reason: str
    by: str
    on: str


def load_waivers(module: str) -> tuple[dict[str, Waiver], list[str]]:
    """Valid waivers by check name, and every problem with the file. An
    invalid waiver (unknown check, missing reason/by/on) waives nothing and is
    reported, so a typo cannot silently switch an assertion off."""
    path = WAIVERS_DIR / f"{module}.json"
    if not path.is_file():
        return {}, []
    problems: list[str] = []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return {}, [f"{path.name}: not valid JSON ({exc})"]
    waivers: dict[str, Waiver] = {}
    for i, item in enumerate(data.get("waivers", [])):
        check = item.get("check")
        missing = [k for k in ("reason", "by", "on") if not str(item.get(k, "")).strip()]
        if check not in CHECK_NAMES:
            problems.append(f"{path.name} waiver #{i + 1}: unknown check {check!r}")
        elif missing:
            problems.append(f"{path.name} waiver #{i + 1} ({check}): missing {', '.join(missing)}")
        else:
            waivers[check] = Waiver(check, item["reason"].strip(), str(item["by"]).strip(), str(item["on"]).strip())
    return waivers, problems


def _source_files_matching(roots: list[Path], pattern: str) -> int:
    rx = re.compile(pattern)
    count = 0
    for root in roots:
        for path in sorted(root.rglob("*.java")):
            if rx.search(path.read_text(encoding="utf-8", errors="ignore")):
                count += 1
    return count


def _ratio(part: int, whole: int, noun: str) -> str:
    return f"{part}/{whole} {noun}"


def run_checks(context, document: ApiDocument, files: dict[str, str], files_again: dict[str, str],
               report, existing: dict[str, str]) -> list[CheckResult]:
    results: list[CheckResult] = []
    endpoints = document.endpoints
    n = len(endpoints)
    source_root = context.source_root
    common = list(context.common_source_roots)
    facts = document.source_facts

    differing = sorted(p for p in set(files) | set(files_again) if files.get(p) != files_again.get(p))
    results.append(CheckResult("deterministic", FAIL if differing else PASS,
                               f"{len(differing)} file(s) differ between two renders: {', '.join(differing)}" if differing
                               else f"two renders identical ({len(files)} files)"))

    if not existing:
        results.append(CheckResult("stale", FAIL, f"no docs on disk at {context.output} — run --function generate"))
    else:
        changed_eps = [c for c in report.endpoint_changes if c.status != "unchanged"]
        changed_shared = [s for s in report.shared_changes if s.status != "unchanged"]
        missing = sorted(p for p in files if p not in existing)
        if changed_eps or changed_shared or report.conflicts or missing:
            results.append(CheckResult("stale", FAIL,
                f"{len(changed_eps)} endpoint section(s), {len(changed_shared)} shared section(s) differ from disk, "
                f"{len(missing)} file(s) missing, {len(report.conflicts)} conflict(s) — regenerate (--function update)"))
        else:
            results.append(CheckResult("stale", PASS, f"disk matches the render ({len(files)} files)"))

    drift = document.contract_drift
    if drift:
        kinds = {}
        for d in drift:
            kinds[d.kind] = kinds.get(d.kind, 0) + 1
        results.append(CheckResult("contract-drift", FAIL,
            ", ".join(f"{v} {k}" for k, v in sorted(kinds.items())) + " (see index.md → Contract Traceability)"))
    elif document.contract_source:
        results.append(CheckResult("contract-drift", PASS, "declared and served agree"))
    else:
        results.append(CheckResult("contract-drift", NA, "no API document or API REGISTRY joined"))

    plan = context.execution_plan
    spec = getattr(context, "api_spec", None)
    stamped = sum(1 for ep in endpoints if ep.api_id)
    if spec is not None and spec.exists():
        # The API document is the contract (schema 7). Zero x-api-id
        # operations in it is the factory's defect, never a reason to fall
        # back to prose.
        if document.contract_source:
            results.append(CheckResult("contract-ids", PASS, f"{_ratio(stamped, n, 'endpoints stamped')} from {spec.name} (API document)"))
        else:
            results.append(CheckResult("contract-ids", FAIL,
                f"{_ratio(0, n, 'endpoints stamped')}: {spec.name} has no operation carrying x-api-id → factory item"))
    elif plan is None or not plan.exists():
        results.append(CheckResult("contract-ids", NA, "no backend execution plan found for this module"))
    else:
        plan_text = plan.read_text(encoding="utf-8", errors="ignore")
        heading = contract_extractor._REGISTRY_HEADING_RE.search(plan_text) is not None
        if document.contract_source:
            results.append(CheckResult("contract-ids", PASS, f"{_ratio(stamped, n, 'endpoints stamped')} from {plan.name} "
                                       f"(FALLBACK: pre-v7 prose API REGISTRY — no API document)"))
        elif heading:
            results.append(CheckResult("contract-ids", FAIL,
                f"{_ratio(0, n, 'endpoints stamped')}: {plan.name} has an API REGISTRY heading but no parseable "
                f"row (a verb+path table is needed; prose registries carry no contract) → factory item"))
        else:
            results.append(CheckResult("contract-ids", INFO, f"{plan.name} has no API REGISTRY heading"))

    if source_root is None:
        results.append(CheckResult("permissions", NA, "no module source root"))
    else:
        found = sum(1 for ep in endpoints if ep.permission or ep.permission_expression)
        declared_none = sum(1 for ep in endpoints if ep.permission_lookup == security_extractor.DECLARED_NONE)
        not_extracted = [ep for ep in endpoints if ep.permission_lookup in security_extractor.NOT_EXTRACTED]
        annotations = facts.get("auth_annotations", 0)
        detail = (f"{_ratio(found, n, 'endpoints carry a rule')}, {declared_none} declared none, "
                  f"{len(not_extracted)} not extracted; {annotations} @PreAuthorize/@Secured in source")
        if not_extracted:
            outcomes = sorted({ep.permission_lookup for ep in not_extracted})
            results.append(CheckResult("permissions", FAIL, detail + f" → not extracted: {', '.join(outcomes)}"))
        elif found == 0 and annotations > 0:
            results.append(CheckResult("permissions", FAIL, detail + " → every rule in source was missed"))
        else:
            results.append(CheckResult("permissions", PASS, detail))

    determined = sum(1 for ep in endpoints if ep.requires_auth is not None)
    chains = _source_files_matching(common, r"\bSecurityFilterChain\b|@EnableWebSecurity")
    if n and determined == 0 and chains:
        results.append(CheckResult("auth-determined", FAIL,
            f"{_ratio(0, n, 'endpoints carry an OpenAPI security requirement')}; a SecurityFilterChain is declared in "
            f"source ({chains} file(s)) → the OpenAPI document declares no SecurityScheme/SecurityRequirement "
            f"(backend: OpenApiConfig)"))
    else:
        results.append(CheckResult("auth-determined", PASS if determined or not chains else NA,
                                   f"{_ratio(determined, n, 'endpoints carry an OpenAPI security requirement')}"))

    envelope_classes = _source_files_matching(common, r"\bclass\s+ApiResponse\b")
    if document.response_envelope is None and envelope_classes:
        results.append(CheckResult("envelope", FAIL, "no response envelope detected in the OpenAPI schemas while "
                                                     "ApiResponse is declared in source"))
    else:
        results.append(CheckResult("envelope", PASS if document.response_envelope else NA,
                                   f"envelope {document.response_envelope.schema_name}" if document.response_envelope
                                   else "no envelope in schemas, none declared in source"))

    module_codes = [c for c in document.error_codes if c.source_file.endswith("ErrorCodes.java")]
    code_files = len(list(source_root.rglob("*ErrorCodes.java"))) if source_root else 0
    if code_files and not module_codes:
        results.append(CheckResult("error-codes", FAIL, f"0 codes read from {code_files} *ErrorCodes.java file(s)"))
    else:
        results.append(CheckResult("error-codes", PASS if module_codes else NA,
                                   f"{len(module_codes)} module codes from {code_files} *ErrorCodes.java file(s)"))

    tables = _source_files_matching(common, r"\benum\s+Status\b|statusMappings\.put")
    if tables and not document.status_mappings:
        results.append(CheckResult("status-table", FAIL, "Status → HTTP table not read although declared in source"))
    else:
        results.append(CheckResult("status-table", PASS if document.status_mappings else NA,
                                   f"{len(document.status_mappings)} Status → HTTP rows"))

    if source_root is None:
        results.append(CheckResult("business-errors", NA, "no module source root"))
    else:
        bound = facts.get("endpoints_with_business_errors", 0)
        walked = facts.get("endpoints_walked_past_controller", 0)
        sites = facts.get("throw_sites", 0)
        unbound = sorted(c.name for c in module_codes if c.throw_sites and not c.bound_endpoints)
        unresolved = sum(len(ep.business_walk.unresolved) for ep in endpoints if ep.business_walk)
        detail = (f"{_ratio(bound, n, 'endpoints bound to ≥1 code')}; {_ratio(walked, n, 'walks left the controller')}; "
                  f"{sites} throw sites in source; {len(unbound)} code(s) thrown but reached by no walk"
                  + (f": {', '.join(unbound)}" if unbound else "")
                  + (f"; {unresolved} ambiguous call(s) skipped" if unresolved else ""))
        if sites and bound == 0:
            results.append(CheckResult("business-errors", FAIL, detail + " → the walk attributed nothing"))
        elif n and walked == 0:
            results.append(CheckResult("business-errors", FAIL, detail + " → no walk resolved a delegate"))
        else:
            results.append(CheckResult("business-errors", PASS, detail))

    field_calls = _source_files_matching(common, r"GlobalExceptionHandler[\s\S]*\.field\s*\(")
    if field_calls and not document.field_error_semantics:
        results.append(CheckResult("field-error-semantics", FAIL,
                                   "GlobalExceptionHandler sets fieldErrors[].field but no semantics row was read"))
    else:
        results.append(CheckResult("field-error-semantics", PASS if document.field_error_semantics else NA,
                                   f"{len(document.field_error_semantics)} handler(s) documented"))

    declared_unique = facts.get("unique_declared", 0)
    rows = len(document.unique_constraints)
    if declared_unique and not rows:
        results.append(CheckResult("unique-constraints", FAIL,
                                   f"0 invariants read while {declared_unique} @UniqueConstraint declared in entities"))
    else:
        results.append(CheckResult("unique-constraints", PASS if rows else NA,
                                   f"{rows} invariant(s); {declared_unique} @UniqueConstraint in entities; "
                                   f"{len(context.migration_roots)} migration dir(s)"))

    waivers, problems = load_waivers(context.module)
    for r in results:
        if r.verdict == FAIL and r.name in waivers:
            w = waivers[r.name]
            r.verdict = WAIVED
            r.waiver = f"waived by {w.by} on {w.on}: {w.reason}"
    for p in problems:
        results.append(CheckResult("waivers", FAIL, p))
    return results


def failed(results: list[CheckResult]) -> list[str]:
    return [r.name for r in results if r.verdict == FAIL]


def format_checks(module: str, results: list[CheckResult], endpoint_count: int, output: Path) -> str:
    lines = [f"CHECK {module} — {endpoint_count} endpoints, docs at {output}"]
    for r in results:
        lines.append(f"  {r.verdict:<6} {r.name:<22} {r.detail}")
        if r.waiver:
            lines.append(f"         {'':<22} {r.waiver}")
    failing = failed(results)
    lines.append(f"VERDICT {module}: " + (f"FAIL ({', '.join(failing)})" if failing else "PASS"))
    return "\n".join(lines)
