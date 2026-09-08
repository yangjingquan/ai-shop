-- I1-02/I1-03/I2-01: durable operations rather than page-local state changes.
CREATE TABLE reconciliation_task (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL,
  task_type VARCHAR(24) NOT NULL COMMENT 'PAYMENT,REFUND,SETTLEMENT',
  merchant_id BIGINT UNSIGNED DEFAULT NULL,
  range_start DATETIME DEFAULT NULL,
  range_end DATETIME DEFAULT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,RUNNING,SUCCESS,PARTIAL,FAILED',
  idempotency_key VARCHAR(128) NOT NULL,
  summary_json JSON DEFAULT NULL,
  error_message VARCHAR(500) NOT NULL DEFAULT '',
  requested_by VARCHAR(64) NOT NULL DEFAULT '',
  started_at DATETIME DEFAULT NULL,
  finished_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_reconcile_task_idempotency (idempotency_key),
  UNIQUE KEY uk_reconcile_task_no (task_no),
  KEY idx_reconcile_task_scope (task_type, merchant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付、退款、结算对账任务与执行日志';

CREATE TABLE after_sales_intervention (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  refund_id BIGINT UNSIGNED NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  merchant_id BIGINT UNSIGNED NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN,RESOLVED,CLOSED',
  reason VARCHAR(500) NOT NULL,
  evidence_note VARCHAR(1000) NOT NULL DEFAULT '',
  resolution VARCHAR(1000) NOT NULL DEFAULT '',
  opened_by VARCHAR(64) NOT NULL DEFAULT '',
  resolved_by VARCHAR(64) NOT NULL DEFAULT '',
  resolved_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_intervention_refund (refund_id, status),
  KEY idx_intervention_merchant (merchant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台售后介入工单';

CREATE TABLE data_sync_event (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  scope VARCHAR(32) NOT NULL COMMENT 'PRODUCT,INVENTORY,BANNER,MARKETING,MERCHANT,COUPON,ORDER,SETTLEMENT',
  merchant_id BIGINT UNSIGNED DEFAULT NULL,
  resource_id VARCHAR(96) NOT NULL DEFAULT '',
  action VARCHAR(32) NOT NULL DEFAULT 'INVALIDATE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_data_sync_scope (scope, merchant_id, id),
  KEY idx_data_sync_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三端缓存失效事件';
