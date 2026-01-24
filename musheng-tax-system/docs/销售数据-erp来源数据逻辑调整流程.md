# 销售数据 - ERP来源数据逻辑调整设计文档

## 1. 背景与目标

### 1.1 现状分析

当前 ERP 销售数据导入存在以下问题：
1. **聚合维度不完整**：现有逻辑按 `orderId + siteCode + sku` 聚合，未考虑"来源"字段
2. **来源类型未区分**：不同来源代表不同的业务场景，需要分别处理
3. **交易类型映射不完整**：部分新发现的交易类型未包含在映射中
4. **结算类型缺失**：缺少基于来源的结算类型定义

### 1.2 目标

1. 分析 ERP 数据中的"来源"字段，定义标准化的结算类型
2. 完善交易类型映射，覆盖所有发现的交易类型
3. 调整聚合逻辑，按 `来源 + 订单号 + SKU` 维度聚合
4. 输出分析设计文档，为后续开发提供指导

---

## 2. 数据分析

### 2.1 来源类型分析

基于 ERP 数据文件分析，发现以下来源类型及其分布：

| 来源（Source） | 数量 | 占比 | 业务含义 | 结算类型定义 |
|---------------|------|------|----------|-------------|
| **Shipment** | 265,716 | 76.9% | 正常发货/销售 | ORDER（订单结算） |
| **Refund** | 62,777 | 18.2% | 退款 | REFUND（退款结算） |
| **ServiceFee** | 12,297 | 3.6% | 服务费 | SERVICE_FEE（服务费结算） |
| **RemovalShipment** | 3,033 | 0.9% | 移除货物发货 | REMOVAL（移除结算） |
| **Adjustment** | 1,008 | 0.3% | 调整（赔偿、追回等） | ADJUSTMENT（调整结算） |
| **Retrocharge** | 502 | 0.1% | 追溯收费（税费补缴） | RETROCHARGE（追溯结算） |
| **CouponPayment** | 88 | <0.1% | 优惠券支付费用 | COUPON（优惠券结算） |
| **RemovalShipmentAdjustment** | 86 | <0.1% | 移除发货调整 | REMOVAL_ADJUSTMENT（移除调整） |
| **SellerDealPayment** | 12 | <0.1% | 卖家促销付款 | DEAL_PAYMENT（促销结算） |
| **Chargeback** | 3 | <0.1% | 退单/拒付 | CHARGEBACK（拒付结算） |
| **ProductAdsPayment** | 1 | <0.1% | 产品广告付款 | ADS_PAYMENT（广告结算） |

### 2.2 交易类型分析

#### 2.2.1 高频交易类型（>1000次）

| 交易类型 | 数量 | 含义 | 目标字段 |
|---------|------|------|----------|
| Principal | 55,564 | 商品销售收入 | productSales |
| Commission | 55,288 | 销售佣金 | sellingFees |
| Tax | 51,988 | 销售税 | productSalesTax |
| FBAPerUnitFulfillmentFee | 44,674 | FBA配送费 | fbaFees |
| MarketplaceFacilitatorVAT-Principal | 33,083 | 平台代收VAT（商品） | marketplaceWithheldTax |
| MarketplaceFacilitatorTax-Principal | 18,859 | 平台代收税（商品） | marketplaceWithheldTax |
| DigitalServicesFee | 17,972 | 数字服务费 | otherTransactionFees |
| RefundCommission | 10,596 | 退货佣金返还 | sellingFees |
| FBACustomerReturnPerUnitFee | 10,556 | FBA退货处理费 | fbaFees |
| PromotionDiscount | 10,533 | 促销折扣 | promotionalRebates |
| ShippingCharge | 9,600 | 运费收入 | shippingCredits |
| PromotionDiscountTax | 7,931 | 促销折扣税 | promotionalRebatesTax |
| ShippingTax | 5,656 | 运费税 | shippingCreditsTax |
| ShippingDiscount | 3,207 | 运费折扣 | shippingCredits |
| ShippingChargeback | 1,351 | 运费退款 | shippingCredits |
| Revenue | 1,301 | 移除货物收入 | other |
| FeeAmount | 1,259 | 移除费用 | other |

#### 2.2.2 中频交易类型（100-1000次）

