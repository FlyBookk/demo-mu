<template>
  <div class="sales-import-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">销售数据导入</h1>
      <p class="page-desc">支持亚马逊原始数据和ERP结算数据两种格式导入</p>
    </div>

    <!-- 导入步骤 - 根据数据源类型动态显示 -->
    <a-card class="steps-card">
      <a-steps :current="currentStep" :items="dynamicStepItems" size="small" />
    </a-card>

    <!-- 步骤1: 选择数据源类型（放到第一步） -->
    <a-card v-show="currentStep === 0" class="step-card">
      <div class="step-content">
        <h3 class="step-title">选择数据源类型</h3>
        <p class="step-desc">请选择要导入的数据格式</p>
        
        <div class="source-type-grid">
          <div
            v-for="type in sourceTypeOptions"
            :key="type.value"
            :class="['source-type-item', { active: formState.sourceType === type.value }]"
            @click="handleSourceTypeChange(type.value)"
          >
            <div class="type-icon">
              <FileTextOutlined v-if="type.value === 'ORIGINAL'" />
              <DatabaseOutlined v-else />
            </div>
            <div class="type-info">
              <div class="type-label">{{ type.label }}</div>
              <div class="type-desc">{{ type.description }}</div>
            </div>
          </div>
        </div>

        <a-alert
          v-if="formState.sourceType === 'ORIGINAL'"
          type="info"
          show-icon
          class="source-tip"
        >
          <template #message>亚马逊原始数据说明</template>
          <template #description>
            <ul class="tip-list">
              <li>每个国家单独文件，系统将自动识别站点</li>
              <li>文件前7-8行为说明信息，系统会自动跳过</li>
              <li>每行是一笔订单的完整信息</li>
              <li>需要选择站点和数据季度</li>
            </ul>
          </template>
        </a-alert>

        <a-alert
          v-if="formState.sourceType === 'ERP'"
          type="info"
          show-icon
          class="source-tip"
        >
          <template #message>ERP结算数据说明</template>
          <template #description>
            <ul class="tip-list">
              <li>多国家合并在一个文件中，系统自动识别站点</li>
              <li>每行是一笔费用明细（系统会自动聚合为订单维度）</li>
              <li>中文表头，无需跳过说明行</li>
              <li>系统根据结算时间自动计算所属季度</li>
            </ul>
          </template>
        </a-alert>
      </div>

      <div class="step-actions">
        <a-button type="primary" :disabled="!formState.sourceType" @click="handleSourceTypeNext">
          下一步 <RightOutlined />
        </a-button>
      </div>
    </a-card>

    <!-- 步骤2（原始数据）: 选择站点 - 仅原始数据模式显示 -->
    <a-card v-show="currentStep === 1 && formState.sourceType === 'ORIGINAL'" class="step-card">
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
        <a-space>
          <a-button @click="goToStep(0)">
            <LeftOutlined /> 上一步
          </a-button>
          <a-button type="primary" :disabled="!formState.siteCode" @click="handleSiteNext">
            下一步 <RightOutlined />
          </a-button>
        </a-space>
      </div>
    </a-card>

    <!-- 步骤（原始数据=2，ERP=1）: 选择映射模板 -->
    <a-card v-show="currentStep === getTemplateStep()" class="step-card">
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

        <!-- 季度选择 - 仅原始数据模式显示 -->
        <a-form-item v-if="formState.sourceType === 'ORIGINAL'" label="数据季度" class="quarter-form">
          <a-date-picker
            v-model:value="formState.quarterDate"
            picker="quarter"
            placeholder="选择数据所属季度（可选）"
            style="width: 200px"
          />
        </a-form-item>

        <!-- ERP模式提示 -->
        <a-alert
          v-if="formState.sourceType === 'ERP'"
          type="info"
          show-icon
          class="erp-auto-tip"
        >
          <template #message>ERP数据导入说明</template>
          <template #description>
            系统将自动从数据中识别站点，并根据结算时间计算所属季度，无需手动选择。
          </template>
        </a-alert>
      </div>

      <div class="step-actions">
        <a-space>
          <a-button @click="handleTemplatePrev">
            <LeftOutlined /> 上一步
          </a-button>
          <a-button type="primary" :disabled="!formState.templateId" @click="handleTemplateNext">
            下一步 <RightOutlined />
          </a-button>
        </a-space>
      </div>
    </a-card>

    <!-- 上传文件步骤（原始数据=3，ERP=2） -->
    <a-card v-show="currentStep === getUploadStep()" class="step-card">
      <div class="step-content">
        <h3 class="step-title">上传数据文件</h3>
        <p class="step-desc">上传CSV格式的销售数据文件</p>
        
        <div class="upload-area">
          <a-upload-dragger
            v-model:file-list="fileList"
            name="file"
            :multiple="false"
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
                <span v-else>{{ formState.sourceType === 'ERP' ? '自动识别' : '-' }}</span>
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
          <a-button @click="handleUploadPrev">
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

    <!-- 预览确认步骤（原始数据=4，ERP=3） -->
    <a-card v-show="currentStep === getPreviewStep()" class="step-card">
      <div class="step-content">
        <h3 class="step-title">预览确认</h3>
        <p class="step-desc">请确认解析后的数据是否正确</p>
        
        <a-spin :spinning="previewing">
          <div v-if="previewResult" class="preview-section">
            <!-- 导入配置概览 -->
            <a-descriptions title="导入配置" :column="formState.sourceType === 'ERP' ? 2 : 4" size="small" bordered class="config-summary">
              <a-descriptions-item v-if="formState.sourceType === 'ORIGINAL'" label="站点">{{ formState.siteCode }}</a-descriptions-item>
              <a-descriptions-item label="数据源">{{ getSourceTypeLabel() }}</a-descriptions-item>
              <a-descriptions-item label="模板">{{ getTemplateName() }}</a-descriptions-item>
              <a-descriptions-item v-if="formState.sourceType === 'ORIGINAL'" label="季度">{{ formState.quarterDate?.format('YYYY-Q') || '未指定' }}</a-descriptions-item>
              <a-descriptions-item v-if="formState.sourceType === 'ERP'" label="季度">根据结算时间自动计算</a-descriptions-item>
            </a-descriptions>

            <!-- 映射状态 -->
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

            <!-- 警告信息 -->
            <a-alert
              v-for="(warning, index) in previewResult.warnings"
              :key="index"
              type="warning"
              :message="warning"
              show-icon
              class="preview-warning"
            />

            <!-- 数据预览表格 -->
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

        <!-- 导入选项 -->
        <div class="import-options">
          <a-checkbox v-model:checked="importOptions.skipDuplicate">
            跳过重复数据
          </a-checkbox>
          <a-checkbox v-model:checked="importOptions.overwriteDuplicate" :disabled="importOptions.skipDuplicate">
            覆盖重复数据
          </a-checkbox>
        </div>
      </div>

      <div class="step-actions">
        <a-space>
          <a-button @click="handlePreviewPrev">
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
import type { Dayjs } from 'dayjs'
import {
  RightOutlined,
  LeftOutlined,
  InboxOutlined,
  CloudUploadOutlined,
  FileTextOutlined,
  DatabaseOutlined,
  CheckCircleFilled
} from '@ant-design/icons-vue'
import {
  uploadSalesFile,
  previewSalesImport,
  executeSalesImport,
  getTemplatesByType
} from '@/api/sales'
import type {
  SalesSourceType,
  SalesUploadResult,
  SalesPreviewResult,
  SalesImportResult,
  FieldMappingTemplateOption
} from '@/types/sales'

