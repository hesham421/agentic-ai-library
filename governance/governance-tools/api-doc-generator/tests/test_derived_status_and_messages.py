"""A detail row's Status comes from the aggregate throw that reaches it, a response's HTTP status from the Status the service returns, an error code's message from the i18n bundle, and a request example from the DTO's own class-level example."""
import unittest

from tests._paths import FIXTURES
from extractors import (
    business_error_extractor as bx,
    exception_extractor,
    message_bundle_extractor as mb,
    openapi_extractor,
    response_status_extractor as rs,
)
from extractors.error_mapping_extractor import http_status_label
from models.api_doc_model import ApiDocument, Endpoint

DERIVED = FIXTURES / "derived"
STATUS_HTTP = {"SUCCESS": "OK", "CREATED": "CREATED", "CONFLICT": "CONFLICT"}


class AggregateDetailStatus(unittest.TestCase):
    """D2: a domain check that RETURNS an ErrorDetail names no Status; the
    method that throws the collected list does, and it is the one that answers."""

    @classmethod
    def setUpClass(cls):
        cls.classes = bx.index_module_source(DERIVED)

    def test_detail_rows_inherit_the_aggregate_throw_status(self):
        errors, walk = bx.walk_endpoint(self.classes, "DxController", "create")
        details = {(e.code, e.status, e.kind) for e in errors if e.kind == "detail"}
        self.assertEqual(details, {
            ("DX_409_PERIOD_NOT_OPEN", "CONFLICT", "detail"),
            ("DX_409_UNBALANCED", "CONFLICT", "detail"),
        })
        self.assertIn("DxDomain.checkBalanced", walk.visited)

    def test_a_detail_with_no_aggregate_anywhere_on_the_walk_has_no_status(self):
        errors, _ = bx.walk_endpoint(self.classes, "DxController", "loose")
        self.assertEqual([(e.code, e.status) for e in errors if e.kind == "detail"],
                         [("DX_409_UNBALANCED", None)])

    def test_registered_status_is_the_last_resort(self):
        document = ApiDocument(module="DX")
        document.error_codes = exception_extractor.find_error_codes(DERIVED)
        for code in document.error_codes:
            code.status = "CONFLICT"
        ep = Endpoint(method="POST", path="/api/v1/dx/widgets/{id}/loose")
        document.endpoints = [ep]
        bx.attach_business_errors(document, self.classes, {id(ep): ("DxController", "loose")}, STATUS_HTTP)
        self.assertEqual([(be.code, be.http_status) for be in ep.business_errors],
                         [("DX_409_UNBALANCED", "409 CONFLICT")])


class ResponseStatus(unittest.TestCase):
    """D3: springdoc says 200 for every operation; the service says otherwise."""

    @classmethod
    def setUpClass(cls):
        cls.classes = bx.index_module_source(DERIVED)

    def test_created_is_read_from_the_service(self):
        self.assertEqual(
            rs.resolve_success_status(self.classes, "DxController", "create", []),
            ("CREATED", "DxService.create"))

    def test_plain_success_factory_default_is_read_from_the_wrapper(self):
        self.assertEqual(
            rs.resolve_success_status(self.classes, "DxController", "search", []),
            ("SUCCESS", "DxService.search"))

    def test_two_different_outcomes_resolve_to_nothing(self):
        self.assertIsNone(rs.resolve_success_status(self.classes, "DxController", "either", []))

    def test_only_the_generic_default_response_is_rewritten(self):
        from models.api_doc_model import ResponseVariant
        ep = Endpoint(method="POST", path="/api/v1/dx/widgets",
                      responses=[ResponseVariant(status_code="200", description="OK"),
                                 ResponseVariant(status_code="409", description="Conflict")])
        document = ApiDocument(module="DX", endpoints=[ep])
        rs.attach_response_statuses(document, self.classes, {id(ep): ("DxController", "create")},
                                    STATUS_HTTP, [], http_status_label)
        self.assertEqual([(r.status_code, r.description) for r in ep.responses],
                         [("201", "Created"), ("409", "Conflict")])
        self.assertIn("DxService.create", ep.responses[0].status_source)


class ErrorCodeMessages(unittest.TestCase):
    """D1: the wire code is also the message key, so the message is knowable."""

    def test_bundles_are_parsed_with_continuations_and_escapes(self):
        files, base = list((DERIVED / "bundles").glob("messages*.properties")), "messages"
        bundles = mb.load_bundles(files, base)
        self.assertEqual(sorted(bundles), ["", "ar"])
        self.assertEqual(bundles[""]["DX-409-UNBALANCED"], "Debits do not equal credits, totals differ")
        self.assertEqual(bundles["ar"]["DX-409-PERIOD-NOT-OPEN"], "الفترة")

    def test_messages_attach_by_value_and_absence_stays_absent(self):
        codes = exception_extractor.find_error_codes(DERIVED)
        bundles = mb.load_bundles(list((DERIVED / "bundles").glob("messages*.properties")), "messages")
        mb.attach_messages(codes, bundles)
        by_name = {c.name: c for c in codes}
        self.assertEqual(by_name["DX_409_PERIOD_NOT_OPEN"].messages,
                         {"": "The target period is not open", "ar": "الفترة"})
        # No ar entry for this one: it must stay missing, not borrow the base text.
        self.assertEqual(set(by_name["DX_409_UNBALANCED"].messages), {""})

    def test_javadoc_summary_is_carried_without_block_tags_or_discussion(self):
        codes = {c.name: c for c in exception_extractor.find_error_codes(DERIVED)}
        note = codes["DX_409_PERIOD_NOT_OPEN"].note
        self.assertIn("the rejection clears once the period is opened", note)
        self.assertNotIn("Discussion", note)
        self.assertNotIn("@deprecated", note)
        self.assertIn("DxDomain.assertPeriodOpen(...)", note)
        self.assertIsNone(codes["DX_409_UNBALANCED"].note)


class RequestExample(unittest.TestCase):
    """D4: a class-level @Schema(example=...) is a whole, owned payload."""

    def _operation(self, schema: dict) -> dict:
        return {"requestBody": {"content": {"application/json": {"schema": {"$ref": "#/components/schemas/Req"}}}}}

    def test_schema_level_example_is_carried(self):
        components = {"schemas": {"Req": {
            "type": "object",
            "description": "the whole payload's own note",
            "example": {"targets": [{"a": 1}, {"b": 2}]},
            "properties": {"targets": {"type": "array", "items": {"type": "object"}}},
        }}}
        body = openapi_extractor._request_body(self._operation(components), components)
        self.assertEqual(body.example_raw, {"targets": [{"a": 1}, {"b": 2}]})
        self.assertEqual(body.description, "the whole payload's own note")

    def test_no_schema_example_leaves_synthesis_in_charge(self):
        components = {"schemas": {"Req": {"type": "object", "properties": {"a": {"type": "string"}}}}}
        body = openapi_extractor._request_body(self._operation(components), components)
        self.assertIsNone(body.example_raw)

    def test_renderer_prefers_the_schema_example(self):
        from renderers import markdown_renderer as md
        from models.api_doc_model import FieldSpec, RequestBody
        body = RequestBody(fields=[FieldSpec(name="a", type="string", example="x", example_raw="x")],
                           example_raw={"a": "whole"})
        self.assertIn('"whole"', md._request_example(body))
        self.assertNotIn('"x"', md._request_example(body))
        body.example_raw = None
        self.assertIn('"x"', md._request_example(body))


if __name__ == "__main__":
    unittest.main()
