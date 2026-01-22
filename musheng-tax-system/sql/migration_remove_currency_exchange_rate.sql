-- 货币表删除汇率字段
-- 汇率是动态变化的，不应该保存在货币表中
-- 实际汇率应该从 t_exchange_rate 表获取

ALTER TABLE t_currency
DROP COLUMN exchange_rate;
