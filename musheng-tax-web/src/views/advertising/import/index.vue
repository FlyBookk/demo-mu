<template>
  <div class="advertising-import-page">
    <a-page-header
      title="广告数据批量导入"
      sub-title="支持发票编号去重、时间字段校验"
      @back="() => $router.back()"
    >
      <template #extra>
        <a-space>
          <a-button @click="downloadTemplate">
            <template #icon><DownloadOutlined /></template>
            下载模板
          </a-button>
          <a-button type="primary" @click="handleShowManualInput">
            <template #icon><PlusOutlined /></template>
            手动录入
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <div class="import-content">
      <!-- 步骤指示器 -->
      <a-steps :current="currentStep" class="import-steps">
        <a-step title="选择文件" description="上传Excel文件或手动录入" />
        <a-step title="数据预览" description="确认导入数据" />
        <a-step title="导入结果" description="查看导入统计" />
      </a-steps>

      <!-- 步骤1: 文件上传或手动录入 -->
      <div v-if="currentStep === 0" class="step-content">
        <a-tabs v-model:activeKey="uploadTab" class="upload-tabs">
          <!-- 文件上传标签页 -->
          <a-tab-pane key="file" tab="文件上传">
            <a-upload-dragger
              v-model:file-list="fileList"
              name="file"
              accept=".xlsx,.xls,.csv"
              :before-upload="beforeUpload"
              :custom-request="handleFileUpload"
              :max-count="1"
              @change="handleFileChange"
            >
              <p class="ant-upload-drag-icon">
                <inbox-outlined style="font-size: 48px; color: #1890ff" />
              </p>
              <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
              <p class="ant-upload-hint">
                支持 Excel (.xlsx, .xls) 或 CSV 格式，单个文件最大 10MB
              </p>
            </a-upload-dragger>

            <a-alert
              v-if="fileList.length > 0"
              :message="`已选择文件: ${fileList[0].name}`"
              type="success"
              show-icon
              closable
              class="file-alert"
            />
            <!-- 解析后即时预览 -->
            <a-alert
              v-if="parsedData.length > 0 && uploadTab === 'file'"
              :message="`解析成功：共 ${parsedData.length} 条，点击「下一步」查看完整预览`"
              type="info"
              show-icon
              class="file-alert"
            />
          </a-tab-pane>

          <!-- 手动录入标签页 -->
          <a-tab-pane key="manual" tab="手动录入">
            <a-button type="dashed" block @click="handleAddRow" class="add-row-btn">
              <template #icon><PlusOutlined /></template>
              添加一行数据
            </a-button>

            <a-table
              :columns="manualColumns"
              :data-source="manualDataSource"
              :pagination="false"
              :scroll="{ x: 2000 }"
              class="manual-table"
            >
              <template #bodyCell="{ column, record, index }">
                <template v-if="column.dataIndex === 'storeName'">
                  <a-input v-model:value="record.storeName" placeholder="店铺名称" />
                </template>
                <template v-else-if="column.dataIndex === 'invoiceNumber'">
                  <a-input v-model:value="record.invoiceNumber" placeholder="发票编号" />
                </template>
                <template v-else-if="column.dataIndex === 'billingPeriod'">
                  <a-range-picker
                    v-model:value="record.billingPeriod"
                    format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </template>
                <template v-else-if="column.dataIndex === 'cost'">
                  <a-input-number
                    v-model:value="record.cost"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                  />
                </template>
                <template v-else-if="column.dataIndex === 'currency'">
                  <a-select v-model:value="record.currency" style="width: 100%">
                    <a-select-option value="USD">USD</a-select-option>
                    <a-select-option value="CAD">CAD</a-select-option>
                    <a-select-option value="GBP">GBP</a-select-option>
                    <a-select-option value="EUR">EUR</a-select-option>
                  </a-select>
                </template>
                <template v-else-if="column.dataIndex === 'action'">
                  <a-button
                    type="link"
                    danger
                    size="small"
                    @click="handleDeleteRow(index)"
                  >
                    删除
                  </a-button>
                </template>
              </template>
            </a-table>
          </a-tab-pane>
        </a-tabs>

        <div class="step-actions step-actions-step1">
          <a-button type="primary" size="large" @click="handleNextStep" :disabled="!canProceed">
            下一步：数据预览
          </a-button>
        </div>
      </div>

      <!-- 步骤2: 数据预览 -->
      <div v-if="currentStep === 1" class="step-content">
        <a-alert
          v-if="previewValidationErrors.length > 0"
          message="部分数据校验未通过"
          :description="`共 ${previewData.length} 条，其中 ${previewValidationErrors.length} 条无效将跳过，仅导入 ${previewData.length - previewValidationErrors.length} 条有效数据`"
          type="warning"
          show-icon
          class="preview-alert"
        />
        <a-alert
          v-else
          message="数据预览"
          :description="`共 ${previewData.length} 条数据，请确认后点击「开始导入」按钮`"
          type="info"
          show-icon
          class="preview-alert"
        />

        <a-table
          :columns="previewColumns"
          :data-source="previewData"
          :pagination="{ pageSize: 10 }"
          :scroll="{ x: 2500 }"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'billingPeriod'">
              {{ record.billingStartDate }} ~ {{ record.billingEndDate }}
            </template>
            <template v-else-if="column.dataIndex === 'cost'">
              {{ record.currency }} {{ record.cost?.toFixed(2) }}
            </template>
          </template>
        </a-table>

        <div class="step-actions step-actions-preview">
          <a-button @click="handlePrevStep">上一步</a-button>
          <a-popconfirm
            title="确认导入"
            :description="`将导入 ${previewData.length - previewValidationErrors.length} 条广告数据，确认继续？`"
            ok-text="确认导入"
            cancel-text="取消"
            @confirm="doExecuteImport"
          >
            <template #default>
              <a-button type="primary" :loading="importing" size="large">
                开始导入
              </a-button>
            </template>
          </a-popconfirm>
        </div>
      </div>

      <!-- 步骤3: 导入结果 -->
      <div v-if="currentStep === 2" class="step-content">
        <a-result
          :status="(importResult.failedCount ?? 0) > 0 ? 'warning' : 'success'"
          :title="(importResult.failedCount ?? 0) > 0 ? '导入完成（部分失败）' : '导入成功'"
          :sub-title="`批次ID: ${importResult.importBatchId ?? '-'}`"
        >
          <template #extra>
            <div class="import-result-extra">
              <div class="import-stats-row">
                <a-statistic title="总记录数" :value="importResult.totalCount ?? 0" suffix="条" />
                <a-statistic
                  title="成功导入"
                  :value="importResult.importedCount ?? 0"
                  suffix="条"
                  value-style="color: #52c41a"
                />
                <a-statistic
                  title="去重记录"
                  :value="importResult.duplicatedCount ?? 0"
                  suffix="条"
                  value-style="color: #faad14"
                />
                <a-statistic
                  title="失败记录"
                  :value="importResult.failedCount ?? 0"
                  suffix="条"
                  value-style="color: #ff4d4f"
                />
              </div>

              <!-- 去重记录详情 -->
              <a-collapse v-if="(importResult.duplicatedInvoices?.length ?? 0) > 0">
                <a-collapse-panel
                  key="duplicated"
                  :header="`去重的发票编号 (${(importResult.duplicatedInvoices ?? []).length}个)`"
                >
                  <a-tag
                    v-for="invoice in (importResult.duplicatedInvoices ?? [])"
                    :key="invoice"
                    color="warning"
                    class="invoice-tag"
                  >
                    {{ invoice }}
                  </a-tag>
                </a-collapse-panel>
              </a-collapse>

              <!-- 失败记录详情（有失败时默认展开，便于用户查看原因） -->
              <a-collapse v-if="(importResult.failedRecords?.length ?? 0) > 0" :default-active-key="['failed']">
                <a-collapse-panel
                  key="failed"
                  :header="`失败记录详情 (${(importResult.failedRecords ?? []).length}条)`"
                >
                  <a-list
                    :data-source="importResult.failedRecords ?? []"
                    size="small"
                  >
                    <template #renderItem="{ item }">
                      <a-list-item>
                        <a-list-item-meta>
                          <template #title>
                            发票编号: {{ item.invoiceNumber || '未知' }}
                            <a-tag color="error" v-if="item.index !== undefined">
                              第 {{ item.index + 1 }} 行
                            </a-tag>
                          </template>
                          <template #description>
                            错误原因: {{ item.errorMessage }}
                          </template>
                        </a-list-item-meta>
                      </a-list-item>
                    </template>
                  </a-list>
                </a-collapse-panel>
              </a-collapse>

              <div class="import-result-actions">
                <a-button type="primary" @click="handleViewList">
                  查看数据列表
                </a-button>
                <a-button @click="handleReimport">
                  重新导入
                </a-button>
              </div>
            </div>
          </template>
        </a-result>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { DownloadOutlined, PlusOutlined, InboxOutlined } from '@ant-design/icons-vue'
