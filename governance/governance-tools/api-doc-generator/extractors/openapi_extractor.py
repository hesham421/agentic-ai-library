"""
Loads a springdoc-generated OpenAPI document (file path or live http(s) URL)
and walks its paths/operations into Endpoint objects.

This is the only extractor that touches the network/filesystem for the
OpenAPI document itself; every other extractor operates on the parsed dict
or (for security/exception) on Java source under an explicit --source root.
"""

import json
import urllib.request
from pathlib import Path
from typing import Optional

from extractors import dto_extractor
from models.api_doc_model import (
    ApiDocument,
    Endpoint,
    Parameter,
    RequestBody,
    ResponseVariant,
    SecurityScheme,
)

HTTP_METHODS = ("get", "post", "put", "patch", "delete")
METHOD_ORDER = {m: i for i, m in enumerate(("GET", "POST", "PUT", "PATCH", "DELETE"))}


def endpoint_sort_key(ep: Endpoint) -> tuple:
    """Group (tag), then path, then verb: a total order that owes nothing to
    the order springdoc happened to emit the paths in."""
    return (ep.group or "Ungrouped", ep.path, METHOD_ORDER.get(ep.method, 99), ep.method)


class OpenApiLoadError(Exception):
    """Raised with the URL/path actually tried and whatever the server said,
    so a failure is diagnosable without re-running curl by hand. A bare
    "HTTP Error 500" names neither which document failed nor why."""


def load_openapi(source: str) -> dict:
    if source.startswith("http://") or source.startswith("https://"):
        try:
            with urllib.request.urlopen(source, timeout=30) as resp:
                raw = resp.read().decode("utf-8")
        except urllib.error.HTTPError as exc:
            body = ""
            try:
                body = exc.read().decode("utf-8", errors="replace")[:500].strip()
            except Exception:  # noqa: BLE001 - a body is a bonus, never required
                pass
            detail = f" Response body: {body}" if body else ""
            raise OpenApiLoadError(
                f"The backend returned HTTP {exc.code} for {source}. The application is running but "
                f"could not produce this group's OpenAPI document -- this is a backend/springdoc "
                f"problem, not a documentation one; check the application log for the stack trace."
                f"{detail}"
            ) from exc
        except urllib.error.URLError as exc:
            raise OpenApiLoadError(
                f"Could not reach {source} ({exc.reason}). Start the backend, or pass --openapi "
                f"pointing at a saved OpenAPI JSON file."
            ) from exc
        try:
            return json.loads(raw)
        except json.JSONDecodeError as exc:
            raise OpenApiLoadError(
                f"{source} did not return valid JSON ({exc}). Confirm springdoc.api-docs.path and "
                f"the group id -- a wrong path is typically served as an HTML error page."
            ) from exc

    path = Path(source)
    if not path.exists():
        raise OpenApiLoadError(f"OpenAPI file not found: {path}")
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise OpenApiLoadError(f"{path} is not valid JSON ({exc}).") from exc


def _security_schemes(openapi: dict) -> list[SecurityScheme]:
    schemes = openapi.get("components", {}).get("securitySchemes", {})
    return [
        SecurityScheme(
            name=name,
            type=s.get("type"),
            scheme=s.get("scheme"),
            bearer_format=s.get("bearerFormat"),
            description=s.get("description"),
        )
        for name, s in sorted(schemes.items())
    ]


def _operation_auth(openapi: dict, operation: dict) -> tuple[Optional[bool], list[str]]:
    global_security = openapi.get("security")
    op_security = operation.get("security")
    effective = op_security if op_security is not None else global_security
    if effective is None:
        return None, []
    scheme_names = sorted({name for req in effective for name in req.keys()})
    return bool(effective), scheme_names


def _parameters(operation: dict) -> tuple[list[Parameter], list[Parameter], list[Parameter]]:
    path_params: list[Parameter] = []
    query_params: list[Parameter] = []
    header_params: list[Parameter] = []
    for p in operation.get("parameters", []):
        schema = p.get("schema", {})
        param = Parameter(
            name=p.get("name", ""),
            location=p.get("in", "query"),
            type=schema.get("type", "string"),
            required=bool(p.get("required", False)),
            description=p.get("description"),
            example=str(p["example"]) if "example" in p else None,
        )
        if param.location == "path":
            path_params.append(param)
        elif param.location == "query":
            query_params.append(param)
        elif param.location == "header":
            header_params.append(param)
    return path_params, query_params, header_params


def _request_body(operation: dict, components: dict) -> Optional[RequestBody]:
    body = operation.get("requestBody")
    if not body:
        return None
    content = body.get("content", {})
    for content_type, media in content.items():
        schema = media.get("schema", {})
        resolved, ref_name = dto_extractor.resolve_ref(schema, components)
        fields = dto_extractor.schema_fields(schema, components)
        # The media type's own example first (an @ExampleObject on the
        # operation), then the schema's (a class-level @Schema(example = ...)
        # on the request DTO). Either is a payload someone wrote and owns as a
        # WHOLE; a field-by-field synthesis is only a fallback for when
        # neither exists -- see RequestBody.example_raw.
        example = media.get("example")
        if example is None:
            example = resolved.get("example")
        return RequestBody(content_type=content_type, schema_name=ref_name, fields=fields,
                           example_raw=example, description=resolved.get("description"))
    return None


def _responses(operation: dict, components: dict) -> list[ResponseVariant]:
    variants: list[ResponseVariant] = []
    for status_code, resp in operation.get("responses", {}).items():
        content = resp.get("content", {})
        if not content:
            variants.append(ResponseVariant(status_code=status_code, description=resp.get("description")))
            continue
        for content_type, media in content.items():
            schema = media.get("schema", {})
            fields, is_array, is_page, schema_name = dto_extractor.describe_payload(schema, components)
            variants.append(ResponseVariant(
                status_code=status_code,
                description=resp.get("description"),
                content_type=content_type,
                schema_name=schema_name,
                fields=fields,
                is_array=is_array,
                is_page=is_page,
            ))
    return variants


def build_document(openapi: dict, module: str) -> ApiDocument:
    components = openapi.get("components", {})
    info = openapi.get("info", {})

    doc = ApiDocument(
        module=module,
        title=info.get("title"),
        description=info.get("description"),
        version=info.get("version"),
        servers=[s.get("url", "") for s in openapi.get("servers", [])],
        security_schemes=_security_schemes(openapi),
    )

    for path, methods in openapi.get("paths", {}).items():
        for method, operation in methods.items():
            if method.lower() not in HTTP_METHODS:
                continue
            path_params, query_params, header_params = _parameters(operation)
            requires_auth, scheme_names = _operation_auth(openapi, operation)
            tags = operation.get("tags") or []

            doc.endpoints.append(Endpoint(
                method=method.upper(),
                path=path,
                group=tags[0] if tags else None,
                operation_id=operation.get("operationId"),
                summary=operation.get("summary"),
                description=operation.get("description"),
                path_params=path_params,
                query_params=query_params,
                header_params=header_params,
                request_body=_request_body(operation, components),
                responses=_responses(operation, components),
                requires_auth=requires_auth,
                security_schemes=scheme_names,
            ))

    doc.endpoints.sort(key=endpoint_sort_key)
    return doc
