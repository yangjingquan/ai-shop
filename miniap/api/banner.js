const { request } = require('../utils/request')

module.exports = {
  list: () => request({ url: '/api/public/banner/list' }),
}
