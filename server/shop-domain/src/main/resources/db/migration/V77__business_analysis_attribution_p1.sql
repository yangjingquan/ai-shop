CREATE TABLE analytics_event (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  event_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(32) NOT NULL COMMENT 'PRODUCT_VIEW',
  product_id BIGINT UNSIGNED NULL,
  channel_code VARCHAR(32) NULL COMMENT '预留：站外/渠道来源编码，不接入第三方平台',
  occurred_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_analytics_event (merchant_id, event_id, deleted),
  KEY idx_analytics_event_funnel (merchant_id, event_type, occurred_at),
  KEY idx_analytics_event_user (merchant_id, user_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序经营分析行为事件';

CREATE TABLE order_attribution (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  primary_source VARCHAR(32) NOT NULL COMMENT 'REFERRAL/ACTIVITY/JOURNEY/COUPON/NATURAL',
  source_id BIGINT UNSIGNED NULL,
  source_name VARCHAR(128) NOT NULL DEFAULT '',
  evidence_type VARCHAR(48) NOT NULL,
  assist_source VARCHAR(128) NOT NULL DEFAULT '' COMMENT '支付前 7 天内最近一次助攻触达，不参与主归因金额分摊',
  attributed_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_order_attribution (order_no, deleted),
  KEY idx_order_attribution_source (merchant_id, primary_source, source_id, attributed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主归因快照';

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:analysis:view', '查看经营分析', '经营分析', 'MENU', 152)
ON DUPLICATE KEY UPDATE name=VALUES(name), module=VALUES(module), type=VALUES(type), sort=VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('owner', 'operator', 'MERCHANT_ADMIN', 'MERCHANT_OPERATOR')
  AND mp.code = 'merchant:analysis:view';
