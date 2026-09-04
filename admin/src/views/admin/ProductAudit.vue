<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminProductApi, type ProductListVO } from '@/api/product'
import { merchantApi, type MerchantVO } from '@/api/merchant'

const loading = ref(false)
const list = ref<ProductListVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, auditStatus: undefined as number | undefined, merchantId: undefined as number | undefined, keyword: '' })
const merchants = ref<MerchantVO[]>([])
const dialogVisible = ref(false)
const current = ref<ProductListVO | null>(null)
const submitting = ref(false)
const auditText: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已驳回' }

async function fetchList() {
  loading.value = true
  try {
    const data = await adminProductApi.auditPage({ page: query.page, size: query.size, auditStatus: query.auditStatus, merchantId: query.merchantId, keyword: query.keyword || undefined })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}

function search() { query.page = 1; fetchList() }

function openDetail(row: ProductListVO) { current.value = row; dialogVisible.value = true }

async function forceOffline(row: ProductListVO) {
  const result = await ElMessageBox.prompt('请输入强制下架原因', '平台强制下架', {
    inputPlaceholder: '例如：商品信息违规、价格异常',
    inputValidator: (value) => value.trim() ? true : '下架原因不能为空',
  }).catch(() => null)
  if (!result) return
  submitting.value = true
  try {
    await adminProductApi.forceOffline(row.id, result.value)
    ElMessage.success('商品已强制下架，需商户审核通过后才能上架')
    await fetchList()
  } finally { submitting.value = false }
}

async function loadMerchants() {
  const data = await merchantApi.list({ page: 1, size: 1000 })
  merchants.value = data.list
}

onMounted(async () => {
  await Promise.all([loadMerchants(), fetchList()])
})
</script>

<template>
  <div class="page">
    <div class="page-header"><div><span class="page-kicker">PRODUCT REVIEW</span><h1 class="page-title">商品审核</h1><p class="page-desc">查看商户自审状态并进行平台商品监管。</p></div></div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="商品名称" clearable style="width: 220px" @keyup.enter="search" />
        <el-select v-model="query.merchantId" placeholder="全部商户" clearable filterable style="width: 180px" @change="search">
          <el-option v-for="merchant in merchants" :key="merchant.id" :label="`${merchant.name}（ID: ${merchant.id}）`" :value="merchant.id" />
        </el-select>
        <el-select v-model="query.auditStatus" placeholder="全部审核状态" clearable style="width: 160px" @change="search"><el-option v-for="(label, value) in auditText" :key="value" :label="label" :value="Number(value)" /></el-select>
        <el-button type="primary" @click="search">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="商品名称" min-width="220" />
        <el-table-column prop="merchantId" label="商家 ID" width="90" />
        <el-table-column prop="categoryName" label="分类" width="130" />
        <el-table-column prop="totalStock" label="库存" width="90" />
        <el-table-column label="审核状态" width="110"><template #default="{ row }"><el-tag :type="row.auditStatus === 0 ? 'warning' : row.auditStatus === 1 ? 'success' : 'danger'">{{ auditText[row.auditStatus ?? 0] }}</el-tag></template></el-table-column>
        <el-table-column prop="auditReason" label="审核意见" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="170"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row as ProductListVO)">查看</el-button><el-button v-if="row.status === 1" link type="warning" :loading="submitting" @click="forceOffline(row as ProductListVO)">强制下架</el-button></template></el-table-column>
      </el-table>
      <div class="pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" background layout="total, sizes, prev, pager, next" @current-change="fetchList" @size-change="fetchList" /></div>
    </el-card>
    <el-dialog v-model="dialogVisible" title="商品审核信息" width="620px">
      <el-descriptions v-if="current" :column="2" border><el-descriptions-item label="商品名称">{{ current.name }}</el-descriptions-item><el-descriptions-item label="商家 ID">{{ current.merchantId }}</el-descriptions-item><el-descriptions-item label="分类">{{ current.categoryName || '-' }}</el-descriptions-item><el-descriptions-item label="库存">{{ current.totalStock }}</el-descriptions-item><el-descriptions-item label="审核状态">{{ auditText[current.auditStatus ?? 0] }}</el-descriptions-item><el-descriptions-item label="审核意见">{{ current.auditReason || '-' }}</el-descriptions-item></el-descriptions>
    </el-dialog>
  </div>
</template>