| 交易类型 | 数量 | 含义 | 目标字段 |
|---------|------|------|----------|
| ShippingDiscountTax | 1,092 | 运费折扣税 | shippingCreditsTax |
| MarketplaceFacilitatorVAT-Shipping | 1,071 | 平台代收VAT（运费） | marketplaceWithheldTax |
| FBAStorageFee | 708 | FBA仓储费 | fbaFees |
| REVERSAL_REIMBURSEMENT | 686 | Amazon赔偿（回转） | other |
| FBALongTermStorageFee | 509 | 长期仓储费 | fbaFees |
| TaxAmount | 464 | 移除税额 | productSalesTax |
| FBADisposalFee | 446 | 库存销毁费 | fbaFees |
| BaseTax | 248 | 基础税（追溯） | productSalesTax |
| MarketplaceFacilitatorTax-Shipping | 191 | 平台代收税（运费） | marketplaceWithheldTax |
| COMPENSATED_CLAWBACK | 138 | 赔偿追回 | other |

#### 2.2.3 低频交易类型（<100次）

| 交易类型 | 数量 | 含义 | 目标字段 |
|---------|------|------|----------|
| WAREHOUSE_LOST | 95 | 仓库丢失赔偿 | other |
| CouponRedemptionFee | 88 | 优惠券兑换费 | otherTransactionFees |
| DigitalServicesFeeFBA | 46 | FBA数字服务费 | otherTransactionFees |
| TaxAmountAdjustment | 41 | 税额调整 | productSalesTax |
| RestockingFee | 34 | 补货费 | other |
| TaxWithheldAdjustment | 28 | 代扣税调整 | marketplaceWithheldTax |
| CouponParticipationFee | 22 | 优惠券参与费 | otherTransactionFees |
| FREE_REPLACEMENT_REFUND_ITEMS | 21 | 免费替换商品退款 | other |
| CouponPerformanceFee | 19 | 优惠券使用费 | otherTransactionFees |
| RevenueAdjustment | 17 | 收入调整 | other |
| WAREHOUSE_DAMAGE | 15 | 仓库损坏赔偿 | other |
| ReserveDebit | 14 | 新增保留资金冻结 | other |
| ReserveCredit | 14 | 保留资金释放 | other |
| DealPerformanceFee | 13 | 秒杀成交服务费 | otherTransactionFees |
| DealParticipationFee | 13 | 秒杀/促销报名费 | otherTransactionFees |
| feeAmount | 12 | 费用金额 | other |
| Debt Adjustment | 12 | 债务调整 | other |
| TaxWithheld | 9 | 代扣税 | marketplaceWithheldTax |
| Subscription | 9 | 专业卖家月费 | otherTransactionFees |
| REMOVAL_ORDER_LOST | 7 | 移除订单丢失 | other |
| Goodwill | 6 | 商誉补偿 | other |
| INCORRECT_FEES_NON_ITEMIZED | 4 | 费用错误调整 | other |
| VineFee | 2 | Vine计划费 | otherTransactionFees |
| MarketplaceFacilitatorTax-Other | 2 | 平台代收税（其他） | marketplaceWithheldTax |
| GiftwrapChargeback | 2 | 礼品包装退款 | giftWrapCredits |
| GiftWrapTax | 2 | 礼品包装税 | giftWrapCreditsTax |
| GiftWrap | 2 | 礼品包装费 | giftWrapCredits |
| BuyerRecharge | 2 | 买家充值 | other |
| baseValue | 1 | 基础价值 | other |

### 2.3 来源与交易类型关联分析

