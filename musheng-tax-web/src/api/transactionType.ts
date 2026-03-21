/**
 * 交易类型映射API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { TransactionTypeMapping, TransactionTypeMappingQuery, TransactionTypeMappingForm } from '@/types/transactionType'

const BASE_URL = '/api/v1/config/transaction-type-mappings'

/**
 * 获取交易类型映射列表
 */
export function getTransactionTypeMappingList(params: TransactionTypeMappingQuery) {
  return request.get<PageResult<TransactionTypeMapping>>(BASE_URL, params)
}

/**
 * 根据ID获取交易类型映射详情
 */
export function getTransactionTypeMappingById(id: number) {
  return request.get<TransactionTypeMapping>(`${BASE_URL}/${id}`)
}

/**
 * 创建交易类型映射
 */
export function createTransactionTypeMapping(data: TransactionTypeMappingForm) {
  return request.post<TransactionTypeMapping>(BASE_URL, data)
}

/**
 * 更新交易类型映射
 */
export function updateTransactionTypeMapping(id: number, data: TransactionTypeMappingForm) {
  return request.put<TransactionTypeMapping>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除交易类型映射
 */
export function deleteTransactionTypeMapping(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除交易类型映射
 */
export function batchDeleteTransactionTypeMapping(ids: number[]) {
  return request.delete<void>(BASE_URL, { data: { ids } })
}

/**
 * 获取标准分类枚举列表（从后端动态获取）
 */
export function getTransactionCategories() {
  return request.get<{ value: string; label: string }[]>(`${BASE_URL}/categories`)
}

/**
 * 获取所有目标交易类型（下拉选项用）
 */
export function getTargetTransactionTypes() {
  return request.get<string[]>(`${BASE_URL}/target-types`)
}

/**
 * 根据源类型查询映射
 */
export function getMappingBySourceType(sourceType: string) {
  return request.get<TransactionTypeMapping>(`${BASE_URL}/source/${encodeURIComponent(sourceType)}`)
}
