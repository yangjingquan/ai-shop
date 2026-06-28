<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const tab = ref<'admin' | 'merchant'>('admin')
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    if (tab.value === 'admin') {
      await userStore.loginAdmin(form.username, form.password)
    } else {
      await userStore.loginMerchant(form.username, form.password)
    }
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || (tab.value === 'admin' ? '/admin' : '/merchant')
    router.replace(redirect)
  } catch (err) {
    // 错误已经被 request 拦截器提示
    console.warn(err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="orb orb-1" />
    <div class="orb orb-2" />
    <div class="login-shell">
      <section class="brand-panel" aria-label="商城后台介绍">
        <p class="eyebrow">SHOP COMMAND CENTER</p>
        <h1>把每一次下单，都变成可增长的经营信号。</h1>
        <p class="brand-copy">
          商品、商家、履约与售后统一管理，实时掌控商城运营节奏。
        </p>
        <div class="metric-grid">
          <div>
            <strong>24h</strong>
            <span>订单看板</span>
          </div>
          <div>
            <strong>98%</strong>
            <span>履约追踪</span>
          </div>
          <div>
            <strong>3min</strong>
            <span>商品上新</span>
          </div>
        </div>
        <div class="product-card product-card-primary">
          <span class="tag">热卖</span>
          <strong>Premium Tote</strong>
          <small>库存充足 · 转化提升 18%</small>
        </div>
        <div class="product-card product-card-secondary">
          <span class="tag">NEW</span>
          <strong>Daily Aroma</strong>
          <small>待审核 · 商家 merchant01</small>
        </div>
      </section>

      <el-card class="login-card" shadow="never">
        <div class="card-head">
          <span class="badge">SECURE LOGIN</span>
          <h2 class="title">商城后台</h2>
          <p>选择入口，登录你的运营工作台</p>
        </div>
        <el-tabs v-model="tab" class="tabs">
          <el-tab-pane label="运营后台" name="admin" />
          <el-tab-pane label="商家后台" name="merchant" />
        </el-tabs>
        <el-form class="login-form" @submit.prevent="handleLogin">
          <label class="field-label">账号</label>
          <el-form-item>
            <el-input v-model="form.username" placeholder="请输入账号" autocomplete="username" />
          </el-form-item>
          <label class="field-label">密码</label>
          <el-form-item>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="submit"
            @click="handleLogin"
          >
            进入控制台
          </el-button>
        </el-form>
        <p class="hint">
          默认账号：admin / admin123 ；merchant01 / merchant123
        </p>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
:global(#app) {
  width: 100%;
  border-inline: 0;
  text-align: initial;
}

.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 48px 24px;
  box-sizing: border-box;
  color: #261305;
  background:
    radial-gradient(circle at 18% 16%, rgba(255, 213, 105, 0.55), transparent 30%),
    radial-gradient(circle at 82% 22%, rgba(255, 105, 64, 0.28), transparent 28%),
    linear-gradient(135deg, #fff7df 0%, #ffe7c7 42%, #f7c470 100%);
}

.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.32;
  background-image:
    linear-gradient(rgba(91, 49, 12, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(91, 49, 12, 0.08) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(90deg, transparent, #000 18%, #000 82%, transparent);
}

.orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(1px);
  opacity: 0.78;
  animation: float 8s ease-in-out infinite;
}

.orb-1 {
  width: 220px;
  height: 220px;
  left: 6%;
  bottom: 10%;
  background: linear-gradient(135deg, #ff7a1a, #ffd45f);
}

.orb-2 {
  width: 140px;
  height: 140px;
  right: 9%;
  top: 12%;
  background: linear-gradient(135deg, #111827, #7c2d12);
  animation-delay: -3s;
}

.login-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) 430px;
  width: min(1120px, 100%);
  min-height: 640px;
  border: 1px solid rgba(83, 44, 10, 0.14);
  border-radius: 36px;
  overflow: hidden;
  background: rgba(255, 252, 242, 0.64);
  box-shadow: 0 32px 90px rgba(117, 68, 13, 0.28);
  backdrop-filter: blur(22px);
}

.brand-panel {
  position: relative;
  padding: 64px;
  overflow: hidden;
  background:
    linear-gradient(130deg, rgba(38, 19, 5, 0.9), rgba(98, 50, 8, 0.74)),
    linear-gradient(45deg, #f97316, #fde68a);
  color: #fff8e8;
}

.brand-panel::before {
  content: '';
  position: absolute;
  right: -90px;
  bottom: -160px;
  width: 440px;
  height: 440px;
  border-radius: 88px;
  transform: rotate(22deg);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(255, 196, 87, 0.05));
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.eyebrow,
.badge {
  letter-spacing: 0.18em;
  font-size: 12px;
  font-weight: 800;
}

.eyebrow {
  margin-bottom: 22px;
  color: #fed7aa;
}

.brand-panel h1 {
  position: relative;
  max-width: 620px;
  margin: 0;
  color: #fffaf0;
  font-size: clamp(42px, 5vw, 72px);
  line-height: 0.96;
  letter-spacing: -0.06em;
  font-weight: 900;
}

.brand-copy {
  position: relative;
  max-width: 460px;
  margin-top: 28px;
  color: rgba(255, 250, 240, 0.78);
  font-size: 17px;
}

.metric-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  max-width: 520px;
  margin-top: 42px;
}

.metric-grid div {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.11);
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.metric-grid strong,
.metric-grid span {
  display: block;
}

.metric-grid strong {
  font-size: 28px;
  line-height: 1;
  color: #fde68a;
}

.metric-grid span {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(255, 250, 240, 0.68);
}

.product-card {
  position: absolute;
  min-width: 190px;
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 250, 240, 0.92);
  color: #2b1608;
  box-shadow: 0 20px 48px rgba(0, 0, 0, 0.22);
}

.product-card-primary {
  right: 48px;
  bottom: 74px;
  transform: rotate(-4deg);
}

.product-card-secondary {
  right: 220px;
  bottom: 34px;
  transform: rotate(6deg);
  opacity: 0.86;
}

.tag {
  display: inline-flex;
  margin-bottom: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #ffedd5;
  color: #c2410c;
  font-size: 11px;
  font-weight: 800;
}

.product-card strong,
.product-card small {
  display: block;
}

.product-card small {
  margin-top: 6px;
  color: #8a5a2b;
}

.login-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  border: 0;
  border-radius: 0;
  background: rgba(255, 252, 242, 0.92);
}

.login-card :deep(.el-card__body) {
  padding: 54px 46px;
}

.card-head {
  margin-bottom: 26px;
}

.badge {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 999px;
  color: #9a3412;
  background: #ffedd5;
}

.title {
  margin: 18px 0 8px;
  color: #1f1308;
  font-size: 34px;
  letter-spacing: -0.04em;
  font-weight: 900;
}

.card-head p {
  color: #8a5a2b;
}

.tabs {
  margin-bottom: 20px;
}

.tabs :deep(.el-tabs__nav) {
  width: 100%;
  padding: 5px;
  border-radius: 18px;
  background: #fff3dd;
}

.tabs :deep(.el-tabs__item) {
  flex: 1;
  height: 44px;
  border-radius: 14px;
  color: #9a6a3a;
  font-weight: 800;
}

.tabs :deep(.el-tabs__item.is-active) {
  color: #1f1308;
  background: #fffaf0;
  box-shadow: 0 10px 24px rgba(146, 64, 14, 0.12);
}

.tabs :deep(.el-tabs__active-bar),
.tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.field-label {
  display: block;
  margin: 0 0 8px 2px;
  color: #6b3f19;
  font-size: 13px;
  font-weight: 800;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 50px;
  border-radius: 16px;
  background: #fffaf0;
  box-shadow: inset 0 0 0 1px rgba(154, 52, 18, 0.14);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    inset 0 0 0 1px #f97316,
    0 0 0 4px rgba(249, 115, 22, 0.12);
}

.submit {
  width: 100%;
  height: 52px;
  margin-top: 4px;
  border: 0;
  border-radius: 18px;
  font-size: 16px;
  font-weight: 900;
  letter-spacing: 0.08em;
  background: linear-gradient(135deg, #1f1308, #9a3412 48%, #f97316);
  box-shadow: 0 18px 34px rgba(194, 65, 12, 0.28);
}

.submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 22px 40px rgba(194, 65, 12, 0.34);
}

.hint {
  margin-top: 18px;
  padding: 12px 14px;
  border-radius: 16px;
  color: #9a6a3a;
  background: rgba(255, 237, 213, 0.72);
  font-size: 12px;
  text-align: center;
}

@keyframes float {
  0%, 100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(0, -18px, 0) scale(1.04);
  }
}

@media (max-width: 900px) {
  .login-page {
    padding: 20px;
  }

  .login-shell {
    grid-template-columns: 1fr;
    min-height: auto;
    border-radius: 28px;
  }

  .brand-panel {
    padding: 38px 30px 120px;
  }

  .brand-panel h1 {
    font-size: 42px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .product-card-primary {
    right: 24px;
    bottom: 28px;
  }

  .product-card-secondary {
    display: none;
  }

  .login-card :deep(.el-card__body) {
    padding: 34px 26px;
  }
}
</style>
