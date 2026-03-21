-- ============================================================
-- 清空销售数据以便用新去重逻辑重新导入
-- 
-- 背景：buildUnifiedUniqueKey 方法已升级，旧逻辑生成的去重键
--       与新逻辑不兼容，需清空旧数据后重新导入。
--
-- 影响范围：仅清空 t_sales_data 表（亚马逊原始销售数据）
-- 不影响：配送数据、FBA数据、广告数据、结算数据等其他表
--
-- 执行前请确认：
--   1. 已备份数据库（或确认数据可从原始CSV重新导入）
--   2. 当前无正在进行的导入任务
--
-- @author wanhua
-- 2026年03月21日
-- ============================================================

-- 查看当前数据量（执行前确认）
SELECT 
    shop_id,
    site_code,
    source_type,
    COUNT(*) AS row_count,
    MIN(transaction_date) AS earliest_date,
    MAX(transaction_date) AS latest_date
FROM t_sales_data
GROUP BY shop_id, site_code, source_type
ORDER BY shop_id, site_code;

-- ============================================================
-- 执行清空（确认上方查询结果后再执行）
-- ============================================================

-- 方案A：清空全部销售数据（推荐，最干净）
TRUNCATE TABLE t_sales_data;

-- 方案B：仅清空指定店铺的数据（如只需重导部分数据）
-- DELETE FROM t_sales_data WHERE shop_id = ?;

-- 方案C：仅清空指定站点的数据
-- DELETE FROM t_sales_data WHERE shop_id = ? AND site_code IN ('US', 'UK');

-- 重置自增ID（TRUNCATE已自动重置，DELETE需手动执行）
-- ALTER TABLE t_sales_data AUTO_INCREMENT = 1;

-- 验证清空结果
SELECT COUNT(*) AS remaining_rows FROM t_sales_data;
