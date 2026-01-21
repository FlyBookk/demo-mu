<template>
  <a-modal
    :open="visible"
    title="设置默认值"
    width="480px"
    @update:open="$emit('update:visible', $event)"
    @ok="handleConfirm"
    @cancel="handleCancel"
  >
    <div class="default-value-modal" v-if="field">
      <a-descriptions :column="1" size="small" bordered>
        <a-descriptions-item label="字段名">
          {{ field.field }}
        </a-descriptions-item>
        <a-descriptions-item label="中文标签">
          {{ field.label }}
        </a-descriptions-item>
        <a-descriptions-item label="字段类型">
          <a-tag>{{ fieldTypeLabel }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item v-if="field.description" label="描述">
          {{ field.description }}
        </a-descriptions-item>
      </a-descriptions>

      <div class="value-input">
        <div class="label">默认值：</div>

        <!-- 字符串类型 -->
        <a-input
          v-if="field.type === 'string'"
          v-model:value="inputValue"
          placeholder="请输入默认值"
          :maxlength="field.maxLength"
        />

        <!-- 数字类型 -->
        <a-input-number
          v-else-if="field.type === 'number'"
          v-model:value="inputValue"
          placeholder="请输入默认值"
          :precision="field.precision || 2"
          style="width: 100%"
        />

        <!-- 日期类型 -->
        <a-date-picker
          v-else-if="field.type === 'datetime'"
          v-model:value="inputValue"
          placeholder="请选择日期"
          style="width: 100%"
        />

        <!-- 布尔类型 -->
        <a-switch v-else-if="field.type === 'boolean'" v-model:checked="inputValue" />
      </div>

      <div v-if="field.required" class="required-hint">
        <WarningOutlined />
        <span>此字段为必填字段，未映射时必须设置默认值</span>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { WarningOutlined } from '@ant-design/icons-vue'
import type { TargetField } from './types'

const props = defineProps<{
  visible: boolean
  field: TargetField | null
  initialValue?: string | number | boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void
  (e: 'confirm', value: string | number | boolean): void
}>()

const inputValue = ref<any>(null)

const fieldTypeLabel = computed(() => {
  const map: Record<string, string> = {
    string: '字符串',
    number: '数字',
    datetime: '日期时间',
    boolean: '布尔值'
  }
  return props.field ? map[props.field.type] || props.field.type : ''
})

// 监听字段变化，初始化值
watch(
  () => [props.visible, props.field],
  ([visible, field]) => {
    if (visible && field) {
      inputValue.value = props.initialValue ?? (field as TargetField).defaultValue ?? null
    }
  },
  { immediate: true }
)

const handleConfirm = () => {
  if (inputValue.value === null || inputValue.value === undefined || inputValue.value === '') {
    // 可以清除默认值
    emit('confirm', '')
  } else {
    emit('confirm', inputValue.value)
  }
  emit('update:visible', false)
}

const handleCancel = () => {
  emit('update:visible', false)
}
</script>

<style lang="scss" scoped>
.default-value-modal {
  .value-input {
    margin-top: 16px;

    .label {
      margin-bottom: 8px;
      font-weight: 500;
    }
  }

  .required-hint {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 16px;
    padding: 8px 12px;
    background: #fff7e6;
    border-radius: 4px;
    color: #d48806;
    font-size: 13px;
  }
}
</style>
