<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { marketingApi, type MarketingFeature } from '@/api/marketing'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const features = ref<MarketingFeature[]>([])

async function load() {
  loading.value = true
  try {
    features.value = (await marketingApi.features()) || []
  } finally {
    loading.value = false
  }
}

async function toggle(feature: MarketingFeature, nextValue: boolean) {
  if (!feature.implemented) return
  if (!nextValue) {
    try {
      await ElMessageBox.confirm(
        `停用“${feature.name}”后，小程序前台将隐藏该活动，相关接口也会拒绝访问。确定停用吗？`,
        '停用营销活动',
        { type: 'warning', confirmButtonText: '确认停用', cancelButtonText: '取消' },
      )
    } catch {
      return
    }
  }
  const previous = feature.enabled
  feature.enabled = nextValue ? 1 : 0
  try {
    await marketingApi.updateFeature(feature.code, feature.enabled)
    ElMessage.success(`${feature.name}已${nextValue ? '启用' : '停用'}`)
  } catch {
    feature.enabled = previous
  }
}

onMounted(load)
</script>

<template>
  <div class="marketing-activity page">
    <div class="page-header">
      <div>
        <span class="page-kicker">MARKETING CAPABILITIES</span>
        <h1 class="page-title">营销活动</h1>
        <p class="page-desc">按商家控制小程序可见的营销能力。未接入的活动先展示规划状态，不会误导用户进入空页面。</p>
      </div>
      <div class="page-actions"><el-button @click="load">刷新状态</el-button></div>
    </div>

    <el-alert
      title="开关是商家级能力控制"
      description="关闭后会同步影响首页入口、活动列表和活动写接口；缓存通常在更新后立即失效。"
      type="info"
      show-icon
      :closable="false"
      class="marketing-tip"
    />

    <el-row v-loading="loading" :gutter="16" class="feature-grid">
      <el-col v-for="feature in features" :key="feature.code" :xs="24" :sm="12" :lg="8">
        <el-card shadow="never" class="feature-card">
          <div class="feature-head">
            <div>
              <h3>{{ feature.name }}</h3>
              <span class="feature-code">{{ feature.code }}</span>
            </div>
            <el-tag :type="feature.implemented ? 'success' : 'info'" size="small">
              {{ feature.implemented ? '已接入' : '待接入' }}
            </el-tag>
          </div>
          <p class="feature-desc">{{ feature.description }}</p>
          <div class="feature-foot">
            <span>{{ feature.implemented ? (feature.enabled === 1 ? '前台已展示' : '前台已隐藏') : '当前版本暂不可配置' }}</span>
            <el-switch
              :model-value="feature.enabled === 1"
              :disabled="!feature.implemented || !userStore.hasPermission('merchant:marketing:feature:update')"
              :aria-label="`${feature.name}开关`"
              @change="(value: string | number | boolean) => toggle(feature, value === true || value === 1 || value === 'true')"
            />
          </div>
          <el-button
            v-if="feature.code === 'FULL_REDUCTION' && feature.enabled === 1"
            link type="primary" class="feature-link" @click="router.push('/merchant/full-reduction')"
          >去配置满减满折 →</el-button>
          <el-button
            v-if="feature.code === 'GROUP_BUY' && feature.enabled === 1"
            link
            type="primary"
            class="feature-link"
            @click="router.push('/merchant/products')"
          >去配置团购商品 →</el-button>
          <el-button
            v-if="feature.code === 'NEW_USER_COUPON' && feature.enabled === 1"
            link
            type="primary"
            class="feature-link"
            @click="router.push('/merchant/coupon-templates')"
          >去配置通用优惠券 →</el-button>
          <el-button
            v-if="feature.code === 'REPURCHASE_COUPON' && feature.enabled === 1"
            link
            type="primary"
            class="feature-link"
            @click="router.push('/merchant/repurchase-coupon-templates')"
          >去配置复购券 →</el-button>
          <el-button
            v-if="feature.code === 'SECKILL' && feature.enabled === 1"
            link
            type="primary"
            class="feature-link"
            @click="router.push('/merchant/seckill')"
          >去配置秒杀活动 →</el-button>
          <el-button
            v-if="feature.code === 'REFERRAL' && feature.enabled === 1"
            link
            type="primary"
            class="feature-link"
            @click="router.push('/merchant/referral')"
          >去配置邀请有礼 →</el-button>
          <el-button
            v-if="feature.code === 'POINTS_MEMBER_DAY' && feature.enabled === 1"
            link type="primary" class="feature-link" @click="router.push('/merchant/points')"
          >去配置积分会员日 →</el-button>
          <el-button
            v-if="feature.code === 'BUNDLE' && feature.enabled === 1"
            link type="primary" class="feature-link" @click="router.push('/merchant/bundles')"
          >去配置搭配购套餐 →</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.marketing-tip { margin-bottom: 18px; }
.feature-grid { min-height: 220px; }
.feature-card { margin-bottom: 16px; border-radius: 18px; }
.feature-head, .feature-foot { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.feature-head h3 { margin: 0 0 5px; color: var(--shop-ink); }
.feature-code { color: var(--shop-muted); font-size: 11px; letter-spacing: .05em; }
.feature-desc { min-height: 40px; color: var(--shop-muted); line-height: 1.6; }
.feature-foot { padding-top: 12px; border-top: 1px solid var(--shop-line); color: var(--shop-muted); font-size: 12px; }
.feature-link { padding-left: 0; margin-top: 10px; }
</style>
