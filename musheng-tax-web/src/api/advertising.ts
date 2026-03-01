/**
 * 广告数据API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AdvertisingBill,
  AdvertisingBillItem,
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
 * 获取广告发票列表
 */
export function getAdvertisingList(params: AdvertisingDataQuery) {
  return request.get<PageResult<AdvertisingBill>>(BASE_URL, params)
}

/**
 * 根据ID获取广告发票详情（含明细）
 */
export function getAdvertisingById(id: number) {
  return request.get<AdvertisingBill>(`${BASE_URL}/${id}`)
}

/**
 * 获取广告活动明细列表（全局视图）
 */
export function getAdvertisingItemList(params: {
  invoiceNumber?: string
  campaignId?: string
  campaignName?: string
  page?: number
  size?: number
}) {
  return request.get<PageResult<AdvertisingBillItem>>(`${BASE_URL}/items`, params)
}

/**
 * 删除广告发票（级联删除明细）
 */
export function deleteAdvertising(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除广告数据（逻辑删除）
 */
export function batchDeleteAdvertising(ids: number[]) {
  return request.post<void>(`${BASE_URL}/batch-delete`, ids)
}

/**
 * 批量物理删除广告数据（仅 admin）
 */
export function batchPhysicalDeleteAdvertising(ids: number[]) {
  return request.delete<void>('/api/v1/admin/data-deletion/advertising/batch', { data: ids })
}

/**
 * 按批次删除广告数据
 */
export function deleteAdvertisingByBatch(batchNo: string) {
  return request.delete<void>(`${BASE_URL}/batch/${batchNo}`)
}

/**
 * 获取广告数据汇总（与搜索条件一致）
 */
export function getAdvertisingSummary(params: {
  siteCode?: string
  billingStartDate?: string
  billingEndDate?: string
  invoiceNumber?: string
}) {
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
  const filename = `广告数据_${new Date().toISOString().slice(0, 10)}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/export`, filename, params)
}

/**
 * 下载广告数据导入模板
 */
export function downloadAdvertisingTemplate(marketplaceId?: number) {
  const filename = `广告数据导入模板.xlsx`
  return request.downloadAndSave(`${BASE_URL}/template`, filename, { marketplaceId })
}

/**
 * 批量导入广告数据
 * 支持发票编号去重、时间字段校验
 */
export function importAdvertisingData(data: AdvertisingImportBatchRequest) {
  return request.post<AdvertisingImportResponse>(`${BASE_URL}/import`, data)
}

/**
 * 按条件查询广告发票
 */
export function searchAdvertisingData(params: AdvertisingSearchQuery) {
  const { current, size, ...rest } = params as Record<string, unknown>
  return request.get<PageResult<AdvertisingBill>>(`${BASE_URL}/search`, {
    ...rest,
    page: current ?? 1,
    size: size ?? 20
  } as Record<string, unknown>)
}
