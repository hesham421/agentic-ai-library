"""
discovery.py — resolves everything generate.py needs from just --module.
════════════════════════════════════════════════════════════════════
This is neither an extractor, a renderer, nor the sync layer — it's the
plumbing that runs *before* extraction, turning "ORG" into the concrete
inputs (an OpenAPI source, a module source root, zero or more shared
common-source roots, an output directory) the existing pipeline already
knows how to consume. Nothing downstream needs to change to accept
discovered inputs instead of hand-typed ones — they're the same shapes
generator.run() already accepted.

Everything here is derived from real, versioned repository artifacts that
this platform already depends on for other reasons, not from guessing a
module's name:

  - governance's location inside this backend/ checkout (governance/ is a
    subfolder of backend/ as of the backend/frontend governance split;
    see backend/governance/governance-tools/README.md)
  - the Maven descriptors (root pom.xml; its <modules> reactor list when one
    exists, each module's own <dependencies> otherwise) for module source +
    shared/common source roots. This platform currently builds as a single
    consolidated POM, which find_common_source_roots handles explicitly.
  - the GroupedOpenApi bean declarations for the springdoc group id,
    package(s), and display name that identify "which module is ORG"
  - application*.properties' own server.port / springdoc.api-docs.path /
    server.servlet.context-path for the dev server's OpenAPI URL

Every discovery step degrades to "not found" rather than guessing, exactly
like the existing best-effort extractors (security_extractor.py,
exception_extractor.py) already do. Callers (generate.py) turn a "not found"
into a clear message naming the explicit override flag to pass instead.
"""

import json
import re
import subprocess
import sys
from dataclasses import dataclass, field
from functools import lru_cache
from pathlib import Path
from typing import Optional

GENERATOR_ROOT = Path(__file__).resolve().parent           # .../backend/governance/governance-tools/api-doc-generator
GOVERNANCE_ROOT = GENERATOR_ROOT.parent.parent              # .../backend/governance
BACKEND_ROOT = GOVERNANCE_ROOT.parent                       # .../backend — governance now lives INSIDE backend/, not as its sibling


class DiscoveryError(Exception):
    """Raised when a required input can't be reliably discovered. Always
    carries the name of the explicit override flag the caller should use."""


@dataclass
class OpenApiGroupInfo:
    method_name: str
    group_id: str
    display_name: Optional[str]
    packages: list[str] = field(default_factory=list)


@dataclass
class RepositoryContext:
    """Everything the extraction pipeline needs, already resolved. This is
    the ONLY object that carries repository-shaped knowledge (locations,
    which module, which shared resources) across the discovery -> pipeline
    boundary. generator.build_document() is the only consumer that unpacks
    it; individual extractors keep taking their own narrow, explicit inputs
    (a Path, a dict, ...) rather than this object itself, so an extractor's
    signature always says exactly what data it needs and stays usable
    outside of this discovery flow (e.g. from a test) without depending on
    discovery.py at all."""
    module: str
    openapi_source: str
    output: Path
    source_root: Optional[Path] = None
    common_source_roots: list[Path] = field(default_factory=list)
    # The module's API document (api-spec-<mod>.yaml, OpenAPI 3.1, factory
    # schema 7) -- THE declared contract: each operation's x-api-id, method
    # and path. Resolved through scripts/gov-module.py, never typed.
    api_spec: Optional[Path] = None
    # The module's backend execution plan as of its delivered version. Read
    # for its prose API REGISTRY only when there is no API document (a pre-v7
    # module) -- the fallback, said out loud in contract_note.
    execution_plan: Optional[Path] = None
    # One line saying where the contract ids come from (or why none do).
    contract_note: Optional[str] = None
    # Flyway migration directories, from spring.flyway.locations. Read for the
    # uniqueness invariants the entities cannot express (partial indexes).
    migration_roots: list[Path] = field(default_factory=list)
    # The i18n resource bundles spring.messages.basename points at, and that
    # basename's file part. Read so each error code can publish the message a
    # consumer will actually receive for it; absent simply means no message
    # column is rendered.
    message_bundles: list[Path] = field(default_factory=list)
    message_basename: Optional[str] = None


