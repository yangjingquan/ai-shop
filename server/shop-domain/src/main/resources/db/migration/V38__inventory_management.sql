CREATE TABLE inventory_transaction (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  sku_id BIGINT UNSIGNED NOT NULL,
  change_qty INT NOT NULL COMMENT '库存变化量，入库为正、出库为负',
  stock_before INT NOT NULL,
  stock_after INT NOT NULL,
  operation_type VARCHAR(32) NOT NULL COMMENT 'MANUAL_ADJUST',
  reference_no VARCHAR(64) NOT NULL DEFAULT '',
  reason VARCHAR(255) NOT NULL DEFAULT '',
  operator_id BIGINT UNSIGNED DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  KEY idx_inventory_tx_merchant_time (merchant_id, created_at),
  KEY idx_inventory_tx_sku_time (sku_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU库存变更流水';
