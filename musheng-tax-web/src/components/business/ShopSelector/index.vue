<template>
  <a-dropdown :trigger="['click']" placement="bottomRight">
    <div class="shop-selector" :class="{ 'no-shop': !hasSelectedShop }">
      <ShopOutlined class="shop-icon" />
      <span class="shop-name">{{ currentShopName }}</span>
      <DownOutlined class="arrow-icon" />
    </div>
    <template #overlay>
      <a-menu @click="handleSelect" class="shop-menu">
        <a-menu-item 
          v-for="shop in shopList" 
          :key="shop.id"
          :class="{ 'selected': shop.id === currentShopId }"
        >
          <div class="shop-item">
            <CheckOutlined v-if="shop.id === currentShopId" class="check-icon" />
            <span v-else class="check-placeholder"></span>
            <span class="item-name">{{ shop.shopName }}</span>
            <span class="item-code">{{ shop.shopCode }}</span>
          </div>
        </a-menu-item>
        <template v-if="showManage">
          <a-menu-divider />
          <a-menu-item key="manage">
            <div class="shop-item">
              <SettingOutlined class="manage-icon" />
              <span class="item-name">店铺管理</span>
            </div>
          </a-menu-item>
        </template>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  ShopOutlined, 
  DownOutlined, 
  CheckOutlined, 
  SettingOutlined 
} from '@ant-design/icons-vue'
import { useShopStore } from '@/stores/modules/shop'
import { useAuthStore } from '@/stores/modules/auth'

const router = useRouter()
const shopStore = useShopStore()
const authStore = useAuthStore()

// 计算属性
const currentShopId = computed(() => shopStore.currentShopId)
const currentShopName = computed(() => shopStore.currentShopName)
const hasSelectedShop = computed(() => shopStore.hasSelectedShop)
const shopList = computed(() => shopStore.shopList)
const showManage = computed(() => authStore.isAdmin)

// 初始化加载店铺列表
onMounted(() => {
  if (!shopStore.initialized) {
    shopStore.fetchShopList()
  }
})

// 选择店铺
function handleSelect({ key }: { key: string | number }) {
  if (key === 'manage') {
    router.push('/config/shop')
    return
  }
  
  const shop = shopList.value.find(s => s.id === key)
  if (shop && shop.id !== currentShopId.value) {
    shopStore.setCurrentShop(shop)
    // 店铺切换后刷新当前页面，确保数据与当前店铺一致
    router.go(0)
  }
}
</script>

<style lang="scss" scoped>
.shop-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid transparent;
  
  &:hover {
    background: rgba(0, 0, 0, 0.04);
    border-color: rgba(0, 0, 0, 0.06);
  }
  
  &.no-shop {
    color: #ff4d4f;
    background: #fff2f0;
    border-color: #ffccc7;
    
    &:hover {
      background: #ffccc7;
    }
  }
  
  .shop-icon {
    font-size: 16px;
  }
  
  .shop-name {
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 14px;
  }
  
  .arrow-icon {
    font-size: 10px;
    color: #999;
  }
}

.shop-menu {
  min-width: 200px;
  max-height: 400px;
  overflow-y: auto;
  
  .shop-item {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .check-icon {
      color: #1890ff;
      font-size: 12px;
    }
    
    .check-placeholder {
      width: 12px;
    }
    
    .manage-icon {
      font-size: 14px;
      color: #666;
    }
    
    .item-name {
      flex: 1;
    }
    
    .item-code {
      color: #999;
      font-size: 12px;
    }
  }
  
  :deep(.ant-dropdown-menu-item) {
    &.selected {
      background: #e6f7ff;
    }
  }
}
</style>
