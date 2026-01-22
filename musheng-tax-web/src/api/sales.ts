/**
 * 销售数据API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { 
  SalesData, 
  SalesDataQuery, 
  SalesImportParams, 
  SalesSummary, 
  SalesStatByType,
  SalesSourceType,
  SalesUploadResult,
  SalesPreviewParams,
  SalesPreviewResult,
  SalesDualImportParams,
  SalesImportResult,
  SalesImportProgress,
  FieldMappingTemplateOption
} from '@/types/sales'
import type { ImportRecord } from '@/types/importRecord'

const BASE_URL = '/api/v1/business/sales'

/**
 * 获取销售数据列表
 */
export function getSalesList(params: SalesDataQuery) {
  return request.post<PageResult<SalesData>>(`${BASE_URL}/list`, params)
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
  const filename = `销售数据_${new Date().toISOString().slice(0, 10)}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/export`, filename, params)
}

/**
 * 下载销售数据导入模板
 */
export function downloadSalesTemplate(marketplaceId?: number) {
  const filename = `销售数据导入模板.xlsx`
  return request.downloadAndSave(`${BASE_URL}/template`, filename, { marketplaceId })
}

// ========== 双格式导入相关接口 ==========

/**
 * 上传销售数据文件
 * 解析文件表头，返回源字段列表和样例数据
 */
export function uploadSalesFile(file: File, sourceType: SalesSourceType, siteCode?: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('sourceType', sourceType)
  if (siteCode) {
    formData.append('siteCode', siteCode)
  }
  return request.post<SalesUploadResult>(`${BASE_URL}/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 预览导入数据
 * 根据选择的模板解析数据，返回前10行预览
 */
export function previewSalesImport(params: SalesPreviewParams) {
  return request.post<SalesPreviewResult>(`${BASE_URL}/preview`, params)
}

/**
 * 执行双格式导入
 * 根据模板配置解析并导入数据
 */
export function executeSalesImport(params: SalesDualImportParams) {
  return request.post<SalesImportResult>(`${BASE_URL}/import/execute`, params)
}

/**
 * 获取导入进度
 */
export function getSalesImportProgress(batchNo: string) {
  return request.get<SalesImportProgress>(`${BASE_URL}/import/progress/${batchNo}`)
}

/**
 * 获取字段映射模板列表（按类型筛选）
 */
export function getTemplatesByType(dataType: string, sourceType?: string, siteCode?: string) {
  return request.get<FieldMappingTemplateOption[]>('/api/v1/config/field-mapping-templates/by-type', {
    dataType,
    sourceType,
    siteCode
  })
}
