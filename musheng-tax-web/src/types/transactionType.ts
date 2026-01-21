/**
 * 交易类型映射类型定义
 * 与后端 t_transaction_type_mapping 表结构对齐
 */

// 交易类型映射
export interface TransactionTypeMapping {
  id: number
  siteCode?: string  // 站点编码(可为空表示通用)
  originalType: string  // 原始交易类型
  standardCategory: string  // 标准分类(income/refund/fee/adjustment/other)
  categoryDesc?: string  // 分类说明
  status: number
  createTime: string
  updateTime?: string
}

// 交易类型映射查询参数
export interface TransactionTypeMappingQuery {
  keyword?: string
  originalType?: string
  standardCategory?: string
  siteCode?: string
  status?: number
  page?: number
  size?: number
}

// 交易类型映射创建/更新参数
export interface TransactionTypeMappingForm {
  id?: number
  siteCode?: string
  originalType: string
  standardCategory: string
  categoryDesc?: string
  status?: number
}

// 标准分类枚举
export enum StandardCategory {
  INCOME = 'income',
  REFUND = 'refund',
  FEE = 'fee',
  ADJUSTMENT = 'adjustment',
  OTHER = 'other'
}

// 标准分类标签映射
export const StandardCategoryLabel: Record<string, string> = {
  income: '收入',
  refund: '退款',
  fee: '费用',
  adjustment: '调整',
  other: '其他'
}
