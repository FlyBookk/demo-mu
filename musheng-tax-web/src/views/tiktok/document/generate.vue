<template>
  <div class="document-generate-page">
    <div class="page-header">
      <h1 class="page-title">PO/DN 生成</h1>
      <p class="page-desc">选择站点和FBT货件，生成PO采购单和DN送货单（按日期自动分组，可能生成多份）</p>
    </div>

    <!-- 步骤一：选择条件 -->
    <a-card title="① 选择站点和货件" class="step-card">
      <a-form layout="vertical" style="max-width: 600px">
        <a-form-item label="站点" required>
          <a-select v-model:value="selectedSite" placeholder="请选择站点" style="width: 100%" :loading="sitesLoading" @change="handleSiteChange">
            <a-select-option v-for="m in marketplaceOptions" :key="m.siteCode" :value="m.siteCode">
              {{ m.siteCode }} - {{ m.siteName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="DN锚点日期（仅生成DN时需要）">
          <a-date-picker v-model:value="anchorDate" placeholder="选择锚点日期" style="width: 100%" value-format="YYYY-MM-DD" />
          <div style="font-size: 12px; color: #999; margin-top: 4px">
            DN按锚点+21天周期分组货件，不同周期生成不同DN
          </div>
        </a-form-item>

        <a-form-item label="FBT货件" required>
          <a-alert v-if="!selectedSite" type="info" message="请先选择站点" show-icon style="margin-bottom: 8px" />
          <a-spin :spinning="shipmentLoading">
            <div v-if="shipmentList.length > 0" style="margin-bottom: 8px">
              <a-button size="small" @click="selectAll">全选</a-button>
              <a-button size="small" style="margin-left: 8px" @click="selectNone">取消全选</a-button>
            </div>
            <a-checkbox-group v-model:value="selectedShipmentIds" style="width: 100%">
              <div v-if="shipmentList.length === 0 && !shipmentLoading && selectedSite" style="color: #999; padding: 8px 0">
                当前站点暂无货件数据
              </div>
              <div v-for="item in shipmentList" :key="item.shipmentId" class="shipment-item">
                <a-checkbox :value="item.shipmentId">
                  <span class="shipment-id">{{ item.shipmentId }}</span>
                  <span class="shipment-info">
                    {{ item.warehouseCode || '' }} | SKU: {{ item.totalSkus || 0 }} | 数量: {{ item.totalQuantity || 0 }}
                  </span>
                </a-checkbox>
              </div>
            </a-checkbox-group>
          </a-spin>
          <div v-if="selectedShipmentIds.length > 0" style="margin-top: 8px; color: #1890ff; font-size: 13px">
            已选 {{ selectedShipmentIds.length }} 个货件
          </div>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 步骤二：生成 -->
    <a-card title="② 生成单据" style="margin-top: 16px" class="step-card">
      <a-space>
        <a-button type="primary" :loading="generating" :disabled="!canGenerate" @click="handleGenerate('PO')">
          生成 PO
        </a-button>
        <a-button type="primary" ghost :loading="generating" :disabled="!canGenerateDn" @click="handleGenerate('DN')">
          生成 DN
        </a-button>
      </a-space>

      <template v-if="results.length > 0">
        <a-divider />
        <a-alert type="success" show-icon style="margin-bottom: 12px">
          <template #message>已生成 {{ results.length }} 份单据</template>
        </a-alert>
        <a-list :data-source="results" size="small" bordered>
          <template #renderItem="{ item }">
            <a-list-item>
              <a-tag :color="item.type === 'PO' ? 'blue' : 'green'">{{ item.type }}</a-tag>
              {{ item.documentNo }} | {{ item.date }} | {{ item.totalQuantity }} 件
            </a-list-item>
          </template>
        </a-list>
        <div style="margin-top: 12px">
          <a-button type="primary" @click="$router.push('/tiktok/document/list')">查看单据列表</a-button>
        </div>
      </template>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getTiktokShipmentList, generateTiktokPo, generateTiktokDn, type TiktokShipment } from '@/api/tiktok'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { Marketplace } from '@/types/marketplace'

const marketplaceOptions = ref<Marketplace[]>([])
const sitesLoading = ref(false)
const selectedSite = ref<string | undefined>(undefined)
const anchorDate = ref<string | undefined>(undefined)
const selectedShipmentIds = ref<string[]>([])
const shipmentList = ref<TiktokShipment[]>([])
const shipmentLoading = ref(false)
const generating = ref(false)
const results = ref<{ type: string; documentNo: string; date: string; totalQuantity: number }[]>([])

async function fetchMarketplaces() {
  sitesLoading.value = true
  try { const res = await getEnabledMarketplaces(); marketplaceOptions.value = res.data || [] }
  finally { sitesLoading.value = false }
}

async function handleSiteChange() {
  selectedShipmentIds.value = []
  results.value = []
  if (!selectedSite.value) { shipmentList.value = []; return }
  shipmentLoading.value = true
  try {
    const res: any = await getTiktokShipmentList({ siteCode: selectedSite.value, current: 1, size: 500 })
    shipmentList.value = (res.data || res).records || []
  } finally { shipmentLoading.value = false }
}

const canGenerate = computed(() => selectedSite.value && selectedShipmentIds.value.length > 0)
const canGenerateDn = computed(() => canGenerate.value && !!anchorDate.value)

function selectAll() { selectedShipmentIds.value = shipmentList.value.map(s => s.shipmentId) }
function selectNone() { selectedShipmentIds.value = [] }

async function handleGenerate(type: 'PO' | 'DN') {
  if (!canGenerate.value) return
  if (type === 'DN' && !anchorDate.value) {
    message.warning('生成DN需要选择锚点日期')
    return
  }
  generating.value = true
  try {
    let data: any[]
    if (type === 'PO') {
      const res: any = await generateTiktokPo({ siteCode: selectedSite.value!, shipmentIds: selectedShipmentIds.value })
      data = Array.isArray(res.data) ? res.data : [res.data]
    } else {
      const res: any = await generateTiktokDn({ siteCode: selectedSite.value!, shipmentIds: selectedShipmentIds.value, anchorDate: anchorDate.value })
      data = Array.isArray(res.data) ? res.data : [res.data]
    }
    for (const item of data) {
      results.value.push({
        type,
        documentNo: item.documentNo,
        date: type === 'PO' ? item.poDate : item.dnDate,
        totalQuantity: item.totalQuantity
      })
    }
    message.success(`${type} 生成成功，共 ${data.length} 份`)
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '生成失败')
  } finally { generating.value = false }
}

onMounted(fetchMarketplaces)
</script>

<style lang="scss" scoped>
.document-generate-page {
  padding: 24px;
  .page-header { margin-bottom: 24px;
    .page-title { font-size: 20px; font-weight: 600; color: #333; margin: 0 0 8px 0; }
    .page-desc { font-size: 14px; color: #999; margin: 0; }
  }
  .step-card { :deep(.ant-card-head-title) { font-weight: 600; } }
  .shipment-item { padding: 6px 0; border-bottom: 1px solid #f0f0f0;
    .shipment-id { font-weight: 500; margin-right: 8px; }
    .shipment-info { color: #888; font-size: 12px; }
  }
}
</style>
