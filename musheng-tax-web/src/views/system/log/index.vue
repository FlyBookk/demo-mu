<template>
  <div class="log-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">操作日志</h1>
      <p class="page-desc">查看系统操作日志，追踪用户操作记录</p>
    </div>

    <!-- 搜索和操作栏 -->
    <a-card class="search-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="4">
          <a-input
            v-model:value="searchParams.username"
            placeholder="操作人"
            allow-clear
            @pressEnter="handleSearch"
          >
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-col>
        <a-col :span="4">
          <a-input
            v-model:value="searchParams.module"
            placeholder="模块"
            allow-clear
            @pressEnter="handleSearch"
          />
        </a-col>
        <a-col :span="4">
          <a-input
            v-model:value="searchParams.operation"
            placeholder="操作"
            allow-clear
            @pressEnter="handleSearch"
          />
        </a-col>
        <a-col :span="3">
          <a-select
            v-model:value="searchParams.status"
            placeholder="状态"
            allow-clear
            style="width: 100%"
            @change="handleSearch"
          >
            <a-select-option :value="1">成功</a-select-option>
            <a-select-option :value="0">失败</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="5">
          <a-range-picker
            v-model:value="dateRange"
            :placeholder="['开始时间', '结束时间']"
            style="width: 100%"
            @change="handleDateChange"
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
      </a-row>
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
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 操作人 -->
          <template v-if="column.key === 'username'">
            <div class="user-info">
              <a-avatar :size="24" style="background-color: #1890ff">
                {{ record.username?.charAt(0) || 'U' }}
              </a-avatar>
              <span class="username">{{ record.username }}</span>
            </div>
          </template>

          <!-- 模块/操作 -->
          <template v-else-if="column.key === 'module'">
            <a-tag color="blue">{{ record.module }}</a-tag>
          </template>

          <template v-else-if="column.key === 'operation'">
            <span>{{ record.operation }}</span>
          </template>

          <!-- 请求方法 -->
          <template v-else-if="column.key === 'method'">
            <a-tag :color="getMethodColor(record.method)">
              {{ getMethodName(record.method) }}
            </a-tag>
          </template>

          <!-- 状态 -->
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'error'">
              {{ record.status === 1 ? '成功' : '失败' }}
            </a-tag>
          </template>

          <!-- 执行时长 -->
          <template v-else-if="column.key === 'executionTime'">
            <span :class="getDurationClass(record.executionTime)">
              {{ record.executionTime }}ms
            </span>
          </template>

          <!-- 请求URL -->
          <template v-else-if="column.key === 'requestUrl'">
            <a-tooltip :title="record.requestUrl">
              <span class="ellipsis-text">{{ record.requestUrl }}</span>
            </a-tooltip>
          </template>

          <!-- IP地址 -->
          <template v-else-if="column.key === 'ip'">
            <span>{{ record.ip || '-' }}</span>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="handleViewDetail(record)">
              <EyeOutlined /> 详情
            </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailModalVisible"
      title="日志详情"
      :footer="null"
      :width="800"
    >
      <a-descriptions
        v-if="currentLog"
        :column="2"
        bordered
        size="small"
      >
        <a-descriptions-item label="日志ID">
          {{ currentLog.id }}
        </a-descriptions-item>
        <a-descriptions-item label="操作时间">
          {{ currentLog.createTime }}
        </a-descriptions-item>
        <a-descriptions-item label="操作人">
          {{ currentLog.username }}
        </a-descriptions-item>
        <a-descriptions-item label="用户ID">
          {{ currentLog.userId }}
        </a-descriptions-item>
        <a-descriptions-item label="模块">
          <a-tag color="blue">{{ currentLog.module }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="操作">
          {{ currentLog.operation }}
        </a-descriptions-item>
        <a-descriptions-item label="请求方法">
          <a-tag :color="getMethodColor(currentLog.method)">
            {{ getMethodName(currentLog.method) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="执行状态">
          <a-tag :color="currentLog.status === 1 ? 'success' : 'error'">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="请求URL" :span="2">
          <code>{{ currentLog.requestUrl }}</code>
        </a-descriptions-item>
        <a-descriptions-item label="执行时长">
          <span :class="getDurationClass(currentLog.executionTime)">
            {{ currentLog.executionTime }}ms
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="IP地址">
          {{ currentLog.ip || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="User-Agent" :span="2">
          <div class="user-agent-text">{{ currentLog.userAgent || '-' }}</div>
        </a-descriptions-item>
        <a-descriptions-item label="请求参数" :span="2">
          <div class="code-block">
            <pre>{{ formatJson(currentLog.requestParams) }}</pre>
          </div>
        </a-descriptions-item>
        <a-descriptions-item v-if="currentLog.status === 0" label="错误信息" :span="2">
          <a-alert
            :message="currentLog.errorMsg || '未知错误'"
            type="error"
            show-icon
          />
        </a-descriptions-item>
        <a-descriptions-item v-if="currentLog.responseData" label="响应数据" :span="2">
          <div class="code-block">
            <pre>{{ formatJson(currentLog.responseData) }}</pre>
          </div>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import {
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { getOperationLogList, getOperationLogById } from '@/api/log'
import type { OperationLog, OperationLogQuery } from '@/types/log'

// ============= 搜索相关 =============
const searchParams = reactive<OperationLogQuery>({
  username: '',
  module: '',
  operation: '',
  status: undefined
})
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<OperationLog[]>([])

const columns = [
  {
    title: '操作人',
    dataIndex: 'username',
    key: 'username',
    width: 140,
    fixed: 'left' as const
  },
  {
    title: '模块',
    dataIndex: 'module',
    key: 'module',
    width: 120
  },
  {
    title: '操作',
    dataIndex: 'operation',
    key: 'operation',
    width: 140
  },
  {
    title: '请求方法',
    dataIndex: 'method',
    key: 'method',
    width: 100,
    align: 'center' as const
  },
  {
    title: '请求URL',
    dataIndex: 'requestUrl',
    key: 'requestUrl',
    width: 250,
    ellipsis: true
  },
  {
    title: 'IP地址',
    dataIndex: 'ip',
    key: 'ip',
    width: 130
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80,
    align: 'center' as const
  },
  {
    title: '耗时',
    dataIndex: 'executionTime',
    key: 'executionTime',
    width: 100,
    align: 'right' as const
  },
  {
    title: '操作时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 80,
    fixed: 'right' as const,
    align: 'center' as const
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

// ============= 详情弹窗相关 =============
const detailModalVisible = ref(false)
const currentLog = ref<OperationLog | null>(null)

// ============= 工具函数 =============
function getMethodColor(method: string): string {
  const colorMap: Record<string, string> = {
    'GET': 'blue',
    'POST': 'green',
    'PUT': 'orange',
    'DELETE': 'red',
    'PATCH': 'purple'
  }
  // 从方法字符串中提取HTTP方法
  const httpMethod = method?.split(' ')?.[0]?.toUpperCase() || method?.toUpperCase()
  return colorMap[httpMethod] || 'default'
}

function getMethodName(method: string): string {
  // 如果方法字符串包含空格（如 "POST /api/xxx"），提取HTTP方法
  const parts = method?.split(' ')
  if (parts && parts.length > 0) {
    return parts[0].toUpperCase()
  }
  return method || '-'
}

function getDurationClass(duration: number): string {
  if (duration < 100) return 'duration-fast'
  if (duration < 500) return 'duration-normal'
  if (duration < 1000) return 'duration-slow'
  return 'duration-very-slow'
}

function formatJson(str: string | undefined): string {
  if (!str) return '-'
  try {
    const obj = JSON.parse(str)
    return JSON.stringify(obj, null, 2)
  } catch {
    return str
  }
}

// ============= 方法 =============
// 获取日志列表
async function fetchData() {
  loading.value = true
  try {
    const params: OperationLogQuery = {
      username: searchParams.username || undefined,
      module: searchParams.module || undefined,
      operation: searchParams.operation || undefined,
      status: searchParams.status,
      startTime: dateRange.value?.[0]?.format('YYYY-MM-DD HH:mm:ss'),
      endTime: dateRange.value?.[1]?.format('YYYY-MM-DD HH:mm:ss'),
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getOperationLogList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取操作日志列表失败:', error)
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
  searchParams.username = ''
  searchParams.module = ''
  searchParams.operation = ''
  searchParams.status = undefined
  dateRange.value = null
  pagination.current = 1
  fetchData()
}

// 日期变化
function handleDateChange() {
  handleSearch()
}

// 表格变化
function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  fetchData()
}

// 查看详情
async function handleViewDetail(record: OperationLog) {
  try {
    const res = await getOperationLogById(record.id)
    currentLog.value = res.data
    detailModalVisible.value = true
  } catch (error) {
    console.error('获取日志详情失败:', error)
    // 如果获取详情失败，使用列表中的数据
    currentLog.value = record
    detailModalVisible.value = true
  }
}

// 初始化
onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.log-page {
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

  .table-card {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;

      .username {
        font-weight: 500;
      }
    }

    .ellipsis-text {
      display: inline-block;
      max-width: 230px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .duration-fast {
      color: #52c41a;
    }

    .duration-normal {
      color: #1890ff;
    }

    .duration-slow {
      color: #fa8c16;
    }

    .duration-very-slow {
      color: #f5222d;
      font-weight: 500;
    }
  }
}

.user-agent-text {
  word-break: break-all;
  font-size: 12px;
  color: #666;
}

.code-block {
  max-height: 200px;
  overflow: auto;
  background: #f5f5f5;
  border-radius: 4px;
  padding: 8px;

  pre {
    margin: 0;
    font-size: 12px;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

.duration-fast {
  color: #52c41a;
}

.duration-normal {
  color: #1890ff;
}

.duration-slow {
  color: #fa8c16;
}

.duration-very-slow {
  color: #f5222d;
  font-weight: 500;
}
</style>
