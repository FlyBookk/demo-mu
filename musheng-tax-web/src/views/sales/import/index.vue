<template>
  <div class="sales-import-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">销售数据导入</h1>
      <p class="page-desc">上传亚马逊原始销售数据CSV文件进行导入</p>
    </div>

    <!-- 导入步骤 -->
    <a-card class="steps-card">
      <a-steps :current="currentStep" :items="stepItems" size="small" />
    </a-card>

    <!-- 步骤1: 选择站点 -->
    <a-card v-show="currentStep === 0" class="step-card">
      <div class="step-content">
        <h3 class="step-title">选择站点</h3>
        <p class="step-desc">请选择导入数据所属的亚马逊站点</p>
        
        <div class="site-grid">
          <div
            v-for="site in siteOptions"
            :key="site.code"
            :class="['site-item', { active: formState.siteCode === site.code }]"
            @click="formState.siteCode = site.code"
          >
            <div class="site-code">{{ site.code }}</div>
            <div class="site-name">{{ site.name }}</div>
          </div>
        </div>
      </div>

      <div class="step-actions">
        <a-button type="primary" :disabled="!formState.siteCode" @click="handleSiteNext">
          下一步 <RightOutlined />
        </a-button>
      </div>
    </a-card>

    <!-- 步骤2: 选择映射模板 -->
    <a-card v-show="currentStep === 1" class="step-card">
      <div class="step-content">
        <h3 class="step-title">选择字段映射模板</h3>
        <p class="step-desc">选择与数据格式匹配的映射模板</p>
        
        <a-spin :spinning="loadingTemplates">
          <div v-if="templateOptions.length > 0" class="template-list">
            <div
              v-for="template in templateOptions"
              :key="template.id"
              :class="['template-item', { active: formState.templateId === template.id }]"
              @click="formState.templateId = template.id"
            >
              <div class="template-info">
                <div class="template-name">
                  {{ template.templateName }}
                  <a-tag v-if="template.isDefault" color="gold" size="small">默认</a-tag>
                </div>
                <div class="template-meta">
                  <span v-if="template.siteCode">站点: {{ template.siteCode }}</span>
                  <span>映射字段: {{ template.mappingCount }}个</span>
                </div>
              </div>
              <CheckCircleFilled v-if="formState.templateId === template.id" class="check-icon" />
            </div>
          </div>
          <a-empty v-else description="暂无可用模板，请先创建字段映射模板" />
        </a-spin>
      </div>

      <div class="step-actions">
        <a-space>
          <a-button @click="currentStep = 0">
            <LeftOutlined /> 上一步
          </a-button>
          <a-button type="primary" :disabled="!formState.templateId" @click="currentStep = 2">
            下一步 <RightOutlined />
          </a-button>
        </a-space>
      </div>
    </a-card>

    <!-- 步骤3: 上传文件 -->
    <a-card v-show="currentStep === 2" class="step-card">
      <div class="step-content">
        <h3 class="step-title">上传数据文件</h3>
        <p class="step-desc">上传CSV格式的销售数据文件</p>
        
        <div class="upload-area">
          <a-upload-dragger
            v-model:file-list="fileList"
            name="file"
            :multiple="false"
            :max-count="1"
            :before-upload="beforeUpload"
            :custom-request="handleUpload"
            accept=".csv"
            :disabled="uploading"
          >
            <p class="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
            <p class="ant-upload-hint">支持 CSV 格式，单个文件最大 100MB</p>
          </a-upload-dragger>
        </div>

        <div v-if="uploadResult" class="upload-result">
          <a-card size="small" title="文件解析结果">
            <a-descriptions :column="3" size="small">
              <a-descriptions-item label="文件名" :span="2">{{ uploadResult.fileName }}</a-descriptions-item>
              <a-descriptions-item label="文件大小">{{ formatFileSize(uploadResult.fileSize) }}</a-descriptions-item>
              <a-descriptions-item label="数据行数">{{ uploadResult.totalRows }} 行</a-descriptions-item>
              <a-descriptions-item label="表头行号">第 {{ uploadResult.headerRow }} 行</a-descriptions-item>
              <a-descriptions-item label="识别站点">
                <a-tag v-if="uploadResult.detectedSiteCode" color="blue">{{ uploadResult.detectedSiteCode }}</a-tag>
                <span v-else>-</span>
              </a-descriptions-item>
              <a-descriptions-item label="源字段数" :span="3">{{ uploadResult.sourceFields?.length || 0 }} 个</a-descriptions-item>
            </a-descriptions>

            <a-divider>源字段列表</a-divider>
            <div class="source-fields">
              <a-tag v-for="field in uploadResult.sourceFields?.slice(0, 15)" :key="field">
                {{ field }}
              </a-tag>
              <a-tag v-if="(uploadResult.sourceFields?.length || 0) > 15">
                +{{ uploadResult.sourceFields!.length - 15 }} 更多
              </a-tag>
            </div>
          </a-card>
        </div>
      </div>

      <div class="step-actions">
        <a-space>
          <a-button @click="currentStep = 1">
            <LeftOutlined /> 上一步
          </a-button>
          <a-button 
            type="primary" 
            :disabled="!uploadResult" 
            :loading="previewing"
            @click="handlePreview"
          >
            预览数据 <RightOutlined />
          </a-button>
        </a-space>
      </div>
    </a-card>

    <!-- 步骤4: 预览确认 -->
    <a-card v-show="currentStep === 3" class="step-card">
      <div class="step-content">
        <h3 class="step-title">预览确认</h3>
        <p class="step-desc">请确认解析后的数据是否正确</p>
        
        <a-spin :spinning="previewing">
          <div v-if="previewResult" class="preview-section">
            <a-descriptions title="导入配置" :column="3" size="small" bordered class="config-summary">
              <a-descriptions-item label="站点">{{ formState.siteCode }}</a-descriptions-item>
              <a-descriptions-item label="数据源">亚马逊原始数据</a-descriptions-item>
              <a-descriptions-item label="模板">{{ getTemplateName() }}</a-descriptions-item>
            </a-descriptions>

            <div class="mapping-status">
              <a-statistic-countdown
                title="映射字段"
                :value="previewResult.mappingStatus.mappedFields"
                :suffix="`/ ${previewResult.mappingStatus.totalFields}`"
                :value-style="{ color: previewResult.mappingStatus.requiredMissing.length > 0 ? '#cf1322' : '#3f8600' }"
              />
              <a-alert
                v-if="previewResult.mappingStatus.requiredMissing.length > 0"
                type="error"
                show-icon
                class="mapping-warning"
              >
                <template #message>缺少必填字段映射</template>
                <template #description>
                  {{ previewResult.mappingStatus.requiredMissing.join(', ') }}
                </template>
              </a-alert>
            </div>

            <a-alert
              v-for="(warning, index) in previewResult.warnings"
              :key="index"
              type="warning"
              :message="warning"
              show-icon
              class="preview-warning"
            />

            <a-table
              :columns="previewColumns"
              :data-source="previewResult.data"
              :pagination="false"
              size="small"
              :scroll="{ x: 1200 }"
              class="preview-table"
            />

            <div class="preview-info">
              <a-typography-text type="secondary">
                共 {{ previewResult.totalRows }} 条数据，以上显示前 {{ previewResult.previewRows }} 条
              </a-typography-text>
            </div>
          </div>
        </a-spin>

        <div class="import-options">
          <a-checkbox v-model:checked="importOptions.skipDuplicate">
            跳过重复数据
          </a-checkbox>
        </div>
      </div>

      <div class="step-actions">
        <a-space>
          <a-button @click="currentStep = 2">
            <LeftOutlined /> 上一步
          </a-button>
          <a-button 
            type="primary" 
            :loading="importing"
            :disabled="(previewResult?.mappingStatus?.requiredMissing?.length ?? 0) > 0"
            @click="handleExecuteImport"
          >
            <CloudUploadOutlined /> 开始导入
          </a-button>
        </a-space>
      </div>
    </a-card>

    <!-- 导入确认弹窗 -->
    <ImportConfirmModal
      ref="importConfirmRef"
      :file-name="uploadResult?.fileName"
      data-type="sales"
      @confirm="doExecuteImport"
      @cancel="handleImportCancel"
    />

    <!-- 导入结果弹窗 -->
    <a-modal
      v-model:open="showResultModal"
      :title="importResult?.status === 'SUCCESS' ? '导入成功' : '导入完成'"
      :footer="null"
      :closable="false"
      :maskClosable="false"
      width="500px"
    >
      <a-result
        :status="getResultStatus()"
        :title="getResultTitle()"
        :sub-title="getResultSubTitle()"
      >
        <template #extra>
          <div v-if="importResult" class="result-stats">
            <a-row :gutter="16">
              <a-col :span="8">
                <a-statistic title="总记录" :value="importResult.totalCount" />
              </a-col>
              <a-col :span="8">
                <a-statistic title="成功" :value="importResult.successCount" :value-style="{ color: '#3f8600' }" />
              </a-col>
              <a-col :span="8">
                <a-statistic title="失败" :value="importResult.failCount" :value-style="{ color: '#cf1322' }" />
              </a-col>
            </a-row>
            <div v-if="importResult.skipCount > 0" class="skip-info">
              跳过重复数据: {{ importResult.skipCount }} 条
            </div>
          </div>
          <a-space class="result-actions">
            <a-button type="primary" @click="handleViewRecords">
              查看导入记录
            </a-button>
            <a-button @click="handleImportAgain">
              继续导入
            </a-button>
          </a-space>
        </template>
      </a-result>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { UploadProps, UploadFile } from 'ant-design-vue'
