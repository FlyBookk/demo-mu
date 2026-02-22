<template>
  <div class="dashboard-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">首页</h1>
        <p class="page-desc">{{ dashboardData.currentQuarter }} 数据概览</p>
      </div>
      <div class="header-right">
        <a-select
          v-model:value="selectedQuarter"
          placeholder="选择季度"
          style="width: 140px"
          :options="quarterOptions"
          @change="handleQuarterChange"
        />
      </div>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stat-cards">
      <a-col :span="6">
        <a-card :loading="loading">
          <a-statistic
            title="本季度收入"
            :value="dashboardData.totalRevenueCny"
            :precision="2"
            prefix="¥"
            :value-style="{ color: '#3f8600' }"
          >
            <template #suffix>
              <span class="stat-trend" :class="dashboardData.revenueGrowthRate >= 0 ? 'up' : 'down'">
                {{ formatGrowth(dashboardData.revenueGrowthRate) }}
              </span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :loading="loading">
          <a-statistic
            title="本季度退款"
            :value="dashboardData.refundCny"
            :precision="2"
            prefix="¥"
            :value-style="{ color: '#cf1322' }"
          >
            <template #suffix>
              <span class="stat-trend" :class="dashboardData.refundGrowthRate >= 0 ? 'up' : 'down'">
                {{ formatGrowth(dashboardData.refundGrowthRate) }}
              </span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :loading="loading">
          <a-statistic
            title="本季度净收入"
            :value="dashboardData.netIncomeCny"
            :precision="2"
            prefix="¥"
            :value-style="{ color: dashboardData.netIncomeCny >= 0 ? '#3f8600' : '#cf1322' }"
          >
            <template #suffix>
              <span class="stat-trend" :class="dashboardData.netIncomeGrowthRate >= 0 ? 'up' : 'down'">
                {{ formatGrowth(dashboardData.netIncomeGrowthRate) }}
              </span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :loading="loading">
          <a-statistic
            title="发货订单数"
            :value="dashboardData.shippingOrderCount"
            :value-style="{ color: '#1890ff' }"
          />
        </a-card>
      </a-col>
    </a-row>

    <!-- 内容区 -->
    <a-row :gutter="16" class="content-row">
      <!-- 各站点收入对比 -->
      <a-col :span="16">
        <a-card title="各站点收入对比" :loading="loading">
          <div ref="siteChartRef" class="chart-container"></div>
        </a-card>
      </a-col>

      <!-- 快捷入口 -->
      <a-col :span="8">
        <a-card title="快捷入口">
          <div class="quick-links">
            <a-button type="primary" block class="quick-btn" @click="goTo('/sales/import')">
              <PlusOutlined /> 导入销售数据
            </a-button>
            <a-button block class="quick-btn" @click="goTo('/shipping/import')">
              <PlusOutlined /> 导入配送数据
            </a-button>
            <a-button block class="quick-btn" @click="goTo('/advertising/add')">
              <PlusOutlined /> 录入广告费
            </a-button>
            <a-button block class="quick-btn" @click="goTo('/report/tax-summary')">
              <BarChartOutlined /> 查看报税汇总
            </a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 季度趋势 -->
    <a-card title="季度收入趋势" :loading="loading" class="trend-card">
      <div ref="trendChartRef" class="chart-container"></div>
    </a-card>

    <!-- 最近导入记录 -->
    <a-card title="最近导入记录" class="recent-imports">
      <template #extra>
        <a @click="goTo('/config/import-record')">查看全部 ></a>
      </template>

      <a-table
        :columns="importColumns"
        :data-source="recentImports"
        :pagination="false"
        :loading="importLoading"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === '成功' ? 'success' : record.status === '失败' ? 'error' : 'warning'">
              {{ record.status }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  PlusOutlined,
  BarChartOutlined
} from '@ant-design/icons-vue'
import { getDashboardData, type DashboardData } from '@/api/report'
import { getImportRecordList } from '@/api/importRecord'

const router = useRouter()

// 季度选择
/** 获取上一季度（首页默认展示） */
function getPreviousQuarter(): string {
  const now = new Date()
  let year = now.getFullYear()
  let month = now.getMonth() + 1
  let q = Math.ceil(month / 3)
  q--
  if (q < 1) {
    q = 4
    year--
  }
  return `${year}-Q${q}`
}

function generateQuarterOptions(): { label: string; value: string }[] {
  const options: { label: string; value: string }[] = []
  let year = new Date().getFullYear()
  let q = Math.ceil((new Date().getMonth() + 1) / 3)
  for (let i = 0; i < 8; i++) {
    options.push({ label: `${year}年 Q${q}`, value: `${year}-Q${q}` })
    q--
    if (q < 1) {
      q = 4
      year--
    }
  }
  return options
}

const quarterOptions = generateQuarterOptions()
const selectedQuarter = ref<string>(getPreviousQuarter())

// 数据
const loading = ref(false)
const importLoading = ref(false)

const dashboardData = reactive<DashboardData>({
  currentQuarter: '',
  totalRevenueCny: 0,
  refundCny: 0,
  netIncomeCny: 0,
  shippingOrderCount: 0,
  revenueGrowthRate: 0,
  refundGrowthRate: 0,
  netIncomeGrowthRate: 0,
  siteRevenues: [],
  quarterTrends: []
})

