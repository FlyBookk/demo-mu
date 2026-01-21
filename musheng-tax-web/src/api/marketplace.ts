/**
 * 站点管理API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { Marketplace, MarketplaceQuery, MarketplaceForm } from '@/types/marketplace'

const BASE_URL = '/api/v1/config/marketplaces'

/**
 * 获取站点列表
 */
export function getMarketplaceList(params: MarketplaceQuery) {
  return request.get<PageResult<Marketplace>>(BASE_URL, params)
}

/**
 * 根据ID获取站点详情
 */
export function getMarketplaceById(id: number) {
  return request.get<Marketplace>(`${BASE_URL}/${id}`)
}

/**
 * 创建站点
 */
export function createMarketplace(data: MarketplaceForm) {
  return request.post<Marketplace>(BASE_URL, data)
}

/**
 * 更新站点
 */
export function updateMarketplace(id: number, data: MarketplaceForm) {
  return request.put<Marketplace>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除站点
 */
export function deleteMarketplace(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除站点
 */
export function batchDeleteMarketplace(ids: number[]) {
  return request.delete<void>(BASE_URL, { data: { ids } })
}

/**
 * 获取所有启用的站点（下拉选项用）
 */
export function getEnabledMarketplaces() {
  return request.get<Marketplace[]>(`${BASE_URL}/enabled`)
}

/**
 * 根据区域获取站点列表
 */
export function getMarketplacesByRegion(region: string) {
  return request.get<Marketplace[]>(`${BASE_URL}/region/${region}`)
}
