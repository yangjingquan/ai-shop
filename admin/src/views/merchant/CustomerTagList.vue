<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { customerOperationsApi, type CustomerTag } from '@/api/customerOperations'

const loading = ref(false)
const tags = ref<CustomerTag[]>([])
const dialogVisible = ref(false)
const bindingVisible = ref(false)
const editing = ref<CustomerTag | null>(null)
const bindingTag = ref<CustomerTag | null>(null)
const form = reactive({ name: '', color: '#D86F22', status: 1 })
const binding = reactive({ userIdsText: '', status: 1 })

async function load() {
  loading.value = true
  try { tags.value = await customerOperationsApi.tags() } finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', color: '#D86F22', status: 1 })
  dialogVisible.value = true
}

function openEdit(value: unknown) {
  const tag = value as CustomerTag
  if (tag.tagType === 'SYSTEM') return
  editing.value = tag
  Object.assign(form, { name: tag.name, color: tag.color, status: tag.status })
  dialogVisible.value = true
}

async function save() {
  if (!form.name.trim()) return ElMessage.warning('请输入标签名称')
  const payload = { ...form, name: form.name.trim() }
  if (editing.value) await customerOperationsApi.updateTag(editing.value.id, payload)
  else await customerOperationsApi.createTag(payload)
  ElMessage.success('标签已保存')
  dialogVisible.value = false
  await load()
}

function openBinding(value: unknown) {
  const tag = value as CustomerTag
  if (tag.tagType === 'SYSTEM') return
  bindingTag.value = tag
  Object.assign(binding, { userIdsText: '', status: 1 })
  bindingVisible.value = true
}

async function saveBinding() {
  const userIds = binding.userIdsText.split(/[，,\s]+/).map((value) => Number(value)).filter((value) => Number.isInteger(value) && value > 0)
  if (!userIds.length || !bindingTag.value) return ElMessage.warning('请输入至少一个用户 ID')
  await customerOperationsApi.bindTag(bindingTag.value.id, { userIds, status: binding.status })
  ElMessage.success(binding.status === 1 ? '已添加手工标签' : '已移除手工标签')
  bindingVisible.value = false
  await load()
}

onMounted(load)
</script>

<template>
  <div class="customer-page" v-loading="loading">
    <div class="page-header">
      <div><h1 class="page-title">用户标签</h1><p class="page-desc">系统标签按用户指标自动更新；手工标签用于商户的精细化运营。</p></div>
      <el-button v-permission="'merchant:customer:manage'" type="primary" @click="openCreate">新建手工标签</el-button>
    </div>
    <el-alert title="系统标签不可手工修改，指标快照每天自动刷新；进入用户详情或刷新分群时也会即时校准。" type="info" :closable="false" show-icon />
    <el-table class="tag-table" :data="tags" empty-text="暂无标签">
      <el-table-column label="标签" min-width="230"><template #default="{ row }"><span class="tag-dot" :style="{ background: row.color }"></span><strong>{{ row.name }}</strong><span class="tag-code">{{ row.code }}</span></template></el-table-column>
      <el-table-column label="类型" width="120"><template #default="{ row }"><el-tag :type="row.tagType === 'SYSTEM' ? 'warning' : 'success'" effect="plain">{{ row.tagType === 'SYSTEM' ? '系统标签' : '手工标签' }}</el-tag></template></el-table-column>
      <el-table-column prop="userCount" label="当前用户数" width="130" align="right" />
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="210" fixed="right"><template #default="{ row }"><el-button v-permission="'merchant:customer:manage'" link type="primary" :disabled="row.tagType === 'SYSTEM'" @click="openBinding(row)">绑定用户</el-button><el-button v-permission="'merchant:customer:manage'" link :disabled="row.tagType === 'SYSTEM'" @click="openEdit(row)">编辑</el-button></template></el-table-column>
    </el-table>
  </div>

  <el-dialog v-model="dialogVisible" :title="editing ? '编辑手工标签' : '新建手工标签'" width="440px" destroy-on-close>
    <el-form label-width="90px"><el-form-item label="标签名称"><el-input v-model="form.name" maxlength="64" show-word-limit placeholder="例如：重点维护客户" /></el-form-item><el-form-item label="标签颜色"><el-color-picker v-model="form.color" /></el-form-item><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" /></el-form-item></el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>

  <el-dialog v-model="bindingVisible" :title="`绑定「${bindingTag?.name || ''}」`" width="500px" destroy-on-close>
    <el-form label-width="96px"><el-form-item label="用户 ID"><el-input v-model="binding.userIdsText" type="textarea" :rows="3" placeholder="输入用户 ID，多个用户用逗号、空格或换行分隔" /></el-form-item><el-form-item label="处理方式"><el-radio-group v-model="binding.status"><el-radio :value="1">添加标签</el-radio><el-radio :value="0">移除标签</el-radio></el-radio-group></el-form-item></el-form>
    <template #footer><el-button @click="bindingVisible = false">取消</el-button><el-button type="primary" @click="saveBinding">确认</el-button></template>
  </el-dialog>
</template>

<style scoped>
.customer-page { width: min(1280px, calc(100% - 56px)); margin: 28px auto 36px; }.tag-table { margin-top: 18px; }.tag-dot { display: inline-block; width: 10px; height: 10px; margin-right: 9px; border-radius: 50%; }.tag-code { margin-left: 10px; color: var(--shop-text-muted); font-size: 12px; }.page-title { margin: 0; color: var(--shop-text); font-size: 28px; }.page-desc { margin: 8px 0 0; color: var(--shop-text-muted); font-size: 14px; }
</style>
