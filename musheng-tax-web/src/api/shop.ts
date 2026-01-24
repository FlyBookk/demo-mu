/**
 * 店铺管理API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { Shop, ShopQuery, ShopForm } from '@/types/shop'

const BASE_URL = '/api/v1/config/shop'

/**
 * 获取店铺列表（分页）
 */
export function getShopList(params: ShopQuery) {
  return request.get<PageResult<Shop>>(`${BASE_URL}/page`, params)
}

/**
 * 根据ID获取店铺详情
 */
export function getShopById(id: number) {
  return request.get<Shop>(`${BASE_URL}/${id}`)
}

/**
 * 创建店铺
 */
export function createShop(data: ShopForm) {
  return request.post<number>(BASE_URL, data)
}

/**
 * 更新店铺
 */
export function updateShop(id: number, data: ShopForm) {
  return request.put<void>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除店铺
 */
export function deleteShop(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 获取所有启用的店铺（下拉选项用）
 */
export function getShopOptions() {
  return request.get<Shop[]>(`${BASE_URL}/options`)
}
