ALTER TABLE merchant
  ADD COLUMN wx_app_id VARCHAR(64) DEFAULT '' COMMENT '微信小程序 AppID' AFTER contact_phone,
  ADD COLUMN wx_secret VARCHAR(128) DEFAULT '' COMMENT '微信小程序 AppSecret' AFTER wx_app_id;
