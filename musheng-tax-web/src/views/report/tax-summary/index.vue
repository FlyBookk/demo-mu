<template>
  <div class="tax-summary-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">报税汇总</h1>
      <p class="page-desc">对齐Amazon报税汇总口径，统计收入、退款、佣金服务费等核心字段</p>
    </div>

    <!-- 筛选条件 -->
    <a-card class="filter-card">
      <a-form layout="inline">
        <a-form-item label="站点">
          <a-select
            v-model:value="filterForm.siteCode"
            placeholder="全部站点"
            allow-clear
            style="width: 150px"
          >
            <a-select-option
              v-for="marketplace in marketplaceOptions"
              :key="marketplace.siteCode"
              :value="marketplace.siteCode"
            >
              {{ marketplace.siteCode }} - {{ marketplace.siteName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="季度">
          <a-select
            v-model:value="filterForm.selectedQuarter"
            placeholder="请选择季度"
            style="width: 150px"
            @change="handleQuarterChange"
          >
            <a-select-option v-for="q in availableQuarters" :key="q" :value="q">
              {{ formatQuarter(q) }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="退款时间维度">
          <a-select
            v-model:value="filterForm.refundDateMode"
            style="width: 140px"
          >
            <a-select-option value="ship">配送日期</a-select-option>
            <a-select-option value="settlement">结算日期</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleQuery" :loading="loading">
              <SearchOutlined /> 查询
            </a-button>
            <a-button @click="handleExport" :loading="exporting">
              <DownloadOutlined /> 导出Excel
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 汇总统计卡片 - Amazon报税口径 -->
    <div class="stat-section">
      <div class="stat-section-label">Amazon报税汇总</div>
      <a-row :gutter="16" class="stat-row">
        <a-col :span="6">
          <a-card class="stat-card">
            <a-statistic
              title="收入总额①(a)"
              :value="Math.abs(totalStats.totalRevenueCny ?? 0)"
              :precision="2"
              :prefix="(totalStats.totalRevenueCny ?? 0) >= 0 ? '¥' : '-¥'"
              :value-style="{ color: (totalStats.totalRevenueCny ?? 0) >= 0 ? '#52c41a' : '#ff4d4f' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card">
            <a-statistic
              title="退款金额②(b)"
              :value="Math.abs(totalStats.refundAmountCny ?? 0)"
              :precision="2"
              :prefix="(totalStats.refundAmountCny ?? 0) >= 0 ? '¥' : '-¥'"
              :value-style="{ color: (totalStats.refundAmountCny ?? 0) >= 0 ? '#52c41a' : '#ff4d4f' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card">
            <a-statistic
              title="收入净额③(c=a-b)"
              :value="Math.abs(totalStats.netRevenueCny ?? 0)"
              :precision="2"
              :prefix="(totalStats.netRevenueCny ?? 0) >= 0 ? '¥' : '-¥'"
              :value-style="{ color: (totalStats.netRevenueCny ?? 0) >= 0 ? '#52c41a' : '#ff4d4f' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card">
            <a-statistic
              title="佣金服务费⑤"
              :value="Math.abs(totalStats.totalCommissionFeeCny ?? 0)"
              :precision="2"
              :prefix="(totalStats.totalCommissionFeeCny ?? 0) >= 0 ? '¥' : '-¥'"
              :value-style="{ color: (totalStats.totalCommissionFeeCny ?? 0) >= 0 ? '#52c41a' : '#ff4d4f' }"
            />
          </a-card>
        </a-col>
      </a-row>
    </div>
    <!-- 辅助字段 -->
    <div class="stat-section">
      <a-row :gutter="16" class="stat-row">
        <a-col :span="6">
          <a-card class="stat-card">
            <a-statistic
              title="其他费用⑦"
              :value="Math.abs(totalStats.otherFeesTotalCny ?? 0)"
              :precision="2"
              :prefix="(totalStats.otherFeesTotalCny ?? 0) >= 0 ? '¥' : '-¥'"
              :value-style="{ color: (totalStats.otherFeesTotalCny ?? 0) >= 0 ? '#52c41a' : '#ff4d4f' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card">
            <a-statistic
              title="平台代扣税④"
              :value="Math.abs(totalStats.consumptionTaxCny ?? 0)"
              :precision="2"
              :prefix="(totalStats.consumptionTaxCny ?? 0) >= 0 ? '¥' : '-¥'"
              :value-style="{ color: (totalStats.consumptionTaxCny ?? 0) >= 0 ? '#52c41a' : '#722ed1' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card">
            <a-statistic
              title="广告费⑥"
              :value="Math.abs(totalStats.advertisingCostCny ?? 0)"
              :precision="2"
              :prefix="(totalStats.advertisingCostCny ?? 0) >= 0 ? '¥' : '-¥'"
              :value-style="{ color: (totalStats.advertisingCostCny ?? 0) >= 0 ? '#52c41a' : '#13c2c2' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card">
            <a-statistic
              title="配送匹配订单数"
              :value="totalStats.shippingMatchCount"
              suffix="笔"
              :value-style="{ color: '#1890ff' }"
            />
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 平台支出与采购成本（按图片公式） -->
    <div class="stat-section">
      <div class="stat-section-label">
        平台支出与采购成本
        <span style="font-size:13px; font-weight:normal; color:#666; margin-left:16px; display:inline-flex; align-items:center">
          利润设置：
          <a-radio-group v-model:value="profitMode" size="small" style="margin: 0 8px">
            <a-radio-button value="percent">百分比</a-radio-button>
            <a-radio-button value="fixed">固定金额</a-radio-button>
          </a-radio-group>
          <a-input-number v-if="profitMode === 'percent'" v-model:value="profitPercent" :min="0" :max="100" :precision="1" size="small" style="width: 80px" />
          <span v-if="profitMode === 'percent'" style="margin-left: 4px">%</span>
          <span v-if="profitMode === 'fixed'" style="margin-right: 4px">¥</span>
          <a-input-number v-if="profitMode === 'fixed'" v-model:value="profitFixed" :min="0" :precision="2" size="small" style="width: 120px" />
        </span>
      </div>
      <a-row :gutter="16" class="stat-row">
        <a-col :span="6">
          <a-card class="stat-card highlight-card">
            <a-statistic
              title="平台支出合计⑨=④+⑤+⑥+⑦"
              :value="Math.abs(totalStats.platformExpensesCny ?? 0)"
              :precision="2"
              :prefix="'¥'"
              :value-style="{ color: '#fa8c16', fontWeight: 'bold' }"
            />
            <div class="stat-desc">消费税+佣金服务费+广告费+其他费用</div>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card highlight-card">
            <a-statistic
              :title="profitMode === 'percent' ? `${profitPercent}%利润⑩=③×${profitPercent}%` : '固定利润⑩'"
              :value="Math.abs(totalStats.profit4PercentCny ?? 0)"
              :precision="2"
              :prefix="'¥'"
              :value-style="{ color: '#52c41a', fontWeight: 'bold' }"
            />
            <div class="stat-desc">{{ profitMode === 'percent' ? `收入净额的${profitPercent}%` : '自定义固定金额' }}</div>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card highlight-card">
            <a-statistic
              title="采购成本⑪=③−⑨−⑩"
              :value="Math.abs(totalStats.procurementCostCny ?? 0)"
              :precision="2"
              :prefix="(totalStats.procurementCostCny ?? 0) >= 0 ? '¥' : '-¥'"
              :value-style="{ color: '#1890ff', fontWeight: 'bold' }"
            />
            <div class="stat-desc">收入净额−平台支出−4%利润</div>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 汇总表格 -->
    <a-card title="报税汇总数据" class="data-card">
      <a-table
        :columns="summaryColumns"
        :data-source="summaryData"
        :loading="loading"
        :pagination="false"
        :scroll="{ x: 1300 }"
        row-key="key"
        bordered
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'siteCode'">
            <a-tag color="blue">{{ record.siteCode }}</a-tag>
            <span style="margin-left: 8px">{{ record.siteName }}</span>
          </template>
          <template v-else-if="column.key === 'totalRevenueCny'">
            <span :class="['amount', (record.totalRevenueCny ?? 0) >= 0 ? 'positive' : 'negative']">{{ formatAmountWithSign(record.totalRevenueCny) }}</span>
          </template>
          <template v-else-if="column.key === 'refundAmountCny'">
            <span :class="['amount', (record.refundAmountCny ?? 0) >= 0 ? 'positive' : 'negative']">{{ formatAmountWithSign(record.refundAmountCny) }}</span>
            <a-tag size="small" style="margin-left: 4px">{{ record.refundCount }}笔</a-tag>
          </template>
          <template v-else-if="column.key === 'netRevenueCny'">
            <span :class="['amount', ((record.totalRevenueCny ?? 0) - Math.abs(record.refundAmountCny ?? 0)) >= 0 ? 'positive' : 'negative']">
              {{ formatAmountWithSign((record.totalRevenueCny ?? 0) - Math.abs(record.refundAmountCny ?? 0)) }}
            </span>
          </template>
          <template v-else-if="column.key === 'totalCommissionFeeCny'">
            <span :class="['amount', (record.totalCommissionFeeCny ?? 0) >= 0 ? 'positive' : 'negative']">{{ formatAmountWithSign(record.totalCommissionFeeCny) }}</span>
          </template>
          <template v-else-if="column.key === 'totalOtherFeeCny'">
            <span :class="['amount', (record.totalOtherFeeCny ?? 0) >= 0 ? 'positive' : 'negative']">{{ formatAmountWithSign(record.totalOtherFeeCny) }}</span>
          </template>
          <template v-else-if="column.key === 'consumptionTaxCny'">
            <span :class="['amount', (record.consumptionTaxCny ?? 0) >= 0 ? 'positive' : 'negative']">{{ formatAmountWithSign(record.consumptionTaxCny) }}</span>
          </template>
          <template v-else-if="column.key === 'advertisingCostCny'">
            <span :class="['amount', (record.advertisingCostCny ?? 0) >= 0 ? 'positive' : 'negative']">{{ formatAmountWithSign(record.advertisingCostCny) }}</span>
          </template>
          <template v-else-if="column.key === 'platformExpensesCny'">
            <span class="amount highlight-amount">
              {{ formatAmountWithSign((record.platformExpensesCny ?? 0)) }}
            </span>
          </template>
          <template v-else-if="column.key === 'profit4PercentCny'">
            <span class="amount highlight-amount">
              {{ formatAmountWithSign((record.profit4PercentCny ?? 0)) }}
            </span>
          </template>
          <template v-else-if="column.key === 'procurementCostCny'">
            <span class="amount highlight-amount">
              {{ formatAmountWithSign((record.procurementCostCny ?? 0)) }}
            </span>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 费用明细表格 -->
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import {
  getTaxSummary,
  exportTaxSummary,
  type TaxReportSummary
} from '@/api/report'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { Marketplace } from '@/types/marketplace'

// ============= 筛选条件 =============
const filterForm = reactive({
  siteCode: undefined as string | undefined,
  selectedQuarter: '',
  refundDateMode: 'settlement' as string
})

const marketplaceOptions = ref<Marketplace[]>([])
const availableQuarters = ref<string[]>([])

// ============= 数据 =============
const loading = ref(false)
const exporting = ref(false)
const profitMode = ref<'percent' | 'fixed'>('percent')
const profitPercent = ref<number>(4)
const profitFixed = ref<number>(0)

const summaryData = ref<(TaxReportSummary & { key: string })[]>([])

// 汇总统计
const totalStats = computed(() => {
  const data = summaryData.value
  const totalRevenueCny = data.reduce((sum, item) => sum + (item.totalRevenueCny || 0), 0)
  const refundAmountCny = data.reduce((sum, item) => sum + (item.refundAmountCny || 0), 0)
  const totalOtherFeeCny = data.reduce((sum, item) => sum + (item.totalOtherFeeCny || 0), 0)
  const shippingMatchCount = data.reduce((sum, item) => sum + (item.shippingMatchCount || 0), 0)
  const netRevenueCny = totalRevenueCny - Math.abs(refundAmountCny)
  const consumptionTaxCny = data.reduce((sum, item) => sum + (item.consumptionTaxCny || 0), 0)
  const totalCommissionFeeCny = data.reduce((sum, item) => sum + (item.totalCommissionFeeCny || 0), 0)
  const advertisingCostCny = data.reduce((sum, item) => sum + (item.advertisingCostCny || 0), 0)

  // 按公式计算
  // ⑨平台支出合计 = ④消费税 + ⑤佣金服务费 + ⑥广告费 + ⑦其他费用
  const platformExpensesCny = Math.abs(consumptionTaxCny) + Math.abs(totalCommissionFeeCny) + Math.abs(advertisingCostCny) + Math.abs(totalOtherFeeCny)
  // ⑩利润 = 百分比或固定金额
  const profit4PercentCny = profitMode.value === 'percent' ? netRevenueCny * (profitPercent.value || 0) / 100 : (profitFixed.value || 0)
  // ⑪采购成本 = ③ − ⑨ − ⑩
  const procurementCostCny = netRevenueCny - platformExpensesCny - profit4PercentCny

  return {
    totalRevenueCny,
    refundAmountCny,
    netRevenueCny,
    consumptionTaxCny,
    totalCommissionFeeCny,
    totalOtherFeeCny,
    otherFeesTotalCny: totalOtherFeeCny,
    advertisingCostCny,
    platformExpensesCny,
    profit4PercentCny,
    procurementCostCny,
    shippingMatchCount
  }
})
const summaryColumns = [
  { title: '站点', dataIndex: 'siteCode', key: 'siteCode', width: 150, fixed: 'left' },
  { title: '季度', dataIndex: 'yearQuarter', key: 'yearQuarter', width: 100 },
  { title: '收入总额①', dataIndex: 'totalRevenueCny', key: 'totalRevenueCny', width: 140, align: 'right' },
  { title: '退款金额②', dataIndex: 'refundAmountCny', key: 'refundAmountCny', width: 160, align: 'right' },
  { title: '配送匹配订单数', dataIndex: 'shippingMatchCount', key: 'shippingMatchCount', width: 130, align: 'right' },
  { title: '收入净额③=①-②', key: 'netRevenueCny', width: 160, align: 'right' },
  { title: '平台代扣税④', dataIndex: 'consumptionTaxCny', key: 'consumptionTaxCny', width: 130, align: 'right' },
  { title: '佣金服务费⑤', dataIndex: 'totalCommissionFeeCny', key: 'totalCommissionFeeCny', width: 150, align: 'right' },
  { title: '其他费用⑦', dataIndex: 'totalOtherFeeCny', key: 'totalOtherFeeCny', width: 130, align: 'right' },
  { title: '广告费⑥', dataIndex: 'advertisingCostCny', key: 'advertisingCostCny', width: 130, align: 'right' },
  { title: '平台支出合计⑨=④+⑤+⑥+⑦', dataIndex: 'platformExpensesCny', key: 'platformExpensesCny', width: 200, align: 'right' },
  { title: '4%利润⑩=③×4%', dataIndex: 'profit4PercentCny', key: 'profit4PercentCny', width: 150, align: 'right' },
  { title: '采购成本⑪=③-⑨-⑩', dataIndex: 'procurementCostCny', key: 'procurementCostCny', width: 170, align: 'right' }
]

// ============= 方法 =============
function formatQuarter(quarter: string): string {
  const [year, q] = quarter.split('-Q')
  return `${year}年Q${q}`
}

function formatNumber(value: number): string {
  if (value === null || value === undefined) return '0.00'
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 金额格式化：正数不标符号，负数显示负号 */
function formatAmountWithSign(value: number | null | undefined): string {
  const num = value ?? 0
  const prefix = num < 0 ? '-¥' : '¥'
  return `${prefix}${formatNumber(Math.abs(num))}`
}

function generateAvailableQuarters() {
  const now = new Date()
  const currentYear = now.getFullYear()
  const currentQuarter = Math.ceil((now.getMonth() + 1) / 3)

  // 从当前季度往过去推3年（共13个季度）
  const quarters: string[] = []
  for (let offset = 0; offset <= 12; offset++) {
    let q = currentQuarter - offset
    let y = currentYear
    while (q < 1) { q += 4; y-- }
    quarters.push(`${y}-Q${q}`)
  }

  availableQuarters.value = quarters
  // 默认选择上一季度
  if (!filterForm.selectedQuarter) {
    const prevQ = currentQuarter === 1 ? 4 : currentQuarter - 1
    const prevY = currentQuarter === 1 ? currentYear - 1 : currentYear
    filterForm.selectedQuarter = `${prevY}-Q${prevQ}`
  }
}

async function fetchMarketplaces() {
  try {
    const res = await getEnabledMarketplaces()
    marketplaceOptions.value = res.data || []
  } catch (error) {
    console.error('获取站点列表失败:', error)
  }
}

function handleQuarterChange() {
  // 季度变更后自动查询
  if (filterForm.selectedQuarter) {
    handleQuery()
  }
}

async function fetchSummary() {
  if (!filterForm.selectedQuarter) {
    message.warning('请选择查询季度')
    return
  }

  loading.value = true
  try {
    const res = await getTaxSummary({
      siteCode: filterForm.siteCode,
      startQuarter: filterForm.selectedQuarter,
      endQuarter: filterForm.selectedQuarter,
      refundDateMode: filterForm.refundDateMode
    })
    summaryData.value = (res.data || []).map((item, index) => ({
      ...item,
      key: `${item.siteCode}-${item.yearQuarter}-${index}`
    }))
  } catch (error) {
    console.error('获取报税汇总失败:', error)
    message.error('获取报税汇总失败')
  } finally {
    loading.value = false
  }
}

async function handleQuery() {
  await fetchSummary()
}

async function handleExport() {
  if (!filterForm.selectedQuarter) {
    message.warning('请选择查询季度')
    return
  }

  exporting.value = true
  try {
    await exportTaxSummary({
      siteCode: filterForm.siteCode,
      startQuarter: filterForm.selectedQuarter,
      endQuarter: filterForm.selectedQuarter,
      refundDateMode: filterForm.refundDateMode
    })
    message.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    message.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// 初始化
onMounted(async () => {
  await fetchMarketplaces()
  generateAvailableQuarters()
  if (filterForm.selectedQuarter) {
    handleQuery()
  }
})
</script>

<style lang="scss" scoped>
.tax-summary-page {
  padding: 24px;

  .page-header {
    margin-bottom: 24px;

    .page-title {
      font-size: 20px;
      font-weight: 600;
      color: #333;
      margin: 0 0 8px 0;
    }

    .page-desc {
      font-size: 14px;
      color: #999;
      margin: 0;
    }
  }

  .filter-card {
    margin-bottom: 16px;
  }

  .stat-section {
    margin-bottom: 16px;

    .stat-section-label {
      font-size: 13px;
      color: #8c8c8c;
      margin-bottom: 8px;
      font-weight: 500;
    }

    .stat-row {
      .stat-card {
        text-align: center;
      }
    }
  }

  .data-card {
    margin-bottom: 16px;
  }

  .chart-card {
    .chart-container {
      height: 300px;
    }
  }

  .amount {
    font-weight: 500;
    font-family: 'Monaco', 'Menlo', monospace;

    &.positive {
      color: #52c41a;
    }

    &.negative {
      color: #ff4d4f;
    }

    &.highlight {
      font-size: 15px;
      font-weight: 600;
    }

    &.highlight-amount {
      font-weight: 600;
      color: #1890ff;
    }
  }

  .highlight-card {
    background: linear-gradient(135deg, #fffbe6 0%, #fff 100%);
    border: 1px solid #ffe58f;

    :deep(.ant-statistic-title) {
      color: #d48806;
      font-weight: 500;
    }

    .stat-desc {
      font-size: 12px;
      color: #8c8c8c;
      margin-top: 4px;
    }
  }
}
</style>
