<template>
  <div class="fba-shipment-detail-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">FBA货件SKU明细</h1>
      <p class="page-desc">全局查看和检索所有FBA货件的SKU级别明细数据</p>
    </div>

    <!-- 搜索栏 -->
    <a-card class="search-card">
      <a-form layout="inline" :model="searchForm">
        <a-row :gutter="16" style="width: 100%">
          <a-col :span="4">
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
          <a-col :span="4">
            <a-form-item>
              <a-input
                v-model:value="searchForm.sku"
                placeholder="内部SKU"
                allow-clear
                @pressEnter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item>
              <a-input
                v-model:value="searchForm.msku"
                placeholder="亚马逊MSKU"
                allow-clear
                @pressEnter="handleSearch"
              />
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item>
              <a-select
                v-model:value="searchForm.shopName"
                placeholder="店铺名称"
                allow-clear
                show-search
                :filter-option="filterOption"
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option
                  v-for="option in shopNameOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item>
              <a-select
                v-model:value="searchForm.country"
                placeholder="国家"
                allow-clear
                show-search
                :filter-option="filterOption"
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option
                  v-for="option in countryOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item>
              <a-range-picker
                v-model:value="searchDateRange"
                style="width: 100%"
                @change="handleSearch"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16" style="width: 100%; margin-top: 8px">
          <a-col :span="24" style="text-align: right">
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

    <!-- 数据表格 -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1400 }"
        row-key="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 货件单号 -->
          <template v-if="column.key === 'shipmentNo'">
            <a-typography-text copyable :content="record.shipmentNo">
              {{ record.shipmentNo }}
            </a-typography-text>
          </template>

          <!-- SKU -->
          <template v-else-if="column.key === 'sku'">
            <a-typography-text copyable :content="record.sku">
              {{ record.sku }}
            </a-typography-text>
          </template>

          <!-- MSKU -->
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
import type { Dayjs } from 'dayjs'
import {
  SearchOutlined,
  ReloadOutlined,
  RollbackOutlined
} from '@ant-design/icons-vue'
import { getFbaShipmentItemList, getFbaShipmentCountries, getFbaShipmentShopNames } from '@/api/fbaShipment'
import type { FbaShipmentItem } from '@/types/fbaShipment'

const router = useRouter()

// ============= 搜索相关 =============
const searchForm = reactive({
  shipmentNo: '',
  sku: '',
  msku: '',
  shopName: '',
  country: undefined as string | undefined
})
const searchDateRange = ref<[Dayjs, Dayjs] | null>(null)
const countryOptions = ref<Array<{ label: string; value: string }>>([])
const shopNameOptions = ref<Array<{ label: string; value: string }>>([])

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<FbaShipmentItem[]>([])

const columns = [
  {
    title: '货件单号',
    dataIndex: 'shipmentNo',
    key: 'shipmentNo',
    width: 150,
    fixed: 'left' as const,
    ellipsis: true
  },
  {
    title: '内部SKU',
    dataIndex: 'sku',
    key: 'sku',
    width: 150,
    ellipsis: true
  },
  {
    title: '亚马逊MSKU',
    dataIndex: 'msku',
    key: 'msku',
    width: 150,
    ellipsis: true
  },
  {
    title: '发货量',
    dataIndex: 'quantity',
    key: 'quantity',
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

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// ============= 方法 =============
async function fetchData() {
  loading.value = true
  try {
    const params = {
      shipmentNo: searchForm.shipmentNo || undefined,
      sku: searchForm.sku || undefined,
      msku: searchForm.msku || undefined,
      shopName: searchForm.shopName || undefined,
      country: searchForm.country,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD'),
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getFbaShipmentItemList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取FBA货件SKU明细列表失败:', error)
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
  searchForm.sku = ''
  searchForm.msku = ''
  searchForm.shopName = ''
  searchForm.country = undefined
  searchDateRange.value = null
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

// 下拉框搜索过滤
function filterOption(input: string, option: any) {
  return option.value.toLowerCase().includes(input.toLowerCase())
}

// 获取国家列表
async function fetchCountries() {
  try {
    const res = await getFbaShipmentCountries()
    countryOptions.value = (res.data || []).map(country => ({
      label: country,
      value: country
    }))
  } catch (error) {
    console.error('获取国家列表失败:', error)
  }
}

// 获取店铺名称列表
async function fetchShopNames() {
  try {
    const res = await getFbaShipmentShopNames()
    shopNameOptions.value = (res.data || []).map(shopName => ({
      label: shopName,
      value: shopName
    }))
  } catch (error) {
    console.error('获取店铺列表失败:', error)
  }
}

// 初始化
onMounted(() => {
  fetchData()
  fetchCountries()
  fetchShopNames()
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
