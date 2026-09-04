<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardApi, type MerchantWorkbench, type MerchantWorkbenchOrder } from '@/api/dashboard'

const router = useRouter()
const loading = ref(false)
const workbench = ref<MerchantWorkbench | null>(null)

const overview = computed(() => workbench.value?.overview)
const todo = computed(() => workbench.value?.todo)
const hasTodo = computed(() => {
  const value = todo.value
  return Boolean(value && (
    value.pendingShipCount > 0
    || value.pendingRefundCount > 0
    || value.pendingReturnReceiveCount > 0
    || value.failedRefundCount > 0
    || value.lowStockSkuCount > 0
  ))
})

async function load() {
  loading.value = true
  try {
    workbench.value = await dashboardApi.merchantWorkbench()
  } finally {
    loading.value = false
  }
}

function money(value?: number | string) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function orderTagType(status: number): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  if (status === 1 || status === 5) return 'warning'
  if (status === 2 || status === 6) return 'primary'
  if (status === 3) return 'success'
  if (status === 4 || status === 7) return 'danger'
  return 'info'
}

function go(path: string, query?: Record<string, string>) {
  router.push({ path, query })
}

function goOrder(row?: MerchantWorkbenchOrder) {
  if (row && (row.status === 1 || row.status === 6)) {
    go('/merchant/order-ship', { scope: 'shipable' })
    return
  }
  go('/merchant/order-ship')
}

onMounted(load)
</script>