import {
  RightOutlined,
  LeftOutlined,
  InboxOutlined,
  CloudUploadOutlined,
  CheckCircleFilled
} from '@ant-design/icons-vue'
import ImportConfirmModal from '@/components/business/ImportConfirmModal/index.vue'
import {
  uploadSalesFile,
  previewSalesImport,
  executeSalesImport,
  getTemplatesByType
} from '@/api/sales'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type {
  SalesUploadResult,
  SalesPreviewResult,
  SalesImportResult,
  FieldMappingTemplateOption
} from '@/types/sales'
import type { Marketplace } from '@/types/marketplace'

const router = useRouter()

// ============= 步骤 =============
const currentStep = ref(0)
const stepItems = [
  { title: '选择站点' },
  { title: '选择模板' },
  { title: '上传文件' },
  { title: '预览确认' }
]

// ============= 站点选项 =============
const siteOptions = ref<{ code: string; name: string }[]>([])

async function fetchSiteOptions() {
  try {
    const res = await getEnabledMarketplaces() as any
    const list: Marketplace[] = res?.data ?? res ?? []
    siteOptions.value = list.map(m => ({ code: m.siteCode, name: m.siteName }))
  } catch {
    siteOptions.value = []
  }
}

// ============= 表单状态 =============
const formState = reactive<{
  siteCode: string
  templateId: number | null
}>({
  siteCode: '',
  templateId: null
})

