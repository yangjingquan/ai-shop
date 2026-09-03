<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantRbacApi, type MerchantPermission, type MerchantRole, type MerchantUser } from '@/api/rbac'

const loading = ref(false)
const activeTab = ref<'users' | 'roles'>('users')
const users = ref<MerchantUser[]>([])
const roles = ref<MerchantRole[]>([])
const permissions = ref<MerchantPermission[]>([])
const roleDialogVisible = ref(false)
const userDialogVisible = ref(false)
const roleSubmitting = ref(false)
const userSubmitting = ref(false)
const editingRole = ref<MerchantRole | null>(null)
const roleForm = reactive({ code: '', name: '', description: '', permissionCodes: [] as string[] })
const userForm = reactive({ username: '', password: '', roleIds: [] as number[] })

const permissionGroups = computed(() => {
  const groups = new Map<string, MerchantPermission[]>()
  for (const permission of permissions.value) {
    const list = groups.get(permission.module) || []
    list.push(permission)
    groups.set(permission.module, list)
  }
  return [...groups.entries()].map(([module, items]) => ({ module, items }))
})

async function load() {
  loading.value = true
  try {
    const [userList, roleList, permissionList] = await Promise.all([
      merchantRbacApi.users(),
      merchantRbacApi.roles(),
      merchantRbacApi.permissions(),
    ])
    users.value = userList || []
    roles.value = roleList || []
    permissions.value = permissionList || []
  } finally {
    loading.value = false
  }
}

function openCreateRole() {
  editingRole.value = null
  Object.assign(roleForm, { code: '', name: '', description: '', permissionCodes: [] })
  roleDialogVisible.value = true
}

function openEditRole(role: MerchantRole) {
  editingRole.value = role
  Object.assign(roleForm, {
    code: role.code,
    name: role.name,
    description: role.description || '',
    permissionCodes: [...role.permissionCodes],
  })
  roleDialogVisible.value = true
}

async function saveRole() {
  if (!roleForm.code.trim() || !roleForm.name.trim()) {
    ElMessage.warning('请填写角色编码和名称')
    return
  }
  roleSubmitting.value = true
  try {
    const payload = { ...roleForm, permissionCodes: [...roleForm.permissionCodes] }
    if (editingRole.value) await merchantRbacApi.updateRole(editingRole.value.id, payload)
    else await merchantRbacApi.createRole(payload)
    roleDialogVisible.value = false
    ElMessage.success('角色保存成功')
    await load()
  } finally {
    roleSubmitting.value = false
  }
}

async function removeRole(role: MerchantRole) {
  await ElMessageBox.confirm(`确定删除角色“${role.name}”吗？`, '删除角色', { type: 'warning' })
  await merchantRbacApi.deleteRole(role.id)
  ElMessage.success('角色已删除')
  await load()
}

function openCreateUser() {
  Object.assign(userForm, { username: '', password: '', roleIds: [] })
  userDialogVisible.value = true
}

async function saveUser() {
  if (!userForm.username.trim() || userForm.password.length < 8 || !userForm.roleIds.length) {
    ElMessage.warning('请填写账号、至少 8 位密码并选择角色')
    return
  }
  userSubmitting.value = true
  try {
    await merchantRbacApi.createUser({ ...userForm, roleIds: [...userForm.roleIds] })
    userDialogVisible.value = false
    ElMessage.success('账号创建成功')
    await load()
  } finally {
    userSubmitting.value = false
  }
}

async function toggleUser(user: MerchantUser) {
  await merchantRbacApi.setUserStatus(user.id, user.status === 1 ? 0 : 1)
  ElMessage.success(user.status === 1 ? '账号已禁用' : '账号已启用')
  await load()
}

async function resetPassword(user: MerchantUser) {
  const result = await ElMessageBox.prompt(`为“${user.username}”设置新密码`, '重置密码', {
    inputType: 'password',
    inputPlaceholder: '请输入至少 8 位新密码',
    inputValidator: (value) => value.length >= 8 || '密码至少 8 位',
  })
  await merchantRbacApi.resetUserPassword(user.id, result.value)
  ElMessage.success('密码已重置')
}

