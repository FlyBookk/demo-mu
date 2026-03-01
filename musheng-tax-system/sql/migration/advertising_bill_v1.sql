-- 广告数据主表+明细表结构（覆盖替换）
-- 执行时间: 2026-03-01
-- 说明：主表按发票汇总，明细表按广告活动记录，无去重，可复核

SET FOREIGN_KEY_CHECKS = 0;

-- 删除旧触发器
DROP TRIGGER IF EXISTS `trg_advertising_before_insert`;

-- 删除旧表
DROP TABLE IF EXISTS `t_advertising_data`;

-- ----------------------------
-- t_advertising_bill 广告发票主表（按发票维度，用于汇总）
-- ----------------------------
CREATE TABLE `t_advertising_bill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `store_name` varchar(100) NOT NULL COMMENT '店铺名称',
  `site_code` varchar(10) DEFAULT NULL COMMENT '站点编码',
  `invoice_number` varchar(50) NOT NULL COMMENT '发票编号',
  `invoice_status` varchar(50) DEFAULT NULL COMMENT '发票状态（PAID_IN_FULL等）',
  `payment_type` varchar(50) DEFAULT NULL COMMENT '支付类型（CREDIT_CARD等）',
  `billing_start_date` date NOT NULL COMMENT '账单开始日期',
  `billing_end_date` date NOT NULL COMMENT '账单结束日期',
  `issue_date` date DEFAULT NULL COMMENT '开具时间',
  `currency` varchar(10) NOT NULL COMMENT '付款币种（GBP/USD等）',
  `invoice_amount` decimal(12,2) NOT NULL COMMENT '账单金额',
  `total_cost` decimal(12,2) DEFAULT NULL COMMENT '费用合计（明细汇总）',
  `total_cost_cny` decimal(15,4) DEFAULT NULL COMMENT '费用合计人民币',
  `import_batch_id` varchar(50) DEFAULT NULL COMMENT '导入批次ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `deleted` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_invoice` (`shop_id`,`invoice_number`),
  KEY `idx_site_code` (`site_code`),
  KEY `idx_billing_dates` (`billing_start_date`,`billing_end_date`),
  KEY `idx_issue_date` (`issue_date`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_import_batch` (`import_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='广告发票主表';

-- ----------------------------
-- t_advertising_bill_item 广告发票明细表（按广告活动维度，可复核）
-- ----------------------------
CREATE TABLE `t_advertising_bill_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID',
  `bill_id` bigint NOT NULL COMMENT '发票主表ID',
  `invoice_number` varchar(50) NOT NULL COMMENT '发票编号（冗余）',
  `campaign_name` varchar(200) DEFAULT NULL COMMENT '广告活动',
  `campaign_id` varchar(50) NOT NULL COMMENT '活动ID',
  `pricing_model` varchar(20) DEFAULT NULL COMMENT '计价方式（CPC/CPM等）',
  `clicks` int DEFAULT 0 COMMENT '点击',
  `avg_cpc` decimal(10,4) DEFAULT 0 COMMENT '平均点击单价',
  `cost` decimal(12,2) NOT NULL COMMENT '费用',
  `other_cost` decimal(12,2) DEFAULT 0 COMMENT '其他费分摊',
  `data_source` varchar(50) DEFAULT NULL COMMENT '取值来源',
  `product_list` text COMMENT '承担商品',
  `ad_type` varchar(100) DEFAULT NULL COMMENT '广告类型',
  `exchange_rate` decimal(10,6) DEFAULT NULL COMMENT '汇率',
  `exchange_rate_date` date DEFAULT NULL COMMENT '汇率日期',
  `amount_cny` decimal(15,4) DEFAULT NULL COMMENT '费用人民币',
  `import_batch_id` varchar(50) DEFAULT NULL COMMENT '导入批次ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `deleted` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_campaign` (`bill_id`,`campaign_id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_invoice_number` (`invoice_number`),
  KEY `idx_campaign_id` (`campaign_id`),
  KEY `idx_import_batch` (`import_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='广告发票明细表';

SET FOREIGN_KEY_CHECKS = 1;
