<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminProductApi, type ProductListVO } from '@/api/product'

const loading = ref(false)
const list = ref<ProductListVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, auditStatus: undefined as number | undefined, keyword: '' })
const dialogVisible = ref(false)
const current = ref<ProductListVO | null>(null)
const auditReason = ref('')
const submitting = ref(false)
const auditText: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已驳回' }

async function fetchList() {
  loading.value = true
  try {
    const data = await adminProductApi.auditPage({ page: query.page, size: query.size, auditStatus: query.auditStatus, keyword: query.keyword || undefined })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}

function search() { query.page = 1; fetchList() }

async function audit(row: ProductListVO, status: number) {
  if (status === 2) {
    const reason = await ElMessageBox.prompt('请输入驳回原因', '驳回商品', { inputPlaceholder: '请填写具体问题', inputValidator: (v) => v.trim() ? true : '驳回原因不能为空' }).catch(() => null)
    if (!reason) return
    auditReason.value = reason.value
  } else {
    auditReason.value = ''
    await ElMessageBox.confirm(`确认通过商品「${row.name}」？`, '商品审核', { type: 'warning' })
  }
  submitting.value = true
  try {
    await adminProductApi.audit(row.id, status, auditReason.value)
    ElMessage.success(status === 1 ? '审核通过' : '已驳回')
    fetchList()
  } finally { submitting.value = false }
}

function openDetail(row: ProductListVO) { current.value = row; dialogVisible.value = true }
onMounted(fetchList)
</script>

<template>
  <div class="page">
    <div class="page-header"><div><span class="page-kicker">PRODUCT REVIEW</span><h1 class="page-title">商品审核</h1><p class="page-desc">审核商家提交的商品资料，审核通过后才允许上架销售。</p></div></div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="商品名称" clearable style="width: 220px" @keyup.enter="search" />
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
        <el-table-column label="操作" width="180"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row as ProductListVO)">查看</el-button><el-button v-if="row.auditStatus !== 1" link type="success" :loading="submitting" @click="audit(row as ProductListVO, 1)">通过</el-button><el-button v-if="row.auditStatus !== 2" link type="danger" :loading="submitting" @click="audit(row as ProductListVO, 2)">驳回</el-button></template></el-table-column>
      </el-table>
      <div class="pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" background layout="total, sizes, prev, pager, next" @current-change="fetchList" @size-change="fetchList" /></div>
    </el-card>
    <el-dialog v-model="dialogVisible" title="商品审核信息" width="620px">
      <el-descriptions v-if="current" :column="2" border><el-descriptions-item label="商品名称">{{ current.name }}</el-descriptions-item><el-descriptions-item label="商家 ID">{{ current.merchantId }}</el-descriptions-item><el-descriptions-item label="分类">{{ current.categoryName || '-' }}</el-descriptions-item><el-descriptions-item label="库存">{{ current.totalStock }}</el-descriptions-item><el-descriptions-item label="审核状态">{{ auditText[current.auditStatus ?? 0] }}</el-descriptions-item><el-descriptions-item label="审核意见">{{ current.auditReason || '-' }}</el-descriptions-item></el-descriptions>
    </el-dialog>
  </div>
</template>
