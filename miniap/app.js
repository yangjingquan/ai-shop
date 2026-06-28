const auth = require('./utils/auth')
const { request } = require('./utils/request')

App({
  request,
  globalData: {
    userInfo: null,
    cartCount: 0,
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
})
