const { request } = require('../utils/request')

module.exports = {
  add: (data) => request({ url: '/api/wx/cart', method: 'POST', data }),
  list: () => request({ url: '/api/wx/cart' }),
  update: (id, data) => request({ url: `/api/wx/cart/${id}`, method: 'PUT', data }),
  remove: (id) => request({ url: `/api/wx/cart/${id}`, method: 'DELETE' }),
}
