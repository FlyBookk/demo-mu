-- ============================================================
-- 资金划转类型映射表数据更新
-- 目标：将 Transfer / Übertrag 的 standard_category 从 'other' 更新为 'transfer'
-- 说明：使系统能够通过 standard_category 统一识别各站点的资金划转交易类型
-- 幂等性：通过 AND standard_category = 'other' 条件保证重复执行不会产生副作用
-- ============================================================

-- ------------------------------------------------------------
-- 1. 执行前验证：查看当前待更新记录数量
-- ------------------------------------------------------------

-- 查看通用 Transfer 映射当前状态
SELECT COUNT(*) AS transfer_count
FROM t_transaction_type_mapping
WHERE original_type = 'Transfer'
  AND site_code IS NULL
  AND standard_category = 'other';

-- 查看德国站 Übertrag 映射当前状态
SELECT COUNT(*) AS uebertrag_count
FROM t_transaction_type_mapping
WHERE original_type = 'Übertrag'
  AND site_code = 'DE'
  AND standard_category = 'other';

-- ------------------------------------------------------------
-- 2. 更新通用 Transfer 映射
--    将 original_type = 'Transfer' 且 site_code IS NULL 的记录
--    的 standard_category 从 'other' 更新为 'transfer'
-- ------------------------------------------------------------
UPDATE t_transaction_type_mapping
SET standard_category = 'transfer'
WHERE original_type = 'Transfer'
  AND site_code IS NULL
  AND mapped_type = 'Transfer'
  AND standard_category = 'other';

-- ------------------------------------------------------------
-- 3. 更新德国站 Übertrag 映射
--    将 original_type = 'Übertrag' 且 site_code = 'DE' 的记录
--    的 standard_category 从 'other' 更新为 'transfer'
-- ------------------------------------------------------------
UPDATE t_transaction_type_mapping
SET standard_category = 'transfer'
WHERE original_type = 'Übertrag'
  AND site_code = 'DE'
  AND mapped_type = 'Transfer'
  AND standard_category = 'other';

-- ------------------------------------------------------------
-- 4. 执行后验证：确认更新结果
-- ------------------------------------------------------------

-- 验证通用 Transfer 映射已更新为 transfer
SELECT id, site_code, original_type, standard_category, mapped_type
FROM t_transaction_type_mapping
WHERE original_type = 'Transfer'
  AND site_code IS NULL;

-- 验证德国站 Übertrag 映射已更新为 transfer
SELECT id, site_code, original_type, standard_category, mapped_type
FROM t_transaction_type_mapping
WHERE original_type = 'Übertrag'
  AND site_code = 'DE';

-- 验证没有遗漏的 mapped_type = 'Transfer' 且 standard_category 仍为 'other' 的记录
SELECT COUNT(*) AS remaining_other_transfer_count
FROM t_transaction_type_mapping
WHERE mapped_type = 'Transfer'
  AND standard_category = 'other';
