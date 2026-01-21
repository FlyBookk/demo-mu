/**
 * 路由守卫
 */

import type { Router } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/modules/auth'
import { useAppStore } from '@/stores/modules/app'

/**
 * 设置路由守卫
 */
export function setupRouterGuards(router: Router) {
  // 前置守卫
  router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore()
    const appStore = useAppStore()

    // 开始页面loading
    appStore.setPageLoading(true)

    // 设置页面标题
    const title = to.meta.title ? `${to.meta.title} - 慕声报税系统` : '慕声报税系统'
    document.title = title

    // 不需要认证的页面直接通过
    if (to.meta.requiresAuth === false) {
      // 如果已登录且访问登录页，跳转到首页
      if (to.path === '/login' && authStore.isLoggedIn) {
        next({ path: '/' })
        return
      }
      next()
      return
    }

    // 未登录，跳转登录页
    if (!authStore.isLoggedIn) {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }

    // 权限检查
    const permission = to.meta.permission as string | undefined
    if (permission && !authStore.hasPermission(permission)) {
      message.error('没有访问权限')
      next({ path: '/dashboard' })
      return
    }

    // 角色检查
    const roles = to.meta.roles as string[] | undefined
    if (roles && roles.length > 0 && !authStore.hasAnyRole(roles)) {
      message.error('没有访问权限')
      next({ path: '/dashboard' })
      return
    }

    next()
  })

  // 后置守卫
  router.afterEach((to) => {
    const appStore = useAppStore()

    // 结束页面loading
    appStore.setPageLoading(false)

    // 滚动到顶部
    window.scrollTo(0, 0)

    // 更新激活的菜单
    const matched = to.matched
    if (matched.length > 1) {
      // 设置一级菜单展开
      const firstLevel = matched[1]?.name as string
      if (firstLevel) {
        appStore.setOpenMenuKeys([firstLevel])
      }
    }
    appStore.setActiveMenu(to.name as string)
  })

  // 错误处理
  router.onError((error) => {
    console.error('路由错误:', error)
    message.error('页面加载失败，请刷新重试')
  })
}
