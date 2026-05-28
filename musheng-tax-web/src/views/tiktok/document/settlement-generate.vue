<template>
  <div class="settlement-generate-page">
    <div class="page-header">
      <h1 class="page-title">结算单/INV 生成</h1>
      <p class="page-desc">根据结算推导数据，按站点生成结算单和INV发票</p>
    </div>

    <!-- 步骤条 -->
    <a-steps :current="currentStep" class="generate-steps" @change="(s: number) => currentStep = s">
      <a-step title="选择条件" :description="stepCompleted[0] ? '已完成' : '待处理'" :status="stepStatus(0)" style="cursor: pointer" />
      <a-step title="生成结算单" :description="stepCompleted[1] ? '已完成' : '待处理'" :status="stepStatus(1)" style="cursor: pointer" />
      <a-step title="生成INV" :description="stepCompleted[2] ? '已完成' : '待处理'" :status="stepStatus(2)" style="cursor: pointer" />
    </a-steps>

    <!-- Step 0: 选择条件 -->
    <a-card v-show="currentStep === 0" class="step-content-card">
      <template #title>第一步：选择站点和结算季度</template>
      <a-alert type="info" show-icon style="margin-bottom: 16px">
        <template #message>
          请确保已在「结算推导」中完成对应季度的推导，系统将根据推导数据生成结算单。
        </template>
      </a-alert>

      <a-form layout="vertical" style="max-width: 500px">
        <a-form-item label="站点" required>
          <a-select v-model:value="selectedSiteCode" placeholder="请选择站点" style="width: 100%" :loading="sitesLoading" @change="handleChange">
            <a-select-option v-for="m in marketplaceOptions" :key="m.siteCode" :value="m.siteCode">
              {{ m.siteCode }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="结算季度" required>
          <a-select v-model:value="selectedQuarter" placeholder="请选择季度" style="width: 100%" @change="handleChange">
            <a-select-option v-for="q in quarterOptions" :key="q.value" :value="q.value">{{ q.label }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>

      <!-- 校验结果 -->
      <div v-if="checkResult === 'checking'" style="margin: 16px 0; color: #1890ff">
        <a-spin size="small" /> 正在检查推导数据...
      </div>
      <a-alert v-else-if="checkResult === 'empty'" type="error" show-icon style="margin: 16px 0">
        <template #message>
          该季度（{{ quarterLabel }}）站点（{{ selectedSiteCode }}）暂无结算推导数据
        </template>
        <template #description>
          请先前往「结算推导」页面完成推导后再来生成单据。
          <br /><a-button type="link" style="padding: 0; margin-top: 8px" @click="$router.push('/tiktok/document/derivation')">→ 前往结算推导</a-button>
        </template>
      </a-alert>
      <a-alert v-else-if="checkResult === 'ok'" type="success" show-icon style="margin: 16px 0">
        <template #message>
          已找到 {{ preCheckCount }} 份结算单数据，可以继续
        </template>
      </a-alert>

      <div class="step-actions">
        <a-button type="primary" size="large" :disabled="checkResult !== 'ok'" @click="stepCompleted[0] = true; currentStep = 1">
          下一步：生成结算单
        </a-button>
      </div>
    </a-card>

    <!-- Step 1: 生成结算单 -->
    <a-card v-show="currentStep === 1" class="step-content-card">
      <template #title>第二步：生成结算单</template>
      <a-alert type="info" show-icon style="margin-bottom: 16px">
        <template #message>
          站点: {{ selectedSiteCode }} | 季度: {{ quarterLabel }}
          <br />系统将按月拆分生成结算单（每月一份），结算日为下月5日（非工作日顺延）
        </template>
      </a-alert>

      <div class="step-actions">
        <a-button type="primary" size="large" :loading="settlementLoading" :disabled="settlementResult.length > 0" @click="handleGenerateSettlements">
          生成结算单并下载
        </a-button>
        <a-button style="margin-left: 12px" @click="currentStep = 0">返回上一步</a-button>
      </div>

      <div v-if="settlementResult.length > 0" class="step-result">
        <a-result status="success" :title="`结算单生成成功，共 ${settlementResult.length} 份（按月拆分）`">
          <template #subTitle>
            <div v-for="s in settlementResult" :key="s.id" style="margin: 4px 0">
              {{ s.documentNo }} | 结算日: {{ s.settlementDate }} | 周期: {{ s.periodStart }}~{{ s.periodEnd }} | {{ s.totalQuantity }}件 | ${{ fmt(s.totalAmount) }}
            </div>
          </template>
          <template #extra>
            <a-space>
              <a-button @click="handleRedownloadSettlement">重新下载</a-button>
              <a-button type="primary" @click="currentStep = 2">继续生成INV</a-button>
            </a-space>
          </template>
        </a-result>
      </div>
    </a-card>

    <!-- Step 2: 生成INV -->
    <a-card v-show="currentStep === 2" class="step-content-card">
      <template #title>第三步：生成INV发票</template>
      <a-alert v-if="settlementResult.length === 0" type="warning" show-icon>
        <template #message>请先生成结算单</template>
      </a-alert>
      <template v-else>
        <a-alert type="info" show-icon style="margin-bottom: 16px">
          <template #message>
            INV已随结算单同步生成（{{ settlementResult.length }}份），点击下方按钮下载。
          </template>
        </a-alert>

        <div class="step-actions">
          <a-button type="primary" size="large" :loading="invLoading" @click="handleDownloadInvoices">
            下载全部INV
          </a-button>
          <a-button style="margin-left: 12px" @click="currentStep = 1">返回上一步</a-button>
        </div>

        <div v-if="invResult.length > 0" class="step-result">
          <a-result status="success" :title="`INV共 ${invResult.length} 份，已下载`">
            <template #subTitle>
              <div v-for="inv in invResult" :key="inv.id" style="margin: 4px 0">
                {{ inv.documentNo }} | {{ inv.documentDate }}
              </div>
            </template>
            <template #extra>
              <a-space>
                <a-button @click="handleDownloadInvoices">重新下载</a-button>
                <a-button type="primary" @click="$router.push('/tiktok/document/list')">查看单据列表</a-button>
              </a-space>
            </template>
          </a-result>
        </div>
      </template>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { exportTiktokSettlement, exportTiktokInv, batchExportSettlement, batchExportInv, getTiktokDocumentList, getInvBySettlements } from '@/api/tiktok'
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
const selectedSiteCode = ref<string | undefined>(undefined)
const selectedQuarter = ref<string | undefined>(undefined)

const quarterOptions = computed(() => {
  const options: { label: string; value: string }[] = []
  const now = new Date()
  let y = now.getFullYear(), q = Math.ceil((now.getMonth() + 1) / 3)
  for (let i = 0; i <= 12; i++) {
    const sm = (q - 1) * 3 + 1, em = q * 3
    const ld = new Date(y, em, 0).getDate()
    const start = `${y}-${String(sm).padStart(2, '0')}-01`
    const end = `${y}-${String(em).padStart(2, '0')}-${ld}`
    options.push({ label: `${y}年 Q${q}（${start} ~ ${end}）`, value: `${y}-Q${q}` })
    q--; if (q < 1) { q = 4; y-- }
  }
  return options
})

const quarterLabel = computed(() => quarterOptions.value.find(o => o.value === selectedQuarter.value)?.label || '')
const canProceed = computed(() => selectedSiteCode.value && selectedQuarter.value)
const checkResult = ref<'idle' | 'checking' | 'ok' | 'empty'>('idle')
const preCheckCount = ref(0)

async function fetchMarketplaces() {
  sitesLoading.value = true
  try { const res = await getEnabledMarketplaces(); marketplaceOptions.value = res.data || [] }
  finally {
    sitesLoading.value = false
    if (!selectedSiteCode.value && marketplaceOptions.value.length > 0) {
      selectedSiteCode.value = marketplaceOptions.value.find(m => m.siteCode === 'US')?.siteCode || marketplaceOptions.value[0].siteCode
    }
  }
}

async function handleChange() {
  settlementResult.value = []; invResult.value = []; stepCompleted.value = [false, false, false]
  checkResult.value = 'idle'
  preCheckCount.value = 0
  if (!selectedSiteCode.value || !selectedQuarter.value) return
  // 自动校验是否有推导数据
  checkResult.value = 'checking'
  try {
    const [yearStr, qStr] = (selectedQuarter.value || '').split('-Q')
    const qNum = parseInt(qStr)
    const sm = (qNum - 1) * 3 + 1, em = qNum * 3
    const startDate = `${yearStr}-${String(sm).padStart(2, '0')}-01`
    const ld = new Date(parseInt(yearStr), em, 0).getDate()
    const endDate = `${yearStr}-${String(em).padStart(2, '0')}-${ld}`
    const res: any = await getTiktokDocumentList({ documentType: 'SETTLEMENT', siteCode: selectedSiteCode.value, startDate, endDate, pageNum: 1, pageSize: 1 })
    const total = (res.data || res).total || 0
    if (total > 0) {
      checkResult.value = 'ok'
      preCheckCount.value = total
    } else {
      checkResult.value = 'empty'
    }
  } catch {
    checkResult.value = 'empty'
  }
}

// ==================== Step 1: 结算单 ====================
const settlementLoading = ref(false)
const settlementResult = ref<any[]>([])

async function handleGenerateSettlements() {
  if (!canProceed.value) return
  settlementLoading.value = true
  try {
    // 查询该季度已有的结算单
    const [yearStr, qStr] = (selectedQuarter.value || '').split('-Q')
    const qNum = parseInt(qStr)
    const sm = (qNum - 1) * 3 + 1, em = qNum * 3
    const startDate = `${yearStr}-${String(sm).padStart(2, '0')}-01`
    const ld = new Date(parseInt(yearStr), em, 0).getDate()
    const endDate = `${yearStr}-${String(em).padStart(2, '0')}-${ld}`

    const res: any = await getTiktokDocumentList({ documentType: 'SETTLEMENT', siteCode: selectedSiteCode.value, startDate, endDate, pageNum: 1, pageSize: 20 })
    const records = (res.data || res).records || []
    if (records.length === 0) {
      message.warning('该季度暂无结算单数据，请先在「结算推导」中完成推导')
      return
    }
    settlementResult.value = records
    stepCompleted.value[1] = true
    await downloadSettlements()
    message.success(`结算单共 ${records.length} 份，已下载`)
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '查询失败')
  } finally { settlementLoading.value = false }
}

async function downloadSettlements() {
  const ids = settlementResult.value.map(s => s.id)
  if (ids.length > 1) await batchExportSettlement(ids)
  else if (ids.length === 1) await exportTiktokSettlement(ids[0])
}

async function handleRedownloadSettlement() {
  try { await downloadSettlements(); message.success('下载成功') } catch { message.error('下载失败') }
}

// ==================== Step 2: INV ====================
const invLoading = ref(false)
const invResult = ref<any[]>([])

async function handleDownloadInvoices() {
  invLoading.value = true
  try {
    const settlementIds = settlementResult.value.map((s: any) => s.id)
    const res: any = await getInvBySettlements(settlementIds)
    const invs = res.data || res || []
    invResult.value = invs
    stepCompleted.value[2] = true

    const ids = invs.map((r: any) => r.id)
    if (ids.length > 1) await batchExportInv(ids)
    else if (ids.length === 1) await exportTiktokInv(ids[0])
    message.success(`INV下载成功，共 ${ids.length} 份`)
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '下载失败')
  } finally { invLoading.value = false }
}

function fmt(val: any) { return val != null ? Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '-' }

onMounted(fetchMarketplaces)
</script>

<style lang="scss" scoped>
.settlement-generate-page {
  padding: 24px;
  .page-header { margin-bottom: 24px;
    .page-title { font-size: 20px; font-weight: 600; margin: 0 0 4px 0; }
    .page-desc { font-size: 14px; color: #666; margin: 0; }
  }
  .generate-steps { margin-bottom: 24px; }
  .step-content-card { margin-bottom: 16px; }
  .step-actions { margin: 16px 0; }
  .step-result { margin-top: 16px; }
}
</style>
