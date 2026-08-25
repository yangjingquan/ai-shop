<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminBannerApi, type BannerPayload, type BannerVO } from '@/api/banner'
import ImageUploader from '@/components/upload/ImageUploader.vue'

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081').replace(/\/$/, '')
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const list = ref<BannerVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const form = reactive<BannerPayload>({ imageUrl: '', linkType: 0, linkValue: '', sort: 0, status: 1 })

function resolveImageUrl(url: string) { return /^(https?:)?\/\//.test(url) ? url : `${apiBaseUrl}${url.startsWith('/') ? url : `/${url}`}` }
const linkTypeText: Record<number, string> = { 0: '不跳转', 3: '外部链接' }

async function fetchList() {
  loading.value = true
  try { const data = await adminBannerApi.page({ page: query.page, size: query.size }); list.value = data.list; total.value = data.total } finally { loading.value = false }
}
function resetForm() { editingId.value = null; form.imageUrl = ''; form.linkType = 0; form.linkValue = ''; form.sort = 0; form.status = 1 }
function onCreate() { resetForm(); dialogVisible.value = true }
function onEdit(row: BannerVO) { editingId.value = row.id; form.imageUrl = row.imageUrl; form.linkType = row.linkType; form.linkValue = row.linkValue || ''; form.sort = row.sort ?? 0; form.status = row.status ?? 1; dialogVisible.value = true }
async function onSave() {
  if (!form.imageUrl) return ElMessage.warning('请上传 Banner 图片')
  if (form.linkType === 3 && !/^https:\/\/[^\s]+$/i.test(form.linkValue || '')) return ElMessage.warning('外部链接必须使用 HTTPS')
  saving.value = true
  try { if (editingId.value) await adminBannerApi.update(editingId.value, form); else await adminBannerApi.create(form); ElMessage.success('保存成功'); dialogVisible.value = false; fetchList() } finally { saving.value = false }
}
async function onRemove(row: BannerVO) { await ElMessageBox.confirm('确定删除该平台 Banner？', '提示', { type: 'warning' }); await adminBannerApi.remove(row.id); ElMessage.success('已删除'); fetchList() }
onMounted(fetchList)
</script>

<template>
  <div class="page"><div class="page-header"><div><span class="page-kicker">PLATFORM CONTENT</span><h1 class="page-title">平台 Banner</h1><p class="page-desc">维护全平台默认轮播内容；平台 Banner 会与商家 Banner 一起展示。</p></div><el-button type="primary" @click="onCreate">新增 Banner</el-button></div>
    <el-card><el-table v-loading="loading" :data="list" stripe><el-table-column prop="id" label="ID" width="70" /><el-table-column label="图片" width="170"><template #default="{ row }"><el-image :src="resolveImageUrl(row.imageUrl)" fit="cover" style="width: 132px; height: 56px; border-radius: 8px" /></template></el-table-column><el-table-column label="类型" width="120"><template #default="{ row }">{{ linkTypeText[row.linkType] || '历史类型' }}</template></el-table-column><el-table-column prop="linkValue" label="链接" min-width="250" show-overflow-tooltip /><el-table-column prop="sort" label="排序" width="80" /><el-table-column label="状态" width="80"><template #default="{ row }"><el-tag>{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="140"><template #default="{ row }"><el-button link type="primary" @click="onEdit(row as BannerVO)">编辑</el-button><el-button link type="danger" @click="onRemove(row as BannerVO)">删除</el-button></template></el-table-column></el-table><div class="pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" background layout="total, sizes, prev, pager, next" @current-change="fetchList" @size-change="fetchList" /></div></el-card>
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑平台 Banner' : '新增平台 Banner'" width="700px"><el-form label-width="110px"><el-form-item label="图片" required><ImageUploader v-model="form.imageUrl" scope="admin" :limit="1" label="上传 Banner" /></el-form-item><el-form-item label="跳转类型"><el-radio-group v-model="form.linkType"><el-radio-button :value="0">不跳转</el-radio-button><el-radio-button :value="3">HTTPS 外链</el-radio-button></el-radio-group></el-form-item><el-form-item v-if="form.linkType === 3" label="外部链接" required><el-input v-model="form.linkValue" placeholder="https://example.com" /></el-form-item><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="9999" /></el-form-item><el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item></el-form><template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="onSave">保存</el-button></template></el-dialog>
  </div>
</template>
