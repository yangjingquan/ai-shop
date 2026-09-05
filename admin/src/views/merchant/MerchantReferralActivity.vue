<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { couponTemplateApi, referralApi, type CouponTemplate, type ReferralCampaign, type ReferralCampaignPayload, type ReferralRelation, type ReferralReward, type ReferralStats } from '@/api/marketing'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const campaigns = ref<ReferralCampaign[]>([])
const templates = ref<CouponTemplate[]>([])
const selectedId = ref<number | null>(null)
const stats = ref<ReferralStats | null>(null)
const relations = ref<ReferralRelation[]>([])
const rewards = ref<ReferralReward[]>([])
const activeTab = ref('overview')

const emptyForm = (): ReferralCampaign => ({
  name: '分享有礼·老带新返券', shareTitle: '邀请好友，双方都能得券', shareDescription: '邀请好友完成首单，双方都能得券，多邀多得',
  landingProductId: null, inviteeCouponTemplateId: null,
  tiers: [1, 3, 5].map((inviteCount) => ({ inviteCount, inviterCouponTemplateId: null })),
  startAt: '', endAt: '', maxDailyInvites: 20, maxTotalInvites: 0, status: 0,
})
const form = reactive<ReferralCampaign>(emptyForm())

const selectedCampaign = computed(() => campaigns.value.find((item) => item.id === selectedId.value) || null)
const activeTemplates = computed(() => templates.value.filter((item) => item.status === 1))

async function load() {
  loading.value = true
  try {
    const [campaignList, templateList] = await Promise.all([referralApi.list(), couponTemplateApi.list()])
    campaigns.value = campaignList || []
    templates.value = templateList || []
    if (!selectedId.value && campaigns.value[0]?.id) selectedId.value = campaigns.value[0].id
    if (selectedId.value) await loadDetails(selectedId.value)
  } finally { loading.value = false }
}

async function loadDetails(id: number) {
  const [statsData, relationData, rewardData] = await Promise.all([referralApi.stats(id), referralApi.relations(id), referralApi.rewards(id)])
  stats.value = statsData || null; relations.value = relationData || []; rewards.value = rewardData || []
}

function openCreate() { editingId.value = null; Object.assign(form, emptyForm()); dialogVisible.value = true }
async function openEdit(row: unknown) {
  const campaign = row as ReferralCampaign
  if (!campaign.id) return
  const detail = await referralApi.get(campaign.id)
  editingId.value = campaign.id
  Object.assign(form, { ...detail, tiers: (detail.tiers || [1, 3, 5].map((inviteCount) => ({ inviteCount, inviterCouponTemplateId: null }))).map((item) => ({ ...item })) })
  dialogVisible.value = true
}

function validate() {
  if (!form.name.trim() || !form.shareTitle.trim()) return '请填写活动名称和分享标题'
  if (!form.inviteeCouponTemplateId) return '请选择好友新人券'
  if (!form.startAt || !form.endAt || form.startAt >= form.endAt) return '请填写正确的活动时间'
  if ((form.tiers || []).some((item) => !item.inviterCouponTemplateId)) return '请为每个阶梯选择邀请人奖励券'
  return ''
}

function buildPayload(): ReferralCampaignPayload {
  return {
    name: form.name.trim(),
    shareTitle: form.shareTitle.trim(),
    shareDescription: form.shareDescription?.trim() || '',
    landingProductId: form.landingProductId ?? null,
    inviteeCouponTemplateId: form.inviteeCouponTemplateId ?? null,
    tiers: form.tiers.map((item) => ({
      inviteCount: item.inviteCount,
      inviterCouponTemplateId: item.inviterCouponTemplateId,
    })),
    startAt: form.startAt,
    endAt: form.endAt,
    maxDailyInvites: form.maxDailyInvites,
    maxTotalInvites: form.maxTotalInvites,
    status: form.status ?? 0,
  }
}

async function save() {
  const error = validate(); if (error) return ElMessage.warning(error)
  saving.value = true
  try {
    const payload = buildPayload()
    if (editingId.value) await referralApi.update(editingId.value, payload)
    else await referralApi.create(payload)
    ElMessage.success('邀请有礼活动已保存'); dialogVisible.value = false; await load()
  } finally { saving.value = false }
}

async function toggleCampaign(row: unknown) {
  const campaign = row as ReferralCampaign
  if (!campaign.id) return
  const next = campaign.status === 1 ? 2 : 1
  if (next === 2) await ElMessageBox.confirm('暂停后小程序入口和活动接口将停止，但已支付订单仍会正常履约。确定暂停吗？', '暂停邀请活动', { type: 'warning' })
  await referralApi.status(campaign.id, next); ElMessage.success(next === 1 ? '活动已发布' : '活动已暂停'); await load()
}

