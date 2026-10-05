-- Uniqueness is case-insensitive and ignores surrounding whitespace.
-- Application writes are trimmed; btrim covers rows inserted outside the app.
ALTER TABLE category DROP CONSTRAINT category_name_key;

CREATE UNIQUE INDEX category_name_ci_key ON category (lower(btrim(name)));
