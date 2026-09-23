<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ImageUploader from '@/components/upload/ImageUploader.vue'
import { storefrontApi, type ProductSource, type StorefrontBlock, type StorefrontModule, type StorefrontModuleCode, type StorefrontPage, type StorefrontPageSummary, type StorefrontTemplate } from '@/api/storefront'

const activeTab = ref<'home' | 'topics' | 'templates'>('home')
const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const homePage = ref<StorefrontPage | null>(null)
const topics = ref<StorefrontPageSummary[]>([])
const templates = ref<StorefrontTemplate[]>([])
const selectedModuleId = ref('')
const selectedTopic = ref<StorefrontPage | null>(null)
const draggedModuleId = ref('')
const homeDraftBaseline = ref('')
const sourceOptions: Array<{ value: ProductSource; label: string }> = [
  { value: 'RECENT', label: '最新上架' },
  { value: 'TOP_SALES', label: '销量最高' },
  { value: 'RECOMMEND', label: '商户推荐' },
]
const library: Array<{ code: StorefrontModuleCode; name: string; description: string }> = [
  { code: 'BANNER', name: '焦点 Banner', description: '首页主视觉与运营活动' },
  { code: 'CATEGORY', name: '分类入口', description: '快速进入店铺分类' },
  { code: 'NEW_ARRIVALS', name: '新品速览', description: '突出近期上架商品' },
  { code: 'POPULAR_PRODUCTS', name: '人气推荐', description: '展示热销或推荐商品' },
  { code: 'PRODUCT_FEED', name: '商品精选流', description: '双列连续浏览商品' },
  { code: 'MARKETING_ZONE', name: '营销会场', description: '汇总店铺已启用活动' },
  { code: 'TOPIC_ENTRY', name: '专题入口', description: '将买家带到专题内容页' },
]
const orderedModules = computed(() => [...(homePage.value?.draft.modules || [])].sort((a, b) => a.sortOrder - b.sortOrder))
const selectedModule = computed(() => orderedModules.value.find((item) => item.id === selectedModuleId.value) || orderedModules.value[0] || null)
const canAdd = (code: StorefrontModuleCode) => orderedModules.value.length < 24 && (!['BANNER', 'CATEGORY', 'MARKETING_ZONE'].includes(code) || !orderedModules.value.some((item) => item.code === code))
const statusText = (status?: string) => status === 'PUBLISHED' ? '已发布' : status === 'OFFLINE' ? '已下线' : '草稿'
const isHomeDirty = computed(() => !!homePage.value && JSON.stringify(homePage.value.draft) !== homeDraftBaseline.value)

