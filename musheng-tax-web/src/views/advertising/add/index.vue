<template>
  <div class="advertising-add-page">
    <div class="page-header">
      <h1 class="page-title">广告费录入</h1>
      <p class="page-desc">录入各站点的广告费用数据，用于VAT报表计算。支持批量导入，<a @click="goToImport">前往批量导入</a></p>
    </div>

    <a-card class="form-card">
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 12 }"
      >
        <a-form-item label="店铺名称" name="storeName">
          <a-input
            v-model:value="formData.storeName"
            placeholder="如：慕声欧洲-UK"
            :maxlength="100"
          />
        </a-form-item>

        <a-form-item label="站点" name="siteCode">
          <a-select
            v-model:value="formData.siteCode"
            placeholder="请选择站点"
            show-search
            :filter-option="filterOption"
            @change="handleSiteChange"
          >
            <a-select-option
              v-for="m in marketplaceOptions"
              :key="m.siteCode"
              :value="m.siteCode"
            >
              {{ m.siteCode }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="月份" name="yearMonth">
          <a-month-picker
            v-model:value="formData.yearMonthDate"
            placeholder="请选择月份"
            style="width: 100%"
            @change="handleYearMonthChange"
          />
        </a-form-item>

        <a-form-item label="广告费用" name="amount">
          <a-input-number
            v-model:value="formData.amount"
            :min="0"
            :precision="2"
            :step="100"
            placeholder="请输入广告费用"
            style="width: 100%"
          >
            <template #addonAfter>
              <span>{{ formData.currencyCode || '货币' }}</span>
            </template>
          </a-input-number>
        </a-form-item>

        <a-form-item label="货币" name="currencyCode">
          <a-select
            v-model:value="formData.currencyCode"
            placeholder="请选择货币（仅支持 USD/CAD/GBP/EUR）"
            style="width: 100%"
          >
            <a-select-option
              v-for="c in validCurrencyOptions"
              :key="c.currencyCode"
              :value="c.currencyCode"
            >
              {{ c.currencyCode }} - {{ c.currencyName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="发票号" name="invoiceNo">
          <a-input
            v-model:value="formData.invoiceNo"
            placeholder="选填，不填则自动生成"
            :maxlength="100"
          />
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="formData.remark"
            placeholder="选填"
            :rows="3"
            :maxlength="500"
            show-count
          />
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 4, span: 12 }">
          <a-space>
            <a-button type="primary" :loading="submitting" @click="handleSubmit">
              <SaveOutlined /> 保存
            </a-button>
            <a-button :loading="submitting" @click="handleSubmitAndContinue">
              <PlusOutlined /> 保存并继续添加
            </a-button>
            <a-button @click="handleReset">
              <ReloadOutlined /> 重置
            </a-button>
            <a-button @click="handleGoList">
              返回列表
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card title="最近录入记录" class="recent-card">
      <a-table
        :columns="recentColumns"
        :data-source="recentData"
        :loading="recentLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'siteCode'">
            <a-tag color="blue">{{ record.siteCode }}</a-tag>
          </template>
          <template v-else-if="column.key === 'totalCost'">
            <span class="amount">{{ record.currency }} {{ (record.totalCost ?? 0).toFixed(2) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm
              title="确定要删除该记录吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDeleteRecent(record)"
            >
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import { SaveOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { importAdvertisingData, deleteAdvertising, searchAdvertisingData } from '@/api/advertising'
import { getEnabledMarketplaces } from '@/api/marketplace'
import { getEnabledCurrencies } from '@/api/currency'
import type { AdvertisingBill } from '@/types/advertising'
import type { Marketplace } from '@/types/marketplace'
import type { Currency } from '@/types/currency'

const router = useRouter()

const marketplaceOptions = ref<Marketplace[]>([])
const currencyOptions = ref<Currency[]>([])
const formRef = ref<FormInstance>()
const submitting = ref(false)

const formData = reactive({
  storeName: '',
  siteCode: '',
  yearMonth: '',
  yearMonthDate: null as Dayjs | null,
  amount: undefined as number | undefined,
  currencyCode: '',
  invoiceNo: '',
  remark: ''
})

const VALID_CURRENCIES = ['USD', 'CAD', 'GBP', 'EUR']

const formRules = {
  storeName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  siteCode: [{ required: true, message: '请选择站点', trigger: 'change' }],
  yearMonth: [{ required: true, message: '请选择月份', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入广告费用', trigger: 'blur' },
    { type: 'number' as const, min: 0.01, message: '广告费用必须大于 0', trigger: 'blur' }
  ],
  currencyCode: [
    { required: true, message: '请选择货币', trigger: 'change' },
    { validator: (_: any, v: string) => (!v || VALID_CURRENCIES.includes(v) ? Promise.resolve() : Promise.reject('仅支持 USD/CAD/GBP/EUR')), trigger: 'change' }
  ]
}

const recentLoading = ref(false)
const recentData = ref<AdvertisingBill[]>([])

// 仅展示后端支持的币种（USD/CAD/GBP/EUR）
const validCurrencyOptions = computed(() =>
  currencyOptions.value.filter(c => VALID_CURRENCIES.includes(c.currencyCode))
)

const recentColumns = [
  { title: '站点', dataIndex: 'siteCode', key: 'siteCode', width: 80 },
  { title: '店铺名称', dataIndex: 'storeName', key: 'storeName', width: 120 },
  { title: '发票号', dataIndex: 'invoiceNumber', key: 'invoiceNumber', width: 150 },
  { title: '费用', dataIndex: 'totalCost', key: 'totalCost', width: 120, align: 'right' as const },
  { title: '录入时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 80 }
]

function filterOption(input: string, option: any) {
  const m = marketplaceOptions.value.find(x => x.siteCode === option.value)
  if (!m) return false
  const s = input.toLowerCase()
  return m.siteCode.toLowerCase().includes(s)
}

function handleSiteChange(siteCode: string) {
  const m = marketplaceOptions.value.find(x => x.siteCode === siteCode)
  if (m?.currencyCode && VALID_CURRENCIES.includes(m.currencyCode)) {
    formData.currencyCode = m.currencyCode
  }
  if (!formData.storeName) formData.storeName = `慕声-${siteCode}`
}

function handleYearMonthChange(date: Dayjs | null) {
  formData.yearMonth = date?.format('YYYY-MM') || ''
}

function buildImportRequest() {
  const start = formData.yearMonth ? `${formData.yearMonth}-01` : dayjs().format('YYYY-MM-DD')
  const end = formData.yearMonth
    ? dayjs(`${formData.yearMonth}-01`).endOf('month').format('YYYY-MM-DD')
    : dayjs().format('YYYY-MM-DD')
  const invoiceNo = formData.invoiceNo?.trim() || `MANUAL-${Date.now()}`
  const amount = Math.max(0, formData.amount ?? 0)
  return {
    storeName: formData.storeName.trim(),
    siteCode: formData.siteCode || undefined,
    invoiceNumber: invoiceNo,
    invoiceStatus: 'PAID_IN_FULL',
    billingStartDate: start,
    billingEndDate: end,
    issueDate: end,
    currency: formData.currencyCode || 'USD',
    invoiceAmount: amount >= 0.01 ? amount : 0.01,
    cost: amount,
    otherCost: 0,
    remark: formData.remark?.trim() || undefined
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitting.value = true
    const item = buildImportRequest()
    const res = await importAdvertisingData({ data: [item] }) as any
    const result = res?.data ?? res
    const failed = result?.failedCount ?? 0
    if (failed > 0) {
      const msg = result?.failedRecords?.[0]?.errorMessage || '导入失败'
      message.error(msg)
      return
    }
    message.success('录入成功')
    router.push('/advertising/list')
  } catch (e: any) {
    if (e?.errorFields) return
    message.error('录入失败: ' + (e?.message || e))
  } finally {
    submitting.value = false
  }
}

async function handleSubmitAndContinue() {
  try {
    await formRef.value?.validate()
    submitting.value = true
    const item = buildImportRequest()
    const res = await importAdvertisingData({ data: [item] }) as any
    const result = res?.data ?? res
    const failed = result?.failedCount ?? 0
    if (failed > 0) {
      const msg = result?.failedRecords?.[0]?.errorMessage || '导入失败'
      message.error(msg)
      return
    }
    message.success('录入成功，可继续添加')
    formData.yearMonth = ''
    formData.yearMonthDate = null
    formData.amount = undefined
    formData.invoiceNo = ''
    formData.remark = ''
    fetchRecentData()
  } catch (e: any) {
    if (e?.errorFields) return
    message.error('录入失败: ' + (e?.message || e))
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
  Object.assign(formData, {
    storeName: '',
    siteCode: '',
    yearMonth: '',
    yearMonthDate: null,
    amount: undefined,
    currencyCode: '',
    invoiceNo: '',
    remark: ''
  })
}

function handleGoList() {
  router.push('/advertising/list')
}

function goToImport() {
  router.push({ name: 'AdvertisingImport' })
}

async function handleDeleteRecent(record: AdvertisingBill) {
  try {
    await deleteAdvertising(record.id)
    message.success('删除成功')
    fetchRecentData()
  } catch (e) {
    message.error('删除失败')
  }
}

async function fetchMarketplaces() {
  try {
    const res = await getEnabledMarketplaces() as any
    marketplaceOptions.value = res?.data ?? res ?? []
  } catch {
    marketplaceOptions.value = []
  }
}

async function fetchCurrencies() {
  try {
    const res = await getEnabledCurrencies() as any
    currencyOptions.value = res?.data ?? res ?? []
  } catch {
    currencyOptions.value = []
  }
}

async function fetchRecentData() {
  recentLoading.value = true
  try {
    const res = await searchAdvertisingData({ current: 1, size: 5 }) as any
    recentData.value = res?.data?.records ?? res?.records ?? []
  } catch {
    recentData.value = []
  } finally {
    recentLoading.value = false
  }
}

onMounted(() => {
  fetchMarketplaces()
  fetchCurrencies()
  fetchRecentData()
})
</script>

<style lang="scss" scoped>
.advertising-add-page {
  padding: $spacing-lg;

  .page-header {
    margin-bottom: $spacing-lg;

    .page-title {
      font-size: $font-size-xl;
      font-weight: 600;
      color: $text-color;
      margin: 0 0 $spacing-xs 0;
    }

    .page-desc {
      font-size: $font-size-md;
      color: $text-color-secondary;
      margin: 0;

      a {
        color: $primary-color;
        cursor: pointer;
      }
    }
  }

  .form-card {
    margin-bottom: $spacing-lg;
  }

  .recent-card .amount {
    font-weight: 500;
    color: $primary-color;
  }
}
</style>
