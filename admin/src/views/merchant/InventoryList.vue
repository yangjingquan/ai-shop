<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { inventoryApi, type InventorySkuVO, type InventoryTransactionVO } from '@/api/product'
import type { PageResult } from '@/api/merchant'

const loading = ref(false)
const route = useRoute()
const list = ref<InventorySkuVO[]>([])
const total = ref(0)
const threshold = ref(Number(route.query.threshold) || 5)
const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  lowStockOnly: route.query.lowStockOnly === 'true',
})
const adjustVisible = ref(false)
const adjusting = ref(false)
const selected = ref<InventorySkuVO | null>(null)
const adjustForm = reactive({ changeQty: 0, reason: '' })
const historyVisible = ref(false)
const historyLoading = ref(false)
const history = ref<InventoryTransactionVO[]>([])
const historyTotal = ref(0)
const historyPage = ref(1)

async function load() {
  loading.value = true
  try {
    const data = await inventoryApi.skus({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      lowStockOnly: query.lowStockOnly,
      threshold: Number(threshold.value || 0),
    })
    list.value = (data as PageResult<InventorySkuVO>).list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function openAdjust(row: InventorySkuVO) {
  selected.value = row
  adjustForm.changeQty = 0
  adjustForm.reason = ''
  adjustVisible.value = true
}

async function submitAdjust() {
  if (!selected.value || !Number.isInteger(adjustForm.changeQty) || adjustForm.changeQty === 0) {
    ElMessage.error('请输入不为 0 的整数变更数量')
    return
  }
  adjusting.value = true
  try {
    await inventoryApi.adjust({
      skuId: selected.value.skuId,
      changeQty: adjustForm.changeQty,
      reason: adjustForm.reason || undefined,
    })
    ElMessage.success('库存调整成功')
    adjustVisible.value = false
    await load()
  } finally {
    adjusting.value = false
  }
}

async function openHistory(row: InventorySkuVO) {
  selected.value = row
  historyVisible.value = true
  historyPage.value = 1
  await loadHistory()
}

async function loadHistory() {
  if (!selected.value) return
  historyLoading.value = true
  try {
    const data = await inventoryApi.transactions({
      page: historyPage.value,
      size: 10,
      skuId: selected.value.skuId,
    })
    history.value = data.list || []
    historyTotal.value = data.total || 0
  } finally {
    historyLoading.value = false
  }
}

function stockTag(stock: number) {
  return stock <= Number(threshold.value || 0) ? 'danger' : stock <= 20 ? 'warning' : 'success'
}

onMounted(load)
</script>

<template>
  <div class="inventory-page">
    <div class="page-header">
      <div>
        <span class="page-kicker">INVENTORY</span>
        <h1 class="page-title">库存设置</h1>
        <p class="page-desc">按 SKU 查看库存，记录入库/出库调整，并保留完整变更流水。</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-card>
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="商品名 / SKU 编码 / 规格" style="width: 260px" @keyup.enter="search" />
        <el-input-number v-model="threshold" :min="0" :max="1000000" :controls="false" style="width: 130px" @change="search" />
        <span class="toolbar-label">预警阈值</span>
        <el-checkbox v-model="query.lowStockOnly" @change="search">只看低库存</el-checkbox>
        <el-button type="primary" @click="search">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="productName" label="商品" min-width="220" />
        <el-table-column prop="specText" label="规格" min-width="180" />
        <el-table-column prop="skuCode" label="SKU 编码" min-width="150" />
        <el-table-column label="当前库存" width="120">
          <template #default="{ row }"><el-tag :type="stockTag(row.stock)">{{ row.stock }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'merchant:inventory:adjust'" link type="primary" @click="openAdjust(row as InventorySkuVO)">调整库存</el-button>
            <el-button v-permission="'merchant:inventory:transaction:view'" link @click="openHistory(row as InventorySkuVO)">变更记录</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" background layout="total, sizes, prev, pager, next" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <el-dialog v-model="adjustVisible" title="调整库存" width="420px">
      <el-descriptions v-if="selected" :column="1" border>
        <el-descriptions-item label="商品">{{ selected.productName }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ selected.specText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前库存">{{ selected.stock }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="90px" style="margin-top: 18px">
        <el-form-item label="变更数量"><el-input-number v-model="adjustForm.changeQty" :step="1" :controls="true" /></el-form-item>
        <el-form-item label="调整原因"><el-input v-model="adjustForm.reason" maxlength="255" placeholder="如：采购入库、盘点修正" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="adjustVisible = false">取消</el-button><el-button v-permission="'merchant:inventory:adjust'" type="primary" :loading="adjusting" @click="submitAdjust">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="historyVisible" title="库存变更记录" width="900px">
      <el-table v-loading="historyLoading" :data="history" border>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column label="变更" width="100"><template #default="{ row }"><span :class="row.changeQty > 0 ? 'inbound' : 'outbound'">{{ row.changeQty > 0 ? '+' : '' }}{{ row.changeQty }}</span></template></el-table-column>
        <el-table-column label="库存" width="130"><template #default="{ row }">{{ row.stockBefore }} → {{ row.stockAfter }}</template></el-table-column>
        <el-table-column prop="reason" label="原因" min-width="220" />
        <el-table-column prop="operationType" label="操作类型" width="140" />
      </el-table>
      <div class="pagination"><el-pagination v-model:current-page="historyPage" :page-size="10" :total="historyTotal" background layout="total, prev, pager, next" @current-change="loadHistory" /></div>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar-label { color: var(--el-text-color-secondary); font-size: 13px; margin-left: -8px; }
.inbound { color: #16a34a; font-weight: 700; }
.outbound { color: #dc2626; font-weight: 700; }
.pagination { display: flex; justify-content: flex-end; margin-top: 18px; }
</style>
