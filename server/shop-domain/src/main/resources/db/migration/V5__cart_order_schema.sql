-- 购物车
CREATE TABLE cart_item (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL COMMENT '冗余,便于按商家 group',
  product_id BIGINT UNSIGNED NOT NULL COMMENT '冗余,便于联表',
  sku_id BIGINT UNSIGNED NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_user (user_id, deleted),
  KEY idx_user_sku (user_id, sku_id, deleted) COMMENT '加购 dedup'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';

-- 订单
CREATE TABLE `order` (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL,
  status TINYINT NOT NULL DEFAULT 0
    COMMENT '0=WAIT_PAY 1=WAIT_SHIP 2=WAIT_RECEIVE 3=FINISHED 4=CANCELLED',
  total_amount DECIMAL(10,2) NOT NULL DEFAULT 0
    COMMENT '商品小计 sum(item.subtotal)',
  freight_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'M4a 永远 0,预留',
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT 'M4a 永远 0,预留',
  pay_amount DECIMAL(10,2) NOT NULL DEFAULT 0
    COMMENT '= total + freight - discount',
  pay_method TINYINT DEFAULT 0 COMMENT '0=未支付 1=微信支付',
  pay_time DATETIME NULL,
  pay_transaction_id VARCHAR(64) DEFAULT '' COMMENT '微信支付订单号 / mock 时为 MOCK_xxx',
  address_snapshot VARCHAR(500) NOT NULL COMMENT 'JSON 快照',
  ship_no VARCHAR(64) DEFAULT '' COMMENT 'M4b 用',
  ship_time DATETIME NULL COMMENT 'M4b 用',
  finish_time DATETIME NULL COMMENT 'M4b 用',
  cancel_time DATETIME NULL,
  cancel_reason VARCHAR(32) DEFAULT '' COMMENT 'USER_CANCEL / TIMEOUT',
  remark VARCHAR(255) DEFAULT '' COMMENT '用户备注,M4a 不收集',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_user_status (user_id, status, deleted),
  KEY idx_merchant_status (merchant_id, status, deleted),
  KEY idx_status_created (status, created_at) COMMENT '超时未支付定时任务扫'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

-- 订单明细（全部快照）
CREATE TABLE order_item (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT UNSIGNED NOT NULL,
  order_no VARCHAR(32) NOT NULL COMMENT '冗余,便于按 order_no 反查',
  product_id BIGINT UNSIGNED NOT NULL,
  sku_id BIGINT UNSIGNED NOT NULL,
  product_name VARCHAR(128) NOT NULL COMMENT '快照',
  main_image VARCHAR(255) DEFAULT '' COMMENT '快照',
  spec_text VARCHAR(128) DEFAULT '' COMMENT '快照',
  unit_price DECIMAL(10,2) NOT NULL COMMENT '快照',
  quantity INT NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL COMMENT 'unit_price * quantity 冗余',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order (order_id),
  KEY idx_product (product_id) COMMENT '便于销量统计'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

-- 支付流水（幂等兜底）
CREATE TABLE payment_log (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL,
  transaction_id VARCHAR(64) NOT NULL COMMENT '微信流水号 / MOCK_xxx',
  amount DECIMAL(10,2) NOT NULL,
  raw_payload JSON COMMENT '回调原始报文 / mock 时为 stub',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_transaction (transaction_id),
  KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付回调流水';
