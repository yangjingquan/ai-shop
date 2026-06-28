<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { productApi, type ProductListVO } from '@/api/product'

const router = useRouter()

const loading = ref(false)
const list = ref<ProductListVO[]>([])
const total = ref(0)
const query = reactive<{
  page: number
  size: number
  keyword: string
  status: number | undefined
}>({ page: 1, size: 10, keyword: '', status: undefined })

async function fetchList() {
  loading.value = true
  try {
    const data = await productApi.page({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status,
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

async function onToggleStatus(row: ProductListVO) {
  const next = row.status === 1 ? 0 : 1
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

function priceRange(row: ProductListVO) {
  const min = Number(row.minPrice ?? 0)
  const max = Number(row.maxPrice ?? 0)
  if (min === max) return `¥ ${min.toFixed(2)}`
  return `¥ ${min.toFixed(2)} - ${max.toFixed(2)}`
}

onMounted(fetchList)
</script>

<template>
  <div class="product-list">
    <div class="page-header">
      <div>
        <span class="page-kicker">PRODUCTS</span>
        <h1 class="page-title">商品管理</h1>
        <p class="page-desc">管理商品资料、价格库存、上下架与 SKU 信息。</p>
      </div>
      <el-button type="primary" @click="onCreate">新增商品</el-button>
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
        <el-button type="primary" @click="onSearch">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="主图" width="80">
          <template #default="{ row }">
            <el-image
              v-if="(row as ProductListVO).mainImage"
              :src="(row as ProductListVO).mainImage"
              fit="cover"
              style="width: 48px; height: 48px; border-radius: 4px"
            />
            <span v-else class="no-image">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="200" />
        <el-table-column prop="categoryName" label="分类" width="140" />
        <el-table-column label="价格" width="160">
          <template #default="{ row }">
            {{ priceRange(row as ProductListVO) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalStock" label="库存" width="90" />
        <el-table-column prop="totalSales" label="销量" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="(row as ProductListVO).status === 1 ? 'success' : 'info'">
              {{ (row as ProductListVO).status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row as ProductListVO)">编辑</el-button>
            <el-button
              link
              :type="(row as ProductListVO).status === 1 ? 'warning' : 'success'"
              @click="onToggleStatus(row as ProductListVO)"
            >
              {{ (row as ProductListVO).status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="onRemove(row as ProductListVO)">删除</el-button>
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
</style>
