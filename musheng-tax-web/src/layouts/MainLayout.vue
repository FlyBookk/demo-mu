<template>
  <a-layout class="main-layout">
    <!-- 侧边栏 -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      :width="200"
      :collapsed-width="60"
      class="layout-sider"
    >
      <!-- Logo -->
      <div class="logo">
        <img src="@/assets/images/logo.svg" alt="Logo" class="logo-img" />
        <span v-show="!collapsed" class="logo-text">远洋税务</span>
      </div>

      <!-- 菜单 -->
      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        mode="inline"
        theme="dark"
        :items="menuItems"
        @click="handleMenuClick"
      />
    </a-layout-sider>

    <a-layout>
      <!-- 头部 -->
      <a-layout-header class="layout-header">
        <div class="header-left">
          <component
            :is="collapsed ? MenuUnfoldOutlined : MenuFoldOutlined"
            class="trigger"
            @click="toggleCollapsed"
          />
          <!-- 面包屑 -->
          <a-breadcrumb class="breadcrumb">
            <a-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              <router-link v-if="item.path" :to="item.path">{{ item.title }}</router-link>
              <span v-else>{{ item.title }}</span>
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 平台切换器 -->
          <PlatformSwitcher style="margin-right: 12px" />
          <!-- 店铺选择器 -->
          <ShopSelector class="shop-selector-wrapper" />
          
          <!-- 用户下拉 -->
          <a-dropdown>
            <div class="user-info">
              <a-avatar :size="32">
                {{ userInitial }}
              </a-avatar>
              <span class="username">{{ authStore.realName || authStore.username }}</span>
            </div>
            <template #overlay>
              <a-menu @click="handleUserMenuClick">
                <a-menu-item key="profile">
                  <UserOutlined />
                  <span>个人信息</span>
                </a-menu-item>
                <a-menu-item key="password">
                  <KeyOutlined />
                  <span>修改密码</span>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout">
                  <LogoutOutlined />
                  <span>退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <!-- 内容区 -->
      <a-layout-content class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>

  <!-- 个人信息弹窗 -->
  <a-modal
    v-model:open="profileVisible"
    title="个人信息"
    :confirm-loading="profileLoading"
    @ok="handleProfileOk"
    ok-text="保存"
    cancel-text="取消"
  >
    <a-form ref="profileFormRef" :model="profileForm" layout="vertical" style="margin-top: 16px">
      <a-form-item label="用户名">
        <a-input :value="authStore.username" disabled />
      </a-form-item>
      <a-form-item label="真实姓名" name="realName" :rules="[{ required: true, message: '请输入真实姓名' }]">
        <a-input v-model:value="profileForm.realName" placeholder="请输入真实姓名" />
      </a-form-item>
      <a-form-item label="邮箱" name="email" :rules="[{ type: 'email', message: '邮箱格式不正确' }]">
        <a-input v-model:value="profileForm.email" placeholder="请输入邮箱" />
      </a-form-item>
      <a-form-item label="手机号" name="phone">
        <a-input v-model:value="profileForm.phone" placeholder="请输入手机号" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 修改密码弹窗 -->
  <a-modal
    v-model:open="passwordVisible"
    title="修改密码"
    :confirm-loading="passwordLoading"
    @ok="handlePasswordOk"
    ok-text="确认修改"
    cancel-text="取消"
  >
    <a-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" layout="vertical" style="margin-top: 16px">
      <a-form-item label="原密码" name="oldPassword">
        <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入原密码" />
      </a-form-item>
      <a-form-item label="新密码" name="newPassword">
        <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码（6-20位）" />
      </a-form-item>
      <a-form-item label="确认新密码" name="confirmPassword">
        <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  KeyOutlined,
  LogoutOutlined,
  DashboardOutlined,
  SettingOutlined,
  ShoppingCartOutlined,
  CarOutlined,
  InboxOutlined,
  FileTextOutlined,
  NotificationOutlined,
  DollarOutlined,
  BarChartOutlined,
  ToolOutlined
} from '@ant-design/icons-vue'
import type { MenuProps, ItemType } from 'ant-design-vue'
import { message, Form } from 'ant-design-vue'
import { useAuthStore } from '@/stores/modules/auth'
import { useAppStore } from '@/stores/modules/app'
import ShopSelector from '@/components/business/ShopSelector/index.vue'
import PlatformSwitcher from '@/components/business/PlatformSwitcher/index.vue'
import { changePassword, updateProfile } from '@/api/auth'
import type { UpdateProfileParams, ChangePasswordParams } from '@/types/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

