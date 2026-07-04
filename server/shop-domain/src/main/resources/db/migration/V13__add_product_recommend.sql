ALTER TABLE product
  ADD COLUMN is_recommend TINYINT(1) DEFAULT 0 COMMENT '1 推荐 / 0 不推荐' AFTER status,
  ADD KEY idx_status_recommend_sort (status, is_recommend, sort, deleted);
