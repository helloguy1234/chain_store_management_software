-- =============================================================
--  Store Management System – MySQL schema & seed data
--  File: init.sql
--  Mục tiêu: Tạo CSDL, bảng, ràng buộc, trigger cơ bản và dữ liệu mẫu
--  Phù hợp với kiến trúc trong README và các FXML đã tạo (customers/inventory/finance)
--  Tested with MySQL 8.x (InnoDB, utf8mb4)
-- =============================================================

-- 1) Tạo database (nếu chưa có) và chọn DB
CREATE DATABASE IF NOT EXISTS store_management
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE store_management;

-- Đảm bảo engine và charset chuẩn
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 2) Xóa bảng cũ (nếu cần – để chạy lại init.sql nhiều lần)
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS stock_moves;
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS finance_reports;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS stores;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
-- 3) BẢNG CỐT LÕI
-- =============================================================

-- 3.1) Cửa hàng (phục vụ filter trong inventory.fxml & finance.fxml)
CREATE TABLE stores (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  code       VARCHAR(16)  NOT NULL UNIQUE,
  name       VARCHAR(128) NOT NULL,
  address    VARCHAR(255),
  phone      VARCHAR(32),
  created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3.2) Khách hàng (map với customers.fxml: ID/Name/Phone/Email/Points/CreatedAt)
