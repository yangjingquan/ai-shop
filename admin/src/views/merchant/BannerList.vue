<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bannerApi, type BannerPayload, type BannerVO } from '@/api/banner'
import ImageUploader from '@/components/upload/ImageUploader.vue'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const list = ref<BannerVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const form = reactive<BannerPayload>({
  imageUrl: '',
  linkType: 0,
  linkValue: '',
  sort: 0,
  status: 1,
})

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081').replace(/\/$/, '')

const linkTypeText: Record<number, string> = {
  0: '不跳转',
  1: '小程序页面',
  3: '外部链接',
}

function resolveImageUrl(url: string) {
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) return url
  return `${apiBaseUrl}${url.startsWith('/') ? url : `/${url}`}`
}

async function fetchList() {
  loading.value = true
  try {
    const data = await bannerApi.page({ page: query.page, size: query.size })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.imageUrl = ''
  form.linkType = 0
  form.linkValue = ''
  form.sort = 0
  form.status = 1
}

function onCreate() {
  resetForm()
  dialogVisible.value = true
}

function onEdit(row: BannerVO) {
  editingId.value = row.id
  form.imageUrl = row.imageUrl
  form.linkType = row.linkType
  form.linkValue = row.linkValue || ''
  form.sort = row.sort ?? 0
  form.status = row.status ?? 1
  dialogVisible.value = true
}

async function onSave() {
  if (!form.imageUrl) {
    ElMessage.warning('请上传 Banner 图片')
    return
  }
  if (form.linkType !== 0 && !form.linkValue) {
    ElMessage.warning('请填写跳转地址')
    return
  }

  saving.value = true
  try {
    const payload = { ...form, linkValue: form.linkType === 0 ? '' : form.linkValue }
    if (editingId.value) {
      await bannerApi.update(editingId.value, payload)
      ElMessage.success('Banner 已更新')
    } else {
      await bannerApi.create(payload)
      ElMessage.success('Banner 已新增')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    saving.value = false
  }
}

async function onRemove(row: BannerVO) {
  await ElMessageBox.confirm('确定要删除该 Banner？此操作不可撤销。', '提示', { type: 'warning' })
  await bannerApi.remove(row.id)
  ElMessage.success('已删除')
  fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="banner-list">
    <div class="page-header">
      <div>
        <span class="page-kicker">MINI PROGRAM</span>
        <h1 class="page-title">Banner 配置</h1>
        <p class="page-desc">配置小程序首页轮播图、展示顺序与点击跳转。</p>
      </div>
      <el-button type="primary" @click="onCreate">新增 Banner</el-button>
    </div>

    <el-card>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="图片" width="170">
          <template #default="{ row }">
            <el-image
              :src="resolveImageUrl((row as BannerVO).imageUrl)"
              fit="cover"
              :preview-src-list="[resolveImageUrl((row as BannerVO).imageUrl)]"
              preview-teleported
              style="width: 132px; height: 56px; border-radius: 8px"
            />
          </template>
        </el-table-column>
        <el-table-column label="跳转类型" width="120">
          <template #default="{ row }">{{ linkTypeText[(row as BannerVO).linkType] || '未知' }}</template>
        </el-table-column>
        <el-table-column prop="linkValue" label="跳转地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="(row as BannerVO).status === 1 ? 'success' : 'info'">
              {{ (row as BannerVO).status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row as BannerVO)">编辑</el-button>
            <el-button link type="danger" @click="onRemove(row as BannerVO)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next"
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑 Banner' : '新增 Banner'"
      width="760px"
    >
      <el-form label-width="116px">
        <el-form-item label="Banner 图片" required>
          <ImageUploader v-model="form.imageUrl" scope="merchant" :limit="1" label="上传 Banner" />
        </el-form-item>
        <el-form-item label="跳转类型" required>
          <el-radio-group v-model="form.linkType">
            <el-radio-button :value="0">不跳转</el-radio-button>
            <el-radio-button :value="1">小程序页面</el-radio-button>
            <el-radio-button :value="3">外部链接</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.linkType !== 0" label="跳转地址" required>
          <el-input
            v-model="form.linkValue"
            :placeholder="form.linkType === 1 ? '/pages/product/detail?id=1' : 'https://example.com'"
          />
        </el-form-item>
        <el-form-item label="排序" required>
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态" required>
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.banner-list :deep(.el-image) {
  overflow: hidden;
  border: 1px solid var(--shop-border);
  background: #fff8ed;
}
</style>
