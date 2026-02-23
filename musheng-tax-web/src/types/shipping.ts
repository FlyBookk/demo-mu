/**
 * 配送数据类型定义
 * 与后端 t_shipping_data 表结构对齐
 */

// 配送数据
export interface ShippingData {
  id: number
  importBatchId?: number
  orderId: string
  shipDate: string
  siteCode: string
  marketplace: string
  currencyCode: string
  // 收入相关字段
  productPrice: number
  productTax: number
  shippingPrice: number
  shippingTax: number
  giftWrapPrice: number
  giftWrapTax: number
  productPromotionDiscount: number
  shipmentPromotionDiscount: number
  // 物流成本
  shippingCost: number
  // 总计费用（导入时由各分项计算）
  totalAmount: number
  // 其他信息
  sku?: string
  quantity?: number
  carrier?: string
  trackingNumber?: string
  // 汇率信息
  exchangeRate?: number
  exchangeRateDate?: string
  createTime: string
}

// 配送数据查询参数
export interface ShippingDataQuery {
  keyword?: string
  siteCode?: string
  startDate?: string
  endDate?: string
  sku?: string
  orderId?: string
  importBatchId?: number
  carrier?: string
  page?: number
  size?: number
}

// 配送数据导入参数
export interface ShippingImportParams {
  siteCode: string
  fileId: string
}

// 配送数据批量导入结果
export interface ShippingBatchImportResult {
  totalFiles: number
  successFiles: number
  failFiles: number
  totalCount: number
  successCount: number
  failCount: number
  duplicateCount: number
  batchNo?: string
  fileResults?: Array<{
    fileName: string
    status: string
    result?: Record<string, unknown>
    message?: string
  }>
}

// 配送数据汇总（人民币）
export interface ShippingSummary {
  totalOrders: number
  totalQuantity: number
  totalProductPriceCny: number
  totalShippingPriceCny: number
  totalAmountCny: number
  totalShippingCostCny: number
  currencyCode: string
}
