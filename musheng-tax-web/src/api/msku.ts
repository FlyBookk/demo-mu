/**
 * MSKU列表API接口
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'

/** MSKU列表视图 */
export interface MskuListVO {
  id: number
  siteCode: string
  msku: string
  currency: string
  unitPrice: number
  quantity: number
  amount: number
  periodStart: string
  periodEnd: string
  procurementCostCny: number | null
  averageExchangeRate: number | null
}

/** MSKU列表查询参数 */
export interface MskuQueryParams {
  siteCode?: string
  msku?: string
  periodStart?: string
  periodEnd?: string
  pageNum?: number
  pageSize?: number
}

/** MSKU更新请求参数 */
export interface MskuUpdateParams {
  id: number
  unitPrice?: number
  quantity?: number
  procurementCostCny?: number
  averageExchangeRate?: number
}

const BASE_URL = '/api/v1/business/document'

/** 查询MSKU列表（分页） */
export function getMskuList(params: MskuQueryParams) {
  return request.get<PageResult<MskuListVO>>(`${BASE_URL}/msku/list`, params)
}

/** 更新MSKU数据 */
export function updateMsku(data: MskuUpdateParams) {
  return request.put(`${BASE_URL}/msku/update`, data)
}
