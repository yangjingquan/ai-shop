const config = require('./config')

function silentLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success(loginRes) {
        if (!loginRes.code) {
          reject(new Error('wx.login failed'))
          return
        }
        wx.request({
          url: config.BASE_URL + '/api/wx/auth/login',
          method: 'POST',
          data: { code: loginRes.code },
          header: { 'content-type': 'application/json' },
          success(res) {
            const data = res.data
            if (data.code === 0 && data.data && data.data.token) {
              wx.setStorageSync('wx_token', data.data.token)
              wx.setStorageSync('has_phone', data.data.hasPhone)
              resolve(data.data)
            } else {
              console.warn('silentLogin failed:', data)
              resolve(null)
            }
          },
          fail(err) {
            console.warn('silentLogin network error:', err)
            resolve(null)
          },
        })
      },
      fail: reject,
    })
  })
}

module.exports = { silentLogin }
