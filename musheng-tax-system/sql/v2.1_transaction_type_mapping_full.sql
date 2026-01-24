-- ====================================================
-- 销售数据交易类型映射表 完整版
-- 创建时间：2026-01-24
-- 说明：包含ERP交易类型和亚马逊原始数据type的映射
-- ====================================================

-- ====================================================
-- 方案一：使用现有表结构（推荐）
-- 表结构：site_code, original_type, standard_category, category_desc, mapped_type
-- ====================================================

-- 清空旧数据（谨慎操作！建议先备份）
-- TRUNCATE TABLE t_transaction_type_mapping;

-- ====================================================
-- 1. 亚马逊原始数据 type 映射（英语站点：CA/US/UK）
-- 用于：原始数据导入时的type到标准分类映射
-- ====================================================

-- 1.1 通用映射（所有站点适用）
INSERT INTO t_transaction_type_mapping (site_code, original_type, standard_category, category_desc, mapped_type, status, create_time, create_by)
VALUES 
    (NULL, 'Order', 'income', '订单', 'Order', 1, NOW(), 1),
    (NULL, 'Refund', 'refund', '退款', 'Refund', 1, NOW(), 1),
    (NULL, 'FBA Customer Return Fee', 'fee', 'FBA客户退货费', 'ServiceFee', 1, NOW(), 1),
    (NULL, 'Fulfilment by Amazon customer return fee', 'fee', 'FBA客户退货费(UK)', 'ServiceFee', 1, NOW(), 1),
    (NULL, 'Adjustment', 'adjustment', '调整', 'Adjustment', 1, NOW(), 1),
    (NULL, 'Liquidations', 'other', '清仓', 'Liquidations', 1, NOW(), 1),
    (NULL, 'FBA Inventory Fee', 'fee', 'FBA库存费', 'FBAInventoryFee', 1, NOW(), 1),
    (NULL, 'Order_Retrocharge', 'income', '订单追溯收费', 'Retrocharge', 1, NOW(), 1),
    (NULL, 'Refund_Retrocharge', 'refund', '退款追溯', 'RefundRetrocharge', 1, NOW(), 1),
    (NULL, 'Service Fee', 'fee', '服务费', 'ServiceFee', 1, NOW(), 1),
    (NULL, 'Transfer', 'other', '转账', 'Transfer', 1, NOW(), 1),
    (NULL, 'Amazon Fees', 'fee', '亚马逊费用', 'AmazonFees', 1, NOW(), 1),
    (NULL, 'Liquidations Adjustments', 'adjustment', '清仓调整', 'LiquidationsAdjustments', 1, NOW(), 1),
    (NULL, 'Chargeback Refund', 'adjustment', '退款追回', 'ChargebackRefund', 1, NOW(), 1),
    (NULL, 'Debt', 'other', '债务', 'Debt', 1, NOW(), 1)
ON DUPLICATE KEY UPDATE 
    standard_category = VALUES(standard_category),
    category_desc = VALUES(category_desc),
    mapped_type = VALUES(mapped_type);

-- ====================================================
-- 2. 德国站(DE)专用映射（德语type）
-- ====================================================

INSERT INTO t_transaction_type_mapping (site_code, original_type, standard_category, category_desc, mapped_type, status, create_time, create_by)
VALUES 
    ('DE', 'Bestellung', 'income', '订单(德语)', 'Order', 1, NOW(), 1),
    ('DE', 'Erstattung', 'refund', '退款(德语)', 'Refund', 1, NOW(), 1),
    ('DE', 'Gebühren für Kundenrücksendungen mit Versand durch Amazon', 'fee', 'FBA客户退货费(德语)', 'ServiceFee', 1, NOW(), 1),
    ('DE', 'Anpassung', 'adjustment', '调整(德语)', 'Adjustment', 1, NOW(), 1),
    ('DE', 'Liquidationen', 'other', '清仓(德语)', 'Liquidations', 1, NOW(), 1),
    ('DE', 'Versand durch Amazon Lagergebühr', 'fee', 'FBA库存费(德语)', 'FBAInventoryFee', 1, NOW(), 1),
    ('DE', 'Bestellung_Wiedereinzug', 'income', '订单追溯收费(德语)', 'Retrocharge', 1, NOW(), 1),
    ('DE', 'Erstattung_Wiedereinzug', 'refund', '退款追溯(德语)', 'RefundRetrocharge', 1, NOW(), 1),
    ('DE', 'Servicegebühr', 'fee', '服务费(德语)', 'ServiceFee', 1, NOW(), 1),
    ('DE', 'Übertrag', 'other', '转账(德语)', 'Transfer', 1, NOW(), 1),
    ('DE', 'Gebühren von Amazon', 'fee', '亚马逊费用(德语)', 'AmazonFees', 1, NOW(), 1),
    ('DE', 'Liquidationsanpassungen', 'adjustment', '清仓调整(德语)', 'LiquidationsAdjustments', 1, NOW(), 1),
    ('DE', 'Verbindlichkeit', 'other', '债务(德语)', 'Debt', 1, NOW(), 1)
