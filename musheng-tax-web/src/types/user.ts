/**
 * 用户管理类型定义
 * 根据后端接口文档定义
 */

// 用户信息
export interface User {
  id: number
  username: string
  realName: string
  email?: string
  phone?: string
  avatar?: string
  roleCode: string
  status: number
  loginFailCount?: number
  lockTime?: string
  lastLoginTime?: string
  lastLoginIp?: string
  createTime: string
  updateTime?: string
  createBy?: number
  updateBy?: number
  deleted?: number
}

// 用户状态
export enum UserStatus {
  DISABLED = 0,
  ENABLED = 1
}

// 用户状态标签映射
export const UserStatusLabel: Record<UserStatus, string> = {
  [UserStatus.DISABLED]: '禁用',
  [UserStatus.ENABLED]: '启用'
}

// 用户查询参数
export interface UserQuery {
  username?: string
  realName?: string
  roleCode?: string
  status?: number
  page?: number
  size?: number
}

// 用户创建参数
export interface UserCreateForm {
  username: string
  password?: string
  realName: string
  email?: string
  phone?: string
  roleCode: string
}

// 用户更新参数
export interface UserUpdateForm {
  id: number
  realName?: string
  email?: string
  phone?: string
  avatar?: string
  roleCode?: string
}

// 重置密码参数
export interface ResetPasswordParams {
  userId: number
  newPassword: string
}

// 切换用户状态参数
export interface ToggleStatusParams {
  userId: number
  status: number
}

// 用户简要信息（下拉选项用）
export interface UserOption {
  id: number
  username: string
  realName: string
}
