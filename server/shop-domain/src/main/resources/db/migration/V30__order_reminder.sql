ALTER TABLE `order`
    ADD COLUMN ship_reminder_at DATETIME DEFAULT NULL COMMENT '用户最近一次提醒发货时间' AFTER ship_time;

UPDATE user_address ua
JOIN (
    SELECT user_id, MIN(id) AS keep_id
    FROM user_address
    WHERE deleted = 0 AND is_default = 1
    GROUP BY user_id
) keep_row ON keep_row.user_id = ua.user_id
SET ua.is_default = IF(ua.id = keep_row.keep_id, 1, 0)
WHERE ua.deleted = 0 AND ua.is_default = 1;

ALTER TABLE user_address
    ADD COLUMN active_default_key TINYINT AS (IF(deleted = 0 AND is_default = 1, 1, NULL)) STORED,
    ADD UNIQUE KEY uk_user_default_active (user_id, active_default_key);

ALTER TABLE admin_user
    ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT '令牌版本';

ALTER TABLE merchant_user
    ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT '令牌版本';

ALTER TABLE `user`
    ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT '令牌版本';
