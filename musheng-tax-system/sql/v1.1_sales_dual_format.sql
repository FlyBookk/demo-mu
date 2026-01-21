-- =====================================================
-- 销售数据双格式导入 - 数据库变更脚本
-- 版本: v1.1
-- 日期: 2026-01-21
-- 作者: BACKEND_AGENT
-- =====================================================

-- 1. 扩展 t_sales_data 表，增加数据源类型等字段
ALTER TABLE t_sales_data 
ADD COLUMN source_type VARCHAR(20) DEFAULT 'ORIGINAL' COMMENT '数据源类型：ORIGINAL-亚马逊原始数据, ERP-ERP结算数据' AFTER import_batch_id,
ADD COLUMN store_name VARCHAR(100) DEFAULT NULL COMMENT '店铺名称（ERP数据专有）' AFTER marketplace,
ADD COLUMN settlement_status VARCHAR(20) DEFAULT NULL COMMENT '结算状态（ERP数据专有）' AFTER store_name,
ADD COLUMN transfer_status VARCHAR(20) DEFAULT NULL COMMENT '转账状态（ERP数据专有）' AFTER settlement_status;

-- 添加索引
CREATE INDEX idx_source_type ON t_sales_data(source_type);
CREATE INDEX idx_store_name ON t_sales_data(store_name);

-- 2. 扩展 t_field_mapping_template 表，增加子类型字段
ALTER TABLE t_field_mapping_template 
ADD COLUMN source_type VARCHAR(20) DEFAULT NULL COMMENT '数据源类型：ORIGINAL/ERP（仅销售数据有效）' AFTER data_type,
ADD COLUMN header_row INT DEFAULT 1 COMMENT '表头行号（从1开始）' AFTER source_type,
ADD COLUMN default_values JSON DEFAULT NULL COMMENT '默认值配置' AFTER header_row;

-- 添加索引
CREATE INDEX idx_data_source_type ON t_field_mapping_template(data_type, source_type);

-- 3. 初始化销售数据字段映射模板

-- US站亚马逊原始数据模板
INSERT INTO t_field_mapping_template (template_name, site_code, data_type, source_type, header_row, is_default, mapping_config) VALUES
('US站亚马逊原始数据模板', 'US', 'SALES', 'ORIGINAL', 8, 1, '[
  {"source": "date/time", "target": "transactionDate"},
  {"source": "settlement id", "target": "settlementId"},
  {"source": "type", "target": "transactionType"},
  {"source": "order id", "target": "orderId"},
  {"source": "sku", "target": "sku"},
  {"source": "description", "target": "description"},
  {"source": "quantity", "target": "quantity"},
  {"source": "marketplace", "target": "marketplace"},
  {"source": "account type", "target": "accountType"},
  {"source": "fulfillment", "target": "fulfillment"},
  {"source": "order city", "target": "orderCity"},
  {"source": "order state", "target": "orderState"},
  {"source": "order postal", "target": "orderPostal"},
  {"source": "tax collection model", "target": "taxCollectionModel"},
  {"source": "product sales", "target": "productSales"},
  {"source": "product sales tax", "target": "productSalesTax"},
  {"source": "shipping credits", "target": "shippingCredits"},
  {"source": "shipping credits tax", "target": "shippingCreditsTax"},
  {"source": "gift wrap credits", "target": "giftWrapCredits"},
  {"source": "giftwrap credits tax", "target": "giftWrapCreditsTax"},
  {"source": "Regulatory Fee", "target": "regulatoryFee"},
  {"source": "Tax On Regulatory Fee", "target": "regulatoryFeeTax"},
  {"source": "promotional rebates", "target": "promotionalRebates"},
  {"source": "promotional rebates tax", "target": "promotionalRebatesTax"},
  {"source": "marketplace withheld tax", "target": "marketplaceWithheldTax"},
  {"source": "selling fees", "target": "sellingFees"},
  {"source": "fba fees", "target": "fbaFees"},
  {"source": "other transaction fees", "target": "otherTransactionFees"},
  {"source": "other", "target": "other"},
  {"source": "total", "target": "total"}
]');