CREATE TABLE customers (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  name       VARCHAR(128) NOT NULL,
  phone      VARCHAR(32)  UNIQUE,
  email      VARCHAR(128) UNIQUE,
  points     INT          NOT NULL DEFAULT 0,  -- điểm tích luỹ, cập nhật bởi ứng dụng khi hoàn tất đơn
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE INDEX idx_customers_name  ON customers(name);
CREATE INDEX idx_customers_phone ON customers(phone);

-- 3.3) Nhà cung cấp
CREATE TABLE suppliers (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  name       VARCHAR(160) NOT NULL,
  contact    VARCHAR(128),
  phone      VARCHAR(32),
  email      VARCHAR(128),
  address    VARCHAR(255),
  created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE INDEX idx_suppliers_name ON suppliers(name);

-- 3.4) Sản phẩm (map một phần với inventory.fxml: Name/SKU/Supplier/UnitPrice)
CREATE TABLE products (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  supplier_id BIGINT,
  sku         VARCHAR(32)  NOT NULL UNIQUE,
  name        VARCHAR(160) NOT NULL,
  unit        VARCHAR(32)  NOT NULL DEFAULT 'pcs',
  unit_price  DECIMAL(12,2) NOT NULL DEFAULT 0.00, -- giá bán đề xuất
  unit_cost   DECIMAL(12,2) NOT NULL DEFAULT 0.00, -- giá vốn (tuỳ chọn)
  active      TINYINT(1)     NOT NULL DEFAULT 1,
  created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_products_supplier
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE INDEX idx_products_name ON products(name);

-- 3.5) Tồn kho theo từng cửa hàng + sản phẩm (unique cặp store_id + product_id)
CREATE TABLE inventory (
  store_id   BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity   INT    NOT NULL DEFAULT 0,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (store_id, product_id),
  CONSTRAINT fk_inventory_store  FOREIGN KEY (store_id)   REFERENCES stores(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3.6) Nhật ký nhập/xuất/điều chỉnh kho (đi kèm trigger cập nhật inventory)
CREATE TABLE stock_moves (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id   BIGINT       NOT NULL,
  product_id BIGINT       NOT NULL,
  move_type  ENUM('IMPORT','EXPORT','ADJUST') NOT NULL,
  quantity   INT          NOT NULL, -- cho phép âm khi ADJUST (delta âm); IMPORT/EXPORT nên dương
  note       VARCHAR(255),
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_moves_store   FOREIGN KEY (store_id)   REFERENCES stores(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_moves_product FOREIGN KEY (product_id) REFERENCES products(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  INDEX idx_moves_store_product (store_id, product_id, created_at)
) ENGINE=InnoDB;

-- 3.7) Đơn hàng & chi tiết (phục vụ tích điểm + tự động trừ kho qua trigger)
CREATE TABLE orders (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_code   VARCHAR(32) UNIQUE, -- mã đơn tuỳ chọn do app sinh
  store_id     BIGINT NOT NULL,
  customer_id  BIGINT NULL,
  total_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_orders_store    FOREIGN KEY (store_id)    REFERENCES stores(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
    ON UPDATE CASCADE ON DELETE SET NULL,
  INDEX idx_orders_store_created (store_id, created_at)
) ENGINE=InnoDB;

-- Gợi ý: dùng cột sinh tự động cho line_total để luôn = quantity * unit_price
CREATE TABLE order_items (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id   BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity   INT    NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  line_total DECIMAL(14,2) AS (quantity * unit_price) STORED,
  CONSTRAINT fk_items_order   FOREIGN KEY (order_id)   REFERENCES orders(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_items_product FOREIGN KEY (product_id) REFERENCES products(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  INDEX idx_items_order (order_id)
) ENGINE=InnoDB;

-- 3.8) Báo cáo tài chính (map với finance.fxml: Store/Date/Type/Amount/Note)
CREATE TABLE finance_reports (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  store_id    BIGINT NOT NULL,
  report_date DATE   NOT NULL,
  type        ENUM('INCOME','EXPENSE') NOT NULL,
  category    VARCHAR(64),
  amount      DECIMAL(14,2) NOT NULL,
  note        VARCHAR(255),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_fin_store FOREIGN KEY (store_id) REFERENCES stores(id)
    ON UPDATE CASCADE ON DELETE CASCADE,
  INDEX idx_fin_store_date (store_id, report_date, type)
) ENGINE=InnoDB;

-- =============================================================
-- 4) TRIGGERS: Đồng bộ hoá tồn kho & tổng tiền đơn hàng
-- =============================================================
DELIMITER $$

-- 4.1) Khi phát sinh một dòng stock_moves → cập nhật bảng inventory tương ứng
CREATE TRIGGER trg_moves_after_insert
AFTER INSERT ON stock_moves
FOR EACH ROW
BEGIN
  DECLARE delta INT;
  DECLARE new_qty INT;

  -- Quy đổi move_type thành delta (+/-)
  SET delta = CASE NEW.move_type
                WHEN 'IMPORT' THEN  NEW.quantity
                WHEN 'EXPORT' THEN -NEW.quantity
                WHEN 'ADJUST' THEN  NEW.quantity   -- có thể âm hoặc dương
              END;

  -- Upsert vào inventory (unique key: store_id + product_id)
  INSERT INTO inventory (store_id, product_id, quantity, updated_at)
  VALUES (NEW.store_id, NEW.product_id, delta, NOW())
  ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity), updated_at = NOW();

  -- Kiểm tra không cho âm kho (nếu âm → rollback toàn bộ statement)
  SELECT quantity INTO new_qty
  FROM inventory
  WHERE store_id = NEW.store_id AND product_id = NEW.product_id
  FOR UPDATE;

  IF new_qty < 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Stock would become negative – operation canceled';
  END IF;
END $$

-- 4.2) Sau khi thêm 1 dòng chi tiết đơn hàng →
--      - Cập nhật tổng tiền đơn (SUM line_total)
--      - Ghi nhận xuất kho tương ứng (EXPORT) qua stock_moves
CREATE TRIGGER trg_items_after_insert
AFTER INSERT ON order_items
FOR EACH ROW
BEGIN
  DECLARE v_store_id BIGINT;
  -- Cập nhật tổng tiền đơn hàng
  UPDATE orders o
    SET o.total_amount = (
      SELECT IFNULL(SUM(line_total),0) FROM order_items WHERE order_id = NEW.order_id
    )
  WHERE o.id = NEW.order_id;

  -- Lấy store_id của đơn để ghi xuất kho
  SELECT store_id INTO v_store_id FROM orders WHERE id = NEW.order_id;

  INSERT INTO stock_moves(store_id, product_id, move_type, quantity, note)
  VALUES (v_store_id, NEW.product_id, 'EXPORT', NEW.quantity, CONCAT('Order #', NEW.order_id));
