const { request } = require('../utils/request')

module.exports = {
  active: () => request({ url: '/api/wx/presales/active' }),
  detail: (activityId) => request({ url: `/api/wx/presales/${activityId}` }),
  order: (orderNo) => request({ url: `/api/wx/presales/orders/${orderNo}` }),
  quoteDeposit: (data) => request({ url: '/api/wx/presales/deposit-quote', method: 'POST', data }),
  createDeposit: (data) => request({ url: '/api/wx/presales/deposit-orders', method: 'POST', data }),
  createBalance: (orderNo) => request({ url: `/api/wx/presales/${orderNo}/balance-orders`, method: 'POST' }),
  refund: (orderNo) => request({ url: `/api/wx/presales/${orderNo}/refund`, method: 'POST' }),
  updateAddress: (orderNo, addressId) => request({ url: `/api/wx/presales/${orderNo}/address`, method: 'POST', data: { addressId } }),
}
