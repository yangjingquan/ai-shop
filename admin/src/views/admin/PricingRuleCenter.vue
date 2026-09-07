<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pricingRuleApi } from '@/api/pricing-rule'

const loading = ref(false)
const form = reactive({ matrixJson: '', zeroPayWhitelistJson: '', maxDiscountRate: 1, reason: '' })
const version = ref<number | null>(null)
const applyRule = (value: Awaited<ReturnType<typeof pricingRuleApi.active>>) => {
  version.value = value.version
  Object.assign(form, {
    matrixJson: value.matrixJson,
    zeroPayWhitelistJson: value.zeroPayWhitelistJson,
    maxDiscountRate: value.maxDiscountRate,
    reason: '',
  })
}
const load = async () => { loading.value = true; try { applyRule(await pricingRuleApi.active()) } finally { loading.value = false } }
const publish = async () => {
  try { JSON.parse(form.matrixJson); JSON.parse(form.zeroPayWhitelistJson) } catch (_) { ElMessage.error('互斥矩阵和零价白名单必须为有效 JSON'); return }
  await ElMessageBox.confirm('发布后，旧报价将被要求刷新；确认发布新的平台规则？', '发布规则版本', { type: 'warning' })
  const value = await pricingRuleApi.publish({ ...form }); applyRule(value); ElMessage.success(`规则版本 v${value.version} 已发布`)
}
onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <el-alert title="平台价格与营销规则中心" type="warning" :closable="false" show-icon description="报价版本是订单快照的事实来源。发布会使未提交的旧报价失效；活动配置必须先通过此处的互斥规则。" class="tip"/>
    <el-card><template #header>当前规则版本：v{{ version || '--' }}</template>
      <el-form label-width="140px"><el-form-item label="营销互斥矩阵"><el-input v-model="form.matrixJson" type="textarea" :rows="10"/></el-form-item><el-form-item label="零价订单白名单"><el-input v-model="form.zeroPayWhitelistJson" type="textarea" :rows="3"/></el-form-item><el-form-item label="最大折扣比例"><el-input-number v-model="form.maxDiscountRate" :min="0" :max="1" :step="0.01" :precision="4"/></el-form-item><el-form-item label="发布原因"><el-input v-model="form.reason" maxlength="255"/></el-form-item><el-form-item><el-button type="primary" @click="publish">校验并发布</el-button><el-button @click="load">还原</el-button></el-form-item></el-form>
    </el-card>
  </div>
</template>

<style scoped>.tip{margin-bottom:18px}</style>
