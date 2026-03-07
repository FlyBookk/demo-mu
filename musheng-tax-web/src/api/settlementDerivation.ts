/**
 * 结算推导API接口（按季度）
 * 路径前缀 /api/v1/settlement-derivation
 */

import { request } from '@/utils/request'

const BASE_URL = '/api/v1/settlement-derivation'

/**
 * 执行推导计算并写入（自动删旧+插新，一步到位）
 *
 * @param data 推导请求，包含季度范围和各站点采购成本
 * @returns 推导结果
 */
export function triggerDerivation(data: {
  startQuarter: string
  endQuarter: string
  siteCosts: { siteCode: string; costCny: number }[]
}) {
  return request.post<any>(`${BASE_URL}/derive`, data)
}

/**
 * 检查指定季度范围是否已有推导数据
 *
 * @param params 季度范围参数
 * @returns 是否已存在推导数据
 */
export function checkExistingData(params: {
  startQuarter: string
  endQuarter: string
}) {
  return request.get<boolean>(`${BASE_URL}/check-existing`, params)
}
