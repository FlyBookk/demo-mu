<template>
  <div class="page-container">
    <div class="page-header">
      <h3>TK FBT货件列表</h3>
      <a-space>
        <a-select v-model:value="siteCode" placeholder="选择站点" style="width: 140px" @change="loadData">
          <a-select-option v-for="s in sites" :key="s.siteCode" :value="s.siteCode">{{ s.siteCode }}</a-select-option>
        </a-select>
        <a-input-search v-model:value="keyword" placeholder="搜索货件单号/仓库" style="width: 260px" @search="loadData" :disabled="!siteCode" />
        <a-range-picker v-model:value="dateRange" :disabled="!siteCode" @change="loadData" style="width: 240px" value-format="YYYY-MM-DD" />
        <a-button type="primary" @click="$router.push('/tiktok/shipment/import')">导入货件</a-button>
      </a-space>
    </div>
    <a-alert v-if="!siteCode" message="请先选择站点" type="info" show-icon style="margin-bottom: 16px" />
    <a-table v-else :columns="columns" :data-source="list" :loading="loading" :pagination="pagination" @change="handleTableChange" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a @click="showItems(record)">查看明细</a>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="itemsVisible" :title="`货件明细 - ${currentShipment}`" width="700px" :footer="null">
      <a-table :columns="itemColumns" :data-source="items" row-key="id" size="small" :pagination="false" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getTiktokShipmentList, getTiktokShipmentItems, type TiktokShipment, type TiktokShipmentItem } from '@/api/tiktok'
import { useTiktokSites } from '@/composables/tiktok/useTiktokSites'

const { sites, currentSite } = useTiktokSites()
const siteCode = ref('')
const keyword = ref('')
const dateRange = ref<[string, string] | null>(null)
const list = ref<TiktokShipment[]>([])
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条` })
const itemsVisible = ref(false)
const currentShipment = ref('')
const items = ref<TiktokShipmentItem[]>([])

watch(currentSite, (val) => { if (val && !siteCode.value) { siteCode.value = val; loadData() } })

const columns = [
  { title: '货件单号', dataIndex: 'shipmentId', key: 'shipmentId', width: 200 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '仓库', dataIndex: 'warehouseCode', key: 'warehouseCode', width: 180 },
  { title: 'SKU数', dataIndex: 'totalSkus', key: 'totalSkus', width: 80 },
  { title: '总数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90 },
  { title: '创建时间', dataIndex: 'creationTime', key: 'creationTime', width: 160 },
  { title: '操作', key: 'action', width: 90 },
]

const itemColumns = [
  { title: 'MSKU', dataIndex: 'msku', key: 'msku' },
  { title: '申报量', dataIndex: 'quantityDeclared', key: 'quantityDeclared', width: 90 },
  { title: '签收量', dataIndex: 'quantityReceived', key: 'quantityReceived', width: 90 },
]

async function loadData() {
  if (!siteCode.value) return
  loading.value = true
  try {
    const res: any = await getTiktokShipmentList({ keyword: keyword.value, siteCode: siteCode.value, startDate: dateRange.value?.[0], endDate: dateRange.value?.[1], current: pagination.value.current, size: pagination.value.pageSize })
    const data = res.data || res
    list.value = data.records || []
    pagination.value.total = data.total || 0
  } finally { loading.value = false }
}

function handleTableChange(pag: any) { pagination.value.current = pag.current; pagination.value.pageSize = pag.pageSize; loadData() }

async function showItems(record: TiktokShipment) {
  currentShipment.value = record.shipmentId
  const res: any = await getTiktokShipmentItems(record.shipmentId, siteCode.value)
  items.value = res.data || res || []
  itemsVisible.value = true
}
</script>

<style scoped>
.page-container { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
