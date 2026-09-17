const config = require('./config')

function request(options, retryCount = 0) {
  return new Promise((resolve, reject) => {
    const token = config.getStorage('wx_token', '') || ''
    const method = options.method || 'GET'
    const url = config.BASE_URL + options.url
    wx.request({
      url,
      method,
      data: options.data || {},
      timeout: options.timeout || config.REQUEST_TIMEOUT,
      header: {
        'content-type': 'application/json',
        'wx-token': token,
        'merchant-code': config.getMerchantCode(),
        'miniapp-appid': config.MINIAPP_APP_ID,
        'x-shop-data-version': String(wx.getStorageSync('shop_data_version') || 0),
        ...options.header,
      },
      success(res) {
        const headers = res.header || {}
        const version = headers['X-Shop-Data-Version'] || headers['x-shop-data-version']
        const invalidated = headers['X-Shop-Invalidated-Scopes'] || headers['x-shop-invalidated-scopes']
        if (version && Number(version) >= Number(wx.getStorageSync('shop_data_version') || 0)) {
          wx.setStorageSync('shop_data_version', Number(version))
          wx.setStorageSync('shop_server_time', headers['X-Shop-Server-Time'] || headers['x-shop-server-time'] || '')
        }
        if (invalidated) wx.setStorageSync('shop_invalidated_scopes', invalidated.split(',').filter(Boolean))
        const data = res.data
        if (res.statusCode === 401 || (data && data.code === 401)) {
          if (retryCount >= 1) {
            config.removeStorage('wx_token')
            const exhausted = { code: 401, msg: '登录已过期，请重新进入小程序', retryExhausted: true }
            wx.showToast({ title: exhausted.msg, icon: 'none' })
            reject(exhausted)
            return
          }
          const auth = require('./auth')
          config.removeStorage('wx_token')
          auth.silentLogin().then((loginData) => {
            if (!loginData || !loginData.token) {
              const failed = { code: 401, msg: '登录失败，请重试', retryExhausted: true }
              wx.showToast({ title: failed.msg, icon: 'none' })
              reject(failed)
              return
            }
            request(options, retryCount + 1).then(resolve).catch(reject)
          }).catch(reject)
          return
        }
        if (!data || data.code !== 0) {
          console.warn('request failed:', { url, method, statusCode: res.statusCode, data })
          wx.showToast({ title: (data && data.msg) || '请求失败', icon: 'none' })
          reject(data)
          return
        }
        resolve(data)
      },
      fail(err) {
        console.warn('network error:', { url, method, err })
        wx.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      },
    })
  })
}

module.exports = { request }
