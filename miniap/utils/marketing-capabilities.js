const marketingApi = require('../api/marketing')
const config = require('./config')

let currentKey = ''
let featureMap = {}
let loadedAt = 0
const CACHE_TTL = 5 * 60 * 1000

function cacheKey() {
  return `marketing_features_${config.getMerchantCode() || 'default'}`
}

function normalize(list) {
  const map = {}
  const items = Array.isArray(list) ? list : Object.keys(list || {}).map((key) => list[key])
  items.forEach((item) => {
    if (item && item.code) map[item.code] = item
  })
  return map
}

function seed(list) {
  currentKey = cacheKey()
  featureMap = normalize(list)
  loadedAt = Date.now()
  wx.setStorageSync(currentKey, { at: loadedAt, features: featureMap })
  return featureMap
}

function readCache() {
  const cached = wx.getStorageSync(cacheKey())
  if (!cached || !cached.features || Date.now() - Number(cached.at || 0) > CACHE_TTL) return null
  currentKey = cacheKey()
  featureMap = cached.features
  loadedAt = Number(cached.at || Date.now())
  return featureMap
}

function load(force) {
  const key = cacheKey()
  if (!force && currentKey === key && Date.now() - loadedAt <= CACHE_TTL) return Promise.resolve(featureMap)
  if (!force) {
    const cached = readCache()
    if (cached) return Promise.resolve(cached)
  }
  return marketingApi.features().then((res) => seed(res && res.data)).catch(() => {
    const cached = readCache()
    return cached || {}
  })
}

function isEnabled(code) {
  return !!(featureMap[code] && (featureMap[code].enabled === true || Number(featureMap[code].enabled) === 1))
}

function ensure(code) {
  return load(false).then((map) => {
    if (map[code] && (map[code].enabled === true || Number(map[code].enabled) === 1)) return true
    wx.showToast({ title: '该营销活动暂未开启', icon: 'none' })
    return false
  })
}

module.exports = { load, seed, isEnabled, ensure }
