<template>
  <div class="msku-list-page">
    <div class="page-header">
      <h1 class="page-title">MSKU列表</h1>
      <p class="page-desc">展示结算推导后的MSKU汇总数据，支持行内编辑</p>
    </div>

    <!-- 筛选条件 -->
    <a-card class="filter-card" style="margin-bottom: 16px">
      <a-form layout="inline">
        <a-form-item label="站点">
          <a-select
            v-model:value="queryParams.siteCode"
            placeholder="全部站点"
            allow-clear
            style="width: 120px"
          >
            <a-select-option value="US">US</a-select-option>
            <a-select-option value="CA">CA</a-select-option>
            <a-select-option value="UK">UK</a-select-option>
            <a-select-option value="DE">DE</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="MSKU">
          <a-input
            v-model:value="queryParams.msku"
            placeholder="输入MSKU搜索"
            allow-clear
            style="width: 200px"
          />
        </a-form-item>
        <a-form-item label="结算季度">
          <a-select
            v-model:value="selectedQuarter"
            placeholder="请选择季度"
            allow-clear
            style="width: 280px"
            :options="quarterOptions"
            @change="handleQuarterChange"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">
              <SearchOutlined /> 查询
            </a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 数据表格 -->
    <a-card>
      <a-table
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        size="middle"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 站点标签 -->
          <template v-if="column.dataIndex === 'siteCode'">
            <a-tag :color="siteColorMap[record.siteCode] || 'default'">
              {{ record.siteCode }}
            </a-tag>
          </template>
          <!-- 可编辑列：单价 -->
          <template v-if="column.dataIndex === 'unitPrice'">
            <template v-if="editingId === record.id">
              <a-input-number
                v-model:value="editForm.unitPrice"
                :min="0"
                :precision="2"
                size="small"
                style="width: 100px"
              />
            </template>
            <template v-else>
              {{ record.currency }} {{ formatNumber(record.unitPrice) }}
            </template>
          </template>

          <!-- 可编辑列：数量 -->
          <template v-if="column.dataIndex === 'quantity'">
            <template v-if="editingId === record.id">
              <a-input-number
                v-model:value="editForm.quantity"
                :min="0"
                :precision="0"
                size="small"
                style="width: 80px"
              />
            </template>
            <template v-else>
              {{ record.quantity }}
            </template>
          </template>

          <!-- 总价（自动计算，不可编辑） -->
          <template v-if="column.dataIndex === 'amount'">
            <template v-if="editingId === record.id">
              <span style="color: #1890ff">
                {{ record.currency }} {{ computedAmount }}
              </span>
            </template>
            <template v-else>
              {{ record.currency }} {{ formatNumber(record.amount) }}
            </template>
          </template>

          <!-- 可编辑列：采购成本 -->
          <template v-if="column.dataIndex === 'procurementCostCny'">
            <template v-if="editingId === record.id">
              <a-input-number
                v-model:value="editForm.procurementCostCny"
                :min="0"
                :precision="2"
                size="small"
                style="width: 100px"
              />
            </template>
            <template v-else>
              {{ record.procurementCostCny != null ? `¥ ${formatNumber(record.procurementCostCny)}` : '-' }}
            </template>
          </template>

          <!-- 可编辑列：汇率 -->
          <template v-if="column.dataIndex === 'averageExchangeRate'">
            <template v-if="editingId === record.id">
              <a-input-number
                v-model:value="editForm.averageExchangeRate"
                :min="0"
                :precision="4"
                size="small"
                style="width: 90px"
              />
            </template>
            <template v-else>
              {{ record.averageExchangeRate ?? '-' }}
            </template>
          </template>

          <!-- 结算周期 -->
          <template v-if="column.dataIndex === 'period'">
            {{ record.periodStart }} ~ {{ record.periodEnd }}
          </template>

          <!-- 操作列 -->
          <template v-if="column.dataIndex === 'action'">
            <template v-if="editingId === record.id">
              <a-space>
                <a-button type="link" size="small" :loading="saving" @click="handleSave(record)">
                  保存
                </a-button>
                <a-button type="link" size="small" danger @click="handleCancel">
                  取消
                </a-button>
              </a-space>
            </template>
            <template v-else>
              <a-button type="link" size="small" :disabled="editingId !== null" @click="handleEdit(record)">
                编辑
              </a-button>
            </template>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { getMskuList, updateMsku } from '@/api/msku'
