<script setup lang="ts">
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
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

    <div class="overview-grid">
      <el-card class="metric-card">
        <span>当前角色</span>
        <strong>admin</strong>
        <p>平台运营权限</p>
      </el-card>
      <el-card class="metric-card">
        <span>商家服务</span>
        <strong>入驻管理</strong>
        <p>维护商家账号与状态</p>
      </el-card>
      <el-card class="metric-card">
        <span>类目体系</span>
        <strong>平台分类</strong>
        <p>管理商品展示层级</p>
      </el-card>
    </div>

    <el-card class="workbench-card">
      <template #header>
        <div class="card-header">
          <span>工作台状态</span>
          <el-tag type="primary">M1</el-tag>
        </div>
      </template>
      <div class="token-row">
        <span>Token 前 20 位</span>
        <code>{{ userStore.token.slice(0, 20) }}...</code>
      </div>
      <el-alert
        title="M1 阶段：已完成登录与基础脚手架，后续业务模块将继续完善。"
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
