CREATE TABLE merchant_wechat_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL UNIQUE COMMENT '商户ID，一商户一条微信配置',
    wx_app_id VARCHAR(64) DEFAULT '' COMMENT '微信小程序 AppID',
    wx_secret VARCHAR(128) DEFAULT '' COMMENT '微信小程序 AppSecret',
    wx_mch_id VARCHAR(32) DEFAULT '' COMMENT '微信支付商户号',
    wx_pay_api_v3_key VARCHAR(256) DEFAULT '' COMMENT '微信支付 API v3 密钥（AES-GCM 密文）',
    wx_pay_mch_serial_no VARCHAR(128) DEFAULT '' COMMENT '微信支付商户 API 证书序列号',
    wx_pay_private_key TEXT COMMENT '微信支付商户 API 私钥 PEM（AES-GCM 密文）',
    wx_pay_notify_url VARCHAR(255) DEFAULT '' COMMENT '微信支付回调地址',
    wx_pay_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用微信支付',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_merchant_wechat_deleted (merchant_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户微信配置';

INSERT INTO merchant_wechat_config (
    merchant_id,
    wx_app_id,
    wx_secret,
    wx_mch_id,
    wx_pay_api_v3_key,
    wx_pay_mch_serial_no,
    wx_pay_private_key,
    wx_pay_notify_url,
    wx_pay_enabled,
    created_at,
    updated_at,
    deleted
)
SELECT
    id,
    COALESCE(wx_app_id, ''),
    COALESCE(wx_secret, ''),
    COALESCE(wx_mch_id, ''),
    COALESCE(wx_pay_api_v3_key, ''),
    COALESCE(wx_pay_mch_serial_no, ''),
    COALESCE(wx_pay_private_key, ''),
    COALESCE(wx_pay_notify_url, ''),
    COALESCE(wx_pay_enabled, 0),
    created_at,
    updated_at,
    deleted
FROM merchant;
