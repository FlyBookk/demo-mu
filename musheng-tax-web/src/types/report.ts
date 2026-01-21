/**
 * 汇总报表类型定义
 */

// 报表汇总数据
export interface ReportSummary {
  id?: number
  marketplaceId: number
  marketplaceName?: string
  quarter: string
  year: number
  currencyCode: string
  // 销售汇总
  totalSales: number
  totalRefunds: number
  netSales: number
  // 费用汇总
  totalCommission: number
  totalFbaFee: number
  totalShippingFee: number
  totalAdvertisingSpend: number
  totalOtherFee: number
  totalFees: number
  // 税务相关
  totalVatCollected: number
  totalVatPaid: number
  netVat: number
  vatRate: number
  // 利润
  grossProfit: number
  netProfit: number
  profitMargin: number
  // 订单统计
  totalOrders: number
  totalUnits: number
  avgOrderValue: number
  // 时间
  reportDate?: string
  createTime?: string
  updateTime?: string
}

// 报表查询参数
export interface ReportQuery {
  marketplaceId?: number
  quarter?: string
  year?: number
  startQuarter?: string
  endQuarter?: string
  currencyCode?: string
}

// 报表导出参数
export interface ReportExportParams {
  marketplaceId?: number
  quarter?: string
  year?: number
  format?: 'EXCEL' | 'PDF' | 'CSV'
  includeDetails?: boolean
}

// 报表趋势数据
export interface ReportTrend {
  period: string
  sales: number
  fees: number
  profit: number
  orders: number
}

// 报表对比数据
export interface ReportComparison {
  currentPeriod: ReportSummary
  previousPeriod: ReportSummary
  salesGrowth: number
  profitGrowth: number
  orderGrowth: number
}

// 站点报表汇总
export interface MarketplaceReportSummary {
  marketplaceId: number
  marketplaceName: string
  marketplaceCode: string
  totalSales: number
  totalFees: number
  netProfit: number
  totalOrders: number
  percentage: number
  currencyCode: string
}

// 季度报表详情
export interface QuarterReportDetail {
  summary: ReportSummary
  salesByType: {
    type: string
    amount: number
    count: number
  }[]
  feesByType: {
    type: string
    amount: number
  }[]
  monthlyTrend: ReportTrend[]
}