function copy<T>(value: T): T { return JSON.parse(JSON.stringify(value)) as T }
function id() { return `block-${Date.now()}-${Math.random().toString(36).slice(2, 8)}` }
function sortModules(items: StorefrontModule[]) { items.forEach((item, index) => { item.sortOrder = (index + 1) * 10 }) }
function addModule(code: StorefrontModuleCode) {
  if (!homePage.value || !canAdd(code)) return
  const definition = library.find((item) => item.code === code)!
  const module: StorefrontModule = { id: id(), code, title: definition.name, subtitle: '', enabled: 1, sortOrder: orderedModules.value.length * 10 + 10, topicPageIds: [] }
  if (['NEW_ARRIVALS', 'POPULAR_PRODUCTS', 'PRODUCT_FEED'].includes(code)) {
    module.productSource = code === 'NEW_ARRIVALS' ? 'RECENT' : code === 'POPULAR_PRODUCTS' ? 'TOP_SALES' : 'RECOMMEND'
    module.productLimit = code === 'NEW_ARRIVALS' ? 4 : 8
  }
  homePage.value.draft.modules.push(module)
  sortModules(homePage.value.draft.modules)
  selectedModuleId.value = module.id
}
function removeModule(module: StorefrontModule) {
  if (!homePage.value) return
  homePage.value.draft.modules = homePage.value.draft.modules.filter((item) => item.id !== module.id)
  sortModules(homePage.value.draft.modules)
  selectedModuleId.value = homePage.value.draft.modules[0]?.id || ''
}
function moveModule(index: number, direction: -1 | 1) {
  const items = orderedModules.value
  const next = index + direction
  if (next < 0 || next >= items.length || !homePage.value) return
  const reordered = [...items]
  ;[reordered[index], reordered[next]] = [reordered[next], reordered[index]]
  sortModules(reordered)
  homePage.value.draft.modules = reordered
}
function beginDrag(event: DragEvent, moduleId: string) {
  draggedModuleId.value = moduleId
  event.dataTransfer?.setData('text/plain', moduleId)
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
}
function dropOn(moduleId: string) {
  if (!homePage.value) return
  const fromId = draggedModuleId.value
  draggedModuleId.value = ''
  if (!fromId || fromId === moduleId) return
  const items = [...orderedModules.value]
  const from = items.findIndex((item) => item.id === fromId)
  const to = items.findIndex((item) => item.id === moduleId)
  if (from < 0 || to < 0) return
  const [item] = items.splice(from, 1)
  items.splice(to, 0, item)
  sortModules(items)
  homePage.value.draft.modules = items
}
async function loadHome() {
  loading.value = true
  try {
    homePage.value = await storefrontApi.home()
    sortModules(homePage.value.draft.modules)
    homeDraftBaseline.value = JSON.stringify(homePage.value.draft)
    selectedModuleId.value = homePage.value.draft.modules[0]?.id || ''
  } finally { loading.value = false }
}
async function loadTopics() { topics.value = await storefrontApi.topics() || [] }
async function loadTemplates() { templates.value = await storefrontApi.templates() || [] }
async function loadAll() {
  loading.value = true
  try { await Promise.all([loadHome(), loadTopics(), loadTemplates()]) } finally { loading.value = false }
}
async function saveHome() {
  if (!homePage.value) return
  saving.value = true
  try {
    sortModules(homePage.value.draft.modules)
    await storefrontApi.saveHome(homePage.value)
    ElMessage.success('首页草稿已保存')
    await loadHome()
  } finally { saving.value = false }
}
async function publishHome() {
  if (!homePage.value) return
  if (!orderedModules.value.some((module) => module.enabled === 1)) { ElMessage.warning('首页至少保留一个已启用模块'); return }
  try {
    await ElMessageBox.confirm('发布后，小程序首页将立即使用这份布局。', '发布首页', { type: 'warning', confirmButtonText: '发布上线' })
    publishing.value = true
    if (isHomeDirty.value) await storefrontApi.saveHome(homePage.value)
    await storefrontApi.publishHome()
    ElMessage.success('首页已发布')
    await loadHome()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error
  } finally { publishing.value = false }
}
async function applyTemplate(template: StorefrontTemplate) {
  if (!homePage.value) return
  try {
    await ElMessageBox.confirm(`“${template.name}”会替换当前编辑区的草稿模块；线上首页不会变化，直到你主动发布。`, '应用首页模板', { type: 'warning', confirmButtonText: '应用到草稿' })
    homePage.value.draft = copy(template.document)
    sortModules(homePage.value.draft.modules)
    selectedModuleId.value = homePage.value.draft.modules[0]?.id || ''
    activeTab.value = 'home'
  } catch { /* keep the current draft */ }
}
function newTopic() {
  selectedTopic.value = { pageType: 'TOPIC', title: '', slug: '', summary: '', coverImage: '', status: 'DRAFT', draft: { modules: [], blocks: [] }, draftChanged: true }
}
function addTopicBlock(type: StorefrontBlock['type']) {
  if (!selectedTopic.value) return
  if (selectedTopic.value.draft.blocks.length >= 40) { ElMessage.warning('每个专题最多添加 40 个内容模块'); return }
  const block: StorefrontBlock = { id: id(), type, title: type === 'PRODUCTS' ? '为你精选' : '', body: '', imageUrl: '', buttonText: '查看详情', linkType: 'PRODUCTS' }
  if (type === 'PRODUCTS') { block.productSource = 'RECOMMEND'; block.productLimit = 8 }
  selectedTopic.value.draft.blocks.push(block)
}
function moveBlock(index: number, direction: -1 | 1) {
  if (!selectedTopic.value) return
  const blocks = selectedTopic.value.draft.blocks
  const next = index + direction
  if (next < 0 || next >= blocks.length) return
  ;[blocks[index], blocks[next]] = [blocks[next], blocks[index]]
}
function removeBlock(block: StorefrontBlock) {
  if (!selectedTopic.value) return
  selectedTopic.value.draft.blocks = selectedTopic.value.draft.blocks.filter((item) => item.id !== block.id)
}
async function editTopic(item: StorefrontPageSummary) {
  selectedTopic.value = await storefrontApi.topic(item.id)
  activeTab.value = 'topics'
}
async function saveTopic() {
  if (!selectedTopic.value) return
  if (!selectedTopic.value.title.trim() || !/^[a-z0-9](?:[a-z0-9-]{0,78}[a-z0-9])?$/.test(selectedTopic.value.slug || '')) {
    ElMessage.warning('请填写标题，并使用小写字母、数字或连字符作为专题链接')
    return
  }
  saving.value = true
  try {
    const current = selectedTopic.value
    selectedTopic.value = current.id ? await storefrontApi.updateTopic(current.id, current) : await storefrontApi.createTopic(current)
    await loadTopics()
    ElMessage.success('专题草稿已保存')
  } finally { saving.value = false }
}
async function publishTopic() {
  if (!selectedTopic.value) return
  if (!selectedTopic.value.draft.blocks.length) { ElMessage.warning('专题至少添加一个内容模块后才能发布'); return }
  await saveTopic()
  if (!selectedTopic.value?.id) return
  try {
    await storefrontApi.publishTopic(selectedTopic.value.id)
    ElMessage.success('专题已发布')
    await loadTopics()
    await editTopic(topics.value.find((item) => item.id === selectedTopic.value?.id)!)
  } catch (error) { throw error }
}
async function offlineTopic(item: StorefrontPageSummary) {
  try {
    await ElMessageBox.confirm('下线后，小程序专题链接将停止访问。', '下线专题', { type: 'warning' })
    await storefrontApi.offlineTopic(item.id)
    await loadTopics()
    if (selectedTopic.value?.id === item.id) await editTopic(topics.value.find((topic) => topic.id === item.id)!)
    ElMessage.success('专题已下线')
  } catch { /* keep current state */ }
}
onMounted(loadAll)
</script>

