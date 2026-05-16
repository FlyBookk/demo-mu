<template>
  <div class="page-container">
    <div class="page-header">
      <h3>TK 单据列表</h3>
      <p class="page-desc">查看和管理TK相关单据（PO/DN/结算单/INV）</p>
    </div>

    <!-- 搜索栏 -->
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-form layout="inline">
        <a-form-item>
          <a-select v-model:value="searchForm.documentType" placeholder="单据类型" allow-clear style="width: 120px" @change="handleSearch">
            <a-select-option value="PO">PO</a-select-option>
            <a-select-option value="DN">DN</a-select-option>
            <a-select-option value="SETTLEMENT">结算单</a-select-option>
            <a-select-option value="INV">INV</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-select v-model:value="searchForm.siteCode" placeholder="站点" allow-clear style="width: 100px" @change="handleSearch">
            <a-select-option v-for="s in sites" :key="s.siteCode" :value="s.siteCode">{{ s.siteCode }} - {{ s.siteName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-input v-model:value="searchForm.documentNo" placeholder="单据编号" allow-clear style="width: 180px" @pressEnter="handleSearch" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 表格 -->
    <a-card :bordered="false">
      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1200 }"
        row-key="id"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'documentType'">
            <a-tag :color="typeColor(record.documentType)">{{ record.documentType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            {{ record.totalAmount != null ? Number(record.totalAmount).toFixed(2) : '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleViewDetail(record)">详情</a-button>
              <a-button type="link" size="small" @click="handleExport(record)">导出</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal v-model:open="detailVisible" :title="detailTitle" width="800px" :footer="null">
      <a-descriptions :column="2" bordered size="small" v-if="detailData" style="margin-bottom: 16px">
        <a-descriptions-item label="单据编号">{{ detailData.documentNo }}</a-descriptions-item>
        <a-descriptions-item label="站点">{{ detailData.siteCode }}</a-descriptions-item>
        <a-descriptions-item label="买方/供应商">{{ detailData.buyerName || detailData.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="卖方/客户">{{ detailData.sellerName || detailData.customerName }}</a-descriptions-item>
        <a-descriptions-item label="总数量">{{ detailData.totalQuantity }}</a-descriptions-item>
        <a-descriptions-item label="总金额" v-if="detailData.totalAmount">{{ Number(detailData.totalAmount).toFixed(2) }}</a-descriptions-item>
      </a-descriptions>
      <a-table :columns="detailColumns" :data-source="detailItems" :pagination="false" size="small" bordered />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  getTiktokDocumentList, getTiktokPoDetail, getTiktokDnDetail,
  getTiktokSettlementDetail, getTiktokInvDetail,
  exportTiktokPo, exportTiktokDn, exportTiktokSettlement, exportTiktokInv,
  type TiktokDocumentListItem
} from '@/api/tiktok'
import { useTiktokSites } from '@/composables/tiktok/useTiktokSites'

const { sites } = useTiktokSites()
const list = ref<TiktokDocumentListItem[]>([])
const loading = ref(false)
const searchForm = reactive({ documentType: undefined as string | undefined, documentNo: undefined as string | undefined, siteCode: undefined as string | undefined })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条` })

const detailVisible = ref(false)
const detailTitle = ref('')
const detailData = ref<any>(null)
const detailItems = ref<any[]>([])
const detailColumns = ref<any[]>([])

const columns = [
  { title: '类型', dataIndex: 'documentType', key: 'documentType', width: 90 },
  { title: '单据编号', dataIndex: 'documentNo', width: 200 },
  { title: '站点', dataIndex: 'siteCode', width: 60 },
  { title: '日期', dataIndex: 'documentDate', width: 110 },
  { title: '买方/供应商', dataIndex: 'buyerName', width: 140 },
  { title: '卖方/客户', dataIndex: 'sellerName', width: 140 },
  { title: '总数量', dataIndex: 'totalQuantity', width: 80 },
  { title: '总金额', key: 'totalAmount', width: 120 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
]

function typeColor(type: string) {
  return { PO: 'blue', DN: 'green', SETTLEMENT: 'orange', INV: 'purple' }[type] || 'default'
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getTiktokDocumentList({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchForm })
    const data = res.data || res
    list.value = data.records || []
    pagination.total = data.total || 0
  } finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.documentType = undefined; searchForm.documentNo = undefined; searchForm.siteCode = undefined; handleSearch() }
function handleTableChange(pag: any) { pagination.current = pag.current; pagination.pageSize = pag.pageSize; loadData() }

async function handleViewDetail(record: TiktokDocumentListItem) {
  detailTitle.value = `${record.documentType} - ${record.documentNo}`
  const type = record.documentType
  let res: any
  if (type === 'PO') {
    res = await getTiktokPoDetail(record.id); detailData.value = (res.data || res).po; detailItems.value = (res.data || res).items || []
    detailColumns.value = [{ title: '序号', dataIndex: 'sortOrder', width: 60 }, { title: '货件编号', dataIndex: 'shipmentNo', width: 160 }, { title: 'MSKU', dataIndex: 'msku', width: 140 }, { title: '数量', dataIndex: 'quantity', width: 80 }, { title: 'FBT地址', dataIndex: 'fbtAddress' }]
  } else if (type === 'DN') {
    res = await getTiktokDnDetail(record.id); detailData.value = (res.data || res).dn; detailItems.value = (res.data || res).items || []
    detailColumns.value = [{ title: '行号', dataIndex: 'lineNo', width: 60 }, { title: 'MSKU', dataIndex: 'msku', width: 140 }, { title: '数量', dataIndex: 'quantity', width: 80 }, { title: '货件编号', dataIndex: 'shipmentNo', width: 160 }]
  } else if (type === 'SETTLEMENT') {
    res = await getTiktokSettlementDetail(record.id); detailData.value = (res.data || res).settlement; detailItems.value = (res.data || res).items || []
    detailColumns.value = [{ title: '行号', dataIndex: 'lineNo', width: 60 }, { title: 'MSKU', dataIndex: 'msku', width: 140 }, { title: '币种', dataIndex: 'currency', width: 60 }, { title: '单价', dataIndex: 'unitPrice', width: 100 }, { title: '数量', dataIndex: 'quantity', width: 80 }, { title: '金额', dataIndex: 'amount', width: 120 }]
  } else if (type === 'INV') {
    res = await getTiktokInvDetail(record.id); detailData.value = (res.data || res).inv; detailItems.value = (res.data || res).items || []
    detailColumns.value = [{ title: '行号', dataIndex: 'lineNo', width: 60 }, { title: 'MSKU', dataIndex: 'msku', width: 140 }, { title: '数量', dataIndex: 'quantity', width: 80 }, { title: '单价', dataIndex: 'unitPrice', width: 100 }, { title: '金额', dataIndex: 'amount', width: 120 }]
  }
  detailVisible.value = true
}

async function handleExport(record: TiktokDocumentListItem) {
  try {
    const fnMap: Record<string, (id: number) => Promise<any>> = { PO: exportTiktokPo, DN: exportTiktokDn, SETTLEMENT: exportTiktokSettlement, INV: exportTiktokInv }
    const fn = fnMap[record.documentType]
    if (fn) { await fn(record.id); message.success('导出成功') }
  } catch (e) { console.error('导出失败:', e) }
}

onMounted(loadData)
</script>

<style scoped>
.page-container { padding: 24px; }
.page-header { margin-bottom: 16px; }
.page-desc { color: #666; font-size: 13px; margin-top: 4px; }
</style>
