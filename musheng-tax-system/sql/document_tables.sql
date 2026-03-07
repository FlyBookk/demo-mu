-- ============================================================
-- FBA单据自动生成 - 建表脚本
-- 包含9张表：PO主表/明细、DN主表/明细、结算单主表/明细、
--            INV主表/明细、结算导入数据表
-- 数据库：MySQL 8.0+
-- 字符集：utf8mb4
-- 引擎：InnoDB
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. t_document_po — PO采购订单主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_document_po` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`         BIGINT       NOT NULL                COMMENT '店铺ID',
  `document_no`     VARCHAR(20)  NOT NULL                COMMENT '单据编号（如20250902001）',
  `po_date`         DATE         NOT NULL                COMMENT 'PO日期',
  `buyer_name`      VARCHAR(100) NOT NULL DEFAULT ''     COMMENT '买方名称',
  `buyer_address`   VARCHAR(500) NOT NULL DEFAULT ''     COMMENT '买方地址',
  `seller_name`     VARCHAR(100) NOT NULL DEFAULT ''     COMMENT '卖方名称',
  `total_quantity`  INT          NOT NULL DEFAULT 0      COMMENT '总数量',
  `shipment_count`  INT          NOT NULL DEFAULT 0      COMMENT '包含货件数',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`       BIGINT                DEFAULT NULL   COMMENT '创建人',
  `update_by`       BIGINT                DEFAULT NULL   COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_no` (`document_no`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PO采购订单主表';

-- ----------------------------
-- 2. t_document_po_item — PO明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_document_po_item` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`         BIGINT       NOT NULL                COMMENT '店铺ID',
  `po_id`           BIGINT       NOT NULL                COMMENT 'PO主表ID',
  `shipment_no`     VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT 'FBA货件编号',
  `msku`            VARCHAR(100) NOT NULL DEFAULT ''     COMMENT 'MSKU编码',
  `quantity`        INT          NOT NULL DEFAULT 0      COMMENT '数量',
  `fba_address`     VARCHAR(500) NOT NULL DEFAULT ''     COMMENT 'FBA仓库地址（仅货件首行填写）',
  `sort_order`      INT          NOT NULL DEFAULT 0      COMMENT '排序序号',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`       BIGINT                DEFAULT NULL   COMMENT '创建人',
  `update_by`       BIGINT                DEFAULT NULL   COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_po_id` (`po_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PO采购订单明细表';

-- ----------------------------
-- 3. t_document_dn — 送货单主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_document_dn` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`         BIGINT       NOT NULL                COMMENT '店铺ID',
  `document_no`     VARCHAR(20)  NOT NULL                COMMENT '单据编号',
  `dn_date`         DATE         NOT NULL                COMMENT '送货日期',
  `supplier_name`   VARCHAR(100) NOT NULL DEFAULT ''     COMMENT '供应商名称',
  `customer_name`   VARCHAR(100) NOT NULL DEFAULT ''     COMMENT '客户名称（繁体）',
  `total_quantity`  INT          NOT NULL DEFAULT 0      COMMENT '总数量',
  `period_start`    DATE                  DEFAULT NULL   COMMENT 'DN周期起始日',
  `period_end`      DATE                  DEFAULT NULL   COMMENT 'DN周期结束日',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`       BIGINT                DEFAULT NULL   COMMENT '创建人',
  `update_by`       BIGINT                DEFAULT NULL   COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_no` (`document_no`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='送货单主表';

-- ----------------------------
-- 4. t_document_dn_item — 送货单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_document_dn_item` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`         BIGINT       NOT NULL                COMMENT '店铺ID',
  `dn_id`           BIGINT       NOT NULL                COMMENT 'DN主表ID',
  `line_no`         INT          NOT NULL DEFAULT 0      COMMENT '行号（1,2,3...）',
  `msku`            VARCHAR(100) NOT NULL DEFAULT ''     COMMENT 'MSKU编码',
  `quantity`        INT          NOT NULL DEFAULT 0      COMMENT '数量',
  `shipment_no`     VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '对应货件编号（备注列）',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`       BIGINT                DEFAULT NULL   COMMENT '创建人',
  `update_by`       BIGINT                DEFAULT NULL   COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_dn_id` (`dn_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='送货单明细表';

-- ----------------------------
-- 5. t_document_settlement — 结算单主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_document_settlement` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`         BIGINT        NOT NULL                COMMENT '店铺ID',
  `document_no`     VARCHAR(20)   NOT NULL                COMMENT '单据编号',
  `settlement_date` DATE          NOT NULL                COMMENT '结算日',
  `period_start`    DATE          NOT NULL                COMMENT '结算周期起始日',
  `period_end`      DATE          NOT NULL                COMMENT '结算周期结束日',
  `site_code`       VARCHAR(10)   NOT NULL DEFAULT ''     COMMENT '站点代码（USD/CAD/GBP/EUR）',
  `site_sequence`   VARCHAR(3)    NOT NULL DEFAULT ''     COMMENT '站点序号（001-004）',
  `buyer_name`      VARCHAR(100)  NOT NULL DEFAULT ''     COMMENT '买方名称',
  `buyer_address`   VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '买方地址',
  `seller_name`     VARCHAR(100)  NOT NULL DEFAULT ''     COMMENT '卖方名称',
  `total_quantity`  INT           NOT NULL DEFAULT 0      COMMENT '总数量',
  `total_amount`    DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '总金额',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`       BIGINT                 DEFAULT NULL   COMMENT '创建人',
  `update_by`       BIGINT                 DEFAULT NULL   COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_no` (`document_no`),
  KEY `idx_period` (`period_start`, `period_end`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='结算单主表';

-- ----------------------------
-- 6. t_document_settlement_item — 结算单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_document_settlement_item` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`         BIGINT        NOT NULL                COMMENT '店铺ID',
  `settlement_id`   BIGINT        NOT NULL                COMMENT '结算单主表ID',
  `line_no`         INT           NOT NULL DEFAULT 0      COMMENT '序号',
  `msku`            VARCHAR(100)  NOT NULL DEFAULT ''     COMMENT 'MSKU编码',
  `currency`        VARCHAR(10)   NOT NULL DEFAULT ''     COMMENT '货币代码',
  `unit_price`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '单价',
  `quantity`        INT           NOT NULL DEFAULT 0      COMMENT '销售数量',
  `amount`          DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '金额（数量×单价）',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`       BIGINT                 DEFAULT NULL   COMMENT '创建人',
  `update_by`       BIGINT                 DEFAULT NULL   COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_settlement_id` (`settlement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='结算单明细表';

-- ----------------------------
-- 7. t_document_inv — INV发票主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_document_inv` (
  `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`             BIGINT        NOT NULL                COMMENT '店铺ID',
  `document_no`         VARCHAR(20)   NOT NULL                COMMENT 'INV编号',
  `inv_date`            DATE          NOT NULL                COMMENT 'INV日期（结算日+1工作日）',
  `settlement_id`       BIGINT        NOT NULL                COMMENT '关联结算单ID',
  `site_code`           VARCHAR(10)   NOT NULL DEFAULT ''     COMMENT '站点代码',
  `site_sequence`       VARCHAR(3)    NOT NULL DEFAULT ''     COMMENT '站点序号（001-004）',
  `seller_name`         VARCHAR(100)  NOT NULL DEFAULT ''     COMMENT '卖方名称',
  `seller_address`      VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '卖方地址',
  `buyer_name`          VARCHAR(100)  NOT NULL DEFAULT ''     COMMENT '买方英文名称',
  `buyer_address`       VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '买方英文地址',
  `seller_phone`        VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '卖方电话',
  `buyer_phone`         VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '买方电话',
  `total_quantity`      INT           NOT NULL DEFAULT 0      COMMENT '总数量',
  `total_amount`        DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '总金额',
  `bank_account_name`   VARCHAR(200)  NOT NULL DEFAULT ''     COMMENT '银行账户名',
  `bank_account_number` VARCHAR(50)   NOT NULL DEFAULT ''     COMMENT '银行账号',
  `bank_name`           VARCHAR(200)  NOT NULL DEFAULT ''     COMMENT '银行名称',
  `bank_address`        VARCHAR(500)  NOT NULL DEFAULT ''     COMMENT '银行地址',
  `swift_code`          VARCHAR(20)   NOT NULL DEFAULT ''     COMMENT 'SWIFT代码',
  `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`           BIGINT                 DEFAULT NULL   COMMENT '创建人',
  `update_by`           BIGINT                 DEFAULT NULL   COMMENT '更新人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_no` (`document_no`),
  KEY `idx_settlement_id` (`settlement_id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='INV发票主表';

-- ----------------------------
-- 8. t_document_inv_item — INV明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_document_inv_item` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`         BIGINT        NOT NULL                COMMENT '店铺ID',
  `inv_id`          BIGINT        NOT NULL                COMMENT 'INV主表ID',
  `line_no`         INT           NOT NULL DEFAULT 0      COMMENT '序号',
  `msku`            VARCHAR(100)  NOT NULL DEFAULT ''     COMMENT 'MSKU编码',
  `quantity`        INT           NOT NULL DEFAULT 0      COMMENT '数量',
  `unit_price`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '单价',
  `amount`          DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '金额',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`       BIGINT                 DEFAULT NULL   COMMENT '创建人',
  `update_by`       BIGINT                 DEFAULT NULL   COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_inv_id` (`inv_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='INV发票明细表';

-- ----------------------------
-- 9. t_settlement_import_data — 结算导入数据表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `t_settlement_import_data` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`         BIGINT        NOT NULL                COMMENT '店铺ID',
  `import_batch_id` BIGINT        NOT NULL                COMMENT '导入批次ID',
  `period_start`    DATE          NOT NULL                COMMENT '结算周期起始日',
  `period_end`      DATE          NOT NULL                COMMENT '结算周期结束日',
  `site_code`       VARCHAR(10)   NOT NULL DEFAULT ''     COMMENT '站点代码',
  `msku`            VARCHAR(100)  NOT NULL DEFAULT ''     COMMENT 'MSKU编码',
  `currency`        VARCHAR(10)   NOT NULL DEFAULT ''     COMMENT '货币代码',
  `unit_price`      DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '单价',
  `quantity`        INT           NOT NULL DEFAULT 0      COMMENT '销售数量',
  `amount`          DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '金额',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by`       BIGINT                 DEFAULT NULL   COMMENT '创建人',
  `update_by`       BIGINT                 DEFAULT NULL   COMMENT '更新人',
  PRIMARY KEY (`id`),
  KEY `idx_period_site` (`period_start`, `period_end`, `site_code`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_import_batch_id` (`import_batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='结算导入数据表';

SET FOREIGN_KEY_CHECKS = 1;
