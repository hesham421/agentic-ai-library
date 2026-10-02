"""Both spellings of every authorization annotation must resolve; a fixture holding only the one the old regex handled would certify the bug."""
import unittest

from tests._paths import FIXTURES
from extractors import security_extractor as se

SPELLINGS = FIXTURES / "spellings"
SOURCE = (SPELLINGS / "SpellingsService.java").read_text()


class AuthorizationRuleParsing(unittest.TestCase):

    def rule(self, method: str):
        return se.find_authorization_rule(SOURCE, method)

    def test_single_line(self):
        self.assertEqual(self.rule("singleLine").constants, ["PERM_FX_SINGLE_LINE"])

    def test_two_part_concatenation(self):
        rule = self.rule("twoParts")
        self.assertEqual(rule.constants, ["PERM_FX_TWO_PARTS"])
        self.assertEqual(rule.expression,
                         "hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_FX_TWO_PARTS)")

    def test_three_line_concatenation_behind_multiline_operation(self):
        self.assertEqual(self.rule("threeLines").constants, ["PERM_FX_THREE_LINES"])

    def test_trailing_plus_style(self):
        self.assertEqual(self.rule("trailingPlus").constants, ["PERM_FX_TRAILING_PLUS"])

    def test_constant_without_perm_prefix_is_not_filtered(self):
        self.assertEqual(self.rule("noPermPrefix").constants, ["CONFIG_CREATE"])

    def test_is_authenticated_is_a_rule_but_not_a_permission(self):
        rule = self.rule("authenticatedOnly")
        self.assertEqual(rule.expression, "isAuthenticated()")
        self.assertEqual(rule.constants, [])

    def test_value_attribute_and_literal_role(self):
        rule = self.rule("valueAttribute")
        self.assertEqual(rule.expression, "hasRole('ADMIN')")
        self.assertEqual(rule.constants, ["ADMIN"])

    def test_secured_array_lists_each_authority(self):
        rule = self.rule("securedArray")
        self.assertEqual(rule.annotation, "Secured")
        self.assertEqual(rule.constants, ["ROLE_ADMIN", "ROLE_OPS"])

    def test_undeclared_method_yields_nothing_and_ignores_string_literal(self):
        self.assertIsNone(self.rule("undeclared"))

    def test_spliced_constant_is_kept_verbatim_not_dropped(self):
        src = '@PreAuthorize("hasAuthority(\'" + PERM + "\')")\npublic void m() {}'
        rule = se.find_authorization_rule(src, "m")
        self.assertIn("PERM", rule.expression)
        self.assertEqual(rule.constants, [])


class ResolvePermissionOutcomes(unittest.TestCase):

    def test_found_on_service_records_both_methods_checked(self):
        controller = ("public class FxController {\n    private final SpellingsService spellingsService;\n"
                      "    @PostMapping\n    public R twoParts() { return spellingsService.twoParts(); }\n}")
        lookup = se.resolve_permission(controller, "twoParts", SPELLINGS, controller_name="FxController")
        self.assertEqual(lookup.outcome, se.FOUND)
        self.assertEqual(lookup.source_label, "service:SpellingsService")
        self.assertEqual(lookup.checked, ["FxController.twoParts", "SpellingsService.twoParts"])

    def test_declared_none_is_distinct_from_not_extracted(self):
        controller = ("public class FxController {\n    private final SpellingsService spellingsService;\n"
                      "    @GetMapping\n    public R undeclared() { return spellingsService.undeclared(); }\n}")
        lookup = se.resolve_permission(controller, "undeclared", SPELLINGS, controller_name="FxController")
        self.assertEqual(lookup.outcome, se.DECLARED_NONE)
        self.assertNotIn(lookup.outcome, se.NOT_EXTRACTED)

    def test_two_service_calls_is_unresolved_never_guessed(self):
        controller = ("public class FxController {\n    private final SpellingsService aService;\n"
                      "    private final SpellingsService bService;\n"
                      "    @GetMapping\n    public R both() { aService.singleLine(); return bService.twoParts(); }\n}")
        lookup = se.resolve_permission(controller, "both", SPELLINGS, controller_name="FxController")
        self.assertEqual(lookup.outcome, se.DELEGATE_UNRESOLVED)
        self.assertIn(lookup.outcome, se.NOT_EXTRACTED)

    def test_missing_service_source_is_not_declared_none(self):
        controller = ("public class FxController {\n    private final GhostService ghostService;\n"
                      "    @GetMapping\n    public R m() { return ghostService.m(); }\n}")
        lookup = se.resolve_permission(controller, "m", SPELLINGS, controller_name="FxController")
        self.assertEqual(lookup.outcome, se.SERVICE_SOURCE_MISSING)


class CommentsAreNotCode(unittest.TestCase):

    def test_comment_with_public_between_mapping_and_method(self):
        import tempfile, pathlib
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            (root / "PubController.java").write_text(
                'package x;\n@RestController\n@RequestMapping("/api/v1/x/auth")\npublic class PubController {\n'
                '    private final XService xService;\n'
                '    @PostMapping("/login")\n'
                '    @SecurityRequirements // public: permitAll — the word public must not be read as a declaration\n'
                '    @Operation(summary = "Login")\n'
                '    public R login(@RequestBody Req r) { return xService.login(r); }\n}\n')
            (root / "XService.java").write_text(
                'package x;\npublic class XService {\n    /* public void login(Req r) — commented out */\n'
                '    public R login(Req r) { return null; }\n}\n')
            controller_file, method = se.find_controller_for_endpoint(root, "POST", "/api/v1/x/auth/login")
            self.assertEqual(method, "login")
            lookup = se.resolve_permission(controller_file.read_text(), method, root, controller_name="PubController")
            self.assertEqual(lookup.outcome, se.DECLARED_NONE)
            self.assertEqual(lookup.checked, ["PubController.login", "XService.login"])


class AnnotationCount(unittest.TestCase):

    def test_counts_annotations_not_javadoc_mentions(self):
        self.assertEqual(se.count_authorization_annotations(SPELLINGS), 8)


if __name__ == "__main__":
    unittest.main()
