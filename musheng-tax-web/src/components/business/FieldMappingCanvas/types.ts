/**
 * 字段映射画布组件 - 类型定义
 */

// ==================== 数据类型枚举 ====================

/** 数据类型枚举 */
export type FieldMappingDataType = 'SALES' | 'SHIPPING' | 'ADVERTISING' | 'RATE'

/** 销售数据子类型 */
export type SalesSubType = 'ORIGINAL' | 'ERP'

/** 字段类型 */
export type FieldType = 'string' | 'number' | 'datetime' | 'boolean'

/** 分隔符类型 */
export type DelimiterType = 'auto' | 'tab' | 'comma' | 'semicolon' | 'pipe' | 'space'

/** 匹配类型 */
export type MatchType = 'exact' | 'ignore_case' | 'alias' | 'label' | 'normalized' | 'contains' | 'similar'

// ==================== 源字段相关 ====================

/** CSV源字段 */
export interface SourceField {
  /** 字段名称（CSV表头） */
  name: string
  /** 示例数据（首行数据值） */
  sample?: string
  /** 字段索引 */
  index: number
  /** 是否已映射 */
  mapped?: boolean
}

// ==================== 目标字段相关 ====================

/** 系统目标字段 */
export interface TargetField {
  /** 字段名（数据库字段名） */
  field: string
  /** 显示标签 */
  label: string
  /** 字段描述 */
  description?: string
  /** 是否必填 */
  required: boolean
  /** 默认值 */
  defaultValue?: string | number | boolean
  /** 数据类型 */
  type: FieldType
  /** 最大长度 */
  maxLength?: number
  /** 精度 */
  precision?: number
  /** 站点别名 */
  siteAliases?: Record<string, string>
  /** 是否已映射 */
  mapped?: boolean
  /** 映射的源字段名 */
  mappedSource?: string
}

/** 目标字段响应 */
export interface TargetFieldsResponse {
  dataType: string
  sourceType?: string
  sourceTypeName?: string
  fields: TargetField[]
  aggregateConfig?: AggregateConfig
}

/** ERP数据聚合配置 */
export interface AggregateConfig {
  groupBy: string[]
  pivotField: string
  valueField: string
  pivotMapping: Record<string, string>
}

// ==================== 映射配置相关 ====================

/** 映射配置项 */
export interface MappingConfig {
  /** 源字段名 */
  source: string
  /** 目标字段名 */
  target: string
  /** 源字段索引 */
  sourceIndex?: number
}

/** 默认值配置 */
export interface DefaultValueConfig {
  /** 目标字段名 */
  field: string
  /** 默认值 */
  value: string | number | boolean
}

/** 映射模板 */
export interface MappingTemplate {
  /** 模板ID */
  id?: number
  /** 模板名称 */
  templateName: string
  /** 站点编码 */
  siteCode: string
  /** 数据类型 */
  dataType: FieldMappingDataType
  /** 子类型（销售数据） */
  subType?: SalesSubType
  /** 映射配置 */
  mappings: MappingConfig[]
  /** 源字段列表 */
  sourceFields?: SourceField[]
  /** 表头行号 */
  headerRow?: number
  /** 默认值配置 */
  defaultValues?: DefaultValueConfig[]
  /** 是否默认模板 */
  isDefault?: boolean
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

// ==================== 文件解析相关 ====================

/** 文件预览行 */
export interface FilePreviewRow {
  rowNum: number
  content: string
  cells: string[]
}

/** 文件预览结果 */
export interface FilePreviewResponse {
  rows: FilePreviewRow[]
  totalRows: number
  encoding: string
  delimiter: string
  sheetName?: string
  sheets?: string[]
}

/** 解析后的源字段结果 */
export interface ParseFieldsResponse {
  fields: SourceField[]
  totalColumns: number
  headerRow: number
  encoding: string
  delimiter: string
}

// ==================== 智能匹配相关 ====================

/** 匹配建议 */
export interface MatchSuggestion {
  source: string
  target: string
  confidence: number
  matchType: MatchType
}

/** 智能匹配请求 */
export interface AutoMatchRequest {
  dataType: string
  sourceType?: string
  sourceFields: string[]
  siteCode?: string
}

/** 智能匹配结果 */
export interface AutoMatchResponse {
  mappings: MatchSuggestion[]
  unmatchedSource: string[]
  unmatchedTarget: string[]
}

// ==================== 界面相关类型 ====================

/** 连线数据 */
export interface MappingLine {
  /** 源字段名 */
  source: string
  /** 目标字段名 */
  target: string
  /** SVG路径 */
  path: string
  /** 源点坐标 */
  sourcePoint: { x: number; y: number }
  /** 目标点坐标 */
  targetPoint: { x: number; y: number }
  /** 是否激活 */
  active?: boolean
}

/** 组件Props */
export interface FieldMappingCanvasProps {
  /** CSV源字段列表 */
  sourceFields?: SourceField[]
  /** 系统目标字段列表 */
  targetFields?: TargetField[]
  /** 初始映射配置 */
  initialMappings?: MappingConfig[]
  /** 初始默认值配置 */
  initialDefaultValues?: DefaultValueConfig[]
  /** 数据类型 */
  dataType?: FieldMappingDataType
  /** 子类型 */
  subType?: SalesSubType
  /** 站点编码 */
  siteCode?: string
  /** 是否只读 */
  readonly?: boolean
  /** 是否自动加载目标字段 */
  autoLoadTargetFields?: boolean
  /** 是否允许删除源字段 */
  allowSourceDelete?: boolean
  /** 是否显示默认值设置 */
  showDefaultValueSetting?: boolean
}

/** 组件Emits */
export interface FieldMappingCanvasEmits {
  (e: 'update:mappings', mappings: MappingConfig[]): void
  (e: 'update:defaultValues', values: DefaultValueConfig[]): void
  (e: 'save-template', mappings: MappingConfig[]): void
  (e: 'load-template', templateId: number): void
  (e: 'mapping-change', mappings: MappingConfig[]): void
  (e: 'source-delete', field: SourceField): void
}

/** 拖拽状态 */
export interface DragState {
  /** 是否正在拖拽 */
  isDragging: boolean
  /** 拖拽的源字段 */
  dragSource: SourceField | null
  /** 当前拖拽位置 */
  dragPosition: { x: number; y: number }
  /** 起始位置 */
  startPosition: { x: number; y: number }
}

// ==================== 数据类型配置 ====================

/** 数据类型配置 */
export const DATA_TYPE_CONFIG = {
  SALES: {
    label: '销售数据',
    hasSubType: true,
    subTypes: [
      { value: 'ORIGINAL', label: '亚马逊原始数据', description: '每行是一笔订单的完整金额信息' },
      { value: 'ERP', label: 'ERP结算明细', description: '每行是一笔费用明细，需按订单聚合' }
    ]
  },
  SHIPPING: {
    label: '配送数据',
    hasSubType: false
  },
  ADVERTISING: {
    label: '广告数据',
    hasSubType: false
  },
  RATE: {
    label: '汇率数据',
    hasSubType: false
  }
} as const

/** 分隔符配置 */
export const DELIMITER_OPTIONS = [
  { value: 'auto', label: '自动检测' },
  { value: 'tab', label: 'Tab制表符' },
  { value: 'comma', label: '逗号' },
  { value: 'semicolon', label: '分号' },
  { value: 'pipe', label: '竖线' },
  { value: 'space', label: '空格' }
] as const
