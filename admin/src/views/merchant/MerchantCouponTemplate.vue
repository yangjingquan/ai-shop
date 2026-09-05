<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { couponTemplateApi, type CouponTemplate, type CouponTemplatePayload } from '@/api/marketing'
import ImageUploader from '@/components/upload/ImageUploader.vue'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const list = ref<CouponTemplate[]>([])
const form = reactive<CouponTemplatePayload>(emptyForm())

function emptyForm(): CouponTemplatePayload {
  return {
    name: '新人首单券', image: '', amount: 20, thresholdAmount: 99, totalStock: 0,
    perUserLimit: 1, validityDays: 30, validFrom: null, validTo: null,
    scopeType: 0, scopeIds: [], newUserOnly: 1, excludeActivityGoods: 1, stackable: 0, status: 1,
  }
}

async function load() {
  loading.value = true
  try { list.value = (await couponTemplateApi.list()) || [] } finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row: CouponTemplate) {
  editingId.value = row.id
  Object.assign(form, { ...row, scopeIds: row.scopeIds || [] })
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
    if (editingId.value) await couponTemplateApi.update(editingId.value, payload)
    else await couponTemplateApi.create(payload)
    ElMessage.success('新人券模板已保存')
    dialogVisible.value = false
    await load()
  } finally { saving.value = false }
}

async function toggle(row: CouponTemplate) {
  const next = row.status === 1 ? 2 : 1
  if (next === 2) await ElMessageBox.confirm('停止后不能再领取新券，但已发放且未过期的券仍可使用。确定停止吗？', '停止新人券', { type: 'warning' })
  await couponTemplateApi.status(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已停止')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page coupon-template-page">
    <div class="page-header">
      <div>
        <span class="page-kicker">NEW USER BENEFIT</span>
        <h1 class="page-title">新人首单券</h1>
        <p class="page-desc">仅支持满减券。活动开关在“营销活动”中控制，模板停止不会影响已发放券。</p>
      </div>
      <div class="header-actions">
        <el-button @click="load">刷新</el-button>
        <el-button v-permission="'merchant:coupon:create'" type="primary" @click="openCreate">新增模板</el-button>
      </div>
    </div>
    <el-alert title="建议先启用营销活动开关，再启用模板" description="每个用户最多领取一张新人券；库存填 0 表示不限库存，默认排除团购等活动商品。" type="info" show-icon :closable="false" class="tip" />
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
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑新人券' : '新增新人券'" width="620px">
      <el-form label-width="112px">
      <el-form-item label="模板名称"><el-input v-model="form.name" maxlength="32" /></el-form-item>
      <el-form-item label="券面图片"><ImageUploader v-model="form.image" scope="merchant" :limit="1" label="上传券面图片" /><span class="hint">用于积分商城等营销展示，不上传则显示默认券面。</span></el-form-item>
        <el-form-item label="优惠金额"><el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="1" /></el-form-item>
        <el-form-item label="使用门槛"><el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" :step="10" /></el-form-item>
        <el-form-item label="总库存"><el-input-number v-model="form.totalStock" :min="0" :step="100" /><span class="hint">0 表示不限</span></el-form-item>
        <el-form-item label="有效期"><el-input-number v-model="form.validityDays" :min="1" :max="365" /><span class="hint">领取后生效天数</span></el-form-item>
        <el-form-item label="新人限定"><el-switch v-model="form.newUserOnly" :active-value="1" :inactive-value="0" /></el-form-item>
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
