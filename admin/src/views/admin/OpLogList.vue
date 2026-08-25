<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { opLogApi, type OpLogRow } from '@/api/op-log'

const loading = ref(false)
const list = ref<OpLogRow[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 20, operatorType: undefined as number | undefined, action: '', targetType: '' })
const detailVisible = ref(false)
const detail = ref<OpLogRow | null>(null)
const operatorText: Record<number, string> = { 1: '运营管理员', 2: '商家', 3: '用户' }

async function fetchList() {
  loading.value = true
  try {
    const data = await opLogApi.page({ page: query.page, size: query.size, operatorType: query.operatorType, action: query.action || undefined, targetType: query.targetType || undefined })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function search() { query.page = 1; fetchList() }
function showPayload(row: OpLogRow) { detail.value = row; detailVisible.value = true }
onMounted(fetchList)
</script>

<template>
  <div class="page">
    <div class="page-header"><div><span class="page-kicker">AUDIT TRAIL</span><h1 class="page-title">操作日志</h1><p class="page-desc">查询后台敏感操作、目标对象和脱敏后的入参摘要。</p></div></div>
    <el-card>
      <div class="toolbar"><el-select v-model="query.operatorType" placeholder="全部操作人" clearable style="width: 150px" @change="search"><el-option v-for="(label, value) in operatorText" :key="value" :label="label" :value="Number(value)" /></el-select><el-input v-model="query.action" placeholder="动作，如 MERCHANT_RESET_PWD" clearable style="width: 240px" @keyup.enter="search" /><el-input v-model="query.targetType" placeholder="目标类型" clearable style="width: 150px" @keyup.enter="search" /><el-button type="primary" @click="search">查询</el-button></div>
      <el-table v-loading="loading" :data="list" stripe><el-table-column prop="createdAt" label="时间" width="180" /><el-table-column label="操作人" width="120"><template #default="{ row }">{{ operatorText[row.operatorType] || row.operatorType }} #{{ row.operatorId }}</template></el-table-column><el-table-column prop="action" label="动作" width="220" /><el-table-column prop="targetType" label="目标类型" width="120" /><el-table-column prop="targetId" label="目标 ID" width="110" /><el-table-column prop="ip" label="IP" width="150" /><el-table-column label="入参" min-width="90"><template #default="{ row }"><el-button link type="primary" @click="showPayload(row as OpLogRow)">查看</el-button></template></el-table-column></el-table>
      <div class="pagination"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[20, 50, 100]" :total="total" background layout="total, sizes, prev, pager, next" @current-change="fetchList" @size-change="fetchList" /></div>
    </el-card>
    <el-dialog v-model="detailVisible" title="操作入参" width="760px"><pre class="payload">{{ detail?.payload || '{}' }}</pre></el-dialog>
  </div>
</template>

<style scoped>.payload { max-height: 420px; margin: 0; padding: 14px; overflow: auto; white-space: pre-wrap; word-break: break-all; border-radius: 12px; background: #fff8ed; color: var(--shop-text); }</style>
