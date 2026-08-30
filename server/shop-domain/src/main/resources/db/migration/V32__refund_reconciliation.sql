ALTER TABLE refund_application
    ADD COLUMN auto_refund TINYINT NOT NULL DEFAULT 0 COMMENT '1=系统自动发起退款' AFTER refund_time,
    ADD COLUMN refund_reconcile_at DATETIME DEFAULT NULL COMMENT '最近一次主动查退款时间' AFTER auto_refund,
    ADD COLUMN refund_reconcile_attempts INT NOT NULL DEFAULT 0 COMMENT '主动查退款/补偿次数' AFTER refund_reconcile_at,
    ADD COLUMN refund_reconcile_error VARCHAR(500) NOT NULL DEFAULT '' COMMENT '最近一次退款对账错误' AFTER refund_reconcile_attempts,
    ADD KEY idx_refund_reconcile (status, auto_refund, refund_reconcile_at);

-- 旧模型中 status=3 代表整团取消；新模型将 3 用于“失败且全部退款”，先迁移旧值。
UPDATE group_buy_group SET status = 4 WHERE status = 3;

ALTER TABLE group_buy_group
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0
        COMMENT '0=WAIT_GROUP 1=FORMED 2=FAILED_WAIT_REFUND 3=FAILED_REFUNDED 4=CANCELLED';

ALTER TABLE group_buy_member
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0
        COMMENT '0=WAIT_PAY 1=PAID 2=CANCELLED 3=WAIT_REFUND 4=REFUNDED';
