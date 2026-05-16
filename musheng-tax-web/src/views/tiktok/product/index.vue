<template>
  <div class="page-container">
    <div class="page-header">
      <h3>TK 商品管理</h3>
      <a-space>
        <a-select v-model:value="siteCode" placeholder="选择站点" style="width: 140px" @change="loadData">
          <a-select-option v-for="s in sites" :key="s.siteCode" :value="s.siteCode">{{ s.siteCode }}</a-select-option>
        </a-select>
        <a-input-search v-model:value="keyword" placeholder="搜索MSKU/商品名/SKU ID" style="width: 280px" @search="loadData" :disabled="!siteCode" />
        <a-button :type="onlyUnmapped ? 'primary' : 'default'" @click="onlyUnmapped = !onlyUnmapped; loadData()" :disabled="!siteCode">
          {{ onlyUnmapped ? '显示全部' : '仅未映射' }}
        </a-button>
        <a-button type="primary" @click="$router.push('/tiktok/product/import')">导入对照表</a-button>
      </a-space>
    </div>
    <a-alert v-if="!siteCode" message="请先选择站点" type="info" show-icon style="margin-bottom: 16px" />
    <a-table v-else :columns="columns" :data-source="list" :loading="loading" :pagination="pagination"
             @change="handleTableChange" row-key="id"
             :row-class-name="() => 'clickable-row'" :custom-row="(record: any) => ({ onClick: () => showDetail(record) })">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.msku ? 'green' : 'orange'">{{ record.msku ? '已映射' : '待补充' }}</a-tag>
        </template>
        <template v-if="column.key === 'msku'">
          <a-typography-text editable :content="record.msku" @update:content="(val: string) => handleUpdateMsku(record.id, val)" />
        </template>
        <template v-if="column.key === 'price'">
          {{ record.price != null ? `$${Number(record.price).toFixed(2)}` : '-' }}
        </template>
      </template>
    </a-table>

    <!-- 详情抽屉 -->
    <a-drawer v-model:open="detailVisible" title="商品详情" width="480" placement="right">
      <template v-if="currentProduct">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="ID">{{ currentProduct.id }}</a-descriptions-item>
          <a-descriptions-item label="站点">{{ currentProduct.siteCode }}</a-descriptions-item>
          <a-descriptions-item label="TK商品ID">{{ currentProduct.productId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="SKU ID">{{ currentProduct.skuId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="MSKU">
            <a-tag v-if="currentProduct.msku" color="green">{{ currentProduct.msku }}</a-tag>
            <a-tag v-else color="orange">待补充</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="商品名称">{{ currentProduct.productName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="类目">{{ currentProduct.category || '-' }}</a-descriptions-item>
          <a-descriptions-item label="变体选项">{{ currentProduct.variationValue || '-' }}</a-descriptions-item>
          <a-descriptions-item label="零售价">{{ currentProduct.price != null ? `$${Number(currentProduct.price).toFixed(2)}` : '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-badge :status="currentProduct.status === 1 ? 'success' : 'default'" :text="currentProduct.status === 1 ? '启用' : '禁用'" />
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ currentProduct.createTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="更新时间">{{ currentProduct.updateTime || '-' }}</a-descriptions-item>
        </a-descriptions>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getTiktokProductList, updateTiktokProductMsku, type TiktokProduct } from '@/api/tiktok'
import { useTiktokSites } from '@/composables/tiktok/useTiktokSites'
import { message } from 'ant-design-vue'

const { sites, currentSite } = useTiktokSites()
const siteCode = ref('')
const keyword = ref('')
const onlyUnmapped = ref(false)
const list = ref<TiktokProduct[]>([])
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条` })
const detailVisible = ref(false)
const currentProduct = ref<TiktokProduct | null>(null)

watch(currentSite, (val) => { if (val && !siteCode.value) { siteCode.value = val; loadData() } })

const columns = [
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: 'SKU ID', dataIndex: 'skuId', key: 'skuId', width: 180 },
  { title: 'MSKU', dataIndex: 'msku', key: 'msku', width: 150 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', ellipsis: true },
  { title: '变体', dataIndex: 'variationValue', key: 'variationValue', width: 120 },
  { title: '零售价', dataIndex: 'price', key: 'price', width: 90 },
  { title: '类目', dataIndex: 'category', key: 'category', width: 150 },
]

async function loadData() {
  if (!siteCode.value) return
  loading.value = true
  try {
    const res: any = await getTiktokProductList({ keyword: keyword.value, siteCode: siteCode.value, current: pagination.value.current, size: pagination.value.pageSize })
    const data = res.data || res
    let records = data.records || []
    if (onlyUnmapped.value) {
      records = records.filter((r: any) => !r.msku)
    }
    list.value = records
    pagination.value.total = onlyUnmapped.value ? records.length : (data.total || 0)
  } finally { loading.value = false }
}

function handleTableChange(pag: any) { pagination.value.current = pag.current; pagination.value.pageSize = pag.pageSize; loadData() }

async function handleUpdateMsku(id: number, msku: string) {
  await updateTiktokProductMsku(id, msku)
  message.success('MSKU已更新')
  loadData()
}

function showDetail(record: TiktokProduct) {
  currentProduct.value = record
  detailVisible.value = true
}
</script>

<style scoped>
.page-container { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
:deep(.clickable-row) { cursor: pointer; }
:deep(.clickable-row:hover td) { background: #e6f7ff !important; }
</style>
