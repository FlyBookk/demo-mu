<template>
  <a-modal
    :open="visible"
    title="智能匹配结果"
    width="700px"
    @update:open="$emit('update:visible', $event)"
    @ok="handleApply"
    @cancel="handleCancel"
    okText="应用选中"
    cancelText="取消"
  >
    <div class="auto-match-preview">
      <!-- 统计信息 -->
      <div class="match-stats">
        <a-statistic-card>
          <a-row :gutter="16">
            <a-col :span="8">
              <a-statistic
                title="成功匹配"
                :value="result?.mappings.length || 0"
                :value-style="{ color: '#52c41a' }"
              >
                <template #suffix>个</template>
              </a-statistic>
            </a-col>
            <a-col :span="8">
              <a-statistic
                title="未匹配源字段"
                :value="result?.unmatchedSource.length || 0"
                :value-style="{ color: '#999' }"
              >
                <template #suffix>个</template>
              </a-statistic>
            </a-col>
            <a-col :span="8">
              <a-statistic
                title="未匹配目标字段"
                :value="result?.unmatchedTarget.length || 0"
                :value-style="{ color: '#faad14' }"
              >
                <template #suffix>个</template>
              </a-statistic>
            </a-col>
          </a-row>
        </a-statistic-card>
      </div>

      <!-- 匹配列表 -->
      <div class="match-list">
        <div class="list-header">
          <a-checkbox
            :checked="allSelected"
            :indeterminate="someSelected && !allSelected"
            @change="toggleSelectAll"
          >
            全选
          </a-checkbox>
        </div>

        <div class="match-items">
          <div
            v-for="item in result?.mappings"
            :key="item.source + item.target"
            class="match-item"
            :class="{ selected: selectedMappings.has(item.source) }"
          >
            <a-checkbox
              :checked="selectedMappings.has(item.source)"
              @change="toggleMapping(item.source)"
            />
            <div class="mapping-info">
              <span class="source">{{ item.source }}</span>
              <ArrowRightOutlined />
              <span class="target">{{ item.target }}</span>
            </div>
            <div class="match-meta">
              <a-tag :color="getConfidenceColor(item.confidence)">
                {{ (item.confidence * 100).toFixed(0) }}%
              </a-tag>
              <span class="match-type">{{ getMatchTypeLabel(item.matchType) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 未匹配字段 -->
      <a-collapse v-if="hasUnmatched" ghost>
        <a-collapse-panel key="unmatched" header="未匹配字段">
          <div class="unmatched-section">
            <div v-if="result?.unmatchedSource.length" class="unmatched-group">
              <div class="group-title">未匹配的源字段：</div>
              <div class="field-tags">
                <a-tag v-for="f in result.unmatchedSource" :key="f">{{ f }}</a-tag>
              </div>
            </div>
            <div v-if="result?.unmatchedTarget.length" class="unmatched-group">
              <div class="group-title">未匹配的目标字段：</div>
              <div class="field-tags">
                <a-tag v-for="f in result.unmatchedTarget" :key="f" color="orange">
                  {{ f }}
                </a-tag>
              </div>
            </div>
          </div>
        </a-collapse-panel>
      </a-collapse>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ArrowRightOutlined } from '@ant-design/icons-vue'
import type { AutoMatchResponse, MappingConfig, MatchType } from './types'

const props = defineProps<{
  visible: boolean
  result: AutoMatchResponse | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void
  (e: 'apply', mappings: MappingConfig[]): void
}>()

const selectedMappings = ref<Set<string>>(new Set())

// 监听结果变化，默认全选
watch(
  () => props.result,
  (result) => {
    if (result) {
      selectedMappings.value = new Set(result.mappings.map((m) => m.source))
    }
  },
  { immediate: true }
)

const allSelected = computed(() => {
  if (!props.result) return false
  return selectedMappings.value.size === props.result.mappings.length
})

const someSelected = computed(() => {
  return selectedMappings.value.size > 0
})

const hasUnmatched = computed(() => {
  if (!props.result) return false
  return props.result.unmatchedSource.length > 0 || props.result.unmatchedTarget.length > 0
})

const toggleSelectAll = () => {
  if (allSelected.value) {
    selectedMappings.value = new Set()
  } else {
    selectedMappings.value = new Set(props.result?.mappings.map((m) => m.source) || [])
  }
}

const toggleMapping = (source: string) => {
  const newSet = new Set(selectedMappings.value)
  if (newSet.has(source)) {
    newSet.delete(source)
  } else {
    newSet.add(source)
  }
  selectedMappings.value = newSet
}

const getConfidenceColor = (confidence: number) => {
  if (confidence >= 0.9) return 'green'
  if (confidence >= 0.7) return 'blue'
  return 'orange'
}

const getMatchTypeLabel = (type: MatchType) => {
  const map: Record<MatchType, string> = {
    exact: '精确匹配',
    ignore_case: '忽略大小写',
    alias: '别名匹配',
    label: '标签匹配',
    normalized: '规范化匹配',
    contains: '包含匹配',
    similar: '相似匹配'
  }
  return map[type] || type
}

const handleApply = () => {
  if (!props.result) return

  const mappings: MappingConfig[] = props.result.mappings
    .filter((m) => selectedMappings.value.has(m.source))
    .map((m) => ({
      source: m.source,
      target: m.target
    }))

  emit('apply', mappings)
  emit('update:visible', false)
}

const handleCancel = () => {
  emit('update:visible', false)
}
</script>

<style lang="scss" scoped>
.auto-match-preview {
  .match-stats {
    margin-bottom: 16px;
    padding: 16px;
    background: #fafafa;
    border-radius: 8px;
  }

  .match-list {
    .list-header {
      padding: 8px 0;
      border-bottom: 1px solid #f0f0f0;
    }

    .match-items {
      max-height: 300px;
      overflow-y: auto;
    }

    .match-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 8px;
      border-bottom: 1px solid #f0f0f0;
      transition: background 0.2s;

      &:hover {
        background: #fafafa;
      }

      &.selected {
        background: rgba(24, 144, 255, 0.05);
      }

      .mapping-info {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 8px;

        .source {
          color: #666;
        }

        .target {
          font-weight: 500;
        }
      }

      .match-meta {
        display: flex;
        align-items: center;
        gap: 8px;

        .match-type {
          color: #999;
          font-size: 12px;
        }
      }
    }
  }

  .unmatched-section {
    .unmatched-group {
      margin-bottom: 12px;

      .group-title {
        margin-bottom: 8px;
        font-weight: 500;
      }

      .field-tags {
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
      }
    }
  }
}
</style>
