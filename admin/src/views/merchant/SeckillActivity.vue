<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { productApi, type ProductDetailVO, type ProductListVO } from '@/api/product'
import { seckillApi, type SeckillActivity, type SeckillSessionConfig, type SeckillSkuConfig } from '@/api/seckill'

const loading = ref(false)
const saving = ref(false)
const activities = ref<SeckillActivity[]>([])
const products = ref<ProductListVO[]>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)

function emptySku(): SeckillSkuConfig {
  return { productId: null, skuId: null, activityPrice: null, activityStock: null, userLimit: 1, skuOptions: [] }
}

function emptySession(): SeckillSessionConfig {
  return { name: '第一场', startAt: '', endAt: '', sort: 0, skus: [emptySku()] }
}

const form = ref<SeckillActivity>({ activityName: '', description: '', preheatAt: '', sessions: [emptySession()] } as SeckillActivity)

async function load() {
  loading.value = true
  try {
    const [page, productPage] = await Promise.all([
      seckillApi.page({ page: 1, size: 50 }),
      productApi.page({ page: 1, size: 100, status: 1, auditStatus: 1 }),
    ])
    activities.value = page?.list || []
    products.value = productPage?.list || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { activityName: '', description: '', preheatAt: '', sessions: [emptySession()] } as SeckillActivity
  dialogVisible.value = true
}

async function openEdit(row: SeckillActivity) {
  if (!row.id) return
  const detail = await seckillApi.get(row.id)
  editingId.value = row.id
  form.value = {
    activityName: detail.name || '',
    description: detail.description || '',
    preheatAt: detail.preheatAt || '',
    sessions: (detail.sessions || []).map((session) => ({
      name: session.name,
      startAt: session.startAt || '',
      endAt: session.endAt || '',
      sort: session.sort || 0,
      skus: (session.skus || []).map((sku) => ({ ...sku, skuOptions: [] })),
    })),
  } as SeckillActivity
  await Promise.all((form.value.sessions || []).flatMap((session) =>
    session.skus.map((sku) => loadSkuOptions(sku, false))))
  dialogVisible.value = true
}

function addSession() {
  form.value.sessions = [...(form.value.sessions || []), emptySession()]
}

function removeSession(index: number) {
  if ((form.value.sessions || []).length <= 1) return
  form.value.sessions?.splice(index, 1)
}

function addSku(session: SeckillSessionConfig) {
  session.skus.push(emptySku())
}

function removeSku(session: SeckillSessionConfig, index: number) {
  if (session.skus.length <= 1) return
  session.skus.splice(index, 1)
}

async function loadSkuOptions(sku: SeckillSkuConfig, reset = true) {
  if (!sku.productId) {
    sku.skuId = null
    sku.skuOptions = []
    return
  }
  const detail: ProductDetailVO = await productApi.get(Number(sku.productId))
  sku.skuOptions = (detail.skus || []).map((item) => ({ id: item.id, specText: item.specText, price: item.price, stock: item.stock }))
  if (reset) sku.skuId = null
}

async function productChanged(sku: SeckillSkuConfig) {
  await loadSkuOptions(sku)
}

function selectedSku(sku: SeckillSkuConfig) {
  return (sku.skuOptions || []).find((item) => item.id === sku.skuId)
}

function sessionCount(row: SeckillActivity) {
  return row.sessions?.length || 0
}

function skuCount(row: SeckillActivity) {
  return (row.sessions || []).reduce((sum: number, session: SeckillSessionConfig) => sum + session.skus.length, 0)
}

function activityTime(row: SeckillActivity) {
  const sessions = row.sessions || []
  if (!sessions.length) return '-'
  return `${sessions[0].startAt || '-'} 至 ${sessions[sessions.length - 1].endAt || '-'}`
}

function validate() {
  if (!form.value.activityName?.trim()) return '请填写活动名称'
  for (const session of form.value.sessions || []) {
    if (!session.name || !session.startAt || !session.endAt) return '请完整填写场次信息'
    if (!session.skus.length) return '每个场次至少配置一个 SKU'
    for (const sku of session.skus) {
      const selected = selectedSku(sku)
      if (!sku.productId || !sku.skuId || !selected || !sku.activityPrice || !sku.activityStock) return '请完整填写商品、SKU、活动价和库存'
      if (Number(sku.activityPrice) >= Number(selected.price)) return '秒杀价必须低于日常销售价'
      if (Number(sku.activityStock) > Number(selected.stock)) return '活动库存不能超过当前 SKU 库存'
    }
  }
  return ''
}

async function save() {
  const message = validate()
  if (message) {
    ElMessage.warning(message)
    return
  }
  saving.value = true
  try {
    // 详情接口包含 soldCount、productName、specText 等只读字段，保存时只提交可编辑字段。
    const payload: SeckillActivity = {
      activityName: form.value.activityName,
      description: form.value.description,
      preheatAt: form.value.preheatAt || null,
      sessions: (form.value.sessions || []).map((session) => ({
        name: session.name,
        startAt: session.startAt,
        endAt: session.endAt,
        sort: session.sort,
        skus: session.skus.map((sku) => ({
          productId: sku.productId,
          skuId: sku.skuId,
          activityPrice: sku.activityPrice,
          activityStock: sku.activityStock,
          userLimit: sku.userLimit,
        })),
      })),
    }
    if (editingId.value) await seckillApi.update(editingId.value, payload)
    else await seckillApi.create(payload)
    ElMessage.success(editingId.value ? '秒杀活动已更新' : '秒杀活动已发布')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function confirmEdit(row: SeckillActivity) {
  if (!row.id) return
  try {
    await ElMessageBox.confirm('活动开始后不允许整体编辑，请确认当前所有场次尚未开始。', '编辑秒杀活动', { type: 'warning' })
    await openEdit(row)
  } catch { /* cancelled */ }
}

onMounted(load)
</script>

<template>
  <div class="seckill-page page">
    <div class="page-header">
      <div>
        <span class="page-kicker">FLASH SALE OPERATIONS</span>
        <h1 class="page-title">秒杀活动</h1>
        <p class="page-desc">配置多场次限时折扣。活动库存独立管理，订单创建时同时校验活动库存和普通库存。</p>
      </div>
      <div class="page-actions"><el-button @click="load">刷新</el-button><el-button type="primary" @click="openCreate">新建活动</el-button></div>
    </div>
    <el-alert title="使用前请先在营销活动中启用“限时秒杀”开关" description="秒杀价必须低于 SKU 日常价；秒杀订单不参与优惠券，活动开始后不可整体编辑。" type="info" show-icon :closable="false" class="tip" />
    <el-table v-loading="loading" :data="activities" class="activity-table">
      <el-table-column prop="name" label="活动名称" min-width="180" />
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.statusText }}</el-tag></template></el-table-column>
      <el-table-column label="场次" width="90"><template #default="{ row }">{{ sessionCount(row) }} 场</template></el-table-column>
      <el-table-column label="时间" min-width="220"><template #default="{ row }">{{ activityTime(row) }}</template></el-table-column>
      <el-table-column label="商品数" width="100"><template #default="{ row }">{{ skuCount(row) }}</template></el-table-column>
      <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="confirmEdit(row)">编辑</el-button></template></el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑秒杀活动' : '新建秒杀活动'" width="980px" top="5vh">
      <el-form label-width="100px">
        <el-form-item label="活动名称"><el-input v-model="form.activityName" maxlength="128" placeholder="如：周末数码秒杀" /></el-form-item>
        <el-form-item label="活动说明"><el-input v-model="form.description" maxlength="500" placeholder="展示在会场顶部的说明" /></el-form-item>
        <el-form-item label="预热时间"><el-date-picker v-model="form.preheatAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="可选" /></el-form-item>
        <div v-for="(session, sessionIndex) in form.sessions" :key="sessionIndex" class="session-editor">
          <div class="session-editor-head"><strong>场次 {{ sessionIndex + 1 }}</strong><el-button v-if="(form.sessions || []).length > 1" link type="danger" @click="removeSession(sessionIndex)">删除场次</el-button></div>
          <el-form-item label="场次名称"><el-input v-model="session.name" placeholder="如：10:00 抢购场" /></el-form-item>
          <el-form-item label="时间"><el-date-picker v-model="session.startAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="开始时间" /><span class="to">至</span><el-date-picker v-model="session.endAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="结束时间" /></el-form-item>
          <div v-for="(sku, skuIndex) in session.skus" :key="skuIndex" class="sku-editor">
            <el-select v-model="sku.productId" filterable placeholder="选择商品" @change="productChanged(sku)"><el-option v-for="product in products" :key="product.id" :label="product.name" :value="product.id" /></el-select>
            <el-select v-model="sku.skuId" placeholder="选择 SKU" :disabled="!sku.skuOptions?.length"><el-option v-for="option in sku.skuOptions" :key="option.id" :label="`${option.specText || '默认规格'} · ¥${option.price} · 库存${option.stock}`" :value="option.id" /></el-select>
            <div class="sku-field"><span class="sku-field-label">秒杀价（元）</span><el-input-number v-model="sku.activityPrice" :min="0.01" :precision="2" placeholder="请输入价格" /></div>
            <div class="sku-field"><span class="sku-field-label">活动库存（件）</span><el-input-number v-model="sku.activityStock" :min="1" :precision="0" placeholder="请输入库存" /></div>
            <div class="sku-field"><span class="sku-field-label">每人限购（件）</span><el-input-number v-model="sku.userLimit" :min="1" :max="99" :precision="0" /></div>
            <el-button link type="danger" @click="removeSku(session, skuIndex)">移除</el-button>
          </div>
          <el-button link type="primary" @click="addSku(session)">+ 添加 SKU</el-button>
        </div>
        <el-button class="add-session" @click="addSession">+ 添加场次</el-button>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存并发布</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tip { margin-bottom: 18px; }
.activity-table { border-radius: 16px; overflow: hidden; }
.session-editor { margin: 18px 0; padding: 16px; border: 1px solid var(--shop-line); border-radius: 14px; background: #fffaf5; }
.session-editor-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; color: var(--shop-ink); }
.sku-editor { display: grid; grid-template-columns: minmax(170px, 1.4fr) minmax(180px, 1.6fr) 130px 130px 120px 50px; gap: 8px; margin: 10px 0; align-items: end; }
.sku-field { min-width: 0; }
.sku-field-label { display: block; margin-bottom: 5px; color: var(--shop-muted); font-size: 12px; line-height: 1.2; }
.sku-field :deep(.el-input-number) { width: 100%; }
.to { margin: 0 10px; color: var(--shop-muted); }
.add-session { width: 100%; margin-top: 8px; border-style: dashed; }
@media (max-width: 900px) { .sku-editor { grid-template-columns: 1fr 1fr; } }
</style>
