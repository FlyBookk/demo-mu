/**
 * 汇率管理API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  RateData,
  RateDataQuery,
  RateRequest,
  RateConvertRequest,
  RateConvertResult,
  RateSyncRequest,
  RateSyncResult
} from '@/types/rate'

const BASE_URL = '/api/v1/business/rates'

/**
 * 获取汇率列表
 */
export function getRateList(params: RateDataQuery) {
  return request.get<PageResult<RateData>>(BASE_URL, params)
}

/**
 * 根据ID获取汇率详情
 */
export function getRateById(id: number) {
  return request.get<RateData>(`${BASE_URL}/${id}`)
}

/**
 * 新增汇率（手动录入）
 */
export function createRate(data: RateRequest) {
  return request.post<RateData>(BASE_URL, data)
}

/**
 * 修改汇率
 */
export function updateRate(id: number, data: RateRequest) {
  return request.put<RateData>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除汇率数据
 */
export function deleteRate(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除汇率数据
 */
export function batchDeleteRates(ids: number[]) {
  return request.delete<void>(BASE_URL, { data: ids })
}

/**
 * 查询指定日期的汇率
 */
export function getRateByDate(
  currencyCode: string,
  date: string
) {
  return request.get<number>(`${BASE_URL}/query`, {
    currencyCode,
    date
  })
}

/**
 * 货币转换
 */
export function convertCurrency(data: RateConvertRequest) {
  return request.post<RateConvertResult>(`${BASE_URL}/convert`, data)
}

/**
 * 同步汇率 - 指定日期范围
 */
export function syncRates(params: RateSyncRequest) {
  return request.post<RateSyncResult>(`${BASE_URL}/sync`, params)
}

/**
 * 同步指定货币的汇率
 */
export function syncSpecificCurrencies(params: {
  startDate: string
  endDate: string
  currencyCodes: string[]
}) {
  return request.post<RateSyncResult>(`${BASE_URL}/sync/currencies`, params)
}

/**
 * 同步最近N天的汇率
 */
export function syncRecentDays(days: number) {
  return request.post<RateSyncResult>(`${BASE_URL}/sync/recent`, { days })
}

/**
 * 导入汇率数据（Excel 或 CSV 文件）
 */
export function importRateData(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<{
    totalCount: number
    successCount: number
    failCount: number
    errors: string[]
  }>(`${BASE_URL}/import`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 导出汇率数据
 */
export function exportRateData(params: RateDataQuery) {
  return request.download(`${BASE_URL}/export`, params)
}
