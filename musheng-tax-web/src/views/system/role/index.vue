<template>
  <div class="role-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">权限管理</h1>
      <p class="page-desc">管理系统角色，配置角色权限，控制用户访问范围</p>
    </div>

    <!-- 搜索和操作栏 -->
    <a-card class="search-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="5">
          <a-input
            v-model:value="searchParams.roleCode"
            placeholder="搜索角色编码"
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
            v-model:value="searchParams.roleName"
            placeholder="搜索角色名称"
            allow-clear
            @pressEnter="handleSearch"
          />
        </a-col>
        <a-col :span="4">
          <a-select
            v-model:value="searchParams.status"
            placeholder="状态"
            allow-clear
            style="width: 100%"
            @change="handleSearch"
          >
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="5">
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
          <a-button type="primary" @click="handleAdd">
            <PlusOutlined /> 新增角色
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
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 角色编码 -->
          <template v-if="column.key === 'roleCode'">
            <a-tag color="blue">{{ record.roleCode }}</a-tag>
          </template>

          <!-- 状态 -->
          <template v-else-if="column.key === 'status'">
            <a-badge
              :status="record.status === 1 ? 'success' : 'default'"
              :text="record.status === 1 ? '启用' : '禁用'"
            />
          </template>

          <!-- 权限数量 -->
          <template v-else-if="column.key === 'permissions'">
            <a-tag v-if="record.permissions" color="green">
              {{ getPermissionCount(record.permissions) }} 项权限
            </a-tag>
            <span v-else class="text-muted">未配置</span>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <EditOutlined /> 编辑
              </a-button>
              <a-button type="link" size="small" @click="handlePermission(record)">
                <SafetyOutlined /> 权限
              </a-button>
              <a-popconfirm
                title="确定要删除该角色吗？"
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
      :title="isEdit ? '编辑角色' : '新增角色'"
      :confirm-loading="submitLoading"
      :width="520"
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
        <a-form-item label="角色编码" name="roleCode">
          <a-input
            v-model:value="formData.roleCode"
            placeholder="请输入角色编码，如 ADMIN"
            :disabled="isEdit"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item label="角色名称" name="roleName">
          <a-input
            v-model:value="formData.roleName"
            placeholder="请输入角色名称"
            :maxlength="100"
          />
        </a-form-item>

        <a-form-item label="角色描述" name="roleDesc">
          <a-textarea
            v-model:value="formData.roleDesc"
            placeholder="请输入角色描述"
            :rows="3"
            :maxlength="255"
          />
        </a-form-item>

        <a-form-item v-if="isEdit" label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 权限配置弹窗 -->
    <a-modal
      v-model:open="permissionModalVisible"
      title="配置权限"
      :confirm-loading="permissionLoading"
      :width="600"
      @ok="handlePermissionSubmit"
      @cancel="handlePermissionCancel"
    >
      <div class="permission-modal-content">
        <a-alert
          v-if="currentRole"
          :message="`正在配置角色 【${currentRole.roleName}】 的权限`"
          type="info"
          show-icon
          style="margin-bottom: 16px"
        />
        
        <a-spin :spinning="permissionTreeLoading">
          <div class="permission-tree-wrapper">
            <a-checkbox
              v-model:checked="checkAll"
              :indeterminate="indeterminate"
              @change="onCheckAllChange"
              style="margin-bottom: 12px"
            >
              全选
            </a-checkbox>
            
            <a-tree
              v-if="permissionTree.length > 0"
              v-model:checkedKeys="checkedPermissions"
              :tree-data="permissionTree"
              checkable
              :selectable="false"
              default-expand-all
              @check="onPermissionCheck"
            />
            <a-empty v-else description="暂无权限数据" />
          </div>
        </a-spin>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance, TablePaginationConfig, TreeProps } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  SafetyOutlined
} from '@ant-design/icons-vue'
import {
  getRoleList,
  createRole,
  updateRole,
  deleteRole,
  getRolePermissions,
  assignRolePermissions
} from '@/api/role'
import type { Role, RoleCreateForm, RoleUpdateForm } from '@/types/role'

