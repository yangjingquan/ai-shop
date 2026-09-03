<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantCategoryApi, type MerchantCategoryVO } from '@/api/category'
import CategoryDialog from './CategoryDialog.vue'
import CategoryImportDialog from './CategoryImportDialog.vue'

const tree = ref<MerchantCategoryVO[]>([])
const loading = ref(false)

const dialogOpen = ref(false)
const importOpen = ref(false)
const dialogMode = ref<'create-root' | 'create-child' | 'edit'>('create-root')
const dialogParent = ref<MerchantCategoryVO | null>(null)
const dialogCurrent = ref<MerchantCategoryVO | null>(null)

async function load() {
  loading.value = true
  try {
    tree.value = (await merchantCategoryApi.tree()) ?? []
  } finally {
    loading.value = false
  }
}

function openCreateRoot() {
  dialogMode.value = 'create-root'
  dialogParent.value = null
  dialogCurrent.value = null
  dialogOpen.value = true
}

function openCreateChild(row: MerchantCategoryVO) {
  dialogMode.value = 'create-child'
  dialogParent.value = row
  dialogCurrent.value = null
  dialogOpen.value = true
}

function openEdit(row: MerchantCategoryVO) {
  dialogMode.value = 'edit'
  dialogParent.value = null
  dialogCurrent.value = row
  dialogOpen.value = true
}

async function toggleStatus(row: MerchantCategoryVO) {
  const next = row.status === 1 ? 0 : 1
  await merchantCategoryApi.setStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已禁用')
  await load()
}

async function remove(row: MerchantCategoryVO) {
  await ElMessageBox.confirm(`确定删除「${row.name}」？有关联商品或子分类时不能删除。`, '提示', {
    type: 'warning',
  })
  await merchantCategoryApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <span class="page-kicker">MERCHANT CATEGORY</span>
        <h1 class="page-title">店铺分类</h1>
        <p class="page-desc">维护本店小程序展示分类，可从平台分类导入，也可手动添加并设置排序。</p>
      </div>
      <div class="header-actions">
        <el-button v-permission="'merchant:category:import'" @click="importOpen = true">从平台导入</el-button>
        <el-button v-permission="'merchant:category:create'" type="primary" @click="openCreateRoot">新增一级分类</el-button>
      </div>
    </div>

    <el-card>
      <el-table
        v-loading="loading"
        :data="tree"
        row-key="id"
        :tree-props="{ children: 'children' }"
        border
      >
        <el-table-column prop="name" label="名称" min-width="200" />
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag :type="(row as MerchantCategoryVO).sourceCategoryId ? 'success' : 'info'" size="small">
              {{ (row as MerchantCategoryVO).sourceCategoryId ? '平台导入' : '手动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="80">
          <template #default="{ row }">
            <el-tag :type="(row as MerchantCategoryVO).level === 1 ? 'primary' : 'info'" size="small">
              {{ (row as MerchantCategoryVO).level === 1 ? '一级' : '二级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="(row as MerchantCategoryVO).status === 1 ? 'success' : 'danger'" size="small">
              {{ (row as MerchantCategoryVO).status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320">
          <template #default="{ row }">
            <el-button
              v-permission="'merchant:category:create'"
              v-if="(row as MerchantCategoryVO).level === 1"
              link
              type="primary"
              @click="openCreateChild(row as MerchantCategoryVO)"
            >
              新增子级
            </el-button>
            <el-button v-permission="'merchant:category:update'" link type="primary" @click="openEdit(row as MerchantCategoryVO)">编辑</el-button>
            <el-button v-permission="'merchant:category:status'" link type="warning" @click="toggleStatus(row as MerchantCategoryVO)">
              {{ (row as MerchantCategoryVO).status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button v-permission="'merchant:category:delete'" link type="danger" @click="remove(row as MerchantCategoryVO)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <CategoryDialog
      v-model="dialogOpen"
      :mode="dialogMode"
      :parent="dialogParent"
      :current="dialogCurrent"
      @saved="load"
    />
    <CategoryImportDialog v-model="importOpen" @saved="load" />
  </div>
</template>

<style scoped>
.header-actions {
  display: flex;
  gap: 12px;
}
</style>
