<template>
  <div class="import-record-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">导入记录</h1>
      <p class="page-desc">查看数据导入历史记录、处理状态、错误详情</p>
    </div>

    <!-- 搜索栏 -->
    <a-card class="search-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="4">
          <a-input
            v-model:value="searchKeyword"
            placeholder="批次号/文件名"
            allow-clear
            @pressEnter="handleSearch"
          >
            <template #prefix>
              <SearchOutlined />
            </template>
          </a-input>
        </a-col>
        <a-col :span="3">
          <a-select
            v-model:value="searchDataType"
            placeholder="数据类型"
            allow-clear
            style="width: 100%"
            @change="handleSearch"
          >
            <a-select-option v-for="item in dataTypeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="3">
          <a-select
            v-model:value="searchStatus"
            placeholder="状态"
            allow-clear
            style="width: 100%"
            @change="handleSearch"
          >
            <a-select-option v-for="item in statusOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="5">
          <a-range-picker
            v-model:value="searchDateRange"
            style="width: 100%"
            @change="handleSearch"
          />
        </a-col>
        <a-col :span="4">
          <a-space>
            <a-button type="primary" @click="handleSearch">
              <SearchOutlined /> 查询
            </a-button>
            <a-button @click="handleReset">
              <ReloadOutlined /> 重置
            </a-button>
          </a-space>
        </a-col>
        <a-col :span="5" style="text-align: right">
          <a-button
            danger
            :disabled="selectedRowKeys.length === 0"
            @click="handleBatchDelete"
          >
            <DeleteOutlined /> 批量删除
          </a-button>
        </a-col>
      </a-row>
    </a-card>

    <!-- 数据表格 -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-selection="rowSelection"
        :scroll="{ x: 1400 }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 批次号 -->
          <template v-if="column.key === 'batchNo'">
            <a-typography-text copyable>{{ record.batchNo }}</a-typography-text>
          </template>

          <!-- 文件名 -->
          <template v-else-if="column.key === 'fileName'">
            <a-tooltip :title="record.fileName">
              <span class="file-name">{{ record.fileName }}</span>
            </a-tooltip>
            <div class="file-size">{{ formatFileSize(record.fileSize) }}</div>
          </template>

          <!-- 数据类型 -->
          <template v-else-if="column.key === 'dataType'">
            <a-tag :color="getDataTypeColor(record.dataType)">
              {{ getDataTypeLabel(record.dataType) }}
            </a-tag>
          </template>

          <!-- 处理结果 -->
          <template v-else-if="column.key === 'result'">
            <div class="result-info">
              <span class="success-count">成功: {{ record.successRows }}</span>
              <span v-if="record.failedRows > 0" class="failed-count"> / 失败: {{ record.failedRows }}</span>
              <div class="total-count">共 {{ record.totalRows }} 条</div>
            </div>
          </template>

          <!-- 状态 -->
          <template v-else-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              <template #icon>
                <LoadingOutlined v-if="record.status === 1" spin />
                <CheckCircleOutlined v-else-if="record.status === 2" />
                <ExclamationCircleOutlined v-else-if="record.status === 3" />
                <CloseCircleOutlined v-else-if="record.status === 4" />
                <ClockCircleOutlined v-else />
              </template>
              {{ getStatusLabel(record.status) }}
            </a-tag>
          </template>

          <!-- 耗时 -->
          <template v-else-if="column.key === 'duration'">
            <span>{{ formatDuration(record.startTime, record.endTime) }}</span>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleViewDetail(record)">
                <EyeOutlined /> 详情
              </a-button>
              <a-button
                v-if="record.status === 4 || record.status === 3"
                type="link"
                size="small"
                @click="handleRetry(record)"
              >
                <RedoOutlined /> 重试
              </a-button>
              <a-button
                v-if="record.failedRows > 0 && record.errorFileUrl"
                type="link"
                size="small"
                @click="handleDownloadErrors(record)"
              >
                <DownloadOutlined /> 错误报告
              </a-button>
              <a-popconfirm
                title="确定要删除该记录吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>
                  <DeleteOutlined />
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
      title="导入记录详情"
      width="800px"
      :footer="null"
    >
      <template v-if="detailData">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="批次号" :span="2">
            <a-typography-text copyable>{{ detailData.batchNo }}</a-typography-text>
          </a-descriptions-item>
          <a-descriptions-item label="文件名">{{ detailData.fileName }}</a-descriptions-item>
          <a-descriptions-item label="文件大小">{{ formatFileSize(detailData.fileSize) }}</a-descriptions-item>
          <a-descriptions-item label="数据类型">
            <a-tag :color="getDataTypeColor(detailData.dataType)">{{ getDataTypeLabel(detailData.dataType) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="getStatusColor(detailData.status)">{{ getStatusLabel(detailData.status) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="站点">{{ detailData.marketplaceName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="模板">{{ detailData.templateName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="总行数">{{ detailData.totalRows }}</a-descriptions-item>
          <a-descriptions-item label="成功行数">
            <span class="success-text">{{ detailData.successRows }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="失败行数">
            <span class="failed-text">{{ detailData.failedRows }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="开始时间">{{ detailData.startTime }}</a-descriptions-item>
          <a-descriptions-item label="结束时间">{{ detailData.endTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="耗时">{{ formatDuration(detailData.startTime, detailData.endTime) }}</a-descriptions-item>
          <a-descriptions-item label="操作人">{{ detailData.createdBy }}</a-descriptions-item>
          <a-descriptions-item v-if="detailData.errorMessage" label="错误信息" :span="2">
            <a-alert type="error" :message="detailData.errorMessage" />
          </a-descriptions-item>
        </a-descriptions>

        <!-- 错误详情表格 -->
        <template v-if="detailData.errorDetails?.length">
          <a-divider>错误详情</a-divider>
          <a-table
            :columns="errorColumns"
            :data-source="detailData.errorDetails"
            :pagination="{ pageSize: 5 }"
            size="small"
            row-key="rowNumber"
          />
        </template>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { TableRowSelection } from 'ant-design-vue/es/table/interface'
import type { Dayjs } from 'dayjs'
import {
  SearchOutlined,
  ReloadOutlined,
  DeleteOutlined,
  EyeOutlined,
  RedoOutlined,
  DownloadOutlined,
  LoadingOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined
} from '@ant-design/icons-vue'
import {
  getImportRecordList,
  getImportRecordById,
  deleteImportRecord,
  batchDeleteImportRecord,
  retryImport,
  downloadErrorReport
} from '@/api/importRecord'
import type { ImportRecord, ImportRecordDetail, ImportDataType, ImportStatus } from '@/types/importRecord'

// ============= 选项配置 =============
const dataTypeOptions = [
  { value: 'SALES', label: '销售数据', color: 'green' },
  { value: 'SHIPPING', label: '配送数据', color: 'blue' },
  { value: 'ADVERTISING', label: '广告数据', color: 'purple' },
  { value: 'RATE', label: '汇率数据', color: 'orange' }
]

const statusOptions = [
  { value: 0, label: '待处理', color: 'default' },
  { value: 1, label: '处理中', color: 'processing' },
  { value: 2, label: '成功', color: 'success' },
  { value: 3, label: '部分成功', color: 'warning' },
  { value: 4, label: '失败', color: 'error' }
]

function getDataTypeColor(type: string): string {
  return dataTypeOptions.find(t => t.value === type)?.color || 'default'
}

function getDataTypeLabel(type: string): string {
  return dataTypeOptions.find(t => t.value === type)?.label || type
}

function getStatusColor(status: number): string {
  return statusOptions.find(s => s.value === status)?.color || 'default'
}

function getStatusLabel(status: number): string {
  return statusOptions.find(s => s.value === status)?.label || '未知'
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

function formatDuration(start: string, end?: string): string {
  if (!start || !end) return '-'
  const startTime = new Date(start).getTime()
  const endTime = new Date(end).getTime()
  const duration = endTime - startTime
  if (duration < 1000) return duration + 'ms'
  if (duration < 60000) return (duration / 1000).toFixed(1) + 's'
  return (duration / 60000).toFixed(1) + 'min'
}

// ============= 搜索相关 =============
const searchKeyword = ref('')
const searchDataType = ref<ImportDataType | undefined>(undefined)
const searchStatus = ref<ImportStatus | undefined>(undefined)
const searchDateRange = ref<[Dayjs, Dayjs] | null>(null)

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<ImportRecord[]>([])
const selectedRowKeys = ref<number[]>([])

const columns = [
  {
    title: '批次号',
    dataIndex: 'batchNo',
    key: 'batchNo',
    width: 200
  },
  {
    title: '文件',
    dataIndex: 'fileName',
    key: 'fileName',
    width: 200,
    ellipsis: true
  },
  {
    title: '数据类型',
    dataIndex: 'dataType',
    key: 'dataType',
    width: 100
  },
  {
    title: '站点',
    dataIndex: 'marketplaceName',
    key: 'marketplaceName',
    width: 80
  },
  {
    title: '处理结果',
    dataIndex: 'result',
    key: 'result',
    width: 150
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 120
  },
  {
    title: '耗时',
    dataIndex: 'duration',
    key: 'duration',
    width: 80
  },
  {
    title: '导入时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 170
  },
  {
    title: '操作',
    key: 'action',
    width: 220,
    fixed: 'right' as const
  }
]

const errorColumns = [
  {
    title: '行号',
    dataIndex: 'rowNumber',
    key: 'rowNumber',
    width: 80
  },
  {
    title: '字段',
    dataIndex: 'fieldName',
    key: 'fieldName',
    width: 120
  },
  {
    title: '原始值',
    dataIndex: 'originalValue',
    key: 'originalValue',
    width: 150,
    ellipsis: true
  },
  {
    title: '错误信息',
    dataIndex: 'errorMessage',
    key: 'errorMessage',
    ellipsis: true
  }
]

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
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
const detailData = ref<ImportRecordDetail | null>(null)

// ============= 方法 =============
// 获取数据列表
async function fetchData() {
  loading.value = true
  try {
    const params = {
      keyword: searchKeyword.value || undefined,
      dataType: searchDataType.value,
      status: searchStatus.value,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD'),
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getImportRecordList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取导入记录列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.current = 1
  fetchData()
}

// 重置搜索
function handleReset() {
  searchKeyword.value = ''
  searchDataType.value = undefined
  searchStatus.value = undefined
  searchDateRange.value = null
  pagination.current = 1
  fetchData()
}

// 表格变化
function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  fetchData()
}

// 查看详情
async function handleViewDetail(record: ImportRecord) {
  try {
    const res = await getImportRecordById(record.id)
    detailData.value = res.data
    detailModalVisible.value = true
  } catch (error) {
    console.error('获取导入记录详情失败:', error)
  }
}

// 重试导入
async function handleRetry(record: ImportRecord) {
  Modal.confirm({
    title: '确认重试',
    content: '确定要重新导入该文件吗？',
    okText: '确定',
    cancelText: '取消',
    async onOk() {
      try {
        await retryImport(record.id)
        message.success('已开始重新导入')
        fetchData()
      } catch (error) {
        console.error('重试失败:', error)
      }
    }
  })
}

// 下载错误报告
async function handleDownloadErrors(record: ImportRecord) {
  try {
    await downloadErrorReport(record.id)
    message.success('下载成功')
  } catch (error) {
    console.error('下载错误报告失败:', error)
  }
}

// 删除
async function handleDelete(record: ImportRecord) {
  try {
    await deleteImportRecord(record.id)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 批量删除
function handleBatchDelete() {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 条记录吗？`,
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await batchDeleteImportRecord(selectedRowKeys.value)
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
  fetchData()
})
</script>

<style lang="scss" scoped>
.import-record-page {
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

  .file-name {
    display: block;
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .file-size {
    font-size: $font-size-sm;
    color: $text-color-secondary;
  }

  .result-info {
    .success-count {
      color: $success-color;
    }
    .failed-count {
      color: $error-color;
    }
    .total-count {
      font-size: $font-size-sm;
      color: $text-color-secondary;
    }
  }

  .success-text {
    color: $success-color;
    font-weight: 600;
  }

  .failed-text {
    color: $error-color;
    font-weight: 600;
  }
}
</style>
