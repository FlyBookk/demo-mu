/**
 * 数据清理API接口
 */

import { request } from '@/utils/request'

const BASE_URL = '/api/v1/config/data-clean'

export interface DataCleanModule {
  moduleCode: string
  moduleName: string
  description: string
  dataCount: number
}

/**
 * 获取可清理模块列表
 */
export function getCleanModules() {
  return request.get<DataCleanModule[]>(`${BASE_URL}/modules`)
}

/**
 * 清理指定模块数据
 */
export function cleanModule(moduleCode: string) {
  return request.delete<number>(`${BASE_URL}/modules/${moduleCode}`)
}
