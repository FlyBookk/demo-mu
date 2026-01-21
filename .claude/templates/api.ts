import { request } from '@/utils/request'
import type { [ModuleName]Data, [ModuleName]Query, PageResult } from '@/types'

/**
 * [模块名称]相关API
 */

/**
 * 查询[模块]列表
 * @param params 查询参数
 * @returns 分页数据
 */
export function get[ModuleName]List(
  params: [ModuleName]Query
): Promise<PageResult<[ModuleName]Data>> {
  return request.get('/api/v1/[module-name]', { params })
}

/**
 * 获取[模块]详情
 * @param id 数据ID
 * @returns [模块]数据
 */
export function get[ModuleName]Detail(id: number): Promise<[ModuleName]Data> {
  return request.get(`/api/v1/[module-name]/${id}`)
}

/**
 * 创建[模块]
 * @param data 创建数据
 * @returns 创建后的数据
 */
export function create[ModuleName](
  data: Partial<[ModuleName]Data>
): Promise<[ModuleName]Data> {
  return request.post('/api/v1/[module-name]', data)
}

/**
 * 更新[模块]
 * @param id 数据ID
 * @param data 更新数据
 * @returns 更新后的数据
 */
export function update[ModuleName](
  id: number,
  data: Partial<[ModuleName]Data>
): Promise<[ModuleName]Data> {
  return request.put(`/api/v1/[module-name]/${id}`, data)
}

/**
 * 删除[模块]
 * @param id 数据ID
 */
export function delete[ModuleName](id: number): Promise<void> {
  return request.delete(`/api/v1/[module-name]/${id}`)
}

/**
 * 批量删除[模块]
 * @param ids ID列表
 */
export function batchDelete[ModuleName](ids: number[]): Promise<void> {
  return request.post('/api/v1/[module-name]/batch-delete', { ids })
}

/**
 * 导出[模块]数据
 * @param params 查询参数
 * @returns Blob数据
 */
export function export[ModuleName](params: [ModuleName]Query): Promise<Blob> {
  return request.download('/api/v1/[module-name]/export', params)
}