import { importAdvertisingData, downloadAdvertisingTemplate } from '@/api/advertising'
import type {
  AdvertisingImportRequest,
  AdvertisingImportResponse
} from '@/types/advertising'
import type { UploadFile, UploadProps } from 'ant-design-vue'
import * as XLSX from 'xlsx'
import dayjs, { Dayjs } from 'dayjs'

const router = useRouter()

// 当前步骤
const currentStep = ref(0)
const uploadTab = ref<string>('file')

// 文件上传
const fileList = ref<UploadFile[]>([])
const parsedData = ref<any[]>([])

// 手动录入
interface ManualRow {
  key: number
  storeName: string
  invoiceNumber: string
  billingPeriod: [Dayjs, Dayjs] | null
  cost: number | null
  currency: string
  [key: string]: any
}

const manualDataSource = ref<ManualRow[]>([])
let rowKeyCounter = 0

// 预览数据与校验错误
const previewData = ref<AdvertisingImportRequest[]>([])
const previewValidationErrors = ref<{ rowIndex: number; invoiceNumber: string; message: string }[]>([])

// 导入状态
const importing = ref(false)

// 导入结果
const importResult = ref<AdvertisingImportResponse>({
  totalCount: 0,
  importedCount: 0,
  duplicatedCount: 0,
  failedCount: 0,
  importBatchId: '',
  duplicatedInvoices: [],
  failedRecords: []
})

