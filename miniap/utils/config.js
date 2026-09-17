const CONFIGS = {
  dev: {
    BASE_URL: 'http://localhost:8082',
  },
  prod: {
    BASE_URL: 'https://miniapi.nexbyte.top',
  },
}

function readExtConfig() {
  try {
    return typeof wx !== 'undefined' && wx.getExtConfigSync ? (wx.getExtConfigSync() || {}) : {}
  } catch (e) {
    return {}
  }
}

function readMiniAppId() {
  try {
    const account = wx.getAccountInfoSync()
    return String(account && account.miniProgram && account.miniProgram.appId || '').trim()
  } catch (e) {
    return ''
  }
}

const extConfig = readExtConfig()
// 生产地址保持为默认值；联调时可通过 extConfig.env 或本地 storage 的 shop_api_env 切换 dev。
const storageEnv = typeof wx !== 'undefined' ? wx.getStorageSync('shop_api_env') : ''
const ENV = String(extConfig.env || storageEnv || 'prod').trim()
const selected = CONFIGS[ENV] || CONFIGS.prod
const MINIAPP_APP_ID = readMiniAppId()

function storageKey(name) {
  const scope = MINIAPP_APP_ID || 'unknown-app'
  return `shop_${name}_${scope}`
}

function getStorage(name, fallback) {
  if (typeof wx === 'undefined') return fallback
  const value = wx.getStorageSync(storageKey(name))
  return value === '' || value === undefined || value === null ? fallback : value
}

function setStorage(name, value) {
  if (typeof wx !== 'undefined') wx.setStorageSync(storageKey(name), value)
}

function removeStorage(name) {
  if (typeof wx !== 'undefined') wx.removeStorageSync(storageKey(name))
}

function getMerchantCode() {
  const stored = getStorage('merchant_code', '')
  return String(extConfig.merchantCode || stored || '').trim()
}

module.exports = {
  BASE_URL: String(extConfig.baseUrl || selected.BASE_URL).replace(/\/$/, ''),
  ENV,
  MINIAPP_APP_ID,
  storageKey,
  getStorage,
  setStorage,
  removeStorage,
  getMerchantCode,
  REQUEST_TIMEOUT: 10000,
}
