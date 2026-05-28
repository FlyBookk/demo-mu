<template>
  <div class="page-container">
    <h3>导入 TK 结算单</h3>
    <a-card>
      <a-form layout="inline" style="margin-bottom: 16px">
        <a-form-item label="站点" required>
          <a-select v-model:value="siteCode" placeholder="请选择站点" style="width: 160px">
            <a-select-option v-for="s in sites" :key="s.siteCode" :value="s.siteCode">{{ s.siteCode }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>

      <a-alert v-if="!siteCode" message="请先选择站点后再导入" type="warning" show-icon style="margin-bottom: 16px" />

      <a-steps :current="step" style="margin-bottom: 24px">
        <a-step title="上传文件" />
        <a-step title="预校验" />
        <a-step title="导入完成" />
      </a-steps>

      <!-- Step 0: 上传 -->
      <div v-if="step === 0">
        <a-upload-dragger :custom-request="handleValidateUpload" :show-upload-list="false" accept=".xlsx,.xls" :disabled="!siteCode">
          <p class="ant-upload-drag-icon"><inbox-outlined /></p>
          <p class="ant-upload-text">点击或拖拽上传结算单 Excel</p>
          <p class="ant-upload-hint">系统将先校验商品映射完整性</p>
        </a-upload-dragger>
      </div>

      <!-- Step 1: 校验结果 -->
      <div v-if="step === 1">
        <a-result v-if="validateResult?.valid" status="success" title="校验通过，所有SKU已映射">
          <template #extra>
            <a-button type="primary" :loading="importing" @click="doImport">确认导入</a-button>
          </template>
        </a-result>
        <a-result v-else status="warning" :title="`有 ${validateResult?.unmappedSkuIds?.length} 个SKU未映射`" sub-title="可继续导入（未映射SKU的MSKU字段为空），或先完善商品库">
          <template #extra>
            <a-space>
              <a-button @click="$router.push('/tiktok/product/import')">去完善商品库</a-button>
              <a-button type="primary" :loading="importing" @click="doImport">仍然导入</a-button>
            </a-space>
          </template>
          <a-typography-paragraph>
            <pre style="max-height:200px;overflow:auto">{{ validateResult?.unmappedSkuIds?.join('\n') }}</pre>
          </a-typography-paragraph>
        </a-result>
      </div>

      <!-- Step 2: 导入完成 -->
      <div v-if="step === 2">
        <a-result status="success" :title="`导入完成：${importResult?.statements} 个结算单，${importResult?.orders} 条明细`">
          <template #extra>
            <a-button type="primary" @click="$router.push('/tiktok/settlement/orders')">查看订单明细</a-button>
          </template>
        </a-result>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import { validateTiktokSettlement, importTiktokSettlement } from '@/api/tiktok'
import { useTiktokSites } from '@/composables/tiktok/useTiktokSites'
import { message } from 'ant-design-vue'

const { sites } = useTiktokSites()
const siteCode = ref('')
const step = ref(0)
const file = ref<File | null>(null)
const validateResult = ref<{ valid: boolean; unmappedSkuIds?: string[] } | null>(null)
const importResult = ref<{ statements: number; orders: number } | null>(null)
const importing = ref(false)

async function handleValidateUpload(options: any) {
  if (!siteCode.value) { message.error('请先选择站点'); options.onError(); return }
  file.value = options.file
  try {
    const res: any = await validateTiktokSettlement(options.file, siteCode.value)
    validateResult.value = res.data || res
    step.value = 1
    options.onSuccess(validateResult.value)
  } catch (e: any) {
    message.error(e.message || '校验失败')
    options.onError(e)
  }
}

async function doImport() {
  if (!file.value || !siteCode.value) return
  importing.value = true
  try {
    const res: any = await importTiktokSettlement(file.value, siteCode.value)
    importResult.value = res.data || res
    step.value = 2
    message.success('导入成功')
  } catch (e: any) { message.error(e.message || '导入失败') }
  finally { importing.value = false }
}
</script>

<style scoped>
.page-container { padding: 24px; max-width: 700px; }
</style>
