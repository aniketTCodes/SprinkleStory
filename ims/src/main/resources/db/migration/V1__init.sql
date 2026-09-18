-- Flyway: V1__init.sql
-- Sprinkle Story IMS — PostgreSQL 16

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE unit (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code    TEXT NOT NULL,
    name    TEXT NOT NULL,
    CONSTRAINT unit_code_key UNIQUE (code),
    CONSTRAINT unit_name_key UNIQUE (name),
    CONSTRAINT unit_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT unit_name_not_blank CHECK (btrim(name) <> '')
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        TEXT NOT NULL,
    password_hash   TEXT NOT NULL,
    role            TEXT NOT NULL,
    status          TEXT NOT NULL,
    version         INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT users_username_key UNIQUE (username),
    CONSTRAINT users_role_chk CHECK (role IN ('ADMIN', 'EMPLOYEE')),
    CONSTRAINT users_status_chk CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT users_version_nonneg CHECK (version >= 0)
);

CREATE TABLE category (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    description     TEXT NOT NULL DEFAULT '',
    version         INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT category_name_key UNIQUE (name),
    CONSTRAINT category_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT category_version_nonneg CHECK (version >= 0)
);

CREATE TABLE sku (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unique_name     TEXT NOT NULL,
    display_name    TEXT NOT NULL,
    category_id     UUID NOT NULL REFERENCES category (id) ON DELETE RESTRICT,
    unit_id         UUID NOT NULL REFERENCES unit (id) ON DELETE RESTRICT,
    mrp             NUMERIC(12, 2) NOT NULL,
    status          TEXT NOT NULL,
    barcode         TEXT,
    version         INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT sku_unique_name_key UNIQUE (unique_name),
    CONSTRAINT sku_unique_name_not_blank CHECK (btrim(unique_name) <> ''),
    CONSTRAINT sku_display_name_not_blank CHECK (btrim(display_name) <> ''),
    CONSTRAINT sku_status_chk CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT sku_mrp_nonneg CHECK (mrp >= 0),
    CONSTRAINT sku_barcode_not_blank CHECK (barcode IS NULL OR btrim(barcode) <> ''),
    CONSTRAINT sku_barcode_key UNIQUE (barcode),
    CONSTRAINT sku_version_nonneg CHECK (version >= 0)
);

CREATE TABLE inventory_lot (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id          UUID NOT NULL REFERENCES sku (id) ON DELETE RESTRICT,
    best_before     DATE NOT NULL,
    qty             NUMERIC(12, 3) NOT NULL DEFAULT 0,
    cost_price      NUMERIC(12, 2) NOT NULL,
    selling_price   NUMERIC(12, 2) NOT NULL,
    version         INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT inventory_lot_sku_bb_key UNIQUE (sku_id, best_before),
    CONSTRAINT inventory_lot_qty_nonneg CHECK (qty >= 0),
    CONSTRAINT inventory_lot_cost_nonneg CHECK (cost_price >= 0),
    CONSTRAINT inventory_lot_sp_nonneg CHECK (selling_price >= 0),
    CONSTRAINT inventory_lot_version_nonneg CHECK (version >= 0)
);

CREATE TABLE supplier (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    contact         TEXT NOT NULL DEFAULT '',
    corp_name       TEXT NOT NULL DEFAULT '',
    poc_name        TEXT NOT NULL DEFAULT '',
    version         INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT supplier_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT supplier_version_nonneg CHECK (version >= 0)
);

CREATE TABLE purchase_order (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id         UUID NOT NULL REFERENCES supplier (id) ON DELETE RESTRICT,
    order_date          DATE NOT NULL,
    eta                 DATE NOT NULL,
    status              TEXT NOT NULL,
    received_date       DATE,
    first_received_at   TIMESTAMPTZ,
    closed_at           TIMESTAMPTZ,
    version             INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT purchase_order_status_chk CHECK (
        status IN ('DRAFT', 'ORDERED', 'PARTIAL', 'RECEIVED', 'CANCELLED')
    ),
    CONSTRAINT purchase_order_closed_chk CHECK (
        (status = 'RECEIVED' AND closed_at IS NOT NULL)
        OR (status <> 'RECEIVED' AND closed_at IS NULL)
    ),
    CONSTRAINT purchase_order_version_nonneg CHECK (version >= 0)
);

CREATE TABLE purchase_order_item (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    po_id           UUID NOT NULL REFERENCES purchase_order (id) ON DELETE CASCADE,
    sku_id          UUID NOT NULL REFERENCES sku (id) ON DELETE RESTRICT,
    qty_ordered     NUMERIC(12, 3) NOT NULL,
    qty_received    NUMERIC(12, 3) NOT NULL DEFAULT 0,
    cost_price      NUMERIC(12, 2) NOT NULL,
    version         INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT purchase_order_item_qty_ordered_pos CHECK (qty_ordered > 0),
    CONSTRAINT purchase_order_item_qty_received_nonneg CHECK (qty_received >= 0),
    CONSTRAINT purchase_order_item_qty_received_cap CHECK (qty_received <= qty_ordered),
    CONSTRAINT purchase_order_item_cost_nonneg CHECK (cost_price >= 0),
    CONSTRAINT purchase_order_item_version_nonneg CHECK (version >= 0)
);

CREATE TABLE receipt_seq (
    day         DATE PRIMARY KEY,
    last_seq    INTEGER NOT NULL,
    CONSTRAINT receipt_seq_last_seq_pos CHECK (last_seq >= 0)
);

CREATE TABLE sale (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_no      TEXT NOT NULL,
    cashier_id      UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    sold_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    total           NUMERIC(12, 2) NOT NULL,
    payment_method  TEXT NOT NULL,
    CONSTRAINT sale_receipt_no_key UNIQUE (receipt_no),
    CONSTRAINT sale_receipt_no_fmt_chk CHECK (receipt_no ~ '^SS-[0-9]{8}-[0-9]{3,}$'),
    CONSTRAINT sale_total_nonneg CHECK (total >= 0),
    CONSTRAINT sale_payment_chk CHECK (payment_method IN ('CASH', 'UPI', 'CARD', 'MIXED'))
);

CREATE TABLE sale_item (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id                 UUID NOT NULL REFERENCES sale (id) ON DELETE RESTRICT,
    sku_id                  UUID NOT NULL REFERENCES sku (id) ON DELETE RESTRICT,
    lot_id                  UUID NOT NULL REFERENCES inventory_lot (id) ON DELETE RESTRICT,
    qty                     NUMERIC(12, 3) NOT NULL,
    unit_price              NUMERIC(12, 2) NOT NULL,
    requested_best_before   DATE,
    CONSTRAINT sale_item_qty_pos CHECK (qty > 0),
    CONSTRAINT sale_item_unit_price_nonneg CHECK (unit_price >= 0)
);

CREATE TABLE stock_movement (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id      UUID NOT NULL REFERENCES inventory_lot (id) ON DELETE RESTRICT,
    type        TEXT NOT NULL,
    qty_delta   NUMERIC(12, 3) NOT NULL,
    qty_after   NUMERIC(12, 3) NOT NULL,
    ref_id      UUID NOT NULL,
    note        TEXT NOT NULL DEFAULT '',
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT stock_movement_type_chk CHECK (
        type IN (
            'SALE',
            'PO_RECEIPT',
            'ADJUST'
        )
    ),
    CONSTRAINT stock_movement_qty_after_nonneg CHECK (qty_after >= 0),
    CONSTRAINT stock_movement_delta_nonzero CHECK (qty_delta <> 0)
);