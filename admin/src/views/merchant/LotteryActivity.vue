<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { couponTemplateApi, type CouponTemplate } from '@/api/marketing'
import { productApi, type ProductDetailVO, type ProductListVO } from '@/api/product'
import { lotteryApi, type LotteryActivity, type LotteryPrize, type LotteryReward, type LotteryStats } from '@/api/lottery'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const activities = ref<LotteryActivity[]>([])
const goods = ref<ProductListVO[]>([])
const coupons = ref<CouponTemplate[]>([])
const selectedId = ref<number | null>(null)
const selected = ref<LotteryActivity | null>(null)
const stats = ref<LotteryStats | null>(null)
const rewards = ref<LotteryReward[]>([])
const dialog = ref(false)
const editingId = ref<number | null>(null)

function defaultCondition() { return { minMemberLevel: 1, minPoints: 0, firstOrderOnly: false, repurchaseOnly: false } }
function emptyPrize(): LotteryPrize { return { name: '', prizeType: 'POINTS', image: '', pointsAmount: 100, couponTemplateId: null, productId: null, skuId: null, totalStock: 0, probability: 0.25, validityDays: 0, sort: 0, status: 1, skuOptions: [] } }
function emptyForm(): LotteryActivity { return { name: '', themeImage: '', entryImage: '', ruleText: '每日可参与抽奖，奖品概率以页面公示为准。中奖后请在我的奖品中查看，实物奖品需填写收货地址。', condition: defaultCondition(), dailyChances: 1, startAt: '', endAt: '', status: 0, prizes: [emptyPrize(), { ...emptyPrize(), name: '满减优惠券', prizeType: 'COUPON', pointsAmount: null, probability: 0.25 }, { ...emptyPrize(), name: '实物好礼', prizeType: 'PHYSICAL', pointsAmount: null, totalStock: 10, probability: 0.1 }, { ...emptyPrize(), name: '谢谢参与', prizeType: 'CONSOLATION', pointsAmount: null, probability: 0.4 }] } }
const form = ref<LotteryActivity>(emptyForm())

async function load() {
  loading.value = true
  try {
    const [list, couponList, productPage] = await Promise.all([
      lotteryApi.list(),
      userStore.hasPermission('merchant:coupon:view') ? couponTemplateApi.list() : Promise.resolve([]),
      userStore.hasPermission('merchant:product:view')
        ? productApi.page({ page: 1, size: 200, status: 1, auditStatus: 1 }) : Promise.resolve(null),
    ])
    activities.value = list || []
    coupons.value = (couponList || []).filter((item) => item.status === 1)
    goods.value = productPage?.list || []
    if (selectedId.value && activities.value.some((item) => item.id === selectedId.value)) await selectActivity(selectedId.value)
    else if (activities.value[0]?.id) await selectActivity(activities.value[0].id)
    else clearSelection()
  } finally {
    loading.value = false
  }
}

async function selectActivity(id: number) {
  selectedId.value = id
  const [detail, summary, rewardList] = await Promise.all([lotteryApi.get(id), lotteryApi.stats(id), lotteryApi.rewards(id)])
  selected.value = detail
  stats.value = summary
  rewards.value = rewardList || []
}

function clearSelection() { selectedId.value = null; selected.value = null; stats.value = null; rewards.value = [] }
function openCreate() { editingId.value = null; form.value = emptyForm(); dialog.value = true }

async function openEdit(row: any) {
  if (!row.id) return
  const detail = await lotteryApi.get(row.id)
  editingId.value = row.id
  form.value = {
    ...detail,
    condition: { ...defaultCondition(), ...(detail.condition || {}) },
    prizes: (detail.prizes || []).map((item) => ({ ...item, skuOptions: [] })),
  }
  if (userStore.hasPermission('merchant:product:view')) {
    await Promise.all(form.value.prizes.filter((item) => item.productId).map((item) => loadSkus(item, false)))
  }
  dialog.value = true
}

