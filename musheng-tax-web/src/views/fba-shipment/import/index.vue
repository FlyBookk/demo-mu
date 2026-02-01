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
          :multiple="true"
          :before-upload="beforeUpload"
          accept=".xlsx,.xls"
        >
          <p class="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
          <p class="ant-upload-hint">
            支持 Excel (.xlsx, .xls) 格式，单个文件最大 50MB<br />
            支持批量上传多个文件，系统会自动去重（已导入的文件会被跳过）<br />
            请确保Excel文件包含"发货单详情"工作表
          </p>
        </a-upload-dragger>

        <div v-if="uploadedFiles.length > 0" class="uploaded-file-info">
          <a-card size="small">
            <div class="file-list-header">
              <span>已选择 {{ uploadedFiles.length }} 个文件</span>
              <a-button type="link" size="small" @click="clearFiles">清空</a-button>
            </div>
            <a-list size="small" :data-source="uploadedFiles">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta>
                    <template #title>{{ item.name }}</template>
                    <template #description>{{ formatFileSize(item.size) }}</template>
                  </a-list-item-meta>
                  <template #actions>
                    <a-button type="link" size="small" danger @click="removeFile(item)">删除</a-button>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </div>
      </div>

      <div class="step-actions">
        <a-button type="primary" :disabled="uploadedFiles.length === 0" :loading="importing" @click="handleStartImport">
          <CloudUploadOutlined /> 开始导入 ({{ uploadedFiles.length }} 个文件)
        </a-button>
      </div>
    </a-card>

    <!-- 导入确认弹窗 -->
    <ImportConfirmModal
      ref="importConfirmRef"
      :file-name="`${uploadedFiles.length} 个文件`"
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
        <a-descriptions :column="3" size="small" bordered>
          <a-descriptions-item label="总文件数">{{ importResult.totalFiles }}</a-descriptions-item>
          <a-descriptions-item label="成功文件">
            <span class="success-text">{{ importResult.successFiles }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="失败文件">
            <span class="error-text">{{ importResult.failFiles }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="总SKU数">{{ importResult.totalSkuCount }}</a-descriptions-item>
          <a-descriptions-item label="成功SKU">
            <span class="success-text">{{ importResult.successSkuCount }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="失败SKU">
            <span class="error-text">{{ importResult.failSkuCount }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="重复SKU（已跳过）">
            <span class="warning-text">{{ importResult.duplicateSkuCount || 0 }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="成功货件">
            <span class="success-text">{{ importResult.totalShipmentCount }}</span>
          </a-descriptions-item>
        </a-descriptions>

        <!-- 文件详情列表 -->
        <div v-if="importResult.fileResults && importResult.fileResults.length > 0" class="file-results">
          <h4 style="margin-top: 16px; margin-bottom: 12px">文件导入详情</h4>
          <a-table
            :columns="fileResultColumns"
            :data-source="importResult.fileResults"
            :pagination="false"
            size="small"
            row-key="fileName"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag v-if="record.status === 'success'" color="success">成功</a-tag>
                <a-tag v-else color="error">失败</a-tag>
              </template>
              <template v-else-if="column.key === 'message'">
                <template v-if="record.result">
                  导入 {{ record.result.shipmentCount }} 个货件
                  <span v-if="record.result.duplicateCount > 0" class="warning-text">
                    (跳过 {{ record.result.duplicateCount }} 个重复SKU)
                  </span>
                </template>
                <template v-else>
                  {{ record.message || '-' }}
                </template>
              </template>
            </template>
          </a-table>
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
import type { FbaShipmentImportResult, FbaShipmentBatchImportResult } from '@/types/fbaShipment'
import { batchImportFbaShipment } from '@/api/fbaShipment'

const router = useRouter()

// ============= 步骤相关 =============
const currentStep = ref(0)
const stepItems = [
  { title: '上传文件', description: 'Excel文件' },
  { title: '导入结果', description: '查看状态' }
]

// 文件结果表格列定义
const fileResultColumns = [
  {
    title: '文件名',
    dataIndex: 'fileName',
    key: 'fileName',
    ellipsis: true
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    align: 'center' as const
  },
  {
    title: '结果说明',
    key: 'message',
    ellipsis: true
  }
]

// ============= 上传相关 =============
const fileList = ref<UploadFile[]>([])
const uploadedFiles = ref<File[]>([])

// ============= 导入相关 =============
const importing = ref(false)
const importResult = ref<FbaShipmentBatchImportResult | null>(null)
const importConfirmRef = ref<InstanceType<typeof ImportConfirmModal> | null>(null)

// ============= 方法 =============
function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isValidType = [
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ].includes(file.type) || file.name.endsWith('.xlsx') || file.name.endsWith('.xls')

  if (!isValidType) {
    message.error('只能上传 Excel 文件!')
    return false
  }

  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    message.error('文件大小不能超过 50MB!')
    return false
  }

  // 添加到文件列表
  uploadedFiles.value.push(file as File)
  message.success(`已添加文件: ${file.name}`)

  // 返回 false 阻止自动上传
  return false
}

function removeFile(file: File) {
  const index = uploadedFiles.value.findIndex(f => f.name === file.name)
  if (index > -1) {
    uploadedFiles.value.splice(index, 1)
    // 同步更新 fileList
    fileList.value = fileList.value.filter(f => f.name !== file.name)
  }
}

function clearFiles() {
  uploadedFiles.value = []
  fileList.value = []
}

// 点击开始导入 - 弹出确认窗口
function handleStartImport() {
  if (uploadedFiles.value.length === 0) {
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
    // 调用批量导入API
    const result = await batchImportFbaShipment(uploadedFiles.value)

    // 保存导入结果
    importResult.value = result.data

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
  if (importResult.value.failFiles === 0) {
    return 'success'
  }
  if (importResult.value.successFiles > 0) {
    return 'warning'
  }
  return 'error'
}

function getResultTitle(): string {
  if (!importResult.value) return '等待处理'
  if (importResult.value.failFiles === 0) {
    return '全部导入成功'
  }
  if (importResult.value.successFiles > 0) {
    return '部分导入成功'
  }
  return '导入失败'
}

function getResultSubTitle(): string {
  if (!importResult.value) return ''
  const parts: string[] = []
  if (importResult.value.successFiles > 0) {
    parts.push(`成功 ${importResult.value.successFiles} 个文件`)
  }
  if (importResult.value.failFiles > 0) {
    parts.push(`失败 ${importResult.value.failFiles} 个文件`)
  }
  if (importResult.value.totalShipmentCount > 0) {
    parts.push(`共导入 ${importResult.value.totalShipmentCount} 个货件`)
  }
  if (importResult.value.duplicateSkuCount && importResult.value.duplicateSkuCount > 0) {
    parts.push(`跳过 ${importResult.value.duplicateSkuCount} 个重复SKU`)
  }
  return parts.join('，')
}

function handleViewList() {
  router.push('/fba-shipment/list')
}

function handleImportAgain() {
  currentStep.value = 0
  fileList.value = []
  uploadedFiles.value = []
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

      .file-list-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: $spacing-sm;
        font-weight: 600;
      }
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

    .file-results {
      margin-top: $spacing-md;
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
