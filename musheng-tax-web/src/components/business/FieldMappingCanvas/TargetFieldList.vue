<template>
  <div class="target-field-list">
    <div class="list-header">
      <span class="title">系统目标字段</span>
      <span class="count">共 {{ fields.length }} 个</span>
    </div>

    <div class="list-content">
      <div
        v-for="field in fields"
        :key="field.field"
        class="field-item"
        :class="{
          mapped: isMapped(field),
          selected: selectedField?.field === field.field,
          required: field.required && !isMapped(field) && !hasDefaultValue(field),
          'has-default': hasDefaultValue(field) && !isMapped(field),
          'drop-target': dropTargetField === field.field
        }"
        :data-target-field="field.field"
        @click="handleClick(field)"
        @dragover="handleDragOver(field, $event)"
        @dragleave="handleDragLeave"
        @drop="handleDrop(field, $event)"
      >
        <div class="field-content">
          <div class="field-header">
            <span class="field-name">
              <span v-if="field.required" class="required-mark">*</span>
              {{ field.label }}
            </span>
            <a-tooltip v-if="field.description" :title="field.description">
              <InfoCircleOutlined class="info-icon" />
            </a-tooltip>
          </div>
          <div class="field-meta">
            <span class="field-key">{{ field.field }}</span>
            <span class="field-type">{{ getDataTypeLabel(field.type) }}</span>
          </div>
          <div v-if="isMapped(field)" class="mapped-source">
            <LinkOutlined />
            <span>{{ getMappedSource(field) }}</span>
          </div>
          <div v-else-if="hasDefaultValue(field)" class="default-value">
            <SettingOutlined />
            <span>默认值: {{ getDefaultValue(field) }}</span>
            <a-button
              type="link"
              size="small"
              class="edit-btn"
              @click.stop="handleSetDefault(field)"
            >
              修改
            </a-button>
          </div>
          <div v-else-if="!isMapped(field)" class="set-default-link">
            <a-button type="link" size="small" @click.stop="handleSetDefault(field)">
              设置默认值
            </a-button>
          </div>
        </div>
        <div class="field-status">
          <CheckCircleFilled v-if="isMapped(field)" class="status-icon mapped" />
          <SettingFilled
            v-else-if="hasDefaultValue(field)"
            class="status-icon has-default"
          />
          <ExclamationCircleOutlined
            v-else-if="field.required"
            class="status-icon required"
          />
          <MinusCircleOutlined v-else class="status-icon unmapped" />
        </div>
      </div>

      <!-- 空状态 -->
      <a-empty
        v-if="fields.length === 0"
        description="请先选择数据类型"
        :image="Empty.PRESENTED_IMAGE_SIMPLE"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Empty } from 'ant-design-vue'
import {
  CheckCircleFilled,
  MinusCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  LinkOutlined,
  SettingOutlined,
  SettingFilled
} from '@ant-design/icons-vue'
import type { TargetField, MappingConfig, DefaultValueConfig } from './types'

const props = defineProps<{
  fields: TargetField[]
  selectedField: TargetField | null
  mappings?: MappingConfig[]
  defaultValues?: DefaultValueConfig[]
  readonly?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', field: TargetField): void
  (e: 'drop', field: TargetField): void
  (e: 'set-default', field: TargetField): void
}>()

const dropTargetField = ref<string | null>(null)

// 检查字段是否已映射
function isMapped(field: TargetField): boolean {
  return props.mappings?.some((m) => m.target === field.field) ?? false
}

// 获取映射的源字段
function getMappedSource(field: TargetField): string {
  const mapping = props.mappings?.find((m) => m.target === field.field)
  return mapping?.source ?? ''
}

// 检查是否有默认值
function hasDefaultValue(field: TargetField): boolean {
  return props.defaultValues?.some((d) => d.field === field.field) ?? false
}

// 获取默认值
function getDefaultValue(field: TargetField): string | number | boolean {
  const config = props.defaultValues?.find((d) => d.field === field.field)
  return config?.value ?? ''
}

// 获取数据类型标签
function getDataTypeLabel(dataType: string): string {
  const typeMap: Record<string, string> = {
    string: '文本',
    number: '数字',
    datetime: '日期时间',
    boolean: '布尔'
  }
  return typeMap[dataType] || dataType
}

// 点击字段
function handleClick(field: TargetField) {
  if (props.readonly) return
  emit('select', field)
}

// 设置默认值
function handleSetDefault(field: TargetField) {
  if (props.readonly) return
  emit('set-default', field)
}

// 拖拽经过
function handleDragOver(field: TargetField, event: DragEvent) {
  if (props.readonly) return
  event.preventDefault()
  dropTargetField.value = field.field
}

// 拖拽离开
function handleDragLeave() {
  dropTargetField.value = null
}

// 拖拽放置
function handleDrop(field: TargetField, event: DragEvent) {
  if (props.readonly) return
  event.preventDefault()
  dropTargetField.value = null
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
    padding: $spacing-sm;

    .field-item {
      min-height: 52px; // 最小高度，确保连线对齐
      margin-bottom: 4px; // 固定间距 = ROW_HEIGHT(56) - min-height(52)
      padding: $spacing-xs $spacing-md;
      border: 1px solid $border-color-light;
      border-radius: $border-radius-md;
      cursor: pointer;
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      transition: all 0.2s ease;
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
      }

      // 已映射+被选中 - 红色高亮（表示将解除原映射重新绑定）
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

      // 必填未映射 - 橙色警告
      &.required:not(.mapped):not(.has-default) {
        border-color: $warning-color;
        background-color: rgba($warning-color, 0.05);
      }

      // 拖拽目标 - 蓝色虚线
      &.drop-target {
        border-color: $primary-color;
        border-width: 2px;
        border-style: dashed;
        background-color: rgba($primary-color, 0.15);
        transform: scale(1.02);
        box-shadow: 0 0 0 3px rgba($primary-color, 0.2);
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
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: $font-size-sm;
          color: #d48806;

          .edit-btn {
            padding: 0 4px;
            font-size: 12px;
          }
        }

        .set-default-link {
          margin-top: 4px;

          .ant-btn {
            padding: 0;
            font-size: 12px;
          }
        }
      }

      &.has-default {
        border-color: #d48806;
        border-style: dashed;
        background-color: rgba(250, 173, 20, 0.05);
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

          &.has-default {
            color: #d48806;
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
