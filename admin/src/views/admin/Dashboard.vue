<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { dashboardApi, type DashboardOverview, type DashboardTrendRow } from '@/api/dashboard'

const loading = ref(false)
const overview = ref<DashboardOverview | null>(null)
const trend = ref<DashboardTrendRow[]>([])
const trendDays = ref(30)

async function load() {
  loading.value = true
  try {
    const [overviewData, trendData] = await Promise.all([
      dashboardApi.adminOverview(),
      dashboardApi.adminTrend(trendDays.value),
    ])
    overview.value = overviewData
    trend.value = trendData
  } finally {
    loading.value = false
  }
}

async function changeTrendDays() {
  trend.value = await dashboardApi.adminTrend(trendDays.value)
}

function exportTrend() {
  const header = ['日期', '支付订单数', '支付金额', '退款金额', '净收入']
  const rows = trend.value.map((row) => [row.date, row.paidOrderCount, row.paidAmount, row.refundAmount, row.netAmount])
  const csv = '\ufeff' + [header, ...rows].map((row) => row.join(',')).join('\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `平台经营趋势-${trendDays.value}天.csv`
  anchor.click()
  URL.revokeObjectURL(url)
}

function money(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

onMounted(load)
</script>

<template>
  <div class="dashboard">
    <div class="page-header">
      <div>
        <span class="page-kicker">ADMIN OVERVIEW</span>
        <h1 class="page-title">运营后台首页</h1>
        <p class="page-desc">快速查看平台经营入口、商家管理与分类维护进度。</p>
      </div>
    </div>

    <div v-loading="loading" class="overview-grid">
      <el-card class="metric-card">
        <span>启用商家</span>
        <strong>{{ overview?.activeMerchantCount ?? '-' }}</strong>
        <p>共 {{ overview?.merchantCount ?? '-' }} 家商家</p>
      </el-card>
      <el-card class="metric-card">
        <span>今日订单</span>
        <strong>{{ overview?.orderCountToday ?? '-' }}</strong>
        <p>平台今日创建订单</p>
      </el-card>
      <el-card class="metric-card">
        <span>今日支付金额</span>
        <strong>{{ overview ? money(overview.paidAmountToday) : '-' }}</strong>
        <p>按支付时间统计</p>
      </el-card>
      <el-card class="metric-card">
        <span>待发货</span>
        <strong>{{ overview?.pendingShipCount ?? '-' }}</strong>
        <p>包含普通单和成团订单</p>
      </el-card>
      <el-card class="metric-card">
        <span>待处理退款</span>
        <strong>{{ overview?.pendingRefundCount ?? '-' }}</strong>
        <p>仅统计待审核申请</p>
      </el-card>
      <el-card class="metric-card">
        <span>库存预警 SKU</span>
        <strong>{{ overview?.lowStockSkuCount ?? '-' }}</strong>
        <p>库存不高于 5 件</p>
      </el-card>
    </div>

    <el-card class="trend-card">
      <template #header>
        <div class="card-header">
          <span>经营趋势</span>
          <div class="trend-actions">
            <el-select v-model="trendDays" style="width: 120px" @change="changeTrendDays">
              <el-option label="最近 7 天" :value="7" />
              <el-option label="最近 30 天" :value="30" />
              <el-option label="最近 90 天" :value="90" />
            </el-select>
            <el-button @click="exportTrend">导出 CSV</el-button>
          </div>
        </div>
      </template>
      <el-table :data="trend" max-height="420" stripe>
        <el-table-column prop="date" label="日期" min-width="130" />
        <el-table-column prop="paidOrderCount" label="支付订单" min-width="110" />
        <el-table-column label="支付金额" min-width="130"><template #default="{ row }">{{ money(row.paidAmount) }}</template></el-table-column>
        <el-table-column label="退款金额" min-width="130"><template #default="{ row }">{{ money(row.refundAmount) }}</template></el-table-column>
        <el-table-column label="净收入" min-width="130"><template #default="{ row }"><strong :class="{ negative: row.netAmount < 0 }">{{ money(row.netAmount) }}</strong></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.metric-card :deep(.el-card__body) {
  min-height: 136px;
}

.metric-card span,
.metric-card strong,
.metric-card p {
  display: block;
}

.metric-card span {
  color: var(--shop-text-muted);
  font-size: 13px;
  font-weight: 700;
}

.metric-card strong {
  margin-top: 12px;
  color: var(--shop-text);
  font-size: 26px;
  line-height: 1;
  font-weight: 900;
}

.metric-card p {
  margin-top: 12px;
  color: var(--shop-text-muted);
  font-size: 13px;
}

.trend-card { width: 100%; }
.trend-actions { display: flex; align-items: center; gap: 10px; }
.negative { color: var(--shop-danger); }

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
