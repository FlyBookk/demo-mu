<template>
  <div class="page-container">
    <div class="page-header">
      <h3>TK 报税汇总</h3>
      <p class="page-desc">按季度查看报税口径和运营口径汇总数据</p>
    </div>

    <!-- 查询条件 -->
    <a-card :bordered="false">
      <a-space>
        <a-select v-model:value="quarter" style="width: 140px" @change="loadData">
          <a-select-option v-for="q in quarterOptions" :key="q" :value="q">{{ q }}</a-select-option>
        </a-select>
        <a-select v-model:value="siteCode" placeholder="选择站点" style="width: 140px" @change="loadData">
          <a-select-option v-for="s in sites" :key="s.siteCode" :value="s.siteCode">{{ s.siteCode }}</a-select-option>
        </a-select>
        <a-input-number v-model:value="exchangeRate" :precision="4" placeholder="汇率（可选）" style="width: 140px" />
        <a-button type="primary" @click="loadData" :loading="loading" :disabled="!siteCode">查询</a-button>
      </a-space>
    </a-card>

    <!-- 报税口径 -->
    <a-card :bordered="false" style="margin-top: 16px" v-if="taxSummary">
      <template #title>
        <div class="card-title-row">
          <span>报税口径（{{ taxSummary.quarter }}）</span>
          <span class="rate-badge">执行汇率：{{ taxSummary.exchangeRate }}</span>
        </div>
      </template>

      <a-table :columns="taxColumns" :data-source="taxMonthData" :pagination="false" size="middle" bordered>
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'month'">
            <strong>{{ record.month }}</strong>
          </template>
          <template v-if="column.dataIndex === 'revenue'">
            <div class="cell-dual">
              <span class="val-usd">{{ fmt(record.revenueUsd) }}</span>
              <span class="val-rmb">¥{{ fmt(record.revenueRmb) }}</span>
            </div>
          </template>
          <template v-if="column.dataIndex === 'refund'">
            <div class="cell-dual">
              <span class="val-usd">{{ fmt(record.refundUsd) }}</span>
              <span class="val-rmb">¥{{ fmt(record.refundRmb) }}</span>
            </div>
          </template>
          <template v-if="column.dataIndex === 'serviceFee'">
            <div class="cell-dual">
              <span class="val-usd">{{ fmt(record.serviceFeeUsd) }}</span>
              <span class="val-rmb">¥{{ fmt(record.serviceFeeRmb) }}</span>
            </div>
          </template>
        </template>
        <template #summary>
          <a-table-summary-row class="summary-row">
            <a-table-summary-cell>合计</a-table-summary-cell>
            <a-table-summary-cell>
              <div class="cell-dual"><span class="val-usd">{{ fmt(taxSummary.totalRevenueUsd) }}</span><span class="val-rmb">¥{{ fmt(taxSummary.totalRevenueRmb) }}</span></div>
            </a-table-summary-cell>
            <a-table-summary-cell>
              <div class="cell-dual"><span class="val-usd">{{ fmt(taxSummary.totalRefundUsd) }}</span><span class="val-rmb">¥{{ fmt(taxSummary.totalRefundRmb) }}</span></div>
            </a-table-summary-cell>
            <a-table-summary-cell>
              <div class="cell-dual"><span class="val-usd">{{ fmt(taxSummary.totalServiceFeeUsd) }}</span><span class="val-rmb">¥{{ fmt(taxSummary.totalServiceFeeRmb) }}</span></div>
            </a-table-summary-cell>
            <a-table-summary-cell>{{ totalOrders }}</a-table-summary-cell>
          </a-table-summary-row>
        </template>
      </a-table>
    </a-card>

    <!-- 运营口径 -->
    <a-card title="运营口径" :bordered="false" style="margin-top: 16px" v-if="opSummary">
      <a-descriptions :column="4" bordered size="small">
        <a-descriptions-item label="净收入">{{ fmt(opSummary.netRevenue) }}</a-descriptions-item>
        <a-descriptions-item label="佣金">{{ fmt(opSummary.commission) }}</a-descriptions-item>
        <a-descriptions-item label="物流">{{ fmt(opSummary.logistics) }}</a-descriptions-item>
        <a-descriptions-item label="联盟">{{ fmt(opSummary.affiliate) }}</a-descriptions-item>
        <a-descriptions-item label="促销">{{ fmt(opSummary.promotion) }}</a-descriptions-item>
        <a-descriptions-item label="税费">{{ fmt(opSummary.tax) }}</a-descriptions-item>
        <a-descriptions-item label="其他">{{ fmt(opSummary.other) }}</a-descriptions-item>
        <a-descriptions-item label="订单利润"><strong>{{ fmt(opSummary.orderProfit) }}</strong></a-descriptions-item>
        <a-descriptions-item label="调整收入">{{ fmt(opSummary.adjustmentIncome) }}</a-descriptions-item>
        <a-descriptions-item label="调整支出">{{ fmt(opSummary.adjustmentExpense) }}</a-descriptions-item>
        <a-descriptions-item label="净利润"><strong style="color:#52c41a">{{ fmt(opSummary.netProfit) }}</strong></a-descriptions-item>
        <a-descriptions-item label="利润率"><strong>{{ ((opSummary.marginRate || 0) * 100).toFixed(2) }}%</strong></a-descriptions-item>
      </a-descriptions>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { getTiktokTaxSummary, getTiktokOperationSummary, type QuarterTaxSummary, type QuarterOperationSummary } from '@/api/tiktok'
