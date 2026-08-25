-- V19 使用了 group_buy_min_users，当前领域模型统一使用 required_count。
SET @has_group_buy_min_users = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'product'
    AND column_name = 'group_buy_min_users'
);

SET @has_group_buy_required_count = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'product'
    AND column_name = 'group_buy_required_count'
);

SET @rename_group_buy_count = IF(
  @has_group_buy_min_users = 1 AND @has_group_buy_required_count = 0,
  'ALTER TABLE product CHANGE COLUMN group_buy_min_users group_buy_required_count INT DEFAULT NULL COMMENT ''几人成团'' AFTER group_buy_price',
  'SELECT 1'
);

PREPARE stmt_rename_group_buy_count FROM @rename_group_buy_count;
EXECUTE stmt_rename_group_buy_count;
DEALLOCATE PREPARE stmt_rename_group_buy_count;