// 手动录入表格列
const manualColumns = [
  { title: '店铺名称', dataIndex: 'storeName', width: 150, fixed: 'left' },
  { title: '发票编号', dataIndex: 'invoiceNumber', width: 150 },
  { title: '账单周期', dataIndex: 'billingPeriod', width: 250 },
  { title: '费用', dataIndex: 'cost', width: 120 },
  { title: '币种', dataIndex: 'currency', width: 100 },
  { title: '操作', dataIndex: 'action', width: 80, fixed: 'right' }
]

// 预览表格列
const previewColumns = [
  { title: '店铺名称', dataIndex: 'storeName', width: 150 },
  { title: '发票编号', dataIndex: 'invoiceNumber', width: 150 },
  { title: '发票状态', dataIndex: 'invoiceStatus', width: 120 },
  { title: '账单周期', dataIndex: 'billingPeriod', width: 220 },
  { title: '费用', dataIndex: 'cost', width: 150 },
  { title: '广告活动', dataIndex: 'campaignName', width: 150 },
  { title: '活动ID', dataIndex: 'campaignId', width: 150 },
  { title: '点击次数', dataIndex: 'clicks', width: 100 },
  { title: '平均CPC', dataIndex: 'avgCpc', width: 100 }
]

// 能否进入下一步
const canProceed = computed(() => {
  if (uploadTab.value === 'file') {
    return parsedData.value.length > 0
  } else {
    return manualDataSource.value.length > 0
  }
})

// 文件上传前校验
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isValidType = file.name.endsWith('.xlsx') || file.name.endsWith('.xls') || file.name.endsWith('.csv')
  if (!isValidType) {
    message.error('只能上传 Excel 或 CSV 文件!')
    return false
  }

  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error('文件大小不能超过 10MB!')
    return false
  }

  return true
}

