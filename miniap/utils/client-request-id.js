function storageKey(scope) {
  return `pending_client_request:${scope}`
}

function create(scope) {
  const key = storageKey(scope)
  const existing = wx.getStorageSync(key)
  if (existing) return existing
  const value = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`
  wx.setStorageSync(key, value)
  return value
}

function clear(scope) {
  wx.removeStorageSync(storageKey(scope))
}

module.exports = { create, clear }
