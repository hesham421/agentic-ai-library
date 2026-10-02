"""
Best-effort discovery of request-side pagination constraints (default page,
default/min/max page size, max page number) -- never visible in the OpenAPI
document, since Bean Validation annotations aren't used for these (they're
plain Java field defaults and hand-enforced Math.min/max logic), and only
discoverable from shared/common source.

This platform's common-utils module has been observed to contain more than
one pagination-limit utility class with the same-shaped constants (e.g.
PageableUtils vs. PageableBuilder) -- so finding a constant by name alone
isn't enough to safely attribute it to a given module. Before reporting a
max/min-size constant, this module verifies the target module's own source
literally imports the class that declares it. If more than one candidate
class is found and the module doesn't clearly import exactly one of them,
nothing is reported for that constraint -- never guessed.

Page/size *defaults* are handled separately: they come from
BaseSearchContractRequest, the one shared base class every search request DTO
extends (confirmed by import, not assumed by name), so there's no equivalent
ambiguity to resolve there.
"""

import re
from pathlib import Path
from typing import Optional

from models.api_doc_model import PaginationConstraints

BLOCK_COMMENT_RE = re.compile(r"/\*.*?\*/", re.DOTALL)
LINE_COMMENT_RE = re.compile(r"//[^\n]*")


def _read_stripped(path: Path) -> str:
    """Reads a .java file with block/line comments removed. Javadoc routinely
    embeds example code (e.g. "public class YourModuleApplication { }" as a
    usage sample) that would otherwise be mistaken for the file's own real
    declaration by a plain regex -- stripping comments first is what makes
    every regex below safe to run directly against the remaining source."""
    text = path.read_text(encoding="utf-8")
    text = BLOCK_COMMENT_RE.sub("", text)
    text = LINE_COMMENT_RE.sub("", text)
    return text


PACKAGE_RE = re.compile(r"^package\s+([\w.]+)\s*;", re.MULTILINE)
CLASS_NAME_RE = re.compile(r"\bpublic\s+(?:final\s+)?class\s+(\w+)")

DEFAULT_SIZE_CONST_RE = re.compile(r"DEFAULT_PAGE_SIZE\s*=\s*([\d_]+)")
MAX_SIZE_CONST_RE = re.compile(r"MAX_PAGE_SIZE\s*=\s*([\d_]+)")
MIN_SIZE_CONST_RE = re.compile(r"MIN_PAGE_SIZE\s*=\s*([\d_]+)")
MAX_PAGE_NUMBER_CONST_RE = re.compile(r"MAX_PAGE_NUMBER\s*=\s*([\d_]+)")

# The class is identified by NAME, not by "any class declaring page/size
# defaults": this platform's common module also has an internal, service-layer
# SearchRequest with the same coincidental defaults that is never bound
# directly from a frontend request, so a shape-only match would cite the wrong
# source. An earlier shape test looked for nested ContractFilter/ContractSort
# types that this platform no longer declares (its filter type is SearchFilter),
# which silently reported no pagination defaults at all.
SEARCH_CONTRACT_FILE = "BaseSearchContractRequest.java"

DEFAULT_PAGE_FIELD_RE = re.compile(r"private\s+int\s+page\s*=\s*(\d+)\s*;")
DEFAULT_SIZE_FIELD_RE = re.compile(r"private\s+int\s+size\s*=\s*(\d+)\s*;")


def _int(s: Optional[str]) -> Optional[int]:
    return int(s.replace("_", "")) if s else None


def _fqcn(text: str) -> Optional[str]:
    pkg_m = PACKAGE_RE.search(text)
    cls_m = CLASS_NAME_RE.search(text)
    if not pkg_m or not cls_m:
        return None
    return f"{pkg_m.group(1)}.{cls_m.group(1)}"


def _module_imports(source_root: Path, fqcn: str) -> bool:
    pattern = re.compile(rf"import\s+{re.escape(fqcn)}\s*;")
    for path in source_root.rglob("*.java"):
        if pattern.search(_read_stripped(path)):
            return True
    return False


def _find_page_size_defaults(common_source_roots: list[Path]) -> tuple[Optional[int], Optional[int], Optional[str]]:
    """Reads the page/size defaults off BaseSearchContractRequest, the shared
    base class every search request DTO extends. See SEARCH_CONTRACT_FILE for
    why it is located by name."""
    for root in common_source_roots:
        for path in sorted(root.rglob(SEARCH_CONTRACT_FILE)):
            text = _read_stripped(path)
            page_m = DEFAULT_PAGE_FIELD_RE.search(text)
            size_m = DEFAULT_SIZE_FIELD_RE.search(text)
            if page_m and size_m:
                return _int(page_m.group(1)), _int(size_m.group(1)), str(path.relative_to(root))
    return None, None, None


def _find_size_limit_candidates(common_source_roots: list[Path]) -> list[tuple[Optional[str], PaginationConstraints]]:
    candidates = []
    for root in common_source_roots:
        for path in sorted(root.rglob("*.java")):
            text = _read_stripped(path)
            max_m = MAX_SIZE_CONST_RE.search(text)
            if not max_m:
                continue
            constraints = PaginationConstraints(
                max_size=_int(max_m.group(1)),
                min_size=_int((MIN_SIZE_CONST_RE.search(text) or [None, None]).group(1)) if MIN_SIZE_CONST_RE.search(text) else None,
                max_page_number=_int(MAX_PAGE_NUMBER_CONST_RE.search(text).group(1)) if MAX_PAGE_NUMBER_CONST_RE.search(text) else None,
                source_file=str(path.relative_to(root)),
            )
            candidates.append((_fqcn(text), constraints))
    return candidates


def find_pagination_constraints(
    source_root: Optional[Path],
    common_source_roots: list[Path],
) -> Optional[PaginationConstraints]:
    default_page, default_size, defaults_source = _find_page_size_defaults(common_source_roots)
    candidates = _find_size_limit_candidates(common_source_roots)

    limits: Optional[PaginationConstraints] = None
    if len(candidates) == 1:
        limits = candidates[0][1]
    elif len(candidates) > 1 and source_root is not None:
        matches = [c for fqcn, c in candidates if fqcn and _module_imports(source_root, fqcn)]
        if len(matches) == 1:
            limits = matches[0]
        # more than one match, or none confidently attributable -- say nothing

    if default_page is None and limits is None:
        return None

    result = limits or PaginationConstraints()
    result.default_page = default_page
    result.default_size = default_size
    if defaults_source and not result.source_file:
        result.source_file = defaults_source
    return result
