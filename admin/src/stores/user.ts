import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

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

    function setAuth(payload: LoginResponse) {
      token.value = payload.token
      role.value = payload.role
      merchantId.value = payload.merchantId ?? null
      storage.setItem('token', payload.token)
      storage.setItem('role', payload.role)
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
      storage.removeItem('token')
      storage.removeItem('role')
      storage.removeItem('merchantId')
      storage.removeItem('merchantName')
    }

    async function loginAdmin(username: string, password: string) {
      const data = await request.post<unknown, LoginResponse>(
        '/api/admin/auth/login',
        { username, password },
      )
      setAuth({ ...data, role: 'admin' })
      return data
    }

    async function loginMerchant(username: string, password: string) {
      const data = await request.post<unknown, LoginResponse>(
        '/api/merchant/auth/login',
        { username, password },
      )
      setAuth({ ...data, role: 'merchant' })
      return data
    }

    return {
      token,
      role,
      merchantId,
      merchantName,
      setAuth,
      setMerchantName,
      logout,
      loginAdmin,
      loginMerchant,
    }
  },
)
