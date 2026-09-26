INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:settlement:view', '查看结算中心', '结算中心', 'MENU', 155),
('merchant:bundle:view', '查看搭配购套餐', '搭配购', 'MENU', 156),
('merchant:bundle:manage', '管理搭配购套餐', '搭配购', 'BUTTON', 157)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort);

-- Preserve effective access for existing custom roles before separating the permissions.
INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT rp.role_id, destination.id
FROM merchant_role_permission rp
JOIN merchant_permission source ON source.id = rp.permission_id
JOIN merchant_permission destination ON destination.code = CASE source.code
    WHEN 'merchant:dashboard:view' THEN 'merchant:settlement:view'
    WHEN 'merchant:marketing:view' THEN 'merchant:bundle:view'
    WHEN 'merchant:marketing:feature:update' THEN 'merchant:bundle:manage'
END
WHERE source.code IN (
    'merchant:dashboard:view', 'merchant:marketing:view', 'merchant:marketing:feature:update'
);
