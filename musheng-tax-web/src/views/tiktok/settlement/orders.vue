<template>
  <div class="page-container">
    <div class="page-header">
      <h3>TK 订单明细</h3>
      <a-space wrap>
        <a-select v-model:value="siteCode" placeholder="选择站点" style="width: 140px" @change="loadData">
          <a-select-option v-for="s in sites" :key="s.siteCode" :value="s.siteCode">{{ s.siteCode }} - {{ s.siteName }}</a-select-option>
        </a-select>
        <a-select v-model:value="filters.type" placeholder="类型" style="width: 160px" allow-clear>
          <a-select-option value="Order">Order</a-select-option>
          <a-select-option value="Logistics reimbursement">物流赔偿</a-select-option>
          <a-select-option value="Other adjustment">其他调整</a-select-option>
        </a-select>
        <a-input v-model:value="filters.msku" placeholder="MSKU" style="width: 140px" />
        <a-date-picker v-model:value="filters.startDate" placeholder="开始日期" value-format="YYYY-MM-DD" />
        <a-date-picker v-model:value="filters.endDate" placeholder="结束日期" value-format="YYYY-MM-DD" />
        <a-button type="primary" @click="loadData" :disabled="!siteCode">查询</a-button>
        <a-button :type="filters.unmappedOnly ? 'primary' : 'default'" danger :ghost="!filters.unmappedOnly" @click="filters.unmappedOnly = !filters.unmappedOnly; loadData()" :disabled="!siteCode">
          {{ filters.unmappedOnly ? '显示全部' : '仅未映射' }}
        </a-button>
      </a-space>
    </div>
    <a-alert v-if="!siteCode" message="请先选择站点" type="info" show-icon style="margin-bottom: 16px" />
    <a-table v-else :columns="columns" :data-source="list" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id" size="small" :scroll="{ x: 1400 }"
             :custom-row="(record: any) => ({ onClick: () => showDetail(record) })" :row-class-name="(record: any) => record.msku ? 'clickable-row' : 'clickable-row unmapped-row'">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'msku'">
          <span v-if="record.msku">{{ record.msku }}</span>
          <a-tag v-else color="orange" size="small">未映射</a-tag>
        </template>
      </template>
    </a-table>

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="detailVisible" title="订单明细详情" width="520" placement="right">
      <template v-if="currentRow">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="日期">{{ currentRow.statementDate }}</a-descriptions-item>
          <a-descriptions-item label="类型">{{ currentRow.type }}</a-descriptions-item>
          <a-descriptions-item label="订单号">{{ currentRow.orderId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="SKU ID">{{ currentRow.skuId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="MSKU">{{ currentRow.msku || '-' }}</a-descriptions-item>
          <a-descriptions-item label="商品名称">{{ currentRow.productName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="SKU名称">{{ currentRow.skuName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="数量">{{ currentRow.quantity }}</a-descriptions-item>
          <a-descriptions-item label="币种">{{ currentRow.currency || '-' }}</a-descriptions-item>
        </a-descriptions>
        <a-divider>金额</a-divider>
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="结算总额">{{ fmt(currentRow.totalSettlementAmount) }}</a-descriptions-item>
          <a-descriptions-item label="总收入">{{ fmt(currentRow.totalRevenue) }}</a-descriptions-item>
          <a-descriptions-item label="折后收入">{{ fmt(currentRow.subtotalAfterDiscount) }}</a-descriptions-item>
          <a-descriptions-item label="折前收入">{{ fmt(currentRow.subtotalBeforeDiscount) }}</a-descriptions-item>
          <a-descriptions-item label="卖家折扣">{{ fmt(currentRow.sellerDiscount) }}</a-descriptions-item>
          <a-descriptions-item label="折后退款">{{ fmt(currentRow.refundAfterDiscount) }}</a-descriptions-item>
          <a-descriptions-item label="折前退款">{{ fmt(currentRow.refundBeforeDiscount) }}</a-descriptions-item>
          <a-descriptions-item label="退款折扣返还">{{ fmt(currentRow.refundOfSellerDiscount) }}</a-descriptions-item>
        </a-descriptions>
        <a-divider>费用</a-divider>
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="佣金">{{ fmt(currentRow.commissionFee) }}</a-descriptions-item>
          <a-descriptions-item label="物流费">{{ fmt(currentRow.logisticsFee) }}</a-descriptions-item>
          <a-descriptions-item label="联盟费">{{ fmt(currentRow.affiliateFee) }}</a-descriptions-item>
          <a-descriptions-item label="推广费">{{ fmt(currentRow.promotionFee) }}</a-descriptions-item>
          <a-descriptions-item label="税费">{{ fmt(currentRow.taxFee) }}</a-descriptions-item>
          <a-descriptions-item label="其他费用">{{ fmt(currentRow.otherFee) }}</a-descriptions-item>
          <a-descriptions-item label="Referral Fee">{{ fmt(currentRow.referralFee) }}</a-descriptions-item>
          <a-descriptions-item label="卖家运费">{{ fmt(currentRow.sellerShippingFee) }}</a-descriptions-item>
          <a-descriptions-item label="FBT履约费">{{ fmt(currentRow.fbtFulfillmentFee) }}</a-descriptions-item>
          <a-descriptions-item label="退款管理费">{{ fmt(currentRow.refundAdminFee) }}</a-descriptions-item>
          <a-descriptions-item label="退货运费">{{ fmt(currentRow.actualReturnShippingFee) }}</a-descriptions-item>
          <a-descriptions-item label="退货运费补偿">{{ fmt(currentRow.returnShippingReimb) }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { getTiktokSettlementOrders, type TiktokSettlementOrder } from '@/api/tiktok'
import { useTiktokSites } from '@/composables/tiktok/useTiktokSites'

const { sites, currentSite } = useTiktokSites()
const siteCode = ref('')
const filters = reactive({ type: undefined as string | undefined, msku: '', startDate: '', endDate: '', unmappedOnly: false })
const list = ref<TiktokSettlementOrder[]>([])
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条` })
const detailVisible = ref(false)
const currentRow = ref<any>(null)

watch(currentSite, (val) => { if (val && !siteCode.value) { siteCode.value = val; loadData() } })

const columns = [
  { title: '日期', dataIndex: 'statementDate', width: 100 },
  { title: '类型', dataIndex: 'type', width: 80 },
  { title: '订单号', dataIndex: 'orderId', width: 180 },
  { title: 'MSKU', dataIndex: 'msku', width: 130 },
  { title: '商品', dataIndex: 'productName', ellipsis: true, width: 200 },
  { title: '数量', dataIndex: 'quantity', width: 60 },
  { title: '收入', dataIndex: 'subtotalAfterDiscount', width: 90 },
  { title: '退款', dataIndex: 'refundAfterDiscount', width: 90 },
  { title: '结算额', dataIndex: 'totalSettlementAmount', width: 90 },
  { title: '佣金', dataIndex: 'commissionFee', width: 80 },
  { title: '物流', dataIndex: 'logisticsFee', width: 80 },
  { title: '联盟', dataIndex: 'affiliateFee', width: 80 },
]

async function loadData() {
  if (!siteCode.value) return
  loading.value = true
  try {
    const res: any = await getTiktokSettlementOrders({
      siteCode: siteCode.value,
      type: filters.type, msku: filters.msku || undefined,
      startDate: filters.startDate || undefined, endDate: filters.endDate || undefined,
      unmappedOnly: filters.unmappedOnly || undefined,
      current: pagination.value.current, size: pagination.value.pageSize
    })
    const data = res.data || res
    list.value = data.records || []
    pagination.value.total = data.total || 0
  } finally { loading.value = false }
}

function handleTableChange(pag: any) { pagination.value.current = pag.current; pagination.value.pageSize = pag.pageSize; loadData() }

function showDetail(record: any) { currentRow.value = record; detailVisible.value = true }
function fmt(val: any) { return val != null ? Number(val).toFixed(2) : '-' }
</script>

<style scoped>
.page-container { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 8px; }
:deep(.clickable-row) { cursor: pointer; }
:deep(.clickable-row:hover td) { background: #e6f7ff !important; }
:deep(.unmapped-row td) { background: #fff7e6 !important; }
</style>
