-- 将“何时发放”与“业务用途”分离，避免邀请阶梯券被新人首页当作新人首单券。
ALTER TABLE coupon_template
    ADD COLUMN purpose_codes VARCHAR(255) NOT NULL DEFAULT 'MARKETING_REWARD'
    COMMENT '逗号分隔：NEW_USER_FIRST_ORDER, REFERRAL_INVITEE, REFERRAL_INVITER, MARKETING_REWARD'
    AFTER issue_scene;

-- 默认券使用明确的新人首单用途；其他历史通用券先归为其他营销奖励，避免被首页自动领取。
UPDATE coupon_template
SET purpose_codes = 'NEW_USER_FIRST_ORDER'
WHERE issue_scene = 'NEW_USER' AND name = '新人首单券' AND deleted = 0;

-- 保留已有邀请活动配置：同一模板可承担多个用途，不会改变既有活动的发券结果。
UPDATE coupon_template ct
JOIN referral_campaign rc ON rc.invitee_coupon_template_id = ct.id AND rc.deleted = 0
SET ct.purpose_codes = CASE
    WHEN FIND_IN_SET('REFERRAL_INVITEE', ct.purpose_codes) > 0 THEN ct.purpose_codes
    ELSE CONCAT(ct.purpose_codes, ',REFERRAL_INVITEE')
END
WHERE ct.deleted = 0;

UPDATE coupon_template ct
JOIN referral_campaign rc ON rc.deleted = 0
    AND JSON_CONTAINS(rc.tier_config_json, JSON_OBJECT('inviterCouponTemplateId', ct.id))
SET ct.purpose_codes = CASE
    WHEN FIND_IN_SET('REFERRAL_INVITER', ct.purpose_codes) > 0 THEN ct.purpose_codes
    ELSE CONCAT(ct.purpose_codes, ',REFERRAL_INVITER')
END
WHERE ct.deleted = 0;
