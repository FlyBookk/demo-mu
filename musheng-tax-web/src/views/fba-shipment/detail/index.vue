<template>
  <div class="fba-shipment-detail-page">
    <div class="page-header">
      <h1 class="page-title">FBA货件MSKU明细</h1>
      <p class="page-desc">全局查看和检索所有FBA货件的MSKU级别明细数据</p>
    </div>

    <a-card class="search-card">
      <a-form layout="inline" :model="searchForm">
        <a-row :gutter="16" style="width: 100%">
          <a-col :span="6">
            <a-form-item>
              <a-input
                v-model:value="searchForm.shipmentNo"
                placeholder="货件单号"
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
                v-model:value="searchForm.msku"
                placeholder="MSKU"
                allow-clear
                @pressEnter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12" style="text-align: right">
            <a-space>
              <a-button type="primary" @click="handleSearch">
                <SearchOutlined /> 查询
              </a-button>
              <a-button @click="handleReset">
                <ReloadOutlined /> 重置
              </a-button>
              <a-button @click="handleGoBack">
                <RollbackOutlined /> 返回货件列表
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
        :scroll="{ x: 800 }"
        row-key="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'shipmentNo'">
            <a-typography-text copyable :content="record.shipmentNo">
              {{ record.shipmentNo }}
            </a-typography-text>
          </template>
          <template v-else-if="column.key === 'msku'">
            <a-typography-text copyable :content="record.msku">
              {{ record.msku }}
            </a-typography-text>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  RollbackOutlined
} from '@ant-design/icons-vue'
import { getFbaShipmentItemList } from '@/api/fbaShipment'
import type { FbaShipmentItem } from '@/types/fbaShipment'

const router = useRouter()

const searchForm = reactive({
  shipmentNo: '',
  msku: ''
})

const loading = ref(false)
const tableData = ref<FbaShipmentItem[]>([])

const columns = [
  {
    title: '货件单号',
    dataIndex: 'shipmentNo',
    key: 'shipmentNo',
    width: 160,
    fixed: 'left' as const,
    ellipsis: true
  },
  {
    title: 'MSKU',
    dataIndex: 'msku',
    key: 'msku',
    width: 200,
    ellipsis: true
  },
  {
    title: '申报量',
    dataIndex: 'quantity',
    key: 'quantity',
    width: 100,
    align: 'right' as const
  },
  {
    title: '签收量',
    dataIndex: 'receivedQuantity',
    key: 'receivedQuantity',
    width: 100,
    align: 'right' as const
  },
  {
    title: '导入时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 160
  }
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
    const params = {
      shipmentNo: searchForm.shipmentNo || undefined,
      msku: searchForm.msku || undefined,
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getFbaShipmentItemList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取MSKU明细列表失败:', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchForm.shipmentNo = ''
  searchForm.msku = ''
  pagination.current = 1
  fetchData()
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  fetchData()
}

function handleGoBack() {
  router.push('/fba-shipment/list')
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.fba-shipment-detail-page {
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
