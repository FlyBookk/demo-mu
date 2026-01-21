/**
 * API响应类型定义
 */

// 通用API响应
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp?: number
}

// 分页请求参数
export interface PageParams {
  current?: number
  size?: number
  [key: string]: any
}

// 分页响应
export interface PageResult<T = any> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

// 列表查询参数
export interface ListQuery extends PageParams {
  keyword?: string
  startDate?: string
  endDate?: string
  status?: number
}
