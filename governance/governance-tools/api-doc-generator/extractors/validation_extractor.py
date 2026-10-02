"""
Turns one already-resolved OpenAPI schema property into a FieldSpec.

springdoc-openapi already translates Bean Validation annotations into plain
JSON Schema keywords by the time they reach the OpenAPI document:
  @NotBlank / @NotNull  -> listed in the parent schema's "required" array
  @Size(min, max)        -> "minLength" / "maxLength"
  @Pattern(regexp)        -> "pattern"
  @Schema(description=…, example=…) -> "description" / "example"

This module does not talk to Java source at all — everything here comes
from the OpenAPI JSON that's already been resolved by dto_extractor.
"""

import json
import re
from typing import Optional

from models.api_doc_model import FieldSpec

# springdoc names the schema for a type it could not fully resolve a
# nullability wrapper for by appending "null" (e.g. Spring Data's Pageable ->
# "Pageablenull", Sort -> "Sortnull"). That suffix is a generator artefact,
# not part of any type a frontend or test will ever see on the wire, so it is
# stripped for DISPLAY only -- the original name is still what $ref lookups
# use, so nothing downstream stops resolving.
_SPRINGDOC_NULL_SUFFIX_RE = re.compile(r"^([A-Z]\w*?)null$")


def display_type_name(name: Optional[str]) -> Optional[str]:
    if not name:
        return name
    m = _SPRINGDOC_NULL_SUFFIX_RE.match(name)
    return m.group(1) if m else name


def _type_label(resolved: dict, ref_name: Optional[str], is_array: bool, item_type: Optional[str]) -> str:
    if is_array:
        return f"array<{display_type_name(item_type) or 'object'}>"
    if ref_name:
        return display_type_name(ref_name)
    t = resolved.get("type", "object")
    fmt = resolved.get("format")
    return f"{t} ({fmt})" if fmt else t


def _stringify(value) -> Optional[str]:
    """Display form of an example. A non-string JSON value is rendered as the
    JSON literal it actually is -- Python's str() would turn `true` into
    "True" and `null` into "None", neither of which is valid JSON, and that
    string was previously what ended up in the copy-pasteable example blocks."""
    if value is None:
        return None
    if isinstance(value, str):
        return value
    try:
        return json.dumps(value, ensure_ascii=False)
    except (TypeError, ValueError):
        return str(value)


def build_field_spec(name: str, prop: dict, required: bool, resolve_ref) -> FieldSpec:
    """resolve_ref: callable(schema_dict) -> (resolved_dict, ref_name_or_None), supplied by
    dto_extractor so this module never has to know how $ref dereferencing works."""
    resolved, ref_name = resolve_ref(prop)

    raw_type = resolved.get("type", "object" if ref_name else "string")
    is_array = raw_type == "array"
    item_type = None
    if is_array:
        items = resolved.get("items", {})
        item_resolved, item_ref = resolve_ref(items)
        item_type = item_ref or item_resolved.get("type", "object")

    return FieldSpec(
        name=name,
        type=_type_label(resolved, ref_name, is_array, item_type),
        required=required,
        description=resolved.get("description"),
        example=_stringify(resolved.get("example")),
        example_raw=resolved.get("example"),
        min_length=resolved.get("minLength"),
        max_length=resolved.get("maxLength"),
        pattern=resolved.get("pattern"),
        format=resolved.get("format"),
        enum=list(resolved.get("enum") or []),
        is_array=is_array,
        item_type=item_type,
    )
