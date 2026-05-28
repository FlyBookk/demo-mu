<template>
  <div class="tax-summary-page">
    <div class="page-header">
      <h1 class="page-title">报税汇总</h1>
      <p class="page-desc">对齐报税汇总口径，统计收入、退款、佣金服务费等核心字段</p>
    </div>

    <!-- 筛选条件 -->
    <a-card class="filter-card">
      <a-form layout="inline">
        <a-form-item label="站点">
          <a-select v-model:value="siteCode" placeholder="选择站点" style="width: 140px">
            <a-select-option v-for="s in sites" :key="s.siteCode" :value="s.siteCode">{{ s.siteCode }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="季度">
          <a-select v-model:value="quarter" style="width: 150px">
            <a-select-option v-for="q in quarterOptions" :key="q" :value="q">{{ formatQ(q) }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="汇率">
          <a-input-number v-model:value="exchangeRate" :precision="4" placeholder="USD→RMB" style="width: 120px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="loadData" :loading="loading" :disabled="!siteCode">查询</a-button>
          <a-button style="margin-left: 8px" @click="handleExport" :disabled="!taxSummary">导出</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <template v-if="taxSummary && opSummary">
      <!-- TK报税汇总卡片 -->
      <div class="stat-section">
        <div class="stat-section-label">TK报税汇总 <span class="rate-badge">执行汇率：{{ exchangeRate }}</span></div>
        <a-row :gutter="16" class="stat-row">
          <a-col :span="6">
            <a-card class="stat-card">
              <div class="stat-title">收入总额①(a)</div>
              <div class="stat-val green">${{ fmt(taxSummary.totalRevenueUsd) }}</div>
              <div class="stat-rmb">¥{{ fmt(stats.revenue) }}</div>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <div class="stat-title">退款金额②(b)</div>
              <div class="stat-val red">${{ fmt(taxSummary.totalRefundUsd) }}</div>
              <div class="stat-rmb">¥{{ fmt(stats.refund) }}</div>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <div class="stat-title">收入净额③(c=a-b)</div>
              <div class="stat-val green">${{ fmt(Number(taxSummary.totalRevenueUsd) - Number(taxSummary.totalRefundUsd)) }}</div>
              <div class="stat-rmb">¥{{ fmt(stats.netRevenue) }}</div>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <div class="stat-title">佣金服务费⑤</div>
              <div class="stat-val red">${{ fmt(taxSummary.totalServiceFeeUsd) }}</div>
              <div class="stat-rmb">¥{{ fmt(stats.serviceFee) }}</div>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <div class="stat-title">联盟费⑤b</div>
              <div class="stat-val red">${{ fmt(taxSummary.totalAffiliateFeeUsd) }}</div>
              <div class="stat-rmb">¥{{ fmt(stats.affiliateFee) }}</div>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <div class="stat-title">营销费⑤c</div>
              <div class="stat-val red">${{ fmt(taxSummary.totalPromotionFeeUsd) }}</div>
              <div class="stat-rmb">¥{{ fmt(stats.promotion) }}</div>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <!-- 辅助字段 -->
      <div class="stat-section">
        <a-row :gutter="16" class="stat-row">
          <a-col :span="6">
            <a-card class="stat-card">
              <div class="stat-title">其他费用⑦</div>
              <div class="stat-val red">${{ fmt(0) }}</div>
              <div class="stat-rmb">¥{{ fmt(0) }}</div>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <div class="stat-title">平台代扣税④</div>
              <div class="stat-val purple">${{ fmt(0) }}</div>
              <div class="stat-rmb">¥{{ fmt(0) }}</div>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <div class="stat-title">广告费⑥</div>
              <div class="stat-val cyan">${{ fmt(0) }}</div>
              <div class="stat-rmb">¥{{ fmt(0) }}</div>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card class="stat-card">
              <div class="stat-title">订单数</div>
              <div class="stat-val blue">{{ stats.orderCount.toLocaleString() }} <span style="font-size:14px">笔</span></div>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <!-- 平台支出与采购成本 -->
      <div class="stat-section">
        <div class="stat-section-label">
          平台支出与采购成本
          <span class="profit-setting">
            利润设置：
            <a-radio-group v-model:value="profitMode" size="small" style="margin: 0 8px">
              <a-radio-button value="percent">百分比</a-radio-button>
              <a-radio-button value="fixed">固定金额</a-radio-button>
            </a-radio-group>
            <a-input-number v-if="profitMode === 'percent'" v-model:value="profitPercent" :min="0" :max="100" :precision="1" size="small" style="width: 80px" />
            <span v-if="profitMode === 'percent'" style="margin-left: 4px">%</span>
            <span v-if="profitMode === 'fixed'" style="margin-right: 4px">¥</span>
            <a-input-number v-if="profitMode === 'fixed'" v-model:value="profitFixed" :min="0" :precision="2" size="small" style="width: 120px" />
          </span>
        </div>
        <a-row :gutter="16" class="stat-row">
          <a-col :span="8">
            <a-card class="stat-card highlight-card">
              <div class="stat-title">平台支出合计⑨=④+⑤+⑤b+⑤c+⑥+⑦</div><!-- ⑥广告费暂为0，待后续补充 -->
              <div class="stat-val orange">${{ fmt(stats.platformExpensesUsd) }}</div>
              <div class="stat-desc">¥{{ fmt(stats.platformExpenses) }}</div>
            </a-card>
          </a-col>
          <a-col :span="8">
            <a-card class="stat-card highlight-card">
              <div class="stat-title">利润⑩（{{ profitMode === 'percent' ? `净额×${profitPercent}%` : '固定金额' }}）</div>
              <div class="stat-val green">${{ fmt(stats.profitUsd) }}</div>
              <div class="stat-desc">¥{{ fmt(stats.profit) }}</div>
            </a-card>
          </a-col>
          <a-col :span="8">
            <a-card class="stat-card highlight-card">
              <div class="stat-title">采购成本⑪（③−⑨−⑩）</div>
              <div class="stat-val blue">${{ fmt(stats.procurementCostUsd) }}</div>
              <div class="stat-desc">¥{{ fmt(stats.procurementCost) }}</div>
            </a-card>
          </a-col>
        </a-row>
      </div>

      <!-- 月度明细表格 -->
      <a-card title="报税汇总数据（按月）" style="margin-top: 16px">
        <a-table :columns="taxColumns" :data-source="taxMonthData" :pagination="false" size="middle" bordered>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'revenue'">
              <div class="cell-dual"><span class="val-usd">${{ fmt(record.revenueUsd) }}</span><span class="val-rmb">¥{{ fmt(record.revenueRmb) }}</span></div>
            </template>
            <template v-if="column.key === 'refund'">
              <div class="cell-dual"><span class="val-usd">${{ fmt(record.refundUsd) }}</span><span class="val-rmb">¥{{ fmt(record.refundRmb) }}</span></div>
            </template>
            <template v-if="column.key === 'serviceFee'">
              <div class="cell-dual"><span class="val-usd">${{ fmt(record.serviceFeeUsd) }}</span><span class="val-rmb">¥{{ fmt(record.serviceFeeRmb) }}</span></div>
            </template>
            <template v-if="column.key === 'affiliateFee'">
              <div class="cell-dual"><span class="val-usd">${{ fmt(record.affiliateFeeUsd) }}</span><span class="val-rmb">¥{{ fmt(record.affiliateFeeRmb) }}</span></div>
            </template>
            <template v-if="column.key === 'promotionFee'">
              <div class="cell-dual"><span class="val-usd">${{ fmt(record.promotionFeeUsd) }}</span><span class="val-rmb">¥{{ fmt(record.promotionFeeRmb) }}</span></div>
            </template>
          </template>
          <template #summary>
            <a-table-summary-row style="font-weight: 600; background: #fafafa">
              <a-table-summary-cell>合计</a-table-summary-cell>
              <a-table-summary-cell><div class="cell-dual"><span class="val-usd">${{ fmt(taxSummary.totalRevenueUsd) }}</span><span class="val-rmb">¥{{ fmt(taxSummary.totalRevenueRmb) }}</span></div></a-table-summary-cell>
              <a-table-summary-cell><div class="cell-dual"><span class="val-usd">${{ fmt(taxSummary.totalRefundUsd) }}</span><span class="val-rmb">¥{{ fmt(taxSummary.totalRefundRmb) }}</span></div></a-table-summary-cell>
              <a-table-summary-cell><div class="cell-dual"><span class="val-usd">${{ fmt(taxSummary.totalServiceFeeUsd) }}</span><span class="val-rmb">¥{{ fmt(taxSummary.totalServiceFeeRmb) }}</span></div></a-table-summary-cell>
              <a-table-summary-cell><div class="cell-dual"><span class="val-usd">${{ fmt(taxSummary.totalAffiliateFeeUsd) }}</span><span class="val-rmb">¥{{ fmt(taxSummary.totalAffiliateFeeRmb) }}</span></div></a-table-summary-cell>
              <a-table-summary-cell><div class="cell-dual"><span class="val-usd">${{ fmt(taxSummary.totalPromotionFeeUsd) }}</span><span class="val-rmb">¥{{ fmt(taxSummary.totalPromotionFeeRmb) }}</span></div></a-table-summary-cell>
              <a-table-summary-cell>{{ stats.orderCount }}</a-table-summary-cell>
            </a-table-summary-row>
          </template>
        </a-table>
      </a-card>

      <!-- 运营口径 -->
      <a-card title="运营口径" style="margin-top: 16px">
        <a-descriptions :column="4" bordered size="small">
          <a-descriptions-item label="净收入">
            <div>${{ fmt(opSummary.netRevenue) }}</div>
            <div class="rmb-sub">¥{{ fmt(Number(opSummary.netRevenue) * exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="佣金">
            <div>${{ fmt(opSummary.commission) }}</div>
            <div class="rmb-sub">¥{{ fmt(Number(opSummary.commission) * exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item>
            <template #label>物流 <a-tooltip title="已包含在报税口径'佣金服务费'中（MAX(Seller shipping, FBT)），此处仅展示不重复计入"><info-circle-outlined class="tip-icon" /></a-tooltip></template>
            <div>${{ fmt(opSummary.logistics) }}</div>
            <div class="rmb-sub">¥{{ fmt(Number(opSummary.logistics) * exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item>
            <template #label>退货运费 <a-tooltip title="已包含在报税口径'佣金服务费'中（Actual return shipping fee），此处仅展示不重复计入"><info-circle-outlined class="tip-icon" /></a-tooltip></template>
            <div>${{ fmt(opSummary.returnShipping) }}</div>
            <div class="rmb-sub">¥{{ fmt(Number(opSummary.returnShipping) * exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="联盟">
            <div>${{ fmt(opSummary.affiliate) }}</div>
            <div class="rmb-sub">¥{{ fmt(Number(opSummary.affiliate) * exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="促销">
            <div>${{ fmt(opSummary.promotion) }}</div>
            <div class="rmb-sub">¥{{ fmt(Number(opSummary.promotion) * exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item>
            <template #label>商品折扣 <a-tooltip title="收入端(subtotal)已是折后价，此费用已从收入中扣除，此处仅展示不重复计入"><info-circle-outlined class="tip-icon" /></a-tooltip></template>
            <div>${{ fmt(opSummary.sellerDiscount) }}</div>
            <div class="rmb-sub">¥{{ fmt(Number(opSummary.sellerDiscount) * exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="订单利润">
            <div><strong>${{ fmt(opSummary.orderProfit) }}</strong></div>
            <div class="rmb-sub">¥{{ fmt(Number(opSummary.orderProfit) * exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="调整收入">
            <div>${{ fmt(opSummary.adjustmentIncome) }}</div>
            <div class="rmb-sub">¥{{ fmt(Number(opSummary.adjustmentIncome) * exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="调整支出">
            <div>${{ fmt(opSummary.adjustmentExpense) }}</div>
            <div class="rmb-sub">¥{{ fmt(Number(opSummary.adjustmentExpense) * exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="净利润">
            <div><strong style="color:#52c41a">${{ fmt(opSummary.netProfit) }}</strong></div>
            <div class="rmb-sub" style="color:#52c41a">¥{{ fmt(Number(opSummary.netProfit) * exchangeRate) }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="利润率"><strong>{{ ((opSummary.marginRate || 0) * 100).toFixed(2) }}%</strong></a-descriptions-item>
        </a-descriptions>
      </a-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { InfoCircleOutlined } from '@ant-design/icons-vue'
import { getTiktokTaxSummary, getTiktokOperationSummary, type QuarterTaxSummary, type QuarterOperationSummary } from '@/api/tiktok'
import { useTiktokSites } from '@/composables/tiktok/useTiktokSites'

const { sites, currentSite } = useTiktokSites()
const quarter = ref('')
const siteCode = ref('')
const exchangeRate = ref<number | null>(null)

const loading = ref(false)
const taxSummary = ref<QuarterTaxSummary | null>(null)
const opSummary = ref<QuarterOperationSummary | null>(null)

watch(currentSite, (val) => { if (val && !siteCode.value) { siteCode.value = val } })
watch([siteCode, quarter], () => { exchangeRate.value = null })

const quarterOptions = (() => {
  const opts: string[] = []
  const now = new Date()
  let y = now.getFullYear(), q = Math.ceil((now.getMonth() + 1) / 3)
  for (let i = 0; i < 8; i++) { opts.push(`${y}-Q${q}`); q--; if (q === 0) { q = 4; y-- } }
  return opts
})()

function formatQ(q: string) { const [y, qn] = q.split('-Q'); return `${y}年Q${qn}` }

const profitMode = ref<'percent' | 'fixed'>('percent')
const profitPercent = ref<number>(4)
const profitFixed = ref<number>(0)



// 计算汇总统计（原币 + RMB）
const stats = computed(() => {
  if (!taxSummary.value || !opSummary.value) return { revenue: 0, refund: 0, netRevenue: 0, serviceFee: 0, affiliateFee: 0, otherFee: 0, tax: 0, promotion: 0, orderCount: 0, platformExpenses: 0, platformExpensesUsd: 0, profit: 0, profitUsd: 0, procurementCost: 0, procurementCostUsd: 0 }
  const rate = exchangeRate.value || 1
  const revenueUsd = Number(taxSummary.value.totalRevenueUsd)
  const refundUsd = Number(taxSummary.value.totalRefundUsd)
  const revenue = revenueUsd * rate
  const refund = refundUsd * rate
  const netRevenue = revenue - refund
  const netRevenueUsd = revenueUsd - refundUsd
  const serviceFee = Number(taxSummary.value.totalServiceFeeUsd) * rate
  const serviceFeeUsd = Number(taxSummary.value.totalServiceFeeUsd)
  const affiliateFee = Number(taxSummary.value.totalAffiliateFeeUsd || 0) * rate
  const affiliateFeeUsd = Number(taxSummary.value.totalAffiliateFeeUsd || 0)
  const tax = 0
  const taxUsd = 0
  const promotion = Number(taxSummary.value.totalPromotionFeeUsd || 0) * rate
  const promotionUsd = Number(taxSummary.value.totalPromotionFeeUsd || 0)
  const otherFee = 0
  const otherFeeUsd = 0
  const orderCount = taxSummary.value.months?.reduce((s: number, m: any) => s + m.orderCount, 0) || 0
  const platformExpenses = tax + serviceFee + affiliateFee + promotion + otherFee
  const platformExpensesUsd = taxUsd + serviceFeeUsd + affiliateFeeUsd + promotionUsd + otherFeeUsd
  // 利润：百分比或固定金额
  const profit = profitMode.value === 'percent' ? netRevenue * (profitPercent.value || 0) / 100 : (profitFixed.value || 0)
  const profitUsd = profitMode.value === 'percent' ? netRevenueUsd * (profitPercent.value || 0) / 100 : (rate > 0 ? (profitFixed.value || 0) / rate : 0)
  // 采购成本 = 净额 - 平台支出 - 利润
  const procurementCost = netRevenue - platformExpenses - profit
  const procurementCostUsd = netRevenueUsd - platformExpensesUsd - profitUsd
  return { revenue, refund, netRevenue, serviceFee, affiliateFee, otherFee, tax, promotion, orderCount, platformExpenses, platformExpensesUsd, profit, profitUsd, procurementCost, procurementCostUsd }
})

const taxColumns = [
  { title: '月份', dataIndex: 'month', width: 100 },
  { title: '收入（USD / RMB）', key: 'revenue', width: 180 },
  { title: '退款（USD / RMB）', key: 'refund', width: 180 },
  { title: '服务费（USD / RMB）', key: 'serviceFee', width: 180 },
  { title: '联盟费（USD / RMB）', key: 'affiliateFee', width: 180 },
  { title: '营销费（USD / RMB）', key: 'promotionFee', width: 180 },
  { title: '订单数', dataIndex: 'orderCount', width: 90 },
]
const taxMonthData = computed(() => taxSummary.value?.months || [])

function fmt(val: any): string { return val != null ? Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '-' }

function handleExport() {
  if (!taxSummary.value || !opSummary.value) return
  import('xlsx').then(XLSX => {
    const wb = XLSX.utils.book_new()
    const rate = exchangeRate.value || 1
    const s = stats.value

    // Sheet 1: 报税汇总
    const sheet1Data: any[] = [
      ['TK报税汇总', '', '', `执行汇率: ${rate}`],
      [],
      ['项目', '金额(USD)', '金额(RMB)'],
      ['收入总额①', taxSummary.value!.totalRevenueUsd, s.revenue],
      ['退款金额②', taxSummary.value!.totalRefundUsd, s.refund],
      ['收入净额③(=①-②)', (Number(taxSummary.value!.totalRevenueUsd) - Number(taxSummary.value!.totalRefundUsd)).toFixed(2), s.netRevenue],
      ['佣金服务费⑤', taxSummary.value!.totalServiceFeeUsd, s.serviceFee],
      ['联盟费⑤b', taxSummary.value!.totalAffiliateFeeUsd, s.affiliateFee],
      ['营销费⑤c', taxSummary.value!.totalPromotionFeeUsd, s.promotion],
      ['广告费⑥', 0, 0],
      ['其他费用⑦', 0, 0],
      ['平台代扣税④', 0, 0],
      [],
      ['平台支出与采购成本'],
      ['项目', '金额(USD)', '金额(RMB)'],
      ['平台支出合计⑨', s.platformExpensesUsd, s.platformExpenses],
      ['利润⑩', s.profitUsd, s.profit],
      ['采购成本⑪(③-⑨-⑩)', s.procurementCostUsd, s.procurementCost],
      [],
      ['订单数', s.orderCount],
    ]
    const ws1 = XLSX.utils.aoa_to_sheet(sheet1Data)
    ws1['!cols'] = [{ wch: 22 }, { wch: 16 }, { wch: 16 }]
    XLSX.utils.book_append_sheet(wb, ws1, '报税汇总')

    // Sheet 2: 月度明细
    const sheet2Data: any[] = [
      ['月份', '收入(USD)', '收入(RMB)', '退款(USD)', '退款(RMB)', '服务费(USD)', '服务费(RMB)', '联盟费(USD)', '联盟费(RMB)', '营销费(USD)', '营销费(RMB)', '订单数'],
    ]
    for (const m of (taxSummary.value!.months || [])) {
      sheet2Data.push([m.month, m.revenueUsd, m.revenueRmb, m.refundUsd, m.refundRmb, m.serviceFeeUsd, m.serviceFeeRmb, m.affiliateFeeUsd, m.affiliateFeeRmb, m.promotionFeeUsd, m.promotionFeeRmb, m.orderCount])
    }
    sheet2Data.push(['合计', taxSummary.value!.totalRevenueUsd, taxSummary.value!.totalRevenueRmb, taxSummary.value!.totalRefundUsd, taxSummary.value!.totalRefundRmb, taxSummary.value!.totalServiceFeeUsd, taxSummary.value!.totalServiceFeeRmb, taxSummary.value!.totalAffiliateFeeUsd, taxSummary.value!.totalAffiliateFeeRmb, taxSummary.value!.totalPromotionFeeUsd, taxSummary.value!.totalPromotionFeeRmb, s.orderCount])
    const ws2 = XLSX.utils.aoa_to_sheet(sheet2Data)
    ws2['!cols'] = [{ wch: 10 }, { wch: 14 }, { wch: 14 }, { wch: 14 }, { wch: 14 }, { wch: 14 }, { wch: 14 }, { wch: 14 }, { wch: 14 }, { wch: 14 }, { wch: 14 }, { wch: 8 }]
    XLSX.utils.book_append_sheet(wb, ws2, '月度明细')

    // Sheet 3: 运营口径
    const op = opSummary.value!
    const sheet3Data: any[] = [
      ['运营口径', '', '', `执行汇率: ${rate}`],
      [],
      ['项目', '金额(USD)', '金额(RMB)', '说明'],
      ['净收入', op.netRevenue, Number(op.netRevenue) * rate, ''],
      ['佣金', op.commission, Number(op.commission) * rate, 'Referral + Refund admin'],
      ['物流', op.logistics, Number(op.logistics) * rate, 'Seller shipping fee（已含在报税服务费中）'],
      ['退货运费', op.returnShipping, Number(op.returnShipping) * rate, '已含在报税服务费中'],
      ['联盟', op.affiliate, Number(op.affiliate) * rate, ''],
      ['促销', op.promotion, Number(op.promotion) * rate, ''],
      ['商品折扣', op.sellerDiscount, Number(op.sellerDiscount) * rate, '收入端已扣除，不重复计入'],
      [],
      ['订单利润', op.orderProfit, Number(op.orderProfit) * rate, ''],
      ['调整收入', op.adjustmentIncome, Number(op.adjustmentIncome) * rate, ''],
      ['调整支出', op.adjustmentExpense, Number(op.adjustmentExpense) * rate, ''],
      ['净利润', op.netProfit, Number(op.netProfit) * rate, ''],
      ['利润率', `${(Number(op.marginRate) * 100).toFixed(2)}%`, '', ''],
      [],
      ['Total Fees', op.totalFees, Number(op.totalFees) * rate, 'orderSettlement - netRevenue'],
    ]
    const ws3 = XLSX.utils.aoa_to_sheet(sheet3Data)
    ws3['!cols'] = [{ wch: 14 }, { wch: 16 }, { wch: 16 }, { wch: 36 }]
    XLSX.utils.book_append_sheet(wb, ws3, '运营口径')

    // 导出
    XLSX.writeFile(wb, `TK报税汇总_${siteCode.value}_${quarter.value}.xlsx`)
  })
}

async function loadData() {
  if (!quarter.value || !siteCode.value) return
  loading.value = true
  try {
    const params: any = { quarter: quarter.value, siteCode: siteCode.value }
    if (exchangeRate.value) params.exchangeRate = exchangeRate.value
    const [taxRes, opRes]: any[] = await Promise.all([getTiktokTaxSummary(params), getTiktokOperationSummary(params)])
    taxSummary.value = taxRes.data || taxRes
    opSummary.value = opRes.data || opRes
    // 用后端返回的实际执行汇率更新显示
    if (taxSummary.value?.exchangeRate) {
      exchangeRate.value = Number(taxSummary.value.exchangeRate)
    }
  } finally { loading.value = false }
}

onMounted(() => { quarter.value = quarterOptions.length > 1 ? quarterOptions[1] : quarterOptions[0] })
</script>

<style lang="scss" scoped>
.tax-summary-page {
  padding: 24px;
  .page-header { margin-bottom: 16px;
    .page-title { font-size: 20px; font-weight: 600; margin: 0 0 4px 0; }
    .page-desc { color: #666; font-size: 13px; margin: 0; }
  }
  .filter-card { margin-bottom: 16px; }
  .stat-section { margin-top: 16px;
    .stat-section-label { font-size: 14px; font-weight: 500; color: #666; margin-bottom: 8px; }
    .stat-row { margin-bottom: 0; }
  }
  .rate-badge { font-size: 13px; font-weight: normal; color: #1890ff; background: #e6f7ff; padding: 2px 10px; border-radius: 4px; margin-left: 12px; }

  .profit-setting { font-size: 13px; font-weight: normal; color: #666; margin-left: 16px; display: inline-flex; align-items: center; }
  .stat-card { text-align: center; padding: 16px 8px;
    .stat-title { font-size: 13px; color: #666; margin-bottom: 8px; }
    .stat-val { font-size: 22px; font-weight: 600; line-height: 1.4;
      &.green { color: #52c41a; }
      &.red { color: #ff4d4f; }
      &.blue { color: #1890ff; }
      &.purple { color: #722ed1; }
      &.cyan { color: #13c2c2; }
      &.orange { color: #fa8c16; }
    }
    .stat-rmb { font-size: 13px; color: #999; margin-top: 2px; }
    .stat-desc { font-size: 12px; color: #999; margin-top: 6px; }
  }
  .highlight-card { border: 1px solid #ffe58f; background: #fffbe6; }
  .cell-dual { display: flex; flex-direction: column; line-height: 1.6; }
  .val-usd { font-weight: 500; color: #333; }
  .val-rmb { font-size: 12px; color: #999; }
  .rmb-sub { font-size: 12px; color: #999; margin-top: 2px; }
  .tip-icon { color: #faad14; margin-left: 4px; cursor: help; }
}
</style>
