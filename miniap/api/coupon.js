const { request } = require('../utils/request')

module.exports = {
  list: (status) => request({ url: '/api/wx/coupons', data: status === undefined ? {} : { status } }),
  eligibility: () => request({ url: '/api/wx/coupons/new-user/eligibility' }),
  receive: (templateId) => request({ url: `/api/wx/coupons/${templateId}/receive`, method: 'POST' }),
}