function addPrize() { form.value.prizes.push(emptyPrize()) }
function removePrize(index: number) { if (form.value.prizes.length > 1) form.value.prizes.splice(index, 1) }
async function loadSkus(prize: LotteryPrize, reset = true) {
  if (!prize.productId) { prize.skuId = null; prize.skuOptions = []; return }
  const detail: ProductDetailVO = await productApi.get(Number(prize.productId))
  prize.skuOptions = (detail.skus || []).map((sku) => ({ id: sku.id, specText: sku.specText, stock: sku.stock }))
  if (reset) prize.skuId = null
}
function prizeTypeLabel(type: string) { return ({ POINTS: '积分', COUPON: '优惠券', PHYSICAL: '实物', CONSOLATION: '谢谢参与' } as Record<string, string>)[type] || type }
function statusLabel(status: number, active?: boolean) { if (active) return '生效中'; return status === 0 ? '草稿' : status === 1 ? '已发布' : status === 2 ? '已暂停' : '已结束' }
function rewardTypeLabel(type: string) { return prizeTypeLabel(type) }
function rewardStatusType(status: number) { return status >= 4 ? 'success' : status === 6 ? 'danger' : status === 2 || status === 3 ? 'warning' : 'info' }
function couponLabel(item: CouponTemplate) { return `${item.name} · ¥${item.amount}${Number(item.thresholdAmount) ? `（满${item.thresholdAmount}可用）` : ''}` }

function validate() {
  if (!form.value.name.trim()) return '请填写活动名称'
  if (!form.value.startAt || !form.value.endAt || form.value.startAt >= form.value.endAt) return '请填写正确的活动时间'
  if (form.value.dailyChances < 1) return '每日抽奖次数至少为 1'
  if (form.value.condition.firstOrderOnly && form.value.condition.repurchaseOnly) return '首单和复购条件不能同时开启'
  if (!form.value.prizes.length) return '至少配置一个奖品'
  const sum = form.value.prizes.reduce((total, prize) => total + Number(prize.probability || 0), 0)
  if (Math.abs(sum - 1) > 0.0001) return `奖品概率合计应为 100%，当前为 ${(sum * 100).toFixed(2)}%`
    for (const prize of form.value.prizes) {
    if (!prize.name.trim() || Number(prize.probability) <= 0) return '请完整填写奖品名称和概率'
    if (prize.prizeType === 'POINTS' && Number(prize.pointsAmount) <= 0) return '积分奖品请填写大于 0 的积分数'
    if (prize.prizeType === 'COUPON' && !prize.couponTemplateId) return '优惠券奖品请选择券模板'
    if (prize.prizeType === 'PHYSICAL' && (!prize.productId || !prize.skuId)) return '实物奖品请选择商品和 SKU'
  }
  return ''
}

async function save() {
  const message = validate()
  if (message) return ElMessage.warning(message)
  saving.value = true
  try {
    const payload = {
      name: form.value.name,
      themeImage: form.value.themeImage,
      entryImage: form.value.entryImage,
      ruleText: form.value.ruleText,
      condition: form.value.condition,
      dailyChances: form.value.dailyChances,
      startAt: form.value.startAt,
      endAt: form.value.endAt,
      status: form.value.status,
      prizes: form.value.prizes.map(({ skuOptions, remainingStock, ...item }) => item),
    }
    if (editingId.value) await lotteryApi.update(editingId.value, payload)
    else await lotteryApi.create(payload)
    ElMessage.success(editingId.value ? '抽奖活动已更新' : '抽奖活动已保存')
    dialog.value = false
    await load()
  } finally { saving.value = false }
}

async function changeStatus(row: any, status: number) {
  if (!row.id) return
  if (status === 2) await ElMessageBox.confirm('暂停后小程序将立即隐藏活动入口，已产生的奖品不受影响。', '确认暂停', { type: 'warning' })
  await lotteryApi.status(row.id, status)
  ElMessage.success(status === 1 ? '活动已发布' : '活动已暂停')
  await load()
}

async function updateRewardStatus(row: any, status: number) {
  if (!selectedId.value) return
  await lotteryApi.rewardStatus(selectedId.value, row.id, status)
  ElMessage.success('奖品状态已更新')
  await selectActivity(selectedId.value)
}

function onCurrentChange(row: any | null) {
  if (row?.id) selectActivity(Number(row.id))
}

onMounted(load)
</script>

