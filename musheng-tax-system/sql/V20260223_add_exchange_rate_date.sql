-- 广告数据表添加汇率取值日期字段
-- 执行时间: 2026-02-23
-- 说明: 用于记录查询汇率时使用的日期，便于追溯和统计

ALTER TABLE `t_advertising_data`
ADD COLUMN `exchange_rate_date` date DEFAULT NULL COMMENT '汇率取值日期（用于查询汇率的日期）' AFTER `exchange_rate`;
