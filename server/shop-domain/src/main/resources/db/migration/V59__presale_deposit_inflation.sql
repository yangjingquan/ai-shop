ALTER TABLE `order`
  ADD COLUMN presale_order_id BIGINT UNSIGNED DEFAULT NULL COMMENT '预售中心订单 ID' AFTER lottery_reward_id,
  ADD COLUMN presale_stage TINYINT DEFAULT NULL COMMENT '预售支付阶段 1=定金 2=尾款' AFTER presale_order_id,
  ADD KEY idx_order_presale (presale_order_id, presale_stage, deleted);

CREATE TABLE presale_activity (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(500) DEFAULT '',
  banner_image VARCHAR(255) DEFAULT '',
  deposit_start_at DATETIME NOT NULL,
  deposit_end_at DATETIME NOT NULL,
  balance_start_at DATETIME NOT NULL,
  balance_end_at DATETIME NOT NULL,
  expected_ship_at DATETIME NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=已发布 2=已停用',
  auto_close_expired TINYINT NOT NULL DEFAULT 1 COMMENT '尾款逾期是否自动关闭并退定金',
  deposit_refund_rule VARCHAR(1000) NOT NULL DEFAULT '',
  merchant_breach_rule VARCHAR(1000) NOT NULL DEFAULT '',
  created_by BIGINT UNSIGNED DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_presale_activity_merchant (merchant_id, status, deleted, deposit_start_at),
  KEY idx_presale_activity_balance (merchant_id, balance_end_at, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预售定金膨胀活动';

CREATE TABLE presale_sku (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT UNSIGNED NOT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  sku_id BIGINT UNSIGNED NOT NULL,
  final_price DECIMAL(10,2) NOT NULL COMMENT 'SKU 预售最终价，尾款=最终价-定金抵扣',
  deposit_amount DECIMAL(10,2) NOT NULL,
  deposit_deduction_amount DECIMAL(10,2) NOT NULL,
  balance_amount DECIMAL(10,2) NOT NULL,
  user_limit INT NOT NULL DEFAULT 1,
  deposit_count INT NOT NULL DEFAULT 0,
  balance_sold_count INT NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_presale_activity_sku (activity_id, sku_id, deleted),
  KEY idx_presale_sku_merchant (merchant_id, sku_id, deleted),
  KEY idx_presale_sku_product (product_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预售活动 SKU 规则';

CREATE TABLE presale_order (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL,
  deposit_order_no VARCHAR(32) NOT NULL,
  balance_order_no VARCHAR(32) DEFAULT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL,
  activity_id BIGINT UNSIGNED NOT NULL,
  presale_sku_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  sku_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  quantity INT NOT NULL,
  stage TINYINT NOT NULL DEFAULT 0 COMMENT '0=待付定金 1=待付尾款 2=尾款待支付 3=待发货 4=已完成 5=退款中 6=尾款逾期待处理',
  product_name VARCHAR(128) NOT NULL,
  main_image VARCHAR(255) DEFAULT '',
  spec_text VARCHAR(128) DEFAULT '',
  deposit_amount DECIMAL(10,2) NOT NULL,
  deposit_deduction_amount DECIMAL(10,2) NOT NULL,
  balance_amount DECIMAL(10,2) NOT NULL,
  final_price DECIMAL(10,2) NOT NULL COMMENT '下单时的最终价快照',
  final_amount DECIMAL(10,2) NOT NULL,
  deposit_paid_at DATETIME DEFAULT NULL,
  balance_deadline DATETIME DEFAULT NULL,
  balance_paid_at DATETIME DEFAULT NULL,
  expected_ship_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_presale_order_no (order_no),
  UNIQUE KEY uk_presale_deposit_order_no (deposit_order_no),
  UNIQUE KEY uk_presale_balance_order_no (balance_order_no),
  KEY idx_presale_order_user (user_id, merchant_id, stage, deleted),
  KEY idx_presale_order_activity (activity_id, stage, deleted),
  KEY idx_presale_order_deadline (balance_deadline, stage, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预售中心订单';

CREATE TABLE presale_payment (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  presale_order_id BIGINT UNSIGNED NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  payment_stage TINYINT NOT NULL COMMENT '1=定金 2=尾款',
  amount DECIMAL(10,2) NOT NULL,
  transaction_id VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1=支付成功 2=退款中 3=退款成功',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_presale_payment_transaction (transaction_id),
  KEY idx_presale_payment_order (presale_order_id, payment_stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预售定金/尾款支付流水';

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:presale:view', '查看预售活动', '预售活动', 'MENU', 118),
('merchant:presale:create', '创建预售活动', '预售活动', 'BUTTON', 119),
('merchant:presale:update', '编辑预售活动', '预售活动', 'BUTTON', 120)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('owner', 'operator')
  AND mp.code IN ('merchant:presale:view', 'merchant:presale:create', 'merchant:presale:update');
