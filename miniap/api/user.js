const { request } = require('../utils/request')

module.exports = {
  bindPhone: (code) =>
    request({ url: '/api/wx/user/bind-phone', method: 'POST', data: { code } }),
}
