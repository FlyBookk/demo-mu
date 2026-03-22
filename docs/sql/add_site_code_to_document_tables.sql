-- 为 t_document_po 和 t_document_dn 添加 site_code 字段
-- 执行时间：2026年03月22日

ALTER TABLE t_document_po
    ADD COLUMN site_code VARCHAR(10) NULL COMMENT '站点代码（US/CA/UK/EU）' AFTER document_no;

ALTER TABLE t_document_dn
    ADD COLUMN site_code VARCHAR(10) NULL COMMENT '站点代码（US/CA/UK/EU）' AFTER document_no;
