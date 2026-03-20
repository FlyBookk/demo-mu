<template>
  <div class="data-clean-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">数据清理</h1>
      <p class="page-desc">按模块清理当前店铺的业务数据，清理后数据不可恢复，请谨慎操作</p>
    </div>

    <!-- 模块卡片列表 -->
    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :sm="12" :lg="8" v-for="item in modules" :key="item.moduleCode">
          <a-card hoverable class="module-card">
            <div class="module-info">
              <div class="module-header">
                <span class="module-name">{{ item.moduleName }}</span>
                <a-tag :color="item.dataCount > 0 ? 'blue' : 'default'">
                  {{ item.dataCount }} 条
                </a-tag>
              </div>
              <p class="module-desc">{{ item.description }}</p>
            </div>
            <div class="module-action">
              <a-button
                danger
                :disabled="item.dataCount === 0"
                :loading="cleaningModule === item.moduleCode"
                @click="openCleanDialog(item)"
              >
                <DeleteOutlined /> 清理数据
              </a-button>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>

    <!-- 站点选择对话框 -->
    <a-modal
      v-model:open="dialogVisible"
      title="选择清理范围"
      :confirm-loading="cleaningModule !== null"
      ok-text="确定清理"
      cancel-text="取消"
      ok-type="danger"
      @ok="handleConfirmClean"
      @cancel="handleCancelClean"
    >
      <div class="clean-dialog-content">
        <a-alert
          :message="`即将清理「${currentModule?.moduleName}」数据，此操作不可恢复！`"
          type="warning"
          show-icon
          style="margin-bottom: 16px"
        />
        <a-form layout="vertical">
          <a-form-item label="选择站点">
            <a-select
              v-model:value="selectedSiteCode"
              placeholder="不选择则清理所有站点数据"
              allow-clear
              style="width: 100%"
            >
              <a-select-option
                v-for="marketplace in marketplaceOptions"
                :key="marketplace.siteCode"
                :value="marketplace.siteCode"
              >
                {{ marketplace.siteCode }} - {{ marketplace.siteName }}
              </a-select-option>
            </a-select>
            <div class="site-hint">
              <span v-if="selectedSiteCode" class="hint-text hint-site">
                仅清理 {{ selectedSiteCode }} 站点下的数据
              </span>
              <span v-else class="hint-text hint-all">
                清理当前店铺所有站点的数据
              </span>
            </div>
          </a-form-item>
        </a-form>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'
import { getCleanModules, cleanModule, type DataCleanModule } from '@/api/dataClean'
import { getEnabledMarketplaces } from '@/api/marketplace'
import type { Marketplace } from '@/types/marketplace'

const loading = ref(false)
const modules = ref<DataCleanModule[]>([])
const cleaningModule = ref<string | null>(null)

// 站点选择对话框
const dialogVisible = ref(false)
const currentModule = ref<DataCleanModule | null>(null)
const selectedSiteCode = ref<string | undefined>(undefined)
const marketplaceOptions = ref<Marketplace[]>([])

/** 获取模块列表 */
async function fetchModules() {
  loading.value = true
  try {
    const res = await getCleanModules()
    modules.value = res.data || []
  } catch (error) {
    console.error('获取清理模块失败:', error)
  } finally {
    loading.value = false
  }
}

/** 获取站点列表 */
async function fetchMarketplaces() {
  try {
    const res = await getEnabledMarketplaces()
    marketplaceOptions.value = res.data || []
  } catch (error) {
    console.error('获取站点列表失败:', error)
  }
}

/** 打开清理对话框 */
function openCleanDialog(item: DataCleanModule) {
  currentModule.value = item
  selectedSiteCode.value = undefined
  dialogVisible.value = true
}

/** 取消清理 */
function handleCancelClean() {
  dialogVisible.value = false
  currentModule.value = null
  selectedSiteCode.value = undefined
}

/** 确认清理 */
async function handleConfirmClean() {
  if (!currentModule.value) return
  const item = currentModule.value
  cleaningModule.value = item.moduleCode
  try {
    const res = await cleanModule(item.moduleCode, selectedSiteCode.value)
    const siteLabel = selectedSiteCode.value ? `${selectedSiteCode.value} 站点` : '所有站点'
    message.success(`已清理「${item.moduleName}」${siteLabel} ${res.data ?? 0} 条数据`)
    dialogVisible.value = false
    currentModule.value = null
    selectedSiteCode.value = undefined
    await fetchModules()
  } catch (error) {
    console.error('清理失败:', error)
  } finally {
    cleaningModule.value = null
  }
}

onMounted(async () => {
  await Promise.all([fetchModules(), fetchMarketplaces()])
})
</script>

<style lang="scss" scoped>
.data-clean-page {
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

  .module-card {
    height: 100%;
    display: flex;
    flex-direction: column;

    :deep(.ant-card-body) {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }

    .module-info {
      margin-bottom: 16px;

      .module-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;

        .module-name {
          font-size: 16px;
          font-weight: 600;
          color: $text-color;
        }
      }

      .module-desc {
        font-size: 13px;
        color: $text-color-secondary;
        margin: 0;
      }
    }

    .module-action {
      text-align: right;
    }
  }
}

.clean-dialog-content {
  .site-hint {
    margin-top: 6px;
    font-size: 12px;

    .hint-text {
      &.hint-site {
        color: #fa8c16;
      }
      &.hint-all {
        color: #ff4d4f;
      }
    }
  }
}
</style>