// 侧边栏折叠状态
const collapsed = computed({
  get: () => appStore.sidebarCollapsed,
  set: (value) => appStore.setSidebarCollapsed(value)
})

// 选中的菜单
const selectedKeys = ref<string[]>([])

// 平台状态
const platform = ref(localStorage.getItem('platform') || 'AMAZON')
// 监听平台切换事件
if (typeof window !== 'undefined') {
  window.addEventListener('platform-change', (e: any) => {
    platform.value = e.detail
  })
}

// 展开的菜单
const openKeys = ref<string[]>([])

// 图标映射
const iconMap: Record<string, any> = {
  DashboardOutlined,
  SettingOutlined,
  ShoppingCartOutlined,
  CarOutlined,
  InboxOutlined,
  NotificationOutlined,
  DollarOutlined,
  BarChartOutlined,
  ToolOutlined
}

// 菜单配置
const menuItems = computed<ItemType[]>(() => {
  const items: ItemType[] = [
    {
      key: 'Dashboard',
      icon: () => h(DashboardOutlined),
      label: '首页'
    },
    {
      key: 'Config',
      icon: () => h(SettingOutlined),
      label: '基础配置',
      children: [
        { key: 'Currency', label: '货币管理' },
        { key: 'Marketplace', label: '站点管理' },
        { key: 'Shop', label: '店铺管理' },
        ...(platform.value === 'AMAZON' ? [
          { key: 'TransactionType', label: '交易类型映射(AMZ)' },
          { key: 'FieldMapping', label: '字段映射模板(AMZ)' },
          { key: 'DocumentPartyConfig', label: '交易方配置(AMZ)' },
        ] : []),
        ...(platform.value === 'TIKTOK' ? [
          { key: 'TiktokPartyConfig', label: '交易方配置' },
        ] : []),
        { key: 'ImportRecord', label: '导入记录' },
        { key: 'DataClean', label: '数据清理' },
        { key: 'ExportSetting', label: '导出设置' }
      ]
    },
    {
      key: 'Rate',
      icon: () => h(DollarOutlined),
      label: '汇率管理',
      children: [
        { key: 'RateImport', label: '汇率导入' },
        { key: 'RateList', label: '汇率查询' }
      ]
    },
    {
      key: 'Sales',
      icon: () => h(ShoppingCartOutlined),
      label: '销售数据',
      children: [
        { key: 'SalesImport', label: '数据导入' },
        { key: 'SalesList', label: '数据列表' }
      ]
    },
    {
      key: 'Shipping',
      icon: () => h(CarOutlined),
      label: '配送数据',
      children: [
        { key: 'ShippingImport', label: '数据导入' },
        { key: 'ShippingList', label: '数据列表' }
      ]
    },
    {
      key: 'FbaShipment',
      icon: () => h(InboxOutlined),
      label: 'FBA货件明细',
      children: [
        { key: 'FbaShipmentImport', label: '数据导入' },
        { key: 'FbaShipmentList', label: '数据列表' }
      ]
    },
    {
      key: 'Document',
      icon: () => h(FileTextOutlined),
      label: 'FBA单据管理',
      children: [
        { key: 'SettlementDerivation', label: '结算推导' },
        { key: 'DocumentList', label: '单据列表' },
        { key: 'DocumentGenerate', label: 'PO/DN生成' },
        { key: 'SettlementGenerate', label: '结算单/INV生成' },
        { key: 'MskuList', label: 'MSKU列表' }
      ]
    },
    {
      key: 'Advertising',
      icon: () => h(NotificationOutlined),
      label: '广告数据',
      children: [
        { key: 'AdvertisingImport', label: '广告费录入' },
        { key: 'AdvertisingList', label: '数据列表' }
      ]
    },
    {
      key: 'Report',
      icon: () => h(BarChartOutlined),
      label: '报税汇总',
      children: [
        { key: 'TaxSummary', label: '报税汇总' }
      ]
    }
  ]

  // ========== TikTok 菜单 ==========
  const tiktokItems: ItemType[] = [
    {
      key: 'TiktokProduct',
      icon: () => h(InboxOutlined),
      label: '商品管理',
      children: [
        { key: 'TiktokProductList', label: '商品列表' },
        { key: 'TiktokProductImport', label: '对照表导入' }
      ]
    },
    {
      key: 'TiktokShipment',
      icon: () => h(InboxOutlined),
      label: 'FBT货件',
      children: [
        { key: 'TiktokShipmentList', label: '货件列表' },
        { key: 'TiktokShipmentImport', label: '货件导入' }
      ]
    },
    {
      key: 'TiktokSettlement',
      icon: () => h(DollarOutlined),
      label: '结算数据',
      children: [
        { key: 'TiktokSettlementImport', label: '结算单导入' },
        { key: 'TiktokSettlementOrders', label: '订单明细' }
      ]
    },
    {
      key: 'TiktokDocument',
      icon: () => h(FileTextOutlined),
      label: '单据管理',
      children: [
        { key: 'TiktokDerivation', label: '结算推导' },
        { key: 'TiktokMskuList', label: 'MSKU列表' },
        { key: 'TiktokDocumentList', label: '单据列表' },
        { key: 'TiktokDocumentGenerate', label: 'PO/DN生成' },
        { key: 'TiktokSettlementGenerate', label: '结算单/INV生成' }
      ]
    },
    {
      key: 'TiktokReport',
      icon: () => h(BarChartOutlined),
      label: '报税汇总'
    }
  ]

  // Amazon 专属菜单（销售/配送/FBA/广告/报税）
  const amazonOnlyKeys = ['Sales', 'Shipping', 'FbaShipment', 'Document', 'Advertising', 'Report']
  // TikTok 专属菜单
  const tiktokOnlyKeys = ['TiktokProduct', 'TiktokShipment', 'TiktokSettlement', 'TiktokDocument', 'TiktokReport']

  // 按平台过滤
  let filteredItems = items.filter(item => {
    const key = (item as any).key as string
    if (platform.value === 'TIKTOK') {
      return !amazonOnlyKeys.includes(key)
    }
    return !tiktokOnlyKeys.includes(key)
  })

  // TikTok 模式下插入 TK 菜单（在汇率管理后面）
  if (platform.value === 'TIKTOK') {
    const rateIdx = filteredItems.findIndex(i => (i as any).key === 'Rate')
    filteredItems.splice(rateIdx + 1, 0, ...tiktokItems)
  }

  // 管理员显示系统管理菜单
  if (authStore.isAdmin) {
    filteredItems.push({
      key: 'System',
      icon: () => h(ToolOutlined),
      label: '系统管理',
      children: [
        { key: 'SystemUser', label: '用户管理' },
        { key: 'SystemRole', label: '权限管理' },
        { key: 'SystemLog', label: '操作日志' }
      ]
    })
  }

  return filteredItems
})

