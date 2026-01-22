/**
 * 汇总报表API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type {
  ReportSummary,
  ReportQuery,
  ReportExportParams,
  ReportTrend,
  ReportComparison,
  MarketplaceReportSummary,
  QuarterReportDetail
} from '@/types/report'

const BASE_URL = '/api/v1/business/reports'

/**
 * 查询报表汇总
 * @param params 查询参数: siteCode, yearQuarter, startQuarter, endQuarter
 */
export function getReportSummary(params: Record<string, string | undefined>) {
  // 过滤掉undefined的参数
  const cleanParams: Record<string, string> = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      cleanParams[key] = value
    }
  })
  return request.get<ReportSummary[]>(`${BASE_URL}/summary`, cleanParams)
}

/**
 * 按站点获取报表汇总
 */
export function getReportSummaryBySite(yearQuarter: string) {
  return request.get<ReportSummary[]>(`${BASE_URL}/summary/by-site`, { yearQuarter })
}

/**
 * 按季度获取报表汇总
 */
export function getReportSummaryByQuarter(siteCode: string, startQuarter?: string, endQuarter?: string) {
  return request.get<ReportSummary[]>(`${BASE_URL}/summary/by-quarter`, { siteCode, startQuarter, endQuarter })
}

/**
 * 导出汇总报表
 */
export function exportReportSummary(params: ReportQuery) {
  const filename = `汇总报表_${new Date().toISOString().slice(0, 10)}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/summary/export`, filename, params)
}

/**
 * 导出明细报表
 */
export function exportReportDetail(siteCode: string, yearQuarter: string, reportType?: string) {
  const filename = `明细报表_${siteCode}_${yearQuarter}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/detail/export`, filename, { siteCode, yearQuarter, reportType })
}

/**
 * 获取各站点报表汇总（通过按站点分组查询）
 */
export function getMarketplaceReportSummary(yearQuarter: string) {
  return request.get<ReportSummary[]>(`${BASE_URL}/summary/by-site`, { yearQuarter })
}

/**
 * 导出汇总报表
 */
export function exportReport(params: ReportExportParams) {
  const filename = `汇总报表_${new Date().toISOString().slice(0, 10)}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/summary/export`, filename, params)
}

/**
 * 导出明细报表（VAT报表）
 */
export function exportVatReport(siteCode: string, yearQuarter: string) {
  const filename = `VAT报表_${siteCode}_${yearQuarter}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/detail/export`, filename, { siteCode, yearQuarter, reportType: 'all' })
}
