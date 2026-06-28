<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

interface OrderRow {
  orderNo: string
  statusText: string
  payAmount: number | string
  createdAt: string
}

const orders = ref<OrderRow[]>([])
const shipNos = ref<Record<string, string>>({})
const loading = ref(false)

async function loadOrders() {
  loading.value = true
  try {
    const res = await axios.get('/api/merchant/order/page', {
      params: { status: 1, page: 1, size: 100 },
    })
    orders.value = res.data.data?.list || []
  } finally {
    loading.value = false
  }
}

async function doShip(orderNo: string) {
  const sn = shipNos.value[orderNo]
  if (!sn || !/^[A-Za-z0-9]{5,30}$/.test(sn)) {
    ElMessage.error('物流单号格式不合法（5-30位字母数字）')
    return
  }
  try {
    await axios.post('/api/merchant/order/ship', { shipNo: sn }, { params: { orderNo } })
    ElMessage.success('发货成功')
    await loadOrders()
  } catch (err: unknown) {
    const message = axios.isAxiosError(err) ? err.response?.data?.msg : undefined
    ElMessage.error(message || '发货失败')
  }
}

onMounted(loadOrders)
</script>

<template>
  <div class="order-ship">
    <div class="page-header">
      <div>
        <span class="page-kicker">FULFILLMENT</span>
        <h1 class="page-title">待发货订单</h1>
        <p class="page-desc">处理已支付订单，录入物流单号完成发货履约。</p>
      </div>
      <el-button @click="loadOrders">刷新列表</el-button>
    </div>

    <el-card>
      <el-table v-loading="loading" :data="orders" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="200" />
        <el-table-column prop="statusText" label="状态" width="110">
          <template #default="{ row }">
            <el-tag type="warning">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payAmount" label="金额" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="发货操作" min-width="320" fixed="right">
          <template #default="{ row }">
            <div class="ship-action">
              <el-input
                v-model="shipNos[row.orderNo]"
                placeholder="输入物流单号"
                size="small"
              />
              <el-button type="primary" size="small" @click="doShip(row.orderNo)">
                发货
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.ship-action {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) auto;
  gap: 10px;
  align-items: center;
}
</style>
