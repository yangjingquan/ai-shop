const { request } = require('../utils/request')

module.exports = {
  get: () => request({ url: '/api/public/home' }),
}
