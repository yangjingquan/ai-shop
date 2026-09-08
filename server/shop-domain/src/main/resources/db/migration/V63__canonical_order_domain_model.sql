-- I0-02: existing numeric order_type/status remain the canonical persisted codes.
-- The new fields make fulfilment and immutable snapshots explicit.
ALTER TABLE `order`
  ADD COLUMN fulfillment_method TINYINT NOT NULL DEFAULT 1 COMMENT '0=NONE 1=EXPRESS' AFTER order_type,
  ADD COLUMN order_snapshot_json JSON DEFAULT NULL COMMENT '下单时订单领域快照' AFTER pricing_snapshot_json,
  ADD KEY idx_order_fulfillment_status (fulfillment_method, status, deleted);

ALTER TABLE order_item
  ADD COLUMN item_snapshot_json JSON DEFAULT NULL COMMENT '下单时商品行快照' AFTER pricing_snapshot_json;

CREATE TABLE order_state_transition (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT UNSIGNED NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  from_state TINYINT NULL,
  to_state TINYINT NOT NULL,
  event VARCHAR(64) NOT NULL,
  reason VARCHAR(255) NOT NULL DEFAULT '',
  payload_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_order_transition (order_id, created_at),
  KEY idx_order_no_transition (order_no, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单标准状态流转审计';

-- All existing persisted orders in this system represent physical-goods fulfilment.
UPDATE `order` SET fulfillment_method = 1 WHERE fulfillment_method IS NULL;
