<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

interface RefundRow {
  id: number
  orderNo: string
  reason: string
  status: number
  rejectReason?: string
}

const refunds = ref<RefundRow[]>([])
const currentTab = ref<0 | -1>(0)
const rejectReasons = ref<Record<number, string>>({})
const loading = ref(false)

async function loadRefunds() {
  loading.value = true
  try {
    const res = await axios.get('/api/merchant/refund/list', {
      params: { status: currentTab.value === -1 ? undefined : currentTab.value, page: 1, size: 100 },
    })
    refunds.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

async function doApprove(id: number) {
  try {
    await axios.post(`/api/merchant/refund/${id}/approve`, { approved: true })
    ElMessage.success('已同意退款')
    await loadRefunds()
  } catch (err: unknown) {
    const message = axios.isAxiosError(err) ? err.response?.data?.msg : undefined
    ElMessage.error(message || '操作失败')
  }
}

async function doReject(id: number) {
  try {
    await axios.post(`/api/merchant/refund/${id}/approve`, {
      approved: false,
      rejectReason: rejectReasons.value[id] || '',
    })
    ElMessage.success('已拒绝退款')
    await loadRefunds()
  } catch (err: unknown) {
    const message = axios.isAxiosError(err) ? err.response?.data?.msg : undefined
    ElMessage.error(message || '操作失败')
  }
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
        <el-table-column prop="reason" label="退款原因" min-width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 0 ? 'warning' : row.status === 1 ? 'success' : 'danger'"
              size="small"
            >
              {{ row.status === 0 ? '待处理' : row.status === 1 ? '已同意' : '已拒绝' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="currentTab === 0" label="审批操作" min-width="360" fixed="right">
          <template #default="{ row }">
            <div class="review-action">
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
          </template>
        </el-table-column>
        <el-table-column v-else label="拒绝原因" prop="rejectReason" min-width="160" />
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
