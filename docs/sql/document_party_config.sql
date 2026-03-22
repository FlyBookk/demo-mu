-- FBA单据交易方配置表
-- 创建时间：2025年
-- 说明：存储各站点（US/CA/UK/EU）的买方、卖方、供应商等交易方信息，替代硬编码常量

CREATE TABLE IF NOT EXISTS t_document_party_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    site_code       VARCHAR(20)     NOT NULL                COMMENT '站点代码（US/CA/UK/EU），唯一',
    buyer_name      VARCHAR(100)    NOT NULL                COMMENT '买方中文名',
    buyer_address   VARCHAR(255)    DEFAULT NULL            COMMENT '买方地址',
    buyer_phone     VARCHAR(50)     DEFAULT NULL            COMMENT '买方电话',
    buyer_name_en   VARCHAR(100)    DEFAULT NULL            COMMENT '买方英文名',
    seller_name     VARCHAR(100)    NOT NULL                COMMENT '卖方名称',
    seller_address  VARCHAR(255)    DEFAULT NULL            COMMENT '卖方地址',
    seller_phone    VARCHAR(50)     DEFAULT NULL            COMMENT '卖方电话',
    supplier_name   VARCHAR(100)    NOT NULL                COMMENT '供应商名称',
    customer_name_tc VARCHAR(100)   NOT NULL                COMMENT '客户繁体名',
    bank_account_name   VARCHAR(100) DEFAULT NULL           COMMENT '银行账户名',
    bank_account_number VARCHAR(50)  DEFAULT NULL           COMMENT '银行账号',
    bank_name       VARCHAR(100)    DEFAULT NULL            COMMENT '银行名称',
    bank_address    VARCHAR(255)    DEFAULT NULL            COMMENT '银行地址',
    swift_code      VARCHAR(20)     DEFAULT NULL            COMMENT 'SWIFT代码',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT(1)      DEFAULT 0               COMMENT '逻辑删除（0=未删除，1=已删除）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_site_code (site_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FBA单据交易方配置';

-- 初始化各站点交易方配置数据（迁移自原硬编码常量）
INSERT INTO t_document_party_config (site_code, buyer_name, buyer_address, buyer_name_en, seller_name, supplier_name, customer_name_tc)
VALUES
('US', '东莞市慕声商贸有限公司', '广东省东莞市虎门镇连升路82号虎门万达广场2栋606房', 'Dongguan Musheng Trade Co., Ltd.', 'Hong Kong Andeo Group Limited', 'Hong Kong Andeo Group Limited', '東莞市慕聲商貿有限公司'),
('CA', '东莞市慕声商贸有限公司', '广东省东莞市虎门镇连升路82号虎门万达广场2栋606房', 'Dongguan Musheng Trade Co., Ltd.', 'Hong Kong Andeo Group Limited', 'Hong Kong Andeo Group Limited', '東莞市慕聲商貿有限公司'),
('UK', '东莞市慕声商贸有限公司', '广东省东莞市虎门镇连升路82号虎门万达广场2栋606房', 'Dongguan Musheng Trade Co., Ltd.', 'Hong Kong Andeo Group Limited', 'Hong Kong Andeo Group Limited', '東莞市慕聲商貿有限公司'),
('EU', '东莞市慕声商贸有限公司', '广东省东莞市虎门镇连升路82号虎门万达广场2栋606房', 'Dongguan Musheng Trade Co., Ltd.', 'Hong Kong Andeo Group Limited', 'Hong Kong Andeo Group Limited', '東莞市慕聲商貿有限公司');
