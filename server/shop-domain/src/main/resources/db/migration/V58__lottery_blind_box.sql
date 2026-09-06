ALTER TABLE member_profile ADD COLUMN level INT NOT NULL DEFAULT 1 COMMENT '会员等级';
ALTER TABLE `order` ADD COLUMN lottery_reward_id BIGINT DEFAULT NULL COMMENT '抽奖实物奖励关联';

CREATE TABLE lottery_activity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  theme_image VARCHAR(512) DEFAULT '',
  entry_image VARCHAR(512) DEFAULT '',
  rule_text TEXT NOT NULL,
  condition_json TEXT NOT NULL,
  daily_chances INT NOT NULL DEFAULT 1,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布 2暂停 3结束',
  probability_version INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_lottery_activity_merchant_status (merchant_id, status, start_at, end_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互动抽奖盲盒活动';

CREATE TABLE lottery_prize (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  activity_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  prize_type VARCHAR(20) NOT NULL COMMENT 'POINTS COUPON PHYSICAL CONSOLATION',
  image VARCHAR(512) DEFAULT '',
  points_amount INT DEFAULT NULL,
  coupon_template_id BIGINT DEFAULT NULL,
  product_id BIGINT DEFAULT NULL,
  sku_id BIGINT DEFAULT NULL,
  total_stock INT NOT NULL DEFAULT 0 COMMENT '0表示不限',
  remaining_stock INT NOT NULL DEFAULT 0,
  probability DECIMAL(12,8) NOT NULL DEFAULT 0,
  validity_days INT NOT NULL DEFAULT 0,
  sort INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_lottery_prize_activity (activity_id, status, sort, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖奖品';

CREATE TABLE lottery_draw_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  draw_no VARCHAR(64) NOT NULL,
  activity_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  draw_date DATE NOT NULL,
  prize_id BIGINT DEFAULT NULL,
  prize_name VARCHAR(128) NOT NULL,
  prize_type VARCHAR(20) NOT NULL,
  probability_snapshot DECIMAL(12,8) NOT NULL DEFAULT 0,
  probability_version INT NOT NULL DEFAULT 1,
  idempotency_key VARCHAR(96) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_lottery_draw_no (draw_no),
  UNIQUE KEY uk_lottery_draw_idempotency (activity_id, user_id, idempotency_key, deleted),
  KEY idx_lottery_draw_user_day (merchant_id, user_id, activity_id, draw_date, deleted),
  KEY idx_lottery_draw_activity (activity_id, prize_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖记录';

CREATE TABLE lottery_reward (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  draw_record_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  prize_id BIGINT DEFAULT NULL,
  prize_name VARCHAR(128) NOT NULL,
  prize_type VARCHAR(20) NOT NULL,
  points_amount INT DEFAULT NULL,
  coupon_template_id BIGINT DEFAULT NULL,
  coupon_id BIGINT DEFAULT NULL,
  order_no VARCHAR(64) DEFAULT NULL,
  address_snapshot VARCHAR(500) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待领取 1已发放 2待填写地址 3待发货 4已发货 5已完成 6失败',
  failure_reason VARCHAR(255) DEFAULT NULL,
  issued_at DATETIME DEFAULT NULL,
  claimed_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_lottery_reward_draw (draw_record_id, deleted),
  KEY idx_lottery_reward_user (merchant_id, user_id, status, created_at, deleted),
  KEY idx_lottery_reward_activity (activity_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖奖励发放';

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:lottery:view', '查看抽奖盲盒', '营销活动', 'MENU', 140),
('merchant:lottery:create', '创建抽奖活动', '营销活动', 'BUTTON', 141),
('merchant:lottery:update', '编辑抽奖活动', '营销活动', 'BUTTON', 142),
('merchant:lottery:status', '发布抽奖活动', '营销活动', 'BUTTON', 143),
('merchant:lottery:reward', '处理抽奖实物奖励', '营销活动', 'BUTTON', 144)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('operator', 'owner')
  AND mp.code IN ('merchant:lottery:view', 'merchant:lottery:create', 'merchant:lottery:update', 'merchant:lottery:status', 'merchant:lottery:reward');
