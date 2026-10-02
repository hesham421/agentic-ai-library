"""The call walk binds only codes it reaches with certainty; ambiguity and depth cut-offs leave a code unbound rather than guessed."""
import unittest

from tests._paths import FIXTURES
from extractors import business_error_extractor as bx

WALK = FIXTURES / "walk"


def codes(errors):
    return sorted({(e.code, e.status, e.throw_site, e.kind) for e in errors})


class CallWalk(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.classes = bx.index_module_source(WALK)

    def walk(self, method, depth=3):
        return bx.walk_endpoint(self.classes, "FxController", method, max_depth=depth)

    def test_indexes_methods_with_annotated_parameters(self):
        self.assertIn("check", self.classes["FxController"].methods)
        self.assertEqual(len(self.classes["FxService"].methods["check"]), 2)

    def test_create_binds_static_domain_call_and_second_service(self):
        errors, walk = self.walk("create")
        self.assertEqual(codes(errors), [
            ("FX_400_BAD_TYPE", "VALIDATION_ERROR", "FxLookupService.assertValidCode", "thrown"),
            ("FX_400_CODE_REQUIRED", "VALIDATION_ERROR", "new FxDomain()", "thrown"),
            ("FX_409_CODE_DUP", "ALREADY_EXISTS", "FxDomain.create", "thrown"),
        ])
        self.assertEqual(walk.visited[0], "FxController.create")
        self.assertIn("FxService.create", walk.visited)

    def test_or_else_throw_lambda_in_service_body(self):
        errors, _ = self.walk("getById")
        self.assertEqual(codes(errors), [("FX_404_WIDGET", "NOT_FOUND", "FxService.getById", "thrown")])

    def test_private_helper_and_chained_static_factory(self):
        errors, _ = self.walk("deactivate")
        self.assertEqual(codes(errors), [
            ("FX_400_CODE_REQUIRED", "VALIDATION_ERROR", "new FxDomain()", "thrown"),
            ("FX_404_WIDGET", "NOT_FOUND", "FxService.requireWidget", "thrown"),
            ("FX_422_ALREADY_INACTIVE", "INVALID_STATE", "FxDomain.assertDeactivatable", "thrown"),
        ])

    def test_local_variable_receiver_details_and_depth_limit(self):
        errors, walk = self.walk("post")
        found = codes(errors)
        self.assertIn(("FX_400_NOT_POSTABLE", "VALIDATION_ERROR", "FxDomain.assertPostable", "thrown"), found)
        self.assertIn(("FX_400_NO_LINES", "VALIDATION_ERROR", "FxDomain.assertPostable", "detail"), found)
        self.assertIn(("FX_400_UNBALANCED", "VALIDATION_ERROR", "FxDomain.assertPostable", "detail"), found)
        self.assertNotIn("FX_409_TOO_DEEP", {c for c, *_ in found})
        self.assertTrue(walk.depth_limit_hit)
        self.assertIn("FxDomain.deep1", walk.visited)
        self.assertNotIn("FxDomain.deep2", walk.visited)
        deeper, deeper_walk = self.walk("post", depth=bx.MAX_DEPTH)
        self.assertIn("FX_409_TOO_DEEP", {e.code for e in deeper})
        self.assertFalse(deeper_walk.depth_limit_hit)

    def test_overloads_that_throw_differently_are_left_unresolved(self):
        errors, walk = self.walk("check")
        self.assertEqual(codes(errors), [])
        self.assertEqual(walk.unresolved, ["FxService.check (overloads differ)"])

    def test_walk_that_finds_nothing_still_shows_it_left_the_controller(self):
        errors, walk = self.walk("list")
        self.assertEqual(errors, [])
        self.assertIn("FxService.list", walk.visited)
        self.assertEqual(walk.visited[0], "FxController.list")

    def test_comment_text_is_not_a_throw_site(self):
        sites = bx.count_throw_sites(self.classes)
        self.assertNotIn("FX_IN_A_COMMENT", sites)
        self.assertEqual(sites["FX_403_INTERNAL"], ["FxWidgetApiImpl.internal"])
        self.assertEqual(sites["FX_409_TOO_DEEP"], ["FxDomain.deep2"])
        self.assertEqual(sites["FX_400_CODE_REQUIRED"], ["new FxDomain()"])

    def test_access_denied_override_with_scope(self):
        overrides = bx.find_access_denied_overrides(self.classes)
        self.assertEqual(len(overrides), 1)
        o = overrides[0]
        self.assertEqual((o.advisor, o.status, o.code, o.scope_prefix),
                         ("FxForbiddenAdvisor", "FORBIDDEN", "FX_403_FORBIDDEN", "com.erp.fx.service."))


if __name__ == "__main__":
    unittest.main()
