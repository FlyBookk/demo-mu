-- ============================================================
-- 交易类型映射数据更新
-- 版本: v1.6
-- 日期: 2026-01-24
-- 说明: 更新 t_transaction_type_mapping 表数据（增量模式，使用 IGNORE 避免重复）
-- ============================================================

-- ============================================================
-- 说明：使用 INSERT IGNORE 增量插入，不会删除现有数据
-- 如需完全重置，可先执行：TRUNCATE TABLE t_transaction_type_mapping;
-- ============================================================

-- ============================================================
-- 第一部分：亚马逊原始数据交易类型（来自 type 列）
-- ============================================================

-- 通用交易类型（适用于所有英文站点，site_code = NULL）
INSERT IGNORE INTO t_transaction_type_mapping (site_code, original_type, standard_category, category_desc) VALUES
(NULL, 'Order', 'income', '订单收入'),
(NULL, 'Refund', 'refund', '退款'),
(NULL, 'FBA Customer Return Fee', 'fee', 'FBA退货费'),
(NULL, 'Service Fee', 'fee', '服务费'),
(NULL, 'Adjustment', 'adjustment', '调整'),
(NULL, 'Liquidations', 'other', '清算'),
(NULL, 'Refund_Retrocharge', 'other', '追溯退款'),
(NULL, 'Transfer', 'other', '转账'),
(NULL, 'FBA Inventory Fee', 'fee', 'FBA库存费'),
(NULL, 'Subscription', 'fee', '订阅费用'),
(NULL, 'Deal Fee', 'fee', '秒杀费用'),
(NULL, 'Lightning Deal Fee', 'fee', '闪购费用'),
(NULL, 'Coupon Payment', 'fee', '优惠券费用'),
(NULL, 'Reserved', 'other', '保留资金'),
(NULL, 'Debt', 'other', '债务'),
-- ERP 来源类型（也存储在 transaction_type 字段）
(NULL, 'Shipment', 'income', 'ERP-发货/订单'),
(NULL, 'ServiceFee', 'fee', 'ERP-服务费'),
(NULL, 'RemovalShipment', 'other', 'ERP-移除发货'),
(NULL, 'RemovalShipmentAdjustment', 'adjustment', 'ERP-移除发货调整'),
(NULL, 'Retrocharge', 'other', 'ERP-追溯费用'),
(NULL, 'CouponPayment', 'fee', 'ERP-优惠券付款'),
(NULL, 'SellerDealPayment', 'fee', 'ERP-卖家秒杀付款'),
(NULL, 'Chargeback', 'other', 'ERP-拒付'),
(NULL, 'ProductAdsPayment', 'fee', 'ERP-产品广告付款');

-- 德语站点交易类型
INSERT IGNORE INTO t_transaction_type_mapping (site_code, original_type, standard_category, category_desc) VALUES
('DE', 'Bestellung', 'income', '订单收入'),
('DE', 'Erstattung', 'refund', '退款'),
('DE', 'Gebühren für Kundenrücksendungen mit Versand durch Amazon', 'fee', 'FBA退货费'),
('DE', 'Servicegebühr', 'fee', '服务费'),
('DE', 'Anpassung', 'adjustment', '调整'),
('DE', 'Übertragung', 'other', '转账');

-- ============================================================
-- 第二部分：ERP交易类型参考（用于金额字段映射）
-- ============================================================
-- 注意：以下类型在 ErpSettlementParser.ERP_TYPE_TO_FIELD 中硬编码处理
-- 不需要在 t_transaction_type_mapping 表中配置
--
-- 金额字段映射关系：
-- +--------------------------------+----------------------+
-- | ERP交易类型                    | SalesData字段        |
-- +--------------------------------+----------------------+
-- | Principal                      | productSales         |
-- | Tax/TaxAmount/BaseTax          | productSalesTax      |
-- | ShippingCharge/ShippingDiscount| shippingCredits      |
-- | ShippingTax/ShippingDiscountTax| shippingCreditsTax   |
-- | GiftWrap/GiftwrapChargeback    | giftWrapCredits      |
-- | GiftWrapTax                    | giftWrapCreditsTax   |
-- | PromotionDiscount              | promotionalRebates   |
-- | PromotionDiscountTax           | promotionalRebatesTax|
-- | MarketplaceFacilitatorVAT-*    | marketplaceWithheldTax|
-- | MarketplaceFacilitatorTax-*    | marketplaceWithheldTax|
-- | Commission/RefundCommission    | sellingFees          |
-- | FBA*Fee (各种FBA费用)          | fbaFees              |
-- | DigitalServicesFee/CouponFee等 | otherTransactionFees |
-- | Revenue/REVERSAL_*/Goodwill等  | other                |
-- +--------------------------------+----------------------+

-- ============================================================
-- 验证脚本
-- ============================================================

-- 查看所有映射
-- SELECT * FROM t_transaction_type_mapping ORDER BY site_code, original_type;

-- 按分类统计
-- SELECT standard_category, COUNT(*) as cnt FROM t_transaction_type_mapping GROUP BY standard_category;

-- 查看重复项（应该为空）
-- SELECT site_code, original_type, COUNT(*) FROM t_transaction_type_mapping 
-- GROUP BY site_code, original_type HAVING COUNT(*) > 1;
