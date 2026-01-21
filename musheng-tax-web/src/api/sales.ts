/**
 * 销售数据API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { SalesData, SalesDataQuery, SalesImportParams, SalesSummary, SalesStatByType } from '@/types/sales'
import type { ImportRecord } from '@/types/importRecord'

const BASE_URL = '/api/v1/business/sales'

/**
 * 获取销售数据列表
 */
export function getSalesList(params: SalesDataQuery) {
  return request.get<PageResult<SalesData>>(BASE_URL, params)
}

/**
 * 根据ID获取销售数据详情
 */
export function getSalesById(id: number) {
  return request.get<SalesData>(`${BASE_URL}/${id}`)
}

/**
 * 导入销售数据
 */
export function importSalesData(data: SalesImportParams) {
  return request.post<ImportRecord>(`${BASE_URL}/import`, data)
}

/**
 * 删除销售数据
 */
export function deleteSalesData(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除销售数据
 */
export function batchDeleteSalesData(ids: number[]) {
  return request.post<void>(`${BASE_URL}/batch-delete`, ids)
}

/**
 * 按批次删除销售数据
 */
export function deleteSalesByBatch(batchNo: string) {
  return request.delete<void>(`${BASE_URL}/batch/${batchNo}`)
}

/**
 * 获取销售数据汇总
 */
export function getSalesSummary(params: SalesDataQuery) {
  return request.get<SalesSummary>(`${BASE_URL}/summary`, params)
}

/**
 * 获取销售数据按交易类型统计
 */
export function getSalesStatByType(params: SalesDataQuery) {
  return request.get<SalesStatByType[]>(`${BASE_URL}/stat-by-type`, params)
}

/**
 * 导出销售数据
 */
export function exportSalesData(params: SalesDataQuery) {
  return request.download(`${BASE_URL}/export`, params)
}

/**
 * 下载销售数据导入模板
 */
export function downloadSalesTemplate(marketplaceId?: number) {
  return request.download(`${BASE_URL}/template`, { marketplaceId })
}
