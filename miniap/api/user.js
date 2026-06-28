const { request } = require('../utils/request')
const config = require('../utils/config')

module.exports = {
  bindPhone: (code) =>
    request({ url: '/api/wx/user/bind-phone', method: 'POST', data: { code } }),
  getProfile: () => request({ url: '/api/wx/user/profile' }),
  updateProfile: (data) => request({ url: '/api/wx/user/profile', method: 'POST', data }),
  uploadAvatar: (filePath) => new Promise((resolve, reject) => {
    wx.uploadFile({
      url: config.BASE_URL + '/api/wx/file/upload',
      filePath,
      name: 'file',
      header: {
        'wx-token': wx.getStorageSync('wx_token') || '',
      },
      success(res) {
        let data
        try {
          data = JSON.parse(res.data || '{}')
        } catch (err) {
          wx.showToast({ title: '头像上传失败', icon: 'none' })
          reject(err)
          return
        }
        if (data.code !== 0) {
          wx.showToast({ title: data.msg || '头像上传失败', icon: 'none' })
          reject(data)
          return
        }
        resolve(data.data && data.data.url)
      },
      fail(err) {
        wx.showToast({ title: '头像上传失败', icon: 'none' })
        reject(err)
      },
    })
  }),
}
