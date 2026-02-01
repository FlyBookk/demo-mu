/**
 * FBA货件API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  FbaShipment,
  FbaShipmentQuery,
  FbaShipmentImportResult,
  FbaShipmentBatchImportResult,
  FbaShipmentSummary,
  FbaShipmentItem,
  FbaShipmentItemQuery,
  // 旧类型（兼容性）
  FbaShipmentDetail,
  FbaShipmentDetailQuery
} from '@/types/fbaShipment'

const BASE_URL = '/api/v1/business/fba-shipment'

/**
 * 导入FBA货件（Excel文件）
 */
export function importFbaShipment(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.upload<FbaShipmentImportResult>(`${BASE_URL}/import`, formData)
}

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
 * 批量删除FBA货件
 */
export function batchDeleteFbaShipment(ids: number[]) {
  return request.post<void>(`${BASE_URL}/batch-delete`, ids)
}

/**
 * 获取FBA货件统计汇总
 */
export function getFbaShipmentSummary(params?: FbaShipmentQuery) {
  return request.get<FbaShipmentSummary>(`${BASE_URL}/summary`, params)
}

/**
 * 导出FBA货件
 */
export function exportFbaShipmentData(params?: FbaShipmentQuery) {
  const filename = `FBA货件_${new Date().toISOString().slice(0, 10)}.xlsx`
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

/**
 * 获取店铺名称列表（动态）
 */
export function getFbaShipmentShopNames() {
  return request.get<string[]>(`${BASE_URL}/shop-names`)
}

// ========== 以下为旧API接口，保留用于兼容性 ==========

/**
 * 新增FBA货件明细（旧）
 */
export function addFbaShipment(data: Partial<FbaShipmentDetail>) {
  return request.post<number>(BASE_URL, data)
}

/**
 * 更新FBA货件明细（旧）
 */
export function updateFbaShipment(id: number, data: Partial<FbaShipmentDetail>) {
  return request.put<void>(`${BASE_URL}/${id}`, data)
}
