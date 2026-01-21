/*
 * 广告模块优化 - 数据库变更脚本
 * 版本: v1.0
 * 创建日期: 2026-01-20
 * 说明: 广告数据表结构优化，支持详细的广告发票数据导入
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 第一步：备份现有广告数据表
-- ============================================================================
DROP TABLE IF EXISTS `t_advertising_data_backup_20260120`;
CREATE TABLE `t_advertising_data_backup_20260120` LIKE `t_advertising_data`;
INSERT INTO `t_advertising_data_backup_20260120` SELECT * FROM `t_advertising_data`;

-- 备份完成后验证
SELECT CONCAT('备份完成，原表记录数: ', COUNT(*)) as backup_status FROM `t_advertising_data`;
SELECT CONCAT('备份表记录数: ', COUNT(*)) as backup_status FROM `t_advertising_data_backup_20260120`;

-- ============================================================================
-- 第二步：删除旧表，创建新表结构
-- ============================================================================
DROP TABLE IF EXISTS `t_advertising_data`;

CREATE TABLE `t_advertising_data` (
  -- 基础字段
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',

  -- 店铺与站点信息
  `store_name` varchar(100) NOT NULL COMMENT '店铺名称',
  `site_code` varchar(10) DEFAULT NULL COMMENT '站点编码（从店铺名称推断）',

  -- 发票信息
  `invoice_number` varchar(50) NOT NULL COMMENT '发票编号（去重关键字段）',
  `invoice_status` varchar(50) NOT NULL COMMENT '发票状态（PAID_IN_FULL等）',
  `payment_type` varchar(50) DEFAULT NULL COMMENT '支付类型（CREDIT_CARD等）',

  -- 账单周期（核心变更：双时间字段）
  `billing_start_date` date NOT NULL COMMENT '账单开始日期',
  `billing_end_date` date NOT NULL COMMENT '账单结束日期',

  -- 时间信息
  `issue_date` date NOT NULL COMMENT '发票开具日期',

  -- 金额与货币
  `currency` varchar(10) NOT NULL COMMENT '付款币种（USD/CAD/GBP/EUR）',
  `invoice_amount` decimal(10,2) NOT NULL COMMENT '账单金额（发票总金额）',
  `cost` decimal(10,2) NOT NULL COMMENT '费用（实际花费）',
  `other_cost` decimal(10,2) DEFAULT 0.00 COMMENT '其他费分摊',

  -- 广告活动信息
  `campaign_name` varchar(200) DEFAULT NULL COMMENT '广告活动名称',
  `campaign_id` varchar(50) DEFAULT NULL COMMENT '活动ID（广告活动唯一标识）',
  `pricing_model` varchar(20) DEFAULT NULL COMMENT '计价方式（CPC/CPM等）',

  -- 广告数据统计
  `clicks` int DEFAULT 0 COMMENT '点击次数',
  `avg_cpc` decimal(10,2) DEFAULT 0.00 COMMENT '平均点击单价',

  -- 元数据
  `data_source` varchar(50) DEFAULT NULL COMMENT '取值来源（业务报告等）',
  `product_list` text DEFAULT NULL COMMENT '承担商品（逗号分隔）',
  `ad_type` varchar(100) DEFAULT NULL COMMENT '广告类型（SPONSORED PRODUCTS等）',

  -- 附件与备注
  `attachment_path` varchar(500) DEFAULT NULL COMMENT '发票附件路径',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',

  -- 汇率与人民币金额（用于报表）
  `exchange_rate` decimal(10,6) DEFAULT NULL COMMENT '使用的汇率',
  `amount_cny` decimal(15,4) DEFAULT NULL COMMENT '费用金额（人民币）',

  -- 导入批次信息
  `import_batch_id` varchar(50) DEFAULT NULL COMMENT '导入批次ID',

  -- 审计字段
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除(0未删除/1已删除)',

  -- 主键
  PRIMARY KEY (`id`),

  -- 唯一索引：发票编号（保证去重）
  UNIQUE KEY `uk_invoice_number` (`invoice_number`),

  -- 普通索引
  KEY `idx_store_name` (`store_name`),
  KEY `idx_billing_dates` (`billing_start_date`, `billing_end_date`),
  KEY `idx_campaign_id` (`campaign_id`),
  KEY `idx_ad_type` (`ad_type`),
  KEY `idx_created_at` (`create_time`),
  KEY `idx_site_code` (`site_code`),
  KEY `idx_import_batch` (`import_batch_id`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='广告发票记录表（优化版）';

-- ============================================================================
-- 第三步：数据迁移（如果备份表中有数据）
-- ============================================================================
-- 注意：旧表结构与新表结构字段差异较大，需要手动映射
-- 以下脚本仅作为示例，实际迁移需根据业务情况调整

/*
INSERT INTO `t_advertising_data` (
  `store_name`,
  `site_code`,
  `invoice_number`,
  `invoice_status`,
  `billing_start_date`,
  `billing_end_date`,
  `issue_date`,
  `currency`,
  `invoice_amount`,
  `cost`,
  `exchange_rate`,
  `amount_cny`,
  `attachment_path`,
  `remark`,
  `create_by`,
  `create_time`,
  `update_by`,
  `update_time`
)
SELECT
  CONCAT('未知店铺-', site_code) as store_name,  -- 旧表无店铺字段，需手动补充
  site_code,
  COALESCE(invoice_no, CONCAT('MIGRATED-', id)) as invoice_number,  -- 如无发票号则生成
  'PAID_IN_FULL' as invoice_status,  -- 默认为已支付
  STR_TO_DATE(CONCAT(year_month, '-01'), '%Y-%m-%d') as billing_start_date,  -- 月份第一天
  LAST_DAY(STR_TO_DATE(CONCAT(year_month, '-01'), '%Y-%m-%d')) as billing_end_date,  -- 月份最后一天
  STR_TO_DATE(CONCAT(year_month, '-01'), '%Y-%m-%d') as issue_date,  -- 默认开具日期为月初
  currency_code as currency,
  amount as invoice_amount,
  amount as cost,
  exchange_rate,
  amount_cny,
  attachment_path,
  remark,
  create_by,
  create_time,
  update_by,
  update_time
FROM `t_advertising_data_backup_20260120`
WHERE 1=1;  -- 可根据需要添加条件
*/

