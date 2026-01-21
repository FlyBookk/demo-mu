/**
 * 用户管理API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  User,
  UserQuery,
  UserCreateForm,
  UserUpdateForm,
  ResetPasswordParams,
  ToggleStatusParams,
  UserOption,
  UserStatus
} from '@/types/user'

const BASE_URL = '/api/v1/system/users'

/**
 * 获取用户列表
 */
export function getUserList(params: UserQuery) {
  return request.get<PageResult<User>>(BASE_URL, params)
}

/**
 * 根据ID获取用户详情
 */
export function getUserById(id: number) {
  return request.get<User>(`${BASE_URL}/${id}`)
}

/**
 * 创建用户
 */
export function createUser(data: UserCreateForm) {
  return request.post<User>(BASE_URL, data)
}

/**
 * 更新用户
 */
export function updateUser(id: number, data: UserUpdateForm) {
  return request.put<User>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除用户
 */
export function deleteUser(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除用户
 */
export function batchDeleteUser(ids: number[]) {
  return request.delete<void>(BASE_URL, { data: { ids } })
}

/**
 * 重置用户密码
 */
export function resetPassword(data: ResetPasswordParams) {
  return request.post<void>(`${BASE_URL}/${data.userId}/reset-password`)
}

/**
 * 切换用户状态
 */
export function toggleUserStatus(data: ToggleStatusParams) {
  return request.post<void>(`${BASE_URL}/${data.userId}/toggle-status`)
}

/**
 * 启用用户
 */
export function enableUser(id: number) {
  return request.post<void>(`${BASE_URL}/${id}/enable`)
}

/**
 * 禁用用户
 */
export function disableUser(id: number) {
  return request.post<void>(`${BASE_URL}/${id}/disable`)
}

/**
 * 获取用户选项列表（下拉选项用）
 */
export function getUserOptions() {
  return request.get<UserOption[]>(`${BASE_URL}/options`)
}

/**
 * 检查用户名是否可用
 */
export function checkUsernameAvailable(username: string, excludeId?: number) {
  return request.get<{ available: boolean }>(`${BASE_URL}/check-username`, {
    username,
    excludeId
  })
}

/**
 * 导出用户列表
 */
export function exportUsers(params: UserQuery) {
  return request.download(`${BASE_URL}/export`, params)
}
