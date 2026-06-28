<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { categoryApi, type CategoryVO } from '@/api/category'
import ImageUploader from '@/components/upload/ImageUploader.vue'

interface Props {
  modelValue: boolean
  // 'create-root' | 'create-child' | 'edit'
  mode: 'create-root' | 'create-child' | 'edit'
  parent?: CategoryVO | null
  current?: CategoryVO | null
}

const props = withDefaults(defineProps<Props>(), {
  parent: null,
  current: null,
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved'): void
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  name: '',
  parentId: 0,
  icon: '',
  sort: 0,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
}

const title = () => {
  if (props.mode === 'create-root') return '新增一级分类'
  if (props.mode === 'create-child') return `在「${props.parent?.name ?? ''}」下新增子级`
  return '编辑分类'
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    if (props.mode === 'edit' && props.current) {
      form.name = props.current.name
      form.parentId = props.current.parentId
      form.icon = props.current.icon ?? ''
      form.sort = props.current.sort
    } else if (props.mode === 'create-root') {
      form.name = ''
      form.parentId = 0
      form.icon = ''
      form.sort = 0
    } else {
      form.name = ''
      form.parentId = props.parent?.id ?? 0
      form.icon = ''
      form.sort = 0
    }
  },
)

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) return
  submitting.value = true
  try {
    if (props.mode === 'edit' && props.current) {
      await categoryApi.update(props.current.id, { ...form })
      ElMessage.success('已更新')
    } else {
      await categoryApi.create({ ...form })
      ElMessage.success('已创建')
    }
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
    :title="title()"
    width="480px"
    @update:model-value="(v) => emit('update:modelValue', v)"
    @close="close"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" maxlength="32" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="0" />
      </el-form-item>
      <el-form-item label="分类图标">
        <ImageUploader v-model="form.icon" scope="admin" :limit="1" label="上传图标" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>
