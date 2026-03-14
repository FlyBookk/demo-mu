/**
 * 认证相关API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { LoginParams, LoginResult, UserInfo, ChangePasswordParams, UpdateProfileParams } from '@/types/auth'

/**
 * 用户登录
 */
export function login(data: LoginParams) {
  return request.post<LoginResult>('/api/v1/auth/login', data)
}

/**
 * 用户登出
 */
export function logout() {
  return request.post<void>('/api/v1/auth/logout')
}

/**
 * 刷新Token
 */
export function refreshToken() {
  return request.post<{ token: string }>('/api/v1/auth/refresh')
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return request.get<UserInfo>('/api/v1/auth/current-user')
}

/**
 * 修改密码
 */
export function changePassword(data: ChangePasswordParams) {
  return request.post<void>('/api/v1/auth/change-password', data)
}

/**
 * 更新个人信息
 */
export function updateProfile(data: UpdateProfileParams) {
  return request.post<void>('/api/v1/auth/update-profile', data)
}

/**
 * 获取用户权限列表
 */
export function getUserPermissions() {
  return request.get<string[]>('/api/v1/auth/permissions')
}
