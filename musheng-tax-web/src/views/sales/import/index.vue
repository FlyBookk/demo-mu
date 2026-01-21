<template>
  <div class="sales-import-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">销售数据导入</h1>
      <p class="page-desc">上传亚马逊销售报告文件，系统将自动解析并导入数据</p>
    </div>

    <!-- 导入步骤 -->
    <a-card class="steps-card">
      <a-steps :current="currentStep" :items="stepItems" />
    </a-card>

    <!-- 步骤1: 选择站点和模板 -->
    <a-card v-show="currentStep === 0" class="step-card">
      <a-form
        ref="step1FormRef"
        :model="importForm"
        :rules="step1Rules"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 12 }"
      >
        <a-form-item label="选择站点" name="marketplaceId">
          <a-select
            v-model:value="importForm.marketplaceId"
            placeholder="请选择导入数据所属站点"
            show-search
            :filter-option="filterOption"
            @change="handleMarketplaceChange"
          >
            <a-select-option
              v-for="marketplace in marketplaceOptions"
              :key="marketplace.id"
              :value="marketplace.id"
            >
              {{ marketplace.siteCode }} - {{ marketplace.siteName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="字段映射模板" name="templateId">
          <a-select
            v-model:value="importForm.templateId"
            placeholder="请选择字段映射模板"
            :disabled="!importForm.marketplaceId"
          >
            <a-select-option
              v-for="template in templateOptions"
              :key="template.id"
              :value="template.id"
            >
              {{ template.name }}
              <a-tag v-if="template.isDefault" color="gold" size="small">默认</a-tag>
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="数据季度" name="quarter">
          <a-date-picker
            v-model:value="importForm.quarterDate"
            picker="quarter"
            placeholder="选择数据所属季度（可选）"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 4, span: 12 }">
          <a-button type="primary" @click="handleStep1Next">
            下一步 <RightOutlined />
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 步骤2: 上传文件 -->
    <a-card v-show="currentStep === 1" class="step-card">
      <div class="upload-area">
        <a-upload-dragger
          v-model:file-list="fileList"
          name="file"
          :multiple="false"
          :before-upload="beforeUpload"
          :custom-request="handleUpload"
          accept=".csv,.xlsx,.xls"
          :disabled="uploading"
        >
          <p class="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
          <p class="ant-upload-hint">
            支持 CSV、Excel (.xlsx, .xls) 格式，单个文件最大 50MB
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
        <a-space>
          <a-button @click="currentStep = 0">
            <LeftOutlined /> 上一步
          </a-button>
          <a-button type="primary" :disabled="!uploadedFileInfo" @click="handleStep2Next">
            下一步 <RightOutlined />
          </a-button>
        </a-space>
      </div>
    </a-card>

    <!-- 步骤3: 确认导入 -->
    <a-card v-show="currentStep === 2" class="step-card">
      <a-descriptions title="导入信息确认" :column="2" bordered>
        <a-descriptions-item label="站点">
          {{ getMarketplaceName(importForm.marketplaceId) }}
        </a-descriptions-item>
        <a-descriptions-item label="映射模板">
          {{ getTemplateName(importForm.templateId) }}
        </a-descriptions-item>
        <a-descriptions-item label="数据季度">
          {{ importForm.quarterDate?.format('YYYY年Q季度') || '未指定' }}
        </a-descriptions-item>
        <a-descriptions-item label="文件名">
          {{ uploadedFileInfo?.fileName }}
        </a-descriptions-item>
        <a-descriptions-item label="文件大小">
          {{ formatFileSize(uploadedFileInfo?.fileSize || 0) }}
        </a-descriptions-item>
      </a-descriptions>

      <a-alert
        class="import-tip"
        type="info"
        show-icon
        message="导入说明"
        description="点击开始导入后，系统将在后台处理数据。您可以在导入记录页面查看进度和结果。"
      />

      <div class="step-actions">
        <a-space>
          <a-button @click="currentStep = 1">
            <LeftOutlined /> 上一步
          </a-button>
          <a-button type="primary" :loading="importing" @click="handleStartImport">
            <CloudUploadOutlined /> 开始导入
          </a-button>
        </a-space>
      </div>
    </a-card>

    <!-- 步骤4: 导入结果 -->
    <a-card v-show="currentStep === 3" class="step-card">
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
        <a-descriptions :column="3" size="small">
          <a-descriptions-item label="批次号">
            <a-typography-text copyable>{{ importResult.batchNo }}</a-typography-text>
          </a-descriptions-item>
          <a-descriptions-item label="总行数">{{ importResult.totalRows }}</a-descriptions-item>
          <a-descriptions-item label="成功">
            <span class="success-text">{{ importResult.successRows }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="失败">
            <span class="error-text">{{ importResult.failedRows }}</span>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance, UploadProps, UploadFile } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import {
  RightOutlined,
  LeftOutlined,
  InboxOutlined,
  CloudUploadOutlined,
  LoadingOutlined
} from '@ant-design/icons-vue'
import { getEnabledMarketplaces } from '@/api/marketplace'
import { getEnabledTemplates } from '@/api/fieldMapping'
import { importSalesData } from '@/api/sales'
import { simpleUpload } from '@/api/upload'
import type { Marketplace } from '@/types/marketplace'
import type { FieldMappingTemplate } from '@/types/fieldMapping'
import type { ImportRecord } from '@/types/importRecord'

const router = useRouter()

// ============= 步骤相关 =============
const currentStep = ref(0)
const stepItems = [
  { title: '选择配置', description: '站点和模板' },
  { title: '上传文件', description: 'CSV/Excel' },
  { title: '确认导入', description: '核对信息' },
  { title: '导入结果', description: '查看状态' }
]

// ============= 表单相关 =============
const step1FormRef = ref<FormInstance>()
const importForm = reactive<{
  marketplaceId: number | undefined
  templateId: number | undefined
  quarterDate: Dayjs | null
}>({
  marketplaceId: undefined,
  templateId: undefined,
  quarterDate: null
})

const step1Rules = {
  marketplaceId: [{ required: true, message: '请选择站点', trigger: 'change' }],
  templateId: [{ required: true, message: '请选择字段映射模板', trigger: 'change' }]
}

// ============= 选项数据 =============
const marketplaceOptions = ref<Marketplace[]>([])
const templateOptions = ref<FieldMappingTemplate[]>([])

// ============= 上传相关 =============
const fileList = ref<UploadFile[]>([])
const uploading = ref(false)
const uploadedFileInfo = ref<{ fileId: string; fileName: string; fileSize: number } | null>(null)

// ============= 导入相关 =============
const importing = ref(false)
const importResult = ref<ImportRecord | null>(null)

// ============= 方法 =============
function filterOption(input: string, option: any): boolean {
  const marketplace = marketplaceOptions.value.find(m => m.siteCode === option.value)
  if (!marketplace) return false
  const searchText = input.toLowerCase()
  return marketplace.siteCode.toLowerCase().includes(searchText) ||
         marketplace.siteName.toLowerCase().includes(searchText)
}

async function fetchMarketplaces() {
  try {
    const res = await getEnabledMarketplaces()
    marketplaceOptions.value = res.data || []
  } catch (error) {
    console.error('获取站点列表失败:', error)
  }
}

async function fetchTemplates() {
  try {
    const res = await getEnabledTemplates('SALES')
    templateOptions.value = res.data || []
    // 自动选择默认模板
    const defaultTemplate = templateOptions.value.find(t => t.isDefault)
    if (defaultTemplate) {
      importForm.templateId = defaultTemplate.id
    }
  } catch (error) {
    console.error('获取模板列表失败:', error)
  }
}

function handleMarketplaceChange() {
  // 站点变更时重新加载模板（如果模板与站点关联）
  fetchTemplates()
}

function getMarketplaceName(id?: number): string {
  return marketplaceOptions.value.find(m => m.id === id)?.siteName || '-'
}

function getTemplateName(id?: number): string {
  return templateOptions.value.find(t => t.id === id)?.name || '-'
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

async function handleStep1Next() {
  try {
    await step1FormRef.value?.validate()
    currentStep.value = 1
  } catch {
    // 验证失败
  }
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

  return true
}

const handleUpload: UploadProps['customRequest'] = async (options) => {
  const { file, onSuccess, onError } = options
  uploading.value = true
  
  try {
    const res = await simpleUpload(file as File, 'SALES', importForm.marketplaceId)
    uploadedFileInfo.value = {
      fileId: res.data.fileId,
      fileName: res.data.fileName,
      fileSize: res.data.fileSize
    }
    onSuccess?.(res.data)
    message.success('文件上传成功')
  } catch (error: any) {
    onError?.(error)
    message.error('文件上传失败')
  } finally {
    uploading.value = false
  }
}

function handleStep2Next() {
  if (!uploadedFileInfo.value) {
    message.warning('请先上传文件')
    return
  }
  currentStep.value = 2
}

async function handleStartImport() {
  if (!uploadedFileInfo.value) {
    message.warning('请先上传文件')
    return
  }

  importing.value = true
  try {
    const res = await importSalesData({
      marketplaceId: importForm.marketplaceId!,
      templateId: importForm.templateId!,
      fileId: uploadedFileInfo.value.fileId,
      quarter: importForm.quarterDate?.format('YYYY-Q') || undefined
    })
    importResult.value = res.data
    currentStep.value = 3
    message.success('导入任务已提交')
  } catch (error) {
    console.error('导入失败:', error)
    message.error('导入失败')
  } finally {
    importing.value = false
  }
}

function getResultTitle(): string {
  if (!importResult.value) return ''
  switch (importResult.value.status) {
    case 1: return '正在处理中...'
    case 2: return '导入成功'
    case 3: return '部分导入成功'
    case 4: return '导入失败'
    default: return '等待处理'
  }
}

function getResultSubTitle(): string {
  if (!importResult.value) return ''
  if (importResult.value.status === 1) {
    return '系统正在后台处理数据，请稍候查看导入记录'
  }
  if (importResult.value.failedRows > 0) {
    return `成功导入 ${importResult.value.successRows} 条，失败 ${importResult.value.failedRows} 条`
  }
  return `成功导入 ${importResult.value.successRows} 条数据`
}

function handleViewRecords() {
  router.push('/config/import-record')
}

function handleImportAgain() {
  currentStep.value = 0
  fileList.value = []
  uploadedFileInfo.value = null
  importResult.value = null
  importForm.marketplaceId = undefined
  importForm.templateId = undefined
  importForm.quarterDate = null
}

// 初始化
onMounted(() => {
  fetchMarketplaces()
  fetchTemplates()
})
</script>

<style lang="scss" scoped>
.sales-import-page {
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
}
</style>
