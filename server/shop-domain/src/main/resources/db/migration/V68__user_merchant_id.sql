-- C 端用户归属商户。历史用户允许为空，新用户由微信登录流程写入。
ALTER TABLE `user`
    ADD COLUMN merchant_id BIGINT DEFAULT NULL COMMENT '首次注册所属商户 ID' AFTER id,
    ADD KEY idx_user_merchant_created (merchant_id, created_at, id);

-- 仅为可以明确归属到单一商户的历史用户回填，存在多商户关系的用户保持 NULL。
UPDATE `user` u
JOIN (
    SELECT user_id, MIN(merchant_id) AS merchant_id
    FROM (
        SELECT user_id, merchant_id FROM member_profile WHERE deleted = 0
        UNION ALL
        SELECT user_id, merchant_id FROM `order` WHERE deleted = 0
        UNION ALL
        SELECT user_id, merchant_id FROM cart_item WHERE deleted = 0
        UNION ALL
        SELECT user_id, merchant_id FROM user_notification WHERE deleted = 0
        UNION ALL
        SELECT user_id, merchant_id FROM user_coupon WHERE deleted = 0
        UNION ALL
        SELECT user_id, merchant_id FROM points_account WHERE deleted = 0
        UNION ALL
        SELECT user_id, merchant_id FROM points_ledger WHERE deleted = 0
        UNION ALL
        SELECT user_id, merchant_id FROM points_redeem_record WHERE deleted = 0
    ) merchant_links
    GROUP BY user_id
    HAVING COUNT(DISTINCT merchant_id) = 1
) links ON links.user_id = u.id
SET u.merchant_id = links.merchant_id
WHERE u.merchant_id IS NULL;
