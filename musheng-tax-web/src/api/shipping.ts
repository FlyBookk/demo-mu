/**
 * 配送数据API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { ShippingData, ShippingDataQuery, ShippingImportParams, ShippingSummary } from '@/types/shipping'
import type { ImportRecord } from '@/types/importRecord'

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
 * 导入配送数据
 */
export function importShippingData(data: ShippingImportParams) {
  return request.post<ImportRecord>(`${BASE_URL}/import`, data)
}

/**
 * 删除配送数据
 */
export function deleteShippingData(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除配送数据
 */
export function batchDeleteShippingData(ids: number[]) {
  return request.post<void>(`${BASE_URL}/batch-delete`, ids)
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
  return request.download(`${BASE_URL}/export`, params)
}

/**
 * 下载配送数据导入模板
 */
export function downloadShippingTemplate(marketplaceId?: number) {
  return request.download(`${BASE_URL}/template`, { marketplaceId })
}
