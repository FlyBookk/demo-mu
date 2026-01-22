-- 销售数据添加汇率字段
-- 交易日期当天汇率（对人民币），如果是节假日/周末则取下一个工作日汇率

ALTER TABLE t_sales_data
ADD COLUMN exchange_rate DECIMAL(10, 6) DEFAULT NULL COMMENT '交易日期当天汇率（对人民币）',
ADD COLUMN exchange_rate_date DATE DEFAULT NULL COMMENT '汇率实际取值日期（如果是节假日则为下一个工作日）';

-- 添加索引
CREATE INDEX idx_sales_exchange_rate_date ON t_sales_data(exchange_rate_date);
