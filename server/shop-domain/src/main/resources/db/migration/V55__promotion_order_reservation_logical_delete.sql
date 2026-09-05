-- PromotionOrderReservation extends BaseEntity, so MyBatis-Plus always filters it
-- with deleted = 0. V54 omitted that physical column; add it without changing the
-- already-released migration checksum.
SET @has_promotion_reservation_deleted = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'promotion_order_reservation'
    AND column_name = 'deleted'
);

SET @add_promotion_reservation_deleted = IF(
  @has_promotion_reservation_deleted = 0,
  'ALTER TABLE promotion_order_reservation ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 AFTER updated_at, ADD KEY idx_promotion_reservation_order_deleted (order_no, deleted)',
  'SELECT 1'
);

PREPARE stmt_add_promotion_reservation_deleted FROM @add_promotion_reservation_deleted;
EXECUTE stmt_add_promotion_reservation_deleted;
DEALLOCATE PREPARE stmt_add_promotion_reservation_deleted;
