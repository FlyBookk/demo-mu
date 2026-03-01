-- 广告明细表补充汇率字段（若表已存在且缺少以下列时执行）
-- 执行时间: 2026-03-01
-- 说明：汇率、汇率取值日期、费用人民币，由后端导入时按发票开具日期匹配并持久化

-- 检查：若报错 "Duplicate column name" 说明列已存在，可忽略
ALTER TABLE `t_advertising_bill_item`
  ADD COLUMN `exchange_rate` decimal(10,6) DEFAULT NULL COMMENT '汇率（按发票开具日期取值，与配送/销售一致）' AFTER `ad_type`,
  ADD COLUMN `exchange_rate_date` date DEFAULT NULL COMMENT '汇率取值日期' AFTER `exchange_rate`,
  ADD COLUMN `amount_cny` decimal(15,4) DEFAULT NULL COMMENT '费用人民币' AFTER `exchange_rate_date`;
