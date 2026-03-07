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
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleQuery" :loading="loading">
              <SearchOutlined /> 查询
            </a-button>
            <a-button @click="handleExport" :loading="exporting">
              <DownloadOutlined /> 导出Excel
            </a-button>
            <a-button @click="handleExportDetail" :loading="exportDetailLoading">
              <DownloadOutlined /> 导出统计明细
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
              title="收入总额(a)"
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
              title="退款金额(b)"
              :value="Math.abs(totalStats.refundBySettlementAmazonCny ?? 0)"
              :precision="2"
              :prefix="(totalStats.refundBySettlementAmazonCny ?? 0) >= 0 ? '¥' : '-¥'"
              :value-style="{ color: (totalStats.refundBySettlementAmazonCny ?? 0) >= 0 ? '#52c41a' : '#ff4d4f' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card">
            <a-statistic
              title="收入净额(c=a-b)"
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
              title="佣金服务费"
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
              title="广告费"
              :value="Math.abs(totalStats.advertisingCostCny ?? 0)"
              :precision="2"
              :prefix="(totalStats.advertisingCostCny ?? 0) >= 0 ? '¥' : '-¥'"
              :value-style="{ color: (totalStats.advertisingCostCny ?? 0) >= 0 ? '#52c41a' : '#13c2c2' }"
            />
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 平台支出与采购成本（按图片公式） -->
    <div class="stat-section">
      <div class="stat-section-label">平台支出与采购成本</div>
      <a-row :gutter="16" class="stat-row">
        <a-col :span="6">
          <a-card class="stat-card highlight-card">
            <a-statistic
              title="平台支出合计⑨=④+⑤+⑥"
              :value="Math.abs(totalStats.platformExpensesCny ?? 0)"
              :precision="2"
              :prefix="'¥'"
              :value-style="{ color: '#fa8c16', fontWeight: 'bold' }"
            />
            <div class="stat-desc">消费税+佣金服务费+广告费</div>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card class="stat-card highlight-card">
            <a-statistic
              title="4%利润⑩=③×4%"
              :value="Math.abs(totalStats.profit4PercentCny ?? 0)"
              :precision="2"
              :prefix="'¥'"
              :value-style="{ color: '#52c41a', fontWeight: 'bold' }"
            />
            <div class="stat-desc">收入净额的4%</div>
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
          <template v-else-if="column.key === 'refundBySettlementAmazonCny'">
            <span :class="['amount', (record.refundBySettlementAmazonCny ?? 0) >= 0 ? 'positive' : 'negative']">{{ formatAmountWithSign(record.refundBySettlementAmazonCny) }}</span>
            <a-tag size="small" style="margin-left: 4px">{{ record.refundCountBySettlementAmazon }}笔</a-tag>
          </template>
          <template v-else-if="column.key === 'netRevenueCny'">
            <span :class="['amount', ((record.totalRevenueCny ?? 0) - Math.abs(record.refundBySettlementAmazonCny ?? 0)) >= 0 ? 'positive' : 'negative']">
              {{ formatAmountWithSign((record.totalRevenueCny ?? 0) - Math.abs(record.refundBySettlementAmazonCny ?? 0)) }}
            </span>
          </template>
          <template v-else-if="column.key === 'totalCommissionFeeCny'">
            <span :class="['amount', (record.totalCommissionFeeCny ?? 0) >= 0 ? 'positive' : 'negative']">{{ formatAmountWithSign(record.totalCommissionFeeCny) }}</span>
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

    <!-- 费用分类图表 -->
    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="12">
        <a-card title="费用分类占比" class="chart-card">
          <div ref="pieChartRef" class="chart-container"></div>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="费用分类对比" class="chart-card">
          <div ref="barChartRef" class="chart-container"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 费用明细表格 -->
    <a-card title="其他费分类明细" class="data-card" style="margin-top: 16px">
      <a-table
        :columns="feeColumns"
        :data-source="feeData"
        :loading="feeLoading"
        :pagination="false"
        row-key="key"
        bordered
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'feeType'">
            <a-tag :color="getFeeTypeColor(record.feeCategory)">{{ record.feeType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'amountCny'">
            <span :class="['amount', (record.amountCny ?? 0) >= 0 ? 'positive' : 'negative']">{{ formatAmountWithSign(record.amountCny) }}</span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import {
  getTaxSummary,
  getFeeBreakdown,
  exportTaxSummary,
  exportTaxSummaryDetail,
  type TaxReportSummary,
  type FeeBreakdown
} from '@/api/report'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { Marketplace } from '@/types/marketplace'

// ============= 筛选条件 =============
const filterForm = reactive({
  siteCode: undefined as string | undefined,
  selectedQuarter: ''
})

const marketplaceOptions = ref<Marketplace[]>([])
const availableQuarters = ref<string[]>([])

// ============= 数据 =============
const loading = ref(false)
const feeLoading = ref(false)
const exporting = ref(false)
const exportDetailLoading = ref(false)

const summaryData = ref<(TaxReportSummary & { key: string })[]>([])
const feeData = ref<(FeeBreakdown & { key: string })[]>([])

// 图表引用
const pieChartRef = ref<HTMLElement | null>(null)
const barChartRef = ref<HTMLElement | null>(null)
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

// 汇总统计
const totalStats = computed(() => {
  const data = summaryData.value
  const totalRevenueCny = data.reduce((sum, item) => sum + (item.totalRevenueCny || 0), 0)
  const refundBySettlementAmazonCny = data.reduce((sum, item) => sum + (item.refundBySettlementAmazonCny || 0), 0)
  const miscServiceFeeCny = data.reduce((sum, item) => sum + (item.miscServiceFeeCny || 0), 0)
  const otherFeesCny = data.reduce((sum, item) => sum + (item.otherFeesCny || 0), 0)
  const netRevenueCny = totalRevenueCny - Math.abs(refundBySettlementAmazonCny)
  const consumptionTaxCny = data.reduce((sum, item) => sum + (item.consumptionTaxCny || 0), 0)
  const totalCommissionFeeCny = data.reduce((sum, item) => sum + (item.totalCommissionFeeCny || 0), 0)
  const advertisingCostCny = data.reduce((sum, item) => sum + (item.advertisingCostCny || 0), 0)

  // 按图片公式计算
  // ⑨平台支出合计 = ④消费税 + ⑤佣金服务费 + ⑥广告费
  const platformExpensesCny = Math.abs(consumptionTaxCny) + Math.abs(totalCommissionFeeCny) + Math.abs(advertisingCostCny)
  // ⑩4%利润 = ③收入净额 × 4%
  const profit4PercentCny = netRevenueCny * 0.04
  // ⑪采购成本 = ③ − ⑨ − ⑩
  const procurementCostCny = netRevenueCny - platformExpensesCny - profit4PercentCny

  return {
    totalRevenueCny,
    refundBySettlementAmazonCny,
    netRevenueCny,
    consumptionTaxCny,
    totalCommissionFeeCny,
    miscServiceFeeCny,
    otherFeesCny,
    otherFeesTotalCny: miscServiceFeeCny + otherFeesCny,
    advertisingCostCny,
    // 新增字段
    platformExpensesCny,
    profit4PercentCny,
    procurementCostCny
  }
})

// 表格列定义 - Amazon口径
const summaryColumns = [
  { title: '站点', dataIndex: 'siteCode', key: 'siteCode', width: 150, fixed: 'left' },
  { title: '季度', dataIndex: 'yearQuarter', key: 'yearQuarter', width: 100 },
  { title: '收入总额①', dataIndex: 'totalRevenueCny', key: 'totalRevenueCny', width: 140, align: 'right' },
  { title: '退款金额②', dataIndex: 'refundBySettlementAmazonCny', key: 'refundBySettlementAmazonCny', width: 160, align: 'right' },
  { title: '收入净额③=①-②', key: 'netRevenueCny', width: 160, align: 'right' },
  { title: '平台代扣税④', dataIndex: 'consumptionTaxCny', key: 'consumptionTaxCny', width: 130, align: 'right' },
  { title: '佣金服务费⑤', dataIndex: 'totalCommissionFeeCny', key: 'totalCommissionFeeCny', width: 150, align: 'right' },
  { title: '广告费⑥', dataIndex: 'advertisingCostCny', key: 'advertisingCostCny', width: 130, align: 'right' },
  { title: '平台支出合计⑨=④+⑤+⑥', dataIndex: 'platformExpensesCny', key: 'platformExpensesCny', width: 180, align: 'right' },
  { title: '4%利润⑩=③×4%', dataIndex: 'profit4PercentCny', key: 'profit4PercentCny', width: 150, align: 'right' },
  { title: '采购成本⑪=③-⑨-⑩', dataIndex: 'procurementCostCny', key: 'procurementCostCny', width: 170, align: 'right' }
]

const feeColumns = [
  { title: '站点', dataIndex: 'siteCode', key: 'siteCode', width: 80 },
  { title: '季度', dataIndex: 'yearQuarter', key: 'yearQuarter', width: 100 },
  { title: '费用类型', dataIndex: 'feeType', key: 'feeType', width: 200 },
  { title: '费用分类', dataIndex: 'feeCategory', key: 'feeCategory', width: 100 },
  { title: '金额(人民币)', dataIndex: 'amountCny', key: 'amountCny', width: 150, align: 'right' },
  { title: '交易笔数', dataIndex: 'transactionCount', key: 'transactionCount', width: 100, align: 'right' }
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

function getFeeTypeColor(category: string): string {
  switch (category) {
    case 'fee': return 'orange'
    case 'adjustment': return 'purple'
    case 'other': return 'default'
    default: return 'blue'
  }
}

function generateAvailableQuarters() {
  const quarters: string[] = []
  const now = new Date()
  let year = now.getFullYear()
  let quarter = Math.ceil((now.getMonth() + 1) / 3)

  for (let i = 0; i < 12; i++) {
    quarters.push(`${year}-Q${quarter}`)
    quarter--
    if (quarter === 0) {
      quarter = 4
      year--
    }
  }

  availableQuarters.value = quarters
  // 默认选择上一季度
  if (!filterForm.selectedQuarter && quarters.length > 1) {
    filterForm.selectedQuarter = quarters[1]
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
      endQuarter: filterForm.selectedQuarter
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

async function fetchFeeBreakdown() {
  if (!filterForm.selectedQuarter) return

  feeLoading.value = true
  try {
    const res = await getFeeBreakdown({
      siteCode: filterForm.siteCode,
      startQuarter: filterForm.selectedQuarter,
      endQuarter: filterForm.selectedQuarter
    })
    feeData.value = (res.data || []).map((item, index) => ({
      ...item,
      key: `${item.siteCode}-${item.yearQuarter}-${item.feeType}-${index}`
    }))
    // 更新图表
    await nextTick()
    updateCharts()
  } catch (error) {
    console.error('获取费用明细失败:', error)
  } finally {
    feeLoading.value = false
  }
}

function updateCharts() {
  // 按费用类型汇总（其他费明细）
  const feeTypeMap = new Map<string, number>()
  feeData.value.forEach(item => {
    const current = feeTypeMap.get(item.feeType) || 0
    feeTypeMap.set(item.feeType, current + item.amountCny)
  })

  // 加入广告费汇总（来自报税汇总数据），按原始正负值累加
  const advertisingTotal = totalStats.value.advertisingCostCny
  if (advertisingTotal != null && advertisingTotal !== 0) {
    const current = feeTypeMap.get('广告费') || 0
    feeTypeMap.set('广告费', current + advertisingTotal)
  }

  // 按原始值排序，图表展示用原始值（柱状图可显示负值）
  const pieDataArr = Array.from(feeTypeMap.entries())
    .map(([name, value]) => ({ name, value, displayValue: Math.abs(value) }))
    .filter(item => item.value !== 0)
    .sort((a, b) => Math.abs(b.value) - Math.abs(a.value))

  if (!pieDataArr.length) return

  // 饼图
  if (pieChartRef.value) {
    if (!pieChart) {
      pieChart = echarts.init(pieChartRef.value)
    }
    pieChart.setOption({
      tooltip: {
        trigger: 'item',
        formatter: (params: any) => {
          const actual = params.data.actualValue ?? params.value
          const prefix = actual < 0 ? '-¥' : '¥'
          return `${params.name}: ${prefix}${formatNumber(Math.abs(actual))} (${params.percent}%)`
        }
      },
      legend: {
        orient: 'vertical',
        right: 10,
        top: 'center'
      },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        data: pieDataArr.map(item => ({ name: item.name, value: item.displayValue, actualValue: item.value })),
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        label: {
          show: false
        }
      }]
    })
  }

  // 柱状图
  if (barChartRef.value) {
    if (!barChart) {
      barChart = echarts.init(barChartRef.value)
    }
    barChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          const item = params[0]
          const v = item.value
          const prefix = v < 0 ? '-¥' : '¥'
          return `${item.name}: ${prefix}${formatNumber(Math.abs(v))}`
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: pieDataArr.slice(0, 10).map(item => item.name),
        axisLabel: {
          rotate: 30,
          interval: 0
        }
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: (value: number) => `¥${(value / 1000).toFixed(0)}k`
        }
      },
      series: [{
        type: 'bar',
        data: pieDataArr.slice(0, 10).map(item => item.value),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#ff7875' },
            { offset: 1, color: '#ff4d4f' }
          ])
        }
      }]
    })
  }
}

async function handleQuery() {
  await fetchSummary()
  await fetchFeeBreakdown()
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
      endQuarter: filterForm.selectedQuarter
    })
    message.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    message.error('导出失败')
  } finally {
    exporting.value = false
  }
}

async function handleExportDetail() {
  if (!filterForm.selectedQuarter) {
    message.warning('请选择查询季度')
    return
  }

  exportDetailLoading.value = true
  try {
    await exportTaxSummaryDetail({
      siteCode: filterForm.siteCode,
      startQuarter: filterForm.selectedQuarter,
      endQuarter: filterForm.selectedQuarter
    })
    message.success('导出成功（收入/退款/费用/其它 分 sheet 或分文件）')
  } catch (error) {
    console.error('导出统计明细失败:', error)
    message.error('导出失败')
  } finally {
    exportDetailLoading.value = false
  }
}

// 窗口大小变化时重绘图表
function handleResize() {
  pieChart?.resize()
  barChart?.resize()
}

// 初始化
onMounted(async () => {
  await fetchMarketplaces()
  generateAvailableQuarters()
  window.addEventListener('resize', handleResize)
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
