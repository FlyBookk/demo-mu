<template>
  <div class="shipping-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">配送数据列表</h1>
      <p class="page-desc">查看和管理已导入的亚马逊配送数据</p>
    </div>

    <!-- 搜索栏 -->
    <a-card class="search-card">
      <a-form layout="inline" :model="searchForm">
        <a-row :gutter="16" style="width: 100%">
          <a-col :span="4">
            <a-form-item>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="订单号/发货单号/SKU"
                allow-clear
                @pressEnter="handleSearch"
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item>
              <a-select
                v-model:value="searchForm.siteCode"
                placeholder="站点"
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
            <a-form-item>
              <a-select
                v-model:value="searchForm.isOwnSite"
                placeholder="数据类型"
                allow-clear
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option :value="1">本站数据</a-select-option>
                <a-select-option :value="0">非本站数据</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item>
              <a-range-picker
                v-model:value="searchDateRange"
                style="width: 100%"
                @change="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item>
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
          <a-col :span="5" style="text-align: right">
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

    <!-- 统计汇总：核心指标突出，费用明细紧凑分组 -->
    <div class="stat-overview">
      <div class="stat-hero">
        <div class="stat-hero-item stat-hero-orders">
          <div class="stat-hero-label">总发货数</div>
          <div class="stat-hero-value">
            <SendOutlined class="stat-hero-icon" />
            {{ (summary.totalOrders ?? 0).toLocaleString() }}
          </div>
        </div>
        <div class="stat-hero-item stat-hero-total">
          <div class="stat-hero-label">总计费用 (CNY)</div>
          <div class="stat-hero-value">¥ {{ (summary.totalAmountCny ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}</div>
        </div>
      </div>
      <div class="stat-fee-breakdown">
        <div class="stat-fee-title">费用构成（8项之和）</div>
        <div class="stat-fee-grid">
          <div class="stat-fee-item">
            <span class="stat-fee-label">商品价格</span>
            <span class="stat-fee-value">¥ {{ (summary.totalProductPriceCny ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
          <div class="stat-fee-item">
            <span class="stat-fee-label">商品税</span>
            <span class="stat-fee-value">¥ {{ (summary.totalProductTaxCny ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
          <div class="stat-fee-item">
            <span class="stat-fee-label">运费</span>
            <span class="stat-fee-value">¥ {{ (summary.totalShippingPriceCny ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
          <div class="stat-fee-item">
            <span class="stat-fee-label">运费税</span>
            <span class="stat-fee-value">¥ {{ (summary.totalShippingTaxCny ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
          <div class="stat-fee-item">
            <span class="stat-fee-label">礼品包装价格</span>
            <span class="stat-fee-value">¥ {{ (summary.totalGiftWrapPriceCny ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
          <div class="stat-fee-item">
            <span class="stat-fee-label">礼品包装税费</span>
            <span class="stat-fee-value">¥ {{ (summary.totalGiftWrapTaxCny ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
          <div class="stat-fee-item stat-fee-discount">
            <span class="stat-fee-label">商品促销折扣</span>
            <span class="stat-fee-value">¥ {{ (summary.totalProductPromotionDiscountCny ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
          <div class="stat-fee-item stat-fee-discount">
            <span class="stat-fee-label">货件促销折扣</span>
            <span class="stat-fee-value">¥ {{ (summary.totalShipmentPromotionDiscountCny ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 2050 }"
        row-key="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 订单号 -->
          <template v-if="column.key === 'orderId'">
            <a-typography-text copyable :content="record.orderId">
              {{ record.orderId }}
            </a-typography-text>
          </template>

          <!-- 站点 -->
          <template v-else-if="column.key === 'siteCode'">
            <a-tag color="blue">{{ record.siteCode }}</a-tag>
          </template>

          <!-- 数据类型 -->
          <template v-else-if="column.key === 'isOwnSite'">
            <a-tag :color="record.isOwnSite === 1 ? 'green' : 'orange'">
              {{ record.isOwnSite === 1 ? '本站' : '非本站' }}
            </a-tag>
          </template>

          <!-- 总计费用 -->
          <template v-else-if="column.key === 'totalAmount'">
            <span class="amount">{{ formatAmount(record.totalAmount, record.currencyCode) }}</span>
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
      title="配送数据详情"
      width="750px"
      :footer="null"
    >
      <a-descriptions v-if="detailData" :column="2" bordered size="small">
        <a-descriptions-item label="订单号" :span="2">
          <a-typography-text copyable>{{ detailData.orderId }}</a-typography-text>
        </a-descriptions-item>
        <a-descriptions-item label="站点">{{ detailData.siteCode }}</a-descriptions-item>
        <a-descriptions-item label="Marketplace">{{ detailData.marketplace }}</a-descriptions-item>
        <a-descriptions-item label="发货日期">{{ detailData.shipDate }}</a-descriptions-item>
        <a-descriptions-item label="货币">{{ detailData.currencyCode }}</a-descriptions-item>
        <a-descriptions-item label="SKU">{{ detailData.sku || '-' }}</a-descriptions-item>
        <a-descriptions-item label="数量">{{ detailData.quantity || 0 }}</a-descriptions-item>
        <a-descriptions-item label="商品价格">{{ formatAmountValue(detailData.productPrice) }}</a-descriptions-item>
        <a-descriptions-item label="商品税">{{ formatAmountValue(detailData.productTax) }}</a-descriptions-item>
        <a-descriptions-item label="运费">{{ formatAmountValue(detailData.shippingPrice) }}</a-descriptions-item>
        <a-descriptions-item label="运费税">{{ formatAmountValue(detailData.shippingTax) }}</a-descriptions-item>
        <a-descriptions-item label="礼品包装价格">{{ formatAmountValue(detailData.giftWrapPrice) }}</a-descriptions-item>
        <a-descriptions-item label="礼品包装税">{{ formatAmountValue(detailData.giftWrapTax) }}</a-descriptions-item>
        <a-descriptions-item label="商品促销折扣">{{ formatAmountValue(detailData.productPromotionDiscount) }}</a-descriptions-item>
        <a-descriptions-item label="货件促销折扣">{{ formatAmountValue(detailData.shipmentPromotionDiscount) }}</a-descriptions-item>
        <a-descriptions-item label="物流费用">{{ formatAmountValue(detailData.shippingCost) }}</a-descriptions-item>
        <a-descriptions-item label="总计费用">
          <span class="highlight-amount">{{ formatAmountValue(detailData.totalAmount) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="承运商">{{ detailData.carrier || '-' }}</a-descriptions-item>
        <a-descriptions-item label="物流单号">{{ detailData.trackingNumber || '-' }}</a-descriptions-item>
        <a-descriptions-item label="汇率">{{ detailData.exchangeRate || '-' }}</a-descriptions-item>
        <a-descriptions-item label="汇率取值日期">{{ detailData.exchangeRateDate || '-' }}</a-descriptions-item>
        <a-descriptions-item label="导入时间" :span="2">{{ detailData.createTime }}</a-descriptions-item>
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
  SendOutlined
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/modules/auth'
import {
  getShippingList,
  getShippingById,
  getShippingSummary,
  deleteShippingData,
  batchDeleteShippingData,
  batchPhysicalDeleteShippingData,
  exportShippingData
} from '@/api/shipping'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { ShippingData, ShippingSummary } from '@/types/shipping'
import type { Marketplace } from '@/types/marketplace'

const router = useRouter()
const authStore = useAuthStore()

function formatAmount(amount: number | null | undefined, currency: string): string {
  const value = amount ?? 0
  return `${currency || ''} ${value.toFixed(2)}`
}

/** 仅格式化金额数值（详情页已有货币字段，无需重复展示） */
function formatAmountValue(amount: number | null | undefined): string {
  return (amount ?? 0).toFixed(2)
}

// ============= 搜索相关 =============
const searchForm = reactive({
  keyword: '',
  siteCode: undefined as string | undefined,
  isOwnSite: undefined as number | undefined
})
const searchDateRange = ref<[Dayjs, Dayjs] | null>(null)
const marketplaceOptions = ref<Marketplace[]>([])

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<ShippingData[]>([])
const selectedRowKeys = ref<number[]>([])

// ============= 统计数据（人民币汇总） =============
const summary = reactive<ShippingSummary>({
  totalOrders: 0,
  totalQuantity: 0,
  totalProductPriceCny: 0,
  totalProductTaxCny: 0,
  totalShippingPriceCny: 0,
  totalShippingTaxCny: 0,
  totalGiftWrapPriceCny: 0,
  totalGiftWrapTaxCny: 0,
  totalProductPromotionDiscountCny: 0,
  totalShipmentPromotionDiscountCny: 0,
  totalAmountCny: 0,
  totalShippingCostCny: 0,
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
    title: '站点',
    dataIndex: 'siteCode',
    key: 'siteCode',
    width: 80
  },
  {
    title: '数据类型',
    dataIndex: 'isOwnSite',
    key: 'isOwnSite',
    width: 90
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
    title: '商品价格',
    dataIndex: 'productPrice',
    key: 'productPrice',
    width: 100,
    align: 'right' as const
  },
  {
    title: '商品税',
    dataIndex: 'productTax',
    key: 'productTax',
    width: 90,
    align: 'right' as const
  },
  {
    title: '运费',
    dataIndex: 'shippingPrice',
    key: 'shippingPrice',
    width: 100,
    align: 'right' as const
  },
  {
    title: '运费税',
    dataIndex: 'shippingTax',
    key: 'shippingTax',
    width: 90,
    align: 'right' as const
  },
  {
    title: '礼品包装价格',
    dataIndex: 'giftWrapPrice',
    key: 'giftWrapPrice',
    width: 110,
    align: 'right' as const
  },
  {
    title: '礼品包装税费',
    dataIndex: 'giftWrapTax',
    key: 'giftWrapTax',
    width: 110,
    align: 'right' as const
  },
  {
    title: '商品促销折扣',
    dataIndex: 'productPromotionDiscount',
    key: 'productPromotionDiscount',
    width: 110,
    align: 'right' as const
  },
  {
    title: '货件促销折扣',
    dataIndex: 'shipmentPromotionDiscount',
    key: 'shipmentPromotionDiscount',
    width: 110,
    align: 'right' as const
  },
  {
    title: '总计费用',
    dataIndex: 'totalAmount',
    key: 'totalAmount',
    width: 110,
    align: 'right' as const
  },
  {
    title: '货币',
    dataIndex: 'currencyCode',
    key: 'currencyCode',
    width: 80
  },
  {
    title: '发货日期',
    dataIndex: 'shipDate',
    key: 'shipDate',
    width: 120
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
    title: '承运商',
    dataIndex: 'carrier',
    key: 'carrier',
    width: 100
  },
  {
    title: '物流单号',
    dataIndex: 'trackingNumber',
    key: 'trackingNumber',
    width: 150,
    ellipsis: true
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 160
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
  pageSize: 20,
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
const detailData = ref<ShippingData | null>(null)

// ============= 方法 =============
async function fetchMarketplaces() {
  try {
    const res = await getEnabledMarketplaces()
    marketplaceOptions.value = res.data || []
  } catch (error) {
    console.error('获取站点列表失败:', error)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      orderId: searchForm.keyword || undefined,
      trackingNumber: undefined as string | undefined,
      siteCode: searchForm.siteCode,
      isOwnSite: searchForm.isOwnSite,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD'),
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getShippingList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取配送数据列表失败:', error)
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  try {
    const params = {
      siteCode: searchForm.siteCode,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD')
    }
    const res = await getShippingSummary(params)
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
  searchForm.siteCode = undefined
  searchForm.isOwnSite = undefined
  searchDateRange.value = null
  pagination.current = 1
  fetchData()
  fetchSummary()
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  fetchData()
}

function handleGoImport() {
  router.push('/shipping/import')
}

async function handleExport() {
  try {
    const params = {
      siteCode: searchForm.siteCode,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD')
    }
    await exportShippingData(params)
    message.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
  }
}

async function handleViewDetail(record: ShippingData) {
  try {
    const res = await getShippingById(record.id)
    detailData.value = res.data
    detailModalVisible.value = true
  } catch (error) {
    console.error('获取详情失败:', error)
  }
}

async function handleDelete(record: ShippingData) {
  try {
    // Admin 用户使用物理删除，普通用户使用逻辑删除
    if (authStore.isAdmin) {
      await batchPhysicalDeleteShippingData([record.id])
    } else {
      await deleteShippingData(record.id)
    }
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
        // Admin 用户使用物理删除，普通用户使用逻辑删除
        if (authStore.isAdmin) {
          await batchPhysicalDeleteShippingData(selectedRowKeys.value)
          message.success('批量删除成功')
        } else {
          await batchDeleteShippingData(selectedRowKeys.value)
          message.success('批量删除成功')
        }
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
  fetchData()
  fetchSummary()
})
</script>

<style lang="scss" scoped>
.shipping-list-page {
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
  }

  .stat-overview {
    margin-bottom: $spacing-lg;
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
    border-radius: $border-radius-lg;
    padding: $spacing-lg;
    border: 1px solid $border-color-light;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  }

  .stat-hero {
    display: flex;
    gap: $spacing-xl;
    margin-bottom: $spacing-lg;
  }

  .stat-hero-item {
    flex: 1;
    padding: $spacing-lg $spacing-xl;
    border-radius: $border-radius-md;
    min-width: 0;
  }

  .stat-hero-orders {
    background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
    color: #fff;
    box-shadow: 0 4px 12px rgba(24, 144, 255, 0.25);
  }

  .stat-hero-total {
    background: linear-gradient(135deg, #2f54eb 0%, #1d39c4 100%);
    color: #fff;
    box-shadow: 0 4px 12px rgba(47, 84, 235, 0.25);
  }

  .stat-hero-label {
    font-size: $font-size-sm;
    opacity: 0.9;
    margin-bottom: $spacing-xs;
  }

  .stat-hero-value {
    font-size: 28px;
    font-weight: 600;
    letter-spacing: -0.02em;
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }

  .stat-hero-icon {
    font-size: 24px;
    opacity: 0.9;
  }

  .stat-fee-breakdown {
    background: #fff;
    border-radius: $border-radius-md;
    padding: $spacing-md $spacing-lg;
    border: 1px solid $border-color-light;
  }

  .stat-fee-title {
    font-size: $font-size-sm;
    color: $text-color-secondary;
    margin-bottom: $spacing-md;
    font-weight: 500;
  }

  .stat-fee-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: $spacing-md $spacing-xl;
  }

  .stat-fee-item {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    padding: $spacing-sm 0;

    &:nth-child(-n+4) {
      border-bottom: 1px dashed $border-color-light;
    }
  }

  .stat-fee-label {
    font-size: $font-size-sm;
    color: $text-color-secondary;
  }

  .stat-fee-value {
    font-size: $font-size-md;
    font-weight: 500;
    color: $text-color;
  }

  .stat-fee-discount .stat-fee-value {
    color: #cf1322;
  }

  .amount {
    font-weight: 500;
  }

  .highlight-amount {
    font-size: $font-size-lg;
    font-weight: 600;
    color: $primary-color;
  }
}
</style>
