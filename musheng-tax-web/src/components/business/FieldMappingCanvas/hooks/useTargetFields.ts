/**
 * 目标字段加载 Hook
 * 用于根据数据类型加载目标字段
 */

import { ref, watch } from 'vue'
import { getTargetFields } from '@/api/fieldMapping'
import type {
  TargetField,
  TargetFieldsResponse,
  AggregateConfig,
  FieldMappingDataType,
  SalesSubType
} from '../types'

export function useTargetFields() {
  const loading = ref(false)
  const error = ref<string | null>(null)
  const targetFields = ref<TargetField[]>([])
  const aggregateConfig = ref<AggregateConfig | null>(null)
  const sourceTypeName = ref<string>('')

  /**
   * 加载目标字段
   */
  const loadTargetFields = async (
    dataType: FieldMappingDataType,
    sourceType?: SalesSubType
  ): Promise<TargetFieldsResponse> => {
    loading.value = true
    error.value = null

    try {
      const response = await getTargetFields(dataType, sourceType)
      const data = response.data // API 返回 { code, message, data }
      targetFields.value = data?.fields || []
      aggregateConfig.value = data?.aggregateConfig || null
      sourceTypeName.value = data?.sourceTypeName || ''
      return data
    } catch (e: any) {
      error.value = e.message
      targetFields.value = []
      throw e
    } finally {
      loading.value = false
    }
  }

  /**
   * 清空目标字段
   */
  const clearTargetFields = () => {
    targetFields.value = []
    aggregateConfig.value = null
    sourceTypeName.value = ''
  }

  /**
   * 获取必填字段
   */
  const getRequiredFields = () => {
    return targetFields.value.filter((f) => f.required)
  }

  /**
   * 检查是否所有必填字段都已映射或有默认值
   */
  const checkRequiredFieldsMapped = (
    mappings: { target: string }[],
    defaultValues: { field: string }[]
  ): { valid: boolean; missing: TargetField[] } => {
    const mappedTargets = new Set(mappings.map((m) => m.target))
    const defaultFields = new Set(defaultValues.map((d) => d.field))

    const missing = targetFields.value.filter(
      (f) => f.required && !mappedTargets.has(f.field) && !defaultFields.has(f.field)
    )

    return {
      valid: missing.length === 0,
      missing
    }
  }

  return {
    loading,
    error,
    targetFields,
    aggregateConfig,
    sourceTypeName,
    loadTargetFields,
    clearTargetFields,
    getRequiredFields,
    checkRequiredFieldsMapped
  }
}
