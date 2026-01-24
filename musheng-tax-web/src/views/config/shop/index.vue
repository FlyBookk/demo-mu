<template>
  <div class="shop-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">店铺管理</h1>
      <p class="page-desc">管理店铺配置，店铺是业务数据隔离的基本单位</p>
    </div>

    <!-- 搜索和操作栏 -->
    <a-card class="search-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="5">
          <a-input
            v-model:value="searchShopCode"
            placeholder="搜索店铺编码"
            allow-clear
            @pressEnter="handleSearch"
          >
            <template #prefix>
              <SearchOutlined />
            </template>
          </a-input>
        </a-col>
        <a-col :span="5">
          <a-input
            v-model:value="searchShopName"
            placeholder="搜索店铺名称"
            allow-clear
            @pressEnter="handleSearch"
          />
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
        <a-col :span="6" style="text-align: right">
          <a-button type="primary" @click="handleAdd">
            <PlusOutlined /> 新增店铺
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
        :scroll="{ x: 1200 }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 店铺编码 -->
          <template v-if="column.key === 'shopCode'">
            <a-tag color="blue">{{ record.shopCode }}</a-tag>
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
                title="确定要删除该店铺吗？删除后相关业务数据将无法关联。"
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
      :title="isEdit ? '编辑店铺' : '新增店铺'"
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
        <a-form-item label="店铺编码" name="shopCode">
          <a-input
            v-model:value="formData.shopCode"
            placeholder="请输入店铺编码，如 SHOP001"
            :disabled="isEdit"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item label="店铺名称" name="shopName">
          <a-input
            v-model:value="formData.shopName"
            placeholder="请输入店铺名称，如 慕声美国店"
            :maxlength="100"
          />
        </a-form-item>

        <a-form-item label="卖家ID" name="sellerId">
          <a-input
            v-model:value="formData.sellerId"
            placeholder="亚马逊卖家ID（可选）"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item label="公司名称" name="companyName">
          <a-input
            v-model:value="formData.companyName"
            placeholder="公司全称（可选）"
            :maxlength="200"
          />
        </a-form-item>

        <a-form-item label="统一社会信用代码" name="taxId">
          <a-input
            v-model:value="formData.taxId"
            placeholder="18位统一社会信用代码（可选）"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="formData.remark"
            placeholder="备注信息（可选）"
            :rows="3"
            :maxlength="500"
            show-count
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
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance, TablePaginationConfig } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined
} from '@ant-design/icons-vue'
import {
  getShopList,
  createShop,
  updateShop,
  deleteShop
} from '@/api/shop'
import { useShopStore } from '@/stores/modules/shop'
import type { Shop, ShopForm } from '@/types/shop'

const shopStore = useShopStore()

// ============= 搜索相关 =============
const searchShopCode = ref('')
const searchShopName = ref('')
const searchStatus = ref<number | undefined>(undefined)

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<Shop[]>([])

const columns = [
  {
    title: '店铺编码',
    dataIndex: 'shopCode',
    key: 'shopCode',
    width: 120,
    fixed: 'left' as const
  },
  {
    title: '店铺名称',
    dataIndex: 'shopName',
    key: 'shopName',
    width: 150
  },
  {
    title: '卖家ID',
    dataIndex: 'sellerId',
    key: 'sellerId',
    width: 150
  },
  {
    title: '公司名称',
    dataIndex: 'companyName',
    key: 'companyName',
    width: 200,
    ellipsis: true
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

// ============= 弹窗相关 =============
const modalVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const formData = reactive<ShopForm>({
  shopCode: '',
  shopName: '',
  sellerId: '',
  companyName: '',
  taxId: '',
  remark: '',
  status: 1
})

const formRules = {
  shopCode: [
    { required: true, message: '请输入店铺编码', trigger: 'blur' },
    { min: 2, max: 50, message: '店铺编码长度为2-50位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '店铺编码只能包含字母、数字、下划线和横线', trigger: 'blur' }
  ],
  shopName: [
    { required: true, message: '请输入店铺名称', trigger: 'blur' },
    { max: 100, message: '店铺名称不能超过100个字符', trigger: 'blur' }
  ]
}

// ============= 方法 =============
// 获取数据列表
async function fetchData() {
  loading.value = true
  try {
    const params = {
      shopCode: searchShopCode.value || undefined,
      shopName: searchShopName.value || undefined,
      status: searchStatus.value,
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getShopList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取店铺列表失败:', error)
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
  searchShopCode.value = ''
  searchShopName.value = ''
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
function handleEdit(record: Shop) {
  isEdit.value = true
  editingId.value = record.id
  Object.assign(formData, {
    shopCode: record.shopCode,
    shopName: record.shopName,
    sellerId: record.sellerId || '',
    companyName: record.companyName || '',
    taxId: record.taxId || '',
    remark: record.remark || '',
    status: record.status
  })
  modalVisible.value = true
}

// 删除
async function handleDelete(record: Shop) {
  try {
    await deleteShop(record.id)
    message.success('删除成功')
    fetchData()
    // 刷新店铺选择器列表
    shopStore.refreshShopList()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 状态切换
async function handleStatusChange(record: Shop, checked: boolean) {
  try {
    await updateShop(record.id, {
      shopCode: record.shopCode,
      shopName: record.shopName,
      sellerId: record.sellerId,
      companyName: record.companyName,
      taxId: record.taxId,
      remark: record.remark,
      status: checked ? 1 : 0
    })
    message.success(checked ? '已启用' : '已禁用')
    fetchData()
    // 刷新店铺选择器列表
    shopStore.refreshShopList()
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
      await updateShop(editingId.value, formData)
      message.success('更新成功')
    } else {
      await createShop(formData)
      message.success('创建成功')
    }

    modalVisible.value = false
    fetchData()
    // 刷新店铺选择器列表
    shopStore.refreshShopList()
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
    shopCode: '',
    shopName: '',
    sellerId: '',
    companyName: '',
    taxId: '',
    remark: '',
    status: 1
  })
}

// 初始化
onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.shop-page {
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
