/**
 * 汇率管理类型定义
 * 与后端 t_exchange_rate 表结构对齐
 */

import type { Dayjs } from 'dayjs'

// 汇率数据
export interface RateData {
  id: number
  rateDate: string
  currencyCode: string
  rate: number
  isWorkday?: number
  actualRateDate?: string
  source?: string
  createTime: string
  updateTime?: string
}

// 汇率来源
export type RateSource = 'MANUAL' | 'CHINA_MONEY' | 'IMPORT'

// 汇率来源标签映射
export const RateSourceLabel: Record<RateSource, string> = {
  MANUAL: '手动录入',
  CHINA_MONEY: '外汇中心',
  IMPORT: '文件导入'
}

// 汇率查询参数
export interface RateDataQuery {
  currencyCode?: string
  yearMonth?: string
  startDate?: string
  endDate?: string
  source?: RateSource
  page?: number
  size?: number
}

// 汇率请求（新增/修改）
export interface RateRequest {
  currencyCode: string
  rateDate: string | Dayjs
  rate: number
  isWorkday?: number
  source?: string
}

// 汇率同步请求
export interface RateSyncRequest {
  startDate: string
  endDate: string
}

// 汇率同步结果
export interface RateSyncResult {
  success: boolean
  startDate: string
  endDate: string
  currencyCodes: string[]
  totalCount: number
  insertCount: number
  updateCount: number
  failCount: number
  errors: string[]
  durationMs: number
  message: string
}

// 汇率导入参数（废弃）
export interface RateImportParams {
  templateId?: number
  fileId: string
  source?: RateSource
}

// 汇率转换请求
export interface RateConvertRequest {
  amount: number
  currencyCode: string
  rateDate?: string
}

// 汇率转换结果
export interface RateConvertResult {
  originalAmount: number
  convertedAmount: number
  currencyCode: string
  rate: number
  rateDate: string
}

// 汇率趋势数据
export interface RateTrend {
  date: string
  rate: number
}
