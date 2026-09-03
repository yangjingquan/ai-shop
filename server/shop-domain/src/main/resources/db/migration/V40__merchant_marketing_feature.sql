CREATE TABLE merchant_marketing_feature (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    feature_code VARCHAR(64) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 0 COMMENT '1=启用 0=停用',
    config_json TEXT NOT NULL COMMENT '活动配置JSON，第一阶段预留',
    sort INT NOT NULL DEFAULT 0,
    updated_by BIGINT DEFAULT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_merchant_marketing_feature (merchant_id, feature_code, deleted),
    INDEX idx_marketing_feature_merchant_enabled (merchant_id, enabled, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO merchant_marketing_feature (merchant_id, feature_code, enabled, config_json, sort)
SELECT m.id, codes.feature_code,
       CASE WHEN codes.feature_code = 'GROUP_BUY' AND EXISTS (
           SELECT 1 FROM product p
           WHERE p.merchant_id = m.id AND p.is_group_buy = 1 AND p.status = 1 AND p.deleted = 0
       ) THEN 1 ELSE 0 END,
       '{}', codes.sort
FROM merchant m
CROSS JOIN (
    SELECT 'NEW_USER_COUPON' AS feature_code, 1 AS sort
    UNION ALL SELECT 'SECKILL', 2
    UNION ALL SELECT 'GROUP_BUY', 3
    UNION ALL SELECT 'FULL_REDUCTION', 4
    UNION ALL SELECT 'REPURCHASE_COUPON', 5
    UNION ALL SELECT 'POINTS_MEMBER_DAY', 6
    UNION ALL SELECT 'REFERRAL', 7
    UNION ALL SELECT 'BUNDLE', 8
    UNION ALL SELECT 'PRESALE', 9
    UNION ALL SELECT 'LOTTERY_BLIND_BOX', 10
) codes
WHERE m.deleted = 0;

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:marketing:view', '查看营销活动', '营销活动', 'MENU', 110),
('merchant:marketing:feature:update', '启停营销活动', '营销活动', 'BUTTON', 111)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code = 'operator' AND mp.code IN ('merchant:marketing:view', 'merchant:marketing:feature:update');

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code = 'owner' AND mp.code IN ('merchant:marketing:view', 'merchant:marketing:feature:update');
