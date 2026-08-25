<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { adminOrderApi, type AdminRefundRow } from '@/api/order'

const loading = ref(false)
const list = ref<AdminRefundRow[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, status: undefined as number | undefined, merchantId: undefined as number | undefined })
const statusText: Record<number, string> = { 0: '待处理', 1: '已同意', 2: '已拒绝' }

async function fetchList() {
  loading.value = true
  try {
    const data = await adminOrderApi.refunds({ page: query.page, size: query.size, status: query.status, merchantId: query.merchantId })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() { query.page = 1; fetchList() }
onMounted(fetchList)
</script>

<template>
  <div class="page">
    <div class="page-header"><div><span class="page-kicker">REFUND MONITOR</span><h1 class="page-title">平台退款</h1><p class="page-desc">查看各商家的售后申请与处理状态；资金退款以支付渠道结果为准。</p></div></div>
    <el-alert title="当前页面仅提供查询，不开放会改变退款资金状态的审批操作。" type="warning" :closable="false" show-icon class="notice" />
    <el-card>
      <div class="toolbar">
        <el-input-number v-model="query.merchantId" :min="1" :controls="false" placeholder="商家 ID" style="width: 150px" />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="search"><el-option v-for="(label, value) in statusText" :key="value" :label="label" :value="Number(value)" /></el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column prop="merchantName" label="商家" width="150" />
        <el-table-column prop="reason" label="申请原因" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'danger'">{{ row.statusText || statusText[row.status] }}</el-tag></template></el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="180" />
        <el-table-column prop="rejectReason" label="拒绝原因" min-width="180" show-overflow-tooltip />
      </el-table>
      <div class="pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" background layout="total, sizes, prev, pager, next" @current-change="fetchList" @size-change="fetchList" /></div>
    </el-card>
  </div>
</template>

<style scoped>.notice { margin-bottom: 16px; }</style>
