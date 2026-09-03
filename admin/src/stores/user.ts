import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminAuthApi, merchantAuthApi } from '@/api/auth'
import { merchantRbacApi, type MerchantAuthMe } from '@/api/rbac'

export type UserRole = 'admin' | 'merchant'

interface LoginResponse {
  token: string
  role: UserRole
  merchantId?: number | null
}

export const useUserStore = defineStore(
  'user',
  () => {
    const storage = window.sessionStorage
    // 清理旧版本持久化凭证，避免令牌长期残留在浏览器磁盘中。
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('merchantId')
    localStorage.removeItem('merchantName')

    const token = ref<string>(storage.getItem('token') || '')
    const role = ref<UserRole | ''>(
      (storage.getItem('role') as UserRole) || '',
    )
    const merchantId = ref<number | null>(
      storage.getItem('merchantId')
        ? Number(storage.getItem('merchantId'))
        : null,
    )
    const merchantName = ref<string>(storage.getItem('merchantName') || '')
    const username = ref<string>(storage.getItem('username') || '')
    function readStringArray(key: string) {
      try {
        const value = JSON.parse(storage.getItem(key) || '[]')
        return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : []
      } catch {
        return []
      }
    }

    const permissions = ref<string[]>(readStringArray('permissions'))
    const roleCodes = ref<string[]>(readStringArray('roleCodes'))
    let authLoaded = false

    function setAuth(payload: LoginResponse, loginUsername = '') {
      authLoaded = false
      token.value = payload.token
      role.value = payload.role
      merchantId.value = payload.merchantId ?? null
      username.value = loginUsername
      permissions.value = []
      roleCodes.value = []
      storage.setItem('token', payload.token)
      storage.setItem('role', payload.role)
      storage.removeItem('permissions')
      storage.removeItem('roleCodes')
      if (loginUsername) {
        storage.setItem('username', loginUsername)
      } else {
        storage.removeItem('username')
      }
      if (payload.merchantId != null) {
        storage.setItem('merchantId', String(payload.merchantId))
      } else {
        storage.removeItem('merchantId')
      }
    }

    function setMerchantName(name: string) {
      merchantName.value = name
      if (name) {
        storage.setItem('merchantName', name)
      } else {
        storage.removeItem('merchantName')
      }
    }

    function logout() {
      token.value = ''
      role.value = ''
      merchantId.value = null
      merchantName.value = ''
      username.value = ''
      permissions.value = []
      roleCodes.value = []
      authLoaded = false
      storage.removeItem('token')
      storage.removeItem('role')
      storage.removeItem('merchantId')
      storage.removeItem('merchantName')
      storage.removeItem('username')
      storage.removeItem('permissions')
      storage.removeItem('roleCodes')
    }

    async function loginAdmin(username: string, password: string) {
      const data = await adminAuthApi.login(username, password) as LoginResponse
      setAuth({ ...data, role: 'admin' }, username)
      return data
    }

    async function loginMerchant(username: string, password: string) {
      const data = await merchantAuthApi.login(username, password) as LoginResponse
      setAuth({ ...data, role: 'merchant' }, username)
      await loadCurrentUser()
      return data
    }

    function setCurrentUser(data: MerchantAuthMe) {
      username.value = data.username
      merchantId.value = data.merchantId
      merchantName.value = data.merchantName
      roleCodes.value = data.roles.map((item) => item.code)
      permissions.value = data.permissions
      storage.setItem('username', data.username)
      storage.setItem('merchantId', String(data.merchantId))
      storage.setItem('merchantName', data.merchantName || '')
      storage.setItem('roleCodes', JSON.stringify(roleCodes.value))
      storage.setItem('permissions', JSON.stringify(permissions.value))
    }

    async function loadCurrentUser() {
      if (!token.value || role.value !== 'merchant' || authLoaded) return
      const data = await merchantRbacApi.me()
      setCurrentUser(data)
      authLoaded = true
    }

    function hasPermission(permission: string) {
      return role.value === 'merchant' && permissions.value.includes(permission)
    }

    return {
      token,
      role,
      merchantId,
      merchantName,
      username,
      roleCodes,
      permissions,
      setAuth,
      setMerchantName,
      logout,
      loginAdmin,
      loginMerchant,
      loadCurrentUser,
      hasPermission,
    }
  },
)
