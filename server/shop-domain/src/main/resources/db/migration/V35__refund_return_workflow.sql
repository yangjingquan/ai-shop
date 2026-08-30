ALTER TABLE refund_application
    ADD COLUMN return_required TINYINT NOT NULL DEFAULT 0 COMMENT '1=退款前需退货' AFTER auto_refund,
    ADD COLUMN return_ship_company VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户退货承运商' AFTER return_required,
    ADD COLUMN return_ship_no VARCHAR(64) NOT NULL DEFAULT '' COMMENT '用户退货单号' AFTER return_ship_company,
    ADD COLUMN return_ship_time DATETIME DEFAULT NULL COMMENT '用户填写退货时间' AFTER return_ship_no,
    ADD COLUMN return_received_time DATETIME DEFAULT NULL COMMENT '商家验货时间' AFTER return_ship_time,
    ADD COLUMN return_receive_note VARCHAR(255) NOT NULL DEFAULT '' COMMENT '商家验货备注' AFTER return_received_time,
    ADD COLUMN evidence_urls JSON DEFAULT NULL COMMENT '退款/退货凭证图片' AFTER reason;

ALTER TABLE refund_application
    ADD KEY idx_refund_return_status (merchant_id, status, return_required, updated_at);
