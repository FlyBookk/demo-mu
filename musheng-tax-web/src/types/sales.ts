/**
 * 销售数据类型定义
 * 与后端 t_sales_data 表结构对齐
 */

// 销售数据
export interface SalesData {
  id: number
  importBatchId?: number
  transactionDate: string
  originalDateStr?: string
  originalTimezone?: string
  settlementId?: string
  transactionType: string
  transactionCategory: string  // income/refund/fee/adjustment/other
  orderId: string
  sku?: string
  description?: string
  quantity?: number
  siteCode: string
  marketplace: string
  currencyCode: string
  accountType?: string
  fulfillment?: string
  orderCity?: string
  orderState?: string
  orderPostal?: string
  taxCollectionModel?: string
  // 金额字段
  productSales: number
  productSalesTax: number
  shippingCredits: number
  shippingCreditsTax: number
  giftWrapCredits: number
  giftWrapCreditsTax: number
  regulatoryFee: number
  regulatoryFeeTax: number
  promotionalRebates: number
  promotionalRebatesTax: number
  marketplaceWithheldTax: number
  sellingFees: number
  fbaFees: number
  otherTransactionFees: number
  other: number
  total: number
  createTime: string
}

// 销售数据查询参数
export interface SalesDataQuery {
  keyword?: string
  siteCode?: string
  transactionType?: string
  transactionCategory?: string
  startDate?: string
  endDate?: string
  sku?: string
  orderId?: string
  importBatchId?: number
  page?: number
  size?: number
}

// 销售数据导入参数
export interface SalesImportParams {
  siteCode: string
  templateId?: number
  fileId: string
}

// 销售数据汇总
export interface SalesSummary {
  totalOrders: number
  totalQuantity: number
  totalProductSales: number
  totalSellingFees: number
  totalFbaFees: number
  totalOtherFees: number
  totalAmount: number
  currencyCode: string
}

// 销售数据统计（按交易类型）
export interface SalesStatByType {
  transactionType: string
  transactionCategory: string
  count: number
  totalAmount: number
  percentage: number
}