<template>
  <div class="dashboard">
    <div class="page-header dashboard-header">
      <div>
        <span class="page-kicker">MERCHANT CENTER</span>
        <h1 class="page-title">商家管理后台首页</h1>
        <p class="page-desc">维护店铺资料、商品上新、订单发货与售后审批。</p>
      </div>
      <div class="dashboard-header-actions">
        <span v-if="workbench?.generatedAt" class="refresh-time">
          更新于 {{ formatTime(workbench.generatedAt) }}
        </span>
        <el-button :loading="loading" @click="load">刷新数据</el-button>
      </div>
    </div>

    <div v-loading="loading" class="dashboard-content">
      <div class="overview-grid">
        <el-card class="metric-card">
          <span>今日支付订单</span>
          <strong>{{ overview?.paidOrderCountToday ?? '-' }}</strong>
          <p>按支付成功时间统计</p>
        </el-card>
        <el-card class="metric-card">
          <span>今日支付金额</span>
          <strong>{{ overview ? money(overview.paidAmountToday) : '-' }}</strong>
          <p>支付成功金额，按支付流水统计</p>
        </el-card>
        <el-card class="metric-card">
          <span>今日净额</span>
          <strong>{{ overview ? money(overview.netAmountToday) : '-' }}</strong>
          <p>支付成功金额 - 退款成功金额</p>
        </el-card>
        <el-card class="metric-card">
          <span>待办提醒</span>
          <strong>{{ overview ? (overview.pendingShipCount + overview.pendingRefundCount) : '-' }}</strong>
          <p>待发货 {{ overview?.pendingShipCount ?? '-' }} · 待退款 {{ overview?.pendingRefundCount ?? '-' }}</p>
        </el-card>
        <el-card class="metric-card">
          <span>在售商品</span>
          <strong>{{ overview?.onSaleProductCount ?? '-' }}</strong>
          <p>已上架销售</p>
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
            <div>
              <span>店铺工作台</span>
              <small>优先处理影响履约和资金安全的事项</small>
            </div>
            <el-tag type="success">实时聚合</el-tag>
          </div>
        </template>

        <div class="todo-heading">
          <div>
            <h2>待办中心</h2>
            <p>点击卡片即可进入对应列表处理</p>
          </div>
          <span v-if="!hasTodo" class="todo-clear">当前暂无紧急待办</span>
        </div>

        <div class="todo-grid">
          <button class="todo-item" type="button" @click="go('/merchant/order-ship', { scope: 'shipable' })">
            <span class="todo-dot todo-dot-warning" />
            <span class="todo-copy">
              <span>待发货</span>
              <strong>{{ todo?.pendingShipCount ?? '-' }}</strong>
              <small>含普通订单与已成团订单</small>
            </span>
            <span class="todo-arrow">›</span>
          </button>
          <button class="todo-item" type="button" @click="go('/merchant/refund-review', { status: '0' })">
            <span class="todo-dot todo-dot-danger" />
            <span class="todo-copy">
              <span>待退款审批</span>
              <strong>{{ todo?.pendingRefundCount ?? '-' }}</strong>
              <small>等待商家审核的申请</small>
            </span>
            <span class="todo-arrow">›</span>
          </button>
          <button class="todo-item" type="button" @click="go('/merchant/refund-review', { status: '6' })">
            <span class="todo-dot todo-dot-primary" />
            <span class="todo-copy">
              <span>待退货验货</span>
              <strong>{{ todo?.pendingReturnReceiveCount ?? '-' }}</strong>
              <small>确认收货后发起退款</small>
            </span>
            <span class="todo-arrow">›</span>
          </button>
          <button class="todo-item" type="button" @click="go('/merchant/inventory', { lowStockOnly: 'true', threshold: '5' })">
            <span class="todo-dot todo-dot-warning" />
            <span class="todo-copy">
              <span>库存预警</span>
              <strong>{{ todo?.lowStockSkuCount ?? '-' }}</strong>
              <small>库存不高于 5 件的 SKU</small>
            </span>
            <span class="todo-arrow">›</span>
          </button>
        </div>

        <el-alert
          v-if="todo?.failedRefundCount"
          class="exception-alert"
          :title="`${todo.failedRefundCount} 笔退款失败，请进入退款审批重试`"
          type="error"
          :closable="false"
          show-icon
          @click="go('/merchant/refund-review', { status: '4' })"
        />

        <div class="workbench-layout">
          <el-card class="inner-card recent-orders-card" shadow="never">
            <template #header>
              <div class="inner-card-header">
                <span>最近订单</span>
                <el-button link type="primary" @click="go('/merchant/order-ship')">查看全部</el-button>
              </div>
            </template>
            <el-table v-if="workbench?.recentOrders?.length" :data="workbench.recentOrders" size="small">
              <el-table-column prop="orderNo" label="订单号" min-width="175" show-overflow-tooltip />
              <el-table-column label="状态" width="105">
                <template #default="{ row }">
                  <el-tag size="small" :type="orderTagType(row.status)">{{ row.statusText }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="金额" width="110">
                <template #default="{ row }">{{ money(row.payAmount) }}</template>
              </el-table-column>
              <el-table-column label="时间" width="135">
                <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="80" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="goOrder(row as MerchantWorkbenchOrder)">处理</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂无订单" :image-size="58" />
          </el-card>

          <el-card class="inner-card stock-card" shadow="never">
            <template #header>
              <div class="inner-card-header">
                <span>库存预警明细</span>
                <el-button link type="primary" @click="go('/merchant/inventory', { lowStockOnly: 'true', threshold: '5' })">查看全部</el-button>
              </div>
            </template>
            <div v-if="workbench?.lowStockSkus?.length" class="stock-list">
              <div v-for="sku in workbench.lowStockSkus" :key="sku.skuId" class="stock-row">
                <div class="stock-copy">
                  <strong>{{ sku.productName }}</strong>
                  <span>{{ sku.specText || sku.skuCode || '默认规格' }}</span>
                </div>
                <el-tag type="danger" size="small">{{ sku.stock }} 件</el-tag>
              </div>
            </div>
            <el-empty v-else description="库存充足" :image-size="58" />
          </el-card>
        </div>

        <div class="quick-actions">
          <div class="quick-actions-heading">
            <div>
              <h2>快捷操作</h2>
              <p>常用店铺管理入口</p>
            </div>
          </div>
          <div class="quick-action-grid">
            <el-button type="primary" @click="go('/merchant/products/edit')">新增商品</el-button>
            <el-button @click="go('/merchant/inventory')">调整库存</el-button>
            <el-button @click="go('/merchant/banners')">配置 Banner</el-button>
            <el-button @click="go('/merchant/profile')">店铺资料</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.dashboard-header-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 18px;
}

