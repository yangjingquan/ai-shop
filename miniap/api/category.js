const { request } = require('../utils/request')

module.exports = {
  tree: () => request({ url: '/api/public/categories/tree' }),
}