<template>
  <main class="storefront page" v-loading="loading">
    <header class="page-header">
      <div>
        <h1 class="page-title">店铺装修</h1>
        <p class="page-desc">搭建小程序首页、专题内容，并在发布前预览买家看到的页面。</p>
      </div>
      <div v-if="activeTab === 'home'" class="header-actions">
        <el-tag :type="homePage?.status === 'PUBLISHED' ? 'success' : 'info'">线上：{{ statusText(homePage?.status) }}</el-tag>
        <el-button :disabled="!isHomeDirty" :loading="saving" @click="saveHome">保存草稿</el-button>
        <el-button type="primary" :loading="publishing" @click="publishHome">发布首页</el-button>
      </div>
      <div v-else-if="activeTab === 'topics' && selectedTopic" class="header-actions">
        <el-tag :type="selectedTopic.status === 'PUBLISHED' ? 'success' : 'info'">{{ statusText(selectedTopic.status) }}</el-tag>
        <el-button :loading="saving" @click="saveTopic">保存草稿</el-button>
        <el-button type="primary" @click="publishTopic">发布专题</el-button>
      </div>
      <div v-else-if="activeTab === 'topics'" class="header-actions"><el-button type="primary" @click="newTopic">新建专题</el-button></div>
    </header>

    <el-tabs v-model="activeTab" class="storefront-tabs">
      <el-tab-pane label="首页搭建" name="home">
        <div class="builder-layout">
          <aside class="builder-sidebar">
            <section class="builder-section">
              <div class="section-heading"><h2>当前首页</h2><span>{{ orderedModules.length }} 个模块</span></div>
              <p class="section-hint">拖动调整位置，也可用按钮排序。</p>
              <div v-if="orderedModules.length" class="module-stack">
                <article v-for="(module, index) in orderedModules" :key="module.id" class="module-row" :class="{ selected: selectedModule?.id === module.id, muted: module.enabled !== 1 }" draggable="true" @dragstart="beginDrag($event, module.id)" @dragover.prevent @drop.prevent="dropOn(module.id)">
                  <button class="drag-handle" type="button" :aria-label="`拖动排序：${module.title}`" @click="selectedModuleId = module.id">拖动</button>
                  <button class="module-select" type="button" @click="selectedModuleId = module.id"><strong>{{ module.title }}</strong><small>{{ library.find((item) => item.code === module.code)?.name }}</small></button>
                  <div class="row-actions">
                    <button type="button" :disabled="index === 0" :aria-label="`${module.title}上移`" @click="moveModule(index, -1)">上移</button>
                    <button type="button" :disabled="index === orderedModules.length - 1" :aria-label="`${module.title}下移`" @click="moveModule(index, 1)">下移</button>
                  </div>
                </article>
              </div>
              <el-empty v-else description="还没有首页模块" :image-size="64" />
            </section>
            <section class="builder-section library-section">
              <div class="section-heading"><h2>添加模块</h2></div>
              <button v-for="item in library" :key="item.code" class="library-item" type="button" :disabled="!canAdd(item.code)" @click="addModule(item.code)">
                <span><strong>{{ item.name }}</strong><small>{{ item.description }}</small></span><span class="add-mark">添加</span>
              </button>
            </section>
          </aside>

          <section class="preview-column" aria-label="小程序首页预览">
            <div class="preview-heading"><div><h2>小程序预览</h2><p>展示已启用模块的顺序与内容结构</p></div><span>买家端</span></div>
            <div class="phone-frame"><div class="phone-top"><span>9:41</span><span>•••　◉</span></div><div class="mini-nav"><span class="mini-brand">潮</span><span class="mini-search">搜索商品</span></div>
              <div class="mini-screen">
                <template v-for="module in orderedModules.filter((item) => item.enabled === 1)" :key="module.id">
                  <section v-if="module.code === 'BANNER'" class="mini-banner"><span>店铺精选</span><strong>{{ module.title }}</strong><small>{{ module.subtitle || '欢迎逛逛本店好物' }}</small></section>
                  <section v-else-if="module.code === 'CATEGORY'" class="mini-categories"><div v-for="name in ['办公文具', '学习用品', '创意好物', '更多分类']" :key="name"><i></i><small>{{ name }}</small></div></section>
                  <section v-else-if="['NEW_ARRIVALS', 'POPULAR_PRODUCTS'].includes(module.code)" class="mini-products"><div class="mini-section-heading"><strong>{{ module.title }}</strong><small>{{ module.subtitle || '精选好物' }}</small></div><div class="mini-product-row"><div v-for="item in Math.min(module.productLimit || 4, 4)" :key="item" class="mini-product"><i></i><b>办公精选商品</b><em>¥15.00</em></div></div></section>
                  <section v-else-if="module.code === 'PRODUCT_FEED'" class="mini-products"><div class="mini-section-heading"><strong>{{ module.title }}</strong><small>{{ module.subtitle || '新品与热销都在这里' }}</small></div><div class="mini-feed"><div v-for="item in 4" :key="item"><i></i><b>日常实用好物</b><em>¥12.90</em></div></div></section>
                  <section v-else-if="module.code === 'MARKETING_ZONE'" class="mini-marketing"><strong>{{ module.title }}</strong><small>{{ module.subtitle || '限时活动与店铺优惠' }}</small><div><span>限时会场</span><span>拼团好物</span></div></section>
                  <section v-else-if="module.code === 'TOPIC_ENTRY'" class="mini-topic"><strong>{{ module.title }}</strong><small>{{ module.subtitle || '逛逛店铺专题' }}</small><div v-for="topic in topics.filter((item) => item.status === 'PUBLISHED' && (module.topicPageIds || []).includes(item.id)).slice(0, 2)" :key="topic.id">{{ topic.title }}　›</div><div v-if="!(module.topicPageIds || []).length" class="topic-placeholder">勾选专题后将在这里展示</div><div v-else-if="!topics.some((item) => item.status === 'PUBLISHED' && (module.topicPageIds || []).includes(item.id))" class="topic-placeholder">所选专题发布后将在这里展示</div></section>
                </template>
                <div v-if="!orderedModules.some((item) => item.enabled === 1)" class="preview-empty">启用一个模块后即可预览首页</div>
              </div><div class="phone-home"></div>
            </div>
          </section>

          <aside class="settings-panel">
            <template v-if="selectedModule">
              <div class="settings-heading"><div><h2>模块设置</h2><p>{{ selectedModule.title }}</p></div><el-switch v-model="selectedModule.enabled" :active-value="1" :inactive-value="0" aria-label="模块展示开关" /></div>
              <el-form label-position="top" class="settings-form">
                <el-form-item label="模块标题"><el-input v-model="selectedModule.title" maxlength="64" show-word-limit /></el-form-item>
                <el-form-item label="副标题"><el-input v-model="selectedModule.subtitle" maxlength="128" show-word-limit /></el-form-item>
                <template v-if="selectedModule.productSource">
                  <el-form-item label="商品来源"><el-select v-model="selectedModule.productSource"><el-option v-for="option in sourceOptions" :key="option.value" :label="option.label" :value="option.value" /></el-select></el-form-item>
                  <el-form-item label="展示数量"><el-input-number v-model="selectedModule.productLimit" :min="1" :max="20" controls-position="right" /></el-form-item>
                </template>
                <el-form-item v-if="selectedModule.code === 'TOPIC_ENTRY'" label="展示专题">
                  <el-checkbox-group v-model="selectedModule.topicPageIds">
                    <el-checkbox v-for="topic in topics.filter((item) => item.status === 'PUBLISHED')" :key="topic.id" :value="topic.id">{{ topic.title }}</el-checkbox>
                  </el-checkbox-group>
                  <small v-if="!topics.some((item) => item.status === 'PUBLISHED')" class="field-hint">先创建并发布专题，再回来选择。</small>
                </el-form-item>
              </el-form>
              <button class="remove-module" type="button" @click="removeModule(selectedModule)">从首页移除</button>
            </template>
            <el-empty v-else description="选择或添加一个模块开始编辑" :image-size="72" />
          </aside>
        </div>
      </el-tab-pane>

      <el-tab-pane label="专题页" name="topics">
        <div class="topic-workspace">
          <aside class="topic-list-panel">
            <div class="section-heading"><h2>专题内容</h2><el-button type="primary" plain @click="newTopic">新建专题</el-button></div>
            <button v-for="topic in topics" :key="topic.id" class="topic-list-item" :class="{ active: selectedTopic?.id === topic.id }" type="button" @click="editTopic(topic)">
              <span class="topic-cover"><img v-if="topic.coverImage" :src="topic.coverImage" :alt="`${topic.title}封面`"><span v-else>专题</span></span>
              <span class="topic-copy"><strong>{{ topic.title }}</strong><small>/pages/topic/detail?slug={{ topic.slug }}</small></span>
              <el-tag size="small" :type="topic.status === 'PUBLISHED' ? 'success' : 'info'">{{ statusText(topic.status) }}</el-tag>
            </button>
            <el-empty v-if="!topics.length" description="创建第一个专题页" :image-size="64" />
          </aside>
          <section v-if="selectedTopic" class="topic-editor">
            <div class="topic-editor-heading"><div><h2>{{ selectedTopic.id ? '编辑专题' : '新建专题' }}</h2><p>用图文和商品模块讲清主题，内容使用纯文本与受控组件。</p></div><button v-if="selectedTopic.id" class="text-action" type="button" @click="newTopic">新建</button></div>
            <el-form label-position="top" class="topic-form">
              <div class="topic-meta-grid"><el-form-item label="专题标题"><el-input v-model="selectedTopic.title" maxlength="128" show-word-limit placeholder="例如：开学季文具精选" /></el-form-item><el-form-item label="短链接"><el-input v-model="selectedTopic.slug" maxlength="80"><template #prepend>/topic/</template></el-input><small class="field-hint">仅支持小写字母、数字和连字符。</small></el-form-item></div>
              <el-form-item label="专题简介"><el-input v-model="selectedTopic.summary" maxlength="256" show-word-limit placeholder="简要说明本专题的内容" /></el-form-item>
              <el-form-item label="专题封面"><ImageUploader v-model="selectedTopic.coverImage" scope="merchant" label="上传专题封面" /></el-form-item>
            </el-form>
            <div class="content-block-heading"><div><h3>页面内容</h3><p>按从上到下的顺序展示在小程序专题页（最多 40 个模块）</p></div><div class="block-tools"><el-button size="small" :disabled="selectedTopic.draft.blocks.length >= 40" @click="addTopicBlock('TEXT')">添加文字</el-button><el-button size="small" :disabled="selectedTopic.draft.blocks.length >= 40" @click="addTopicBlock('IMAGE_TEXT')">添加图文</el-button><el-button size="small" :disabled="selectedTopic.draft.blocks.length >= 40" @click="addTopicBlock('PRODUCTS')">添加商品</el-button><el-button size="small" :disabled="selectedTopic.draft.blocks.length >= 40" @click="addTopicBlock('BUTTON')">添加按钮</el-button></div></div>
            <div v-if="selectedTopic.draft.blocks.length" class="block-list">
              <article v-for="(block, index) in selectedTopic.draft.blocks" :key="block.id" class="content-block">
                <div class="block-order"><strong>{{ block.type === 'TEXT' ? '文字' : block.type === 'IMAGE_TEXT' ? '图文' : block.type === 'PRODUCTS' ? '商品集合' : '跳转按钮' }}</strong><div><button type="button" :disabled="index === 0" @click="moveBlock(index, -1)">上移</button><button type="button" :disabled="index === selectedTopic.draft.blocks.length - 1" @click="moveBlock(index, 1)">下移</button><button class="danger-action" type="button" @click="removeBlock(block)">移除</button></div></div>
                <el-form label-position="top" class="block-form">
                  <el-form-item v-if="block.type !== 'BUTTON'" label="标题"><el-input v-model="block.title" maxlength="100" /></el-form-item>
                  <el-form-item v-if="block.type === 'TEXT' || block.type === 'IMAGE_TEXT'" label="正文"><el-input v-model="block.body" type="textarea" :rows="4" maxlength="4000" show-word-limit /></el-form-item>
                  <el-form-item v-if="block.type === 'IMAGE_TEXT'" label="配图"><ImageUploader v-model="block.imageUrl" scope="merchant" label="上传内容图片" /></el-form-item>
                  <template v-if="block.type === 'PRODUCTS'"><el-form-item label="商品来源"><el-select v-model="block.productSource"><el-option v-for="option in sourceOptions" :key="option.value" :label="option.label" :value="option.value" /></el-select></el-form-item><el-form-item label="商品数量"><el-input-number v-model="block.productLimit" :min="1" :max="20" controls-position="right" /></el-form-item></template>
                  <template v-if="block.type === 'BUTTON'"><el-form-item label="按钮文字"><el-input v-model="block.buttonText" maxlength="24" placeholder="查看商品" /></el-form-item><el-form-item label="点击后前往"><el-select v-model="block.linkType"><el-option label="全部商品" value="PRODUCTS"/><el-option label="商品分类" value="CATEGORY"/><el-option label="优惠券" value="COUPON"/><el-option label="拼团会场" value="GROUP_BUY"/></el-select></el-form-item></template>
                </el-form>
              </article>
            </div>
            <el-empty v-else description="添加文字、图文或商品模块，丰富专题内容" :image-size="72" />
            <div v-if="selectedTopic.id && selectedTopic.status === 'PUBLISHED'" class="topic-footer"><span>线上专题仍展示上次发布版本</span><el-button type="danger" plain @click="offlineTopic(topics.find((item) => item.id === selectedTopic?.id)!)">下线专题</el-button></div>
          </section>
          <section v-else class="topic-blank"><el-empty description="选择专题或新建专题开始编辑" /></section>
          <aside v-if="selectedTopic" class="topic-preview-column"><div class="preview-heading"><div><h2>专题预览</h2><p>草稿内容</p></div></div><div class="phone-frame topic-phone"><div class="phone-top"><span>9:41</span><span>•••　◉</span></div><div class="mini-nav"><span class="mini-brand">潮</span><span class="mini-search">{{ selectedTopic.title || '专题标题' }}</span></div><div class="mini-screen topic-mini-screen"><img v-if="selectedTopic.coverImage" class="topic-hero-image" :src="selectedTopic.coverImage" alt="专题封面预览"><div v-else class="topic-hero-placeholder">{{ selectedTopic.title || '专题封面' }}</div><p class="topic-summary-preview">{{ selectedTopic.summary || '专题简介会显示在这里' }}</p><section v-for="block in selectedTopic.draft.blocks" :key="block.id" class="topic-mini-block"><strong v-if="block.title">{{ block.title }}</strong><img v-if="block.type === 'IMAGE_TEXT' && block.imageUrl" :src="block.imageUrl" alt="专题内容图片"><p v-if="block.body">{{ block.body }}</p><div v-if="block.type === 'PRODUCTS'" class="mini-feed"><div v-for="item in Math.min(block.productLimit || 4, 4)" :key="item"><i></i><b>精选商品</b><em>¥15.00</em></div></div><span v-if="block.type === 'BUTTON'" class="preview-button">{{ block.buttonText || '查看详情' }}</span></section></div><div class="phone-home"></div></div></aside>
        </div>
      </el-tab-pane>

      <el-tab-pane label="模板库" name="templates">
        <div class="template-intro"><div><h2>选择一套首页结构</h2><p>模板只写入当前草稿。检查预览、保存后，再决定是否发布。</p></div><el-button @click="activeTab = 'home'">返回首页搭建</el-button></div>
        <div class="template-grid">
          <article v-for="template in templates" :key="template.code" class="template-card">
            <div class="template-preview"><span class="template-banner"></span><span class="template-line"></span><span class="template-products"><i></i><i></i></span><span class="template-line short"></span></div>
            <div class="template-copy"><div><h3>{{ template.name }}</h3><span>{{ template.document.modules.length }} 个模块</span></div><p>{{ template.description }}</p><el-button type="primary" plain @click="applyTemplate(template)">预览并应用</el-button></div>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>
  </main>
