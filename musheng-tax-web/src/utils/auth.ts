/**
 * 认证工具函数 - Token管理
 */

const TOKEN_KEY = 'musheng_tax_token'
const USER_KEY = 'musheng_tax_user'

/**
 * 获取Token
 */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 设置Token
 */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 移除Token
 */
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

/**
 * 获取用户信息
 */
export function getUserInfo(): any | null {
  const userStr = localStorage.getItem(USER_KEY)
  if (userStr) {
    try {
      return JSON.parse(userStr)
    } catch {
      return null
    }
  }
  return null
}

/**
 * 设置用户信息
 */
export function setUserInfo(user: any): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

/**
 * 清除所有认证信息
 */
export function clearAuth(): void {
  removeToken()
  localStorage.removeItem(USER_KEY)
}
