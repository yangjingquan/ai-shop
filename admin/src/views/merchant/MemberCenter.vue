<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { memberApi, type MemberProfileVO } from '@/api/member'
import { pointsApi, type MemberLevel } from '@/api/marketing'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const level = ref<number | undefined>()
const levels = ref<MemberLevel[]>([])
const members = ref<MemberProfileVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const memberCount = computed(() => total.value)
const pointsMemberCount = computed(() => members.value.filter((item) => item.pointsBalance > 0).length)

async function load(resetPage = false) {
  if (resetPage) page.value = 1
  loading.value = true
  try {
    const result = await memberApi.page({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      level: level.value,
    })
    members.value = result.list || []
    total.value = result.total || 0
  } finally {
    loading.value = false
  }
}

async function loadLevels() {
  levels.value = await pointsApi.levels()
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function goMemberSettings() {
  router.push('/merchant/points')
}

onMounted(async () => {
  await Promise.all([loadLevels(), load()])
})
</script>

<template>
  <div class="member-center-page" v-loading="loading">
    <div class="page-header">
      <div>
        <span class="page-kicker">MEMBER CENTER</span>
        <h1 class="page-title">会员中心</h1>
        <p class="page-desc">查看店铺会员的基本资料、会员等级和积分情况。</p>
      </div>
      <el-button type="primary" @click="goMemberSettings">会员规则配置</el-button>
    </div>

    <div class="overview-grid">
      <el-card class="metric-card" shadow="never">
        <span>会员总数</span>
        <strong>{{ memberCount }}</strong>
        <p>已加入店铺会员体系</p>
      </el-card>
      <el-card class="metric-card" shadow="never">
        <span>本页有积分会员</span>
        <strong>{{ pointsMemberCount }}</strong>
        <p>积分余额大于 0 的会员</p>
      </el-card>
      <el-card class="member-level-card" shadow="never">
        <span>已配置等级</span>
        <strong>{{ levels.length }}</strong>
        <p>{{ levels.length ? levels.map((item) => item.name).join(' · ') : '暂未配置会员等级' }}</p>
      </el-card>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <span>会员列表</span>
            <small>会员在小程序注册后自动进入店铺会员体系</small>
          </div>
          <el-button @click="() => load()">刷新</el-button>
        </div>
      </template>

      <div class="toolbar member-toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索昵称或手机号" @keyup.enter="load(true)" />
        <el-select v-model="level" clearable placeholder="全部等级" @change="load(true)">
          <el-option v-for="item in levels" :key="item.level" :label="item.name" :value="item.level ?? 1" />
        </el-select>
        <el-button type="primary" @click="load(true)">查询</el-button>
        <el-button @click="keyword = ''; level = undefined; load(true)">重置</el-button>
      </div>

      <el-table :data="members" empty-text="暂无会员数据">
        <el-table-column label="会员" min-width="220">
          <template #default="{ row }">
            <div class="member-cell">
              <el-avatar :size="38" :src="row.avatar">{{ (row.nickname || `会`).slice(0, 1) }}</el-avatar>
              <div>
                <strong>{{ row.nickname || `会员 #${row.userId}` }}</strong>
                <span>会员 ID：{{ row.userId }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="会员等级" min-width="130">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.levelName || `Lv.${row.level || 1}` }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pointsBalance" label="可用积分" min-width="110" align="right" />
        <el-table-column prop="totalPoints" label="累计积分" min-width="110" align="right" />
        <el-table-column label="加入时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.joinedAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="() => load(true)"
          @current-change="() => load()"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.member-center-page {
  width: min(1280px, calc(100% - 56px));
  margin: 28px auto 36px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 20px;
}

.metric-card,
.member-level-card {
  min-height: 148px;
}

.metric-card span,
.member-level-card span {
  color: var(--shop-text-muted);
  font-size: 13px;
  font-weight: 700;
}

.metric-card strong,
.member-level-card strong {
  display: block;
  margin: 12px 0 8px;
  color: var(--shop-text);
  font-size: 32px;
  line-height: 1;
}

.metric-card p,
.member-level-card p {
  overflow: hidden;
  margin: 0;
  color: var(--shop-text-muted);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-level-card {
  border-color: #edc993 !important;
  background: linear-gradient(135deg, #fff7e9, #fffdf8) !important;
}

.card-header small {
  display: block;
  margin-top: 4px;
  color: var(--shop-text-muted);
  font-size: 12px;
  font-weight: 400;
}

.member-toolbar .el-input {
  width: 250px;
}

.member-toolbar .el-select {
  width: 150px;
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-cell strong,
.member-cell span {
  display: block;
}

.member-cell strong {
  color: var(--shop-text);
}

.member-cell span {
  margin-top: 3px;
  color: var(--shop-text-muted);
  font-size: 12px;
}

@media (max-width: 900px) {
  .member-center-page {
    width: calc(100% - 32px);
    margin: 16px auto 24px;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .member-toolbar .el-input,
  .member-toolbar .el-select {
    width: 100%;
  }
}
</style>