def default_backend_root() -> Path:
    return BACKEND_ROOT


@lru_cache(maxsize=1)
def _shared_modules_root() -> Path:
    """Resolves the shared submodule's ANALYSIS modules directory (where each
    module's plans live) via governance/shared/platform/profile-summary.json's
    paths.modules (today "analysis/modules"), never a typed path. It is not
    the backend partition (backend/modules/<MOD>), which holds the delivered
    packages, the API document and this track's own writes."""
    shared = GOVERNANCE_ROOT / "shared"
    summary_path = shared / "platform" / "profile-summary.json"
    if not summary_path.is_file():
        raise SystemExit(
            f"governance/shared is not initialised at {shared}\n"
            f"  run: git submodule update --init governance/shared")
    summary = json.loads(summary_path.read_text(encoding="utf-8"))
    modules_rel = summary["paths"]["modules"]
    return shared / modules_rel


def _backend_partition_root(module: str) -> Path:
    """This track's own writable partition inside the shared repo, as the factory
    publishes it (profile-summary.json -> tracks.backend.partition, e.g.
    "backend/modules/{MOD}"). Never spelled literally here: a second profile, or
    a renamed track folder, is then the factory's edit and not this tool's."""
    shared = GOVERNANCE_ROOT / "shared"
    summary_path = shared / "platform" / "profile-summary.json"
    if not summary_path.is_file():
        raise SystemExit(
            f"governance/shared is not initialised at {shared}\n"
            f"  run: git submodule update --init governance/shared")
    summary = json.loads(summary_path.read_text(encoding="utf-8"))
    try:
        partition = summary["tracks"]["backend"]["partition"]
    except KeyError as exc:
        raise SystemExit(
            f"profile-summary.json declares no tracks.backend.partition ({exc}) -- "
            f"cannot resolve where this repo may write api-docs") from exc
    return shared / partition.replace("{MOD}", module)


def default_output_dir(module: str) -> Path:
    """Where generated api-docs land.

    Schema 7 DECLARES it -- profile-summary.json -> consumer.partitions.api-docs
    .path (backend/modules/{MOD}/api-docs) -- and that is read first. Everything
    below is the pre-v7 inference, kept only for a project that declares nothing.

    They live in the shared repo, not in this one: the factory and the frontend
    read the SAME copy, so there is no second copy to drift from. This repo still
    authors them — the generator reads the running app — and the shared repo's
    CODEOWNERS grants this repo write access to exactly this path and no other.

    WHICH path that is depends on the profile's layout, and the two layouts in
    use disagree, so neither is hard-coded:

      * module-root layout   -> <paths.modules>/<MOD>/api-docs
        (profile "erp": erp/modules/FIN/api-docs — what CLAUDE.md's ownership
        table documents)
      * track-partition layout -> <tracks.backend.partition>/api-docs
        (the v7 project-repo layout: backend/modules/FIN/api-docs)

    Resolving against the wrong one does not fail loudly — it reports every
    endpoint as "added" and writes a second, empty-history copy of the docs
    beside the real ones. That happened on 2026-09-19 in both directions: first
    because this function assumed the module root while the checkout was v7,
    then, after the naive repair, because it assumed the partition while the
    checkout was "erp". So it asks the checkout instead of assuming, and where
    the checkout cannot answer it refuses to guess.

    **This is a workaround, not the fix.** The profile publishes paths.modules,
    tracks.backend.partition and module_dirs, but NOTHING that declares where a
    module's api-docs belong — so the one consumer that writes them has to infer
    it. The three places that do state the path disagree, and one of them names
    a directory that does not exist on this profile at all:

      * platform/rules/api-verify-config.md:14 -> governance/shared/backend/modules/<MOD>/api-docs/
      * the backend repo's CLAUDE.md ownership table -> $GOV/modules/<MOD>/api-docs/
      * the tree as delivered -> erp/modules/<MOD>/api-docs/

    The real repair is a declared key in profile-summary.json that this function
    reads and fails on when absent, the way it already fails on a missing
    tracks.backend.partition. That is the factory's to publish, and is recorded
    for it in the backend's execution-state.json rather than guessed at here.
    """
    declared = _declared_api_docs_dir(module)
    if declared is not None:
        return declared
    candidates = [
        _shared_modules_root() / module / "api-docs",
        _backend_partition_root(module) / "api-docs",
    ]
    existing = [c for c in candidates if c.is_dir()]
    if len(existing) == 1:
        return existing[0]
    if len(existing) > 1:
        raise SystemExit(
            "two api-docs directories exist for {} and only one can be authoritative:\n"
            "  {}\n  {}\n"
            "delete the stale one before regenerating -- writing to either would "
            "leave the other silently out of date".format(module, *existing))
    raise SystemExit(
        "cannot tell where {mod}'s api-docs belong: neither candidate exists yet, and the\n"
        "profile declares no api-docs path to settle it.\n"
        "  module root      : {a}\n"
        "  track partition  : {b}\n"
        "Pass --output with the correct one (and ask the factory to publish the path in\n"
        "profile-summary.json, so the next module does not hit this). Guessing here is what\n"
        "produced two empty-history copies of FIN's docs on 2026-09-19.".format(
            mod=module, a=candidates[0], b=candidates[1]))


