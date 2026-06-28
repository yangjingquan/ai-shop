<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  productApi,
  type ProductDetailVO,
  type ProductSavePayload,
  type ProductSkuInput,
} from '@/api/product'
import { categoryApi, type CategoryVO } from '@/api/category'
import ImageUploader from '@/components/upload/ImageUploader.vue'

interface SpecForm {
  name: string
  values: string[]
}

interface SkuRow {
  specValueIndexes: number[]
  specText: string
  price: number
  stock: number
  skuCode: string
  image: string
}

const route = useRoute()
const router = useRouter()

const editId = computed(() => {
  const raw = route.params.id
  if (!raw) return undefined
  const n = Number(raw)
  return Number.isFinite(n) && n > 0 ? n : undefined
})
const isEdit = computed(() => editId.value !== undefined)

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitting = ref(false)

const form = reactive({
  name: '',
  subtitle: '',
  categoryId: undefined as number | undefined,
  mainImage: '',
  images: [] as string[],
  description: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

const specs = ref<SpecForm[]>([])
const skuRows = ref<SkuRow[]>([])

// 二级分类树（CascadeSelect 需要）
const catTree = ref<CategoryVO[]>([])
const catOptions = computed(() =>
  catTree.value
    .filter((t) => t.status !== 0)
    .map((t) => ({
      value: t.id,
      label: t.name,
      children: (t.children ?? [])
        .filter((c) => c.status !== 0)
        .map((c) => ({ value: c.id, label: c.name })),
    })),
)

// 编辑回填用：detail 加载完后才生成 skuRows
const initialDetail = ref<ProductDetailVO | null>(null)
let suppressSkuRebuild = false

async function loadCategories() {
  catTree.value = (await categoryApi.publicTree()) ?? []
}

function emptySpec(): SpecForm {
  return { name: '', values: [''] }
}

function addSpec() {
  if (specs.value.length >= 3) {
    ElMessage.warning('最多 3 个规格')
    return
  }
  specs.value.push(emptySpec())
}

function removeSpec(idx: number) {
  specs.value.splice(idx, 1)
}

function addSpecValue(spec: SpecForm) {
  spec.values.push('')
}

function removeSpecValue(spec: SpecForm, idx: number) {
  if (spec.values.length <= 1) return
  spec.values.splice(idx, 1)
}

// 笛卡尔积重新生成 SKU 行
function rebuildSkuRows() {
  if (suppressSkuRebuild) return
  // 校验：所有 spec 必须有 name 且 values 至少有一个非空
  const valid = specs.value.every(
    (s) => s.name.trim() && s.values.some((v) => v.trim()),
  )
  if (!valid || specs.value.length === 0) {
    skuRows.value = []
    return
  }

  const valueArrays = specs.value.map((s) =>
    s.values.map((v, i) => ({ value: v.trim(), idx: i })).filter((x) => x.value),
  )

  // 笛卡尔积
  let combos: { value: string; idx: number }[][] = [[]]
  for (const vals of valueArrays) {
    const next: { value: string; idx: number }[][] = []
    for (const c of combos) {
      for (const v of vals) {
        next.push([...c, v])
      }
    }
    combos = next
  }

  // 保留旧值（按 specText 匹配）
  const prev = new Map<string, SkuRow>()
  for (const r of skuRows.value) prev.set(r.specText, r)

  skuRows.value = combos.map((c) => {
    const specText = c.map((x) => x.value).join(' / ')
    const old = prev.get(specText)
    return {
      specValueIndexes: c.map((x) => x.idx),
      specText,
      price: old?.price ?? 0,
      stock: old?.stock ?? 0,
      skuCode: old?.skuCode ?? '',
      image: old?.image ?? '',
    }
  })
}

watch(specs, () => rebuildSkuRows(), { deep: true })

// 批量设置
const batchPriceVisible = ref(false)
const batchStockVisible = ref(false)
const batchPrice = ref(0)
const batchStock = ref(0)

function applyBatchPrice() {
  for (const r of skuRows.value) r.price = batchPrice.value
  batchPriceVisible.value = false
  ElMessage.success('已批量设置价格')
}
function applyBatchStock() {
  for (const r of skuRows.value) r.stock = batchStock.value
  batchStockVisible.value = false
  ElMessage.success('已批量设置库存')
}

async function loadDetail(id: number) {
  loading.value = true
  try {
    const data = await productApi.get(id)
    initialDetail.value = data
    // 基础信息
    form.name = data.name
    form.subtitle = data.subtitle ?? ''
    form.categoryId = data.categoryId
    form.mainImage = data.mainImage ?? ''
    form.images = (data.images ?? []).filter(Boolean)
    form.description = data.description ?? ''

    // 暂停 watch 触发的 rebuild
    suppressSkuRebuild = true
    specs.value = data.specs.map((s) => ({
      name: s.name,
      values: s.values.map((v) => v.value),
    }))
    // 用真实 spec_value_id 计算 indexes
    const valueIdToIdx = new Map<string, number>() // key: `${specIdx}-${valueId}`
    data.specs.forEach((s, si) => {
      s.values.forEach((v, vi) => {
        valueIdToIdx.set(`${si}-${v.id}`, vi)
      })
    })
    skuRows.value = data.skus.map((sku) => {
      const indexes = sku.specValueIds.map((vid, si) => {
        const key = `${si}-${vid}`
        return valueIdToIdx.get(key) ?? 0
      })
      const specText =
        sku.specText ??
        indexes
          .map((idx, si) => specs.value[si]?.values[idx] ?? '')
          .filter(Boolean)
          .join(' / ')
      return {
        specValueIndexes: indexes,
        specText,
        price: Number(sku.price ?? 0),
        stock: sku.stock ?? 0,
        skuCode: sku.skuCode ?? '',
        image: sku.image ?? '',
      }
    })
    suppressSkuRebuild = false
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (specs.value.length === 0) {
      ElMessage.error('请至少添加一个规格')
      return
    }
    // 同步规格名重复校验
    const names = specs.value.map((s) => s.name.trim())
    if (names.some((n) => !n)) {
      ElMessage.error('规格名不能为空')
      return
    }
    if (new Set(names).size !== names.length) {
      ElMessage.error('规格名不能重复')
      return
    }
    for (const s of specs.value) {
      const vals = s.values.map((v) => v.trim()).filter(Boolean)
      if (vals.length === 0) {
        ElMessage.error(`规格「${s.name}」至少需要一个规格值`)
        return
      }
    }
    if (skuRows.value.length === 0) {
      ElMessage.error('请至少有一个 SKU')
      return
    }
    if (skuRows.value.length > 100) {
      ElMessage.error('SKU 数量不能超过 100')
      return
    }
    for (const r of skuRows.value) {
      if (r.price <= 0) {
        ElMessage.error(`SKU「${r.specText}」价格必须大于 0`)
        return
      }
      if (r.stock < 0) {
        ElMessage.error(`SKU「${r.specText}」库存不能小于 0`)
        return
      }
    }

    const payload: ProductSavePayload = {
      name: form.name.trim(),
      subtitle: form.subtitle?.trim() || undefined,
      categoryId: form.categoryId!,
      mainImage: form.mainImage?.trim() || undefined,
      images: form.images.length ? [...form.images] : undefined,
      description: form.description?.trim() || undefined,
      specs: specs.value.map((s) => ({
        name: s.name.trim(),
        values: s.values.map((v) => v.trim()).filter(Boolean),
      })),
      skus: skuRows.value.map<ProductSkuInput>((r) => ({
        specValueIndexes: r.specValueIndexes,
        price: Number(r.price),
        stock: Number(r.stock),
        skuCode: r.skuCode || undefined,
        image: r.image || undefined,
      })),
    }

    submitting.value = true
    try {
      if (isEdit.value && editId.value !== undefined) {
        await productApi.update(editId.value, payload)
        ElMessage.success('更新成功')
      } else {
        await productApi.create(payload)
        ElMessage.success('创建成功')
      }
      router.push('/merchant/products')
    } finally {
      submitting.value = false
    }
  })
}

function handleCancel() {
  router.push('/merchant/products')
}

onMounted(async () => {
  await loadCategories()
  if (isEdit.value && editId.value !== undefined) {
    await loadDetail(editId.value)
  } else {
    // 新建默认一个空规格
    specs.value = [emptySpec()]
  }
})
</script>

<template>
  <div class="product-edit" v-loading="loading">
    <div class="page-header">
      <div>
        <span class="page-kicker">PRODUCT EDITOR</span>
        <h1 class="page-title">{{ isEdit ? '编辑商品' : '新增商品' }}</h1>
        <p class="page-desc">维护商品基础资料、规格组合、SKU 价格与库存。</p>
      </div>
    </div>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>商品基础信息</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-cascader
            v-model="form.categoryId"
            :options="catOptions"
            :props="{ checkStrictly: false, emitPath: false }"
            placeholder="选择二级分类"
            clearable
            style="width: 320px"
          />
        </el-form-item>
        <el-form-item label="主图">
          <ImageUploader v-model="form.mainImage" scope="merchant" :limit="1" label="上传主图" />
        </el-form-item>
        <el-form-item label="详情图">
          <ImageUploader v-model="form.images" scope="merchant" multiple :limit="9" label="批量上传" />
        </el-form-item>
        <el-form-item label="商品详情">
          <el-input v-model="form.description" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">规格定义</el-divider>

      <div class="spec-list">
        <div v-for="(spec, idx) in specs" :key="idx" class="spec-row">
          <div class="spec-head">
            <el-input
              v-model="spec.name"
              placeholder="规格名 (如: 颜色)"
              style="width: 200px"
              maxlength="32"
            />
            <el-button link type="danger" @click="removeSpec(idx)">删除规格</el-button>
          </div>
          <div class="spec-values">
            <div v-for="(_, vIdx) in spec.values" :key="vIdx" class="value-cell">
              <el-input
                v-model="spec.values[vIdx]"
                placeholder="规格值"
                style="width: 140px"
                maxlength="32"
              />
              <el-button link type="danger" @click="removeSpecValue(spec, vIdx)">×</el-button>
            </div>
            <el-button size="small" @click="addSpecValue(spec)">+ 添加值</el-button>
          </div>
        </div>
        <el-button type="primary" plain @click="addSpec">+ 添加规格</el-button>
      </div>

      <el-divider content-position="left">SKU 列表（共 {{ skuRows.length }} 条）</el-divider>

      <div class="sku-toolbar">
        <el-button :disabled="!skuRows.length" @click="batchPriceVisible = true">
          批量设价
        </el-button>
        <el-button :disabled="!skuRows.length" @click="batchStockVisible = true">
          批量设库存
        </el-button>
        <span v-if="skuRows.length === 0" class="hint">先填好规格名和值，下方会自动生成 SKU</span>
      </div>

      <el-table :data="skuRows" border size="small">
        <el-table-column prop="specText" label="规格" min-width="180" />
        <el-table-column label="价格" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="(row as SkuRow).price"
              :min="0"
              :precision="2"
              :step="1"
              :controls="false"
              style="width: 130px"
            />
          </template>
        </el-table-column>
        <el-table-column label="库存" width="140">
          <template #default="{ row }">
            <el-input-number
              v-model="(row as SkuRow).stock"
              :min="0"
              :step="1"
              :controls="false"
              style="width: 110px"
            />
          </template>
        </el-table-column>
        <el-table-column label="SKU 编码" width="180">
          <template #default="{ row }">
            <el-input v-model="(row as SkuRow).skuCode" maxlength="64" />
          </template>
        </el-table-column>
        <el-table-column label="SKU 图" min-width="150">
          <template #default="{ row }">
            <ImageUploader v-model="(row as SkuRow).image" scope="merchant" :limit="1" label="上传" />
          </template>
        </el-table-column>
      </el-table>

      <div class="actions">
        <el-button :loading="submitting" type="primary" @click="handleSubmit">
          保存
        </el-button>
        <el-button @click="handleCancel">取消</el-button>
      </div>
    </el-card>

    <el-dialog v-model="batchPriceVisible" title="批量设价" width="320px">
      <el-input-number v-model="batchPrice" :min="0" :precision="2" :step="1" />
      <template #footer>
        <el-button @click="batchPriceVisible = false">取消</el-button>
        <el-button type="primary" @click="applyBatchPrice">应用到全部</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchStockVisible" title="批量设库存" width="320px">
      <el-input-number v-model="batchStock" :min="0" :step="1" />
      <template #footer>
        <el-button @click="batchStockVisible = false">取消</el-button>
        <el-button type="primary" @click="applyBatchStock">应用到全部</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.product-edit {
  max-width: 1120px;
}

.spec-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 16px;
}

.spec-row {
  border: 1px dashed #e2c7a8;
  border-radius: 18px;
  padding: 16px;
  background: linear-gradient(180deg, #fffaf2, #fffdf8);
}

.spec-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.spec-values {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.value-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.actions {
  position: sticky;
  bottom: 0;
  z-index: 2;
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding: 16px 0 4px;
  background: linear-gradient(180deg, rgba(246, 242, 234, 0), var(--shop-bg) 35%);
}
</style>
