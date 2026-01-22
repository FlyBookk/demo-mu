<template>
  <div class="currency-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">货币管理</h1>
      <p class="page-desc">管理系统支持的货币类型，包括货币代码、名称、符号等信息</p>
    </div>

    <!-- 搜索和操作栏 -->
    <a-card class="search-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-input
            v-model:value="searchKeyword"
            placeholder="搜索货币代码或名称"
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
        <a-col :span="10" style="text-align: right">
          <a-space>
            <a-button type="primary" @click="handleAdd">
              <PlusOutlined /> 新增货币
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
        row-key="id"
        @change="handleTableChange"
      >
        <!-- 货币代码 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'currencyCode'">
            <a-tag color="blue">{{ record.currencyCode }}</a-tag>
          </template>

          <!-- 货币符号 -->
          <template v-else-if="column.key === 'currencySymbol'">
            <span class="currency-symbol">{{ record.currencySymbol }}</span>
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
                title="确定要删除该货币吗？"
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
      :title="isEdit ? '编辑货币' : '新增货币'"
      :confirm-loading="submitLoading"
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
        <a-form-item label="货币代码" name="currencyCode">
          <a-input
            v-model:value="formData.currencyCode"
            placeholder="请输入货币代码，如 USD、EUR"
            :disabled="isEdit"
            :maxlength="10"
          />
        </a-form-item>

        <a-form-item label="货币名称" name="currencyName">
          <a-input
            v-model:value="formData.currencyName"
            placeholder="请输入货币名称，如 美元、欧元"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item label="货币符号" name="currencySymbol">
          <a-input
            v-model:value="formData.currencySymbol"
            placeholder="请输入货币符号，如 $、€"
            :maxlength="10"
          />
        </a-form-item>

        <a-form-item label="小数位数" name="decimalPlaces">
          <a-input-number
            v-model:value="formData.decimalPlaces"
            :min="0"
            :max="6"
            style="width: 100%"
          />
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
  getCurrencyList,
  createCurrency,
  updateCurrency,
  deleteCurrency,
  batchDeleteCurrency
} from '@/api/currency'
import type { Currency, CurrencyForm } from '@/types/currency'

// ============= 搜索相关 =============
const searchKeyword = ref('')
const searchStatus = ref<number | undefined>(undefined)

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<Currency[]>([])
const selectedRowKeys = ref<number[]>([])

const columns = [
  {
    title: '货币代码',
    dataIndex: 'currencyCode',
    key: 'currencyCode',
    width: 120
  },
  {
    title: '货币名称',
    dataIndex: 'currencyName',
    key: 'currencyName',
    width: 150
  },
  {
    title: '货币符号',
    dataIndex: 'currencySymbol',
    key: 'currencySymbol',
    width: 100,
    align: 'center' as const
  },
  {
    title: '小数位数',
    dataIndex: 'decimalPlaces',
    key: 'decimalPlaces',
    width: 100,
    align: 'center' as const
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 120,
    align: 'center' as const
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180
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

const formData = reactive<CurrencyForm>({
  currencyCode: '',
  currencyName: '',
  currencySymbol: '',
  decimalPlaces: 2,
  status: 1
})

const formRules = {
  currencyCode: [
    { required: true, message: '请输入货币代码', trigger: 'blur' },
    { min: 2, max: 10, message: '货币代码长度为2-10位', trigger: 'blur' },
    { pattern: /^[A-Z]+$/, message: '货币代码只能包含大写字母', trigger: 'blur' }
  ],
  currencyName: [
    { required: true, message: '请输入货币名称', trigger: 'blur' },
    { max: 50, message: '货币名称不能超过50个字符', trigger: 'blur' }
  ],
  currencySymbol: [
    { required: true, message: '请输入货币符号', trigger: 'blur' },
    { max: 10, message: '货币符号不能超过10个字符', trigger: 'blur' }
  ]
}

// ============= 方法 =============
// 获取数据列表
async function fetchData() {
  loading.value = true
  try {
    // 后端参数: currencyCode, currencyName, status, page, size
    const params = {
      currencyCode: searchKeyword.value || undefined,
      status: searchStatus.value,
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getCurrencyList(params)
    // API返回结构: { code, message, data: { records, total, ... } }
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取货币列表失败:', error)
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
function handleEdit(record: Currency) {
  isEdit.value = true
  editingId.value = record.id
  Object.assign(formData, {
    currencyCode: record.currencyCode,
    currencyName: record.currencyName,
    currencySymbol: record.currencySymbol,
    decimalPlaces: record.decimalPlaces,
    status: record.status
  })
  modalVisible.value = true
}

// 删除
async function handleDelete(record: Currency) {
  try {
    await deleteCurrency(record.id)
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
        await batchDeleteCurrency(selectedRowKeys.value)
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
async function handleStatusChange(record: Currency, checked: boolean) {
  try {
    await updateCurrency(record.id, {
      currencyCode: record.currencyCode,
      currencyName: record.currencyName,
      currencySymbol: record.currencySymbol,
      decimalPlaces: record.decimalPlaces,
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
      await updateCurrency(editingId.value, formData)
      message.success('更新成功')
    } else {
      await createCurrency(formData)
      message.success('创建成功')
    }

    modalVisible.value = false
    fetchData()
  } catch (error: any) {
    if (error?.errorFields) {
      // 表单校验失败
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
    currencyCode: '',
    currencyName: '',
    currencySymbol: '',
    decimalPlaces: 2,
    status: 1
  })
}

// 初始化
onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.currency-page {
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
    .currency-symbol {
      display: inline-block;
      width: 32px;
      height: 32px;
      line-height: 32px;
      text-align: center;
      background: $background-color-light;
      border-radius: $border-radius-md;
      font-weight: 600;
      font-size: $font-size-lg;
      color: $primary-color;
    }
  }
}
</style>
