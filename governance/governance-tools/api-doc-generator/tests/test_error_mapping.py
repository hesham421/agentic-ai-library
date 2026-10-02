"""Both registrations of the shared Status table, both spellings of a framework code, and the fieldErrors[].field semantics read from the handler."""
import unittest

from tests._paths import FIXTURES
from extractors import error_mapping_extractor as em

COMMON = FIXTURES / "common"
LEGACY = FIXTURES / "common_legacy"


class StatusTable(unittest.TestCase):

    def test_enum_constructor_form(self):
        mapping, source = em.find_status_http_mapping_with_source([COMMON])
        self.assertEqual(source, "Status.java")
        self.assertEqual(mapping["ALREADY_EXISTS"], "CONFLICT")
        self.assertEqual(len(mapping), 6)

    def test_status_mappings_put_form(self):
        mapping, source = em.find_status_http_mapping_with_source([LEGACY])
        self.assertEqual(source, "OperationCodeImpl.java")
        self.assertEqual(mapping, {"NOT_FOUND": "NOT_FOUND", "ALREADY_EXISTS": "CONFLICT"})


class FrameworkCodes(unittest.TestCase):

    def test_constant_reference_and_string_literal_both_found(self):
        codes = {c.name: c for c in em.find_framework_error_codes([COMMON])}
        self.assertEqual(codes["VALIDATION_ERROR"].http_status, "BAD_REQUEST")
        self.assertEqual(codes["ACCESS_DENIED"].http_status, "FORBIDDEN")
        self.assertEqual(codes["DATA_INTEGRITY_VIOLATION"].http_status, "CONFLICT")
        self.assertNotIn("LocalizedException", codes)


class FieldSemantics(unittest.TestCase):

    def test_each_handler_that_sets_field_is_read_verbatim(self):
        rows = {r.handler: r for r in em.find_field_error_semantics([COMMON])}
        self.assertEqual(set(rows), {"handleLocalizedException", "handleValidation", "handleRequestParameter"})
        biz = rows["handleLocalizedException"]
        self.assertEqual(biz.field_expression, "detail.field() != null ? detail.field() : detail.errorCode()")
        self.assertEqual(biz.field_meaning,
                         "`detail.field()` when `detail.field()` is non-null, otherwise `detail.errorCode()`")
        self.assertEqual(biz.http_status, "the thrown Status's HTTP status")
        val = rows["handleValidation"]
        self.assertEqual((val.field_expression, val.http_status, val.code), ("fe.getField()", "400 BAD_REQUEST", "VALIDATION_ERROR"))
        self.assertEqual(val.exceptions, ["MethodArgumentNotValidException"])
        param = rows["handleRequestParameter"]
        self.assertEqual(param.exceptions, ["MissingServletRequestParameterException", "MethodArgumentTypeMismatchException"])
        self.assertEqual(param.field_meaning, "the value of `parameterName`")


class AuthEntryPoint(unittest.TestCase):

    def test_entry_point_resolved_through_the_wired_field_and_commence_body(self):
        entry = em.find_auth_entry_point([COMMON])
        self.assertIsNotNone(entry)
        self.assertEqual((entry.handler, entry.code, entry.value, entry.config),
                         ("FxSecurityErrorHandler", "FX_401_INVALID_CREDENTIALS", "FX-401-INVALID-CREDENTIALS", "SecurityConfig"))

    def test_no_wiring_means_no_claim(self):
        self.assertIsNone(em.find_auth_entry_point([LEGACY]))


if __name__ == "__main__":
    unittest.main()
