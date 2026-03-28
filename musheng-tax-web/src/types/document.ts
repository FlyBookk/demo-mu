/**
 * FBA单据管理类型定义
 * 对应后端 VO/DTO 类
 */

// ==================== 列表视图 ====================

/** 单据列表通用视图 */
export interface DocumentListVO {
  id: number
  documentType: string
  documentNo: string
  documentDate: string
  buyerName: string
  sellerName: string
  totalQuantity: number
  totalAmount: number | null
}

// ==================== PO ====================

export interface PoItemVO {
  id: number
  shipmentNo: string
  msku: string
  quantity: number
  fbaAddress: string
  sortOrder: number
}

export interface PoVO {
  id: number
  documentNo: string
  poDate: string
  buyerName: string
  buyerAddress: string
  sellerName: string
  totalQuantity: number
  shipmentCount: number
  items: PoItemVO[]
}

// ==================== DN ====================

export interface DnItemVO {
  id: number
  lineNo: number
  msku: string
  quantity: number
  shipmentNo: string
}


export interface DnVO {
  id: number
  documentNo: string
  dnDate: string
  supplierName: string
  customerName: string
  totalQuantity: number
  periodStart: string
  periodEnd: string
  items: DnItemVO[]
}

// ==================== 结算单 ====================

export interface SettlementItemVO {
  id: number
  lineNo: number
  msku: string
  currency: string
  unitPrice: number
  quantity: number
  amount: number
}

export interface SettlementVO {
  id: number
  documentNo: string
  settlementDate: string
  periodStart: string
  periodEnd: string
  siteCode: string
  siteSequence: string
  buyerName: string
  sellerName: string
  totalQuantity: number
  totalAmount: number
  items: SettlementItemVO[]
}

// ==================== INV ====================

export interface InvItemVO {
  id: number
  lineNo: number
  msku: string
  quantity: number
  unitPrice: number
  amount: number
}

export interface InvVO {
  id: number
  documentNo: string
  invDate: string
  settlementId: number
  siteCode: string
  siteSequence: string
  sellerName: string
  sellerAddress: string
  buyerName: string
  buyerAddress: string
  sellerPhone: string
  buyerPhone: string
  totalQuantity: number
  totalAmount: number
  bankAccountName: string
  bankAccountNumber: string
  bankName: string
  bankAddress: string
  swiftCode: string
  items: InvItemVO[]
}

// ==================== 请求类型 ====================

/** PO生成请求 */
export interface PoGenerateRequest {
  siteCode: string
  shipmentIds: number[]
}

/** DN生成请求 */
export interface DnGenerateRequest {
  siteCode: string
  anchorDate: string
  shipmentIds: number[]
}

/** 结算单生成请求 */
export interface SettlementGenerateRequest {
  periodStart: string
  periodEnd: string
  siteCodes: string[]
}

/** 结算数据导入明细项 */
export interface SettlementImportItem {
  siteCode: string
  msku: string
  currency: string
  unitPrice: number
  quantity: number
  amount: number
}

/** 结算数据导入请求 */
export interface SettlementImportRequest {
  shopId: number
  periodStart: string
  periodEnd: string
  items: SettlementImportItem[]
}

/** 单据查询请求 */
export interface DocumentQueryRequest {
  documentType?: string
  documentNo?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

/** 单据类型选项 */
export const DocumentTypeOptions = [
  { label: 'PO采购订单', value: 'PO' },
  { label: 'DN送货单', value: 'DN' },
  { label: '结算单', value: 'SETTLEMENT' },
  { label: 'INV发票', value: 'INV' }
]
