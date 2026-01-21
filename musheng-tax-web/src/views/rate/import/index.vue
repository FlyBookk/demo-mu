<template>
  <div class="rate-import-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">汇率数据导入</h1>
      <p class="page-desc">通过导入 Excel 文件批量导入汇率数据（按天存储）</p>
    </div>

    <!-- 文件导入说明 -->
    <a-card class="info-card" style="margin-bottom: 24px">
      <a-alert
        message="Excel 文件格式说明"
        type="info"
        show-icon
      >
        <template #description>
          <div style="margin-top: 8px">
            <p>支持两种 Excel 格式：</p>
            <ol>
              <li><strong>矩阵格式（推荐）</strong>：第一列为日期，其余列为货币对（如 USD/CNY, EUR/CNY）</li>
              <li><strong>CSV 格式</strong>：包含日期、货币编码、汇率三列</li>
            </ol>
            <p style="margin-top: 8px">
              <a-typography-text type="secondary">
                示例：可从中国货币网（www.chinamoney.com.cn）下载"人民币汇率中间价历史数据"
              </a-typography-text>
            </p>
          </div>
        </template>
      </a-alert>
    </a-card>

    <!-- 文件导入 -->
    <a-card class="step-card">
      <a-form
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="上传文件">
          <a-upload-dragger
            v-model:file-list="fileList"
            name="file"
            :multiple="false"
            :before-upload="beforeUpload"
            accept=".csv,.xlsx,.xls"
            :disabled="importing"
            @remove="handleRemoveFile"
          >
            <p class="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
            <p class="ant-upload-hint">
              支持 .xlsx、.xls、.csv 格式，单个文件不超过 10MB
            </p>
          </a-upload-dragger>
        </a-form-item>

        <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
          <a-space>
            <a-button
              type="primary"
              :loading="importing"
              :disabled="fileList.length === 0"
              @click="handleImport"
            >
              <CloudUploadOutlined /> 开始导入
            </a-button>
            <a-button @click="handleReset">
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 导入结果 -->
    <a-card v-if="importResult" class="result-card">
      <a-result
        :status="importResult.failCount > 0 ? 'warning' : 'success'"
        :title="getResultTitle()"
        :sub-title="getResultSubTitle()"
      >
        <template #extra>
          <a-space>
            <a-button type="primary" @click="handleViewList">
              查看汇率列表
            </a-button>
            <a-button @click="handleImportAgain">
              继续导入
            </a-button>
          </a-space>
        </template>
      </a-result>

      <a-descriptions :column="3" size="small" bordered>
        <a-descriptions-item label="总记录数">
          {{ importResult.totalCount }}
        </a-descriptions-item>
        <a-descriptions-item label="导入成功">
          <span class="success-text">{{ importResult.successCount }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="已存在">
          <span class="warning-text">{{ importResult.existsCount }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="导入失败">
          <span class="error-text">{{ importResult.failCount }}</span>
        </a-descriptions-item>
      </a-descriptions>

      <!-- 错误详情 -->
      <div v-if="importResult.errors && importResult.errors.length > 0" style="margin-top: 16px">
        <a-divider>错误详情</a-divider>
        <a-list size="small" :data-source="importResult.errors" bordered>
          <template #renderItem="{ item }">
            <a-list-item>
              <a-typography-text type="danger">{{ item }}</a-typography-text>
            </a-list-item>
          </template>
        </a-list>
        <a-typography-text type="secondary" v-if="importResult.errors.length >= 10">
          （仅显示前 10 条错误）
        </a-typography-text>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { UploadProps, UploadFile } from 'ant-design-vue'
import {
  InboxOutlined,
  CloudUploadOutlined
} from '@ant-design/icons-vue'
import { importRateData } from '@/api/rate'

const router = useRouter()

// ============= 文件导入 =============
const fileList = ref<UploadFile[]>([])
const importing = ref(false)

// ============= 导入结果 =============
const importResult = ref<{
  totalCount: number
  successCount: number
  existsCount: number
  failCount: number
  errors: string[]
} | null>(null)

// ============= 方法 =============
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isValidType = [
    'text/csv',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ].includes(file.type) ||
  file.name.endsWith('.csv') ||
  file.name.endsWith('.xlsx') ||
  file.name.endsWith('.xls')

  if (!isValidType) {
    message.error('只能上传 CSV 或 Excel 文件!')
    return false
  }

  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    message.error('文件大小不能超过 10MB!')
    return false
  }

  // 阻止自动上传
  return false
}

function handleRemoveFile() {
  fileList.value = []
  importResult.value = null
}

async function handleImport() {
  if (fileList.value.length === 0) {
    message.warning('请选择要导入的文件')
    return
  }

  const file = fileList.value[0].originFileObj as File
  if (!file) {
    message.error('无法获取文件对象')
    return
  }

  importing.value = true
  importResult.value = null

  try {
    const result = await importRateData(file)
    importResult.value = result.data

    if (result.data.failCount === 0) {
      message.success(`导入成功！共导入 ${result.data.successCount} 条汇率数据`)
    } else {
      message.warning(
        `导入完成！成功 ${result.data.successCount} 条，失败 ${result.data.failCount} 条`
      )
    }
  } catch (error: any) {
    console.error('导入失败:', error)
    message.error(error.message || '导入失败，请检查文件格式')
  } finally {
    importing.value = false
  }
}

function handleReset() {
  fileList.value = []
  importResult.value = null
}

function getResultTitle(): string {
  if (!importResult.value) return ''
  if (importResult.value.failCount === 0) {
    return '导入成功'
  } else if (importResult.value.successCount > 0) {
    return '部分导入成功'
  } else {
    return '导入失败'
  }
}

function getResultSubTitle(): string {
  if (!importResult.value) return ''
  const { successCount, existsCount, failCount } = importResult.value
  const parts = []
  if (successCount > 0) parts.push(`成功导入 ${successCount} 条`)
  if (existsCount > 0) parts.push(`已存在 ${existsCount} 条`)
  if (failCount > 0) parts.push(`失败 ${failCount} 条`)
  return parts.join('，')
}

function handleViewList() {
  router.push('/rate/list')
}

function handleImportAgain() {
  fileList.value = []
  importResult.value = null
}
</script>

<style lang="scss" scoped>
.rate-import-page {
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

  .info-card {
    ol {
      margin: 8px 0;
      padding-left: 20px;
    }
  }

  .step-card {
    margin-bottom: $spacing-lg;
  }

  .result-card {
    .success-text {
      color: $success-color;
      font-weight: 600;
    }

    .warning-text {
      color: $warning-color;
      font-weight: 600;
    }

    .error-text {
      color: $error-color;
      font-weight: 600;
    }
  }
}
</style>
