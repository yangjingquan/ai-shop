<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { dashboardApi, type SettlementAnalysis } from '@/api/dashboard'

const loading = ref(false)
const report = ref<SettlementAnalysis | null>(null)
const loadError = ref('')
function money(value?: number) { return `¥${Number(value || 0).toFixed(2)}` }
function time(value?: string) { return value ? value.replace('T', ' ').slice(0, 19) : '-' }
async function load() {
  loading.value = true
  loadError.value = ''
  try { report.value = await dashboardApi.merchantSettlementAnalysis() }
  catch (error: any) { report.value = null; loadError.value = error?.message || '结算数据暂时无法加载，请稍后重试。' }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <div class="page" v-loading="loading">
    <div class="page-header"><div><span class="page-kicker">SETTLEMENT CENTER</span><h1 class="page-title">结算中心</h1><p class="page-desc">按支付、退款与结算规则逐项核对；经营净额不会被当作可提现金额。</p></div><el-button @click="load">刷新</el-button></div>
    <el-alert v-if="loadError" :title="loadError" description="数据未加载时不会展示或推断可结算金额。请在后端升级完成后刷新重试。" type="error" :closable="false" show-icon class="notice" />
    <el-alert v-if="report" :title="report.settlementNotice" type="warning" :closable="false" show-icon class="notice" />
    <div class="metric-grid">
      <el-card><span>GMV（下单）</span><strong>{{ money(report?.createdGmv) }}</strong><small>{{ report?.createdOrderCount || 0 }} 笔，按创建时间</small></el-card>
      <el-card><span>支付金额</span><strong>{{ money(report?.paidAmount) }}</strong><small>{{ report?.paidOrderCount || 0 }} 笔，按支付入账</small></el-card>
      <el-card><span>退款成功</span><strong>{{ money(report?.successfulRefundAmount) }}</strong><small>按退款成功时间</small></el-card>
      <el-card><span>经营净额</span><strong>{{ money(report?.operatingNetAmount) }}</strong><small>仅支付 - 成功退款</small></el-card>
      <el-card class="hold"><span>可结算金额</span><strong>{{ money(report?.provisionalSettlementAmount) }}</strong><small>{{ report?.settlementStatus || '-' }}</small></el-card>
    </div>
    <el-card v-if="report" class="detail"><template #header>口径与数据新鲜度</template><el-descriptions :column="1" border><el-descriptions-item label="统计区间">{{ time(report.rangeStart) }} 至 {{ time(report.rangeEnd) }}</el-descriptions-item><el-descriptions-item label="数据截至">{{ time(report.dataAsOf) }}（{{ report.timezone }}）</el-descriptions-item><el-descriptions-item label="处理中售后">{{ money(report.processingRefundAmount) }}</el-descriptions-item><el-descriptions-item label="口径">{{ report.metricDefinition }}</el-descriptions-item></el-descriptions></el-card>
  </div>
</template>

<style scoped>
.notice { margin-bottom: 16px; }.metric-grid { display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:14px; margin-bottom:16px; }.metric-grid span,.metric-grid strong,.metric-grid small{display:block}.metric-grid span,.metric-grid small{color:var(--shop-text-muted);font-size:12px}.metric-grid strong{margin:10px 0;font-size:23px}.hold strong{color:var(--shop-warning)}@media(max-width:1000px){.metric-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
