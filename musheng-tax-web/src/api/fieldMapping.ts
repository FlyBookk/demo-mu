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