-- 迁移后验证
-- SELECT CONCAT('迁移完成，新表记录数: ', COUNT(*)) as migration_status FROM `t_advertising_data`;

-- ============================================================================
-- 第四步：创建触发器（可选：自动填充站点编码）
-- ============================================================================
DELIMITER $$

DROP TRIGGER IF EXISTS `trg_advertising_before_insert` $$
CREATE TRIGGER `trg_advertising_before_insert`
BEFORE INSERT ON `t_advertising_data`
FOR EACH ROW
BEGIN
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
END$$

DELIMITER ;

-- ============================================================================
-- 第五步：添加表注释和字段说明
-- ============================================================================
ALTER TABLE `t_advertising_data` COMMENT = '广告发票记录表（优化版） - 支持详细的广告发票数据导入，包含双时间字段和发票编号去重';

-- ============================================================================
-- 验证脚本
-- ============================================================================
-- 查看表结构
DESC `t_advertising_data`;

-- 查看索引
SHOW INDEX FROM `t_advertising_data`;

-- 查看触发器
SHOW TRIGGERS LIKE 't_advertising_data';

-- ============================================================================
-- 回滚脚本（如需回滚，执行以下语句）
-- ============================================================================
/*
DROP TABLE IF EXISTS `t_advertising_data`;
RENAME TABLE `t_advertising_data_backup_20260120` TO `t_advertising_data`;
*/

-- ============================================================================
-- 完成
-- ============================================================================
SELECT '数据库变更脚本执行完成' as status;

SET FOREIGN_KEY_CHECKS = 1;
