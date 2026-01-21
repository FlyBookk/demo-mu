/**
 * 本地存储工具函数
 */

/**
 * 设置localStorage
 */
export function setStorage(key: string, value: any): void {
  if (value === undefined || value === null) {
    localStorage.removeItem(key)
    return
  }
  localStorage.setItem(key, JSON.stringify(value))
}

/**
 * 获取localStorage
 */
export function getStorage<T = any>(key: string, defaultValue?: T): T | null {
  const value = localStorage.getItem(key)
  if (value === null) {
    return defaultValue ?? null
  }
  try {
    return JSON.parse(value) as T
  } catch {
    return value as unknown as T
  }
}

/**
 * 移除localStorage
 */
export function removeStorage(key: string): void {
  localStorage.removeItem(key)
}

/**
 * 清空localStorage
 */
export function clearStorage(): void {
  localStorage.clear()
}

/**
 * 设置sessionStorage
 */
export function setSessionStorage(key: string, value: any): void {
  if (value === undefined || value === null) {
    sessionStorage.removeItem(key)
    return
  }
  sessionStorage.setItem(key, JSON.stringify(value))
}

/**
 * 获取sessionStorage
 */
export function getSessionStorage<T = any>(key: string, defaultValue?: T): T | null {
  const value = sessionStorage.getItem(key)
  if (value === null) {
    return defaultValue ?? null
  }
  try {
    return JSON.parse(value) as T
  } catch {
    return value as unknown as T
  }
}

/**
 * 移除sessionStorage
 */
export function removeSessionStorage(key: string): void {
  sessionStorage.removeItem(key)
}
