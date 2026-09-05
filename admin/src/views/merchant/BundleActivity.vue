<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { bundleApi, type BundleActivity, type BundleActivityPayload } from '@/api/marketing'
import { productApi, type ProductListVO } from '@/api/product'

const router = useRouter()
const loading = ref(false)
const products = ref<ProductListVO[]>([])
const activities = ref<BundleActivity[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref<BundleActivityPayload>(emptyForm())

function emptyForm(): BundleActivityPayload {
  return { name: '', mainProductId: 0, itemProductIds: [], discountAmount: 0, startAt: '', endAt: '', status: 0 }
}

const productMap = computed(() => new Map(products.value.map(item => [item.id, item])))

async function load() {
  loading.value = true
  try {
    const [activityList, productPage] = await Promise.all([
      bundleApi.list(),
      productApi.page({ page: 1, size: 200, status: 1 }),
    ])
    activities.value = activityList || []
    products.value = productPage?.list || []
  } finally { loading.value = false }
}

function openCreate() { editingId.value = null; form.value = emptyForm(); dialogVisible.value = true }

function openEdit(item: any) {
  editingId.value = item.id || null
  form.value = { name: item.name, mainProductId: item.mainProductId, itemProductIds: item.items.map((child: any) => child.productId), discountAmount: item.discountAmount, startAt: item.startAt, endAt: item.endAt, status: item.status }
  dialogVisible.value = true
}

async function submit() {
  if (!form.value.name || !form.value.mainProductId || !form.value.itemProductIds.length || !form.value.discountAmount) {
    ElMessage.warning('请完整填写主商品、搭配商品和优惠金额'); return
  }
  if (!form.value.startAt || !form.value.endAt || new Date(form.value.startAt).getTime() >= new Date(form.value.endAt).getTime()) {
    ElMessage.warning('请填写有效的活动时间'); return
  }
  const payload = { ...form.value, itemProductIds: [...new Set(form.value.itemProductIds)] }
  if (editingId.value) await bundleApi.update(editingId.value, payload)
  else await bundleApi.create(payload)
  ElMessage.success('套餐已保存'); dialogVisible.value = false; await load()
}

async function disable(item: any) {
  await ElMessageBox.confirm(`停用“${item.name}”后，不会影响历史订单。确定停用吗？`, '停用套餐', { type: 'warning', confirmButtonText: '确认停用', cancelButtonText: '取消' })
  await bundleApi.disable(item.id!)
  ElMessage.success('套餐已停用'); await load()
}

function productLabel(id: number) { return productMap.value.get(id)?.name || `商品 #${id}` }

onMounted(load)
</script>

<template>
  <div class="bundle-activity page" v-loading="loading">
    <div class="page-header">
      <div><span class="page-kicker">BUNDLE COMBINATION</span><h1 class="page-title">搭配购套餐</h1><p class="page-desc">一个主商品搭配多个配件，固定减免套餐优惠；套餐不与其他营销活动叠加。</p></div>
      <div class="page-actions"><el-button @click="router.push('/merchant/marketing')">返回营销活动</el-button><el-button type="primary" @click="openCreate">新建套餐</el-button></div>
    </div>
    <el-alert title="活动边界" description="只有营销能力开关开启、活动在有效期内、商品上架且库存充足时，小程序才会展示并允许下单。" type="info" show-icon :closable="false" class="bundle-tip" />
    <el-card shadow="never" class="bundle-card">
      <el-table :data="activities" empty-text="暂无套餐">
        <el-table-column label="套餐" min-width="220"><template #default="{ row }"><div class="bundle-title">{{ row.name }}</div><div class="bundle-meta">主商品：{{ row.mainProductName || productLabel(row.mainProductId) }}</div></template></el-table-column>
        <el-table-column label="搭配商品" min-width="260"><template #default="{ row }"><el-tag v-for="child in row.items" :key="child.productId" size="small" class="item-tag">{{ child.productName || productLabel(child.productId) }}</el-tag></template></el-table-column>
        <el-table-column label="套餐优惠" width="120"><template #default="{ row }"><span class="price">- ¥{{ Number(row.discountAmount).toFixed(2) }}</span></template></el-table-column>
        <el-table-column label="有效期" min-width="250"><template #default="{ row }">{{ row.startAt }}<br>至 {{ row.endAt }}</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 && row.active ? 'success' : row.status === 2 ? 'info' : 'warning'">{{ row.active ? '生效中' : row.statusText }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="150"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button v-if="row.status !== 2" link type="danger" @click="disable(row)">停用</el-button></template></el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑套餐' : '新建套餐'" width="620px">
      <el-form label-width="100px">
        <el-form-item label="套餐名称"><el-input v-model="form.name" placeholder="如：耳机出行套餐" /></el-form-item>
        <el-form-item label="主商品"><el-select v-model="form.mainProductId" filterable placeholder="选择主商品" style="width:100%"><el-option v-for="item in products" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="搭配商品"><el-select v-model="form.itemProductIds" multiple filterable placeholder="选择 1 个或多个搭配商品" style="width:100%"><el-option v-for="item in products.filter(product => product.id !== form.mainProductId)" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="固定优惠"><el-input-number v-model="form.discountAmount" :min="0.01" :precision="2" :step="1" /><span class="form-suffix">元</span></el-form-item>
        <el-form-item label="活动时间"><el-date-picker v-model="form.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="开始时间" /><span class="date-separator">至</span><el-date-picker v-model="form.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="结束时间" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="0">草稿</el-radio><el-radio :value="1">启用</el-radio><el-radio :value="2">停用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">保存套餐</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.bundle-tip { margin-bottom: 18px; }.bundle-card { border-radius: 18px; }.bundle-title { color: var(--shop-ink); font-weight: 700; }.bundle-meta { margin-top: 4px; color: var(--shop-muted); font-size: 12px; }.item-tag { margin: 2px 6px 2px 0; }.price { color: #f4511e; font-weight: 700; }.form-suffix { margin-left: 8px; color: var(--shop-muted); }.date-separator { margin: 0 8px; color: var(--shop-muted); }
</style>
