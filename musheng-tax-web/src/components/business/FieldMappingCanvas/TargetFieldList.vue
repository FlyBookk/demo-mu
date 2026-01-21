<template>
  <div class="target-field-list">
    <div class="list-header">
      <span class="title">系统目标字段</span>
      <span class="count">共 {{ fields.length }} 个</span>
    </div>

    <div class="list-content">
      <div
        v-for="field in fields"
        :key="field.name"
        class="field-item"
        :class="{
          'mapped': field.mapped,
          'selected': selectedField?.name === field.name,
          'required': field.required && !field.mapped,
          'drop-target': isDropTarget
        }"
        :data-target-field="field.name"
        @click="handleClick(field)"
        @dragover="handleDragOver($event)"
        @dragleave="handleDragLeave"
        @drop="handleDrop(field, $event)"
      >
        <div class="field-content">
          <div class="field-header">
            <span class="field-name">
              <span v-if="field.required" class="required-mark">*</span>
              {{ field.label }}
            </span>
            <a-tooltip :title="field.description">
              <InfoCircleOutlined class="info-icon" />
            </a-tooltip>
          </div>
          <div class="field-meta">
            <span class="field-key">{{ field.name }}</span>
            <span class="field-type">{{ getDataTypeLabel(field.dataType) }}</span>
          </div>
          <div v-if="field.mapped && field.mappedSource" class="mapped-source">
            <LinkOutlined />
            <span>{{ field.mappedSource }}</span>
          </div>
          <div v-else-if="field.defaultValue !== undefined" class="default-value">
            默认值: {{ field.defaultValue }}
          </div>
        </div>
        <div class="field-status">
          <CheckCircleFilled v-if="field.mapped" class="status-icon mapped" />
          <ExclamationCircleOutlined v-else-if="field.required" class="status-icon required" />
          <MinusCircleOutlined v-else class="status-icon unmapped" />
        </div>
      </div>

      <!-- 空状态 -->
      <a-empty
        v-if="fields.length === 0"
        description="暂无字段数据"
        :image="Empty.PRESENTED_IMAGE_SIMPLE"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Empty } from 'ant-design-vue'
import {
  CheckCircleFilled,
  MinusCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  LinkOutlined
} from '@ant-design/icons-vue'
import type { TargetField } from './types'

const props = defineProps<{
  fields: TargetField[]
  selectedField: TargetField | null
  readonly?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', field: TargetField): void
  (e: 'drop', field: TargetField): void
}>()

const isDropTarget = ref(false)

// 获取数据类型标签
function getDataTypeLabel(dataType: string): string {
  const typeMap: Record<string, string> = {
    string: '文本',
    number: '数字',
    datetime: '日期时间'
  }
  return typeMap[dataType] || dataType
}

// 点击字段
function handleClick(field: TargetField) {
  if (props.readonly) return
  emit('select', field)
}

// 拖拽经过
function handleDragOver(event: DragEvent) {
  if (props.readonly) return
  event.preventDefault()
  isDropTarget.value = true
}

// 拖拽离开
function handleDragLeave() {
  isDropTarget.value = false
}

// 拖拽放置
function handleDrop(field: TargetField, event: DragEvent) {
  if (props.readonly) return
  event.preventDefault()
  isDropTarget.value = false
  emit('drop', field)
}
</script>

<style lang="scss" scoped>
.target-field-list {
  width: 280px;
  flex-shrink: 0;
  background: $background-color-white;
  border: 1px solid $border-color;
  border-radius: $border-radius-lg;
  display: flex;
  flex-direction: column;

  .list-header {
    padding: $spacing-md;
    border-bottom: 1px solid $border-color-light;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      font-weight: 500;
      color: $text-color;
    }

    .count {
      font-size: $font-size-sm;
      color: $text-color-secondary;
    }
  }

  .list-content {
    flex: 1;
    overflow-y: auto;
    padding: $spacing-sm;

    .field-item {
      padding: $spacing-sm $spacing-md;
      margin-bottom: $spacing-xs;
      border: 1px solid $border-color-light;
      border-radius: $border-radius-md;
      cursor: pointer;
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      transition: all 0.2s ease;

      &:hover {
        border-color: $primary-color;
        background-color: rgba($primary-color, 0.05);
      }

      &.selected {
        border-color: $primary-color;
        background-color: rgba($primary-color, 0.1);
      }

      &.mapped {
        border-color: $success-color;
        background-color: rgba($success-color, 0.05);
      }

      &.required:not(.mapped) {
        border-color: $warning-color;
        background-color: rgba($warning-color, 0.05);
      }

      &.drop-target {
        border-color: $primary-color;
        border-style: dashed;
        background-color: rgba($primary-color, 0.1);
        transform: scale(1.02);
      }

      .field-content {
        flex: 1;
        min-width: 0;

        .field-header {
          display: flex;
          align-items: center;
          gap: $spacing-xs;

          .field-name {
            font-weight: 500;
            color: $text-color;

            .required-mark {
              color: $error-color;
              margin-right: 2px;
            }
          }

          .info-icon {
            color: $text-color-secondary;
            font-size: 12px;
          }
        }

        .field-meta {
          margin-top: 2px;
          display: flex;
          gap: $spacing-sm;
          font-size: $font-size-sm;
          color: $text-color-secondary;

          .field-key {
            font-family: 'SFMono-Regular', Consolas, monospace;
          }

          .field-type {
            padding: 0 $spacing-xs;
            background: $background-color-light;
            border-radius: $border-radius-sm;
          }
        }

        .mapped-source {
          margin-top: 4px;
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: $font-size-sm;
          color: $success-color;
        }

        .default-value {
          margin-top: 4px;
          font-size: $font-size-sm;
          color: $text-color-secondary;
        }
      }

      .field-status {
        margin-left: $spacing-sm;
        margin-top: 2px;

        .status-icon {
          font-size: 16px;

          &.mapped {
            color: $success-color;
          }

          &.required {
            color: $warning-color;
          }

          &.unmapped {
            color: $text-color-disabled;
          }
        }
      }
    }
  }
}
</style>
