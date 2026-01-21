/**
 * FBA货件明细API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  FbaShipmentDetail,
  FbaShipmentDetailQuery,
  FbaShipmentImportResult,
  FbaShipmentSummary
} from '@/types/fbaShipment'

const BASE_URL = '/api/v1/business/fba-shipment'

/**
 * 获取FBA货件明细列表
 */
export function getFbaShipmentList(params: FbaShipmentDetailQuery) {
  return request.get<PageResult<FbaShipmentDetail>>(BASE_URL, params)
}

/**
 * 根据ID获取FBA货件明细详情
 */
export function getFbaShipmentById(id: number) {
  return request.get<FbaShipmentDetail>(`${BASE_URL}/${id}`)
}

/**
 * 新增FBA货件明细
 */
export function addFbaShipment(data: Partial<FbaShipmentDetail>) {
  return request.post<number>(BASE_URL, data)
}

/**
 * 更新FBA货件明细
 */
export function updateFbaShipment(id: number, data: Partial<FbaShipmentDetail>) {
  return request.put<void>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除FBA货件明细
 */
export function deleteFbaShipment(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除FBA货件明细
 */
export function batchDeleteFbaShipment(ids: number[]) {
  return request.post<void>(`${BASE_URL}/batch-delete`, ids)
}

/**
 * 获取FBA货件统计汇总
 */
export function getFbaShipmentSummary(params?: FbaShipmentDetailQuery) {
  return request.get<FbaShipmentSummary>(`${BASE_URL}/summary`, params)
}

/**
 * 导出FBA货件明细
 */
export function exportFbaShipmentData(params?: FbaShipmentDetailQuery) {
  return request.download(`${BASE_URL}/export`, params)
}
