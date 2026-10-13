<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chinaProvinces } from '@/data/chinaProvinces'
import { freightApi, type FreightTemplate, type FreightRule } from '@/api/freight'

const rows = ref<FreightTemplate[]>([])
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const editing = ref<number>()
const provinceOptions = Object.keys(chinaProvinces)
const cityOptions = (province: string) => chinaProvinces[province] || []

function emptyRule(): FreightRule {
  return {
    province: '',
    city: '',
    firstWeightGram: 1000,
    firstFee: 0,
    additionalWeightGram: 1000,
    additionalFee: 0,
    freeThresholdAmount: null,
  }
}

const form = reactive({
  name: '',
  enabled: 1,
  rules: [emptyRule()],
})

async function load() {
  loading.value = true
  try {
    rows.value = await freightApi.list()
  } finally {
    loading.value = false
  }
}

function open(row?: FreightTemplate) {
  editing.value = row?.id
  form.name = row?.name || ''
  form.enabled = row?.enabled ?? 1
  form.rules = row?.rules?.map((rule) => ({
    ...emptyRule(),
    ...rule,
    province: rule.province ?? '',
    city: rule.city ?? '',
    freeThresholdAmount: rule.freeThresholdAmount ?? null,
  })) || [emptyRule()]
  visible.value = true
}

function onProvinceChange(rule: FreightRule) {
  rule.city = ''
}

function add() {
  form.rules.push(emptyRule())
}

function regionName(rule: FreightRule) {
  if (!rule.province) return '全国默认'
  return rule.city ? `${rule.province} / ${rule.city}` : `${rule.province}（全省）`
}

function ruleTitle(rule: FreightRule, index: number) {
  if (rule.province || rule.city) return '地区规则'
  const blankCount = form.rules.filter((item) => !item.province && !item.city).length
  return blankCount > 1 && index > 0 ? '请选择地区' : '默认地区规则'
}

