const { request } = require('../utils/request')

module.exports = {
  list: () => request({ url: '/api/wx/addresses' }),
  get: (id) => request({ url: `/api/wx/addresses/${id}` }),
  create: (data) => request({ url: '/api/wx/addresses', method: 'POST', data }),
  update: (id, data) => request({ url: `/api/wx/addresses/${id}`, method: 'PUT', data }),
  remove: (id) => request({ url: `/api/wx/addresses/${id}`, method: 'DELETE' }),
  setDefault: (id) => request({ url: `/api/wx/addresses/${id}/default`, method: 'PUT' }),
}
