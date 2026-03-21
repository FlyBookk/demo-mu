/**
 * 系统配置API
 */
import { request } from '@/utils/request'

const BASE_URL = '/api/v1/sys-config'

/** 获取配置值 */
export function getConfigValue(key: string) {
  return request.get<string>(`${BASE_URL}/${key}`)
}

/** 更新配置值 */
export function updateConfigValue(key: string, value: string) {
  return request.put<void>(`${BASE_URL}/${key}`, { value })
}
