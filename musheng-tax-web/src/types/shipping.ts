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
  // 计算字段
  revenueTotal: number
  // 其他信息
  sku?: string
  quantity?: number
  carrier?: string
  trackingNumber?: string
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

// 配送数据汇总
export interface ShippingSummary {
  totalShipments: number
  totalQuantity: number
  totalProductPrice: number
  totalShippingPrice: number
  totalRevenueTotal: number
  totalShippingCost: number
  currencyCode: string
}
