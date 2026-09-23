const { request } = require('../utils/request')

module.exports = {
  get: () => request({ url: '/api/public/home' }),
  topic: (slug) => request({ url: `/api/public/store-pages/${encodeURIComponent(slug)}` }),
}
