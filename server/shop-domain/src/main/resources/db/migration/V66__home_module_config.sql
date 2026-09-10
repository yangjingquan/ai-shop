CREATE TABLE home_module_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  module_code VARCHAR(32) NOT NULL,
  title VARCHAR(64) DEFAULT NULL,
  subtitle VARCHAR(128) DEFAULT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  product_source VARCHAR(24) DEFAULT NULL COMMENT 'RECENT TOP_SALES RECOMMEND',
  product_limit INT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_home_module_merchant_code (merchant_id, module_code, deleted),
  KEY idx_home_module_merchant_sort (merchant_id, enabled, sort_order, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家首页模块配置';

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:home:config', '配置首页模块', '首页装修', 'MENU', 150)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('operator', 'owner') AND mp.code = 'merchant:home:config';
