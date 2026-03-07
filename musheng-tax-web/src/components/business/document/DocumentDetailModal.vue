<template>
  <a-modal
    :open="open"
    :title="modalTitle"
    width="900px"
    :footer="null"
    @update:open="$emit('update:open', $event)"
  >
    <a-spin :spinning="loading">
      <!-- PO详情 -->
      <template v-if="documentType === 'PO' && poData">
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="单据编号">{{ poData.documentNo }}</a-descriptions-item>
          <a-descriptions-item label="PO日期">{{ poData.poDate }}</a-descriptions-item>
          <a-descriptions-item label="买方">{{ poData.buyerName }}</a-descriptions-item>
          <a-descriptions-item label="卖方">{{ poData.sellerName }}</a-descriptions-item>
          <a-descriptions-item label="买方地址" :span="2">{{ poData.buyerAddress }}</a-descriptions-item>
          <a-descriptions-item label="总数量">{{ poData.totalQuantity }}</a-descriptions-item>
          <a-descriptions-item label="货件数">{{ poData.shipmentCount }}</a-descriptions-item>
        </a-descriptions>
        <h4 style="margin-bottom: 12px">PO明细</h4>
        <a-table
          :columns="poItemColumns"
          :data-source="poData.items || []"
          :pagination="false"
          :scroll="{ y: 300 }"
          size="small"
          row-key="id"
        />
      </template>

      <!-- DN详情 -->
      <template v-if="documentType === 'DN' && dnData">
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="单据编号">{{ dnData.documentNo }}</a-descriptions-item>
          <a-descriptions-item label="送货日期">{{ dnData.dnDate }}</a-descriptions-item>
          <a-descriptions-item label="供应商">{{ dnData.supplierName }}</a-descriptions-item>
          <a-descriptions-item label="客户">{{ dnData.customerName }}</a-descriptions-item>
          <a-descriptions-item label="DN周期">{{ dnData.periodStart }} ~ {{ dnData.periodEnd }}</a-descriptions-item>
          <a-descriptions-item label="总数量">{{ dnData.totalQuantity }}</a-descriptions-item>
        </a-descriptions>
        <h4 style="margin-bottom: 12px">DN明细</h4>
        <a-table
          :columns="dnItemColumns"
          :data-source="dnData.items || []"
          :pagination="false"
          :scroll="{ y: 300 }"
          size="small"
          row-key="id"
        />
      </template>

      <!-- 结算单详情 -->
      <template v-if="documentType === 'SETTLEMENT' && settlementData">
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="单据编号">{{ settlementData.documentNo }}</a-descriptions-item>
          <a-descriptions-item label="结算日">{{ settlementData.settlementDate }}</a-descriptions-item>
          <a-descriptions-item label="结算周期">{{ settlementData.periodStart }} ~ {{ settlementData.periodEnd }}</a-descriptions-item>
          <a-descriptions-item label="站点">{{ settlementData.siteCode }} ({{ settlementData.siteSequence }})</a-descriptions-item>
          <a-descriptions-item label="买方">{{ settlementData.buyerName }}</a-descriptions-item>
          <a-descriptions-item label="卖方">{{ settlementData.sellerName }}</a-descriptions-item>
          <a-descriptions-item label="总数量">{{ settlementData.totalQuantity }}</a-descriptions-item>
          <a-descriptions-item label="总金额">{{ settlementData.totalAmount?.toFixed(2) }}</a-descriptions-item>
        </a-descriptions>
        <h4 style="margin-bottom: 12px">结算明细</h4>
        <a-table
          :columns="settlementItemColumns"
          :data-source="settlementData.items || []"
          :pagination="false"
          :scroll="{ y: 300 }"
          size="small"
          row-key="id"
        />
      </template>

      <!-- INV详情 -->
      <template v-if="documentType === 'INV' && invData">
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="INV编号">{{ invData.documentNo }}</a-descriptions-item>
          <a-descriptions-item label="INV日期">{{ invData.invDate }}</a-descriptions-item>
          <a-descriptions-item label="站点">{{ invData.siteCode }} ({{ invData.siteSequence }})</a-descriptions-item>
          <a-descriptions-item label="关联结算单ID">{{ invData.settlementId }}</a-descriptions-item>
          <a-descriptions-item label="卖方">{{ invData.sellerName }}</a-descriptions-item>
          <a-descriptions-item label="买方">{{ invData.buyerName }}</a-descriptions-item>
          <a-descriptions-item label="卖方地址" :span="2">{{ invData.sellerAddress }}</a-descriptions-item>
          <a-descriptions-item label="买方地址" :span="2">{{ invData.buyerAddress }}</a-descriptions-item>
          <a-descriptions-item label="总数量">{{ invData.totalQuantity }}</a-descriptions-item>
          <a-descriptions-item label="总金额">{{ invData.totalAmount?.toFixed(2) }}</a-descriptions-item>
        </a-descriptions>
        <!-- 银行信息 -->
        <h4 style="margin: 16px 0 12px">银行信息</h4>
        <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
          <a-descriptions-item label="账户名">{{ invData.bankAccountName }}</a-descriptions-item>
          <a-descriptions-item label="账号">{{ invData.bankAccountNumber }}</a-descriptions-item>
          <a-descriptions-item label="银行名称">{{ invData.bankName }}</a-descriptions-item>
          <a-descriptions-item label="SWIFT">{{ invData.swiftCode }}</a-descriptions-item>
          <a-descriptions-item label="银行地址" :span="2">{{ invData.bankAddress }}</a-descriptions-item>
        </a-descriptions>
        <h4 style="margin-bottom: 12px">INV明细</h4>
        <a-table
          :columns="invItemColumns"
          :data-source="invData.items || []"
          :pagination="false"
          :scroll="{ y: 300 }"
          size="small"
          row-key="id"
        />
      </template>

      <!-- 无数据 -->
      <a-empty v-if="!loading && !poData && !dnData && !settlementData && !invData" />
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  getPoDetail,
  getDnDetail,
  getSettlementDetail,
  getInvDetail
} from '@/api/document'
import type { PoVO, DnVO, SettlementVO, InvVO } from '@/types/document'

