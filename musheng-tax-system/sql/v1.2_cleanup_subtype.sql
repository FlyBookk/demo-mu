-- =====================================================
-- 字段统一：清理 sub_type 字段，统一使用 source_type
-- 版本: v1.2
-- 日期: 2026-01-21
-- 说明: 将 sub_type 数据迁移到 source_type，然后删除 sub_type
-- =====================================================

-- 1. 检查并迁移 t_field_mapping_template 表数据
-- 如果 source_type 为空但 sub_type 有值，则从 sub_type 拷贝
UPDATE t_field_mapping_template 
SET source_type = sub_type 
WHERE source_type IS NULL AND sub_type IS NOT NULL;

-- 2. 检查并迁移 t_target_field_metadata 表数据（如果有 source_type 字段）
-- 由于 t_target_field_metadata 表可能没有 source_type 字段，暂时保留 sub_type
-- 该表主要用于目标字段元数据定义，sub_type 作为查询参数名使用

-- 3. 删除 t_field_mapping_template 表的 sub_type 字段（可选）
-- 注意：在生产环境请谨慎执行，确认数据迁移完成后再删除
-- ALTER TABLE t_field_mapping_template DROP COLUMN sub_type;

-- 4. 验证数据
SELECT id, template_name, data_type, source_type, sub_type 
FROM t_field_mapping_template 
WHERE data_type = 'SALES';

-- 5. 如果需要回滚：将 source_type 同步回 sub_type
-- UPDATE t_field_mapping_template SET sub_type = source_type WHERE source_type IS NOT NULL;
