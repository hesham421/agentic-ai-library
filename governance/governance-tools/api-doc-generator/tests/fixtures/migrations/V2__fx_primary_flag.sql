/* JPA cannot express a partial index, so this lives only here. */
CREATE UNIQUE INDEX UQ_FX_WIDGET_PRIMARY
  ON FX_WIDGET (is_primary_fl)
  WHERE is_primary_fl = 1;
DROP TABLE IF EXISTS FX_LEGACY CASCADE;
