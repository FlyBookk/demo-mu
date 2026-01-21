<template>
  <div class="field-mapper-canvas">
    <ValueMapperRoot v-model="internalMapping" class="mapper-root">
      <!-- 隐藏 SVG 连线，使用行对齐方式显示映射关系 -->
      <template #connections>
        <!-- 不渲染 SVG 连线，行布局已经清晰显示映射关系 -->
      </template>

      <!-- 对齐行布局 -->
      <div class="aligned-layout">
        <!-- 表头 -->
        <div class="layout-header">
          <div class="header-left">
            <span class="title">CSV文件字段</span>
            <span class="count">共 {{ sourceFields.length }} 个</span>
          </div>
          <div class="header-center"></div>
          <div class="header-right">
            <span class="title">系统目标字段</span>
            <span class="count">共 {{ targetFields.length }} 个</span>
          </div>
        </div>

        <!-- 映射行列表 -->
        <div class="rows-container">
          <div
            v-for="(row, index) in alignedRows"
            :key="index"
            class="mapping-row"
            :class="{ 'has-mapping': row.source && row.target }"
          >
            <!-- 左侧：源字段 -->
            <div class="row-left">
              <ValueMapperNode
                v-if="row.source"
                :identifier="row.source.name"
                type="source"
                v-slot="{ isConnected, isConnecting }"
              >
                <div
                  class="field-item source-item"
                  :class="{ connected: isConnected, connecting: isConnecting }"
                >
                  <div class="field-info">
                    <span class="field-name">{{ row.source.name }}</span>
                  </div>
                  <div class="field-status">
                    <CheckCircleFilled v-if="isConnected" class="status-icon connected" />
                    <MinusCircleOutlined v-else class="status-icon" />
                  </div>
                </div>
              </ValueMapperNode>
              <div v-else class="field-placeholder"></div>
            </div>

            <!-- 中间：连线指示 -->
            <div class="row-center">
              <div v-if="row.source && row.target" class="link-indicator">
                <LinkOutlined />
              </div>
            </div>

            <!-- 右侧：目标字段 -->
            <div class="row-right">
              <ValueMapperNode
                v-if="row.target"
                :identifier="row.target.field"
                type="target"
                v-slot="{ isConnected, isConnecting }"
              >
                <div
                  class="field-item target-item"
                  :class="{
                    connected: isConnected,
                    connecting: isConnecting,
                    required: row.target.required,
                    'has-default': !!defaultValues[row.target.field]
                  }"
                >
                  <div class="field-info">
                    <div class="field-header">
                      <span v-if="row.target.required" class="required-mark">*</span>
                      <span class="field-label">{{ row.target.label }}</span>
                    </div>
                    <div class="field-meta">
                      <span class="field-code">{{ row.target.field }}</span>
                    </div>
                  </div>
                  <div class="field-status">
                    <CheckCircleFilled v-if="isConnected" class="status-icon connected" />
                    <ExclamationCircleOutlined
                      v-else-if="row.target.required && !defaultValues[row.target.field]"
                      class="status-icon warning"
                    />
                    <MinusCircleOutlined v-else class="status-icon" />
                  </div>
                </div>
              </ValueMapperNode>
              <div v-else class="field-placeholder"></div>
            </div>
          </div>
        </div>
      </div>
    </ValueMapperRoot>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ValueMapperRoot, ValueMapperNode } from '@sketchmonk/value-mapper-vue'
