const auth = require('./utils/auth')
const { request } = require('./utils/request')

App({
  request,
  globalData: {
    userInfo: null,
    cartCount: 0,
    newUserCouponPopupShown: false,
  },
  onLaunch() {
    auth.silentLogin().then((res) => {
      if (res) {
        console.log('[app] silentLogin ok, hasPhone=', res.hasPhone)
      }
    }).catch((err) => {
      console.warn('[app] silentLogin error:', err)
    })
  },
  onShow() {
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
