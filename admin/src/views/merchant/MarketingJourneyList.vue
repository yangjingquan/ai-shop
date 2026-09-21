<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { couponTemplateApi, type CouponTemplate } from '@/api/marketing'
import { customerOperationsApi, type CustomerSegment } from '@/api/customerOperations'
import { marketingJourneyApi, type MarketingJourney, type MarketingJourneyPayload, type MarketingTrigger } from '@/api/marketingJourney'

const loading = ref(false), saving = ref(false), journeys = ref<MarketingJourney[]>([]), segments = ref<CustomerSegment[]>([]), coupons = ref<CouponTemplate[]>([])
const dialogVisible = ref(false), editing = ref<MarketingJourney | null>(null)
const triggerOptions: Array<{ value: MarketingTrigger; label: string; hint: string }> = [
  { value: 'REGISTER', label: '新用户注册', hint: '注册后尚未支付的用户' },
  { value: 'ORDER_PAID', label: '订单支付', hint: '订单支付完成后触达' },
  { value: 'BROWSE_NO_PURCHASE', label: '浏览未购', hint: '近 7 天浏览且未支付' },
  { value: 'COUPON_EXPIRING', label: '优惠券将过期', hint: '3 天内到期的可用券' },
  { value: 'DORMANT', label: '沉睡召回', hint: '超过 30 天未支付' },
  { value: 'MEMBER_UPGRADED', label: '会员升级', hint: '会员等级提升后触达' },
]
const form = reactive<MarketingJourneyPayload>({ name: '', triggerType: 'REGISTER', segmentId: null, delayMinutes: 0, couponTemplateId: null, notificationTitle: '', notificationContent: '', frequencyDays: 7, stopOnPaid: 1, status: 1 })

function emptyForm(trigger: MarketingTrigger = 'REGISTER'): MarketingJourneyPayload {
  const option = triggerOptions.find((item) => item.value === trigger)
  return { name: `${option?.label || '自动营销'}触达`, triggerType: trigger, segmentId: null, delayMinutes: 0, couponTemplateId: null, notificationTitle: '', notificationContent: '', frequencyDays: 7, stopOnPaid: trigger === 'ORDER_PAID' ? 0 : 1, status: 1 }
}
function couponLabel(item: CouponTemplate) { return `${item.name} · ¥${Number(item.amount).toFixed(2)}${Number(item.thresholdAmount) > 0 ? `（满${item.thresholdAmount}可用）` : ''}` }
function triggerLabel(trigger: MarketingTrigger) { return triggerOptions.find((item) => item.value === trigger)?.label || trigger }
function triggerHint(trigger: MarketingTrigger) { return triggerOptions.find((item) => item.value === trigger)?.hint || '' }
function formatTime(value?: string) { return value ? value.replace('T', ' ').slice(0, 16) : '-' }
async function load() {
  loading.value = true
  try {
    const [journeyData, segmentData, newUserCoupons, repurchaseCoupons] = await Promise.all([marketingJourneyApi.list(), customerOperationsApi.segments(), couponTemplateApi.list('NEW_USER'), couponTemplateApi.list('REPURCHASE_AFTER_PAID')])
    journeys.value = journeyData
    segments.value = segmentData.filter((item) => item.status === 1)
    coupons.value = [...new Map([...newUserCoupons, ...repurchaseCoupons].map((item) => [item.id, item])).values()].filter((item) => item.status === 1)
  } finally { loading.value = false }
}
function openCreate(trigger?: MarketingTrigger) { editing.value = null; Object.assign(form, emptyForm(trigger)); dialogVisible.value = true }
function openEdit(row: MarketingJourney) { editing.value = row; Object.assign(form, { name: row.name, triggerType: row.triggerType, segmentId: row.segmentId || null, delayMinutes: row.delayMinutes, couponTemplateId: row.couponTemplateId || null, notificationTitle: row.notificationTitle || '', notificationContent: row.notificationContent || '', frequencyDays: row.frequencyDays, stopOnPaid: row.stopOnPaid, status: row.status }); dialogVisible.value = true }
async function save() {
  if (!form.name.trim()) return ElMessage.warning('请填写旅程名称')
  if (!form.couponTemplateId && !(form.notificationTitle || '').trim() && !(form.notificationContent || '').trim()) return ElMessage.warning('请至少配置发券或站内消息')
  saving.value = true
  try {
    const payload = { ...form, name: form.name.trim(), notificationTitle: (form.notificationTitle || '').trim(), notificationContent: (form.notificationContent || '').trim() }
    if (editing.value) await marketingJourneyApi.update(editing.value.id, payload); else await marketingJourneyApi.create(payload)
    ElMessage.success('自动营销已保存')
    dialogVisible.value = false
    await load()
  } finally { saving.value = false }
}
async function scan(row: MarketingJourney) { await marketingJourneyApi.scan(row.id); ElMessage.success('已按最新用户指标扫描入旅，延时动作将由系统自动执行'); await load() }
onMounted(load)
</script>