const router = useRouter()

// ============= 步骤相关 =============
const currentStep = ref(0)

// 原始数据模式步骤
const originalStepItems = [
  { title: '数据源类型' },
  { title: '选择站点' },
  { title: '选择模板' },
  { title: '上传文件' },
  { title: '预览确认' }
]

// ERP数据模式步骤（简化版）
const erpStepItems = [
  { title: '数据源类型' },
  { title: '选择模板' },
  { title: '上传文件' },
  { title: '预览确认' }
]

// 动态步骤列表
const dynamicStepItems = computed(() => {
  if (formState.sourceType === 'ERP') {
    return erpStepItems
  }
  return originalStepItems
})

// 获取各步骤的索引（根据模式不同）
function getTemplateStep(): number {
  return formState.sourceType === 'ERP' ? 1 : 2
}

function getUploadStep(): number {
  return formState.sourceType === 'ERP' ? 2 : 3
}

function getPreviewStep(): number {
  return formState.sourceType === 'ERP' ? 3 : 4
}

// ============= 站点选项 =============
const siteOptions = [
  { code: 'US', name: '美国站' },
  { code: 'UK', name: '英国站' },
  { code: 'DE', name: '德国站' },
  { code: 'CA', name: '加拿大站' },
  { code: 'FR', name: '法国站' },
  { code: 'IT', name: '意大利站' },
  { code: 'ES', name: '西班牙站' }
]

