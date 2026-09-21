CREATE TABLE customer_metric_snapshot (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  paid_order_count INT NOT NULL DEFAULT 0,
  total_paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  first_paid_at DATETIME NULL,
  last_paid_at DATETIME NULL,
  member_level INT NOT NULL DEFAULT 1,
  points_balance INT NOT NULL DEFAULT 0,
  unused_coupon_count INT NOT NULL DEFAULT 0,
  expiring_coupon_count INT NOT NULL DEFAULT 0,
  favorite_count INT NOT NULL DEFAULT 0,
  history_count INT NOT NULL DEFAULT 0,
  last_viewed_at DATETIME NULL,
  registered_at DATETIME NULL,
  member_level_updated_at DATETIME NULL,
  calculated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_customer_metric (merchant_id, user_id, deleted),
  KEY idx_customer_metric_paid (merchant_id, last_paid_at),
  KEY idx_customer_metric_value (merchant_id, total_paid_amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户用户运营指标快照';

CREATE TABLE customer_tag (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(64) NOT NULL,
  tag_type VARCHAR(16) NOT NULL COMMENT 'SYSTEM/MANUAL',
  color VARCHAR(16) NOT NULL DEFAULT '#D86F22',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_customer_tag_code (merchant_id, code, deleted),
  KEY idx_customer_tag_merchant (merchant_id, tag_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户用户标签';

CREATE TABLE customer_tag_binding (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  tag_id BIGINT UNSIGNED NOT NULL,
  source VARCHAR(16) NOT NULL COMMENT 'SYSTEM/MANUAL',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_customer_tag_binding (merchant_id, user_id, tag_id, deleted),
  KEY idx_customer_tag_user (merchant_id, user_id, status),
  KEY idx_customer_tag_tag (merchant_id, tag_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户标签绑定';

CREATE TABLE customer_segment (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(255) NOT NULL DEFAULT '',
  segment_type VARCHAR(16) NOT NULL COMMENT 'DYNAMIC/STATIC',
  condition_json JSON NOT NULL,
  member_count INT NOT NULL DEFAULT 0,
  calculated_at DATETIME NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_customer_segment_merchant (merchant_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户用户分群';

CREATE TABLE customer_segment_member (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  segment_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  snapshot_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_customer_segment_member (segment_id, user_id, deleted),
  KEY idx_customer_segment_member (merchant_id, segment_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='静态分群成员快照';

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:customer:view', '查看用户运营', '用户运营', 'MENU', 148),
('merchant:customer:manage', '管理用户运营', '用户运营', 'BUTTON', 149)
ON DUPLICATE KEY UPDATE name=VALUES(name), module=VALUES(module), type=VALUES(type), sort=VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('owner', 'operator', 'MERCHANT_ADMIN', 'MERCHANT_OPERATOR')
  AND mp.code IN ('merchant:customer:view', 'merchant:customer:manage');
