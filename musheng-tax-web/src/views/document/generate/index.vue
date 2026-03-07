<template>
  <div class="document-generate-page">
    <div class="page-header">
      <h1 class="page-title">单据生成</h1>
      <p class="page-desc">按步骤生成FBA相关单据：选站点 → 选季度 → 选货件 → PO → DN → 结算单/INV</p>
    </div>

    <!-- 步骤条 -->
    <a-steps :current="currentStep" class="generate-steps" @change="goToStep">
      <a-step title="选择条件" :description="stepDesc(0)" :status="stepStatus(0)" style="cursor: pointer" />
      <a-step title="生成PO" :description="stepDesc(1)" :status="stepStatus(1)" style="cursor: pointer" />
      <a-step title="生成DN" :description="stepDesc(2)" :status="stepStatus(2)" style="cursor: pointer" />
      <a-step title="生成结算单/INV" :description="stepDesc(3)" :status="stepStatus(3)" style="cursor: pointer" />
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
          <a-spin :spinning="shipmentLoading">
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
              <a-button size="small" @click="handleExportPo(poResult.id)">
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
          基于已生成的PO，生成对应的DN送货单。请参考下方货件日期，选择合适的DN日期锚点。
        </template>
      </a-alert>

      <!-- 已选货件摘要 -->
      <a-table
        :dataSource="selectedShipments"
        :columns="shipmentSummaryColumns"
        :pagination="false"
        size="small"
        bordered
        style="margin-bottom: 16px"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'createdDate'">
            {{ record.createdDate ? dayjs(record.createdDate).format('YYYY-MM-DD') : '-' }}
          </template>
        </template>
      </a-table>

      <a-form layout="vertical" style="max-width: 400px">
        <a-form-item label="DN日期（锚点）" required>
          <a-date-picker
            v-model:value="dnDate"
            style="width: 100%"
            :disabledDate="dnDisabledDate"
            :defaultPickerValue="dnDefaultPickerValue"
          />
        </a-form-item>
      </a-form>
      <div class="step-actions">
        <a-button
          type="primary"
          size="large"
          :loading="dnLoading"
          :disabled="!dnDate"
          @click="handleGenerateDn"
        >
          <DownloadOutlined /> 生成DN并下载Excel
        </a-button>
      </div>
      <div v-if="dnResult" class="step-result">
        <a-result status="success" title="DN生成成功">
          <template #subTitle>
            <span>{{ dnResult.documentNo }}，Excel文件已自动下载</span>
          </template>
          <template #extra>
            <a-space>
              <a-button size="small" @click="handleExportDn(dnResult.id)">
                <DownloadOutlined /> 重新下载
              </a-button>
              <a-button type="primary" @click="currentStep = 3">
                继续生成结算单/INV
              </a-button>
            </a-space>
          </template>
        </a-result>
      </div>
    </a-card>

    <!-- Step 3: 生成结算单 + INV -->
    <a-card v-show="currentStep === 3" class="step-content-card">
      <template #title>第四步：生成结算单和INV发票</template>
      <a-alert type="info" show-icon style="margin-bottom: 16px">
        <template #message>
          系统根据结算推导数据，按站点自动拆分生成结算单，然后自动生成对应的INV发票。
          请确保已在"结算推导"中完成对应季度的推导。
        </template>
      </a-alert>
      <a-form layout="vertical" style="max-width: 500px">
        <a-form-item label="结算周期">
          <a-select
            v-model:value="selectedQuarter"
            style="width: 100%"
            :options="quarterOptions"
            placeholder="请选择结算季度"
            @change="handleSettlementQuarterChange"
          />
        </a-form-item>
        <a-form-item v-if="periodStart && periodEnd">
          <a-descriptions :column="2" size="small" bordered>
            <a-descriptions-item label="周期开始">{{ periodStart }}</a-descriptions-item>
            <a-descriptions-item label="周期结束">{{ periodEnd }}</a-descriptions-item>
          </a-descriptions>
        </a-form-item>
      </a-form>

      <!-- 生成结算单 -->
      <div class="step-actions">
        <a-button
          type="primary"
          size="large"
          :loading="settlementLoading"
          :disabled="settlementResult.length > 0"
          @click="handleGenerateSettlements"
        >
          <DownloadOutlined /> 生成结算单并下载
        </a-button>
      </div>

      <div v-if="settlementResult.length > 0" class="step-result">
        <a-result status="success" :title="`结算单生成成功，共 ${settlementResult.filter(s => s.totalQuantity > 0).length} 份有数据`">
          <template #subTitle>
            <span>Excel文件已自动下载（仅下载有数据的站点）</span>
          </template>
          <template #extra>
            <div>
              <div class="download-list">
                <a-button
                  v-for="s in settlementResult.filter(s => s.totalQuantity > 0)"
                  :key="s.id"
                  size="small"
                  @click="handleExportSettlement(s.id)"
                >
                  <DownloadOutlined /> {{ s.documentNo }} ({{ s.siteCode }})
                </a-button>
              </div>
              <!-- 生成INV -->
              <a-divider />
              <a-button
                type="primary"
                size="large"
                :loading="invLoading"
                :disabled="invResult.length > 0"
                @click="handleGenerateInvoices"
              >
                <DownloadOutlined /> 生成INV并下载
              </a-button>
            </div>
          </template>
        </a-result>
      </div>

      <div v-if="invResult.length > 0" class="step-result">
        <a-result status="success" :title="`INV生成成功，共 ${invResult.length} 份`">
          <template #subTitle>
            <span>Excel文件已自动下载</span>
          </template>
          <template #extra>
            <div class="download-list">
              <a-button
                v-for="inv in invResult"
                :key="inv.id"
                size="small"
                @click="handleExportInv(inv.id)"
              >
                <DownloadOutlined /> {{ inv.documentNo }}
              </a-button>
            </div>
          </template>
        </a-result>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import {
  generatePo,
  generateDn,
  generateSettlements,
  generateInvoices,
  exportPo,
  exportDn,
  exportSettlement,
  exportInv
} from '@/api/document'
import { getFbaShipmentList } from '@/api/fbaShipment'
import type { FbaShipment } from '@/types/fbaShipment'

