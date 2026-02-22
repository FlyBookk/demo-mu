/*
 Navicat Premium SQL 导出文件

 源数据库服务器         : localhost
 源数据库类型           : MySQL
 源数据库版本           : 90300 (9.3.0)
 源数据库主机           : localhost:3306
 源数据库名称           : musheng_tax_bak

 目标数据库类型         : MySQL
 目标数据库版本         : 90300 (9.3.0)
 文件编码               : 65001

 导出日期: 2026-01-19 22:49:48
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 表结构: t_advertising_data (广告数据表)
-- ----------------------------
DROP TABLE IF EXISTS `t_advertising_data`;
CREATE TABLE `t_advertising_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `site_code` varchar(10) NOT NULL COMMENT '站点编码',
  `year_month` varchar(10) NOT NULL COMMENT '年月(YYYY-MM)',
  `invoice_no` varchar(100) DEFAULT NULL COMMENT '发票号',
  `amount` decimal(15,4) NOT NULL COMMENT '广告费金额(原币)',
  `currency_code` varchar(10) NOT NULL COMMENT '货币编码',
  `amount_cny` decimal(15,4) DEFAULT NULL COMMENT '广告费金额(人民币)',
  `exchange_rate` decimal(10,6) DEFAULT NULL COMMENT '使用的汇率',
  `attachment_path` varchar(500) DEFAULT NULL COMMENT '发票附件路径',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_site_month` (`site_code`,`year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='广告数据表';

-- ----------------------------
-- 数据记录: t_advertising_data
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- 表结构: t_currency (货币表)
-- ----------------------------
DROP TABLE IF EXISTS `t_currency`;
CREATE TABLE `t_currency` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `currency_code` varchar(10) NOT NULL COMMENT '货币编码(USD/CAD/GBP/EUR/CNY)',
  `currency_name` varchar(50) NOT NULL COMMENT '货币名称',
  `currency_symbol` varchar(10) DEFAULT NULL COMMENT '货币符号',
  `decimal_places` int DEFAULT '2' COMMENT '小数位数',
  `status` tinyint DEFAULT '1' COMMENT '状态(1启用/0禁用)',
  `pair_direction` varchar(20) DEFAULT 'DIRECT' COMMENT '货币对方向(DIRECT=XXX/CNY, REVERSE=CNY/XXX)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `exchange_rate` decimal(10,6) DEFAULT NULL COMMENT '使用的汇率',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_currency_code` (`currency_code`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='货币表';

-- ----------------------------
-- 数据记录: t_currency
-- ----------------------------
BEGIN;
INSERT INTO `t_currency` (`id`, `currency_code`, `currency_name`, `currency_symbol`, `decimal_places`, `status`, `pair_direction`, `create_time`, `update_time`, `exchange_rate`, `create_by`, `update_by`) VALUES (6, 'USD', 'US Dollar', '$', 2, 1, 'DIRECT', '2026-01-19 22:03:43', '2026-01-19 22:03:43', NULL, NULL, NULL);
INSERT INTO `t_currency` (`id`, `currency_code`, `currency_name`, `currency_symbol`, `decimal_places`, `status`, `pair_direction`, `create_time`, `update_time`, `exchange_rate`, `create_by`, `update_by`) VALUES (7, 'CAD', 'Canadian Dollar', 'C$', 2, 1, 'DIRECT', '2026-01-19 22:03:43', '2026-01-19 22:03:43', NULL, NULL, NULL);
INSERT INTO `t_currency` (`id`, `currency_code`, `currency_name`, `currency_symbol`, `decimal_places`, `status`, `pair_direction`, `create_time`, `update_time`, `exchange_rate`, `create_by`, `update_by`) VALUES (8, 'GBP', 'British Pound', '£', 2, 1, 'DIRECT', '2026-01-19 22:03:43', '2026-01-19 22:03:43', NULL, NULL, NULL);
INSERT INTO `t_currency` (`id`, `currency_code`, `currency_name`, `currency_symbol`, `decimal_places`, `status`, `pair_direction`, `create_time`, `update_time`, `exchange_rate`, `create_by`, `update_by`) VALUES (9, 'EUR', 'Euro', '€', 2, 1, 'DIRECT', '2026-01-19 22:03:43', '2026-01-19 22:03:43', NULL, NULL, NULL);
INSERT INTO `t_currency` (`id`, `currency_code`, `currency_name`, `currency_symbol`, `decimal_places`, `status`, `pair_direction`, `create_time`, `update_time`, `exchange_rate`, `create_by`, `update_by`) VALUES (10, 'CNY', 'Chinese Yuan', '¥', 2, 1, 'DIRECT', '2026-01-19 22:03:43', '2026-01-19 22:03:43', NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- 表结构: t_exchange_rate (汇率数据表)
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='汇率数据表';

-- ----------------------------
-- 数据记录: t_exchange_rate
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- 表结构: t_field_mapping_template (字段映射模板表)
-- ----------------------------
DROP TABLE IF EXISTS `t_field_mapping_template`;
CREATE TABLE `t_field_mapping_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `site_code` varchar(10) NOT NULL COMMENT '站点编码',
  `data_type` varchar(20) NOT NULL DEFAULT 'sales' COMMENT '数据类型(sales/shipping)',
  `mapping_config` json NOT NULL COMMENT '映射配置(JSON格式)',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认模板(1是/0否)',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_site_type` (`site_code`,`data_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字段映射模板表';

-- ----------------------------
-- 数据记录: t_field_mapping_template
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- 表结构: t_import_error_detail (导入错误明细表)
-- ----------------------------
DROP TABLE IF EXISTS `t_import_error_detail`;
CREATE TABLE `t_import_error_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `import_record_id` bigint NOT NULL COMMENT '关联导入记录ID',
  `row_number` int NOT NULL COMMENT '行号',
  `error_field` varchar(100) DEFAULT NULL COMMENT '错误字段',
  `error_value` text COMMENT '错误值',
  `error_code` varchar(50) DEFAULT NULL COMMENT '错误代码',
  `error_message` varchar(500) NOT NULL COMMENT '错误原因',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_import_record` (`import_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导入错误明细表';

-- ----------------------------
-- 数据记录: t_import_error_detail
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- 表结构: t_import_record (导入记录表)
-- ----------------------------
DROP TABLE IF EXISTS `t_import_record`;
CREATE TABLE `t_import_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_no` varchar(50) NOT NULL COMMENT '导入批次号',
  `data_type` varchar(20) NOT NULL COMMENT '数据类型(sales/shipping/advertising/rate)',
  `file_name` varchar(200) NOT NULL COMMENT '文件名',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小(字节)',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件存储路径',
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
  KEY `idx_status` (`import_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导入记录表';

-- ----------------------------
-- 数据记录: t_import_record
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- 表结构: t_log_operation (操作日志表)
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
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表';

-- ----------------------------
-- 数据记录: t_log_operation
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- 表结构: t_marketplace (站点表)
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
-- 数据记录: t_marketplace
-- ----------------------------
BEGIN;
INSERT INTO `t_marketplace` (`id`, `site_code`, `site_name`, `marketplace_id`, `currency_code`, `seller_id`, `header_language`, `date_format`, `number_format`, `timezone`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1, 'US', 'US Site', 'amazon.com', 'USD', 'APNDJLWNA7H88', 'EN', 'MMM d, yyyy h:mm:ss a z', '.', 'PDT', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL);
INSERT INTO `t_marketplace` (`id`, `site_code`, `site_name`, `marketplace_id`, `currency_code`, `seller_id`, `header_language`, `date_format`, `number_format`, `timezone`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (2, 'CA', 'Canada Site', 'amazon.ca', 'CAD', 'APNDJLWNA7H88', 'EN', 'MMM d, yyyy h:mm:ss a z', '.', 'PDT', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL);
INSERT INTO `t_marketplace` (`id`, `site_code`, `site_name`, `marketplace_id`, `currency_code`, `seller_id`, `header_language`, `date_format`, `number_format`, `timezone`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (3, 'UK', 'UK Site', 'amazon.co.uk', 'GBP', 'A11Y51I5ZXY4AO', 'EN', 'dd MMM yyyy HH:mm:ss z', '.', 'UTC', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL);
INSERT INTO `t_marketplace` (`id`, `site_code`, `site_name`, `marketplace_id`, `currency_code`, `seller_id`, `header_language`, `date_format`, `number_format`, `timezone`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (4, 'DE', 'Germany Site', 'amazon.de', 'EUR', 'A11Y51I5ZXY4AO', 'DE', 'dd.MM.yyyy HH:mm:ss z', ',', 'UTC', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL);
COMMIT;

-- ----------------------------
-- 表结构: t_role (角色表)
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
-- 数据记录: t_role
-- ----------------------------
BEGIN;
INSERT INTO `t_role` (`id`, `role_code`, `role_name`, `role_desc`, `permissions`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1, 'admin', 'Administrator', 'Full system access', '[\"*\"]', 1, '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
INSERT INTO `t_role` (`id`, `role_code`, `role_name`, `role_desc`, `permissions`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (2, 'finance', 'Finance Staff', 'All permissions except user management', '[\"config:*\", \"sales:*\", \"shipping:*\", \"advertising:*\", \"rate:*\", \"report:*\", \"import:*\"]', 1, '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
INSERT INTO `t_role` (`id`, `role_code`, `role_name`, `role_desc`, `permissions`, `status`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (3, 'viewer', 'Viewer', 'View-only access based on menu permissions', '[]', 1, '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
COMMIT;

-- ----------------------------
-- 表结构: t_sales_data (销售数据表)
-- ----------------------------
DROP TABLE IF EXISTS `t_sales_data`;
CREATE TABLE `t_sales_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `import_batch_id` bigint DEFAULT NULL COMMENT '导入批次ID',
  `transaction_date` datetime NOT NULL COMMENT '交易日期时间',
  `original_date_str` varchar(50) DEFAULT NULL COMMENT '原始日期字符串',
  `original_timezone` varchar(20) DEFAULT NULL COMMENT '原始时区',
  `settlement_id` varchar(50) DEFAULT '' COMMENT '结算编号',
  `transaction_type` varchar(100) NOT NULL COMMENT '交易类型(保留原始值)',
  `transaction_category` varchar(20) NOT NULL COMMENT '交易分类(income/refund/fee/adjustment/other)',
  `order_id` varchar(50) NOT NULL COMMENT '订单号',
  `sku` varchar(100) DEFAULT '' COMMENT 'SKU',
  `description` text COMMENT '商品描述',
  `quantity` int DEFAULT '0' COMMENT '数量',
  `site_code` varchar(10) NOT NULL COMMENT '站点编码',
  `marketplace` varchar(50) NOT NULL COMMENT '原始marketplace值',
  `currency_code` varchar(10) NOT NULL COMMENT '货币编码',
  `account_type` varchar(50) DEFAULT '' COMMENT '账户类型(仅US有)',
  `fulfillment` varchar(50) DEFAULT '' COMMENT '配送方式',
  `order_city` varchar(100) DEFAULT '' COMMENT '订单城市',
  `order_state` varchar(100) DEFAULT '' COMMENT '订单省/州',
  `order_postal` varchar(20) DEFAULT '' COMMENT '订单邮编',
  `tax_collection_model` varchar(50) DEFAULT '' COMMENT '税收模式',
  `product_sales` decimal(15,4) DEFAULT '0.0000' COMMENT '产品销售额',
  `product_sales_tax` decimal(15,4) DEFAULT '0.0000' COMMENT '产品税',
  `shipping_credits` decimal(15,4) DEFAULT '0.0000' COMMENT '运费收入',
  `shipping_credits_tax` decimal(15,4) DEFAULT '0.0000' COMMENT '运费税',
  `gift_wrap_credits` decimal(15,4) DEFAULT '0.0000' COMMENT '礼品包装费',
  `gift_wrap_credits_tax` decimal(15,4) DEFAULT '0.0000' COMMENT '礼品包装税',
  `regulatory_fee` decimal(15,4) DEFAULT '0.0000' COMMENT '监管费(UK/DE默认0)',
  `regulatory_fee_tax` decimal(15,4) DEFAULT '0.0000' COMMENT '监管费税(UK/DE默认0)',
  `promotional_rebates` decimal(15,4) DEFAULT '0.0000' COMMENT '促销折扣',
  `promotional_rebates_tax` decimal(15,4) DEFAULT '0.0000' COMMENT '促销折扣税',
  `marketplace_withheld_tax` decimal(15,4) DEFAULT '0.0000' COMMENT '平台代扣税',
  `selling_fees` decimal(15,4) DEFAULT '0.0000' COMMENT '销售费用',
  `fba_fees` decimal(15,4) DEFAULT '0.0000' COMMENT 'FBA费用',
  `other_transaction_fees` decimal(15,4) DEFAULT '0.0000' COMMENT '其他交易费',
  `other` decimal(15,4) DEFAULT '0.0000' COMMENT '其他',
  `total` decimal(15,4) DEFAULT '0.0000' COMMENT '合计',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_category` (`order_id`,`transaction_category`),
  KEY `idx_site_date` (`site_code`,`transaction_date`),
  KEY `idx_batch` (`import_batch_id`),
  KEY `idx_transaction_category` (`transaction_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售数据表';

-- ----------------------------
-- 数据记录: t_sales_data
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- 表结构: t_shipping_data (配送数据表)
-- ----------------------------
DROP TABLE IF EXISTS `t_shipping_data`;
CREATE TABLE `t_shipping_data` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
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
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_site_ship_date` (`site_code`,`ship_date`),
  KEY `idx_batch` (`import_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='配送数据表';

-- ----------------------------
-- 数据记录: t_shipping_data
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- 表结构: t_summary_cache (汇总结果缓存表)
-- ----------------------------
DROP TABLE IF EXISTS `t_summary_cache`;
CREATE TABLE `t_summary_cache` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `summary_year` int NOT NULL COMMENT '汇总年份',
  `summary_quarter` varchar(10) NOT NULL COMMENT '汇总季度(Q1/Q2/Q3/Q4)',
  `site_code` varchar(10) NOT NULL COMMENT '站点编码',
  `seller_id` varchar(50) DEFAULT NULL COMMENT '卖家ID(MCID)',
  `company_name` varchar(200) DEFAULT NULL COMMENT '公司名称',
  `tax_id` varchar(50) DEFAULT NULL COMMENT '统一社会信用代码',
  `total_revenue` decimal(18,4) DEFAULT '0.0000' COMMENT '收入总额(人民币)',
  `refund_amount` decimal(18,4) DEFAULT '0.0000' COMMENT '退款金额(人民币)',
  `net_income` decimal(18,4) DEFAULT '0.0000' COMMENT '收入净额(人民币)',
  `platform_fees` decimal(18,4) DEFAULT '0.0000' COMMENT '平台费用(人民币)',
  `advertising_fees` decimal(18,4) DEFAULT '0.0000' COMMENT '广告费(人民币)',
  `transaction_count` int DEFAULT '0' COMMENT '交易数量',
  `order_count` int DEFAULT '0' COMMENT '订单数量',
  `cache_status` varchar(20) DEFAULT 'valid' COMMENT '缓存状态(valid/invalid/calculating)',
  `calculate_time` datetime DEFAULT NULL COMMENT '计算时间',
  `data_version` bigint DEFAULT '1' COMMENT '数据版本号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_year_quarter_site` (`summary_year`,`summary_quarter`,`site_code`),
  KEY `idx_cache_status` (`cache_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='汇总结果缓存表';

-- ----------------------------
-- 数据记录: t_summary_cache
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- 表结构: t_sys_config (系统配置表)
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
-- 数据记录: t_sys_config
-- ----------------------------
BEGIN;
INSERT INTO `t_sys_config` (`id`, `config_key`, `config_value`, `config_desc`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (1, 'company_name_en', 'dongguanshimushengshangmaoyouxiangongsi', 'Company English Name', '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
INSERT INTO `t_sys_config` (`id`, `config_key`, `config_value`, `config_desc`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (2, 'company_tax_id', '91441900MA4WNG4C6H', 'Company Tax ID', '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
INSERT INTO `t_sys_config` (`id`, `config_key`, `config_value`, `config_desc`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (3, 'purchase_ratio', '0.96', 'Purchase Amount Ratio', '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
INSERT INTO `t_sys_config` (`id`, `config_key`, `config_value`, `config_desc`, `create_time`, `update_time`, `create_by`, `update_by`) VALUES (4, 'rate_defer_max_days', '10', 'Exchange Rate Defer Max Days', '2026-01-19 20:38:39', '2026-01-19 20:38:39', NULL, NULL);
COMMIT;

-- ----------------------------
-- 表结构: t_transaction_type_mapping (交易类型映射表)
-- ----------------------------
DROP TABLE IF EXISTS `t_transaction_type_mapping`;
CREATE TABLE `t_transaction_type_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `site_code` varchar(10) DEFAULT NULL COMMENT '站点编码(可为空表示通用)',
  `original_type` varchar(100) NOT NULL COMMENT '原始交易类型',
  `standard_category` varchar(20) NOT NULL COMMENT '标准分类(income/refund/fee/adjustment/other)',
  `category_desc` varchar(50) DEFAULT NULL COMMENT '分类说明',
  `status` tinyint DEFAULT '1' COMMENT '状态(1启用/0禁用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `mapped_type` varchar(20) DEFAULT '' COMMENT '映射后的交易类型',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_site_type` (`site_code`,`original_type`),
  KEY `idx_standard_category` (`standard_category`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易类型映射表';

-- ----------------------------
-- 数据记录: t_transaction_type_mapping
-- ----------------------------
BEGIN;
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `mapped_type`) VALUES (1, NULL, 'Order', 'income', 'Income', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL, '');
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `mapped_type`) VALUES (2, NULL, 'Refund', 'refund', 'Refund', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL, '');
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `mapped_type`) VALUES (3, NULL, 'FBA Customer Return Fee', 'fee', 'FBA Return Fee', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL, '');
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `mapped_type`) VALUES (4, NULL, 'Service Fee', 'fee', 'Service Fee', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL, '');
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `mapped_type`) VALUES (5, NULL, 'Adjustment', 'adjustment', 'Adjustment', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL, '');
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `mapped_type`) VALUES (6, NULL, 'Liquidations', 'other', 'Liquidations', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL, '');
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `mapped_type`) VALUES (7, NULL, 'Refund_Retrocharge', 'other', 'Retrocharge Refund (not calculated)', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL, '');
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `mapped_type`) VALUES (8, 'DE', 'Bestellung', 'income', 'Income', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL, '');
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `mapped_type`) VALUES (9, 'DE', 'Erstattung', 'refund', 'Refund', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL, '');
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `mapped_type`) VALUES (10, 'DE', 'Gebühren für Kundenrücksendungen mit Versand durch Amazon', 'fee', 'FBA Return Fee', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL, '');
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `mapped_type`) VALUES (11, 'DE', 'Servicegebühr', 'fee', 'Service Fee', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL, '');
INSERT INTO `t_transaction_type_mapping` (`id`, `site_code`, `original_type`, `standard_category`, `category_desc`, `status`, `create_time`, `update_time`, `create_by`, `update_by`, `mapped_type`) VALUES (12, 'DE', 'Anpassung', 'adjustment', 'Adjustment', 1, '2026-01-19 20:30:07', '2026-01-19 20:30:07', NULL, NULL, '');
COMMIT;

-- ----------------------------
-- 表结构: t_user (用户表)
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
-- 数据记录: t_user
-- ----------------------------
BEGIN;
INSERT INTO `t_user` (`id`, `username`, `password`, `real_name`, `email`, `phone`, `avatar`, `role_code`, `status`, `login_fail_count`, `lock_time`, `last_login_time`, `last_login_ip`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`) VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuP/BamFMGn3oodMYeib/YnU6EWYiKUL.', 'System Administrator', NULL, NULL, NULL, 'admin', 1, 0, NULL, '2026-01-19 22:41:13', '0:0:0:0:0:0:0:1', NULL, '2026-01-19 20:38:39', NULL, '2026-01-19 21:00:54', 0);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
