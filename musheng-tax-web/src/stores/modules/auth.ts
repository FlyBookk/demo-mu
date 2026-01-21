/**
 * 认证状态管理Store
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser, refreshToken as refreshTokenApi } from '@/api/auth'
import { getToken, setToken, removeToken, setUserInfo, getUserInfo, clearAuth } from '@/utils/auth'
import type { LoginParams, UserInfo } from '@/types/auth'
import router from '@/router'

export const useAuthStore = defineStore('auth', () => {
  // ============= State =============
  const token = ref<string>(getToken() || '')
  const userInfo = ref<UserInfo | null>(getUserInfo())
  const permissions = ref<string[]>([])

  // ============= Getters =============
  /**
   * 是否已登录
   */
  const isLoggedIn = computed(() => !!token.value)

  /**
   * 是否是管理员
   */
  const isAdmin = computed(() => userInfo.value?.role === 'admin')

  /**
   * 是否是财务人员
   */
  const isFinance = computed(() => userInfo.value?.role === 'finance')

  /**
   * 是否是查看者
   */
  const isViewer = computed(() => userInfo.value?.role === 'viewer')

  /**
   * 用户角色
   */
  const userRole = computed(() => userInfo.value?.role || '')

  /**
   * 用户名
   */
  const username = computed(() => userInfo.value?.username || '')

  /**
   * 真实姓名
   */
  const realName = computed(() => userInfo.value?.realName || '')

  // ============= Actions =============
  /**
   * 登录
   */
  async function loginAction(params: LoginParams) {
    try {
      const res = await loginApi(params)
      token.value = res.data.token
      userInfo.value = res.data.userInfo
      permissions.value = res.data.permissions || []

      // 持久化存储
      setToken(res.data.token)
      setUserInfo(res.data.userInfo)

      return res.data
    } catch (error) {
      throw error
    }
  }

  /**
   * 登出
   */
  async function logoutAction() {
    try {
      await logoutApi()
    } catch {
      // 即使登出接口失败也要清理本地状态
    } finally {
      resetState()
      router.push('/login')
    }
  }

  /**
   * 刷新Token
   */
  async function refreshTokenAction() {
    try {
      const res = await refreshTokenApi()
      token.value = res.data.token
      setToken(res.data.token)
      return res.data.token
    } catch (error) {
      // 刷新失败，清理状态并跳转登录页
      resetState()
      router.push('/login')
      throw error
    }
  }

  /**
   * 获取用户信息
   */
  async function fetchUserInfo() {
    try {
      const res = await getCurrentUser()
      userInfo.value = res.data
      setUserInfo(res.data)
      return res.data
    } catch (error) {
      throw error
    }
  }

  /**
   * 检查是否有指定权限
   */
  function hasPermission(permission: string): boolean {
    // 管理员拥有所有权限
    if (isAdmin.value) return true

    // 检查权限列表
    return permissions.value.includes(permission)
  }

  /**
   * 检查是否有任一权限
   */
  function hasAnyPermission(permissionList: string[]): boolean {
    if (isAdmin.value) return true
    return permissionList.some(p => permissions.value.includes(p))
  }

  /**
   * 检查是否有所有权限
   */
  function hasAllPermissions(permissionList: string[]): boolean {
    if (isAdmin.value) return true
    return permissionList.every(p => permissions.value.includes(p))
  }

  /**
   * 检查是否有指定角色
   */
  function hasRole(role: string): boolean {
    return userInfo.value?.role === role
  }

  /**
   * 检查是否有任一角色
   */
  function hasAnyRole(roles: string[]): boolean {
    return roles.includes(userInfo.value?.role || '')
  }

  /**
   * 重置状态
   */
  function resetState() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    clearAuth()
  }

  /**
   * 设置权限列表
   */
  function setPermissions(perms: string[]) {
    permissions.value = perms
  }

  return {
    // State
    token,
    userInfo,
    permissions,

    // Getters
    isLoggedIn,
    isAdmin,
    isFinance,
    isViewer,
    userRole,
    username,
    realName,

    // Actions
    loginAction,
    logoutAction,
    refreshTokenAction,
    fetchUserInfo,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    hasRole,
    hasAnyRole,
    resetState,
    setPermissions
  }
})
