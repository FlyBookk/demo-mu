<template>
  <div class="mapping-toolbar">
    <div class="toolbar-left">
      <a-space>
        <!-- 模板选择 -->
        <a-select
          v-model:value="selectedTemplateId"
          placeholder="选择映射模板"
          style="width: 180px"
          allow-clear
          @change="handleTemplateChange"
        >
          <a-select-option
            v-for="template in templates"
            :key="template.id"
            :value="template.id"
          >
            {{ template.templateName }}
            <a-tag v-if="template.isDefault" color="blue" size="small">默认</a-tag>
          </a-select-option>
        </a-select>

        <!-- 自动映射 -->
        <a-tooltip title="根据字段名自动匹配">
          <a-button @click="handleAutoMap" :disabled="!hasSourceFields">
            <ThunderboltOutlined />
            智能映射
          </a-button>
        </a-tooltip>

        <!-- 清空源字段 -->
        <a-popconfirm
          title="确定清空所有源字段吗？"
          description="这将同时清除所有映射关系"
          @confirm="handleClearSource"
        >
          <a-button :disabled="!hasSourceFields">
            <ClearOutlined />
            清空源字段
          </a-button>
        </a-popconfirm>
      </a-space>
    </div>

    <div class="toolbar-right">
      <a-space>
        <!-- 映射统计 -->
        <span class="mapping-info">
          已映射 <span class="count">{{ mappingCount }}</span> 个
          <span v-if="unmappedRequiredCount > 0" class="warning">
            ({{ unmappedRequiredCount }} 个必填未映射)
          </span>
        </span>

        <!-- 重置 -->
        <a-button :disabled="!hasChanges" @click="handleReset">
          <UndoOutlined />
          重置
        </a-button>

        <!-- 保存为模板 -->
        <a-button type="primary" @click="showSaveModal = true">
          <SaveOutlined />
          保存模板
        </a-button>
      </a-space>
    </div>

    <!-- 保存模板弹窗 -->
    <a-modal
      v-model:open="showSaveModal"
      title="保存映射模板"
      :confirm-loading="saving"
      @ok="handleSaveConfirm"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="模板名称" required>
          <a-input
            v-model:value="newTemplateName"
            placeholder="请输入模板名称"
            :maxlength="50"
          />
        </a-form-item>
        <a-form-item label="设为默认">
          <a-switch v-model:checked="setAsDefault" />
          <span class="form-hint">设为默认后，该站点导入时将自动使用此模板</span>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  ThunderboltOutlined,
  UndoOutlined,
  SaveOutlined,
  ClearOutlined
} from '@ant-design/icons-vue'
import type { MappingTemplate } from './types'

const props = defineProps<{
  siteCode?: string
  hasChanges: boolean
  mappingCount: number
  unmappedRequiredCount: number
  templates?: MappingTemplate[]
  currentTemplateId?: number
  hasSourceFields?: boolean
}>()

const emit = defineEmits<{
  (e: 'save', templateName: string): void
  (e: 'load', templateId: number): void
  (e: 'reset'): void
  (e: 'auto-map'): void
  (e: 'clear-source'): void
}>()

// 选中的模板ID
const selectedTemplateId = ref<number | undefined>(props.currentTemplateId)

// 保存模板弹窗
const showSaveModal = ref(false)
const newTemplateName = ref('')
const setAsDefault = ref(false)
const saving = ref(false)

// 监听currentTemplateId变化
watch(() => props.currentTemplateId, (newId) => {
  selectedTemplateId.value = newId
})

// 模板变更
function handleTemplateChange(templateId: number | undefined) {
  if (templateId) {
    emit('load', templateId)
  }
}

// 自动映射
function handleAutoMap() {
  emit('auto-map')
}

// 重置
function handleReset() {
  emit('reset')
}

// 清空源字段
function handleClearSource() {
  emit('clear-source')
}

// 保存确认
function handleSaveConfirm() {
  if (!newTemplateName.value.trim()) {
    message.warning('请输入模板名称')
    return
  }

  saving.value = true
  emit('save', newTemplateName.value.trim())

  // 模拟保存完成
  setTimeout(() => {
    saving.value = false
    showSaveModal.value = false
    newTemplateName.value = ''
    setAsDefault.value = false
  }, 500)
}
</script>

<style lang="scss" scoped>
.mapping-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: $spacing-md;
  background: $background-color-white;
  border-bottom: 1px solid $border-color-light;
  border-radius: $border-radius-lg $border-radius-lg 0 0;

  .toolbar-left,
  .toolbar-right {
    display: flex;
    align-items: center;
  }

  .mapping-info {
    color: $text-color-secondary;

    .count {
      color: $primary-color;
      font-weight: 500;
    }

    .warning {
      color: $warning-color;
    }
  }

  .form-hint {
    margin-left: $spacing-sm;
    font-size: $font-size-sm;
    color: $text-color-secondary;
  }
}
</style>
