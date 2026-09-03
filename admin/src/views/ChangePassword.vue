<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { adminAuthApi, merchantAuthApi } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const accountLabel = computed(() => userStore.username || (userStore.role === 'admin' ? '运营管理员' : '商家账号'))

const rules: FormRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度需为 8-32 位', trigger: 'blur' },
  ],
  confirmPassword: [{ required: true, message: '请确认新密码', trigger: 'blur' }],
}

function reset() {
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  formRef.value?.clearValidate()
}

function goBack() {
  router.push(userStore.role === 'admin' ? '/admin' : '/merchant')
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  submitting.value = true
  try {
    const data = {
      currentPassword: form.currentPassword,
      newPassword: form.newPassword,
    }
    if (userStore.role === 'admin') {
      await adminAuthApi.changePassword(data)
    } else {
      await merchantAuthApi.changePassword(data)
    }
    userStore.logout()
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    router.replace('/login')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="password-page">
    <div class="page-header">
      <div>
        <span class="page-kicker">PERSONAL CENTER</span>
        <h1 class="page-title">个人中心</h1>
        <p class="page-desc">管理当前账号信息与登录密码。</p>
      </div>
    </div>

    <el-card class="password-card">
      <template #header>
        <div class="card-header">
          <span>账号信息</span>
        </div>
      </template>
      <div class="account-summary">
        <span>当前账号</span>
        <strong>{{ accountLabel }}</strong>
      </div>

      <el-divider />

      <div class="section-heading">
        <div>
          <h2>修改登录密码</h2>
          <p>密码修改成功后，当前会话将自动退出。</p>
        </div>
      </div>

      <el-form ref="formRef" class="password-form" :model="form" :rules="rules" label-width="108px">
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="form.currentPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password maxlength="32" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password maxlength="32" autocomplete="new-password" />
        </el-form-item>
        <el-form-item class="form-actions">
          <el-button type="primary" :loading="submitting" @click="submit">确认修改</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button text @click="goBack">返回首页</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.password-page {
  width: min(920px, calc(100% - 56px));
  margin: 28px auto 36px;
}

.password-card {
  max-width: 920px;
}

.account-summary {
  display: flex;
  align-items: center;
  gap: 34px;
  min-height: 48px;
  color: var(--shop-text-muted);
  font-size: 14px;
}

.account-summary strong {
  color: var(--shop-text);
  font-size: 15px;
}

.section-heading h2 {
  color: var(--shop-text);
  font-size: 17px;
}

.section-heading p {
  margin-top: 6px;
  color: var(--shop-text-muted);
  font-size: 13px;
}

.password-form {
  max-width: 620px;
  margin-top: 24px;
}

.password-form :deep(.el-input) {
  max-width: 360px;
}

.form-actions {
  margin-top: 8px;
}

.form-actions :deep(.el-form-item__content) {
  gap: 2px;
}
</style>
