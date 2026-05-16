<template>
  <div class="document-generate-page">
    <div class="page-header">
      <h1 class="page-title">PO/DN 生成</h1>
      <p class="page-desc">按步骤生成FBT相关单据：选站点 → 选季度 → 选货件 → PO → DN</p>
    </div>

    <!-- 步骤条 -->
    <a-steps :current="currentStep" class="generate-steps" @change="(s: number) => currentStep = s">
      <a-step title="选择条件" :description="stepCompleted[0] ? '已完成' : '待处理'" :status="stepStatus(0)" style="cursor: pointer" />
      <a-step title="生成PO" :description="stepCompleted[1] ? '已完成' : '待处理'" :status="stepStatus(1)" style="cursor: pointer" />
      <a-step title="生成DN" :description="stepCompleted[2] ? '已完成' : '待处理'" :status="stepStatus(2)" style="cursor: pointer" />
    </a-steps>

    <!-- Step 0: 选择条件 -->
    <a-card v-show="currentStep === 0" class="step-content-card">
      <template #title>第一步：选择站点、季度和FBT货件</template>
      <a-form layout="vertical" style="max-width: 600px">
        <a-form-item label="站点" required>
          <a-select v-model:value="selectedSite" placeholder="请选择站点" style="width: 100%" :loading="sitesLoading" @change="handleSiteChange">
            <a-select-option v-for="m in marketplaceOptions" :key="m.siteCode" :value="m.siteCode">
              {{ m.siteCode }} - {{ m.siteName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="结算季度" required>
          <a-select v-model:value="selectedQuarter" placeholder="请选择季度" style="width: 100%" @change="handleQuarterChange">
            <a-select-option v-for="q in quarterOptions" :key="q.value" :value="q.value">{{ q.label }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="FBT货件" required>
          <a-alert v-if="!selectedSite" type="info" message="请先选择站点" show-icon style="margin-bottom: 8px" />
          <a-alert v-else-if="!selectedQuarter" type="info" message="请先选择结算季度" show-icon style="margin-bottom: 8px" />
          <a-spin :spinning="shipmentLoading">
            <div v-if="shipmentList.length > 0" style="margin-bottom: 8px">
              <a-button size="small" @click="selectedShipmentIds = shipmentList.map(s => s.shipmentId)">全选</a-button>
              <a-button size="small" style="margin-left: 8px" @click="selectedShipmentIds = []">取消全选</a-button>
              <span style="margin-left: 12px; color: #1890ff; font-size: 13px">已选 {{ selectedShipmentIds.length }} / {{ shipmentList.length }}</span>
            </div>
            <a-checkbox-group v-model:value="selectedShipmentIds" style="width: 100%">
              <div v-if="shipmentList.length === 0 && !shipmentLoading && selectedSite && selectedQuarter" style="color: #999; padding: 8px 0">
                当前站点和时间范围内暂无货件数据
              </div>
              <div v-for="item in shipmentList" :key="item.shipmentId" class="shipment-item">
                <a-checkbox :value="item.shipmentId">
                  <span class="shipment-id">{{ item.shipmentId }}</span>
                  <span class="shipment-info">
                    {{ item.warehouseCode || '' }} | SKU: {{ item.totalSkus || 0 }} | 数量: {{ item.totalQuantity || 0 }} | {{ item.creationTime?.substring(0, 10) || '' }}
                  </span>
                </a-checkbox>
              </div>
            </a-checkbox-group>
          </a-spin>
        </a-form-item>
      </a-form>

      <div class="step-actions">
        <a-button type="primary" size="large" :disabled="!canProceed" @click="stepCompleted[0] = true; currentStep = 1">
          下一步：生成PO
        </a-button>
      </div>
    </a-card>

    <!-- Step 1: 生成PO -->
    <a-card v-show="currentStep === 1" class="step-content-card">
      <template #title>第二步：生成PO采购订单</template>
      <a-alert type="info" show-icon style="margin-bottom: 16px">
        <template #message>
          站点: {{ selectedSite }} | 季度: {{ quarterLabel }} | 已选货件: {{ selectedShipmentIds.length }} 个
          <br />系统将按货件创建时间自动分组（同一PO日期的货件合并为一份PO）
        </template>
      </a-alert>
      <div class="step-actions">
        <a-button type="primary" size="large" :loading="poLoading" @click="handleGeneratePo">
          生成PO并下载
        </a-button>
        <a-button style="margin-left: 12px" @click="currentStep = 0">返回上一步</a-button>
      </div>
      <div v-if="poResults.length > 0" class="step-result">
        <a-result status="success" :title="`PO生成成功，共 ${poResults.length} 份`">
          <template #subTitle>
            <div v-for="po in poResults" :key="po.id" style="margin: 4px 0">
              {{ po.documentNo }} | {{ po.poDate }} | {{ po.totalQuantity }}件 | {{ po.shipmentCount }}个货件
            </div>
          </template>
          <template #extra>
            <a-space>
              <a-button @click="handleRedownloadPo">重新下载</a-button>
              <a-button type="primary" @click="currentStep = 2">继续生成DN</a-button>
            </a-space>
          </template>
        </a-result>
      </div>
    </a-card>

    <!-- Step 2: 生成DN -->
    <a-card v-show="currentStep === 2" class="step-content-card">
      <template #title>第三步：生成DN送货单</template>
      <a-alert type="info" show-icon style="margin-bottom: 16px">
        <template #message>
          基于已生成的 {{ poResults.length }} 份PO，为每份PO单独设置DN锚点日期（默认PO日期+3工作日）。
        </template>
      </a-alert>

      <!-- 每份PO对应一行，独立设置锚点 -->
      <a-table :data-source="dnAnchorRows" :columns="dnAnchorColumns" :pagination="false" size="small" bordered style="margin-bottom: 16px" row-key="poId">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'anchorDate'">
            <a-date-picker v-model:value="record.anchorDate" style="width: 160px" />
          </template>
          <template v-else-if="column.key === 'autoFill'">
            <a-space>
              <a-button size="small" @click="record.anchorDate = addWorkingDays(dayjs(record.poDate), 3)">+3工作日</a-button>
              <a-button size="small" @click="record.anchorDate = nextTuesday(dayjs(record.poDate))">下周二</a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <div class="step-actions">
        <a-button type="primary" size="large" :loading="dnLoading" :disabled="!canGenerateDn" @click="handleGenerateDn">
          生成全部DN并下载
        </a-button>
        <a-button style="margin-left: 12px" @click="currentStep = 1">返回上一步</a-button>
      </div>

      <div v-if="dnResults.length > 0" class="step-result">
        <a-result status="success" :title="`DN生成成功，共 ${dnResults.length} 份`">
          <template #subTitle>
            <div v-for="dn in dnResults" :key="dn.id" style="margin: 4px 0">
              {{ dn.documentNo }} | {{ dn.dnDate }} | {{ dn.totalQuantity }}件 | 周期: {{ dn.periodStart }} ~ {{ dn.periodEnd }}
            </div>
          </template>
          <template #extra>
            <a-space>
              <a-button @click="handleRedownloadDn">重新下载</a-button>
              <a-button type="primary" @click="$router.push('/tiktok/document/list')">查看单据列表</a-button>
            </a-space>
          </template>
        </a-result>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { getTiktokShipmentList, generateTiktokPo, generateTiktokDn, exportTiktokPo, exportTiktokDn, batchExportPo, batchExportDn, type TiktokShipment } from '@/api/tiktok'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { Marketplace } from '@/types/marketplace'

// ==================== 步骤控制 ====================
const currentStep = ref(0)
const stepCompleted = ref([false, false, false])

function stepStatus(index: number) {
  if (stepCompleted.value[index]) return 'finish'
  if (index === currentStep.value) return 'process'
  return 'wait'
}

// ==================== Step 0 ====================
const marketplaceOptions = ref<Marketplace[]>([])
const sitesLoading = ref(false)
const selectedSite = ref<string | undefined>(undefined)
const selectedQuarter = ref<string | undefined>(undefined)
const shipmentLoading = ref(false)
const shipmentList = ref<TiktokShipment[]>([])
const selectedShipmentIds = ref<string[]>([])

const quarterOptions = computed(() => {
  const now = new Date()
  const options = []
  let y = now.getFullYear(), q = Math.ceil((now.getMonth() + 1) / 3)
  for (let i = 0; i <= 12; i++) {
    const sm = (q - 1) * 3 + 1
    const em = q * 3
    const ld = new Date(y, em, 0).getDate()
    const start = `${y}-${String(sm).padStart(2, '0')}-01`
    const end = `${y}-${String(em).padStart(2, '0')}-${ld}`
    options.push({ label: `${y}年 Q${q}（${start} ~ ${end}）`, value: `${start}|${end}` })
    q--; if (q < 1) { q = 4; y-- }
  }
  return options
})

const quarterLabel = computed(() => quarterOptions.value.find(o => o.value === selectedQuarter.value)?.label || '')
const periodStart = computed(() => selectedQuarter.value?.split('|')[0] || '')
const periodEnd = computed(() => selectedQuarter.value?.split('|')[1] || '')
const canProceed = computed(() => selectedSite.value && selectedQuarter.value && selectedShipmentIds.value.length > 0)

async function fetchMarketplaces() {
  sitesLoading.value = true
  try { const res = await getEnabledMarketplaces(); marketplaceOptions.value = res.data || [] }
  finally {
    sitesLoading.value = false
    if (!selectedSite.value && marketplaceOptions.value.length > 0) {
      selectedSite.value = marketplaceOptions.value.find(m => m.siteCode === 'US')?.siteCode || marketplaceOptions.value[0].siteCode
    }
  }
}

function handleSiteChange() {
  selectedShipmentIds.value = []; shipmentList.value = []; resetResults()
  if (selectedSite.value && selectedQuarter.value) loadShipments()
}

function handleQuarterChange() {
  selectedShipmentIds.value = []; shipmentList.value = []; resetResults()
  if (selectedSite.value && selectedQuarter.value) loadShipments()
}

function resetResults() {
  poResults.value = []; dnResults.value = []; stepCompleted.value = [false, false, false]
}

async function loadShipments() {
  shipmentLoading.value = true
  try {
    const res: any = await getTiktokShipmentList({ siteCode: selectedSite.value!, startDate: periodStart.value, endDate: periodEnd.value, current: 1, size: 200 })
    shipmentList.value = (res.data || res).records || []
  } finally { shipmentLoading.value = false }
}

// ==================== Step 1: PO ====================
const poLoading = ref(false)
const poResults = ref<any[]>([])

async function handleGeneratePo() {
  poLoading.value = true
  try {
    const res: any = await generateTiktokPo({ siteCode: selectedSite.value!, shipmentIds: selectedShipmentIds.value })
    poResults.value = Array.isArray(res.data) ? res.data : [res.data]
    stepCompleted.value[1] = true
    // 初始化每份PO对应的DN锚点行（默认PO日期+3工作日）
    dnAnchorRows.value = poResults.value.map((po: any) => ({
      poId: po.id,
      poDocumentNo: po.documentNo,
      poDate: po.poDate,
      shipmentCount: po.shipmentCount,
      totalQuantity: po.totalQuantity,
      anchorDate: addWorkingDays(dayjs(po.poDate), 3)
    }))
    dnResults.value = []
    // 自动下载
    await downloadPos()
    message.success(`PO生成成功，共 ${poResults.value.length} 份`)
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || 'PO生成失败')
  } finally { poLoading.value = false }
}

async function downloadPos() {
  if (poResults.value.length === 1) {
    await exportTiktokPo(poResults.value[0].id)
  } else if (poResults.value.length > 1) {
    await batchExportPo(poResults.value.map((p: any) => p.id))
  }
}

async function handleRedownloadPo() {
  try { await downloadPos(); message.success('下载成功') } catch { message.error('下载失败') }
}

// ==================== Step 2: DN ====================
const dnLoading = ref(false)
const dnResults = ref<any[]>([])

interface DnAnchorRow {
  poId: number
  poDocumentNo: string
  poDate: string
  shipmentCount: number
  totalQuantity: number
  anchorDate: Dayjs | null
}
const dnAnchorRows = ref<DnAnchorRow[]>([])

const dnAnchorColumns = [
  { title: 'PO编号', dataIndex: 'poDocumentNo', key: 'poDocumentNo', width: 200 },
  { title: 'PO日期', dataIndex: 'poDate', key: 'poDate', width: 110 },
  { title: '货件数', dataIndex: 'shipmentCount', key: 'shipmentCount', width: 80, align: 'center' as const },
  { title: '总数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'center' as const },
  { title: 'DN锚点日期', key: 'anchorDate', width: 200 },
  { title: '快速填入', key: 'autoFill', width: 180 }
]

function addWorkingDays(date: Dayjs, n: number): Dayjs {
  let d = date, count = 0
  while (count < n) { d = d.add(1, 'day'); if (d.day() !== 0 && d.day() !== 6) count++ }
  return d
}

function nextTuesday(date: Dayjs): Dayjs {
  let d = date.add(1, 'day')
  while (d.day() !== 2) d = d.add(1, 'day')
  return d
}

const canGenerateDn = computed(() => dnAnchorRows.value.length > 0 && dnAnchorRows.value.every(r => r.anchorDate))

async function handleGenerateDn() {
  if (!canGenerateDn.value) { message.warning('请为每份PO设置锚点日期'); return }
  dnLoading.value = true
  try {
    const allDns: any[] = []
    for (const row of dnAnchorRows.value) {
      const res: any = await generateTiktokDn({
        siteCode: selectedSite.value!,
        shipmentIds: [],
        anchorDate: row.anchorDate!.format('YYYY-MM-DD'),
        poId: row.poId
      })
      const data = Array.isArray(res.data) ? res.data : [res.data]
      allDns.push(...data)
    }
    dnResults.value = allDns
    stepCompleted.value[2] = true
    await downloadDns()
    message.success(`DN生成成功，共 ${allDns.length} 份`)
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || 'DN生成失败')
  } finally { dnLoading.value = false }
}

async function downloadDns() {
  if (dnResults.value.length === 1) {
    await exportTiktokDn(dnResults.value[0].id)
  } else if (dnResults.value.length > 1) {
    await batchExportDn(dnResults.value.map((d: any) => d.id))
  }
}

async function handleRedownloadDn() {
  try { await downloadDns(); message.success('下载成功') } catch { message.error('下载失败') }
}

onMounted(fetchMarketplaces)
</script>

<style lang="scss" scoped>
.document-generate-page {
  padding: 24px;
  .page-header { margin-bottom: 24px;
    .page-title { font-size: 20px; font-weight: 600; margin: 0 0 4px 0; }
    .page-desc { font-size: 14px; color: #666; margin: 0; }
  }
  .generate-steps { margin-bottom: 24px; }
  .step-content-card { margin-bottom: 16px; }
  .step-actions { margin: 16px 0; }
  .step-result { margin-top: 16px; }
  .shipment-item { padding: 6px 0; border-bottom: 1px solid #f0f0f0;
    &:last-child { border-bottom: none; }
    .shipment-id { font-weight: 500; margin-right: 8px; }
    .shipment-info { color: #888; font-size: 12px; }
  }
}
</style>
