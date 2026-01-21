<template>
  <div class="user-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <p class="page-desc">管理系统用户，包括用户创建、编辑、权限分配等操作</p>
    </div>

    <!-- 搜索和操作栏 -->
    <a-card class="search-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="5">
          <a-input
            v-model:value="searchParams.username"
            placeholder="搜索用户名"
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
            v-model:value="searchParams.realName"
            placeholder="搜索真实姓名"
            allow-clear
            @pressEnter="handleSearch"
          />
        </a-col>
        <a-col :span="4">
          <a-select
            v-model:value="searchParams.roleCode"
            placeholder="角色"
            allow-clear
            style="width: 100%"
            @change="handleSearch"
          >
            <a-select-option
              v-for="role in roleOptions"
              :key="role.roleCode"
              :value="role.roleCode"
            >
              {{ role.roleName }}
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="3">
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
        <a-col :span="3" style="text-align: right">
          <a-button type="primary" @click="handleAdd">
            <PlusOutlined /> 新增用户
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
          <!-- 用户名 -->
          <template v-if="column.key === 'username'">
            <div class="user-info">
              <a-avatar :size="32" style="background-color: #1890ff">
                {{ record.realName?.charAt(0) || record.username?.charAt(0) || 'U' }}
              </a-avatar>
              <span class="username">{{ record.username }}</span>
            </div>
          </template>

          <!-- 角色 -->
          <template v-else-if="column.key === 'roleCode'">
            <a-tag :color="getRoleColor(record.roleCode)">
              {{ getRoleName(record.roleCode) }}
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

          <!-- 最后登录时间 -->
          <template v-else-if="column.key === 'lastLoginTime'">
            <span v-if="record.lastLoginTime">{{ record.lastLoginTime }}</span>
            <span v-else class="text-muted">从未登录</span>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <EditOutlined /> 编辑
              </a-button>
              <a-popconfirm
                title="确定要重置该用户密码吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleResetPassword(record)"
              >
                <a-button type="link" size="small">
                  <KeyOutlined /> 重置密码
                </a-button>
              </a-popconfirm>
              <a-popconfirm
                title="确定要删除该用户吗？"
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
      :title="isEdit ? '编辑用户' : '新增用户'"
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
        <a-form-item label="用户名" name="username">
          <a-input
            v-model:value="formData.username"
            placeholder="请输入用户名"
            :disabled="isEdit"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item v-if="!isEdit" label="密码" name="password">
          <a-input-password
            v-model:value="formData.password"
            placeholder="请输入密码（默认：123456）"
            :maxlength="100"
          />
        </a-form-item>

        <a-form-item label="真实姓名" name="realName">
          <a-input
            v-model:value="formData.realName"
            placeholder="请输入真实姓名"
            :maxlength="50"
          />
        </a-form-item>

        <a-form-item label="邮箱" name="email">
          <a-input
            v-model:value="formData.email"
            placeholder="请输入邮箱"
            :maxlength="100"
          />
        </a-form-item>

        <a-form-item label="手机号" name="phone">
          <a-input
            v-model:value="formData.phone"
            placeholder="请输入手机号"
            :maxlength="20"
          />
        </a-form-item>

        <a-form-item label="角色" name="roleCode">
          <a-select
            v-model:value="formData.roleCode"
            placeholder="请选择角色"
            style="width: 100%"
          >
            <a-select-option
              v-for="role in roleOptions"
              :key="role.roleCode"
              :value="role.roleCode"
            >
              {{ role.roleName }}
            </a-select-option>
          </a-select>
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
  EditOutlined,
  KeyOutlined
} from '@ant-design/icons-vue'
import {
  getUserList,
  createUser,
  updateUser,
  deleteUser,
  resetPassword,
  toggleUserStatus
} from '@/api/user'
import { getAllRoles } from '@/api/role'
import type { User, UserCreateForm, UserUpdateForm } from '@/types/user'
import type { Role } from '@/types/role'

// ============= 搜索相关 =============
const searchParams = reactive({
  username: '',
  realName: '',
  roleCode: undefined as string | undefined,
  status: undefined as number | undefined
})

