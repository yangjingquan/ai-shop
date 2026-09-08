<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminOrderApi, type AdminPaymentRow } from '@/api/order'

const loading = ref(false)
const reconciling = ref(false)
const list = ref<AdminPaymentRow[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  merchantId: undefined as number | undefined,
  orderNo: '',
  transactionId: '',
})

async function fetchList() {
  loading.value = true
  try {
    const data = await adminOrderApi.payments({ ...query })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  fetchList()
}

async function reconcile() {
  reconciling.value = true
  try {
    const preview = await adminOrderApi.previewReconciliation('PAYMENT')
    await ElMessageBox.confirm(`本次将检查最多 ${preview.affectedCount} 笔待支付订单，并生成可审计、可幂等追踪的任务。`, '确认发起支付对账', { type: 'warning', confirmButtonText: '生成任务并执行' })
    const result = await adminOrderApi.createReconciliation('PAYMENT')
    ElMessage.success(`对账任务 ${result.taskNo} 已${result.status === 'SUCCESS' ? '完成' : '创建'}`)
    await fetchList()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.message || '创建对账任务失败')
  } finally {
    reconciling.value = false
  }
}

function money(value?: number) {
  return `¥${Number(value || 0).toFixed(2)}`
}

onMounted(fetchList)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <span class="page-kicker">PAYMENT LEDGER</span>
        <h1 class="page-title">支付管理</h1>
        <p class="page-desc">查询微信支付流水，查看主动查单记录；对账会先预览范围并生成可审计任务。</p>
      </div>
      <el-button type="primary" :loading="reconciling" @click="reconcile">预览并发起对账</el-button>
    </div>

    <el-card>
      <div class="toolbar">
        <el-input v-model="query.orderNo" clearable placeholder="订单号" style="width: 210px" @keyup.enter="search" />
        <el-input v-model="query.transactionId" clearable placeholder="微信交易号" style="width: 230px" @keyup.enter="search" />
        <el-input-number v-model="query.merchantId" :min="1" :controls="false" placeholder="商家 ID" style="width: 150px" />
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column prop="transactionId" label="微信交易号" min-width="220" show-overflow-tooltip />
        <el-table-column prop="merchantName" label="商家" width="150" />
        <el-table-column label="支付金额" width="120"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
        <el-table-column prop="payTime" label="支付时间" width="180" />
        <el-table-column label="主动查单" width="110"><template #default="{ row }">{{ row.payReconcileAttempts || 0 }} 次</template></el-table-column>
        <el-table-column prop="payReconcileAt" label="最近查单" width="180" />
        <el-table-column prop="payReconcileError" label="查单异常" min-width="200" show-overflow-tooltip>
          <template #default="{ row }"><span :class="{ error: row.payReconcileError }">{{ row.payReconcileError || '-' }}</span></template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" background layout="total, sizes, prev, pager, next" @current-change="fetchList" @size-change="fetchList" />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.error { color: var(--shop-danger); }
</style>