-- UK站亚马逊原始数据模板
INSERT INTO t_field_mapping_template (template_name, site_code, data_type, source_type, header_row, is_default, mapping_config) VALUES
('UK站亚马逊原始数据模板', 'UK', 'SALES', 'ORIGINAL', 8, 1, '[
  {"source": "date/time", "target": "transactionDate"},
  {"source": "settlement id", "target": "settlementId"},
  {"source": "type", "target": "transactionType"},
  {"source": "order id", "target": "orderId"},
  {"source": "sku", "target": "sku"},
  {"source": "description", "target": "description"},
  {"source": "quantity", "target": "quantity"},
  {"source": "marketplace", "target": "marketplace"},
  {"source": "fulfilment", "target": "fulfillment"},
  {"source": "order city", "target": "orderCity"},
  {"source": "order state", "target": "orderState"},
  {"source": "order postal", "target": "orderPostal"},
  {"source": "tax collection model", "target": "taxCollectionModel"},
  {"source": "product sales", "target": "productSales"},
  {"source": "product sales tax", "target": "productSalesTax"},
  {"source": "postage credits", "target": "shippingCredits"},
  {"source": "shipping credits tax", "target": "shippingCreditsTax"},
  {"source": "gift wrap credits", "target": "giftWrapCredits"},
  {"source": "giftwrap credits tax", "target": "giftWrapCreditsTax"},
  {"source": "promotional rebates", "target": "promotionalRebates"},
  {"source": "promotional rebates tax", "target": "promotionalRebatesTax"},
  {"source": "marketplace withheld tax", "target": "marketplaceWithheldTax"},
  {"source": "selling fees", "target": "sellingFees"},
  {"source": "fba fees", "target": "fbaFees"},
  {"source": "other transaction fees", "target": "otherTransactionFees"},
  {"source": "other", "target": "other"},
  {"source": "total", "target": "total"}
]');

-- DE站亚马逊原始数据模板（德语表头）
INSERT INTO t_field_mapping_template (template_name, site_code, data_type, source_type, header_row, is_default, mapping_config) VALUES
('DE站亚马逊原始数据模板', 'DE', 'SALES', 'ORIGINAL', 8, 1, '[
  {"source": "Datum/Uhrzeit", "target": "transactionDate"},
  {"source": "Abrechnungsnummer", "target": "settlementId"},
  {"source": "Typ", "target": "transactionType"},
  {"source": "Bestellnummer", "target": "orderId"},
  {"source": "SKU", "target": "sku"},
  {"source": "Beschreibung", "target": "description"},
  {"source": "Menge", "target": "quantity"},
  {"source": "Marketplace", "target": "marketplace"},
  {"source": "Versand", "target": "fulfillment"},
  {"source": "Ort der Bestellung", "target": "orderCity"},
  {"source": "Bundesland", "target": "orderState"},
  {"source": "Postleitzahl", "target": "orderPostal"},
  {"source": "Steuererhebungsmodell", "target": "taxCollectionModel"},
  {"source": "Umsätze", "target": "productSales"},
  {"source": "Produktumsatzsteuer", "target": "productSalesTax"},
  {"source": "Gutschrift für Versandkosten", "target": "shippingCredits"},
  {"source": "Steuer auf Versandgutschrift", "target": "shippingCreditsTax"},
  {"source": "Gutschrift für Geschenkverpackung", "target": "giftWrapCredits"},
  {"source": "Steuer auf Geschenkverpackungsgutschriften", "target": "giftWrapCreditsTax"},
  {"source": "Rabatte aus Werbeaktionen", "target": "promotionalRebates"},
  {"source": "Steuer auf Aktionsrabatte", "target": "promotionalRebatesTax"},
  {"source": "Einbehaltene Steuer auf Marketplace", "target": "marketplaceWithheldTax"},
  {"source": "Verkaufsgebühren", "target": "sellingFees"},
  {"source": "Gebühren zu Versand durch Amazon", "target": "fbaFees"},
  {"source": "Andere Transaktionsgebühren", "target": "otherTransactionFees"},
  {"source": "Andere", "target": "other"},
  {"source": "Gesamt", "target": "total"}
]');

