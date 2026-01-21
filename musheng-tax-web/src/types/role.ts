/**
 * 角色管理类型定义
 * 根据后端接口文档定义
 */

// 角色信息
export interface Role {
  id: number
  roleCode: string
  roleName: string
  roleDesc?: string
  permissions?: string
  status: number
  createTime?: string
  updateTime?: string
  createBy?: number
  updateBy?: number
}

// 角色状态
export enum RoleStatus {
  DISABLED = 0,
  ENABLED = 1
}

// 角色状态标签映射
export const RoleStatusLabel: Record<RoleStatus, string> = {
  [RoleStatus.DISABLED]: '禁用',
  [RoleStatus.ENABLED]: '启用'
}

// 角色查询参数
export interface RoleQuery {
  roleCode?: string
  roleName?: string
  status?: number
  page?: number
  size?: number
}

// 角色创建参数
export interface RoleCreateForm {
  roleCode: string
  roleName: string
  roleDesc?: string
  permissions?: string[]
}

// 角色更新参数
export interface RoleUpdateForm {
  roleName?: string
  roleDesc?: string
  status?: number
}

// 角色创建/更新参数 (兼容)
export interface RoleForm {
  id?: number
  roleCode: string
  roleName: string
  roleDesc?: string
  status?: number
}

// 角色权限分配参数
export interface RolePermissionParams {
  roleId: number
  permissions: string[]
}

// 权限信息
export interface Permission {
  id: string
  code: string
  name: string
  type: PermissionType
  parentId?: string
  path?: string
  icon?: string
  sort: number
  children?: Permission[]
}

// 权限类型
export type PermissionType = 'MENU' | 'BUTTON' | 'API'

// 权限树节点
export interface PermissionTreeNode {
  key: string
  title: string
  type: PermissionType
  children?: PermissionTreeNode[]
}

// 角色简要信息（下拉选项用）
export interface RoleOption {
  id: number
  roleCode: string
  roleName: string
}
