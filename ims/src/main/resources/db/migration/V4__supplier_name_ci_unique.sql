-- Uniqueness is case-insensitive and ignores surrounding whitespace.
-- Application writes are trimmed; btrim covers rows inserted outside the app.
CREATE UNIQUE INDEX supplier_name_ci_key ON supplier (lower(btrim(name)));