// 图表
const siteChartRef = ref<HTMLElement | null>(null)
const trendChartRef = ref<HTMLElement | null>(null)
let siteChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

// 导入记录
const importColumns = [
  { title: '类型', dataIndex: 'dataType', key: 'dataType' },
  { title: '文件名', dataIndex: 'fileName', key: 'fileName' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '条数', dataIndex: 'totalCount', key: 'totalCount' },
  { title: '时间', dataIndex: 'createTime', key: 'createTime' }
]
const recentImports = ref<any[]>([])

// 方法
function goTo(path: string) {
  router.push(path)
}

function formatGrowth(rate: number): string {
  if (rate === 0) return '-'
  const sign = rate >= 0 ? '+' : ''
  return `${sign}${rate.toFixed(1)}%`
}

async function fetchDashboard() {
  loading.value = true
  try {
    const res = await getDashboardData(selectedQuarter.value)
    if (res.data) {
      Object.assign(dashboardData, res.data)
      await nextTick()
      renderCharts()
    }
  } catch (error) {
    console.error('获取首页数据失败:', error)
  } finally {
    loading.value = false
  }
}

function handleQuarterChange() {
  fetchDashboard()
}

async function fetchRecentImports() {
  importLoading.value = true
  try {
    const res = await getImportRecordList({ page: 1, pageSize: 5 })
    if (res.data?.records) {
      recentImports.value = res.data.records.map((item: any) => ({
        ...item,
        key: item.id,
        status: item.status === 'success' ? '成功' : item.status === 'failed' ? '失败' : '部分'
      }))
    }
  } catch (error) {
    console.error('获取导入记录失败:', error)
  } finally {
    importLoading.value = false
  }
}

function renderCharts() {
  // 站点收入对比图
  if (siteChartRef.value && dashboardData.siteRevenues.length > 0) {
    if (!siteChart) {
      siteChart = echarts.init(siteChartRef.value)
    }

    const sites = dashboardData.siteRevenues.map(s => s.siteName || s.siteCode)
    const revenues = dashboardData.siteRevenues.map(s => s.revenue || 0)
    const refunds = dashboardData.siteRevenues.map(s => s.refund || 0)

    siteChart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        formatter: (params: any) => {
          let html = `${params[0].name}<br/>`
          params.forEach((item: any) => {
            html += `${item.marker} ${item.seriesName}: ¥${item.value.toLocaleString()}<br/>`
          })
          return html
        }
      },
      legend: {
        data: ['收入', '退款']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: sites
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: (value: number) => `¥${(value / 10000).toFixed(0)}万`
        }
      },
      series: [
        {
          name: '收入',
          type: 'bar',
          data: revenues,
          itemStyle: { color: '#52c41a' }
        },
        {
          name: '退款',
          type: 'bar',
          data: refunds,
          itemStyle: { color: '#ff4d4f' }
        }
      ]
    })
  }

  // 季度趋势图
  if (trendChartRef.value && dashboardData.quarterTrends.length > 0) {
    if (!trendChart) {
      trendChart = echarts.init(trendChartRef.value)
    }

    const quarters = dashboardData.quarterTrends.map(t => t.quarter)
    const revenues = dashboardData.quarterTrends.map(t => t.revenue || 0)
    const netIncomes = dashboardData.quarterTrends.map(t => t.netIncome || 0)

    trendChart.setOption({
      tooltip: {
        trigger: 'axis',
        formatter: (params: any) => {
          let html = `${params[0].name}<br/>`
          params.forEach((item: any) => {
            html += `${item.marker} ${item.seriesName}: ¥${item.value.toLocaleString()}<br/>`
          })
          return html
        }
      },
      legend: {
        data: ['收入', '净收入']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: quarters
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: (value: number) => `¥${(value / 10000).toFixed(0)}万`
        }
      },
      series: [
        {
          name: '收入',
          type: 'line',
          data: revenues,
          smooth: true,
          itemStyle: { color: '#1890ff' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
              { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
            ])
          }
        },
        {
          name: '净收入',
          type: 'line',
          data: netIncomes,
          smooth: true,
          itemStyle: { color: '#52c41a' }
        }
      ]
    })
  }
}

function handleResize() {
  siteChart?.resize()
  trendChart?.resize()
}

// 初始化
onMounted(() => {
  fetchDashboard()
  fetchRecentImports()
  window.addEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  padding: 24px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24px;

    .header-left {
      .page-title {
        font-size: 20px;
        font-weight: 500;
        margin: 0 0 4px 0;
      }

      .page-desc {
        color: #999;
        font-size: 14px;
        margin: 0;
      }
    }

    .header-right {
      flex-shrink: 0;
    }
  }

  .stat-cards {
    margin-bottom: 16px;

    .stat-trend {
      font-size: 12px;
      margin-left: 8px;

      &.up {
        color: #52c41a;
      }

      &.down {
        color: #ff4d4f;
      }
    }
  }

  .content-row {
    margin-bottom: 16px;

    .chart-container {
      height: 300px;
    }

    .quick-links {
      .quick-btn {
        margin-bottom: 12px;

        &:last-child {
          margin-bottom: 0;
        }
      }
    }
  }

  .trend-card {
    margin-bottom: 16px;

    .chart-container {
      height: 250px;
    }
  }

  .recent-imports {
    :deep(a) {
      color: #1890ff;
    }
  }
}
</style>