async function save() {
  if (!form.name.trim()) return ElMessage.error('请输入模板名称')
  if (form.rules.some((rule) => !rule.province && rule.city)) {
    return ElMessage.error('请选择城市所属省份')
  }
  if (form.rules.filter((rule) => !rule.province && !rule.city).length !== 1) {
    return ElMessage.error('必须保留一条全国默认规则')
  }
  const regions = form.rules.map((rule) => `${rule.province || ''}/${rule.city || ''}`)
  if (new Set(regions).size !== regions.length) return ElMessage.error('地区规则不能重复')

  saving.value = true
  try {
    const data = { name: form.name.trim(), enabled: form.enabled, rules: form.rules }
    if (editing.value) await freightApi.update(editing.value, data)
    else await freightApi.create(data)
    ElMessage.success('已保存')
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row: FreightTemplate) {
  await ElMessageBox.confirm(`删除「${row.name}」后商品将无法使用该模板，确定继续？`, '删除模板', { type: 'warning' })
  await freightApi.remove(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="page freight-page" v-loading="loading">
    <div class="page-header">
      <div>
        <h1 class="page-title">运费模板</h1>
        <p class="page-desc">按收货省市和 SKU 重量计算普通订单运费；满额包邮按活动优惠后金额判断。</p>
      </div>
      <el-button v-permission="'merchant:freight:manage'" type="primary" @click="open()">新建模板</el-button>
    </div>

    <el-table :data="rows" empty-text="暂无运费模板，请先创建一份模板">
      <el-table-column prop="name" label="模板名称" min-width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="地区规则" min-width="380">
        <template #default="{ row }">
          <div v-for="rule in row.rules" :key="rule.province + rule.city" class="rule-summary">
            {{ regionName(rule) }}：首重 {{ rule.firstWeightGram }}g ¥{{ rule.firstFee }}，续重 {{ rule.additionalWeightGram }}g ¥{{ rule.additionalFee }}，{{ rule.freeThresholdAmount != null ? `满 ¥${rule.freeThresholdAmount} 包邮` : '不包邮' }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button v-permission="'merchant:freight:manage'" link type="primary" @click="open(row as FreightTemplate)">编辑</el-button>
          <el-button v-permission="'merchant:freight:manage'" link type="danger" @click="remove(row as FreightTemplate)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="visible"
      :title="editing ? '编辑运费模板' : '新建运费模板'"
      width="min(940px, calc(100vw - 32px))"
      top="5vh"
      class="freight-dialog"
      destroy-on-close
    >
      <el-form label-width="100px" class="template-form">
        <el-form-item label="模板名称">
          <el-input v-model="form.name" maxlength="64" placeholder="例如：华东地区运费" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.enabled">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-divider content-position="left">地区规则</el-divider>
        <p class="rules-intro">先选省份，再选城市；城市留空表示覆盖该省。全国默认规则用于未单独配置的地区。</p>

        <div class="rule-list">
          <section v-for="(rule, index) in form.rules" :key="index" class="rule-card">
            <div class="rule-heading">
              <div class="rule-title">
                <span class="rule-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <strong>{{ ruleTitle(rule, index) }}</strong>
              </div>
              <el-button
                link
                type="danger"
                :disabled="form.rules.length === 1"
                @click="form.rules.splice(index, 1)"
              >删除规则</el-button>
            </div>

            <div class="region-fields">
              <label class="region-field">
                <span>省份</span>
                <el-select
                  v-model="rule.province"
                  filterable
                  placeholder="全国默认"
                  @change="onProvinceChange(rule)"
                >
                  <el-option label="全国（默认规则）" value="" />
                  <el-option v-for="province in provinceOptions" :key="province" :label="province" :value="province" />
                </el-select>
              </label>
              <label class="region-field">
                <span>城市</span>
                <el-select
                  v-model="rule.city"
                  filterable
                  clearable
                  :disabled="!rule.province"
                  :placeholder="rule.province ? '留空表示全省' : '先选择省份'"
                >
                  <el-option v-for="city in cityOptions(rule.province || '')" :key="city" :label="city" :value="city" />
                </el-select>
              </label>
            </div>

            <div class="pricing-grid">
              <label class="price-field">
                <span class="field-label">首重重量 <i>g</i></span>
                <el-input-number v-model="rule.firstWeightGram" :min="1" :step="100" :controls="false" />
                <small>首费覆盖的重量范围</small>
              </label>
              <label class="price-field">
                <span class="field-label">首重运费 <i>¥</i></span>
                <el-input-number v-model="rule.firstFee" :min="0" :precision="2" :step="1" :controls="false" />
                <small>重量不超过首重时收取</small>
              </label>
              <label class="price-field">
                <span class="field-label">续重单位 <i>g</i></span>
                <el-input-number v-model="rule.additionalWeightGram" :min="1" :step="100" :controls="false" />
                <small>超出首重后每档增加</small>
              </label>
              <label class="price-field">
                <span class="field-label">每档续费 <i>¥</i></span>
                <el-input-number v-model="rule.additionalFee" :min="0" :precision="2" :step="1" :controls="false" />
                <small>每增加一档续重加收</small>
              </label>
              <label class="price-field">
                <span class="field-label">包邮门槛 <i>¥</i></span>
                <el-input-number
                  v-model="rule.freeThresholdAmount"
                  :min="0"
                  :precision="2"
                  :step="10"
                  :controls="false"
                  placeholder="不包邮"
                />
                <small>达到金额免运费，留空则不包邮</small>
              </label>
            </div>
          </section>
        </div>

        <el-button plain class="add-rule" @click="add">添加地区规则</el-button>
      </el-form>

      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style>
.freight-page {
  max-width: 1280px;
}

.rule-summary + .rule-summary {
  margin-top: 4px;
}

.freight-dialog .template-form .el-divider {
  margin: 18px 0 12px;
}

.freight-dialog .rules-intro {
  margin: -2px 0 14px 100px;
  color: var(--shop-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.freight-dialog .rule-list {
  display: grid;
  gap: 12px;
}

.freight-dialog .rule-card {
  min-width: 0;
  padding: 14px 16px 13px;
  border: 1px solid var(--shop-border);
  border-radius: 12px;
  background: var(--shop-bg-soft);
}

.freight-dialog .rule-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.freight-dialog .rule-title {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--shop-text);
  font-size: 13px;
}

.freight-dialog .rule-index {
  display: grid;
  width: 23px;
  height: 23px;
  place-items: center;
  border-radius: 7px;
  background: var(--shop-primary-soft);
  color: var(--shop-primary-dark);
  font-size: 11px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.freight-dialog .region-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.freight-dialog .region-field,
.freight-dialog .price-field {
  display: grid;
  min-width: 0;
  gap: 6px;
}

.freight-dialog .region-field > span,
.freight-dialog .field-label {
  color: var(--shop-text);
  font-size: 12px;
  font-weight: 650;
}

.freight-dialog .region-field .el-select,
.freight-dialog .price-field .el-input-number {
  width: 100%;
}

.freight-dialog .pricing-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.freight-dialog .field-label {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 5px;
  white-space: nowrap;
}

.freight-dialog .field-label i {
  color: var(--shop-text-muted);
  font-size: 11px;
  font-style: normal;
  font-weight: 500;
}

.freight-dialog .price-field small {
  min-height: 30px;
  color: var(--shop-text-muted);
  font-size: 11px;
  line-height: 1.4;
}

.freight-dialog .add-rule {
  margin-top: 12px;
}

.freight-dialog .el-dialog {
  display: flex;
  max-height: 90vh;
  flex-direction: column;
  overflow: hidden;
  border-radius: 16px;
}

.freight-dialog .el-dialog__header {
  flex: none;
  margin-right: 0;
  padding: 20px 24px 14px;
  border-bottom: 1px solid var(--shop-border);
}

.freight-dialog .el-dialog__body {
  min-height: 0;
  overflow-y: auto;
  padding: 18px 24px 20px;
}

.freight-dialog .el-dialog__footer {
  flex: none;
  padding: 13px 24px 18px;
  border-top: 1px solid var(--shop-border);
}

@media (max-width: 760px) {
  .freight-dialog .rules-intro {
    margin-left: 0;
  }

  .pricing-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .price-field:last-child {
    grid-column: 1 / -1;
  }

  .freight-dialog .el-dialog__header {
    padding: 18px 18px 12px;
  }

  .freight-dialog .el-dialog__body {
    padding: 16px 18px;
  }

  .freight-dialog .el-dialog__footer {
    padding: 12px 18px 16px;
  }
}

@media (max-width: 480px) {
  .freight-dialog .region-fields,
  .freight-dialog .pricing-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .freight-dialog .price-field:last-child {
    grid-column: auto;
  }
}
</style>
