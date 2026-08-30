ALTER TABLE merchant_wechat_config
    ADD COLUMN active_wx_app_id VARCHAR(64)
        AS (NULLIF(TRIM(wx_app_id), '')) STORED,
    ADD UNIQUE KEY uk_merchant_active_wx_app_id (active_wx_app_id);
