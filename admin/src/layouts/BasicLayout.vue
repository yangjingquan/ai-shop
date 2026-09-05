<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { profileApi } from '@/api/profile'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const title = computed(() =>
  userStore.role === 'admin' ? '商城运营后台' : '商家管理后台',
)

const subtitle = computed(() =>
  userStore.role === 'admin' ? '平台经营与商家服务中心' : '店铺商品与订单履约中心',
)

const roleLabel = computed(() =>
  userStore.role === 'admin' ? '运营管理员' : userStore.merchantName || '商家账号',
)

const currentUserLabel = computed(() => userStore.username || roleLabel.value)

interface MenuItem {
  index: string
  label: string
  path?: string
  desc: string
  icon: string
  permission?: string
  children?: MenuItem[]
}

const adminMenus: MenuItem[] = [
  { index: 'admin', label: '首页', path: '/admin', desc: '经营概览', icon: '店' },
  { index: 'admin-merchants', label: '商家管理', path: '/admin/merchants', desc: '入驻与状态', icon: '商' },
  { index: 'admin-categories', label: '平台分类', path: '/admin/categories', desc: '类目层级', icon: '类' },
  { index: 'admin-orders', label: '平台订单', path: '/admin/orders', desc: '订单监控', icon: '单' },
  { index: 'admin-payments', label: '支付管理', path: '/admin/payments', desc: '流水与对账', icon: '支' },
  { index: 'admin-refunds', label: '平台退款', path: '/admin/refunds', desc: '售后跟踪', icon: '退' },
  { index: 'admin-banners', label: '平台 Banner', path: '/admin/banners', desc: '内容运营', icon: '图' },
  { index: 'admin-product-audit', label: '商品审核', path: '/admin/product-audit', desc: '商品监管', icon: '审' },
  { index: 'admin-op-logs', label: '操作日志', path: '/admin/op-logs', desc: '审计追踪', icon: '志' },
  { index: 'admin-wechat-settings', label: '微信设置', path: '/admin/wechat-settings', desc: '商户微信配置', icon: '微' },
]

const merchantMenus: MenuItem[] = [
  { index: 'merchant', label: '首页', path: '/merchant', desc: '店铺概览', icon: '店', permission: 'merchant:dashboard:view' },
  { index: 'merchant-profile', label: '店铺信息', path: '/merchant/profile', desc: '资料维护', icon: '铺', permission: 'merchant:profile:view' },
  { index: 'merchant-categories', label: '分类管理', path: '/merchant/categories', desc: '店铺类目', icon: '类', permission: 'merchant:category:view' },
  {
    index: 'merchant-product-management',
    label: '商品管理',
    desc: '商品与库存',
    icon: '货',
    children: [
      { index: 'merchant-products', label: '商品设置', path: '/merchant/products', desc: '上新与库存', icon: '货', permission: 'merchant:product:view' },
      { index: 'merchant-inventory', label: '库存设置', path: '/merchant/inventory', desc: '调整与流水', icon: '库', permission: 'merchant:inventory:view' },
    ],
  },
  { index: 'merchant-banners', label: 'Banner 配置', path: '/merchant/banners', desc: '首页轮播', icon: '图', permission: 'merchant:banner:view' },
  { index: 'merchant-marketing', label: '营销活动', path: '/merchant/marketing', desc: '活动开关', icon: '营', permission: 'merchant:marketing:view' },
  {
    index: 'merchant-activity-configuration',
    label: '活动配置',
    desc: '优惠与促销',
    icon: '营',
    children: [
      { index: 'merchant-coupon-templates', label: '通用优惠券', path: '/merchant/coupon-templates', desc: '新人、积分与奖励用券', icon: '券', permission: 'merchant:coupon:view' },
      { index: 'merchant-repurchase-coupon-templates', label: '复购优惠券', path: '/merchant/repurchase-coupon-templates', desc: '支付后自动发券', icon: '券', permission: 'merchant:coupon:view' },
      { index: 'merchant-seckill', label: '秒杀活动', path: '/merchant/seckill', desc: '场次与库存', icon: '秒', permission: 'merchant:seckill:view' },
      { index: 'merchant-referral', label: '邀请有礼', path: '/merchant/referral', desc: '老带新返券', icon: '礼', permission: 'merchant:referral:view' },
      { index: 'merchant-points', label: '积分会员日', path: '/merchant/points', desc: '积分、兑换与会员日', icon: '积', permission: 'merchant:points:view' },
    ],
  },
  { index: 'merchant-order-ship', label: '订单发货', path: '/merchant/order-ship', desc: '履约处理', icon: '单', permission: 'merchant:order:view' },
  { index: 'merchant-refund-review', label: '退款审批', path: '/merchant/refund-review', desc: '售后审核', icon: '退', permission: 'merchant:refund:view' },
  { index: 'merchant-access-control', label: '账号与权限', path: '/merchant/access-control', desc: '角色与成员', icon: '权', permission: 'merchant:rbac:manage' },
]