def default_module_dir(module: str) -> Path:
    return _shared_modules_root() / module


def _summary() -> dict:
    path = GOVERNANCE_ROOT / "shared" / "platform" / "profile-summary.json"
    return json.loads(path.read_text(encoding="utf-8")) if path.is_file() else {}


def _declared_api_docs_dir(module: str) -> Optional[Path]:
    """The api-docs partition the factory DECLARES (schema 7:
    profile-summary.json -> consumer.partitions.api-docs.path). None on a
    pre-v7 project, which declares nothing and falls back to the inference
    in default_output_dir."""
    part = ((_summary().get("consumer") or {}).get("partitions") or {}).get("api-docs") or {}
    path = part.get("path")
    if not path:
        return None
    return GOVERNANCE_ROOT / "shared" / path.replace("{MOD}", module.upper()).replace("{mod}", module.lower())


_PLAN_ERRORS: dict[str, str] = {}


@lru_cache(maxsize=None)
def module_plan(module: str) -> Optional[dict]:
    """`scripts/gov-module.py plan MOD --json` -- the consumer's one reader of
    the project repo (schema 7). None when the tool is absent, the project
    predates schema 7 (exit 3), or the module has no backend delivery yet
    (exit 1); the reason is kept in module_plan_error."""
    tool = BACKEND_ROOT / "scripts" / "gov-module.py"
    if not tool.is_file():
        _PLAN_ERRORS[module] = f"{tool} not found"
        return None
    proc = subprocess.run(
        [sys.executable, str(tool), "--project", str(GOVERNANCE_ROOT / "shared"), "--track", "backend",
         "plan", module.upper(), "--json"],
        capture_output=True, text=True)
    if proc.returncode != 0:
        said = (proc.stderr or proc.stdout).strip().splitlines()
        _PLAN_ERRORS[module] = said[-1] if said else f"gov-module.py exit {proc.returncode}"
        return None
    try:
        return json.loads(proc.stdout)
    except ValueError as exc:
        _PLAN_ERRORS[module] = f"gov-module.py plan printed no JSON ({exc})"
        return None


def module_plan_error(module: str) -> Optional[str]:
    return _PLAN_ERRORS.get(module)


def find_api_spec(module: str) -> Optional[Path]:
    """The module's API document: `plan.api_spec` from gov-module.py, else
    (no delivery yet, but a v7 project) the summary's
    consumer.publications.api-specs. Only an existing file counts."""
    plan = module_plan(module)
    if plan and plan.get("api_spec"):
        p = Path(plan["api_spec"])
        return p if p.is_file() else None
    home = ((_summary().get("consumer") or {}).get("publications") or {}).get("api-specs")
    if home:
        p = GOVERNANCE_ROOT / "shared" / home.replace("{MOD}", module.upper()).replace("{mod}", module.lower())
        return p if p.is_file() else None
    return None


