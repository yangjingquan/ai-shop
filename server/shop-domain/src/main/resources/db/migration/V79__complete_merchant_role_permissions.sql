-- Earlier freight and engagement migrations used role codes that do not exist in merchant_role.
INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id
FROM merchant_role mr
JOIN merchant_permission mp ON mp.code IN (
    'merchant:freight:view', 'merchant:freight:manage', 'merchant:engagement:manage'
)
WHERE mr.code = 'operator';

-- Keep stored owner grants in sync with the complete permission catalog.
INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id
FROM merchant_role mr
CROSS JOIN merchant_permission mp
WHERE mr.code = 'owner';
