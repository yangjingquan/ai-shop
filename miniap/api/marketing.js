const { request } = require('../utils/request')

module.exports = {
  features: () => request({ url: '/api/public/marketing/features' }),
}
