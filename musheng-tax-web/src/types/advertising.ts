/**
 * 广告数据类型定义
 * 对应后端 AdvertisingData 实体（广告发票数据模型）
 */

// 广告发票数据实体
export interface AdvertisingData {
  id: number
  storeName: string
  siteCode: string
  invoiceNumber: string
  invoiceStatus: string
  paymentType?: string
  billingStartDate: string
  billingEndDate: string
  issueDate: string
  currency: string
  invoiceAmount: number
  cost: number
  otherCost?: number
  campaignName?: string
  campaignId?: string
  pricingModel?: string
  clicks?: number
  avgCpc?: number
  dataSource?: string
  productList?: string
  adType?: string
  attachmentPath?: string
  remark?: string
  exchangeRate?: number
  amountCny?: number
  importBatchId?: string
  createBy?: number
  updateBy?: number
  createTime?: string
  updateTime?: string
}

// 广告费录入表单（单条录入）
export interface AdvertisingDataForm {
  siteCode: string
  yearMonth: string
  amount: number
  currencyCode: string
  invoiceNo?: string
  attachmentPath?: string
  remark?: string
}

// 广告数据查询参数（基础列表查询）
export interface AdvertisingDataQuery {
  siteCode?: string
  yearMonth?: string
  page?: number
  size?: number
  current?: number
}

// ==================== 批量导入相关类型 ====================

// 单条广告数据导入请求
export interface AdvertisingImportRequest {
  storeName: string
  siteCode?: string
  invoiceNumber: string
  invoiceStatus: string
  paymentType?: string
  billingStartDate: string
  billingEndDate: string
  issueDate: string
  currency: string
  invoiceAmount: number
  cost: number
  otherCost?: number
  campaignName?: string
  campaignId?: string
  pricingModel?: string
  clicks?: number
  avgCpc?: number
  dataSource?: string
  productList?: string
  adType?: string
  attachmentPath?: string
  remark?: string
  exchangeRate?: number
}

// 批量导入请求
export interface AdvertisingImportBatchRequest {
  data: AdvertisingImportRequest[]
  importBatchId?: string
}

// 导入失败详情
export interface ImportFailureDetail {
  index?: number
  invoiceNumber?: string
  errorMessage: string
}

// 导入响应
export interface AdvertisingImportResponse {
  totalCount: number
  importedCount: number
  duplicatedCount: number
  failedCount: number
  importBatchId: string
  duplicatedInvoices: string[]
  failedRecords: ImportFailureDetail[]
}

// ==================== 高级搜索相关类型 ====================

// 广告数据高级搜索参数
export interface AdvertisingSearchQuery {
  siteCode?: string
  billingStartDate?: string
  billingEndDate?: string
  invoiceNumber?: string
  current?: number
  size?: number
}
