<template>
  <div class="document-generate-page">
    <div class="page-header">
      <h1 class="page-title">PO/DN 生成</h1>
      <p class="page-desc">按步骤生成FBA相关单据：选站点 → 选季度 → 选货件 → PO → DN</p>
    </div>

    <!-- 步骤条 -->
    <a-steps :current="currentStep" class="generate-steps" @change="goToStep">
      <a-step title="选择条件" :description="stepDesc(0)" :status="stepStatus(0)" style="cursor: pointer" />
      <a-step title="生成PO" :description="stepDesc(1)" :status="stepStatus(1)" style="cursor: pointer" />
      <a-step title="生成DN" :description="stepDesc(2)" :status="stepStatus(2)" style="cursor: pointer" />
    </a-steps>

    <!-- Step 0: 选择站点/季度/货件 -->
    <a-card v-show="currentStep === 0" class="step-content-card">
      <template #title>第一步：选择站点、季度和FBA货件</template>
      <a-form layout="vertical" style="max-width: 600px">
        <a-form-item label="站点" required>
          <a-select
            v-model:value="selectedSite"
            placeholder="请选择站点"
            style="width: 100%"
            @change="handleSiteChange"
          >
            <a-select-option v-for="site in siteOptions" :key="site.value" :value="site.value">
              {{ site.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="结算季度" required>
          <a-select
            v-model:value="selectedQuarter"
            placeholder="请选择季度"
            style="width: 100%"
            :options="quarterOptions"
            @change="handleQuarterChange"
          />
        </a-form-item>

        <a-form-item label="FBA货件" required>
          <a-alert v-if="!selectedSite" type="info" message="请先选择站点" show-icon style="margin-bottom: 8px" />
          <a-alert v-else-if="!selectedQuarter" type="info" message="请先选择结算季度" show-icon style="margin-bottom: 8px" />
          <a-alert
            v-if="selectedShipmentIds.length > MAX_SHIPMENT_COUNT"
            type="error"
            :message="`最多选择 ${MAX_SHIPMENT_COUNT} 个货件，当前已选 ${selectedShipmentIds.length} 个，请减少选择`"
            show-icon
            style="margin-bottom: 8px"
          />
          <a-spin :spinning="shipmentLoading">
            <div v-if="shipmentList.length > 0" style="margin-bottom: 8px">
              <a-button size="small" @click="selectedShipmentIds = shipmentList.map(s => s.id)">全选</a-button>
              <a-button size="small" style="margin-left: 8px" @click="selectedShipmentIds = []">取消全选</a-button>
              <span style="margin-left: 12px; color: #1890ff; font-size: 13px">已选 {{ selectedShipmentIds.length }} / {{ shipmentList.length }}</span>
            </div>
            <a-checkbox-group v-model:value="selectedShipmentIds" style="width: 100%">
              <div v-if="shipmentList.length === 0 && !shipmentLoading && selectedSite" style="color: #999; padding: 8px 0">
                当前站点和时间范围内暂无货件数据
              </div>
              <div v-for="item in shipmentList" :key="item.id" class="shipment-item">
                <a-checkbox :value="item.id">
                  <span class="shipment-id">{{ item.shipmentId }}</span>
                  <span class="shipment-info">
                    {{ item.shipmentName || '' }} | SKU: {{ item.skuCount || 0 }} | 数量: {{ item.totalQuantity || 0 }}
                  </span>
                </a-checkbox>
              </div>
            </a-checkbox-group>
          </a-spin>
        </a-form-item>
      </a-form>

      <div class="step-actions">
        <a-button
          type="primary"
          size="large"
          :disabled="!canProceedToGenerate"
          @click="proceedToGeneratePo"
        >
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
        </template>
      </a-alert>
      <div class="step-actions">
        <a-button
          type="primary"
          size="large"
          :loading="poLoading"
          @click="handleGeneratePo"
        >
          <DownloadOutlined /> 生成PO并下载Excel
        </a-button>
      </div>
      <div v-if="poResult" class="step-result">
        <a-result status="success" title="PO生成成功">
          <template #subTitle>
            <span>{{ poResult.documentNo }}，Excel文件已自动下载</span>
          </template>
          <template #extra>
            <a-space>
              <a-button size="small" @click="handleBatchExportPo">
                <DownloadOutlined /> 重新下载
              </a-button>
              <a-button type="primary" @click="currentStep = 2">
                继续生成DN
              </a-button>
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
          基于已生成的 {{ poResults.length }} 份PO，为每份PO单独设置DN日期锚点，默认已按规则自动填入（PO日期+3工作日）。
        </template>
      </a-alert>

      <!-- 每份PO对应一行，独立设置锚点 -->
      <a-table
        :dataSource="dnAnchorRows"
        :columns="dnAnchorColumns"
        :pagination="false"
        size="small"
        bordered
        style="margin-bottom: 16px"
        rowKey="poId"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'anchorDate'">
            <a-date-picker
              v-model:value="record.anchorDate"
              style="width: 160px"
              :disabledDate="(d: Dayjs) => d.isBefore(dayjs(record.poDate), 'day')"
              @change="() => validateAnchor(record)"
            />
            <span v-if="record.error" style="color: #ff4d4f; margin-left: 8px; font-size: 12px">
              {{ record.error }}
            </span>
          </template>
          <template v-else-if="column.key === 'autoFill'">
            <a-space>
              <a-button size="small" @click="setAnchorPlus3(record)">+3工作日</a-button>
              <a-button size="small" @click="setAnchorNextTuesday(record)">下周二</a-button>
            </a-space>
          </template>
        </template>
      </a-table>

      <div class="step-actions">
        <a-button
          type="primary"
          size="large"
          :loading="dnLoading"
          :disabled="!canGenerateDn"
          @click="handleGenerateDn"
        >
          <DownloadOutlined /> 生成全部DN并下载ZIP
        </a-button>
      </div>

      <div v-if="dnResults.length > 0" class="step-result">
        <a-result status="success" :title="`DN生成成功，共 ${dnResults.length} 份`">
          <template #extra>
            <a-space>
              <a-button size="small" @click="handleBatchExportDn">
                <DownloadOutlined /> 重新下载
              </a-button>
              <a-button type="primary" disabled>✅ 全部完成</a-button>
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
import { DownloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import {
  generatePo,
  generateDn,
  exportPo,
  exportDn,
  batchExportPoZip,
  batchExportDnZip
} from '@/api/document'
import { getFbaShipmentList } from '@/api/fbaShipment'
import type { FbaShipment } from '@/types/fbaShipment'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { Marketplace } from '@/types/marketplace'

// ==================== 步骤控制 ====================
const currentStep = ref(0)
const stepCompleted = ref<boolean[]>([false, false, false])

/** 货件最大选择数量 */
const MAX_SHIPMENT_COUNT = 50

function goToStep(step: number) {
  currentStep.value = step
}

function stepDesc(index: number) {
  return stepCompleted.value[index] ? '已完成' : '待处理'
}

function stepStatus(index: number) {
  if (stepCompleted.value[index]) return 'finish'
  if (index === currentStep.value) return 'process'
  return 'wait'
}

// ==================== Step 0: 选择条件 ====================

// 站点选项（动态从 t_marketplace 接口获取）
const siteOptions = ref<{ label: string; value: string }[]>([])

async function fetchSiteOptions() {
  try {
    const res = await getEnabledMarketplaces() as any
    const list: Marketplace[] = res?.data ?? res ?? []
    siteOptions.value = list.map(m => ({ label: `${m.siteCode} - ${m.siteName}`, value: m.siteCode }))
  } catch {
    siteOptions.value = []
  }
}

const selectedSite = ref<string | undefined>(undefined)

// 季度选项：从当前季度往过去推3年（共13个季度）
const quarterOptions = computed(() => {
  const now = new Date()
  const currentYear = now.getFullYear()
  const currentQuarter = Math.ceil((now.getMonth() + 1) / 3)
  const options = []
  for (let offset = 0; offset <= 12; offset++) {
    let q = currentQuarter - offset
    let y = currentYear
    while (q < 1) { q += 4; y-- }
    const startMonth = (q - 1) * 3 + 1
    const endMonth = q * 3
    const lastDay = new Date(y, endMonth, 0).getDate()
    const start = `${y}-${String(startMonth).padStart(2, '0')}-01`
    const end = `${y}-${String(endMonth).padStart(2, '0')}-${lastDay}`
    options.push({
      label: `${y}年 Q${q}（${start} ~ ${end}）`,
      value: `${start}|${end}`
    })
  }
  return options
})

const selectedQuarter = ref<string | undefined>(undefined)
const periodStart = computed(() => selectedQuarter.value?.split('|')[0] || '')
const periodEnd = computed(() => selectedQuarter.value?.split('|')[1] || '')
const quarterLabel = computed(() => {
  if (!selectedQuarter.value) return ''
  const opt = quarterOptions.value.find(o => o.value === selectedQuarter.value)
  return opt?.label || ''
})

// FBA货件
const shipmentLoading = ref(false)
const shipmentList = ref<FbaShipment[]>([])
const selectedShipmentIds = ref<number[]>([])

async function handleSiteChange() {
  selectedShipmentIds.value = []
  shipmentList.value = []
  if (!selectedSite.value || !selectedQuarter.value) return
  await loadShipments()
}

function handleQuarterChange() {
  poResult.value = null
  dnResults.value = []
  dnAnchorRows.value = []
  stepCompleted.value = [false, false, false]
  selectedShipmentIds.value = []
  shipmentList.value = []
  if (selectedSite.value && selectedQuarter.value) {
    loadShipments()
  }
}

async function loadShipments() {
  if (!selectedSite.value || !selectedQuarter.value) return
  shipmentLoading.value = true
  try {
    const res = await getFbaShipmentList({
      page: 1,
      size: 200,
      siteCode: selectedSite.value,
      startDate: periodStart.value,
      endDate: periodEnd.value
    })
    shipmentList.value = res.data?.records || []
  } catch (error: any) {
    message.error(error?.message || '加载货件失败')
  } finally {
    shipmentLoading.value = false
  }
}

// 已选货件摘要（供 Step 2 展示）
const selectedShipments = computed(() => {
  return shipmentList.value.filter(s => selectedShipmentIds.value.includes(s.id))
})

const shipmentSummaryColumns = [
  { title: '货件编号', dataIndex: 'shipmentId', width: 180 },
  { title: '货件名称', dataIndex: 'shipmentName', ellipsis: true },
  { title: '创建日期', dataIndex: 'createdDate', width: 120 },
  { title: 'SKU数', dataIndex: 'skuCount', width: 80, align: 'center' as const },
  { title: '总数量', dataIndex: 'totalQuantity', width: 90, align: 'center' as const }
]

const canProceedToGenerate = computed(() => {
  return selectedSite.value
    && selectedQuarter.value
    && selectedShipmentIds.value.length > 0
    && selectedShipmentIds.value.length <= MAX_SHIPMENT_COUNT
})

function proceedToGeneratePo() {
  stepCompleted.value[0] = true
  currentStep.value = 1
}

// ==================== Step 1: 生成PO ====================
const poLoading = ref(false)
const poResult = ref<any>(null)
const poResults = ref<any[]>([])  // 多站点时可能有多个PO

async function handleGeneratePo() {
  poLoading.value = true
  try {
    const res = await generatePo({ shipmentIds: selectedShipmentIds.value, siteCode: selectedSite.value })
    // 兼容单个或数组返回
    const data = res.data
    if (Array.isArray(data)) {
      poResults.value = data
      poResult.value = data[0]
    } else {
      poResult.value = data
      poResults.value = data ? [data] : []
    }
    stepCompleted.value[1] = true

    // 初始化每份PO对应的DN锚点行，默认 PO日期+3工作日 与 下周二 取较晚者
    dnAnchorRows.value = poResults.value.map((po: any) => ({
      poId: po.id,
      poDocumentNo: po.documentNo,
      poDate: po.poDate,
      shipmentCount: po.shipmentCount,
      totalQuantity: po.totalQuantity,
      anchorDate: defaultAnchor(po.poDate),
      error: ''
    }))
    dnResults.value = []

    if (poResults.value.length === 1) {
      // 单个直接下载 Excel
      await exportPo(poResults.value[0].id)
    } else if (poResults.value.length > 1) {
      // 多个打 ZIP
      await batchExportPoZip(poResults.value.map((p: any) => p.id))
    }
    message.success('PO生成成功')
  } catch (error: any) {
    message.error(error?.message || 'PO生成失败')
  } finally {
    poLoading.value = false
  }
}

async function handleExportPo(id: number) {
  try {
    await exportPo(id)
    message.success('下载成功')
  } catch (error) {
    message.error('下载失败')
  }
}

async function handleBatchExportPo() {
  if (poResults.value.length === 0) return
  try {
    if (poResults.value.length === 1) {
      await exportPo(poResults.value[0].id)
    } else {
      await batchExportPoZip(poResults.value.map((p: any) => p.id))
    }
    message.success('下载成功')
  } catch (error) {
    message.error('下载失败')
  }
}

// ==================== Step 2: 生成DN ====================
const dnLoading = ref(false)
const dnResults = ref<any[]>([])

/** 每份PO对应的锚点行数据 */
interface DnAnchorRow {
  poId: number
  poDocumentNo: string
  poDate: string
  shipmentCount: number
  totalQuantity: number
  anchorDate: Dayjs | null
  error: string
}
const dnAnchorRows = ref<DnAnchorRow[]>([])

const dnAnchorColumns = [
  { title: 'PO编号', dataIndex: 'poDocumentNo', key: 'poDocumentNo', width: 200 },
  { title: 'PO日期', dataIndex: 'poDate', key: 'poDate', width: 110 },
  { title: '货件数', dataIndex: 'shipmentCount', key: 'shipmentCount', width: 80, align: 'center' as const },
  { title: '总数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'center' as const },
  { title: 'DN日期（锚点）', key: 'anchorDate', width: 260 },
  { title: '快速填入', key: 'autoFill', width: 180 }
]

/** 计算 PO日期 + n 个工作日（跳过周末） */
function addWorkingDays(date: Dayjs, n: number): Dayjs {
  let d = date
  let count = 0
  while (count < n) {
    d = d.add(1, 'day')
    const dow = d.day() // 0=Sun, 6=Sat
    if (dow !== 0 && dow !== 6) count++
  }
  return d
}

/** 计算下周二 */
function nextTuesday(date: Dayjs): Dayjs {
  let d = date.add(1, 'day')
  while (d.day() !== 2) d = d.add(1, 'day') // 2 = Tuesday
  return d
}

/** 默认锚点：PO日期+3工作日 与 下周二 取较晚者 */
function defaultAnchor(poDate: string): Dayjs {
  const base = dayjs(poDate)
  const plus3 = addWorkingDays(base, 3)
  const tue = nextTuesday(base)
  return plus3.isAfter(tue) ? plus3 : tue
}

function setAnchorPlus3(row: DnAnchorRow) {
  row.anchorDate = addWorkingDays(dayjs(row.poDate), 3)
  row.error = ''
}

function setAnchorNextTuesday(row: DnAnchorRow) {
  row.anchorDate = nextTuesday(dayjs(row.poDate))
  row.error = ''
}

function validateAnchor(row: DnAnchorRow) {
  if (!row.anchorDate) {
    row.error = '请选择DN日期'
    return false
  }
  if (row.anchorDate.isBefore(dayjs(row.poDate), 'day')) {
    row.error = `不能早于PO日期 ${row.poDate}`
    return false
  }
  row.error = ''
  return true
}

const canGenerateDn = computed(() => {
  if (dnAnchorRows.value.length === 0) return false
  return dnAnchorRows.value.every(r => r.anchorDate && !r.error)
})

async function handleGenerateDn() {
  // 校验所有行
  let hasError = false
  for (const row of dnAnchorRows.value) {
    if (!validateAnchor(row)) hasError = true
  }
  if (hasError) {
    message.error('请检查DN日期设置，存在错误项')
    return
  }

  dnLoading.value = true
  try {
    const allDns: any[] = []
    // 按每份PO的锚点分别调用生成接口，传 poId 让后端自动提取对应货件
    for (const row of dnAnchorRows.value) {
      const res = await generateDn({
        siteCode: selectedSite.value,
        anchorDate: row.anchorDate!.format('YYYY-MM-DD'),
        poId: row.poId,
        shipmentIds: []
      })
      const data = res.data
      if (Array.isArray(data)) allDns.push(...data)
      else if (data) allDns.push(data)
    }
    dnResults.value = allDns
    stepCompleted.value[2] = true

    if (allDns.length === 1) {
      await exportDn(allDns[0].id)
    } else if (allDns.length > 1) {
      await batchExportDnZip(allDns.map((d: any) => d.id))
    }
    message.success(`DN生成成功，共 ${allDns.length} 份`)
  } catch (error: any) {
    message.error(error?.message || 'DN生成失败')
  } finally {
    dnLoading.value = false
  }
}

async function handleBatchExportDn() {
  if (dnResults.value.length === 0) return
  try {
    if (dnResults.value.length === 1) {
      await exportDn(dnResults.value[0].id)
    } else {
      await batchExportDnZip(dnResults.value.map((d: any) => d.id))
    }
    message.success('下载成功')
  } catch (error) {
    message.error('下载失败')
  }
}

onMounted(() => {
  fetchSiteOptions()
})
</script>

<style lang="scss" scoped>
.document-generate-page {
  padding: 24px;

  .page-header {
    margin-bottom: 24px;
    .page-title {
      font-size: 20px;
      font-weight: 600;
      margin: 0 0 4px 0;
    }
    .page-desc {
      font-size: 14px;
      color: #666;
      margin: 0;
    }
  }

  .generate-steps {
    margin-bottom: 24px;
  }

  .step-content-card {
    margin-bottom: 16px;
  }

  .step-actions {
    margin: 16px 0;
  }

  .step-result {
    margin-top: 16px;
  }

  .shipment-item {
    padding: 6px 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .shipment-id {
      font-weight: 500;
      margin-right: 8px;
    }

    .shipment-info {
      color: #888;
      font-size: 12px;
    }
  }
}
</style>
