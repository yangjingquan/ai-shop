CREATE TABLE coupon_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    type TINYINT NOT NULL DEFAULT 1 COMMENT '1=满减券',
    amount DECIMAL(10,2) NOT NULL,
    threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_stock INT NOT NULL DEFAULT 0 COMMENT '0=不限库存',
    received_count INT NOT NULL DEFAULT 0,
    used_count INT NOT NULL DEFAULT 0,
    per_user_limit INT NOT NULL DEFAULT 1,
    validity_days INT NOT NULL DEFAULT 30,
    valid_from DATETIME DEFAULT NULL,
    valid_to DATETIME DEFAULT NULL,
    scope_type TINYINT NOT NULL DEFAULT 0 COMMENT '0=全场 1=分类 2=商品',
    scope_ids_json TEXT,
    new_user_only TINYINT NOT NULL DEFAULT 1,
    exclude_activity_goods TINYINT NOT NULL DEFAULT 1,
    stackable TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=草稿 1=进行中 2=已停止',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_coupon_template_merchant_status (merchant_id, status, deleted),
    INDEX idx_coupon_template_validity (valid_from, valid_to, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    template_name_snapshot VARCHAR(128) NOT NULL,
    type TINYINT NOT NULL,
    amount_snapshot DECIMAL(10,2) NOT NULL,
    threshold_snapshot DECIMAL(10,2) NOT NULL DEFAULT 0,
    scope_type_snapshot TINYINT NOT NULL DEFAULT 0,
    scope_ids_snapshot TEXT,
    exclude_activity_goods_snapshot TINYINT NOT NULL DEFAULT 1,
    valid_from DATETIME NOT NULL,
    valid_to DATETIME NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=可使用 1=已使用 2=已过期 3=已失效',
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at DATETIME DEFAULT NULL,
    used_order_no VARCHAR(64) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_coupon_template (user_id, template_id, deleted),
    INDEX idx_user_coupon_user_merchant_status (user_id, merchant_id, status, valid_to, deleted),
    INDEX idx_user_coupon_used_order (used_order_no, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `order`
    ADD COLUMN coupon_id BIGINT DEFAULT NULL COMMENT '用户券 ID' AFTER discount_amount,
    ADD COLUMN coupon_template_id BIGINT DEFAULT NULL COMMENT '优惠券模板 ID' AFTER coupon_id,
    ADD COLUMN coupon_discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠券优惠金额' AFTER coupon_template_id,
    ADD COLUMN coupon_snapshot_json TEXT DEFAULT NULL COMMENT '下单时优惠券快照' AFTER coupon_discount_amount,
    ADD INDEX idx_order_coupon_id (coupon_id);

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:coupon:view', '查看优惠券', '营销活动', 'MENU', 112),
('merchant:coupon:create', '创建优惠券', '营销活动', 'BUTTON', 113),
('merchant:coupon:update', '编辑优惠券', '营销活动', 'BUTTON', 114),
('merchant:coupon:status', '启停优惠券', '营销活动', 'BUTTON', 115)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('operator', 'owner')
  AND mp.code IN ('merchant:coupon:view', 'merchant:coupon:create', 'merchant:coupon:update', 'merchant:coupon:status');

INSERT INTO coupon_template (merchant_id, name, type, amount, threshold_amount, total_stock,
                             per_user_limit, validity_days, scope_type, scope_ids_json,
                             new_user_only, exclude_activity_goods, stackable, status)
SELECT m.id, '新人首单券', 1, 20.00, 99.00, 0, 1, 30, 0, '[]', 1, 1, 0, 1
FROM merchant m
WHERE m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM coupon_template ct
                  WHERE ct.merchant_id = m.id AND ct.deleted = 0 AND ct.name = '新人首单券');
