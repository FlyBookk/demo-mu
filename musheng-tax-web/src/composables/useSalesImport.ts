/**
 * 销售数据双格式导入 - 组合式函数
 * 封装导入流程的状态管理和业务逻辑
 */

import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import {
  uploadSalesFile,
  previewSalesImport,
  executeSalesImport,
  getSalesImportProgress,
  getTemplatesByType
} from '@/api/sales'
import type {
  SalesSourceType,
  SalesUploadResult,
  SalesPreviewResult,
  SalesImportResult,
  SalesImportProgress,
  FieldMappingTemplateOption
} from '@/types/sales'

/**
 * 导入步骤枚举
 */
export enum ImportStep {
  SELECT_SITE = 0,
  SELECT_SOURCE_TYPE = 1,
  SELECT_TEMPLATE = 2,
  UPLOAD_FILE = 3,
  PREVIEW_CONFIRM = 4
}

/**
 * 表单状态接口
 */
export interface ImportFormState {
  siteCode: string
  sourceType: SalesSourceType | null
  templateId: number | null
  quarterDate: Dayjs | null
}

/**
 * 导入选项接口
 */
export interface ImportOptions {
  skipDuplicate: boolean
  overwriteDuplicate: boolean
}

/**
 * 销售数据导入组合式函数
 */