async function saveUserRoles(user: MerchantUser, roleIds: number[]) {
  await merchantRbacApi.setUserRoles(user.id, roleIds)
  ElMessage.success('角色分配成功')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="access-control page">
    <div class="page-header">
      <div>
        <span class="page-kicker">ACCESS CONTROL</span>
        <h1 class="page-title">账号与权限</h1>
        <p class="page-desc">按角色分配后台功能，控制成员可以查看和操作的业务范围。</p>
      </div>
      <div class="page-actions">
        <el-button v-if="activeTab === 'users'" type="primary" @click="openCreateUser">新增账号</el-button>
        <el-button v-else type="primary" @click="openCreateRole">新增角色</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="账号管理" name="users">
          <el-table v-loading="loading" :data="users" stripe>
            <el-table-column prop="username" label="账号" min-width="180" />
            <el-table-column label="角色" min-width="260">
              <template #default="{ row }">
                <el-tag v-for="role in row.roles" :key="role.id" size="small" effect="plain">{{ role.name }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="分配角色" width="250">
              <template #default="{ row }">
                <el-select
                  :model-value="row.roles.map((item: MerchantRole) => item.id)"
                  multiple
                  collapse-tags
                  size="small"
                  @change="(value: number[]) => saveUserRoles(row as MerchantUser, value)"
                >
                  <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-button link type="warning" @click="toggleUser(row as MerchantUser)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
                <el-button link type="primary" @click="resetPassword(row as MerchantUser)">重置密码</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="角色管理" name="roles">
          <el-table v-loading="loading" :data="roles" stripe>
            <el-table-column prop="name" label="角色名称" width="160" />
            <el-table-column prop="code" label="编码" width="180" />
            <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
            <el-table-column prop="userCount" label="账号数" width="90" />
            <el-table-column label="权限数" width="90">
              <template #default="{ row }">{{ row.permissionCodes.length }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.code === 'owner'" @click="openEditRole(row as MerchantRole)">
                  {{ row.code === 'owner' ? '系统角色' : '配置权限' }}
                </el-button>
                <el-button v-if="row.builtin !== 1" link type="danger" @click="removeRole(row as MerchantRole)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="roleDialogVisible" :title="editingRole ? '配置角色权限' : '新增角色'" width="720px">
      <el-form label-width="90px">
        <el-form-item label="角色编码"><el-input v-model="roleForm.code" :disabled="!!editingRole" maxlength="64" /></el-form-item>
        <el-form-item label="角色名称"><el-input v-model="roleForm.name" maxlength="100" /></el-form-item>
        <el-form-item label="角色说明"><el-input v-model="roleForm.description" maxlength="255" /></el-form-item>
        <el-form-item label="权限配置">
          <div class="permission-groups">
            <div v-for="group in permissionGroups" :key="group.module" class="permission-group">
              <strong>{{ group.module }}</strong>
              <el-checkbox-group v-model="roleForm.permissionCodes">
                <el-checkbox v-for="item in group.items" :key="item.code" :label="item.code">{{ item.name }}</el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="userDialogVisible" title="新增后台账号" width="480px">
      <el-form label-width="90px">
        <el-form-item label="账号"><el-input v-model="userForm.username" maxlength="20" /></el-form-item>
        <el-form-item label="初始密码"><el-input v-model="userForm.password" type="password" show-password maxlength="32" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="userSubmitting" @click="saveUser">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.permission-groups { width: 100%; }
.permission-group { padding: 12px 0; border-bottom: 1px solid var(--shop-border); }
.permission-group strong { display: block; margin-bottom: 8px; color: var(--shop-text); }
.permission-group :deep(.el-checkbox) { min-width: 180px; margin-right: 12px; }
.el-tag + .el-tag { margin-left: 6px; }
</style>
