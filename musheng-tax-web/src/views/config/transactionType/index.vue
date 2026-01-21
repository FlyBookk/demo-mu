<template>
  <div class="transaction-type-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">交易类型映射</h1>
      <p class="page-desc">管理亚马逊交易类型到系统标准类型的映射关系，用于数据导入时自动分类</p>
    </div>

    <!-- 搜索和操作栏 -->
    <a-card class="search-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="5">
          <a-input
            v-model:value="searchKeyword"
            placeholder="搜索源类型或目标类型"
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
            v-model:value="searchStandardCategory"
            placeholder="标准分类"
            allow-clear
            style="width: 100%"
            @change="handleSearch"
          >
            <a-select-option v-for="item in standardCategoryOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
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
              <PlusOutlined /> 新增映射
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
        <template #bodyCell="{ column, record }">
          <!-- 站点 -->
          <template v-if="column.key === 'siteCode'">
            <a-tag v-if="record.siteCode" color="blue">{{ record.siteCode }}</a-tag>
            <span v-else>通用</span>
          </template>

          <!-- 原始交易类型 -->
          <template v-else-if="column.key === 'originalType'">
            <a-typography-text code>{{ record.originalType }}</a-typography-text>
          </template>

          <!-- 标准分类 -->
          <template v-else-if="column.key === 'standardCategory'">
            <a-tag :color="getStandardCategoryColor(record.standardCategory)">
              {{ getStandardCategoryLabel(record.standardCategory) }}
            </a-tag>
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
                title="确定要删除该映射吗？"
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
      :title="isEdit ? '编辑映射' : '新增映射'"
      :confirm-loading="submitLoading"
      width="550px"
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
        <a-form-item label="站点" name="siteCode">
          <a-select
            v-model:value="formData.siteCode"
            placeholder="留空表示通用"
            allow-clear
            show-search
            :filter-option="filterMarketplaceOption"
          >
            <a-select-option
              v-for="marketplace in marketplaceOptions"
              :key="marketplace.siteCode"
              :value="marketplace.siteCode"
            >
              {{ marketplace.siteCode }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="原始类型" name="originalType">
          <a-input
            v-model:value="formData.originalType"
            placeholder="亚马逊原始交易类型，如 Order"
            :maxlength="100"
          />
        </a-form-item>

        <a-form-item label="标准分类" name="standardCategory">
          <a-select
            v-model:value="formData.standardCategory"
            placeholder="请选择标准分类"
          >
            <a-select-option v-for="item in standardCategoryOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="分类说明" name="categoryDesc">
          <a-input
            v-model:value="formData.categoryDesc"
            placeholder="分类说明（可选）"
            :maxlength="50"
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
  getTransactionTypeMappingList,
  createTransactionTypeMapping,
  updateTransactionTypeMapping,
  deleteTransactionTypeMapping,
  batchDeleteTransactionTypeMapping
} from '@/api/transactionType'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { TransactionTypeMapping, TransactionTypeMappingForm } from '@/types/transactionType'
import type { Marketplace } from '@/types/marketplace'

// ============= 标准分类选项 =============
const standardCategoryOptions = [
  { value: 'income', label: '收入', color: 'green' },
  { value: 'refund', label: '退款', color: 'red' },
  { value: 'fee', label: '费用', color: 'orange' },
  { value: 'adjustment', label: '调整', color: 'blue' },
  { value: 'other', label: '其他', color: 'default' }
]

function getStandardCategoryColor(category: string): string {
  return standardCategoryOptions.find(c => c.value === category)?.color || 'default'
}

function getStandardCategoryLabel(category: string): string {
  return standardCategoryOptions.find(c => c.value === category)?.label || category
}

// ============= 搜索相关 =============
const searchKeyword = ref('')
const searchStandardCategory = ref<string | undefined>(undefined)
const searchStatus = ref<number | undefined>(undefined)

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<TransactionTypeMapping[]>([])
const selectedRowKeys = ref<number[]>([])
const marketplaceOptions = ref<Marketplace[]>([])

const columns = [
  {
    title: '站点',
    dataIndex: 'siteCode',
    key: 'siteCode',
    width: 100
  },
  {
    title: '原始交易类型',
    dataIndex: 'originalType',
    key: 'originalType',
    width: 200
  },
  {
    title: '标准分类',
    dataIndex: 'standardCategory',
    key: 'standardCategory',
    width: 120
  },
  {
    title: '分类说明',
    dataIndex: 'categoryDesc',
    key: 'categoryDesc',
    width: 150
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

const formData = reactive<TransactionTypeMappingForm>({
  siteCode: undefined,
  originalType: '',
  standardCategory: '',
  categoryDesc: '',
  status: 1
})

const formRules = {
  originalType: [
    { required: true, message: '请输入原始交易类型', trigger: 'blur' },
    { max: 100, message: '原始交易类型不能超过100个字符', trigger: 'blur' }
  ],
  standardCategory: [
    { required: true, message: '请选择标准分类', trigger: 'change' }
  ]
}

// ============= 辅助方法 =============
function filterMarketplaceOption(input: string, option: any): boolean {
  const marketplace = marketplaceOptions.value.find(m => m.siteCode === option.value)
  if (!marketplace) return false
  const searchText = input.toLowerCase()
  return marketplace.siteCode.toLowerCase().includes(searchText)
}

// ============= 方法 =============
// 获取数据列表
async function fetchData() {
  loading.value = true
  try {
    const params = {
      keyword: searchKeyword.value || undefined,
      standardCategory: searchStandardCategory.value,
      status: searchStatus.value,
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getTransactionTypeMappingList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取交易类型映射列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取站点选项
async function fetchMarketplaceOptions() {
  try {
    const res = await getEnabledMarketplaces()
    marketplaceOptions.value = res.data || []
  } catch (error) {
    console.error('获取站点列表失败:', error)
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
  searchStandardCategory.value = undefined
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
function handleEdit(record: TransactionTypeMapping) {
  isEdit.value = true
  editingId.value = record.id
  Object.assign(formData, {
    siteCode: record.siteCode,
    originalType: record.originalType,
    standardCategory: record.standardCategory,
    categoryDesc: record.categoryDesc || '',
    status: record.status
  })
  modalVisible.value = true
}

// 删除
async function handleDelete(record: TransactionTypeMapping) {
  try {
    await deleteTransactionTypeMapping(record.id)
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
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 条映射吗？`,
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await batchDeleteTransactionTypeMapping(selectedRowKeys.value)
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
async function handleStatusChange(record: TransactionTypeMapping, checked: boolean) {
  try {
    await updateTransactionTypeMapping(record.id, {
      siteCode: record.siteCode,
      originalType: record.originalType,
      standardCategory: record.standardCategory,
      categoryDesc: record.categoryDesc,
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
      await updateTransactionTypeMapping(editingId.value, formData)
      message.success('更新成功')
    } else {
      await createTransactionTypeMapping(formData)
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
    siteCode: undefined,
    originalType: '',
    standardCategory: '',
    categoryDesc: '',
    status: 1
  })
}

// 初始化
onMounted(() => {
  fetchData()
  fetchMarketplaceOptions()
})
</script>

<style lang="scss" scoped>
.transaction-type-page {
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