ON DUPLICATE KEY UPDATE 
    standard_category = VALUES(standard_category),
    category_desc = VALUES(category_desc),
    mapped_type = VALUES(mapped_type);

-- ====================================================
-- 3. ERP来源类型(来源列)映射
-- 用于：ERP数据导入时的来源到标准分类映射
-- site_code 设为 'ERP' 表示这是ERP数据专用映射
-- ====================================================

INSERT INTO t_transaction_type_mapping (site_code, original_type, standard_category, category_desc, mapped_type, status, create_time, create_by)
VALUES 
    ('ERP', 'Shipment', 'income', '发货', 'Shipment', 1, NOW(), 1),
    ('ERP', 'Refund', 'refund', '退款', 'Refund', 1, NOW(), 1),
    ('ERP', 'ServiceFee', 'fee', '服务费', 'ServiceFee', 1, NOW(), 1),
    ('ERP', 'RemovalShipment', 'fee', 'FBA移除发货', 'RemovalShipment', 1, NOW(), 1),
    ('ERP', 'Adjustment', 'adjustment', '调整', 'Adjustment', 1, NOW(), 1),
    ('ERP', 'Retrocharge', 'income', '追溯收费', 'Retrocharge', 1, NOW(), 1),
    ('ERP', 'CouponPayment', 'fee', '优惠券支付', 'CouponPayment', 1, NOW(), 1),
    ('ERP', 'RemovalShipmentAdjustment', 'adjustment', '移除发货调整', 'RemovalShipmentAdjustment', 1, NOW(), 1),
    ('ERP', 'SellerDealPayment', 'fee', '秒杀支付', 'SellerDealPayment', 1, NOW(), 1),
    ('ERP', 'Liquidations Adjustments', 'adjustment', '清仓调整', 'LiquidationsAdjustments', 1, NOW(), 1),
    ('ERP', 'Chargeback', 'adjustment', '退款追回', 'Chargeback', 1, NOW(), 1),
    ('ERP', 'ProductAdsPayment', 'fee', '产品广告支付', 'ProductAdsPayment', 1, NOW(), 1)
ON DUPLICATE KEY UPDATE 
    standard_category = VALUES(standard_category),
    category_desc = VALUES(category_desc),
    mapped_type = VALUES(mapped_type);

-- ====================================================
-- 统计
-- ====================================================

-- 查看各站点的映射数量
SELECT COALESCE(site_code, '通用') as site, COUNT(*) as count 
FROM t_transaction_type_mapping 
GROUP BY site_code 
ORDER BY count DESC;

-- 查看所有映射
SELECT site_code, original_type, standard_category, category_desc, mapped_type, status
FROM t_transaction_type_mapping 
ORDER BY site_code, original_type;


-- ====================================================
-- ====================================================
-- 方案二：ERP交易类型→费用字段映射表（新表）
-- 说明：如果需要独立存储ERP交易类型到费用字段的映射，
--       可以创建这个新表
-- ====================================================
-- ====================================================

-- 如需创建新表，取消下面的注释

