"""Uniqueness invariants: entity and migration declarations merged, partial-index scope verbatim, drops replayed in version order, foreign tables ignored."""
import unittest

from tests._paths import FIXTURES
from extractors import constraint_extractor as cx


class UniqueConstraints(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.rows = {r.name: r for r in cx.find_unique_constraints(FIXTURES / "entities", [FIXTURES / "migrations"])}

    def test_only_live_constraints_on_module_tables(self):
        self.assertEqual(set(self.rows), {"UQ_FX_WIDGET_CODE", "UQ_FX_WIDGET_NAME", "UQ_FX_WIDGET_PRIMARY"})

    def test_entity_and_migration_agree_cite_both(self):
        row = self.rows["UQ_FX_WIDGET_CODE"]
        self.assertEqual(row.sources, ["FxWidget.java", "V1__fx_schema.sql"])
        self.assertEqual(row.entity_fields, ["code"])
        self.assertEqual(row.scope, "all rows")
        self.assertIsNone(row.note)

    def test_partial_index_scope_is_the_where_clause_verbatim(self):
        row = self.rows["UQ_FX_WIDGET_PRIMARY"]
        self.assertEqual(row.scope, "WHERE is_primary_fl = 1")
        self.assertEqual(row.sources, ["V2__fx_primary_flag.sql"])
        self.assertEqual(row.entity_fields, ["isPrimaryFl"])
        self.assertEqual(row.entity, "FxWidget")

    def test_disagreement_is_reported_not_resolved(self):
        row = self.rows["UQ_FX_WIDGET_NAME"]
        self.assertIn("entity declares (NAME) but V1__fx_schema.sql declares (name_en)", row.note)
        self.assertEqual(row.entity_fields, [""])

    def test_version_order_is_numeric_not_lexical(self):
        files = [p.name for p in cx._migration_files([FIXTURES / "migrations"])]
        self.assertEqual(files, ["V1__fx_schema.sql", "V2__fx_primary_flag.sql", "V10__fx_drop_tmp.sql"])


if __name__ == "__main__":
    unittest.main()
