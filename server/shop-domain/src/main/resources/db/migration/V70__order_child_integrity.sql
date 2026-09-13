-- T-006：订单为不可物理删除的业务根，防止支付、退款、预占及拼团成员残留为孤儿记录。
-- 此迁移需在清理现有孤儿数据后执行；所有线上业务删除均使用 deleted 逻辑删除，不受此约束影响。
ALTER TABLE order_item
  ADD CONSTRAINT fk_order_item_order
  FOREIGN KEY (order_id) REFERENCES `order` (id) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE payment_log
  ADD CONSTRAINT fk_payment_log_order
  FOREIGN KEY (order_no) REFERENCES `order` (order_no) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE refund_application
  ADD CONSTRAINT fk_refund_application_order
  FOREIGN KEY (order_no) REFERENCES `order` (order_no) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE resource_reservation
  ADD CONSTRAINT fk_resource_reservation_order
  FOREIGN KEY (order_no) REFERENCES `order` (order_no) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE group_buy_member
  ADD CONSTRAINT fk_group_buy_member_order
  FOREIGN KEY (order_id) REFERENCES `order` (id) ON DELETE RESTRICT ON UPDATE RESTRICT;
