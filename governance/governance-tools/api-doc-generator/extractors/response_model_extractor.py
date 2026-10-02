"""
Derives the shared ApiResponse<T> envelope shape once per module, so it can
be documented in a single shared section instead of being repeated on every
endpoint. The envelope is identical across the whole system (success /
message / data / error / timestamp) — only "data" varies per endpoint, and
that variation is already covered by each endpoint's own Response section.

Detection: springdoc names each generic ApiResponse<T> instantiation
differently (e.g. "ApiResponseLegalEntityResponse"), so this looks for any
schema in components.schemas matching the envelope's key set rather than a
fixed name.
"""

from typing import Optional

from extractors import dto_extractor
from models.api_doc_model import ResponseEnvelope


def _generic_name(names: list[str]) -> str:
    """"ApiResponseUserResponse" + "ApiResponsePageUserResponse" -> "ApiResponse<T>".
    Falls back to the single/only name when there is no shared prefix to
    derive a generic base from -- never invents one."""
    if len(names) == 1:
        return names[0]
    prefix = names[0]
    for name in names[1:]:
        while prefix and not name.startswith(prefix):
            prefix = prefix[:-1]
    return f"{prefix}<T>" if prefix else names[0]


def find_envelope(openapi: dict) -> Optional[ResponseEnvelope]:
    components = openapi.get("components", {})
    schemas = components.get("schemas", {})

    # springdoc emits one schema per generic instantiation
    # (ApiResponseUserResponse, ApiResponsePageUserResponse, ...). Picking
    # whichever happens to come first and printing its name would document
    # the shared envelope under one arbitrary endpoint's payload type, so the
    # generic name is recovered from the instantiations' common prefix
    # instead -- derived from the real schema names, not hardcoded.
    matches = sorted(((name, schema) for name, schema in schemas.items()
                      if dto_extractor.is_envelope_shape(schema)), key=lambda m: m[0])
    if not matches:
        return None

    envelope_name = _generic_name([name for name, _ in matches])
    envelope_schema = matches[0][1]

    # Walk every declared property generically (not a fixed key subset) so any
    # envelope field beyond the five originally assumed here -- e.g.
    # correlationId -- is still documented instead of silently dropped.
    # dto_extractor.schema_fields already expands nested object fields (e.g.
    # error.fieldErrors -> FieldErrorItem's own fields) via the same recursive
    # mechanism used for request/response DTOs, so "error" needs no special
    # hand-rolled expansion here anymore.
    fields = dto_extractor.schema_fields(envelope_schema, components)
    for f in fields:
        if f.name == "data":
            f.type = "object"
            f.description = "Endpoint-specific payload — see each endpoint's Response section"
            f.nested = []
        elif f.name == "error":
            f.description = f.description or "Present only when success=false"

    return ResponseEnvelope(schema_name=envelope_name, fields=fields)
