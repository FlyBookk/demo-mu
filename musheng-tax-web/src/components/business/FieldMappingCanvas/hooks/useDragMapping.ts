/**
 * 拖拽映射逻辑Hook
 */

import { ref, type Ref } from 'vue'
import type { SourceField, DragState } from '../types'

export function useDragMapping() {
  // 拖拽状态
  const isDragging = ref(false)
  const dragSourceField = ref<SourceField | null>(null)
  const dragPosition = ref({ x: 0, y: 0 })
  const startPosition = ref({ x: 0, y: 0 })

  /**
   * 开始拖拽
   */
  function handleDragStart(field: SourceField, event: MouseEvent | DragEvent) {
    isDragging.value = true
    dragSourceField.value = field

    const rect = (event.target as HTMLElement).getBoundingClientRect()
    startPosition.value = {
      x: rect.left + rect.width / 2,
      y: rect.top + rect.height / 2
    }
    dragPosition.value = { ...startPosition.value }

    // 添加全局鼠标移动监听
    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }

  /**
   * 鼠标移动
   */
  function handleMouseMove(event: MouseEvent) {
    if (isDragging.value) {
      dragPosition.value = {
        x: event.clientX,
        y: event.clientY
      }
    }
  }

  /**
   * 鼠标释放
   */
  function handleMouseUp() {
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  /**
   * 结束拖拽
   */
  function handleDragEnd() {
    isDragging.value = false
    dragSourceField.value = null
    dragPosition.value = { x: 0, y: 0 }
  }

  /**
   * 获取拖拽状态
   */
  function getDragState(): DragState {
    return {
      isDragging: isDragging.value,
      dragSource: dragSourceField.value,
      dragPosition: dragPosition.value,
      startPosition: startPosition.value
    }
  }

  return {
    isDragging,
    dragSourceField,
    dragPosition,
    startPosition,
    handleDragStart,
    handleDragEnd,
    getDragState
  }
}
