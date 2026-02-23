-- 将 revenue_total 重命名为 total_amount
ALTER TABLE t_shipping_data
  CHANGE COLUMN revenue_total total_amount decimal(15,4) DEFAULT '0.0000' COMMENT '总计费用(导入时由各分项计算)';
