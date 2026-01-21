<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h1 class="page-title">首页</h1>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stat-cards">
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="本季度收入"
            :value="1234567"
            :precision="2"
            prefix="¥"
            :value-style="{ color: '#3f8600' }"
          >
            <template #suffix>
              <span class="stat-trend up">+12.5%</span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="本季度退款"
            :value="45678"
            :precision="2"
            prefix="¥"
            :value-style="{ color: '#cf1322' }"
          >
            <template #suffix>
              <span class="stat-trend down">-5.2%</span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="本季度净收入"
            :value="1188889"
            :precision="2"
            prefix="¥"
            :value-style="{ color: '#3f8600' }"
          >
            <template #suffix>
              <span class="stat-trend up">+15.3%</span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="待处理任务"
            :value="3"
            :value-style="{ color: '#1890ff' }"
          />
        </a-card>
      </a-col>
    </a-row>

    <!-- 内容区 -->
    <a-row :gutter="16" class="content-row">
      <!-- 站点收入对比 -->
      <a-col :span="16">
        <a-card title="各站点收入对比">
          <div class="chart-container">
            <div class="chart-placeholder">
              图表区域 - 各站点收入柱状图
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- 快捷入口 -->
      <a-col :span="8">
        <a-card title="快捷入口">
          <div class="quick-links">
            <a-button type="primary" block class="quick-btn" @click="goTo('/sales/import')">
              <PlusOutlined /> 导入销售数据
            </a-button>
            <a-button block class="quick-btn" @click="goTo('/shipping/import')">
              <PlusOutlined /> 导入配送数据
            </a-button>
            <a-button block class="quick-btn" @click="goTo('/advertising/add')">
              <PlusOutlined /> 录入广告费
            </a-button>
            <a-button block class="quick-btn" @click="goTo('/report/summary')">
              <BarChartOutlined /> 生成季度报表
            </a-button>
            <a-button block class="quick-btn" @click="goTo('/report/download')">
              <DownloadOutlined /> 下载汇总数据
            </a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 最近导入记录 -->
    <a-card title="最近导入记录" class="recent-imports">
      <template #extra>
        <a @click="goTo('/config/import-record')">查看全部 ></a>
      </template>

      <a-table
        :columns="importColumns"
        :data-source="recentImports"
        :pagination="false"
        size="small"
      />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  PlusOutlined,
  BarChartOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue'

const router = useRouter()

function goTo(path: string) {
  router.push(path)
}

// 导入记录表格列
const importColumns = [
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '文件名', dataIndex: 'fileName', key: 'fileName' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '条数', dataIndex: 'count', key: 'count' },
  { title: '时间', dataIndex: 'time', key: 'time' }
]

// 模拟数据
const recentImports = ref([
  { key: '1', type: '销售数据', fileName: 'US_2025Q3.csv', status: '成功', count: '12,345', time: '2026-01-19' },
  { key: '2', type: '配送数据', fileName: 'Shipping_US.csv', status: '成功', count: '8,234', time: '2026-01-18' },
  { key: '3', type: '汇率数据', fileName: 'Rate_2025.xlsx', status: '部分', count: '365', time: '2026-01-17' }
])
</script>

<style lang="scss" scoped>
.dashboard-page {
  padding: $spacing-lg;

  .page-header {
    margin-bottom: $spacing-lg;

    .page-title {
      font-size: $font-size-xl;
      font-weight: 500;
      margin: 0;
    }
  }

  .stat-cards {
    margin-bottom: $spacing-lg;

    .stat-trend {
      font-size: 12px;
      margin-left: 8px;

      &.up {
        color: #52c41a;
      }

      &.down {
        color: #ff4d4f;
      }
    }
  }

  .content-row {
    margin-bottom: $spacing-lg;

    .chart-container {
      height: 300px;

      .chart-placeholder {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #fafafa;
        border-radius: 4px;
        color: #999;
      }
    }

    .quick-links {
      .quick-btn {
        margin-bottom: 12px;

        &:last-child {
          margin-bottom: 0;
        }
      }
    }
  }

  .recent-imports {
    :deep(a) {
      color: $primary-color;
    }
  }
}
</style>