| 来源 | 主要交易类型 |
|------|-------------|
| **Shipment** | Principal, Commission, Tax, FBAPerUnitFulfillmentFee, MarketplaceFacilitatorVAT-Principal, DigitalServicesFee, PromotionDiscount, ShippingCharge, ShippingTax 等 |
| **Refund** | Principal(负), Commission, RefundCommission, Tax(负), MarketplaceFacilitatorVAT-Principal, PromotionDiscount, RestockingFee, Goodwill 等 |
| **ServiceFee** | FBACustomerReturnPerUnitFee, FBAStorageFee, FBALongTermStorageFee, FBADisposalFee, CouponParticipationFee, CouponPerformanceFee, DealParticipationFee, DealPerformanceFee, Subscription, VineFee |
| **RemovalShipment** | Revenue, FeeAmount, TaxAmount, TaxWithheld |
| **Adjustment** | REVERSAL_REIMBURSEMENT, COMPENSATED_CLAWBACK, WAREHOUSE_LOST, WAREHOUSE_DAMAGE, FREE_REPLACEMENT_REFUND_ITEMS, ReserveDebit, ReserveCredit, REMOVAL_ORDER_LOST, INCORRECT_FEES_NON_ITEMIZED, BuyerRecharge, Debt Adjustment |
| **Retrocharge** | BaseTax, MarketplaceFacilitatorTax-Principal, MarketplaceFacilitatorVAT-Principal, MarketplaceFacilitatorVAT-Shipping, ShippingTax |
| **CouponPayment** | CouponRedemptionFee |
| **RemovalShipmentAdjustment** | TaxAmountAdjustment, TaxWithheldAdjustment, RevenueAdjustment |
| **SellerDealPayment** | feeAmount |
| **Chargeback** | Principal, Commission, RefundCommission |
| **ProductAdsPayment** | baseValue |

---

## 3. 设计方案

### 3.1 结算类型枚举定义

```java
public enum SettlementType {
    ORDER("Shipment", "订单结算", "正常销售发货"),
    REFUND("Refund", "退款结算", "客户退货退款"),
    SERVICE_FEE("ServiceFee", "服务费结算", "FBA服务费、优惠券费等"),
    REMOVAL("RemovalShipment", "移除结算", "库存移除发货"),
    ADJUSTMENT("Adjustment", "调整结算", "赔偿、追回、资金调整"),
    RETROCHARGE("Retrocharge", "追溯结算", "税费追溯补缴"),
    COUPON("CouponPayment", "优惠券结算", "优惠券兑换费用"),
    REMOVAL_ADJUSTMENT("RemovalShipmentAdjustment", "移除调整", "移除相关税费调整"),
    DEAL_PAYMENT("SellerDealPayment", "促销结算", "卖家促销活动费用"),
    CHARGEBACK("Chargeback", "拒付结算", "买家拒付/退单"),
    ADS_PAYMENT("ProductAdsPayment", "广告结算", "产品广告费用"),
    OTHER("Other", "其他结算", "未分类结算");
}
```

### 3.2 聚合逻辑调整

**现有逻辑：** `orderId + siteCode + sku`
**新逻辑：** `source(来源) + orderId + siteCode + sku`

核心变更：
1. 在聚合 Key 中增加来源（source）字段
2. 同一订单在不同来源下产生的费用分别聚合
3. 保留原有的交易类型到金额字段映射

### 3.3 交易类型映射更新

需要新增以下映射：