// ============= 搜索相关 =============
const searchParams = reactive({
  roleCode: '',
  roleName: '',
  status: undefined as number | undefined
})

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<Role[]>([])

const columns = [
  {
    title: '角色编码',
    dataIndex: 'roleCode',
    key: 'roleCode',
    width: 150
  },
  {
    title: '角色名称',
    dataIndex: 'roleName',
    key: 'roleName',
    width: 150
  },
  {
    title: '角色描述',
    dataIndex: 'roleDesc',
    key: 'roleDesc',
    width: 200,
    ellipsis: true
  },
  {
    title: '权限',
    dataIndex: 'permissions',
    key: 'permissions',
    width: 120,
    align: 'center' as const
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
    width: 180
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

// ============= 弹窗相关 =============
const modalVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const formData = reactive<RoleCreateForm & { id?: number; status?: number }>({
  roleCode: '',
  roleName: '',
  roleDesc: '',
  status: 1
})

const formRules = {
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { max: 50, message: '角色编码不能超过50个字符', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '角色编码只能包含大写字母和下划线', trigger: 'blur' }
  ],
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 100, message: '角色名称不能超过100个字符', trigger: 'blur' }
  ]
}

// ============= 权限配置相关 =============
const permissionModalVisible = ref(false)
const permissionLoading = ref(false)
const permissionTreeLoading = ref(false)
const currentRole = ref<Role | null>(null)
const checkedPermissions = ref<string[]>([])
const permissionTree = ref<TreeProps['treeData']>([])
const checkAll = ref(false)
const indeterminate = ref(false)

// 预定义权限树
const defaultPermissionTree: TreeProps['treeData'] = [
  {
    title: '首页',
    key: 'dashboard',
    children: [
      { title: '查看首页', key: 'dashboard:view' }
    ]
  },
  {
    title: '基础配置',
    key: 'config',
    children: [
      { title: '货币管理', key: 'config:currency' },
      { title: '站点管理', key: 'config:marketplace' },
      { title: '交易类型映射', key: 'config:transactionType' },
      { title: '字段映射模板', key: 'config:fieldMapping' },
      { title: '导入记录', key: 'config:importRecord' }
    ]
  },
  {
    title: '销售数据',
    key: 'sales',
    children: [
      { title: '数据导入', key: 'sales:import' },
      { title: '数据列表', key: 'sales:list' }
    ]
  },
  {
    title: '配送数据',
    key: 'shipping',
    children: [
      { title: '数据导入', key: 'shipping:import' },
      { title: '数据列表', key: 'shipping:list' }
    ]
  },
  {
    title: '广告数据',
    key: 'advertising',
    children: [
      { title: '广告费录入', key: 'advertising:add' },
      { title: '数据列表', key: 'advertising:list' }
    ]
  },
  {
    title: '汇率管理',
    key: 'rate',
    children: [
      { title: '汇率导入', key: 'rate:import' },
      { title: '汇率查询', key: 'rate:list' }
    ]
  },
  {
    title: '汇总报表',
    key: 'report',
    children: [
      { title: '汇总查询', key: 'report:summary' },
      { title: '报表下载', key: 'report:download' }
    ]
  },
  {
    title: '系统管理',
    key: 'system',
    children: [
      { title: '用户管理', key: 'system:user' },
      { title: '权限管理', key: 'system:role' },
      { title: '操作日志', key: 'system:log' }
    ]
  }
]

// ============= 工具函数 =============
function getPermissionCount(permissions: string): number {
  if (!permissions) return 0
  try {
    const arr = JSON.parse(permissions)
    return Array.isArray(arr) ? arr.length : 0
  } catch {
    return permissions.split(',').filter(p => p.trim()).length
  }
}

