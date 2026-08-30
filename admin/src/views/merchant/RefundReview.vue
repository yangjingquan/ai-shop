<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

interface RefundRow {
  id: number
  orderNo: string
  reason: string
  status: number
  statusText?: string
  rejectReason?: string
  returnRequired?: number
  returnShipCompany?: string
  returnShipNo?: string
}

const refunds = ref<RefundRow[]>([])
const currentTab = ref<0 | -1>(0)
const rejectReasons = ref<Record<number, string>>({})
const loading = ref(false)
const statusLabels: Record<number, string> = { 0: '待处理', 1: '退款处理中', 2: '已拒绝', 3: '退款成功', 4: '退款失败', 5: '待退货', 6: '待验货' }

function refundStatusText(row: { status?: number; statusText?: string }) {
  return row.statusText || (row.status === undefined ? '未知状态' : statusLabels[row.status] || '未知状态')
}

async function loadRefunds() {
  loading.value = true
  try {
    const data = await request.get<unknown, RefundRow[]>('/api/merchant/refund/list', {
      params: { status: currentTab.value === -1 ? undefined : currentTab.value, page: 1, size: 100 },
    })
    refunds.value = data || []
  } finally {
    loading.value = false
  }
}

async function doApprove(id: number) {
  await request.post<unknown, void>(`/api/merchant/refund/${id}/approve`, { approved: true })
  ElMessage.success('已发起退款，等待微信处理结果')
  await loadRefunds()
}

async function doReject(id: number) {
  await request.post<unknown, void>(`/api/merchant/refund/${id}/approve`, {
    approved: false,
    rejectReason: rejectReasons.value[id] || '',
  })
  ElMessage.success('已拒绝退款')
  await loadRefunds()
}

async function confirmReturn(id: number) {
  await request.post<unknown, void>(`/api/merchant/refund/${id}/return-received`, { note: '' })
  ElMessage.success('已确认收货，正在发起原路退款')
  await loadRefunds()
}

onMounted(loadRefunds)
</script>

<template>
  <div class="refund-review">
    <div class="page-header">
      <div>
        <span class="page-kicker">AFTER SALES</span>
        <h1 class="page-title">退款审批</h1>
        <p class="page-desc">集中处理退款申请，跟进售后体验与资金安全。</p>
      </div>
    </div>

    <el-card>
      <div class="toolbar">
        <el-radio-group v-model="currentTab" @change="loadRefunds">
          <el-radio-button :value="0">待处理</el-radio-button>
          <el-radio-button :value="-1">全部</el-radio-button>
        </el-radio-group>
      </div>

      <el-table v-loading="loading" :data="refunds" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderNo" label="订单号" min-width="200" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.reason === '拼团未成团'" type="warning" size="small">拼团退款</el-tag>
            <span v-else>普通退款</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="180" />
        <el-table-column prop="refundAmount" label="退款金额" width="110" />
        <el-table-column label="退货物流" min-width="160">
          <template #default="{ row }">
            {{ row.returnShipNo ? `${row.returnShipCompany || '物流'} ${row.returnShipNo}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 0 ? 'warning' : row.status === 1 ? 'primary' : row.status === 2 ? 'danger' : row.status === 3 ? 'success' : 'danger'"
              size="small"
            >
              {{ refundStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="售后操作" min-width="360" fixed="right">
          <template #default="{ row }">
            <div v-if="row.status === 0" class="review-action">
              <el-input
                v-model="rejectReasons[row.id]"
                placeholder="拒绝原因（可选）"
                size="small"
              />
              <el-button type="success" size="small" @click="doApprove(row.id)">
                同意退款
              </el-button>
              <el-button type="danger" size="small" @click="doReject(row.id)">
                拒绝
              </el-button>
            </div>
            <el-button v-else-if="row.status === 6" type="primary" size="small" @click="confirmReturn(row.id)">
              验货并退款
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.review-action {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) auto auto;
  gap: 10px;
  align-items: center;
}
</style>
