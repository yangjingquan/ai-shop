<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { merchantRbacApi, type MerchantPermission, type MerchantRole, type MerchantUser } from '@/api/rbac'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const router = useRouter()
const loading = ref(false)
const activeTab = ref<'users' | 'roles'>('users')
const users = ref<MerchantUser[]>([])
const roles = ref<MerchantRole[]>([])
const permissions = ref<MerchantPermission[]>([])
const roleDialogVisible = ref(false)
const userDialogVisible = ref(false)
const assignDialogVisible = ref(false)
const roleSubmitting = ref(false)
const userSubmitting = ref(false)
const assignmentSubmitting = ref(false)
const editingRole = ref<MerchantRole | null>(null)
const assigningUser = ref<MerchantUser | null>(null)
const roleForm = reactive({ code: '', name: '', description: '', permissionCodes: [] as string[] })
const userForm = reactive({ username: '', password: '', roleIds: [] as number[] })
const assignmentRoleIds = ref<number[]>([])
const permissionSearch = ref('')
const permissionCount = computed(() => roleForm.permissionCodes.length)

const permissionGroups = computed(() => {
  const groups = new Map<string, MerchantPermission[]>()
  for (const permission of permissions.value) {
    const list = groups.get(permission.module) || []
    list.push(permission)
    groups.set(permission.module, list)
  }
  return [...groups.entries()].map(([module, items]) => ({ module, items }))
})
const visiblePermissionGroups = computed(() => {
  const query = permissionSearch.value.trim().toLowerCase()
  if (!query) return permissionGroups.value
  return permissionGroups.value
    .map((group) => ({
      module: group.module,
      items: group.items.filter((item) =>
        `${group.module} ${item.name} ${item.code}`.toLowerCase().includes(query)),
    }))
    .filter((group) => group.items.length)
})

function togglePermissionGroup(items: MerchantPermission[]) {
  const codes = items.map((item) => item.code)
  const selected = new Set(roleForm.permissionCodes)
  const allSelected = codes.every((code) => selected.has(code))
  codes.forEach((code) => allSelected ? selected.delete(code) : selected.add(code))
  roleForm.permissionCodes = [...selected]
}

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
  permissionSearch.value = ''
  Object.assign(roleForm, { code: '', name: '', description: '', permissionCodes: [] })
  roleDialogVisible.value = true
}

function openEditRole(role: MerchantRole) {
  editingRole.value = role
  permissionSearch.value = ''
  Object.assign(roleForm, {
    code: role.code,
    name: role.name,
    description: role.description || '',
    permissionCodes: [...role.permissionCodes],
  })
  roleDialogVisible.value = true
}

