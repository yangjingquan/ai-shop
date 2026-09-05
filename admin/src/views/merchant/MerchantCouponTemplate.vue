<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { couponTemplateApi, type CouponIssueScene, type CouponTemplate, type CouponTemplatePayload } from '@/api/marketing'
import ImageUploader from '@/components/upload/ImageUploader.vue'
import { merchantCategoryApi, type MerchantCategoryVO } from '@/api/category'
import { productApi, type ProductListVO } from '@/api/product'

const route = useRoute()
const issueScene = computed<CouponIssueScene>(() => route.meta.couponIssueScene === 'REPURCHASE_AFTER_PAID'
  ? 'REPURCHASE_AFTER_PAID' : 'NEW_USER')
const isRepurchase = computed(() => issueScene.value === 'REPURCHASE_AFTER_PAID')
const sceneLabel = computed(() => isRepurchase.value ? '购后复购券' : '通用优惠券')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const list = ref<CouponTemplate[]>([])
const form = reactive<CouponTemplatePayload>(emptyForm())
const goods = ref<ProductListVO[]>([])
const categories = ref<MerchantCategoryVO[]>([])
const categoryOptions = computed(() => {
  const result: MerchantCategoryVO[] = []
  const walk = (items: MerchantCategoryVO[]) => items.forEach((item) => { result.push(item); if (item.children) walk(item.children) })
  walk(categories.value)
  return result
})

function emptyForm(scene = issueScene.value): CouponTemplatePayload {
  return {
    name: scene === 'REPURCHASE_AFTER_PAID' ? '购后复购券' : '通用优惠券', image: '', amount: 20, thresholdAmount: 99, totalStock: 0,
    perUserLimit: 1, validityDays: 30, validFrom: null, validTo: null,
    scopeType: 0, scopeIds: [], newUserOnly: scene === 'REPURCHASE_AFTER_PAID' ? 0 : 1, issueScene: scene,
    repurchaseTargetType: 0, repurchaseTargetIds: [], repurchaseMinOrderAmount: 0,
    repurchaseFirstPurchaseOnly: 0, repurchasePriority: 0,
    excludeActivityGoods: 1, stackable: 0, status: 1,
  }
}

async function load() {
  loading.value = true
  try {
    const [templates, productPage, tree] = await Promise.all([
      couponTemplateApi.list(issueScene.value), productApi.page({ page: 1, size: 200, status: 1, auditStatus: 1 }), merchantCategoryApi.enabledTree(),
    ])
    list.value = templates || []
    goods.value = productPage?.list || []
    categories.value = tree || []
  } finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm(issueScene.value))
  dialogVisible.value = true
}

function openEdit(row: CouponTemplate) {
  editingId.value = row.id
  Object.assign(form, { ...emptyForm(issueScene.value), ...row, issueScene: issueScene.value, scopeIds: row.scopeIds || [], repurchaseTargetIds: row.repurchaseTargetIds || [] })
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim() || form.amount <= 0 || form.thresholdAmount < 0) {
    ElMessage.warning('请完善券名称、面额和使用门槛')
    return
  }
  saving.value = true
  try {
    const { id, type, receivedCount, usedCount, statusText, ...payload } = form as CouponTemplate & CouponTemplatePayload
    payload.issueScene = issueScene.value
    if (editingId.value) await couponTemplateApi.update(editingId.value, payload)
    else await couponTemplateApi.create(issueScene.value, payload)
    ElMessage.success(`${sceneLabel.value}模板已保存`)
    dialogVisible.value = false
    await load()
  } finally { saving.value = false }
}

