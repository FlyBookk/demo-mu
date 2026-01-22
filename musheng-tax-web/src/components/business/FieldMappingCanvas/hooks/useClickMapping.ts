/**
 * 点击映射逻辑Hook
 */

import { ref } from 'vue'
import type { SourceField, TargetField } from '../types'

export function useClickMapping() {
  // 选中的源字段
  const selectedSourceField = ref<SourceField | null>(null)
  // 选中的目标字段
  const selectedTargetField = ref<TargetField | null>(null)

  /**
   * 选择源字段
   */
  function handleSourceSelect(field: SourceField) {
    // 如果已选中相同字段，取消选中
    if (selectedSourceField.value?.name === field.name) {
      selectedSourceField.value = null
      return
    }
    selectedSourceField.value = field
  }

  /**
   * 选择目标字段
   */
  function handleTargetSelect(field: TargetField) {
    // 如果已选中相同字段，取消选中
    if (selectedTargetField.value?.field === field.field) {
      selectedTargetField.value = null
      return
    }
    selectedTargetField.value = field
  }

  /**
   * 清除选择
   */
  function clearSelection() {
    selectedSourceField.value = null
    selectedTargetField.value = null
  }

  /**
   * 是否有完整的选择（可建立映射）
   */
  function hasCompleteSelection(): boolean {
    return !!selectedSourceField.value && !!selectedTargetField.value
  }

  return {
    selectedSourceField,
    selectedTargetField,
    handleSourceSelect,
    handleTargetSelect,
    clearSelection,
    hasCompleteSelection
  }
}