const props = defineProps<{
  open: boolean
  documentType: string
  documentId: number
}>()

defineEmits<{
  'update:open': [value: boolean]
}>()

const loading = ref(false)
const poData = ref<PoVO | null>(null)
const dnData = ref<DnVO | null>(null)
const settlementData = ref<SettlementVO | null>(null)
const invData = ref<InvVO | null>(null)

const modalTitle = computed(() => {
  const titleMap: Record<string, string> = {
    PO: 'PO采购订单详情',
    DN: 'DN送货单详情',
    SETTLEMENT: '结算单详情',
    INV: 'INV发票详情'
  }
  return titleMap[props.documentType] || '单据详情'
})

// PO明细列
const poItemColumns = [
  { title: '货件编号', dataIndex: 'shipmentNo', width: 160 },
  { title: 'MSKU', dataIndex: 'msku', width: 200, ellipsis: true },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'right' as const },
  { title: 'FBA仓库地址', dataIndex: 'fbaAddress', ellipsis: true }
]

// DN明细列
const dnItemColumns = [
  { title: '行号', dataIndex: 'lineNo', width: 60 },
  { title: 'MSKU', dataIndex: 'msku', width: 200, ellipsis: true },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'right' as const },
  { title: '对应货件', dataIndex: 'shipmentNo', width: 160 }
]

// 结算单明细列
const settlementItemColumns = [
  { title: '序号', dataIndex: 'lineNo', width: 60 },
  { title: 'MSKU', dataIndex: 'msku', width: 180, ellipsis: true },
  { title: '货币', dataIndex: 'currency', width: 60 },
  { title: '单价', dataIndex: 'unitPrice', width: 100, align: 'right' as const },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'right' as const },
  { title: '金额', dataIndex: 'amount', width: 100, align: 'right' as const }
]

// INV明细列
const invItemColumns = [
  { title: '序号', dataIndex: 'lineNo', width: 60 },
  { title: 'MSKU', dataIndex: 'msku', width: 180, ellipsis: true },
  { title: '数量', dataIndex: 'quantity', width: 80, align: 'right' as const },
  { title: '单价', dataIndex: 'unitPrice', width: 100, align: 'right' as const },
  { title: '金额', dataIndex: 'amount', width: 100, align: 'right' as const }
]

async function fetchDetail() {
  if (!props.documentId || !props.documentType) return
  // 清空旧数据
  poData.value = null
  dnData.value = null
  settlementData.value = null
  invData.value = null

  loading.value = true
  try {
    switch (props.documentType) {
      case 'PO': {
        const res = await getPoDetail(props.documentId)
        poData.value = res.data
        break
      }
      case 'DN': {
        const res = await getDnDetail(props.documentId)
        dnData.value = res.data
        break
      }
      case 'SETTLEMENT': {
        const res = await getSettlementDetail(props.documentId)
        settlementData.value = res.data
        break
      }
      case 'INV': {
        const res = await getInvDetail(props.documentId)
        invData.value = res.data
        break
      }
    }
  } catch (error) {
    console.error('获取单据详情失败:', error)
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.open, props.documentId],
  ([newOpen]) => {
    if (newOpen && props.documentId) {
      fetchDetail()
    }
  }
)
</script>