async function toggle(row: CouponTemplate) {
  const next = row.status === 1 ? 2 : 1
  if (next === 2) await ElMessageBox.confirm('停止后不会再发放新券，但已发放且未过期的券仍可使用。确定停止吗？', `停止${sceneLabel.value}`, { type: 'warning' })
  await couponTemplateApi.status(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已停止')
  await load()
}

onMounted(load)
watch(issueScene, load)
</script>

<template>
  <div class="page coupon-template-page">
    <div class="page-header">
      <div>
        <span class="page-kicker">COUPON CAMPAIGNS</span>
        <h1 class="page-title">{{ sceneLabel }}配置</h1>
        <p class="page-desc">{{ isRepurchase ? '支付成功后立即发放的复购券模板；停止活动或模板均不影响已发放券。' : '供新人首单、积分兑换、会员日和邀请奖励等活动发放使用。' }}</p>
      </div>
      <div class="header-actions">
        <el-button @click="load">刷新</el-button>
        <el-button v-permission="'merchant:coupon:create'" type="primary" @click="openCreate">新增{{ sceneLabel }}</el-button>
      </div>
    </div>
    <el-alert v-if="isRepurchase" title="复购券需要同时启用“营销活动 - 复购券”开关" description="第一版按支付成功立即发放；每个用户每个模板仅获得一张，支付成功后的全额退款会回收未使用券。" type="info" show-icon :closable="false" class="tip" />
    <el-alert v-else title="通用券与复购券独立管理" description="此处模板可由新人首单、积分兑换、会员日及邀请奖励使用，并非仅限新人；支付成功后自动发放的券请在“购后复购券配置”中维护。" type="info" show-icon :closable="false" class="tip" />
    <el-card>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="name" label="模板名称" min-width="150" />
        <el-table-column label="优惠" width="150"><template #default="{ row }">满{{ row.thresholdAmount }}减{{ row.amount }}</template></el-table-column>
        <el-table-column label="库存 / 已领" width="140"><template #default="{ row }">{{ row.totalStock === 0 ? '不限' : row.totalStock }} / {{ row.receivedCount }}</template></el-table-column>
        <el-table-column label="已使用" prop="usedCount" width="90" />
        <el-table-column label="有效期" width="150"><template #default="{ row }">领取后 {{ row.validityDays }} 天</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'">{{ row.statusText }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right"><template #default="{ row }">
          <el-button v-permission="'merchant:coupon:update'" link type="primary" @click="openEdit(row as CouponTemplate)">编辑</el-button>
          <el-button v-permission="'merchant:coupon:status'" link :type="row.status === 1 ? 'warning' : 'success'" @click="toggle(row as CouponTemplate)">{{ row.status === 1 ? '停止' : '启用' }}</el-button>
        </template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="editingId ? `编辑${sceneLabel}模板` : `新增${sceneLabel}`" width="680px">
      <el-form label-width="112px">
      <el-form-item label="模板名称"><el-input v-model="form.name" maxlength="32" /></el-form-item>
        <el-form-item label="发放场景"><el-tag :type="isRepurchase ? 'success' : 'primary'">{{ sceneLabel }}</el-tag><span class="hint">创建后不可变更</span></el-form-item>
      <el-form-item label="券面图片"><ImageUploader v-model="form.image" scope="merchant" :limit="1" label="上传券面图片" /><span class="hint">用于积分商城等营销展示，不上传则显示默认券面。</span></el-form-item>
        <el-form-item label="优惠金额"><el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="1" /></el-form-item>
        <el-form-item label="使用门槛"><el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" :step="10" /></el-form-item>
        <el-form-item label="总库存"><el-input-number v-model="form.totalStock" :min="0" :step="100" /><span class="hint">0 表示不限</span></el-form-item>
        <el-form-item label="有效期"><el-input-number v-model="form.validityDays" :min="1" :max="365" /><span class="hint">领取后生效天数</span></el-form-item>
        <template v-if="isRepurchase">
          <el-form-item label="触发节点"><el-tag type="success">支付成功立即发放</el-tag></el-form-item>
          <el-form-item label="目标订单"><el-select v-model="form.repurchaseTargetType"><el-option :value="0" label="所有普通订单"/><el-option :value="1" label="指定商品"/><el-option :value="2" label="指定分类"/></el-select></el-form-item>
          <el-form-item v-if="form.repurchaseTargetType === 1" label="指定商品"><el-select v-model="form.repurchaseTargetIds" multiple filterable placeholder="搜索商品"><el-option v-for="item in goods" :key="item.id" :value="item.id" :label="`${item.name} · ID ${item.id}`" /></el-select></el-form-item>
          <el-form-item v-if="form.repurchaseTargetType === 2" label="指定分类"><el-select v-model="form.repurchaseTargetIds" multiple filterable placeholder="选择分类"><el-option v-for="item in categoryOptions" :key="item.id" :value="item.id" :label="item.name" /></el-select></el-form-item>
          <el-form-item label="最低实付金额"><el-input-number v-model="form.repurchaseMinOrderAmount" :min="0" :precision="2" :step="10" /><span class="hint">0 表示不限制</span></el-form-item>
          <el-form-item label="仅首笔购买"><el-switch v-model="form.repurchaseFirstPurchaseOnly" :active-value="1" :inactive-value="0" /></el-form-item>
          <el-form-item label="匹配优先级"><el-input-number v-model="form.repurchasePriority" :min="0" /><span class="hint">数值越大越优先；每笔订单最多发一张</span></el-form-item>
          <el-form-item label="每人上限"><el-tag>每模板仅 1 张（P1 固定）</el-tag></el-form-item>
        </template>
        <el-form-item v-else label="新人限定"><el-tag>仅首单用户（固定）</el-tag></el-form-item>
        <el-form-item label="排除活动商品"><el-switch v-model="form.excludeActivityGoods" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="保存后状态"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">草稿</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button v-permission="editingId ? 'merchant:coupon:update' : 'merchant:coupon:create'" type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-actions { display: flex; gap: 12px; }
.tip { margin-bottom: 18px; }
.hint { margin-left: 12px; color: var(--shop-muted); font-size: 12px; }
</style>
