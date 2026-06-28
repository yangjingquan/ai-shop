import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000,
})

let redirectingToLogin = false

function redirectToLogin() {
  const userStore = useUserStore()
  userStore.logout()
  if (redirectingToLogin || window.location.pathname === '/login') return
  redirectingToLogin = true
  ElMessage.error('登录已过期，请重新登录')
  const redirect = encodeURIComponent(`${window.location.pathname}${window.location.search}`)
  window.location.href = `/login?redirect=${redirect}`
}

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

request.interceptors.response.use(
  (resp: AxiosResponse) => {
    const data = resp.data
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code === 0) {
        return data.data
      }
      if (data.code === 401) {
        redirectToLogin()
        return Promise.reject(new Error(data.msg || '登录已过期'))
      }
      ElMessage.error(data.msg || '请求失败')
      return Promise.reject(new Error(data.msg || '请求失败'))
    }
    return data
  },
  (err) => {
    const status = err?.response?.status
    if (status === 401) {
      redirectToLogin()
    } else {
      ElMessage.error(err?.response?.data?.msg || err.message || '网络错误')
    }
    return Promise.reject(err)
  },
)

export default request
