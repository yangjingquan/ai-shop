<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

interface OrderRow {
  orderNo: string
  status: number
  statusText: string
  payAmount: number | string
  createdAt: string
}

interface PageResult<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}

interface AddressSnapshot {
  receiver?: string
  phone?: string
  region?: string
  detail?: string
}

interface OrderDetailItem {
  productId: number
  skuId: number
  productName: string
  mainImage?: string
  specText?: string
  unitPrice: number | string
  quantity: number
  subtotal: number | string
}

interface OrderDetail {
  orderNo: string
  status: number
  statusText: string
  totalAmount: number | string
  freightAmount: number | string
  discountAmount: number | string
  payAmount: number | string
  merchantId: number
  merchantName: string
  address?: AddressSnapshot | null
  createdAt: string
  payTime?: string
  payTransactionId?: string
  shipNo?: string
  shipTime?: string
  finishTime?: string
  cancelTime?: string
  cancelReason?: string
  remark?: string
  items: OrderDetailItem[]
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081').replace(/\/$/, '')

const orders = ref<OrderRow[]>([])
const shipNos = ref<Record<string, string>>({})
const loading = ref(false)
const total = ref(0)
const detailVisible = ref(false)
const detailLoading = ref(false)
const orderDetail = ref<OrderDetail | null>(null)
const query = reactive<{
  page: number
  size: number
  status: number | undefined
}>({ page: 1, size: 10, status: undefined })

async function loadOrders() {
  loading.value = true
  try {
    const data = await request.get<unknown, PageResult<OrderRow>>('/api/merchant/order/page', {
      params: {
        page: query.page,
        size: query.size,
        status: query.status,
      },
    })
    orders.value = data?.list || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadOrders()
}

function statusTagType(status: number): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  if (status === 1) return 'warning'
  if (status === 2) return 'primary'
  if (status === 3) return 'success'
  if (status === 4) return 'danger'
  return 'info'
}

function displayValue(value?: string | number | null) {
  return value === undefined || value === null || value === '' ? '-' : value
}

function formatAddress(address?: AddressSnapshot | null) {
  if (!address) return '-'
  return [address.region, address.detail].filter(Boolean).join(' ') || '-'
}

function resolveImageUrl(url?: string) {
  if (!url) return ''
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url
  return `${apiBaseUrl}${url.startsWith('/') ? url : `/${url}`}`
}

async function openOrderDetail(orderNo: string) {
  detailVisible.value = true
  detailLoading.value = true
  orderDetail.value = null
  try {
    orderDetail.value = await request.get<unknown, OrderDetail>(`/api/merchant/order/${orderNo}`)
  } finally {
    detailLoading.value = false
  }
}

async function doShip(orderNo: string) {
  const sn = shipNos.value[orderNo]
  if (!sn || !/^[A-Za-z0-9]{5,30}$/.test(sn)) {
    ElMessage.error('物流单号格式不合法（5-30位字母数字）')
    return
  }
  await request.post<unknown, void>('/api/merchant/order/ship', { shipNo: sn }, { params: { orderNo } })
  ElMessage.success('发货成功')
  await loadOrders()
}

onMounted(loadOrders)
</script>

<template>
  <div class="order-ship">
    <div class="page-header">
      <div>
        <span class="page-kicker">FULFILLMENT</span>
        <h1 class="page-title">订单发货</h1>
        <p class="page-desc">查看当前商户全部订单，可按订单状态筛选，并对待发货订单录入物流单号完成发货。</p>
      </div>
      <el-button @click="loadOrders">刷新列表</el-button>
    </div>

