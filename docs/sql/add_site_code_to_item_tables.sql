-- ============================================================
-- 数据清理 site_code 字段补全
-- 目标：为所有缺少 site_code 的子表/主表补充字段，并从主表同步数据
-- 执行顺序：先 ALTER TABLE，再 UPDATE 同步，最后加索引
-- ============================================================

-- ------------------------------------------------------------
-- 1. t_advertising_bill_item（广告明细）
--    关联主表：t_advertising_bill.site_code
-- ------------------------------------------------------------
ALTER TABLE `t_advertising_bill_item`
    ADD COLUMN `site_code` varchar(10) DEFAULT NULL COMMENT '站点编码（从主表冗余）' AFTER `bill_id`;

UPDATE `t_advertising_bill_item` i
    JOIN `t_advertising_bill` b ON i.bill_id = b.id
SET i.site_code = b.site_code;

ALTER TABLE `t_advertising_bill_item`
    ADD INDEX `idx_site_code` (`site_code`);

-- ------------------------------------------------------------
-- 2. t_fba_shipment_item（FBA货件明细）
--    关联主表：t_fba_shipment.site_code
-- ------------------------------------------------------------
ALTER TABLE `t_fba_shipment_item`
    ADD COLUMN `site_code` varchar(10) DEFAULT NULL COMMENT '站点代码（从主表冗余）' AFTER `shipment_id`;

UPDATE `t_fba_shipment_item` i
    JOIN `t_fba_shipment` s ON i.shipment_id = s.id
SET i.site_code = s.site_code;

ALTER TABLE `t_fba_shipment_item`
    ADD INDEX `idx_site_code` (`site_code`);

-- ------------------------------------------------------------
-- 3. t_document_po（PO主表，本身没有 site_code）
--    PO 不直接关联站点，补空字段供后续业务填充
-- ------------------------------------------------------------
ALTER TABLE `t_document_po`
    ADD COLUMN `site_code` varchar(10) NOT NULL DEFAULT '' COMMENT '站点代码' AFTER `shop_id`;

ALTER TABLE `t_document_po`
    ADD INDEX `idx_site_code` (`site_code`);

-- ------------------------------------------------------------
-- 4. t_document_po_item（PO明细）
--    关联主表：t_document_po.site_code
-- ------------------------------------------------------------
ALTER TABLE `t_document_po_item`
    ADD COLUMN `site_code` varchar(10) NOT NULL DEFAULT '' COMMENT '站点代码（从主表冗余）' AFTER `po_id`;

UPDATE `t_document_po_item` i
    JOIN `t_document_po` p ON i.po_id = p.id
SET i.site_code = p.site_code;

ALTER TABLE `t_document_po_item`
    ADD INDEX `idx_site_code` (`site_code`);

-- ------------------------------------------------------------
-- 5. t_document_dn（DN主表，本身没有 site_code）
--    DN 不直接关联站点，补空字段供后续业务填充
-- ------------------------------------------------------------
ALTER TABLE `t_document_dn`
    ADD COLUMN `site_code` varchar(10) NOT NULL DEFAULT '' COMMENT '站点代码' AFTER `shop_id`;

ALTER TABLE `t_document_dn`
    ADD INDEX `idx_site_code` (`site_code`);

-- ------------------------------------------------------------
-- 6. t_document_dn_item（DN明细）
--    关联主表：t_document_dn.site_code
-- ------------------------------------------------------------
ALTER TABLE `t_document_dn_item`
    ADD COLUMN `site_code` varchar(10) NOT NULL DEFAULT '' COMMENT '站点代码（从主表冗余）' AFTER `dn_id`;

UPDATE `t_document_dn_item` i
    JOIN `t_document_dn` d ON i.dn_id = d.id
SET i.site_code = d.site_code;

ALTER TABLE `t_document_dn_item`
    ADD INDEX `idx_site_code` (`site_code`);

-- ------------------------------------------------------------
-- 7. t_document_settlement_item（结算单明细）
--    关联主表：t_document_settlement.site_code
-- ------------------------------------------------------------
ALTER TABLE `t_document_settlement_item`
    ADD COLUMN `site_code` varchar(10) NOT NULL DEFAULT '' COMMENT '站点代码（从主表冗余）' AFTER `settlement_id`;

UPDATE `t_document_settlement_item` i
    JOIN `t_document_settlement` s ON i.settlement_id = s.id
SET i.site_code = s.site_code;

ALTER TABLE `t_document_settlement_item`
    ADD INDEX `idx_site_code` (`site_code`);

-- ------------------------------------------------------------
-- 8. t_document_inv_item（INV明细）
--    关联主表：t_document_inv.site_code
-- ------------------------------------------------------------
ALTER TABLE `t_document_inv_item`
    ADD COLUMN `site_code` varchar(10) NOT NULL DEFAULT '' COMMENT '站点代码（从主表冗余）' AFTER `inv_id`;

UPDATE `t_document_inv_item` i
    JOIN `t_document_inv` v ON i.inv_id = v.id
SET i.site_code = v.site_code;

ALTER TABLE `t_document_inv_item`
    ADD INDEX `idx_site_code` (`site_code`);
