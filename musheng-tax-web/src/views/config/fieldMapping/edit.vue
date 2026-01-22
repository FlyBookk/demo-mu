<template>
  <div class="field-mapping-edit-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <a-button type="link" @click="handleBack">
          <ArrowLeftOutlined />
          返回列表
        </a-button>
        <h1 class="page-title">{{ isEdit ? '编辑' : '新增' }}字段映射模板</h1>
      </div>
      <div class="header-right">
        <a-space>
          <a-button @click="handleBack">取消</a-button>
          <a-button type="primary" :loading="saving" @click="handleSave">
            保存模板
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- 基础信息 -->
    <a-card title="基础信息" class="info-card">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="模板名称" required>
              <a-input
                v-model:value="formData.templateName"
                placeholder="请输入模板名称"
                :maxlength="50"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="站点">
              <a-select
                v-model:value="formData.siteCode"
                placeholder="请选择站点（通用模板可不选）"
                :options="siteOptions"
                allow-clear
                @change="handleSiteChange"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="数据类型" required>
              <a-select
                v-model:value="formData.dataType"
                placeholder="请选择数据类型"
                :options="dataTypeOptions"
                @change="handleDataTypeChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item v-if="showSubType" label="数据来源">
              <a-radio-group v-model:value="formData.subType" @change="handleSubTypeChange">
                <a-radio-button value="ORIGINAL">
                  亚马逊原始数据
                </a-radio-button>
                <a-radio-button value="ERP">
                  ERP结算明细
                </a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="设为默认">
              <a-switch v-model:checked="formData.isDefault" />
              <span class="form-hint">设为默认后，该站点导入时将自动使用此模板</span>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 源字段获取 -->
    <a-card title="源字段获取" class="source-card">
      <SourceFieldInput @parsed="handleSourceFieldsParsed" />
    </a-card>

    <!-- 字段映射画布 -->
    <a-card title="字段映射配置" class="canvas-card">
      <div v-if="!hasSourceFields || !hasTargetFields" class="canvas-placeholder">
        <a-empty>
          <template #description>
            <span v-if="!formData.dataType">请先选择数据类型</span>
            <span v-else-if="!hasSourceFields">请上传样例文件或粘贴表头获取源字段</span>
            <span v-else>正在加载目标字段...</span>
          </template>
        </a-empty>
      </div>

      <div v-else class="canvas-wrapper">
        <MappingToolbar
          :has-changes="hasChanges"
          :mapping-count="mappings.length"
          :unmapped-required-count="unmappedRequiredCount"
          :has-source-fields="hasSourceFields"
          @auto-map="handleAutoMatch"
          @reset="handleResetMappings"
          @clear-source="handleClearSource"
        />

        <!-- 字段映射表格 -->
        <FieldMappingTable
          :source-fields="sourceFields"
          :target-fields="targetFields"
          :mappings="mappings"
          @update:mappings="handleMappingsUpdate"
          @delete-source="handleSourceDelete"
        />

        <!-- 必填未映射警告 -->
        <div v-if="unmappedRequiredCount > 0" class="mapping-warning">
          <WarningOutlined />
          <span>
            还有 {{ unmappedRequiredCount }} 个必填字段未映射：
            {{ unmappedRequiredFields.map((f) => `${f.field} - ${f.label}`).join('、') }}
          </span>
        </div>
      </div>
    </a-card>

    <!-- 默认值设置弹窗 -->
    <DefaultValueModal
      v-model:visible="defaultValueModalVisible"
      :field="currentDefaultValueField"
      :initial-value="getCurrentDefaultValue()"
      @confirm="handleDefaultValueConfirm"
    />

    <!-- 智能匹配预览弹窗 -->
    <AutoMatchPreview
      v-model:visible="autoMatchPreviewVisible"
      :result="autoMatchResult"
      @apply="handleApplyAutoMatch"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ArrowLeftOutlined, WarningOutlined } from '@ant-design/icons-vue'
