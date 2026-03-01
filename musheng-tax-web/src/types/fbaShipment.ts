/**
 * FBA货件类型定义 - V2
 * 适配新CSV格式
 */

export interface FbaShipment {
  id: number
  shopId: number
  shipmentId: string
  shipmentName?: string
  status?: string
  warehouseCode?: string
  shopName?: string
  country?: string
  state?: string
  city?: string
  streetAddress?: string
  houseNumber?: string
  createdDate?: string
  updatedDate?: string
  skuCount?: number
  totalQuantity?: number
  totalReceivedQuantity?: number
  recipient?: string
  postalCode?: string
  importBatchId?: number
  createTime: string
  updateTime?: string
  items?: FbaShipmentItem[]
}

export interface FbaShipmentItem {
  id: number
  shopId: number
  shipmentId: number
  shipmentNo: string
  sku?: string
  msku: string
  quantity: number
  receivedQuantity?: number
  importBatchId?: number
  createTime: string
  updateTime?: string
}

export interface FbaShipmentQuery {
  shipmentId?: string
  status?: string
  country?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export interface FbaShipmentImportResult {
  batchNo: string
  totalCount: number
  successCount: number
  failCount: number
  duplicateCount: number
  duplicateShipmentCount?: number
  shipmentCount: number
  errors: string[]
}

export interface FbaShipmentBatchImportResult {
  totalFiles: number
  successFiles: number
  failFiles: number
  totalSkuCount: number
  successSkuCount: number
  failSkuCount: number
  duplicateSkuCount: number
  totalShipmentCount: number
  fileResults: Array<{
    fileName: string
    status: 'success' | 'fail'
    message?: string
    result?: FbaShipmentImportResult
  }>
}

export interface FbaShipmentItemQuery {
  shipmentNo?: string
  msku?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export interface FbaShipmentSummary {
  totalShipments: number
  totalSkuCount: number
  totalQuantity: number
}

export const FbaShipmentStatusOptions = [
  { label: 'CLOSED', value: 'CLOSED' },
  { label: 'WORKING', value: 'WORKING' },
  { label: 'SHIPPED', value: 'SHIPPED' },
  { label: 'IN_TRANSIT', value: 'IN_TRANSIT' },
  { label: 'RECEIVING', value: 'RECEIVING' },
  { label: 'CHECKED_IN', value: 'CHECKED_IN' }
]