```java
// 新增的交易类型映射
ERP_TYPE_TO_FIELD.put("FBACustomerReturnPerUnitFee", "fbaFees");
ERP_TYPE_TO_FIELD.put("FBAStorageFee", "fbaFees");
ERP_TYPE_TO_FIELD.put("FBALongTermStorageFee", "fbaFees");
ERP_TYPE_TO_FIELD.put("FBADisposalFee", "fbaFees");
ERP_TYPE_TO_FIELD.put("ShippingDiscount", "shippingCredits");
ERP_TYPE_TO_FIELD.put("ShippingChargeback", "shippingCredits");
ERP_TYPE_TO_FIELD.put("ShippingDiscountTax", "shippingCreditsTax");
ERP_TYPE_TO_FIELD.put("MarketplaceFacilitatorVAT-Shipping", "marketplaceWithheldTax");
ERP_TYPE_TO_FIELD.put("MarketplaceFacilitatorTax-Shipping", "marketplaceWithheldTax");
ERP_TYPE_TO_FIELD.put("MarketplaceFacilitatorTax-Other", "marketplaceWithheldTax");
ERP_TYPE_TO_FIELD.put("TaxAmount", "productSalesTax");
ERP_TYPE_TO_FIELD.put("TaxAmountAdjustment", "productSalesTax");
ERP_TYPE_TO_FIELD.put("TaxWithheld", "marketplaceWithheldTax");
ERP_TYPE_TO_FIELD.put("TaxWithheldAdjustment", "marketplaceWithheldTax");
ERP_TYPE_TO_FIELD.put("BaseTax", "productSalesTax");
ERP_TYPE_TO_FIELD.put("CouponRedemptionFee", "otherTransactionFees");
ERP_TYPE_TO_FIELD.put("CouponParticipationFee", "otherTransactionFees");
ERP_TYPE_TO_FIELD.put("CouponPerformanceFee", "otherTransactionFees");
ERP_TYPE_TO_FIELD.put("DealParticipationFee", "otherTransactionFees");
ERP_TYPE_TO_FIELD.put("DealPerformanceFee", "otherTransactionFees");
ERP_TYPE_TO_FIELD.put("DigitalServicesFeeFBA", "otherTransactionFees");
ERP_TYPE_TO_FIELD.put("Subscription", "otherTransactionFees");
ERP_TYPE_TO_FIELD.put("VineFee", "otherTransactionFees");
ERP_TYPE_TO_FIELD.put("RevenueAdjustment", "other");
ERP_TYPE_TO_FIELD.put("RestockingFee", "other");
ERP_TYPE_TO_FIELD.put("Goodwill", "other");
ERP_TYPE_TO_FIELD.put("WAREHOUSE_LOST", "other");
ERP_TYPE_TO_FIELD.put("WAREHOUSE_DAMAGE", "other");
ERP_TYPE_TO_FIELD.put("REMOVAL_ORDER_LOST", "other");
ERP_TYPE_TO_FIELD.put("FREE_REPLACEMENT_REFUND_ITEMS", "other");
ERP_TYPE_TO_FIELD.put("INCORRECT_FEES_NON_ITEMIZED", "other");
ERP_TYPE_TO_FIELD.put("ReserveDebit", "other");
ERP_TYPE_TO_FIELD.put("ReserveCredit", "other");
ERP_TYPE_TO_FIELD.put("BuyerRecharge", "other");
ERP_TYPE_TO_FIELD.put("Debt Adjustment", "other");
ERP_TYPE_TO_FIELD.put("feeAmount", "other");
ERP_TYPE_TO_FIELD.put("baseValue", "other");
ERP_TYPE_TO_FIELD.put("GiftwrapChargeback", "giftWrapCredits");
```

### 3.4 数据实体调整

在 `SalesData` 实体中新增字段：

```java
/**
 * 结算类型（ORDER/REFUND/SERVICE_FEE/REMOVAL/ADJUSTMENT等）
 */
@FieldMapping(label = "结算类型", description = "ERP数据来源分类")
private String settlementCategory;

/**
 * 原始来源值（Shipment/Refund等）
 */
@FieldMapping(label = "来源", description = "ERP数据原始来源")
private String source;
```

---

## 4. 实施计划

### 4.1 第一阶段：枚举与映射完善
- [ ] 创建 `SettlementType` 枚举类
- [ ] 更新 `ErpSettlementParser` 中的交易类型映射
- [ ] 在 `SalesData` 实体中添加 `settlementCategory` 和 `source` 字段

### 4.2 第二阶段：聚合逻辑调整
- [ ] 修改 `ErpSettlementParser.buildAggregateKey()` 方法，增加来源维度
- [ ] 修改 `ErpRow` 和 `ErpAggregateRow` 添加来源字段
- [ ] 在 `convertToSalesData()` 中设置结算类型

### 4.3 第三阶段：数据库与前端适配
- [ ] 更新数据库表结构，添加新字段
- [ ] 前端销售数据列表增加结算类型筛选
- [ ] 导出功能添加结算类型列

### 4.4 第四阶段：测试与验证
- [ ] 编写单元测试覆盖所有来源类型
- [ ] 使用真实 ERP 数据进行集成测试
- [ ] 验证聚合结果与原始数据一致性

---

## 5. 参考资料

### 5.1 相关文件
- 后端控制器：`SalesDataController.java`
- 数据实体：`SalesData.java`
- ERP解析器：`ErpSettlementParser.java`
- 数据服务：`SalesDataServiceImpl.java`

### 5.2 ERP数据样本
- 文件路径：`参考资料/销售数据/结算中心-结算明细-872116414561021952.csv`
- 字段列表：结算编号、订单号、店铺、国家、报告类型、配送方式、**来源**、MSKU、**交易类型**、结算时间、币种、金额、数量、结算状态、转账状态、Settlement ID、SKU、品名、FNSKU

---

## 6. 附录：交易类型完整定义

