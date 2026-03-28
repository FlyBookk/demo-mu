/**
 * FBA单据管理API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  DocumentListVO,
  PoVO,
  DnVO,
  SettlementVO,
  InvVO,
  PoGenerateRequest,
  DnGenerateRequest,
  SettlementGenerateRequest,
  DocumentQueryRequest
} from '@/types/document'

const BASE_URL = '/api/v1/business/document'

// ==================== 生成API ====================

/** 生成PO采购订单 */
export function generatePo(data: PoGenerateRequest) {
  return request.post<any>(`${BASE_URL}/po/generate`, data)
}

/** 生成DN送货单 */
export function generateDn(data: DnGenerateRequest) {
  return request.post<any>(`${BASE_URL}/dn/generate`, data)
}

/** 生成结算单（4份，按站点拆分） */
export function generateSettlements(data: SettlementGenerateRequest) {
  return request.post<any[]>(`${BASE_URL}/settlement/generate`, data)
}

/** 生成INV发票（4份） */
export function generateInvoices(settlementIds: number[]) {
  return request.post<any[]>(`${BASE_URL}/inv/generate`, settlementIds)
}


// ==================== 查询API ====================

/** 单据列表（分页查询） */
export function getDocumentList(params: DocumentQueryRequest) {
  return request.get<PageResult<DocumentListVO>>(`${BASE_URL}/list`, params)
}

/** PO详情 */
export function getPoDetail(id: number) {
  return request.get<PoVO>(`${BASE_URL}/po/${id}`)
}

/** DN详情 */
export function getDnDetail(id: number) {
  return request.get<DnVO>(`${BASE_URL}/dn/${id}`)
}

/** 结算单详情 */
export function getSettlementDetail(id: number) {
  return request.get<SettlementVO>(`${BASE_URL}/settlement/${id}`)
}

/** INV详情 */
export function getInvDetail(id: number) {
  return request.get<InvVO>(`${BASE_URL}/inv/${id}`)
}

/** 按结算周期查看关联单据 */
export function listBySettlementPeriod(periodStart: string, periodEnd: string) {
  return request.get<DocumentListVO[]>(`${BASE_URL}/period`, { periodStart, periodEnd })
}

// ==================== 导出API ====================

/** 导出PO Excel */
export function exportPo(id: number) {
  const filename = `PO采购订单_${id}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/export/po/${id}`, filename)
}

/** 导出DN Excel */
export function exportDn(id: number) {
  const filename = `DN送货单_${id}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/export/dn/${id}`, filename)
}

/** 导出结算单 Excel */
export function exportSettlement(id: number) {
  const filename = `结算单_${id}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/export/settlement/${id}`, filename)
}

/** 导出INV Excel */
export function exportInv(id: number) {
  const filename = `INV发票_${id}.xlsx`
  return request.downloadAndSave(`${BASE_URL}/export/inv/${id}`, filename)
}

/** 批量导出结算单为ZIP */
export function batchExportSettlementZip(settlementIds: number[]) {
  const filename = `结算单_${settlementIds.length}份.zip`
  return request.downloadAndSavePost(`${BASE_URL}/export/settlement/batch`, filename, settlementIds)
}

/** 批量导出INV为ZIP */
export function batchExportInvZip(invIds: number[]) {
  const filename = `INV发票_${invIds.length}份.zip`
  return request.downloadAndSavePost(`${BASE_URL}/export/inv/batch`, filename, invIds)
}

/** 批量导出PO为ZIP */
export function batchExportPoZip(poIds: number[]) {
  const filename = `PO采购订单_${poIds.length}份.zip`
  return request.downloadAndSavePost(`${BASE_URL}/export/po/batch`, filename, poIds)
}

/** 批量导出DN为ZIP */
export function batchExportDnZip(dnIds: number[]) {
  const filename = `DN送货单_${dnIds.length}份.zip`
  return request.downloadAndSavePost(`${BASE_URL}/export/dn/batch`, filename, dnIds)
}

/** 批量导出结算周期8份文件 */
export function batchExportByPeriod(periodStart: string, periodEnd: string) {
  const filename = `结算周期_${periodStart}_${periodEnd}.zip`
  return request.downloadAndSave(`${BASE_URL}/export/period`, filename, { periodStart, periodEnd })
}

// ==================== 结算数据导入API ====================

/** 导入结算数据（JSON方式） */
export function importSettlementData(data: any) {
  return request.post<number>(`${BASE_URL}/settlement-data/import`, data)
}

/** 校验结算周期数据一致性 */
export function validatePeriod(periodStart: string, periodEnd: string) {
  return request.get<Record<string, string[]>>(`${BASE_URL}/validate/period`, { periodStart, periodEnd })
}
