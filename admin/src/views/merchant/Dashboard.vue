<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { dashboardApi, type DashboardOverview } from '@/api/dashboard'
const loading = ref(false)
const overview = ref<DashboardOverview | null>(null)

async function load() {
  loading.value = true
  try {
    overview.value = await dashboardApi.merchantOverview()
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
        <span class="page-kicker">MERCHANT CENTER</span>
        <h1 class="page-title">商家管理后台首页</h1>
        <p class="page-desc">维护店铺资料、商品上新、订单发货与售后审批。</p>
      </div>
    </div>

    <div v-loading="loading" class="overview-grid">
      <el-card class="metric-card">
        <span>今日订单</span>
        <strong>{{ overview?.orderCountToday ?? '-' }}</strong>
        <p>今日创建订单</p>
      </el-card>
      <el-card class="metric-card">
        <span>今日销售额</span>
        <strong>{{ overview ? money(overview.paidAmountToday) : '-' }}</strong>
        <p>按支付时间统计</p>
      </el-card>
      <el-card class="metric-card">
        <span>待办提醒</span>
        <strong>{{ (overview?.pendingShipCount ?? 0) + (overview?.pendingRefundCount ?? 0) }}</strong>
        <p>待发货 {{ overview?.pendingShipCount ?? '-' }} · 待退款 {{ overview?.pendingRefundCount ?? '-' }}</p>
      </el-card>
      <el-card class="metric-card">
        <span>在售商品</span>
        <strong>{{ overview?.onSaleProductCount ?? '-' }}</strong>
        <p>审核通过且已上架</p>
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
          <span>店铺工作台</span>
          <el-tag type="primary">M1</el-tag>
        </div>
      </template>
      <el-alert
        title="请优先处理待发货、待退款和库存预警；退款金额以支付渠道最终结果为准。"
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