    <el-card>
      <div class="toolbar">
        <el-select
          v-model="query.status"
          placeholder="全部状态"
          clearable
          style="width: 160px"
          @change="onSearch"
        >
          <el-option label="待支付" :value="0" />
          <el-option label="待发货" :value="1" />
          <el-option label="待收货" :value="2" />
          <el-option label="已完成" :value="3" />
          <el-option label="已取消" :value="4" />
        </el-select>
        <el-button type="primary" @click="onSearch">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="orders" stripe>
        <el-table-column label="订单号" min-width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="openOrderDetail(row.orderNo)">
              {{ row.orderNo }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payAmount" label="金额" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="发货操作" min-width="320" fixed="right">
          <template #default="{ row }">
            <div v-if="row.status === 1" class="ship-action">
              <el-input
                v-model="shipNos[row.orderNo]"
                placeholder="输入物流单号"
                size="small"
              />
              <el-button type="primary" size="small" @click="doShip(row.orderNo)">
                发货
              </el-button>
            </div>
            <span v-else class="text-muted">无需发货</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next"
          @current-change="loadOrders"
          @size-change="loadOrders"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="820px">
      <div v-loading="detailLoading" class="order-detail">
        <template v-if="orderDetail">
          <div class="detail-section">
            <div class="section-title">基础信息</div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="订单号">{{ orderDetail.orderNo }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="statusTagType(orderDetail.status)">{{ orderDetail.statusText }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ displayValue(orderDetail.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="支付时间">{{ displayValue(orderDetail.payTime) }}</el-descriptions-item>
              <el-descriptions-item label="支付流水号" :span="2">
                {{ displayValue(orderDetail.payTransactionId) }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="detail-section">
            <div class="section-title">收货信息</div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="收货人">{{ displayValue(orderDetail.address?.receiver) }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ displayValue(orderDetail.address?.phone) }}</el-descriptions-item>
              <el-descriptions-item label="收货地址" :span="2">
                {{ formatAddress(orderDetail.address) }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="detail-section">
            <div class="section-title">商品明细</div>
            <el-table :data="orderDetail.items || []" border>
              <el-table-column label="商品" min-width="260">
                <template #default="{ row }">
                  <div class="detail-item">
                    <el-image
                      v-if="row.mainImage"
                      :src="resolveImageUrl(row.mainImage)"
                      :preview-src-list="[resolveImageUrl(row.mainImage)]"
                      preview-teleported
                      fit="cover"
                      class="item-image"
                    />
                    <div v-else class="item-image item-image-empty">无图</div>
                    <div>
                      <div class="item-name">{{ row.productName }}</div>
                      <div class="item-spec">{{ displayValue(row.specText) }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="unitPrice" label="单价" width="110" />
              <el-table-column prop="quantity" label="数量" width="80" />
              <el-table-column prop="subtotal" label="小计" width="110" />
            </el-table>
          </div>

          <div class="detail-section">
            <div class="section-title">金额信息</div>
            <el-descriptions :column="4" border>
              <el-descriptions-item label="商品金额">{{ displayValue(orderDetail.totalAmount) }}</el-descriptions-item>
              <el-descriptions-item label="运费">{{ displayValue(orderDetail.freightAmount) }}</el-descriptions-item>
              <el-descriptions-item label="优惠">{{ displayValue(orderDetail.discountAmount) }}</el-descriptions-item>
              <el-descriptions-item label="实付金额">{{ displayValue(orderDetail.payAmount) }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="detail-section">
            <div class="section-title">履约信息</div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="物流单号">{{ displayValue(orderDetail.shipNo) }}</el-descriptions-item>
              <el-descriptions-item label="发货时间">{{ displayValue(orderDetail.shipTime) }}</el-descriptions-item>
              <el-descriptions-item label="完成时间">{{ displayValue(orderDetail.finishTime) }}</el-descriptions-item>
              <el-descriptions-item label="取消时间">{{ displayValue(orderDetail.cancelTime) }}</el-descriptions-item>
              <el-descriptions-item label="取消原因">{{ displayValue(orderDetail.cancelReason) }}</el-descriptions-item>
              <el-descriptions-item label="买家备注">{{ displayValue(orderDetail.remark) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.ship-action {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) auto;
  gap: 10px;
  align-items: center;
}
.text-muted {
  color: var(--el-text-color-secondary);
}

.order-detail {
  min-height: 160px;
}

.detail-section + .detail-section {
  margin-top: 18px;
}

.section-title {
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.detail-item {
  display: flex;
  gap: 10px;
  align-items: center;
}

.item-image {
  width: 48px;
  height: 48px;
  flex: none;
  border-radius: 8px;
}

.item-image-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  font-size: 12px;
}

.item-name {
  color: var(--el-text-color-primary);
}

.item-spec {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