import SourceFieldInput from '@/components/business/FieldMappingCanvas/SourceFieldInput.vue'
import FieldMappingTable from '@/components/business/FieldMappingCanvas/FieldMappingTable.vue'
import MappingToolbar from '@/components/business/FieldMappingCanvas/MappingToolbar.vue'
import DefaultValueModal from '@/components/business/FieldMappingCanvas/DefaultValueModal.vue'
import AutoMatchPreview from '@/components/business/FieldMappingCanvas/AutoMatchPreview.vue'
import { useTargetFields } from '@/components/business/FieldMappingCanvas/hooks/useTargetFields'
import { useAutoMatch } from '@/components/business/FieldMappingCanvas/hooks/useAutoMatch'
import {
  getFieldMappingTemplateById,
  createFieldMappingTemplate,
  updateFieldMappingTemplate
} from '@/api/fieldMapping'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type {
  SourceField,
  TargetField,
  MappingConfig,
  DefaultValueConfig,
  AutoMatchResponse,
  FieldMappingDataType,
  SalesSubType
} from '@/components/business/FieldMappingCanvas/types'

const route = useRoute()
const router = useRouter()

// 判断是否编辑模式
const isEdit = computed(() => !!route.params.id)
const templateId = computed(() => (route.params.id ? Number(route.params.id) : null))

// 表单数据
const formData = ref({
  templateName: '',
  siteCode: '',
  dataType: '' as FieldMappingDataType | '',
  subType: 'ORIGINAL' as SalesSubType,
  isDefault: false
})

// 站点选项
const siteOptions = ref<{ label: string; value: string }[]>([])

// 数据类型选项
const dataTypeOptions = [
  { label: '销售数据', value: 'SALES' },
  { label: '配送数据', value: 'SHIPPING' },
  { label: '广告数据', value: 'ADVERTISING' },
  { label: '汇率数据', value: 'RATE' }
]

// 是否显示子类型
const showSubType = computed(() => formData.value.dataType === 'SALES')

// 源字段
const sourceFields = ref<SourceField[]>([])
const selectedSourceField = ref<SourceField | null>(null)

// 目标字段
const { targetFields, loadTargetFields } = useTargetFields()
const selectedTargetField = ref<TargetField | null>(null)

// 映射配置
const mappings = ref<MappingConfig[]>([])
const defaultValues = ref<DefaultValueConfig[]>([])
const initialMappings = ref<MappingConfig[]>([])

// 默认值映射表（用于画布组件）
const defaultValuesMap = computed(() => {
  const map: Record<string, string> = {}
  for (const dv of defaultValues.value) {
    map[dv.field] = dv.value
  }
  return map
})

// 保存状态
const saving = ref(false)

// 默认值弹窗
const defaultValueModalVisible = ref(false)
const currentDefaultValueField = ref<TargetField | null>(null)

// 智能匹配
const { executeAutoMatch } = useAutoMatch()
const autoMatchPreviewVisible = ref(false)
const autoMatchResult = ref<AutoMatchResponse | null>(null)

// 计算属性
const hasSourceFields = computed(() => sourceFields.value.length > 0)
const hasTargetFields = computed(() => targetFields.value.length > 0)
const hasChanges = computed(() => {
  return JSON.stringify(mappings.value) !== JSON.stringify(initialMappings.value)
})

const unmappedRequiredFields = computed(() => {
  return targetFields.value.filter((field) => {
    if (!field.required) return false
    const hasMapping = mappings.value.some((m) => m.target === field.field)
    const hasDefault = defaultValues.value.some((d) => d.field === field.field)
    return !hasMapping && !hasDefault
  })
})

const unmappedRequiredCount = computed(() => unmappedRequiredFields.value.length)

// 加载站点列表
const loadMarketplaces = async () => {
  try {
    const response = await getEnabledMarketplaces()
    const list = response.data || [] // API 返回 { code, message, data }
    siteOptions.value = list.map((m: any) => ({
      label: `${m.siteCode} - ${m.siteName}`,
      value: m.siteCode
    }))
  } catch (error) {
    console.error('加载站点失败', error)
  }
}