export function useSalesImport() {
  // ============= 步骤状态 =============
  const currentStep = ref<ImportStep>(ImportStep.SELECT_SITE)
  
  // ============= 表单状态 =============
  const formState = reactive<ImportFormState>({
    siteCode: '',
    sourceType: null,
    templateId: null,
    quarterDate: null
  })

  // ============= 导入选项 =============
  const importOptions = reactive<ImportOptions>({
    skipDuplicate: true,
    overwriteDuplicate: false
  })

  // ============= 模板相关 =============
  const loadingTemplates = ref(false)
  const templateOptions = ref<FieldMappingTemplateOption[]>([])

  // ============= 上传相关 =============
  const uploading = ref(false)
  const uploadResult = ref<SalesUploadResult | null>(null)

  // ============= 预览相关 =============
  const previewing = ref(false)
  const previewResult = ref<SalesPreviewResult | null>(null)

  // ============= 导入相关 =============
  const importing = ref(false)
  const importResult = ref<SalesImportResult | null>(null)
  const importProgress = ref<SalesImportProgress | null>(null)
  const pollingTimer = ref<NodeJS.Timeout | null>(null)

  // ============= 计算属性 =============
  
  /**
   * 是否可以进入下一步
   */
  const canGoNext = computed(() => {
    switch (currentStep.value) {
      case ImportStep.SELECT_SITE:
        return !!formState.siteCode
      case ImportStep.SELECT_SOURCE_TYPE:
        return !!formState.sourceType
      case ImportStep.SELECT_TEMPLATE:
        return !!formState.templateId
      case ImportStep.UPLOAD_FILE:
        return !!uploadResult.value
      case ImportStep.PREVIEW_CONFIRM:
        return !!previewResult.value && 
               previewResult.value.mappingStatus.requiredMissing.length === 0
      default:
        return false
    }
  })

  /**
   * 是否正在加载
   */
  const isLoading = computed(() => {
    return uploading.value || previewing.value || importing.value || loadingTemplates.value
  })

  // ============= 方法 =============

  /**
   * 跳转到指定步骤
   */
  function goToStep(step: ImportStep) {
    currentStep.value = step
  }

  /**
   * 下一步
   */
  function nextStep() {
    if (currentStep.value < ImportStep.PREVIEW_CONFIRM) {
      currentStep.value++
    }
  }

  /**
   * 上一步
   */
  function prevStep() {
    if (currentStep.value > ImportStep.SELECT_SITE) {
      currentStep.value--
    }
  }

  /**
   * 设置站点
   */
  function setSiteCode(code: string) {
    formState.siteCode = code
  }

  /**
   * 设置数据源类型
   */
  async function setSourceType(type: SalesSourceType) {
    formState.sourceType = type
    formState.templateId = null
    // 加载对应类型的模板
    await fetchTemplates()
  }

  /**
   * 设置模板
   */
  function setTemplateId(id: number) {
    formState.templateId = id
  }

  /**
   * 获取模板列表
   */
  async function fetchTemplates() {
    if (!formState.sourceType) return
    
    loadingTemplates.value = true
    try {
      const res = await getTemplatesByType('SALES', formState.sourceType, formState.siteCode)
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

  /**
   * 上传文件
   */
  async function handleUploadFile(file: File): Promise<boolean> {
    if (!formState.sourceType) {
      message.warning('请先选择数据源类型')
      return false
    }

    uploading.value = true
    uploadResult.value = null

    try {
      const res = await uploadSalesFile(file, formState.sourceType, formState.siteCode)
      uploadResult.value = res.data
      
      // 如果是原始数据且检测到站点，更新表单
      if (res.data.detectedSiteCode && formState.sourceType === 'ORIGINAL') {
        formState.siteCode = res.data.detectedSiteCode
      }
      
      message.success('文件上传并解析成功')
      return true
    } catch (error: any) {
      message.error('文件上传失败: ' + (error.message || '未知错误'))
      return false
    } finally {
      uploading.value = false
    }
  }

  /**
   * 预览数据
   */
  async function handlePreview(): Promise<boolean> {
    if (!uploadResult.value || !formState.templateId) {
      message.warning('请先上传文件并选择模板')
      return false
    }

    previewing.value = true
    try {
      const res = await previewSalesImport({
        fileId: uploadResult.value.fileId,
        sourceType: formState.sourceType!,
        siteCode: formState.siteCode,
        templateId: formState.templateId,
        quarter: formState.quarterDate?.format('YYYY-Q')
      })
      previewResult.value = res.data
      return true
    } catch (error: any) {
      message.error('预览失败: ' + (error.message || '未知错误'))
      return false
    } finally {
      previewing.value = false
    }
  }

  /**
   * 执行导入
   */
  async function handleExecuteImport(): Promise<boolean> {
    if (!uploadResult.value || !formState.templateId) {
      message.warning('请先完成上传和配置')
      return false
    }

    importing.value = true
    try {
      const res = await executeSalesImport({
        fileId: uploadResult.value.fileId,
        sourceType: formState.sourceType!,
        siteCode: formState.siteCode,
        templateId: formState.templateId,
        quarter: formState.quarterDate?.format('YYYY-Q'),
        skipDuplicate: importOptions.skipDuplicate,
        overwriteDuplicate: importOptions.overwriteDuplicate
      })
      importResult.value = res.data
      
      // 如果是异步处理，开始轮询进度
      if (res.data.async) {
        startProgressPolling(res.data.batchNo)
      }
      
      message.success('导入任务已提交')
      return true
    } catch (error: any) {
      message.error('导入失败: ' + (error.message || '未知错误'))
      return false
    } finally {
      importing.value = false
    }
  }

  /**
   * 开始轮询导入进度
   */
  function startProgressPolling(batchNo: string) {
    stopProgressPolling()
    
    const poll = async () => {
      try {
        const res = await getSalesImportProgress(batchNo)
        importProgress.value = res.data
        
        // 如果还在处理中，继续轮询
        if (res.data.status === 'PROCESSING' || res.data.status === 'PENDING') {
          pollingTimer.value = setTimeout(poll, 2000)
        } else {
          // 处理完成，更新导入结果
          if (importResult.value) {
            importResult.value.status = res.data.status
            importResult.value.successCount = res.data.successCount
            importResult.value.failCount = res.data.failCount
            importResult.value.skipCount = res.data.skipCount
          }
        }
      } catch (error) {
        console.error('获取进度失败:', error)
      }
    }
    
    poll()
  }

  /**
   * 停止轮询
   */
  function stopProgressPolling() {
    if (pollingTimer.value) {
      clearTimeout(pollingTimer.value)
      pollingTimer.value = null
    }
  }

  /**
   * 重置状态
   */
  function reset() {
    stopProgressPolling()
    
    currentStep.value = ImportStep.SELECT_SITE
    formState.siteCode = ''
    formState.sourceType = null
    formState.templateId = null
    formState.quarterDate = null
    
    importOptions.skipDuplicate = true
    importOptions.overwriteDuplicate = false
    
    templateOptions.value = []
    uploadResult.value = null
    previewResult.value = null
    importResult.value = null
    importProgress.value = null
  }

  /**
   * 获取模板名称
   */
  function getTemplateName(): string {
    return templateOptions.value.find(t => t.id === formState.templateId)?.templateName || '-'
  }

  /**
   * 获取数据源类型标签
   */
  function getSourceTypeLabel(): string {
    const labels: Record<SalesSourceType, string> = {
      'ORIGINAL': '亚马逊原始数据',
      'ERP': 'ERP结算数据'
    }
    return formState.sourceType ? labels[formState.sourceType] : '-'
  }

  /**
   * 格式化文件大小
   */
  function formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
    return (bytes / 1024 / 1024).toFixed(2) + ' MB'
  }

  return {
    // 状态
    currentStep,
    formState,
    importOptions,
    loadingTemplates,
    templateOptions,
    uploading,
    uploadResult,
    previewing,
    previewResult,
    importing,
    importResult,
    importProgress,
    
    // 计算属性
    canGoNext,
    isLoading,
    
    // 方法
    goToStep,
    nextStep,
    prevStep,
    setSiteCode,
    setSourceType,
    setTemplateId,
    fetchTemplates,
    handleUploadFile,
    handlePreview,
    handleExecuteImport,
    startProgressPolling,
    stopProgressPolling,
    reset,
    getTemplateName,
    getSourceTypeLabel,
    formatFileSize
  }
}

export type UseSalesImportReturn = ReturnType<typeof useSalesImport>