async function saveRole() {
  if (!editingRole.value && !/^[a-zA-Z][a-zA-Z0-9_]{1,63}$/.test(roleForm.code.trim())) {
    ElMessage.warning('角色编码需以字母开头，只能使用字母、数字和下划线，长度为 2-64 位')
    return
  }
  if (!roleForm.name.trim()) {
    ElMessage.warning('请填写角色名称')
    return
  }
  roleSubmitting.value = true
  try {
    const payload = { code: roleForm.code.trim(), name: roleForm.name.trim(),
      description: roleForm.description.trim(), permissionCodes: [...roleForm.permissionCodes] }
    if (editingRole.value) await merchantRbacApi.updateRole(editingRole.value.id, payload)
    else await merchantRbacApi.createRole(payload)
    roleDialogVisible.value = false
    ElMessage.success('角色保存成功')
    await userStore.refreshCurrentUser()
    if (!userStore.hasPermission('merchant:rbac:manage')) {
      await router.replace('/merchant')
      return
    }
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
  if (!/^[a-zA-Z0-9_]{4,20}$/.test(userForm.username.trim())) {
    ElMessage.warning('账号需为 4-20 位字母、数字或下划线')
    return
  }
  if (userForm.password.length < 8 || userForm.password.length > 32 || !userForm.roleIds.length) {
    ElMessage.warning('请输入 8-32 位密码并至少选择一个角色')
    return
  }
  userSubmitting.value = true
  try {
    await merchantRbacApi.createUser({ username: userForm.username.trim(), password: userForm.password,
      roleIds: [...userForm.roleIds] })
    userDialogVisible.value = false
    ElMessage.success('账号创建成功')
    await load()
  } finally {
    userSubmitting.value = false
  }
}

async function toggleUser(user: MerchantUser) {
  if (user.status === 1) {
    await ElMessageBox.confirm(`禁用“${user.username}”后，该账号将立即退出登录。确定继续吗？`, '禁用账号', { type: 'warning' })
  }
  await merchantRbacApi.setUserStatus(user.id, user.status === 1 ? 0 : 1)
  ElMessage.success(user.status === 1 ? '账号已禁用' : '账号已启用')
  await load()
}

async function resetPassword(user: MerchantUser) {
  const result = await ElMessageBox.prompt(`为“${user.username}”设置新密码`, '重置密码', {
    inputType: 'password',
    inputPlaceholder: '请输入至少 8 位新密码',
    inputValidator: (value) => (value.length >= 8 && value.length <= 32) || '密码需为 8-32 位',
  })
  await merchantRbacApi.resetUserPassword(user.id, result.value)
  ElMessage.success('密码已重置')
  if (user.username === userStore.username) {
    userStore.logout()
    await router.replace('/login')
  }
}

function openAssignRoles(user: MerchantUser) {
  assigningUser.value = user
  assignmentRoleIds.value = user.roles.map((role) => role.id)
  assignDialogVisible.value = true
}

async function saveUserRoles() {
  if (!assigningUser.value || !assignmentRoleIds.value.length) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  assignmentSubmitting.value = true
  try {
    await merchantRbacApi.setUserRoles(assigningUser.value.id, [...assignmentRoleIds.value])
    assignDialogVisible.value = false
    ElMessage.success('角色分配成功')
    if (assigningUser.value.username === userStore.username) {
      userStore.logout()
      await router.replace('/login')
      return
    }
    await load()
  } finally {
    assignmentSubmitting.value = false
  }
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
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAssignRoles(row as MerchantUser)">分配角色</el-button>
                <el-button link type="warning" :disabled="row.username === userStore.username" @click="toggleUser(row as MerchantUser)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
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
            <p class="permission-hint">操作权限需要同时授予对应页面的查看权限；活动配置若需选择商品、分类或优惠券，还需授予这些数据的查看权限。</p>
            <div class="permission-toolbar">
              <el-input v-model="permissionSearch" clearable placeholder="搜索模块、权限名称或编码" />
              <span>已选 {{ permissionCount }} / {{ permissions.length }} 项</span>
            </div>
            <div class="permission-scroll">
              <div v-for="group in visiblePermissionGroups" :key="group.module" class="permission-group">
                <div class="permission-group-header">
                  <strong>{{ group.module }}</strong>
                  <el-button link type="primary" @click="togglePermissionGroup(group.items)">
                    {{ group.items.every((item) => roleForm.permissionCodes.includes(item.code)) ? '取消全选' : '全选' }}
                  </el-button>
                </div>
                <el-checkbox-group v-model="roleForm.permissionCodes">
                  <el-checkbox v-for="item in group.items" :key="item.code" :label="item.code">
                    {{ item.name }} <span class="permission-code">{{ item.code }}</span>
                  </el-checkbox>
                </el-checkbox-group>
              </div>
              <el-empty v-if="!visiblePermissionGroups.length" description="没有匹配的权限" :image-size="64" />
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
        <el-form-item label="账号"><el-input v-model="userForm.username" maxlength="20" placeholder="4-20 位字母、数字或下划线" /></el-form-item>
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

    <el-dialog v-model="assignDialogVisible" :title="`分配角色 · ${assigningUser?.username || ''}`" width="480px">
      <p class="assignment-hint">更改角色后，该账号的现有登录会失效，需要重新登录。</p>
      <el-select v-model="assignmentRoleIds" multiple placeholder="请选择角色" style="width: 100%">
        <el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignmentSubmitting" @click="saveUserRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.permission-groups { width: 100%; }
.permission-hint { margin: 0 0 10px; color: var(--shop-text-secondary, #6b7280); font-size: 12px; line-height: 1.5; }
.permission-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.permission-toolbar .el-input { flex: 1; }
.permission-toolbar span { white-space: nowrap; color: var(--shop-text-secondary, #6b7280); font-size: 12px; }
.permission-scroll { max-height: 360px; overflow-y: auto; padding-right: 8px; }
.permission-group { padding: 12px 0; border-bottom: 1px solid var(--shop-border); }
.permission-group-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.permission-group strong { color: var(--shop-text); }
.permission-group :deep(.el-checkbox) { display: flex; width: 100%; min-height: 30px; margin-right: 0; }
.permission-code { margin-left: 8px; color: var(--shop-text-secondary, #6b7280); font-size: 11px; }
.assignment-hint { margin: 0 0 16px; color: var(--shop-text-secondary, #6b7280); font-size: 13px; }
.el-tag + .el-tag { margin-left: 6px; }
</style>
