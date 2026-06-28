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

interface MenuItem {
  index: string
  label: string
  path: string
  desc: string
  icon: string
}

const adminMenus: MenuItem[] = [
  { index: 'admin', label: '首页', path: '/admin', desc: '经营概览', icon: '店' },
  { index: 'admin-merchants', label: '商家管理', path: '/admin/merchants', desc: '入驻与状态', icon: '商' },
  { index: 'admin-categories', label: '平台分类', path: '/admin/categories', desc: '类目层级', icon: '类' },
]

const merchantMenus: MenuItem[] = [
  { index: 'merchant', label: '首页', path: '/merchant', desc: '店铺概览', icon: '店' },
  { index: 'merchant-profile', label: '店铺信息', path: '/merchant/profile', desc: '资料维护', icon: '铺' },
  { index: 'merchant-products', label: '商品管理', path: '/merchant/products', desc: '上新与库存', icon: '货' },
  { index: 'merchant-banners', label: 'Banner 配置', path: '/merchant/banners', desc: '首页轮播', icon: '图' },
  { index: 'merchant-order-ship', label: '订单发货', path: '/merchant/order-ship', desc: '履约处理', icon: '单' },
  { index: 'merchant-refund-review', label: '退款审批', path: '/merchant/refund-review', desc: '售后审核', icon: '退' },
]

const menus = computed<MenuItem[]>(() =>
  userStore.role === 'admin' ? adminMenus : merchantMenus,
)

const activeIndex = computed(() => {
  const matched = [...menus.value]
    .sort((a, b) => b.path.length - a.path.length)
    .find((m) => route.path === m.path || route.path.startsWith(`${m.path}/`))
  return matched?.index ?? menus.value[0]?.index ?? ''
})

function handleSelect(index: string) {
  const target = menus.value.find((m) => m.index === index)
  if (target && target.path !== route.path) router.push(target.path)
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
        class="side-menu"
        @select="handleSelect"
      >
        <el-menu-item v-for="m in menus" :key="m.index" :index="m.index">
          <span class="menu-icon">{{ m.icon }}</span>
          <span class="menu-copy">
            <span class="menu-label">{{ m.label }}</span>
            <span class="menu-desc">{{ m.desc }}</span>
          </span>
        </el-menu-item>
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
          <span class="role-tag">{{ roleLabel }}</span>
          <el-button class="logout-btn" plain @click="handleLogout">退出登录</el-button>
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

.side-menu :deep(.el-menu-item:hover) {
  color: #fff7ea;
  background: rgba(255, 255, 255, 0.08);
}

.side-menu :deep(.el-menu-item.is-active) {
  color: #fff7ea;
  background: linear-gradient(135deg, rgba(216, 111, 34, 0.96), rgba(159, 63, 18, 0.92));
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.18);
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
  gap: 12px;
}

.role-tag {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #f0c89a;
  border-radius: 999px;
  color: var(--shop-primary-dark);
  background: var(--shop-primary-soft);
  font-size: 12px;
  font-weight: 800;
}

.logout-btn {
  border-color: rgba(255, 255, 255, 0.92) !important;
  color: var(--shop-text) !important;
  background: #fff !important;
}

.logout-btn:hover,
.logout-btn:focus {
  border-color: #ef4444 !important;
  color: #ef4444 !important;
  background: #fff5f5 !important;
}

.main {
  min-height: calc(100vh - 74px);
  padding: 0;
  overflow: auto;
}
</style>