// ============= 模板 =============
const loadingTemplates = ref(false)
const templateOptions = ref<FieldMappingTemplateOption[]>([])

async function fetchTemplates() {
  loadingTemplates.value = true
  try {
    const res = await getTemplatesByType('SALES', 'ORIGINAL', formState.siteCode)
    templateOptions.value = res.data || []
    const defaultTemplate = templateOptions.value.find(t => t.isDefault)
    if (defaultTemplate) {
      formState.templateId = defaultTemplate.id
    }
  } catch (error) {
    console.error('获取模板列表失败:', error)
    message.error('获取模板列表失败')
  } finally {
    loadingTemplates.value = false
  }
}

function getTemplateName(): string {
  return templateOptions.value.find(t => t.id === formState.templateId)?.templateName || '-'
}

function handleSiteNext() {
  fetchTemplates()
  currentStep.value = 1
}

// ============= 上传 =============
const fileList = ref<UploadFile[]>([])
const uploading = ref(false)
const uploadResult = ref<SalesUploadResult | null>(null)

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isCSV = file.type === 'text/csv' || file.name.endsWith('.csv')
  if (!isCSV) {
    message.error('只能上传 CSV 文件!')
    return false
  }
  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isLt100M) {
    message.error('文件大小不能超过 100MB!')
    return false
  }
  return true
}

