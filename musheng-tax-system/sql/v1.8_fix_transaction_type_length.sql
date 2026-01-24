-- ============================================================
-- 修复交易类型字段长度 & 更新去重索引
-- 版本: v1.8
-- 日期: 2026-01-24
-- ============================================================

-- 1. 扩大 transaction_type 字段长度
-- 问题: 德语交易类型 "Gebühren für Kundenrücksendungen mit Versand durch Amazon" 
--       长度为60字符，超过原定义的 VARCHAR(50)
ALTER TABLE t_sales_data MODIFY COLUMN transaction_type VARCHAR(100) NOT NULL COMMENT '交易类型(Order/Refund/Shipment等，德语可达60字符)';

-- 2. 更新去重索引
-- 唯一键：settlement_id + order_id + transaction_type + sku
-- 注：不包含 transaction_date，因为原始数据和ERP数据的时间不一致
DROP INDEX IF EXISTS idx_sales_dedup ON t_sales_data;
CREATE INDEX idx_sales_dedup ON t_sales_data (settlement_id, order_id, transaction_type, sku);

-- 验证
-- SHOW INDEX FROM t_sales_data WHERE Key_name = 'idx_sales_dedup';
