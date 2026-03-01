/**
 * FBA货件API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  FbaShipment,
  FbaShipmentQuery,
  FbaShipmentBatchImportResult,
  FbaShipmentSummary,
  FbaShipmentItem,
  FbaShipmentItemQuery
} from '@/types/fbaShipment'

const BASE_URL = '/api/v1/business/fba-shipment'

/**
 * 批量导入FBA货件（多个Excel文件，支持幂等性）
 */
export function batchImportFbaShipment(files: File[]) {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  return request.upload<FbaShipmentBatchImportResult>(`${BASE_URL}/batch-import`, formData)
}

/**
 * 获取FBA货件列表
 */
export function getFbaShipmentList(params: FbaShipmentQuery) {
  return request.get<PageResult<FbaShipment>>(`${BASE_URL}/list`, params)
}

/**
 * 根据ID获取FBA货件详情（包含SKU明细）
 */
export function getFbaShipmentById(id: number) {
  return request.get<FbaShipment>(`${BASE_URL}/${id}`)
}

/**
 * 删除FBA货件
 */
export function deleteFbaShipment(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除FBA货件（逻辑删除）
 */
export function batchDeleteFbaShipment(ids: number[]) {
  return request.post<void>(`${BASE_URL}/batch-delete`, ids)
}

/**
 * 批量物理删除FBA货件（仅 admin）
 */
export function batchPhysicalDeleteFbaShipment(ids: number[]) {
  return request.delete<void>('/api/v1/admin/data-deletion/fba-shipment/batch', { data: ids })
}

/**
 * 获取FBA货件统计汇总
 */
export function getFbaShipmentSummary(params?: FbaShipmentQuery) {
  return request.get<FbaShipmentSummary>(`${BASE_URL}/summary`, params)
}

/**
 * 导出FBA货件（CSV格式，与导入文档格式一致）
 */
export function exportFbaShipmentData(params?: FbaShipmentQuery) {
  const filename = `慕声FBA发货明细数据_${new Date().toISOString().slice(0, 10)}.csv`
  return request.downloadAndSave(`${BASE_URL}/export`, filename, params)
}

/**
 * 获取FBA货件SKU明细列表（全局视图）
 */
export function getFbaShipmentItemList(params: FbaShipmentItemQuery) {
  return request.get<PageResult<FbaShipmentItem>>('/api/v1/business/fba-shipment-item/list', params)
}

/**
 * 获取国家列表（动态）
 */
export function getFbaShipmentCountries() {
  return request.get<string[]>(`${BASE_URL}/countries`)
}