import {
  CheckCircleFilled,
  MinusCircleOutlined,
  LinkOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons-vue'
import type { SourceField, TargetField, MappingConfig } from './types'

const props = defineProps<{
  sourceFields: SourceField[]
  targetFields: TargetField[]
  mappings: MappingConfig[]
  defaultValues: Record<string, string>
}>()

const emit = defineEmits<{
  (e: 'update:mappings', mappings: MappingConfig[]): void
  (e: 'set-default', field: TargetField): void
}>()

// 内部映射状态：source -> target
const internalMapping = ref<Record<string, string>>({})

// 反向映射：target -> source
const reverseMapping = computed(() => {
  const map: Record<string, string> = {}
  for (const [source, target] of Object.entries(internalMapping.value)) {
    map[target] = source
  }
  return map
})

// 对齐排列：生成左右两侧对齐的字段列表
// 每行要么是已映射的一对，要么是未映射的单个字段
const alignedRows = computed(() => {
  const rows: Array<{
    source: SourceField | null
    target: TargetField | null
  }> = []

  const usedSources = new Set<string>()

  // 首先，按目标字段顺序添加已映射的行
  for (const targetField of props.targetFields) {
    const sourceName = reverseMapping.value[targetField.field]
    if (sourceName) {
      const sourceField = props.sourceFields.find((s) => s.name === sourceName)
      if (sourceField) {
        rows.push({ source: sourceField, target: targetField })
        usedSources.add(sourceName)
      }
    }
  }

  // 添加未映射的源字段
  for (const sourceField of props.sourceFields) {
    if (!usedSources.has(sourceField.name)) {
      rows.push({ source: sourceField, target: null })
    }
  }

  // 添加未映射的目标字段
  for (const targetField of props.targetFields) {
    if (!reverseMapping.value[targetField.field]) {
      rows.push({ source: null, target: targetField })
    }
  }

  return rows
})

// 从 props.mappings 初始化内部映射
watch(
  () => props.mappings,
  (newMappings) => {
    const map: Record<string, string> = {}
    for (const m of newMappings) {
      map[m.source] = m.target
    }
    internalMapping.value = map
  },
  { immediate: true, deep: true }
)

// 当内部映射变化时，同步到父组件
watch(
  internalMapping,
  (newMap) => {
    const mappings: MappingConfig[] = Object.entries(newMap).map(([source, target]) => ({
      source,
      target
    }))
    emit('update:mappings', mappings)
  },
  { deep: true }
)

</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.field-mapper-canvas {
  .mapper-root {
    width: 100%;
    position: relative;
  }

  // 对齐行布局
  .aligned-layout {
    .layout-header {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      background: #fafafa;
      border: 1px solid $border-color-light;
      border-radius: 8px 8px 0 0;

      .header-left,
      .header-right {
        flex: 1;
        display: flex;
        justify-content: space-between;
        align-items: center;

        .title {
          font-weight: 600;
          color: $text-color;
        }

        .count {
          font-size: 13px;
          color: $text-color-secondary;
        }
      }

      .header-center {
        width: 60px;
        flex-shrink: 0;
      }
    }

    .rows-container {
      max-height: 500px;
      overflow-y: auto;
      border: 1px solid $border-color-light;
      border-top: none;
      border-radius: 0 0 8px 8px;
      background: #fff;
    }

    .mapping-row {
      display: flex;
      align-items: stretch;
      padding: 6px 16px;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      &.has-mapping {
        background: rgba($success-color, 0.02);
      }

      .row-left,
      .row-right {
        flex: 1;
      }

      .row-center {
        width: 60px;
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: center;

        .link-indicator {
          color: $success-color;
          font-size: 16px;
        }
      }
    }

    .field-placeholder {
      height: 48px;
    }
  }

  .field-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: #fff;
    border: 2px solid $border-color-light;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s ease;
    min-height: 44px;

    &:hover {
      border-color: $primary-color;
      box-shadow: 0 2px 8px rgba($primary-color, 0.15);
    }

    &.connecting {
      border-color: $primary-color;
      background: rgba($primary-color, 0.05);
      box-shadow: 0 0 0 3px rgba($primary-color, 0.2);
    }

    &.connected {
      border-color: $success-color;
      background: rgba($success-color, 0.05);
    }

    &.required:not(.connected):not(.has-default) {
      border-color: $warning-color;
      background: rgba($warning-color, 0.03);
    }
  }

  .field-info {
    flex: 1;
    min-width: 0;

    .field-name {
      font-weight: 500;
      color: $text-color;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .field-header {
      display: flex;
      align-items: center;
      gap: 4px;

      .required-mark {
        color: $error-color;
        font-weight: bold;
      }

      .field-label {
        font-weight: 500;
        color: $text-color;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .field-meta {
      margin-top: 2px;
      font-size: 12px;
      color: $text-color-secondary;

      .field-code {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .field-status {
    margin-left: 8px;
    flex-shrink: 0;

    .status-icon {
      font-size: 16px;
      color: $text-color-disabled;

      &.connected {
        color: $success-color;
      }

      &.warning {
        color: $warning-color;
      }
    }
  }

}
</style>
