-- ============================================================
-- 结算数据推导功能 — t_settlement_import_data 表结构变更
-- 功能说明：为结算导入数据表新增推导批次管理和逻辑删除支持字段
-- 关联需求：9.1, 9.2, 9.3, 9.4, 9.5
-- ============================================================

-- ----------------------------
-- 1. 新增 settlement_batch_id 字段
-- 用途：推导批次标识（UUID），用于关联和追溯同一次推导生成的所有记录
-- 需求：9.1
-- ----------------------------
ALTER TABLE `t_settlement_import_data`
    ADD COLUMN `settlement_batch_id` VARCHAR(64) DEFAULT NULL COMMENT '推导批次标识（UUID）';

-- ----------------------------
-- 2. 新增 del_flag 字段
-- 用途：逻辑删除标记，0=正常，1=已删除；覆盖写入时旧数据标记为1
-- 需求：9.2
-- ----------------------------
ALTER TABLE `t_settlement_import_data`
    ADD COLUMN `del_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记（0=正常，1=已删除）';

-- ----------------------------
-- 3. 新增 procurement_cost_cny 字段
-- 用途：记录该批次对应站点的采购成本（人民币），由财务人员输入
-- 需求：9.3
-- ----------------------------
ALTER TABLE `t_settlement_import_data`
    ADD COLUMN `procurement_cost_cny` DECIMAL(18,2) DEFAULT NULL COMMENT '采购成本（人民币）';

-- ----------------------------
-- 4. 新增 average_exchange_rate 字段
-- 用途：记录该批次使用的周期平均汇率，用于人民币到原币的换算
-- 需求：9.4
-- ----------------------------
ALTER TABLE `t_settlement_import_data`
    ADD COLUMN `average_exchange_rate` DECIMAL(18,6) DEFAULT NULL COMMENT '周期平均汇率';

-- ----------------------------
-- 5. 新增索引
-- 用途：优化按批次标识和逻辑删除标记的查询性能
-- 需求：9.5
-- ----------------------------
ALTER TABLE `t_settlement_import_data`
    ADD INDEX `idx_settlement_batch_id` (`settlement_batch_id`);

ALTER TABLE `t_settlement_import_data`
    ADD INDEX `idx_del_flag` (`del_flag`);
