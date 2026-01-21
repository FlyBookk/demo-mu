/**
 * 应用全局状态管理Store
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAppStore = defineStore('app', () => {
  // ============= State =============
  /**
   * 侧边栏是否折叠
   */
  const sidebarCollapsed = ref(false)

  /**
   * 当前激活的菜单key
   */
  const activeMenuKey = ref<string>('')

  /**
   * 展开的菜单keys
   */
  const openMenuKeys = ref<string[]>([])

  /**
   * 全局loading状态
   */
  const globalLoading = ref(false)

  /**
   * 页面loading状态
   */
  const pageLoading = ref(false)

  // ============= Getters =============
  /**
   * 侧边栏宽度
   */
  const sidebarWidth = computed(() => {
    return sidebarCollapsed.value ? 60 : 200
  })

  // ============= Actions =============
  /**
   * 切换侧边栏折叠状态
   */
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  /**
   * 设置侧边栏折叠状态
   */
  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed
  }

  /**
   * 设置激活的菜单
   */
  function setActiveMenu(key: string) {
    activeMenuKey.value = key
  }

  /**
   * 设置展开的菜单
   */
  function setOpenMenuKeys(keys: string[]) {
    openMenuKeys.value = keys
  }

  /**
   * 设置全局loading
   */
  function setGlobalLoading(loading: boolean) {
    globalLoading.value = loading
  }

  /**
   * 设置页面loading
   */
  function setPageLoading(loading: boolean) {
    pageLoading.value = loading
  }

  return {
    // State
    sidebarCollapsed,
    activeMenuKey,
    openMenuKeys,
    globalLoading,
    pageLoading,

    // Getters
    sidebarWidth,

    // Actions
    toggleSidebar,
    setSidebarCollapsed,
    setActiveMenu,
    setOpenMenuKeys,
    setGlobalLoading,
    setPageLoading
  }
})
