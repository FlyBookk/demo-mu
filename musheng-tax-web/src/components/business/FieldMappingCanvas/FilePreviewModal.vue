<template>
  <a-modal
    :open="visible"
    title="文件预览 - 选择表头行"
    width="800px"
    :confirm-loading="confirmLoading"
    @update:open="$emit('update:visible', $event)"
    @ok="handleConfirm"
    @cancel="handleCancel"
  >
    <a-spin :spinning="loading">
      <div class="file-preview-modal">
        <!-- 文件信息 -->
        <div v-if="previewData" class="file-info">
          <span>检测编码: {{ previewData.encoding }}</span>
          <span v-if="previewData.sheetName">Sheet: {{ previewData.sheetName }}</span>
          <span>总行数: {{ previewData.totalRows }}</span>
        </div>

        <!-- Sheet 选择（如有多个） -->
        <div v-if="previewData?.sheets && previewData.sheets.length > 1" class="sheet-select">
          <span>选择Sheet：</span>
          <a-select
            v-model:value="selectedSheet"
            style="width: 200px"
            :options="previewData.sheets.map((s) => ({ label: s, value: s }))"
          />
        </div>

        <!-- 预览表格 -->
        <div class="preview-table">
          <table>
            <tbody>
              <tr
                v-for="row in previewData?.rows"
                :key="row.rowNum"
                :class="{ selected: row.rowNum === selectedRow }"
                @click="selectedRow = row.rowNum"
              >
                <td class="row-num">
                  <a-radio :checked="row.rowNum === selectedRow" />
                  {{ row.rowNum }}
                </td>
                <td class="row-content">
                  <div class="cells">
                    <span v-for="(cell, idx) in row.cells" :key="idx" class="cell">
                      {{ cell || '(空)' }}
                    </span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 表头行选择 -->
        <div class="header-row-select">
          <span>表头所在行：</span>
          <a-input-number
            v-model:value="selectedRow"
            :min="1"
            :max="previewData?.totalRows || 100"
            style="width: 100px"
          />
          <span class="hint">点击行可快速选择</span>
        </div>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FilePreviewResponse } from './types'

const props = defineProps<{
  visible: boolean
  previewData: FilePreviewResponse | null
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void
  (e: 'confirm', headerRow: number, sheetName?: string): void
}>()

const selectedRow = ref(1)
const selectedSheet = ref<string>('')
const confirmLoading = ref(false)

// 监听 previewData 变化，重置选择
watch(
  () => props.previewData,
  (data) => {
    if (data) {
      selectedRow.value = 1
      selectedSheet.value = data.sheetName || ''
    }
  }
)

const handleConfirm = () => {
  confirmLoading.value = true
  emit('confirm', selectedRow.value, selectedSheet.value || undefined)
  confirmLoading.value = false
}

const handleCancel = () => {
  emit('update:visible', false)
}
</script>

<style lang="scss" scoped>
.file-preview-modal {
  .file-info {
    display: flex;
    gap: 24px;
    margin-bottom: 16px;
    padding: 8px 12px;
    background: #f5f5f5;
    border-radius: 4px;
    font-size: 13px;
    color: #666;
  }

  .sheet-select {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;
  }

  .preview-table {
    max-height: 400px;
    overflow: auto;
    border: 1px solid #d9d9d9;
    border-radius: 4px;

    table {
      width: 100%;
      border-collapse: collapse;

      tr {
        cursor: pointer;
        transition: background 0.2s;

        &:hover {
          background: rgba(24, 144, 255, 0.05);
        }

        &.selected {
          background: rgba(24, 144, 255, 0.1);

          .row-num {
            background: rgba(24, 144, 255, 0.2);
          }
        }
      }

      td {
        padding: 8px 12px;
        border-bottom: 1px solid #f0f0f0;
        vertical-align: top;
      }

      .row-num {
        width: 80px;
        background: #fafafa;
        font-weight: 500;
        text-align: center;
        white-space: nowrap;
      }

      .row-content {
        .cells {
          display: flex;
          flex-wrap: wrap;
          gap: 8px;

          .cell {
            padding: 2px 8px;
            background: #fff;
            border: 1px solid #e8e8e8;
            border-radius: 3px;
            font-size: 12px;
            max-width: 150px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }
    }
  }

  .header-row-select {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 16px;

    .hint {
      color: #999;
      font-size: 12px;
    }
  }
}
</style>
