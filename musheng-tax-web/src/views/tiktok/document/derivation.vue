<template>
  <div class="settlement-derivation-page">
    <div class="page-header">
      <h1 class="page-title">结算推导</h1>
      <p class="page-desc">选择站点和季度，输入采购成本，自动推导结算明细数据</p>
    </div>

    <!-- 步骤一：选择站点 -->
    <a-card title="① 选择站点" class="step-card">
      <a-select v-model:value="selectedSiteCode" placeholder="请选择站点" :loading="sitesLoading" style="width: 200px" @change="handleSiteChange">
        <a-select-option v-for="m in marketplaceOptions" :key="m.siteCode" :value="m.siteCode">
          {{ m.siteCode }} - {{ m.siteName }}
        </a-select-option>
      </a-select>
    </a-card>

    <!-- 步骤二：选择季度 -->
    <a-card title="② 选择季度" style="margin-top: 16px" class="step-card">
      <a-select v-model:value="selectedQuarter" placeholder="请选择季度" style="width: 200px" @change="handleQuarterChange">
        <a-select-option v-for="q in availableQuarters" :key="q" :value="q">{{ formatQuarter(q) }}</a-select-option>
      </a-select>
    </a-card>

    <!-- 步骤三：输入采购成本 -->
    <a-card title="③ 输入采购成本（原币USD）" style="margin-top: 16px" class="step-card">
      <a-form layout="inline">
        <a-form-item :label="`${selectedSiteCode || '—'} 站点采购成本`">
          <a-input-number v-model:value="costAmount" :min="0" :precision="2" :disabled="!selectedSiteCode" placeholder="请输入采购成本" style="width: 200px" />
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 步骤四：推导计算 -->
    <a-card title="④ 推导计算" style="margin-top: 16px" class="step-card">
      <a-button type="primary" :loading="deriving" :disabled="!canDerive" @click="handleDerive">
        预览推导结果
      </a-button>
      <a-button style="margin-left: 12px" @click="$router.push('/tiktok/document/settlement-generate')">
        去生成结算单/INV
      </a-button>

      <template v-if="derivationResult.length > 0">
        <a-divider />
        <a-alert type="success" :message="resultSummary" show-icon style="margin-bottom: 16px" />
        <a-table :columns="resultColumns" :data-source="derivationResult" :pagination="false" size="small" row-key="msku" bordered />
      </template>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { generateTiktokSettlement, getTiktokSettlementDetail } from '@/api/tiktok'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { Marketplace } from '@/types/marketplace'

const marketplaceOptions = ref<Marketplace[]>([])
const sitesLoading = ref(false)
const selectedSiteCode = ref<string | undefined>(undefined)
const selectedQuarter = ref('')
const costAmount = ref<number | null>(null)
const deriving = ref(false)
const derivationResult = ref<any[]>([])
const resultSummary = ref('')
const availableQuarters = ref<string[]>([])

async function fetchMarketplaces() {
  sitesLoading.value = true
  try {
    const res = await getEnabledMarketplaces()
    marketplaceOptions.value = res.data || []
  } finally {
    sitesLoading.value = false
    if (!selectedSiteCode.value && marketplaceOptions.value.length > 0) {
      selectedSiteCode.value = marketplaceOptions.value.find(m => m.siteCode === 'US')?.siteCode || marketplaceOptions.value[0].siteCode
    }
  }
}

function handleSiteChange() { derivationResult.value = []; resultSummary.value = '' }
function handleQuarterChange() { derivationResult.value = []; resultSummary.value = '' }

function formatQuarter(quarter: string): string {
  const [year, q] = quarter.split('-Q')
  return `${year}年Q${q}`
}

function generateAvailableQuarters() {
  const now = new Date()
  let y = now.getFullYear(); let q = Math.ceil((now.getMonth() + 1) / 3)
  const quarters: string[] = []
  for (let i = 0; i <= 12; i++) {
    quarters.push(`${y}-Q${q}`)
    q--; if (q < 1) { q = 4; y-- }
  }
  availableQuarters.value = quarters
  // 默认上一季度
  const cq = Math.ceil((now.getMonth() + 1) / 3)
  const pq = cq === 1 ? 4 : cq - 1
  const py = cq === 1 ? now.getFullYear() - 1 : now.getFullYear()
  selectedQuarter.value = `${py}-Q${pq}`
}

const canDerive = computed(() => selectedSiteCode.value && selectedQuarter.value && costAmount.value && costAmount.value > 0)

const resultColumns = [
  { title: '月份', dataIndex: 'month', key: 'month' },
  { title: 'MSKU', dataIndex: 'msku', key: 'msku' },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', align: 'right' as const },
  { title: '单价(USD)', dataIndex: 'unitPrice', key: 'unitPrice', align: 'right' as const },
  { title: '金额(USD)', dataIndex: 'amount', key: 'amount', align: 'right' as const }
]

async function handleDerive() {
  if (!selectedSiteCode.value || !selectedQuarter.value) { message.warning('请先选择站点和季度'); return }
  if (!costAmount.value || costAmount.value <= 0) { message.warning('请输入有效的采购成本'); return }

  deriving.value = true
  derivationResult.value = []
  resultSummary.value = ''

  try {
    const res: any = await generateTiktokSettlement({ siteCode: selectedSiteCode.value, quarter: selectedQuarter.value, costAmount: costAmount.value })
    const settlements: any[] = Array.isArray(res.data) ? res.data : [res.data]

    // 汇总所有结算单的明细
    let allItems: any[] = []
    let totalAmount = 0
    for (const settlement of settlements) {
      const detailRes: any = await getTiktokSettlementDetail(settlement.id)
      const detail = detailRes.data || detailRes
      const items = (detail?.items || []).map((item: any) => ({
        msku: item.msku,
        quantity: item.quantity,
        unitPrice: Number(item.unitPrice).toFixed(4),
        amount: Number(item.amount).toFixed(2),
        month: `${settlement.periodStart} ~ ${settlement.periodEnd}`
      }))
      allItems = allItems.concat(items)
      totalAmount += Number(settlement.totalAmount || 0)
    }

    derivationResult.value = allItems
    resultSummary.value = `推导完成，按月生成 ${settlements.length} 份结算单，共 ${allItems.length} 条明细，总金额 $${totalAmount.toLocaleString('en-US', {minimumFractionDigits: 2})}`
    message.success(resultSummary.value)
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '推导失败')
  } finally { deriving.value = false }
}

onMounted(async () => {
  await fetchMarketplaces()
  generateAvailableQuarters()
})
</script>

<style lang="scss" scoped>
.settlement-derivation-page {
  padding: 24px;
  .page-header { margin-bottom: 24px;
    .page-title { font-size: 20px; font-weight: 600; color: #333; margin: 0 0 8px 0; }
    .page-desc { font-size: 14px; color: #999; margin: 0; }
  }
  .step-card { :deep(.ant-card-head-title) { font-weight: 600; } }
}
</style>
