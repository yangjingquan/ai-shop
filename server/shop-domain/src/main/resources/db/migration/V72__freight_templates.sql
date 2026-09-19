CREATE TABLE freight_template (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(64) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  version BIGINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_freight_template_merchant (merchant_id, enabled, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户运费模板';

CREATE TABLE freight_template_rule (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  template_id BIGINT UNSIGNED NOT NULL,
  province VARCHAR(32) NOT NULL DEFAULT '' COMMENT '空=默认规则',
  city VARCHAR(32) NOT NULL DEFAULT '' COMMENT '空=全省规则',
  first_weight_gram INT NOT NULL,
  first_fee DECIMAL(10,2) NOT NULL,
  additional_weight_gram INT NOT NULL,
  additional_fee DECIMAL(10,2) NOT NULL,
  free_threshold_amount DECIMAL(10,2) NULL COMMENT '活动优惠后、优惠券前，达到即包邮',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_freight_rule_template_region (template_id, province, city, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费模板地区规则';

ALTER TABLE product ADD COLUMN freight_template_id BIGINT UNSIGNED NULL AFTER category_id;
ALTER TABLE product_sku ADD COLUMN weight_gram INT NULL AFTER stock;

-- 保障存量商品上线后交易口径不突变：为每个现有商户生成一份包邮模板并回填。
INSERT INTO freight_template (merchant_id, name, enabled, version, created_at, updated_at, deleted)
SELECT m.id, '历史商品包邮', 1, 1, NOW(), NOW(), 0 FROM merchant m WHERE m.deleted = 0;
INSERT INTO freight_template_rule (template_id, province, city, first_weight_gram, first_fee, additional_weight_gram, additional_fee, free_threshold_amount, created_at, updated_at, deleted)
SELECT t.id, '', '', 1, 0.00, 1, 0.00, 0.00, NOW(), NOW(), 0 FROM freight_template t WHERE t.name = '历史商品包邮';
UPDATE product p JOIN freight_template t ON t.merchant_id = p.merchant_id AND t.name = '历史商品包邮' AND t.deleted = 0 SET p.freight_template_id = t.id WHERE p.freight_template_id IS NULL;
UPDATE product_sku SET weight_gram = 1 WHERE weight_gram IS NULL;

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:freight:view', '查看运费模板', '物流设置', 'MENU', 145),
('merchant:freight:manage', '管理运费模板', '物流设置', 'BUTTON', 146)
ON DUPLICATE KEY UPDATE name=VALUES(name), module=VALUES(module), type=VALUES(type), sort=VALUES(sort);
INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('MERCHANT_ADMIN', 'MERCHANT_OPERATOR') AND mp.code IN ('merchant:freight:view', 'merchant:freight:manage');
