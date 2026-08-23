ALTER TABLE merchant
    ADD COLUMN wx_pay_api_v3_key VARCHAR(128) DEFAULT '' COMMENT '微信支付 API v3 密钥' AFTER wx_mch_id,
    ADD COLUMN wx_pay_mch_serial_no VARCHAR(128) DEFAULT '' COMMENT '微信支付商户 API 证书序列号' AFTER wx_pay_api_v3_key,
    ADD COLUMN wx_pay_private_key TEXT COMMENT '微信支付商户 API 私钥 PEM' AFTER wx_pay_mch_serial_no,
    ADD COLUMN wx_pay_notify_url VARCHAR(255) DEFAULT '' COMMENT '微信支付回调地址' AFTER wx_pay_private_key,
    ADD COLUMN wx_pay_enabled TINYINT(1) DEFAULT 0 COMMENT '是否启用微信支付' AFTER wx_pay_notify_url;
