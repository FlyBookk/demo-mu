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
          <a-col :span="5">
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
          <a-col :span="4" style="text-align: right">
            <a-space>
              <a-button type="primary" @click="handleAdd">
                <PlusOutlined /> 广告费录入
              </a-button>
              <a-button @click="handleImport">
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

    <!-- 数据表格 -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
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

          <!-- 费用 -->
          <template v-else-if="column.key === 'cost'">
            <span class="amount">{{ formatAmount(record.cost, record.currency) }}</span>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
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
  DeleteOutlined
} from '@ant-design/icons-vue'
import {
  deleteAdvertising,
  batchDeleteAdvertising,
  searchAdvertisingData
} from '@/api/advertising'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { AdvertisingData } from '@/types/advertising'
import type { Marketplace } from '@/types/marketplace'

const router = useRouter()

function formatAmount(amount: number | null | undefined, currency: string): string {
  const value = amount ?? 0
  return `${currency || ''} ${value.toFixed(2)}`
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
const tableData = ref<AdvertisingData[]>([])
const selectedRowKeys = ref<number[]>([])

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
    title: '费用',
    dataIndex: 'cost',
    key: 'cost',
    width: 140,
    align: 'right' as const
  },
  {
    title: '币种',
    dataIndex: 'currency',
    key: 'currency',
    width: 80
  },
  {
    title: '广告活动',
    dataIndex: 'campaignName',
    key: 'campaignName',
    width: 150
  },
  {
    title: '点击次数',
    dataIndex: 'clicks',
    key: 'clicks',
    width: 100,
    align: 'right' as const
  },
  {
    title: '平均CPC',
    dataIndex: 'avgCpc',
    key: 'avgCpc',
    width: 100,
    align: 'right' as const
  },
  {
    title: '备注',
    dataIndex: 'remark',
    key: 'remark',
    width: 200,
    ellipsis: true
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

// 行选择配置
const rowSelection = computed<TableRowSelection>(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => {
    selectedRowKeys.value = keys as number[]
  }
}))

// ============= 弹窗相关 =============
// 列表数据为批量导入的发票数据，使用广告费录入页面进行录入
function handleAdd() {
  router.push({ name: 'AdvertisingAdd' })
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

async function fetchData() {
  loading.value = true
  try {
    const [startDate, endDate] = searchForm.billingPeriod || []

    const params = {
      siteCode: searchForm.siteCode,
      billingStartDate: startDate?.format('YYYY-MM-DD'),
      billingEndDate: endDate?.format('YYYY-MM-DD'),
      invoiceNumber: searchForm.invoiceNumber,
      current: pagination.current,
      size: pagination.pageSize
    }
    const res = await searchAdvertisingData(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
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

async function handleDelete(record: AdvertisingData) {
  try {
    await deleteAdvertising(record.id)
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
        await batchDeleteAdvertising(selectedRowKeys.value)
        message.success('批量删除成功')
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
