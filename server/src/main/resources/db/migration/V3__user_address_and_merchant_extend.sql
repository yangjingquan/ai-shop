-- user 表 phone 改为 NULL 语义：未绑定 = NULL（多 NULL 不冲突），已绑定 = 唯一手机号
-- M1 期间 phone 列默认 ''，先把 '' 全部归一化为 NULL，再放宽列允许 NULL，最后加唯一索引
UPDATE user SET phone = NULL WHERE phone = '' OR phone IS NULL;
ALTER TABLE user MODIFY COLUMN phone VARCHAR(20) NULL DEFAULT NULL COMMENT '手机号，未绑定为 NULL';
ALTER TABLE user ADD UNIQUE KEY uk_user_phone (phone);

-- merchant 表扩可选展示字段
ALTER TABLE merchant
  ADD COLUMN description VARCHAR(500) DEFAULT '' COMMENT '店铺简介' AFTER logo,
  ADD COLUMN address VARCHAR(255) DEFAULT '' COMMENT '店铺地址' AFTER description;

-- 收货地址表
CREATE TABLE user_address (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  receiver VARCHAR(50) NOT NULL COMMENT '收货人',
  phone VARCHAR(20) NOT NULL COMMENT '联系电话',
  region VARCHAR(100) NOT NULL COMMENT '省市区，例: 北京市/北京市/朝阳区',
  detail VARCHAR(255) NOT NULL COMMENT '详细地址',
  is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_user (user_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址';
