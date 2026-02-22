<template>
  <div class="rate-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">汇率列表</h1>
      <p class="page-desc">查看和管理已导入的汇率数据</p>
    </div>

    <!-- 搜索栏 -->
    <a-card class="search-card">
      <a-form layout="inline" :model="searchForm">
        <a-row :gutter="16" style="width: 100%">
          <a-col :span="4">
            <a-form-item>
              <a-select
                v-model:value="searchForm.currencyCode"
                placeholder="货币"
                allow-clear
                show-search
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option
                  v-for="currency in currencyOptions"
                  :key="currency.currencyCode"
                  :value="currency.currencyCode"
                >
                  {{ currency.currencyCode }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item>
              <a-select
                v-model:value="searchForm.source"
                placeholder="数据来源"
                allow-clear
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option v-for="item in sourceOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
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
          <a-col :span="6" style="text-align: right">
            <a-space>
              <a-button type="primary" @click="showSyncModal">
                <SyncOutlined /> 同步汇率
              </a-button>
              <a-button type="primary" @click="handleGoImport">
                <CloudUploadOutlined /> 导入汇率
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

    <!-- 货币转换工具 -->
    <a-card class="converter-card" title="货币转换（转人民币）">
      <a-form layout="inline">
        <a-form-item>
          <a-input-number
            v-model:value="converter.amount"
            :min="0"
            :precision="2"
            placeholder="金额"
            style="width: 150px"
          />
        </a-form-item>
        <a-form-item>
          <a-select
            v-model:value="converter.currencyCode"
            placeholder="货币"
            show-search
            style="width: 100px"
          >
            <a-select-option
              v-for="currency in currencyOptions"
              :key="currency.currencyCode"
              :value="currency.currencyCode"
            >
              {{ currency.currencyCode }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <SwapOutlined style="font-size: 18px; color: #1890ff" />
        </a-form-item>
        <a-form-item>
          <a-tag color="red">CNY</a-tag>
        </a-form-item>
        <a-form-item>
          <a-date-picker
            v-model:value="converter.date"
            placeholder="汇率日期"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="converting" @click="handleConvert">
            转换
          </a-button>
        </a-form-item>
        <a-form-item v-if="convertResult">
          <a-statistic
            :value="convertResult.convertedAmount"
            :precision="2"
            suffix="CNY"
            :value-style="{ color: '#52c41a', fontSize: '20px' }"
          />
          <span class="rate-info">汇率: {{ convertResult.rate?.toFixed(6) }}</span>
        </a-form-item>
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
          <!-- 货币 -->
          <template v-if="column.key === 'currencyCode'">
            <a-tag color="blue">{{ record.currencyCode }}</a-tag>
          </template>

          <!-- 汇率 -->
          <template v-else-if="column.key === 'rate'">
            <span class="rate-value">{{ record.rate?.toFixed(6) }}</span>
          </template>

          <!-- 是否工作日 -->
          <template v-else-if="column.key === 'isWorkday'">
            <a-tag :color="record.isWorkday ? 'green' : 'orange'">
              {{ record.isWorkday ? '是' : '否' }}
            </a-tag>
          </template>

          <!-- 数据来源 -->
          <template v-else-if="column.key === 'source'">
            <a-tag :color="getSourceColor(record.source)">
              {{ getSourceLabel(record.source) }}
            </a-tag>
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
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 同步汇率弹窗 -->
    <a-modal
      v-model:open="syncModalVisible"
      title="从中国外汇交易中心同步汇率"
      :confirm-loading="syncing"
      width="640px"
      :ok-text="'开始同步'"
      @ok="handleSync"
    >
      <a-form layout="vertical">
        <a-alert
          message="操作步骤"
          type="info"
          show-icon
          style="margin-bottom: 16px"
        >
          <template #description>
            <ol style="margin: 0; padding-left: 18px; line-height: 1.8">
              <li>打开 <a href="https://www.chinamoney.com.cn/chinese/bkccpr/" target="_blank" rel="noopener">中国货币网汇率页面</a></li>
              <li>按 <strong>F12</strong> 打开浏览器开发者工具，切换到 <strong>Network（网络）</strong> 标签</li>
              <li>在页面上查询汇率数据，在请求列表中找到 <strong>CcprHisNew</strong> 请求</li>
              <li>右键该请求 → <strong>Copy</strong> → <strong>Copy as cURL</strong></li>
              <li>将复制的完整 curl 命令粘贴到下方输入框</li>
            </ol>
          </template>
        </a-alert>
        <a-form-item label="操作示意图">
          <img
            src="@/assets/images/rate-sync-guide.jpg"
            alt="复制 curl 操作示意"
            style="max-width: 100%; border-radius: 4px; border: 1px solid #e8e8e8"
          />
        </a-form-item>
        <a-form-item label="粘贴 curl 命令（必填）">
          <a-textarea
            v-model:value="syncForm.curl"
            placeholder="粘贴从浏览器复制的完整 curl 命令，例如：curl 'https://www.chinamoney.com.cn/ags/ms/cm-u-bk-ccpr/CcprHisNew?...' -X 'POST' -H '...' -b '...'"
            :rows="6"
            allow-clear
          />
        </a-form-item>
      </a-form>
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
  SwapOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'
import {
  getRateList,
  deleteRate,
  batchDeleteRates,
  exportRateData,
  convertCurrency,
  syncFromCurl
} from '@/api/rate'
import { getEnabledCurrencies } from '@/api/currency'
import type { RateData, RateSource, RateConvertResult } from '@/types/rate'
import type { Currency } from '@/types/currency'

const router = useRouter()

// ============= 数据来源配置 =============
const sourceOptions = [
  { value: 'PBOC', label: '人民银行', color: 'blue' },
  { value: 'MANUAL', label: '手动录入', color: 'default' },
  { value: 'IMPORT', label: '文件导入', color: 'green' },
  { value: 'CHINA_MONEY', label: '外汇中心', color: 'cyan' }
]

function getSourceColor(source: string): string {
  return sourceOptions.find(s => s.value === source)?.color || 'default'
}

function getSourceLabel(source: string): string {
  return sourceOptions.find(s => s.value === source)?.label || source
}

// ============= 搜索相关 =============
const searchForm = reactive({
  currencyCode: undefined as string | undefined,
  source: undefined as RateSource | undefined
})
const searchDateRange = ref<[Dayjs, Dayjs] | null>(null)
const currencyOptions = ref<Currency[]>([])

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<RateData[]>([])
const selectedRowKeys = ref<number[]>([])

// ============= 货币转换 =============
const converter = reactive({
  amount: 100,
  currencyCode: 'USD',
  date: null as Dayjs | null
})
const converting = ref(false)
const convertResult = ref<RateConvertResult | null>(null)

// ============= 同步相关 =============
const syncModalVisible = ref(false)
const syncing = ref(false)
const syncForm = reactive({
  curl: ''
})

const columns = [
  {
    title: '货币',
    dataIndex: 'currencyCode',
    key: 'currencyCode',
    width: 100
  },
  {
    title: '汇率日期',
    dataIndex: 'rateDate',
    key: 'rateDate',
    width: 120
  },
  {
    title: '汇率(对人民币)',
    dataIndex: 'rate',
    key: 'rate',
    width: 150,
    align: 'right' as const
  },
  {
    title: '是否工作日',
    dataIndex: 'isWorkday',
    key: 'isWorkday',
    width: 100
  },
  {
    title: '实际汇率日期',
    dataIndex: 'actualRateDate',
    key: 'actualRateDate',
    width: 130
  },
  {
    title: '数据来源',
    dataIndex: 'source',
    key: 'source',
    width: 120
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 170
  },
  {
    title: '操作',
    key: 'action',
    width: 80,
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

// ============= 方法 =============
async function fetchCurrencies() {
  try {
    const res = await getEnabledCurrencies()
    currencyOptions.value = res.data || []
  } catch (error) {
    console.error('获取货币列表失败:', error)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      currencyCode: searchForm.currencyCode,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD'),
      source: searchForm.source,
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getRateList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取汇率列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchForm.currencyCode = undefined
  searchForm.source = undefined
  searchDateRange.value = null
  pagination.current = 1
  fetchData()
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  fetchData()
}

function showSyncModal() {
  syncForm.curl = ''
  syncModalVisible.value = true
}

async function handleSync() {
  const curl = syncForm.curl?.trim()
  if (!curl) {
    message.warning('请粘贴从浏览器复制的 curl 命令')
    return
  }
  if (!curl.toLowerCase().startsWith('curl')) {
    message.warning('请粘贴完整的 curl 命令（以 curl 开头）')
    return
  }

  syncing.value = true
  try {
    const res = await syncFromCurl(curl)
    const result = res.data
    if (result?.success) {
      message.success(result.message || '同步成功')
      syncModalVisible.value = false
      fetchData()
    } else {
      message.error(result?.message || '同步失败')
    }
  } catch (error) {
    console.error('同步失败:', error)
    message.error('同步失败，请确认 curl 来自 CcprHisNew 请求且格式正确')
  } finally {
    syncing.value = false
  }
}

function handleGoImport() {
  router.push('/rate/import')
}

async function handleExport() {
  try {
    const params = {
      currencyCode: searchForm.currencyCode,
      startDate: searchDateRange.value?.[0]?.format('YYYY-MM-DD'),
      endDate: searchDateRange.value?.[1]?.format('YYYY-MM-DD'),
      source: searchForm.source
    }
    await exportRateData(params)
    message.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
  }
}

async function handleConvert() {
  if (!converter.amount || !converter.currencyCode) {
    message.warning('请填写完整的转换信息')
    return
  }

  converting.value = true
  try {
    const res = await convertCurrency({
      amount: converter.amount,
      currencyCode: converter.currencyCode,
      rateDate: converter.date?.format('YYYY-MM-DD')
    })
    convertResult.value = res.data
  } catch (error) {
    console.error('转换失败:', error)
    message.error('转换失败，未找到对应汇率')
  } finally {
    converting.value = false
  }
}

async function handleDelete(record: RateData) {
  try {
    await deleteRate(record.id)
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
        await batchDeleteRates(selectedRowKeys.value)
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
  fetchCurrencies()
  fetchData()
})
</script>

<style lang="scss" scoped>
.rate-list-page {
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

  .converter-card {
    margin-bottom: $spacing-md;

    .rate-info {
      margin-left: $spacing-sm;
      color: $text-color-secondary;
      font-size: $font-size-sm;
    }
  }

  .currency-pair {
    display: inline-flex;
    align-items: center;
    gap: $spacing-xs;
  }

  .rate-value {
    font-family: 'Monaco', 'Menlo', monospace;
    font-weight: 500;

    &.secondary {
      color: $text-color-secondary;
    }
  }
}
</style>
