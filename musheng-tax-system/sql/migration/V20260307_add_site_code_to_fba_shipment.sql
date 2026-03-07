-- ============================================================
-- 迁移脚本：t_fba_shipment 新增 site_code 字段
-- 描述：为 FBA 货件主表新增站点代码字段，支持按站点维度过滤数据
-- 日期：2026-03-07
-- ============================================================

-- 新增站点代码字段（允许为空，兼容历史数据）
ALTER TABLE t_fba_shipment
    ADD COLUMN site_code VARCHAR(10) NULL COMMENT '站点代码（如 US/CA/UK/DE）'
    AFTER country;

-- 为站点代码字段创建索引，加速按站点查询
CREATE INDEX idx_fba_shipment_site_code ON t_fba_shipment (site_code);