| 代码 | 中文名称 | 说明 |
|------|----------|------|
| BaseTax | 基础税 | 追溯收费基础税 |
| Commission | 销售佣金 | 平台销售佣金 |
| COMPENSATED_CLAWBACK | 赔偿追回 | Amazon扣回之前的赔偿 |
| CouponParticipationFee | 优惠券参与费 | 参与优惠券活动的费用 |
| CouponPerformanceFee | 优惠券使用费 | 优惠券被使用产生的费用 |
| CouponRedemptionFee | 优惠券兑换费 | 客户兑换优惠券的费用 |
| DealParticipationFee | 秒杀/促销报名费 | 参与促销活动的报名费 |
| DealPerformanceFee | 秒杀成交服务费 | 促销活动成交服务费 |
| DigitalServicesFee | 数字服务费 | 平台数字服务费 |
| DigitalServicesFeeFBA | FBA数字服务费 | FBA相关数字服务费 |
| FBACustomerReturnPerUnitFee | FBA退货处理费 | 处理客户退货的费用 |
| FBADisposalFee | 库存销毁费 | 销毁库存的费用 |
| FBALongTermStorageFee | 长期仓储费 | 长期存放库存的费用 |
| FBAPerUnitFulfillmentFee | FBA配送费 | 每单位FBA配送费用 |
| FBAStorageFee | FBA仓储费 | 常规仓储费用 |
| FeeAmount | 费用金额 | 通用费用金额 |
| FREE_REPLACEMENT_REFUND_ITEMS | 免费替换商品退款 | 免费替换产生的退款 |
| GiftWrap | 礼品包装费 | 礼品包装服务费 |
| GiftwrapChargeback | 礼品包装退款 | 礼品包装退款 |
| GiftWrapTax | 礼品包装税 | 礼品包装税金 |
| Goodwill | 商誉补偿 | Amazon商誉补偿 |
| INCORRECT_FEES_NON_ITEMIZED | 费用错误调整 | 费用错误的调整 |
| MarketplaceFacilitatorTax-Principal | 平台代收商品税 | 平台代收的商品销售税 |
| MarketplaceFacilitatorTax-Shipping | 平台代收运费税 | 平台代收的运费税 |
| MarketplaceFacilitatorTax-Other | 平台代收其他税 | 平台代收的其他税 |
| MarketplaceFacilitatorVAT-Principal | 平台代收商品VAT | 平台代收的商品VAT |
| MarketplaceFacilitatorVAT-Shipping | 平台代收运费VAT | 平台代收的运费VAT |
| Principal | 商品销售收入 | 商品销售金额（不含税/运费） |
| PromotionDiscount | 促销折扣 | 促销优惠金额 |
| PromotionDiscountTax | 促销折扣税 | 促销折扣对应税金 |
| RefundCommission | 退货佣金返还 | 退货返还的佣金 |
| REMOVAL_ORDER_LOST | 移除订单丢失 | 移除订单丢失赔偿 |
| ReserveCredit | 保留资金释放 | 释放冻结的保留资金 |
| ReserveDebit | 新增保留资金冻结 | 新增冻结保留资金 |
| RestockingFee | 补货费 | 退货补货扣费 |
| Revenue | 收入 | 移除货物收入 |
| RevenueAdjustment | 收入调整 | 收入金额调整 |
| REVERSAL_REIMBURSEMENT | Amazon赔偿 | Amazon回转赔偿 |
| ShippingCharge | 运费收入 | 买家支付的运费 |
| ShippingChargeback | 运费退款 | 运费退款金额 |
| ShippingDiscount | 运费折扣 | 运费折扣金额 |
| ShippingDiscountTax | 运费折扣税 | 运费折扣对应税金 |
| ShippingTax | 运费税 | 运费税金 |
| Subscription | 专业卖家月费 | 专业卖家订阅费 |
| Tax | 销售税 | 销售税总额 |
| TaxAmount | 税额 | 移除相关税额 |
| TaxAmountAdjustment | 税额调整 | 税额调整金额 |
| TaxWithheld | 代扣税 | 代扣税金 |
| TaxWithheldAdjustment | 代扣税调整 | 代扣税调整金额 |
| VineFee | Vine计划费 | Vine评测计划费用 |
| WAREHOUSE_DAMAGE | 仓库损坏赔偿 | 仓库损坏赔偿金 |
| WAREHOUSE_LOST | 仓库丢失赔偿 | 仓库丢失赔偿金 |
