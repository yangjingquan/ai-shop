-- 团购成员记录绑定订单主键，便于订单与成员数据一致关联。
SET @has_group_buy_member_order_id = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'group_buy_member'
    AND column_name = 'order_id'
);

SET @add_group_buy_member_order_id = IF(
  @has_group_buy_member_order_id = 0,
  'ALTER TABLE group_buy_member ADD COLUMN order_id BIGINT UNSIGNED NULL COMMENT ''关联订单 ID'' AFTER user_id',
  'SELECT 1'
);

PREPARE stmt_add_group_buy_member_order_id FROM @add_group_buy_member_order_id;
EXECUTE stmt_add_group_buy_member_order_id;
DEALLOCATE PREPARE stmt_add_group_buy_member_order_id;

-- 通过现有 order_no 回填历史成员记录；无法匹配的记录暂不误绑订单。
UPDATE group_buy_member m
JOIN `order` o ON o.order_no = m.order_no
SET m.order_id = o.id
WHERE m.order_id IS NULL;

-- 历史数据完整回填后收紧约束；仍有异常历史记录则暂不阻断迁移。
SET @missing_group_buy_member_order_id = (
  SELECT COUNT(*)
  FROM group_buy_member
  WHERE order_id IS NULL
);

SET @require_group_buy_member_order_id = IF(
  @missing_group_buy_member_order_id = 0,
  'ALTER TABLE group_buy_member MODIFY COLUMN order_id BIGINT UNSIGNED NOT NULL COMMENT ''关联订单 ID''',
  'SELECT 1'
);

PREPARE stmt_require_group_buy_member_order_id FROM @require_group_buy_member_order_id;
EXECUTE stmt_require_group_buy_member_order_id;
DEALLOCATE PREPARE stmt_require_group_buy_member_order_id;
