<template>
  <div class="settlement-generate-page">
    <div class="page-header">
      <h1 class="page-title">结算单/INV 生成</h1>
      <p class="page-desc">根据结算推导数据，按站点生成结算单和INV发票</p>
    </div>

    <!-- 结算单生成 -->
    <a-card class="section-card">
      <template #title>结算单生成</template>
      <a-alert type="info" show-icon style="margin-bottom: 16px">
        <template #message>
          请确保已在「结算推导」中完成对应季度的推导，系统将根据推导数据生成选中站点的结算单。
        </template>
      </a-alert>

      <a-form layout="vertical" style="max-width: 500px">
        <a-form-item label="结算季度" required>
          <a-select
            v-model:value="selectedQuarter"
            style="width: 100%"
            :options="quarterOptions"
            placeholder="请选择结算季度"
            @change="handleQuarterChange"
          />
        </a-form-item>
        <a-form-item v-if="periodStart" label="结算周期">
          <span style="color: #888">{{ periodStart }} ~ {{ periodEnd }}</span>
        </a-form-item>
        <a-form-item label="站点" required>
          <a-spin :spinning="sitesLoading">
            <a-checkbox-group v-model:value="selectedSiteCodes" style="width: 100%">
              <a-row :gutter="16">
                <a-col v-for="m in marketplaceOptions" :key="m.siteCode" :span="6">
                  <a-checkbox :value="m.siteCode">{{ m.siteCode }} - {{ m.siteName }}</a-checkbox>
                </a-col>
              </a-row>
            </a-checkbox-group>
          </a-spin>
        </a-form-item>
      </a-form>

      <div style="margin: 16px 0">
        <a-button
          type="primary"
          :loading="settlementLoading"
          :disabled="!canGenerate || settlementResult.length > 0"
          @click="handleGenerateSettlements"
        >
          生成结算单并下载
        </a-button>
      </div>

      <div v-if="settlementResult.length > 0" style="margin-top: 16px">
        <a-result
          status="success"
          :title="`结算单生成成功，共 ${settlementResult.filter(s => s.totalQuantity > 0).length} 份有数据`"
          sub-title="Excel文件已自动下载（仅下载有数据的站点）"
        >
          <template #extra>
            <a-space wrap>
              <a-button
                v-for="s in settlementResult.filter(s => s.totalQuantity > 0)"
                :key="s.id"
                size="small"
                @click="handleExportSettlement(s.id)"
              >
                <DownloadOutlined /> {{ s.documentNo }} ({{ s.siteCode }})
              </a-button>
            </a-space>
          </template>
        </a-result>
      </div>
    </a-card>

    <!-- INV 发票生成 -->
    <a-card class="section-card">
      <template #title>INV 发票生成</template>
      <a-alert v-if="settlementResult.length === 0" type="warning" show-icon>
        <template #message>请先生成结算单</template>
      </a-alert>
      <template v-else>
        <div>
          <a-button
            type="primary"
            :loading="invLoading"
            :disabled="invResult.length > 0"
            @click="handleGenerateInvoices"
          >
            生成INV并下载
          </a-button>
        </div>

        <div v-if="invResult.length > 0" style="margin-top: 16px">
          <a-result status="success" :title="`INV生成成功，共 ${invResult.length} 份`" sub-title="Excel文件已自动下载">
            <template #extra>
              <a-space wrap>
                <a-button
                  v-for="inv in invResult"
                  :key="inv.id"
                  size="small"
                  @click="handleExportInv(inv.id)"
                >
                  <DownloadOutlined /> {{ inv.documentNo }}
                </a-button>
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
import { DownloadOutlined } from '@ant-design/icons-vue'
import {
  generateSettlements,
  generateInvoices,
  exportSettlement,
  exportInv
} from '@/api/document'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { Marketplace } from '@/types/marketplace'

// ==================== 站点 ====================
const marketplaceOptions = ref<Marketplace[]>([])
const sitesLoading = ref(false)
const selectedSiteCodes = ref<string[]>([])

async function fetchMarketplaces() {
  sitesLoading.value = true
  try {
    const res = await getEnabledMarketplaces()
    marketplaceOptions.value = res.data || []
  } catch {
    marketplaceOptions.value = []
  } finally {
    sitesLoading.value = false
  }
}

// ==================== 季度选择 ====================
const selectedQuarter = ref<string>()

const quarterOptions = computed(() => {
  const options: { label: string; value: string }[] = []
  const now = new Date()
  const currentYear = now.getFullYear()
  const currentQuarter = Math.ceil((now.getMonth() + 1) / 3)
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

const periodStart = computed(() => selectedQuarter.value?.split('|')[0] || '')
const periodEnd = computed(() => selectedQuarter.value?.split('|')[1] || '')

const canGenerate = computed(() => selectedQuarter.value && selectedSiteCodes.value.length > 0)

function handleQuarterChange() {
  settlementResult.value = []
  invResult.value = []
}

// ==================== 结算单 ====================
const settlementLoading = ref(false)
const settlementResult = ref<any[]>([])

async function handleGenerateSettlements() {
  if (!canGenerate.value) return
  settlementLoading.value = true
  try {
    const res = await generateSettlements({
      periodStart: periodStart.value,
      periodEnd: periodEnd.value,
      siteCodes: selectedSiteCodes.value
    })
    settlementResult.value = res.data || []
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
  } catch { message.error('下载失败') }
}

// ==================== INV ====================
const invLoading = ref(false)
const invResult = ref<any[]>([])

async function handleGenerateInvoices() {
  const ids = settlementResult.value.filter(s => s.totalQuantity > 0).map(s => s.id)
  if (ids.length === 0) return
  invLoading.value = true
  try {
    const res = await generateInvoices(ids)
    invResult.value = res.data || []
    for (const inv of invResult.value) {
      if (inv.id) await exportInv(inv.id)
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
  } catch { message.error('下载失败') }
}

onMounted(() => {
  fetchMarketplaces()
})
</script>

<style lang="scss" scoped>
.settlement-generate-page {
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

  .section-card {
    margin-bottom: 16px;
  }
}
</style>
