/**
 * 认证相关类型定义
 * 根据后端接口文档定义
 */

// 登录参数
export interface LoginParams {
  username: string
  password: string
  remember?: boolean
}

// 登录响应
export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
  permissions: string[]
}

// 用户信息
export interface UserInfo {
  id: number
  username: string
  realName: string
  role: string
  roleCode: string
  roleName: string
  avatar?: string
  email?: string
  phone?: string
  status: number
  createTime?: string
  lastLoginTime?: string
}

// 用户角色
export type UserRole = 'admin' | 'finance' | 'viewer' | 'ADMIN' | 'FINANCE' | 'OPERATOR'

// 用户状态 - 从 user.ts 导入使用，此处不再重复定义
// export { UserStatus } from './user'

// 修改密码参数
export interface ChangePasswordParams {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}
