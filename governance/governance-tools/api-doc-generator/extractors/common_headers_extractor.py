"""
Best-effort discovery of headers applied globally by shared servlet filters,
e.g. a correlation/trace-ID header. These are structurally invisible in the
OpenAPI document -- a Filter runs outside Spring MVC's request-mapping layer,
so springdoc never introspects it, unlike an @RequestHeader method parameter
(which openapi_extractor already captures straight from the OpenAPI JSON).
Source is the only place this fact exists at all.

Generic by construction: matches any OncePerRequestFilter/HttpFilter subclass
declaring a header-name constant, by shape, not by any specific class name --
works for whatever shared filters this platform has today or adds later.
"""

import re
from pathlib import Path

from models.api_doc_model import Parameter

FILTER_CLASS_RE = re.compile(r"class\s+\w+\s+extends\s+(?:OncePerRequestFilter|HttpFilter)\b")
HEADER_CONST_RE = re.compile(r'private\s+static\s+final\s+String\s+\w*HEADER\w*\s*=\s*"([^"]+)"', re.IGNORECASE)


def find_common_headers(common_source_roots: list[Path]) -> list[Parameter]:
    headers: list[Parameter] = []
    seen: set[str] = set()
    for root in common_source_roots:
        for path in sorted(root.rglob("*.java")):
            text = path.read_text(encoding="utf-8")
            if not FILTER_CLASS_RE.search(text):
                continue
            for m in HEADER_CONST_RE.finditer(text):
                name = m.group(1)
                if name in seen:
                    continue
                seen.add(name)
                headers.append(Parameter(
                    name=name,
                    location="header",
                    required=False,
                    description=f"Applied globally by {path.stem} — present on every request/response, not endpoint-specific.",
                ))
    return headers
