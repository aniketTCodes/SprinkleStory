ALTER TABLE sku RENAME COLUMN unique_name TO product_code;

DROP INDEX IF EXISTS sku_unique_name_ci_key;

ALTER TABLE sku DROP CONSTRAINT IF EXISTS sku_unique_name_not_blank;

CREATE SEQUENCE sku_product_code_seq;

WITH numbered AS (
    SELECT id, row_number() OVER (ORDER BY id) AS n
    FROM sku
)
UPDATE sku
SET product_code = 'SS-' || lpad(numbered.n::text, 5, '0')
FROM numbered
WHERE sku.id = numbered.id;

SELECT setval(
    'sku_product_code_seq',
    GREATEST((SELECT COUNT(*)::bigint FROM sku), 1),
    (SELECT COUNT(*) FROM sku) > 0
);

ALTER TABLE sku ADD CONSTRAINT sku_product_code_key UNIQUE (product_code);

ALTER TABLE sku ADD CONSTRAINT sku_product_code_fmt_chk CHECK (product_code ~ '^SS-[0-9]{5}$');
