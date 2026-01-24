-- ============================================================
-- 销售数据 - ERP来源数据逻辑调整
-- 版本：v1.4
-- 日期：2026-01-24
-- 说明：添加结算类型字段，支持按来源维度聚合
-- 注：ERP的"来源"值存储在 transaction_type 字段中，不再单独创建 source 字段
-- ============================================================

-- 1. 添加结算类型字段（settlement_category）
-- 根据来源转换的标准化结算类型，如 ORDER, REFUND, SERVICE_FEE 等
ALTER TABLE t_sales_data
ADD COLUMN IF NOT EXISTS `settlement_category` VARCHAR(50) DEFAULT NULL COMMENT '结算类型(ORDER/REFUND/SERVICE_FEE等)' AFTER `transfer_status`;

-- 2. 创建索引以提高查询性能
CREATE INDEX IF NOT EXISTS idx_sales_settlement_category ON t_sales_data(`settlement_category`);

-- 3. 组合索引优化按交易类型（来源）聚合查询
CREATE INDEX IF NOT EXISTS idx_sales_txn_type_order_site_sku ON t_sales_data(`transaction_type`, `order_id`, `site_code`, `sku`);

-- ============================================================
-- 数据迁移（可选）：为现有ERP数据设置结算类型
-- 如果之前已有ERP导入数据，可以根据 transaction_type（来源值）设置 settlement_category
-- ============================================================

-- 注意：以下语句仅供参考，实际执行需要根据数据情况调整
-- UPDATE t_sales_data SET settlement_category = 'ORDER' WHERE source_type = 'ERP' AND transaction_type = 'Shipment';
-- UPDATE t_sales_data SET settlement_category = 'REFUND' WHERE source_type = 'ERP' AND transaction_type = 'Refund';
-- UPDATE t_sales_data SET settlement_category = 'SERVICE_FEE' WHERE source_type = 'ERP' AND transaction_type = 'ServiceFee';
-- UPDATE t_sales_data SET settlement_category = 'REMOVAL' WHERE source_type = 'ERP' AND transaction_type = 'RemovalShipment';
-- UPDATE t_sales_data SET settlement_category = 'ADJUSTMENT' WHERE source_type = 'ERP' AND transaction_type = 'Adjustment';
-- UPDATE t_sales_data SET settlement_category = 'RETROCHARGE' WHERE source_type = 'ERP' AND transaction_type = 'Retrocharge';
-- UPDATE t_sales_data SET settlement_category = 'COUPON' WHERE source_type = 'ERP' AND transaction_type = 'CouponPayment';
-- UPDATE t_sales_data SET settlement_category = 'REMOVAL_ADJUSTMENT' WHERE source_type = 'ERP' AND transaction_type = 'RemovalShipmentAdjustment';
-- UPDATE t_sales_data SET settlement_category = 'DEAL_PAYMENT' WHERE source_type = 'ERP' AND transaction_type = 'SellerDealPayment';
-- UPDATE t_sales_data SET settlement_category = 'CHARGEBACK' WHERE source_type = 'ERP' AND transaction_type = 'Chargeback';
-- UPDATE t_sales_data SET settlement_category = 'ADS_PAYMENT' WHERE source_type = 'ERP' AND transaction_type = 'ProductAdsPayment';

-- ============================================================
-- 验证脚本
-- ============================================================

-- 检查字段是否添加成功
-- SHOW COLUMNS FROM t_sales_data LIKE 'settlement_category';

-- 检查索引是否创建成功
-- SHOW INDEX FROM t_sales_data WHERE Key_name LIKE 'idx_sales_settlement%';
-- SHOW INDEX FROM t_sales_data WHERE Key_name LIKE 'idx_sales_txn_type%';
