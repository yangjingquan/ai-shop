-- 团购成员支付时间，供支付回调和成团逻辑记录成员付款时间。
SET @has_group_buy_member_paid_at = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'group_buy_member'
    AND column_name = 'paid_at'
);

SET @add_group_buy_member_paid_at = IF(
  @has_group_buy_member_paid_at = 0,
  'ALTER TABLE group_buy_member ADD COLUMN paid_at DATETIME NULL COMMENT ''成员支付时间'' AFTER status',
  'SELECT 1'
);

PREPARE stmt_add_group_buy_member_paid_at FROM @add_group_buy_member_paid_at;
EXECUTE stmt_add_group_buy_member_paid_at;
DEALLOCATE PREPARE stmt_add_group_buy_member_paid_at;
