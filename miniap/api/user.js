const { request } = require('../utils/request')
const config = require('../utils/config')
const auth = require('../utils/auth')

function waitForAuth() {
  let app = null
  try {
    app = getApp()
  } catch (e) {
    // API tests or non-page callers may not have an App instance.
  }
  const authReady = app && app.globalData && app.globalData.authReady
  return Promise.resolve(authReady).catch(() => null).then(() => {
    if (config.getStorage('wx_token', '')) return null
    return auth.silentLogin()
  })
}

function uploadFile(filePath) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: config.BASE_URL + '/api/wx/file/upload',
      filePath,
      name: 'file',
      header: {
        'wx-token': config.getStorage('wx_token', '') || '',
        'merchant-code': config.getMerchantCode(),
        'miniapp-appid': config.MINIAPP_APP_ID,
      },
      success(res) {
        let data
        try {
          data = JSON.parse(res.data || '{}')
        } catch (err) {
          const message = uploadFailureMessage(res.statusCode)
          console.warn('avatar upload returned a non-JSON response:', {
            statusCode: res.statusCode,
            response: String(res.data || '').slice(0, 300),
          })
          reject({ code: res.statusCode || 500, msg: message, cause: err })
          return
        }
        if (res.statusCode === 401 || (data && data.code === 401)) {
          reject({ code: 401, msg: data.msg || '登录已过期，请重试', statusCode: res.statusCode })
          return
        }
        if (res.statusCode < 200 || res.statusCode >= 300 || !data || data.code !== 0) {
          reject({
            code: data && data.code,
            msg: (data && data.msg) || '头像上传失败',
            statusCode: res.statusCode,
          })
          return
        }
        const url = data.data && data.data.url
        if (!url) {
          reject({ code: 500, msg: '头像上传失败' })
          return
        }
        resolve(url)
      },
      fail(err) {
        console.warn('avatar upload request failed:', err)
        reject({ code: err && err.errCode, msg: '头像上传请求失败，请检查网络后重试', cause: err })
      },
    })
  })
}

// chooseAvatar 在开发者工具通常返回 http://tmp/...，真机通常返回
// wxfile://tmp_...。先保存为本地缓存文件，避免上传组件在不同运行时对临时
// 路径协议的处理不一致，也避免等待登录时临时文件被回收。
function stabilizeFilePath(filePath) {
  return new Promise((resolve) => {
    if (!filePath || typeof wx === 'undefined' || !wx.getFileSystemManager) {
      resolve(filePath)
      return
    }
    const fs = wx.getFileSystemManager()
    fs.saveFile({
      tempFilePath: filePath,
      success(res) {
        resolve(res && res.savedFilePath ? res.savedFilePath : filePath)
      },
      fail(err) {
        // 某些基础库已经把路径视为稳定路径；保存失败时继续使用原路径，
        // 不因为兼容处理反而阻断正常上传。
        console.warn('avatar temp file save skipped:', err)
        resolve(filePath)
      },
    })
  })
}

function uploadFailureMessage(statusCode) {
  if (statusCode === 413) return '头像图片过大，请换一张小于 10MB 的图片'
  if (statusCode === 401) return '登录已过期，请重试'
  if (statusCode >= 500) return '上传服务暂时不可用，请稍后重试'
  return '头像上传失败，请重试'
}

function uploadAvatar(filePath, retryCount = 0) {
  let stablePath = filePath
  return stabilizeFilePath(filePath)
    .then((path) => {
      stablePath = path || filePath
      return waitForAuth().then(() => uploadFile(stablePath))
    })
    .catch((err) => {
      const unauthorized = err && (err.code === 401 || err.statusCode === 401)
      if (!unauthorized || retryCount >= 1) throw err
      config.removeStorage('wx_token')
      return auth.silentLogin().then((loginData) => {
        if (!loginData || !loginData.token) {
          throw { code: 401, msg: '登录失败，请重试' }
        }
        return uploadFile(stablePath)
      })
    })
}

module.exports = {
  bindPhone: (code) =>
    request({ url: '/api/wx/user/bind-phone', method: 'POST', data: { code } }),
  getProfile: () => request({ url: '/api/wx/user/profile' }),
  updateProfile: (data) => request({ url: '/api/wx/user/profile', method: 'POST', data }),
  uploadAvatar,
}
