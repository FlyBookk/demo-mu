/**
 * 分片上传API接口
 * 接口路径与后端对齐，使用/api/v1前缀
 */

import { request } from '@/utils/request'
import type {
  UploadInitParams,
  UploadInitResult,
  ChunkUploadParams,
  ChunkUploadResult,
  MergeChunksParams,
  MergeChunksResult,
  FileStatusResult,
  QuickUploadCheckParams,
  QuickUploadCheckResult
} from '@/types/upload'

const BASE_URL = '/api/v1/upload'

/**
 * 上传分片
 */
export function uploadChunk(
  fileId: string,
  chunkNumber: number,
  totalChunks: number,
  fileName: string,
  file: Blob
) {
  const formData = new FormData()
  formData.append('file', file)
  return request.upload<ChunkUploadResult>(`${BASE_URL}/chunk`, formData, {
    params: { fileId, chunkNumber, totalChunks, fileName }
  })
}

/**
 * 合并分片
 */
export function mergeChunks(fileId: string, totalChunks: number, fileName: string) {
  return request.post<MergeChunksResult>(`${BASE_URL}/merge`, null, {
    params: { fileId, totalChunks, fileName }
  })
}

/**
 * 检查分片是否存在
 */
export function checkChunkExists(fileId: string, chunkNumber: number) {
  return request.get<boolean>(`${BASE_URL}/chunk/check`, { fileId, chunkNumber })
}

/**
 * 获取已上传的分片列表
 */
export function getUploadedChunks(fileId: string) {
  return request.get<number[]>(`${BASE_URL}/chunks`, { fileId })
}

/**
 * 清理未完成的上传分片
 */
export function cleanupChunks(fileId: string) {
  return request.delete<void>(`${BASE_URL}/chunks`, { params: { fileId } })
}

/**
 * 删除已上传的文件
 */
export function deleteUploadedFile(fileId: string) {
  return request.delete<void>(`${BASE_URL}/file/${fileId}`)
}

/**
 * 获取上传进度
 */
export function getUploadProgress(fileId: string) {
  return request.get<{
    fileId: string
    progress: number
    uploadedChunks: number
    totalChunks: number
    status: string
  }>(`${BASE_URL}/progress/${fileId}`)
}

/**
 * 简单文件上传（小文件直接上传）
 */
export function simpleUpload(file: File, dataType?: string, marketplaceId?: number) {
  const formData = new FormData()
  formData.append('file', file)
  if (dataType) {
    formData.append('dataType', dataType)
  }
  if (marketplaceId) {
    formData.append('marketplaceId', String(marketplaceId))
  }
  return request.upload<MergeChunksResult>(`${BASE_URL}/simple`, formData)
}
