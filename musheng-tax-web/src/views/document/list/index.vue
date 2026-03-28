<template>
  <div class="document-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">FBA单据列表</h1>
      <p class="page-desc">查看和管理FBA相关单据（PO/DN/结算单/INV）</p>
    </div>

    <!-- 搜索栏 -->
    <a-card class="search-card">
      <a-form layout="inline" :model="searchForm">
        <a-row :gutter="16" style="width: 100%">
          <a-col :span="4">
            <a-form-item>
              <a-select
                v-model:value="searchForm.documentType"
                placeholder="单据类型"
                allow-clear
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option
                  v-for="opt in DocumentTypeOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item>
              <a-select
                v-model:value="searchForm.siteCode"
                placeholder="站点"
                allow-clear
                style="width: 100%"
                @change="handleSearch"
              >
                <a-select-option v-for="site in siteOptions" :key="site" :value="site">
                  {{ site }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item>
              <a-input
                v-model:value="searchForm.documentNo"
                placeholder="单据编号"
                allow-clear
                @pressEnter="handleSearch"
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item>
              <a-range-picker
                v-model:value="createTimeRange"
                show-time
                format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
                placeholder="['导出时间起', '导出时间止']"
                @change="handleSearch"
              />
            </a-form-item>
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
        </a-row>
        <a-row :gutter="16" style="width: 100%; margin-top: 8px">
          <a-col :span="24" style="text-align: right">
            <a-button type="primary" @click="$router.push('/document/generate')">
              <PlusOutlined /> 生成单据
            </a-button>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 数据表格 -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1400 }"
        row-key="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'documentType'">
            <a-tag :color="typeColorMap[record.documentType]">
              {{ typeLabelMap[record.documentType] }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            {{ record.totalAmount != null ? record.totalAmount.toFixed(2) : '-' }}
          </template>
          <template v-else-if="column.key === 'siteCode'">
            {{ record.siteCode || '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleViewDetail(record)">
                详情
              </a-button>
              <a-button type="link" size="small" @click="handleExport(record)">
                导出
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <DocumentDetailModal
      v-model:open="detailModalVisible"
      :document-type="currentDetailType"
      :document-id="currentDetailId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import {
  getDocumentList,
  exportPo,
  exportDn,
  exportSettlement,
  exportInv
} from '@/api/document'
import type { DocumentListVO } from '@/types/document'
import { DocumentTypeOptions } from '@/types/document'
import DocumentDetailModal from '@/components/business/document/DocumentDetailModal.vue'

// 类型映射
const typeLabelMap: Record<string, string> = {
  PO: 'PO采购订单',
  DN: 'DN送货单',
  SETTLEMENT: '结算单',
  INV: 'INV发票'
}
const typeColorMap: Record<string, string> = {
  PO: 'blue',
  DN: 'green',
  SETTLEMENT: 'orange',
  INV: 'purple'
}

// 站点选项（固定枚举）
const siteOptions = ['US', 'CA', 'UK', 'EU']

// 搜索
const searchForm = reactive({
  documentType: undefined as string | undefined,
  documentNo: '',
  siteCode: undefined as string | undefined
})
const createTimeRange = ref<[Dayjs, Dayjs] | null>(null)

// 表格
const loading = ref(false)
const tableData = ref<DocumentListVO[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '单据编号', dataIndex: 'documentNo', key: 'documentNo', width: 180, ellipsis: true },
  { title: '单据类型', dataIndex: 'documentType', key: 'documentType', width: 120 },
  { title: '站点', dataIndex: 'siteCode', key: 'siteCode', width: 80 },
  { title: '单据日期', dataIndex: 'documentDate', key: 'documentDate', width: 120 },
  { title: '买方', dataIndex: 'buyerName', key: 'buyerName', width: 180, ellipsis: true },
  { title: '卖方', dataIndex: 'sellerName', key: 'sellerName', width: 180, ellipsis: true },
  { title: '总数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right' as const },
  { title: '总金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, align: 'right' as const },
  { title: '导出时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const }
]

// 详情弹窗
const detailModalVisible = ref(false)
const currentDetailType = ref('')
const currentDetailId = ref(0)

async function fetchData() {
  loading.value = true
  try {
    const res = await getDocumentList({
      documentType: searchForm.documentType,
      documentNo: searchForm.documentNo || undefined,
      siteCode: searchForm.siteCode,
      createTimeStart: createTimeRange.value?.[0]?.format('YYYY-MM-DD HH:mm:ss'),
      createTimeEnd: createTimeRange.value?.[1]?.format('YYYY-MM-DD HH:mm:ss'),
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    const pageData = res.data
    tableData.value = pageData?.records || []
    pagination.total = pageData?.total || 0
  } catch (error) {
    console.error('获取单据列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchForm.documentType = undefined
  searchForm.documentNo = ''
  searchForm.siteCode = undefined
  createTimeRange.value = null
  pagination.current = 1
  fetchData()
}

function handleTableChange(pag: TablePaginationConfig) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  fetchData()
}

function handleViewDetail(record: DocumentListVO) {
  currentDetailType.value = record.documentType
  currentDetailId.value = record.id
  detailModalVisible.value = true
}

async function handleExport(record: DocumentListVO) {
  try {
    const exportMap: Record<string, (id: number) => Promise<void>> = {
      PO: exportPo,
      DN: exportDn,
      SETTLEMENT: exportSettlement,
      INV: exportInv
    }
    const fn = exportMap[record.documentType]
    if (fn) {
      await fn(record.id)
      message.success('导出成功')
    }
  } catch (error) {
    console.error('导出失败:', error)
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.document-list-page {
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
}
</style>