// ============= 角色选项 =============
const roleOptions = ref<Role[]>([])

// ============= 表格相关 =============
const loading = ref(false)
const tableData = ref<User[]>([])

const columns = [
  {
    title: '用户名',
    dataIndex: 'username',
    key: 'username',
    width: 180
  },
  {
    title: '真实姓名',
    dataIndex: 'realName',
    key: 'realName',
    width: 120
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    key: 'email',
    width: 180
  },
  {
    title: '手机号',
    dataIndex: 'phone',
    key: 'phone',
    width: 140
  },
  {
    title: '角色',
    dataIndex: 'roleCode',
    key: 'roleCode',
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
    title: '最后登录',
    dataIndex: 'lastLoginTime',
    key: 'lastLoginTime',
    width: 180
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
    width: 220,
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

const formData = reactive<UserCreateForm & { id?: number }>({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  roleCode: ''
})

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为3-50位', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { max: 50, message: '真实姓名不能超过50个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  roleCode: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// ============= 工具函数 =============
function getRoleColor(roleCode: string): string {
  const colorMap: Record<string, string> = {
    'admin': 'red',
    'ADMIN': 'red',
    'finance': 'blue',
    'FINANCE': 'blue',
    'operator': 'green',
    'OPERATOR': 'green'
  }
  return colorMap[roleCode] || 'default'
}

function getRoleName(roleCode: string): string {
  const role = roleOptions.value.find(r => r.roleCode === roleCode)
  return role?.roleName || roleCode
}

// ============= 方法 =============
// 获取角色列表
async function fetchRoles() {
  try {
    const res = await getAllRoles()
    roleOptions.value = res.data || []
  } catch (error) {
    console.error('获取角色列表失败:', error)
  }
}

// 获取用户列表
async function fetchData() {
  loading.value = true
  try {
    const params = {
      username: searchParams.username || undefined,
      realName: searchParams.realName || undefined,
      roleCode: searchParams.roleCode,
      status: searchParams.status,
      page: pagination.current,
      size: pagination.pageSize
    }
    const res = await getUserList(params)
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
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
  searchParams.realName = ''
  searchParams.roleCode = undefined
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
function handleEdit(record: User) {
  isEdit.value = true
  editingId.value = record.id
  Object.assign(formData, {
    id: record.id,
    username: record.username,
    realName: record.realName,
    email: record.email || '',
    phone: record.phone || '',
    roleCode: record.roleCode,
    password: ''
  })
  modalVisible.value = true
}

// 删除
async function handleDelete(record: User) {
  try {
    await deleteUser(record.id)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 状态切换
async function handleStatusChange(record: User, checked: boolean) {
  try {
    await toggleUserStatus({ userId: record.id, status: checked ? 1 : 0 })
    message.success(checked ? '已启用' : '已禁用')
    fetchData()
  } catch (error) {
    console.error('状态更新失败:', error)
  }
}

// 重置密码
async function handleResetPassword(record: User) {
  try {
    await resetPassword({ userId: record.id, newPassword: '123456' })
    message.success('密码已重置为默认密码：123456')
  } catch (error) {
    console.error('重置密码失败:', error)
  }
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true

    if (isEdit.value && editingId.value) {
      const updateData: UserUpdateForm = {
        id: editingId.value,
        realName: formData.realName,
        email: formData.email,
        phone: formData.phone,
        roleCode: formData.roleCode
      }
      await updateUser(editingId.value, updateData)
      message.success('更新成功')
    } else {
      const createData: UserCreateForm = {
        username: formData.username,
        password: formData.password || '123456',
        realName: formData.realName,
        email: formData.email,
        phone: formData.phone,
        roleCode: formData.roleCode
      }
      await createUser(createData)
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
    username: '',
    password: '',
    realName: '',
    email: '',
    phone: '',
    roleCode: ''
  })
}

// 初始化
onMounted(() => {
  fetchRoles()
  fetchData()
})
</script>

<style lang="scss" scoped>
.user-page {
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

    .text-muted {
      color: $text-color-secondary;
      font-size: 12px;
    }
  }
}
</style>