// 加载模板详情
const loadTemplate = async () => {
  if (!templateId.value) return

  try {
    const response = await getFieldMappingTemplateById(templateId.value)
    console.log('[DEBUG] 模板详情 API 响应:', response)
    const template = response.data // API 返回 { code, message, data }，需要取 data
    console.log('[DEBUG] 模板数据:', template)
    
    if (!template) {
      message.error('模板不存在')
      return
    }

    formData.value = {
      templateName: template.templateName,
      siteCode: template.siteCode,
      dataType: template.dataType as FieldMappingDataType,
      subType: ((template.sourceType || template.subType) as SalesSubType) || 'ORIGINAL',
      isDefault: template.isDefault || false
    }
    console.log('[DEBUG] formData:', formData.value)

    // 解析映射配置
    console.log('[DEBUG] template.mappingConfig:', template.mappingConfig)
    if (template.mappingConfig) {
      try {
        mappings.value = JSON.parse(template.mappingConfig)
        initialMappings.value = [...mappings.value]
        console.log('[DEBUG] 解析后的 mappings:', mappings.value)
      } catch (e) {
        console.error('解析映射配置失败', e)
      }
    }

    // 解析源字段
    console.log('[DEBUG] template.sourceFields:', template.sourceFields)
    if (template.sourceFields) {
      try {
        sourceFields.value = JSON.parse(template.sourceFields)
        console.log('[DEBUG] 解析后的 sourceFields:', sourceFields.value)
      } catch (e) {
        console.error('解析源字段失败', e)
      }
    } else if (mappings.value.length > 0) {
      // 如果没有 sourceFields 但有 mappingConfig，从映射中提取源字段
      sourceFields.value = mappings.value.map((m, index) => ({
        name: m.source,
        index,
        sample: ''
      }))
      console.log('[DEBUG] 从 mappings 提取的 sourceFields:', sourceFields.value)
    }

    // 解析默认值
    console.log('[DEBUG] template.defaultValues:', template.defaultValues)
    if (template.defaultValues) {
      try {
        defaultValues.value = JSON.parse(template.defaultValues)
        console.log('[DEBUG] 解析后的 defaultValues:', defaultValues.value)
      } catch (e) {
        console.error('解析默认值失败', e)
      }
    }

    // 加载目标字段
    if (formData.value.dataType) {
      console.log('[DEBUG] 开始加载目标字段, dataType:', formData.value.dataType, 'subType:', formData.value.subType)
      await loadTargetFields(
        formData.value.dataType,
        formData.value.dataType === 'SALES' ? formData.value.subType : undefined
      )
      console.log('[DEBUG] 目标字段加载完成, targetFields:', targetFields.value.length, '个')
    }
    
    console.log('[DEBUG] 最终状态 - sourceFields:', sourceFields.value.length, '个, targetFields:', targetFields.value.length, '个, mappings:', mappings.value.length, '个')
  } catch (error) {
    message.error('加载模板详情失败')
    console.error(error)
  }
}

// 事件处理
const handleBack = () => {
  router.push('/config/field-mapping')
}

const handleSiteChange = () => {
  // 站点变更时可能需要重新进行智能匹配
}

const handleDataTypeChange = async () => {
  if (formData.value.dataType) {
    await loadTargetFields(
      formData.value.dataType as FieldMappingDataType,
      formData.value.dataType === 'SALES' ? formData.value.subType : undefined
    )
    // 清空现有映射（数据类型变更）
    mappings.value = []
    defaultValues.value = []
  }
}

const handleSubTypeChange = async () => {
  if (formData.value.dataType === 'SALES') {
    await loadTargetFields('SALES', formData.value.subType)
    // 清空现有映射
    mappings.value = []
    defaultValues.value = []
  }
}