END $$

-- 4.3) Khi cập nhật chi tiết đơn → điều chỉnh tồn kho theo chênh lệch số lượng
CREATE TRIGGER trg_items_after_update
AFTER UPDATE ON order_items
FOR EACH ROW
BEGIN
  DECLARE v_store_id BIGINT;
  DECLARE v_delta INT;

  -- Cập nhật tổng tiền đơn
  UPDATE orders o
    SET o.total_amount = (
      SELECT IFNULL(SUM(line_total),0) FROM order_items WHERE order_id = NEW.order_id
    )
  WHERE o.id = NEW.order_id;

  -- Tính delta số lượng (mới - cũ); nếu dương → xuất thêm; âm → cộng trả kho
  SET v_delta = NEW.quantity - OLD.quantity;
  IF v_delta <> 0 THEN
    SELECT store_id INTO v_store_id FROM orders WHERE id = NEW.order_id;
    INSERT INTO stock_moves(store_id, product_id, move_type, quantity, note)
    VALUES (v_store_id, NEW.product_id, 'ADJUST', -v_delta, CONCAT('Adjust item order #', NEW.order_id));
    -- Giải thích: dùng 'ADJUST' với quantity = -delta để giữ quy ước: delta dương nghĩa là đã xuất thêm,
    -- ta cần điều chỉnh kho giảm thêm (âm). Trigger trg_moves_after_insert sẽ cộng delta vào inventory.
  END IF;
END $$

-- 4.4) Khi xoá chi tiết đơn → hoàn kho lượng đã xuất
CREATE TRIGGER trg_items_after_delete
AFTER DELETE ON order_items
FOR EACH ROW
BEGIN
  DECLARE v_store_id BIGINT;
  -- Cập nhật tổng tiền đơn
  UPDATE orders o
    SET o.total_amount = (
      SELECT IFNULL(SUM(line_total),0) FROM order_items WHERE order_id = OLD.order_id
    )
  WHERE o.id = OLD.order_id;

  -- Hoàn kho lượng đã xuất
  SELECT store_id INTO v_store_id FROM orders WHERE id = OLD.order_id;
  INSERT INTO stock_moves(store_id, product_id, move_type, quantity, note)
  VALUES (v_store_id, OLD.product_id, 'IMPORT', OLD.quantity, CONCAT('Remove item order #', OLD.order_id));
END $$

DELIMITER ;

-- =============================================================
-- 5) VIEW tiện lợi (tuỳ chọn)
-- =============================================================
CREATE OR REPLACE VIEW v_inventory_overview AS
SELECT s.id AS store_id, s.code AS store_code, s.name AS store_name,
       p.id AS product_id, p.sku, p.name AS product_name,
       i.quantity, i.updated_at
FROM inventory i
JOIN stores s   ON s.id = i.store_id
JOIN products p ON p.id = i.product_id;

CREATE OR REPLACE VIEW v_finance_summary_by_store AS
SELECT store_id,
       SUM(CASE WHEN type='INCOME'  THEN amount ELSE 0 END) AS total_income,
       SUM(CASE WHEN type='EXPENSE' THEN amount ELSE 0 END) AS total_expense,
       SUM(CASE WHEN type='INCOME'  THEN amount ELSE -amount END) AS balance
FROM finance_reports
GROUP BY store_id;

-- =============================================================
-- 6) DỮ LIỆU MẪU (để test nhanh các màn hình)
-- =============================================================
START TRANSACTION;

-- Cửa hàng
INSERT INTO stores(code, name, address, phone) VALUES
 ('S001','HUST Mart – Cơ sở 1','1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội','024-000111'),
 ('S002','HUST Mart – Cơ sở 2','Hòa Lạc, Thạch Thất, Hà Nội','024-000222');

