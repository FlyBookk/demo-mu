<template>
  <div class="field-mapping-canvas">
    <!-- 工具栏 -->
    <MappingToolbar
      :site-code="siteCode"
      :has-changes="hasChanges"
      :mapping-count="mappings.length"
      :unmapped-required-count="unmappedRequiredCount"
      :templates="templates"
      :current-template-id="currentTemplate?.id"
      @save="handleSaveTemplate"
      @load="handleLoadTemplate"
      @reset="handleReset"
      @auto-map="handleAutoMap"
    />

    <!-- 映射区域 -->
    <div class="mapping-area" ref="mappingAreaRef">
      <!-- 左侧：CSV字段列表 -->
      <SourceFieldList
        :fields="sourceFieldsWithStatus"
        :selected-field="selectedSourceField"
        :readonly="readonly"
        @select="handleSourceSelect"
        @drag-start="handleDragStart"
      />

      <!-- 中间：连线区域 -->
      <MappingLines
        ref="mappingLinesRef"
        :lines="mappingLines"
        :active-line="activeLine"
        :dragging="isDragging"
        :drag-start="dragStartPosition"
        :drag-end="dragPosition"
        @line-click="handleLineClick"
        @line-delete="handleDeleteMapping"
      />

      <!-- 右侧：系统字段列表 -->
      <TargetFieldList
        :fields="targetFieldsWithStatus"
        :selected-field="selectedTargetField"
        :readonly="readonly"
        @select="handleTargetSelect"
        @drop="handleDrop"
      />
    </div>

    <!-- 未映射必填字段提示 -->
    <div v-if="unmappedRequiredCount > 0" class="unmapped-warning">
      <a-alert
        type="warning"
        :message="`还有 ${unmappedRequiredCount} 个必填字段未映射`"
        show-icon
      />
    </div>

    <!-- 映射统计 -->
    <div class="mapping-stats">
      <a-space>
        <a-tag color="green">已映射: {{ mappings.length }}</a-tag>
        <a-tag color="orange">未映射源字段: {{ unmappedSourceCount }}</a-tag>
        <a-tag :color="unmappedRequiredCount > 0 ? 'red' : 'blue'">
          未映射必填: {{ unmappedRequiredCount }}
        </a-tag>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import SourceFieldList from './SourceFieldList.vue'
import TargetFieldList from './TargetFieldList.vue'
import MappingLines from './MappingLines.vue'
import MappingToolbar from './MappingToolbar.vue'
import { useDragMapping } from './hooks/useDragMapping'
import { useClickMapping } from './hooks/useClickMapping'
import { useLineCalculation } from './hooks/useLineCalculation'
import { useMappingTemplate } from './hooks/useMappingTemplate'
import type {
  FieldMappingCanvasProps,
  MappingConfig,
  SourceField,
  TargetField,
  MappingLine
} from './types'

// Props定义
const props = withDefaults(defineProps<FieldMappingCanvasProps>(), {
  initialMappings: () => [],
  readonly: false
})

// Emits定义
const emit = defineEmits<{
  (e: 'update:mappings', mappings: MappingConfig[]): void
  (e: 'save-template', mappings: MappingConfig[]): void
  (e: 'load-template', templateId: number): void
  (e: 'mapping-change', mappings: MappingConfig[]): void
}>()

// ============= Refs =============
const mappingAreaRef = ref<HTMLElement>()
const mappingLinesRef = ref<InstanceType<typeof MappingLines>>()

// ============= 映射数据 =============
const mappings = ref<MappingConfig[]>([...props.initialMappings])
const originalMappings = ref<MappingConfig[]>([...props.initialMappings])

// ============= Hooks =============
// 拖拽相关
const {
  isDragging,
  dragPosition,
  startPosition: dragStartPosition,
  dragSourceField,
  handleDragStart: onDragStart,
  handleDragEnd
} = useDragMapping()

// 点击选择相关
const {
  selectedSourceField,
  selectedTargetField,
  handleSourceSelect: onSourceSelect,
  handleTargetSelect: onTargetSelect,
  clearSelection
} = useClickMapping()

// 连线计算
const { activeLine, calculateLines, setActiveLine, clearActiveLine } = useLineCalculation(mappingAreaRef)

// 模板管理
const {
  templates,
  currentTemplate,
  loadTemplates,
  loadTemplate,
  saveAsTemplate
} = useMappingTemplate(props.siteCode)

// ============= 计算属性 =============
// 带映射状态的源字段
const sourceFieldsWithStatus = computed<SourceField[]>(() => {
  return props.sourceFields.map(field => ({
    ...field,
    mapped: mappings.value.some(m => m.source === field.name)
  }))
})

// 带映射状态的目标字段
const targetFieldsWithStatus = computed<TargetField[]>(() => {
  return props.targetFields.map(field => {
    const mapping = mappings.value.find(m => m.target === field.name)
    return {
      ...field,
      mapped: !!mapping,
      mappedSource: mapping?.source
    }
  })
})

// 映射连线
const mappingLines = computed<MappingLine[]>(() => {
  return calculateLines(
    mappings.value,
    sourceFieldsWithStatus.value,
    targetFieldsWithStatus.value
  )
})

// 未映射必填字段数
const unmappedRequiredCount = computed(() => {
  return props.targetFields.filter(
    f => f.required && !mappings.value.some(m => m.target === f.name)
  ).length
})

