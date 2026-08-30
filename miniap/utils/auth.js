const config = require('./config')

let loginPromise = null

function silentLogin() {
  if (loginPromise) return loginPromise
  loginPromise = new Promise((resolve, reject) => {
    wx.login({
      success(loginRes) {
        if (!loginRes.code) {
          reject(new Error('wx.login failed'))
          return
        }
        const merchantCode = config.getMerchantCode()
        const miniAppId = String(config.MINIAPP_APP_ID || '').trim()
        if (!merchantCode && !miniAppId) {
          console.warn('silentLogin skipped: merchant identity is empty')
          resolve(null)
          return
        }
        const payload = { code: String(loginRes.code || ''), merchantCode, miniAppId }
        console.info('wx login request:', {
          url: config.BASE_URL + '/api/wx/auth/login',
          hasCode: !!loginRes.code,
          merchantCode,
          miniAppId,
        })
        wx.request({
          url: config.BASE_URL + '/api/wx/auth/login',
          method: 'POST',
          data: payload,
          header: { 'content-type': 'application/json', Accept: 'application/json' },
          success(res) {
            const data = res.data
            if (data && data.code === 0 && data.data && data.data.token) {
              wx.setStorageSync('wx_token', data.data.token)
              wx.setStorageSync('wx_openid', data.data.openid || '')
              wx.setStorageSync('has_phone', data.data.hasPhone)
              if (data.data.merchantCode) wx.setStorageSync('merchant_code', data.data.merchantCode)
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
  }).finally(() => {
    loginPromise = null
  })
  return loginPromise
}

module.exports = { silentLogin }
