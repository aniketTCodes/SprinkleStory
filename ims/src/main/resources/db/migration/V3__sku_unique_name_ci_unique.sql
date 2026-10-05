ALTER TABLE sku DROP CONSTRAINT sku_unique_name_key;

CREATE UNIQUE INDEX sku_unique_name_ci_key ON sku (lower(btrim(unique_name)));
