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
          'mapped': isMapped(field),
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
        <div class="field-actions">
          <CheckCircleFilled v-if="isMapped(field)" class="status-icon mapped" />
          <MinusCircleOutlined v-else class="status-icon unmapped" />
          <a-button
            v-if="allowDelete && !readonly"
            type="text"
            size="small"
            class="delete-btn"
            @click.stop="handleDelete(field)"
          >
            <DeleteOutlined />
          </a-button>
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
import { computed } from 'vue'
import { Empty } from 'ant-design-vue'
import {
  CheckCircleFilled,
  MinusCircleOutlined,
  InfoCircleOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import type { SourceField, MappingConfig } from './types'

const props = defineProps<{
  fields: SourceField[]
  selectedField: SourceField | null
  mappings?: MappingConfig[]
  readonly?: boolean
  allowDelete?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', field: SourceField): void
  (e: 'drag-start', field: SourceField, event: MouseEvent): void
  (e: 'delete', field: SourceField): void
}>()

// 计算字段是否已映射
function isMapped(field: SourceField): boolean {
  return props.mappings?.some((m) => m.source === field.name) ?? field.mapped ?? false
}

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

// 删除字段
function handleDelete(field: SourceField) {
  emit('delete', field)
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
    padding: $spacing-sm;

    .field-item {
      height: 52px; // 固定高度，确保连线对齐
      margin-bottom: 4px; // 固定间距 = ROW_HEIGHT(56) - height(52)
      padding: 0 $spacing-md;
      border: 1px solid $border-color-light;
      border-radius: $border-radius-md;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: space-between;
      transition: all 0.2s ease;
      user-select: none;
      box-sizing: border-box;

      &:hover {
        border-color: $primary-color;
        background-color: rgba($primary-color, 0.05);
      }

      // 普通选中状态 - 蓝色高亮
      &.selected {
        border-color: $primary-color;
        border-width: 2px;
        background-color: rgba($primary-color, 0.15);
        box-shadow: 0 0 0 3px rgba($primary-color, 0.2);

        .field-name {
          color: $primary-color;
        }
      }

      // 已映射状态 - 绿色
      &.mapped {
        border-color: $success-color;
        background-color: rgba($success-color, 0.05);

        &:hover {
          background-color: rgba($success-color, 0.1);
        }
      }

      // 已映射+被选中 - 红色高亮（表示将解除映射）
      &.mapped.selected {
        border-color: $error-color;
        border-width: 2px;
        background-color: rgba($error-color, 0.15);
        box-shadow: 0 0 0 3px rgba($error-color, 0.2);

        .field-name {
          color: $error-color;
        }

        .status-icon.mapped {
          color: $error-color;
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
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
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
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .field-actions {
        margin-left: $spacing-sm;
        display: flex;
        align-items: center;
        gap: 4px;

        .status-icon {
          font-size: 16px;

          &.mapped {
            color: $success-color;
          }

          &.unmapped {
            color: $text-color-disabled;
          }
        }

        .delete-btn {
          opacity: 0;
          color: $text-color-secondary;
          transition: opacity 0.2s;

          &:hover {
            color: $error-color;
          }
        }
      }

      &:hover .delete-btn {
        opacity: 1;
      }
    }
  }
}
</style>
