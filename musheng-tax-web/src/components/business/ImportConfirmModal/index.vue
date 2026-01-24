<template>
  <a-modal
    v-model:open="visible"
    title="确认导入"
    :mask-closable="false"
    width="480px"
    @ok="handleConfirm"
    @cancel="handleCancel"
  >
    <div class="import-confirm-content">
      <a-alert type="warning" show-icon class="confirm-alert">
        <template #message>
          <div class="alert-content">
            <p class="alert-title">您即将向以下店铺导入数据：</p>
            <div class="shop-info">
              <ShopOutlined class="shop-icon" />
              <span class="shop-name">{{ shopName }}</span>
              <a-tag color="blue">{{ shopCode }}</a-tag>
            </div>
            <p class="alert-desc">
              请确认店铺选择正确，导入后数据将归属该店铺，无法更改。
            </p>
          </div>
        </template>
      </a-alert>
      
      <div class="import-info" v-if="fileName">
        <div class="info-item">
          <span class="info-label">导入文件：</span>
          <span class="info-value">{{ fileName }}</span>
        </div>
        <div class="info-item" v-if="dataType">
          <span class="info-label">数据类型：</span>
          <span class="info-value">{{ dataTypeLabel }}</span>
        </div>
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
}

const props = withDefaults(defineProps<Props>(), {
  fileName: '',
  dataType: ''
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
  return labels[props.dataType] || props.dataType
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
  .confirm-alert {
    margin-bottom: 16px;
    
    .alert-content {
      .alert-title {
        margin: 0 0 12px 0;
        font-weight: 500;
        color: #333;
      }
      
      .shop-info {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px 16px;
        background: #f0f5ff;
        border-radius: 6px;
        margin-bottom: 12px;
        
        .shop-icon {
          font-size: 20px;
          color: #1890ff;
        }
        
        .shop-name {
          font-size: 16px;
          font-weight: 600;
          color: #1890ff;
        }
      }
      
      .alert-desc {
        margin: 0;
        color: #666;
        font-size: 13px;
      }
    }
  }
  
  .import-info {
    padding: 12px 16px;
    background: #fafafa;
    border-radius: 6px;
    
    .info-item {
      display: flex;
      align-items: center;
      margin-bottom: 8px;
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .info-label {
        color: #666;
        width: 80px;
      }
      
      .info-value {
        color: #333;
        word-break: break-all;
      }
    }
  }
}
</style>
