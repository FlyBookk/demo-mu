<template>
  <div class="fba-shipment-import-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">FBA货件明细导入</h1>
      <p class="page-desc">上传亚马逊FBA货件明细报告文件，系统将自动解析并导入数据</p>
    </div>

    <!-- 导入步骤 -->
    <a-card class="steps-card">
      <a-steps :current="currentStep" :items="stepItems" />
    </a-card>

    <!-- 步骤1: 上传文件 -->
    <a-card v-show="currentStep === 0" class="step-card">
      <div class="upload-area">
        <a-upload-dragger
          v-model:file-list="fileList"
          name="file"
          :multiple="false"
          :before-upload="beforeUpload"
          accept=".csv,.xlsx,.xls"
        >
          <p class="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
          <p class="ant-upload-hint">
            支持 CSV、Excel (.xlsx, .xls) 格式，单个文件最大 50MB<br />
            系统将自动识别编码（UTF-8/GBK）并解析日期格式
          </p>
        </a-upload-dragger>

        <div v-if="uploadedFileInfo" class="uploaded-file-info">
          <a-card size="small">
            <a-descriptions :column="2">
              <a-descriptions-item label="文件名">{{ uploadedFileInfo.fileName }}</a-descriptions-item>
              <a-descriptions-item label="文件大小">{{ formatFileSize(uploadedFileInfo.fileSize) }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </div>
      </div>

      <div class="step-actions">
        <a-button type="primary" :disabled="!uploadedFileInfo" :loading="importing" @click="handleStartImport">
          <CloudUploadOutlined /> 开始导入
        </a-button>
      </div>
    </a-card>

    <!-- 导入确认弹窗 -->
    <ImportConfirmModal
      ref="importConfirmRef"
      :file-name="uploadedFileInfo?.fileName"
      data-type="fba-shipment"
      @confirm="doExecuteImport"
    />

    <!-- 步骤2: 导入结果 -->
    <a-card v-show="currentStep === 1" class="step-card">
      <a-result
        :status="getResultStatus()"
        :title="getResultTitle()"
        :sub-title="getResultSubTitle()"
      >
        <template #extra>
          <a-space>
            <a-button type="primary" @click="handleViewList">
              查看货件列表
            </a-button>
            <a-button @click="handleImportAgain">
              继续导入
            </a-button>
          </a-space>
        </template>
      </a-result>

      <div v-if="importResult" class="import-progress">
        <a-descriptions :column="3" size="small">
          <a-descriptions-item label="批次号">
            <a-typography-text copyable>{{ importResult.batchNo }}</a-typography-text>
          </a-descriptions-item>
          <a-descriptions-item label="总行数">{{ importResult.totalCount }}</a-descriptions-item>
          <a-descriptions-item label="成功">
            <span class="success-text">{{ importResult.successCount }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="失败">
            <span class="error-text">{{ importResult.failCount }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="重复">
            <span class="warning-text">{{ importResult.duplicateCount }}</span>
          </a-descriptions-item>
        </a-descriptions>

        <!-- 错误信息展示 -->
        <div v-if="importResult.errors && importResult.errors.length > 0" class="error-details">
          <a-alert
            type="warning"
            message="部分数据导入失败或重复"
            show-icon
          >
            <template #description>
              <div class="error-list">
                <div v-for="(error, index) in importResult.errors.slice(0, 10)" :key="index" class="error-item">
                  {{ error }}
                </div>
                <div v-if="importResult.errors.length > 10" class="error-more">
                  ... 还有 {{ importResult.errors.length - 10 }} 条错误信息
                </div>
              </div>
            </template>
          </a-alert>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { UploadProps, UploadFile } from 'ant-design-vue'
import {
  InboxOutlined,
  CloudUploadOutlined
} from '@ant-design/icons-vue'
import { request } from '@/utils/request'
import ImportConfirmModal from '@/components/business/ImportConfirmModal/index.vue'
import type { FbaShipmentImportResult } from '@/types/fbaShipment'

const router = useRouter()

// ============= 步骤相关 =============
const currentStep = ref(0)
const stepItems = [
  { title: '上传文件', description: 'CSV/Excel' },
  { title: '导入结果', description: '查看状态' }
]

// ============= 上传相关 =============
const fileList = ref<UploadFile[]>([])
const uploadedFile = ref<File | null>(null)
const uploadedFileInfo = ref<{ fileName: string; fileSize: number } | null>(null)

// ============= 导入相关 =============
const importing = ref(false)
const importResult = ref<FbaShipmentImportResult | null>(null)
const importConfirmRef = ref<InstanceType<typeof ImportConfirmModal> | null>(null)

// ============= 方法 =============
function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isValidType = [
    'text/csv',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ].includes(file.type) || file.name.endsWith('.csv')

  if (!isValidType) {
    message.error('只能上传 CSV 或 Excel 文件!')
    return false
  }

  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    message.error('文件大小不能超过 50MB!')
    return false
  }

  // 保存文件用于后续导入
  uploadedFile.value = file
  uploadedFileInfo.value = {
    fileName: file.name,
    fileSize: file.size
  }
  message.success('文件已选择')

  // 返回 false 阻止自动上传
  return false
}

// 点击开始导入 - 弹出确认窗口
function handleStartImport() {
  if (!uploadedFile.value) {
    message.warning('请先上传文件')
    return
  }
  
  // 弹出确认窗口
  importConfirmRef.value?.show()
}

// 确认导入后执行
async function doExecuteImport() {
  importing.value = true
  importConfirmRef.value?.setLoading(true)
  
  try {
    // 使用 request.upload 上传文件（自动添加token）
    const formData = new FormData()
    formData.append('file', uploadedFile.value!)

    const result = await request.upload<any>('/api/v1/business/fba-shipment/import', formData)

    // 保存导入结果
    importResult.value = {
      totalCount: result.data.totalCount,
      successCount: result.data.successCount,
      failCount: result.data.failCount,
      duplicateCount: result.data.duplicateCount,
      errors: result.data.errors || [],
      batchNo: result.data.batchNo
    }

    importConfirmRef.value?.hide()
    currentStep.value = 1
    message.success('导入完成')
  } catch (error: any) {
    console.error('导入失败:', error)
    message.error(error.message || '导入失败')
    importConfirmRef.value?.setLoading(false)
  } finally {
    importing.value = false
  }
}

function getResultStatus() {
  if (!importResult.value) return 'info'
  if (importResult.value.failCount === 0 && importResult.value.duplicateCount === 0) {
    return 'success'
  }
  if (importResult.value.successCount > 0) {
    return 'warning'
  }
  return 'error'
}

function getResultTitle(): string {
  if (!importResult.value) return '等待处理'
  if (importResult.value.failCount === 0 && importResult.value.duplicateCount === 0) {
    return '导入成功'
  }
  if (importResult.value.successCount > 0) {
    return '部分导入成功'
  }
  return '导入失败'
}

function getResultSubTitle(): string {
  if (!importResult.value) return ''
  const parts: string[] = []
  if (importResult.value.successCount > 0) {
    parts.push(`成功导入 ${importResult.value.successCount} 条`)
  }
  if (importResult.value.failCount > 0) {
    parts.push(`失败 ${importResult.value.failCount} 条`)
  }
  if (importResult.value.duplicateCount > 0) {
    parts.push(`重复 ${importResult.value.duplicateCount} 条`)
  }
  return parts.join('，')
}

function handleViewList() {
  router.push('/fba-shipment/list')
}

function handleImportAgain() {
  currentStep.value = 0
  fileList.value = []
  uploadedFile.value = null
  uploadedFileInfo.value = null
  importResult.value = null
}
</script>

<style lang="scss" scoped>
.fba-shipment-import-page {
  padding: $spacing-lg;

  .page-header {
    margin-bottom: $spacing-lg;

    .page-title {
      font-size: $font-size-xl;
      font-weight: 600;
      color: $text-color;
      margin: 0 0 $spacing-xs 0;
    }

    .page-desc {
      font-size: $font-size-md;
      color: $text-color-secondary;
      margin: 0;
    }
  }

  .steps-card {
    margin-bottom: $spacing-lg;
  }

  .step-card {
    .upload-area {
      max-width: 600px;
      margin: 0 auto;
    }

    .uploaded-file-info {
      margin-top: $spacing-md;
    }

    .step-actions {
      margin-top: $spacing-lg;
      text-align: center;
    }
  }

  .import-progress {
    margin-top: $spacing-lg;
    padding: $spacing-md;
    background: $background-color-light;
    border-radius: $border-radius-md;

    .error-details {
      margin-top: $spacing-md;

      .error-list {
        max-height: 200px;
        overflow-y: auto;

        .error-item {
          padding: 4px 0;
          font-size: $font-size-sm;
          color: $text-color-secondary;
        }

        .error-more {
          padding: 4px 0;
          font-size: $font-size-sm;
          color: $warning-color;
          font-weight: 600;
        }
      }
    }
  }

  .success-text {
    color: $success-color;
    font-weight: 600;
  }

  .error-text {
    color: $error-color;
    font-weight: 600;
  }

  .warning-text {
    color: $warning-color;
    font-weight: 600;
  }
}
</style>
