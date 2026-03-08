# 销售数据 - ERP字段映射关系文档

> **创建时间**：2026-01-24
> 
> **目的**：梳理 ERP 数据字段与数据库表字段的映射关系，分析现状是否符合业务需求

---

## 1. 核心概念说明

### 1.1 你描述的业务逻辑

| ERP 字段 | 对应数据库字段 | 说明 |
|----------|---------------|------|
| **来源**（如 Shipment/Refund） | `transaction_type` | ERP 的"来源"代表业务交易类型 |
| **交易类型**（如 Principal/Commission） | 各费用金额字段 | ERP 的"交易类型"代表具体费用项，累加到对应金额字段 |

### 1.2 当前代码实现的逻辑（已调整）

| ERP 字段 | 对应数据库字段 | 说明 |
|----------|---------------|------|
| **来源**（如 Shipment/Refund） | `transaction_type` | ✅ 来源值存入交易类型字段 |
| **来源**（如 Shipment/Refund） | `settlement_category` | ✅ 转换后的标准化结算类型（ORDER/REFUND等） |
| **交易类型**（如 Principal/Commission） | 各费用金额字段 | ✅ 累加到对应金额字段 |

> **注意**：已移除冗余的 `source` 字段，来源值统一存储在 `transaction_type` 中

---

## 2. ERP CSV 文件 → 数据库表字段映射

### 2.1 基础信息字段映射

| ERP CSV 列名 | Java 属性名 | 数据库字段名 | 说明 |
|-------------|-------------|-------------|------|
| 结算编号 | settlementId | settlement_id | 结算批次编号 |
| 订单号 | orderId | order_id | 亚马逊订单编号 |
| 店铺 | storeName | store_name | ERP 店铺名称 |
| 国家 | siteCode | site_code | 站点编码（US/UK/DE等） |
| 报告类型 | - | - | 未映射 |
| 配送方式 | fulfillment | fulfillment | FBA/FBM |
| **来源** | transactionType | transaction_type | ✅ ERP来源存入此字段（Shipment/Refund等） |
| **来源** | settlementCategory | settlement_category | ✅ 转换后的结算类型（ORDER/REFUND等） |
| MSKU | - | - | 未映射到独立字段 |
| **交易类型** | - | 各金额字段 | 用于判断累加到哪个金额字段 |
| 结算时间 | transactionDate | transaction_date | 交易日期时间 |
| 币种 | currencyCode | currency_code | 货币编码 |
| 金额 | - | 各金额字段 | 根据交易类型累加 |
| 数量 | quantity | quantity | 商品数量 |
| 结算状态 | settlementStatus | settlement_status | 已结算/未结算 |
| 转账状态 | transferStatus | transfer_status | 已转账/未转账 |
| Settlement ID | - | - | 亚马逊原始 Settlement ID |
| SKU | sku | sku | 商品 SKU |
| 品名 | description | description | 商品描述 |
| FNSKU | - | - | 未映射 |

### 2.2 费用金额字段映射（ERP 交易类型 → 数据库金额字段）

