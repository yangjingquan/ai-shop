ALTER TABLE refund_application
    ADD COLUMN out_refund_no VARCHAR(64) DEFAULT '' COMMENT '商户退款单号，微信退款幂等键' AFTER order_no,
    ADD COLUMN wx_refund_id VARCHAR(64) DEFAULT '' COMMENT '微信退款单号' AFTER out_refund_no,
    ADD COLUMN refund_amount DECIMAL(12,2) DEFAULT NULL COMMENT '退款金额' AFTER status,
    ADD COLUMN refund_fail_reason VARCHAR(500) DEFAULT '' COMMENT '退款失败原因' AFTER refund_amount,
    ADD COLUMN refund_raw_payload TEXT COMMENT '退款通知原文' AFTER refund_fail_reason,
    ADD COLUMN refund_time DATETIME DEFAULT NULL COMMENT '退款成功时间' AFTER refund_raw_payload;

UPDATE refund_application
SET out_refund_no = CONCAT('LEGACY_', id)
WHERE out_refund_no = '' OR out_refund_no IS NULL;

-- V6/V7 中的 status=1 只代表“商家已同意”，并没有调用过微信退款；迁移后必须重新进入待处理。
UPDATE refund_application
SET status = 0
WHERE status = 1;

ALTER TABLE refund_application
    MODIFY COLUMN out_refund_no VARCHAR(64) NOT NULL COMMENT '商户退款单号，微信退款幂等键',
    ADD UNIQUE KEY uk_out_refund_no (out_refund_no);
