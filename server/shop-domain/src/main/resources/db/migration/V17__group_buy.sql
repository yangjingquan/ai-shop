ALTER TABLE product
  ADD COLUMN is_group_buy TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=参加团购 0=不参加' AFTER is_recommend,
  ADD COLUMN group_buy_price DECIMAL(10,2) DEFAULT NULL COMMENT '团购价格' AFTER is_group_buy,
  ADD COLUMN group_buy_required_count INT DEFAULT NULL COMMENT '几人成团' AFTER group_buy_price,
  ADD KEY idx_group_buy_status (is_group_buy, status, deleted);

ALTER TABLE `order`
  ADD COLUMN order_type TINYINT NOT NULL DEFAULT 0 COMMENT '0=普通订单 1=团购订单' AFTER status,
  ADD COLUMN group_buy_group_id BIGINT UNSIGNED DEFAULT NULL COMMENT '团购团实例ID' AFTER order_type,
  ADD KEY idx_group_buy_group (group_buy_group_id, deleted),
  ADD KEY idx_order_type_status (order_type, status, deleted);

CREATE TABLE group_buy_group (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  leader_user_id BIGINT UNSIGNED NOT NULL,
  required_count INT NOT NULL,
  paid_count INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0=WAIT_GROUP 1=FORMED 2=FAILED_WAIT_REFUND 3=CANCELLED',
  expire_at DATETIME NOT NULL,
  formed_at DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_product_status_expire (product_id, status, expire_at, deleted),
  KEY idx_status_expire (status, expire_at, deleted),
  KEY idx_merchant_status (merchant_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团购团实例';

CREATE TABLE group_buy_member (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  group_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  order_no VARCHAR(32) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0=WAIT_PAY 1=PAID 2=CANCELLED 3=WAIT_REFUND',
  paid_at DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_group_user (group_id, user_id, deleted),
  KEY idx_group_status (group_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团购参团成员';