.refresh-time {
  color: var(--shop-text-muted);
  font-size: 12px;
  white-space: nowrap;
}

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
  width: 100%;
}

.card-header > div {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.card-header small {
  color: var(--shop-text-muted);
  font-size: 12px;
  font-weight: 500;
}

.todo-heading,
.quick-actions-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.todo-heading h2,
.quick-actions-heading h2 {
  color: var(--shop-text);
  font-size: 16px;
  font-weight: 800;
}

.todo-heading p,
.quick-actions-heading p {
  margin-top: 5px;
  color: var(--shop-text-muted);
  font-size: 12px;
}

.todo-clear {
  color: var(--shop-success);
  font-size: 12px;
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.todo-item {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 15px 14px;
  border: 1px solid var(--shop-border);
  border-radius: 16px;
  color: var(--shop-text);
  text-align: left;
  background: linear-gradient(135deg, #fffdf9, #fff8ed);
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.todo-item:hover {
  border-color: rgba(216, 111, 34, 0.42);
  box-shadow: var(--shop-shadow-soft);
  transform: translateY(-1px);
}

.todo-dot {
  flex: 0 0 auto;
  width: 9px;
  height: 9px;
  margin-right: 11px;
  border-radius: 999px;
}

.todo-dot-warning { background: var(--shop-warning); }
.todo-dot-danger { background: var(--shop-danger); }
.todo-dot-primary { background: #4b82d8; }

.todo-copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
}

.todo-copy > span {
  font-size: 13px;
  font-weight: 800;
}

.todo-copy strong {
  margin-top: 5px;
  font-size: 22px;
  line-height: 1;
}

.todo-copy small {
  overflow: hidden;
  margin-top: 7px;
  color: var(--shop-text-muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-arrow {
  margin-left: 8px;
  color: var(--shop-primary);
  font-size: 22px;
  line-height: 1;
}

.exception-alert {
  margin-top: 14px;
  cursor: pointer;
}

.workbench-layout {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 16px;
  margin-top: 18px;
}

.inner-card {
  border-radius: 16px !important;
  background: rgba(255, 253, 248, 0.72) !important;
  box-shadow: none !important;
}

.inner-card :deep(.el-card__header) {
  padding: 14px 16px !important;
}

.inner-card :deep(.el-card__body) {
  padding: 10px 16px 14px !important;
}

.inner-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 800;
}

.recent-orders-card :deep(.el-table) {
  --el-table-border-color: transparent;
  --el-table-row-hover-bg-color: #fff7e9;
  background: transparent;
}

.recent-orders-card :deep(.el-table th.el-table__cell),
.recent-orders-card :deep(.el-table td.el-table__cell) {
  background: transparent;
}

.stock-list {
  display: flex;
  flex-direction: column;
}

.stock-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(234, 223, 206, 0.72);
}

.stock-row:last-child {
  border-bottom: 0;
}

.stock-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.stock-copy strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stock-copy span {
  overflow: hidden;
  margin-top: 4px;
  color: var(--shop-text-muted);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-actions {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--shop-border);
}

.quick-actions-heading {
  margin-bottom: 12px;
}

.quick-action-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

@media (max-width: 1100px) {
  .todo-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .dashboard-header-actions {
    width: 100%;
    justify-content: space-between;
    margin-top: 0;
  }

  .overview-grid,
  .todo-grid,
  .workbench-layout {
    grid-template-columns: 1fr;
  }
}
</style>
