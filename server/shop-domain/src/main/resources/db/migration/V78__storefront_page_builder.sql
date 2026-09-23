CREATE TABLE storefront_page (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  merchant_id BIGINT UNSIGNED NOT NULL,
  page_type VARCHAR(16) NOT NULL COMMENT 'HOME/TOPIC',
  page_key VARCHAR(100) NOT NULL COMMENT 'HOME 或 TOPIC-<slug>',
  slug VARCHAR(80) NULL,
  draft_slug VARCHAR(80) NULL,
  title VARCHAR(128) NOT NULL,
  summary VARCHAR(256) NOT NULL DEFAULT '',
  cover_image VARCHAR(512) NOT NULL DEFAULT '',
  draft_title VARCHAR(128) NULL,
  draft_summary VARCHAR(256) NULL,
  draft_cover_image VARCHAR(512) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
  draft_json LONGTEXT NOT NULL,
  published_json LONGTEXT NULL,
  published_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_storefront_page_key (merchant_id, page_key, deleted),
  UNIQUE KEY uk_storefront_page_slug (merchant_id, slug, deleted),
  KEY idx_storefront_page_list (merchant_id, page_type, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺首页与专题页草稿及发布快照';

INSERT INTO merchant_permission (code, name, module, type, sort) VALUES
('merchant:home:config', '店铺装修', '店铺装修', 'MENU', 150)
ON DUPLICATE KEY UPDATE name=VALUES(name), module=VALUES(module), type=VALUES(type), sort=VALUES(sort);

INSERT IGNORE INTO merchant_role_permission (role_id, permission_id)
SELECT mr.id, mp.id FROM merchant_role mr JOIN merchant_permission mp
WHERE mr.code IN ('operator', 'owner', 'MERCHANT_ADMIN', 'MERCHANT_OPERATOR')
  AND mp.code = 'merchant:home:config';
