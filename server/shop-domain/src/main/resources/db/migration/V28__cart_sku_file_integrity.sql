-- 保留被订单引用的历史 SKU。active=1 才允许售卖，active=0 仅用于历史订单库存回滚。
ALTER TABLE product_sku
    ADD COLUMN active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1当前可售 0历史规格' AFTER image,
    ADD KEY idx_product_active (product_id, active, deleted);

-- 仅合并仍有效的重复购物车行；已删除行允许保留历史记录。
CREATE TEMPORARY TABLE cart_item_active_merge AS
SELECT user_id, sku_id, MIN(id) AS keep_id, SUM(quantity) AS total_quantity
FROM cart_item
WHERE deleted = 0
GROUP BY user_id, sku_id
HAVING COUNT(*) > 1;

UPDATE cart_item c
JOIN cart_item_active_merge m ON m.keep_id = c.id
SET c.quantity = LEAST(m.total_quantity, 2147483647);

DELETE c
FROM cart_item c
JOIN cart_item_active_merge m
  ON m.user_id = c.user_id AND m.sku_id = c.sku_id
WHERE c.deleted = 0 AND c.id <> m.keep_id;

DROP TEMPORARY TABLE cart_item_active_merge;

-- MySQL 唯一索引允许多个 NULL，因此用生成列只约束有效购物车行。
ALTER TABLE cart_item
    ADD COLUMN active_unique_key TINYINT AS (IF(deleted = 0, 1, NULL)) STORED,
    ADD UNIQUE KEY uk_cart_user_sku_active (user_id, sku_id, active_unique_key);

CREATE TABLE file_asset (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    public_url VARCHAR(500) NOT NULL,
    owner_type VARCHAR(16) NOT NULL COMMENT 'ADMIN / MERCHANT / USER',
    owner_id BIGINT UNSIGNED NOT NULL,
    merchant_id BIGINT UNSIGNED DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_file_asset_url (public_url),
    KEY idx_file_asset_owner (owner_type, owner_id, deleted),
    KEY idx_file_asset_merchant (merchant_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传文件归属与生命周期';
