-- =====================================================
-- 字段映射画布功能 - 数据库迁移脚本
-- 版本: v1.0
-- 创建时间: 2026-01-21
-- 说明: 扩展字段映射模板表，新增目标字段元数据表和ERP聚合规则表
-- =====================================================

-- 1. 扩展字段映射模板表
-- -----------------------------------------------------
ALTER TABLE `t_field_mapping_template` 
ADD COLUMN `sub_type` VARCHAR(20) DEFAULT NULL COMMENT '子类型：ORIGINAL-原始数据, ERP-ERP数据（仅销售数据有效）' AFTER `data_type`,
ADD COLUMN `source_fields` JSON DEFAULT NULL COMMENT '源字段列表（可选保存）' AFTER `mapping_config`,
ADD COLUMN `header_row` INT DEFAULT 1 COMMENT '表头行号' AFTER `source_fields`,
ADD COLUMN `default_values` JSON DEFAULT NULL COMMENT '默认值配置 [{field, value}]' AFTER `header_row`;

-- 添加索引
CREATE INDEX `idx_data_type_sub` ON `t_field_mapping_template`(`data_type`, `sub_type`);

-- 2. 目标字段元数据表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `t_target_field_metadata`;
CREATE TABLE `t_target_field_metadata` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `data_type` VARCHAR(20) NOT NULL COMMENT '数据类型：SALES, SHIPPING, ADVERTISING, RATE',
    `sub_type` VARCHAR(20) DEFAULT NULL COMMENT '子类型：ORIGINAL, ERP（仅SALES有效）',
    `field_name` VARCHAR(50) NOT NULL COMMENT '字段名（数据库字段）',
    `field_label` VARCHAR(100) NOT NULL COMMENT '中文标签',
    `field_description` VARCHAR(255) DEFAULT NULL COMMENT '字段描述',
    `field_type` VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT '字段类型：string, number, datetime, boolean',
    `required` TINYINT NOT NULL DEFAULT 0 COMMENT '是否必填：0-否, 1-是',
    `max_length` INT DEFAULT NULL COMMENT '最大长度（string类型）',
    `precision_value` INT DEFAULT NULL COMMENT '精度（number类型）',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号',
    `site_aliases` JSON DEFAULT NULL COMMENT '站点字段别名 {"US": "order id", "DE": "Bestellnummer"}',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_field` (`data_type`, `sub_type`, `field_name`),
    INDEX `idx_data_type` (`data_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目标字段元数据表';

