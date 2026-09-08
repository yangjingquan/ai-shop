-- I0-03: one durable state machine for all order-bound inventory and entitlements.
CREATE TABLE resource_reservation (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED DEFAULT NULL,
  sku_id BIGINT UNSIGNED DEFAULT NULL,
  resource_type VARCHAR(32) NOT NULL COMMENT 'SKU,SECKILL_STOCK,COUPON,PROMOTION,POINTS_PRODUCT,POINTS_BALANCE',
  resource_id VARCHAR(96) NOT NULL,
  quantity INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0=RESERVED 1=CONFIRMED 2=RELEASED 3=RESTOCKED',
  reason VARCHAR(255) NOT NULL DEFAULT '',
  confirmed_at DATETIME DEFAULT NULL,
  released_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_resource_reservation (order_no, resource_type, resource_id, deleted),
  KEY idx_resource_reservation_order (order_no, status, deleted),
  KEY idx_resource_reservation_exception (status, created_at, deleted),
  KEY idx_resource_reservation_merchant (merchant_id, created_at, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单库存与权益预占流水';

ALTER TABLE `order`
  ADD COLUMN client_request_id VARCHAR(64) DEFAULT NULL COMMENT '客户端幂等请求号' AFTER rule_version,
  ADD UNIQUE KEY uk_order_client_request (user_id, client_request_id, deleted);

ALTER TABLE points_redeem_record
  ADD COLUMN client_request_id VARCHAR(64) DEFAULT NULL COMMENT '客户端幂等请求号' AFTER redeem_no,
  ADD UNIQUE KEY uk_points_redeem_request (merchant_id, user_id, client_request_id, deleted);
