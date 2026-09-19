-- 支持商家确认订单时选择顾客自取，不再要求录入快递信息。
ALTER TABLE `order`
  MODIFY COLUMN fulfillment_method TINYINT NOT NULL DEFAULT 1 COMMENT '0=NONE 1=EXPRESS 2=PICKUP';
