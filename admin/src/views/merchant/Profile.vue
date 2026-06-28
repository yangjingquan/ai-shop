<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { profileApi, type MerchantSelfVO } from '@/api/profile'
import { useUserStore } from '@/stores/user'
import ImageUploader from '@/components/upload/ImageUploader.vue'

const formRef = ref<FormInstance>()
const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)

const form = reactive({
  name: '',
  username: '',
  logo: '',
  description: '',
  address: '',
  contactName: '',
  contactPhone: '',
})

const rules: FormRules = {
  contactPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
}

async function load() {
  loading.value = true
  try {
    const data = (await profileApi.get()) as unknown as MerchantSelfVO
    form.name = data.name ?? ''
    userStore.setMerchantName(form.name)
    form.logo = data.logo ?? ''
    form.description = data.description ?? ''
    form.address = data.address ?? ''
    form.contactName = data.contactName ?? ''
    form.contactPhone = data.contactPhone ?? ''
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await profileApi.update({
        logo: form.logo || undefined,
        description: form.description || undefined,
        address: form.address || undefined,
        contactName: form.contactName || undefined,
        contactPhone: form.contactPhone || undefined,
      })
      ElMessage.success('保存成功')
      await load()
    } finally {
      submitting.value = false
    }
  })
}

onMounted(load)
</script>

<template>
  <div class="profile-page" v-loading="loading">
    <div class="page-header">
      <div>
        <span class="page-kicker">STORE PROFILE</span>
        <h1 class="page-title">店铺信息</h1>
        <p class="page-desc">完善店铺展示资料、联系方式与经营地址。</p>
      </div>
    </div>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>基础资料</span>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="店铺名称">
          <el-input v-model="form.name" disabled />
          <div class="hint">店铺名称由运营维护，如需修改请联系运营。</div>
        </el-form-item>
        <el-form-item label="店铺 Logo">
          <ImageUploader v-model="form.logo" scope="merchant" :limit="1" label="上传 Logo" />
        </el-form-item>
        <el-form-item label="店铺简介">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="店铺地址">
          <el-input v-model="form.address" maxlength="255" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" maxlength="32" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" maxlength="20" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            保存
          </el-button>
          <el-button @click="load">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 820px;
}

.hint {
  margin-top: 6px;
  line-height: 1.5;
}
</style>
