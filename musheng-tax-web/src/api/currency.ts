/**
 * 货币管理API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { Currency, CurrencyQuery, CurrencyForm } from '@/types/currency'

const BASE_URL = '/api/v1/config/currencies'

/**
 * 获取货币列表（分页）
 */
export function getCurrencyList(params: CurrencyQuery) {
  return request.get<PageResult<Currency>>(BASE_URL, params)
}

/**
 * 根据ID获取货币详情
 */
export function getCurrencyById(id: number) {
  return request.get<Currency>(`${BASE_URL}/${id}`)
}

/**
 * 创建货币
 */
export function createCurrency(data: CurrencyForm) {
  return request.post<Currency>(BASE_URL, data)
}

/**
 * 更新货币
 */
export function updateCurrency(id: number, data: CurrencyForm) {
  return request.put<Currency>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除货币
 */
export function deleteCurrency(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除货币
 */
export function batchDeleteCurrency(ids: number[]) {
  return request.delete<void>(BASE_URL, { data: { ids } })
}

/**
 * 获取所有启用的货币（下拉选项用）
 */
export function getEnabledCurrencies() {
  return request.get<Currency[]>(`${BASE_URL}/enabled`)
}
