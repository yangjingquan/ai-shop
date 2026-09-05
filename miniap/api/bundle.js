const request = require('../utils/request')

module.exports = {
  product: (productId) => request({ url: `/api/wx/bundles/product/${productId}` }),
  get: (bundleId) => request({ url: `/api/wx/bundles/${bundleId}` }),
  preview: (data) => request({ url: '/api/wx/bundles/preview', method: 'POST', data }),
  addToCart: (data) => request({ url: '/api/wx/bundles/cart', method: 'POST', data }),
}
