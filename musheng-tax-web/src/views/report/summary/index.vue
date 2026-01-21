<template>
  <div class="report-summary-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">汇总报表</h1>
      <p class="page-desc">查看各站点季度销售、费用、VAT及利润汇总数据</p>
    </div>

    <!-- 筛选条件 -->
    <a-card class="filter-card">
      <a-form layout="inline">
        <a-form-item label="站点">
          <a-select
            v-model:value="filterForm.marketplaceId"
            placeholder="全部站点"
            allow-clear
            style="width: 150px"
            @change="handleFilterChange"
          >
            <a-select-option
              v-for="marketplace in marketplaceOptions"
              :key="marketplace.id"
              :value="marketplace.id"
            >
              {{ marketplace.siteCode }} - {{ marketplace.siteName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="季度">
          <a-select
            v-model:value="filterForm.quarter"
            placeholder="请选择季度"
            style="width: 150px"
            @change="handleFilterChange"
          >
            <a-select-option v-for="q in availableQuarters" :key="q" :value="q">
              {{ formatQuarter(q) }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleQuery">
              <SearchOutlined /> 查询
            </a-button>
            <a-button @click="handleGenerateReport" :loading="generating">
              <SyncOutlined /> 生成报表
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 汇总统计卡片 -->
    <a-row :gutter="16" class="stat-row">
      <a-col :span="4">
        <a-card class="stat-card">
          <a-statistic
            title="净销售额"
            :value="summary.netSales"
            :precision="2"
            :value-style="{ color: '#52c41a' }"
          >
            <template #prefix><DollarOutlined /></template>
            <template #suffix>{{ summary.currencyCode }}</template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card class="stat-card">
          <a-statistic
            title="总费用"
            :value="summary.totalFees"
            :precision="2"
            :value-style="{ color: '#ff4d4f' }"
          >
            <template #prefix><MoneyCollectOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card class="stat-card">
          <a-statistic
            title="净VAT"
            :value="summary.netVat"
            :precision="2"
            :value-style="{ color: '#faad14' }"
          >
            <template #prefix><BankOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card class="stat-card">
          <a-statistic
            title="净利润"
            :value="summary.netProfit"
            :precision="2"
            :value-style="{ color: summary.netProfit >= 0 ? '#52c41a' : '#ff4d4f' }"
          >
            <template #prefix><RiseOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card class="stat-card">
          <a-statistic
            title="利润率"
            :value="summary.profitMargin * 100"
            :precision="2"
            suffix="%"
            :value-style="{ color: '#1890ff' }"
          >
            <template #prefix><PercentageOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card class="stat-card">
          <a-statistic
            title="总订单数"
            :value="summary.totalOrders"
            :value-style="{ color: '#722ed1' }"
          >
            <template #prefix><ShoppingCartOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- 详细报表 -->
    <a-row :gutter="16">
      <!-- 销售明细 -->
      <a-col :span="12">
        <a-card title="销售明细" class="detail-card">
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="总销售额">
              <span class="amount positive">{{ formatCurrency(summary.totalSales, summary.currencyCode) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="退款金额">
              <span class="amount negative">{{ formatCurrency(summary.totalRefunds, summary.currencyCode) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="净销售额">
              <span class="amount highlight">{{ formatCurrency(summary.netSales, summary.currencyCode) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="总订单数">{{ summary.totalOrders }}</a-descriptions-item>
            <a-descriptions-item label="总销量">{{ summary.totalUnits }}</a-descriptions-item>
            <a-descriptions-item label="平均订单金额">
              {{ formatCurrency(summary.avgOrderValue, summary.currencyCode) }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <!-- 费用明细 -->
      <a-col :span="12">
        <a-card title="费用明细" class="detail-card">
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="平台佣金">
              <span class="amount negative">{{ formatCurrency(summary.totalCommission, summary.currencyCode) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="FBA费用">
              <span class="amount negative">{{ formatCurrency(summary.totalFbaFee, summary.currencyCode) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="运费">
              <span class="amount negative">{{ formatCurrency(summary.totalShippingFee, summary.currencyCode) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="广告费用">
              <span class="amount negative">{{ formatCurrency(summary.totalAdvertisingSpend, summary.currencyCode) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="其他费用">
              <span class="amount negative">{{ formatCurrency(summary.totalOtherFee, summary.currencyCode) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="费用合计">
              <span class="amount highlight negative">{{ formatCurrency(summary.totalFees, summary.currencyCode) }}</span>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <!-- VAT明细 -->
      <a-col :span="12">
        <a-card title="VAT明细" class="detail-card">
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="VAT税率">{{ summary.vatRate }}%</a-descriptions-item>
            <a-descriptions-item label="已收VAT">
              <span class="amount positive">{{ formatCurrency(summary.totalVatCollected, summary.currencyCode) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="已付VAT">
              <span class="amount negative">{{ formatCurrency(summary.totalVatPaid, summary.currencyCode) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="净VAT (应付)">
              <span class="amount highlight" :class="summary.netVat >= 0 ? 'negative' : 'positive'">
                {{ formatCurrency(summary.netVat, summary.currencyCode) }}
              </span>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <!-- 利润汇总 -->
      <a-col :span="12">
        <a-card title="利润汇总" class="detail-card">
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="净销售额">
              {{ formatCurrency(summary.netSales, summary.currencyCode) }}
            </a-descriptions-item>
            <a-descriptions-item label="费用合计">
              <span class="amount negative">{{ formatCurrency(summary.totalFees, summary.currencyCode) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="毛利润">
              <span class="amount" :class="summary.grossProfit >= 0 ? 'positive' : 'negative'">
                {{ formatCurrency(summary.grossProfit, summary.currencyCode) }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="净利润">
              <span class="amount highlight" :class="summary.netProfit >= 0 ? 'positive' : 'negative'">
                {{ formatCurrency(summary.netProfit, summary.currencyCode) }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="利润率">
              <span :class="summary.profitMargin >= 0 ? 'positive' : 'negative'">
                {{ (summary.profitMargin * 100).toFixed(2) }}%
              </span>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
    </a-row>

    <!-- 各站点汇总表 -->
    <a-card title="各站点数据汇总" class="marketplace-card" style="margin-top: 16px">
      <a-table
        :columns="marketplaceColumns"
        :data-source="marketplaceSummary"
        :loading="loading"
        :pagination="false"
        row-key="marketplaceId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'marketplaceCode'">
            <a-tag color="blue">{{ record.marketplaceCode }}</a-tag>
          </template>
          <template v-else-if="column.key === 'totalSales'">
            <span class="amount positive">{{ formatCurrency(record.totalSales, record.currencyCode) }}</span>
          </template>
          <template v-else-if="column.key === 'totalFees'">
            <span class="amount negative">{{ formatCurrency(record.totalFees, record.currencyCode) }}</span>
          </template>
          <template v-else-if="column.key === 'netProfit'">
            <span class="amount" :class="record.netProfit >= 0 ? 'positive' : 'negative'">
              {{ formatCurrency(record.netProfit, record.currencyCode) }}
            </span>
          </template>
          <template v-else-if="column.key === 'percentage'">
            <a-progress :percent="record.percentage" :size="[100, 8]" />
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  SearchOutlined,
  SyncOutlined,
  DollarOutlined,
  MoneyCollectOutlined,
  BankOutlined,
  RiseOutlined,
  PercentageOutlined,
  ShoppingCartOutlined
} from '@ant-design/icons-vue'
import {
  getReportSummary,
  getMarketplaceReportSummary
} from '@/api/report'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { ReportSummary, MarketplaceReportSummary } from '@/types/report'
import type { Marketplace } from '@/types/marketplace'

// ============= 筛选条件 =============
const filterForm = reactive({
  marketplaceId: undefined as number | undefined,
  quarter: ''
})

const marketplaceOptions = ref<Marketplace[]>([])
const availableQuarters = ref<string[]>([])

// ============= 数据 =============
const loading = ref(false)
const generating = ref(false)

const summary = reactive<ReportSummary>({
  marketplaceId: 0,
  quarter: '',
  year: 0,
  currencyCode: 'EUR',
  totalSales: 0,
  totalRefunds: 0,
  netSales: 0,
  totalCommission: 0,
  totalFbaFee: 0,
  totalShippingFee: 0,
  totalAdvertisingSpend: 0,
  totalOtherFee: 0,
  totalFees: 0,
  totalVatCollected: 0,
  totalVatPaid: 0,
  netVat: 0,
  vatRate: 0,
  grossProfit: 0,
  netProfit: 0,
  profitMargin: 0,
  totalOrders: 0,
  totalUnits: 0,
  avgOrderValue: 0
})

const marketplaceSummary = ref<MarketplaceReportSummary[]>([])

const marketplaceColumns = [
  {
    title: '站点',
    dataIndex: 'marketplaceCode',
    key: 'marketplaceCode',
    width: 100
  },
  {
    title: '站点名称',
    dataIndex: 'marketplaceName',
    key: 'marketplaceName',
    width: 120
  },
  {
    title: '总销售额',
    dataIndex: 'totalSales',
    key: 'totalSales',
    width: 150,
    align: 'right' as const
  },
  {
    title: '总费用',
    dataIndex: 'totalFees',
    key: 'totalFees',
    width: 150,
    align: 'right' as const
  },
  {
    title: '净利润',
    dataIndex: 'netProfit',
    key: 'netProfit',
    width: 150,
    align: 'right' as const
  },
  {
    title: '订单数',
    dataIndex: 'totalOrders',
    key: 'totalOrders',
    width: 100,
    align: 'right' as const
  },
  {
    title: '占比',
    dataIndex: 'percentage',
    key: 'percentage',
    width: 150
  }
]

// ============= 方法 =============
function formatQuarter(quarter: string): string {
  // 格式: 2024-Q1 -> 2024年第1季度
  const [year, q] = quarter.split('-Q')
  return `${year}年第${q}季度`
}

function formatCurrency(amount: number, currency: string): string {
  return `${currency} ${amount.toFixed(2)}`
}

async function fetchMarketplaces() {
  try {
    const res = await getEnabledMarketplaces()
    marketplaceOptions.value = res.data || []
  } catch (error) {
    console.error('获取站点列表失败:', error)
  }
}

function generateAvailableQuarters() {
  // 生成最近8个季度的选项
  const quarters: string[] = []
  const now = new Date()
  let year = now.getFullYear()
  let quarter = Math.ceil((now.getMonth() + 1) / 3)
  
  for (let i = 0; i < 8; i++) {
    quarters.push(`${year}-Q${quarter}`)
    quarter--
    if (quarter === 0) {
      quarter = 4
      year--
    }
  }
  
  availableQuarters.value = quarters
  // 默认选择当前季度
  if (quarters.length > 0 && !filterForm.quarter) {
    filterForm.quarter = quarters[0]
  }
}

async function fetchSummary() {
  if (!filterForm.quarter) {
    return
  }

  loading.value = true
  try {
    // 获取选中站点的siteCode
    const marketplace = marketplaceOptions.value.find(m => m.id === filterForm.marketplaceId)
    const siteCode = marketplace?.siteCode

    // 构建查询参数，只传有值的参数
    const params: Record<string, string> = {
      yearQuarter: filterForm.quarter
    }
    if (siteCode) {
      params.siteCode = siteCode
    }

    // 获取汇总数据
    const summaryRes = await getReportSummary(params)
    
    // API返回的是数组
    const dataList = summaryRes.data || []
    if (dataList.length > 0) {
      const summaryData = dataList[0]
      // 映射字段到页面使用的格式
      Object.assign(summary, {
        ...summaryData,
        netSales: summaryData.totalSalesAmountEur || 0,
        totalFees: (summaryData.totalShippingCostEur || 0) + (summaryData.totalAdvertisingCostEur || 0),
        netVat: summaryData.vatAmountEur || 0,
        netProfit: summaryData.netAmountEur || 0,
        profitMargin: summaryData.totalSalesAmountEur ? (summaryData.netAmountEur || 0) / summaryData.totalSalesAmountEur : 0,
        totalOrders: summaryData.transactionCount || 0,
        currencyCode: summaryData.currencyCode || 'EUR'
      })
      
      // 如果没有选择特定站点，使用返回的列表作为各站点汇总
      if (!siteCode && dataList.length > 1) {
        marketplaceSummary.value = dataList.map((item: any) => ({
          marketplaceCode: item.siteCode,
          marketplaceName: item.siteName,
          totalSales: item.totalSalesAmountEur,
          totalFees: (item.totalShippingCostEur || 0) + (item.totalAdvertisingCostEur || 0),
          netProfit: item.netAmountEur,
          totalOrders: item.transactionCount,
          currencyCode: item.currencyCode,
          percentage: 0
        }))
      } else {
        marketplaceSummary.value = []
      }
    } else {
      // 没有数据时重置
      resetSummary()
      marketplaceSummary.value = []
    }
  } catch (error: any) {
    console.error('获取报表数据失败:', error)
    // 如果是系统错误，可能是没有数据，显示空状态
    resetSummary()
    marketplaceSummary.value = []
  } finally {
    loading.value = false
  }
}

function resetSummary() {
  Object.assign(summary, {
    netSales: 0,
    totalFees: 0,
    netVat: 0,
    netProfit: 0,
    profitMargin: 0,
    totalOrders: 0,
    totalUnits: 0,
    totalSales: 0,
    totalRefunds: 0,
    totalCommission: 0,
    totalFbaFee: 0,
    totalShippingFee: 0,
    totalAdvertisingSpend: 0,
    totalOtherFee: 0,
    totalVatCollected: 0,
    totalVatPaid: 0,
    grossProfit: 0,
    avgOrderValue: 0,
    currencyCode: 'EUR'
  })
}

function handleFilterChange() {
  // 筛选条件变化时的处理
}

function handleQuery() {
  fetchSummary()
}

async function handleGenerateReport() {
  if (!filterForm.quarter) {
    message.warning('请先选择季度')
    return
  }

  generating.value = true
  try {
    // 直接刷新数据，后端暂不支持手动生成报表
    await fetchSummary()
    message.success('报表数据已刷新')
  } catch (error) {
    console.error('刷新报表失败:', error)
    message.error('刷新报表失败')
  } finally {
    generating.value = false
  }
}

// 初始化
onMounted(async () => {
  await fetchMarketplaces()
  generateAvailableQuarters()
  if (filterForm.quarter) {
    fetchSummary()
  }
})
</script>

<style lang="scss" scoped>
.report-summary-page {
  padding: $spacing-lg;

  .page-header {
    margin-bottom: $spacing-lg;

    .page-title {
      font-size: $font-size-xl;
      font-weight: 600;
      color: $text-color;
      margin: 0 0 $spacing-xs 0;
    }

    .page-desc {
      font-size: $font-size-md;
      color: $text-color-secondary;
      margin: 0;
    }
  }

  .filter-card {
    margin-bottom: $spacing-lg;
  }

  .stat-row {
    margin-bottom: $spacing-lg;

    .stat-card {
      text-align: center;
    }
  }

  .detail-card {
    height: 100%;
  }

  .amount {
    font-weight: 500;

    &.positive {
      color: $success-color;
    }

    &.negative {
      color: $error-color;
    }

    &.highlight {
      font-size: $font-size-lg;
      font-weight: 600;
    }
  }

  .positive {
    color: $success-color;
  }

  .negative {
    color: $error-color;
  }
}
</style>
