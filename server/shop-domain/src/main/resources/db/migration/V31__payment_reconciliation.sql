ALTER TABLE `order`
    ADD COLUMN pay_reconcile_at DATETIME DEFAULT NULL COMMENT '最近一次主动查单时间' AFTER pay_transaction_id,
    ADD COLUMN pay_reconcile_attempts INT NOT NULL DEFAULT 0 COMMENT '主动查单次数' AFTER pay_reconcile_at,
    ADD COLUMN pay_reconcile_error VARCHAR(500) NOT NULL DEFAULT '' COMMENT '最近一次主动查单错误' AFTER pay_reconcile_attempts,
    ADD KEY idx_order_payment_reconcile (status, pay_reconcile_at, created_at, deleted);
