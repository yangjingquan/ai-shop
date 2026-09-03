ALTER TABLE product
    ADD COLUMN group_buy_duration_hours INT NOT NULL DEFAULT 24 COMMENT '团购有效期（小时）' AFTER group_buy_required_count,
    ADD COLUMN group_buy_user_limit INT NOT NULL DEFAULT 1 COMMENT '同一用户同一团限购数量' AFTER group_buy_duration_hours,
    ADD COLUMN group_buy_show_active TINYINT NOT NULL DEFAULT 1 COMMENT '是否展示进行中的团' AFTER group_buy_user_limit,
    ADD COLUMN group_buy_sku_ids_json TEXT COMMENT '团购适用 SKU ID 列表，空表示全部 SKU' AFTER group_buy_show_active;

UPDATE product
SET group_buy_duration_hours = 24,
    group_buy_user_limit = 1,
    group_buy_show_active = 1
WHERE is_group_buy = 1;

UPDATE merchant_marketing_feature
SET config_json = '{"durationHours":24,"userLimit":1,"showActiveGroups":1,"formedTemplateId":"sg0sw0AxgcxKZN1_Rz03ggc50HltbY1FK-Me2ZDGWcc","expiringTemplateId":"RevYrSvVjLuJ4WEhySpfQ2FrWEyDKGyxcHYz-QiyzN0","failedTemplateId":"9eLlvp1elpSJeHU-BgET6tZL2NOaqZfj6CB8vTX8s0A"}'
WHERE feature_code = 'GROUP_BUY' AND (config_json IS NULL OR config_json = '{}');

CREATE TABLE group_buy_share_event (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    group_id BIGINT UNSIGNED NOT NULL,
    sharer_user_id BIGINT NOT NULL,
    opener_user_id BIGINT DEFAULT NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'unknown',
    opened_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_group_share_group (group_id, created_at),
    INDEX idx_group_share_user (sharer_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团购分享与打开事件';

CREATE TABLE group_buy_subscription (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    group_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT NOT NULL,
    template_type VARCHAR(32) NOT NULL,
    template_id VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'reject' COMMENT 'accept/reject/ban/filter',
    subscribed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME DEFAULT NULL,
    send_result VARCHAR(500) NOT NULL DEFAULT '',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_group_subscription (group_id, user_id, template_type, deleted),
    INDEX idx_group_subscription_send (group_id, template_type, status, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团购订阅消息授权记录';

CREATE TABLE group_buy_notification_log (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    group_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    template_type VARCHAR(32) NOT NULL,
    template_id VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/success/failed/skipped',
    errcode INT DEFAULT NULL,
    errmsg VARCHAR(500) NOT NULL DEFAULT '',
    sent_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_group_notification_event (group_id, user_id, event_type, template_type, deleted),
    INDEX idx_group_notification_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团购订阅消息发送日志';

CREATE TABLE group_refund_task (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT NOT NULL,
    group_id BIGINT UNSIGNED NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    refund_application_id BIGINT UNSIGNED DEFAULT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/success/failed',
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500) NOT NULL DEFAULT '',
    next_retry_at DATETIME DEFAULT NULL,
    completed_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_group_refund_order (order_no, deleted),
    INDEX idx_group_refund_status (merchant_id, status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='团购失败退款追踪任务';