const handleUpload: UploadProps['customRequest'] = async (options) => {
  const { file, onSuccess, onError } = options
  uploading.value = true
  uploadResult.value = null
  previewResult.value = null
  fileList.value = [{ uid: String(Date.now()), name: (file as File).name, status: 'uploading' }] as UploadFile[]
  
  try {
    const res = await uploadSalesFile(file as File, 'ORIGINAL', formState.siteCode)
    uploadResult.value = res.data
    onSuccess?.(res.data)
    message.success('文件上传并解析成功')
  } catch (error: any) {
    onError?.(error)
    message.error('文件上传失败: ' + (error.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}

// ============= 预览 =============
const previewing = ref(false)
const previewResult = ref<SalesPreviewResult | null>(null)

const previewColumns = computed(() => {
  if (!previewResult.value?.columns) return []
  return previewResult.value.columns.map(col => ({
    title: col.label,
    dataIndex: col.field,
    key: col.field,
    width: 120,
    ellipsis: true
  }))
})

async function handlePreview() {
  if (!uploadResult.value || !formState.templateId) {
    message.warning('请先上传文件并选择模板')
    return
  }

  const selectedTemplate = templateOptions.value.find(t => t.id === formState.templateId)
  const detectedSite = uploadResult.value.detectedSiteCode
  if (selectedTemplate?.siteCode && detectedSite && selectedTemplate.siteCode.toUpperCase() !== detectedSite.toUpperCase()) {
    message.error(`所选模板站点(${selectedTemplate.siteCode})与文件中识别到的站点(${detectedSite})不一致，请选择正确的模板或上传对应站点的数据文件`)
    return
  }

  previewing.value = true
  try {
    const res = await previewSalesImport({
      fileId: uploadResult.value.fileId,
      sourceType: 'ORIGINAL',
      siteCode: formState.siteCode,
      templateId: formState.templateId
    })
    previewResult.value = res.data
    currentStep.value = 3
  } catch (error: any) {
    message.error('预览失败: ' + (error.message || '未知错误'))
  } finally {
    previewing.value = false
  }
}

// ============= 导入 =============
const importing = ref(false)
const importResult = ref<SalesImportResult | null>(null)
const showResultModal = ref(false)
const importOptions = reactive({ skipDuplicate: true })
const importConfirmRef = ref<InstanceType<typeof ImportConfirmModal> | null>(null)

function handleExecuteImport() {
  if (!uploadResult.value || !formState.templateId) {
    message.warning('请先完成上传和配置')
    return
  }

  const selectedTemplate = templateOptions.value.find(t => t.id === formState.templateId)
  const detectedSite = uploadResult.value.detectedSiteCode
  if (selectedTemplate?.siteCode && detectedSite && selectedTemplate.siteCode.toUpperCase() !== detectedSite.toUpperCase()) {
    message.error(`所选模板站点(${selectedTemplate.siteCode})与文件中识别到的站点(${detectedSite})不一致，请选择正确的模板或上传对应站点的数据文件`)
    return
  }

  importConfirmRef.value?.show()
}

async function doExecuteImport() {
  importing.value = true
  importConfirmRef.value?.setLoading(true)
  
  try {
    const res = await executeSalesImport({
      fileId: uploadResult.value!.fileId,
      sourceType: 'ORIGINAL',
      siteCode: formState.siteCode,
      templateId: formState.templateId!,
      skipDuplicate: importOptions.skipDuplicate
    })
    importResult.value = res.data
    showResultModal.value = true
    importConfirmRef.value?.hide()
    message.success('导入任务已提交')
  } catch (error: any) {
    message.error('导入失败: ' + (error.message || '未知错误'))
    importConfirmRef.value?.setLoading(false)
  } finally {
    importing.value = false
  }
}

function handleImportCancel() {}

function getResultStatus(): 'success' | 'error' | 'info' | 'warning' {
  if (!importResult.value) return 'info'
  switch (importResult.value.status) {
    case 'SUCCESS': return 'success'
    case 'FAIL': return 'error'
    case 'PARTIAL': return 'warning'
    default: return 'info'
  }
}

function getResultTitle(): string {
  if (!importResult.value) return ''
  switch (importResult.value.status) {
    case 'SUCCESS': return '导入成功'
    case 'FAIL': return '导入失败'
    case 'PARTIAL': return '部分导入成功'
    case 'PROCESSING': return '正在处理中...'
    default: return '等待处理'
  }
}

function getResultSubTitle(): string {
  if (!importResult.value) return ''
  const { successCount, failCount, totalCount } = importResult.value
  if (importResult.value.status === 'PROCESSING') {
    return `批次号: ${importResult.value.batchNo}，系统正在后台处理`
  }
  if (failCount > 0) {
    return `成功导入 ${successCount}/${totalCount} 条，失败 ${failCount} 条`
  }
  return `成功导入 ${successCount} 条数据`
}

function handleViewRecords() {
  showResultModal.value = false
  router.push('/config/import-record')
}

function handleImportAgain() {
  showResultModal.value = false
  currentStep.value = 0
  formState.siteCode = ''
  formState.templateId = null
  fileList.value = []
  uploadResult.value = null
  previewResult.value = null
  importResult.value = null
  templateOptions.value = []
}

onMounted(() => {
  fetchSiteOptions()
})
</script>

<style lang="scss" scoped>
.sales-import-page {
  padding: 24px;

  .page-header {
    margin-bottom: 24px;

    .page-title {
      font-size: 20px;
      font-weight: 600;
      color: #262626;
      margin: 0 0 8px 0;
    }

    .page-desc {
      font-size: 14px;
      color: #8c8c8c;
      margin: 0;
    }
  }

  .steps-card {
    margin-bottom: 24px;
  }

  .step-card {
    .step-content {
      min-height: 300px;
    }

    .step-title {
      font-size: 16px;
      font-weight: 600;
      margin: 0 0 8px 0;
    }

    .step-desc {
      color: #8c8c8c;
      margin-bottom: 24px;
    }

    .step-actions {
      margin-top: 24px;
      padding-top: 24px;
      border-top: 1px solid #f0f0f0;
      text-align: center;
    }
  }

  .site-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 16px;
    max-width: 600px;

    .site-item {
      padding: 16px;
      border: 2px solid #f0f0f0;
      border-radius: 8px;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        border-color: #1890ff;
      }

      &.active {
        border-color: #1890ff;
        background: #e6f7ff;
      }

      .site-code {
        font-size: 18px;
        font-weight: 600;
        color: #262626;
      }

      .site-name {
        font-size: 12px;
        color: #8c8c8c;
        margin-top: 4px;
      }
    }
  }

  .template-list {
    max-width: 600px;

    .template-item {
      padding: 16px;
      border: 2px solid #f0f0f0;
      border-radius: 8px;
      margin-bottom: 12px;
      cursor: pointer;
      transition: all 0.3s;
      display: flex;
      justify-content: space-between;
      align-items: center;

      &:hover {
        border-color: #1890ff;
      }

      &.active {
        border-color: #1890ff;
        background: #e6f7ff;
      }

      .template-name {
        font-weight: 500;
        margin-bottom: 4px;
      }

      .template-meta {
        font-size: 12px;
        color: #8c8c8c;

        span {
          margin-right: 16px;
        }
      }

      .check-icon {
        font-size: 20px;
        color: #1890ff;
      }
    }
  }

  .upload-area {
    max-width: 600px;
    margin: 0 auto;
  }

  .upload-result {
    margin-top: 24px;
    max-width: 600px;

    .source-fields {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
  }

  .preview-section {
    .config-summary {
      margin-bottom: 24px;
    }

    .mapping-status {
      margin-bottom: 16px;
    }

    .mapping-warning {
      margin-top: 12px;
    }

    .preview-warning {
      margin-bottom: 12px;
    }

    .preview-table {
      margin-top: 16px;
    }

    .preview-info {
      margin-top: 12px;
      text-align: right;
    }
  }

  .import-options {
    margin-top: 24px;
    padding: 16px;
    background: #fafafa;
    border-radius: 8px;
    display: flex;
    gap: 24px;
  }

  .result-stats {
    margin-bottom: 24px;

    .skip-info {
      margin-top: 12px;
      text-align: center;
      color: #8c8c8c;
    }
  }

  .result-actions {
    margin-top: 16px;
  }
}
</style>
