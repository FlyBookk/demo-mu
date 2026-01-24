/**
 * 店铺状态管理Store
 * 用于管理当前选中的店铺，实现类SaaS多租户数据隔离
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getShopOptions } from '@/api/shop'
import type { Shop } from '@/types/shop'

const STORAGE_KEY = 'currentShopId'

export const useShopStore = defineStore('shop', () => {
  // ============= State =============
  
  /** 当前选中的店铺 */
  const currentShop = ref<Shop | null>(null)
  
  /** 可用的店铺列表 */
  const shopList = ref<Shop[]>([])
  
  /** 是否正在加载 */
  const loading = ref(false)
  
  /** 是否已初始化 */
  const initialized = ref(false)

  // ============= Getters =============
  
  /** 当前店铺ID */
  const currentShopId = computed(() => currentShop.value?.id)
  
  /** 当前店铺名称 */
  const currentShopName = computed(() => currentShop.value?.shopName || '请选择店铺')
  
  /** 当前店铺编码 */
  const currentShopCode = computed(() => currentShop.value?.shopCode || '')
  
  /** 是否已选择店铺 */
  const hasSelectedShop = computed(() => !!currentShop.value)

  // ============= Actions =============
  
  /**
   * 获取店铺列表
   */
  async function fetchShopList() {
    if (loading.value) return
    
    loading.value = true
    try {
      const res = await getShopOptions()
      shopList.value = res.data || []
      
      // 如果只有一个店铺，自动选择
      if (shopList.value.length === 1 && !currentShop.value) {
        setCurrentShop(shopList.value[0])
      }
      
      // 恢复之前选择的店铺
      const savedShopId = localStorage.getItem(STORAGE_KEY)
      if (savedShopId && !currentShop.value) {
        const shop = shopList.value.find(s => s.id === Number(savedShopId))
        if (shop) {
          setCurrentShop(shop)
        }
      }
      
      initialized.value = true
    } catch (error) {
      console.error('获取店铺列表失败:', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 设置当前店铺
   */
  function setCurrentShop(shop: Shop | null) {
    currentShop.value = shop
    if (shop) {
      localStorage.setItem(STORAGE_KEY, String(shop.id))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  /**
   * 根据ID设置当前店铺
   */
  function setCurrentShopById(shopId: number) {
    const shop = shopList.value.find(s => s.id === shopId)
    if (shop) {
      setCurrentShop(shop)
    }
  }

  /**
   * 清除当前店铺
   */
  function clearCurrentShop() {
    currentShop.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  /**
   * 刷新店铺列表
   */
  async function refreshShopList() {
    initialized.value = false
    await fetchShopList()
  }

  /**
   * 重置状态
   */
  function resetState() {
    currentShop.value = null
    shopList.value = []
    loading.value = false
    initialized.value = false
    localStorage.removeItem(STORAGE_KEY)
  }

  return {
    // State
    currentShop,
    shopList,
    loading,
    initialized,
    
    // Getters
    currentShopId,
    currentShopName,
    currentShopCode,
    hasSelectedShop,
    
    // Actions
    fetchShopList,
    setCurrentShop,
    setCurrentShopById,
    clearCurrentShop,
    refreshShopList,
    resetState
  }
})
