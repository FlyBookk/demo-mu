<template>
  <div class="settlement-derivation-page">
    <div class="page-header">
      <h1 class="page-title">结算推导</h1>
      <p class="page-desc">选择站点和季度，输入采购成本，自动推导结算明细数据</p>
    </div>

    <!-- 步骤一：选择站点 -->
    <a-card title="① 选择站点" class="step-card">
      <a-select
        v-model:value="selectedSiteCode"
        placeholder="请选择站点"
        :loading="sitesLoading"
        style="width: 200px"
        @change="handleSiteChange"
      >
        <a-select-option
          v-for="m in marketplaceOptions"
          :key="m.siteCode"
          :value="m.siteCode"
        >
          {{ m.siteCode }} - {{ m.siteName }}
        </a-select-option>
      </a-select>
    </a-card>

    <!-- 步骤二：选择季度 -->
    <a-card title="② 选择季度" style="margin-top: 16px" class="step-card">
      <a-select
        v-model:value="selectedQuarter"
        placeholder="请选择季度"
        style="width: 200px"
        @change="handleQuarterChange"
      >
        <a-select-option v-for="q in availableQuarters" :key="q" :value="q">
          {{ formatQuarter(q) }}
        </a-select-option>
      </a-select>
    </a-card>

    <!-- 步骤三：输入采购成本 -->
    <a-card title="③ 输入采购成本（人民币）" style="margin-top: 16px" class="step-card">
      <a-form layout="inline">
        <a-form-item :label="`${selectedSiteCode || '—'} 站点采购成本`">
          <a-input-number
            v-model:value="costCny"
            :min="0"
            :precision="2"
            :disabled="!selectedSiteCode"
            placeholder="请输入采购成本"
            style="width: 200px"
          />
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 步骤四：推导计算 -->
    <a-card title="④ 推导计算" style="margin-top: 16px" class="step-card">
      <a-button
        type="primary"
        :loading="deriving"
        :disabled="!canDerive"
        @click="handleDerive"
      >
        开始推导（自动覆盖旧数据）
      </a-button>

      <!-- 推导结果表格 -->
      <template v-if="derivationResult.length > 0">
        <a-divider />
        <a-alert
          type="success"
          :message="resultSummary"
          show-icon
          style="margin-bottom: 16px"
        />
        <a-table
          :columns="resultColumns"
          :data-source="derivationResult"
          :pagination="false"
          size="small"
          row-key="msku"
          bordered
        />
      </template>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { triggerDerivation } from '@/api/settlementDerivation'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { Marketplace } from '@/types/marketplace'

// ==================== 站点选择 ====================
const marketplaceOptions = ref<Marketplace[]>([])
const sitesLoading = ref(false)
const selectedSiteCode = ref<string | undefined>(undefined)

async function fetchMarketplaces() {
  sitesLoading.value = true
  try {
    const res = await getEnabledMarketplaces()
    marketplaceOptions.value = res.data || []
  } catch (error) {
    console.error('获取站点列表失败:', error)
  } finally {
    sitesLoading.value = false
  }
}

function handleSiteChange() {
  // 切换站点时清空推导结果
  derivationResult.value = []
  resultSummary.value = ''
}

function handleQuarterChange() {
  // 切换季度时清空推导结果
  derivationResult.value = []
  resultSummary.value = ''
}

// ==================== 季度选择 ====================
const availableQuarters = ref<string[]>([])
const selectedQuarter = ref('')

/** 格式化季度显示，如 2025-Q3 → 2025年Q3 */
function formatQuarter(quarter: string): string {
  const [year, q] = quarter.split('-Q')
  return `${year}年Q${q}`
}

/** 生成从当前季度往过去推3年（共13个季度）的选项列表 */
function generateAvailableQuarters() {
  const now = new Date()
  const currentYear = now.getFullYear()
  const currentQuarter = Math.ceil((now.getMonth() + 1) / 3)

  const quarters: string[] = []
  for (let offset = 0; offset <= 12; offset++) {
    let q = currentQuarter - offset
    let y = currentYear
    while (q < 1) { q += 4; y-- }
    quarters.push(`${y}-Q${q}`)
  }

  availableQuarters.value = quarters
  // 默认选上一季度
  const prevQ = currentQuarter === 1 ? 4 : currentQuarter - 1
  const prevY = currentQuarter === 1 ? currentYear - 1 : currentYear
  selectedQuarter.value = `${prevY}-Q${prevQ}`
}

// ==================== 采购成本 ====================
const costCny = ref<number | null>(null)

// ==================== 推导计算 ====================
const deriving = ref(false)
const derivationResult = ref<any[]>([])
const resultSummary = ref('')

/** 是否可以点击推导 */
const canDerive = computed(() => {
  return selectedSiteCode.value && selectedQuarter.value && costCny.value && costCny.value > 0
})

const resultColumns = [
  { title: 'MSKU', dataIndex: 'msku', key: 'msku' },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', align: 'right' as const },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', align: 'right' as const },
  { title: '金额', dataIndex: 'amount', key: 'amount', align: 'right' as const }
]

/** 点击"开始推导" */
async function handleDerive() {
  if (!selectedSiteCode.value || !selectedQuarter.value) {
    message.warning('请先选择站点和季度')
    return
  }
  if (!costCny.value || costCny.value <= 0) {
    message.warning('请输入有效的采购成本')
    return
  }

  deriving.value = true
  derivationResult.value = []
  resultSummary.value = ''

  try {
    const res = await triggerDerivation({
      startQuarter: selectedQuarter.value,
      endQuarter: selectedQuarter.value,
      siteCosts: [{ siteCode: selectedSiteCode.value, costCny: costCny.value }]
    })

    const data = res.data
    // 展开 siteResults 为扁平行
    const flatRows: any[] = []
    if (data && data.siteResults) {
      for (const site of data.siteResults) {
        if (site.items) {
          for (const item of site.items) {
            flatRows.push({
              siteCode: site.siteCode,
              currency: site.currency,
              msku: item.msku,
              quantity: item.quantity,
              unitPrice: item.unitPrice,
              amount: item.amount
            })
          }
        }
      }
    }
    derivationResult.value = flatRows

    if (flatRows.length > 0) {
      resultSummary.value = `推导完成并已写入，共 ${flatRows.length} 条 MSKU 明细（旧数据已自动覆盖）`
      message.success(resultSummary.value)
    } else {
      message.warning('该季度范围内无可推导数据，请检查销售数据')
    }
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '推导失败'
    message.error(msg)
    console.error('推导失败:', error)
  } finally {
    deriving.value = false
  }
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await fetchMarketplaces()
  generateAvailableQuarters()
})
</script>

<style lang="scss" scoped>
.settlement-derivation-page {
  padding: 24px;

  .page-header {
    margin-bottom: 24px;

    .page-title {
      font-size: 20px;
      font-weight: 600;
      color: #333;
      margin: 0 0 8px 0;
    }

    .page-desc {
      font-size: 14px;
      color: #999;
      margin: 0;
    }
  }

  .step-card {
    :deep(.ant-card-head-title) {
      font-weight: 600;
    }
  }
}
</style>