// 处理文件上传
const handleFileUpload: UploadProps['customRequest'] = async (options) => {
  const { file } = options
  try {
    const data = await readExcelFile(file as File)
    parsedData.value = data
    message.success(`成功解析 ${data.length} 条数据`)
  } catch (error) {
    message.error('文件解析失败: ' + (error as Error).message)
  }
}

// 币种映射（支持中文）
const CURRENCY_MAP: Record<string, string> = {
  USD: 'USD', CAD: 'CAD', GBP: 'GBP', EUR: 'EUR',
  美元: 'USD', 加元: 'CAD', 英镑: 'GBP', 欧元: 'EUR'
}

// 将 Excel 日期（Date/序列号/字符串）转为 YYYY-MM-DD
function formatExcelDate(val: unknown): string {
  if (val == null || val === '') return ''
  if (val instanceof Date) return dayjs(val).format('YYYY-MM-DD')
  if (typeof val === 'number') {
    // Excel 序列号：1900-01-01 为 1，25569 ≈ 1970-01-01
    const d = new Date((val - 25569) * 86400 * 1000)
    return isNaN(d.getTime()) ? '' : dayjs(d).format('YYYY-MM-DD')
  }
  const s = String(val).trim()
  if (/^\d{4}-\d{2}-\d{2}/.test(s)) return s.slice(0, 10)
  return s || ''
}

// 读取 Excel 文件（启用 cellDates 正确解析日期）
const readExcelFile = (file: File): Promise<any[]> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const data = new Uint8Array(e.target?.result as ArrayBuffer)
        const workbook = XLSX.read(data, { type: 'array', cellDates: true })
        const firstSheet = workbook.Sheets[workbook.SheetNames[0]]
        const jsonData = XLSX.utils.sheet_to_json(firstSheet) as any[]
        // 过滤全空行和空发票编号行
        const filtered = jsonData.filter(row => {
          const inv = row?.['发票编号'] ?? row?.invoiceNumber ?? ''
          const store = row?.['店铺'] ?? row?.storeName ?? ''
          return String(inv).trim() || String(store).trim()
        })
        resolve(filtered)
      } catch (error) {
        reject(error)
      }
    }
    reader.onerror = reject
    reader.readAsArrayBuffer(file)
  })
}

// 文件变化
const handleFileChange = (info: any) => {
  if (info.file.status === 'removed') {
    parsedData.value = []
  }
}

// 显示手动录入
const handleShowManualInput = () => {
  uploadTab.value = 'manual'
  if (manualDataSource.value.length === 0) {
    handleAddRow()
  }
}

// 添加一行
const handleAddRow = () => {
  manualDataSource.value.push({
    key: rowKeyCounter++,
    storeName: '',
    invoiceNumber: '',
    billingPeriod: null,
    cost: null,
    currency: 'USD'
  })
}

// 删除一行
const handleDeleteRow = (index: number) => {
  manualDataSource.value.splice(index, 1)
}

// 前端预校验单条数据，返回错误信息
interface ValidationError { rowIndex: number; invoiceNumber: string; message: string }
function validateImportRow(row: AdvertisingImportRequest, index: number): string | null {
  if (!row.storeName?.trim()) return '店铺名称不能为空'
  if (!row.invoiceNumber?.trim()) return '发票编号不能为空'
  if (!row.billingStartDate) return '账单开始日期不能为空'
  if (!row.billingEndDate) return '账单结束日期不能为空'
  if (!row.issueDate) return '开具时间不能为空'
  if (!row.currency) return '付款币种不能为空'
  const validCurrencies = ['USD', 'CAD', 'GBP', 'EUR']
  if (!validCurrencies.includes(row.currency)) return `不支持的币种: ${row.currency}`
  if (row.invoiceAmount == null || row.invoiceAmount < 0.01) return '账单金额必须大于0'
  if (row.cost == null || row.cost < 0) return '费用不能为负数'
  if (row.billingStartDate > row.billingEndDate) return '账单开始日期不能晚于结束日期'
  return null
}

