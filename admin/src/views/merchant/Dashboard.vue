<script setup lang="ts">
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
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

    <div class="overview-grid">
      <el-card class="metric-card">
        <span>当前角色</span>
        <strong>merchant</strong>
        <p>店铺经营权限</p>
      </el-card>
      <el-card class="metric-card">
        <span>商家 ID</span>
        <strong>{{ userStore.merchantId ?? '-' }}</strong>
        <p>用于关联商品与订单</p>
      </el-card>
      <el-card class="metric-card">
        <span>待办提醒</span>
        <strong>履约 / 售后</strong>
        <p>关注发货与退款处理</p>
      </el-card>
    </div>

    <el-card class="workbench-card">
      <template #header>
        <div class="card-header">
          <span>店铺工作台</span>
          <el-tag type="primary">M1</el-tag>
        </div>
      </template>
      <div class="token-row">
        <span>Token 前 20 位</span>
        <code>{{ userStore.token.slice(0, 20) }}...</code>
      </div>
      <el-alert
        title="M1 阶段：已完成登录与基础脚手架，商品、订单、售后能力将持续完善。"
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

.token-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #fff8ed;
}

.token-row span {
  color: var(--shop-text-muted);
  font-size: 13px;
  font-weight: 700;
}

.token-row code {
  color: var(--shop-primary-dark);
  font-weight: 800;
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