<template>
  <div class="journey-page" v-loading="loading">
    <div class="page-header"><div><h1 class="page-title">自动营销</h1><p class="page-desc">按用户行为或状态进入旅程，支持分群过滤、延时触达、发券与站内消息；每 5 分钟自动扫描并执行到期动作。</p></div><el-button v-permission="'merchant:journey:manage'" type="primary" @click="openCreate()">新建旅程</el-button></div>
    <div v-permission="'merchant:journey:manage'" class="quick-start"><span>快捷创建</span><el-button v-for="item in triggerOptions" :key="item.value" text type="primary" @click="openCreate(item.value)">{{ item.label }}</el-button></div>
    <el-table :data="journeys" empty-text="暂无自动营销旅程"><el-table-column prop="name" label="旅程名称" min-width="170" /><el-table-column label="触发条件" min-width="150"><template #default="{ row }"><div>{{ triggerLabel(row.triggerType) }}</div><small>{{ triggerHint(row.triggerType) }}</small></template></el-table-column><el-table-column label="目标人群" min-width="130"><template #default="{ row }">{{ row.segmentId ? (segments.find((item) => item.id === row.segmentId)?.name || `分群 #${row.segmentId}`) : '全量用户' }}</template></el-table-column><el-table-column label="触达动作" min-width="160"><template #default="{ row }"><el-tag v-if="row.couponTemplateId" size="small">发券</el-tag><el-tag v-if="row.notificationTitle || row.notificationContent" size="small" type="info">站内消息</el-tag><span v-if="!row.couponTemplateId && !row.notificationTitle && !row.notificationContent">-</span></template></el-table-column><el-table-column label="效果记录" min-width="185"><template #default="{ row }"><span>入旅 {{ row.enrollmentCount || 0 }}</span><span class="stat-sep">成功 {{ row.successCount || 0 }}</span><br><small>券 {{ row.couponCount || 0 }} · 消息 {{ row.notificationCount || 0 }} · 跳过 {{ row.skippedCount || 0 }}</small></template></el-table-column><el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="更新时间" min-width="150"><template #default="{ row }">{{ formatTime(row.updatedAt) }}</template></el-table-column><el-table-column label="操作" width="190" fixed="right"><template #default="{ row }"><el-button v-permission="'merchant:journey:manage'" link type="primary" :disabled="row.status !== 1" @click="scan(row as unknown as MarketingJourney)">立即扫描</el-button><el-button v-permission="'merchant:journey:manage'" link @click="openEdit(row as unknown as MarketingJourney)">编辑</el-button></template></el-table-column></el-table>
  </div>
  <el-dialog v-model="dialogVisible" :title="editing ? '编辑自动营销' : '新建自动营销'" width="720px" destroy-on-close><el-form label-width="112px"><el-form-item label="旅程名称"><el-input v-model="form.name" maxlength="96" show-word-limit /></el-form-item><el-form-item label="触发条件"><el-select v-model="form.triggerType" style="width: 100%"><el-option v-for="item in triggerOptions" :key="item.value" :value="item.value" :label="`${item.label} · ${item.hint}`" /></el-select></el-form-item><el-form-item label="目标分群"><el-select v-model="form.segmentId" clearable placeholder="不选择则面向全部用户" style="width: 100%"><el-option v-for="item in segments" :key="item.id" :value="item.id" :label="`${item.name} · ${item.memberCount || 0}人`" /></el-select></el-form-item><el-form-item label="触达延时"><el-input-number v-model="form.delayMinutes" :min="0" :max="43200" /><span class="form-hint">分钟；0 表示满足条件后立即执行</span></el-form-item><el-form-item label="频控"><el-input-number v-model="form.frequencyDays" :min="1" :max="90" /><span class="form-hint">天内同一用户只成功触达一次</span></el-form-item><el-form-item label="用户已支付"><el-switch v-model="form.stopOnPaid" :active-value="1" :inactive-value="0" /><span class="form-hint">开启后，等待期间支付完成将跳过本次触达</span></el-form-item><el-divider content-position="left">触达动作（至少配置一个）</el-divider><el-form-item label="发放优惠券"><el-select v-model="form.couponTemplateId" clearable filterable placeholder="选择启用中的券模板" style="width: 100%"><el-option v-for="coupon in coupons" :key="coupon.id" :value="coupon.id" :label="couponLabel(coupon)" /></el-select></el-form-item><el-form-item label="站内消息标题"><el-input v-model="form.notificationTitle" maxlength="96" show-word-limit placeholder="例如：你的专属优惠已到账" /></el-form-item><el-form-item label="站内消息内容"><el-input v-model="form.notificationContent" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="可选；仅填写内容也会发送站内消息" /></el-form-item><el-form-item label="启用状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /><span class="form-hint">停用后不再扫描入旅，尚未执行的记录会自动取消</span></el-form-item></el-form><template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template></el-dialog>
</template>

<style scoped>
.journey-page { width: min(1360px, calc(100% - 56px)); margin: 28px auto 36px; }.page-header { display:flex; justify-content:space-between; align-items:flex-start; gap:24px; margin-bottom:16px; }.page-title { margin:0; color:var(--shop-text); font-size:28px; }.page-desc { margin:8px 0 0; color:var(--shop-text-muted); font-size:14px; line-height:1.65; }.quick-start { display:flex; align-items:center; flex-wrap:wrap; gap:2px 10px; padding:12px 14px; margin-bottom:18px; border:1px solid var(--shop-border); border-radius:10px; background:var(--shop-bg-soft); }.quick-start > span { margin-right:6px; color:var(--shop-text-muted); font-size:13px; }.form-hint { margin-left:10px; color:var(--shop-text-muted); font-size:12px; }.stat-sep { margin-left:10px; } small { color:var(--shop-text-muted); }
</style>
