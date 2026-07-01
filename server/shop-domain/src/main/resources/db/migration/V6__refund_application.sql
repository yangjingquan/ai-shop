CREATE TABLE refund_application (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0=PENDING 1=APPROVED 2=REJECTED',
  reject_reason VARCHAR(255) DEFAULT '',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order_active (order_no, status) COMMENT '同一订单同时只有一个未决申请(status=0)',
  KEY idx_merchant_status (merchant_id, status),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款申请';
