/**
 * FBA货件类型定义
 * 与后端新表结构对齐（t_fba_shipment + t_fba_shipment_item）
 */

// FBA货件主表
export interface FbaShipment {
  id: number
  shopId: number
  shipmentId: string
  warehouseCode?: string
  shopName?: string
  country?: string
  createdDate?: string
  skuCount?: number
  totalQuantity?: number
  importBatchId?: number
  createTime: string
  updateTime?: string
  createBy?: number
  updateBy?: number
  // 关联的SKU明细列表
  items?: FbaShipmentItem[]
}

// FBA货件明细（SKU级别）
export interface FbaShipmentItem {
  id: number
  shopId: number
  shipmentId: number
  shipmentNo: string
  sku: string
  msku?: string
  quantity: number
  importBatchId?: number
  createTime: string
  updateTime?: string
  createBy?: number
  updateBy?: number
}

// FBA货件查询参数
export interface FbaShipmentQuery {
  shipmentId?: string
  shopName?: string
  country?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

// FBA货件导入结果
export interface FbaShipmentImportResult {
  batchNo: string
  totalCount: number        // 总SKU数
  successCount: number      // 成功导入的SKU数
  failCount: number         // 失败的SKU数
  duplicateCount: number    // 重复的SKU数（已跳过）
  duplicateShipmentCount?: number  // 重复的货件数
  shipmentCount: number     // 成功导入的货件数
  errors: string[]
}

// FBA货件批量导入结果
export interface FbaShipmentBatchImportResult {
  totalFiles: number        // 总文件数
  successFiles: number      // 成功文件数
  failFiles: number         // 失败文件数
  totalSkuCount: number     // 总SKU数
  successSkuCount: number   // 成功SKU数
  failSkuCount: number      // 失败SKU数
  duplicateSkuCount: number // 重复SKU数（已跳过）
  totalShipmentCount: number // 总货件数
  fileResults: Array<{
    fileName: string
    status: 'success' | 'fail'
    message?: string
    result?: FbaShipmentImportResult
  }>
}

// FBA货件明细查询参数
export interface FbaShipmentItemQuery {
  shipmentNo?: string
  sku?: string
  msku?: string
  shopName?: string
  country?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

// FBA货件统计汇总
export interface FbaShipmentSummary {
  totalShipments: number    // 总货件数
  totalSkuCount: number     // 总SKU种类数
  totalQuantity: number     // 总发货量
}

// 国家选项（可根据实际业务扩展）
export const FbaShipmentCountryOptions = [
  { label: '英国', value: '英国' },
  { label: '美国', value: '美国' },
  { label: '加拿大', value: '加拿大' },
  { label: '德国', value: '德国' },
  { label: '法国', value: '法国' },
  { label: '日本', value: '日本' }
]

// ========== 以下为旧类型定义，保留用于兼容性 ==========

// FBA货件明细（旧）
export interface FbaShipmentDetail {
  id: number
  shipmentName: string
  shipmentId: string
  createdDate?: string
  lastUpdated?: string
  receivingAddress?: string
  skuCount?: number
  expectedQuantity?: number
  foundQuantity?: number
  status?: string
  importBatchId?: number
  createTime: string
  updateTime?: string
  createBy?: number
  updateBy?: number
}

// FBA货件明细查询参数（旧）
export interface FbaShipmentDetailQuery {
  shipmentId?: string
  shipmentName?: string
  status?: string
  receivingAddress?: string
  startDate?: string
  endDate?: string
  importBatchId?: number
  page?: number
  size?: number
}

// FBA货件状态选项（旧）
export const FbaShipmentStatusOptions = [
  { label: '已完成', value: '已完成' },
  { label: '接受中', value: '接受中' },
  { label: '已结账', value: '已结账' },
  { label: '配送中', value: '配送中' },
  { label: '测试中', value: '测试中' }
]
