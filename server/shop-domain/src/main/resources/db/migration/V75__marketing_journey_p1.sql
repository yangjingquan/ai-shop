CREATE TABLE marketing_journey (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(96) NOT NULL,
  trigger_type VARCHAR(32) NOT NULL,
  segment_id BIGINT UNSIGNED NULL,
  delay_minutes INT NOT NULL DEFAULT 0,
  coupon_template_id BIGINT UNSIGNED NULL,
  notification_title VARCHAR(96) NOT NULL DEFAULT '',
  notification_content VARCHAR(500) NOT NULL DEFAULT '',
  frequency_days INT NOT NULL DEFAULT 7,
  stop_on_paid TINYINT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_marketing_journey_active (merchant_id, status, trigger_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动营销旅程';

CREATE TABLE marketing_journey_enrollment (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  journey_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  trigger_key VARCHAR(128) NOT NULL,
  trigger_at DATETIME NOT NULL,
  execute_at DATETIME NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  skip_reason VARCHAR(255) NOT NULL DEFAULT '',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_journey_enrollment (journey_id, user_id, trigger_key, deleted),
  KEY idx_journey_due (merchant_id, status, execute_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动营销入旅记录';

CREATE TABLE marketing_journey_execution (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  journey_id BIGINT UNSIGNED NOT NULL,
  enrollment_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  coupon_id BIGINT UNSIGNED NULL,
  notification_id BIGINT UNSIGNED NULL,
  status VARCHAR(16) NOT NULL,
  reason VARCHAR(255) NOT NULL DEFAULT '',
  executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_journey_execution (enrollment_id, deleted),
  KEY idx_journey_execution (merchant_id, journey_id, status, executed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动营销执行与效果记录';

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:journey:view', '查看自动营销', '用户运营', 'MENU', 150),
('merchant:journey:manage', '管理自动营销', '用户运营', 'BUTTON', 151)
ON DUPLICATE KEY UPDATE name=VALUES(name), module=VALUES(module), type=VALUES(type), sort=VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('owner', 'operator', 'MERCHANT_ADMIN', 'MERCHANT_OPERATOR')
  AND mp.code IN ('merchant:journey:view', 'merchant:journey:manage');
