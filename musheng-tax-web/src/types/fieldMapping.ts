/**
 * 字段映射模板类型定义
 */

// 字段映射项
export interface FieldMappingItem {
  sourceField: string
  targetField: string
  required: boolean
  defaultValue?: string
  transformer?: string
}

// 字段映射模板
// 与后端 t_field_mapping_template 表结构对齐
export interface FieldMappingTemplate {
  id: number
  templateName: string
  siteCode: string
  dataType: FieldMappingDataType
  mappingConfig: FieldMappingItem[]  // JSON格式存储
  isDefault: boolean
  createBy?: number
  createTime: string
  updateBy?: number
  updateTime?: string
}

// 字段映射数据类型
export type FieldMappingDataType = 'SALES' | 'SHIPPING' | 'ADVERTISING' | 'RATE'

// 字段映射模板查询参数
export interface FieldMappingTemplateQuery {
  keyword?: string
  siteCode?: string
  dataType?: FieldMappingDataType
  page?: number
  size?: number
}

// 字段映射模板创建/更新参数
export interface FieldMappingTemplateForm {
  id?: number
  templateName: string
  siteCode: string
  dataType: FieldMappingDataType
  mappingConfig: FieldMappingItem[]
  isDefault?: boolean
}

// 复制模板参数
export interface CopyTemplateParams {
  sourceId: number
  newName: string
}

// 目标字段定义
export interface TargetFieldDefinition {
  field: string
  label: string
  type: 'string' | 'number' | 'date' | 'boolean'
  required: boolean
  description?: string
}
