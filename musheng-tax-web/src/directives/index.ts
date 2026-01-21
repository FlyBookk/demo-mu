import type { App, Directive } from 'vue'
import { useAuthStore } from '@/stores/modules/auth'

/**
 * 权限指令
 * 用法: v-permission="'config:currency'"
 */
const permissionDirective: Directive = {
  mounted(el: HTMLElement, binding) {
    const { value } = binding
    const authStore = useAuthStore()

    if (value && !authStore.hasPermission(value)) {
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * 加载指令
 * 用法: v-loading="isLoading"
 */
const loadingDirective: Directive = {
  mounted(el: HTMLElement, binding) {
    const { value } = binding
    el.style.position = 'relative'

    if (value) {
      addLoadingMask(el)
    }
  },
  updated(el: HTMLElement, binding) {
    const { value, oldValue } = binding

    if (value !== oldValue) {
      if (value) {
        addLoadingMask(el)
      } else {
        removeLoadingMask(el)
      }
    }
  }
}

function addLoadingMask(el: HTMLElement) {
  const existingMask = el.querySelector('.v-loading-mask')
  if (existingMask) return

  const mask = document.createElement('div')
  mask.className = 'v-loading-mask'
  mask.innerHTML = `
    <div class="v-loading-spinner">
      <span class="ant-spin-dot ant-spin-dot-spin">
        <i class="ant-spin-dot-item"></i>
        <i class="ant-spin-dot-item"></i>
        <i class="ant-spin-dot-item"></i>
        <i class="ant-spin-dot-item"></i>
      </span>
    </div>
  `
  mask.style.cssText = `
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(255, 255, 255, 0.6);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 100;
  `
  el.appendChild(mask)
}

function removeLoadingMask(el: HTMLElement) {
  const mask = el.querySelector('.v-loading-mask')
  if (mask) {
    el.removeChild(mask)
  }
}

/**
 * 注册所有自定义指令
 */
export function setupDirectives(app: App) {
  app.directive('permission', permissionDirective)
  app.directive('loading', loadingDirective)
}

export { permissionDirective, loadingDirective }
