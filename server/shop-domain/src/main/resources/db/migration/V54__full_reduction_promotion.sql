CREATE TABLE promotion_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    activity_type VARCHAR(32) NOT NULL COMMENT 'FULL_REDUCTION/FULL_DISCOUNT',
    priority INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=draft 1=enabled 2=disabled',
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    scope_type TINYINT NOT NULL DEFAULT 0 COMMENT '0=all 1=categories 2=products',
    stack_new_user_coupon TINYINT NOT NULL DEFAULT 0,
    stack_repurchase_coupon TINYINT NOT NULL DEFAULT 0,
    show_recommendations TINYINT NOT NULL DEFAULT 0,
    budget_amount DECIMAL(12,2) NULL,
    max_order_count INT NULL,
    reserved_budget DECIMAL(12,2) NOT NULL DEFAULT 0,
    reserved_order_count INT NOT NULL DEFAULT 0,
    paid_budget DECIMAL(12,2) NOT NULL DEFAULT 0,
    paid_order_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_promotion_active (merchant_id, status, start_at, end_at, priority, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE promotion_threshold (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    threshold_amount DECIMAL(12,2) NOT NULL,
    reduction_amount DECIMAL(12,2) NULL,
    discount_rate DECIMAL(4,2) NULL COMMENT '折扣，例如8.80表示8.8折',
    discount_cap DECIMAL(12,2) NULL COMMENT '本阶梯最高优惠金额',
    sort INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_promotion_threshold_activity (activity_id, sort, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE promotion_scope (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    target_type TINYINT NOT NULL COMMENT '1=category 2=product 3=excluded_product 4=recommend_product',
    target_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_promotion_scope (activity_id, target_type, target_id, deleted),
    KEY idx_promotion_scope_activity (activity_id, target_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE promotion_order_reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    qualified_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    snapshot_json TEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=reserved 1=paid 2=released',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_promotion_reservation_order (order_no),
    KEY idx_promotion_reservation_activity_status (activity_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `order`
    ADD COLUMN promotion_activity_id BIGINT NULL AFTER coupon_snapshot_json,
    ADD COLUMN promotion_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER promotion_activity_id,
    ADD COLUMN promotion_snapshot_json TEXT NULL AFTER promotion_discount_amount;

ALTER TABLE refund_application ADD COLUMN refund_item_json TEXT NULL AFTER refund_amount;

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:promotion:view', '查看满减满折', '营销活动', 'MENU', 112),
('merchant:promotion:manage', '配置满减满折', '营销活动', 'BUTTON', 113)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('operator', 'owner') AND mp.code IN ('merchant:promotion:view', 'merchant:promotion:manage');