-- Nhà cung cấp
INSERT INTO suppliers(name, contact, phone, email) VALUES
 ('VinSup','Mr. Vinh','0901234567','vinh@vinsup.vn'),
 ('GreenFarm','Ms. Lan','0902345678','lan@greenfarm.vn');

-- Sản phẩm
INSERT INTO products(supplier_id, sku, name, unit, unit_price, unit_cost) VALUES
 (1,'SKU-MILK-1L','Sữa tươi 1L','bottle', 34000, 26000),
 (1,'SKU-MILK-500','Sữa tươi 500ml','bottle', 19000, 15000),
 (2,'SKU-OAT-1KG','Yến mạch 1kg','bag',   52000, 41000),
 (2,'SKU-TEA-200','Trà túi lọc 200g','box', 38000, 30000),
 (2,'SKU-SUGAR-1KG','Đường 1kg','bag',    24000, 19000);

-- Khách hàng
INSERT INTO customers(name, phone, email, points) VALUES
 ('Nguyễn Văn A','0900000001','a@example.com',10),
 ('Trần Thị B','0900000002','b@example.com', 5),
 ('Phạm Ngọc Hưng','0900002023','hung@example.com', 0);

-- Nhập kho ban đầu qua stock_moves (IMPORT) → trigger sẽ cập nhật inventory
-- Cơ sở 1: nhập mỗi mặt hàng 100 đơn vị
INSERT INTO stock_moves(store_id, product_id, move_type, quantity, note)
SELECT 1 AS store_id, p.id, 'IMPORT' AS move_type, 100 AS quantity, 'Initial import S001'
FROM products p;
-- Cơ sở 2: nhập mỗi mặt hàng 60 đơn vị
INSERT INTO stock_moves(store_id, product_id, move_type, quantity, note)
SELECT 2 AS store_id, p.id, 'IMPORT' AS move_type, 60 AS quantity, 'Initial import S002'
FROM products p;

-- Tạo 1 đơn hàng mẫu tại Cơ sở 1 cho khách hàng A
INSERT INTO orders(order_code, store_id, customer_id) VALUES ('ORD-0001', 1, 1);
-- Thêm 2 dòng chi tiết → trigger sẽ tự trừ kho và cộng tổng tiền
INSERT INTO order_items(order_id, product_id, quantity, unit_price) VALUES
 (LAST_INSERT_ID(), 1, 2, 34000),  -- 2 chai Sữa 1L
 (LAST_INSERT_ID(), 3, 1, 52000);  -- 1 túi Yến mạch 1kg

-- Báo cáo tài chính mẫu
INSERT INTO finance_reports(store_id, report_date, type, category, amount, note) VALUES
 (1, CURDATE(), 'INCOME',  'Sales',      120000.00, 'Doanh thu bán lẻ'),
 (1, CURDATE(), 'EXPENSE', 'Utilities',   15000.00, 'Tiền điện nước'),
 (1, CURDATE(), 'EXPENSE', 'Equipment',   30000.00, 'Sửa chữa kệ'),
 (2, CURDATE(), 'INCOME',  'Sales',       89000.00, 'Doanh thu bán lẻ');

COMMIT;

-- =============================================================
-- 7) GỢI Ý KẾT NỐI (db.properties)
-- =============================================================
-- File: src/main/resources/database/db.properties
-- url=jdbc:mysql://localhost:3306/store_management
-- user=root
-- password=yourpassword

-- =============================================================
-- 8) GHI CHÚ THỰC THI
-- =============================================================
-- Chạy file này bằng MySQL Workbench hoặc CLI:
--   mysql -u root -p < init.sql
-- Sau khi chạy, các màn hình:
--   - customers.fxml: sẽ có 3 khách hàng mẫu
--   - inventory.fxml: tồn kho đã được nhập (100 tại S001, 60 tại S002 mỗi mặt hàng)
--   - finance.fxml: có bản ghi thu/chi mẫu hôm nay
-- Đặt logic tích điểm ở tầng Service/DAO khi “chốt đơn” (không tạo trigger tích điểm ở đây
-- để tránh cộng trùng khi sửa/xoá chi tiết đơn).