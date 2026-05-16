<template>
  <div class="settlement-generate-page">
    <div class="page-header">
      <h1 class="page-title">结算单/INV 生成</h1>
      <p class="page-desc">基于结算推导的 MSKU 数据生成结算单和 INV 发票</p>
    </div>

    <a-card class="section-card">
      <template #title>生成条件</template>
      <a-alert type="info" show-icon style="margin-bottom: 16px">
        <template #message>
          请确保已在「结算推导」中完成对应季度的推导，系统将基于推导数据生成结算单并同步生成 INV。
        </template>
      </a-alert>

      <a-form layout="vertical" style="max-width: 500px">
        <a-form-item label="站点" required>
          <a-select v-model:value="selectedSiteCode" placeholder="请选择站点" style="width: 100%" :loading="sitesLoading">
            <a-select-option v-for="m in marketplaceOptions" :key="m.siteCode" :value="m.siteCode">
              {{ m.siteCode }} - {{ m.siteName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="结算季度" required>
          <a-select v-model:value="selectedQuarter" placeholder="请选择季度" style="width: 100%" @change="checkExisting">
            <a-select-option v-for="q in availableQuarters" :key="q" :value="q">{{ formatQuarter(q) }}</a-select-option>
          </a-select>
        </a-form-item>

        <!-- 已有推导结果提示 -->
        <a-alert v-if="existingSettlement" type="success" show-icon style="margin-bottom: 16px">
          <template #message>
            该季度已有结算单：{{ existingSettlement.documentNo }} | {{ existingSettlement.totalQuantity }}件 | ${{ fmt(existingSettlement.totalAmount) }}
          </template>
        </a-alert>
        <a-alert v-else-if="checked && !existingSettlement" type="warning" show-icon style="margin-bottom: 16px">
          <template #message>该季度暂无推导数据，请先到「结算推导」页面执行推导</template>
        </a-alert>
      </a-form>

      <div style="margin: 16px 0">
        <a-space v-if="existingSettlement">
          <a-button type="primary" @click="handleExport">导出结算单</a-button>
          <a-button @click="$router.push('/tiktok/document/list')">查看单据列表</a-button>
        </a-space>
        <a-button v-else-if="checked" type="primary" @click="$router.push('/tiktok/document/derivation')">
          去结算推导（生成结算单+INV）
        </a-button>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getTiktokDocumentList, exportTiktokSettlement, generateTiktokSettlement } from '@/api/tiktok'
import { getEnabledMarketplaces } from '@/api/marketplace'
import { message } from 'ant-design-vue'
import type { Marketplace } from '@/types/marketplace'

const marketplaceOptions = ref<Marketplace[]>([])
const sitesLoading = ref(false)
const selectedSiteCode = ref<string | undefined>(undefined)
const selectedQuarter = ref('')
const existingSettlement = ref<any>(null)
const checked = ref(false)
const generating = ref(false)
const generateResult = ref<any>(null)
const availableQuarters = ref<string[]>([])

const canGenerate = computed(() => selectedSiteCode.value && selectedQuarter.value && !existingSettlement.value)

async function fetchMarketplaces() {
  sitesLoading.value = true
  try { const res = await getEnabledMarketplaces(); marketplaceOptions.value = res.data || [] }
  finally { sitesLoading.value = false }
}

function formatQuarter(q: string) { const [y, qn] = q.split('-Q'); return `${y}年Q${qn}` }

function generateAvailableQuarters() {
  const now = new Date()
  let y = now.getFullYear(); let q = Math.ceil((now.getMonth() + 1) / 3)
  const quarters: string[] = []
  for (let i = 0; i <= 12; i++) { quarters.push(`${y}-Q${q}`); q--; if (q < 1) { q = 4; y-- } }
  availableQuarters.value = quarters
  const cq = Math.ceil((now.getMonth() + 1) / 3)
  selectedQuarter.value = `${cq === 1 ? now.getFullYear() - 1 : now.getFullYear()}-Q${cq === 1 ? 4 : cq - 1}`
}

async function checkExisting() {
  existingSettlement.value = null
  checked.value = false
  if (!selectedSiteCode.value || !selectedQuarter.value) return

  const [year, qn] = selectedQuarter.value.split('-Q')
  const startMonth = (parseInt(qn) - 1) * 3 + 1
  const endMonth = parseInt(qn) * 3
  const startDate = `${year}-${String(startMonth).padStart(2, '0')}-01`
  const endDate = `${year}-${String(endMonth).padStart(2, '0')}-${[4,6,9,11].includes(endMonth) ? '30' : endMonth === 2 ? '28' : '31'}`

  const res: any = await getTiktokDocumentList({ documentType: 'SETTLEMENT', siteCode: selectedSiteCode.value, startDate, endDate, pageNum: 1, pageSize: 1 })
  const records = (res.data || res).records || []
  existingSettlement.value = records.length > 0 ? records[0] : null
  checked.value = true
}

async function handleExport() {
  if (existingSettlement.value) await exportTiktokSettlement(existingSettlement.value.id)
}

async function handleGenerate() {
  if (!selectedSiteCode.value || !selectedQuarter.value) return
  generating.value = true
  try {
    const res: any = await generateTiktokSettlement({ siteCode: selectedSiteCode.value, quarter: selectedQuarter.value, costAmount: 0 })
    generateResult.value = res.data || res
    message.success('生成成功')
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '生成失败，请先执行结算推导')
  } finally { generating.value = false }
}

function fmt(val: any) { return val != null ? Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '-' }

onMounted(async () => { await fetchMarketplaces(); generateAvailableQuarters() })
</script>

<style lang="scss" scoped>
.settlement-generate-page {
  padding: 24px;
  .page-header { margin-bottom: 24px;
    .page-title { font-size: 20px; font-weight: 600; color: #333; margin: 0 0 8px 0; }
    .page-desc { font-size: 14px; color: #999; margin: 0; }
  }
}
</style>
