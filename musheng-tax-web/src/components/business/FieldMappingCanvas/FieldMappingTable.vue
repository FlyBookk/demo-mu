<template>
  <div class="field-mapping-table">
    <!-- 表头 -->
    <div class="table-header">
      <div class="header-cell source-header">
        <span class="title">源字段 (CSV)</span>
        <span class="count">{{ sourceFields.length }} 个</span>
      </div>
      <div class="header-cell arrow-header"></div>
      <div class="header-cell target-header">
        <span class="title">目标字段 (系统)</span>
        <span class="count">{{ targetFields.length }} 个</span>
      </div>
    </div>

    <!-- 映射行 -->
    <div class="table-body">
      <div
        v-for="(field, index) in sourceFields"
        :key="field.name"
        class="mapping-row"
        :class="{ mapped: getMappedTarget(field.name) }"
      >
        <!-- 源字段 -->
        <div class="source-cell">
          <div class="field-info">
            <span class="field-index">{{ index + 1 }}</span>
            <span class="field-name" :title="field.name">{{ field.name }}</span>
          </div>
          <a-button
            type="text"
            size="small"
            class="delete-btn"
            @click="$emit('delete-source', field)"
          >
            <DeleteOutlined />
          </a-button>
        </div>

        <!-- 箭头 -->
        <div class="arrow-cell">
          <ArrowRightOutlined v-if="getMappedTarget(field.name)" class="arrow-icon mapped" />
          <span v-else class="arrow-placeholder">→</span>
        </div>

        <!-- 目标字段选择 -->
        <div class="target-cell">
          <a-select
            :value="getMappedTarget(field.name)"
            placeholder="请选择目标字段"
            allow-clear
            show-search
            :filter-option="filterOption"
            style="width: 100%"
            @change="(value: string | undefined) => handleMappingChange(field.name, value)"
          >
            <a-select-option
              v-for="target in availableTargets(field.name)"
              :key="target.field"
              :value="target.field"
              :disabled="isTargetUsed(target.field, field.name)"
            >
              <div class="target-option">
                <span v-if="target.required" class="required-mark">*</span>
                <span class="target-label">{{ target.label }}</span>
                <span class="target-field">({{ target.field }})</span>
              </div>
            </a-select-option>
          </a-select>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="sourceFields.length === 0" class="empty-state">
        <a-empty description="请上传文件或粘贴表头获取源字段" />
      </div>
    </div>

    <!-- 未映射的必填目标字段提示 -->
    <div v-if="unmappedRequiredTargets.length > 0" class="unmapped-warning">
      <WarningOutlined />
      <span>以下必填字段未映射：</span>
      <a-tag v-for="t in unmappedRequiredTargets" :key="t.field" color="warning">
        {{ t.label }}
      </a-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  ArrowRightOutlined,
  DeleteOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import type { SourceField, TargetField, MappingConfig } from './types'

const props = defineProps<{
  sourceFields: SourceField[]
  targetFields: TargetField[]
  mappings: MappingConfig[]
}>()

const emit = defineEmits<{
  (e: 'update:mappings', mappings: MappingConfig[]): void
  (e: 'delete-source', field: SourceField): void
}>()

// 根据源字段获取映射的目标字段
function getMappedTarget(sourceName: string): string | undefined {
  const mapping = props.mappings.find((m) => m.source === sourceName)
  return mapping?.target
}

// 检查目标字段是否已被其他源字段使用
function isTargetUsed(targetField: string, currentSource: string): boolean {
  return props.mappings.some(
    (m) => m.target === targetField && m.source !== currentSource
  )
}

// 获取可用的目标字段（包括当前已选的）
function availableTargets(currentSource: string): TargetField[] {
  return props.targetFields
}

// 处理映射变更
function handleMappingChange(sourceName: string, targetField: string | undefined) {
  // 移除该源字段的旧映射
  let newMappings = props.mappings.filter((m) => m.source !== sourceName)

  // 如果选择了新目标，添加新映射
  if (targetField) {
    // 移除该目标字段的旧映射（如果有其他源字段映射到它）
    newMappings = newMappings.filter((m) => m.target !== targetField)
    newMappings.push({ source: sourceName, target: targetField })
  }

  emit('update:mappings', newMappings)
}

// 下拉搜索过滤
function filterOption(input: string, option: any): boolean {
  const target = props.targetFields.find((t) => t.field === option.value)
  if (!target) return false
  const searchText = input.toLowerCase()
  return (
    target.label.toLowerCase().includes(searchText) ||
    target.field.toLowerCase().includes(searchText)
  )
}

// 未映射的必填目标字段
const unmappedRequiredTargets = computed(() => {
  const mappedTargets = new Set(props.mappings.map((m) => m.target))
  return props.targetFields.filter(
    (t) => t.required && !mappedTargets.has(t.field)
  )
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.field-mapping-table {
  border: 1px solid $border-color-light;
  border-radius: 8px;
  overflow: hidden;

  .table-header {
    display: flex;
    background: #fafafa;
    border-bottom: 1px solid $border-color-light;

    .header-cell {
      padding: 12px 16px;
      display: flex;
      align-items: center;
      gap: 8px;

      .title {
        font-weight: 600;
        color: $text-color;
      }

      .count {
        font-size: 12px;
        color: $text-color-secondary;
      }
    }

    .source-header {
      flex: 1;
      min-width: 200px;
    }

    .arrow-header {
      width: 50px;
      flex-shrink: 0;
    }

    .target-header {
      flex: 1.5;
      min-width: 280px;
    }
  }

  .table-body {
    max-height: 500px;
    overflow-y: auto;
  }

  .mapping-row {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #f5f5f5;
    transition: background 0.2s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #fafafa;

      .delete-btn {
        opacity: 1;
      }
    }

    &.mapped {
      background: rgba($success-color, 0.03);

      .arrow-icon {
        color: $success-color;
      }
    }

    .source-cell {
      flex: 1;
      min-width: 200px;
      padding: 10px 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;

      .field-info {
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
        flex: 1;

        .field-index {
          width: 24px;
          height: 24px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: #f0f0f0;
          border-radius: 4px;
          font-size: 12px;
          color: $text-color-secondary;
          flex-shrink: 0;
        }

        .field-name {
          font-weight: 500;
          color: $text-color;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .delete-btn {
        opacity: 0;
        color: $text-color-secondary;
        transition: opacity 0.2s, color 0.2s;

        &:hover {
          color: $error-color;
        }
      }
    }

    .arrow-cell {
      width: 50px;
      flex-shrink: 0;
      display: flex;
      align-items: center;
      justify-content: center;

      .arrow-icon {
        font-size: 16px;
      }

      .arrow-placeholder {
        color: #d9d9d9;
      }
    }

    .target-cell {
      flex: 1.5;
      min-width: 280px;
      padding: 10px 16px;
    }
  }

  .empty-state {
    padding: 40px;
  }

  .unmapped-warning {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: #fffbe6;
    border-top: 1px solid #ffe58f;
    color: #d48806;
    font-size: 13px;
  }

  .target-option {
    display: flex;
    align-items: center;
    gap: 4px;

    .required-mark {
      color: $error-color;
      font-weight: bold;
    }

    .target-label {
      color: $text-color;
    }

    .target-field {
      color: $text-color-secondary;
      font-size: 12px;
    }
  }
}
</style>
