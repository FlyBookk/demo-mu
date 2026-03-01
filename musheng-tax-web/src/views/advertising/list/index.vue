<template>
  <div class="advertising-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">广告数据列表</h1>
      <p class="page-desc">查看和管理已录入的广告费用数据</p>
    </div>

    <!-- 搜索栏 -->
    <a-card class="search-card">
      <a-form layout="inline" :model="searchForm">
        <a-row :gutter="16" style="width: 100%">
          <a-col :span="4">
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
          <a-col :span="6">
            <a-form-item>
              <a-range-picker
                v-model:value="searchForm.billingPeriod"
                placeholder="['开始日期', '结束日期']"
                format="YYYY-MM-DD"
                style="width: 100%"
                @change="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item>
              <a-input
                v-model:value="searchForm.invoiceNumber"
                placeholder="发票编号"
                allow-clear
                style="width: 100%"
                @press-enter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :span="9">
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
        </a-row>
        <a-row :gutter="16" style="width: 100%; margin-top: 8px">
          <a-col :span="24" style="text-align: right">
            <a-space>
              <a-button @click="handleGoToDetailView">
                <UnorderedListOutlined /> 活动明细视图
              </a-button>
              <a-button type="primary" @click="handleImport">
                <PlusOutlined /> 批量导入
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

    <!-- 统计汇总 -->
    <a-row :gutter="16" class="stat-row">
      <a-col :flex="1">
        <a-card class="stat-card" size="small">
          <a-statistic title="发票数量" :value="summary.invoiceCount" :value-style="{ color: '#1890ff' }" />
        </a-card>
      </a-col>
      <a-col :flex="1">
        <a-card class="stat-card" size="small">
          <a-statistic title="活动明细" :value="summary.itemCount" :value-style="{ color: '#722ed1' }" />
        </a-card>
      </a-col>
      <a-col :flex="1">
        <a-card class="stat-card" size="small">
          <a-statistic title="费用合计(CNY)" :value="summary.totalCostCny" :precision="2" :value-style="{ color: '#52c41a' }">
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
        :scroll="{ x: 1420 }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 站点 -->
          <template v-if="column.key === 'siteCode'">
            <a-tag color="blue">{{ record.siteCode }}</a-tag>
          </template>

          <!-- 账单周期 -->
          <template v-else-if="column.key === 'billingPeriod'">
            <span>{{ record.billingStartDate }} ~ {{ record.billingEndDate }}</span>
          </template>

          <!-- 费用合计 -->
          <template v-else-if="column.key === 'totalCost'">
            <span class="amount">{{ formatAmount(record.totalCost, record.currency) }}</span>
          </template>

          <!-- 汇率（由 totalCostCny/totalCost 推算，与发票开具日期一致） -->
          <template v-else-if="column.key === 'exchangeRate'">
            <span>{{ formatExchangeRate(record) }}</span>
          </template>

          <!-- 汇率取值日期（使用发票开具日期） -->
          <template v-else-if="column.key === 'exchangeRateDate'">
            <span>{{ record.issueDate || '-' }}</span>
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
                <a-button type="link" size="small" danger>
                  <DeleteOutlined /> 删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailModalVisible"
      title="广告发票详情"
      width="900px"
      :footer="null"
    >
      <div v-if="detailData">
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="发票编号">{{ detailData.invoiceNumber }}</a-descriptions-item>
          <a-descriptions-item label="发票状态">{{ detailData.invoiceStatus || '-' }}</a-descriptions-item>
          <a-descriptions-item label="店铺">{{ detailData.storeName }}</a-descriptions-item>
          <a-descriptions-item label="站点">{{ detailData.siteCode || '-' }}</a-descriptions-item>
          <a-descriptions-item label="账单周期">{{ detailData.billingStartDate }} ~ {{ detailData.billingEndDate }}</a-descriptions-item>
          <a-descriptions-item label="开具时间">{{ detailData.issueDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="账单金额">{{ detailData.currency }} {{ detailData.invoiceAmount?.toFixed(2) }}</a-descriptions-item>
          <a-descriptions-item label="费用合计">{{ detailData.currency }} {{ detailData.totalCost?.toFixed(2) }}</a-descriptions-item>
          <a-descriptions-item label="费用(CNY)">¥{{ detailData.totalCostCny?.toFixed(2) }}</a-descriptions-item>
        </a-descriptions>
        <div style="margin-top: 16px">
          <h4 style="margin-bottom: 12px">广告活动明细 ({{ detailData.items?.length ?? 0 }} 条)</h4>
          <a-table
            :columns="itemColumns"
            :data-source="detailData.items || []"
            :pagination="false"
            :scroll="{ y: 300 }"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'index'">{{ index + 1 }}</template>
              <template v-else-if="column.key === 'cost'">
                {{ record.cost != null ? record.cost.toFixed(2) : '-' }}
              </template>
              <template v-else-if="column.key === 'amountCny'">
                {{ record.amountCny != null ? '¥' + record.amountCny.toFixed(2) : '-' }}
              </template>
            </template>
          </a-table>
        </div>
      </div>
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
  PlusOutlined,
  DeleteOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/modules/auth'
import {
  getAdvertisingById,
  deleteAdvertising,
  batchDeleteAdvertising,
  batchPhysicalDeleteAdvertising,
  searchAdvertisingData,
  getAdvertisingSummary
} from '@/api/advertising'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { AdvertisingBill, AdvertisingSummary } from '@/types/advertising'
import type { Marketplace } from '@/types/marketplace'

const router = useRouter()
const authStore = useAuthStore()

function formatAmount(amount: number | null | undefined, currency: string): string {
  const value = amount ?? 0
  return `${currency || ''} ${value.toFixed(2)}`
}

function formatExchangeRate(record: AdvertisingBill): string {
  const total = record.totalCost ?? 0
  const cny = record.totalCostCny ?? 0
  if (total <= 0) return '-'
  return (cny / total).toFixed(6)
}

// ============= 搜索相关 =============
const searchForm = reactive({
  siteCode: undefined as string | undefined,
  billingPeriod: null as [Dayjs, Dayjs] | null,
  invoiceNumber: undefined as string | undefined
})
const marketplaceOptions = ref<Marketplace[]>([])

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<AdvertisingBill[]>([])
const selectedRowKeys = ref<number[]>([])

// ============= 统计汇总 =============
const summary = reactive<AdvertisingSummary>({
  invoiceCount: 0,
  itemCount: 0,
  totalCost: 0,
  totalCostCny: 0
})

const columns = [
  {
    title: '站点',
    dataIndex: 'siteCode',
    key: 'siteCode',
    width: 100
  },
  {
    title: '店铺名称',
    dataIndex: 'storeName',
    key: 'storeName',
    width: 150
  },
  {
    title: '发票号',
    dataIndex: 'invoiceNumber',
    key: 'invoiceNumber',
    width: 150
  },
  {
    title: '发票状态',
    dataIndex: 'invoiceStatus',
    key: 'invoiceStatus',
    width: 120
  },
  {
    title: '账单周期',
    key: 'billingPeriod',
    width: 220
  },
  {
    title: '发票开具日期',
    dataIndex: 'issueDate',
    key: 'issueDate',
    width: 120
  },
  {
    title: '账单金额',
    dataIndex: 'invoiceAmount',
    key: 'invoiceAmount',
    width: 120,
    align: 'right' as const
  },
  {
    title: '费用合计',
    dataIndex: 'totalCost',
    key: 'totalCost',
    width: 120,
    align: 'right' as const
  },
  {
    title: '费用(CNY)',
    dataIndex: 'totalCostCny',
    key: 'totalCostCny',
    width: 120,
    align: 'right' as const
  },
  {
    title: '汇率',
    key: 'exchangeRate',
    width: 100,
    align: 'right' as const
  },
  {
    title: '汇率取值日期',
    key: 'exchangeRateDate',
    width: 120
  },
  {
    title: '币种',
    dataIndex: 'currency',
    key: 'currency',
    width: 80
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
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

// 明细表格列
const itemColumns = [
  { title: '序号', key: 'index', width: 60 },
  { title: '广告活动', dataIndex: 'campaignName', key: 'campaignName', width: 150 },
  { title: '活动ID', dataIndex: 'campaignId', key: 'campaignId', width: 180 },
  { title: '计价方式', dataIndex: 'pricingModel', key: 'pricingModel', width: 80 },
  { title: '点击', dataIndex: 'clicks', key: 'clicks', width: 80, align: 'right' as const },
  { title: '平均CPC', dataIndex: 'avgCpc', key: 'avgCpc', width: 90, align: 'right' as const },
  { title: '费用', key: 'cost', width: 100, align: 'right' as const },
  { title: '费用(CNY)', key: 'amountCny', width: 110, align: 'right' as const }
]

// 行选择配置
const rowSelection = computed<TableRowSelection>(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => {
    selectedRowKeys.value = keys as number[]
  }
}))

// ============= 弹窗相关 =============
const detailModalVisible = ref(false)
const detailData = ref<AdvertisingBill | null>(null)

function handleViewDetail(record: AdvertisingBill) {
  getAdvertisingById(record.id).then(res => {
    detailData.value = res.data
    detailModalVisible.value = true
  }).catch(() => message.error('获取详情失败'))
}

function handleGoToDetailView() {
  router.push({ name: 'AdvertisingDetail' })
}

function handleImport() {
  router.push({ name: 'AdvertisingImport' })
}

async function fetchMarketplaces() {
  try {
    const res = await getEnabledMarketplaces()
    marketplaceOptions.value = res.data || []
  } catch (error) {
    console.error('获取站点列表失败:', error)
  }
}

const summaryParams = () => {
  const [startDate, endDate] = searchForm.billingPeriod || []
  return {
    siteCode: searchForm.siteCode,
    billingStartDate: startDate?.format('YYYY-MM-DD'),
    billingEndDate: endDate?.format('YYYY-MM-DD'),
    invoiceNumber: searchForm.invoiceNumber
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      ...summaryParams(),
      current: pagination.current,
      size: pagination.pageSize
    }
    const [listRes, summaryRes] = await Promise.all([
      searchAdvertisingData(params),
      getAdvertisingSummary(summaryParams())
    ])
    const pageData = listRes.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
    const s = summaryRes.data
    Object.assign(summary, {
      invoiceCount: s?.invoiceCount ?? 0,
      itemCount: s?.itemCount ?? 0,
      totalCost: s?.totalCost ?? 0,
      totalCostCny: s?.totalCostCny ?? 0
    })
  } catch (error) {
    console.error('获取广告数据列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchForm.siteCode = undefined
  searchForm.billingPeriod = null
  searchForm.invoiceNumber = undefined
  pagination.current = 1
  fetchData()
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  fetchData()
}

async function handleDelete(record: AdvertisingBill) {
  try {
    // Admin 用户使用物理删除，普通用户使用逻辑删除
    if (authStore.isAdmin) {
      await batchPhysicalDeleteAdvertising([record.id])
    } else {
      await deleteAdvertising(record.id)
    }
    message.success('删除成功')
    fetchData()
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
          await batchPhysicalDeleteAdvertising(selectedRowKeys.value)
          message.success('批量删除成功')
        } else {
          await batchDeleteAdvertising(selectedRowKeys.value)
          message.success('批量删除成功')
        }
        selectedRowKeys.value = []
        fetchData()
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
})
</script>

<style lang="scss" scoped>
.advertising-list-page {
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

  .stat-row {
    margin-bottom: $spacing-md;

    .stat-card {
      text-align: center;
    }
  }

  .amount {
    font-weight: 500;
    color: $primary-color;
  }

  .amount-cny {
    font-weight: 500;
    color: $success-color;
  }
}
</style>
