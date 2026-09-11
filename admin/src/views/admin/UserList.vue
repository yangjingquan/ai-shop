<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { adminUserApi, type AdminUserRow } from '@/api/user'
import { merchantApi, type MerchantVO } from '@/api/merchant'

const loading = ref(false)
const list = ref<AdminUserRow[]>([])
const merchants = ref<MerchantVO[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  merchantId: undefined as number | undefined,
  keyword: '',
})

async function fetchList(resetPage = false) {
  if (resetPage) query.page = 1
  loading.value = true
  try {
    const data = await adminUserApi.page({
      page: query.page,
      size: query.size,
      merchantId: query.merchantId,
      keyword: query.keyword || undefined,
    })
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadMerchants() {
  const data = await merchantApi.list({ page: 1, size: 1000 })
  merchants.value = data.list || []
}

function reset() {
  query.keyword = ''
  query.merchantId = undefined
  fetchList(true)
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

onMounted(async () => {
  await Promise.all([loadMerchants(), fetchList()])
})
</script>

<template>
  <div class="user-list-page" v-loading="loading">
    <div class="page-header">
      <div>
        <span class="page-kicker">USERS</span>
        <h1 class="page-title">用户管理</h1>
        <p class="page-desc">按商户查看小程序用户，并搜索昵称或手机号。</p>
      </div>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-select
          v-model="query.merchantId"
          clearable
          filterable
          placeholder="全部商户"
          style="width: 220px"
          @change="fetchList(true)"
        >
          <el-option
            v-for="merchant in merchants"
            :key="merchant.id"
            :label="`${merchant.name} · ${merchant.id}`"
            :value="merchant.id"
          />
        </el-select>
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="搜索昵称或手机号"
          style="width: 260px"
          @keyup.enter="fetchList(true)"
        />
        <el-button type="primary" @click="fetchList(true)">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="list" empty-text="暂无用户数据">
        <el-table-column prop="id" label="用户 ID" width="100" />
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="38" :src="row.avatar">{{ (row.nickname || '用').slice(0, 1) }}</el-avatar>
              <div>
                <strong>{{ row.nickname || `用户 #${row.id}` }}</strong>
                <span>{{ row.phone || '未绑定手机号' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商户" min-width="190">
          <template #default="{ row }">
            {{ row.merchantName || (row.merchantId ? `商户 #${row.merchantId}` : '历史数据未归属') }}
          </template>
        </el-table-column>
        <el-table-column label="注册时间" min-width="165">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="最后登录" min-width="165">
          <template #default="{ row }">{{ formatTime(row.lastLoginAt) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next"
          @current-change="() => fetchList()"
          @size-change="() => fetchList(true)"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.user-list-page {
  width: min(1280px, calc(100% - 56px));
  margin: 28px auto 36px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell strong,
.user-cell span {
  display: block;
}

.user-cell strong {
  color: var(--shop-text);
}

.user-cell span {
  margin-top: 3px;
  color: var(--shop-text-muted);
  font-size: 12px;
}

@media (max-width: 900px) {
  .user-list-page {
    width: calc(100% - 32px);
    margin: 16px auto 24px;
  }

  .toolbar {
    flex-wrap: wrap;
  }

  .toolbar .el-input,
  .toolbar .el-select {
    width: 100% !important;
  }
}
</style>