_VERSION_DIR_RE = re.compile(r"^v(\d+)$")


def _registry_version(module: str) -> Optional[int]:
    """The module's version as the published registry states it: the backend
    delivery's version, else current_version. A delta v<N>/ folder holds only
    what changed, so the HIGHEST folder on disk is never the answer."""
    reg = GOVERNANCE_ROOT / "shared" / "platform" / "modules-registry.json"
    if not reg.is_file():
        return None
    mods = json.loads(reg.read_text(encoding="utf-8")).get("modules") or {}
    if isinstance(mods, list):
        mods = {m.get("code"): m for m in mods}
    m = mods.get(module.upper()) or {}
    delivered = ((m.get("delivery") or {}).get("backend") or {}).get("version")
    v = delivered or m.get("current_version")
    return int(v) if v else None


def _plan_search_roots(module_dir: Path, upto: Optional[int]) -> list[Path]:
    """Where the plan as of version `upto` lives: v<upto>/, v<upto-1>/, ...
    v2/, then the module dir itself (v1). The latest folder that HOLDS a plan
    wins -- the same rule gov-module.py applies to every artifact."""
    versions = [d for d in module_dir.iterdir()
                if d.is_dir() and _VERSION_DIR_RE.match(d.name)
                and (upto is None or int(_VERSION_DIR_RE.match(d.name).group(1)) <= upto)]
    if upto is None:
        versions = []          # no registry version: v1 only, never "the highest folder"
    versions.sort(key=lambda d: int(_VERSION_DIR_RE.match(d.name).group(1)), reverse=True)
    return versions + [module_dir]


def find_execution_plan(module: str) -> Optional[Path]:
    """The module's backend execution plan as of its DELIVERED version.

    Schema 7: `plan.exec_plan` from gov-module.py. Pre-v7 fallback: the
    registry's version (backend delivery, else current_version), searched
    v<N>/ down to v1 for the latest folder that holds a plan. Matched by role
    -- backend-execution-plan[-<mod>].md -- never by a hard-coded phase
    folder; no match means no contract ids."""
    plan = module_plan(module)
    if plan and plan.get("exec_plan"):
        p = Path(plan["exec_plan"])
        if p.is_file():
            return p
    module_dir = default_module_dir(module)
    if not module_dir.is_dir():
        return None
    for root in _plan_search_roots(module_dir, _registry_version(module)):
        # The module dir (v1) must not see its own v<N>/ subfolders.
        matches = sorted(p for p in root.rglob("backend-execution-plan*.md")
                         if not _VERSION_DIR_RE.match(p.relative_to(root).parts[0]))
        if matches:
            # A split package copy (packages/backend-execution/...) restates
            # parts of the plan; the shallowest match is the plan proper.
            return min(matches, key=lambda p: (len(p.parts), str(p)))
    return None


def resolve_contract(module: str, api_spec_override: Optional[Path] = None,
                     execution_plan_override: Optional[Path] = None) -> tuple[Optional[Path], Optional[Path], str]:
    """(api_spec, execution_plan, note). The API document wins whenever it
    exists; the prose API REGISTRY of the plan is read only for a pre-v7
    module with no API document, and the note says so."""
    spec = api_spec_override or find_api_spec(module)
    plan = execution_plan_override or find_execution_plan(module)
    if spec is not None:
        return spec, plan, f"API document {spec.name}"
    why = module_plan_error(module) or "no API document published for this module"
    if plan is not None:
        return None, plan, (f"FALLBACK — no API document ({why}); contract ids read from the prose "
                            f"API REGISTRY of {plan.name} (pre-v7 module)")
    return None, None, f"none — no API document ({why}) and no backend execution plan"


def _normalize(s: str) -> str:
    return re.sub(r"[^a-z0-9]", "", s.lower())


