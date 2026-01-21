<template>
  <div class="marketplace-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">站点管理</h1>
      <p class="page-desc">管理亚马逊各站点配置，包括站点代码、国家、区域、币种、时区等信息</p>
    </div>

    <!-- 搜索和操作栏 -->
    <a-card class="search-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="5">
          <a-input
            v-model:value="searchSiteCode"
            placeholder="搜索站点代码"
            allow-clear
            @pressEnter="handleSearch"
          >
            <template #prefix>
              <SearchOutlined />
            </template>
          </a-input>
        </a-col>
        <a-col :span="4">
          <a-select
            v-model:value="searchStatus"
            placeholder="状态"
            allow-clear
            style="width: 100%"
            @change="handleSearch"
          >
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
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
        <a-col :span="7" style="text-align: right">
          <a-space>
            <a-button type="primary" @click="handleAdd">
              <PlusOutlined /> 新增站点
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
          <!-- 站点代码 -->
          <template v-if="column.key === 'siteCode'">
            <a-tag color="blue">{{ record.siteCode }}</a-tag>
          </template>

          <!-- 状态 -->
          <template v-else-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 1"
              checked-children="启用"
              un-checked-children="禁用"
              @change="(checked: boolean) => handleStatusChange(record, checked)"
            />
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <EditOutlined /> 编辑
              </a-button>
              <a-popconfirm
                title="确定要删除该站点吗？"
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

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑站点' : '新增站点'"
      :confirm-loading="submitLoading"
      width="600px"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="站点代码" name="siteCode">
          <a-input
            v-model:value="formData.siteCode"
            placeholder="请输入站点代码，如 US、DE、UK"
            :disabled="isEdit"
            :maxlength="10"
          />
        </a-form-item>

        <a-form-item label="站点名称" name="siteName">
          <a-input
            v-model:value="formData.siteName"
            placeholder="请输入站点名称，如 美国站、德国站"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item label="Marketplace ID" name="marketplaceId">
          <a-input
            v-model:value="formData.marketplaceId"
            placeholder="亚马逊 Marketplace ID"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item label="Seller ID" name="sellerId">
          <a-input
            v-model:value="formData.sellerId"
            placeholder="卖家 ID"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item label="货币代码" name="currencyCode">
          <a-select
            v-model:value="formData.currencyCode"
            placeholder="请选择货币"
            show-search
            :filter-option="filterCurrencyOption"
          >
            <a-select-option
              v-for="currency in currencyOptions"
              :key="currency.currencyCode"
              :value="currency.currencyCode"
            >
              {{ currency.currencySymbol }} {{ currency.currencyCode }} - {{ currency.currencyName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="表头语言" name="headerLanguage">
          <a-select
            v-model:value="formData.headerLanguage"
            placeholder="请选择表头语言"
          >
            <a-select-option value="EN">英文 (EN)</a-select-option>
            <a-select-option value="DE">德文 (DE)</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="日期格式" name="dateFormat">
          <a-select
            v-model:value="formData.dateFormat"
            placeholder="请选择日期格式"
          >
            <a-select-option value="yyyy-MM-dd">yyyy-MM-dd</a-select-option>
            <a-select-option value="MM/dd/yyyy">MM/dd/yyyy</a-select-option>
            <a-select-option value="dd/MM/yyyy">dd/MM/yyyy</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="数字格式" name="numberFormat">
          <a-select
            v-model:value="formData.numberFormat"
            placeholder="请选择小数点格式"
          >
            <a-select-option value=".">点号 (.)</a-select-option>
            <a-select-option value=",">逗号 (,)</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="时区" name="timezone">
          <a-select
            v-model:value="formData.timezone"
            placeholder="请选择时区"
            show-search
          >
            <a-select-option value="America/Los_Angeles">太平洋时间 (PST)</a-select-option>
            <a-select-option value="America/New_York">东部时间 (EST)</a-select-option>
            <a-select-option value="Europe/London">伦敦时间 (GMT)</a-select-option>
            <a-select-option value="Europe/Berlin">柏林时间 (CET)</a-select-option>
            <a-select-option value="Asia/Tokyo">东京时间 (JST)</a-select-option>
            <a-select-option value="Asia/Shanghai">北京时间 (CST)</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, TablePaginationConfig } from 'ant-design-vue'
import type { TableRowSelection } from 'ant-design-vue/es/table/interface'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined
} from '@ant-design/icons-vue'
import {
  getMarketplaceList,
  createMarketplace,
  updateMarketplace,
  deleteMarketplace,
  batchDeleteMarketplace
} from '@/api/marketplace'
import { getEnabledCurrencies } from '@/api/currency'
import type { Marketplace, MarketplaceForm } from '@/types/marketplace'
import type { Currency } from '@/types/currency'

// ============= 搜索相关 =============
const searchSiteCode = ref('')
const searchStatus = ref<number | undefined>(undefined)

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<Marketplace[]>([])
const selectedRowKeys = ref<number[]>([])
const currencyOptions = ref<Currency[]>([])

