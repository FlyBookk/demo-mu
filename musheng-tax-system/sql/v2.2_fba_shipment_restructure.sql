/*
 FBA货件明细模块重构 - 数据库迁移脚本

 功能说明:
 1. 删除旧的货件明细表（t_fba_shipment_detail）
 2. 创建新的货件主表（t_fba_shipment）- 存储货件级别汇总信息
 3. 创建新的货件明细表（t_fba_shipment_item）- 存储SKU级别明细信息

 变更原因:
 - 旧格式：CSV文件，货件级别汇总数据（一行一个货件）
 - 新格式：Excel文件，SKU明细级别数据（一行一个SKU）
 - 需要支持一个货件对应多个SKU的数据结构

 创建时间: 2026-01-22
 版本: v2.2
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 删除旧表
-- ----------------------------
DROP TABLE IF EXISTS `t_fba_shipment_detail`;

-- ----------------------------
-- 2. 创建货件主表
-- ----------------------------
CREATE TABLE `t_fba_shipment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID（数据隔离）',
  `shipment_id` varchar(50) NOT NULL COMMENT '货件单号（FBA货件编号，如：FBA15KYVTSMJ）',
  `warehouse_code` varchar(255) DEFAULT NULL COMMENT '物流中心编码（亚马逊仓库地址）',
  `shop_name` varchar(100) DEFAULT NULL COMMENT '店铺名称（如：慕声欧洲-UK）',
  `country` varchar(50) DEFAULT NULL COMMENT '国家（如：英国）',
  `created_date` datetime DEFAULT NULL COMMENT '货件创建时间',
  `sku_count` int DEFAULT 0 COMMENT 'SKU种类数量（自动计算）',
  `total_quantity` int DEFAULT 0 COMMENT '总发货量（自动汇总）',
  `import_batch_id` bigint DEFAULT NULL COMMENT '导入批次ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shop_shipment` (`shop_id`, `shipment_id`) COMMENT '店铺+货件单号唯一索引',
  KEY `idx_shop_id` (`shop_id`) COMMENT '店铺索引',
  KEY `idx_shipment_id` (`shipment_id`) COMMENT '货件单号索引',
  KEY `idx_created_date` (`created_date`) COMMENT '创建时间索引',
  KEY `idx_import_batch` (`import_batch_id`) COMMENT '导入批次索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='FBA货件主表';

-- ----------------------------
-- 3. 创建货件明细表
-- ----------------------------
CREATE TABLE `t_fba_shipment_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shop_id` bigint NOT NULL COMMENT '店铺ID（数据隔离）',
  `shipment_id` bigint NOT NULL COMMENT '货件主表ID（外键）',
  `shipment_no` varchar(50) NOT NULL COMMENT '货件单号（冗余字段，便于查询）',
  `sku` varchar(100) NOT NULL COMMENT '内部SKU编码',
  `msku` varchar(100) DEFAULT NULL COMMENT '亚马逊MSKU',
  `quantity` int NOT NULL DEFAULT 0 COMMENT '发货量',
  `import_batch_id` bigint DEFAULT NULL COMMENT '导入批次ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shipment_sku` (`shipment_id`, `sku`) COMMENT '货件+SKU唯一索引',
  KEY `idx_shop_id` (`shop_id`) COMMENT '店铺索引',
  KEY `idx_shipment_no` (`shipment_no`) COMMENT '货件单号索引',
  KEY `idx_sku` (`sku`) COMMENT 'SKU索引',
  KEY `idx_msku` (`msku`) COMMENT 'MSKU索引',
  KEY `idx_import_batch` (`import_batch_id`) COMMENT '导入批次索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='FBA货件明细表';

-- ----------------------------
-- 4. 添加外键约束（可选，根据实际需求决定是否启用）
-- ----------------------------
-- ALTER TABLE `t_fba_shipment_item`
--   ADD CONSTRAINT `fk_shipment_item_shipment`
--   FOREIGN KEY (`shipment_id`) REFERENCES `t_fba_shipment` (`id`)
--   ON DELETE CASCADE ON UPDATE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- 变更说明
-- ----------------------------
-- 1. 删除了旧的 t_fba_shipment_detail 表
-- 2. 新增 t_fba_shipment 表用于存储货件级别的汇总信息
-- 3. 新增 t_fba_shipment_item 表用于存储SKU级别的明细信息
-- 4. 支持一个货件对应多个SKU的数据结构
-- 5. 通过 shop_id + shipment_id 保证货件唯一性
-- 6. 通过 shipment_id + sku 保证明细唯一性
