ALTER TABLE `presale_sku`
  DROP INDEX `uk_presale_activity_sku`,
  ADD COLUMN `active_sku_id` BIGINT UNSIGNED
    GENERATED ALWAYS AS (IF(`deleted` = 0, `sku_id`, NULL)) STORED
    COMMENT '仅有效预售 SKU 参与唯一约束',
  ADD UNIQUE KEY `uk_presale_activity_sku` (`activity_id`, `active_sku_id`);
