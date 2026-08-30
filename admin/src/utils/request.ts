import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const defaultApiBaseUrl = import.meta.env.PROD
  ? 'https://conapi.nexbyte.top'
  : 'http://localhost:8081'

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || defaultApiBaseUrl,
  timeout: 15000,
})

let redirectingToLogin = false

function isLocalHost(hostname: string) {
  return hostname === 'localhost'
    || hostname === '127.0.0.1'
    || hostname === '[::1]'
    || hostname === '::1'
}

function isCredentialEndpoint(config: InternalAxiosRequestConfig) {
  if (!config.url) return false
  const url = new URL(config.url, config.baseURL || window.location.origin)
  return /^\/api\/(admin|merchant)\/auth\/(login|password|public-key)$/.test(url.pathname)
}

function ensureSecureCredentialTransport(config: InternalAxiosRequestConfig) {
  if (!isCredentialEndpoint(config)) return

  const url = new URL(config.url!, config.baseURL || window.location.origin)
  const localDevelopment = isLocalHost(url.hostname) && isLocalHost(window.location.hostname)
  if (!localDevelopment && url.protocol !== 'https:') {
    throw new Error('登录接口必须通过 HTTPS 访问，已阻止明文传输密码')
  }
}

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
  ensureSecureCredentialTransport(config)
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
