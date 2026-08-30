ALTER TABLE merchant_wechat_config
    ADD COLUMN wx_pay_public_key TEXT COMMENT '微信支付公钥（AES-GCM 密文）' AFTER wx_pay_private_key,
    ADD COLUMN wx_pay_public_key_id VARCHAR(64) DEFAULT '' COMMENT '微信支付公钥 ID' AFTER wx_pay_public_key;
