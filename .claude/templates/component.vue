<template>
  <div class="component-name">
    <!-- 组件内容 -->
    <slot />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'

/**
 * [组件名称]
 *
 * @description [组件功能描述]
 */

// ==================== Props定义 ====================
interface Props {
  /** 示例属性 - 数据列表 */
  data?: any[]
  /** 示例属性 - 加载状态 */
  loading?: boolean
  /** 示例属性 - 尺寸 */
  size?: 'small' | 'medium' | 'large'
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  loading: false,
  size: 'medium'
})

// ==================== Emits定义 ====================
interface Emits {
  /** 选择事件 */
  (e: 'select', item: any): void
  /** 变更事件 */
  (e: 'change', value: any): void
}

const emit = defineEmits<Emits>()

// ==================== 响应式数据 ====================
const internalValue = ref<any>(null)
const activeIndex = ref<number>(0)

// ==================== 计算属性 ====================
const computedValue = computed(() => {
  // 计算逻辑
  return props.data.length
})

const isDisabled = computed(() => {
  return props.loading || props.data.length === 0
})

// ==================== 方法 ====================
/**
 * 处理选择
 */
const handleSelect = (item: any) => {
  if (isDisabled.value) return

  emit('select', item)
}

/**
 * 处理变更
 */
const handleChange = (value: any) => {
  internalValue.value = value
  emit('change', value)
}

// ==================== 监听器 ====================
watch(
  () => props.data,
  (newData) => {
    // 数据变化时的处理
    console.log('Data changed:', newData)
  },
  { deep: true }
)

// ==================== 生命周期 ====================
onMounted(() => {
  // 组件挂载后的初始化逻辑
})

// ==================== 暴露方法 ====================
defineExpose({
  /**
   * 刷新组件
   */
  refresh: () => {
    // 刷新逻辑
  },

  /**
   * 重置组件
   */
  reset: () => {
    internalValue.value = null
    activeIndex.value = 0
  }
})
</script>

<style lang="scss" scoped>
.component-name {
  // 样式

  // 尺寸变体
  &--small {
    font-size: 12px;
  }

  &--medium {
    font-size: 14px;
  }

  &--large {
    font-size: 16px;
  }
}
</style>
