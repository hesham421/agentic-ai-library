"""Plan discovery follows the DELIVERED version, never the highest v<N>/ folder.

Schema 7: the plan (and the API document) come from `scripts/gov-module.py plan MOD --json`.
Pre-v7 fallback: the registry's version (backend delivery, else current_version), searched
v<N>/ down to v1 for the latest folder that holds a plan -- a delta v<N>/ holds only what changed."""
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tests import _paths  # noqa: F401  (puts the generator root on sys.path)
import discovery


def _touch(root: Path, rel: str) -> Path:
    path = root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("**API REGISTRY**\n", encoding="utf-8")
    return path


class ExecutionPlanDiscovery(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.module_dir = Path(self._tmp.name) / "FX"
        self.module_dir.mkdir()
        self.version = None
        for name, kw in (("default_module_dir", dict(return_value=self.module_dir)),
                         ("module_plan", dict(return_value=None)),
                         ("_registry_version", dict(side_effect=lambda _m: self.version))):
            patcher = mock.patch.object(discovery, name, **kw)
            patcher.start()
            self.addCleanup(patcher.stop)
        self.addCleanup(self._tmp.cleanup)

    def test_delivered_version_wins_not_the_highest_folder(self):
        v2 = _touch(self.module_dir, "v2/P3_1/backend-execution-plan-fx.md")
        _touch(self.module_dir, "v3/P3_1/backend-execution-plan-fx.md")
        _touch(self.module_dir, "P3_1/backend-execution-plan-fx.md")
        self.version = 2
        self.assertEqual(discovery.find_execution_plan("FX"), v2)

    def test_version_order_is_numeric_not_lexical(self):
        _touch(self.module_dir, "P3_1/backend-execution-plan-fx.md")
        _touch(self.module_dir, "v2/P3_1/backend-execution-plan-fx.md")
        v10 = _touch(self.module_dir, "v10/P3_1/backend-execution-plan-fx.md")
        self.version = 10
        self.assertEqual(discovery.find_execution_plan("FX"), v10)

    def test_delta_version_without_a_plan_keeps_the_previous_plan(self):
        plan = _touch(self.module_dir, "P3_1/backend-execution-plan-fx.md")
        (self.module_dir / "v2" / "P3_1").mkdir(parents=True)
        self.version = 2
        self.assertEqual(discovery.find_execution_plan("FX"), plan)

    def test_v1_never_sees_its_own_version_subfolders(self):
        plan = _touch(self.module_dir, "P3_1/backend-execution-plan-fx.md")
        _touch(self.module_dir, "v2/P3_1/backend-execution-plan-fx.md")
        self.version = 1
        self.assertEqual(discovery.find_execution_plan("FX"), plan)

    def test_no_registry_version_reads_v1_only(self):
        plan = _touch(self.module_dir, "P3_1/backend-execution-plan-fx.md")
        _touch(self.module_dir, "v2/P3_1/backend-execution-plan-fx.md")
        self.assertEqual(discovery.find_execution_plan("FX"), plan)

    def test_shallowest_match_is_the_plan_proper(self):
        plan = _touch(self.module_dir, "P3_1/backend-execution-plan-fx.md")
        _touch(self.module_dir, "packages/backend-execution/SVC-API/backend-execution-plan-fx.md")
        self.version = 1
        self.assertEqual(discovery.find_execution_plan("FX"), plan)

    def test_no_plan_anywhere_is_none(self):
        self.assertIsNone(discovery.find_execution_plan("FX"))

    def test_gov_module_plan_wins(self):
        _touch(self.module_dir, "P3_1/backend-execution-plan-fx.md")
        delivered = _touch(self.module_dir, "v2/P3_1/backend-execution-plan-fx.md")
        with mock.patch.object(discovery, "module_plan", return_value={"exec_plan": str(delivered)}):
            self.assertEqual(discovery.find_execution_plan("FX"), delivered)


class ContractResolution(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self._tmp.cleanup)
        self.root = Path(self._tmp.name)

    def test_api_document_wins_over_the_prose_registry(self):
        spec = self.root / "api-spec-fx.yaml"
        spec.write_text("paths: {}\n", encoding="utf-8")
        plan = _touch(self.root, "P3_1/backend-execution-plan-fx.md")
        got_spec, got_plan, note = discovery.resolve_contract("FX", spec, plan)
        self.assertEqual((got_spec, got_plan), (spec, plan))
        self.assertIn("API document", note)

    def test_pre_v7_falls_back_and_says_so(self):
        plan = _touch(self.root, "P3_1/backend-execution-plan-fx.md")
        with mock.patch.object(discovery, "find_api_spec", return_value=None):
            got_spec, got_plan, note = discovery.resolve_contract("FX", None, plan)
        self.assertIsNone(got_spec)
        self.assertEqual(got_plan, plan)
        self.assertTrue(note.startswith("FALLBACK"))


if __name__ == "__main__":
    unittest.main()
