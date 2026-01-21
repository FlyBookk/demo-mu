/**
 * 操作日志类型定义
 * 根据后端接口文档定义
 */

// 操作日志
export interface OperationLog {
  id: number
  userId: number
  username: string
  module: string
  operation: string
  method: string
  requestUrl: string
  requestParams?: string
  responseData?: string
  ip?: string
  userAgent?: string
  executionTime: number
  status: number
  errorMsg?: string
  createTime: string
  updateTime?: string
  createBy?: number
  updateBy?: number
}

// 日志状态
export enum LogStatus {
  FAILED = 0,
  SUCCESS = 1
}

// 日志状态标签映射
export const LogStatusLabel: Record<LogStatus, string> = {
  [LogStatus.FAILED]: '失败',
  [LogStatus.SUCCESS]: '成功'
}

// 操作日志查询参数
export interface OperationLogQuery {
  username?: string
  module?: string
  operation?: string
  status?: number
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

// 登录日志
export interface LoginLog {
  id: number
  username: string
  loginType: LoginType
  status: number
  loginIp?: string
  loginLocation?: string
  userAgent?: string
  browser?: string
  os?: string
  errorMessage?: string
  createTime: string
}

// 登录类型
export type LoginType = 'LOGIN' | 'LOGOUT' | 'REFRESH'

// 登录日志查询参数
export interface LoginLogQuery {
  keyword?: string
  username?: string
  loginType?: LoginType
  status?: number
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}

// 日志统计
export interface LogStatistics {
  totalCount: number
  successCount: number
  failedCount: number
  todayCount: number
  avgDuration: number
}

// 模块操作统计
export interface ModuleOperationStat {
  module: string
  operationCount: number
  successCount: number
  failedCount: number
  avgDuration: number
}
