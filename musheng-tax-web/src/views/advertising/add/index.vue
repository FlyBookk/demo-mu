<template>
  <div class="advertising-add-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">广告费录入</h1>
      <p class="page-desc">录入各站点的广告费用数据，用于VAT报表计算</p>
    </div>

    <!-- 录入表单 -->
    <a-card class="form-card">
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 12 }"
      >
        <a-form-item label="站点" name="siteCode">
          <a-select
            v-model:value="formData.siteCode"
            placeholder="请选择站点"
            show-search
            :filter-option="filterOption"
            @change="handleSiteChange"
          >
            <a-select-option
              v-for="marketplace in marketplaceOptions"
              :key="marketplace.siteCode"
              :value="marketplace.siteCode"
            >
              {{ marketplace.siteCode }} - {{ marketplace.siteName }}
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
            placeholder="请选择货币"
            style="width: 100%"
          >
            <a-select-option
              v-for="currency in currencyOptions"
              :key="currency.currencyCode"
              :value="currency.currencyCode"
            >
              {{ currency.currencyCode }} - {{ currency.currencyName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="发票号" name="invoiceNo">
          <a-input
            v-model:value="formData.invoiceNo"
            placeholder="请输入发票号（可选）"
            :maxlength="100"
          />
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="formData.remark"
            placeholder="请输入备注（可选）"
            :rows="4"
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

    <!-- 最近录入记录 -->
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
          <template v-else-if="column.key === 'cost'">
            <span class="amount">{{ record.currency }} {{ record.cost?.toFixed(2) }}</span>
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import {
  SaveOutlined,
  PlusOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import { getAdvertisingList, createAdvertising, deleteAdvertising, searchAdvertisingData } from '@/api/advertising'
import { getEnabledMarketplaces } from '@/api/marketplace'
import { getEnabledCurrencies } from '@/api/currency'
import type { AdvertisingData, AdvertisingDataForm } from '@/types/advertising'
import type { Marketplace } from '@/types/marketplace'
import type { Currency } from '@/types/currency'

const router = useRouter()

// ============= 选项数据 =============
const marketplaceOptions = ref<Marketplace[]>([])
const currencyOptions = ref<Currency[]>([])

// ============= 表单相关 =============
const formRef = ref<FormInstance>()
const submitting = ref(false)

const formData = reactive<AdvertisingDataForm & { yearMonthDate: Dayjs | null }>({
  siteCode: '',
  yearMonth: '',
  yearMonthDate: null,
  amount: undefined as number | undefined,
  currencyCode: '',
  invoiceNo: '',
  remark: ''
})

const formRules = {
  siteCode: [{ required: true, message: '请选择站点', trigger: 'change' }],
  yearMonth: [{ required: true, message: '请选择月份', trigger: 'change' }],
  amount: [{ required: true, message: '请输入广告费用', trigger: 'blur' }],
  currencyCode: [{ required: true, message: '请选择货币', trigger: 'change' }]
}

// ============= 最近记录 =============
const recentLoading = ref(false)
const recentData = ref<AdvertisingData[]>([])

const recentColumns = [
  {
    title: '站点',
    dataIndex: 'siteCode',
    key: 'siteCode',
    width: 80
  },
  {
    title: '店铺名称',
    dataIndex: 'storeName',
    key: 'storeName',
    width: 120
  },
  {
    title: '发票号',
    dataIndex: 'invoiceNumber',
    key: 'invoiceNumber',
    width: 150
  },
  {
    title: '费用',
    dataIndex: 'cost',
    key: 'cost',
    width: 150,
    align: 'right' as const
  },
  {
    title: '录入时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 80
  }
]

// ============= 方法 =============
function filterOption(input: string, option: any): boolean {
  const marketplace = marketplaceOptions.value.find(m => m.siteCode === option.value)
  if (!marketplace) return false
  const searchText = input.toLowerCase()
  return marketplace.siteCode.toLowerCase().includes(searchText) ||
         marketplace.siteName.toLowerCase().includes(searchText)
}

async function fetchMarketplaces() {
  try {
    const res = await getEnabledMarketplaces()
    marketplaceOptions.value = res.data || []
  } catch (error) {
    console.error('获取站点列表失败:', error)
  }
}

async function fetchCurrencies() {
  try {
    const res = await getEnabledCurrencies()
    currencyOptions.value = res.data || []
  } catch (error) {
    console.error('获取货币列表失败:', error)
  }
}

async function fetchRecentData() {
  recentLoading.value = true
  try {
    const res = await searchAdvertisingData({ page: 1, size: 5 })
    recentData.value = res.data?.records || []
  } catch (error) {
    console.error('获取最近记录失败:', error)
  } finally {
    recentLoading.value = false
  }
}

function handleSiteChange(siteCode: string) {
  // 自动设置该站点对应的货币
  const marketplace = marketplaceOptions.value.find(m => m.siteCode === siteCode)
  if (marketplace?.currencyCode) {
    formData.currencyCode = marketplace.currencyCode
  }
}

function handleYearMonthChange(date: Dayjs | null) {
  formData.yearMonth = date?.format('YYYY-MM') || ''
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitting.value = true

    const submitData: AdvertisingDataForm = {
      siteCode: formData.siteCode,
      yearMonth: formData.yearMonth,
      amount: formData.amount!,
      currencyCode: formData.currencyCode,
      invoiceNo: formData.invoiceNo,
      remark: formData.remark
    }

    await createAdvertising(submitData)
    message.success('录入成功')
    router.push('/advertising/list')
  } catch (error: any) {
    if (error?.errorFields) {
      return
    }
    console.error('录入失败:', error)
  } finally {
    submitting.value = false
  }
}

async function handleSubmitAndContinue() {
  try {
    await formRef.value?.validate()
    submitting.value = true

    const submitData: AdvertisingDataForm = {
      siteCode: formData.siteCode,
      yearMonth: formData.yearMonth,
      amount: formData.amount!,
      currencyCode: formData.currencyCode,
      invoiceNo: formData.invoiceNo,
      remark: formData.remark
    }

    await createAdvertising(submitData)
    message.success('录入成功，可继续添加')
    
    // 重置部分字段，保留站点和货币
    formData.yearMonth = ''
    formData.yearMonthDate = null
    formData.amount = undefined
    formData.invoiceNo = ''
    formData.remark = ''
    
    // 刷新最近记录
    fetchRecentData()
  } catch (error: any) {
    if (error?.errorFields) {
      return
    }
    console.error('录入失败:', error)
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
  Object.assign(formData, {
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

async function handleDeleteRecent(record: AdvertisingData) {
  try {
    await deleteAdvertising(record.id)
    message.success('删除成功')
    fetchRecentData()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 初始化
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
    }
  }

  .form-card {
    margin-bottom: $spacing-lg;
  }

  .recent-card {
    .amount {
      font-weight: 500;
      color: $primary-color;
    }
  }
}
</style>