async function chooseCampaign(id: number) { selectedId.value = id; activeTab.value = 'overview'; await loadDetails(id) }
async function freeze(row: unknown) { const relation = row as ReferralRelation; if (!selectedId.value) return; await ElMessageBox.confirm('冻结后该关系不再产生邀请奖励，确定冻结吗？', '冻结邀请关系', { type: 'warning' }); await referralApi.freezeRelation(selectedId.value, relation.id); ElMessage.success('关系已冻结'); await loadDetails(selectedId.value) }
async function revoke(row: unknown) { const reward = row as ReferralReward; if (!selectedId.value) return; await ElMessageBox.confirm('仅未使用优惠券可以撤销，确定撤销该奖励吗？', '撤销奖励', { type: 'warning' }); await referralApi.revokeReward(selectedId.value, reward.id, '后台风控撤销'); ElMessage.success('奖励已撤销'); await loadDetails(selectedId.value) }
function templateLabel(id: number | null | undefined) { const item = templates.value.find((row) => row.id === id); return item ? `${item.name} · 满${item.thresholdAmount}减${item.amount}` : '未配置' }
onMounted(load)
</script>

<template>
  <div class="page referral-page">
    <div class="page-header"><div><span class="page-kicker">REFERRAL GROWTH</span><h1 class="page-title">分享有礼</h1><p class="page-desc">邀请新用户完成首单，按 1 / 3 / 5 人阶梯发放店铺优惠券。活动开关在“营销活动”中控制。</p></div><div class="header-actions"><el-button @click="load">刷新</el-button><el-button v-permission="'merchant:referral:create'" type="primary" @click="openCreate">新建活动</el-button></div></div>
    <el-alert title="活动生效条件" description="营销开关开启、活动状态为进行中且在活动时间内，三项同时满足才会展示。关闭或暂停不会影响已支付订单履约。" type="info" show-icon :closable="false" class="tip" />
    <el-table v-loading="loading" :data="campaigns" class="campaign-table" @row-click="(row: ReferralCampaign) => row.id && chooseCampaign(row.id)"><el-table-column prop="name" label="活动名称" min-width="220" /><el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : 'info'">{{ row.statusText }}</el-tag></template></el-table-column><el-table-column label="活动时间" min-width="250"><template #default="{ row }">{{ row.startAt }} 至 {{ row.endAt }}</template></el-table-column><el-table-column label="操作" width="210"><template #default="{ row }"><el-button v-permission="'merchant:referral:update'" link type="primary" @click.stop="openEdit(row)">编辑</el-button><el-button v-permission="'merchant:referral:status'" link :type="row.status === 1 ? 'warning' : 'success'" @click.stop="toggleCampaign(row)">{{ row.status === 1 ? '暂停' : '发布' }}</el-button></template></el-table-column></el-table>
    <template v-if="selectedCampaign && selectedId">
      <div class="detail-head"><div><h2>{{ selectedCampaign.name }}</h2><span>{{ selectedCampaign.startAt }} 至 {{ selectedCampaign.endAt }}</span></div><el-tag>{{ selectedCampaign.statusText }}</el-tag></div>
      <el-tabs v-model="activeTab" class="detail-tabs"><el-tab-pane label="数据概览" name="overview"><el-row :gutter="14" class="stat-grid"><el-col v-for="item in [{ label: '分享发起', value: stats?.shares }, { label: '分享打开', value: stats?.opens }, { label: '落地注册', value: stats?.registrations }, { label: '好友首购', value: stats?.firstPurchases }, { label: '已发奖励', value: stats?.rewardsIssued }]" :key="item.label" :span="4"><el-card shadow="never" class="stat-card"><div>{{ item.value || 0 }}</div><span>{{ item.label }}</span></el-card></el-col><el-col :span="4"><el-card shadow="never" class="stat-card"><div>¥{{ Number(stats?.rewardCost || 0).toFixed(2) }}</div><span>奖励成本</span></el-card></el-col></el-row><el-card shadow="never" class="rule-card"><template #header>当前活动规则</template><p>好友新人券：{{ templateLabel(selectedCampaign.inviteeCouponTemplateId) }}</p><p v-for="tier in selectedCampaign.tiers" :key="tier.inviteCount">邀请满 {{ tier.inviteCount }} 人：{{ templateLabel(tier.inviterCouponTemplateId) }}</p></el-card></el-tab-pane><el-tab-pane label="邀请关系" name="relations"><el-table :data="relations" stripe><el-table-column prop="id" label="关系 ID" width="90" /><el-table-column label="邀请人 / 好友" width="180"><template #default="{ row }">{{ row.inviterUserId }} / {{ row.inviteeUserId }}</template></el-table-column><el-table-column prop="statusText" label="状态" width="120" /><el-table-column prop="firstOrderNo" label="首单号" min-width="170" /><el-table-column prop="boundAt" label="绑定时间" min-width="170" /><el-table-column label="操作" width="110"><template #default="{ row }"><el-button v-if="row.status === 0 || row.status === 1" v-permission="'merchant:referral:relation:freeze'" link type="danger" @click="freeze(row)">冻结</el-button></template></el-table-column></el-table></el-tab-pane><el-tab-pane label="奖励记录" name="rewards"><el-table :data="rewards" stripe><el-table-column prop="id" label="奖励 ID" width="90" /><el-table-column label="用户 / 类型" width="180"><template #default="{ row }">{{ row.userId }} / {{ row.role === 'INVITER' ? '邀请人' : '好友' }}</template></el-table-column><el-table-column label="阶梯" width="90"><template #default="{ row }">{{ row.tier || '新人券' }}</template></el-table-column><el-table-column label="金额" width="100"><template #default="{ row }">¥{{ row.rewardAmount }}</template></el-table-column><el-table-column prop="statusText" label="状态" width="110" /><el-table-column prop="triggerOrderNo" label="触发订单" min-width="170" /><el-table-column label="操作" width="110"><template #default="{ row }"><el-button v-if="row.status === 1" v-permission="'merchant:referral:reward:revoke'" link type="danger" @click="revoke(row)">撤销</el-button></template></el-table-column></el-table></el-tab-pane></el-tabs>
    </template>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑邀请活动' : '新建邀请活动'" width="720px"><el-form label-width="120px"><el-form-item label="活动名称"><el-input v-model="form.name" maxlength="128" /></el-form-item><el-form-item label="分享标题"><el-input v-model="form.shareTitle" maxlength="128" /></el-form-item><el-form-item label="活动说明"><el-input v-model="form.shareDescription" maxlength="255" /></el-form-item><el-form-item label="落地商品 ID"><el-input-number v-model="form.landingProductId" :min="1" :controls="false" /><span class="hint">可选，不填则返回商城</span></el-form-item><el-form-item label="好友新人券"><el-select v-model="form.inviteeCouponTemplateId" filterable placeholder="选择新人券" style="width: 100%"><el-option v-for="item in activeTemplates" :key="item.id" :label="templateLabel(item.id)" :value="item.id" /></el-select></el-form-item><el-form-item v-for="tier in form.tiers" :key="tier.inviteCount" :label="`邀请满${tier.inviteCount}人`"><el-select v-model="tier.inviterCouponTemplateId" filterable placeholder="选择邀请人奖励券" style="width: 100%"><el-option v-for="item in activeTemplates" :key="item.id" :label="templateLabel(item.id)" :value="item.id" /></el-select></el-form-item><el-form-item label="活动时间"><el-date-picker v-model="form.startAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="开始时间" /><span class="to">至</span><el-date-picker v-model="form.endAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="结束时间" /></el-form-item><el-form-item label="每日邀请上限"><el-input-number v-model="form.maxDailyInvites" :min="0" /><span class="hint">0 表示不限</span></el-form-item><el-form-item label="活动总上限"><el-input-number v-model="form.maxTotalInvites" :min="0" /><span class="hint">0 表示不限</span></el-form-item><el-form-item label="保存后状态"><el-radio-group v-model="form.status"><el-radio :value="0">草稿</el-radio><el-radio :value="1">发布</el-radio></el-radio-group></el-form-item></el-form><template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.header-actions { display: flex; gap: 12px; }.tip { margin-bottom: 18px; }.campaign-table { border-radius: 16px; overflow: hidden; }.detail-head { display: flex; align-items: center; justify-content: space-between; margin: 26px 0 12px; padding: 18px 20px; border: 1px solid var(--shop-border); border-radius: 16px; background: rgba(255,253,248,.82); }.detail-head h2 { margin: 0 0 6px; font-size: 18px; }.detail-head span { color: var(--shop-muted); font-size: 13px; }.stat-grid { margin-bottom: 16px; }.stat-card { text-align: center; border-radius: 14px; }.stat-card div { color: var(--shop-primary-dark); font-size: 24px; font-weight: 800; }.stat-card span { display: block; margin-top: 6px; color: var(--shop-muted); font-size: 12px; }.rule-card { margin-top: 16px; }.rule-card p { margin: 8px 0; color: var(--shop-muted); }.hint { margin-left: 12px; color: var(--shop-muted); font-size: 12px; }.to { margin: 0 10px; color: var(--shop-muted); }
</style>
