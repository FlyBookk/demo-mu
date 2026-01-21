<template>
  <div class="mapping-lines" ref="containerRef">
    <svg class="lines-svg" :viewBox="`0 0 ${svgWidth} ${svgHeight}`">
      <!-- 已建立的映射连线 -->
      <g class="mapping-lines-group">
        <g
          v-for="line in lines"
          :key="`${line.source}-${line.target}`"
          class="line-group"
          :class="{ active: isLineActive(line) }"
          @click="handleLineClick(line)"
        >
          <!-- 连线路径 -->
          <path
            :d="line.path"
            class="mapping-line"
            :class="{ active: isLineActive(line) }"
          />
          <!-- 删除按钮 -->
          <g
            v-if="isLineActive(line)"
            class="delete-btn"
            :transform="`translate(${getLineMidPoint(line).x - 10}, ${getLineMidPoint(line).y - 10})`"
            @click.stop="handleDelete(line)"
          >
            <circle r="10" fill="#ff4d4f" />
            <text x="0" y="4" text-anchor="middle" fill="white" font-size="12">x</text>
          </g>
        </g>
      </g>

      <!-- 拖拽时的临时连线 -->
      <path
        v-if="dragging && dragStart && dragEnd"
        :d="getDragLinePath()"
        class="drag-line"
      />
    </svg>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { MappingLine, MappingConfig } from './types'

const props = defineProps<{
  lines: MappingLine[]
  activeLine: MappingLine | null
  dragging?: boolean
  dragStart?: { x: number; y: number }
  dragEnd?: { x: number; y: number }
}>()

const emit = defineEmits<{
  (e: 'line-click', line: MappingLine): void
  (e: 'line-delete', mapping: MappingConfig): void
}>()

const containerRef = ref<HTMLElement>()
const svgWidth = ref(200)
const svgHeight = ref(600)

// 判断连线是否激活
function isLineActive(line: MappingLine): boolean {
  if (!props.activeLine) return false
  return (
    props.activeLine.source === line.source &&
    props.activeLine.target === line.target
  )
}

// 获取连线中点
function getLineMidPoint(line: MappingLine): { x: number; y: number } {
  return {
    x: (line.sourcePoint.x + line.targetPoint.x) / 2,
    y: (line.sourcePoint.y + line.targetPoint.y) / 2
  }
}

// 计算拖拽连线路径
function getDragLinePath(): string {
  if (!props.dragStart || !props.dragEnd) return ''

  const { x: x1, y: y1 } = props.dragStart
  const { x: x2, y: y2 } = props.dragEnd

  // 转换为SVG坐标
  const containerRect = containerRef.value?.getBoundingClientRect()
  if (!containerRect) return ''

  const svgX1 = x1 - containerRect.left
  const svgY1 = y1 - containerRect.top
  const svgX2 = x2 - containerRect.left
  const svgY2 = y2 - containerRect.top

  const midX = (svgX1 + svgX2) / 2
  return `M ${svgX1} ${svgY1} C ${midX} ${svgY1}, ${midX} ${svgY2}, ${svgX2} ${svgY2}`
}

// 点击连线
function handleLineClick(line: MappingLine) {
  emit('line-click', line)
}

// 删除连线
function handleDelete(line: MappingLine) {
  emit('line-delete', { source: line.source, target: line.target })
}

// 重新计算SVG尺寸
function recalculate() {
  if (containerRef.value) {
    const rect = containerRef.value.getBoundingClientRect()
    svgWidth.value = rect.width
    svgHeight.value = rect.height
  }
}

// 监听容器尺寸变化
onMounted(() => {
  recalculate()
  const resizeObserver = new ResizeObserver(() => {
    recalculate()
  })
  if (containerRef.value) {
    resizeObserver.observe(containerRef.value)
  }
})

// 暴露方法
defineExpose({
  recalculate
})
</script>

<style lang="scss" scoped>
.mapping-lines {
  flex: 1;
  position: relative;
  min-width: 100px;

  .lines-svg {
    width: 100%;
    height: 100%;
    overflow: visible;

    .mapping-line {
      fill: none;
      stroke: $success-color;
      stroke-width: 2;
      transition: stroke 0.2s, stroke-width 0.2s;
      cursor: pointer;

      &:hover {
        stroke: $primary-color;
        stroke-width: 3;
      }

      &.active {
        stroke: $primary-color;
        stroke-width: 3;
      }
    }

    .drag-line {
      fill: none;
      stroke: $primary-color;
      stroke-width: 2;
      stroke-dasharray: 5, 5;
      opacity: 0.7;
    }

    .delete-btn {
      cursor: pointer;
      opacity: 0;
      transition: opacity 0.2s;

      &:hover {
        circle {
          fill: #ff7875;
        }
      }
    }

    .line-group {
      &.active .delete-btn {
        opacity: 1;
      }

      &:hover .delete-btn {
        opacity: 1;
      }
    }
  }
}
</style>
