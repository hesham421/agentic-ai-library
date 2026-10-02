"""The same OpenAPI content in any key order must render byte-identical files."""
import copy
import random
import unittest

from tests._paths import ROOT  # noqa: F401
from extractors import dto_extractor, openapi_extractor, response_model_extractor
from renderers.markdown_renderer import MarkdownRenderer


def _shuffled(obj, rng, key=None):
    """Shuffles every mapping except a DTO's own `properties`, whose order the
    tool keeps on purpose (it matches the Java class); Page schemas' properties
    ARE shuffled, since that is the order springdoc varies between JVM runs."""
    if isinstance(obj, dict):
        items = list(obj.items())
        if key != "properties" or "totalElements" in obj:
            rng.shuffle(items)
        return {k: _shuffled(v, rng, k) for k, v in items}
    if isinstance(obj, list):
        return [_shuffled(v, rng, key) for v in obj]
    return obj


PAGE_PROPS = {k: {"type": "integer"} for k in
              ("totalElements", "totalPages", "size", "number", "first", "last", "empty", "numberOfElements")}
PAGE_PROPS["content"] = {"type": "array", "items": {"$ref": "#/components/schemas/Widget"}}
PAGE_PROPS["pageable"] = {"$ref": "#/components/schemas/Pageablenull"}

OPENAPI = {
    "info": {"title": "T", "version": "1"},
    "paths": {
        "/api/v1/w/{id}": {"get": {"tags": ["B"], "operationId": "get", "responses": {"200": {"description": "OK"}}},
                            "put": {"tags": ["B"], "operationId": "put", "responses": {"200": {"description": "OK"}}}},
        "/api/v1/w": {"post": {"tags": ["B"], "operationId": "create", "responses": {"200": {"description": "OK"}}}},
        "/api/v1/a": {"get": {"tags": ["A"], "operationId": "a", "responses": {"200": {"description": "OK"}}}},
    },
    "components": {"schemas": {
        "PageWidget": {"type": "object", "properties": PAGE_PROPS},
        "PageOther": {"type": "object", "properties": PAGE_PROPS},
        "Pageablenull": {"type": "object", "properties": {"pageNumber": {"type": "integer"}}},
        "Widget": {"type": "object", "properties": {"id": {"type": "integer"}, "name": {"type": "string"}}},
        "ApiResponseWidget": {"type": "object", "properties": {
            "success": {"type": "boolean"}, "data": {"$ref": "#/components/schemas/Widget"},
            "error": {"type": "object"}, "timestamp": {"type": "string"}}},
        "ApiResponsePageWidget": {"type": "object", "properties": {
            "success": {"type": "boolean"}, "data": {"$ref": "#/components/schemas/PageWidget"},
            "error": {"type": "object"}, "timestamp": {"type": "string"}}},
    }},
}


def render(openapi: dict) -> dict[str, str]:
    doc = openapi_extractor.build_document(openapi, "FX")
    doc.response_envelope = response_model_extractor.find_envelope(openapi)
    name, fields = dto_extractor.find_page_envelope(openapi)
    if name:
        from models.api_doc_model import ResponseEnvelope
        doc.pagination_envelope = ResponseEnvelope(schema_name=name, fields=fields)
    return MarkdownRenderer().render(doc)


class Determinism(unittest.TestCase):

    def test_shuffled_schema_and_path_order_renders_identically(self):
        reference = render(copy.deepcopy(OPENAPI))
        for seed in range(6):
            shuffled = _shuffled(copy.deepcopy(OPENAPI), random.Random(seed))
            self.assertEqual(render(shuffled), reference, f"seed {seed} rendered differently")

    def test_page_envelope_rows_are_sorted_by_name(self):
        _, fields = dto_extractor.find_page_envelope(OPENAPI)
        names = [f.name for f in fields]
        self.assertEqual(names, sorted(names))
        self.assertNotIn("content", names)

    def test_endpoints_sorted_by_group_path_verb(self):
        doc = openapi_extractor.build_document(OPENAPI, "FX")
        self.assertEqual([(e.group, e.path, e.method) for e in doc.endpoints], [
            ("A", "/api/v1/a", "GET"), ("B", "/api/v1/w", "POST"),
            ("B", "/api/v1/w/{id}", "GET"), ("B", "/api/v1/w/{id}", "PUT")])

    def test_per_endpoint_dto_tables_keep_declaration_order(self):
        fields = dto_extractor.schema_fields({"$ref": "#/components/schemas/Widget"}, OPENAPI["components"])
        self.assertEqual([f.name for f in fields], ["id", "name"])


if __name__ == "__main__":
    unittest.main()
