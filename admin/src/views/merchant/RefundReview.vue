<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { adminOrderApi, type AdminRefundRow, type GroupRefundTaskRow } from '@/api/order'

type RefundRow = AdminRefundRow

const refunds = ref<RefundRow[]>([])
const groupRefundTasks = ref<GroupRefundTaskRow[]>([])
const route = useRoute()
const routeStatus = Number(route.query.status)
const currentTab = ref<number | ''>([0, 1, 2, 3, 4, 5, 6].includes(routeStatus) ? routeStatus : 0)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const rejectReasons = ref<Record<number, string>>({})
const loading = ref(false)
const groupTaskLoading = ref(false)
const statusLabels: Record<number, string> = { 0: '待处理', 1: '退款处理中', 2: '已拒绝', 3: '退款成功', 4: '退款失败', 5: '待填写退货物流', 6: '待商家验货' }
const statusOptions = [
  { value: null, label: '全部' },
  { value: 0, label: '待处理' },
  { value: 1, label: '退款处理中' },
  { value: 2, label: '已拒绝' },
  { value: 4, label: '退款失败' },
  { value: 5, label: '待填写退货物流' },
  { value: 6, label: '待商家验货' },
  { value: 3, label: '退款成功' },
]

function refundStatusText(row: { status?: number; statusText?: string }) {
  return row.statusText || (row.status === undefined ? '未知状态' : statusLabels[row.status] || '未知状态')
}

async function loadRefunds() {
  loading.value = true
  try {
    const data = await request.get<unknown, { list: RefundRow[]; total: number }>('/api/merchant/refund/list', {
      params: { status: currentTab.value === '' ? undefined : currentTab.value, page: page.value, size: size.value },
    })
    refunds.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadGroupRefundTasks() {
  groupTaskLoading.value = true
  try {
    const data = await adminOrderApi.groupRefundTasks({ page: 1, size: 20 })
    groupRefundTasks.value = data.list || []
  } finally {
    groupTaskLoading.value = false
  }
}

function changeStatus(value: number | '') {
  currentTab.value = value
  page.value = 1
  loadRefunds()
}

function refundTagType(status?: number) {
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

async function doRetry(id: number) {
  await request.post<unknown, void>(`/api/merchant/refund/${id}/retry`)
  ElMessage.success('已重新发起退款，等待微信处理结果')
  await loadRefunds()
}

onMounted(() => {
  loadRefunds()
  loadGroupRefundTasks()
})
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
        <el-select :model-value="currentTab" placeholder="筛选状态" style="width: 180px" @update:model-value="changeStatus">
          <el-option v-for="option in statusOptions" :key="String(option.value)" :label="option.label" :value="option.value === null ? '' : option.value" />
        </el-select>
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
        <el-table-column label="退款凭证" min-width="150">
          <template #default="{ row }">
            <div v-if="row.evidenceUrls?.length" class="evidence-list">
              <el-image
                v-for="url in row.evidenceUrls"
                :key="url"
                :src="resolveImageUrl(url)"
                :preview-src-list="row.evidenceUrls.map(resolveImageUrl)"
                fit="cover"
                class="evidence-image"
              />
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="退货物流" min-width="180">
          <template #default="{ row }">
            {{ row.returnShipNo ? `${row.returnShipCompany || '物流'} ${row.returnShipNo}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag
              :type="refundTagType(row.status)"
              size="small"
            >
              {{ refundStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="refundFailReason" label="失败原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="售后操作" min-width="360" fixed="right">
          <template #default="{ row }">
            <div v-if="row.status === 0" class="review-action">
              <el-input
                v-model="rejectReasons[row.id]"
                placeholder="拒绝原因（可选）"
                size="small"
              />
              <el-button v-permission="'merchant:refund:approve'" type="success" size="small" @click="doApprove(row.id)">
                同意退款
              </el-button>
              <el-button v-permission="'merchant:refund:approve'" type="danger" size="small" @click="doReject(row.id)">
                拒绝
              </el-button>
            </div>
            <el-button v-else-if="row.status === 6" v-permission="'merchant:refund:return-received'" type="primary" size="small" @click="confirmReturn(row.id)">
              验货并退款
            </el-button>
            <el-button v-else-if="row.status === 4" v-permission="'merchant:refund:retry'" type="warning" size="small" @click="doRetry(row.id)">
              重试退款
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next"
          @current-change="loadRefunds"
          @size-change="loadRefunds"
        />
      </div>
    </el-card>

    <el-card class="group-refund-card">
      <div class="group-refund-head">
        <div>
          <h2>团购退款追踪</h2>
          <p>团购未成团后自动退款，开关关闭也不会中断存量退款。</p>
        </div>
        <el-button size="small" @click="loadGroupRefundTasks">刷新</el-button>
      </div>
      <el-table v-loading="groupTaskLoading" :data="groupRefundTasks" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column prop="groupId" label="团ID" width="90" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : row.status === 'failed' ? 'danger' : 'warning'" size="small">
              {{ row.statusText || row.status || '未知状态' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试次数" width="100" />
        <el-table-column prop="lastError" label="最近错误" min-width="220" show-overflow-tooltip />
        <el-table-column prop="completedAt" label="到账时间" width="170" />
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
.evidence-list { display: flex; gap: 6px; align-items: center; }
.evidence-image { width: 42px; height: 42px; border-radius: 4px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
.group-refund-card { margin-top: 18px; }
.group-refund-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 14px; }
.group-refund-head h2 { margin: 0 0 6px; font-size: 18px; color: var(--shop-ink); }
.group-refund-head p { margin: 0; color: var(--shop-muted); font-size: 13px; }
</style>
