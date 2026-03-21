<template>
  <div class="sales-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">销售数据列表</h1>
      <p class="page-desc">查看和管理已导入的亚马逊销售数据</p>
    </div>

    <!-- 搜索栏 -->
    <a-card class="search-card">
      <a-form :model="searchForm">
        <!-- 第一行：搜索条件 -->
        <a-row :gutter="16">
          <a-col :span="3">
            <a-form-item label="数据归属">
              <a-select
                v-model:value="searchForm.isOwnSite"
                placeholder="全部"
                allow-clear
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option :value="1">本站</a-select-option>
                <a-select-option :value="0">非本站</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item label="关键字">
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="订单号/SKU/ASIN"
                allow-clear
                @pressEnter="handleSearch"
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="结算ID">
              <a-input
                v-model:value="searchForm.settlementId"
                placeholder="请输入结算ID"
                allow-clear
                @pressEnter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item label="数据来源">
              <a-select
                v-model:value="searchForm.sourceType"
                placeholder="全部"
                allow-clear
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option value="ORIGINAL">原始数据</a-select-option>
                <a-select-option value="ERP">ERP结算</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item label="站点">
              <a-select
                v-model:value="searchForm.siteCode"
                placeholder="全部"
                allow-clear
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option
                  v-for="marketplace in marketplaceOptions"
                  :key="marketplace.siteCode"
                  :value="marketplace.siteCode"
                >
                  {{ marketplace.siteCode }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item label="分类">
              <a-select
                v-model:value="searchForm.transactionCategory"
                placeholder="全部"
                allow-clear
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option
                  v-for="item in transactionCategoryOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item label="日期">
              <a-range-picker
                v-model:value="searchDateRange"
                style="width: 100%"
                @change="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :span="2">
            <a-form-item label=" " :colon="false">
              <a-space>
                <a-button type="primary" @click="handleSearch">
                  <SearchOutlined /> 查询
                </a-button>
                <a-button @click="handleReset">
                  <ReloadOutlined /> 重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
        <!-- 第二行：操作按钮 -->
        <a-row>
          <a-col :span="24" style="text-align: right">
            <a-space>
              <a-button type="primary" @click="handleGoImport">
                <CloudUploadOutlined /> 导入数据
              </a-button>
              <a-button @click="handleExport">
                <DownloadOutlined /> 导出
              </a-button>
              <a-button
                danger
                :disabled="selectedRowKeys.length === 0"
                @click="handleBatchDelete"
              >
                <DeleteOutlined /> 批量删除
              </a-button>
            </a-space>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 统计卡片（按汇率换算为人民币汇总） -->
    <a-row :gutter="16" class="stat-row">
      <a-col :flex="1">
        <a-card class="stat-card">
          <a-statistic title="总订单数" :value="summary.totalOrders" :value-style="{ color: '#1890ff' }">
            <template #prefix><ShoppingOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :flex="1">
        <a-card class="stat-card">
          <a-statistic title="产品销售(CNY)" :value="summary.totalProductSalesCny" :precision="2" :value-style="{ color: '#52c41a' }">
            <template #prefix>¥</template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :flex="1">
        <a-card class="stat-card">
          <a-statistic title="销售费用(CNY)" :value="summary.totalSellingFeesCny" :precision="2" :value-style="{ color: '#faad14' }">
            <template #prefix>¥</template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :flex="1">
        <a-card class="stat-card">
          <a-statistic title="FBA费用(CNY)" :value="summary.totalFbaFeesCny" :precision="2" :value-style="{ color: '#ff4d4f' }">
            <template #prefix>¥</template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :flex="1">
        <a-card class="stat-card">
          <a-statistic title="合计(CNY)" :value="summary.totalAmountCny" :precision="2" :value-style="{ color: summary.totalAmountCny >= 0 ? '#1890ff' : '#ff4d4f' }">
            <template #prefix>¥</template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- 数据表格 -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: TABLE_SCROLL_WIDTH }"
        row-key="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 订单号：悬停显示完整内容，带复制按钮 -->
          <template v-if="column.key === 'orderId'">
            <div class="ellipsis-with-copy">
              <a-tooltip :title="record.orderId">
                <span class="ellipsis-text">{{ record.orderId || '-' }}</span>
              </a-tooltip>
              <a-tooltip title="复制订单号">
                <a-button
                  type="text"
                  size="small"
                  class="copy-btn"
                  @click.stop="handleCopyOrderId(record.orderId)"
                >
                  <CopyOutlined />
                </a-button>
              </a-tooltip>
            </div>
          </template>

          <!-- 结算ID：悬停显示完整内容，带复制按钮 -->
          <template v-else-if="column.key === 'settlementId'">
            <div class="ellipsis-with-copy">
              <a-tooltip :title="record.settlementId || '-'">
                <span class="ellipsis-text">{{ record.settlementId || '-' }}</span>
              </a-tooltip>
              <a-tooltip title="复制结算ID">
                <a-button
                  type="text"
                  size="small"
                  class="copy-btn"
                  @click.stop="handleCopyOrderId(record.settlementId)"
                >
                  <CopyOutlined />
                </a-button>
              </a-tooltip>
            </div>
          </template>

          <!-- 数据来源 -->
          <template v-else-if="column.key === 'sourceType'">
            <a-tag :color="record.sourceType === 'ERP' ? 'purple' : 'cyan'">
              {{ record.sourceType === 'ERP' ? 'ERP结算' : '原始数据' }}
            </a-tag>
          </template>

          <!-- 数据归属 -->
          <template v-else-if="column.key === 'isOwnSite'">
            <a-tag :color="record.isOwnSite === 0 ? 'red' : 'green'">
              {{ record.isOwnSite === 0 ? '非本站' : '本站' }}
            </a-tag>
          </template>

          <!-- 站点 -->
          <template v-else-if="column.key === 'siteCode'">
            <a-tag color="blue">{{ record.siteCode }}</a-tag>
          </template>

          <!-- 交易分类 -->
          <template v-else-if="column.key === 'transactionCategory'">
            <a-tag :color="getTransactionCategoryColor(record.transactionCategory)">
              {{ getTransactionCategoryLabel(record.transactionCategory) }}
            </a-tag>
          </template>

          <!-- 交易说明（从映射表获取） -->
          <template v-else-if="column.key === 'categoryDesc'">
            {{ getCategoryDesc(record.transactionType) }}
          </template>

          <!-- 交易日期 -->
          <template v-else-if="column.key === 'transactionDate'">
            {{ formatDate(record.transactionDate) }}
          </template>

          <!-- 合计金额 -->
          <template v-else-if="column.key === 'total'">
            <span :class="record.total >= 0 ? 'amount-positive' : 'amount-negative'">
              {{ formatAmount(record.total) }}
            </span>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleViewDetail(record)">
                详情
              </a-button>
              <a-popconfirm
                title="确定要删除该记录吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailModalVisible"
      title="销售数据详情"
      width="800px"
      :footer="null"
    >
      <a-descriptions v-if="detailData" :column="2" bordered size="small">
        <a-descriptions-item label="订单号" :span="2">
          <a-typography-text copyable>{{ detailData.orderId }}</a-typography-text>
        </a-descriptions-item>
        <a-descriptions-item label="数据来源">
          <a-tag :color="detailData.sourceType === 'ERP' ? 'purple' : 'cyan'">
            {{ detailData.sourceType === 'ERP' ? 'ERP结算数据' : '亚马逊原始数据' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="数据归属">
          <a-tag :color="detailData.isOwnSite === 0 ? 'red' : 'green'">
            {{ detailData.isOwnSite === 0 ? '非本站' : '本站' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="站点">{{ detailData.siteCode }}</a-descriptions-item>
        <a-descriptions-item label="Marketplace">{{ detailData.marketplace }}</a-descriptions-item>
        <a-descriptions-item label="结算日期">{{ formatDate(detailData.transactionDate) }}</a-descriptions-item>
        <a-descriptions-item label="结算编号">{{ detailData.settlementId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="原始交易类型">{{ detailData.transactionType }}</a-descriptions-item>
        <a-descriptions-item label="交易分类">
          <a-tag :color="getTransactionCategoryColor(detailData.transactionCategory)">
            {{ getTransactionCategoryLabel(detailData.transactionCategory) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="SKU">{{ detailData.sku || '-' }}</a-descriptions-item>
        <a-descriptions-item label="数量">{{ detailData.quantity || 0 }}</a-descriptions-item>
        <a-descriptions-item label="描述" :span="2">
          <div v-if="detailData.description" class="detail-description">{{ detailData.description }}</div>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item label="货币">{{ detailData.currencyCode }}</a-descriptions-item>
        <a-descriptions-item label="配送方式">{{ detailData.fulfillment || '-' }}</a-descriptions-item>
        <!-- 收入类金额 -->
        <a-descriptions-item label="产品销售">{{ formatAmount(detailData.productSales) }}</a-descriptions-item>
        <a-descriptions-item label="产品税">{{ formatAmount(detailData.productSalesTax) }}</a-descriptions-item>
        <a-descriptions-item label="运费支出">{{ formatAmount(detailData.shippingCredits) }}</a-descriptions-item>
        <a-descriptions-item label="运费税">{{ formatAmount(detailData.shippingCreditsTax) }}</a-descriptions-item>
        <a-descriptions-item label="礼品包装费">{{ formatAmount(detailData.giftWrapCredits) }}</a-descriptions-item>
        <a-descriptions-item label="礼品包装税">{{ formatAmount(detailData.giftWrapCreditsTax) }}</a-descriptions-item>
        <a-descriptions-item label="监管费">{{ formatAmount(detailData.regulatoryFee) }}</a-descriptions-item>
        <a-descriptions-item label="监管费税">{{ formatAmount(detailData.regulatoryFeeTax) }}</a-descriptions-item>
        <a-descriptions-item label="促销折扣">{{ formatAmount(detailData.promotionalRebates) }}</a-descriptions-item>
        <a-descriptions-item label="促销折扣税">{{ formatAmount(detailData.promotionalRebatesTax) }}</a-descriptions-item>
        <!-- 平台代扣税 -->
        <a-descriptions-item label="平台代扣税" :span="2">
          <span class="warning-amount">{{ formatAmount(detailData.marketplaceWithheldTax) }}</span>
        </a-descriptions-item>
        <!-- 费用类金额 -->
        <a-descriptions-item label="销售费用">{{ formatAmount(detailData.sellingFees) }}</a-descriptions-item>
        <a-descriptions-item label="FBA费用">{{ formatAmount(detailData.fbaFees) }}</a-descriptions-item>
        <a-descriptions-item label="其他交易费">{{ formatAmount(detailData.otherTransactionFees) }}</a-descriptions-item>
        <a-descriptions-item label="其他">{{ formatAmount(detailData.other) }}</a-descriptions-item>
        <!-- 合计 -->
        <a-descriptions-item label="合计" :span="2">
          <span class="highlight-amount">{{ formatAmount(detailData.total) }}</span>
        </a-descriptions-item>
        <!-- 汇率信息 -->
        <a-descriptions-item label="汇率">{{ detailData.exchangeRate ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="汇率取值日期">{{ detailData.exchangeRateDate ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="导入时间">{{ detailData.createTime }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { TableRowSelection } from 'ant-design-vue/es/table/interface'
import type { Dayjs } from 'dayjs'
import {
  SearchOutlined,
  ReloadOutlined,
  CloudUploadOutlined,
  DownloadOutlined,
  DeleteOutlined,
  ShoppingOutlined,
  CopyOutlined
} from '@ant-design/icons-vue'

// ============= 常量配置 =============
const DEFAULT_PAGE_SIZE = 20
const TABLE_SCROLL_WIDTH = 2000
import {
  getSalesList,
  getSalesById,
  getSalesSummary,
  deleteSalesData,
  batchDeleteSalesData,
  exportSalesData
} from '@/api/sales'
import { getEnabledMarketplaces } from '@/api/marketplace'
import { getTransactionTypeMappingList, getTransactionCategories } from '@/api/transactionType'
import type { SalesData, SalesSummary } from '@/types/sales'
import type { Marketplace } from '@/types/marketplace'
import type { TransactionTypeMapping } from '@/types/transactionType'

const router = useRouter()

// ============= 交易分类配置（从后端动态获取） =============
const CATEGORY_COLOR_MAP: Record<string, string> = {
  income: 'green',
  refund: 'red',
  fee: 'orange',
  adjustment: 'blue',
  transfer: 'purple',
  other: 'default'
}

interface CategoryOption {
  value: string
  label: string
}

const transactionCategoryOptions = ref<CategoryOption[]>([])

// 交易类型映射缓存（originalType -> categoryDesc）
const transactionTypeMappingCache = ref<Map<string, string>>(new Map())

function getTransactionCategoryColor(category?: string): string {
  return CATEGORY_COLOR_MAP[category || ''] || 'default'
}

function getTransactionCategoryLabel(category?: string): string {
  return transactionCategoryOptions.value.find(t => t.value === category)?.label || category || '-'
}

// 根据交易类型获取分类说明
function getCategoryDesc(transactionType?: string): string {
  if (!transactionType) return '-'
  return transactionTypeMappingCache.value.get(transactionType) || transactionType
}

function formatAmount(amount: number | null | undefined): string {
  const value = amount ?? 0
  return value.toFixed(2)
}

// 格式化日期（yyyy-MM-dd HH:mm:ss -> yyyy-MM-dd HH:mm）
function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  // 处理 ISO 格式或带T的格式
  const normalized = dateStr.replace('T', ' ')
  // 只取到分钟，去掉秒
  const match = normalized.match(/^(\d{4}-\d{2}-\d{2})\s*(\d{2}:\d{2})/)
  if (match) {
    return `${match[1]} ${match[2]}`
  }
  return dateStr
}

// 复制到剪贴板
async function handleCopyOrderId(text: string | null | undefined) {
  const value = text || ''
  if (!value) {
    message.warning('无内容可复制')
    return
  }
  try {
    await navigator.clipboard.writeText(value)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败，请手动复制')
  }
}

// ============= 搜索相关 =============
const searchForm = reactive({
  keyword: '',
  sourceType: undefined as string | undefined,
  siteCode: undefined as string | undefined,
  transactionCategory: undefined as string | undefined,
  settlementId: '',
  isOwnSite: undefined as number | undefined
})
const searchDateRange = ref<[Dayjs, Dayjs] | null>(null)
const marketplaceOptions = ref<Marketplace[]>([])

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<SalesData[]>([])
const selectedRowKeys = ref<number[]>([])

// ============= 统计数据（人民币汇总） =============
const summary = reactive<SalesSummary>({
  totalOrders: 0,
  totalQuantity: 0,
  totalProductSalesCny: 0,
  totalSellingFeesCny: 0,
  totalFbaFeesCny: 0,
  totalOtherFeesCny: 0,
  totalAmountCny: 0,
  currencyCode: 'CNY'
})

const columns = [
  {
    title: '订单号',
    dataIndex: 'orderId',
    key: 'orderId',
    width: 180,
    fixed: 'left' as const,
    ellipsis: true
  },
  {
    title: '结算ID',
    dataIndex: 'settlementId',
    key: 'settlementId',
    width: 130,
    ellipsis: true
  },
  {
    title: '数据来源',
    dataIndex: 'sourceType',
    key: 'sourceType',
    width: 100
  },
  {
    title: '数据归属',
    dataIndex: 'isOwnSite',
    key: 'isOwnSite',
    width: 90
  },
  {
    title: '站点',
    dataIndex: 'siteCode',
    key: 'siteCode',
    width: 80
  },
  {
    title: '交易分类',
    dataIndex: 'transactionCategory',
    key: 'transactionCategory',
    width: 100
  },
  {
    title: '交易说明',
    dataIndex: 'transactionType',
    key: 'categoryDesc',
    width: 150,
    ellipsis: true
  },
  {
    title: '结算日期',
    dataIndex: 'transactionDate',
    key: 'transactionDate',
    width: 120
  },
  {
    title: 'SKU',
    dataIndex: 'sku',
    key: 'sku',
    width: 120,
    ellipsis: true
  },
  {
    title: '数量',
    dataIndex: 'quantity',
    key: 'quantity',
    width: 70,
    align: 'right' as const
  },
  {
    title: '产品销售',
    dataIndex: 'productSales',
    key: 'productSales',
    width: 100,
    align: 'right' as const
  },
  {
    title: '销售费用',
    dataIndex: 'sellingFees',
    key: 'sellingFees',
    width: 100,
    align: 'right' as const
  },
  {
    title: 'FBA费用',
    dataIndex: 'fbaFees',
    key: 'fbaFees',
    width: 100,
    align: 'right' as const
  },
  {
    title: '合计',
    dataIndex: 'total',
    key: 'total',
    width: 120,
    align: 'right' as const
  },
  {
    title: '货币',
    dataIndex: 'currencyCode',
    key: 'currencyCode',
    width: 80
  },
  {
    title: '汇率',
    dataIndex: 'exchangeRate',
    key: 'exchangeRate',
    width: 90,
    align: 'right' as const
  },
  {
    title: '汇率取值日期',
    dataIndex: 'exchangeRateDate',
    key: 'exchangeRateDate',
    width: 120
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    fixed: 'right' as const
  }
]

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: DEFAULT_PAGE_SIZE,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 行选择配置
const rowSelection = computed<TableRowSelection>(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => {
    selectedRowKeys.value = keys as number[]
  }
}))

// ============= 详情弹窗 =============
const detailModalVisible = ref(false)
const detailData = ref<SalesData | null>(null)

// ============= 方法 =============
async function fetchMarketplaces() {
  try {
    const res = await getEnabledMarketplaces()
    marketplaceOptions.value = res.data || []
  } catch (error) {
    console.error('获取站点列表失败:', error)
  }
}

// 获取交易分类选项
async function fetchCategories() {
  try {
    const res = await getTransactionCategories()
    transactionCategoryOptions.value = res.data || []
  } catch (error) {
    console.error('获取交易分类失败:', error)
  }
}

// 获取交易类型映射并缓存
async function fetchTransactionTypeMappings() {
  try {
    const res = await getTransactionTypeMappingList({ status: 1, size: 1000 })
    const mappings = res.data?.records || []
    const cache = new Map<string, string>()
    mappings.forEach((m: TransactionTypeMapping) => {
      if (m.originalType && m.categoryDesc) {
        cache.set(m.originalType, m.categoryDesc)
      }
    })
    transactionTypeMappingCache.value = cache
  } catch (error) {
    console.error('获取交易类型映射失败:', error)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      keyword: searchForm.keyword || undefined,
      sourceType: searchForm.sourceType,
      siteCode: searchForm.siteCode,
      settlementId: searchForm.settlementId || undefined,
      transactionCategory: searchForm.transactionCategory,
      isOwnSite: searchForm.isOwnSite,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD'),
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getSalesList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取销售数据列表失败:', error)
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  try {
    const params = {
      keyword: searchForm.keyword || undefined,
      sourceType: searchForm.sourceType,
      siteCode: searchForm.siteCode,
      settlementId: searchForm.settlementId || undefined,
      transactionCategory: searchForm.transactionCategory,
      isOwnSite: searchForm.isOwnSite,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD')
    }
    const res = await getSalesSummary(params)
    Object.assign(summary, res.data)
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
  fetchSummary()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.sourceType = undefined
  searchForm.siteCode = undefined
  searchForm.transactionCategory = undefined
  searchForm.settlementId = ''
  searchForm.isOwnSite = undefined
  searchDateRange.value = null
  pagination.current = 1
  fetchData()
  fetchSummary()
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || DEFAULT_PAGE_SIZE
  fetchData()
}

function handleGoImport() {
  router.push('/sales/import')
}

async function handleExport() {
  try {
    const params = {
      siteCode: searchForm.siteCode,
      transactionCategory: searchForm.transactionCategory,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD')
    }
    await exportSalesData(params)
    message.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
  }
}

async function handleViewDetail(record: SalesData) {
  try {
    const res = await getSalesById(record.id)
    detailData.value = res.data
    detailModalVisible.value = true
  } catch (error) {
    console.error('获取详情失败:', error)
  }
}

async function handleDelete(record: SalesData) {
  try {
    await deleteSalesData(record.id)
    message.success('删除成功')
    fetchData()
    fetchSummary()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

function handleBatchDelete() {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 条记录吗？`,
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await batchDeleteSalesData(selectedRowKeys.value)
        message.success('批量删除成功')
        selectedRowKeys.value = []
        fetchData()
        fetchSummary()
      } catch (error) {
        console.error('批量删除失败:', error)
      }
    }
  })
}

// 初始化
onMounted(() => {
  fetchMarketplaces()
  fetchCategories()
  fetchTransactionTypeMappings()
  fetchData()
  fetchSummary()
})
</script>

<style lang="scss" scoped>
.sales-list-page {
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

  .search-card {
    margin-bottom: $spacing-md;
    
    :deep(.ant-form-item) {
      margin-bottom: 12px;
    }
    
    :deep(.ant-form-item-label) {
      padding-bottom: 4px;
    }
  }

  .stat-row {
    margin-bottom: $spacing-md;

    .stat-card {
      text-align: center;
    }
  }

  .amount-positive {
    color: $success-color;
  }

  .amount-negative {
    color: $error-color;
  }

  .highlight-amount {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $primary-color;
  }

  .warning-amount {
    font-weight: 500;
    color: $warning-color;
  }

  .detail-description {
    word-break: break-word;
    white-space: pre-wrap;
    line-height: 1.5;
    max-width: 100%;
  }

  .ellipsis-with-copy {
    display: flex;
    align-items: center;
    gap: 4px;
    min-width: 0;

    .ellipsis-text {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .copy-btn {
      flex-shrink: 0;
      padding: 0 4px;
      color: $text-color-secondary;
      &:hover {
        color: $primary-color;
      }
    }
  }
}
</style>
