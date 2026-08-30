<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantApi, type MerchantVO } from '@/api/merchant'
import MerchantDialog from './MerchantDialog.vue'

const loading = ref(false)
const list = ref<MerchantVO[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const dialogRow = ref<MerchantVO | null>(null)
const resetVisible = ref(false)
const resetRow = ref<MerchantVO | null>(null)
const resetPassword = ref('')
const resetPasswordConfirm = ref('')
const resetting = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const data = await merchantApi.list({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  fetchList()
}

function onCreate() {
  dialogMode.value = 'create'
  dialogRow.value = null
  dialogVisible.value = true
}

async function onEdit(row: MerchantVO) {
  loading.value = true
  try {
    dialogMode.value = 'edit'
    dialogRow.value = await merchantApi.get(row.id)
    dialogVisible.value = true
  } finally {
    loading.value = false
  }
}

async function onToggleStatus(row: MerchantVO) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 0 ? '冻结' : '启用'
  await ElMessageBox.confirm(`确定要${action}商家「${row.name}」？`, '提示', {
    type: 'warning',
  })
  await merchantApi.setStatus(row.id, next)
  ElMessage.success(`${action}成功`)
  fetchList()
}

function onResetPassword(row: MerchantVO) {
  resetRow.value = row
  resetPassword.value = ''
  resetPasswordConfirm.value = ''
  resetVisible.value = true
}

async function submitResetPassword() {
  if (resetPassword.value.length < 8 || resetPassword.value.length > 32) {
    ElMessage.warning('密码长度需为 8-32 位')
    return
  }
  if (resetPassword.value !== resetPasswordConfirm.value) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (!resetRow.value) return
  resetting.value = true
  try {
    await merchantApi.resetPassword(resetRow.value.id, resetPassword.value)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally {
    resetting.value = false
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="merchant-list">
    <div class="page-header">
      <div>
        <span class="page-kicker">MERCHANTS</span>
        <h1 class="page-title">商家管理</h1>
        <p class="page-desc">统一维护入驻商家、登录账号、联系人与经营状态。</p>
      </div>
      <el-button type="primary" @click="onCreate">新增商家</el-button>
    </div>

    <el-card>
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="按商家名称搜索"
          clearable
          style="width: 260px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
        <el-button type="primary" @click="onSearch">搜索</el-button>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="56" />
        <el-table-column prop="merchantCode" label="商户代码" width="110" show-overflow-tooltip />
        <el-table-column prop="name" label="商家名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="username" label="登录账号" width="96" show-overflow-tooltip />
        <el-table-column prop="contactName" label="联系人" width="80" show-overflow-tooltip />
        <el-table-column prop="contactPhone" label="联系电话" width="108" />
        <el-table-column label="状态" width="72">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '冻结' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="132" />
        <el-table-column label="操作" width="184">
          <template #default="{ row }">
            <el-button link type="primary" @click="onEdit(row as MerchantVO)">编辑</el-button>
            <el-button link type="warning" @click="onResetPassword(row as MerchantVO)">修改登录密码</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="onToggleStatus(row as MerchantVO)"
            >
              {{ row.status === 1 ? '冻结' : '启用' }}
            </el-button>
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

    <MerchantDialog
      v-model="dialogVisible"
      :mode="dialogMode"
      :row="dialogRow"
      @success="fetchList"
    />

    <el-dialog v-model="resetVisible" title="修改商家登录密码" width="460px">
      <el-form label-width="110px">
        <el-form-item label="商家">
          <span>{{ resetRow?.name || '-' }}</span>
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="resetPassword" type="password" show-password maxlength="32" />
        </el-form-item>
        <el-form-item label="确认密码" required>
          <el-input v-model="resetPasswordConfirm" type="password" show-password maxlength="32" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="submitResetPassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
</style>
