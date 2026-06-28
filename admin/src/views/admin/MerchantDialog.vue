<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { merchantApi, type MerchantVO } from '@/api/merchant'
import ImageUploader from '@/components/upload/ImageUploader.vue'

const props = defineProps<{
  modelValue: boolean
  mode: 'create' | 'edit'
  row?: MerchantVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'success'): void
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  name: '',
  username: '',
  password: '',
  contactName: '',
  contactPhone: '',
  description: '',
  address: '',
  logo: '',
  wxAppId: '',
  wxSecret: '',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入商家名称', trigger: 'blur' }],
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '4-20 位字母/数字/下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 32, message: '6-32 位', trigger: 'blur' },
  ],
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    if (props.mode === 'edit' && props.row) {
      form.name = props.row.name ?? ''
      form.contactName = props.row.contactName ?? ''
      form.contactPhone = props.row.contactPhone ?? ''
      form.description = props.row.description ?? ''
      form.address = props.row.address ?? ''
      form.logo = props.row.logo ?? ''
      form.wxAppId = props.row.wxAppId ?? ''
      form.wxSecret = ''
      form.username = ''
      form.password = ''
    } else {
      form.name = ''
      form.username = ''
      form.password = ''
      form.contactName = ''
      form.contactPhone = ''
      form.description = ''
      form.address = ''
      form.logo = ''
      form.wxAppId = ''
      form.wxSecret = ''
    }
  },
)

function handleClose() {
  emit('update:modelValue', false)
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (props.mode === 'create') {
        await merchantApi.create({
          name: form.name,
          username: form.username,
          password: form.password,
          contactName: form.contactName || undefined,
          contactPhone: form.contactPhone || undefined,
          description: form.description || undefined,
          address: form.address || undefined,
          logo: form.logo || undefined,
          wxAppId: form.wxAppId || undefined,
          wxSecret: form.wxSecret || undefined,
        })
        ElMessage.success('创建成功')
      } else if (props.row) {
        await merchantApi.update(props.row.id, {
          name: form.name,
          contactName: form.contactName || undefined,
          contactPhone: form.contactPhone || undefined,
          description: form.description || undefined,
          address: form.address || undefined,
          logo: form.logo || undefined,
          wxAppId: form.wxAppId,
          wxSecret: form.wxSecret || undefined,
        })
        ElMessage.success('保存成功')
      }
      emit('success')
      emit('update:modelValue', false)
    } finally {
      submitting.value = false
    }
  })
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="mode === 'create' ? '新增商家' : '编辑商家'"
    width="680px"
    @update:model-value="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="130px">
      <el-form-item v-if="mode === 'edit'" label="商户代码">
        <el-input :model-value="row?.merchantCode || '-'" disabled />
      </el-form-item>
      <el-form-item label="商家名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <template v-if="mode === 'create'">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="form.password" show-password />
        </el-form-item>
      </template>
      <el-form-item label="联系人">
        <el-input v-model="form.contactName" />
      </el-form-item>
      <el-form-item label="联系电话">
        <el-input v-model="form.contactPhone" />
      </el-form-item>
      <el-form-item label="店铺简介">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="店铺地址">
        <el-input v-model="form.address" />
      </el-form-item>
      <el-form-item label="小程序 AppID">
        <el-input v-model="form.wxAppId" placeholder="请输入 WX_APPID" maxlength="64" />
      </el-form-item>
      <el-form-item label="小程序 AppSecret">
        <el-input
          v-model="form.wxSecret"
          show-password
          maxlength="128"
          :placeholder="mode === 'edit' ? '留空则不修改 WX_SECRET' : '请输入 WX_SECRET'"
        />
        <div v-if="mode === 'edit' && row?.wxSecretConfigured" class="hint">当前商户已配置 AppSecret，留空不会覆盖。</div>
      </el-form-item>
      <el-form-item label="店铺 Logo">
        <ImageUploader v-model="form.logo" scope="admin" :limit="1" label="上传 Logo" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.hint {
  margin-top: 6px;
  color: var(--shop-text-muted);
  font-size: 12px;
}
</style>
