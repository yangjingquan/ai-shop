<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { dashboardApi, type DashboardOverview } from '@/api/dashboard'

const loading = ref(false)
const overview = ref<DashboardOverview | null>(null)

async function load() {
  loading.value = true
  try {
    overview.value = await dashboardApi.adminOverview()
  } finally {
    loading.value = false
  }
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

    <el-card class="workbench-card">
      <template #header>
        <div class="card-header">
          <span>工作台状态</span>
          <el-tag type="primary">M1</el-tag>
        </div>
      </template>
      <el-alert
        title="指标用于平台日常巡检；资金退款仍需以真实退款回调和对账结果为准。"
        type="info"
        :closable="false"
        show-icon
      />
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

.workbench-card {
  max-width: 840px;
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