function hasPermission(item: MenuItem) {
  return !item.permission || userStore.hasPermission(item.permission)
}

const menus = computed<MenuItem[]>(() => {
  const source = userStore.role === 'admin' ? adminMenus : merchantMenus
  return source.flatMap((item) => {
    if (!item.children) return hasPermission(item) ? [item] : []
    const children = item.children.filter(hasPermission)
    return children.length > 0 ? [{ ...item, children }] : []
  })
})

const leafMenus = computed<MenuItem[]>(() =>
  menus.value.flatMap((item) => item.children ?? [item]),
)

const activeIndex = computed(() => {
  if (route.path === '/admin/profile' || route.path === '/merchant/password') return ''
  const matched = [...leafMenus.value]
    .sort((a, b) => (b.path?.length ?? 0) - (a.path?.length ?? 0))
    .find((m) => m.path && (route.path === m.path || route.path.startsWith(`${m.path}/`)))
  return matched?.index ?? leafMenus.value[0]?.index ?? ''
})

const openMenuIndexes = computed(() =>
  menus.value
    .filter((item) => item.children?.some((child) => child.index === activeIndex.value))
    .map((item) => item.index),
)

function handleSelect(index: string) {
  const target = leafMenus.value.find((m) => m.index === index)
  if (target?.path && target.path !== route.path) router.push(target.path)
}

async function loadMerchantName() {
  if (userStore.role !== 'merchant') return
  try {
    const profile = await profileApi.get()
    userStore.setMerchantName(profile.name || '')
  } catch {
    // 登录失效由请求拦截器统一处理，这里只避免布局渲染被打断。
  }
}

function handleLogout() {
  userStore.logout()
  router.replace('/login')
}

function handleUserCommand(command: 'profile' | 'logout') {
  if (command === 'profile') {
    router.push(userStore.role === 'admin' ? '/admin/profile' : '/merchant/password')
    return
  }
  handleLogout()
}
onMounted(loadMerchantName)
</script>

<template>
  <el-container class="basic-layout">
    <el-aside class="aside" width="236px">
      <div class="brand">
        <div class="brand-mark">S</div>
        <div>
          <div class="brand-title">Shop Suite</div>
          <div class="brand-subtitle">{{ roleLabel }}</div>
        </div>
      </div>

      <el-menu
        :default-active="activeIndex"
        :default-openeds="openMenuIndexes"
        class="side-menu"
        @select="handleSelect"
      >
        <template v-for="m in menus" :key="m.index">
          <el-sub-menu v-if="m.children" :index="m.index">
            <template #title>
              <span class="menu-icon">{{ m.icon }}</span>
              <span class="menu-copy">
                <span class="menu-label">{{ m.label }}</span>
                <span class="menu-desc">{{ m.desc }}</span>
              </span>
            </template>
            <el-menu-item v-for="child in m.children" :key="child.index" :index="child.index">
              <span class="menu-icon">{{ child.icon }}</span>
              <span class="menu-copy">
                <span class="menu-label">{{ child.label }}</span>
                <span class="menu-desc">{{ child.desc }}</span>
              </span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="m.index">
            <span class="menu-icon">{{ m.icon }}</span>
            <span class="menu-copy">
              <span class="menu-label">{{ m.label }}</span>
              <span class="menu-desc">{{ m.desc }}</span>
            </span>
          </el-menu-item>
        </template>
      </el-menu>

      <div class="aside-card">
        <span>今日经营提醒</span>
        <strong>关注库存、发货与售后处理效率</strong>
      </div>
    </el-aside>

    <el-container class="layout-main">
      <el-header class="header">
        <div>
          <div class="title">{{ title }}</div>
          <div class="subtitle">{{ subtitle }}</div>
        </div>
        <div class="actions">
          <el-dropdown
            trigger="click"
            placement="bottom-end"
            @command="handleUserCommand"
          >
            <button class="user-trigger" type="button" :aria-label="`打开${currentUserLabel}菜单`">
              <span class="user-avatar">{{ currentUserLabel.slice(0, 1).toUpperCase() }}</span>
              <span class="user-name">{{ currentUserLabel }}</span>
              <span class="user-chevron" aria-hidden="true">⌄</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu class="user-menu">
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background:
    radial-gradient(circle at 88% 8%, rgba(216, 111, 34, 0.12), transparent 28%),
    var(--shop-bg);
}

