CREATE TABLE product_review (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, merchant_id BIGINT UNSIGNED NOT NULL, product_id BIGINT UNSIGNED NOT NULL, order_item_id BIGINT UNSIGNED NOT NULL, order_no VARCHAR(32) NOT NULL, user_id BIGINT UNSIGNED NOT NULL,
 rating TINYINT NOT NULL, content VARCHAR(1000) NOT NULL DEFAULT '', images_json JSON NULL, anonymous TINYINT NOT NULL DEFAULT 0, status TINYINT NOT NULL DEFAULT 1 COMMENT '1展示 0隐藏', hidden_reason VARCHAR(255) NOT NULL DEFAULT '', merchant_reply VARCHAR(1000) NOT NULL DEFAULT '', replied_at DATETIME NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted TINYINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_review_order_item (order_item_id, deleted), KEY idx_review_product (merchant_id, product_id, status, created_at), KEY idx_review_user (user_id, merchant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE product_question (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, merchant_id BIGINT UNSIGNED NOT NULL, product_id BIGINT UNSIGNED NOT NULL, user_id BIGINT UNSIGNED NOT NULL,
 content VARCHAR(500) NOT NULL, anonymous TINYINT NOT NULL DEFAULT 0, purchased TINYINT NOT NULL DEFAULT 0, status TINYINT NOT NULL DEFAULT 1, hidden_reason VARCHAR(255) NOT NULL DEFAULT '', answer VARCHAR(1000) NOT NULL DEFAULT '', answered_at DATETIME NULL, answered_by BIGINT UNSIGNED NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted TINYINT NOT NULL DEFAULT 0,
 KEY idx_question_product (merchant_id, product_id, status, created_at), KEY idx_question_user (user_id, merchant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE user_product_favorite (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, merchant_id BIGINT UNSIGNED NOT NULL, product_id BIGINT UNSIGNED NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted TINYINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_favorite_user_product (user_id, merchant_id, product_id, deleted), KEY idx_favorite_user (user_id, merchant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE user_product_history (
 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT, user_id BIGINT UNSIGNED NOT NULL, merchant_id BIGINT UNSIGNED NOT NULL, product_id BIGINT UNSIGNED NOT NULL, view_count INT NOT NULL DEFAULT 1, last_viewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, deleted TINYINT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_history_user_product (user_id, merchant_id, product_id, deleted), KEY idx_history_user (user_id, merchant_id, last_viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO merchant_permission (code,name,module,type,sort) VALUES ('merchant:engagement:manage','管理商品互动','商品管理','MENU',147) ON DUPLICATE KEY UPDATE name=VALUES(name),module=VALUES(module),type=VALUES(type),sort=VALUES(sort);
INSERT IGNORE INTO merchant_role_permission (role_id,permission_id) SELECT mr.id,mp.id FROM merchant_role mr JOIN merchant_permission mp WHERE mr.code IN ('MERCHANT_ADMIN','MERCHANT_OPERATOR') AND mp.code='merchant:engagement:manage';