function getAllPermissionKeys(tree: TreeProps['treeData']): string[] {
  const keys: string[] = []
  const traverse = (nodes: TreeProps['treeData']) => {
    nodes?.forEach(node => {
      if (node.key) keys.push(node.key as string)
      if (node.children) traverse(node.children)
    })
  }
  traverse(tree)
  return keys
}

// ============= 方法 =============
// 获取角色列表
async function fetchData() {
  loading.value = true
  try {
    const params = {
      roleCode: searchParams.roleCode || undefined,
      roleName: searchParams.roleName || undefined,
      status: searchParams.status,
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getRoleList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取角色列表失败:', error)
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
  searchParams.roleCode = ''
  searchParams.roleName = ''
  searchParams.status = undefined
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
function handleEdit(record: Role) {
  isEdit.value = true
  editingId.value = record.id
  Object.assign(formData, {
    id: record.id,
    roleCode: record.roleCode,
    roleName: record.roleName,
    roleDesc: record.roleDesc || '',
    status: record.status
  })
  modalVisible.value = true
}

// 删除
async function handleDelete(record: Role) {
  try {
    await deleteRole(record.id)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 权限配置
async function handlePermission(record: Role) {
  currentRole.value = record
  permissionTree.value = defaultPermissionTree
  permissionModalVisible.value = true
  permissionTreeLoading.value = true
  
  try {
    const res = await getRolePermissions(record.id)
    const permissions = res.data || []
    checkedPermissions.value = permissions
    updateCheckAllState()
  } catch (error) {
    console.error('获取角色权限失败:', error)
    checkedPermissions.value = []
  } finally {
    permissionTreeLoading.value = false
  }
}

// 更新全选状态
function updateCheckAllState() {
  const allKeys = getAllPermissionKeys(permissionTree.value)
  const checkedCount = checkedPermissions.value.length
  checkAll.value = checkedCount === allKeys.length && allKeys.length > 0
  indeterminate.value = checkedCount > 0 && checkedCount < allKeys.length
}

// 全选变化
function onCheckAllChange(e: any) {
  const allKeys = getAllPermissionKeys(permissionTree.value)
  checkedPermissions.value = e.target.checked ? allKeys : []
  indeterminate.value = false
}

// 权限选择变化
function onPermissionCheck() {
  updateCheckAllState()
}

// 提交权限配置
async function handlePermissionSubmit() {
  if (!currentRole.value) return
  
  permissionLoading.value = true
  try {
    await assignRolePermissions({
      roleId: currentRole.value.id,
      permissions: checkedPermissions.value
    })
    message.success('权限配置成功')
    permissionModalVisible.value = false
    fetchData()
  } catch (error) {
    console.error('权限配置失败:', error)
  } finally {
    permissionLoading.value = false
  }
}

// 取消权限配置
function handlePermissionCancel() {
  permissionModalVisible.value = false
  currentRole.value = null
  checkedPermissions.value = []
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    if (isEdit.value && editingId.value) {
      const updateData: RoleUpdateForm = {
        roleName: formData.roleName,
        roleDesc: formData.roleDesc,
        status: formData.status
      }
      await updateRole(editingId.value, updateData)
      message.success('更新成功')
    } else {
      const createData: RoleCreateForm = {
        roleCode: formData.roleCode,
        roleName: formData.roleName,
        roleDesc: formData.roleDesc
      }
      await createRole(createData)
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
    id: undefined,
    roleCode: '',
    roleName: '',
    roleDesc: '',
    status: 1
  })
}

// 初始化
onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.role-page {
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
    .text-muted {
      color: $text-color-secondary;
      font-size: 12px;
    }
  }
}

.permission-modal-content {
  .permission-tree-wrapper {
    max-height: 400px;
    overflow-y: auto;
    padding: 8px;
    border: 1px solid #e8e8e8;
    border-radius: 4px;
  }
}
</style>