import { useTiktokSites } from '@/composables/tiktok/useTiktokSites'

const { sites, currentSite } = useTiktokSites()

const quarter = ref('')
const siteCode = ref('')
const exchangeRate = ref<number | undefined>(undefined)
const loading = ref(false)
const taxSummary = ref<QuarterTaxSummary | null>(null)
const opSummary = ref<QuarterOperationSummary | null>(null)

watch(currentSite, (val) => { if (val && !siteCode.value) { siteCode.value = val; loadData() } })

// 生成季度选项（最近8个季度），默认上一季度
const quarterOptions = (() => {
  const opts: string[] = []
  const now = new Date()
  let y = now.getFullYear()
  let q = Math.ceil((now.getMonth() + 1) / 3)
  for (let i = 0; i < 8; i++) {
    opts.push(`${y}-Q${q}`)
    q--
    if (q === 0) { q = 4; y-- }
  }
  return opts
})()

// 默认选上一季度（index=1）
const defaultQuarter = quarterOptions.length > 1 ? quarterOptions[1] : quarterOptions[0]

const taxColumns = [
  { title: '月份', dataIndex: 'month', width: 100 },
  { title: '收入（USD / RMB）', dataIndex: 'revenue', width: 180 },
  { title: '退款（USD / RMB）', dataIndex: 'refund', width: 180 },
  { title: '服务费（USD / RMB）', dataIndex: 'serviceFee', width: 180 },
  { title: '订单数', dataIndex: 'orderCount', width: 90 },
]

const taxMonthData = computed(() => taxSummary.value?.months || [])
const totalOrders = computed(() => taxMonthData.value.reduce((s, m) => s + m.orderCount, 0))

function fmt(val: number | undefined | null): string {
  if (val == null) return '-'
  return val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadData() {
  if (!quarter.value || !siteCode.value) return
  loading.value = true
  try {
    const params: any = { quarter: quarter.value, siteCode: siteCode.value }
    if (exchangeRate.value) params.exchangeRate = exchangeRate.value

    const [taxRes, opRes]: any[] = await Promise.all([
      getTiktokTaxSummary(params),
      getTiktokOperationSummary(params),
    ])
    taxSummary.value = taxRes.data || taxRes
    opSummary.value = opRes.data || opRes
  } finally { loading.value = false }
}

onMounted(() => {
  quarter.value = defaultQuarter
})
</script>

<style scoped>
.page-container { padding: 24px; }
.page-header { margin-bottom: 16px; }
.page-desc { color: #666; font-size: 13px; margin-top: 4px; }

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.rate-badge {
  font-size: 13px;
  font-weight: normal;
  color: #1890ff;
  background: #e6f7ff;
  padding: 2px 10px;
  border-radius: 4px;
}

.cell-dual {
  display: flex;
  flex-direction: column;
  line-height: 1.6;
}
.val-usd {
  font-weight: 500;
  color: #333;
}
.val-rmb {
  font-size: 12px;
  color: #999;
}

.summary-row {
  font-weight: 600;
  background: #fafafa;
}
</style>
