-- 平台分类
CREATE TABLE category (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0 表示一级',
  name VARCHAR(32) NOT NULL,
  icon VARCHAR(255) DEFAULT '',
  level TINYINT NOT NULL COMMENT '1 一级 / 2 二级',
  sort INT DEFAULT 0,
  status TINYINT(1) DEFAULT 1 COMMENT '1 启用 / 0 禁用',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_parent (parent_id, deleted),
  KEY idx_level_sort (level, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台分类';

-- 商品 SPU
CREATE TABLE product (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  category_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(128) NOT NULL,
  subtitle VARCHAR(255) DEFAULT '',
  main_image VARCHAR(255) DEFAULT '',
  images JSON COMMENT '详情图 URL 列表',
  description LONGTEXT,
  min_price DECIMAL(10,2) DEFAULT 0,
  max_price DECIMAL(10,2) DEFAULT 0,
  total_stock INT DEFAULT 0,
  total_sales INT DEFAULT 0,
  status TINYINT(1) DEFAULT 0 COMMENT '1 上架 / 0 下架',
  sort INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_merchant_status (merchant_id, status, deleted),
  KEY idx_category_status (category_id, status, deleted),
  KEY idx_status_sort (status, sort, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品 SPU';

-- 规格定义
CREATE TABLE product_spec (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(32) NOT NULL,
  sort INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_product (product_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格定义';

-- 规格值
CREATE TABLE product_spec_value (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  spec_id BIGINT UNSIGNED NOT NULL,
  value VARCHAR(32) NOT NULL,
  sort INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_spec (spec_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品规格值';

-- SKU
CREATE TABLE product_sku (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT UNSIGNED NOT NULL,
  sku_code VARCHAR(64) DEFAULT '',
  spec_value_ids JSON COMMENT 'spec_value.id 列表，按 spec 顺序',
  spec_text VARCHAR(128) DEFAULT '' COMMENT '冗余可读，例: 黑色 / 256G',
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  stock INT NOT NULL DEFAULT 0,
  image VARCHAR(255) DEFAULT '',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) DEFAULT 0,
  KEY idx_product (product_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品 SKU';
