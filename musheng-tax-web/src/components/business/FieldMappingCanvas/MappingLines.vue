<template>
  <div class="mapping-lines" ref="containerRef">
    <svg 
      class="lines-svg" 
      :width="svgWidth" 
      :height="svgHeight"
      :viewBox="`0 0 ${svgWidth} ${svgHeight}`"
    >
      <!-- 已建立的映射连线 -->
      <g class="mapping-lines-group">
        <g
          v-for="line in computedLines"
          :key="`${line.source}-${line.target}`"
          class="line-group"
          @click="handleLineClick(line)"
        >
          <!-- 连线路径 -->
          <path
            :d="line.path"
            class="mapping-line"
          />
          <!-- 悬停时显示删除按钮 -->
          <circle
            class="delete-btn"
            :cx="getLineMidPoint(line).x"
            :cy="getLineMidPoint(line).y"
            r="8"
            @click.stop="handleDelete(line)"
          />
          <text
            class="delete-text"
            :x="getLineMidPoint(line).x"
            :y="getLineMidPoint(line).y + 3"
            text-anchor="middle"
            @click.stop="handleDelete(line)"
          >×</text>
        </g>
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { MappingLine, MappingConfig, SourceField, TargetField } from './types'

const props = defineProps<{
  mappings: MappingConfig[]
  sourceFields: SourceField[]
  targetFields: TargetField[]
}>()

const emit = defineEmits<{
  (e: 'line-click', line: MappingLine): void
  (e: 'line-delete', mapping: MappingConfig): void
}>()

const containerRef = ref<HTMLElement>()

// 固定配置 - 与 CSS 样式保持一致
const ROW_HEIGHT = 56 // 每行高度（字段项高度 + 间距）

// SVG 尺寸
const svgWidth = ref(120)
const svgHeight = computed(() => {
  const maxRows = Math.max(props.sourceFields.length, props.targetFields.length)
  return Math.max(400, maxRows * ROW_HEIGHT + 20)
})

// 基于索引计算连线
const computedLines = computed(() => {
  const lines: MappingLine[] = []
  const width = svgWidth.value

  for (const mapping of props.mappings) {
    // 根据字段名找到索引
    const sourceIndex = props.sourceFields.findIndex((f) => f.name === mapping.source)
    const targetIndex = props.targetFields.findIndex((f) => f.field === mapping.target)

    // 如果找不到对应字段，跳过
    if (sourceIndex === -1 || targetIndex === -1) continue

    // 基于索引计算 Y 坐标（每行固定高度，居中）
    const sourceY = sourceIndex * ROW_HEIGHT + ROW_HEIGHT / 2
    const targetY = targetIndex * ROW_HEIGHT + ROW_HEIGHT / 2

    const sourcePoint = { x: 0, y: sourceY }
    const targetPoint = { x: width, y: targetY }

    // 贝塞尔曲线：水平方向的 S 形曲线
    const midX = width / 2
    const path = `M ${sourcePoint.x} ${sourcePoint.y} C ${midX} ${sourcePoint.y}, ${midX} ${targetPoint.y}, ${targetPoint.x} ${targetPoint.y}`

    lines.push({
      source: mapping.source,
      target: mapping.target,
      sourcePoint,
      targetPoint,
      path
    })
  }

  return lines
})

// 获取连线中点（用于删除按钮位置）
function getLineMidPoint(line: MappingLine): { x: number; y: number } {
  return {
    x: (line.sourcePoint.x + line.targetPoint.x) / 2,
    y: (line.sourcePoint.y + line.targetPoint.y) / 2
  }
}

// 点击连线
function handleLineClick(line: MappingLine) {
  emit('line-click', line)
}

// 删除连线
function handleDelete(line: MappingLine) {
  emit('line-delete', { source: line.source, target: line.target })
}

// 更新 SVG 宽度
function updateWidth() {
  if (containerRef.value) {
    svgWidth.value = containerRef.value.offsetWidth || 120
  }
}

// 监听容器尺寸变化
onMounted(() => {
  updateWidth()
  
  const resizeObserver = new ResizeObserver(() => {
    updateWidth()
  })
  if (containerRef.value) {
    resizeObserver.observe(containerRef.value)
  }
})

// 暴露方法
defineExpose({
  updateWidth
})
</script>

<style lang="scss" scoped>
.mapping-lines {
  flex: 1;
  position: relative;
  min-width: 80px;
  display: flex;
  align-items: flex-start;

  .lines-svg {
    display: block;

    .line-group {
      cursor: pointer;

      .mapping-line {
        fill: none;
        stroke: $success-color;
        stroke-width: 2;
        transition: stroke 0.2s, stroke-width 0.2s;
      }

      .delete-btn {
        fill: #ff4d4f;
        opacity: 0;
        transition: opacity 0.2s, fill 0.2s;
        cursor: pointer;
      }

      .delete-text {
        fill: white;
        font-size: 12px;
        font-weight: bold;
        opacity: 0;
        transition: opacity 0.2s;
        cursor: pointer;
        user-select: none;
      }

      &:hover {
        .mapping-line {
          stroke: $primary-color;
          stroke-width: 3;
        }

        .delete-btn {
          opacity: 1;
        }

        .delete-text {
          opacity: 1;
        }
      }

      .delete-btn:hover {
        fill: #ff7875;
      }
    }
  }
}
</style>
