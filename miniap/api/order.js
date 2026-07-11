const { request } = require('../utils/request')

module.exports = {
  page: (params) => request({ url: '/api/wx/order/page', data: params }),
  detail: (orderNo) => request({ url: `/api/wx/order/${orderNo}` }),
  cancel: (orderNo) => request({ url: `/api/wx/order/${orderNo}/cancel`, method: 'POST' }),
  confirmReceive: (orderNo) => request({ url: `/api/wx/order/${orderNo}/confirm-receive`, method: 'POST' }),
  repay: (orderNo) => request({ url: `/api/wx/order/${orderNo}/repay`, method: 'POST' }),
  mockPay: (orderNo) => request({ url: `/api/wx/order/${orderNo}/mock-pay`, method: 'POST' }),
}
