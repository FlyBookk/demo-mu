/*
 FBA货件明细表 - 建表SQL

 功能说明: 管理亚马逊FBA入库货件信息，追踪货件从创建到入库完成的全过程
 创建时间: 2026-01-21
 数据来源: 慕声加拿大2025年3-12月FBA货件明细表.csv
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 表结构: t_fba_shipment_detail (FBA货件明细表)
-- ----------------------------
DROP TABLE IF EXISTS `t_fba_shipment_detail`;
CREATE TABLE `t_fba_shipment_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `shipment_name` varchar(100) NOT NULL COMMENT '货件名称(货件批次名称)',
  `shipment_id` varchar(50) NOT NULL COMMENT '货件编号(FBA货件ID,唯一)',
  `created_date` datetime DEFAULT NULL COMMENT '货件创建时间(来自CSV)',
  `last_updated` datetime DEFAULT NULL COMMENT '货件最后更新时间(来自CSV)',
  `receiving_address` varchar(50) DEFAULT NULL COMMENT '收货地址(亚马逊仓库代码,如YEG2)',
  `sku_count` int DEFAULT NULL COMMENT 'SKU种类数量',
  `expected_quantity` int DEFAULT NULL COMMENT '预计商品数量(计划发货数量)',
  `found_quantity` int DEFAULT NULL COMMENT '找到的商品数量(实际入库数量)',
  `status` varchar(20) DEFAULT NULL COMMENT '货件状态(已完成/处理中等)',
  `import_batch_id` bigint DEFAULT NULL COMMENT '导入批次ID(关联导入记录表)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间(自动填充)',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间(自动填充)',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID(自动填充)',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID(自动填充)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shipment_id` (`shipment_id`) COMMENT '货件编号唯一索引',
  KEY `idx_status` (`status`) COMMENT '状态索引',
  KEY `idx_receiving_address` (`receiving_address`) COMMENT '收货地址索引',
  KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引',
  KEY `idx_import_batch` (`import_batch_id`) COMMENT '导入批次索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='FBA货件明细表';

-- ----------------------------
-- 数据记录: t_fba_shipment_detail
-- ----------------------------
BEGIN;
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
