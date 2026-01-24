-- =============================================
-- 店铺系统数据库变更
-- 版本: 2.0
-- 日期: 2026-01-24
-- 说明: 实现类SaaS多租户的店铺数据隔离机制
-- =============================================

SET NAMES utf8mb4;

-- =============================================
-- 1. 创建店铺表
-- =============================================
DROP TABLE IF EXISTS `t_shop`;
CREATE TABLE `t_shop` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `shop_code` varchar(50) NOT NULL COMMENT '店铺编码',
    `shop_name` varchar(100) NOT NULL COMMENT '店铺名称',
    `seller_id` varchar(50) DEFAULT NULL COMMENT '亚马逊卖家ID',
    `company_name` varchar(200) DEFAULT NULL COMMENT '公司名称',
    `tax_id` varchar(50) DEFAULT NULL COMMENT '统一社会信用代码',
    `status` tinyint DEFAULT 1 COMMENT '状态(1启用/0禁用)',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shop_code` (`shop_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='店铺表';

-- =============================================
-- 2. 业务表添加 shop_id 字段
-- =============================================

-- 2.1 销售数据表
ALTER TABLE `t_sales_data` ADD COLUMN `shop_id` bigint DEFAULT NULL COMMENT '店铺ID' AFTER `id`;
ALTER TABLE `t_sales_data` ADD INDEX `idx_shop_id` (`shop_id`);

-- 2.2 配送数据表
ALTER TABLE `t_shipping_data` ADD COLUMN `shop_id` bigint DEFAULT NULL COMMENT '店铺ID' AFTER `id`;
ALTER TABLE `t_shipping_data` ADD INDEX `idx_shop_id` (`shop_id`);

-- 2.3 广告数据表
ALTER TABLE `t_advertising_data` ADD COLUMN `shop_id` bigint DEFAULT NULL COMMENT '店铺ID' AFTER `id`;
ALTER TABLE `t_advertising_data` ADD INDEX `idx_shop_id` (`shop_id`);

-- 2.4 汇总缓存表
ALTER TABLE `t_summary_cache` ADD COLUMN `shop_id` bigint DEFAULT NULL COMMENT '店铺ID' AFTER `id`;
ALTER TABLE `t_summary_cache` ADD INDEX `idx_shop_id` (`shop_id`);

-- 2.5 FBA货件明细表
ALTER TABLE `t_fba_shipment_detail` ADD COLUMN `shop_id` bigint DEFAULT NULL COMMENT '店铺ID' AFTER `id`;
ALTER TABLE `t_fba_shipment_detail` ADD INDEX `idx_shop_id` (`shop_id`);

-- 2.6 导入记录表（业务数据导入关联店铺，汇率导入不关联）
ALTER TABLE `t_import_record` ADD COLUMN `shop_id` bigint DEFAULT NULL COMMENT '店铺ID（业务数据导入）' AFTER `id`;
ALTER TABLE `t_import_record` ADD INDEX `idx_shop_id` (`shop_id`);

-- =============================================
-- 3. 插入默认店铺
-- =============================================
INSERT INTO `t_shop` (`shop_code`, `shop_name`, `seller_id`, `company_name`, `tax_id`, `status`, `remark`)
VALUES ('DEFAULT', '默认店铺', NULL, '东莞市慕声商贸有限公司', '91441900MA4WNG4C6H', 1, '系统默认店铺');

-- =============================================
-- 4. 将现有业务数据关联到默认店铺（可选）
-- =============================================
-- UPDATE `t_sales_data` SET `shop_id` = 1 WHERE `shop_id` IS NULL;
-- UPDATE `t_shipping_data` SET `shop_id` = 1 WHERE `shop_id` IS NULL;
-- UPDATE `t_advertising_data` SET `shop_id` = 1 WHERE `shop_id` IS NULL;
-- UPDATE `t_fba_shipment_detail` SET `shop_id` = 1 WHERE `shop_id` IS NULL;
-- UPDATE `t_summary_cache` SET `shop_id` = 1 WHERE `shop_id` IS NULL;
-- UPDATE `t_import_record` SET `shop_id` = 1 WHERE `shop_id` IS NULL AND `data_type` IN ('sales', 'shipping', 'advertising', 'fba_shipment');
