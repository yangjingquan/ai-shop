ALTER TABLE product_sku
  ADD COLUMN original_price DECIMAL(10,2) DEFAULT NULL COMMENT '原价/划线价' AFTER price;

ALTER TABLE product
  ADD COLUMN min_original_price DECIMAL(10,2) DEFAULT NULL COMMENT '最低原价/划线价' AFTER max_price,
  ADD COLUMN max_original_price DECIMAL(10,2) DEFAULT NULL COMMENT '最高原价/划线价' AFTER min_original_price;
