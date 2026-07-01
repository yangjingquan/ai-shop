ALTER TABLE merchant
    ADD COLUMN wx_mch_id VARCHAR(32) DEFAULT '' COMMENT '微信支付商户号' AFTER wx_secret;
