"""
Best-effort module-level error catalog discovery.

No controller anywhere in this codebase declares per-endpoint
`@ApiResponse`/`@ApiResponses` (springdoc annotations), so per-endpoint error
responses are not discoverable and must not be fabricated.

What IS consistent across every module is a `*ErrorCodes.java` class holding
`public static final String NAME = "VALUE";` constants (OrgErrorCodes,
SecurityErrorCodes, MasterDataErrorCodes, GlErrorCodes, CommonErrorCodes).
This is surfaced as the module-level "Known Error Codes" table. Which endpoint
can raise which code is business_error_extractor's job (a source walk from each
controller method), not this module's.

Each constant's own Javadoc is carried alongside it, flattened to one
paragraph: it is where a module's author states when the code is raised and
what makes it stop being raised, and that is precisely what an integrator
reading the catalogue needs and cannot get from the constant's name. Nothing
is reworded — only Javadoc markup is removed.
"""

import re
from pathlib import Path
from typing import Optional

from models.api_doc_model import ErrorCode

CONST_RE = re.compile(r'public\s+static\s+final\s+String\s+(\w+)\s*=\s*"([^"]+)"\s*;')
INLINE_TAG_RE = re.compile(r"\{@(?:code|link|linkplain|literal|value)\s+([^}]*)\}")
HTML_TAG_RE = re.compile(r"</?[a-zA-Z][^>]*>")


def _inline_tag_text(body: str) -> str:
    """`{@link #FIN_409_X}` -> "FIN_409_X"; `{@code Status.CONFLICT}` ->
    "Status.CONFLICT"; `{@link Foo#bar(X) the label}` -> "the label"."""
    parts = body.split()
    if not parts:
        return ""
    if len(parts) > 1:
        return " ".join(parts[1:])
    return parts[0].lstrip("#")


SUMMARY_BREAK_RE = re.compile(r"<p\b[^>]*>", re.IGNORECASE)


def flatten_javadoc(block: str) -> Optional[str]:
    """The SUMMARY paragraph of a Javadoc comment body (the text between /**
    and */): leading `*` stripped, inline tags reduced to the text they wrap,
    HTML dropped, and block tags (@param, @throws, @return, ...) cut off —
    those describe a signature, and a constant has none.

    Only the leading paragraph is kept — Javadoc's own convention is that it
    is the summary, and the `<p>` that follows opens the discussion behind it.
    That is a documented boundary in the text itself, not a length cut: a
    catalogue row states what the code means, and a reader who wants the
    reasoning opens the constant."""
    lines: list[str] = []
    for line in block.splitlines():
        stripped = re.sub(r"^\s*\*+", "", line).strip()
        if stripped.startswith("@"):
            break
        lines.append(stripped)
    text = " ".join(l for l in lines if l)
    text = SUMMARY_BREAK_RE.split(text, maxsplit=1)[0]
    text = INLINE_TAG_RE.sub(lambda m: _inline_tag_text(m.group(1)), text)
    text = HTML_TAG_RE.sub(" ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text or None


def javadoc_before(text: str, decl_start: int) -> Optional[str]:
    """The Javadoc block that ends immediately before `decl_start`, with only
    whitespace in between. A block separated from the declaration by anything
    else belongs to something else and is not claimed for this constant."""
    head = text[:decl_start]
    end = head.rfind("*/")
    if end == -1 or head[end + 2:].strip():
        return None
    start = head.rfind("/**", 0, end)
    if start == -1:
        return None
    return flatten_javadoc(head[start + 3:end])


DEFAULT_CODE_CLASSES = ("*ErrorCodes.java",)


def find_error_codes(source_root: Path, class_globs=DEFAULT_CODE_CLASSES) -> list[ErrorCode]:
    """class_globs: the file globs of the classes holding the module's codes
    (conventions.json -> error_code_classes; aias adds its in-process
    *RejectionCodes / *RefusalCodes classes). A file matched by two globs is
    read once."""
    codes: list[ErrorCode] = []
    paths = sorted({p for glob in class_globs for p in source_root.rglob(glob)})
    for path in paths:
        text = path.read_text(encoding="utf-8")
        try:
            rel = str(path.relative_to(source_root))
        except ValueError:
            rel = path.name
        for m in CONST_RE.finditer(text):
            name, value = m.groups()
            codes.append(ErrorCode(name=name, value=value, source_file=rel,
                                   note=javadoc_before(text, m.start())))
    return codes
