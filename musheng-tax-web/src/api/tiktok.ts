/**
 * TikTok Shop API接口
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'

const BASE = '/api/v1/tiktok'

// ========== 商品库 ==========

export interface TiktokProduct {
  id: number
  shopId: number
  siteCode: string
  productId: string
  skuId: string
  msku: string
  productName: string
  category: string
  variationValue: string
  price: number
  status: number
  createTime: string
  updateTime: string
}

export function getTiktokProductList(params: { keyword?: string; siteCode: string; current?: number; size?: number }) {
  return request.get<PageResult<TiktokProduct>>(`${BASE}/product/list`, params)
}

export function importTiktokProduct(file: File, siteCode: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('siteCode', siteCode)
  return request.post<{ inserted: number; updated: number }>(`${BASE}/product/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function updateTiktokProductMsku(id: number, msku: string) {
  return request.put<void>(`${BASE}/product/${id}/msku`, null, { params: { msku } })
}

// ========== FBT货件 ==========

export interface TiktokShipment {
  id: number
  siteCode: string
  shipmentId: string
  shipmentName: string
  status: string
  warehouseCode: string
  country: string
  state: string
  city: string
  totalSkus: number
  totalQuantity: number
  creationTime: string
}

export interface TiktokShipmentItem {
  id: number
  shipmentId: string
  msku: string
  quantityDeclared: number
  quantityReceived: number
}

export function getTiktokShipmentList(params: { keyword?: string; siteCode: string; startDate?: string; endDate?: string; current?: number; size?: number }) {
  return request.get<PageResult<TiktokShipment>>(`${BASE}/shipment/list`, params)
}

export function getTiktokShipmentItems(shipmentId: string, siteCode: string) {
  return request.get<TiktokShipmentItem[]>(`${BASE}/shipment/${shipmentId}/items`, { siteCode })
}

export function importTiktokShipment(file: File, siteCode: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('siteCode', siteCode)
  return request.post<{ shipments: number; items: number }>(`${BASE}/shipment/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// ========== 结算单 ==========

export interface TiktokSettlementOrder {
  id: number
  statementId: string
  statementDate: string
  type: string
  orderId: string
  skuId: string
  msku: string
  quantity: number
  productName: string
  skuName: string
  subtotalAfterDiscount: number
  refundAfterDiscount: number
  totalSettlementAmount: number
  commissionFee: number
  logisticsFee: number
  affiliateFee: number
}

export function validateTiktokSettlement(file: File, siteCode: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('siteCode', siteCode)
  return request.post<{ valid: boolean; unmappedSkuIds?: string[]; message: string }>(`${BASE}/settlement/validate`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function importTiktokSettlement(file: File, siteCode: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('siteCode', siteCode)
  return request.post<{ statements: number; orders: number }>(`${BASE}/settlement/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getTiktokSettlementOrders(params: { siteCode: string; type?: string; msku?: string; startDate?: string; endDate?: string; current?: number; size?: number }) {
  return request.get<PageResult<TiktokSettlementOrder>>(`${BASE}/settlement/orders`, params)
}

export function getTiktokStatements(params: { siteCode: string; current?: number; size?: number }) {
  return request.get<PageResult<any>>(`${BASE}/settlement/statements`, params)
}

// ========== 报税汇总（按季度） ==========

export interface MonthTaxSummary {
  month: string
  revenueUsd: number
  revenueRmb: number
  refundUsd: number
  refundRmb: number
  serviceFeeUsd: number
  serviceFeeRmb: number
  orderCount: number
}

export interface QuarterTaxSummary {
  quarter: string
  siteCode: string
  exchangeRate: number
  months: MonthTaxSummary[]
  totalRevenueUsd: number
  totalRevenueRmb: number
  totalRefundUsd: number
  totalRefundRmb: number
  totalServiceFeeUsd: number
  totalServiceFeeRmb: number
}

export interface QuarterOperationSummary {
  quarter: string
  siteCode: string
  exchangeRate: number
  netRevenue: number
  commission: number
  logistics: number
  affiliate: number
  promotion: number
  tax: number
  other: number
  orderProfit: number
  adjustmentIncome: number
  adjustmentExpense: number
  netProfit: number
  marginRate: number
}

export function getTiktokTaxSummary(params: { quarter: string; siteCode?: string; exchangeRate?: number }) {
  return request.get<QuarterTaxSummary>(`${BASE}/report/tax-summary`, params)
}

export function getTiktokOperationSummary(params: { quarter: string; siteCode?: string; exchangeRate?: number }) {
  return request.get<QuarterOperationSummary>(`${BASE}/report/operation-summary`, params)
}

// ========== 单据管理（对齐亚马逊） ==========

export interface TiktokDocumentListItem {
  id: number
  documentType: string
  documentNo: string
  siteCode: string
  documentDate: string
  buyerName: string
  sellerName: string
  totalQuantity: number
  totalAmount: number
  createTime: string
}

export function getTiktokDocumentList(params: {
  documentType?: string
  documentNo?: string
  startDate?: string
  endDate?: string
  siteCode?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get<PageResult<TiktokDocumentListItem>>(`${BASE}/document/list`, params)
}

export function getTiktokPoDetail(id: number) {
  return request.get<any>(`${BASE}/document/po/${id}`)
}

export function getTiktokDnDetail(id: number) {
  return request.get<any>(`${BASE}/document/dn/${id}`)
}

export function getTiktokSettlementDetail(id: number) {
  return request.get<any>(`${BASE}/document/settlement/${id}`)
}

export function getTiktokInvDetail(id: number) {
  return request.get<any>(`${BASE}/document/inv/${id}`)
}

// 生成
export function generateTiktokPo(data: { siteCode: string; shipmentIds: string[] }) {
  return request.post<any>(`${BASE}/document/po/generate`, data)
}

export function generateTiktokDn(data: { siteCode: string; shipmentIds: string[]; anchorDate?: string }) {
  return request.post<any>(`${BASE}/document/dn/generate`, data)
}

export function generateTiktokSettlement(data: { siteCode: string; quarter: string; costAmount: number }) {
  return request.post<any>(`${BASE}/document/settlement/generate`, data)
}

export function generateTiktokInv(settlementId: number) {
  return request.post<any>(`${BASE}/document/inv/generate/${settlementId}`)
}

// 导出
export function exportTiktokPo(id: number) {
  return request.downloadAndSave(`${BASE}/document/export/po/${id}`, `TK-PO_${id}.xlsx`)
}
export function exportTiktokDn(id: number) {
  return request.downloadAndSave(`${BASE}/document/export/dn/${id}`, `TK-DN_${id}.xlsx`)
}
export function exportTiktokSettlement(id: number) {
  return request.downloadAndSave(`${BASE}/document/export/settlement/${id}`, `TK-结算单_${id}.xlsx`)
}
export function exportTiktokInv(id: number) {
  return request.downloadAndSave(`${BASE}/document/export/inv/${id}`, `TK-INV_${id}.xlsx`)
}

export function batchExportPo(ids: number[]) {
  return request.post(`${BASE}/document/export/po/batch`, ids, { responseType: 'blob' })
}
export function batchExportDn(ids: number[]) {
  return request.post(`${BASE}/document/export/dn/batch`, ids, { responseType: 'blob' })
}
export function batchExportSettlement(ids: number[]) {
  return request.post(`${BASE}/document/export/settlement/batch`, ids, { responseType: 'blob' })
}
export function batchExportInv(ids: number[]) {
  return request.post(`${BASE}/document/export/inv/batch`, ids, { responseType: 'blob' })
}

// ========== 交易方配置 ==========

export interface TiktokPartyConfig {
  id?: number
  shopId?: number
  siteCode: string
  buyerName: string
  buyerAddress: string
  buyerPhone: string
  buyerNameEn: string
  sellerName: string
  sellerAddress: string
  sellerPhone: string
  supplierName: string
  customerNameTc: string
  bankAccountName: string
  bankAccountNumber: string
  bankName: string
  bankAddress: string
  swiftCode: string
}

export function getTiktokPartyConfigList() {
  return request.get<TiktokPartyConfig[]>(`${BASE}/party-config/list`)
}

export function getTiktokPartyConfig(siteCode: string) {
  return request.get<TiktokPartyConfig>(`${BASE}/party-config/${siteCode}`)
}

export function saveTiktokPartyConfig(data: TiktokPartyConfig) {
  return request.post<TiktokPartyConfig>(`${BASE}/party-config`, data)
}

export function deleteTiktokPartyConfig(id: number) {
  return request.delete<void>(`${BASE}/party-config/${id}`)
}
