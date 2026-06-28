const config = require('./config')

function request(options) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('wx_token') || ''
    wx.request({
      url: config.BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      timeout: options.timeout || config.REQUEST_TIMEOUT,
      header: {
        'content-type': 'application/json',
        'wx-token': token,
        ...options.header,
      },
      success(res) {
        const data = res.data
        if (data.code === 401) {
          const auth = require('./auth')
          auth.silentLogin().then(() => {
            request(options).then(resolve).catch(reject)
          }).catch(reject)
          return
        }
        if (data.code !== 0) {
          wx.showToast({ title: data.msg || '请求失败', icon: 'none' })
          reject(data)
          return
        }
        resolve(data)
      },
      fail(err) {
        wx.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      },
    })
  })
}

module.exports = { request }
