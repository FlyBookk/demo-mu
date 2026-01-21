/**
 * [模块名称]相关类型定义
 */

/**
 * [模块]数据
 */
export interface [ModuleName]Data {
  /** 数据ID */
  id: number

  /** 名称 */
  name: string

  /** 描述 */
  description?: string

  /** 状态: 0-禁用 1-启用 */
  status: 0 | 1

  /** 创建时间 */
  createdAt: string

  /** 更新时间 */
  updatedAt: string

  /** 创建人 */
  createdBy: string

  /** 更新人 */
  updatedBy?: string
}

/**
 * [模块]查询参数
 */
export interface [ModuleName]Query {
  /** 页码 */
  page: number

  /** 每页条数 */
  pageSize: number

  /** 名称关键词（可选） */
  keyword?: string

  /** 状态（可选） */
  status?: 0 | 1

  /** 开始日期（可选） */
  startDate?: string

  /** 结束日期（可选） */
  endDate?: string

  /** 排序字段（可选） */
  sortField?: string

  /** 排序方向（可选） */
  sortOrder?: 'asc' | 'desc'
}

/**
 * [模块]表单数据
 */
export interface [ModuleName]Form {
  /** 名称 */
  name: string

  /** 描述 */
  description?: string

  /** 状态 */
  status: 0 | 1
}

/**
 * [模块]统计数据
 */
export interface [ModuleName]Statistics {
  /** 总数 */
  total: number

  /** 启用数量 */
  activeCount: number

  /** 禁用数量 */
  inactiveCount: number

  /** 今日新增 */
  todayCount: number
}
