SET @has_uk_group_user_deleted = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'group_buy_member'
    AND index_name = 'uk_group_user_deleted'
);

SET @add_uk_group_user_deleted = IF(
  @has_uk_group_user_deleted = 0,
  'ALTER TABLE group_buy_member ADD UNIQUE KEY uk_group_user_deleted (group_id, user_id, deleted)',
  'SELECT 1'
);

PREPARE stmt_add_uk_group_user_deleted FROM @add_uk_group_user_deleted;
EXECUTE stmt_add_uk_group_user_deleted;
DEALLOCATE PREPARE stmt_add_uk_group_user_deleted;

SET @has_ship_company = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'order'
    AND column_name = 'ship_company'
);

SET @add_ship_company = IF(
  @has_ship_company = 0,
  'ALTER TABLE `order` ADD COLUMN ship_company VARCHAR(64) DEFAULT '' COMMENT ''物流公司'' AFTER ship_no',
  'SELECT 1'
);

PREPARE stmt_add_ship_company FROM @add_ship_company;
EXECUTE stmt_add_ship_company;
DEALLOCATE PREPARE stmt_add_ship_company;
