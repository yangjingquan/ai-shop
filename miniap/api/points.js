const { request } = require('../utils/request')
module.exports = {
  profile: () => request({ url: '/api/wx/points/profile' }),
  ledger: (limit = 30) => request({ url: '/api/wx/points/ledger', data: { limit } }),
  signIn: () => request({ url: '/api/wx/points/sign-in', method: 'POST' }),
  mall: () => request({ url: '/api/wx/points/mall' }),
  redeem: (data) => request({ url: '/api/wx/points/redeem', method: 'POST', data }),
  memberDay: () => request({ url: '/api/wx/points/member-day' }),
  receiveMemberDayCoupon: () => request({ url: '/api/wx/points/member-day/receive-coupon', method: 'POST' }),
}