# ---------------------------------------------------------------------------
# GroupedOpenApi discovery (springdoc group id, packages, display name)
# ---------------------------------------------------------------------------

_METHOD_RE = re.compile(r"\bGroupedOpenApi\s+(\w+)\s*\(\s*\)\s*\{")
_GROUP_ID_RE = re.compile(r'\.group\(\s*"([^"]+)"\s*\)')
_DISPLAY_NAME_RE = re.compile(r'\.displayName\(\s*"([^"]+)"\s*\)')
_PACKAGES_RE = re.compile(r'\.packagesToScan\(\s*((?:"[^"]*"\s*,?\s*)+)\)', re.DOTALL)
_QUOTED_RE = re.compile(r'"([^"]*)"')


def _brace_span(text: str, open_brace_index: int) -> int:
    """Returns the index just past the matching closing brace for the '{' at
    open_brace_index."""
    depth = 0
    for i in range(open_brace_index, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return i + 1
    return len(text)


def _parse_groups_from_source(text: str) -> list[OpenApiGroupInfo]:
    groups: list[OpenApiGroupInfo] = []
    for m in _METHOD_RE.finditer(text):
        body_start = text.index("{", m.end() - 1)
        body_end = _brace_span(text, body_start)
        body = text[body_start:body_end]

        group_m = _GROUP_ID_RE.search(body)
        if not group_m:
            continue
        display_m = _DISPLAY_NAME_RE.search(body)
        packages_m = _PACKAGES_RE.search(body)
        packages = _QUOTED_RE.findall(packages_m.group(1)) if packages_m else []

        groups.append(OpenApiGroupInfo(
            method_name=m.group(1),
            group_id=group_m.group(1),
            display_name=display_m.group(1) if display_m else None,
            packages=packages,
        ))
    return groups


def find_openapi_groups(backend_root: Path) -> list[OpenApiGroupInfo]:
    groups: list[OpenApiGroupInfo] = []
    for path in sorted(backend_root.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="ignore")
        if "GroupedOpenApi.builder()" not in text:
            continue
        groups.extend(_parse_groups_from_source(text))
    return groups


def match_openapi_group(groups: list[OpenApiGroupInfo], module: str) -> Optional[OpenApiGroupInfo]:
    """Matches --module against method name / group id / display name first
    (specific, intentional identifiers); only falls back to packagesToScan if
    nothing matched there, then breaks ties by preferring the group that
    scans the fewest packages -- so a combined "all modules" group (which by
    construction lists every module's package) never wins over a genuine
    single-module group. Returns None on no match or genuine ambiguity --
    callers must fall back to an explicit override, never guess."""
    norm_module = _normalize(module)
    if not norm_module:
        return None

    tier1 = [
        g for g in groups
        if norm_module in _normalize(g.method_name)
        or norm_module in _normalize(g.group_id)
        or norm_module in _normalize(g.display_name or "")
    ]
    if len(tier1) == 1:
        return tier1[0]
    if len(tier1) > 1:
        return None  # genuinely ambiguous among specific identifiers

    tier2 = [g for g in groups if any(norm_module in _normalize(p) for p in g.packages)]
    if not tier2:
        return None
    fewest = min(len(g.packages) for g in tier2)
    narrowed = [g for g in tier2 if len(g.packages) == fewest]
    return narrowed[0] if len(narrowed) == 1 else None


# ---------------------------------------------------------------------------
# Server / springdoc URL discovery
# ---------------------------------------------------------------------------
# The OpenAPI document's URL is NOT "localhost:8080/api-docs/<group>" by
# assumption -- all three of its parts are configuration this backend already
# declares in its own application*.properties, and all three move
# independently:
#
#   server.port                  -- this platform runs on 7272, not Spring's 8080 default
#   springdoc.api-docs.path      -- springdoc's own default is /v3/api-docs, and a group is
#                                   served at <that path>/<groupId>; /api-docs is NOT a
#                                   valid fallback, it is simply a different path
#   server.servlet.context-path  -- prefixes everything when set
#
# So they are read from the property files themselves. The @Value("${server.port:NNNN}")
# Java form is still honoured as a fallback for a backend that declares the
# default in code instead, and only then does a literal default apply.

_PORT_VALUE_ANNOTATION_RE = re.compile(r'@Value\(\s*"\$\{server\.port:(\d+)\}"\s*\)')

_PROPERTY_RE_CACHE: dict[str, re.Pattern] = {}

SPRINGDOC_DEFAULT_API_DOCS_PATH = "/v3/api-docs"   # springdoc-openapi's own documented default


def _property_files(backend_root: Path) -> list[Path]:
    """application.properties first (the un-profiled base every profile
    inherits), then any profile-specific file, so a base declaration wins
    over a profile override this tool has no way to know is active."""
    resources = backend_root / "src" / "main" / "resources"
    if not resources.is_dir():
        return []
    base = resources / "application.properties"
    files = [base] if base.exists() else []
    files += sorted(p for p in resources.glob("application-*.properties") if p != base)
    return files


def _read_property(backend_root: Path, key: str) -> Optional[str]:
    pattern = _PROPERTY_RE_CACHE.get(key)
    if pattern is None:
        pattern = re.compile(rf"^\s*{re.escape(key)}\s*[=:]\s*(\S+)\s*$", re.MULTILINE)
        _PROPERTY_RE_CACHE[key] = pattern
    for path in _property_files(backend_root):
        m = pattern.search(path.read_text(encoding="utf-8", errors="ignore"))
        if m:
            return m.group(1)
    return None


def find_server_port(backend_root: Path, default: str = "8080") -> str:
    declared = _read_property(backend_root, "server.port")
    if declared and declared.isdigit():
        return declared
    for path in sorted(backend_root.rglob("*.java")):
        text = path.read_text(encoding="utf-8", errors="ignore")
        m = _PORT_VALUE_ANNOTATION_RE.search(text)
        if m:
            return m.group(1)
    return default


def find_api_docs_path(backend_root: Path) -> str:
    path = _read_property(backend_root, "springdoc.api-docs.path") or SPRINGDOC_DEFAULT_API_DOCS_PATH
    return "/" + path.strip("/")


def find_context_path(backend_root: Path) -> str:
    path = _read_property(backend_root, "server.servlet.context-path")
    if not path or path.strip("/") == "":
        return ""
    return "/" + path.strip("/")


def build_openapi_url(backend_root: Path, group_id: str) -> str:
    """The live URL springdoc actually serves this group at, assembled from
    the backend's own declared configuration rather than assumed."""
    port = find_server_port(backend_root)
    context = find_context_path(backend_root)
    api_docs = find_api_docs_path(backend_root)
    return f"http://localhost:{port}{context}{api_docs}/{group_id}"


FLYWAY_DEFAULT_LOCATIONS = "classpath:db/migration"


def find_migration_roots(backend_root: Path) -> list[Path]:
    """Every existing directory spring.flyway.locations points at (Flyway's own
    default when the property is absent), under each src/main/resources of a
    consolidated or reactor layout."""
    declared = _read_property(backend_root, "spring.flyway.locations") or FLYWAY_DEFAULT_LOCATIONS
    resource_dirs = [backend_root / "src" / "main" / "resources"] + sorted(backend_root.glob("*/src/main/resources"))
    roots: list[Path] = []
    for location in declared.split(","):
        location = location.strip()
        if location.startswith("filesystem:"):
            candidate = Path(location[len("filesystem:"):])
            candidates = [candidate if candidate.is_absolute() else backend_root / candidate]
        else:
            rel = location.split(":", 1)[1] if ":" in location else location
            candidates = [d / rel.strip("/") for d in resource_dirs]
        roots += [c for c in candidates if c.is_dir() and c not in roots]
    return roots


MESSAGES_DEFAULT_BASENAME = "messages"


def find_message_bundles(backend_root: Path) -> tuple[list[Path], str]:
    """(bundle files, base file name) for spring.messages.basename -- Spring's
    own default ("messages") when the property is absent, exactly as Spring
    itself falls back. Returns every sibling locale variant of the base file
    (messages.properties, messages_ar.properties, ...) under each
    src/main/resources of a consolidated or reactor layout.

    Only the first basename is read when several are declared comma-separated:
    the error-code convention on this platform puts every module's keys into
    one merged bundle (spring.messages.basename is single-based), so a second
    basename would be a different concern, not more of the same one."""
    declared = _read_property(backend_root, "spring.messages.basename") or MESSAGES_DEFAULT_BASENAME
    first = declared.split(",")[0].strip()
    rel = first.split(":", 1)[1] if ":" in first else first
    rel = rel.strip("/")
    base_name = rel.split("/")[-1]
    parent_rel = "/".join(rel.split("/")[:-1])
    resource_dirs = [backend_root / "src" / "main" / "resources"] + sorted(backend_root.glob("*/src/main/resources"))
    files: list[Path] = []
    for d in resource_dirs:
        directory = d / parent_rel if parent_rel else d
        if not directory.is_dir():
            continue
        for candidate in sorted(directory.glob(f"{base_name}*.properties")):
            tag = candidate.stem
            if (tag == base_name or tag.startswith(base_name + "_")) and candidate not in files:
                files.append(candidate)
    return files, base_name


# ---------------------------------------------------------------------------
# Module source root discovery (from a matched group's packages)
# ---------------------------------------------------------------------------

def find_module_source_root(backend_root: Path, packages: list[str]) -> Optional[Path]:
    """Given the controller package(s) a matched OpenAPI group scans, finds
    which Maven module's src/main/java actually contains that package, then
    returns the module's own domain-root directory rather than the whole
    src/main/java tree -- so module-own best-effort extractors (error codes,
    throw-site Status associations, controller/service lookup) only ever see
    this module's own code, never another module's. This matters most under a
    single consolidated POM layout (src/main/java directly under
    backend_root, e.g. this platform's own erp-system, as opposed to one
    Maven module directory per domain) where every module's code shares one
    physical src/main/java tree and would otherwise all be visible at once.

    Every GroupedOpenApi bean on this platform scans a "<domain>.controller"
    leaf package (e.g. "com.erp.security.controller"), not the module's
    whole domain package -- but the domain's other code (service/,
    exception/, dto/, ...) lives one level up, as siblings of controller/,
    not inside it. So when the matched package's last segment is literally
    "controller", scope to its parent (the domain root) instead of the
    controller subpackage itself; otherwise scope to the matched package
    directory as-is, in case a future module doesn't follow that convention."""
    candidate_roots = [backend_root / "src" / "main" / "java"]
    candidate_roots += sorted(backend_root.glob("*/src/main/java"))
    for java_root in candidate_roots:
        for package in packages:
            package_dir = java_root / Path(*package.split("."))
            if package_dir.is_dir():
                return package_dir.parent if package_dir.name == "controller" else package_dir
    return None


# ---------------------------------------------------------------------------
# Shared/common source discovery (Maven reactor dependency graph)
# ---------------------------------------------------------------------------

_ARTIFACT_ID_RE = re.compile(r"<artifactId>([^<]+)</artifactId>")
_MODULE_RE = re.compile(r"<module>([^<]+)</module>")
_DEPENDENCY_BLOCK_RE = re.compile(r"<dependency>(.*?)</dependency>", re.DOTALL)


def find_common_source_roots(backend_root: Path, module_source_root: Path) -> list[Path]:
    """Resolves every OTHER reactor module that module_source_root's own
    module actually declares as a Maven dependency -- the real, versioned,
    build-enforced link to shared/common code, as opposed to guessing a name
    like "common-utils". Works for any number of shared modules, not just one.

    module_source_root is now package-scoped (see find_module_source_root),
    so its owning Maven module directory (the one with the pom.xml) is found
    by walking upward until a pom.xml turns up, rather than assuming a fixed
    number of parent hops -- that walk naturally lands on backend_root itself
    under a single consolidated POM layout (this platform's own erp-system),
    where no nearer pom.xml exists between the package directory and the
    repo root."""
    root_pom = backend_root / "pom.xml"
    if not root_pom.exists():
        return []
    reactor_modules = set(_MODULE_RE.findall(root_pom.read_text(encoding="utf-8")))

    module_root = module_source_root
    while module_root != backend_root and not (module_root / "pom.xml").exists():
        module_root = module_root.parent

    if module_root == backend_root:
        # Single consolidated POM (no <modules> reactor split) -- there is no
        # separate shared module to resolve, since shared/common code
        # (com.erp.common, GlobalExceptionHandler, OperationCodeImpl, ...)
        # already lives inside this same src/main/java tree. Search that
        # whole tree rather than just this module's own package.
        return [backend_root / "src" / "main" / "java"]

    module_pom = module_root / "pom.xml"
    if not module_pom.exists():
        return []
    module_text = module_pom.read_text(encoding="utf-8")

    dep_artifacts = set()
    for dep_block in _DEPENDENCY_BLOCK_RE.findall(module_text):
        m = _ARTIFACT_ID_RE.search(dep_block)
        if m:
            dep_artifacts.add(m.group(1))

    roots = []
    for artifact in sorted(dep_artifacts & reactor_modules):
        candidate = backend_root / artifact / "src" / "main" / "java"
        if candidate.is_dir():
            roots.append(candidate)
    return roots


# ---------------------------------------------------------------------------
# Top-level resolve()
# ---------------------------------------------------------------------------

def resolve(
    module: str,
    *,
    backend_root: Optional[Path] = None,
    openapi_override: Optional[str] = None,
    source_override: Optional[Path] = None,
    common_source_overrides: Optional[list[Path]] = None,
    output_override: Optional[Path] = None,
    execution_plan_override: Optional[Path] = None,
    api_spec_override: Optional[Path] = None,
) -> RepositoryContext:
    backend_root = backend_root or default_backend_root()
    output = output_override or default_output_dir(module)
    api_spec, execution_plan, contract_note = resolve_contract(module, api_spec_override, execution_plan_override)
    contract = dict(api_spec=api_spec, execution_plan=execution_plan, contract_note=contract_note)

    if openapi_override and source_override:
        # Every input explicitly given -- no repository discovery needed at all.
        return RepositoryContext(
            module=module,
            openapi_source=openapi_override,
            output=output,
            source_root=source_override,
            common_source_roots=common_source_overrides or [],
            **contract,
        )

    if not backend_root.exists():
        if not openapi_override:
            raise DiscoveryError(
                f"Backend repository not found at expected path '{backend_root}'. "
                f"Pass --openapi explicitly (and --source, if permission/error-code sections are wanted)."
            )
        return RepositoryContext(module=module, openapi_source=openapi_override, output=output,
                               source_root=source_override, common_source_roots=common_source_overrides or [],
                               **contract)

    groups = find_openapi_groups(backend_root)
    matched = match_openapi_group(groups, module)
    message_bundles, message_basename = find_message_bundles(backend_root)

    openapi_source = openapi_override
    source_root = source_override
    common_source_roots = list(common_source_overrides or [])

    if openapi_source is None:
        if matched is None:
            raise DiscoveryError(
                f"Could not identify a unique springdoc GroupedOpenApi group for module '{module}' "
                f"under '{backend_root}'. Pass --openapi explicitly (a live URL or a saved JSON file)."
            )
        openapi_source = build_openapi_url(backend_root, matched.group_id)

    if source_root is None and matched is not None:
        source_root = find_module_source_root(backend_root, matched.packages)

    if not common_source_overrides and source_root is not None:
        common_source_roots = find_common_source_roots(backend_root, source_root)

    return RepositoryContext(
        module=module,
        openapi_source=openapi_source,
        output=output,
        source_root=source_root,
        common_source_roots=common_source_roots,
        **contract,
        migration_roots=find_migration_roots(backend_root),
        message_bundles=message_bundles,
        message_basename=message_basename,
    )
