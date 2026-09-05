CREATE TABLE referral_campaign (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    share_title VARCHAR(128) NOT NULL,
    share_description VARCHAR(255) DEFAULT '',
    landing_product_id BIGINT DEFAULT NULL,
    invitee_coupon_template_id BIGINT NOT NULL,
    tier_config_json TEXT NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    max_daily_invites INT NOT NULL DEFAULT 20,
    max_total_invites INT NOT NULL DEFAULT 0 COMMENT '0=不限',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=进行中 2=暂停 3=结束',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_referral_campaign_merchant_status (merchant_id, status, start_at, end_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE referral_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    inviter_user_id BIGINT NOT NULL,
    invitee_user_id BIGINT NOT NULL,
    source_token VARCHAR(64) NOT NULL,
    first_order_no VARCHAR(64) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待首购 1=已完成 2=已冻结 3=退款待处理 4=已失效',
    bound_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_referral_relation_invitee (campaign_id, invitee_user_id, deleted),
    KEY idx_referral_relation_inviter (campaign_id, inviter_user_id, status, deleted),
    KEY idx_referral_relation_order (first_order_no, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE referral_reward (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    relation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL COMMENT 'INVITER / INVITEE',
    tier INT NOT NULL DEFAULT 0,
    coupon_template_id BIGINT NOT NULL,
    coupon_id BIGINT DEFAULT NULL,
    reward_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    trigger_order_no VARCHAR(64) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待发放 1=已发券 2=已撤销 3=发放失败 4=已冻结',
    failure_reason VARCHAR(255) DEFAULT NULL,
    revoke_reason VARCHAR(255) DEFAULT NULL,
    issued_at DATETIME DEFAULT NULL,
    revoked_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_referral_reward_tier (campaign_id, user_id, role, tier, deleted),
    KEY idx_referral_reward_relation (relation_id, status, deleted),
    KEY idx_referral_reward_order (trigger_order_no, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE referral_share_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    inviter_user_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL,
    expires_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_referral_share_token (token, deleted),
    UNIQUE KEY uk_referral_share_owner (campaign_id, inviter_user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE share_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    relation_id BIGINT DEFAULT NULL,
    inviter_user_id BIGINT DEFAULT NULL,
    invitee_user_id BIGINT DEFAULT NULL,
    token VARCHAR(64) DEFAULT NULL,
    event_type VARCHAR(32) NOT NULL COMMENT 'SHARE / OPEN / REGISTER / FIRST_PURCHASE / REWARD_ISSUED',
    order_no VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_share_event_campaign_type (campaign_id, event_type, deleted),
    KEY idx_share_event_token (token, event_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:referral:view', '查看邀请有礼', '营销活动', 'MENU', 116),
('merchant:referral:create', '创建邀请活动', '营销活动', 'BUTTON', 117),
('merchant:referral:update', '编辑邀请活动', '营销活动', 'BUTTON', 118),
('merchant:referral:status', '启停邀请活动', '营销活动', 'BUTTON', 119),
('merchant:referral:relation:freeze', '冻结邀请关系', '营销活动', 'BUTTON', 120),
('merchant:referral:reward:revoke', '撤销邀请奖励', '营销活动', 'BUTTON', 121)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('operator', 'owner')
  AND mp.code IN ('merchant:referral:view', 'merchant:referral:create', 'merchant:referral:update',
                  'merchant:referral:status', 'merchant:referral:relation:freeze', 'merchant:referral:reward:revoke');
