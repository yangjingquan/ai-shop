<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminOrderApi, type AdminRefundRow, type ReconciliationTask } from '@/api/order'

const loading = ref(false)
const reconciling = ref(false)
const list = ref<AdminRefundRow[]>([])
const total = ref(0)
const tasks = ref<ReconciliationTask[]>([])
const query = reactive({ page: 1, size: 10, status: undefined as number | undefined, merchantId: undefined as number | undefined })
const statusText: Record<number, string> = { 0: '待处理', 1: '退款处理中', 2: '已拒绝', 3: '退款成功', 4: '退款失败', 5: '待填写退货物流', 6: '待商家验货' }

function statusTagType(status?: number) {
  if (status === 0 || status === 5) return 'warning'
  if (status === 1 || status === 6) return 'primary'
  if (status === 3) return 'success'
  return 'danger'
}

function resolveImageUrl(url?: string) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  const configuredBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'
  const base = configuredBase.endsWith('/') ? configuredBase.slice(0, -1) : configuredBase
  return `${base}${url.startsWith('/') ? url : `/${url}`}`
}

async function fetchList() {
  loading.value = true
  try {
    const data = await adminOrderApi.refunds({ page: query.page, size: query.size, status: query.status, merchantId: query.merchantId })
    list.value = data.list
    total.value = data.total
    tasks.value = await adminOrderApi.reconciliationTasks('REFUND')
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; fetchList() }
async function reconcile() {
  reconciling.value = true
  try {
    const preview = await adminOrderApi.previewReconciliation('REFUND')
    await ElMessageBox.confirm(`本次将检查最多 ${preview.affectedCount} 笔处理中或自动退款记录，生成可审计任务；不会代替商家审批。`, '确认发起退款对账', { type: 'warning', confirmButtonText: '生成任务并执行' })
    const result = await adminOrderApi.createReconciliation('REFUND')
    ElMessage.success(`对账任务 ${result.taskNo} 已${result.status === 'SUCCESS' ? '完成' : '创建'}`)
    await fetchList()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '创建对账任务失败')
  } finally {
    reconciling.value = false
  }
}
onMounted(fetchList)
</script>

<template>
  <div class="page">
    <div class="page-header"><div><span class="page-kicker">REFUND MONITOR</span><h1 class="page-title">平台退款</h1><p class="page-desc">查看退款申请、微信资金状态与主动对账结果；审批仍由所属商家处理。</p></div><el-button type="primary" :loading="reconciling" @click="reconcile">预览并发起对账</el-button></div>
    <el-alert title="对账先展示影响范围并生成幂等任务、执行日志和异常信息；不会代替商家审批普通售后。" type="info" :closable="false" show-icon class="notice" />
    <el-card>
      <div class="toolbar">
        <el-input-number v-model="query.merchantId" :min="1" :controls="false" placeholder="商家 ID" style="width: 150px" />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="search"><el-option v-for="(label, value) in statusText" :key="value" :label="label" :value="Number(value)" /></el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column prop="outRefundNo" label="商户退款单号" min-width="220" show-overflow-tooltip />
        <el-table-column prop="merchantName" label="商家" width="150" />
        <el-table-column label="来源" width="100"><template #default="{ row }"><el-tag :type="row.autoRefund ? 'primary' : 'info'">{{ row.autoRefund ? '系统自动' : '用户申请' }}</el-tag></template></el-table-column>
        <el-table-column prop="reason" label="申请原因" min-width="240" show-overflow-tooltip />
        <el-table-column prop="refundAmount" label="退款金额" width="110" />
        <el-table-column label="状态" width="130"><template #default="{ row }"><el-tag :type="statusTagType(row.status)">{{ row.statusText || statusText[row.status] }}</el-tag></template></el-table-column>
        <el-table-column label="退货物流" min-width="190"><template #default="{ row }">{{ row.returnShipNo ? `${row.returnShipCompany || '物流'} ${row.returnShipNo}` : '-' }}</template></el-table-column>
        <el-table-column label="退款凭证" min-width="150"><template #default="{ row }"><div v-if="row.evidenceUrls?.length" class="evidence-list"><el-image v-for="url in row.evidenceUrls" :key="url" :src="resolveImageUrl(url)" :preview-src-list="row.evidenceUrls.map(resolveImageUrl)" fit="cover" class="evidence-image" /></div><span v-else>-</span></template></el-table-column>
        <el-table-column label="对账次数" width="100"><template #default="{ row }">{{ row.refundReconcileAttempts || 0 }}</template></el-table-column>
        <el-table-column prop="refundReconcileAt" label="最近对账" width="180" />
        <el-table-column prop="refundReconcileError" label="对账异常" min-width="180" show-overflow-tooltip />
        <el-table-column prop="refundFailReason" label="渠道失败原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="申请时间" width="180" />
      </el-table>
      <div class="pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" background layout="total, sizes, prev, pager, next" @current-change="fetchList" @size-change="fetchList" /></div>
    </el-card>
    <el-card class="task-card"><template #header>最近退款对账任务</template><el-table :data="tasks" size="small"><el-table-column prop="taskNo" label="任务 ID" min-width="220"/><el-table-column prop="status" label="状态" width="110"/><el-table-column prop="affectedCount" label="处理笔数" width="110"/><el-table-column prop="startedAt" label="开始时间" min-width="170"/><el-table-column prop="errorMessage" label="异常" min-width="220" show-overflow-tooltip/></el-table></el-card>
  </div>
</template>

<style scoped>
.notice { margin-bottom: 16px; }
.task-card { margin-top: 16px; }
.evidence-list { display: flex; gap: 6px; align-items: center; }
.evidence-image { width: 42px; height: 42px; border-radius: 4px; }
</style>
