<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { homeModuleApi, type HomeModule, type ProductSource } from '@/api/home'

const loading = ref(false)
const saving = ref(false)
const modules = ref<HomeModule[]>([])
const sourceOptions: Array<{ value: ProductSource; label: string }> = [
  { value: 'RECENT', label: '新品（按添加时间）' },
  { value: 'TOP_SALES', label: '销量最高' },
  { value: 'RECOMMEND', label: '推荐商品' },
]
const orderedModules = computed(() => [...modules.value].sort((a, b) => a.sortOrder - b.sortOrder))

async function load() {
  loading.value = true
  try {
    modules.value = (await homeModuleApi.list()) || []
  } finally {
    loading.value = false
  }
}

function move(index: number, direction: -1 | 1) {
  const items = orderedModules.value
  const next = index + direction
  if (next < 0 || next >= items.length) return
  const current = items[index]
  items[index] = items[next]
  items[next] = current
  modules.value = items.map((item, order) => ({ ...item, sortOrder: (order + 1) * 10 }))
}

async function save() {
  saving.value = true
  try {
    await homeModuleApi.update(orderedModules.value.map((item, index) => ({
      code: item.code,
      title: item.title,
      subtitle: item.subtitle,
      enabled: item.enabled,
      sortOrder: (index + 1) * 10,
      productSource: item.productSource,
      productLimit: item.productLimit,
    })))
    ElMessage.success('首页模块已保存')
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="home-modules page">
    <div class="page-header">
      <div>
        <span class="page-kicker">STOREFRONT COMPOSITION</span>
        <h1 class="page-title">首页装修</h1>
        <p class="page-desc">控制小程序首页模块的展示、排序和商品数据来源。没有商品或有效活动的模块会在前台自动隐藏。</p>
      </div>
      <div class="page-actions">
        <el-button @click="load">恢复配置</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存发布</el-button>
      </div>
    </div>

    <el-alert class="module-tip" type="info" :closable="false" show-icon title="营销会场会汇总已启用且进行中的预售、秒杀、团购、积分等活动，无须在这里重复维护活动内容。" />

    <div v-loading="loading" class="module-list">
      <el-card v-for="(module, index) in orderedModules" :key="module.code" shadow="never" class="module-card" :class="{ disabled: module.enabled !== 1 }">
        <div class="module-order">{{ String(index + 1).padStart(2, '0') }}</div>
        <div class="module-main">
          <div class="module-heading">
            <div>
              <h3>{{ module.name }}</h3>
              <p>{{ module.description }}</p>
            </div>
            <el-switch v-model="module.enabled" :active-value="1" :inactive-value="0" :aria-label="`${module.name}展示开关`" />
          </div>
          <div class="module-fields">
            <el-input v-model="module.title" maxlength="64" show-word-limit placeholder="模块标题" />
            <el-input v-model="module.subtitle" maxlength="128" show-word-limit placeholder="副标题（可选）" />
            <template v-if="module.productSource">
              <el-select v-model="module.productSource" aria-label="商品来源">
                <el-option v-for="option in sourceOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
              <el-input-number v-model="module.productLimit" :min="1" :max="20" controls-position="right" aria-label="商品数量" />
            </template>
          </div>
        </div>
        <div class="module-actions">
          <el-button text :disabled="index === 0" @click="move(index, -1)">上移</el-button>
          <el-button text :disabled="index === orderedModules.length - 1" @click="move(index, 1)">下移</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.module-tip { margin-bottom: 18px; }
.module-list { display: flex; flex-direction: column; gap: 12px; min-height: 260px; }
.module-card :deep(.el-card__body) { display: grid; grid-template-columns: 44px minmax(0, 1fr) 72px; gap: 18px; align-items: stretch; padding: 20px; }
.module-card.disabled { opacity: .58; background: #fafafa; }
.module-order { color: var(--shop-orange); font-size: 20px; font-weight: 900; letter-spacing: .04em; }
.module-heading { display: flex; align-items: start; justify-content: space-between; gap: 16px; }
.module-heading h3 { margin: 0; color: var(--shop-ink); font-size: 16px; }
.module-heading p { margin: 6px 0 0; color: var(--shop-muted); font-size: 12px; }
.module-fields { display: grid; grid-template-columns: minmax(140px, 1fr) minmax(180px, 1.2fr) 180px 120px; gap: 10px; margin-top: 14px; }
.module-actions { width: 72px; align-self: stretch; display: flex; flex-direction: column; justify-content: center; gap: 4px; }
.module-actions :deep(.el-button) { width: 72px; min-height: 32px; margin: 0; justify-content: center; }
@media (max-width: 900px) { .module-fields { grid-template-columns: 1fr 1fr; } }
</style>
