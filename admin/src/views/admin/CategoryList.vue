<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi, type CategoryVO } from '@/api/category'
import CategoryDialog from './CategoryDialog.vue'

const tree = ref<CategoryVO[]>([])
const loading = ref(false)

const dialogOpen = ref(false)
const dialogMode = ref<'create-root' | 'create-child' | 'edit'>('create-root')
const dialogParent = ref<CategoryVO | null>(null)
const dialogCurrent = ref<CategoryVO | null>(null)

async function load() {
  loading.value = true
  try {
    tree.value = (await categoryApi.tree()) ?? []
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

function openCreateChild(row: CategoryVO) {
  dialogMode.value = 'create-child'
  dialogParent.value = row
  dialogCurrent.value = null
  dialogOpen.value = true
}

function openEdit(row: CategoryVO) {
  dialogMode.value = 'edit'
  dialogParent.value = null
  dialogCurrent.value = row
  dialogOpen.value = true
}

async function toggleStatus(row: CategoryVO) {
  const next = row.status === 1 ? 0 : 1
  await categoryApi.setStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已禁用')
  await load()
}

async function remove(row: CategoryVO) {
  await ElMessageBox.confirm(`确定删除「${row.name}」？`, '提示', {
    type: 'warning',
  })
  await categoryApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <span class="page-kicker">CATEGORY</span>
        <h1 class="page-title">平台分类</h1>
        <p class="page-desc">维护商城前台类目层级、排序与上下架状态。</p>
      </div>
      <el-button type="primary" @click="openCreateRoot">新增一级分类</el-button>
    </div>

    <el-card>
      <el-table
        v-loading="loading"
        :data="tree"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        border
      >
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column prop="level" label="级别" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.level === 1 ? 'primary' : 'info'" size="small">
            {{ scope.row.level === 1 ? '一级' : '二级' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320">
        <template #default="{ row }">
          <el-button
            v-if="(row as CategoryVO).level === 1"
            link
            type="primary"
            @click="openCreateChild(row as CategoryVO)"
          >
            新增子级
          </el-button>
          <el-button link type="primary" @click="openEdit(row as CategoryVO)">编辑</el-button>
          <el-button link type="warning" @click="toggleStatus(row as CategoryVO)">
            {{ (row as CategoryVO).status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button link type="danger" @click="remove(row as CategoryVO)">删除</el-button>
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
  </div>
</template>

<style scoped>
</style>
