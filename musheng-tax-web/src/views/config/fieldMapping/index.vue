<template>
  <div class="field-mapping-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">字段映射模板</h1>
      <p class="page-desc">配置CSV/Excel文件导入时源字段与系统字段的映射关系</p>
    </div>

    <!-- 搜索和操作栏 -->
    <a-card class="search-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="5">
          <a-input
            v-model:value="searchKeyword"
            placeholder="搜索模板名称或代码"
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
        <a-col :span="4">
          <a-select
            v-model:value="searchSiteCode"
            placeholder="站点"
            allow-clear
            style="width: 100%"
            @change="handleSearch"
          >
            <a-select-option
              v-for="marketplace in marketplaceOptions"
              :key="marketplace.siteCode"
              :value="marketplace.siteCode"
            >
              {{ marketplace.siteCode }}
            </a-select-option>
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
              <PlusOutlined /> 新增模板
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
        :scroll="{ x: 1200 }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 站点 -->
          <template v-if="column.key === 'siteCode'">
            <a-tag color="blue">{{ record.siteCode }}</a-tag>
          </template>

          <!-- 数据类型 -->
          <template v-else-if="column.key === 'dataType'">
            <a-tag :color="getDataTypeColor(record.dataType)">
              {{ getDataTypeLabel(record.dataType) }}
            </a-tag>
          </template>

          <!-- 映射字段数 -->
          <template v-else-if="column.key === 'mappingCount'">
            <a-badge :count="record.mappingConfig?.length || 0" :number-style="{ backgroundColor: '#1890ff' }" />
          </template>

          <!-- 默认模板 -->
          <template v-else-if="column.key === 'isDefault'">
            <a-tag v-if="record.isDefault" color="gold">
              <StarFilled /> 默认
            </a-tag>
            <a-button
              v-else
              type="link"
              size="small"
              @click="handleSetDefault(record)"
            >
              设为默认
            </a-button>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <EditOutlined /> 编辑
              </a-button>
              <a-button type="link" size="small" @click="handleCopy(record)">
                <CopyOutlined /> 复制
              </a-button>
              <a-popconfirm
                title="确定要删除该模板吗？"
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
      :title="isEdit ? '编辑模板' : '新增模板'"
      :confirm-loading="submitLoading"
      width="900px"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 19 }"
      >
        <a-row :gutter="16">
          <a-col :span="24">
            <a-form-item label="模板名称" name="templateName">
              <a-input
                v-model:value="formData.templateName"
                placeholder="请输入模板名称"
                :maxlength="100"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="站点" name="siteCode" :label-col="{ span: 8 }" :wrapper-col="{ span: 15 }">
              <a-select
                v-model:value="formData.siteCode"
                placeholder="请选择站点"
                :disabled="isEdit"
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
          </a-col>
          <a-col :span="12">
            <a-form-item label="数据类型" name="dataType" :label-col="{ span: 8 }" :wrapper-col="{ span: 15 }">
              <a-select
                v-model:value="formData.dataType"
                placeholder="请选择数据类型"
                :disabled="isEdit"
              >
                <a-select-option v-for="item in dataTypeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider>字段映射配置</a-divider>

        <a-form-item label="映射" name="mappingConfig" :wrapper-col="{ span: 24 }">
          <div class="mapping-list">
            <div v-for="(mapping, index) in formData.mappingConfig" :key="index" class="mapping-item">
              <a-row :gutter="8" align="middle">
                <a-col :span="7">
                  <a-input
                    v-model:value="mapping.sourceField"
                    placeholder="源字段名"
                  />
                </a-col>
                <a-col :span="1" class="mapping-arrow">
                  <ArrowRightOutlined />
                </a-col>
                <a-col :span="6">
                  <a-input
                    v-model:value="mapping.targetField"
                    placeholder="目标字段名"
                  />
                </a-col>
                <a-col :span="4">
                  <a-input
                    v-model:value="mapping.defaultValue"
                    placeholder="默认值"
                  />
                </a-col>
                <a-col :span="3">
                  <a-checkbox v-model:checked="mapping.required">必填</a-checkbox>
                </a-col>
                <a-col :span="3" style="text-align: right">
                  <a-button
                    type="link"
                    danger
                    @click="removeMapping(index)"
                    :disabled="formData.mappingConfig.length <= 1"
                  >
                    <DeleteOutlined />
                  </a-button>
                </a-col>
              </a-row>
            </div>
            <a-button type="dashed" block @click="addMapping">
              <PlusOutlined /> 添加映射
            </a-button>
          </div>
        </a-form-item>

      </a-form>
    </a-modal>

    <!-- 复制模板弹窗 -->
    <a-modal
      v-model:open="copyModalVisible"
      title="复制模板"
      :confirm-loading="copyLoading"
      @ok="handleCopySubmit"
      @cancel="copyModalVisible = false"
    >
      <a-form
        ref="copyFormRef"
        :model="copyFormData"
        :rules="copyFormRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="新模板名称" name="newName">
          <a-input
            v-model:value="copyFormData.newName"
            placeholder="请输入新模板名称"
            :maxlength="100"
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
import type { FormInstance, TablePaginationConfig } from 'ant-design-vue'
import type { TableRowSelection } from 'ant-design-vue/es/table/interface'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  CopyOutlined,
  StarFilled,
  ArrowRightOutlined
} from '@ant-design/icons-vue'
import {
  getFieldMappingTemplateList,
  createFieldMappingTemplate,
  updateFieldMappingTemplate,
  deleteFieldMappingTemplate,
  batchDeleteFieldMappingTemplate,
  copyFieldMappingTemplate,
  setDefaultTemplate
} from '@/api/fieldMapping'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { FieldMappingTemplate, FieldMappingTemplateForm, FieldMappingDataType, FieldMappingItem } from '@/types/fieldMapping'
import type { Marketplace } from '@/types/marketplace'

