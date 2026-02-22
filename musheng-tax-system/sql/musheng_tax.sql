/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 90300 (9.3.0)
 Source Host           : localhost:3306
 Source Schema         : musheng_tax

 Target Server Type    : MySQL
 Target Server Version : 90300 (9.3.0)
 File Encoding         : 65001

 Date: 22/02/2026 22:55:22
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_advertising_data
-- ----------------------------
DROP TABLE IF EXISTS `t_advertising_data`;
CREATE TABLE `t_advertising_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint DEFAULT NULL COMMENT '店铺ID',
  `store_name` varchar(100) NOT NULL COMMENT '店铺名称',
  `site_code` varchar(10) DEFAULT NULL COMMENT '站点编码（从店铺名称推断）',
  `invoice_number` varchar(50) NOT NULL COMMENT '发票编号（去重关键字段）',
  `invoice_status` varchar(50) NOT NULL COMMENT '发票状态（PAID_IN_FULL等）',
  `payment_type` varchar(50) DEFAULT NULL COMMENT '支付类型（CREDIT_CARD等）',
  `billing_start_date` date NOT NULL COMMENT '账单开始日期',
  `billing_end_date` date NOT NULL COMMENT '账单结束日期',
  `issue_date` date NOT NULL COMMENT '发票开具日期',
  `currency` varchar(10) NOT NULL COMMENT '付款币种（USD/CAD/GBP/EUR）',
  `invoice_amount` decimal(10,2) NOT NULL COMMENT '账单金额（发票总金额）',
  `cost` decimal(10,2) NOT NULL COMMENT '费用（实际花费）',
  `other_cost` decimal(10,2) DEFAULT '0.00' COMMENT '其他费分摊',
  `campaign_name` varchar(200) DEFAULT NULL COMMENT '广告活动名称',
  `campaign_id` varchar(50) DEFAULT NULL COMMENT '活动ID（广告活动唯一标识）',
  `pricing_model` varchar(20) DEFAULT NULL COMMENT '计价方式（CPC/CPM等）',
  `clicks` int DEFAULT '0' COMMENT '点击次数',
  `avg_cpc` decimal(10,2) DEFAULT '0.00' COMMENT '平均点击单价',
  `data_source` varchar(50) DEFAULT NULL COMMENT '取值来源（业务报告等）',
  `product_list` text COMMENT '承担商品（逗号分隔）',
  `ad_type` varchar(100) DEFAULT NULL COMMENT '广告类型（SPONSORED PRODUCTS等）',
  `attachment_path` varchar(500) DEFAULT NULL COMMENT '发票附件路径',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `exchange_rate` decimal(10,6) DEFAULT NULL COMMENT '使用的汇率',
  `amount_cny` decimal(15,4) DEFAULT NULL COMMENT '费用金额（人民币）',
  `import_batch_id` varchar(50) DEFAULT NULL COMMENT '导入批次ID',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除(0未删除/1已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoice_number` (`invoice_number`),
  KEY `idx_store_name` (`store_name`),
  KEY `idx_billing_dates` (`billing_start_date`,`billing_end_date`),
  KEY `idx_campaign_id` (`campaign_id`),
  KEY `idx_ad_type` (`ad_type`),
  KEY `idx_created_at` (`create_time`),
  KEY `idx_site_code` (`site_code`),
  KEY `idx_import_batch` (`import_batch_id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='广告发票记录表（优化版） - 支持详细的广告发票数据导入，包含双时间字段和发票编号去重';



-- ----------------------------
-- Table structure for t_currency
-- ----------------------------
DROP TABLE IF EXISTS `t_currency`;
CREATE TABLE `t_currency` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `currency_code` varchar(10) NOT NULL COMMENT '货币编码(USD/CAD/GBP/EUR/CNY)',
  `currency_name` varchar(50) NOT NULL COMMENT '货币名称',
  `currency_symbol` varchar(10) DEFAULT NULL COMMENT '货币符号',
  `decimal_places` int DEFAULT '2' COMMENT '小数位数',
  `status` tinyint DEFAULT '1' COMMENT '状态(1启用/0禁用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `pair_direction` varchar(20) DEFAULT 'DIRECT' COMMENT '货币对方向(DIRECT=XXX/CNY, REVERSE=CNY/XXX)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_currency_code` (`currency_code`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='货币表';

-- ----------------------------
-- Records of t_currency
-- ----------------------------
BEGIN;
INSERT INTO `t_currency` (`id`, `currency_code`, `currency_name`, `currency_symbol`, `decimal_places`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `pair_direction`) VALUES (6, 'USD', '美元', '$', 2, 1, '2026-01-19 22:03:43', '2026-01-19 22:03:43', NULL, 1, 'DIRECT');
INSERT INTO `t_currency` (`id`, `currency_code`, `currency_name`, `currency_symbol`, `decimal_places`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `pair_direction`) VALUES (7, 'CAD', '加币', 'C$', 2, 1, '2026-01-19 22:03:43', '2026-01-19 22:03:43', NULL, 1, 'DIRECT');
INSERT INTO `t_currency` (`id`, `currency_code`, `currency_name`, `currency_symbol`, `decimal_places`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `pair_direction`) VALUES (8, 'GBP', '英镑', '£', 2, 1, '2026-01-19 22:03:43', '2026-01-19 22:03:43', NULL, 1, 'DIRECT');
INSERT INTO `t_currency` (`id`, `currency_code`, `currency_name`, `currency_symbol`, `decimal_places`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `pair_direction`) VALUES (9, 'EUR', '欧元', '€', 2, 1, '2026-01-19 22:03:43', '2026-01-19 22:03:43', NULL, 1, 'DIRECT');
INSERT INTO `t_currency` (`id`, `currency_code`, `currency_name`, `currency_symbol`, `decimal_places`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `pair_direction`) VALUES (10, 'CNY', '人民币', '¥', 2, 1, '2026-01-19 22:03:43', '2026-01-19 22:03:43', NULL, 1, 'DIRECT');
COMMIT;

-- ----------------------------
-- Table structure for t_erp_aggregate_rule
-- ----------------------------
DROP TABLE IF EXISTS `t_erp_aggregate_rule`;
CREATE TABLE `t_erp_aggregate_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `transaction_type` varchar(50) NOT NULL COMMENT 'ERP交易类型',
  `target_field` varchar(50) NOT NULL COMMENT '目标金额字段',
  `description` varchar(100) DEFAULT NULL COMMENT '说明',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trans_type` (`transaction_type`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ERP数据聚合规则表';

-- ----------------------------
-- Records of t_erp_aggregate_rule
-- ----------------------------
BEGIN;
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (1, 'Principal', 'product_sales', '产品销售额', 1, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (2, 'Tax', 'product_sales_tax', '产品税', 2, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (3, 'ShippingCharge', 'shipping_credits', '运费收入', 3, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (4, 'ShippingTax', 'shipping_credits_tax', '运费税', 4, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (5, 'Commission', 'selling_fees', '销售佣金', 5, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (6, 'FBAPerUnitFulfillmentFee', 'fba_fees', 'FBA费用', 6, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (7, 'FBAWeightBasedFee', 'fba_fees', 'FBA重量费用', 7, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (8, 'DigitalServicesFee', 'other_transaction_fees', '数字服务费', 8, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (9, 'PromotionDiscount', 'promotional_rebates', '促销折扣', 9, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (10, 'GiftWrap', 'gift_wrap_credits', '礼品包装', 10, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (11, 'MarketplaceFacilitatorVAT-Principal', 'marketplace_withheld_tax', '平台代扣税-产品', 11, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (12, 'MarketplaceFacilitatorVAT-Shipping', 'marketplace_withheld_tax', '平台代扣税-运费', 12, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (13, 'Refund', 'refund_amount', '退款', 13, '2026-01-21 18:38:49');
INSERT INTO `t_erp_aggregate_rule` (`id`, `transaction_type`, `target_field`, `description`, `sort_order`, `create_time`) VALUES (14, 'RefundCommission', 'refund_commission', '退款佣金返还', 14, '2026-01-21 18:38:49');
COMMIT;

-- ----------------------------
-- Table structure for t_exchange_rate
-- ----------------------------
DROP TABLE IF EXISTS `t_exchange_rate`;
CREATE TABLE `t_exchange_rate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rate_date` date NOT NULL COMMENT '汇率日期',
  `currency_code` varchar(10) NOT NULL COMMENT '货币编码',
  `rate` decimal(10,6) NOT NULL COMMENT '汇率中间价(对人民币)',
  `is_workday` tinyint DEFAULT '1' COMMENT '是否工作日(1是/0否)',
  `actual_rate_date` date DEFAULT NULL COMMENT '实际汇率日期(节假日顺延后)',
  `source` varchar(50) DEFAULT 'PBOC' COMMENT '数据来源(PBOC-中国人民银行)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date_currency` (`rate_date`,`currency_code`),
  KEY `idx_currency_date` (`currency_code`,`rate_date`)
) ENGINE=InnoDB AUTO_INCREMENT=5091 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='汇率数据表';



-- ----------------------------
-- Table structure for t_fba_shipment
-- ----------------------------
DROP TABLE IF EXISTS `t_fba_shipment`;
CREATE TABLE `t_fba_shipment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID（数据隔离）',
  `shipment_id` varchar(50) NOT NULL COMMENT '货件单号（FBA货件编号，如：FBA15KYVTSMJ）',
  `warehouse_code` varchar(255) DEFAULT NULL COMMENT '物流中心编码（亚马逊仓库地址）',
  `shop_name` varchar(100) DEFAULT NULL COMMENT '店铺名称（如：慕声欧洲-UK）',
  `country` varchar(50) DEFAULT NULL COMMENT '国家（如：英国）',
  `created_date` datetime DEFAULT NULL COMMENT '货件创建时间',
  `sku_count` int DEFAULT '0' COMMENT 'SKU种类数量（自动计算）',
  `total_quantity` int DEFAULT '0' COMMENT '总发货量（自动汇总）',
  `import_batch_id` bigint DEFAULT NULL COMMENT '导入批次ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_shipment` (`shop_id`,`shipment_id`) COMMENT '店铺+货件单号唯一索引',
  KEY `idx_shop_id` (`shop_id`) COMMENT '店铺索引',
  KEY `idx_shipment_id` (`shipment_id`) COMMENT '货件单号索引',
  KEY `idx_created_date` (`created_date`) COMMENT '创建时间索引',
  KEY `idx_import_batch` (`import_batch_id`) COMMENT '导入批次索引'
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='FBA货件主表';



-- ----------------------------
-- Table structure for t_fba_shipment_item
-- ----------------------------
DROP TABLE IF EXISTS `t_fba_shipment_item`;
CREATE TABLE `t_fba_shipment_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID（数据隔离）',
  `shipment_id` bigint NOT NULL COMMENT '货件主表ID（外键）',
  `shipment_no` varchar(50) NOT NULL COMMENT '货件单号（冗余字段，便于查询）',
  `sku` varchar(100) NOT NULL COMMENT '内部SKU编码',
  `msku` varchar(100) DEFAULT NULL COMMENT '亚马逊MSKU',
  `quantity` int NOT NULL DEFAULT '0' COMMENT '发货量',
  `import_batch_id` bigint DEFAULT NULL COMMENT '导入批次ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shipment_sku` (`shipment_id`,`sku`) COMMENT '货件+SKU唯一索引',
  KEY `idx_shop_id` (`shop_id`) COMMENT '店铺索引',
  KEY `idx_shipment_no` (`shipment_no`) COMMENT '货件单号索引',
  KEY `idx_sku` (`sku`) COMMENT 'SKU索引',
  KEY `idx_msku` (`msku`) COMMENT 'MSKU索引',
  KEY `idx_import_batch` (`import_batch_id`) COMMENT '导入批次索引'
) ENGINE=InnoDB AUTO_INCREMENT=1196 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='FBA货件明细表';



-- ----------------------------
-- Table structure for t_field_mapping_template
-- ----------------------------
DROP TABLE IF EXISTS `t_field_mapping_template`;
CREATE TABLE `t_field_mapping_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `site_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '站点编码',
  `data_type` varchar(20) NOT NULL DEFAULT 'sales' COMMENT '数据类型(sales/shipping)',
  `source_type` varchar(20) DEFAULT NULL COMMENT '数据源类型：ORIGINAL/ERP（仅销售数据有效）',
  `sub_type` varchar(20) DEFAULT NULL COMMENT '子类型：ORIGINAL-原始数据, ERP-ERP数据（仅销售数据有效）',
  `mapping_config` json NOT NULL COMMENT '映射配置(JSON格式)',
  `source_fields` json DEFAULT NULL COMMENT '源字段列表（可选保存）',
  `header_row` int DEFAULT '1' COMMENT '表头行号',
  `default_values` json DEFAULT NULL COMMENT '默认值配置 [{field, value}]',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认模板(1是/0否)',
  `is_visible` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否在列表中显示(0-否，1-是)',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_site_type` (`site_code`,`data_type`),
  KEY `idx_data_type_sub` (`data_type`,`sub_type`),
  KEY `idx_data_source_type` (`data_type`,`source_type`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字段映射模板表';

-- ----------------------------
-- Records of t_field_mapping_template
-- ----------------------------
BEGIN;
INSERT INTO `t_field_mapping_template` (`id`, `template_name`, `site_code`, `data_type`, `source_type`, `sub_type`, `mapping_config`, `source_fields`, `header_row`, `default_values`, `is_default`, `is_visible`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (8, 'US站亚马逊原始数据模板', 'US', 'SALES', 'ORIGINAL', NULL, '[{\"source\": \"date/time\", \"target\": \"transactionDate\"}, {\"source\": \"settlement id\", \"target\": \"settlementId\"}, {\"source\": \"type\", \"target\": \"transactionType\"}, {\"source\": \"order id\", \"target\": \"orderId\"}, {\"source\": \"sku\", \"target\": \"sku\"}, {\"source\": \"description\", \"target\": \"description\"}, {\"source\": \"quantity\", \"target\": \"quantity\"}, {\"source\": \"marketplace\", \"target\": \"marketplace\"}, {\"source\": \"fulfillment\", \"target\": \"fulfillment\"}, {\"source\": \"product sales\", \"target\": \"productSales\"}, {\"source\": \"product sales tax\", \"target\": \"productSalesTax\"}, {\"source\": \"shipping credits\", \"target\": \"shippingCredits\"}, {\"source\": \"shipping credits tax\", \"target\": \"shippingCreditsTax\"}, {\"source\": \"gift wrap credits\", \"target\": \"giftWrapCredits\"}, {\"source\": \"giftwrap credits tax\", \"target\": \"giftWrapCreditsTax\"}, {\"source\": \"Regulatory Fee\", \"target\": \"regulatoryFee\"}, {\"source\": \"Tax On Regulatory Fee\", \"target\": \"regulatoryFeeTax\"}, {\"source\": \"promotional rebates\", \"target\": \"promotionalRebates\"}, {\"source\": \"promotional rebates tax\", \"target\": \"promotionalRebatesTax\"}, {\"source\": \"marketplace withheld tax\", \"target\": \"marketplaceWithheldTax\"}, {\"source\": \"selling fees\", \"target\": \"sellingFees\"}, {\"source\": \"fba fees\", \"target\": \"fbaFees\"}, {\"source\": \"other transaction fees\", \"target\": \"otherTransactionFees\"}, {\"source\": \"other\", \"target\": \"other\"}, {\"source\": \"total\", \"target\": \"total\"}]', NULL, 8, NULL, 1, 1, NULL, '2026-01-24 14:57:17', NULL, '2026-01-24 14:57:17');
INSERT INTO `t_field_mapping_template` (`id`, `template_name`, `site_code`, `data_type`, `source_type`, `sub_type`, `mapping_config`, `source_fields`, `header_row`, `default_values`, `is_default`, `is_visible`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (9, 'UK站亚马逊原始数据模板', 'UK', 'SALES', 'ORIGINAL', NULL, '[{\"source\": \"date/time\", \"target\": \"transactionDate\"}, {\"source\": \"settlement id\", \"target\": \"settlementId\"}, {\"source\": \"type\", \"target\": \"transactionType\"}, {\"source\": \"order id\", \"target\": \"orderId\"}, {\"source\": \"sku\", \"target\": \"sku\"}, {\"source\": \"description\", \"target\": \"description\"}, {\"source\": \"quantity\", \"target\": \"quantity\"}, {\"source\": \"marketplace\", \"target\": \"marketplace\"}, {\"source\": \"fulfilment\", \"target\": \"fulfillment\"}, {\"source\": \"product sales\", \"target\": \"productSales\"}, {\"source\": \"product sales tax\", \"target\": \"productSalesTax\"}, {\"source\": \"postage credits\", \"target\": \"shippingCredits\"}, {\"source\": \"shipping credits tax\", \"target\": \"shippingCreditsTax\"}, {\"source\": \"gift wrap credits\", \"target\": \"giftWrapCredits\"}, {\"source\": \"giftwrap credits tax\", \"target\": \"giftWrapCreditsTax\"}, {\"source\": \"promotional rebates\", \"target\": \"promotionalRebates\"}, {\"source\": \"promotional rebates tax\", \"target\": \"promotionalRebatesTax\"}, {\"source\": \"marketplace withheld tax\", \"target\": \"marketplaceWithheldTax\"}, {\"source\": \"selling fees\", \"target\": \"sellingFees\"}, {\"source\": \"fba fees\", \"target\": \"fbaFees\"}, {\"source\": \"other transaction fees\", \"target\": \"otherTransactionFees\"}, {\"source\": \"other\", \"target\": \"other\"}, {\"source\": \"total\", \"target\": \"total\"}]', NULL, 8, NULL, 1, 1, NULL, '2026-01-24 14:57:17', NULL, '2026-01-24 14:57:17');
INSERT INTO `t_field_mapping_template` (`id`, `template_name`, `site_code`, `data_type`, `source_type`, `sub_type`, `mapping_config`, `source_fields`, `header_row`, `default_values`, `is_default`, `is_visible`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (10, 'DE站亚马逊原始数据模板', 'DE', 'SALES', 'ORIGINAL', NULL, '[{\"source\": \"Datum/Uhrzeit\", \"target\": \"transactionDate\"}, {\"source\": \"Abrechnungsnummer\", \"target\": \"settlementId\"}, {\"source\": \"Typ\", \"target\": \"transactionType\"}, {\"source\": \"Bestellnummer\", \"target\": \"orderId\"}, {\"source\": \"SKU\", \"target\": \"sku\"}, {\"source\": \"Beschreibung\", \"target\": \"description\"}, {\"source\": \"Menge\", \"target\": \"quantity\"}, {\"source\": \"Marketplace\", \"target\": \"marketplace\"}, {\"source\": \"Versand\", \"target\": \"fulfillment\"}, {\"source\": \"Umsätze\", \"target\": \"productSales\"}, {\"source\": \"Produktumsatzsteuer\", \"target\": \"productSalesTax\"}, {\"source\": \"Gutschrift für Versandkosten\", \"target\": \"shippingCredits\"}, {\"source\": \"Steuer auf Versandgutschrift\", \"target\": \"shippingCreditsTax\"}, {\"source\": \"Gutschrift für Geschenkverpackung\", \"target\": \"giftWrapCredits\"}, {\"source\": \"Steuer auf Geschenkverpackungsgutschriften\", \"target\": \"giftWrapCreditsTax\"}, {\"source\": \"Rabatte aus Werbeaktionen\", \"target\": \"promotionalRebates\"}, {\"source\": \"Steuer auf Aktionsrabatte\", \"target\": \"promotionalRebatesTax\"}, {\"source\": \"Einbehaltene Steuer auf Marketplace\", \"target\": \"marketplaceWithheldTax\"}, {\"source\": \"Verkaufsgebühren\", \"target\": \"sellingFees\"}, {\"source\": \"Gebühren zu Versand durch Amazon\", \"target\": \"fbaFees\"}, {\"source\": \"Andere Transaktionsgebühren\", \"target\": \"otherTransactionFees\"}, {\"source\": \"Andere\", \"target\": \"other\"}, {\"source\": \"Gesamt\", \"target\": \"total\"}]', NULL, 8, NULL, 1, 1, NULL, '2026-01-24 14:57:17', NULL, '2026-01-24 14:57:17');
INSERT INTO `t_field_mapping_template` (`id`, `template_name`, `site_code`, `data_type`, `source_type`, `sub_type`, `mapping_config`, `source_fields`, `header_row`, `default_values`, `is_default`, `is_visible`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (11, 'CA站亚马逊原始数据模板', 'CA', 'SALES', 'ORIGINAL', NULL, '[{\"source\": \"date/time\", \"target\": \"transactionDate\"}, {\"source\": \"settlement id\", \"target\": \"settlementId\"}, {\"source\": \"type\", \"target\": \"transactionType\"}, {\"source\": \"order id\", \"target\": \"orderId\"}, {\"source\": \"sku\", \"target\": \"sku\"}, {\"source\": \"description\", \"target\": \"description\"}, {\"source\": \"quantity\", \"target\": \"quantity\"}, {\"source\": \"marketplace\", \"target\": \"marketplace\"}, {\"source\": \"fulfillment\", \"target\": \"fulfillment\"}, {\"source\": \"product sales\", \"target\": \"productSales\"}, {\"source\": \"product sales tax\", \"target\": \"productSalesTax\"}, {\"source\": \"shipping credits\", \"target\": \"shippingCredits\"}, {\"source\": \"shipping credits tax\", \"target\": \"shippingCreditsTax\"}, {\"source\": \"gift wrap credits\", \"target\": \"giftWrapCredits\"}, {\"source\": \"gift wrap credits tax\", \"target\": \"giftWrapCreditsTax\"}, {\"source\": \"Regulatory fee\", \"target\": \"regulatoryFee\"}, {\"source\": \"Tax on regulatory fee\", \"target\": \"regulatoryFeeTax\"}, {\"source\": \"promotional rebates\", \"target\": \"promotionalRebates\"}, {\"source\": \"promotional rebates tax\", \"target\": \"promotionalRebatesTax\"}, {\"source\": \"marketplace withheld tax\", \"target\": \"marketplaceWithheldTax\"}, {\"source\": \"selling fees\", \"target\": \"sellingFees\"}, {\"source\": \"fba fees\", \"target\": \"fbaFees\"}, {\"source\": \"other transaction fees\", \"target\": \"otherTransactionFees\"}, {\"source\": \"other\", \"target\": \"other\"}, {\"source\": \"total\", \"target\": \"total\"}]', NULL, 8, NULL, 1, 1, NULL, '2026-01-24 14:57:17', NULL, '2026-01-24 14:57:17');
INSERT INTO `t_field_mapping_template` (`id`, `template_name`, `site_code`, `data_type`, `source_type`, `sub_type`, `mapping_config`, `source_fields`, `header_row`, `default_values`, `is_default`, `is_visible`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (12, 'ERP结算数据通用模板', NULL, 'SALES', 'ERP', NULL, '[{\"source\": \"Settlement ID\", \"target\": \"settlementId\"}, {\"source\": \"订单号\", \"target\": \"orderId\"}, {\"source\": \"店铺\", \"target\": \"storeName\"}, {\"source\": \"国家\", \"target\": \"siteCode\"}, {\"source\": \"配送方式\", \"target\": \"fulfillment\"}, {\"source\": \"来源\", \"target\": \"transactionType\"}, {\"source\": \"结算时间\", \"target\": \"transactionDate\"}, {\"source\": \"币种\", \"target\": \"currencyCode\"}, {\"source\": \"数量\", \"target\": \"quantity\"}, {\"source\": \"MSKU\", \"target\": \"sku\"}, {\"source\": \"品名\", \"target\": \"description\"}]', NULL, 1, NULL, 1, 0, NULL, '2026-01-24 14:57:17', NULL, '2026-02-22 16:19:38');
COMMIT;

-- ----------------------------
-- Table structure for t_import_record
-- ----------------------------
DROP TABLE IF EXISTS `t_import_record`;
CREATE TABLE `t_import_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint DEFAULT NULL COMMENT '店铺ID（业务数据导入）',
  `batch_no` varchar(50) NOT NULL COMMENT '导入批次号',
  `data_type` varchar(20) NOT NULL COMMENT '数据类型(sales/shipping/advertising/rate)',
  `file_name` varchar(200) NOT NULL COMMENT '文件名',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小(字节)',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件存储路径',
  `file_hash` varchar(64) DEFAULT NULL COMMENT '文件哈希值（MD5，用于幂等性检查）',
  `total_count` int DEFAULT '0' COMMENT '总记录数',
  `success_count` int DEFAULT '0' COMMENT '成功条数',
  `fail_count` int DEFAULT '0' COMMENT '失败条数',
  `import_status` varchar(20) DEFAULT 'pending' COMMENT '导入状态(pending/processing/success/partial/fail)',
  `error_message` text COMMENT '错误信息摘要',
  `import_by` bigint DEFAULT NULL COMMENT '导入人',
  `import_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batch_no`),
  KEY `idx_type_time` (`data_type`,`import_time`),
  KEY `idx_status` (`import_status`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_file_hash` (`file_hash`) COMMENT '文件哈希索引',
  KEY `idx_shop_hash_type` (`shop_id`,`file_hash`,`data_type`) COMMENT '店铺+哈希+类型联合索引'
) ENGINE=InnoDB AUTO_INCREMENT=243 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导入记录表';



-- ----------------------------
-- Table structure for t_log_operation
-- ----------------------------
DROP TABLE IF EXISTS `t_log_operation`;
CREATE TABLE `t_log_operation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '操作用户名',
  `module` varchar(50) DEFAULT NULL COMMENT '操作模块',
  `operation` varchar(100) DEFAULT NULL COMMENT '操作类型',
  `method` varchar(200) DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `request_params` text COMMENT '请求参数',
  `response_data` text COMMENT '响应数据',
  `ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `execution_time` bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
  `status` tinyint DEFAULT '1' COMMENT '状态(1成功/0失败)',
  `error_msg` text COMMENT '错误信息',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';



-- ----------------------------
-- Table structure for t_marketplace
-- ----------------------------
DROP TABLE IF EXISTS `t_marketplace`;
CREATE TABLE `t_marketplace` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `site_code` varchar(10) NOT NULL COMMENT '站点编码(US/CA/UK/DE)',
  `site_name` varchar(50) NOT NULL COMMENT '站点名称',
  `marketplace_id` varchar(50) NOT NULL COMMENT 'Marketplace标识(amazon.com等)',
  `currency_code` varchar(10) NOT NULL COMMENT '关联货币编码',
  `seller_id` varchar(50) DEFAULT NULL COMMENT '卖家ID',
  `header_language` varchar(10) DEFAULT 'EN' COMMENT '表头语言(EN/DE)',
  `date_format` varchar(100) DEFAULT NULL COMMENT '日期解析格式',
  `number_format` varchar(10) DEFAULT '.' COMMENT '数字格式(.或,)',
  `timezone` varchar(20) DEFAULT NULL COMMENT '时区',
  `status` tinyint DEFAULT '1' COMMENT '状态(1启用/0禁用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_code` (`site_code`),
  KEY `idx_marketplace_id` (`marketplace_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站点表';

-- ----------------------------
-- Records of t_marketplace
-- ----------------------------
BEGIN;
INSERT INTO `t_marketplace` (`id`, `site_code`, `site_name`, `marketplace_id`, `currency_code`, `seller_id`, `header_language`, `date_format`, `number_format`, `timezone`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1, 'US', 'US Site', 'amazon.com', 'USD', 'APNDJLWNA7H88', 'EN', 'MMM d, yyyy h:mm:ss a z', '.', 'PDT', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL);
INSERT INTO `t_marketplace` (`id`, `site_code`, `site_name`, `marketplace_id`, `currency_code`, `seller_id`, `header_language`, `date_format`, `number_format`, `timezone`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (2, 'CA', 'Canada Site', 'amazon.ca', 'CAD', 'APNDJLWNA7H88', 'EN', 'MMM d, yyyy h:mm:ss a z', '.', 'PDT', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL);
INSERT INTO `t_marketplace` (`id`, `site_code`, `site_name`, `marketplace_id`, `currency_code`, `seller_id`, `header_language`, `date_format`, `number_format`, `timezone`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (3, 'UK', 'UK Site', 'amazon.co.uk', 'GBP', 'A11Y51I5ZXY4AO', 'EN', 'dd MMM yyyy HH:mm:ss z', '.', 'UTC', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL);
INSERT INTO `t_marketplace` (`id`, `site_code`, `site_name`, `marketplace_id`, `currency_code`, `seller_id`, `header_language`, `date_format`, `number_format`, `timezone`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (4, 'DE', 'Germany Site', 'amazon.de', 'EUR', 'A11Y51I5ZXY4AO', 'DE', 'dd.MM.yyyy HH:mm:ss z', ',', 'UTC', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` varchar(20) NOT NULL COMMENT '角色编码',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_desc` varchar(200) DEFAULT NULL COMMENT '角色描述',
  `permissions` json DEFAULT NULL COMMENT '权限配置(JSON)',
  `status` tinyint DEFAULT '1' COMMENT '状态(1启用/0禁用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

-- ----------------------------
-- Records of t_role
-- ----------------------------
BEGIN;
INSERT INTO `t_role` (`id`, `role_code`, `role_name`, `role_desc`, `permissions`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1, 'admin', 'Administrator', 'Full system access', '[\"*\"]', 1, '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
INSERT INTO `t_role` (`id`, `role_code`, `role_name`, `role_desc`, `permissions`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (2, 'finance', 'Finance Staff', 'All permissions except user management', '[\"dashboard\", \"dashboard:view\", \"config\", \"config:currency\", \"config:marketplace\", \"config:transactionType\", \"config:fieldMapping\", \"config:importRecord\", \"sales\", \"sales:import\", \"sales:list\", \"shipping\", \"shipping:import\", \"shipping:list\", \"advertising\", \"advertising:add\", \"advertising:list\", \"rate\", \"rate:import\", \"rate:list\", \"report\", \"report:summary\", \"report:download\", \"system:log\"]', 1, '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
INSERT INTO `t_role` (`id`, `role_code`, `role_name`, `role_desc`, `permissions`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (3, 'viewer', 'Viewer', 'View-only access based on menu permissions', '[\"sales\", \"sales:import\", \"sales:list\", \"shipping\", \"shipping:import\", \"shipping:list\", \"advertising\", \"advertising:add\", \"advertising:list\"]', 1, '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_sales_data
-- ----------------------------
DROP TABLE IF EXISTS `t_sales_data`;
CREATE TABLE `t_sales_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint DEFAULT NULL COMMENT '店铺ID',
  `import_batch_id` bigint DEFAULT NULL COMMENT '导入批次ID',
  `source_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ORIGINAL' COMMENT '数据源类型(ORIGINAL-亚马逊原始数据/ERP-ERP结算数据)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `store_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '店铺名称（预留字段）',
  `transaction_date` datetime NOT NULL COMMENT '交易/结算时间',
  `settlement_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '亚马逊结算批次号（去重关键字段）',
  `erp_settlement_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'ERP结算编号(用于ERP数据按结算编号合并及重复导入校验)',
  `transaction_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交易类型(Order/Refund/Shipment等)',
  `transaction_category` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '交易分类(income/refund/fee/adjustment/other)',
  `order_id` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '亚马逊订单编号',
  `sku` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商品SKU',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '商品描述',
  `quantity` int DEFAULT '0' COMMENT '商品数量',
  `site_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '站点代码(US/UK/DE/CA等)',
  `marketplace` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '站点域名(amazon.com/amazon.de等)',
  `currency_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '货币代码(USD/EUR/GBP/CAD等)',
  `fulfillment` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配送方式(FBA/FBM)',
  `product_sales` decimal(15,2) DEFAULT '0.00' COMMENT '产品销售额',
  `product_sales_tax` decimal(15,2) DEFAULT '0.00' COMMENT '产品税',
  `shipping_credits` decimal(15,2) DEFAULT '0.00' COMMENT '运费收入',
  `shipping_credits_tax` decimal(15,2) DEFAULT '0.00' COMMENT '运费税',
  `gift_wrap_credits` decimal(15,2) DEFAULT '0.00' COMMENT '礼品包装费',
  `gift_wrap_credits_tax` decimal(15,2) DEFAULT '0.00' COMMENT '礼品包装税',
  `regulatory_fee` decimal(15,2) DEFAULT '0.00' COMMENT '监管费(仅CA/US站点)',
  `regulatory_fee_tax` decimal(15,2) DEFAULT '0.00' COMMENT '监管费税(仅CA/US站点)',
  `promotional_rebates` decimal(15,2) DEFAULT '0.00' COMMENT '促销折扣',
  `promotional_rebates_tax` decimal(15,2) DEFAULT '0.00' COMMENT '促销折扣税',
  `marketplace_withheld_tax` decimal(15,2) DEFAULT '0.00' COMMENT '平台代扣税',
  `selling_fees` decimal(15,2) DEFAULT '0.00' COMMENT '销售佣金',
  `fba_fees` decimal(15,2) DEFAULT '0.00' COMMENT 'FBA配送费',
  `other_transaction_fees` decimal(15,2) DEFAULT '0.00' COMMENT '其他交易费',
  `other` decimal(15,2) DEFAULT '0.00' COMMENT '其他金额',
  `total` decimal(15,2) DEFAULT '0.00' COMMENT '合计金额',
  `exchange_rate` decimal(10,6) DEFAULT NULL COMMENT '当日汇率（对人民币）',
  `exchange_rate_date` date DEFAULT NULL COMMENT '汇率取值日期',
  PRIMARY KEY (`id`),
  KEY `idx_sales_transaction_date` (`transaction_date`),
  KEY `idx_sales_site_code` (`site_code`),
  KEY `idx_sales_order_id` (`order_id`,`settlement_id`,`transaction_type`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_sales_erp_settlement_id` (`erp_settlement_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1177895 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售数据表（优化版）';



-- ----------------------------
-- Table structure for t_shipping_data
-- ----------------------------
DROP TABLE IF EXISTS `t_shipping_data`;
CREATE TABLE `t_shipping_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint DEFAULT NULL COMMENT '店铺ID',
  `import_batch_id` bigint DEFAULT NULL COMMENT '导入批次ID',
  `order_id` varchar(50) NOT NULL COMMENT '订单号',
  `ship_date` date NOT NULL COMMENT '发货日期(配送日期)',
  `site_code` varchar(10) NOT NULL COMMENT '站点编码',
  `marketplace` varchar(50) NOT NULL COMMENT '原始marketplace值(销售渠道)',
  `currency_code` varchar(10) NOT NULL COMMENT '货币编码',
  `product_price` decimal(15,4) DEFAULT '0.0000' COMMENT '商品价格',
  `product_tax` decimal(15,4) DEFAULT '0.0000' COMMENT '商品税',
  `shipping_price` decimal(15,4) DEFAULT '0.0000' COMMENT '运费',
  `shipping_tax` decimal(15,4) DEFAULT '0.0000' COMMENT '运费税',
  `gift_wrap_price` decimal(15,4) DEFAULT '0.0000' COMMENT '礼品包装价格',
  `gift_wrap_tax` decimal(15,4) DEFAULT '0.0000' COMMENT '礼品包装价格税',
  `product_promotion_discount` decimal(15,4) DEFAULT '0.0000' COMMENT '商品促销折扣',
  `shipment_promotion_discount` decimal(15,4) DEFAULT '0.0000' COMMENT '货件促销折扣',
  `shipping_cost` decimal(15,4) DEFAULT '0.0000' COMMENT '物流费用(成本)',
  `revenue_total` decimal(15,4) DEFAULT '0.0000' COMMENT '收入总额(计算值)',
  `sku` varchar(100) DEFAULT '' COMMENT 'SKU',
  `quantity` int DEFAULT '0' COMMENT '数量',
  `carrier` varchar(100) DEFAULT '' COMMENT '承运商',
  `tracking_number` varchar(100) DEFAULT '' COMMENT '物流单号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `exchange_rate` decimal(10,6) DEFAULT NULL COMMENT '配送日期当天汇率（对人民币）',
  `exchange_rate_date` date DEFAULT NULL COMMENT '汇率实际取值日期（如果是节假日则为下一个工作日）',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_site_ship_date` (`site_code`,`ship_date`),
  KEY `idx_batch` (`import_batch_id`),
  KEY `idx_shipping_exchange_rate_date` (`exchange_rate_date`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=365881 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配送数据表';



-- ----------------------------
-- Table structure for t_shop
-- ----------------------------
DROP TABLE IF EXISTS `t_shop`;
CREATE TABLE `t_shop` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_code` varchar(50) NOT NULL COMMENT '店铺编码',
  `shop_name` varchar(100) NOT NULL COMMENT '店铺名称',
  `seller_id` varchar(50) DEFAULT NULL COMMENT '亚马逊卖家ID',
  `company_name` varchar(200) DEFAULT NULL COMMENT '公司名称',
  `tax_id` varchar(50) DEFAULT NULL COMMENT '统一社会信用代码',
  `status` tinyint DEFAULT '1' COMMENT '状态(1启用/0禁用)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_code` (`shop_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='店铺表';

-- ----------------------------
-- Records of t_shop
-- ----------------------------
BEGIN;
INSERT INTO `t_shop` (`id`, `shop_code`, `shop_name`, `seller_id`, `company_name`, `tax_id`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1, 'DEFAULT', '默认店铺', NULL, '东莞市慕声商贸有限公司', '91441900MA4WNG4C6H', 1, '系统默认店铺', NULL, '2026-01-24 17:43:37', NULL, '2026-01-24 17:43:37');
COMMIT;

-- ----------------------------
-- Table structure for t_sys_config
-- ----------------------------
DROP TABLE IF EXISTS `t_sys_config`;
CREATE TABLE `t_sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  `config_desc` varchar(200) DEFAULT NULL COMMENT '配置描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';

-- ----------------------------
-- Records of t_sys_config
-- ----------------------------
BEGIN;
INSERT INTO `t_sys_config` (`id`, `config_key`, `config_value`, `config_desc`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1, 'company_name_en', 'dongguanshimushengshangmaoyouxiangongsi', 'Company English Name', '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
INSERT INTO `t_sys_config` (`id`, `config_key`, `config_value`, `config_desc`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (2, 'company_tax_id', '91441900MA4WNG4C6H', 'Company Tax ID', '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
INSERT INTO `t_sys_config` (`id`, `config_key`, `config_value`, `config_desc`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (3, 'purchase_ratio', '0.96', 'Purchase Amount Ratio', '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
INSERT INTO `t_sys_config` (`id`, `config_key`, `config_value`, `config_desc`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (4, 'rate_defer_max_days', '10', 'Exchange Rate Defer Max Days', '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_transaction_type_mapping
-- ----------------------------
DROP TABLE IF EXISTS `t_transaction_type_mapping`;
CREATE TABLE `t_transaction_type_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `site_code` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '站点代码(NULL表示通用)',
  `original_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始交易类型',
  `standard_category` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标准分类(income/refund/fee/adjustment/other)',
  `category_desc` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类说明',
  `mapped_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '映射后的交易类型',
  `status` int DEFAULT '1' COMMENT '状态(1启用/0禁用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_site_original_type` (`site_code`,`original_type`),
  KEY `idx_standard_category` (`standard_category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=73 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易类型映射表';

-- ----------------------------
-- Records of t_transaction_type_mapping
-- ----------------------------
BEGIN;
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (32, NULL, 'Order', 'income', '订单', 'Order', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (33, NULL, 'Refund', 'refund', '退款', 'Refund', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (34, NULL, 'FBA Customer Return Fee', 'fee', 'FBA客户退货费', 'ServiceFee', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (35, NULL, 'Fulfilment by Amazon customer return fee', 'fee', 'FBA客户退货费(UK)', 'ServiceFee', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (36, NULL, 'Adjustment', 'adjustment', '调整', 'Adjustment', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (37, NULL, 'Liquidations', 'other', '清仓', 'Liquidations', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (38, NULL, 'FBA Inventory Fee', 'fee', 'FBA库存费', 'FBAInventoryFee', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (39, NULL, 'Order_Retrocharge', 'income', '订单追溯收费', 'Retrocharge', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (40, NULL, 'Refund_Retrocharge', 'refund', '退款追溯', 'RefundRetrocharge', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (41, NULL, 'Service Fee', 'fee', '服务费', 'ServiceFee', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (42, NULL, 'Transfer', 'other', '转账', 'Transfer', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (43, NULL, 'Amazon Fees', 'fee', '亚马逊费用', 'AmazonFees', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (44, NULL, 'Liquidations Adjustments', 'adjustment', '清仓调整', 'LiquidationsAdjustments', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (45, NULL, 'Chargeback Refund', 'adjustment', '退款追回', 'ChargebackRefund', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (46, NULL, 'Debt', 'other', '债务', 'Debt', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (47, 'DE', 'Bestellung', 'income', '订单(德语)', 'Order', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (48, 'DE', 'Erstattung', 'refund', '退款(德语)', 'Refund', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (49, 'DE', 'Gebühren für Kundenrücksendungen mit Versand durch Amazon', 'fee', 'FBA客户退货费(德语)', 'ServiceFee', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (50, 'DE', 'Anpassung', 'adjustment', '调整(德语)', 'Adjustment', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (51, 'DE', 'Liquidationen', 'other', '清仓(德语)', 'Liquidations', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (52, 'DE', 'Versand durch Amazon Lagergebühr', 'fee', 'FBA库存费(德语)', 'FBAInventoryFee', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (53, 'DE', 'Bestellung_Wiedereinzug', 'income', '订单追溯收费(德语)', 'Retrocharge', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (54, 'DE', 'Erstattung_Wiedereinzug', 'refund', '退款追溯(德语)', 'RefundRetrocharge', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (55, 'DE', 'Servicegebühr', 'fee', '服务费(德语)', 'ServiceFee', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (56, 'DE', 'Übertrag', 'other', '转账(德语)', 'Transfer', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (57, 'DE', 'Gebühren von Amazon', 'fee', '亚马逊费用(德语)', 'AmazonFees', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (58, 'DE', 'Liquidationsanpassungen', 'adjustment', '清仓调整(德语)', 'LiquidationsAdjustments', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (59, 'DE', 'Verbindlichkeit', 'other', '债务(德语)', 'Debt', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (60, 'ERP', 'Shipment', 'income', '发货', 'Shipment', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (61, 'ERP', 'Refund', 'refund', '退款', 'Refund', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (62, 'ERP', 'ServiceFee', 'fee', '服务费', 'ServiceFee', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (63, 'ERP', 'RemovalShipment', 'fee', 'FBA移除发货', 'RemovalShipment', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (64, 'ERP', 'Adjustment', 'adjustment', '调整', 'Adjustment', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (65, 'ERP', 'Retrocharge', 'income', '追溯收费', 'Retrocharge', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (66, 'ERP', 'CouponPayment', 'fee', '优惠券支付', 'CouponPayment', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (67, 'ERP', 'RemovalShipmentAdjustment', 'adjustment', '移除发货调整', 'RemovalShipmentAdjustment', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (68, 'ERP', 'SellerDealPayment', 'fee', '秒杀支付', 'SellerDealPayment', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (69, 'ERP', 'Liquidations Adjustments', 'adjustment', '清仓调整', 'LiquidationsAdjustments', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (70, 'ERP', 'Chargeback', 'adjustment', '退款追回', 'Chargeback', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `mapped_type`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (71, 'ERP', 'ProductAdsPayment', 'fee', '产品广告支付', 'ProductAdsPayment', 1, '2026-01-24 22:54:36', '2026-01-24 22:54:36', '1', NULL);
COMMIT;

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码(BCrypt加密)',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `role_code` varchar(20) NOT NULL COMMENT '角色编码(admin/finance/viewer)',
  `status` tinyint DEFAULT '1' COMMENT '状态(1启用/0禁用)',
  `login_fail_count` int DEFAULT '0' COMMENT '登录失败次数',
  `lock_time` datetime DEFAULT NULL COMMENT '锁定时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除(0正常/1删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ----------------------------
-- Records of t_user
-- ----------------------------
BEGIN;
INSERT INTO `t_user` (`id`, `username`, `password`, `real_name`, `email`, `phone`, `avatar`, `role_code`, `status`, `login_fail_count`, `lock_time`, `last_login_time`, `last_login_ip`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuP/BamFMGn3oodMYeib/YnU6EWYiKUL.', 'System Administrator', NULL, NULL, NULL, 'admin', 1, 0, NULL, '2026-02-22 21:19:48', '0:0:0:0:0:0:0:1', NULL, '2026-01-19 20:38:39', NULL, '2026-01-19 21:00:54', 0);
COMMIT;

-- ----------------------------
-- Triggers structure for table t_advertising_data
-- ----------------------------
DROP TRIGGER IF EXISTS `trg_advertising_before_insert`;
delimiter ;;
CREATE TRIGGER `musheng_tax`.`trg_advertising_before_insert` BEFORE INSERT ON `t_advertising_data` FOR EACH ROW BEGIN
  -- 根据店铺名称推断站点编码
  IF NEW.site_code IS NULL OR NEW.site_code = '' THEN
    SET NEW.site_code = CASE
      WHEN NEW.store_name LIKE '%US%' THEN 'US'
      WHEN NEW.store_name LIKE '%CA%' THEN 'CA'
      WHEN NEW.store_name LIKE '%UK%' THEN 'UK'
      WHEN NEW.store_name LIKE '%DE%' THEN 'DE'
      ELSE NULL
    END;
  END IF;

  -- 计算人民币金额（如果有汇率）
  IF NEW.exchange_rate IS NOT NULL AND NEW.exchange_rate > 0 THEN
    SET NEW.amount_cny = NEW.cost * NEW.exchange_rate;
  END IF;
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
