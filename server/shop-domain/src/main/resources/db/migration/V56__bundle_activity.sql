CREATE TABLE bundle_activity (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(128) NOT NULL,
  main_product_id BIGINT UNSIGNED NOT NULL,
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=启用 2=停用',
  created_by BIGINT UNSIGNED DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_bundle_activity_product (merchant_id, main_product_id, status, deleted),
  KEY idx_bundle_activity_time (merchant_id, status, start_at, end_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭配购套餐活动';

CREATE TABLE bundle_item (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  bundle_activity_id BIGINT UNSIGNED NOT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  required TINYINT NOT NULL DEFAULT 0 COMMENT '1=必选 0=可选，主商品不在本表中',
  sort INT NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_bundle_item_product (bundle_activity_id, product_id, deleted),
  KEY idx_bundle_item_activity (bundle_activity_id, deleted, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭配购套餐商品';

ALTER TABLE cart_item
  ADD COLUMN bundle_group_id VARCHAR(64) DEFAULT NULL COMMENT '搭配购购物车分组号' AFTER quantity,
  ADD COLUMN bundle_activity_id BIGINT UNSIGNED DEFAULT NULL COMMENT '搭配购活动 ID' AFTER bundle_group_id,
  ADD KEY idx_cart_bundle_group (user_id, bundle_group_id, deleted);

ALTER TABLE `order`
  ADD COLUMN bundle_activity_id BIGINT UNSIGNED DEFAULT NULL COMMENT '搭配购活动 ID' AFTER seckill_sku_id,
  ADD COLUMN bundle_discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '搭配购优惠金额' AFTER discount_amount,
  ADD COLUMN bundle_snapshot_json TEXT DEFAULT NULL COMMENT '搭配购订单快照' AFTER promotion_snapshot_json,
  ADD KEY idx_bundle_order (bundle_activity_id, deleted);

ALTER TABLE order_item
  ADD COLUMN bundle_group_id VARCHAR(64) DEFAULT NULL COMMENT '搭配购分组号' AFTER subtotal,
  ADD KEY idx_order_item_bundle_group (bundle_group_id);