const handleSourceFieldsParsed = (fields: SourceField[]) => {
  sourceFields.value = fields
  // 如果已有目标字段，可以自动触发智能匹配提示
  if (targetFields.value.length > 0 && fields.length > 0) {
    message.info('已解析源字段，可点击"智能映射"自动匹配')
  }
}

const handleSourceSelect = (field: SourceField) => {
  // 检查是否已经有映射
  const existingMapping = mappings.value.find((m) => m.source === field.name)
  
  if (selectedSourceField.value?.name === field.name) {
    // 再次点击取消选中
    selectedSourceField.value = null
  } else if (existingMapping && !selectedTargetField.value) {
    // 点击已映射的源字段（且没有选中目标字段）-> 询问是否取消映射
    Modal.confirm({
      title: '取消映射',
      content: `确定要取消 "${field.name}" 的映射关系吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: () => {
        mappings.value = mappings.value.filter((m) => m.source !== field.name)
        message.success('已取消映射')
      }
    })
  } else {
    selectedSourceField.value = field
    // 如果有选中的目标字段，建立映射（会替换原有映射）
    if (selectedTargetField.value) {
      addMapping(field.name, selectedTargetField.value.field)
      selectedSourceField.value = null
      selectedTargetField.value = null
    }
  }
}

const handleSourceDelete = (field: SourceField) => {
  sourceFields.value = sourceFields.value.filter((f) => f.name !== field.name)
  // 删除相关映射
  mappings.value = mappings.value.filter((m) => m.source !== field.name)
}

// 点击连线删除映射
const handleLineDelete = (mapping: MappingConfig) => {
  mappings.value = mappings.value.filter(
    (m) => !(m.source === mapping.source && m.target === mapping.target)
  )
  message.success('已删除映射')
}

// 画布映射更新（从新组件）
const handleMappingsUpdate = (newMappings: MappingConfig[]) => {
  mappings.value = newMappings
}

const handleTargetSelect = (field: TargetField) => {
  // 检查是否已经有映射
  const existingMapping = mappings.value.find((m) => m.target === field.field)
  
  if (selectedTargetField.value?.field === field.field) {
    // 再次点击取消选中
    selectedTargetField.value = null
  } else if (existingMapping && !selectedSourceField.value) {
    // 点击已映射的目标字段（且没有选中源字段）-> 询问是否取消映射
    Modal.confirm({
      title: '取消映射',
      content: `确定要取消 "${field.field} - ${field.label}" 的映射关系吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: () => {
        mappings.value = mappings.value.filter((m) => m.target !== field.field)
        message.success('已取消映射')
      }
    })
  } else {
    selectedTargetField.value = field
    // 如果有选中的源字段，建立映射（会替换原有映射）
    if (selectedSourceField.value) {
      addMapping(selectedSourceField.value.name, field.field)
      selectedSourceField.value = null
      selectedTargetField.value = null
    }
  }
}

const handleTargetDrop = (targetField: TargetField) => {
  if (selectedSourceField.value) {
    addMapping(selectedSourceField.value.name, targetField.field)
    selectedSourceField.value = null
  }
}

const addMapping = (source: string, target: string) => {
  // 移除已有的同目标映射
  mappings.value = mappings.value.filter((m) => m.target !== target)
  // 移除已有的同源映射
  mappings.value = mappings.value.filter((m) => m.source !== source)
  // 添加新映射
  mappings.value.push({ source, target })
  // 移除该目标字段的默认值
  defaultValues.value = defaultValues.value.filter((d) => d.field !== target)
}

const handleSetDefault = (field: TargetField) => {
  currentDefaultValueField.value = field
  defaultValueModalVisible.value = true
}

const getCurrentDefaultValue = () => {
  if (!currentDefaultValueField.value) return undefined
  const config = defaultValues.value.find((d) => d.field === currentDefaultValueField.value?.field)
  return config?.value
}

