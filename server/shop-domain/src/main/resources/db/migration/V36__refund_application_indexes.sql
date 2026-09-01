-- V6 的 uk_order_active 以 (order_no, status) 做唯一约束，无法支持同一订单的多次部分退款、失败重试和重新申请。
-- 未决退款由事务锁和服务层状态校验保证，同一订单的历史申请必须允许保留。
ALTER TABLE refund_application
    DROP INDEX uk_order_active,
    ADD KEY idx_order_status (order_no, status);