// ==================== 步骤控制 ====================
const currentStep = ref(0)
const stepCompleted = ref<boolean[]>([false, false, false, false])

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

// 站点选项
const siteOptions = [
  { label: 'US - 美国', value: 'US' },
  { label: 'CA - 加拿大', value: 'CA' },
  { label: 'UK - 英国', value: 'UK' },
  { label: 'DE - 德国', value: 'DE' }
]

const selectedSite = ref<string | undefined>(undefined)

// 季度选项
const currentYear = new Date().getFullYear()
const quarterOptions = computed(() => {
  const options = []
  for (let y = currentYear; y >= currentYear - 1; y--) {
    for (let q = 4; q >= 1; q--) {
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
  // 站点和季度都选了才加载货件
  if (!selectedSite.value || !selectedQuarter.value) return
  await loadShipments()
}

function handleQuarterChange() {
  // 季度变更时重置后续步骤结果
  poResult.value = null
  dnResult.value = null
  settlementResult.value = []
  invResult.value = []
  stepCompleted.value = [false, false, false, false]
  // 季度选好后，如果站点也已选，则加载货件
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
    // 按季度时间范围过滤货件（shop_id 已通过请求头隔离）
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
  return selectedSite.value && selectedQuarter.value && selectedShipmentIds.value.length > 0
})

function proceedToGeneratePo() {
  stepCompleted.value[0] = true
  currentStep.value = 1
}

// ==================== Step 1: 生成PO ====================
const poLoading = ref(false)
const poResult = ref<any>(null)

async function handleGeneratePo() {
  poLoading.value = true
  try {
    const res = await generatePo({ shipmentIds: selectedShipmentIds.value })
    poResult.value = res.data
    stepCompleted.value[1] = true
    if (poResult.value?.id) {
      await exportPo(poResult.value.id)
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

// ==================== Step 2: 生成DN ====================
const dnLoading = ref(false)
const dnDate = ref<Dayjs | null>(null)
const dnResult = ref<any>(null)

// 已选货件最早创建日期（DN日期不能早于此日期）
const earliestShipmentDate = computed(() => {
  const dates = selectedShipments.value
    .map(s => s.createdDate)
    .filter(Boolean)
    .map(d => dayjs(d))
  if (dates.length === 0) return null
  return dates.reduce((min, d) => d.isBefore(min) ? d : min
  )
})

// DN日期禁用规则：
// 1. 若PO已生成，不能选早于PO日期的日期
// 2. 否则不能选早于最早货件创建日期的日期
function dnDisabledDate(current: Dayjs) {
  if (poResult.value?.poDate) {
    return current.isBefore(dayjs(poResult.value.poDate), 'day')
  }
  if (!earliestShipmentDate.value) return false
  return current.isBefore(earliestShipmentDate.value, 'day')
}

// 日历默认定位到PO日期（若已生成），否则定位到最早货件日期附近
const dnDefaultPickerValue = computed(() => {
  if (poResult.value?.poDate) return dayjs(poResult.value.poDate)
  return earliestShipmentDate.value || dayjs()
})

async function handleGenerateDn() {
  if (!dnDate.value) return
  dnLoading.value = true
  try {
    const res = await generateDn({
      anchorDate: dnDate.value.format('YYYY-MM-DD'),
      shipmentIds: selectedShipmentIds.value
    })
    dnResult.value = res.data
    stepCompleted.value[2] = true
    if (dnResult.value?.id) {
      await exportDn(dnResult.value.id)
    }
    message.success('DN生成成功')
  } catch (error: any) {
    message.error(error?.message || 'DN生成失败')
  } finally {
    dnLoading.value = false
  }
}

async function handleExportDn(id: number) {
  try {
    await exportDn(id)
    message.success('下载成功')
  } catch (error) {
    message.error('下载失败')
  }
}

// ==================== Step 3: 生成结算单 + INV ====================
const settlementLoading = ref(false)
const settlementResult = ref<any[]>([])
const invLoading = ref(false)
const invResult = ref<any[]>([])

// 切换结算季度时重置结算单和INV结果
function handleSettlementQuarterChange() {
  settlementResult.value = []
  invResult.value = []
}

async function handleGenerateSettlements() {
  if (!selectedQuarter.value) return
  settlementLoading.value = true
  try {
    const res = await generateSettlements({
      periodStart: periodStart.value,
      periodEnd: periodEnd.value,
      shipmentIds: selectedShipmentIds.value
    })
    settlementResult.value = res.data || []
    // 自动下载有数据的结算单
    for (const s of settlementResult.value) {
      if (s.totalQuantity > 0 && s.id) {
        await exportSettlement(s.id)
      }
    }
    message.success('结算单生成成功')
  } catch (error: any) {
    message.error(error?.message || '结算单生成失败')
  } finally {
    settlementLoading.value = false
  }
}

async function handleExportSettlement(id: number) {
  try {
    await exportSettlement(id)
    message.success('下载成功')
  } catch (error) {
    message.error('下载失败')
  }
}

async function handleGenerateInvoices() {
  const ids = settlementResult.value
    .filter(s => s.totalQuantity > 0)
    .map(s => s.id)
  if (ids.length === 0) return
  invLoading.value = true
  try {
    const res = await generateInvoices(ids)
    invResult.value = res.data || []
    stepCompleted.value[3] = true
    // 自动下载INV
    for (const inv of invResult.value) {
      if (inv.id) {
        await exportInv(inv.id)
      }
    }
    message.success('INV生成成功')
  } catch (error: any) {
    message.error(error?.message || 'INV生成失败')
  } finally {
    invLoading.value = false
  }
}

async function handleExportInv(id: number) {
  try {
    await exportInv(id)
    message.success('下载成功')
  } catch (error) {
    message.error('下载失败')
  }
}
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

  .download-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
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
