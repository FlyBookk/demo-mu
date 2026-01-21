<template>
  <div class="source-field-input">
    <!-- 模式切换 -->
    <a-radio-group v-model:value="inputMode" class="mode-switch">
      <a-radio-button value="upload">上传样例文件</a-radio-button>
      <a-radio-button value="paste">粘贴表头行</a-radio-button>
    </a-radio-group>

    <!-- 上传模式 -->
    <div v-if="inputMode === 'upload'" class="upload-mode">
      <a-upload-dragger
        :before-upload="handleBeforeUpload"
        :show-upload-list="false"
        accept=".csv,.xlsx,.xls"
      >
        <p class="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p class="ant-upload-text">点击或拖拽文件到此处</p>
        <p class="ant-upload-hint">支持 CSV、Excel 格式，最大 10MB</p>
      </a-upload-dragger>

      <div v-if="uploadedFile" class="uploaded-file-info">
        <FileExcelOutlined />
        <span class="file-name">{{ uploadedFile.name }}</span>
        <a-button type="link" size="small" @click="handlePreviewFile">
          预览并选择表头行
        </a-button>
        <a-button type="link" size="small" danger @click="clearFile">
          移除
        </a-button>
      </div>
    </div>

    <!-- 粘贴模式 -->
    <div v-if="inputMode === 'paste'" class="paste-mode">
      <a-textarea
        v-model:value="pasteContent"
        placeholder="从 Excel/CSV 复制表头行，粘贴到此处..."
        :rows="3"
      />

      <div class="delimiter-select">
        <span>分隔符：</span>
        <a-radio-group v-model:value="delimiter" size="small">
          <a-radio-button value="auto">自动检测</a-radio-button>
          <a-radio-button value="tab">Tab</a-radio-button>
          <a-radio-button value="comma">逗号</a-radio-button>
          <a-radio-button value="semicolon">分号</a-radio-button>
        </a-radio-group>
      </div>

      <a-button type="primary" @click="handleParsePaste" :loading="parsing">
        解析表头
      </a-button>
    </div>

    <!-- 文件预览弹窗 -->
    <FilePreviewModal
      v-model:visible="previewModalVisible"
      :preview-data="filePreviewData"
      :loading="previewLoading"
      @confirm="handlePreviewConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { InboxOutlined, FileExcelOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import FilePreviewModal from './FilePreviewModal.vue'
import { useFieldParser } from './hooks/useFieldParser'
import type { SourceField, FilePreviewResponse, DelimiterType } from './types'

const emit = defineEmits<{
  (e: 'parsed', fields: SourceField[]): void
}>()

// 状态
const inputMode = ref<'upload' | 'paste'>('upload')
const uploadedFile = ref<File | null>(null)
const pasteContent = ref('')
const delimiter = ref<DelimiterType>('auto')
const previewModalVisible = ref(false)
const filePreviewData = ref<FilePreviewResponse | null>(null)
const previewLoading = ref(false)

// 使用文件解析 Hook
const { parsing, previewFile, parseFile, parseHeaderText } = useFieldParser()

// 文件上传处理
const handleBeforeUpload = (file: File) => {
  // 大小校验
  if (file.size > 10 * 1024 * 1024) {
    message.error('文件大小不能超过 10MB')
    return false
  }

  uploadedFile.value = file
  // 自动打开预览
  handlePreviewFile()
  return false
}

// 清除文件
const clearFile = () => {
  uploadedFile.value = null
  filePreviewData.value = null
}

// 预览文件
const handlePreviewFile = async () => {
  if (!uploadedFile.value) return

  previewLoading.value = true
  previewModalVisible.value = true

  try {
    const result = await previewFile(uploadedFile.value, 10)
    filePreviewData.value = result
  } catch (error: any) {
    message.error('文件预览失败: ' + error.message)
  } finally {
    previewLoading.value = false
  }
}

// 确认预览选择
const handlePreviewConfirm = async (headerRow: number, sheetName?: string) => {
  if (!uploadedFile.value) return

  try {
    const result = await parseFile(uploadedFile.value, headerRow, sheetName)
    emit('parsed', result.fields)
    previewModalVisible.value = false
    message.success(`成功解析 ${result.fields.length} 个字段`)
  } catch (error: any) {
    message.error('字段解析失败: ' + error.message)
  }
}

// 粘贴解析
const handleParsePaste = () => {
  if (!pasteContent.value.trim()) {
    message.warning('请先粘贴表头内容')
    return
  }

  try {
    const fields = parseHeaderText(pasteContent.value, delimiter.value)

    if (fields.length === 0) {
      message.warning('未检测到有效的表头字段')
      return
    }

    if (fields.length === 1) {
      message.warning('检测到字段过少，请确认分隔符是否正确')
    }

    emit('parsed', fields)
    message.success(`成功解析 ${fields.length} 个字段`)
  } catch (error: any) {
    message.error('解析失败: ' + error.message)
  }
}
</script>

<style lang="scss" scoped>
.source-field-input {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;

  .mode-switch {
    margin-bottom: 16px;
  }

  .upload-mode,
  .paste-mode {
    .uploaded-file-info {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 12px;
      padding: 8px 12px;
      background: #fff;
      border-radius: 4px;
      border: 1px solid #d9d9d9;

      .file-name {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .paste-mode {
    .delimiter-select {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 12px 0;
    }
  }
}
</style>