-- 3. ERP数据聚合规则表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `t_erp_aggregate_rule`;
CREATE TABLE `t_erp_aggregate_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `transaction_type` VARCHAR(50) NOT NULL COMMENT 'ERP交易类型',
    `target_field` VARCHAR(50) NOT NULL COMMENT '目标金额字段',
    `description` VARCHAR(100) DEFAULT NULL COMMENT '说明',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trans_type` (`transaction_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ERP数据聚合规则表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 4. 销售数据 - 亚马逊原始数据 (SALES_ORIGINAL) 目标字段
-- -----------------------------------------------------
INSERT INTO `t_target_field_metadata` 
(`data_type`, `sub_type`, `field_name`, `field_label`, `field_description`, `field_type`, `required`, `max_length`, `sort_order`, `site_aliases`) VALUES
('SALES', 'ORIGINAL', 'order_id', '订单ID', '亚马逊订单编号', 'string', 1, 50, 1, '{"US": "order id", "DE": "Bestellnummer", "UK": "order id", "CA": "order id"}'),
('SALES', 'ORIGINAL', 'sku', '商品SKU', 'SKU编码', 'string', 1, 100, 2, '{"US": "sku", "DE": "SKU", "UK": "sku", "CA": "sku"}'),
('SALES', 'ORIGINAL', 'transaction_date', '交易日期', '交易发生时间', 'datetime', 1, NULL, 3, '{"US": "date/time", "DE": "Datum/Uhrzeit", "UK": "date/time", "CA": "date/time"}'),
('SALES', 'ORIGINAL', 'settlement_id', '结算ID', '结算批次编号', 'string', 1, 50, 4, '{"US": "settlement id", "DE": "Abrechnungsnummer", "UK": "settlement id", "CA": "settlement id"}'),
('SALES', 'ORIGINAL', 'transaction_type', '交易类型', '订单/退款/服务费等', 'string', 1, 50, 5, '{"US": "type", "DE": "Typ", "UK": "type", "CA": "type"}'),
('SALES', 'ORIGINAL', 'marketplace', '站点域名', '亚马逊站点域名', 'string', 1, 50, 6, '{"US": "marketplace", "DE": "Marketplace", "UK": "marketplace", "CA": "marketplace"}'),
('SALES', 'ORIGINAL', 'quantity', '数量', '商品数量', 'number', 0, NULL, 7, '{"US": "quantity", "DE": "Menge", "UK": "quantity", "CA": "quantity"}'),
('SALES', 'ORIGINAL', 'product_sales', '产品销售额', '产品销售金额', 'number', 0, NULL, 8, '{"US": "product sales", "DE": "Umsätze", "UK": "product sales", "CA": "product sales"}'),
('SALES', 'ORIGINAL', 'product_sales_tax', '产品税', '产品销售税', 'number', 0, NULL, 9, '{"US": "product sales tax", "DE": "Produktumsatzsteuer", "UK": "product sales tax", "CA": "product sales tax"}'),
('SALES', 'ORIGINAL', 'shipping_credits', '运费收入', '运费抵扣金额', 'number', 0, NULL, 10, '{"US": "shipping credits", "DE": "Gutschrift für Versandkosten", "UK": "shipping credits", "CA": "shipping credits"}'),
('SALES', 'ORIGINAL', 'shipping_credits_tax', '运费税', '运费税金', 'number', 0, NULL, 11, '{"US": "shipping credits tax", "DE": "Steuer auf Versandgutschrift", "UK": "shipping credits tax", "CA": "shipping credits tax"}'),
('SALES', 'ORIGINAL', 'gift_wrap_credits', '礼品包装收入', '礼品包装费用', 'number', 0, NULL, 12, '{"US": "gift wrap credits", "DE": "Geschenkverpackung-Gutschriften", "UK": "gift wrap credits", "CA": "gift wrap credits"}'),
('SALES', 'ORIGINAL', 'promotional_rebates', '促销折扣', '促销优惠金额', 'number', 0, NULL, 13, '{"US": "promotional rebates", "DE": "Rabatte aus Werbeaktionen", "UK": "promotional rebates", "CA": "promotional rebates"}'),
('SALES', 'ORIGINAL', 'selling_fees', '销售费用', '平台销售佣金', 'number', 0, NULL, 14, '{"US": "selling fees", "DE": "Verkaufsgebühren", "UK": "selling fees", "CA": "selling fees"}'),
('SALES', 'ORIGINAL', 'fba_fees', 'FBA费用', 'FBA配送费用', 'number', 0, NULL, 15, '{"US": "fba fees", "DE": "Gebühren zu Versand durch Amazon", "UK": "fba fees", "CA": "fba fees"}'),
('SALES', 'ORIGINAL', 'other_transaction_fees', '其他费用', '其他交易费用', 'number', 0, NULL, 16, '{"US": "other transaction fees", "DE": "Andere Transaktionsgebühren", "UK": "other transaction fees", "CA": "other transaction fees"}'),
('SALES', 'ORIGINAL', 'other', '其他', '其他金额', 'number', 0, NULL, 17, '{"US": "other", "DE": "Andere", "UK": "other", "CA": "other"}'),
('SALES', 'ORIGINAL', 'total', '合计', '订单合计金额', 'number', 1, NULL, 18, '{"US": "total", "DE": "Gesamt", "UK": "total", "CA": "total"}');

-- 5. 销售数据 - ERP结算明细 (SALES_ERP) 目标字段
-- -----------------------------------------------------
INSERT INTO `t_target_field_metadata` 
(`data_type`, `sub_type`, `field_name`, `field_label`, `field_description`, `field_type`, `required`, `max_length`, `sort_order`, `site_aliases`) VALUES
('SALES', 'ERP', 'order_id', '订单ID', '订单编号', 'string', 1, 50, 1, '{"default": "订单号"}'),
('SALES', 'ERP', 'sku', '商品SKU', 'SKU编码', 'string', 1, 100, 2, '{"default": "SKU"}'),
('SALES', 'ERP', 'site_code', '站点编码', 'US/UK/DE等', 'string', 1, 10, 3, '{"default": "国家"}'),
('SALES', 'ERP', 'transaction_date', '交易日期', '结算时间', 'datetime', 1, NULL, 4, '{"default": "结算时间"}'),
('SALES', 'ERP', 'settlement_id', '结算ID', '结算编号', 'string', 0, 50, 5, '{"default": "结算编号"}'),
('SALES', 'ERP', 'store_name', '店铺名称', 'ERP店铺', 'string', 0, 100, 6, '{"default": "店铺"}'),
('SALES', 'ERP', 'fulfillment', '配送方式', 'FBA/FBM', 'string', 0, 20, 7, '{"default": "配送方式"}'),
('SALES', 'ERP', 'currency_code', '币种', '货币代码', 'string', 0, 10, 8, '{"default": "币种"}'),
('SALES', 'ERP', 'quantity', '数量', '商品数量', 'number', 0, NULL, 9, '{"default": "数量"}'),
('SALES', 'ERP', 'transaction_type', '交易类型', '用于行转列的分类字段', 'string', 1, 50, 10, '{"default": "交易类型"}'),
('SALES', 'ERP', 'amount', '金额', '根据交易类型映射到不同目标字段', 'number', 1, NULL, 11, '{"default": "金额"}');

-- 6. 配送数据 (SHIPPING) 目标字段
-- -----------------------------------------------------
INSERT INTO `t_target_field_metadata` 
(`data_type`, `sub_type`, `field_name`, `field_label`, `field_description`, `field_type`, `required`, `max_length`, `sort_order`, `site_aliases`) VALUES
('SHIPPING', NULL, 'shipment_id', '配送单号', '配送单编号', 'string', 1, 50, 1, '{"US": "shipment-id", "DE": "Sendungs-ID"}'),
('SHIPPING', NULL, 'order_id', '订单ID', '关联订单编号', 'string', 1, 50, 2, '{"US": "amazon-order-id", "DE": "Amazon-Bestellnummer"}'),
('SHIPPING', NULL, 'ship_date', '发货日期', '发货时间', 'datetime', 1, NULL, 3, '{"US": "shipment-date", "DE": "Versanddatum"}'),
('SHIPPING', NULL, 'sku', '商品SKU', 'SKU编码', 'string', 0, 100, 4, '{"US": "sku", "DE": "SKU"}'),
('SHIPPING', NULL, 'quantity', '数量', '发货数量', 'number', 0, NULL, 5, '{"US": "quantity-shipped", "DE": "Versandmenge"}'),
('SHIPPING', NULL, 'carrier', '承运商', '物流承运商', 'string', 0, 50, 6, '{"US": "carrier", "DE": "Spediteur"}'),
('SHIPPING', NULL, 'tracking_no', '运单号', '物流跟踪号', 'string', 0, 100, 7, '{"US": "tracking-number", "DE": "Tracking-Nummer"}');

-- 7. 广告数据 (ADVERTISING) 目标字段
-- -----------------------------------------------------
INSERT INTO `t_target_field_metadata` 
(`data_type`, `sub_type`, `field_name`, `field_label`, `field_description`, `field_type`, `required`, `max_length`, `sort_order`, `site_aliases`) VALUES
('ADVERTISING', NULL, 'invoice_no', '发票号', '广告发票编号', 'string', 1, 100, 1, '{"default": "Invoice Number"}'),
('ADVERTISING', NULL, 'site_code', '站点编码', '站点', 'string', 1, 10, 2, '{"default": "Marketplace"}'),
('ADVERTISING', NULL, 'year_month', '年月', '广告费用所属月份', 'string', 1, 10, 3, '{"default": "Billing Period"}'),
('ADVERTISING', NULL, 'amount', '金额', '广告费金额(原币)', 'number', 1, NULL, 4, '{"default": "Amount"}'),
('ADVERTISING', NULL, 'currency_code', '币种', '货币编码', 'string', 0, 10, 5, '{"default": "Currency"}'),
('ADVERTISING', NULL, 'remark', '备注', '备注信息', 'string', 0, 500, 6, '{"default": "Description"}');

-- 8. 汇率数据 (RATE) 目标字段
-- -----------------------------------------------------
INSERT INTO `t_target_field_metadata` 
(`data_type`, `sub_type`, `field_name`, `field_label`, `field_description`, `field_type`, `required`, `max_length`, `sort_order`, `site_aliases`) VALUES
('RATE', NULL, 'rate_date', '汇率日期', '汇率生效日期', 'datetime', 1, NULL, 1, '{"default": "日期"}'),
('RATE', NULL, 'currency_code', '货币编码', '货币代码', 'string', 1, 10, 2, '{"default": "货币"}'),
('RATE', NULL, 'rate', '汇率值', '兑人民币汇率', 'number', 1, NULL, 3, '{"default": "汇率"}');

-- 9. ERP数据聚合规则
-- -----------------------------------------------------
INSERT INTO `t_erp_aggregate_rule` (`transaction_type`, `target_field`, `description`, `sort_order`) VALUES
('Principal', 'product_sales', '产品销售额', 1),
('Tax', 'product_sales_tax', '产品税', 2),
('ShippingCharge', 'shipping_credits', '运费收入', 3),
('ShippingTax', 'shipping_credits_tax', '运费税', 4),
('Commission', 'selling_fees', '销售佣金', 5),
('FBAPerUnitFulfillmentFee', 'fba_fees', 'FBA费用', 6),
('FBAWeightBasedFee', 'fba_fees', 'FBA重量费用', 7),
('DigitalServicesFee', 'other_transaction_fees', '数字服务费', 8),
('PromotionDiscount', 'promotional_rebates', '促销折扣', 9),
('GiftWrap', 'gift_wrap_credits', '礼品包装', 10),
('MarketplaceFacilitatorVAT-Principal', 'marketplace_withheld_tax', '平台代扣税-产品', 11),
('MarketplaceFacilitatorVAT-Shipping', 'marketplace_withheld_tax', '平台代扣税-运费', 12),
('Refund', 'refund_amount', '退款', 13),
('RefundCommission', 'refund_commission', '退款佣金返还', 14);

-- =====================================================
-- 验证脚本
-- =====================================================
-- SELECT COUNT(*) as total_fields FROM t_target_field_metadata;
-- SELECT data_type, sub_type, COUNT(*) as field_count FROM t_target_field_metadata GROUP BY data_type, sub_type;
-- SELECT COUNT(*) as total_rules FROM t_erp_aggregate_rule;
