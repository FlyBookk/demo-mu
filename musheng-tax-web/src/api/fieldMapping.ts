/**
 * 字段映射模板API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  FieldMappingTemplate,
  FieldMappingTemplateQuery,
  FieldMappingTemplateForm,
  CopyTemplateParams,
  TargetFieldDefinition,
  FieldMappingDataType
} from '@/types/fieldMapping'
import type {
  TargetFieldsResponse,
  FilePreviewResponse,
  ParseFieldsResponse,
  AutoMatchRequest,
  AutoMatchResponse,
  SalesSubType
} from '@/components/business/FieldMappingCanvas/types'

const BASE_URL = '/api/v1/config/field-mapping-templates'

/**
 * 获取字段映射模板列表
 */
export function getFieldMappingTemplateList(params: FieldMappingTemplateQuery) {
  return request.get<PageResult<FieldMappingTemplate>>(BASE_URL, params)
}

/**
 * 根据ID获取字段映射模板详情
 */
export function getFieldMappingTemplateById(id: number) {
  return request.get<FieldMappingTemplate>(`${BASE_URL}/${id}`)
}

/**
 * 创建字段映射模板
 */
export function createFieldMappingTemplate(data: FieldMappingTemplateForm) {
  return request.post<FieldMappingTemplate>(BASE_URL, data)
}

/**
 * 更新字段映射模板
 */
export function updateFieldMappingTemplate(id: number, data: FieldMappingTemplateForm) {
  return request.put<FieldMappingTemplate>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除字段映射模板
 */
export function deleteFieldMappingTemplate(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除字段映射模板
 */
export function batchDeleteFieldMappingTemplate(ids: number[]) {
  return request.delete<void>(BASE_URL, { data: { ids } })
}

/**
 * 复制字段映射模板
 */
export function copyFieldMappingTemplate(id: number, newName: string) {
  return request.post<FieldMappingTemplate>(`${BASE_URL}/${id}/copy`, null, {
    params: { newName }
  })
}

/**
 * 设置默认模板
 */
export function setDefaultTemplate(id: number) {
  return request.put<void>(`${BASE_URL}/${id}/default`)
}

/**
 * 获取目标字段定义（按数据类型）
 */
export function getTargetFieldDefinitions(dataType: FieldMappingDataType) {
  return request.get<TargetFieldDefinition[]>(`${BASE_URL}/target-fields/${dataType}`)
}

/**
 * 获取所有启用的模板（按数据类型）
 */
export function getEnabledTemplates(dataType: FieldMappingDataType) {
  return request.get<FieldMappingTemplate[]>(`${BASE_URL}/enabled`, { dataType })
}

/**
 * 验证模板映射配置
 */
export function validateTemplateMapping(data: FieldMappingTemplateForm) {
  return request.post<{ valid: boolean; errors?: string[] }>(`${BASE_URL}/validate`, data)
}

// ==================== 画布相关接口 ====================

/**
 * 获取目标字段定义（增强版，支持数据源类型）
 * @param dataType 数据类型
 * @param sourceType 数据源类型（仅销售数据有效）：ORIGINAL/ERP
 */
export function getTargetFields(dataType: FieldMappingDataType, sourceType?: SalesSubType) {
  return request.get<TargetFieldsResponse>(`${BASE_URL}/target-fields/${dataType}`, { sourceType })
}

/**
 * 预览文件
 * @param file 文件
 * @param previewRows 预览行数
 */
export function previewFile(file: File, previewRows: number = 10) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('previewRows', String(previewRows))

  return request.post<FilePreviewResponse>(`${BASE_URL}/preview-file`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 解析文件获取源字段
 * @param file 文件
 * @param headerRow 表头行号
 * @param sheetName Sheet名称（Excel可选）
 */
export function parseFileFields(file: File, headerRow: number = 1, sheetName?: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('headerRow', String(headerRow))
  if (sheetName) {
    formData.append('sheetName', sheetName)
  }

  return request.post<ParseFieldsResponse>(`${BASE_URL}/parse-fields`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 智能匹配建议
 * @param params 匹配请求参数
 */
export function autoMatchFields(params: AutoMatchRequest) {
  return request.post<AutoMatchResponse>(`${BASE_URL}/auto-match`, params)
}

/**
 * 刷新目标字段缓存
 * 重新扫描实体类，刷新目标字段定义缓存
 * @param dataType 数据类型（可选，不传则刷新全部）
 */
export function refreshTargetFieldsCache(dataType?: FieldMappingDataType) {
  return request.post<void>(`${BASE_URL}/target-fields/refresh`, null, {
    params: dataType ? { dataType } : undefined
  })
}
