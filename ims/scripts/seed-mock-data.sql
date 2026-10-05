-- Mock data for Sprinkle Story IMS API tests.
-- Safe to re-run: truncates business tables first.
-- All users use password "password" (bcrypt hash below). Schema has no auth yet.
--
-- Apply:
--   docker exec -i my-postgres psql -U anikettcodes -d ims < scripts/seed-mock-data.sql
--   or:  .\scripts\seed-mock-data.ps1

BEGIN;

TRUNCATE TABLE
    stock_movement,
    sale_item,
    sale,
    receipt_seq,
    purchase_order_item,
    purchase_order,
    inventory_lot,
    sku,
    supplier,
    category,
    unit,
    users
    RESTART IDENTITY CASCADE;

-- bcrypt of "password"
INSERT INTO users (username, password_hash, role, status) VALUES
    ('admin',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN',    'ACTIVE'),
    ('priya',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'EMPLOYEE', 'ACTIVE'),
    ('rahul',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'EMPLOYEE', 'ACTIVE'),
    ('disabled',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'EMPLOYEE', 'DISABLED');

INSERT INTO unit (code, name) VALUES
    ('PCS',  'Piece'),
    ('BOX',  'Box'),
    ('KG',   'Kilogram'),
    ('G',    'Gram'),
    ('L',    'Litre');

INSERT INTO category (name, description) VALUES
    ('Cakes',              'Whole cakes and slices'),
    ('Cupcakes',           'Single-serve frosted cupcakes'),
    ('Cookies',            'Baked cookies and biscuit packs'),
    ('Ice Cream',          'Scoops, tubs, and novelty ice cream'),
    ('Beverages',          'Cold drinks, shakes, and coffee'),
    ('Sprinkles & Toppings','Decorations sold by weight');

INSERT INTO sku (product_code, display_name, category_id, unit_id, mrp, status, barcode) VALUES
    ('SS-00001',       'Vanilla Celebration Cake 1kg',
        (SELECT id FROM category WHERE name = 'Cakes'),
        (SELECT id FROM unit WHERE code = 'PCS'),
        899.00, 'ACTIVE',   '8900000000011'),
    ('SS-00002',         'Chocolate Fudge Cake 500g',
        (SELECT id FROM category WHERE name = 'Cakes'),
        (SELECT id FROM unit WHERE code = 'PCS'),
        549.00, 'ACTIVE',   '8900000000028'),
    ('SS-00003',      'Red Velvet Cupcake',
        (SELECT id FROM category WHERE name = 'Cupcakes'),
        (SELECT id FROM unit WHERE code = 'PCS'),
        89.00,  'ACTIVE',   '8900000000035'),
    ('SS-00004',        'Vanilla Sprinkle Cupcake',
        (SELECT id FROM category WHERE name = 'Cupcakes'),
        (SELECT id FROM unit WHERE code = 'PCS'),
        69.00,  'ACTIVE',   '8900000000042'),
    ('SS-00005',   'Chocolate Chip Cookies (Box of 6)',
        (SELECT id FROM category WHERE name = 'Cookies'),
        (SELECT id FROM unit WHERE code = 'BOX'),
        199.00, 'ACTIVE',   '8900000000059'),
    ('SS-00006',   'Alphonso Mango Ice Cream 500ml',
        (SELECT id FROM category WHERE name = 'Ice Cream'),
        (SELECT id FROM unit WHERE code = 'PCS'),
        249.00, 'ACTIVE',   '8900000000066'),
    ('SS-00007',   'Cold Coffee 300ml',
        (SELECT id FROM category WHERE name = 'Beverages'),
        (SELECT id FROM unit WHERE code = 'PCS'),
        149.00, 'ACTIVE',   '8900000000073'),
    ('SS-00008',   'Rainbow Sprinkles 100g',
        (SELECT id FROM category WHERE name = 'Sprinkles & Toppings'),
        (SELECT id FROM unit WHERE code = 'G'),
        79.00,  'ACTIVE',   '8900000000080'),
    ('SS-00009',  'Seasonal Rose Cake (Retired)',
        (SELECT id FROM category WHERE name = 'Cakes'),
        (SELECT id FROM unit WHERE code = 'PCS'),
        999.00, 'INACTIVE', '8900000000097');

SELECT setval('sku_product_code_seq', 9, true);

INSERT INTO supplier (name, contact, corp_name, poc_name) VALUES
    ('Sweet Mills Distributors', '9876543210', 'Sweet Mills Pvt Ltd',    'Anita Desai'),
    ('Frost & Co',               '9123456780', 'Frost & Co Foods',       'Vikram Shah'),
    ('Dairy Fresh',              '9988776655', 'Dairy Fresh India Ltd',  'Meera Iyer');

INSERT INTO inventory_lot (sku_id, best_before, qty, cost_price, selling_price) VALUES
    ((SELECT id FROM sku WHERE product_code = 'SS-00001'),     DATE '2026-10-15',  8.000,  420.00, 849.00),
    ((SELECT id FROM sku WHERE product_code = 'SS-00002'),       DATE '2026-10-10', 12.000,  260.00, 499.00),
    ((SELECT id FROM sku WHERE product_code = 'SS-00003'),    DATE '2026-09-28', 34.000,   28.00,  85.00),
    ((SELECT id FROM sku WHERE product_code = 'SS-00004'),      DATE '2026-09-27', 18.000,   22.00,  65.00),
    ((SELECT id FROM sku WHERE product_code = 'SS-00005'), DATE '2026-11-01', 19.000,   95.00, 189.00),
    ((SELECT id FROM sku WHERE product_code = 'SS-00006'), DATE '2026-12-31', 15.000,  110.00, 239.00),
    ((SELECT id FROM sku WHERE product_code = 'SS-00007'), DATE '2026-09-25', 24.000,   45.00, 139.00),
    ((SELECT id FROM sku WHERE product_code = 'SS-00008'), DATE '2027-03-01', 40.000,   18.00,  75.00),
    -- second lot for FEFO / multi-lot sale tests
    ((SELECT id FROM sku WHERE product_code = 'SS-00004'),      DATE '2026-10-05', 40.000,   22.00,  65.00);

INSERT INTO purchase_order (
    supplier_id, order_date, eta, status, received_date, first_received_at, closed_at
) VALUES
    (
        (SELECT id FROM supplier WHERE name = 'Sweet Mills Distributors'),
        DATE '2026-09-01', DATE '2026-09-05', 'RECEIVED',
        DATE '2026-09-04', TIMESTAMPTZ '2026-09-04 09:15:00+05:30', TIMESTAMPTZ '2026-09-04 11:40:00+05:30'
    ),
    (
        (SELECT id FROM supplier WHERE name = 'Frost & Co'),
        DATE '2026-09-12', DATE '2026-09-18', 'PARTIAL',
        DATE '2026-09-16', TIMESTAMPTZ '2026-09-16 14:00:00+05:30', NULL
    ),
    (
        (SELECT id FROM supplier WHERE name = 'Dairy Fresh'),
        DATE '2026-09-18', DATE '2026-09-22', 'ORDERED',
        NULL, NULL, NULL
    ),
    (
        (SELECT id FROM supplier WHERE name = 'Frost & Co'),
        DATE '2026-09-19', DATE '2026-09-26', 'DRAFT',
        NULL, NULL, NULL
    ),
    (
        (SELECT id FROM supplier WHERE name = 'Sweet Mills Distributors'),
        DATE '2026-08-20', DATE '2026-08-25', 'CANCELLED',
        NULL, NULL, NULL
    );

INSERT INTO purchase_order_item (po_id, sku_id, qty_ordered, qty_received, cost_price)
SELECT po.id, sku.id, v.qty_ordered, v.qty_received, v.cost_price
FROM (VALUES
    (DATE '2026-09-01', 'SS-00001',     10.000, 10.000, 420.00),
    (DATE '2026-09-01', 'SS-00002',       12.000, 12.000, 260.00),
    (DATE '2026-09-01', 'SS-00004',      60.000, 60.000,  22.00),
    (DATE '2026-09-12', 'SS-00003',    48.000, 36.000,  28.00),
    (DATE '2026-09-12', 'SS-00005', 24.000, 20.000,  95.00),
    (DATE '2026-09-18', 'SS-00006', 20.000,  0.000, 110.00),
    (DATE '2026-09-18', 'SS-00007', 30.000,  0.000,  45.00),
    (DATE '2026-09-19', 'SS-00008', 50.000,  0.000,  18.00),
    (DATE '2026-08-20', 'SS-00009', 6.000,  0.000, 450.00)
) AS v(order_date, sku_name, qty_ordered, qty_received, cost_price)
JOIN purchase_order po ON po.order_date = v.order_date
JOIN sku ON sku.product_code = v.sku_name;

INSERT INTO receipt_seq (day, last_seq) VALUES
    (DATE '2026-09-18', 2),
    (DATE '2026-09-19', 1);

INSERT INTO sale (receipt_no, cashier_id, sold_at, total, payment_method) VALUES
    ('SS-20260918-001',
        (SELECT id FROM users WHERE username = 'priya'),
        TIMESTAMPTZ '2026-09-18 11:22:00+05:30',
        219.00, 'UPI'),
    ('SS-20260918-002',
        (SELECT id FROM users WHERE username = 'rahul'),
        TIMESTAMPTZ '2026-09-18 16:05:00+05:30',
        849.00, 'CARD'),
    ('SS-20260919-001',
        (SELECT id FROM users WHERE username = 'priya'),
        TIMESTAMPTZ '2026-09-19 10:40:00+05:30',
        278.00, 'CASH');

INSERT INTO sale_item (sale_id, sku_id, lot_id, qty, unit_price, requested_best_before)
SELECT s.id, sku.id, lot.id, v.qty, v.unit_price, v.requested_best_before
FROM (VALUES
    ('SS-20260918-001', 'SS-00004',      DATE '2026-09-27', 2.000,  65.00, DATE '2026-09-27'),
    ('SS-20260918-001', 'SS-00005', DATE '2026-11-01', 1.000, 189.00, NULL),
    ('SS-20260918-002', 'SS-00001',     DATE '2026-10-15', 1.000, 849.00, DATE '2026-10-15'),
    ('SS-20260919-001', 'SS-00003',    DATE '2026-09-28', 2.000,  85.00, NULL),
    ('SS-20260919-001', 'SS-00007', DATE '2026-09-25', 1.000, 139.00, NULL)
) AS v(receipt_no, sku_name, lot_bb, qty, unit_price, requested_best_before)
JOIN sale s ON s.receipt_no = v.receipt_no
JOIN sku ON sku.product_code = v.sku_name
JOIN inventory_lot lot ON lot.sku_id = sku.id AND lot.best_before = v.lot_bb;

-- Ledger-style movements aligned with remaining lot qty.
INSERT INTO stock_movement (lot_id, type, qty_delta, qty_after, ref_id, note, user_id, at)
SELECT lot.id, v.type, v.qty_delta, v.qty_after, v.ref_id, v.note, u.id, v.at
FROM (VALUES
    ('SS-00004', DATE '2026-09-27', 'PO_RECEIPT',  20.000, 20.000,
        (SELECT id FROM purchase_order WHERE order_date = DATE '2026-09-01'),
        'Received from Sweet Mills', 'admin',
        TIMESTAMPTZ '2026-09-04 09:20:00+05:30'),
    ('SS-00004', DATE '2026-09-27', 'SALE',        -2.000, 18.000,
        (SELECT id FROM sale WHERE receipt_no = 'SS-20260918-001'),
        'POS sale', 'priya',
        TIMESTAMPTZ '2026-09-18 11:22:00+05:30'),
    ('SS-00001', DATE '2026-10-15', 'PO_RECEIPT', 10.000, 10.000,
        (SELECT id FROM purchase_order WHERE order_date = DATE '2026-09-01'),
        'Received from Sweet Mills', 'admin',
        TIMESTAMPTZ '2026-09-04 09:25:00+05:30'),
    ('SS-00001', DATE '2026-10-15', 'ADJUST',     -1.000,  9.000,
        (SELECT id FROM users WHERE username = 'admin'),
        'Damaged in display fridge', 'admin',
        TIMESTAMPTZ '2026-09-10 18:00:00+05:30'),
    ('SS-00001', DATE '2026-10-15', 'SALE',       -1.000,  8.000,
        (SELECT id FROM sale WHERE receipt_no = 'SS-20260918-002'),
        'POS sale', 'rahul',
        TIMESTAMPTZ '2026-09-18 16:05:00+05:30'),
    ('SS-00003', DATE '2026-09-28', 'PO_RECEIPT', 36.000, 36.000,
        (SELECT id FROM purchase_order WHERE order_date = DATE '2026-09-12'),
        'Partial receipt Frost & Co', 'admin',
        TIMESTAMPTZ '2026-09-16 14:05:00+05:30'),
    ('SS-00003', DATE '2026-09-28', 'SALE',       -2.000, 34.000,
        (SELECT id FROM sale WHERE receipt_no = 'SS-20260919-001'),
        'POS sale', 'priya',
        TIMESTAMPTZ '2026-09-19 10:40:00+05:30'),
    ('SS-00005', DATE '2026-11-01', 'PO_RECEIPT', 20.000, 20.000,
        (SELECT id FROM purchase_order WHERE order_date = DATE '2026-09-12'),
        'Partial receipt Frost & Co', 'admin',
        TIMESTAMPTZ '2026-09-16 14:10:00+05:30'),
    ('SS-00005', DATE '2026-11-01', 'SALE',       -1.000, 19.000,
        (SELECT id FROM sale WHERE receipt_no = 'SS-20260918-001'),
        'POS sale', 'priya',
        TIMESTAMPTZ '2026-09-18 11:22:00+05:30'),
    ('SS-00007', DATE '2026-09-25', 'ADJUST',     25.000, 25.000,
        (SELECT id FROM users WHERE username = 'admin'),
        'Opening stock', 'admin',
        TIMESTAMPTZ '2026-09-15 08:00:00+05:30'),
    ('SS-00007', DATE '2026-09-25', 'SALE',       -1.000, 24.000,
        (SELECT id FROM sale WHERE receipt_no = 'SS-20260919-001'),
        'POS sale', 'priya',
        TIMESTAMPTZ '2026-09-19 10:40:00+05:30')
) AS v(sku_name, lot_bb, type, qty_delta, qty_after, ref_id, note, username, at)
JOIN sku ON sku.product_code = v.sku_name
JOIN inventory_lot lot ON lot.sku_id = sku.id AND lot.best_before = v.lot_bb
JOIN users u ON u.username = v.username;

COMMIT;

SELECT 'users' AS table_name, COUNT(*) AS n FROM users
UNION ALL SELECT 'unit', COUNT(*) FROM unit
UNION ALL SELECT 'category', COUNT(*) FROM category
UNION ALL SELECT 'sku', COUNT(*) FROM sku
UNION ALL SELECT 'supplier', COUNT(*) FROM supplier
UNION ALL SELECT 'inventory_lot', COUNT(*) FROM inventory_lot
UNION ALL SELECT 'purchase_order', COUNT(*) FROM purchase_order
UNION ALL SELECT 'purchase_order_item', COUNT(*) FROM purchase_order_item
UNION ALL SELECT 'sale', COUNT(*) FROM sale
UNION ALL SELECT 'sale_item', COUNT(*) FROM sale_item
UNION ALL SELECT 'stock_movement', COUNT(*) FROM stock_movement
UNION ALL SELECT 'receipt_seq', COUNT(*) FROM receipt_seq
ORDER BY table_name;
