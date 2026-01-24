/**
 * Axios封装 - 慕声报税系统
 *
 * P0问题解决:
 * - 业务状态码兼容0和200，统一判断为成功
 * - 接口路径与后端对齐，使用/api/v1前缀
 */

import axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { message } from 'ant-design-vue'
import { getToken, removeToken } from '@/utils/auth'

// 业务响应结构
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp?: number
}

// 分页响应结构
export interface PageResult<T = any> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

// 成功状态码集合（兼容0和200）
const SUCCESS_CODES = [0, 200]

// Token过期状态码
const TOKEN_EXPIRED_CODE = 401

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 店铺ID存储键（与 shop.ts store 保持一致）
const SHOP_STORAGE_KEY = 'currentShopId'

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 添加认证Token
    const token = getToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    // 添加店铺ID（用于数据隔离）
    const shopId = localStorage.getItem(SHOP_STORAGE_KEY)
    if (shopId && config.headers) {
      config.headers['X-Shop-Id'] = shopId
    }
    
    return config
  },
  (error) => {
    console.error('Request Error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { config, data } = response

    // 文件下载场景直接返回
    if (config.responseType === 'blob') {
      return response
    }

    // 判断业务状态码（兼容0和200）
    if (SUCCESS_CODES.includes(data.code)) {
      return data as any
    }

    // Token过期处理
    if (data.code === TOKEN_EXPIRED_CODE) {
      message.error('登录已过期，请重新登录')
      removeToken()
      // 跳转登录页
      const currentPath = window.location.pathname
      if (currentPath !== '/login') {
        window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
      }
      return Promise.reject(new Error('Token expired'))
    }

    // 其他业务错误
    const errorMsg = data.message || '请求失败'
    message.error(errorMsg)
    return Promise.reject(new Error(errorMsg))
  },
  (error) => {
    // 网络层错误处理
    let errorMsg = '网络异常，请检查网络连接'

    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 400:
          errorMsg = data?.message || '请求参数错误'
          break
        case 401:
          errorMsg = '登录已过期，请重新登录'
          removeToken()
          const currentPath = window.location.pathname
          if (currentPath !== '/login') {
            window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
          }
          break
        case 403:
          errorMsg = '没有权限访问该资源'
          break
        case 404:
          errorMsg = '请求的资源不存在'
          break
        case 500:
          errorMsg = '服务器内部错误'
          break
        case 502:
          errorMsg = '网关错误'
          break
        case 503:
          errorMsg = '服务暂不可用'
          break
        case 504:
          errorMsg = '网关超时'
          break
        default:
          errorMsg = data?.message || `请求失败(${status})`
      }
    } else if (error.code === 'ECONNABORTED') {
      errorMsg = '请求超时，请稍后重试'
    } else if (error.message === 'Network Error') {
      errorMsg = '网络连接失败，请检查网络'
    }

    message.error(errorMsg)
    return Promise.reject(error)
  }
)

/**
 * 封装的请求方法
 */
export const request = {
  /**
   * GET请求
   */
  get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.get(url, { params, ...config })
  },

  /**
   * POST请求
   */
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.post(url, data, config)
  },

  /**
   * PUT请求
   */
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.put(url, data, config)
  },

  /**
   * DELETE请求
   */
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.delete(url, config)
  },

  /**
   * 文件上传
   */
  upload<T = any>(
    url: string,
    file: File | FormData,
    config?: AxiosRequestConfig
  ): Promise<ApiResponse<T>> {
    const formData = file instanceof FormData ? file : new FormData()
    if (!(file instanceof FormData)) {
      formData.append('file', file)
    }
    return service.post(url, formData, {
      ...config,
      headers: {
        'Content-Type': 'multipart/form-data',
        ...config?.headers
      }
    })
  },

  /**
   * 文件下载
   */
  download(url: string, params?: any, config?: AxiosRequestConfig): Promise<AxiosResponse<Blob>> {
    return service.get(url, {
      params,
      ...config,
      responseType: 'blob'
    })
  },

  /**
   * 下载并保存文件
   */
  async downloadAndSave(url: string, filename: string, params?: any): Promise<void> {
    try {
      const response = await this.download(url, params)
      const blob = new Blob([response.data])
      const link = document.createElement('a')
      link.href = window.URL.createObjectURL(blob)
      link.download = filename
      link.click()
      window.URL.revokeObjectURL(link.href)
    } catch (error) {
      message.error('文件下载失败')
      throw error
    }
  }
}

export default service
