const { request } = require('../utils/request')
const config = require('../utils/config')

function uploadRefundEvidence(filePath) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: config.BASE_URL + '/api/wx/file/upload',
      filePath,
      name: 'file',
      header: {
        'wx-token': wx.getStorageSync('wx_token') || '',
        'merchant-code': config.getMerchantCode(),
        'miniapp-appid': config.MINIAPP_APP_ID,
      },
      success(res) {
        let data
        try {
          data = JSON.parse(res.data || '{}')
        } catch (err) {
          reject(err)
          return
        }
        if (data.code !== 0 || !data.data || !data.data.url) {
          reject(data || new Error('凭证上传失败'))
          return
        }
        resolve(data.data.url)
      },
      fail: reject,
    })
  })
}

module.exports = {
  page: (params) => request({ url: '/api/wx/order/page', data: params }),
  detail: (orderNo) => request({ url: `/api/wx/order/${orderNo}` }),
  logistics: (orderNo, forceRefresh) => request({
    url: `/api/wx/order/${orderNo}/logistics`,
    data: forceRefresh ? { forceRefresh: true } : {},
  }),
  refreshLogistics: (orderNo) => request({ url: `/api/wx/order/${orderNo}/logistics/refresh`, method: 'POST' }),
  cancel: (orderNo) => request({ url: `/api/wx/order/${orderNo}/cancel`, method: 'POST' }),
  remove: (orderNo) => request({ url: `/api/wx/order/${orderNo}/delete`, method: 'POST' }),
  confirmReceive: (orderNo) => request({ url: `/api/wx/order/${orderNo}/confirm-receive`, method: 'POST' }),
  remindShip: (orderNo) => request({ url: `/api/wx/order/${orderNo}/remind-ship`, method: 'POST' }),
  refund: (orderNo, payload) => request({
    url: `/api/wx/order/${orderNo}/refund`,
    method: 'POST',
    data: typeof payload === 'string' ? { reason: payload } : (payload || {}),
  }),
  uploadRefundEvidence,
  submitReturnShipment: (refundId, data) => request({ url: `/api/wx/order/refund/${refundId}/return-shipment`, method: 'POST', data }),
  repay: (orderNo) => request({ url: `/api/wx/order/${orderNo}/repay`, method: 'POST' }),
}
