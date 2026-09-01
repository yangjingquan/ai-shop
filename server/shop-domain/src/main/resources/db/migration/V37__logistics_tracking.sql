-- 物流履约闭环：保存快递鸟所需的承运商编码和最近一次查询摘要。
ALTER TABLE `order`
    ADD COLUMN shipper_code VARCHAR(20) NOT NULL DEFAULT '' COMMENT '快递鸟承运商编码' AFTER ship_company,
    ADD COLUMN logistics_state VARCHAR(10) NOT NULL DEFAULT '0' COMMENT '物流状态：0无轨迹/1揽收/2途中/3签收/4问题件' AFTER shipper_code,
    ADD COLUMN logistics_state_text VARCHAR(32) NOT NULL DEFAULT '' COMMENT '物流状态文案' AFTER logistics_state,
    ADD COLUMN logistics_last_time DATETIME NULL COMMENT '最新轨迹时间' AFTER logistics_state_text,
    ADD COLUMN logistics_last_content VARCHAR(500) NOT NULL DEFAULT '' COMMENT '最新轨迹内容' AFTER logistics_last_time,
    ADD COLUMN logistics_synced_at DATETIME NULL COMMENT '最近一次物流查询时间' AFTER logistics_last_content,
    ADD COLUMN logistics_error VARCHAR(500) NOT NULL DEFAULT '' COMMENT '最近一次物流查询错误' AFTER logistics_synced_at;

CREATE TABLE order_logistics_trace (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    shipper_code VARCHAR(20) NOT NULL DEFAULT '',
    logistic_code VARCHAR(64) NOT NULL,
    state VARCHAR(10) NOT NULL DEFAULT '0',
    accept_time DATETIME NOT NULL,
    accept_station VARCHAR(500) NOT NULL DEFAULT '',
    trace_hash CHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order_trace_hash (order_no, trace_hash),
    KEY idx_order_trace_time (order_no, accept_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单物流轨迹快照';
