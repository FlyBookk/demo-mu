<template>
  <div class="advertising-detail-page">
    <div class="page-header">
      <h1 class="page-title">广告活动明细</h1>
      <p class="page-desc">全局查看和检索所有广告发票的活动明细</p>
    </div>

    <a-card class="search-card">
      <a-form layout="inline" :model="searchForm">
        <a-row :gutter="16" style="width: 100%">
          <a-col :span="6">
            <a-form-item>
              <a-input
                v-model:value="searchForm.invoiceNumber"
                placeholder="发票编号"
                allow-clear
                @pressEnter="handleSearch"
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item>
              <a-input
                v-model:value="searchForm.campaignId"
                placeholder="活动ID"
                allow-clear
                @pressEnter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item>
              <a-input
                v-model:value="searchForm.campaignName"
                placeholder="广告活动名称"
                allow-clear
                @pressEnter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6" style="text-align: right">
            <a-space>
              <a-button type="primary" @click="handleSearch">
                <SearchOutlined /> 查询
              </a-button>
              <a-button @click="handleReset">
                <ReloadOutlined /> 重置
              </a-button>
              <a-button @click="handleGoBack">
                <RollbackOutlined /> 返回列表
              </a-button>
            </a-space>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1000 }"
        row-key="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'invoiceNumber'">
            <a-typography-text copyable :content="record.invoiceNumber">
              {{ record.invoiceNumber }}
            </a-typography-text>
          </template>
          <template v-else-if="column.key === 'cost'">
            {{ record.cost != null ? record.cost.toFixed(2) : '-' }}
          </template>
          <template v-else-if="column.key === 'amountCny'">
            {{ record.amountCny != null ? '¥' + record.amountCny.toFixed(2) : '-' }}
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { TablePaginationConfig } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined, RollbackOutlined } from '@ant-design/icons-vue'
import { getAdvertisingItemList } from '@/api/advertising'
import type { AdvertisingBillItem } from '@/types/advertising'

const router = useRouter()

const searchForm = reactive({
  invoiceNumber: '',
  campaignId: '',
  campaignName: ''
})

const loading = ref(false)
const tableData = ref<AdvertisingBillItem[]>([])

const columns = [
  { title: '发票编号', dataIndex: 'invoiceNumber', key: 'invoiceNumber', width: 150 },
  { title: '广告活动', dataIndex: 'campaignName', key: 'campaignName', width: 150 },
  { title: '活动ID', dataIndex: 'campaignId', key: 'campaignId', width: 180 },
  { title: '计价方式', dataIndex: 'pricingModel', key: 'pricingModel', width: 80 },
  { title: '点击', dataIndex: 'clicks', key: 'clicks', width: 80, align: 'right' as const },
  { title: '平均CPC', dataIndex: 'avgCpc', key: 'avgCpc', width: 90, align: 'right' as const },
  { title: '费用', key: 'cost', width: 100, align: 'right' as const },
  { title: '费用(CNY)', key: 'amountCny', width: 110, align: 'right' as const }
]

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdvertisingItemList({
      invoiceNumber: searchForm.invoiceNumber || undefined,
      campaignId: searchForm.campaignId || undefined,
      campaignName: searchForm.campaignName || undefined,
      page: pagination.current,
      size: pagination.pageSize
    })
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取明细失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchForm.invoiceNumber = ''
  searchForm.campaignId = ''
  searchForm.campaignName = ''
  pagination.current = 1
  fetchData()
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  fetchData()
}

function handleGoBack() {
  router.push('/advertising/list')
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.advertising-detail-page {
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
}
</style>