// 面包屑
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta.title)
  const crumbs = matched.map(item => ({
    title: item.meta.title as string,
    path: item.path
  }))
  return crumbs
})

// 用户首字母
const userInitial = computed(() => {
  const name = authStore.realName || authStore.username || 'U'
  return name.charAt(0).toUpperCase()
})

// 切换折叠状态
function toggleCollapsed() {
  appStore.toggleSidebar()
}

// 菜单点击
const handleMenuClick: MenuProps['onClick'] = (info) => {
  const key = info.key as string
  router.push({ name: key })
}

// ── 个人信息弹窗 ──────────────────────────────────────────
const profileVisible = ref(false)
const profileLoading = ref(false)
const profileFormRef = ref()
const profileForm = ref({
  realName: '',
  email: '',
  phone: ''
})

function openProfileModal() {
  profileForm.value = {
    realName: authStore.userInfo?.realName || '',
    email: authStore.userInfo?.email || '',
    phone: authStore.userInfo?.phone || ''
  }
  profileVisible.value = true
}

async function handleProfileOk() {
  try {
    await profileFormRef.value.validate()
    profileLoading.value = true
    await updateProfile(profileForm.value)
    // 更新本地 store 中的用户信息
    if (authStore.userInfo) {
      authStore.userInfo.realName = profileForm.value.realName
      authStore.userInfo.email = profileForm.value.email
      authStore.userInfo.phone = profileForm.value.phone
    }
    message.success('个人信息更新成功')
    profileVisible.value = false
  } catch (e: any) {
    if (e?.errorFields) return // 表单校验失败，不提示
    message.error(e?.message || '更新失败，请重试')
  } finally {
    profileLoading.value = false
  }
}

