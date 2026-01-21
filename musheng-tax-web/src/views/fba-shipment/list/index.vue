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
                placeholder="货件编号"
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
                v-model:value="searchForm.status"
                placeholder="状态"
                allow-clear
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option
                  v-for="option in statusOptions"
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
              <a-input
                v-model:value="searchForm.receivingAddress"
                placeholder="收货地址"
                allow-clear
                @pressEnter="handleSearch"
              />
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

    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stat-row">
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic title="总货件数" :value="summary.totalShipments" :value-style="{ color: '#1890ff' }">
            <template #prefix><InboxOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic title="总SKU数" :value="summary.totalSkuCount" :value-style="{ color: '#52c41a' }">
            <template #prefix><TagsOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic title="预计总数量" :value="summary.totalExpectedQuantity" :value-style="{ color: '#faad14' }">
            <template #prefix><ShoppingOutlined /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stat-card">
          <a-statistic title="实际总数量" :value="summary.totalFoundQuantity" :value-style="{ color: '#ff4d4f' }">
            <template #prefix><CheckCircleOutlined /></template>
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
        :scroll="{ x: 1600 }"
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

          <!-- 状态 -->
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ record.status }}</a-tag>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleViewDetail(record)">
                详情
              </a-button>
              <a-button type="link" size="small" @click="handleEdit(record)">
                编辑
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
      title="FBA货件明细详情"
      width="750px"
      :footer="null"
    >
      <a-descriptions v-if="detailData" :column="2" bordered size="small">
        <a-descriptions-item label="货件名称" :span="2">{{ detailData.shipmentName }}</a-descriptions-item>
        <a-descriptions-item label="货件编号" :span="2">
          <a-typography-text copyable>{{ detailData.shipmentId }}</a-typography-text>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ detailData.createdDate }}</a-descriptions-item>
        <a-descriptions-item label="最后更新">{{ detailData.lastUpdated }}</a-descriptions-item>
        <a-descriptions-item label="收货地址">{{ detailData.receivingAddress || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(detailData.status)">{{ detailData.status }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="SKU种类数">{{ detailData.skuCount || 0 }}</a-descriptions-item>
        <a-descriptions-item label="预计商品数量">{{ detailData.expectedQuantity || 0 }}</a-descriptions-item>
        <a-descriptions-item label="找到的商品数量" :span="2">
          <span :class="{ 'warning-text': detailData.foundQuantity !== detailData.expectedQuantity }">
            {{ detailData.foundQuantity || 0 }}
            <span v-if="detailData.foundQuantity !== detailData.expectedQuantity" class="diff-text">
              (差异: {{ (detailData.foundQuantity || 0) - (detailData.expectedQuantity || 0) }})
            </span>
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="导入批次ID">{{ detailData.importBatchId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="导入时间">{{ detailData.createTime }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑FBA货件明细"
      width="750px"
      @ok="handleSaveEdit"
    >
      <a-form
        v-if="editData"
        ref="editFormRef"
        :model="editData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="货件名称" name="shipmentName" :rules="[{ required: true, message: '请输入货件名称' }]">
          <a-input v-model:value="editData.shipmentName" />
        </a-form-item>
        <a-form-item label="货件编号" name="shipmentId" :rules="[{ required: true, message: '请输入货件编号' }]">
          <a-input v-model:value="editData.shipmentId" disabled />
        </a-form-item>
        <a-form-item label="收货地址" name="receivingAddress">
          <a-input v-model:value="editData.receivingAddress" />
        </a-form-item>
        <a-form-item label="SKU种类数" name="skuCount">
          <a-input-number v-model:value="editData.skuCount" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="预计商品数量" name="expectedQuantity">
          <a-input-number v-model:value="editData.expectedQuantity" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="找到的商品数量" name="foundQuantity">
          <a-input-number v-model:value="editData.foundQuantity" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-select v-model:value="editData.status">
            <a-select-option v-for="option in statusOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
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
  CheckCircleOutlined
} from '@ant-design/icons-vue'
import {
  getFbaShipmentList,
  getFbaShipmentById,
  getFbaShipmentSummary,
  updateFbaShipment,
  deleteFbaShipment,
  batchDeleteFbaShipment,
  exportFbaShipmentData
} from '@/api/fbaShipment'
import type { FbaShipmentDetail, FbaShipmentSummary } from '@/types/fbaShipment'
import { FbaShipmentStatusOptions } from '@/types/fbaShipment'

const router = useRouter()

// ============= 搜索相关 =============
const searchForm = reactive({
  shipmentId: '',
  status: undefined as string | undefined,
  receivingAddress: ''
})
const searchDateRange = ref<[Dayjs, Dayjs] | null>(null)
const statusOptions = FbaShipmentStatusOptions

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<FbaShipmentDetail[]>([])
const selectedRowKeys = ref<number[]>([])

// ============= 统计数据 =============
const summary = reactive<FbaShipmentSummary>({
  totalShipments: 0,
  totalSkuCount: 0,
  totalExpectedQuantity: 0,
  totalFoundQuantity: 0
})

const columns = [
  {
    title: '货件名称',
    dataIndex: 'shipmentName',
    key: 'shipmentName',
    width: 150,
    fixed: 'left' as const,
    ellipsis: true
  },
  {
    title: '货件编号',
    dataIndex: 'shipmentId',
    key: 'shipmentId',
    width: 180,
    ellipsis: true
  },
  {
    title: '创建时间',
    dataIndex: 'createdDate',
    key: 'createdDate',
    width: 160
  },
  {
    title: '最后更新',
    dataIndex: 'lastUpdated',
    key: 'lastUpdated',
    width: 160
  },
  {
    title: '收货地址',
    dataIndex: 'receivingAddress',
    key: 'receivingAddress',
    width: 100
  },
  {
    title: 'SKU数',
    dataIndex: 'skuCount',
    key: 'skuCount',
    width: 80,
    align: 'right' as const
  },
  {
    title: '预计数量',
    dataIndex: 'expectedQuantity',
    key: 'expectedQuantity',
    width: 100,
    align: 'right' as const
  },
  {
    title: '实际数量',
    dataIndex: 'foundQuantity',
    key: 'foundQuantity',
    width: 100,
    align: 'right' as const
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '操作',
    key: 'action',
    width: 180,
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
const detailData = ref<FbaShipmentDetail | null>(null)

// ============= 编辑弹窗 =============
const editModalVisible = ref(false)
const editData = ref<Partial<FbaShipmentDetail> | null>(null)
const editFormRef = ref<FormInstance>()

// ============= 方法 =============
function getStatusColor(status: string | undefined): string {
  if (!status) return 'default'
  switch (status) {
    case '已完成': return 'success'
    case '接受中': return 'processing'
    case '已结账': return 'default'
    case '配送中': return 'warning'
    default: return 'default'
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      shipmentId: searchForm.shipmentId || undefined,
      status: searchForm.status,
      receivingAddress: searchForm.receivingAddress || undefined,
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
      receivingAddress: searchForm.receivingAddress || undefined,
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
  searchForm.receivingAddress = ''
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

async function handleExport() {
  try {
    const params = {
      status: searchForm.status,
      receivingAddress: searchForm.receivingAddress || undefined,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD')
    }
    await exportFbaShipmentData(params)
    message.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
  }
}

async function handleViewDetail(record: FbaShipmentDetail) {
  try {
    const res = await getFbaShipmentById(record.id)
    detailData.value = res.data
    detailModalVisible.value = true
  } catch (error) {
    console.error('获取详情失败:', error)
  }
}

function handleEdit(record: FbaShipmentDetail) {
  editData.value = { ...record }
  editModalVisible.value = true
}

async function handleSaveEdit() {
  try {
    await editFormRef.value?.validate()
    if (!editData.value || !editData.value.id) {
      message.error('数据异常')
      return
    }
    await updateFbaShipment(editData.value.id, editData.value)
    message.success('更新成功')
    editModalVisible.value = false
    fetchData()
    fetchSummary()
  } catch (error) {
    console.error('更新失败:', error)
  }
}

async function handleDelete(record: FbaShipmentDetail) {
  try {
    await deleteFbaShipment(record.id)
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
        await batchDeleteFbaShipment(selectedRowKeys.value)
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
  fetchData()
  fetchSummary()
})
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
