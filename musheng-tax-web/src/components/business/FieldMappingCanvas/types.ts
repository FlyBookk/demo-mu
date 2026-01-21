/**
 * 字段映射画布组件 - 类型定义
 */

/** CSV源字段 */
export interface SourceField {
  /** 字段名称（CSV表头） */
  name: string
  /** 示例数据（首行数据值） */
  sample: string
  /** 字段索引 */
  index: number
  /** 是否已映射 */
  mapped?: boolean
}

/** 系统目标字段 */
export interface TargetField {
  /** 字段名（数据库字段名） */
  name: string
  /** 显示标签 */
  label: string
  /** 字段描述 */
  description: string
  /** 是否必填 */
  required: boolean
  /** 默认值 */
  defaultValue?: string | number
  /** 数据类型 */
  dataType: 'string' | 'number' | 'datetime'
  /** 是否已映射 */
  mapped?: boolean
  /** 映射的源字段名 */
  mappedSource?: string
}

/** 映射配置项 */
export interface MappingConfig {
  /** 源字段名 */
  source: string
  /** 目标字段名 */
  target: string
}

/** 映射模板 */
export interface MappingTemplate {
  /** 模板ID */
  id: number
  /** 模板名称 */
  templateName: string
  /** 站点编码 */
  siteCode: string
  /** 映射配置 */
  mappings: MappingConfig[]
  /** 未映射字段默认值 */
  unmappedDefaults: Record<string, string | number>
  /** 是否默认模板 */
  isDefault: boolean
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

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
  sourceFields: SourceField[]
  /** 系统目标字段列表 */
  targetFields: TargetField[]
  /** 初始映射配置 */
  initialMappings?: MappingConfig[]
  /** 站点编码 */
  siteCode?: string
  /** 是否只读 */
  readonly?: boolean
}

/** 组件Emits */
export interface FieldMappingCanvasEmits {
  (e: 'update:mappings', mappings: MappingConfig[]): void
  (e: 'save-template', mappings: MappingConfig[]): void
  (e: 'load-template', templateId: number): void
  (e: 'mapping-change', mappings: MappingConfig[]): void
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
