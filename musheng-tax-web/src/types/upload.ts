/**
 * 分片上传类型定义
 */

// 上传初始化参数
export interface UploadInitParams {
  fileName: string
  fileSize: number
  fileMd5: string
  chunkSize?: number
  dataType?: string
  marketplaceId?: number
}

// 上传初始化响应
export interface UploadInitResult {
  fileId: string
  fileName: string
  fileSize: number
  chunkSize: number
  totalChunks: number
  uploadedChunks: number[]
}

// 分片上传参数
export interface ChunkUploadParams {
  fileId: string
  chunkIndex: number
  chunk: File | Blob
  chunkMd5?: string
}

// 分片上传响应
export interface ChunkUploadResult {
  fileId: string
  chunkIndex: number
  uploaded: boolean
}

// 合并分片参数
export interface MergeChunksParams {
  fileId: string
  fileName: string
  fileMd5: string
}

// 合并分片响应
export interface MergeChunksResult {
  fileId: string
  fileName: string
  fileUrl: string
  fileSize: number
}

// 检查文件状态响应
export interface FileStatusResult {
  fileId: string
  fileName: string
  fileSize: number
  status: UploadStatus
  uploadedChunks: number[]
  totalChunks: number
  progress: number
  fileUrl?: string
}

// 上传状态
export enum UploadStatus {
  INIT = 'INIT',
  UPLOADING = 'UPLOADING',
  MERGING = 'MERGING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

// 上传状态标签映射
export const UploadStatusLabel: Record<UploadStatus, string> = {
  [UploadStatus.INIT]: '初始化',
  [UploadStatus.UPLOADING]: '上传中',
  [UploadStatus.MERGING]: '合并中',
  [UploadStatus.COMPLETED]: '已完成',
  [UploadStatus.FAILED]: '失败',
  [UploadStatus.CANCELLED]: '已取消'
}

// 秒传检查参数
export interface QuickUploadCheckParams {
  fileMd5: string
  fileName: string
  fileSize: number
}

// 秒传检查响应
export interface QuickUploadCheckResult {
  exists: boolean
  fileId?: string
  fileUrl?: string
}
