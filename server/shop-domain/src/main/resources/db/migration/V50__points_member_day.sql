CREATE TABLE member_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_member_profile_user_merchant (user_id, merchant_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户会员档案';

CREATE TABLE points_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  balance INT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_points_account_user_merchant (user_id, merchant_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员积分账户';

CREATE TABLE points_ledger (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  change_value INT NOT NULL,
  balance_after INT NOT NULL,
  source VARCHAR(32) NOT NULL,
  business_no VARCHAR(64) NOT NULL,
  related_ledger_id BIGINT DEFAULT NULL,
  description VARCHAR(255) NOT NULL DEFAULT '',
  expire_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_points_ledger_business (merchant_id, user_id, source, business_no, deleted),
  KEY idx_points_ledger_user (merchant_id, user_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分不可篡改流水';

CREATE TABLE points_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  register_points INT NOT NULL DEFAULT 0,
  points_per_yuan INT NOT NULL DEFAULT 1,
  sign_in_points INT NOT NULL DEFAULT 0,
  valid_days INT NOT NULL DEFAULT 0,
  deduction_per_yuan INT NOT NULL DEFAULT 100,
  deduction_max_points INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_points_rule_merchant (merchant_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分规则';

CREATE TABLE points_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  product_id BIGINT DEFAULT NULL,
  sku_id BIGINT DEFAULT NULL,
  coupon_template_id BIGINT DEFAULT NULL,
  title VARCHAR(128) NOT NULL,
  image VARCHAR(512) NOT NULL DEFAULT '',
  points_price INT NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  per_user_limit INT NOT NULL DEFAULT 0,
  valid_from DATETIME DEFAULT NULL,
  valid_to DATETIME DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_points_product_merchant (merchant_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分兑换商品或券';

CREATE TABLE points_redeem_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  redeem_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  points_product_id BIGINT NOT NULL,
  order_no VARCHAR(64) DEFAULT NULL,
  coupon_id BIGINT DEFAULT NULL,
  points_cost INT NOT NULL,
  quantity INT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_points_redeem_no (redeem_no),
  KEY idx_points_redeem_user (merchant_id, user_id, points_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分兑换记录';

CREATE TABLE member_day_activity (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  day_of_month TINYINT NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  double_points TINYINT NOT NULL DEFAULT 0,
  coupon_template_id BIGINT DEFAULT NULL,
  product_scope_type TINYINT NOT NULL DEFAULT 0,
  product_scope_ids_json TEXT DEFAULT NULL,
  stackable TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_member_day_merchant (merchant_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分会员日活动';

ALTER TABLE `order` ADD COLUMN points_redeem_id BIGINT DEFAULT NULL COMMENT '积分兑换记录ID';

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:points:view', '查看积分会员', '会员运营', 'MENU', 130),
('merchant:points:update', '配置积分会员', '会员运营', 'BUTTON', 131)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort);
INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('operator', 'owner') AND mp.code IN ('merchant:points:view', 'merchant:points:update');
