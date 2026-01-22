-- 配送数据添加汇率字段
-- 用于存储配送日期当天的汇率（如果当天是节假日/周末，则取下一个工作日的汇率）

ALTER TABLE t_shipping_data
ADD COLUMN exchange_rate DECIMAL(10, 6) DEFAULT NULL COMMENT '配送日期当天汇率（对人民币）',
ADD COLUMN exchange_rate_date DATE DEFAULT NULL COMMENT '汇率实际取值日期（如果是节假日则为下一个工作日）';

-- 添加索引以便按汇率日期查询
CREATE INDEX idx_shipping_exchange_rate_date ON t_shipping_data(exchange_rate_date);
