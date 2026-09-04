-- 用户侧隐藏订单，不影响商家后台及订单相关业务数据。
ALTER TABLE `order`
  ADD COLUMN user_deleted TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '用户侧隐藏：0=显示 1=隐藏' AFTER deleted,
  ADD KEY idx_user_status_user_deleted (user_id, status, user_deleted, deleted, created_at);