| ERP 交易类型 | 数据库字段 | 中文说明 |
|-------------|-----------|----------|
| **商品销售收入** | | |
| Principal | product_sales | 商品销售额 |
| **商品销售税** | | |
| Tax | product_sales_tax | 销售税 |
| TaxAmount | product_sales_tax | 移除税额 |
| TaxAmountAdjustment | product_sales_tax | 税额调整 |
| BaseTax | product_sales_tax | 基础税（追溯） |
| **运费收入** | | |
| ShippingCharge | shipping_credits | 运费收入 |
| ShippingDiscount | shipping_credits | 运费折扣 |
| ShippingChargeback | shipping_credits | 运费退款 |
| **运费税** | | |
| ShippingTax | shipping_credits_tax | 运费税 |
| ShippingDiscountTax | shipping_credits_tax | 运费折扣税 |
| **礼品包装** | | |
| GiftWrap | gift_wrap_credits | 礼品包装费 |
| GiftwrapChargeback | gift_wrap_credits | 礼品包装退款 |
| GiftWrapTax | gift_wrap_credits_tax | 礼品包装税 |
| **促销折扣** | | |
| PromotionDiscount | promotional_rebates | 促销折扣 |
| PromotionDiscountTax | promotional_rebates_tax | 促销折扣税 |
| **平台代扣税** | | |
| MarketplaceFacilitatorVAT-Principal | marketplace_withheld_tax | 平台代收VAT（商品） |
| MarketplaceFacilitatorTax-Principal | marketplace_withheld_tax | 平台代收税（商品） |
| MarketplaceFacilitatorVAT-Shipping | marketplace_withheld_tax | 平台代收VAT（运费） |
| MarketplaceFacilitatorTax-Shipping | marketplace_withheld_tax | 平台代收税（运费） |
| MarketplaceFacilitatorTax-Other | marketplace_withheld_tax | 平台代收税（其他） |
| TaxWithheld | marketplace_withheld_tax | 代扣税 |
| TaxWithheldAdjustment | marketplace_withheld_tax | 代扣税调整 |
| **销售佣金** | | |
| Commission | selling_fees | 销售佣金 |
| RefundCommission | selling_fees | 退货佣金返还 |
| **FBA 费用** | | |
| FBAPerUnitFulfillmentFee | fba_fees | FBA配送费 |
| FBAWeightBasedFee | fba_fees | FBA重量费 |
| FBACustomerReturnPerUnitFee | fba_fees | FBA退货处理费 |
| FBAStorageFee | fba_fees | FBA仓储费 |
| FBALongTermStorageFee | fba_fees | 长期仓储费 |
| FBADisposalFee | fba_fees | 库存销毁费 |
| **其他交易费用** | | |
| DigitalServicesFee | other_transaction_fees | 数字服务费 |
| DigitalServicesFeeFBA | other_transaction_fees | FBA数字服务费 |
| CouponRedemptionFee | other_transaction_fees | 优惠券兑换费 |
| CouponParticipationFee | other_transaction_fees | 优惠券参与费 |
| CouponPerformanceFee | other_transaction_fees | 优惠券使用费 |
| DealParticipationFee | other_transaction_fees | 促销报名费 |
| DealPerformanceFee | other_transaction_fees | 秒杀成交服务费 |
| Subscription | other_transaction_fees | 专业卖家月费 |
| VineFee | other_transaction_fees | Vine计划费 |
| **其他金额** | | |
| Revenue | other | 移除货物收入 |
| RevenueAdjustment | other | 收入调整 |
| FeeAmount | other | 费用金额 |
| feeAmount | other | 费用金额（小写） |
| baseValue | other | 基础价值 |
| REVERSAL_REIMBURSEMENT | other | Amazon赔偿 |
| COMPENSATED_CLAWBACK | other | 赔偿追回 |
| WAREHOUSE_LOST | other | 仓库丢失赔偿 |
| WAREHOUSE_DAMAGE | other | 仓库损坏赔偿 |
| REMOVAL_ORDER_LOST | other | 移除订单丢失 |
| FREE_REPLACEMENT_REFUND_ITEMS | other | 免费替换商品退款 |
| INCORRECT_FEES_NON_ITEMIZED | other | 费用错误调整 |
| RestockingFee | other | 补货费 |
| Goodwill | other | 商誉补偿 |
| ReserveDebit | other | 保留资金冻结 |
| ReserveCredit | other | 保留资金释放 |
| BuyerRecharge | other | 买家充值 |
| Debt Adjustment | other | 债务调整 |

---

## 3. 来源（Source）→ 结算类型映射

| ERP 来源值 | 结算类型代码 | 结算类型名称 | 交易分类 |
|-----------|-------------|-------------|----------|
| Shipment | ORDER | 订单结算 | income |
| Refund | REFUND | 退款结算 | refund |
| ServiceFee | SERVICE_FEE | 服务费结算 | fee |
| RemovalShipment | REMOVAL | 移除结算 | other |
| Adjustment | ADJUSTMENT | 调整结算 | adjustment |
| Retrocharge | RETROCHARGE | 追溯结算 | adjustment |
| CouponPayment | COUPON | 优惠券结算 | fee |
| RemovalShipmentAdjustment | REMOVAL_ADJUSTMENT | 移除调整 | adjustment |
| SellerDealPayment | DEAL_PAYMENT | 促销结算 | fee |
| Chargeback | CHARGEBACK | 拒付结算 | refund |
| ProductAdsPayment | ADS_PAYMENT | 广告结算 | fee |

---

## 4. 现状分析与问题

### 4.1 ✅ 已确认：`transaction_type` 字段存储来源值

**业务需求**：
- ERP 的"来源"应该存到 `transaction_type`

**当前实现**：
```java
// convertToSalesData() 中的代码
data.setTransactionType(aggregate.getSource() != null ? aggregate.getSource() : "ERP_SETTLEMENT");
```

**结论**：✅ **符合要求**，`transaction_type` 存的是来源值（Shipment/Refund等）

### 4.2 ✅ 已确认：ERP 交易类型 → 金额字段

