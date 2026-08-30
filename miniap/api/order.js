const { request } = require('../utils/request')

module.exports = {
  page: (params) => request({ url: '/api/wx/order/page', data: params }),
  detail: (orderNo) => request({ url: `/api/wx/order/${orderNo}` }),
  cancel: (orderNo) => request({ url: `/api/wx/order/${orderNo}/cancel`, method: 'POST' }),
  confirmReceive: (orderNo) => request({ url: `/api/wx/order/${orderNo}/confirm-receive`, method: 'POST' }),
  remindShip: (orderNo) => request({ url: `/api/wx/order/${orderNo}/remind-ship`, method: 'POST' }),
  refund: (orderNo, reason) => request({ url: `/api/wx/order/${orderNo}/refund`, method: 'POST', data: { reason } }),
  repay: (orderNo) => request({ url: `/api/wx/order/${orderNo}/repay`, method: 'POST' }),
}
