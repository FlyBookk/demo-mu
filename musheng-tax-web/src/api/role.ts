/**
 * 角色管理API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  Role,
  RoleQuery,
  RoleForm,
  RolePermissionParams,
  Permission,
  PermissionTreeNode,
  RoleOption
} from '@/types/role'

const BASE_URL = '/api/v1/system/roles'

/**
 * 获取角色列表
 */
export function getRoleList(params: RoleQuery) {
  return request.get<PageResult<Role>>(BASE_URL, params)
}

/**
 * 获取所有角色（不分页）
 */
export function getAllRoles() {
  return request.get<Role[]>(`${BASE_URL}/all`)
}

/**
 * 根据ID获取角色详情
 */
export function getRoleById(id: number) {
  return request.get<Role>(`${BASE_URL}/${id}`)
}

/**
 * 创建角色
 */
export function createRole(data: RoleForm) {
  return request.post<Role>(BASE_URL, data)
}

/**
 * 更新角色
 */
export function updateRole(id: number, data: RoleForm) {
  return request.put<Role>(`${BASE_URL}/${id}`, data)
}

/**
 * 删除角色
 */
export function deleteRole(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/**
 * 批量删除角色
 */
export function batchDeleteRole(ids: number[]) {
  return request.delete<void>(BASE_URL, { data: { ids } })
}

/**
 * 获取角色权限
 */
export function getRolePermissions(roleId: number) {
  return request.get<string[]>(`${BASE_URL}/${roleId}/permissions`)
}

/**
 * 分配角色权限
 */
export function assignRolePermissions(data: RolePermissionParams) {
  return request.post<void>(`${BASE_URL}/${data.roleId}/permissions`, {
    permissions: data.permissions
  })
}

/**
 * 获取所有权限列表
 */
export function getAllPermissions() {
  return request.get<Permission[]>(`${BASE_URL}/permissions/all`)
}

/**
 * 获取权限树
 */
export function getPermissionTree() {
  return request.get<PermissionTreeNode[]>(`${BASE_URL}/permissions/tree`)
}

/**
 * 获取角色选项列表（下拉选项用）
 */
export function getRoleOptions() {
  return request.get<RoleOption[]>(`${BASE_URL}/options`)
}

/**
 * 检查角色编码是否可用
 */
export function checkRoleCodeAvailable(code: string, excludeId?: number) {
  return request.get<{ available: boolean }>(`${BASE_URL}/check-code`, {
    code,
    excludeId
  })
}
