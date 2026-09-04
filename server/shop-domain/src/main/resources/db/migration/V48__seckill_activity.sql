ALTER TABLE `order`
  ADD COLUMN seckill_session_id BIGINT UNSIGNED DEFAULT NULL COMMENT '秒杀场次 ID' AFTER group_buy_group_id,
  ADD COLUMN seckill_sku_id BIGINT UNSIGNED DEFAULT NULL COMMENT '秒杀 SKU 配置 ID' AFTER seckill_session_id,
  ADD KEY idx_seckill_session (seckill_session_id, deleted);

CREATE TABLE seckill_activity (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(128) NOT NULL,
  description VARCHAR(500) DEFAULT '',
  preheat_at DATETIME NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=已发布 2=已停用',
  created_by BIGINT UNSIGNED DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_seckill_activity_merchant (merchant_id, status, deleted, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动';

CREATE TABLE seckill_session (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT UNSIGNED NOT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(64) NOT NULL,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  sort INT NOT NULL DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_seckill_session_activity (activity_id, deleted, sort, start_at),
  KEY idx_seckill_session_merchant_time (merchant_id, deleted, start_at, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀场次';

CREATE TABLE seckill_sku (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NOT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  sku_id BIGINT UNSIGNED NOT NULL,
  activity_price DECIMAL(10,2) NOT NULL,
  activity_stock INT NOT NULL DEFAULT 0 COMMENT '可被秒杀订单预占的活动库存',
  sold_count INT NOT NULL DEFAULT 0 COMMENT '已支付销量',
  user_limit INT NOT NULL DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_seckill_session_sku (session_id, sku_id, deleted),
  KEY idx_seckill_sku_session (session_id, deleted, activity_stock),
  KEY idx_seckill_sku_product (product_id, sku_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀场次 SKU';

CREATE TABLE seckill_order (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL,
  activity_id BIGINT UNSIGNED NOT NULL,
  session_id BIGINT UNSIGNED NOT NULL,
  seckill_sku_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  sku_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  quantity INT NOT NULL,
  activity_price DECIMAL(10,2) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待支付 1=已支付 2=已取消',
  stock_released TINYINT NOT NULL DEFAULT 0 COMMENT '活动库存是否已释放',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_seckill_order_no (order_no),
  KEY idx_seckill_order_user (user_id, session_id, seckill_sku_id, status, deleted),
  KEY idx_seckill_order_session (session_id, status, deleted),
  KEY idx_seckill_order_product (product_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单扩展';

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:seckill:view', '查看秒杀活动', '秒杀活动', 'MENU', 115),
('merchant:seckill:create', '创建秒杀活动', '秒杀活动', 'BUTTON', 116),
('merchant:seckill:update', '编辑秒杀活动', '秒杀活动', 'BUTTON', 117)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('owner', 'operator')
  AND mp.code IN ('merchant:seckill:view', 'merchant:seckill:create', 'merchant:seckill:update');
