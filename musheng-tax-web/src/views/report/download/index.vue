<template>
  <div class="report-download-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">报表下载</h1>
      <p class="page-desc">导出各类业务报表，支持 Excel、PDF、CSV 格式</p>
    </div>

    <!-- 报表类型选择 -->
    <a-row :gutter="16">
      <!-- 季度VAT报表 -->
      <a-col :span="8">
        <a-card class="report-card" hoverable>
          <template #cover>
            <div class="card-icon vat">
              <FileExcelOutlined />
            </div>
          </template>
          <a-card-meta title="季度VAT报表" description="包含销售、费用、VAT计算明细，适用于VAT申报" />
          <div class="card-actions">
            <a-form layout="vertical" :model="vatReportForm">
              <a-form-item label="站点" :required="true">
                <a-select
                  v-model:value="vatReportForm.marketplaceId"
                  placeholder="请选择站点"
                  style="width: 100%"
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
              <a-form-item label="季度" :required="true">
                <a-select
                  v-model:value="vatReportForm.quarter"
                  placeholder="请选择季度"
                  style="width: 100%"
                >
                  <a-select-option v-for="q in availableQuarters" :key="q" :value="q">
                    {{ formatQuarter(q) }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-button type="primary" block :loading="downloading.vat" @click="handleDownloadVatReport">
                <DownloadOutlined /> 下载报表
              </a-button>
            </a-form>
          </div>
        </a-card>
      </a-col>

      <!-- 销售数据导出 -->
      <a-col :span="8">
        <a-card class="report-card" hoverable>
          <template #cover>
            <div class="card-icon sales">
              <ShoppingCartOutlined />
            </div>
          </template>
          <a-card-meta title="销售数据报表" description="导出指定时间段的销售明细数据" />
          <div class="card-actions">
            <a-form layout="vertical" :model="salesExportForm">
              <a-form-item label="站点">
                <a-select
                  v-model:value="salesExportForm.marketplaceId"
                  placeholder="全部站点"
                  allow-clear
                  style="width: 100%"
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
              <a-form-item label="日期范围" :required="true">
                <a-range-picker
                  v-model:value="salesExportForm.dateRange"
                  style="width: 100%"
                />
              </a-form-item>
              <a-form-item label="导出格式">
                <a-radio-group v-model:value="salesExportForm.format">
                  <a-radio-button value="EXCEL">Excel</a-radio-button>
                  <a-radio-button value="CSV">CSV</a-radio-button>
                </a-radio-group>
              </a-form-item>
              <a-button type="primary" block :loading="downloading.sales" @click="handleDownloadSalesReport">
                <DownloadOutlined /> 导出数据
              </a-button>
            </a-form>
          </div>
        </a-card>
      </a-col>

      <!-- 汇总报表导出 -->
      <a-col :span="8">
        <a-card class="report-card" hoverable>
          <template #cover>
            <div class="card-icon summary">
              <BarChartOutlined />
            </div>
          </template>
          <a-card-meta title="综合汇总报表" description="包含销售、费用、利润等多维度汇总数据" />
          <div class="card-actions">
            <a-form layout="vertical" :model="summaryExportForm">
              <a-form-item label="站点">
                <a-select
                  v-model:value="summaryExportForm.marketplaceId"
                  placeholder="全部站点"
                  allow-clear
                  style="width: 100%"
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
              <a-form-item label="年份">
                <a-date-picker
                  v-model:value="summaryExportForm.year"
                  picker="year"
                  placeholder="选择年份"
                  style="width: 100%"
                />
              </a-form-item>
              <a-form-item label="导出格式">
                <a-radio-group v-model:value="summaryExportForm.format">
                  <a-radio-button value="EXCEL">Excel</a-radio-button>
                  <a-radio-button value="PDF">PDF</a-radio-button>
                </a-radio-group>
              </a-form-item>
              <a-form-item>
                <a-checkbox v-model:checked="summaryExportForm.includeDetails">包含明细数据</a-checkbox>
              </a-form-item>
              <a-button type="primary" block :loading="downloading.summary" @click="handleDownloadSummaryReport">
                <DownloadOutlined /> 导出报表
              </a-button>
            </a-form>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 更多报表类型 -->
    <a-row :gutter="16" style="margin-top: 16px">
      <!-- 配送数据导出 -->
      <a-col :span="8">
        <a-card class="report-card" hoverable>
          <template #cover>
            <div class="card-icon shipping">
              <CarOutlined />
            </div>
          </template>
          <a-card-meta title="配送数据报表" description="导出指定时间段的配送明细数据" />
          <div class="card-actions">
            <a-form layout="vertical" :model="shippingExportForm">
              <a-form-item label="站点">
                <a-select
                  v-model:value="shippingExportForm.marketplaceId"
                  placeholder="全部站点"
                  allow-clear
                  style="width: 100%"
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
              <a-form-item label="日期范围" :required="true">
                <a-range-picker
                  v-model:value="shippingExportForm.dateRange"
                  style="width: 100%"
                />
              </a-form-item>
              <a-button type="primary" block :loading="downloading.shipping" @click="handleDownloadShippingReport">
                <DownloadOutlined /> 导出数据
              </a-button>
            </a-form>
          </div>
        </a-card>
      </a-col>

      <!-- 汇率数据导出 -->
      <a-col :span="8">
        <a-card class="report-card" hoverable>
          <template #cover>
            <div class="card-icon rate">
              <SwapOutlined />
            </div>
          </template>
          <a-card-meta title="汇率数据报表" description="导出指定时间段的汇率历史数据" />
          <div class="card-actions">
            <a-form layout="vertical" :model="rateExportForm">
              <a-form-item label="货币对">
                <a-input-group compact>
                  <a-select
                    v-model:value="rateExportForm.sourceCurrency"
                    placeholder="源货币"
                    style="width: 50%"
                  >
                    <a-select-option v-for="c in currencyOptions" :key="c.currencyCode" :value="c.currencyCode">
                      {{ c.currencyCode }}
                    </a-select-option>
                  </a-select>
                  <a-select
                    v-model:value="rateExportForm.targetCurrency"
                    placeholder="目标货币"
                    style="width: 50%"
                  >
                    <a-select-option v-for="c in currencyOptions" :key="c.currencyCode" :value="c.currencyCode">
                      {{ c.currencyCode }}
                    </a-select-option>
                  </a-select>
                </a-input-group>
              </a-form-item>
              <a-form-item label="日期范围">
                <a-range-picker
                  v-model:value="rateExportForm.dateRange"
                  style="width: 100%"
                />
              </a-form-item>
              <a-button type="primary" block :loading="downloading.rate" @click="handleDownloadRateReport">
                <DownloadOutlined /> 导出数据
              </a-button>
            </a-form>
          </div>
        </a-card>
      </a-col>

      <!-- 占位 -->
      <a-col :span="8">
        <a-card class="report-card placeholder" :bordered="false">
          <a-empty description="更多报表类型即将上线" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import {
  DownloadOutlined,
  FileExcelOutlined,
  ShoppingCartOutlined,
  BarChartOutlined,
  CarOutlined,
  SwapOutlined
} from '@ant-design/icons-vue'
import { exportVatReport, exportReport } from '@/api/report'
import { exportSalesData } from '@/api/sales'
import { exportShippingData } from '@/api/shipping'
import { exportRateData } from '@/api/rate'
import { getEnabledMarketplaces } from '@/api/marketplace'
import { getEnabledCurrencies } from '@/api/currency'
import type { Marketplace } from '@/types/marketplace'
import type { Currency } from '@/types/currency'

// ============= 选项数据 =============
const marketplaceOptions = ref<Marketplace[]>([])
const currencyOptions = ref<Currency[]>([])
const availableQuarters = ref<string[]>([])

// ============= 下载状态 =============
const downloading = reactive({
  vat: false,
  sales: false,
  summary: false,
  shipping: false,
  rate: false
})

// ============= 表单数据 =============
const vatReportForm = reactive({
  marketplaceId: undefined as number | undefined,
  quarter: ''
})

const salesExportForm = reactive({
  marketplaceId: undefined as number | undefined,
  dateRange: null as [Dayjs, Dayjs] | null,
  format: 'EXCEL' as 'EXCEL' | 'CSV'
})

const summaryExportForm = reactive({
  marketplaceId: undefined as number | undefined,
  year: null as Dayjs | null,
  format: 'EXCEL' as 'EXCEL' | 'PDF',
  includeDetails: false
})

const shippingExportForm = reactive({
  marketplaceId: undefined as number | undefined,
  dateRange: null as [Dayjs, Dayjs] | null
})

const rateExportForm = reactive({
  sourceCurrency: undefined as string | undefined,
  targetCurrency: undefined as string | undefined,
  dateRange: null as [Dayjs, Dayjs] | null
})

// ============= 方法 =============
function formatQuarter(quarter: string): string {
  const [year, q] = quarter.split('-Q')
  return `${year}年第${q}季度`
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
}

async function fetchOptions() {
  try {
    const [marketplaceRes, currencyRes] = await Promise.all([
      getEnabledMarketplaces(),
      getEnabledCurrencies()
    ])
    marketplaceOptions.value = marketplaceRes.data || []
    currencyOptions.value = currencyRes.data || []
    generateAvailableQuarters()
  } catch (error) {
    console.error('获取选项数据失败:', error)
  }
}

async function handleDownloadVatReport() {
  if (!vatReportForm.marketplaceId || !vatReportForm.quarter) {
    message.warning('请选择站点和季度')
    return
  }

  downloading.vat = true
  try {
    // 获取站点的siteCode
    const marketplace = marketplaceOptions.value.find(m => m.id === vatReportForm.marketplaceId)
    if (!marketplace) {
      message.error('站点信息不存在')
      return
    }
    await exportVatReport(marketplace.siteCode, vatReportForm.quarter)
    message.success('报表下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    message.error('下载失败')
  } finally {
    downloading.vat = false
  }
}

async function handleDownloadSalesReport() {
  if (!salesExportForm.dateRange) {
    message.warning('请选择日期范围')
    return
  }

  downloading.sales = true
  try {
    await exportSalesData({
      marketplaceId: salesExportForm.marketplaceId,
      startDate: salesExportForm.dateRange[0].format('YYYY-MM-DD'),
      endDate: salesExportForm.dateRange[1].format('YYYY-MM-DD')
    })
    message.success('数据导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    message.error('导出失败')
  } finally {
    downloading.sales = false
  }
}

async function handleDownloadSummaryReport() {
  downloading.summary = true
  try {
    await exportReport({
      marketplaceId: summaryExportForm.marketplaceId,
      year: summaryExportForm.year?.year(),
      format: summaryExportForm.format,
      includeDetails: summaryExportForm.includeDetails
    })
    message.success('报表下载成功')
  } catch (error) {
    console.error('下载失败:', error)
    message.error('下载失败')
  } finally {
    downloading.summary = false
  }
}

async function handleDownloadShippingReport() {
  if (!shippingExportForm.dateRange) {
    message.warning('请选择日期范围')
    return
  }

  downloading.shipping = true
  try {
    await exportShippingData({
      marketplaceId: shippingExportForm.marketplaceId,
      startDate: shippingExportForm.dateRange[0].format('YYYY-MM-DD'),
      endDate: shippingExportForm.dateRange[1].format('YYYY-MM-DD')
    })
    message.success('数据导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    message.error('导出失败')
  } finally {
    downloading.shipping = false
  }
}

async function handleDownloadRateReport() {
  downloading.rate = true
  try {
    await exportRateData({
      sourceCurrencyCode: rateExportForm.sourceCurrency,
      targetCurrencyCode: rateExportForm.targetCurrency,
      startDate: rateExportForm.dateRange?.[0]?.format('YYYY-MM-DD'),
      endDate: rateExportForm.dateRange?.[1]?.format('YYYY-MM-DD')
    })
    message.success('数据导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    message.error('导出失败')
  } finally {
    downloading.rate = false
  }
}

// 初始化
onMounted(() => {
  fetchOptions()
})
</script>

<style lang="scss" scoped>
.report-download-page {
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

  .report-card {
    height: 100%;

    .card-icon {
      height: 80px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40px;
      color: white;

      &.vat {
        background: linear-gradient(135deg, #52c41a 0%, #237804 100%);
      }

      &.sales {
        background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
      }

      &.summary {
        background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);
      }

      &.shipping {
        background: linear-gradient(135deg, #fa8c16 0%, #d46b08 100%);
      }

      &.rate {
        background: linear-gradient(135deg, #13c2c2 0%, #08979c 100%);
      }
    }

    .card-actions {
      margin-top: $spacing-md;
    }

    &.placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 400px;
      background: $background-color-light;
    }
  }
}
</style>
