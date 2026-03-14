/**
 * 报税汇总API接口
 */

import { request } from '@/utils/request'

const BASE_URL = '/api/v1/business/reports'

// ============= 首页仪表盘 =============

/**
 * 站点收入
 */
export interface SiteRevenue {
  siteCode: string
  siteName: string
  revenue: number
  refund: number
  netIncome: number
}

/**
 * 季度趋势
 */
export interface QuarterTrend {
  quarter: string
  revenue: number
  refund: number
  netIncome: number
}

/**
 * 首页仪表盘数据
 */
export interface DashboardData {
  currentQuarter: string
  totalRevenueCny: number
  refundCny: number
  netIncomeCny: number
  shippingOrderCount: number
  revenueGrowthRate: number
  refundGrowthRate: number
  netIncomeGrowthRate: number
  siteRevenues: SiteRevenue[]
  quarterTrends: QuarterTrend[]
}

/**
 * 获取首页仪表盘数据
 * @param quarter 季度(格式:2024-Q1，不传则默认当前季度)
 */
export function getDashboardData(quarter?: string) {
  return request.get<DashboardData>(`${BASE_URL}/dashboard`, quarter ? { quarter } : undefined)
}

// ============= 报税汇总接口 =============

/**
 * 报税汇总数据 V2
 */
export interface TaxReportSummary {
  siteCode: string
  siteName: string
  yearQuarter: string
  currencyCode: string
  // 收入（按发货）
  totalRevenue: number
  totalRevenueCny: number
  // 退款-按发货归属
  refundByShipment: number
  refundByShipmentCny: number
  refundCountByShipment: number
  // 退款-按结算时间
  refundBySettlement: number
  refundBySettlementCny: number
  refundCountBySettlement: number
  // 退款-Amazon口径
  refundBySettlementAmazon: number
  refundBySettlementAmazonCny: number
  refundCountBySettlementAmazon: number
  // 消费税
  consumptionTax: number
  consumptionTaxCny: number
  // 佣金/服务费明细
  sellingFees: number
  sellingFeesCny: number
  fbaFees: number
  fbaFeesCny: number
  otherTransactionFees: number
  otherTransactionFeesCny: number
  otherAmount: number
  otherAmountCny: number
  totalServiceFee: number
  totalServiceFeeCny: number
  // 佣金-Amazon口径
  totalCommissionFee: number
  totalCommissionFeeCny: number
  // 其他费（拆分）
  miscServiceFee: number
  miscServiceFeeCny: number
  otherFees: number
  otherFeesCny: number
  miscFeesCount: number
  // 广告费
  advertisingCost: number
  advertisingCostCny: number
  // 总成本
  totalCost: number
  // 平台支出与采购成本（按图片公式）
  platformExpenses: number
  platformExpensesCny: number
  profit4Percent: number
  profit4PercentCny: number
  procurementCost: number
  procurementCostCny: number
}

/**
 * 费用分类明细
 */
export interface FeeBreakdown {
  siteCode: string
  yearQuarter: string
  feeType: string
  feeCategory: string
  amount: number
  amountCny: number
  transactionCount: number
}

/**
 * 查询报税汇总
 */
export function getTaxSummary(params: { siteCode?: string; startQuarter: string; endQuarter: string }) {
  return request.get<TaxReportSummary[]>(`${BASE_URL}/tax-summary`, params)
}

/**
 * 查询费用分类明细
 */
export function getFeeBreakdown(params: { siteCode?: string; startQuarter: string; endQuarter: string }) {
  return request.get<FeeBreakdown[]>(`${BASE_URL}/fee-breakdown`, params)
}

/**
 * 导出报税汇总列表（表头与列表一致）
 */
export function exportTaxSummary(params: { siteCode?: string; startQuarter: string; endQuarter: string }) {
  const filename = `报税汇总_${new Date().toISOString().slice(0, 10)}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/tax-summary/export`, filename, params)
}
