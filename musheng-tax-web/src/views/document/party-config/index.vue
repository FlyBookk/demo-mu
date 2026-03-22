<template>
  <div class="party-config-page">
    <div class="page-header">
      <h1 class="page-title">交易方配置</h1>
      <p class="page-desc">管理各站点的买方、卖方、供应商等交易方信息</p>
    </div>

    <a-card>
      <div class="toolbar">
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新增配置
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" size="small" @click="handleCopy(record)">复制</a-button>
              <a-popconfirm
                title="确认删除该站点配置？"
                ok-text="确认"
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

    <!-- 新增/编辑抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :title="isEdit ? '编辑配置' : '新增配置'"
      width="720"
      :destroy-on-close="true"
    >
      <PartyConfigForm
        v-if="drawerVisible"
        :config="currentConfig"
        :is-edit="isEdit"
        @saved="handleSaved"
        @cancel="drawerVisible = false"
      />
    </a-drawer>

    <!-- 复制站点选择弹窗 -->
    <a-modal
      v-model:open="copyModalVisible"
      title="复制配置到站点"
      ok-text="确认复制"
      cancel-text="取消"
      :confirm-loading="copying"
      @ok="confirmCopy"
      @cancel="copyModalVisible = false"
    >
      <div style="padding: 16px 0">
        <p style="margin-bottom: 12px">
          将 <strong>{{ copySource?.siteCode }}</strong> 的配置复制到以下站点（若目标站点已存在配置则覆盖）：
        </p>
        <a-select
          v-model:value="copyTargetSiteCode"
          placeholder="请选择目标站点"
          style="width: 100%"
          :options="copyTargetOptions"
        />
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import {
  listAllPartyConfigs,
  deletePartyConfig,
  copyPartyConfig
} from '@/api/documentPartyConfig'
import type { DocumentPartyConfig } from '@/api/documentPartyConfig'
import PartyConfigForm from './PartyConfigForm.vue'

// 支持的站点列表
const SITE_CODES = ['US', 'CA', 'UK', 'EU', 'JP', 'AU', 'MX', 'IN', 'SG', 'AE']

const list = ref<DocumentPartyConfig[]>([])
const loading = ref(false)

const drawerVisible = ref(false)
const isEdit = ref(false)
const currentConfig = ref<DocumentPartyConfig>({
  siteCode: '',
  buyerName: '',
  sellerName: '',
  supplierName: '',
  customerNameTc: ''
})

// 复制相关状态
const copyModalVisible = ref(false)
const copySource = ref<DocumentPartyConfig | null>(null)
const copyTargetSiteCode = ref<string>('')
const copying = ref(false)

/** 复制目标站点选项（排除来源站点） */
const copyTargetOptions = computed(() =>
  SITE_CODES
    .filter(code => code !== copySource.value?.siteCode)
    .map(code => ({ label: code, value: code }))
)

const columns = [
  { title: '站点代码', dataIndex: 'siteCode', key: 'siteCode', width: 100 },
  { title: '买方名称', dataIndex: 'buyerName', key: 'buyerName' },
  { title: '卖方名称', dataIndex: 'sellerName', key: 'sellerName' },
  { title: '供应商名称', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '客户繁体名', dataIndex: 'customerNameTc', key: 'customerNameTc' },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const }
]

async function fetchList() {
  loading.value = true
  try {
    const res = await listAllPartyConfigs()
    list.value = res.data || []
  } catch (e) {
    message.error('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  currentConfig.value = { siteCode: '', buyerName: '', sellerName: '', supplierName: '', customerNameTc: '' }
  drawerVisible.value = true
}

function handleEdit(record: DocumentPartyConfig) {
  isEdit.value = true
  currentConfig.value = { ...record }
  drawerVisible.value = true
}

async function handleDelete(record: DocumentPartyConfig) {
  try {
    await deletePartyConfig(record.id!)
    message.success('删除成功')
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

function handleSaved() {
  drawerVisible.value = false
  fetchList()
}

/** 打开复制弹窗 */
function handleCopy(record: DocumentPartyConfig) {
  copySource.value = record
  copyTargetSiteCode.value = ''
  copyModalVisible.value = true
}

/** 确认复制 */
async function confirmCopy() {
  if (!copyTargetSiteCode.value) {
    message.warning('请选择目标站点')
    return
  }
  copying.value = true
  try {
    await copyPartyConfig(copySource.value!.id!, copyTargetSiteCode.value)
    message.success(`已将 ${copySource.value!.siteCode} 的配置复制到 ${copyTargetSiteCode.value}`)
    copyModalVisible.value = false
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '复制失败，请重试')
  } finally {
    copying.value = false
  }
}

onMounted(fetchList)
</script>

<style lang="scss" scoped>
.party-config-page {
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

  .toolbar {
    margin-bottom: 16px;
  }
}
</style>
