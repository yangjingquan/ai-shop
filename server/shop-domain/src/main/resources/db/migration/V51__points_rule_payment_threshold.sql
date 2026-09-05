ALTER TABLE points_rule
  ADD COLUMN pay_amount_yuan INT NOT NULL DEFAULT 1 COMMENT '每满多少实付商品金额赠一次积分' AFTER register_points;
