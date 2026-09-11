const auth = require('./utils/auth')
const { request } = require('./utils/request')

function referralShareContext(options) {
  const query = options && options.query || {}
  const campaignId = Number(query.campaignId || 0)
  const token = String(query.token || '').trim()
  return campaignId > 0 && token ? { campaignId, token } : null
}

App({
  request,
  globalData: {
    userInfo: null,
    cartCount: 0,
    newUserCouponPopupShown: false,
    referralShareContext: null,
  },
  onLaunch(options) {
    const context = referralShareContext(options)
    if (context) this.globalData.referralShareContext = context
    auth.silentLogin().then((res) => {
      if (res) {
        console.log('[app] silentLogin ok, hasPhone=', res.hasPhone)
      }
    }).catch((err) => {
      console.warn('[app] silentLogin error:', err)
    })
  },
  onShow(options) {
    const context = referralShareContext(options)
    if (context) this.globalData.referralShareContext = context
    const since = wx.getStorageSync('shop_data_version') || 0
    request({ url: `/api/wx/sync/version?since=${encodeURIComponent(since)}`, method: 'GET' })
      .then((res) => {
        const data = res && res.data
        if (!data) return
        wx.setStorageSync('shop_data_version', Number(data.dataVersion || since))
        wx.setStorageSync('shop_server_time', data.serverTime || '')
        wx.setStorageSync('shop_invalidated_scopes', data.invalidatedScopes || [])
      }).catch(() => {
        // Foreground recovery must not block browsing; sensitive checkout data is always revalidated server-side.
      })
  },
})
