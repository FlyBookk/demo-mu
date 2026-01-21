/**
 * FBA货件明细类型定义
 * 与后端 t_fba_shipment_detail 表结构对齐
 */

// FBA货件明细
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

// FBA货件明细查询参数
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

// FBA货件导入结果
export interface FbaShipmentImportResult {
  totalCount: number
  successCount: number
  failCount: number
  duplicateCount: number
  errors: string[]
  batchNo: string
}

// FBA货件统计汇总
export interface FbaShipmentSummary {
  totalShipments: number
  totalSkuCount: number
  totalExpectedQuantity: number
  totalFoundQuantity: number
}

// FBA货件状态选项
export const FbaShipmentStatusOptions = [
  { label: '已完成', value: '已完成' },
  { label: '接受中', value: '接受中' },
  { label: '已结账', value: '已结账' },
  { label: '配送中', value: '配送中' },
  { label: '测试中', value: '测试中' }
]
