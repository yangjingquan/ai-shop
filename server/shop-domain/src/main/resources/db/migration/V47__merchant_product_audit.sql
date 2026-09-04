ALTER TABLE product
    ADD COLUMN audit_operator_type TINYINT DEFAULT NULL COMMENT '审核操作人类型：1运营管理员 2商户账号' AFTER audited_by;

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:product:audit', '审核商品', '商品管理', 'BUTTON', 45)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id
FROM merchant_role mr
JOIN merchant_permission mp ON mp.code = 'merchant:product:audit'
WHERE mr.code IN ('owner', 'operator');
