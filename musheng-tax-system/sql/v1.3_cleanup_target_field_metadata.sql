-- =====================================================
-- 清理目标字段元数据表
-- 版本: v1.3
-- 说明: 目标字段配置已改为从实体类 @FieldMapping 注解动态生成，
--       不再需要数据库表存储，此脚本用于清理废弃的表。
-- 日期: 2026-01-22
-- =====================================================

-- 备份表（可选，如需保留历史数据）
-- CREATE TABLE t_target_field_metadata_backup AS SELECT * FROM t_target_field_metadata;

-- 删除目标字段元数据表
DROP TABLE IF EXISTS t_target_field_metadata;

-- 确认删除
-- SELECT 'Table t_target_field_metadata has been dropped' AS result;
