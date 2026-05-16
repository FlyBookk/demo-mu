<template>
  <a-select
    v-model:value="currentPlatform"
    style="width: 140px"
    @change="handleChange"
  >
    <a-select-option value="AMAZON">
      <span>🛒 Amazon</span>
    </a-select-option>
    <a-select-option value="TIKTOK">
      <span>🎵 TikTok</span>
    </a-select-option>
  </a-select>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const currentPlatform = ref<string>(localStorage.getItem('platform') || 'AMAZON')

onMounted(() => {
  if (!localStorage.getItem('platform')) {
    localStorage.setItem('platform', 'AMAZON')
  }
})

function handleChange(value: string) {
  localStorage.setItem('platform', value)
  // 切换平台后跳转到首页
  router.push('/dashboard')
  // 触发菜单刷新
  window.dispatchEvent(new CustomEvent('platform-change', { detail: value }))
}
</script>
