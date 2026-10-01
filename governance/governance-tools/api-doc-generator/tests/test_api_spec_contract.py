"""Contract ids come from the module's API document (OpenAPI 3.1, x-api-id) -- parsed without a YAML library."""
import unittest

from tests import _paths  # noqa: F401
from extractors import contract_extractor
from models.api_doc_model import ApiDocument

SPEC = """\
# derived by the factory
openapi: 3.1.0
info:
  title: FX
  description: >-
    paths: is mentioned in prose and must not open the block
paths:
  /v1/widgets:
    post:
      summary: API-FX-001 — Create a widget
      x-api-id: API-FX-001
      responses:
        '201':
          description: created
    get:
      x-api-id: API-FX-002
      x-paginated: true
  '/v1/widgets/{id}':
    get:
      operationId: read
      x-api-id: "API-FX-003"
    parameters:
      - name: id
        in: path
  /v1/untracked:
    delete:
      summary: no id here
components:
  schemas:
    Widget:
      x-api-id: NOT-AN-OPERATION
"""


class _Ep:
    def __init__(self, method, path):
        self.method, self.path, self.api_id = method, path, None


class ApiSpecParsing(unittest.TestCase):

    def test_operations_with_an_id_are_the_contract(self):
        got = [(e.api_id, e.method, e.path) for e in contract_extractor.parse_api_spec(SPEC)]
        self.assertEqual(got, [("API-FX-001", "POST", "/v1/widgets"),
                               ("API-FX-002", "GET", "/v1/widgets"),
                               ("API-FX-003", "GET", "/v1/widgets/{id}")])

    def test_summary_is_the_operation_label(self):
        first = contract_extractor.parse_api_spec(SPEC)[0]
        self.assertEqual(first.operation, "API-FX-001 — Create a widget")

    def test_served_prefix_matches_by_trailing_segments(self):
        doc = ApiDocument.__new__(ApiDocument)
        doc.endpoints = [_Ep("POST", "/api/v1/widgets"), _Ep("GET", "/api/v1/widgets/{widgetId}"),
                         _Ep("GET", "/api/v1/other")]
        doc.contract_drift, doc.contract_source, doc.contract_kind = [], None, None
        contract_extractor.attach_contract_ids(doc, contract_extractor.parse_api_spec(SPEC), "api-spec-fx.yaml",
                                               kind="api-spec")
        self.assertEqual([e.api_id for e in doc.endpoints], ["API-FX-001", "API-FX-003", None])
        kinds = sorted((d.kind, d.api_id) for d in doc.contract_drift)
        self.assertEqual(kinds, [("undeclared", None), ("unimplemented", "API-FX-002")])
        self.assertIn("API document", doc.contract_drift[0].detail)


if __name__ == "__main__":
    unittest.main()
