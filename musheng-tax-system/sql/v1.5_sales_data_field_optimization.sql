-- ============================================================
-- 销售数据字段优化 - 重建表结构
-- 版本: v1.5
-- 日期: 2026-01-24
-- 说明: 重建 t_sales_data 表结构（37个字段），保留 t_transaction_type_mapping 表
-- ============================================================

-- ============================================================
-- 第一部分：删除旧表
-- ============================================================

-- 删除销售数据表（重建）
DROP TABLE IF EXISTS t_sales_data;

-- 注意：t_transaction_type_mapping 表保留！
-- 该表用于动态配置交易类型到分类的映射，有以下引用：
--   - SalesDataServiceImpl.getTransactionTypeMapping() 
--   - TransactionTypeMappingController (CRUD API)
--   - TransactionTypeMappingService/Impl
-- 如需更新映射数据，请执行 v1.6_transaction_type_mapping_update.sql

-- ============================================================
-- 第二部分：创建优化后的销售数据表
-- ============================================================

CREATE TABLE t_sales_data (
    -- ========== 系统字段 ==========
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    import_batch_id BIGINT COMMENT '导入批次ID',
    source_type VARCHAR(20) NOT NULL DEFAULT 'ORIGINAL' COMMENT '数据源类型(ORIGINAL-亚马逊原始数据/ERP-ERP结算数据)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    
    -- ========== 业务标识字段 ==========
    store_name VARCHAR(100) COMMENT '店铺名称（预留字段）',
    transaction_date DATETIME NOT NULL COMMENT '交易/结算时间',
    settlement_id VARCHAR(50) NOT NULL COMMENT '亚马逊结算批次号（去重关键字段）',
    transaction_type VARCHAR(50) NOT NULL COMMENT '交易类型(Order/Refund/Shipment等)',
    transaction_category VARCHAR(20) COMMENT '交易分类(income/refund/fee/adjustment/other)',
    order_id VARCHAR(50) NOT NULL COMMENT '亚马逊订单编号',
    sku VARCHAR(100) COMMENT '商品SKU',
    description VARCHAR(500) COMMENT '商品描述',
    quantity INT DEFAULT 0 COMMENT '商品数量',
    
    -- ========== 站点与货币字段 ==========
    site_code VARCHAR(10) COMMENT '站点代码(US/UK/DE/CA等)',
    marketplace VARCHAR(50) COMMENT '站点域名(amazon.com/amazon.de等)',
    currency_code VARCHAR(10) COMMENT '货币代码(USD/EUR/GBP/CAD等)',
    fulfillment VARCHAR(50) COMMENT '配送方式(FBA/FBM)',
    
    -- ========== 费用字段（全部保留，共16个） ==========
    product_sales DECIMAL(15,2) DEFAULT 0 COMMENT '产品销售额',
    product_sales_tax DECIMAL(15,2) DEFAULT 0 COMMENT '产品税',
    shipping_credits DECIMAL(15,2) DEFAULT 0 COMMENT '运费收入',
    shipping_credits_tax DECIMAL(15,2) DEFAULT 0 COMMENT '运费税',
    gift_wrap_credits DECIMAL(15,2) DEFAULT 0 COMMENT '礼品包装费',
    gift_wrap_credits_tax DECIMAL(15,2) DEFAULT 0 COMMENT '礼品包装税',
    regulatory_fee DECIMAL(15,2) DEFAULT 0 COMMENT '监管费(仅CA/US站点)',
    regulatory_fee_tax DECIMAL(15,2) DEFAULT 0 COMMENT '监管费税(仅CA/US站点)',
    promotional_rebates DECIMAL(15,2) DEFAULT 0 COMMENT '促销折扣',
    promotional_rebates_tax DECIMAL(15,2) DEFAULT 0 COMMENT '促销折扣税',
    marketplace_withheld_tax DECIMAL(15,2) DEFAULT 0 COMMENT '平台代扣税',
    selling_fees DECIMAL(15,2) DEFAULT 0 COMMENT '销售佣金',
    fba_fees DECIMAL(15,2) DEFAULT 0 COMMENT 'FBA配送费',
    other_transaction_fees DECIMAL(15,2) DEFAULT 0 COMMENT '其他交易费',
    other DECIMAL(15,2) DEFAULT 0 COMMENT '其他金额',
    total DECIMAL(15,2) DEFAULT 0 COMMENT '合计金额',
    
    -- ========== 汇率字段 ==========
    exchange_rate DECIMAL(10,6) COMMENT '当日汇率（对人民币）',
    exchange_rate_date DATE COMMENT '汇率取值日期',
    
    -- ========== 索引 ==========
    -- 统一去重索引（原始数据和ERP数据都使用此组合）
    INDEX idx_sales_dedup (settlement_id, order_id, transaction_type),
    -- 交易日期索引（用于日期范围查询）
    INDEX idx_sales_transaction_date (transaction_date),
    -- 站点索引（用于站点筛选）
    INDEX idx_sales_site_code (site_code),
    -- 导入批次索引（用于按批次查询/删除）
    INDEX idx_sales_import_batch (import_batch_id),
    -- 店铺索引（用于店铺筛选，预留）
    INDEX idx_sales_store_name (store_name),
    -- 数据源类型索引
    INDEX idx_sales_source_type (source_type),
    -- 订单号索引（用于订单查询）
    INDEX idx_sales_order_id (order_id)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售数据表（优化版）';

-- ============================================================
-- 字段说明
-- ============================================================
-- 
-- 删除的字段（共10个）：
--   1. original_date_str     - 调试字段，生产不需要
--   2. original_timezone     - 调试字段，生产不需要
--   3. account_type          - 仅US站点有，业务价值低
--   4. order_city            - 非财务核心字段
--   5. order_state           - 非财务核心字段
--   6. order_postal          - 非财务核心字段
--   7. tax_collection_model  - 非财务核心字段
--   8. settlement_status     - ERP状态字段，非核心
--   9. transfer_status       - ERP状态字段，非核心
--  10. settlement_category   - 可从transaction_type推断
--
-- 保留的字段（共37个）：
--   - 系统字段：6个
--   - 业务标识字段：9个
--   - 站点与货币字段：4个
--   - 费用字段：16个
--   - 汇率字段：2个
--
-- 去重逻辑：
--   - 统一使用 settlement_id + order_id + transaction_type 作为业务唯一键
--   - 原始数据：settlement_id 来自 "settlement id" 列
--   - ERP数据：settlement_id 来自 "Settlement ID" 列（非"结算编号"）
--
-- ============================================================
