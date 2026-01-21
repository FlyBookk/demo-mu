/**
 * 连线计算逻辑Hook
 */

import { ref, type Ref } from 'vue'
import type { MappingConfig, SourceField, TargetField, MappingLine } from '../types'

export function useLineCalculation(containerRef: Ref<HTMLElement | undefined>) {
  const activeLine = ref<MappingLine | null>(null)

  /**
   * 计算贝塞尔曲线路径
   */
  function calculateBezierPath(
    x1: number,
    y1: number,
    x2: number,
    y2: number
  ): string {
    // 计算控制点，使曲线平滑
    const midX = (x1 + x2) / 2
    const controlOffset = Math.abs(x2 - x1) * 0.5

    // 贝塞尔曲线控制点
    const cp1x = x1 + controlOffset
    const cp1y = y1
    const cp2x = x2 - controlOffset
    const cp2y = y2

    return `M ${x1} ${y1} C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${x2} ${y2}`
  }

  /**
   * 获取字段元素位置
   */
  function getFieldPosition(
    fieldName: string,
    type: 'source' | 'target'
  ): { x: number; y: number } | null {
    if (!containerRef.value) return null

    const selector = type === 'source'
      ? `[data-source-field="${fieldName}"]`
      : `[data-target-field="${fieldName}"]`

    const element = containerRef.value.querySelector(selector)
    if (!element) return null

    const containerRect = containerRef.value.getBoundingClientRect()
    const elementRect = element.getBoundingClientRect()

    // 源字段取右边中点，目标字段取左边中点
    const x = type === 'source'
      ? elementRect.right - containerRect.left
      : elementRect.left - containerRect.left
    const y = elementRect.top - containerRect.top + elementRect.height / 2

    return { x, y }
  }

  /**
   * 计算所有映射连线
   */
  function calculateLines(
    mappings: MappingConfig[],
    sourceFields: SourceField[],
    targetFields: TargetField[]
  ): MappingLine[] {
    const lines: MappingLine[] = []

    // 使用setTimeout确保DOM已更新
    mappings.forEach(mapping => {
      const sourcePos = getFieldPosition(mapping.source, 'source')
      const targetPos = getFieldPosition(mapping.target, 'target')

      if (sourcePos && targetPos) {
        lines.push({
          source: mapping.source,
          target: mapping.target,
          sourcePoint: sourcePos,
          targetPoint: targetPos,
          path: calculateBezierPath(
            sourcePos.x, sourcePos.y,
            targetPos.x, targetPos.y
          ),
          active: activeLine.value?.source === mapping.source &&
                  activeLine.value?.target === mapping.target
        })
      }
    })

    return lines
  }

  /**
   * 计算拖拽时的临时连线
   */
  function calculateDragLine(
    startX: number,
    startY: number,
    endX: number,
    endY: number
  ): string {
    return calculateBezierPath(startX, startY, endX, endY)
  }

  /**
   * 设置激活的连线
   */
  function setActiveLine(line: MappingLine | null) {
    activeLine.value = line
  }

  /**
   * 清除激活状态
   */
  function clearActiveLine() {
    activeLine.value = null
  }

  return {
    activeLine,
    calculateLines,
    calculateDragLine,
    setActiveLine,
    clearActiveLine,
    getFieldPosition
  }
}
