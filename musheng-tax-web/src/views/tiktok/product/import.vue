<template>
  <div class="page-container">
    <h3>导入 SKU 对照表</h3>
    <a-card>
      <a-form layout="inline" style="margin-bottom: 16px">
        <a-form-item label="站点" required>
          <a-select v-model:value="siteCode" placeholder="请选择站点" style="width: 160px">
            <a-select-option v-for="s in sites" :key="s.siteCode" :value="s.siteCode">{{ s.siteCode }} - {{ s.siteName }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
      <a-upload-dragger :custom-request="handleUpload" :show-upload-list="false" accept=".xlsx,.xls" :disabled="!siteCode">
        <p class="ant-upload-drag-icon"><inbox-outlined /></p>
        <p class="ant-upload-text">点击或拖拽上传 SKU 对照表</p>
        <p class="ant-upload-hint">支持 TK 卖家中心导出的商品管理模板（.xlsx）</p>
      </a-upload-dragger>
      <a-alert v-if="!siteCode" message="请先选择站点后再导入" type="warning" show-icon style="margin-top: 12px" />
      <a-spin :spinning="loading" style="margin-top: 16px">
        <a-result v-if="result" status="success" :title="`导入完成：新增 ${result.inserted} 条，更新 ${result.updated} 条`">
          <template #extra>
            <a-button type="primary" @click="$router.push('/tiktok/product/list')">查看商品列表</a-button>
          </template>
        </a-result>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import { importTiktokProduct } from '@/api/tiktok'
import { useTiktokSites } from '@/composables/tiktok/useTiktokSites'
import { message } from 'ant-design-vue'

const { sites } = useTiktokSites()
const siteCode = ref('')
const result = ref<{ inserted: number; updated: number } | null>(null)
const loading = ref(false)

async function handleUpload(options: any) {
  if (!siteCode.value) { message.error('请先选择站点'); options.onError(); return }
  const { file } = options
  loading.value = true
  try {
    const res: any = await importTiktokProduct(file, siteCode.value)
    result.value = res.data || res
    message.success('导入成功')
    options.onSuccess(result.value)
  } catch (e: any) {
    message.error(e.message || '导入失败')
    options.onError(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-container { padding: 24px; max-width: 600px; }
</style>
