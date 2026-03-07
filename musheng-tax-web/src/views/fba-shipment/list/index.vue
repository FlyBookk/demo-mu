<template>
  <div class="fba-shipment-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">FBA货件明细列表</h1>
      <p class="page-desc">查看和管理已导入的亚马逊FBA货件明细数据</p>
    </div>

    <!-- 搜索栏 -->
    <a-card class="search-card">
      <a-form layout="inline" :model="searchForm">
        <a-row :gutter="16" style="width: 100%">
          <a-col :span="4">
            <a-form-item>
              <a-input
                v-model:value="searchForm.shipmentId"
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
              <a-select
                v-model:value="searchForm.status"
                placeholder="货件状态"
                allow-clear
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option
                  v-for="option in FbaShipmentStatusOptions"
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
                  v-for="option in siteCodeOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </a-select-option>
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
              <a-button @click="handleGoToDetailView">
                <UnorderedListOutlined /> SKU明细视图
              </a-button>
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

    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stat-row">
      <a-col :span="8">
        <a-card class="stat-card">
          <a-statistic title="总货件数" :value="summary.totalShipments" :value-style="{ color: '#1890ff' }">
            <template #prefix><InboxOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card class="stat-card">
          <a-statistic title="总SKU种类数" :value="summary.totalSkuCount" :value-style="{ color: '#52c41a' }">
            <template #prefix><TagsOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card class="stat-card">
          <a-statistic title="总发货量" :value="summary.totalQuantity" :value-style="{ color: '#faad14' }">
            <template #prefix><ShoppingOutlined /></template>
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
        :scroll="{ x: 1680 }"
        row-key="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 货件编号 -->
          <template v-if="column.key === 'shipmentId'">
            <a-typography-text copyable :content="record.shipmentId">
              {{ record.shipmentId }}
            </a-typography-text>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleViewDetail(record)">
                详情
              </a-button>
              <a-popconfirm
                title="确定要删除该货件吗？将同时删除所有SKU明细。"
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
      title="FBA货件详情"
      width="900px"
      :footer="null"
    >
      <div v-if="detailData">
        <!-- 货件信息 -->
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="货件单号">
            <a-typography-text copyable>{{ detailData.shipmentId }}</a-typography-text>
          </a-descriptions-item>
          <a-descriptions-item label="货件名称">{{ detailData.shipmentName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="货件状态">{{ detailData.status || '-' }}</a-descriptions-item>
          <a-descriptions-item label="物流中心编码">{{ detailData.warehouseCode || '-' }}</a-descriptions-item>
          <a-descriptions-item label="收件人">{{ detailData.recipient || '-' }}</a-descriptions-item>
          <a-descriptions-item label="收件邮编">{{ detailData.postalCode || '-' }}</a-descriptions-item>
          <a-descriptions-item label="收件国家">{{ detailData.country || '-' }}</a-descriptions-item>
          <a-descriptions-item label="站点">{{ detailData.siteCode || '-' }}</a-descriptions-item>
          <a-descriptions-item label="收件州/省">{{ detailData.state || '-' }}</a-descriptions-item>
          <a-descriptions-item label="收件城市">{{ detailData.city || '-' }}</a-descriptions-item>
          <a-descriptions-item label="收件街道地址" :span="2">{{ detailData.streetAddress || '-' }}</a-descriptions-item>
          <a-descriptions-item label="收件门牌号">{{ detailData.houseNumber || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detailData.createdDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ detailData.updatedDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="MSKU种类数">{{ detailData.skuCount || 0 }}</a-descriptions-item>
          <a-descriptions-item label="总申报量">{{ detailData.totalQuantity || 0 }}</a-descriptions-item>
          <a-descriptions-item label="总签收量">{{ detailData.totalReceivedQuantity || 0 }}</a-descriptions-item>
        </a-descriptions>

        <!-- MSKU明细表格 -->
        <div style="margin-top: 16px">
          <h4 style="margin-bottom: 12px">MSKU明细列表</h4>
          <a-table
            :columns="itemColumns"
            :data-source="detailData.items || []"
            :pagination="false"
            :scroll="{ y: 300 }"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'index'">
                {{ index + 1 }}
              </template>
              <template v-else-if="column.key === 'msku'">
                <a-typography-text copyable :content="record.msku">{{ record.msku }}</a-typography-text>
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
import type { TablePaginationConfig, FormInstance } from 'ant-design-vue'
import type { TableRowSelection } from 'ant-design-vue/es/table/interface'
import type { Dayjs } from 'dayjs'
import {
  SearchOutlined,
  ReloadOutlined,
  CloudUploadOutlined,
  DownloadOutlined,
  DeleteOutlined,
  InboxOutlined,
  TagsOutlined,
  ShoppingOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import {
  getFbaShipmentList,
  getFbaShipmentById,
  getFbaShipmentSummary,
  deleteFbaShipment,
  batchDeleteFbaShipment,
  batchPhysicalDeleteFbaShipment,
  exportFbaShipmentData,
  getFbaShipmentCountries
} from '@/api/fbaShipment'
import type { FbaShipment, FbaShipmentSummary } from '@/types/fbaShipment'
import { FbaShipmentStatusOptions } from '@/types/fbaShipment'
import { useAuthStore } from '@/stores/modules/auth'

const router = useRouter()
const authStore = useAuthStore()

// ============= 搜索相关 =============
const searchForm = reactive({
  shipmentId: '',
  status: undefined as string | undefined,
  country: undefined as string | undefined,
  siteCode: undefined as string | undefined
})

// 站点选项（与导入页面保持一致）
const siteCodeOptions = [
  { label: 'US（美国）', value: 'US' },
  { label: 'CA（加拿大）', value: 'CA' },
  { label: 'UK（英国）', value: 'UK' },
  { label: 'DE（德国）', value: 'DE' },
  { label: 'FR（法国）', value: 'FR' },
  { label: 'IT（意大利）', value: 'IT' },
  { label: 'ES（西班牙）', value: 'ES' },
  { label: 'JP（日本）', value: 'JP' },
  { label: 'AU（澳大利亚）', value: 'AU' },
  { label: 'MX（墨西哥）', value: 'MX' },
]
const searchDateRange = ref<[Dayjs, Dayjs] | null>(null)
const countryOptions = ref<Array<{ label: string; value: string }>>([])

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<FbaShipment[]>([])
const selectedRowKeys = ref<number[]>([])

// ============= 统计数据 =============
const summary = reactive<FbaShipmentSummary>({
  totalShipments: 0,
  totalSkuCount: 0,
  totalQuantity: 0
})

const columns = [
  {
    title: '货件单号',
    dataIndex: 'shipmentId',
    key: 'shipmentId',
    width: 160,
    fixed: 'left' as const,
    ellipsis: true
  },
  {
    title: '货件名称',
    dataIndex: 'shipmentName',
    key: 'shipmentName',
    width: 140,
    ellipsis: true
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 90
  },
  {
    title: '物流中心',
    dataIndex: 'warehouseCode',
    key: 'warehouseCode',
    width: 100,
    ellipsis: true
  },
  {
    title: '收件国家',
    dataIndex: 'country',
    key: 'country',
    width: 180,
    ellipsis: true
  },
  {
    title: '站点',
    dataIndex: 'siteCode',
    key: 'siteCode',
    width: 80,
    customRender: ({ text }: { text: string }) => text || '-'
  },
  {
    title: '创建时间',
    dataIndex: 'createdDate',
    key: 'createdDate',
    width: 140
  },
  {
    title: 'MSKU种类',
    dataIndex: 'skuCount',
    key: 'skuCount',
    width: 90,
    align: 'right' as const
  },
  {
    title: '总申报量',
    dataIndex: 'totalQuantity',
    key: 'totalQuantity',
    width: 90,
    align: 'right' as const
  },
  {
    title: '总签收量',
    dataIndex: 'totalReceivedQuantity',
    key: 'totalReceivedQuantity',
    width: 90,
    align: 'right' as const
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    fixed: 'right' as const
  }
]

// MSKU明细表格列定义
const itemColumns = [
  {
    title: '序号',
    key: 'index',
    width: 60,
    align: 'right' as const
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
const detailData = ref<FbaShipment | null>(null)

// ============= 方法 =============
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      shipmentId: searchForm.shipmentId || undefined,
      status: searchForm.status,
      country: searchForm.country,
      siteCode: searchForm.siteCode,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD'),
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getFbaShipmentList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取FBA货件明细列表失败:', error)
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  try {
    const params = {
      status: searchForm.status,
      country: searchForm.country,
      siteCode: searchForm.siteCode,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD')
    }
    const res = await getFbaShipmentSummary(params)
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
  searchForm.shipmentId = ''
  searchForm.status = undefined
  searchForm.country = undefined
  searchForm.siteCode = undefined
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
  router.push('/fba-shipment/import')
}

function handleGoToDetailView() {
  router.push('/fba-shipment/detail')
}

// 下拉框搜索过滤
function filterOption(input: string, option: any) {
  return option.value.toLowerCase().includes(input.toLowerCase())
}

async function handleExport() {
  try {
    const params = {
      status: searchForm.status,
      country: searchForm.country,
      siteCode: searchForm.siteCode,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD')
    }
    await exportFbaShipmentData(params)
    message.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
  }
}

async function handleViewDetail(record: FbaShipment) {
  try {
    const res = await getFbaShipmentById(record.id)
    detailData.value = res.data
    detailModalVisible.value = true
  } catch (error) {
    console.error('获取详情失败:', error)
  }
}

async function handleDelete(record: FbaShipment) {
  try {
    // Admin 用户使用物理删除，普通用户使用逻辑删除
    if (authStore.isAdmin) {
      await batchPhysicalDeleteFbaShipment([record.id])
    } else {
      await deleteFbaShipment(record.id)
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
          await batchPhysicalDeleteFbaShipment(selectedRowKeys.value)
          message.success('批量删除成功')
        } else {
          await batchDeleteFbaShipment(selectedRowKeys.value)
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
  fetchData()
  fetchSummary()
  fetchCountries()
})

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

</script>

<style lang="scss" scoped>
.fba-shipment-list-page {
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

  .warning-text {
    color: $warning-color;
    font-weight: 500;
  }

  .diff-text {
    font-size: $font-size-sm;
    margin-left: $spacing-xs;
  }
}
</style>