// ============= 数据源类型选项 =============
const sourceTypeOptions = [
  { 
    value: 'ORIGINAL' as SalesSourceType, 
    label: '亚马逊原始数据',
    description: '按国家分散的CSV文件，每行是完整订单信息'
  },
  { 
    value: 'ERP' as SalesSourceType, 
    label: 'ERP结算数据',
    description: '多国家合并文件，系统自动识别站点和季度'
  }
]

// ============= 表单状态 =============
const formState = reactive<{
  siteCode: string
  sourceType: SalesSourceType | null
  templateId: number | null
  quarterDate: Dayjs | null
}>({
  siteCode: '',
  sourceType: null,
  templateId: null,
  quarterDate: null
})

// ============= 模板相关 =============
const loadingTemplates = ref(false)
const templateOptions = ref<FieldMappingTemplateOption[]>([])

// ============= 上传相关 =============
const fileList = ref<UploadFile[]>([])
const uploading = ref(false)
const uploadResult = ref<SalesUploadResult | null>(null)

// ============= 预览相关 =============
const previewing = ref(false)
const previewResult = ref<SalesPreviewResult | null>(null)

// ============= 导入相关 =============
const importing = ref(false)
const importResult = ref<SalesImportResult | null>(null)
const showResultModal = ref(false)
const importOptions = reactive({
  skipDuplicate: true,
  overwriteDuplicate: false
})

// ============= 预览表格列 =============
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

// ============= 方法 =============
function goToStep(step: number) {
  currentStep.value = step
}

// 选择数据源类型（仅选择，不跳转）
function handleSourceTypeChange(type: SalesSourceType) {
  formState.sourceType = type
  formState.templateId = null
  // 清空站点（ERP模式不需要手动选择站点）
  if (type === 'ERP') {
    formState.siteCode = ''
    formState.quarterDate = null
  }
}

// 数据源类型选择后点击下一步
function handleSourceTypeNext() {
  if (formState.sourceType === 'ORIGINAL') {
    // 原始数据模式：跳转到选择站点
    goToStep(1)
  } else {
    // ERP模式：跳转到选择模板，并加载模板
    fetchTemplates()
    goToStep(1)  // ERP模式的模板步骤是1
  }
}

// 站点选择后点击下一步（仅原始数据模式）
function handleSiteNext() {
  fetchTemplates()
  goToStep(2)  // 跳转到模板选择
}

// 模板选择上一步
function handleTemplatePrev() {
  if (formState.sourceType === 'ERP') {
    goToStep(0)  // ERP模式返回数据源类型选择
  } else {
    goToStep(1)  // 原始数据模式返回站点选择
  }
}

// 模板选择下一步
function handleTemplateNext() {
  goToStep(getUploadStep())
}

// 上传文件上一步
function handleUploadPrev() {
  goToStep(getTemplateStep())
}

// 预览上一步
function handlePreviewPrev() {
  goToStep(getUploadStep())
}