// ── 修改密码弹窗 ──────────────────────────────────────────
const passwordVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref()
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 6, max: 20, message: '密码长度6-20位' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码' },
    {
      validator: (_: any, value: string) => {
        if (value && value !== passwordForm.value.newPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      }
    }
  ]
}

function openPasswordModal() {
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  passwordVisible.value = true
}

async function handlePasswordOk() {
  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true
    await changePassword(passwordForm.value)
    message.success('密码修改成功，请重新登录')
    passwordVisible.value = false
    authStore.logoutAction()
  } catch (e: any) {
    if (e?.errorFields) return
    message.error(e?.message || '修改失败，请重试')
  } finally {
    passwordLoading.value = false
  }
}

// 用户菜单点击
function handleUserMenuClick({ key }: { key: string }) {
  switch (key) {
    case 'profile':
      openProfileModal()
      break
    case 'password':
      openPasswordModal()
      break
    case 'logout':
      authStore.logoutAction()
      break
  }
}

// 监听路由变化，更新菜单状态
watch(
  () => route.name,
  (name) => {
    if (name) {
      selectedKeys.value = [name as string]
      // 找到父级菜单展开
      const matched = route.matched
      if (matched.length > 1) {
        const parent = matched[1]
        if (parent?.name) {
          openKeys.value = [parent.name as string]
        }
      }
    }
  },
  { immediate: true }
)
</script>

<style lang="scss" scoped>
.main-layout {
  min-height: 100vh;

  .layout-sider {
    background: $background-color-dark;

    .logo {
      height: $header-height;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 $spacing-md;
      background: rgba(255, 255, 255, 0.05);

      .logo-img {
        width: 32px;
        height: 32px;
      }

      .logo-text {
        margin-left: $spacing-sm;
        color: #fff;
        font-size: $font-size-lg;
        font-weight: 600;
        white-space: nowrap;
      }
    }
  }

  .layout-header {
    background: $background-color-white;
    padding: 0 $spacing-lg;
    display: flex;
    align-items: center;
    justify-content: space-between;
    box-shadow: $box-shadow-sm;

    .header-left {
      display: flex;
      align-items: center;

      .trigger {
        font-size: 18px;
        cursor: pointer;
        transition: color 0.3s;

        &:hover {
          color: $primary-color;
        }
      }

      .breadcrumb {
        margin-left: $spacing-lg;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: $spacing-md;
      
      .shop-selector-wrapper {
        margin-right: $spacing-sm;
      }
      
      .user-info {
        display: flex;
        align-items: center;
        cursor: pointer;
        padding: $spacing-xs $spacing-sm;
        border-radius: $border-radius-md;

        &:hover {
          background: $background-color-light;
        }

        .username {
          margin-left: $spacing-sm;
          font-size: $font-size-md;
        }
      }
    }
  }

  .layout-content {
    margin: $spacing-lg;
    background: $background-color-white;
    border-radius: $border-radius-lg;
    min-height: calc(100vh - #{$header-height} - #{$spacing-lg * 2});
    overflow: auto;
  }
}

// 页面切换动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