const handleDefaultValueConfirm = (value: string | number | boolean) => {
  if (!currentDefaultValueField.value) return

  if (value === '' || value === null || value === undefined) {
    // 清除默认值
    defaultValues.value = defaultValues.value.filter(
      (d) => d.field !== currentDefaultValueField.value?.field
    )
  } else {
    // 设置默认值
    const existing = defaultValues.value.findIndex(
      (d) => d.field === currentDefaultValueField.value?.field
    )

    if (existing >= 0) {
      defaultValues.value[existing].value = value
    } else {
      defaultValues.value.push({
        field: currentDefaultValueField.value.field,
        value
      })
    }
  }

  defaultValueModalVisible.value = false
}

const handleAutoMatch = async () => {
  try {
    const result = await executeAutoMatch(
      sourceFields.value,
      targetFields.value,
      formData.value.siteCode,
      false,
      formData.value.dataType,
      formData.value.dataType === 'SALES' ? formData.value.subType : undefined
    )
    autoMatchResult.value = result
    autoMatchPreviewVisible.value = true
  } catch (error) {
    message.error('智能匹配失败')
    console.error(error)
  }
}

const handleApplyAutoMatch = (newMappings: MappingConfig[]) => {
  // 合并新映射
  for (const m of newMappings) {
    addMapping(m.source, m.target)
  }
  message.success(`成功应用 ${newMappings.length} 个映射`)
}

const handleResetMappings = () => {
  mappings.value = [...initialMappings.value]
  defaultValues.value = []
  selectedSourceField.value = null
  selectedTargetField.value = null
}

const handleClearSource = () => {
  sourceFields.value = []
  mappings.value = []
  selectedSourceField.value = null
}

const handleSave = async () => {
  // 表单验证
  if (!formData.value.templateName) {
    message.warning('请输入模板名称')
    return
  }
  // 站点非必填，通用模板可以不选
  if (!formData.value.dataType) {
    message.warning('请选择数据类型')
    return
  }

  // 检查必填字段
  if (unmappedRequiredCount.value > 0) {
    message.warning(`还有 ${unmappedRequiredCount.value} 个必填字段未映射或设置默认值`)
    return
  }

  saving.value = true

  try {
    const payload = {
      templateName: formData.value.templateName,
      siteCode: formData.value.siteCode || '', // 允许为空（通用模板）
      dataType: formData.value.dataType,
      sourceType: formData.value.dataType === 'SALES' ? formData.value.subType : undefined,
      mappingConfig: JSON.stringify(mappings.value),
      sourceFields: JSON.stringify(sourceFields.value),
      headerRow: 1,
      defaultValues: JSON.stringify(defaultValues.value),
      isDefault: formData.value.isDefault
    }

    if (isEdit.value && templateId.value) {
      await updateFieldMappingTemplate(templateId.value, payload)
      message.success('模板更新成功')
    } else {
      await createFieldMappingTemplate(payload)
      message.success('模板创建成功')
    }

    router.push('/config/field-mapping')
  } catch (error: any) {
    message.error('保存失败: ' + (error.message || '未知错误'))
    console.error(error)
  } finally {
    saving.value = false
  }
}

// 初始化
onMounted(async () => {
  await loadMarketplaces()
  if (isEdit.value) {
    await loadTemplate()
  }
})
</script>

<style lang="scss" scoped>
.field-mapping-edit-page {
  padding: 24px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 8px;

      .page-title {
        margin: 0;
        font-size: 20px;
        font-weight: 600;
      }
    }
  }

  .info-card,
  .source-card,
  .canvas-card {
    margin-bottom: 16px;
  }

  .form-hint {
    margin-left: 12px;
    color: #999;
    font-size: 13px;
  }

  .canvas-placeholder {
    padding: 60px 0;
    text-align: center;
  }

  .canvas-wrapper {

    .mapping-warning {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: #fff7e6;
      border-radius: 4px;
      color: #d48806;
      font-size: 13px;
    }
  }
}
</style>