async function fetchTemplates() {
  if (!formState.sourceType) return
  
  loadingTemplates.value = true
  try {
    // ERP模式不需要传站点
    const siteCode = formState.sourceType === 'ERP' ? undefined : formState.siteCode
    const res = await getTemplatesByType('SALES', formState.sourceType, siteCode)
    templateOptions.value = res.data || []
    // 自动选择默认模板
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

function getSourceTypeLabel(): string {
  return sourceTypeOptions.find(t => t.value === formState.sourceType)?.label || '-'
}

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
  
  try {
    const res = await uploadSalesFile(
      file as File, 
      formState.sourceType!, 
      formState.sourceType === 'ERP' ? undefined : formState.siteCode
    )
    uploadResult.value = res.data
    onSuccess?.(res.data)
    message.success('文件上传并解析成功')
    
    // 如果检测到站点，更新表单（仅原始数据模式）
    if (res.data.detectedSiteCode && formState.sourceType === 'ORIGINAL') {
      formState.siteCode = res.data.detectedSiteCode
    }
  } catch (error: any) {
    onError?.(error)
    message.error('文件上传失败: ' + (error.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}

async function handlePreview() {
  if (!uploadResult.value || !formState.templateId) {
    message.warning('请先上传文件并选择模板')
    return
  }

  previewing.value = true
  try {
    const res = await previewSalesImport({
      fileId: uploadResult.value.fileId,
      sourceType: formState.sourceType!,
      siteCode: formState.sourceType === 'ERP' ? undefined : formState.siteCode,
      templateId: formState.templateId,
      quarter: formState.sourceType === 'ERP' ? undefined : formState.quarterDate?.format('YYYY-Q')
    })
    previewResult.value = res.data
    goToStep(getPreviewStep())
  } catch (error: any) {
    message.error('预览失败: ' + (error.message || '未知错误'))
  } finally {
    previewing.value = false
  }
}

async function handleExecuteImport() {
  if (!uploadResult.value || !formState.templateId) {
    message.warning('请先完成上传和配置')
    return
  }

  importing.value = true
  try {
    const res = await executeSalesImport({
      fileId: uploadResult.value.fileId,
      sourceType: formState.sourceType!,
      siteCode: formState.sourceType === 'ERP' ? undefined : formState.siteCode,
      templateId: formState.templateId,
      quarter: formState.sourceType === 'ERP' ? undefined : formState.quarterDate?.format('YYYY-Q'),
      skipDuplicate: importOptions.skipDuplicate,
      overwriteDuplicate: importOptions.overwriteDuplicate
    })
    importResult.value = res.data
    showResultModal.value = true
    message.success('导入任务已提交')
  } catch (error: any) {
    message.error('导入失败: ' + (error.message || '未知错误'))
  } finally {
    importing.value = false
  }
}

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
  // 重置状态
  currentStep.value = 0
  formState.siteCode = ''
  formState.sourceType = null
  formState.templateId = null
  formState.quarterDate = null
  fileList.value = []
  uploadResult.value = null
  previewResult.value = null
  importResult.value = null
  templateOptions.value = []
}

// 初始化
onMounted(() => {
  // 无需初始加载
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

  // 站点选择网格
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

  // 数据源类型选择
  .source-type-grid {
    display: flex;
    gap: 24px;
    margin-bottom: 24px;

    .source-type-item {
      flex: 1;
      max-width: 300px;
      padding: 24px;
      border: 2px solid #f0f0f0;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s;
      display: flex;
      align-items: flex-start;
      gap: 16px;

      &:hover {
        border-color: #1890ff;
      }

      &.active {
        border-color: #1890ff;
        background: #e6f7ff;
      }

      .type-icon {
        font-size: 32px;
        color: #1890ff;
      }

      .type-info {
        .type-label {
          font-size: 16px;
          font-weight: 600;
          margin-bottom: 4px;
        }

        .type-desc {
          font-size: 12px;
          color: #8c8c8c;
        }
      }
    }
  }

  .source-tip {
    margin-top: 16px;
    max-width: 600px;

    .tip-list {
      margin: 0;
      padding-left: 20px;
      
      li {
        margin-bottom: 4px;
      }
    }
  }

  // 模板列表
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

  .quarter-form {
    margin-top: 24px;
    max-width: 400px;
  }

  .erp-auto-tip {
    margin-top: 24px;
    max-width: 600px;
  }

  // 上传区域
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

  // 预览区域
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

  // 结果弹窗
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
