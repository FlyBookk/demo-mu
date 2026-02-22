<template>
  <div class="rate-manage-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">汇率管理</h1>
      <p class="page-desc">管理汇率数据，支持手动录入、修改和自动同步</p>
    </div>

    <!-- 操作栏 -->
    <a-card class="action-card">
      <a-space size="middle">
        <a-button type="primary" @click="handleAdd">
          <PlusOutlined /> 新增汇率
        </a-button>
        <a-button type="primary" ghost @click="showSyncModal">
          <SyncOutlined /> 同步汇率
        </a-button>
        <a-button @click="handleExport">
          <DownloadOutlined /> 导出
        </a-button>
        <a-button
          danger
          :disabled="selectedRowKeys.length === 0"
          @click="handleBatchDelete"
        >
          <DeleteOutlined /> 批量删除 ({{ selectedRowKeys.length }})
        </a-button>
      </a-space>
    </a-card>

    <!-- 搜索栏 -->
    <a-card class="search-card">
      <a-form layout="inline" :model="searchForm">
        <a-form-item label="货币">
          <a-select
            v-model:value="searchForm.currencyCode"
            placeholder="全部"
            allow-clear
            show-search
            style="width: 120px"
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
        <a-form-item label="年月">
          <a-date-picker
            v-model:value="searchForm.yearMonth"
            picker="month"
            format="YYYY-MM"
            style="width: 150px"
          />
        </a-form-item>
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
          <template v-if="column.key === 'currencyCode'">
            <a-tag color="blue">{{ record.currencyCode }}</a-tag>
          </template>
          <template v-else-if="column.key === 'rate'">
            <span class="rate-value">{{ record.rate?.toFixed(6) }}</span>
          </template>
          <template v-else-if="column.key === 'isWorkday'">
            <a-tag :color="record.isWorkday ? 'green' : 'orange'">
              {{ record.isWorkday ? '是' : '否' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'source'">
            <a-tag :color="getSourceColor(record.source)">
              {{ getSourceLabel(record.source) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
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

    <!-- 新增/编辑汇率弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="formMode === 'add' ? '新增汇率' : '编辑汇率'"
      :width="600"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="货币编码"
          name="currencyCode"
          :rules="[{ required: true, message: '请选择货币' }]"
        >
          <a-select
            v-model:value="formData.currencyCode"
            placeholder="请选择货币"
            show-search
          >
            <a-select-option
              v-for="currency in currencyOptions"
              :key="currency.currencyCode"
              :value="currency.currencyCode"
            >
              {{ currency.currencyCode }} - {{ currency.currencyName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          label="汇率日期"
          name="rateDate"
          :rules="[{ required: true, message: '请选择日期' }]"
        >
          <a-date-picker
            v-model:value="formData.rateDate"
            style="width: 100%"
            placeholder="请选择日期"
          />
        </a-form-item>

        <a-form-item
          label="汇率"
          name="rate"
          :rules="[
            { required: true, message: '请输入汇率' },
            { type: 'number', min: 0, message: '汇率必须大于0' }
          ]"
        >
          <a-input-number
            v-model:value="formData.rate"
            :precision="6"
            :min="0"
            :step="0.000001"
            style="width: 100%"
            placeholder="请输入汇率（对人民币）"
          />
        </a-form-item>

        <a-form-item label="是否工作日" name="isWorkday">
          <a-radio-group v-model:value="formData.isWorkday">
            <a-radio :value="1">是</a-radio>
            <a-radio :value="0">否</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="数据来源" name="source">
          <a-input v-model:value="formData.source" placeholder="默认为MANUAL" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 同步汇率弹窗（curl 方式） -->
    <a-modal
      v-model:open="syncVisible"
      title="同步汇率"
      :width="700"
      :confirm-loading="syncing"
      @ok="handleSync"
      @cancel="syncVisible = false"
    >
      <a-alert
        message="curl 同步说明"
        description="请打开中国货币网汇率页面，F12 → Network → 找到 CcprHisNew 请求 → 右键 Copy as cURL，粘贴到下方。"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-form-item label="curl 命令">
        <a-textarea
          v-model:value="syncForm.curl"
          placeholder="粘贴完整的 curl 命令..."
          :rows="6"
          allow-clear
        />
      </a-form-item>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, TablePaginationConfig } from 'ant-design-vue'
import type { TableRowSelection } from 'ant-design-vue/es/table/interface'
import type { Dayjs } from 'dayjs'
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  SyncOutlined,
  DownloadOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import {
  getRateList,
  createRate,
  updateRate,
  deleteRate,
  batchDeleteRates,
  syncFromCurl,
  exportRateData
} from '@/api/rate'
import { getEnabledCurrencies } from '@/api/currency'
import type { RateData, RateRequest } from '@/types/rate'
import type { Currency } from '@/types/currency'

// 数据来源配置
const sourceOptions = [
  { value: 'MANUAL', label: '手动录入', color: 'default' },
  { value: 'CHINA_MONEY', label: '外汇中心', color: 'blue' },
  { value: 'IMPORT', label: '文件导入', color: 'green' }
]

function getSourceColor(source: string): string {
  return sourceOptions.find(s => s.value === source)?.color || 'default'
}

function getSourceLabel(source: string): string {
  return sourceOptions.find(s => s.value === source)?.label || source
}

// 搜索相关
const searchForm = reactive({
  currencyCode: undefined as string | undefined,
  yearMonth: null as Dayjs | null
})

const currencyOptions = ref<Currency[]>([])

// 表格相关
const loading = ref(false)
const tableData = ref<RateData[]>([])
const selectedRowKeys = ref<number[]>([])

const columns = [
  { title: '货币', dataIndex: 'currencyCode', key: 'currencyCode', width: 100 },
  { title: '汇率日期', dataIndex: 'rateDate', key: 'rateDate', width: 120 },
  { title: '汇率(对人民币)', dataIndex: 'rate', key: 'rate', width: 150, align: 'right' as const },
  { title: '是否工作日', dataIndex: 'isWorkday', key: 'isWorkday', width: 100 },
  { title: '数据来源', dataIndex: 'source', key: 'source', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const }
]

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const rowSelection = computed<TableRowSelection>(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => {
    selectedRowKeys.value = keys as number[]
  }
}))

// 表单相关
const formRef = ref<FormInstance>()
const formVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formData = reactive<RateRequest & { id?: number }>({
  currencyCode: '',
  rateDate: null as unknown as Dayjs,
  rate: undefined as unknown as number,
  isWorkday: 1,
  source: 'MANUAL'
})

// 同步相关
const syncVisible = ref(false)
const syncing = ref(false)
const syncForm = reactive({
  curl: ''
})

// 方法
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
      yearMonth: searchForm.yearMonth?.format('YYYY-MM'),
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getRateList(params)
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
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
  searchForm.yearMonth = null
  pagination.current = 1
  fetchData()
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  fetchData()
}

function handleAdd() {
  formMode.value = 'add'
  Object.assign(formData, {
    currencyCode: '',
    rateDate: null,
    rate: undefined,
    isWorkday: 1,
    source: 'MANUAL'
  })
  formVisible.value = true
}

function handleEdit(record: RateData) {
  formMode.value = 'edit'
  Object.assign(formData, {
    id: record.id,
    currencyCode: record.currencyCode,
    rateDate: record.rateDate,
    rate: record.rate,
    isWorkday: record.isWorkday,
    source: record.source
  })
  formVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()

    const data = {
      currencyCode: formData.currencyCode,
      rateDate: formData.rateDate?.format('YYYY-MM-DD'),
      rate: formData.rate,
      isWorkday: formData.isWorkday,
      source: formData.source
    }

    if (formMode.value === 'add') {
      await createRate(data)
      message.success('新增成功')
    } else {
      await updateRate(formData.id!, data)
      message.success('修改成功')
    }

    formVisible.value = false
    fetchData()
  } catch (error) {
    console.error('提交失败:', error)
  }
}

