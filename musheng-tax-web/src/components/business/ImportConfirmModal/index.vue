<template>
  <a-modal
    v-model:open="visible"
    title="确认导入"
    :mask-closable="false"
    width="400px"
    @ok="handleConfirm"
    @cancel="handleCancel"
  >
    <div class="import-confirm-content">
      <div class="confirm-summary">
        <template v-if="recordCount != null">
          <span class="summary-main">将导入 <strong>{{ recordCount }}</strong> 条{{ dataTypeLabel }}到</span>
        </template>
        <template v-else>
          <span class="summary-main">将导入{{ dataTypeLabel }}到</span>
        </template>
        <div class="shop-badge">
          <ShopOutlined class="shop-icon" />
          <span>{{ shopName || '当前店铺' }}</span>
          <a-tag v-if="shopCode" size="small">{{ shopCode }}</a-tag>
        </div>
      </div>
      <p class="confirm-hint" v-if="shopName">导入后数据归属该店铺</p>
      <div class="import-info" v-if="fileName">
        <span class="info-label">文件：</span>
        <span class="info-value">{{ fileName }}</span>
      </div>
    </div>
    <template #footer>
      <a-button @click="handleCancel">取消</a-button>
      <a-button type="primary" :loading="confirmLoading" @click="handleConfirm">
        确认导入
      </a-button>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ShopOutlined } from '@ant-design/icons-vue'
import { useShopStore } from '@/stores/modules/shop'

interface Props {
  fileName?: string
  dataType?: string
  /** 导入条数，展示「将导入 X 条」 */
  recordCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  fileName: '',
  dataType: '',
  recordCount: undefined
})

const emit = defineEmits<{
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()

const shopStore = useShopStore()

// 状态
const visible = ref(false)
const confirmLoading = ref(false)

// 计算属性
const shopName = computed(() => shopStore.currentShop?.shopName || '')
const shopCode = computed(() => shopStore.currentShop?.shopCode || '')

const dataTypeLabel = computed(() => {
  const labels: Record<string, string> = {
    sales: '销售数据',
    shipping: '配送数据',
    advertising: '广告数据',
    'fba-shipment': 'FBA货件明细'
  }
  return labels[props.dataType] || props.dataType || '数据'
})

// 方法
function show() {
  visible.value = true
  confirmLoading.value = false
}

function hide() {
  visible.value = false
  confirmLoading.value = false
}

function handleConfirm() {
  confirmLoading.value = true
  emit('confirm')
  // 注意：由父组件控制关闭弹窗，以便在导入成功后关闭
}

function handleCancel() {
  visible.value = false
  emit('cancel')
}

// 导出方法供父组件调用
defineExpose({
  show,
  hide,
  setLoading: (loading: boolean) => { confirmLoading.value = loading }
})
</script>

<style lang="scss" scoped>
.import-confirm-content {
  .confirm-summary {
    margin-bottom: 12px;
    .summary-main {
      display: block;
      color: #333;
      margin-bottom: 8px;
      strong { color: #1890ff; }
    }
    .shop-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 8px 12px;
      background: #f5f7fa;
      border-radius: 6px;
      font-size: 14px;
      .shop-icon { color: #1890ff; font-size: 16px; }
    }
  }
  .confirm-hint {
    margin: 0 0 12px 0;
    font-size: 12px;
    color: #999;
  }
  .import-info {
    font-size: 13px;
    color: #666;
    .info-label { margin-right: 4px; }
    .info-value { word-break: break-all; }
  }
}
</style>
