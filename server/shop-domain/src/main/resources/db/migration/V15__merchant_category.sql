CREATE TABLE merchant_category (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    merchant_id BIGINT UNSIGNED NOT NULL COMMENT '商家ID',
    source_category_id BIGINT UNSIGNED DEFAULT NULL COMMENT '来源平台分类ID，手动创建为空',
    parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类ID，0表示一级',
    name VARCHAR(32) NOT NULL COMMENT '分类名称',
    icon VARCHAR(255) DEFAULT '' COMMENT '分类图标',
    level TINYINT NOT NULL COMMENT '层级：1一级 2二级',
    sort INT DEFAULT 0 COMMENT '排序，越小越靠前',
    status TINYINT(1) DEFAULT 1 COMMENT '状态：1启用 0禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0,
    KEY idx_merchant_parent (merchant_id, parent_id, deleted),
    KEY idx_merchant_level_sort (merchant_id, level, sort, deleted),
    KEY idx_merchant_source (merchant_id, source_category_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家分类';
