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
 * @param cookie 可选，从浏览器复制的 Cookie（curl -b 后的内容）
 */
export function syncRates(params: RateSyncRequest & { cookie?: string }) {
  const searchParams = new URLSearchParams()
  searchParams.append('startDate', params.startDate)
  searchParams.append('endDate', params.endDate)
  if (params.cookie?.trim()) searchParams.append('cookie', params.cookie.trim())
  return request.post<RateSyncResult>(`${BASE_URL}/sync`, searchParams, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

/**
 * 同步指定货币的汇率
 */
export function syncSpecificCurrencies(params: {
  startDate: string
  endDate: string
  currencyCodes: string[]
  cookie?: string
}) {
  const searchParams = new URLSearchParams()
  searchParams.append('startDate', params.startDate)
  searchParams.append('endDate', params.endDate)
  params.currencyCodes.forEach(c => searchParams.append('currencyCodes', c))
  if (params.cookie?.trim()) searchParams.append('cookie', params.cookie.trim())
  return request.post<RateSyncResult>(`${BASE_URL}/sync/currencies`, searchParams, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

/**
 * 同步最近N天的汇率
 * @param cookie 可选，从浏览器复制的 Cookie（curl -b 后的内容），用于绕过中国货币网反爬虫
 */
export function syncRecentDays(days: number, cookie?: string) {
  const searchParams = new URLSearchParams()
  searchParams.append('days', String(days))
  if (cookie?.trim()) searchParams.append('cookie', cookie.trim())
  return request.post<RateSyncResult>(`${BASE_URL}/sync/recent`, searchParams, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
  })
}

/**
 * 通过粘贴 curl 命令同步汇率
 * 从中国货币网 F12 → Network → CcprHisNew → 右键 Copy as cURL
 */
export function syncFromCurl(curl: string) {
  return request.post<RateSyncResult>(`${BASE_URL}/sync/curl`, { curl: curl.trim() })
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
  const filename = `汇率数据_${new Date().toISOString().slice(0, 10)}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/export`, filename, params)
}
