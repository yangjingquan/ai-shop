-- 团实例必须绑定具体 SKU，避免不同规格共用同一拼团。
SET @has_group_buy_sku_id = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'group_buy_group'
    AND column_name = 'sku_id'
);

SET @add_group_buy_sku_id = IF(
  @has_group_buy_sku_id = 0,
  'ALTER TABLE group_buy_group ADD COLUMN sku_id BIGINT UNSIGNED NULL COMMENT ''团购 SKU ID'' AFTER product_id',
  'SELECT 1'
);

PREPARE stmt_add_group_buy_sku_id FROM @add_group_buy_sku_id;
EXECUTE stmt_add_group_buy_sku_id;
DEALLOCATE PREPARE stmt_add_group_buy_sku_id;

-- 对历史上只有一个明确 SKU 的团实例进行回填；无法唯一确定的记录保留 NULL，避免误绑规格。
UPDATE group_buy_group g
JOIN (
  SELECT o.group_buy_group_id AS group_id, MIN(oi.sku_id) AS sku_id
  FROM `order` o
  JOIN order_item oi ON oi.order_id = o.id
  WHERE o.order_type = 1
    AND o.group_buy_group_id IS NOT NULL
    AND oi.sku_id IS NOT NULL
  GROUP BY o.group_buy_group_id
  HAVING COUNT(DISTINCT oi.sku_id) = 1
) old_group_sku ON old_group_sku.group_id = g.id
SET g.sku_id = old_group_sku.sku_id
WHERE g.sku_id IS NULL;

-- 新安装/历史数据已完整回填时收紧约束；仍有无法安全回填的历史数据则暂不阻断迁移。
SET @missing_group_buy_sku_id = (
  SELECT COUNT(*)
  FROM group_buy_group
  WHERE sku_id IS NULL
);

SET @require_group_buy_sku_id = IF(
  @missing_group_buy_sku_id = 0,
  'ALTER TABLE group_buy_group MODIFY COLUMN sku_id BIGINT UNSIGNED NOT NULL COMMENT ''团购 SKU ID''',
  'SELECT 1'
);

PREPARE stmt_require_group_buy_sku_id FROM @require_group_buy_sku_id;
EXECUTE stmt_require_group_buy_sku_id;
DEALLOCATE PREPARE stmt_require_group_buy_sku_id;