const columns = [
  {
    title: '站点代码',
    dataIndex: 'siteCode',
    key: 'siteCode',
    width: 100,
    fixed: 'left' as const
  },
  {
    title: '站点名称',
    dataIndex: 'siteName',
    key: 'siteName',
    width: 150
  },
  {
    title: 'Marketplace ID',
    dataIndex: 'marketplaceId',
    key: 'marketplaceId',
    width: 180
  },
  {
    title: '货币',
    dataIndex: 'currencyCode',
    key: 'currencyCode',
    width: 80
  },
  {
    title: '时区',
    dataIndex: 'timezone',
    key: 'timezone',
    width: 160
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
    align: 'center' as const
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
    width: 150,
    fixed: 'right' as const
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

// ============= 弹窗相关 =============
const modalVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const formData = reactive<MarketplaceForm>({
  siteCode: '',
  siteName: '',
  marketplaceId: '',
  currencyCode: '',
  sellerId: '',
  headerLanguage: 'EN',
  dateFormat: 'yyyy-MM-dd',
  numberFormat: '.',
  timezone: '',
  status: 1
})

const formRules = {
  siteCode: [
    { required: true, message: '请输入站点代码', trigger: 'blur' },
    { min: 2, max: 10, message: '站点代码长度为2-10位', trigger: 'blur' }
  ],
  siteName: [
    { required: true, message: '请输入站点名称', trigger: 'blur' },
    { max: 50, message: '站点名称不能超过50个字符', trigger: 'blur' }
  ],
  currencyCode: [
    { required: true, message: '请选择货币', trigger: 'change' }
  ]
}

// ============= 辅助方法 =============
function filterCurrencyOption(input: string, option: any): boolean {
  const currency = option.value?.toLowerCase() || ''
  return currency.includes(input.toLowerCase())
}

// ============= 方法 =============
// 获取数据列表
async function fetchData() {
  loading.value = true
  try {
    const params = {
      siteCode: searchSiteCode.value || undefined,
      status: searchStatus.value,
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getMarketplaceList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取站点列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取货币选项
async function fetchCurrencyOptions() {
  try {
    const res = await getEnabledCurrencies()
    currencyOptions.value = res.data || []
  } catch (error) {
    console.error('获取货币列表失败:', error)
  }
}

// 搜索
function handleSearch() {
  pagination.current = 1
  fetchData()
}

// 重置搜索
function handleReset() {
  searchSiteCode.value = ''
  searchStatus.value = undefined
  pagination.current = 1
  fetchData()
}

// 表格变化
function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  fetchData()
}

// 新增
function handleAdd() {
  isEdit.value = false
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 编辑
function handleEdit(record: Marketplace) {
  isEdit.value = true
  editingId.value = record.id
  Object.assign(formData, {
    siteCode: record.siteCode,
    siteName: record.siteName,
    marketplaceId: record.marketplaceId || '',
    currencyCode: record.currencyCode,
    sellerId: record.sellerId || '',
    headerLanguage: record.headerLanguage || 'EN',
    dateFormat: record.dateFormat || 'yyyy-MM-dd',
    numberFormat: record.numberFormat || '.',
    timezone: record.timezone || '',
    status: record.status
  })
  modalVisible.value = true
}

// 删除
async function handleDelete(record: Marketplace) {
  try {
    await deleteMarketplace(record.id)
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
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个站点吗？`,
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await batchDeleteMarketplace(selectedRowKeys.value)
        message.success('批量删除成功')
        selectedRowKeys.value = []
        fetchData()
      } catch (error) {
        console.error('批量删除失败:', error)
      }
    }
  })
}

// 状态切换
async function handleStatusChange(record: Marketplace, checked: boolean) {
  try {
    await updateMarketplace(record.id, {
      siteCode: record.siteCode,
      siteName: record.siteName,
      marketplaceId: record.marketplaceId,
      currencyCode: record.currencyCode,
      sellerId: record.sellerId,
      headerLanguage: record.headerLanguage,
      dateFormat: record.dateFormat,
      numberFormat: record.numberFormat,
      timezone: record.timezone,
      status: checked ? 1 : 0
    })
    message.success(checked ? '已启用' : '已禁用')
    fetchData()
  } catch (error) {
    console.error('状态更新失败:', error)
  }
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    if (isEdit.value && editingId.value) {
      await updateMarketplace(editingId.value, formData)
      message.success('更新成功')
    } else {
      await createMarketplace(formData)
      message.success('创建成功')
    }

    modalVisible.value = false
    fetchData()
  } catch (error: any) {
    if (error?.errorFields) {
      return
    }
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// 取消
function handleCancel() {
  modalVisible.value = false
  resetForm()
}

// 重置表单
function resetForm() {
  formRef.value?.resetFields()
  Object.assign(formData, {
    siteCode: '',
    siteName: '',
    marketplaceId: '',
    currencyCode: '',
    sellerId: '',
    headerLanguage: 'EN',
    dateFormat: 'yyyy-MM-dd',
    numberFormat: '.',
    timezone: '',
    status: 1
  })
}

// 初始化
onMounted(() => {
  fetchData()
  fetchCurrencyOptions()
})
</script>

<style lang="scss" scoped>
.marketplace-page {
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
    // 表格样式
  }
}
</style>