/*
DROP TABLE IF EXISTS t_erp_fee_type_mapping;

CREATE TABLE t_erp_fee_type_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    erp_fee_type VARCHAR(100) NOT NULL COMMENT 'ERP交易类型(费用明细)',
    target_field VARCHAR(50) NOT NULL COMMENT '目标费用字段',
    description VARCHAR(100) COMMENT '说明',
    category VARCHAR(20) COMMENT '分类(income/fee/refund/adjustment/other)',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态(1启用/0禁用)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    UNIQUE KEY uk_erp_fee_type (erp_fee_type),
    INDEX idx_target_field (target_field)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP费用类型到数据库字段映射表';

-- 产品销售相关 (product_sales / product_sales_tax)
INSERT INTO t_erp_fee_type_mapping (erp_fee_type, target_field, description, category, sort_order) VALUES 
    ('Principal', 'product_sales', '产品销售额', 'income', 1),
    ('Tax', 'product_sales_tax', '产品税', 'income', 2),
    ('TaxAmount', 'product_sales_tax', '税额', 'income', 3),
    ('TaxAmountAdjustment', 'product_sales_tax', '税额调整', 'income', 4),
    ('BaseTax', 'product_sales_tax', '基础税', 'income', 5);

-- 运费相关 (shipping_credits / shipping_credits_tax)
INSERT INTO t_erp_fee_type_mapping (erp_fee_type, target_field, description, category, sort_order) VALUES 
    ('ShippingCharge', 'shipping_credits', '运费收入', 'income', 10),
    ('ShippingDiscount', 'shipping_credits', '运费折扣', 'income', 11),
    ('ShippingChargeback', 'shipping_credits', '运费退款', 'income', 12),
    ('ShippingTax', 'shipping_credits_tax', '运费税', 'income', 13),
    ('ShippingDiscountTax', 'shipping_credits_tax', '运费折扣税', 'income', 14);

-- 礼品包装相关 (gift_wrap_credits / gift_wrap_credits_tax)
INSERT INTO t_erp_fee_type_mapping (erp_fee_type, target_field, description, category, sort_order) VALUES 
    ('GiftWrap', 'gift_wrap_credits', '礼品包装费', 'income', 20),
    ('GiftwrapChargeback', 'gift_wrap_credits', '礼品包装退款', 'income', 21),
    ('GiftWrapTax', 'gift_wrap_credits_tax', '礼品包装税', 'income', 22);

-- 促销折扣相关 (promotional_rebates / promotional_rebates_tax)
INSERT INTO t_erp_fee_type_mapping (erp_fee_type, target_field, description, category, sort_order) VALUES 
    ('PromotionDiscount', 'promotional_rebates', '促销折扣', 'income', 30),
    ('PromotionDiscountTax', 'promotional_rebates_tax', '促销折扣税', 'income', 31);

-- 平台代扣税相关 (marketplace_withheld_tax)
INSERT INTO t_erp_fee_type_mapping (erp_fee_type, target_field, description, category, sort_order) VALUES 
    ('MarketplaceFacilitatorVAT-Principal', 'marketplace_withheld_tax', '平台代扣税(商品VAT)', 'fee', 40),
    ('MarketplaceFacilitatorTax-Principal', 'marketplace_withheld_tax', '平台代扣税(商品)', 'fee', 41),
    ('MarketplaceFacilitatorVAT-Shipping', 'marketplace_withheld_tax', '平台代扣税(运费VAT)', 'fee', 42),
    ('MarketplaceFacilitatorTax-Shipping', 'marketplace_withheld_tax', '平台代扣税(运费)', 'fee', 43),
    ('MarketplaceFacilitatorTax-Other', 'marketplace_withheld_tax', '平台代扣税(其他)', 'fee', 44),
    ('TaxWithheld', 'marketplace_withheld_tax', '代扣税', 'fee', 45),
    ('TaxWithheldAdjustment', 'marketplace_withheld_tax', '代扣税调整', 'fee', 46);

-- 销售佣金相关 (selling_fees)
INSERT INTO t_erp_fee_type_mapping (erp_fee_type, target_field, description, category, sort_order) VALUES 
    ('Commission', 'selling_fees', '销售佣金', 'fee', 50),
    ('RefundCommission', 'selling_fees', '退款佣金', 'fee', 51);

-- FBA费用相关 (fba_fees)
INSERT INTO t_erp_fee_type_mapping (erp_fee_type, target_field, description, category, sort_order) VALUES 
    ('FBAPerUnitFulfillmentFee', 'fba_fees', 'FBA单位配送费', 'fee', 60),
    ('FBAWeightBasedFee', 'fba_fees', 'FBA重量费', 'fee', 61),
    ('FBACustomerReturnPerUnitFee', 'fba_fees', 'FBA退货费', 'fee', 62),
    ('FBAStorageFee', 'fba_fees', 'FBA仓储费', 'fee', 63),
    ('FBALongTermStorageFee', 'fba_fees', 'FBA长期仓储费', 'fee', 64),
    ('FBADisposalFee', 'fba_fees', 'FBA处置费', 'fee', 65);

-- 其他交易费相关 (other_transaction_fees)
INSERT INTO t_erp_fee_type_mapping (erp_fee_type, target_field, description, category, sort_order) VALUES 
    ('DigitalServicesFee', 'other_transaction_fees', '数字服务费', 'fee', 70),
    ('DigitalServicesFeeFBA', 'other_transaction_fees', 'FBA数字服务费', 'fee', 71),
    ('CouponRedemptionFee', 'other_transaction_fees', '优惠券兑换费', 'fee', 72),
    ('CouponParticipationFee', 'other_transaction_fees', '优惠券参与费', 'fee', 73),
    ('CouponPerformanceFee', 'other_transaction_fees', '优惠券使用费', 'fee', 74),
    ('DealParticipationFee', 'other_transaction_fees', '秒杀报名费', 'fee', 75),
    ('DealPerformanceFee', 'other_transaction_fees', '秒杀成交费', 'fee', 76),
    ('Subscription', 'other_transaction_fees', '专业卖家月费', 'fee', 77),
    ('VineFee', 'other_transaction_fees', 'Vine计划费', 'fee', 78);

-- 其他金额相关 (other)
INSERT INTO t_erp_fee_type_mapping (erp_fee_type, target_field, description, category, sort_order) VALUES 
    ('Revenue', 'other', '收入', 'other', 80),
    ('RevenueAdjustment', 'other', '收入调整', 'adjustment', 81),
    ('FeeAmount', 'other', '费用金额', 'fee', 82),
    ('feeAmount', 'other', '费用金额(小写)', 'fee', 83),
    ('baseValue', 'other', '基础价值', 'other', 84),
    ('REVERSAL_REIMBURSEMENT', 'other', '赔偿', 'adjustment', 85),
    ('COMPENSATED_CLAWBACK', 'other', '追回赔偿', 'adjustment', 86),
    ('WAREHOUSE_LOST', 'other', '仓库丢失', 'adjustment', 87),
    ('WAREHOUSE_DAMAGE', 'other', '仓库损坏', 'adjustment', 88),
    ('REMOVAL_ORDER_LOST', 'other', '移除订单丢失', 'adjustment', 89),
    ('FREE_REPLACEMENT_REFUND_ITEMS', 'other', '免费替换退款', 'refund', 90),
    ('INCORRECT_FEES_NON_ITEMIZED', 'other', '费用错误调整', 'adjustment', 91),
    ('RestockingFee', 'other', '补货费', 'fee', 92),
    ('Goodwill', 'other', '商誉补偿', 'adjustment', 93),
    ('ReserveDebit', 'other', '保留资金冻结', 'other', 94),
    ('ReserveCredit', 'other', '保留资金释放', 'other', 95),
    ('BuyerRecharge', 'other', '买家充值', 'other', 96),
    ('Debt Adjustment', 'other', '债务调整', 'adjustment', 97),
    ('LiquidationsBrokerageFee', 'other', '清仓经纪费', 'fee', 98);
*/

-- ====================================================
-- 说明
-- ====================================================
/*
本SQL文件包含两种映射：

1. 方案一（使用现有表 t_transaction_type_mapping）:
   - 通用映射(site_code=NULL): 适用于所有站点的英语type
   - 德国站映射(site_code='DE'): 德语type到标准分类
   - ERP映射(site_code='ERP'): ERP来源类型到标准分类

2. 方案二（新表 t_erp_fee_type_mapping，需取消注释）:
   - 专门存储ERP交易类型(费用明细)到数据库费用字段的映射
   - 如：Principal → product_sales, Commission → selling_fees

注意：
- ERP交易类型到费用字段的映射目前硬编码在 ErpSettlementParser.java 中
- 如果需要配置化，可以使用方案二的新表
- 建议保持现有代码硬编码方式，映射关系稳定不易出错
*/
