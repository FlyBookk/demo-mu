<template>
  <div class="source-field-list">
    <div class="list-header">
      <span class="title">CSV文件字段</span>
      <span class="count">共 {{ fields.length }} 个</span>
    </div>

    <div class="list-content">
      <div
        v-for="field in fields"
        :key="field.name"
        class="field-item"
        :class="{
          'mapped': field.mapped,
          'selected': selectedField?.name === field.name
        }"
        :data-source-field="field.name"
        :draggable="!readonly"
        @click="handleClick(field)"
        @dragstart="handleDragStart(field, $event)"
        @mousedown="handleMouseDown(field, $event)"
      >
        <div class="field-content">
          <div class="field-header">
            <span class="field-name">{{ field.name }}</span>
            <a-tooltip v-if="field.sample" :title="`示例: ${field.sample}`">
              <InfoCircleOutlined class="info-icon" />
            </a-tooltip>
          </div>
          <div class="field-sample" v-if="field.sample">
            {{ truncateSample(field.sample) }}
          </div>
        </div>
        <div class="field-status">
          <CheckCircleFilled v-if="field.mapped" class="status-icon mapped" />
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
import { Empty } from 'ant-design-vue'
import {
  CheckCircleFilled,
  MinusCircleOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue'
import type { SourceField } from './types'

const props = defineProps<{
  fields: SourceField[]
  selectedField: SourceField | null
  readonly?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', field: SourceField): void
  (e: 'drag-start', field: SourceField, event: MouseEvent): void
}>()

// 截断示例数据
function truncateSample(sample: string): string {
  return sample.length > 30 ? sample.slice(0, 30) + '...' : sample
}

// 点击字段
function handleClick(field: SourceField) {
  if (props.readonly) return
  emit('select', field)
}

// 拖拽开始
function handleDragStart(field: SourceField, event: DragEvent) {
  if (props.readonly) {
    event.preventDefault()
    return
  }
  // 设置拖拽数据
  event.dataTransfer?.setData('text/plain', field.name)
}

// 鼠标按下
function handleMouseDown(field: SourceField, event: MouseEvent) {
  if (props.readonly) return
  emit('drag-start', field, event)
}
</script>

<style lang="scss" scoped>
.source-field-list {
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
      align-items: center;
      justify-content: space-between;
      transition: all 0.2s ease;
      user-select: none;

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

        &:hover {
          background-color: rgba($success-color, 0.1);
        }
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
            @include text-ellipsis;
          }

          .info-icon {
            color: $text-color-secondary;
            font-size: 12px;
          }
        }

        .field-sample {
          margin-top: 2px;
          font-size: $font-size-sm;
          color: $text-color-secondary;
          @include text-ellipsis;
        }
      }

      .field-status {
        margin-left: $spacing-sm;

        .status-icon {
          font-size: 16px;

          &.mapped {
            color: $success-color;
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