<template>
  <div class="lottery-page page">
    <div class="page-header"><div><span class="page-kicker">DAILY SURPRISE</span><h1 class="page-title">互动抽奖盲盒</h1><p class="page-desc">配置活动时间、参与门槛和透明概率；系统保证同一商家同一时间仅一个抽奖活动在线。</p></div><div class="page-actions"><el-button @click="load">刷新</el-button><el-button v-permission="'merchant:lottery:create'" type="primary" @click="openCreate">新建活动</el-button></div></div>
    <el-alert title="发布边界" description="活动时间不能重叠，发布时会校验奖品概率合计为 100%。暂停活动后，前台入口和抽奖接口都会关闭。" type="warning" show-icon :closable="false" class="tip" />
    <el-card shadow="never" class="block"><el-table v-loading="loading" :data="activities" highlight-current-row @current-change="onCurrentChange"><el-table-column prop="name" label="活动名称" min-width="190"/><el-table-column label="活动时间" min-width="280"><template #default="{row}">{{ row.startAt }} 至 {{ row.endAt }}</template></el-table-column><el-table-column label="奖品数" width="90"><template #default="{row}">{{ row.prizes?.length || 0 }}</template></el-table-column><el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="row.active ? 'success' : row.status === 2 ? 'warning' : 'info'">{{ statusLabel(row.status, row.active) }}</el-tag></template></el-table-column><el-table-column label="操作" width="270" fixed="right"><template #default="{row}"><el-button v-permission="'merchant:lottery:update'" link type="primary" @click="openEdit(row)">编辑</el-button><el-button v-if="row.status !== 1" v-permission="'merchant:lottery:status'" link type="success" @click="changeStatus(row, 1)">发布</el-button><el-button v-if="row.status === 1" v-permission="'merchant:lottery:status'" link type="warning" @click="changeStatus(row, 2)">暂停</el-button><el-button v-if="row.status === 2" v-permission="'merchant:lottery:status'" link type="success" @click="changeStatus(row, 1)">恢复</el-button></template></el-table-column></el-table></el-card>
    <el-row v-if="selected" :gutter="16" class="summary-row"><el-col v-for="item in [{label:'参与人数',value:stats?.participants||0},{label:'抽奖次数',value:stats?.drawCount||0},{label:'已发奖品',value:stats?.rewardsIssued||0},{label:'待处理奖品',value:stats?.rewardsPending||0}]" :key="item.label" :xs="12" :sm="6"><el-card shadow="never" class="metric"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></el-card></el-col></el-row>
    <el-card v-if="selected" shadow="never" class="block detail-block"><template #header><div class="card-title"><span>奖品履约 · {{ selected.name }}</span><span class="detail-hint">积分 {{ stats?.pointsRewards || 0 }} · 优惠券 {{ stats?.couponRewards || 0 }} · 实物 {{ stats?.physicalRewards || 0 }}</span></div></template><el-table :data="rewards" empty-text="暂无中奖记录"><el-table-column prop="id" label="奖品 ID" width="90"/><el-table-column prop="userId" label="用户 ID" width="100"/><el-table-column label="奖品" min-width="180"><template #default="{row}"><el-tag size="small">{{ rewardTypeLabel(row.prizeType) }}</el-tag> {{ row.prizeName }}</template></el-table-column><el-table-column prop="createdAt" label="中奖时间" min-width="170"/><el-table-column label="状态" width="120"><template #default="{row}"><el-tag :type="rewardStatusType(row.status)">{{ row.statusText }}</el-tag></template></el-table-column><el-table-column label="处理" width="180"><template #default="{row}"><el-button v-if="row.status === 2" v-permission="'merchant:lottery:reward'" link type="primary" @click="updateRewardStatus(row, 3)">确认待发货</el-button><el-button v-if="row.status === 3" v-permission="'merchant:lottery:reward'" link type="success" @click="updateRewardStatus(row, 4)">标记已发货</el-button><el-button v-if="row.status === 4" v-permission="'merchant:lottery:reward'" link type="success" @click="updateRewardStatus(row, 5)">标记已完成</el-button></template></el-table-column></el-table></el-card>
    <el-dialog v-model="dialog" :title="editingId ? '编辑抽奖活动' : '新建抽奖活动'" width="1040px" destroy-on-close><el-form label-width="130px" class="lottery-form"><el-form-item label="活动名称"><el-input v-model="form.name" maxlength="80" placeholder="例如：520 互动抽奖盲盒"/></el-form-item><el-form-item label="活动时间"><el-date-picker v-model="form.startAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="开始时间"/><span class="to">至</span><el-date-picker v-model="form.endAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="结束时间"/></el-form-item><el-form-item label="每日次数"><el-input-number v-model="form.dailyChances" :min="1" :max="99"/></el-form-item><el-form-item label="参与门槛"><div class="conditions"><span>会员 ≥</span><el-input-number v-model="form.condition.minMemberLevel" :min="1"/><span>级，积分 ≥</span><el-input-number v-model="form.condition.minPoints" :min="0"/><el-checkbox v-model="form.condition.firstOrderOnly">仅首单</el-checkbox><el-checkbox v-model="form.condition.repurchaseOnly">仅复购</el-checkbox></div></el-form-item><el-form-item label="主题图 URL"><el-input v-model="form.themeImage" placeholder="可选，留空使用默认盲盒图"/></el-form-item><el-form-item label="规则说明"><el-input v-model="form.ruleText" type="textarea" :rows="3"/></el-form-item><el-form-item label="奖品池"><div class="prizes"><div v-for="(prize,index) in form.prizes" :key="index" class="prize-row"><el-input v-model="prize.name" placeholder="奖品名称" class="prize-name"/><el-select v-model="prize.prizeType" class="prize-type"><el-option value="POINTS" label="积分"/><el-option value="COUPON" label="优惠券"/><el-option value="PHYSICAL" label="实物"/><el-option value="CONSOLATION" label="谢谢参与"/></el-select><el-input-number v-if="prize.prizeType === 'POINTS' || prize.prizeType === 'CONSOLATION'" v-model="prize.pointsAmount" :min="1" placeholder="积分（可选）"/><el-select v-if="prize.prizeType === 'COUPON'" v-model="prize.couponTemplateId" filterable clearable placeholder="选择券模板" class="prize-ref"><el-option v-for="coupon in coupons" :key="coupon.id" :value="coupon.id" :label="couponLabel(coupon)"/></el-select><el-select v-if="prize.prizeType === 'PHYSICAL'" v-model="prize.productId" filterable clearable placeholder="选择商品" class="prize-ref" @change="loadSkus(prize)"><el-option v-for="product in goods" :key="product.id" :value="product.id" :label="`${product.name} · ${product.id}`"/></el-select><el-select v-if="prize.prizeType === 'PHYSICAL'" v-model="prize.skuId" filterable clearable placeholder="选择 SKU" class="prize-ref"><el-option v-for="sku in prize.skuOptions || []" :key="sku.id" :value="sku.id" :label="`${sku.specText || '默认规格'} · 库存 ${sku.stock}`"/></el-select><el-input-number v-model="prize.totalStock" :min="0" placeholder="库存 0不限"/><el-input-number v-model="prize.probability" :min="0.0001" :max="1" :precision="4" :step="0.01" placeholder="概率"/><el-button link type="danger" @click="removePrize(index)">删除</el-button></div><el-button link type="primary" @click="addPrize">+ 添加奖品</el-button><div class="prize-tip">概率按小数填写，例如 0.45；全部奖品合计必须为 1。</div></div></el-form-item></el-form><template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存活动</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.tip,.block{margin-bottom:18px}.summary-row{margin-bottom:2px}.metric{margin-bottom:16px;border-radius:16px}.metric span{display:block;color:var(--shop-muted);font-size:12px}.metric strong{display:block;margin-top:8px;color:#f15e17;font-size:28px}.card-title{display:flex;justify-content:space-between;align-items:center}.detail-hint{color:var(--shop-muted);font-size:12px}.lottery-form :deep(.el-input),.lottery-form :deep(.el-textarea),.lottery-form :deep(.el-select){max-width:460px}.to{margin:0 10px;color:var(--shop-muted)}.conditions{display:flex;align-items:center;gap:10px;flex-wrap:wrap}.prizes{width:100%}.prize-row{display:flex;align-items:center;gap:8px;margin-bottom:12px;flex-wrap:wrap}.prize-name{width:150px!important}.prize-type{width:120px!important}.prize-ref{width:190px!important}.prize-row :deep(.el-input-number){width:125px}.prize-tip{margin-top:8px;color:var(--shop-muted);font-size:12px}
</style>
