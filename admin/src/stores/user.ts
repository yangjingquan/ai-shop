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
    const token = ref<string>(localStorage.getItem('token') || '')
    const role = ref<UserRole | ''>(
      (localStorage.getItem('role') as UserRole) || '',
    )
    const merchantId = ref<number | null>(
      localStorage.getItem('merchantId')
        ? Number(localStorage.getItem('merchantId'))
        : null,
    )
    const merchantName = ref<string>(localStorage.getItem('merchantName') || '')

    function setAuth(payload: LoginResponse) {
      token.value = payload.token
      role.value = payload.role
      merchantId.value = payload.merchantId ?? null
      localStorage.setItem('token', payload.token)
      localStorage.setItem('role', payload.role)
      if (payload.merchantId != null) {
        localStorage.setItem('merchantId', String(payload.merchantId))
      } else {
        localStorage.removeItem('merchantId')
      }
    }

    function setMerchantName(name: string) {
      merchantName.value = name
      if (name) {
        localStorage.setItem('merchantName', name)
      } else {
        localStorage.removeItem('merchantName')
      }
    }

    function logout() {
      token.value = ''
      role.value = ''
      merchantId.value = null
      merchantName.value = ''
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('merchantId')
      localStorage.removeItem('merchantName')
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