.aside {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 18px 14px;
  border-right: 1px solid rgba(234, 223, 206, 0.9);
  background: linear-gradient(180deg, #2a1a0d 0%, #3a2110 58%, #24160b 100%);
  box-shadow: 18px 0 42px rgba(65, 38, 14, 0.1);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 10px 20px;
  color: #fff7ea;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 16px;
  color: #3a2110;
  font-size: 20px;
  font-weight: 900;
  background: linear-gradient(135deg, #ffd88a, #d86f22);
  box-shadow: 0 12px 28px rgba(216, 111, 34, 0.32);
}

.brand-title {
  font-size: 16px;
  font-weight: 900;
  letter-spacing: 0.02em;
}

.brand-subtitle {
  margin-top: 3px;
  color: rgba(255, 247, 234, 0.58);
  font-size: 12px;
}

.side-menu {
  border-right: 0;
  background: transparent;
}

.side-menu :deep(.el-menu-item) {
  height: 58px;
  margin: 6px 0;
  padding: 0 12px !important;
  border-radius: 16px;
  color: rgba(255, 247, 234, 0.68);
  line-height: 1;
}

.side-menu :deep(.el-sub-menu) {
  margin: 6px 0;
}

.side-menu :deep(.el-sub-menu__title) {
  height: 58px;
  padding: 0 12px !important;
  border-radius: 16px;
  color: rgba(255, 247, 234, 0.68);
  line-height: 1;
}

.side-menu :deep(.el-sub-menu__title .menu-copy) {
  flex: 1;
  min-width: 0;
}

.side-menu :deep(.el-menu-item:hover) {
  color: #fff7ea;
  background: rgba(255, 255, 255, 0.08);
}

.side-menu :deep(.el-sub-menu__title:hover),
.side-menu :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: #fff7ea;
  background: rgba(255, 255, 255, 0.08);
}

.side-menu :deep(.el-menu-item.is-active) {
  color: #fff7ea;
  background: linear-gradient(135deg, rgba(216, 111, 34, 0.96), rgba(159, 63, 18, 0.92));
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.18);
}

.side-menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
}

.side-menu :deep(.el-sub-menu .el-menu-item) {
  height: 52px;
  margin: 4px 0;
  padding-left: 28px !important;
}

.menu-icon {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 32px;
  height: 32px;
  margin-right: 10px;
  border-radius: 12px;
  color: #ffdca6;
  background: rgba(255, 255, 255, 0.1);
  font-size: 13px;
  font-weight: 900;
}

.side-menu :deep(.el-menu-item.is-active) .menu-icon {
  color: #6b2b0c;
  background: rgba(255, 255, 255, 0.82);
}

.menu-copy {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.menu-label {
  font-size: 14px;
  font-weight: 800;
}

.menu-desc {
  font-size: 12px;
  opacity: 0.62;
}

.aside-card {
  position: absolute;
  right: 14px;
  bottom: 18px;
  left: 14px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  color: #fff7ea;
  background: rgba(255, 255, 255, 0.08);
}

.aside-card span,
.aside-card strong {
  display: block;
}

.aside-card span {
  color: rgba(255, 247, 234, 0.58);
  font-size: 12px;
}

.aside-card strong {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.45;
}

.layout-main {
  min-width: 0;
}

.header {
  height: 74px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  border-bottom: 1px solid rgba(234, 223, 206, 0.86);
  background: rgba(255, 253, 248, 0.82);
  backdrop-filter: blur(18px);
}

.title {
  color: var(--shop-text);
  font-size: 20px;
  font-weight: 900;
  letter-spacing: -0.02em;
}

.subtitle {
  margin-top: 4px;
  color: var(--shop-text-muted);
  font-size: 13px;
}

.actions {
  display: flex;
  align-items: center;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  min-height: 42px;
  padding: 5px 8px 5px 6px;
  border: 1px solid transparent;
  border-radius: 12px;
  color: var(--shop-text);
  background: transparent;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.user-trigger:hover,
.user-trigger:focus-visible {
  outline: none;
  border-color: #f0c89a;
  background: var(--shop-primary-soft);
}

.user-avatar {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  color: #fff8ed;
  background: linear-gradient(135deg, #df7d32, #b64e18);
  font-size: 13px;
  font-weight: 900;
}

.user-name {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 800;
}

.user-chevron {
  margin-top: -4px;
  color: var(--shop-text-muted);
  font-size: 18px;
  line-height: 1;
}

.main {
  min-height: calc(100vh - 74px);
  padding: 0;
  overflow: auto;
}
</style>
