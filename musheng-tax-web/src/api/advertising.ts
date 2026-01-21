/**
 * 广告数据API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AdvertisingData,
  AdvertisingDataQuery,
  AdvertisingDataForm,
  AdvertisingSummary,
  AdvertisingStatByType,
  AdvertisingImportBatchRequest,
  AdvertisingImportResponse,
  AdvertisingSearchQuery
} from '@/types/advertising'

const BASE_URL = '/api/v1/advertising'

/**
 * 获取广告数据列表
 */
export function getAdvertisingList(params: AdvertisingDataQuery) {
  return request.get<PageResult<AdvertisingData>>(BASE_URL, params)
}

/**
 * 根据ID获取广告数据详情
 */
export function getAdvertisingById(id: number) {
  return request.get<AdvertisingData>(`${BASE_URL}/${id}`)
}

/**
 * 创建广告数据
 */
export function createAdvertising(data: AdvertisingDataForm) {
  return request.post<AdvertisingData>(BASE_URL, data)
}

/**
 * 更新广告数据
 */
export function updateAdvertising(id: number, data: AdvertisingDataForm) {
  return request.put<AdvertisingData>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除广告数据
 */
export function deleteAdvertising(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除广告数据
 */
export function batchDeleteAdvertising(ids: number[]) {
  return request.delete<void>(BASE_URL, { data: { ids } })
}

/**
 * 按批次删除广告数据
 */
export function deleteAdvertisingByBatch(batchNo: string) {
  return request.delete<void>(`${BASE_URL}/batch/${batchNo}`)
}

/**
 * 获取广告数据汇总
 */
export function getAdvertisingSummary(params: AdvertisingDataQuery) {
  return request.get<AdvertisingSummary>(`${BASE_URL}/summary`, params)
}

/**
 * 获取广告数据按类型统计
 */
export function getAdvertisingStatByType(params: AdvertisingDataQuery) {
  return request.get<AdvertisingStatByType[]>(`${BASE_URL}/stat-by-type`, params)
}

/**
 * 导出广告数据
 */
export function exportAdvertisingData(params: AdvertisingDataQuery) {
  return request.download(`${BASE_URL}/export`, params)
}

/**
 * 下载广告数据导入模板
 */
export function downloadAdvertisingTemplate(marketplaceId?: number) {
  return request.download(`${BASE_URL}/template`, { marketplaceId })
}

/**
 * 批量导入广告数据
 * 支持发票编号去重、时间字段校验
 */
export function importAdvertisingData(data: AdvertisingImportBatchRequest) {
  return request.post<AdvertisingImportResponse>(`${BASE_URL}/import`, data)
}

/**
 * 按条件查询广告数据
 * 支持按账单周期、发票编号等条件查询
 */
export function searchAdvertisingData(params: AdvertisingSearchQuery) {
  return request.get<PageResult<AdvertisingData>>(`${BASE_URL}/search`, params)
}