**你的预期**：
- ERP 的"交易类型"（Principal/Commission等）应该累加到对应的费用字段

**当前实现**：
```java
// accumulateAmount() 方法
String targetField = ERP_TYPE_TO_FIELD.get(row.getTransactionType());
// 根据 targetField 累加到对应金额字段
```

**结论**：✅ **完全符合**，48 种交易类型都有对应的金额字段映射

### 4.3 字段调整说明

| 字段 | 状态 | 说明 |
|------|------|------|
| `source` | ❌ 已移除 | 与 `transaction_type` 冗余，不再单独存储 |
| `settlement_category` | ✅ 保留 | 标准化结算类型代码，便于统计分析 |

---

## 5. 数据流转示意图

```
ERP CSV 原始数据（一行）
┌─────────────────────────────────────────────────────────────────┐
│ 结算编号: 'LWCBH4OW1XWS                                          │
│ 订单号: 204-3372444-8169904                                      │
│ 来源: Shipment                    ─────┐                         │
│ 交易类型: Commission              ─────┼──┐                      │
│ 金额: -1.20                       ─────┼──┼──┐                   │
│ ...                                    │  │  │                   │
└────────────────────────────────────────┼──┼──┼───────────────────┘
                                         │  │  │
                                         ▼  ▼  ▼
                              ┌──────────────────────────────────┐
                              │        解析与映射逻辑             │
                              │                                  │
                              │  1. 来源 → transaction_type      │
                              │  2. 来源 → settlement_category   │
                              │  3. 交易类型 → 确定金额字段       │
                              │     Commission → selling_fees    │
                              │  4. 金额 → 累加到 selling_fees   │
                              └──────────────────────────────────┘
                                         │
                                         ▼
                              ┌──────────────────────────────────┐
                              │    聚合后的数据库记录              │
                              │                                  │
                              │  transaction_type: Shipment      │
                              │  settlement_category: ORDER      │
                              │  selling_fees: -1.20 (累加)      │
                              │  ...                             │
                              └──────────────────────────────────┘
```

---

## 6. 聚合逻辑说明

### 6.1 聚合维度（Key）

当前 Key 格式：`source|orderId|siteCode|sku`

**含义**：同一来源 + 同一订单 + 同一站点 + 同一SKU 的数据合并为一条记录

### 6.2 聚合示例

假设 ERP 有以下 5 行数据（同一订单）：

| 来源 | 订单号 | 交易类型 | 金额 |
|------|--------|----------|------|
| Shipment | 204-xxx | Principal | 12.49 |
| Shipment | 204-xxx | Commission | -1.20 |
| Shipment | 204-xxx | FBAPerUnitFulfillmentFee | -2.72 |
| Shipment | 204-xxx | Tax | 2.50 |
| Shipment | 204-xxx | ShippingCharge | 0.62 |

**聚合后**（一条记录）：

| 字段 | 值 |
|------|-----|
| transaction_type | Shipment |
| settlement_category | ORDER |
| product_sales | 12.49 |
| selling_fees | -1.20 |
| fba_fees | -2.72 |
| product_sales_tax | 2.50 |
| shipping_credits | 0.62 |
| total | 11.69 |

---

## 7. 总结

| 检查项 | 状态 | 说明 |
|--------|------|------|
| ERP"来源" → `transaction_type` | ✅ 符合 | 来源值存入 transaction_type |
| ERP"交易类型" → 金额字段 | ✅ 符合 | 48种交易类型正确映射到金额字段 |
| 聚合逻辑 | ✅ 符合 | 按来源+订单+站点+SKU 聚合 |
| settlement_category 字段 | ✅ 保留 | 标准化分类，便于统计 |
| source 字段 | ❌ 已移除 | 与 transaction_type 冗余 |

**结论**：当前实现 **完全符合** 你描述的业务逻辑，`settlement_category` 字段用于更精细的数据管理。

---

## 8. 已确认事项

| 事项 | 决定 | 说明 |
|------|------|------|
| `source` 字段 | ❌ 移除 | 与 `transaction_type` 冗余 |
| `settlement_category` 字段 | ✅ 保留 | 标准化分类，便于统计分析 |
| 聚合维度 | ✅ 保留来源维度 | `transactionType\|orderId\|siteCode\|sku` |
| 交易类型映射 | ✅ 完整 | 48 种映射覆盖所有发现的类型 |

---

## 9. 更新日志

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-01-24 | 创建文档 | 初始映射关系梳理 |
| 2026-01-24 | 调整设计 | 移除冗余 source 字段，保留 settlement_category |
