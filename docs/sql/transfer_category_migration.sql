-- ============================================================
-- 历史数据迁移：资金划转记录 transaction_category 更新
-- 目标：将 t_sales_data 表中 transaction_type 为 Transfer / Übertrag
--       且 transaction_category 仍为 'other' 的历史记录更新为 'transfer'
-- 说明：配合映射表更新（transfer_category_mapping_update.sql），
--       确保历史已导入数据也能被报税计算正确识别和排除
-- 幂等性：正向迁移通过 AND transaction_category = 'other' 条件保证
--         重复执行不会产生副作用；回滚脚本通过 AND transaction_category = 'transfer'
--         条件保证重复执行不会影响非 Transfer 类型的记录
-- ============================================================


-- ############################################################
-- 第一部分：正向迁移
-- 将历史 Transfer / Übertrag 记录的 transaction_category 从 'other' 更新为 'transfer'
-- ############################################################

-- ------------------------------------------------------------
-- 1.1 执行前验证：统计待更新记录数量
-- ------------------------------------------------------------

-- 查看 transaction_type = 'Transfer' 且 transaction_category = 'other' 的记录数
SELECT COUNT(*) AS transfer_to_migrate
FROM t_sales_data
WHERE transaction_type = 'Transfer'
  AND transaction_category = 'other';

-- 查看 transaction_type = 'Übertrag' 且 transaction_category = 'other' 的记录数
SELECT COUNT(*) AS uebertrag_to_migrate
FROM t_sales_data
WHERE transaction_type = 'Übertrag'
  AND transaction_category = 'other';

-- 查看待更新记录总数
SELECT COUNT(*) AS total_to_migrate
FROM t_sales_data
WHERE transaction_type IN ('Transfer', 'Übertrag')
  AND transaction_category = 'other';

-- ------------------------------------------------------------
-- 1.2 执行正向迁移
--     将 transaction_type 为 Transfer 或 Übertrag 且 transaction_category 为 'other'
--     的记录更新为 transaction_category = 'transfer'
-- ------------------------------------------------------------
UPDATE t_sales_data
SET transaction_category = 'transfer'
WHERE transaction_type IN ('Transfer', 'Übertrag')
  AND transaction_category = 'other';

-- ------------------------------------------------------------
-- 1.3 执行后验证：确认迁移结果
-- ------------------------------------------------------------

-- 验证：不应再存在 Transfer/Übertrag 且 transaction_category = 'other' 的记录
SELECT COUNT(*) AS remaining_other_count
FROM t_sales_data
WHERE transaction_type IN ('Transfer', 'Übertrag')
  AND transaction_category = 'other';
-- 预期结果：0

-- 验证：查看已迁移为 transfer 的记录总数
SELECT COUNT(*) AS migrated_transfer_count
FROM t_sales_data
WHERE transaction_type IN ('Transfer', 'Übertrag')
  AND transaction_category = 'transfer';


-- ############################################################
-- 第二部分：回滚脚本
-- 如需回滚，将 Transfer / Übertrag 记录的 transaction_category 从 'transfer' 恢复为 'other'
-- 注意：仅在需要回滚时执行以下语句
-- ############################################################

-- ------------------------------------------------------------
-- 2.1 回滚前验证：统计待回滚记录数量
-- ------------------------------------------------------------

-- 查看 transaction_type 为 Transfer/Übertrag 且 transaction_category = 'transfer' 的记录数
-- SELECT COUNT(*) AS total_to_rollback
-- FROM t_sales_data
-- WHERE transaction_type IN ('Transfer', 'Übertrag')
--   AND transaction_category = 'transfer';

-- ------------------------------------------------------------
-- 2.2 执行回滚
--     将 transaction_type 为 Transfer 或 Übertrag 且 transaction_category 为 'transfer'
--     的记录恢复为 transaction_category = 'other'
-- ------------------------------------------------------------
-- UPDATE t_sales_data
-- SET transaction_category = 'other'
-- WHERE transaction_type IN ('Transfer', 'Übertrag')
--   AND transaction_category = 'transfer';

-- ------------------------------------------------------------
-- 2.3 回滚后验证：确认回滚结果
-- ------------------------------------------------------------

-- 验证：不应再存在 Transfer/Übertrag 且 transaction_category = 'transfer' 的记录
-- SELECT COUNT(*) AS remaining_transfer_count
-- FROM t_sales_data
-- WHERE transaction_type IN ('Transfer', 'Übertrag')
--   AND transaction_category = 'transfer';
-- 预期结果：0

-- 验证：查看已恢复为 other 的记录总数
-- SELECT COUNT(*) AS rollback_other_count
-- FROM t_sales_data
-- WHERE transaction_type IN ('Transfer', 'Übertrag')
--   AND transaction_category = 'other';
