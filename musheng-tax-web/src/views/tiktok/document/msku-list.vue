<template>
  <div class="msku-list-page">
    <div class="page-header">
      <h1 class="page-title">MSKU列表</h1>
      <p class="page-desc">展示结算推导后的MSKU汇总数据，按季度查看</p>
    </div>

    <!-- 筛选条件 -->
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-form layout="inline">
        <a-form-item label="站点">
          <a-select v-model:value="queryParams.siteCode" placeholder="请选择站点" style="width: 140px" @change="handleSearch">
            <a-select-option v-for="m in marketplaceOptions" :key="m.siteCode" :value="m.siteCode">
              {{ m.siteCode }} - {{ m.siteName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="结算季度">
          <a-select v-model:value="queryParams.quarter" placeholder="请选择季度" style="width: 160px" @change="handleSearch">
            <a-select-option v-for="q in availableQuarters" :key="q" :value="q">{{ formatQuarter(q) }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="MSKU">
          <a-input v-model:value="queryParams.msku" placeholder="输入MSKU搜索" allow-clear style="width: 180px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch" :disabled="!queryParams.siteCode || !queryParams.quarter">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 结果 -->
    <a-card :bordered="false" v-if="settlement">
      <template #title>
        <span>{{ settlement.documentNo }} | {{ settlement.periodStart }} ~ {{ settlement.periodEnd }}</span>
        <a-tag color="blue" style="margin-left: 12px">{{ settlement.totalQuantity }}件</a-tag>
        <a-tag color="green" style="margin-left: 4px">${{ fmt(settlement.totalAmount) }}</a-tag>
      </template>
      <a-table :columns="columns" :data-source="filteredItems" :pagination="{ pageSize: 20 }" size="small" bordered row-key="lineNo">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'unitPrice'">{{ Number(record.unitPrice).toFixed(4) }}</template>
          <template v-else-if="column.key === 'amount'">{{ fmt(record.amount) }}</template>
        </template>
        <template #summary>
          <a-table-summary-row>
            <a-table-summary-cell>合计</a-table-summary-cell>
            <a-table-summary-cell />
            <a-table-summary-cell><strong>{{ settlement.totalQuantity }}</strong></a-table-summary-cell>
            <a-table-summary-cell />
            <a-table-summary-cell><strong>{{ fmt(settlement.totalAmount) }}</strong></a-table-summary-cell>
          </a-table-summary-row>
        </template>
      </a-table>
    </a-card>

    <a-empty v-else-if="searched && !loading" description="该季度暂无推导数据，请先执行结算推导" style="margin-top: 60px" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getTiktokDocumentList, getTiktokSettlementDetail } from '@/api/tiktok'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { Marketplace } from '@/types/marketplace'

const marketplaceOptions = ref<Marketplace[]>([])
const queryParams = reactive({ siteCode: '', quarter: '', msku: '' })
const settlement = ref<any>(null)
const items = ref<any[]>([])
const loading = ref(false)
const searched = ref(false)
const availableQuarters = ref<string[]>([])

const columns = [
  { title: '序号', dataIndex: 'lineNo', width: 60 },
  { title: 'MSKU', dataIndex: 'msku', width: 160 },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'right' as const },
  { title: '单价(USD)', key: 'unitPrice', dataIndex: 'unitPrice', width: 120, align: 'right' as const },
  { title: '金额(USD)', key: 'amount', dataIndex: 'amount', width: 130, align: 'right' as const },
]

const filteredItems = computed(() => {
  if (!queryParams.msku) return items.value
  return items.value.filter(i => i.msku?.toLowerCase().includes(queryParams.msku.toLowerCase()))
})

function fmt(val: any) { return val != null ? Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '-' }
function formatQuarter(q: string) { const [y, qn] = q.split('-Q'); return `${y}年Q${qn}` }

function generateAvailableQuarters() {
  const now = new Date()
  let y = now.getFullYear(); let q = Math.ceil((now.getMonth() + 1) / 3)
  const quarters: string[] = []
  for (let i = 0; i <= 12; i++) { quarters.push(`${y}-Q${q}`); q--; if (q < 1) { q = 4; y-- } }
  availableQuarters.value = quarters
}

async function handleSearch() {
  if (!queryParams.siteCode || !queryParams.quarter) return
  loading.value = true
  searched.value = true
  settlement.value = null
  items.value = []

  try {
    // 根据季度算日期范围
    const [year, qn] = queryParams.quarter.split('-Q')
    const startMonth = (parseInt(qn) - 1) * 3 + 1
    const startDate = `${year}-${String(startMonth).padStart(2, '0')}-01`
    const endMonth = parseInt(qn) * 3
    const endDate = `${year}-${String(endMonth).padStart(2, '0')}-${endMonth === 2 ? '28' : (endMonth === 4 || endMonth === 6 || endMonth === 9 || endMonth === 11) ? '30' : '31'}`

    // 查该站点+季度范围的结算单
    const listRes: any = await getTiktokDocumentList({
      documentType: 'SETTLEMENT', siteCode: queryParams.siteCode,
      startDate, endDate, pageNum: 1, pageSize: 1
    })
    const records = (listRes.data || listRes).records || []
    if (records.length === 0) return

    // 取第一份结算单的详情
    const detailRes: any = await getTiktokSettlementDetail(records[0].id)
    const detail = detailRes.data || detailRes
    settlement.value = detail.settlement
    items.value = detail.items || []
  } finally { loading.value = false }
}

function handleReset() {
  queryParams.msku = ''
  settlement.value = null
  items.value = []
  searched.value = false
}

onMounted(async () => {
  const res = await getEnabledMarketplaces()
  marketplaceOptions.value = res.data || []
  generateAvailableQuarters()
})
</script>

<style lang="scss" scoped>
.msku-list-page {
  padding: 24px;
  .page-header { margin-bottom: 24px;
    .page-title { font-size: 20px; font-weight: 600; color: #333; margin: 0 0 8px 0; }
    .page-desc { font-size: 14px; color: #999; margin: 0; }
  }
}
</style>