// 未映射源字段数
const unmappedSourceCount = computed(() => {
  return props.sourceFields.filter(
    f => !mappings.value.some(m => m.source === f.name)
  ).length
})

// 是否有变更
const hasChanges = computed(() => {
  return JSON.stringify(mappings.value) !== JSON.stringify(originalMappings.value)
})

// ============= 方法 =============
// 添加映射
function addMapping(source: string, target: string) {
  // 移除目标字段已有的映射
  const existingIndex = mappings.value.findIndex(m => m.target === target)
  if (existingIndex > -1) {
    mappings.value.splice(existingIndex, 1)
  }
  // 移除源字段已有的映射
  const sourceIndex = mappings.value.findIndex(m => m.source === source)
  if (sourceIndex > -1) {
    mappings.value.splice(sourceIndex, 1)
  }
  // 添加新映射
  mappings.value.push({ source, target })
  emit('update:mappings', mappings.value)
  emit('mapping-change', mappings.value)
  clearSelection()
  clearActiveLine()
}

// 删除映射
function handleDeleteMapping(mapping: MappingConfig) {
  const index = mappings.value.findIndex(
    m => m.source === mapping.source && m.target === mapping.target
  )
  if (index > -1) {
    mappings.value.splice(index, 1)
    emit('update:mappings', mappings.value)
    emit('mapping-change', mappings.value)
    clearActiveLine()
  }
}

// 处理源字段选择
function handleSourceSelect(field: SourceField) {
  if (props.readonly) return
  onSourceSelect(field)
}

// 处理目标字段选择
function handleTargetSelect(field: TargetField) {
  if (props.readonly) return
  onTargetSelect(field)
}

// 处理拖拽开始
function handleDragStart(field: SourceField, event: MouseEvent) {
  if (props.readonly) return
  onDragStart(field, event)
}

// 处理拖拽放置
function handleDrop(targetField: TargetField) {
  if (props.readonly) return
  if (dragSourceField.value) {
    addMapping(dragSourceField.value.name, targetField.name)
  }
  handleDragEnd()
}

// 处理连线点击
function handleLineClick(line: MappingLine) {
  setActiveLine(line)
}

// 保存模板
function handleSaveTemplate(templateName: string) {
  if (unmappedRequiredCount.value > 0) {
    message.warning('还有必填字段未映射，建议完成映射后再保存')
  }
  emit('save-template', mappings.value)
  saveAsTemplate(templateName, mappings.value)
  originalMappings.value = [...mappings.value]
}

// 加载模板
async function handleLoadTemplate(templateId: number) {
  const templateMappings = await loadTemplate(templateId)
  if (templateMappings.length > 0) {
    mappings.value = [...templateMappings]
    emit('update:mappings', mappings.value)
    emit('load-template', templateId)
    message.success('模板加载成功')
  }
}

// 重置映射
function handleReset() {
  mappings.value = [...originalMappings.value]
  emit('update:mappings', mappings.value)
  clearSelection()
  clearActiveLine()
}

// 自动映射（按字段名相似度匹配）
function handleAutoMap() {
  const autoMappings: MappingConfig[] = []

  props.sourceFields.forEach(source => {
    const sourceLower = source.name.toLowerCase().replace(/[\s_\-]/g, '')

    // 查找最匹配的目标字段
    const matchedTarget = props.targetFields.find(target => {
      const targetLower = target.name.toLowerCase().replace(/[\s_\-]/g, '')
      const labelLower = target.label.toLowerCase().replace(/[\s_\-]/g, '')

      return (
        sourceLower === targetLower ||
        sourceLower === labelLower ||
        sourceLower.includes(targetLower) ||
        targetLower.includes(sourceLower) ||
        sourceLower.includes(labelLower) ||
        labelLower.includes(sourceLower)
      )
    })

    if (matchedTarget && !autoMappings.some(m => m.target === matchedTarget.name)) {
      autoMappings.push({ source: source.name, target: matchedTarget.name })
    }
  })

  if (autoMappings.length > 0) {
    mappings.value = autoMappings
    emit('update:mappings', mappings.value)
    emit('mapping-change', mappings.value)
    message.success(`自动匹配了 ${autoMappings.length} 个字段`)
  } else {
    message.info('未找到可自动匹配的字段')
  }
}

// 重新计算连线
async function recalculateLines() {
  await nextTick()
  mappingLinesRef.value?.recalculate?.()
}

// ============= 监听器 =============
// 监听点击选择，建立映射
watch([selectedSourceField, selectedTargetField], ([source, target]) => {
  if (source && target) {
    addMapping(source.name, target.name)
  }
})

// 监听初始映射变化
watch(
  () => props.initialMappings,
  (newMappings) => {
    mappings.value = [...newMappings]
    originalMappings.value = [...newMappings]
  },
  { deep: true }
)

// 窗口大小变化时重新计算连线
onMounted(() => {
  loadTemplates()
  window.addEventListener('resize', recalculateLines)
})

onUnmounted(() => {
  window.removeEventListener('resize', recalculateLines)
})

// 暴露方法
defineExpose({
  getMappings: () => mappings.value,
  setMappings: (newMappings: MappingConfig[]) => {
    mappings.value = [...newMappings]
    emit('update:mappings', mappings.value)
  },
  recalculateLines
})
</script>

<style lang="scss" scoped>
@import './style.scss';
</style>
