ALTER TABLE coupon_template
    ADD COLUMN issue_scene VARCHAR(32) NOT NULL DEFAULT 'NEW_USER' COMMENT 'NEW_USER/REPURCHASE_AFTER_PAID' AFTER new_user_only,
    ADD COLUMN repurchase_target_type TINYINT NOT NULL DEFAULT 0 COMMENT '0=全部订单 1=指定商品 2=指定分类' AFTER issue_scene,
    ADD COLUMN repurchase_target_ids_json TEXT DEFAULT NULL AFTER repurchase_target_type,
    ADD COLUMN repurchase_min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER repurchase_target_ids_json,
    ADD COLUMN repurchase_first_purchase_only TINYINT NOT NULL DEFAULT 0 AFTER repurchase_min_order_amount,
    ADD COLUMN repurchase_priority INT NOT NULL DEFAULT 0 AFTER repurchase_first_purchase_only;

ALTER TABLE user_coupon
    ADD COLUMN issue_scene VARCHAR(32) NOT NULL DEFAULT 'NEW_USER' AFTER template_id,
    ADD COLUMN source_order_no VARCHAR(64) DEFAULT NULL AFTER issue_scene,
    ADD COLUMN invalid_reason VARCHAR(255) DEFAULT NULL AFTER used_order_no,
    ADD INDEX idx_user_coupon_source_order (source_order_no, deleted);

CREATE TABLE coupon_issue_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    source_order_no VARCHAR(64) NOT NULL,
    template_id BIGINT NOT NULL,
    user_coupon_id BIGINT DEFAULT NULL,
    issue_scene VARCHAR(32) NOT NULL DEFAULT 'REPURCHASE_AFTER_PAID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=待发放 1=已发放 2=跳过 3=退款取消 4=已回收',
    idempotency_key VARCHAR(160) NOT NULL,
    skip_reason VARCHAR(255) DEFAULT NULL,
    refund_id BIGINT DEFAULT NULL,
    revoked_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_coupon_issue_idempotency (idempotency_key, deleted),
    KEY idx_coupon_issue_source_order (merchant_id, source_order_no, deleted),
    KEY idx_coupon_issue_user_template (user_id, template_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE coupon_template
SET issue_scene = CASE WHEN new_user_only = 1 THEN 'NEW_USER' ELSE 'NEW_USER' END
WHERE issue_scene IS NULL OR issue_scene = '';