// 下一步
const handleNextStep = () => {
  try {
    let data: AdvertisingImportRequest[] = []
    if (uploadTab.value === 'file') {
      data = parsedData.value.map(row => convertRowToImportRequest(row))
    } else {
      data = manualDataSource.value
        .filter(row => row.storeName && row.invoiceNumber)
        .map(row => convertManualRowToImportRequest(row))
    }

    if (data.length === 0) {
      message.warning('没有有效的数据可以导入')
      return
    }

    // 前端预校验
    const errors: ValidationError[] = []
    data.forEach((row, i) => {
      const err = validateImportRow(row, i)
      if (err) errors.push({ rowIndex: i + 1, invoiceNumber: row.invoiceNumber || '-', message: err })
    })

    if (errors.length > 0) {
      const validCount = data.length - errors.length
      const msg = `共 ${data.length} 条，其中 ${errors.length} 条校验失败。\n示例: 第${errors[0].rowIndex}行 ${errors[0].message}`
      message.warning({
        content: msg,
        duration: 5
      })
      // 仍进入预览，但标记错误行；用户可删除无效行或修正后重试
      previewData.value = data
      previewValidationErrors.value = errors
      currentStep.value = 1
      return
    }

    previewData.value = data
    previewValidationErrors.value = []
    currentStep.value = 1
  } catch (error) {
    message.error('数据转换失败: ' + (error as Error).message)
  }
}

// 上一步
const handlePrevStep = () => {
  currentStep.value = 0
}

// 转换Excel行数据为导入请求（兼容利润报表-广告发票记录格式）
const convertRowToImportRequest = (row: any): AdvertisingImportRequest => {
  // 解析账单周期：支持 "2025-06-30至2025-07-03" 或 Excel 日期
  const billingPeriodRaw = row['账单周期'] ?? row['billingPeriod'] ?? ''
  let startDate = ''
  let endDate = ''
  if (billingPeriodRaw) {
    const parts = String(billingPeriodRaw).split(/[至~]/).map((p: string) => p.trim())
    startDate = formatExcelDate(parts[0] || '') || parts[0] || ''
    endDate = formatExcelDate(parts[1] || '') || parts[1] || ''
  }
  const issueDateRaw = row['开具时间'] ?? row['issueDate'] ?? ''
  const issueDate = formatExcelDate(issueDateRaw) || dayjs().format('YYYY-MM-DD')
  if (!startDate || !endDate) {
    startDate = startDate || issueDate
    endDate = endDate || issueDate
  }

  const costVal = parseFloat(row['费用'] ?? row['cost'] ?? '0') || 0
  const invoiceAmountVal = parseFloat(row['账单金额'] ?? row['invoiceAmount'] ?? '0') || 0
  // 后端要求 invoiceAmount >= 0.01，空/0 时用 cost 或 0.01 兜底
  const invoiceAmount = invoiceAmountVal >= 0.01 ? invoiceAmountVal : (costVal >= 0.01 ? costVal : 0.01)

  const currencyRaw = String(row['付款币种'] ?? row['currency'] ?? 'USD').trim()
  const currency = CURRENCY_MAP[currencyRaw] || (CURRENCY_MAP[currencyRaw.toUpperCase()] ?? 'USD')

  return {
    storeName: String(row['店铺'] ?? row['storeName'] ?? '').trim() || '未填写',
    invoiceNumber: String(row['发票编号'] ?? row['invoiceNumber'] ?? '').trim(),
    invoiceStatus: String(row['发票状态'] ?? row['invoiceStatus'] ?? 'PAID_IN_FULL').trim() || 'PAID_IN_FULL',
    paymentType: row['支付类型'] ?? row['paymentType'],
    billingStartDate: startDate || issueDate,
    billingEndDate: endDate || issueDate,
    issueDate,
    currency,
    invoiceAmount,
    cost: costVal,
    otherCost: parseFloat(row['其他费分摊'] ?? row['otherCost'] ?? '0') || 0,
    campaignName: row['广告活动'] ?? row['campaignName'],
    campaignId: row['活动ID'] ?? row['campaignId'],
    pricingModel: row['计价方式'] ?? row['pricingModel'],
    clicks: parseInt(row['点击'] ?? row['clicks'] ?? '0', 10) || undefined,
    avgCpc: parseFloat(row['平均点击单价'] ?? row['avgCpc'] ?? '0') || undefined,
    dataSource: row['取值来源'] ?? row['dataSource'],
    productList: row['承担商品'] ?? row['productList'],
    adType: row['广告类型'] ?? row['adType'],
    remark: row['备注'] ?? row['remark']
  }
}

