/**
 * 站点（Marketplace）管理类型定义
 * 与后端 t_marketplace 表结构对齐
 */

// 站点信息
export interface Marketplace {
  id: number
  siteCode: string
  siteName: string
  marketplaceId?: string
  currencyCode: string
  sellerId?: string
  headerLanguage?: string
  dateFormat?: string
  numberFormat?: string
  timezone?: string
  status: number
  createTime: string
  updateTime?: string
}

// 站点查询参数
export interface MarketplaceQuery {
  siteCode?: string
  siteName?: string
  status?: number
  page?: number
  size?: number
}

// 站点创建/更新参数
export interface MarketplaceForm {
  id?: number
  siteCode: string
  siteName: string
  marketplaceId?: string
  currencyCode: string
  sellerId?: string
  headerLanguage?: string
  dateFormat?: string
  numberFormat?: string
  timezone?: string
  status?: number
}
