<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { productApi, type ProductListVO } from '@/api/product'
import { merchantCategoryApi, type MerchantCategoryVO } from '@/api/category'

const router = useRouter()
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081').replace(/\/$/, '')

const loading = ref(false)
const list = ref<ProductListVO[]>([])
const selectedRows = ref<ProductListVO[]>([])
const total = ref(0)
const query = reactive<{
  page: number
  size: number
  keyword: string
  categoryId: number | undefined
  status: number | undefined
  isRecommend: number | undefined
  isGroupBuy: number | undefined
}>({
  page: 1,
  size: 10,
  keyword: '',
  categoryId: undefined,
  status: undefined,
  isRecommend: undefined,
  isGroupBuy: undefined,
})

const catTree = ref<MerchantCategoryVO[]>([])
const selectedCount = computed(() => selectedRows.value.length)
const auditText: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已驳回' }
const catOptions = computed(() =>
  catTree.value.map((t) => ({
    value: t.id,
    label: t.name,
    children: (t.children ?? []).map((c) => ({ value: c.id, label: c.name })),
  })),
)

async function loadCategories() {
  catTree.value = (await merchantCategoryApi.enabledTree()) ?? []
}

async function fetchList() {
  loading.value = true
  try {
    const data = await productApi.page({
      page: query.page,
      size: query.size,
      categoryId: query.categoryId,
      keyword: query.keyword || undefined,
      status: query.status,
      isRecommend: query.isRecommend,
      isGroupBuy: query.isGroupBuy,
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  fetchList()
}

function onCreate() {
  router.push('/merchant/products/edit')
}

function onEdit(row: ProductListVO) {
  router.push(`/merchant/products/edit/${row.id}`)
}

function onSelectionChange(rows: ProductListVO[]) {
  selectedRows.value = rows
}

async function onBatchSetStatus(status: number) {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择商品')
    return
  }
  const action = status === 1 ? '上架' : '下架'
  const rows = status === 1
    ? selectedRows.value.filter((row) => row.auditStatus === 1)
    : [...selectedRows.value]
  if (status === 1 && rows.length !== selectedRows.value.length) {
    ElMessage.warning('仅审核通过的商品可以上架，已跳过未通过审核的商品')
  }
  if (!rows.length) return
  await ElMessageBox.confirm(`确定要批量${action}选中的 ${rows.length} 个商品？`, '提示', {
    type: 'warning',
  })
  loading.value = true
  try {
    await Promise.all(rows.map((row) => productApi.setStatus(row.id, status)))
    ElMessage.success(`批量${action}成功`)
    await fetchList()
  } finally {
    loading.value = false
  }
}

async function onToggleStatus(row: ProductListVO) {
  const next = row.status === 1 ? 0 : 1
  if (next === 1 && row.auditStatus !== 1) {
    ElMessage.warning(`当前商品${auditText[row.auditStatus ?? 0]}，请等待平台审核通过`)
    return
  }
  const action = next === 1 ? '上架' : '下架'
  await ElMessageBox.confirm(`确定要${action}商品「${row.name}」？`, '提示', {
    type: 'warning',
  })
  await productApi.setStatus(row.id, next)
  ElMessage.success(`${action}成功`)
  fetchList()
}

async function onRemove(row: ProductListVO) {
  await ElMessageBox.confirm(`确定要删除商品「${row.name}」？此操作不可撤销。`, '提示', {
    type: 'warning',
  })
  await productApi.remove(row.id)
  ElMessage.success('已删除')
  fetchList()
}

function resolveImageUrl(url?: string) {
  if (!url) return ''
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url
  return `${apiBaseUrl}${url.startsWith('/') ? url : `/${url}`}`
}

function priceRange(row: ProductListVO) {
  const min = Number(row.minPrice ?? 0)
  const max = Number(row.maxPrice ?? 0)
  if (min === max) return `¥ ${min.toFixed(2)}`
  return `¥ ${min.toFixed(2)} - ${max.toFixed(2)}`
}

function hasOriginalPrice(row: ProductListVO) {
  return Number(row.minOriginalPrice ?? 0) > 0 || Number(row.maxOriginalPrice ?? 0) > 0
}

function originalPriceRange(row: ProductListVO) {
  const min = Number(row.minOriginalPrice ?? 0)
  const max = Number(row.maxOriginalPrice ?? 0)
  if (!min && !max) return ''
  if (min === max || !max) return `¥ ${min.toFixed(2)}`
  if (!min) return `¥ ${max.toFixed(2)}`
  return `¥ ${min.toFixed(2)} - ${max.toFixed(2)}`
}

onMounted(async () => {
  await loadCategories()
  await fetchList()
})
</script>

<template>
  <div class="product-list">
    <div class="page-header">
      <div>
        <span class="page-kicker">PRODUCTS</span>
        <h1 class="page-title">商品管理</h1>
        <p class="page-desc">管理商品资料、价格库存、上下架与 SKU 信息。</p>
      </div>
      <el-button v-permission="'merchant:product:create'" type="primary" @click="onCreate">新增商品</el-button>
    </div>

    <el-card>
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="按商品名称搜索"
          clearable
          style="width: 260px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <el-cascader
          v-model="query.categoryId"
          :options="catOptions"
          :props="{ checkStrictly: true, emitPath: false }"
          placeholder="全部分类"
          clearable
          style="width: 220px"
          @change="onSearch"
          @clear="onSearch"
        />
        <el-select
          v-model="query.status"
          placeholder="全部状态"
          clearable
          style="width: 150px"
          @change="onSearch"
        >
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-select
          v-model="query.isRecommend"
          placeholder="全部推荐"
          clearable
          style="width: 150px"
          @change="onSearch"
        >
          <el-option label="推荐" :value="1" />
          <el-option label="普通" :value="0" />
        </el-select>
        <el-select
          v-model="query.isGroupBuy"
          placeholder="全部团购"
          clearable
          style="width: 150px"
          @change="onSearch"
        >
          <el-option label="团购" :value="1" />
          <el-option label="非团购" :value="0" />
        </el-select>
        <el-button type="primary" @click="onSearch">搜索</el-button>
        <el-button v-permission="'merchant:product:status'" :disabled="!selectedCount" @click="onBatchSetStatus(1)">
          一键上架
        </el-button>
        <el-button v-permission="'merchant:product:status'" :disabled="!selectedCount" @click="onBatchSetStatus(0)">
          一键下架
        </el-button>
        <span v-if="selectedCount" class="selection-tip">已选 {{ selectedCount }} 个</span>
      </div>

      <el-table v-loading="loading" :data="list" stripe @selection-change="onSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="主图" width="80">
          <template #default="{ row }">
            <el-image
              v-if="(row as ProductListVO).mainImage"
              :src="resolveImageUrl((row as ProductListVO).mainImage)"
              :preview-src-list="[resolveImageUrl((row as ProductListVO).mainImage)]"
              preview-teleported
              fit="cover"
              style="width: 48px; height: 48px; border-radius: 4px"
            />
            <span v-else class="no-image">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="200" />
        <el-table-column prop="categoryName" label="分类" width="140" />
        <el-table-column label="价格" width="170">
          <template #default="{ row }">
            <div class="price-cell">
              <div class="sale-price">{{ priceRange(row as ProductListVO) }}</div>
              <div v-if="hasOriginalPrice(row as ProductListVO)" class="original-price">
                {{ originalPriceRange(row as ProductListVO) }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="totalStock" label="库存" width="90" />
        <el-table-column prop="totalSales" label="销量" width="90" />
        <el-table-column label="团购" width="130">
          <template #default="{ row }">
            <div v-if="(row as ProductListVO).isGroupBuy === 1" class="group-buy-cell">
              <el-tag type="warning">团购</el-tag>
              <div class="group-buy-price">¥ {{ Number((row as ProductListVO).groupBuyPrice ?? 0).toFixed(2) }}</div>
              <div class="group-buy-count">{{ (row as ProductListVO).groupBuyRequiredCount }} 人成团</div>
            </div>
            <el-tag v-else type="info">普通</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="推荐" width="90">
          <template #default="{ row }">
            <el-tag :type="(row as ProductListVO).isRecommend === 1 ? 'success' : 'info'">
              {{ (row as ProductListVO).isRecommend === 1 ? '推荐' : '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="(row as ProductListVO).status === 1 ? 'success' : 'info'">
              {{ (row as ProductListVO).status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核" width="100">
          <template #default="{ row }">
            <el-tooltip v-if="(row as ProductListVO).auditStatus === 2 && (row as ProductListVO).auditReason" :content="(row as ProductListVO).auditReason" placement="top">
              <el-tag type="danger">已驳回</el-tag>
            </el-tooltip>
            <el-tag v-else :type="(row as ProductListVO).auditStatus === 1 ? 'success' : 'warning'">
              {{ auditText[(row as ProductListVO).auditStatus ?? 0] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'merchant:product:update'" link type="primary" @click="onEdit(row as ProductListVO)">编辑</el-button>
            <el-button
              v-permission="'merchant:product:status'"
              link
              :type="(row as ProductListVO).status === 1 ? 'warning' : 'success'"
              :disabled="(row as ProductListVO).status !== 1 && (row as ProductListVO).auditStatus !== 1"
              @click="onToggleStatus(row as ProductListVO)"
            >
              {{ (row as ProductListVO).status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button v-permission="'merchant:product:delete'" link type="danger" @click="onRemove(row as ProductListVO)">删除</el-button>
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
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.product-list :deep(.el-image) {
  overflow: hidden;
  border: 1px solid var(--shop-border);
  border-radius: 12px !important;
  background: #fff8ed;
}
.selection-tip {
  color: var(--shop-text-2);
  font-size: 13px;
}
.price-cell {
  line-height: 1.35;
}
.sale-price {
  color: #ff6600;
  font-weight: 700;
}
.original-price {
  margin-top: 2px;
  color: var(--shop-text-2);
  font-size: 12px;
  text-decoration: line-through;
}
.group-buy-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.group-buy-price {
  color: #ff6600;
  font-size: 12px;
  font-weight: 700;
}
.group-buy-count {
  color: var(--shop-text-2);
  font-size: 12px;
}
</style>
