/**
 * 操作日志API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  OperationLog,
  OperationLogQuery,
  LoginLog,
  LoginLogQuery,
  LogStatistics,
  ModuleOperationStat
} from '@/types/log'

const BASE_URL = '/api/v1/system/logs'

/**
 * 获取操作日志列表
 */
export function getOperationLogList(params: OperationLogQuery) {
  return request.get<PageResult<OperationLog>>(BASE_URL, params)
}

/**
 * 根据ID获取操作日志详情
 */
export function getOperationLogById(id: number) {
  return request.get<OperationLog>(`${BASE_URL}/${id}`)
}

/**
 * 获取登录日志列表
 */
export function getLoginLogList(params: LoginLogQuery) {
  return request.get<PageResult<LoginLog>>(`${BASE_URL}/login`, params)
}

/**
 * 根据ID获取登录日志详情
 */
export function getLoginLogById(id: number) {
  return request.get<LoginLog>(`${BASE_URL}/login/${id}`)
}

/**
 * 获取日志统计数据
 */
export function getLogStatistics() {
  return request.get<LogStatistics>(`${BASE_URL}/statistics`)
}

/**
 * 获取模块操作统计
 */
export function getModuleOperationStats(startTime?: string, endTime?: string) {
  return request.get<ModuleOperationStat[]>(`${BASE_URL}/module-stats`, {
    startTime,
    endTime
  })
}

/**
 * 导出操作日志
 */
export function exportOperationLogs(params: OperationLogQuery) {
  return request.download(`${BASE_URL}/operation/export`, params)
}

/**
 * 导出登录日志
 */
export function exportLoginLogs(params: LoginLogQuery) {
  return request.download(`${BASE_URL}/login/export`, params)
}

/**
 * 清理操作日志（保留指定天数）
 */
export function cleanOperationLogs(retainDays: number) {
  return request.delete<{ deletedCount: number }>(`${BASE_URL}/operation/clean`, {
    params: { retainDays }
  })
}

/**
 * 清理登录日志（保留指定天数）
 */
export function cleanLoginLogs(retainDays: number) {
  return request.delete<{ deletedCount: number }>(`${BASE_URL}/login/clean`, {
    params: { retainDays }
  })
}

/**
 * 获取可用的模块列表（筛选用）
 */
export function getAvailableModules() {
  return request.get<string[]>(`${BASE_URL}/modules`)
}

/**
 * 获取可用的操作类型列表（筛选用）
 */
export function getAvailableOperations(module?: string) {
  return request.get<string[]>(`${BASE_URL}/operations`, { module })
}
