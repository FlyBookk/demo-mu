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
              <a-popconfirm
                :title="`确定要清理「${item.moduleName}」的所有数据吗？此操作不可恢复！`"
                ok-text="确定清理"
                cancel-text="取消"
                ok-type="danger"
                @confirm="handleClean(item)"
              >
                <a-button
                  danger
                  :disabled="item.dataCount === 0"
                  :loading="cleaningModule === item.moduleCode"
                >
                  <DeleteOutlined /> 清理数据
                </a-button>
              </a-popconfirm>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'
import { getCleanModules, cleanModule, type DataCleanModule } from '@/api/dataClean'

const loading = ref(false)
const modules = ref<DataCleanModule[]>([])
const cleaningModule = ref<string | null>(null)

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

/** 执行清理 */
async function handleClean(item: DataCleanModule) {
  cleaningModule.value = item.moduleCode
  try {
    const res = await cleanModule(item.moduleCode)
    message.success(`已清理「${item.moduleName}」${res.data ?? 0} 条数据`)
    // 刷新数据量
    await fetchModules()
  } catch (error) {
    console.error('清理失败:', error)
  } finally {
    cleaningModule.value = null
  }
}

onMounted(() => {
  fetchModules()
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
</style>