function handleCancel() {
  formVisible.value = false
  formRef.value?.resetFields()
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

function showSyncModal() {
  syncForm.curl = ''
  syncVisible.value = true
}

async function handleSync() {
  const curl = syncForm.curl?.trim()
  if (!curl) {
    message.warning('请粘贴 curl 命令')
    return
  }
  syncing.value = true
  try {
    const result = await syncFromCurl(curl)
    const data = result.data
    message.success(data?.message || '同步完成')
    syncVisible.value = false
    fetchData()
  } catch (error) {
    console.error('同步失败:', error)
    message.error('同步失败，请检查 curl 是否来自中国货币网 CcprHisNew 请求')
  } finally {
    syncing.value = false
  }
}

async function handleExport() {
  try {
    await exportRateData({
      currencyCode: searchForm.currencyCode,
      yearMonth: searchForm.yearMonth?.format('YYYY-MM')
    })
    message.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
  }
}

onMounted(() => {
  fetchCurrencies()
  fetchData()
})
</script>

<style lang="scss" scoped>
.rate-manage-page {
  padding: 24px;

  .page-header {
    margin-bottom: 24px;

    .page-title {
      font-size: 24px;
      font-weight: 600;
      margin: 0 0 8px 0;
    }

    .page-desc {
      font-size: 14px;
      color: #888;
      margin: 0;
    }
  }

  .action-card {
    margin-bottom: 16px;
  }

  .search-card {
    margin-bottom: 16px;
  }

  .rate-value {
    font-family: 'Monaco', 'Menlo', monospace;
    font-weight: 500;
  }
}
</style>