import type { MskuListVO, MskuQueryParams, MskuUpdateParams } from '@/api/msku'

const loading = ref(false)
const saving = ref(false)
const dataList = ref<MskuListVO[]>([])
const total = ref(0)

const queryParams = reactive<MskuQueryParams>({
  siteCode: undefined,
  msku: undefined,
  periodStart: undefined,
  periodEnd: undefined,
  pageNum: 1,
  pageSize: 20
})

const selectedQuarter = ref<string | undefined>(undefined)

// 行内编辑状态
const editingId = ref<number | null>(null)
const editForm = reactive<MskuUpdateParams>({
  id: 0,
  unitPrice: undefined,
  quantity: undefined,
  procurementCostCny: undefined,
  averageExchangeRate: undefined
})

// 编辑时自动计算总价
const computedAmount = computed(() => {
  const price = editForm.unitPrice ?? 0
  const qty = editForm.quantity ?? 0
  return formatNumber(price * qty)
})

// 站点颜色映射
const siteColorMap: Record<string, string> = {
  US: 'blue',
  CA: 'red',
  UK: 'green',
  DE: 'orange'
}

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

function handleQuarterChange(val: string | undefined) {
  if (val) {
    const [start, end] = val.split('|')
    queryParams.periodStart = start
    queryParams.periodEnd = end
  } else {
    queryParams.periodStart = undefined
    queryParams.periodEnd = undefined
  }
}

// 表格列定义
const columns = [
  { title: '站点', dataIndex: 'siteCode', width: 80 },
  { title: 'MSKU', dataIndex: 'msku', width: 200, ellipsis: true },
  { title: '数量', dataIndex: 'quantity', width: 100, align: 'right' as const },
  { title: '单价', dataIndex: 'unitPrice', width: 140, align: 'right' as const },
  { title: '总价', dataIndex: 'amount', width: 140, align: 'right' as const },
  { title: '采购成本(CNY)', dataIndex: 'procurementCostCny', width: 140, align: 'right' as const },
  { title: '汇率', dataIndex: 'averageExchangeRate', width: 110, align: 'right' as const },
  { title: '结算周期', dataIndex: 'period', width: 220 },
  { title: '操作', dataIndex: 'action', width: 120, fixed: 'right' as const }
]

const pagination = computed(() => ({
  current: queryParams.pageNum,
  pageSize: queryParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (t: number) => `共 ${t} 条`
}))

function formatNumber(val: number | null | undefined) {
  if (val == null) return '-'
  return Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getMskuList(queryParams)
    dataList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error: any) {
    message.error(error?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryParams.pageNum = 1
  fetchData()
}

function handleReset() {
  queryParams.siteCode = undefined
  queryParams.msku = undefined
  queryParams.periodStart = undefined
  queryParams.periodEnd = undefined
  selectedQuarter.value = undefined
  queryParams.pageNum = 1
  fetchData()
}

function handleTableChange(pag: any) {
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  fetchData()
}

/** 进入编辑模式 */
function handleEdit(record: MskuListVO) {
  editingId.value = record.id
  editForm.id = record.id
  editForm.unitPrice = record.unitPrice
  editForm.quantity = record.quantity
  editForm.procurementCostCny = record.procurementCostCny ?? undefined
  editForm.averageExchangeRate = record.averageExchangeRate ?? undefined
}

/** 取消编辑 */
function handleCancel() {
  editingId.value = null
  editForm.id = 0
  editForm.unitPrice = undefined
  editForm.quantity = undefined
  editForm.procurementCostCny = undefined
  editForm.averageExchangeRate = undefined
}

/** 保存编辑 */
async function handleSave(record: MskuListVO) {
  saving.value = true
  try {
    await updateMsku({
      id: record.id,
      unitPrice: editForm.unitPrice,
      quantity: editForm.quantity,
      procurementCostCny: editForm.procurementCostCny,
      averageExchangeRate: editForm.averageExchangeRate
    })
    message.success('保存成功')
    handleCancel()
    fetchData()
  } catch (error: any) {
    message.error(error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.msku-list-page {
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
}
</style>
