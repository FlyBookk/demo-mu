/**
 * 配送数据API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { ShippingData, ShippingDataQuery, ShippingBatchImportResult, ShippingSummary } from '@/types/shipping'

const BASE_URL = '/api/v1/business/shipping'

/**
 * 获取配送数据列表
 */
export function getShippingList(params: ShippingDataQuery) {
  return request.get<PageResult<ShippingData>>(BASE_URL, params)
}

/**
 * 根据ID获取配送数据详情
 */
export function getShippingById(id: number) {
  return request.get<ShippingData>(`${BASE_URL}/${id}`)
}

/**
 * 导入配送数据（单个文件）
 */
export function importShippingData(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.upload<Record<string, unknown>>(`${BASE_URL}/import`, formData)
}

/**
 * 批量导入配送数据（多个文件）
 */
export function batchImportShippingData(files: File[]) {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  return request.upload<ShippingBatchImportResult>(`${BASE_URL}/batch-import`, formData)
}

/**
 * 删除配送数据
 */
export function deleteShippingData(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除配送数据（逻辑删除）
 */
export function batchDeleteShippingData(ids: number[]) {
  return request.post<void>(`${BASE_URL}/batch-delete`, ids)
}

/**
 * 批量物理删除配送数据（仅 admin）
 */
export function batchPhysicalDeleteShippingData(ids: number[]) {
  return request.delete<void>('/api/v1/admin/data-deletion/shipping/batch', { data: ids })
}

/**
 * 按批次删除配送数据
 */
export function deleteShippingByBatch(batchNo: string) {
  return request.delete<void>(`${BASE_URL}/batch/${batchNo}`)
}

/**
 * 获取配送数据汇总
 */
export function getShippingSummary(params: ShippingDataQuery) {
  return request.get<ShippingSummary>(`${BASE_URL}/summary`, params)
}

/**
 * 导出配送数据
 */
export function exportShippingData(params: ShippingDataQuery) {
  const filename = `配送数据_${new Date().toISOString().slice(0, 10)}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/export`, filename, params)
}

/**
 * 下载配送数据导入模板
 */
export function downloadShippingTemplate(marketplaceId?: number) {
  const filename = `配送数据导入模板.xlsx`
  return request.downloadAndSave(`${BASE_URL}/template`, filename, { marketplaceId })
}
