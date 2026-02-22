<template>
  <div class="shipping-import-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">配送数据导入</h1>
      <p class="page-desc">上传亚马逊配送报告文件，系统将自动解析并导入数据</p>
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
          accept=".csv,.xlsx,.xls"
        >
          <p class="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
          <p class="ant-upload-hint">
            支持 CSV、Excel (.xlsx, .xls) 格式，单个文件最大 50MB<br />
            支持批量上传多个文件，系统将自动识别销售渠道并分配站点，根据配送日期计算数据季度
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
      :file-name="uploadedFiles.length > 0 ? `${uploadedFiles.length} 个文件` : ''"
      data-type="shipping"
      @confirm="doExecuteImport"
    />

    <!-- 步骤2: 导入结果 -->
    <a-card v-show="currentStep === 1" class="step-card">
      <a-result
        :status="importResult?.status === 2 ? 'success' : importResult?.status === 4 ? 'error' : 'info'"
        :title="getResultTitle()"
        :sub-title="getResultSubTitle()"
      >
        <template #extra>
          <a-space>
            <a-button type="primary" @click="handleViewRecords">
              查看导入记录
            </a-button>
            <a-button @click="handleImportAgain">
              继续导入
            </a-button>
          </a-space>
        </template>

        <template v-if="importResult" #icon>
          <LoadingOutlined v-if="importResult.status === 1" spin style="color: #1890ff" />
        </template>
      </a-result>

      <div v-if="importResult" class="import-progress">
        <a-descriptions :column="3" size="small" bordered>
          <a-descriptions-item v-if="importResult.totalFiles > 1" label="总文件数">{{ importResult.totalFiles }}</a-descriptions-item>
          <a-descriptions-item v-if="importResult.totalFiles > 1" label="成功文件">
            <span class="success-text">{{ importResult.successFiles }}</span>
          </a-descriptions-item>
          <a-descriptions-item v-if="importResult.totalFiles > 1" label="失败文件">
            <span class="error-text">{{ importResult.failFiles }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="批次号">
            <a-typography-text copyable>{{ importResult.batchNo || '-' }}</a-typography-text>
          </a-descriptions-item>
          <a-descriptions-item label="总行数">{{ importResult.totalRows }}</a-descriptions-item>
          <a-descriptions-item label="成功">
            <span class="success-text">{{ importResult.successRows }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="失败">
            <span class="error-text">{{ importResult.failedRows }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="重复">
            <span class="warning-text">{{ importResult.duplicateRows || 0 }}</span>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { UploadProps, UploadFile } from 'ant-design-vue'
import {
  InboxOutlined,
  CloudUploadOutlined,
  LoadingOutlined
} from '@ant-design/icons-vue'
import { importShippingData, batchImportShippingData } from '@/api/shipping'
import ImportConfirmModal from '@/components/business/ImportConfirmModal/index.vue'
import type { ImportRecord } from '@/types/importRecord'

const router = useRouter()

// ============= 步骤相关 =============
const currentStep = ref(0)
const stepItems = [
  { title: '上传文件', description: 'CSV/Excel' },
  { title: '导入结果', description: '查看状态' }
]

// ============= 上传相关 =============
const fileList = ref<UploadFile[]>([])
const uploadedFiles = ref<File[]>([])

// ============= 导入相关 =============
const importing = ref(false)
const importResult = ref<(ImportRecord & { totalFiles?: number; successFiles?: number; failFiles?: number }) | null>(null)
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

  if (uploadedFiles.value.some(f => f.name === file.name && f.size === file.size)) {
    message.info('文件已存在，无需重复添加')
    return false
  }

  uploadedFiles.value.push(file as File)
  message.success(`已添加文件: ${file.name}`)

  return false
}

function removeFile(file: File) {
  const index = uploadedFiles.value.findIndex(f => f.name === file.name && f.size === file.size)
  if (index > -1) {
    uploadedFiles.value.splice(index, 1)
    fileList.value = fileList.value.filter(f => f.name !== file.name || f.size !== file.size)
  }
}

function clearFiles() {
  uploadedFiles.value = []
  fileList.value = []
}

// 点击开始导入 - 弹出确认窗口
function handleStartImport() {
  if (uploadedFiles.value.length === 0) {
    message.warning('请先选择文件')
    return
  }

  importConfirmRef.value?.show()
}

// 确认导入后执行
async function doExecuteImport() {
  importing.value = true
  importConfirmRef.value?.setLoading(true)

  try {
    let result: { data: Record<string, unknown> }

    if (uploadedFiles.value.length === 1) {
      result = await importShippingData(uploadedFiles.value[0])
      const d = result.data
      importResult.value = {
        batchNo: d.batchNo as string,
        totalRows: d.totalCount as number,
        successRows: d.successCount as number,
        failedRows: d.failCount as number,
        duplicateRows: (d.duplicateCount as number) || 0,
        totalFiles: 1,
        successFiles: 1,
        failFiles: 0,
        status: (d.failCount as number) === 0 && ((d.duplicateCount as number) || 0) === 0 ? 2 : 3
      }
    } else {
      result = await batchImportShippingData(uploadedFiles.value)
      const d = result.data
      importResult.value = {
        batchNo: d.batchNo as string,
        totalRows: d.totalCount as number,
        successRows: d.successCount as number,
        failedRows: d.failCount as number,
        duplicateRows: (d.duplicateCount as number) || 0,
        totalFiles: d.totalFiles as number,
        successFiles: d.successFiles as number,
        failFiles: d.failFiles as number,
        status: (d.failFiles as number) === 0 ? 2 : 3
      }
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

function getResultTitle(): string {
  if (!importResult.value) return ''
  switch (importResult.value.status) {
    case 1: return '正在处理中...'
    case 2: return importResult.value.totalFiles && importResult.value.totalFiles > 1 ? '全部导入成功' : '导入成功'
    case 3: return '部分导入成功'
    case 4: return '导入失败'
    default: return '等待处理'
  }
}

function getResultSubTitle(): string {
  if (!importResult.value) return ''
  if (importResult.value.status === 1) {
    return '系统正在后台处理数据,请稍候查看导入记录'
  }
  const parts: string[] = []
  if (importResult.value.totalFiles && importResult.value.totalFiles > 1) {
    parts.push(`成功 ${importResult.value.successFiles} 个文件`)
    if (importResult.value.failFiles && importResult.value.failFiles > 0) {
      parts.push(`失败 ${importResult.value.failFiles} 个文件`)
    }
  }
  if (importResult.value.successRows > 0) {
    parts.push(`成功导入 ${importResult.value.successRows} 条`)
  }
  if (importResult.value.failedRows > 0) {
    parts.push(`失败 ${importResult.value.failedRows} 条`)
  }
  if (importResult.value.duplicateRows && importResult.value.duplicateRows > 0) {
    parts.push(`重复 ${importResult.value.duplicateRows} 条`)
  }
  return parts.join('，')
}

function handleViewRecords() {
  router.push('/config/import-record')
}

function handleImportAgain() {
  currentStep.value = 0
  fileList.value = []
  uploadedFiles.value = []
  importResult.value = null
}

// 初始化无需加载额外数据
onMounted(() => {
  // 页面已准备就绪
})
</script>

<style lang="scss" scoped>
.shipping-import-page {
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
    }
  }

    .step-actions {
      margin-top: $spacing-lg;
      text-align: center;
    }

    .import-tip {
      margin-top: $spacing-lg;
    }
  }

  .import-progress {
    margin-top: $spacing-lg;
    padding: $spacing-md;
    background: $background-color-light;
    border-radius: $border-radius-md;
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
