ALTER TABLE merchant
    MODIFY COLUMN wx_pay_api_v3_key VARCHAR(256) DEFAULT '' COMMENT '微信支付 API v3 密钥（AES-GCM 密文）' AFTER wx_mch_id;