// 转换手动录入行为导入请求（补全必填字段）
const convertManualRowToImportRequest = (row: ManualRow): AdvertisingImportRequest => {
  const [startDate, endDate] = row.billingPeriod || []
  const today = dayjs().format('YYYY-MM-DD')
  const costVal = row.cost ?? 0
  const billingStart = startDate ? dayjs(startDate).format('YYYY-MM-DD') : today
  const billingEnd = endDate ? dayjs(endDate).format('YYYY-MM-DD') : today

  return {
    storeName: row.storeName?.trim() || '未填写',
    invoiceNumber: row.invoiceNumber?.trim() || '',
    invoiceStatus: 'PAID_IN_FULL',
    billingStartDate: billingStart,
    billingEndDate: billingEnd,
    issueDate: today,
    currency: row.currency || 'USD',
    invoiceAmount: costVal >= 0.01 ? costVal : 0.01,
    cost: costVal
  }
}

// 执行导入（由 Popconfirm 确认后直接调用）
const doExecuteImport = async () => {
  importing.value = true

  // 若有预校验错误，仅导入有效行
  const invalidRows = new Set(previewValidationErrors.value.map(e => e.rowIndex))
  const dataToImport = invalidRows.size > 0
    ? previewData.value.filter((_, i) => !invalidRows.has(i + 1))
    : previewData.value

  if (dataToImport.length === 0) {
    message.error('没有可导入的有效数据，请修正后重试')
    importing.value = false
    return
  }

  try {
    const res = await importAdvertisingData({
      data: dataToImport
    })
    // API 返回 { code, message, data }，实际导入结果在 data 中
    const result = (res as any)?.data ?? res
    importResult.value = result

    currentStep.value = 2

    const failed = result?.failedCount ?? 0
    const imported = result?.importedCount ?? 0
    if (failed === 0) {
      message.success(`导入成功！共导入 ${imported} 条数据`)
    } else {
      message.warning(`导入完成，${failed} 条失败`)
    }
  } catch (error) {
    message.error('导入失败: ' + (error as Error).message)
  } finally {
    importing.value = false
  }
}

// 下载模板
const downloadTemplate = async () => {
  try {
    await downloadAdvertisingTemplate()
    message.success('模板下载成功')
  } catch (error) {
    message.error('模板下载失败')
  }
}

// 查看列表
const handleViewList = () => {
  router.push({ name: 'AdvertisingList' })
}

// 重新导入
const handleReimport = () => {
  currentStep.value = 0
  fileList.value = []
  parsedData.value = []
  manualDataSource.value = []
  previewData.value = []
  previewValidationErrors.value = []
}
</script>

<style lang="scss" scoped>
.advertising-import-page {
  padding: 24px;
  background: #f0f2f5;
  min-height: 100vh;
}

.import-content {
  background: white;
  padding: 24px;
  border-radius: 8px;
  margin-top: 16px;
}

.import-steps {
  margin-bottom: 32px;
}

.step-content {
  min-height: 400px;
}

.upload-tabs {
  margin-top: 24px;
}

.file-alert {
  margin-top: 16px;
}

.add-row-btn {
  margin-bottom: 16px;
}

.manual-table {
  margin-top: 16px;
}

.preview-alert {
  margin-bottom: 16px;
}

.step-actions {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}
.step-actions-step1 {
  text-align: right;
}
.step-actions-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.invoice-tag {
  margin: 4px;
}

.import-result-extra {
  width: 100%;
}
.import-stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 32px 48px;
  justify-content: center;
  margin-bottom: 24px;
}
.import-stats-row :deep(.ant-statistic) {
  min-width: 100px;
}
.import-result-extra .ant-collapse {
  margin-bottom: 16px;
}
.import-result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
}
</style>