const router = useRouter()

// ============= 数据类型选项 =============
const dataTypeOptions = [
  { value: 'SALES', label: '销售数据', color: 'green' },
  { value: 'SHIPPING', label: '配送数据', color: 'blue' },
  { value: 'ADVERTISING', label: '广告数据', color: 'purple' },
  { value: 'RATE', label: '汇率数据', color: 'orange' }
]

function getDataTypeColor(type: string): string {
  return dataTypeOptions.find(t => t.value === type)?.color || 'default'
}

function getDataTypeLabel(type: string): string {
  return dataTypeOptions.find(t => t.value === type)?.label || type
}

// ============= 搜索相关 =============
const searchKeyword = ref('')
const searchDataType = ref<FieldMappingDataType | undefined>(undefined)
const searchSiteCode = ref<string | undefined>(undefined)

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<FieldMappingTemplate[]>([])
const selectedRowKeys = ref<number[]>([])
const marketplaceOptions = ref<Marketplace[]>([])

const columns = [
  {
    title: '模板名称',
    dataIndex: 'templateName',
    key: 'templateName',
    width: 200
  },
  {
    title: '站点',
    dataIndex: 'siteCode',
    key: 'siteCode',
    width: 100
  },
  {
    title: '数据类型',
    dataIndex: 'dataType',
    key: 'dataType',
    width: 120
  },
  {
    title: '字段数',
    dataIndex: 'mappingCount',
    key: 'mappingCount',
    width: 80,
    align: 'center' as const
  },
  {
    title: '默认',
    dataIndex: 'isDefault',
    key: 'isDefault',
    width: 100
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
    width: 200,
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

const createEmptyMapping = (): FieldMappingItem => ({
  sourceField: '',
  targetField: '',
  required: false,
  defaultValue: ''
})

const formData = reactive<FieldMappingTemplateForm>({
  templateName: '',
  siteCode: '',
  dataType: 'SALES',
  mappingConfig: [createEmptyMapping()],
  isDefault: false
})

const formRules = {
  templateName: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { max: 100, message: '模板名称不能超过100个字符', trigger: 'blur' }
  ],
  siteCode: [
    { required: true, message: '请选择站点', trigger: 'change' }
  ],
  dataType: [
    { required: true, message: '请选择数据类型', trigger: 'change' }
  ]
}

// 复制模板弹窗
const copyModalVisible = ref(false)
const copyLoading = ref(false)
const copyFormRef = ref<FormInstance>()
const copySourceId = ref<number | null>(null)
const copyFormData = reactive({
  newName: ''
})
const copyFormRules = {
  newName: [
    { required: true, message: '请输入新模板名称', trigger: 'blur' }
  ]
}

// ============= 方法 =============
// 获取数据列表
async function fetchData() {
  loading.value = true
  try {
    const params = {
      keyword: searchKeyword.value || undefined,
      siteCode: searchSiteCode.value,
      dataType: searchDataType.value,
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getFieldMappingTemplateList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取字段映射模板列表失败:', error)
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
  searchDataType.value = undefined
  searchSiteCode.value = undefined
  pagination.current = 1
  fetchData()
}

// 表格变化
function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  fetchData()
}

// 新增 - 跳转到画布编辑页面
function handleAdd() {
  router.push('/config/field-mapping/add')
}

// 编辑 - 跳转到画布编辑页面
function handleEdit(record: FieldMappingTemplate) {
  router.push(`/config/field-mapping/edit/${record.id}`)
}

// 传统弹窗新增（保留备用）
function handleAddLegacy() {
  isEdit.value = false
  editingId.value = null
  resetForm()
  modalVisible.value = true
}

// 传统弹窗编辑（保留备用）
function handleEditLegacy(record: FieldMappingTemplate) {
  isEdit.value = true
  editingId.value = record.id
  Object.assign(formData, {
    templateName: record.templateName,
    siteCode: record.siteCode,
    dataType: record.dataType,
    mappingConfig: record.mappingConfig?.length ? [...record.mappingConfig] : [createEmptyMapping()],
    isDefault: record.isDefault
  })
  modalVisible.value = true
}

// 复制
function handleCopy(record: FieldMappingTemplate) {
  copySourceId.value = record.id
  copyFormData.newName = `${record.templateName}_副本`
  copyModalVisible.value = true
}

// 复制提交
async function handleCopySubmit() {
  try {
    await copyFormRef.value?.validate()
    copyLoading.value = true
    await copyFieldMappingTemplate({
      sourceId: copySourceId.value!,
      newName: copyFormData.newName
    })
    message.success('复制成功')
    copyModalVisible.value = false
    fetchData()
  } catch (error: any) {
    if (error?.errorFields) return
    console.error('复制失败:', error)
  } finally {
    copyLoading.value = false
  }
}

// 设为默认
async function handleSetDefault(record: FieldMappingTemplate) {
  try {
    await setDefaultTemplate(record.id)
    message.success('设置成功')
    fetchData()
  } catch (error) {
    console.error('设置默认模板失败:', error)
  }
}

// 删除
async function handleDelete(record: FieldMappingTemplate) {
  try {
    await deleteFieldMappingTemplate(record.id)
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
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个模板吗？`,
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await batchDeleteFieldMappingTemplate(selectedRowKeys.value)
        message.success('批量删除成功')
        selectedRowKeys.value = []
        fetchData()
      } catch (error) {
        console.error('批量删除失败:', error)
      }
    }
  })
}

// 添加映射
function addMapping() {
  formData.mappingConfig.push(createEmptyMapping())
}

// 删除映射
function removeMapping(index: number) {
  formData.mappingConfig.splice(index, 1)
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    
    // 验证映射配置
    const validMappings = formData.mappingConfig.filter(m => m.sourceField && m.targetField)
    if (validMappings.length === 0) {
      message.error('请至少配置一个有效的字段映射')
      return
    }

    submitLoading.value = true
    const submitData = { ...formData, mappingConfig: validMappings }

    if (isEdit.value && editingId.value) {
      await updateFieldMappingTemplate(editingId.value, submitData)
      message.success('更新成功')
    } else {
      await createFieldMappingTemplate(submitData)
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
    templateName: '',
    siteCode: '',
    dataType: 'SALES',
    mappingConfig: [createEmptyMapping()],
    isDefault: false
  })
}

// 初始化
onMounted(() => {
  fetchData()
  fetchMarketplaceOptions()
})
</script>

<style lang="scss" scoped>
.field-mapping-page {
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

  .mapping-list {
    .mapping-item {
      padding: $spacing-sm;
      margin-bottom: $spacing-sm;
      background: $background-color-light;
      border-radius: $border-radius-md;

      .mapping-arrow {
        text-align: center;
        color: $text-color-secondary;
      }
    }
  }
}
</style>
