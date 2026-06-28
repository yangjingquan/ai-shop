-- 插入一个初始运营账号: admin / admin123（幂等：重复执行时更新密码）
INSERT INTO admin_user (username, password_hash, role)
VALUES ('admin', '$2a$10$rh5wna6Xhb3tKVmJK3EKpuZArt31w5oqNGiDzowuYqRYyGX8ay6FK', 'admin')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);

-- 插入测试商家
INSERT INTO merchant (id, name, logo, contact_name, contact_phone, status, created_by_admin_id)
VALUES (1, '测试商家', '', '张三', '13800000001', 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 插入测试商家账号: merchant01 / merchant123
INSERT INTO merchant_user (merchant_id, username, password_hash, role)
VALUES (1, 'merchant01', '$2a$10$0VpzXiThga7aTISvjSOteuE9UKxIO6CQo/mpl5kk9jrpuiaaMiLIe', 'merchant')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);
