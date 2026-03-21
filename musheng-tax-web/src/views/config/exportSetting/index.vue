<template>
  <div class="page-container">
    <a-card title="导出设置">
      <a-spin :spinning="loading">
        <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 12 }">
          <a-form-item label="导出签章与Logo">
            <a-switch
              v-model:checked="stampEnabled"
              checked-children="开启"
              un-checked-children="关闭"
              @change="handleStampChange"
            />
            <div class="setting-desc">
              关闭后，导出的结算单、INV、送货单、PO将不包含签章和公司Logo
            </div>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getConfigValue, updateConfigValue } from '@/api/sysConfig'

const loading = ref(false)
const stampEnabled = ref(true)

onMounted(async () => {
  loading.value = true
  try {
    const res = await getConfigValue('export_stamp_enabled')
    stampEnabled.value = res.data === 'true'
  } finally {
    loading.value = false
  }
})

async function handleStampChange(checked: boolean) {
  try {
    await updateConfigValue('export_stamp_enabled', String(checked))
    message.success('设置已保存')
  } catch {
    stampEnabled.value = !checked
    message.error('保存失败')
  }
}
</script>

<style scoped>
.page-container {
  padding: 24px;
}
.setting-desc {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}
</style>
