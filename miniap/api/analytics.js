const { request } = require('../utils/request')

module.exports = {
  productView: (productId, eventId, channelCode) => request({
    url: '/api/wx/analytics/product-view', method: 'POST', data: { productId, eventId, channelCode }, timeout: 5000,
  }),
}
