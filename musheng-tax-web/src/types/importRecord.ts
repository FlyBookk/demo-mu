/**
 * 导入记录类型定义
 */

// 导入记录
export interface ImportRecord {
  id: number
  batchNo: string
  fileName: string
  fileSize: number
  dataType: ImportDataType
  marketplaceId?: number
  marketplaceName?: string
  templateId?: number
  templateName?: string
  totalRows: number
  successRows: number
  failedRows: number
  duplicateRows?: number  // 重复数据条数
  status: ImportStatus
  errorMessage?: string
  errorFileUrl?: string
  startTime: string
  endTime?: string
  createTime: string
  createdBy: string
}

// 导入数据类型
export type ImportDataType = 'SALES' | 'SHIPPING' | 'ADVERTISING' | 'RATE'

// 导入状态
export enum ImportStatus {
  PENDING = 0,
  PROCESSING = 1,
  SUCCESS = 2,
  PARTIAL_SUCCESS = 3,
  FAILED = 4
}

// 导入状态标签映射
export const ImportStatusLabel: Record<ImportStatus, string> = {
  [ImportStatus.PENDING]: '待处理',
  [ImportStatus.PROCESSING]: '处理中',
  [ImportStatus.SUCCESS]: '成功',
  [ImportStatus.PARTIAL_SUCCESS]: '部分成功',
  [ImportStatus.FAILED]: '失败'
}

// 导入记录查询参数
export interface ImportRecordQuery {
  keyword?: string
  dataType?: ImportDataType
  marketplaceId?: number
  status?: ImportStatus
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

// 导入记录详情（包含错误明细）
export interface ImportRecordDetail extends ImportRecord {
  errorDetails?: ImportErrorDetail[]
}

// 导入错误详情
export interface ImportErrorDetail {
  rowNumber: number
  fieldName?: string
  originalValue?: string
  errorMessage: string
}
