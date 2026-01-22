/**
 * 导入记录API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { ImportRecord, ImportRecordQuery, ImportRecordDetail } from '@/types/importRecord'

const BASE_URL = '/api/v1/config/import-records'

/**
 * 获取导入记录列表
 */
export function getImportRecordList(params: ImportRecordQuery) {
  return request.get<PageResult<ImportRecord>>(BASE_URL, params)
}

/**
 * 根据ID获取导入记录详情
 */
export function getImportRecordById(id: number) {
  return request.get<ImportRecordDetail>(`${BASE_URL}/${id}`)
}

/**
 * 根据批次号获取导入记录详情
 */
export function getImportRecordByBatchNo(batchNo: string) {
  return request.get<ImportRecordDetail>(`${BASE_URL}/batch/${batchNo}`)
}

/**
 * 删除导入记录
 */
export function deleteImportRecord(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除导入记录
 */
export function batchDeleteImportRecord(ids: number[]) {
  return request.delete<void>(BASE_URL, { data: { ids } })
}

/**
 * 下载错误报告
 */
export function downloadErrorReport(id: number) {
  const filename = `导入错误报告_${id}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/${id}/error-report`, filename)
}

/**
 * 重新导入（重试失败的记录）
 */
export function retryImport(id: number) {
  return request.post<ImportRecord>(`${BASE_URL}/${id}/retry`)
}

/**
 * 获取导入进度
 */
export function getImportProgress(batchNo: string) {
  return request.get<{
    batchNo: string
    status: number
    progress: number
    processedRows: number
    totalRows: number
  }>(`${BASE_URL}/progress/${batchNo}`)
}