-- CA站亚马逊原始数据模板
INSERT INTO t_field_mapping_template (template_name, site_code, data_type, source_type, header_row, is_default, mapping_config) VALUES
('CA站亚马逊原始数据模板', 'CA', 'SALES', 'ORIGINAL', 8, 1, '[
  {"source": "date/time", "target": "transactionDate"},
  {"source": "settlement id", "target": "settlementId"},
  {"source": "type", "target": "transactionType"},
  {"source": "order id", "target": "orderId"},
  {"source": "sku", "target": "sku"},
  {"source": "description", "target": "description"},
  {"source": "quantity", "target": "quantity"},
  {"source": "marketplace", "target": "marketplace"},
  {"source": "account type", "target": "accountType"},
  {"source": "fulfillment", "target": "fulfillment"},
  {"source": "order city", "target": "orderCity"},
  {"source": "order state", "target": "orderState"},
  {"source": "order postal", "target": "orderPostal"},
  {"source": "tax collection model", "target": "taxCollectionModel"},
  {"source": "product sales", "target": "productSales"},
  {"source": "product sales tax", "target": "productSalesTax"},
  {"source": "shipping credits", "target": "shippingCredits"},
  {"source": "shipping credits tax", "target": "shippingCreditsTax"},
  {"source": "gift wrap credits", "target": "giftWrapCredits"},
  {"source": "gift wrap credits tax", "target": "giftWrapCreditsTax"},
  {"source": "Regulatory fee", "target": "regulatoryFee"},
  {"source": "Tax on regulatory fee", "target": "regulatoryFeeTax"},
  {"source": "promotional rebates", "target": "promotionalRebates"},
  {"source": "promotional rebates tax", "target": "promotionalRebatesTax"},
  {"source": "marketplace withheld tax", "target": "marketplaceWithheldTax"},
  {"source": "selling fees", "target": "sellingFees"},
  {"source": "fba fees", "target": "fbaFees"},
  {"source": "other transaction fees", "target": "otherTransactionFees"},
  {"source": "other", "target": "other"},
  {"source": "total", "target": "total"}
]');

-- ERP结算数据通用模板（适用于所有站点）
INSERT INTO t_field_mapping_template (template_name, site_code, data_type, source_type, header_row, is_default, mapping_config) VALUES
('ERP结算数据通用模板', NULL, 'SALES', 'ERP', 1, 1, '[
  {"source": "结算编号", "target": "settlementId"},
  {"source": "订单号", "target": "orderId"},
  {"source": "店铺", "target": "storeName"},
  {"source": "国家", "target": "siteCode"},
  {"source": "配送方式", "target": "fulfillment"},
  {"source": "MSKU", "target": "msku"},
  {"source": "交易类型", "target": "transactionType"},
  {"source": "结算时间", "target": "transactionDate"},
  {"source": "币种", "target": "currencyCode"},
  {"source": "金额", "target": "amount"},
  {"source": "数量", "target": "quantity"},
  {"source": "结算状态", "target": "settlementStatus"},
  {"source": "转账状态", "target": "transferStatus"},
  {"source": "SKU", "target": "sku"},
  {"source": "品名", "target": "description"}
]');

-- 4. 创建导入进度缓存表（可选，用于持久化进度）
CREATE TABLE IF NOT EXISTS t_import_progress_cache (
    batch_no VARCHAR(50) PRIMARY KEY COMMENT '批次号',
    status VARCHAR(20) NOT NULL COMMENT '状态',
    total_count INT DEFAULT 0 COMMENT '总数',
    processed_count INT DEFAULT 0 COMMENT '已处理数',
    success_count INT DEFAULT 0 COMMENT '成功数',
    fail_count INT DEFAULT 0 COMMENT '失败数',
    skip_count INT DEFAULT 0 COMMENT '跳过数',
    percentage INT DEFAULT 0 COMMENT '进度百分比',
    message VARCHAR(500) DEFAULT NULL COMMENT '消息',
    error_details JSON DEFAULT NULL COMMENT '错误详情',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导入进度缓存表';

-- 5. 添加索引优化查询性能
CREATE INDEX idx_sales_order_site_sku ON t_sales_data(order_id, site_code, sku);
CREATE INDEX idx_sales_import_batch ON t_sales_data(import_batch_id);
