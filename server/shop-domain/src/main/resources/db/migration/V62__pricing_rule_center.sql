-- I0-01: the active platform policy is the single source for marketing compatibility.
CREATE TABLE pricing_rule_version (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  version BIGINT UNSIGNED NOT NULL,
  matrix_json JSON NOT NULL,
  zero_pay_whitelist_json JSON NOT NULL,
  max_discount_rate DECIMAL(5,4) NOT NULL DEFAULT 1.0000,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0=draft 1=active 2=retired',
  approved_by BIGINT UNSIGNED DEFAULT NULL,
  approved_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_pricing_rule_version (version),
  KEY idx_pricing_rule_active (status, deleted, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台报价规则版本';

INSERT INTO pricing_rule_version (version, matrix_json, zero_pay_whitelist_json, max_discount_rate, status, approved_at)
VALUES (1,
  JSON_OBJECT(
    'SECKILL', JSON_ARRAY('COUPON', 'FULL_REDUCTION', 'FULL_DISCOUNT', 'POINTS'),
    'GROUP_BUY', JSON_ARRAY('COUPON', 'FULL_REDUCTION', 'FULL_DISCOUNT', 'POINTS'),
    'PRESALE_DEPOSIT', JSON_ARRAY('COUPON', 'FULL_REDUCTION', 'FULL_DISCOUNT', 'POINTS'),
    'BUNDLE', JSON_ARRAY('COUPON', 'FULL_REDUCTION', 'FULL_DISCOUNT', 'POINTS'),
    'FULL_REDUCTION', JSON_ARRAY('FULL_DISCOUNT'),
    'FULL_DISCOUNT', JSON_ARRAY('FULL_REDUCTION')
  ),
  JSON_ARRAY('POINTS_EXCHANGE', 'LOTTERY_PHYSICAL'), 1.0000, 1, NOW());

CREATE TABLE price_quote (
  id VARCHAR(40) PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL,
  scene VARCHAR(32) NOT NULL,
  rule_version BIGINT UNSIGNED NOT NULL,
  original_amount DECIMAL(12,2) NOT NULL,
  activity_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  coupon_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  points_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  freight_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  payable_amount DECIMAL(12,2) NOT NULL,
  coupon_id BIGINT UNSIGNED DEFAULT NULL,
  snapshot_json JSON NOT NULL,
  expires_at DATETIME NOT NULL,
  consumed_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_price_quote_user (user_id, merchant_id, expires_at),
  KEY idx_price_quote_rule (rule_version, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='最终报价快照';

CREATE TABLE marketing_rule_audit (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  rule_version BIGINT UNSIGNED NOT NULL,
  operator_id BIGINT UNSIGNED DEFAULT NULL,
  action VARCHAR(32) NOT NULL,
  reason VARCHAR(255) DEFAULT '',
  before_json JSON DEFAULT NULL,
  after_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_marketing_rule_audit_version (rule_version, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销规则发布审计';

ALTER TABLE `order`
  ADD COLUMN quote_id VARCHAR(40) DEFAULT NULL AFTER order_no,
  ADD COLUMN rule_version BIGINT UNSIGNED DEFAULT NULL AFTER quote_id,
  ADD COLUMN pricing_snapshot_json JSON DEFAULT NULL AFTER promotion_snapshot_json,
  ADD KEY idx_order_quote (quote_id),
  ADD KEY idx_order_rule_version (rule_version);

ALTER TABLE order_item
  ADD COLUMN pricing_snapshot_json JSON DEFAULT NULL AFTER subtotal;
