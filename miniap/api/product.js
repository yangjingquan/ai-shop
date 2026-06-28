const { request } = require('../utils/request')

module.exports = {
  page: (params) => request({ url: '/api/public/products/page', data: params }),
  get: (id) => request({ url: `/api/public/products/${id}` }),
}