</template>

<style scoped>
.storefront{width:min(1600px,calc(100% - 48px));margin:26px auto 48px;color:var(--shop-ink,#292521)}
.page-header,.header-actions,.section-heading,.preview-heading,.settings-heading,.topic-editor-heading,.content-block-heading,.template-intro{display:flex;align-items:center;justify-content:space-between;gap:16px}
.page-title{margin:0;font-size:27px;font-weight:800;letter-spacing:-.025em}.page-desc{margin:8px 0 0;color:var(--shop-muted,#81776e);font-size:14px;line-height:1.55;max-width:68ch}.header-actions{flex-wrap:nowrap;white-space:nowrap}.header-actions :deep(.el-button){flex:0 0 auto;margin:0}
.storefront-tabs{margin-top:24px}.storefront-tabs :deep(.el-tabs__item){height:48px;font-weight:650}.builder-layout{display:grid;grid-template-columns:minmax(245px,300px) minmax(300px,1fr) minmax(260px,320px);gap:20px;align-items:start;padding-top:14px}
.builder-sidebar,.settings-panel,.topic-list-panel,.topic-editor,.topic-preview-column{min-width:0;border:1px solid var(--shop-border,#e9e2da);border-radius:14px;background:var(--shop-surface,#fff)}.builder-sidebar,.settings-panel,.topic-list-panel,.topic-editor{padding:18px}.builder-section+.builder-section{margin-top:22px;padding-top:18px;border-top:1px solid var(--shop-border,#e9e2da)}.section-heading h2,.preview-heading h2,.settings-heading h2,.topic-editor-heading h2,.template-intro h2{margin:0;font-size:16px;font-weight:750}.section-heading>span,.preview-heading>span{color:var(--shop-muted,#81776e);font-size:12px}.section-hint,.preview-heading p,.settings-heading p,.topic-editor-heading p,.template-intro p,.content-block-heading p{margin:5px 0 0;color:var(--shop-muted,#81776e);font-size:12px;line-height:1.5}
.module-stack{display:flex;flex-direction:column;gap:7px;margin-top:12px}.module-row{display:grid;grid-template-columns:43px minmax(0,1fr);gap:8px;padding:8px;border:1px solid #e9e2da;border-radius:10px;background:#fff;transition:border-color .16s,background .16s}.module-row.selected{border-color:#ef6a2b;background:#fff8f2}.module-row.muted{opacity:.64}.drag-handle{grid-row:span 2;align-self:stretch;min-width:42px;border:0;border-radius:7px;background:#f5f1ec;color:#61554c;font-size:11px;cursor:grab}.drag-handle:active{cursor:grabbing}.module-select{min-width:0;padding:1px 0;border:0;background:transparent;text-align:left;color:inherit;cursor:pointer}.module-select strong,.module-select small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.module-select strong{font-size:13px}.module-select small{margin-top:3px;color:var(--shop-muted,#81776e);font-size:11px}.row-actions{display:flex;gap:12px}.row-actions button,.block-order button{padding:2px 0;border:0;background:none;color:#a34a21;font-size:11px;cursor:pointer}.row-actions button:disabled,.block-order button:disabled{color:#bdb5ac;cursor:not-allowed}.library-section .section-heading{margin-bottom:8px}.library-item{width:100%;display:flex;align-items:center;justify-content:space-between;gap:8px;padding:10px 2px;border:0;border-bottom:1px solid #f1ece7;background:transparent;text-align:left;color:inherit;cursor:pointer}.library-item:last-child{border-bottom:0}.library-item strong,.library-item small{display:block}.library-item strong{font-size:12px}.library-item small{margin-top:3px;color:var(--shop-muted,#81776e);font-size:11px}.library-item:disabled{opacity:.5;cursor:not-allowed}.add-mark{flex:none;color:#a34a21;font-size:11px}
.preview-column{display:flex;flex-direction:column;align-items:center;min-width:0}.preview-heading{width:min(100%,400px);margin-bottom:12px}.preview-heading>div{min-width:0}.phone-frame{width:min(100%,370px);height:730px;display:flex;flex-direction:column;overflow:hidden;border:9px solid #252525;border-radius:36px;background:#f8f5f0;box-shadow:0 12px 28px rgba(29,22,17,.14)}.phone-top{height:30px;flex:none;display:flex;justify-content:space-between;align-items:center;padding:0 13px;color:#282521;background:#fff;font-size:10px}.mini-nav{height:42px;flex:none;display:flex;align-items:center;gap:9px;padding:0 12px;background:#fff}.mini-brand{width:27px;height:27px;display:grid;place-items:center;border-radius:8px;color:#fff;background:#26221e;font-weight:800}.mini-search{flex:1;padding:8px 10px;border:1px solid #eee6dc;border-radius:9px;color:#8b8179;font-size:10px}.mini-screen{flex:1;overflow:auto;padding:10px 11px 20px;background:#f8f5f0}.phone-home{height:8px;width:88px;flex:none;align-self:center;margin:6px 0 4px;border-radius:5px;background:#181818}.mini-banner{min-height:118px;display:flex;flex-direction:column;justify-content:flex-end;padding:15px;border-radius:12px;color:#fff;background:linear-gradient(130deg,#2a211b,#a94c24 63%,#ed7840)}.mini-banner span{font-size:9px;opacity:.8}.mini-banner strong{margin-top:7px;font-size:19px;line-height:1.2}.mini-banner small{margin-top:5px;font-size:10px;opacity:.85}.mini-categories{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;margin:11px 0;padding:12px 7px;border-radius:10px;background:#fff}.mini-categories div{text-align:center}.mini-categories i{width:30px;height:30px;display:block;margin:0 auto 5px;border-radius:50%;background:#f7d8c6}.mini-categories small{font-size:8px;color:#544b43}.mini-products,.mini-topic{margin-top:10px;padding:11px;border-radius:11px;background:#fff}.mini-section-heading strong,.mini-section-heading small,.mini-topic>strong,.mini-topic>small{display:block}.mini-section-heading strong,.mini-topic>strong{font-size:12px}.mini-section-heading small,.mini-topic>small{margin-top:4px;color:#857a71;font-size:9px}.mini-product-row{display:grid;grid-template-columns:repeat(2,1fr);gap:7px;margin-top:9px}.mini-product{min-width:0}.mini-product i,.mini-feed i{display:block;height:62px;border-radius:7px;background:linear-gradient(145deg,#eee6dc,#ddd5ca)}.mini-product b,.mini-feed b{display:block;overflow:hidden;margin-top:5px;text-overflow:ellipsis;white-space:nowrap;font-size:8px;font-weight:500}.mini-product em,.mini-feed em{display:block;margin-top:3px;color:#d64e22;font-size:9px;font-style:normal;font-weight:750}.mini-feed{display:grid;grid-template-columns:repeat(2,1fr);gap:8px;margin-top:9px}.mini-feed>div{min-width:0;padding:6px;border:1px solid #eee8e1;border-radius:7px;background:#fff}.mini-marketing{margin-top:10px;padding:12px;border-radius:10px;color:#fff;background:#eb6129}.mini-marketing strong,.mini-marketing small{display:block}.mini-marketing small{margin-top:4px;font-size:9px;opacity:.8}.mini-marketing>div{display:flex;gap:8px;margin-top:10px}.mini-marketing span{flex:1;padding:10px 5px;border-radius:7px;color:#6b321d;background:#fff1e7;text-align:center;font-size:9px}.mini-topic>div{margin-top:8px;padding:11px;border-radius:7px;background:#f7eee6;font-size:9px}.mini-topic .topic-placeholder{color:#887b70}.preview-empty{padding:44px 10px;color:#887b70;text-align:center;font-size:11px}
.settings-panel{position:sticky;top:20px}.settings-heading{align-items:flex-start;margin-bottom:18px}.settings-heading p{font-weight:600;color:#53493f}.settings-form :deep(.el-form-item){margin-bottom:16px}.settings-form :deep(.el-form-item__label){padding-bottom:5px;font-size:12px;font-weight:650;color:#625950}.settings-form :deep(.el-checkbox){display:flex;margin:0 0 5px;white-space:normal}.field-hint{display:block;margin-top:6px;color:#81776e;font-size:11px}.remove-module{width:100%;min-height:38px;margin-top:10px;border:1px solid #ead8ca;border-radius:8px;background:#fffaf6;color:#a34a21;cursor:pointer}
.topic-workspace{display:grid;grid-template-columns:minmax(240px,290px) minmax(350px,1fr) minmax(280px,350px);gap:16px;align-items:start;padding-top:14px}.topic-list-panel{min-height:390px}.topic-list-panel .section-heading{margin-bottom:10px}.topic-list-item{width:100%;display:flex;align-items:center;gap:9px;padding:10px 0;border:0;border-bottom:1px solid #f0ebe6;background:transparent;text-align:left;color:inherit;cursor:pointer}.topic-list-item.active .topic-copy strong{color:#c9521d}.topic-cover{width:43px;height:43px;flex:none;display:grid;place-items:center;overflow:hidden;border-radius:8px;background:#f3e6db;color:#8a6550;font-size:10px}.topic-cover img{width:100%;height:100%;object-fit:cover}.topic-copy{flex:1;min-width:0}.topic-copy strong,.topic-copy small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.topic-copy strong{font-size:12px}.topic-copy small{margin-top:4px;color:#81776e;font-size:10px}.topic-editor{min-height:390px}.topic-editor-heading{margin-bottom:16px}.text-action{border:0;background:none;color:#a34a21;cursor:pointer}.topic-meta-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.topic-form :deep(.el-form-item){margin-bottom:10px}.topic-form :deep(.el-form-item__label),.block-form :deep(.el-form-item__label){padding-bottom:4px;font-size:12px;font-weight:650}.content-block-heading{align-items:flex-end;margin:16px 0 12px}.content-block-heading h3{margin:0;font-size:15px}.block-tools{display:flex;flex-wrap:wrap;gap:6px}.content-block{margin-top:10px;padding:13px;border:1px solid #eae4de;border-radius:10px;background:#fff}.block-order{display:flex;align-items:center;justify-content:space-between;margin-bottom:10px}.block-order strong{font-size:12px}.block-order>div{display:flex;gap:12px}.block-order button{font-size:12px}.danger-action{color:#b74636!important}.block-form{display:grid;grid-template-columns:1fr 1fr;gap:0 12px}.block-form :deep(.el-form-item){margin-bottom:8px}.block-form :deep(.el-form-item:has(textarea)){grid-column:1/-1}.topic-footer{display:flex;align-items:center;justify-content:space-between;gap:10px;margin-top:16px;padding-top:14px;border-top:1px solid #eee7e0;color:#81776e;font-size:11px}.topic-blank{min-height:390px;display:grid;place-items:center;border:1px dashed #ddd2c8;border-radius:14px;background:#fff}.topic-preview-column{display:flex;flex-direction:column;align-items:center;padding:15px}.topic-preview-column .preview-heading{width:100%}.topic-phone{width:min(100%,330px);height:690px}.topic-mini-screen{padding:0 0 18px}.topic-hero-image,.topic-hero-placeholder{width:100%;height:142px;object-fit:cover}.topic-hero-placeholder{display:grid;place-items:center;color:#fff;background:linear-gradient(135deg,#533827,#d77b42);font-size:17px;font-weight:800}.topic-summary-preview{margin:0;padding:10px 12px;color:#786e65;background:#fff;font-size:9px;line-height:1.6}.topic-mini-block{margin:9px;padding:10px;border-radius:9px;background:#fff}.topic-mini-block>strong{display:block;font-size:11px}.topic-mini-block>p{margin:7px 0 0;color:#655c53;font-size:9px;line-height:1.6;white-space:pre-wrap}.topic-mini-block>img{width:100%;max-height:140px;margin-top:8px;object-fit:cover;border-radius:7px}.preview-button{display:block;margin-top:8px;padding:8px;border-radius:7px;color:#fff;background:#e95d25;text-align:center;font-size:9px}
.template-intro{margin:8px 0 18px}.template-intro h2{font-size:19px}.template-intro p{font-size:13px}.template-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}.template-card{display:grid;grid-template-columns:130px minmax(0,1fr);gap:18px;align-items:center;padding:16px;border:1px solid #e9e2da;border-radius:14px;background:#fff}.template-preview{height:174px;display:flex;flex-direction:column;gap:8px;overflow:hidden;padding:9px;border-radius:11px;background:#f8f5f0}.template-banner{height:57px;flex:none;border-radius:7px;background:linear-gradient(120deg,#30251e,#cf6933)}.template-line{height:13px;flex:none;border-radius:5px;background:#fff}.template-line.short{width:70%}.template-products{height:48px;display:flex;gap:7px}.template-products i{flex:1;border-radius:6px;background:linear-gradient(150deg,#e8ddd1,#fff)}.template-copy{min-width:0}.template-copy>div{display:flex;align-items:center;justify-content:space-between;gap:8px}.template-copy h3{margin:0;font-size:16px}.template-copy>div span{color:#81776e;font-size:11px}.template-copy p{min-height:40px;margin:9px 0 14px;color:#81776e;font-size:12px;line-height:1.6}
button:focus-visible{outline:2px solid #bf4a19;outline-offset:2px}.drag-handle:focus-visible,.library-item:focus-visible,.topic-list-item:focus-visible{border-radius:6px}
@media(max-width:1200px){.builder-layout{grid-template-columns:minmax(220px,280px) minmax(300px,1fr)}.settings-panel{grid-column:1/-1;position:static}.topic-workspace{grid-template-columns:250px minmax(330px,1fr)}.topic-preview-column{grid-column:1/-1}.template-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:760px){.storefront{width:calc(100% - 24px);margin-top:16px}.page-header{align-items:flex-start;flex-direction:column}.header-actions{width:100%;justify-content:flex-start;flex-wrap:wrap}.header-actions :deep(.el-button){width:auto}.builder-layout,.topic-workspace{grid-template-columns:1fr}.builder-sidebar{order:2}.preview-column{order:1}.settings-panel{order:3;grid-column:auto}.phone-frame{height:670px}.topic-list-panel{order:1}.topic-editor{order:2}.topic-preview-column{order:3;grid-column:auto}.template-grid{grid-template-columns:1fr}.template-card{grid-template-columns:100px minmax(0,1fr);gap:12px;padding:12px}.template-preview{height:150px}.topic-meta-grid{grid-template-columns:1fr}.content-block-heading{align-items:flex-start;flex-direction:column}.block-tools{width:100%}}
</style>
