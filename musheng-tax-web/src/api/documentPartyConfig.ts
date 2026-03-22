/**
 * FBA单据交易方配置API接口
 */

import { request } from '@/utils/request'

const BASE_URL = '/api/v1/business/document-party-config'

export interface DocumentPartyConfig {
  id?: number
  siteCode: string
  buyerName: string
  buyerAddress?: string
  buyerPhone?: string
  buyerNameEn?: string
  sellerName: string
  sellerAddress?: string
  sellerPhone?: string
  supplierName: string
  customerNameTc: string
  bankAccountName?: string
  bankAccountNumber?: string
  bankName?: string
  bankAddress?: string
  swiftCode?: string
}

/** 根据站点代码查询配置 */
export function getPartyConfigBySiteCode(siteCode: string) {
  return request.get<DocumentPartyConfig>(`${BASE_URL}/${siteCode}`)
}

/** 查询所有站点配置 */
export function listAllPartyConfigs() {
  return request.get<DocumentPartyConfig[]>(`${BASE_URL}/list`)
}

/** 新增配置 */
export function addPartyConfig(data: DocumentPartyConfig) {
  return request.post<void>(`${BASE_URL}/add`, data)
}

/** 更新配置 */
export function updatePartyConfig(data: DocumentPartyConfig) {
  return request.put<void>(`${BASE_URL}/update`, data)
}

/** 删除配置 */
export function deletePartyConfig(id: number) {
  return request.delete<void>(`${BASE_URL}/${id}`)
}

/** 复制配置到目标站点（目标站点已存在则更新，不存在则新增） */
export function copyPartyConfig(sourceId: number, targetSiteCode: string) {
  return request.post<void>(`${BASE_URL}/copy`, { sourceId, targetSiteCode })
}
