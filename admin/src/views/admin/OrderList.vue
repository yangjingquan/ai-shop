<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { adminOrderApi, type AdminOrderDetail, type AdminOrderRow } from '@/api/order'
import type { PageResult } from '@/api/merchant'

const loading = ref(false)
const list = ref<AdminOrderRow[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, orderNo: '', status: undefined as number | undefined, merchantId: undefined as number | undefined })
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminOrderDetail | null>(null)

const statusText: Record<number, string> = {
  0: '待支付', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消',
  5: '待成团', 6: '已成团', 7: '待退款',
}

async function fetchList() {
  loading.value = true
  try {
    const data = await adminOrderApi.page({
      page: query.page,
      size: query.size,
      orderNo: query.orderNo || undefined,
      status: query.status,
      merchantId: query.merchantId,
    })
    list.value = (data as PageResult<AdminOrderRow>).list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  fetchList()
}

async function openDetail(orderNo: string) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await adminOrderApi.detail(orderNo)
    if (detail.value?.shipNo) await loadLogistics()
  } finally {
    detailLoading.value = false
  }
}

const logisticsLoading = ref(false)

async function loadLogistics(forceRefresh = false) {
  if (!detail.value?.orderNo) return
  logisticsLoading.value = true
  try {
    const logistics = forceRefresh
      ? await adminOrderApi.refreshLogistics(detail.value.orderNo)
      : await adminOrderApi.logistics(detail.value.orderNo)
    detail.value = { ...detail.value, logistics }
  } finally {
    logisticsLoading.value = false
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
      <div><span class="page-kicker">PLATFORM ORDERS</span><h1 class="page-title">平台订单</h1><p class="page-desc">跨商家查看订单状态、支付金额与履约信息。</p></div>
    </div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="query.orderNo" placeholder="订单号" clearable style="width: 220px" @keyup.enter="search" />
        <el-input-number v-model="query.merchantId" :min="1" :controls="false" placeholder="商家 ID" style="width: 140px" />
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="search">
          <el-option v-for="(label, value) in statusText" :key="value" :label="label" :value="Number(value)" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="190" />
        <el-table-column prop="merchantName" label="商家" width="140" />
        <el-table-column label="类型" width="80"><template #default="{ row }">{{ row.orderType === 1 ? '拼团' : '普通' }}</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag>{{ statusText[row.status] || row.statusText }}</el-tag></template></el-table-column>
        <el-table-column label="实付金额" width="120"><template #default="{ row }">{{ money(row.payAmount) }}</template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="90"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row.orderNo)">详情</el-button></template></el-table-column>
      </el-table>
      <div class="pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" background layout="total, sizes, prev, pager, next" @current-change="fetchList" @size-change="fetchList" /></div>
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="900px">
      <div v-loading="detailLoading" v-if="detail">
          <el-descriptions :column="3" border>
          <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="商家">{{ detail.merchantName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.statusText }}</el-descriptions-item>
          <el-descriptions-item label="商品金额">{{ money(detail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">{{ money(detail.payAmount) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ detail.payTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="交易单号">{{ detail.payTransactionId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="物流">{{ detail.shipCompany || '-' }} {{ detail.shipNo || '' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createdAt || '-' }}</el-descriptions-item>
        </el-descriptions>
        <template v-if="detail.shipNo">
          <el-divider />
          <div class="logistics-head">
            <span>物流轨迹</span>
            <el-button link type="primary" :loading="logisticsLoading" @click="loadLogistics(true)">刷新物流</el-button>
          </div>
          <el-alert v-if="detail.logistics?.error" :title="detail.logistics.error" type="warning" :closable="false" />
          <el-empty v-if="!detail.logistics?.traces?.length" description="暂无物流轨迹" :image-size="70" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="(trace, index) in detail.logistics.traces"
              :key="`${trace.acceptTime}-${index}`"
              :timestamp="trace.acceptTime"
              :type="index === 0 ? 'primary' : 'info'"
            >
              {{ trace.acceptStation }}
            </el-timeline-item>
          </el-timeline>
        </template>
        <el-divider />
        <el-table :data="detail.items || []" border>
          <el-table-column prop="productName" label="商品" min-width="220" />
          <el-table-column prop="specText" label="规格" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="110"><template #default="{ row }">{{ money(row.subtotal) }}</template></el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.logistics-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 600;
}
</style>
