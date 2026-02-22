/**
 * 销售数据类型定义
 * 与后端 t_sales_data 表结构对齐
 */

// 销售数据
export interface SalesData {
  id: number
  importBatchId?: number
  sourceType?: string            // 数据源类型(ORIGINAL/ERP)
  storeName?: string             // 店铺名称（预留字段）
  transactionDate: string
  settlementId?: string
  transactionType: string
  transactionCategory: string    // income/refund/fee/adjustment/other
  orderId: string
  sku?: string
  description?: string
  quantity?: number
  siteCode: string
  marketplace: string
  currencyCode: string
  fulfillment?: string
  // 金额字段（全部保留）
  productSales: number
  productSalesTax: number
  shippingCredits: number
  shippingCreditsTax: number
  giftWrapCredits: number
  giftWrapCreditsTax: number
  regulatoryFee: number          // 监管费(仅CA/US)
  regulatoryFeeTax: number       // 监管费税(仅CA/US)
  promotionalRebates: number
  promotionalRebatesTax: number
  marketplaceWithheldTax: number
  sellingFees: number
  fbaFees: number
  otherTransactionFees: number
  other: number
  total: number
  // 汇率信息
  exchangeRate?: number
  exchangeRateDate?: string
  createTime: string
}

// 销售数据查询参数
export interface SalesDataQuery {
  keyword?: string
  sourceType?: string    // 数据来源(ORIGINAL/ERP)
  siteCode?: string
  settlementId?: string  // 结算ID
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

// 销售数据汇总（人民币）
export interface SalesSummary {
  totalOrders: number
  totalQuantity: number
  totalProductSalesCny: number
  totalSellingFeesCny: number
  totalFbaFeesCny: number
  totalOtherFeesCny: number
  totalAmountCny: number
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

// ========== 双格式导入相关类型 ==========

/**
 * 数据源类型
 */
export type SalesSourceType = 'ORIGINAL' | 'ERP'

/**
 * 数据源类型选项
 */
export const SalesSourceTypeOptions = [
  { value: 'ORIGINAL' as SalesSourceType, label: '亚马逊原始数据' },
  { value: 'ERP' as SalesSourceType, label: 'ERP结算数据' }
]

/**
 * 上传文件请求参数
 */
export interface SalesUploadParams {
  file: File
  sourceType: SalesSourceType
  siteCode?: string
}

/**
 * 上传文件结果
 */
export interface SalesUploadResult {
  fileId: string
  fileName: string
  fileSize: number
  filePath: string
  totalRows: number
  headerRow: number
  sourceFields: string[]
  detectedSiteCode: string
  detectedSiteCodes: string[]
  encoding: string
  sampleData: Record<string, any>[]
}

/**
 * 预览请求参数
 */
export interface SalesPreviewParams {
  fileId: string
  sourceType: SalesSourceType
  siteCode: string
  templateId: number
  quarter?: string
}

/**
 * 列元数据
 */
export interface ColumnMeta {
  field: string
  label: string
  type: string
  sourceField: string
}

/**
 * 映射状态
 */
export interface MappingStatus {
  totalFields: number
  mappedFields: number
  requiredMissing: string[]
}

/**
 * 预览结果
 */
export interface SalesPreviewResult {
  totalRows: number
  previewRows: number
  columns: ColumnMeta[]
  data: Record<string, any>[]
  mappingStatus: MappingStatus
  warnings: string[]
}

/**
 * 双格式导入请求参数
 */
export interface SalesDualImportParams {
  fileId: string
  sourceType: SalesSourceType
  siteCode: string
  templateId: number
  quarter?: string
  skipDuplicate?: boolean
  overwriteDuplicate?: boolean
}

/**
 * 导入结果
 */
export interface SalesImportResult {
  batchNo: string
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'PARTIAL' | 'FAIL'
  totalCount: number
  successCount: number
  failCount: number
  skipCount: number
  importRecordId: number
  async: boolean
  estimatedSeconds: number
}

/**
 * 销售导入错误详情
 */
export interface SalesImportErrorDetail {
  row: number
  orderId: string
  error: string
}

/**
 * 导入进度
 */
export interface SalesImportProgress {
  batchNo: string
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'PARTIAL' | 'FAIL'
  totalCount: number
  processedCount: number
  successCount: number
  failCount: number
  skipCount: number
  percentage: number
  message: string
  errorDetails: SalesImportErrorDetail[]
  startTime: string
  endTime?: string
}

/**
 * 字段映射模板（用于选择）
 */
export interface FieldMappingTemplateOption {
  id: number
  templateName: string
  siteCode: string
  dataType: string
  sourceType: string
  isDefault: boolean
  mappingCount: number
}
