-- ============================================================
-- 字段映射模板更新
-- 版本: v1.7
-- 日期: 2026-01-24
-- 说明: 更新字段映射模板，移除已删除的字段映射，确保与优化后的 SalesData 一致
-- ============================================================

-- ============================================================
-- 删除旧模板数据，重新插入优化后的模板
-- ============================================================

-- 删除销售数据相关的旧模板
DELETE FROM t_field_mapping_template WHERE data_type = 'SALES';

-- ============================================================
-- US站亚马逊原始数据模板（移除已删除字段）
-- ============================================================
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
  {"source": "fulfillment", "target": "fulfillment"},
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

-- ============================================================
-- UK站亚马逊原始数据模板
-- ============================================================
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

-- ============================================================
-- DE站亚马逊原始数据模板（德语表头）
-- ============================================================
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

-- ============================================================
-- CA站亚马逊原始数据模板
-- ============================================================
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
  {"source": "fulfillment", "target": "fulfillment"},
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

-- ============================================================
-- ERP结算数据通用模板（优化后，移除已删除字段）
-- 注意：ERP数据由 ErpSettlementParser 自动处理，此模板仅作参考
-- ============================================================
INSERT INTO t_field_mapping_template (template_name, site_code, data_type, source_type, header_row, is_default, mapping_config) VALUES
('ERP结算数据通用模板', NULL, 'SALES', 'ERP', 1, 1, '[
  {"source": "Settlement ID", "target": "settlementId"},
  {"source": "订单号", "target": "orderId"},
  {"source": "店铺", "target": "storeName"},
  {"source": "国家", "target": "siteCode"},
  {"source": "配送方式", "target": "fulfillment"},
  {"source": "来源", "target": "transactionType"},
  {"source": "结算时间", "target": "transactionDate"},
  {"source": "币种", "target": "currencyCode"},
  {"source": "数量", "target": "quantity"},
  {"source": "MSKU", "target": "sku"},
  {"source": "品名", "target": "description"}
]');

-- ============================================================
-- 移除的字段（已从模板中删除）：
-- - accountType（仅US有，业务价值低）
-- - orderCity（非财务核心）
-- - orderState（非财务核心）
-- - orderPostal（非财务核心）
-- - taxCollectionModel（非财务核心）
-- - settlementStatus（ERP状态，非核心）
-- - transferStatus（ERP状态，非核心）
-- - SKU（ERP字段，使用MSKU代替）
-- - amount（ERP字段，按交易类型分配到具体金额字段）
-- ============================================================

-- ============================================================
-- 验证脚本
-- ============================================================

-- SELECT * FROM t_field_mapping_template WHERE data_type = 'SALES' ORDER BY site_code, source_type;
