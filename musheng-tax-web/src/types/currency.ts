/**
 * 货币管理类型定义
 * 与后端 CurrencyController 接口对齐
 */

// 货币信息（对应后端 Currency 实体）
export interface Currency {
  id: number
  currencyCode: string      // 货币代码 USD/EUR/GBP/CAD
  currencyName: string      // 货币名称
  currencySymbol: string    // 货币符号 $, EUR, etc.
  decimalPlaces: number     // 小数位数
  status: number            // 状态 1-启用 0-禁用
  createTime: string
  updateTime?: string
}

// 货币查询参数（对应后端 CurrencyQueryRequest）
export interface CurrencyQuery {
  currencyCode?: string
  currencyName?: string
  status?: number
  page?: number
  size?: number
}

// 货币创建/更新参数（对应后端 CurrencyRequest）
export interface CurrencyForm {
  id?: number
  currencyCode: string
  currencyName: string
  currencySymbol: string
  decimalPlaces?: number
  status?: number
}
