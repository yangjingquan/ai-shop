<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { ElMessage, type TreeInstance } from 'element-plus'
import { merchantCategoryApi, type CategoryVO } from '@/api/category'

const props = defineProps<{ modelValue: boolean }>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved'): void
}>()

const treeRef = ref<TreeInstance>()
const loading = ref(false)
const submitting = ref(false)
const platformTree = ref<CategoryVO[]>([])
const includeChildren = ref(true)

async function load() {
  loading.value = true
  try {
    platformTree.value = (await merchantCategoryApi.platformTree()) ?? []
    await nextTick()
    treeRef.value?.setCheckedKeys([])
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) load()
  },
)

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  const ids = treeRef.value?.getCheckedKeys(false).map((id) => Number(id)) ?? []
  if (ids.length === 0) {
    ElMessage.warning('请选择要导入的分类')
    return
  }
  submitting.value = true
  try {
    await merchantCategoryApi.importFromPlatform({
      sourceCategoryIds: ids,
      includeChildren: includeChildren.value,
    })
    ElMessage.success('导入完成')
    emit('saved')
    close()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="从平台分类导入"
    width="560px"
    @update:model-value="(v) => emit('update:modelValue', v)"
    @close="close"
  >
    <el-alert
      title="导入后会成为本店分类，可独立修改名称、图标和排序，不影响平台分类。"
      type="info"
      show-icon
      :closable="false"
    />
    <div class="import-options">
      <el-checkbox v-model="includeChildren">选择一级分类时同时导入其二级分类</el-checkbox>
    </div>
    <el-tree
      ref="treeRef"
      v-loading="loading"
      :data="platformTree"
      node-key="id"
      show-checkbox
      default-expand-all
      :props="{ label: 'name', children: 'children' }"
      class="platform-tree"
    />
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">导入</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.import-options {
  margin: 14px 0;
}
.platform-tree {
  max-height: 360px;
  overflow: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 8px;
}
</style>
