ALTER TABLE merchant_user
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用' AFTER role;

CREATE TABLE merchant_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    module VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'API',
    parent_id BIGINT DEFAULT NULL,
    sort INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_permission_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE merchant_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) DEFAULT '',
    builtin TINYINT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_merchant_role_code (merchant_id, code),
    INDEX idx_role_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE merchant_role_permission (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    INDEX idx_role_permission_permission (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE merchant_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    INDEX idx_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:dashboard:view', '查看经营首页', '经营首页', 'MENU', 10),
('merchant:profile:view', '查看店铺信息', '店铺信息', 'MENU', 20),
('merchant:profile:update', '修改店铺信息', '店铺信息', 'BUTTON', 21),
('merchant:category:view', '查看分类', '分类管理', 'MENU', 30),
('merchant:category:create', '新增分类', '分类管理', 'BUTTON', 31),
('merchant:category:update', '编辑分类', '分类管理', 'BUTTON', 32),
('merchant:category:status', '启停分类', '分类管理', 'BUTTON', 33),
('merchant:category:delete', '删除分类', '分类管理', 'BUTTON', 34),
('merchant:category:import', '导入平台分类', '分类管理', 'BUTTON', 35),
('merchant:product:view', '查看商品', '商品管理', 'MENU', 40),
('merchant:product:create', '新增商品', '商品管理', 'BUTTON', 41),
('merchant:product:update', '编辑商品', '商品管理', 'BUTTON', 42),
('merchant:product:status', '上下架商品', '商品管理', 'BUTTON', 43),
('merchant:product:delete', '删除商品', '商品管理', 'BUTTON', 44),
('merchant:inventory:view', '查看库存', '库存管理', 'MENU', 50),
('merchant:inventory:adjust', '调整库存', '库存管理', 'BUTTON', 51),
('merchant:inventory:transaction:view', '查看库存流水', '库存管理', 'BUTTON', 52),
('merchant:banner:view', '查看Banner', 'Banner配置', 'MENU', 60),
('merchant:banner:create', '新增Banner', 'Banner配置', 'BUTTON', 61),
('merchant:banner:update', '编辑Banner', 'Banner配置', 'BUTTON', 62),
('merchant:banner:delete', '删除Banner', 'Banner配置', 'BUTTON', 63),
('merchant:order:view', '查看订单', '订单发货', 'MENU', 70),
('merchant:order:detail', '查看订单详情', '订单发货', 'BUTTON', 71),
('merchant:order:ship', '订单发货', '订单发货', 'BUTTON', 72),
('merchant:order:logistics:view', '查看物流', '订单发货', 'BUTTON', 73),
('merchant:order:logistics:refresh', '刷新物流', '订单发货', 'BUTTON', 74),
('merchant:refund:view', '查看退款', '退款审批', 'MENU', 80),
('merchant:refund:approve', '审批退款', '退款审批', 'BUTTON', 81),
('merchant:refund:return-received', '确认收货', '退款审批', 'BUTTON', 82),
('merchant:refund:retry', '重试退款', '退款审批', 'BUTTON', 83),
('merchant:file:upload', '上传文件', '文件管理', 'API', 90),
('merchant:file:delete', '删除文件', '文件管理', 'API', 91),
('merchant:rbac:manage', '配置账号与权限', '账号与权限', 'MENU', 100)
ON DUPLICATE KEY UPDATE name = VALUES(name), module = VALUES(module), type = VALUES(type), sort = VALUES(sort);

INSERT INTO merchant_role (merchant_id, code, name, description, builtin, status, sort)
SELECT id, 'owner', '店主', '拥有商户后台全部权限', 1, 1, 10 FROM merchant WHERE deleted = 0
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), builtin = 1;

INSERT INTO merchant_role (merchant_id, code, name, description, builtin, status, sort)
SELECT id, 'operator', '运营人员', '负责商品、分类、Banner、订单和退款处理', 1, 1, 20 FROM merchant WHERE deleted = 0
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), builtin = 1;

INSERT INTO merchant_role (merchant_id, code, name, description, builtin, status, sort)
SELECT id, 'warehouse', '仓库人员', '负责库存和订单发货', 1, 1, 30 FROM merchant WHERE deleted = 0
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), builtin = 1;

INSERT INTO merchant_role (merchant_id, code, name, description, builtin, status, sort)
SELECT id, 'customer_service', '客服人员', '负责订单查询和退款处理', 1, 1, 40 FROM merchant WHERE deleted = 0
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description), builtin = 1;

INSERT IGNORE INTO merchant_user_role (user_id, role_id)
SELECT mu.id, mr.id
FROM merchant_user mu
JOIN merchant_role mr ON mr.merchant_id = mu.merchant_id AND mr.code = 'owner'
WHERE mu.deleted = 0;

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr CROSS JOIN merchant_permission mp
WHERE mr.code = 'owner';

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code = 'operator'
  AND mp.code IN (
    'merchant:dashboard:view', 'merchant:profile:view', 'merchant:category:view',
    'merchant:category:create', 'merchant:category:update', 'merchant:category:status',
    'merchant:category:delete', 'merchant:category:import', 'merchant:product:view',
    'merchant:product:create', 'merchant:product:update', 'merchant:product:status',
    'merchant:product:delete', 'merchant:banner:view', 'merchant:banner:create',
    'merchant:banner:update', 'merchant:banner:delete', 'merchant:order:view',
    'merchant:order:detail', 'merchant:order:ship', 'merchant:order:logistics:view',
    'merchant:order:logistics:refresh', 'merchant:refund:view', 'merchant:refund:approve',
    'merchant:refund:return-received', 'merchant:refund:retry', 'merchant:file:upload',
    'merchant:file:delete'
  );

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code = 'warehouse'
  AND mp.code IN (
    'merchant:dashboard:view', 'merchant:product:view', 'merchant:inventory:view',
    'merchant:inventory:adjust', 'merchant:inventory:transaction:view', 'merchant:order:view',
    'merchant:order:detail', 'merchant:order:ship', 'merchant:order:logistics:view',
    'merchant:order:logistics:refresh', 'merchant:file:upload', 'merchant:file:delete'
  );

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code = 'customer_service'
  AND mp.code IN (
    'merchant:dashboard:view', 'merchant:order:view', 'merchant:order:detail',
    'merchant:order:logistics:view', 'merchant:order:logistics:refresh', 'merchant:refund:view',
    'merchant:refund:approve', 'merchant:refund:return-received', 'merchant:refund:retry'
  );

UPDATE merchant_user SET status = 1 WHERE status IS NULL;
