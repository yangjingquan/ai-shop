const { request } = require('../utils/request')

module.exports = {
  active: () => request({ url: '/api/wx/promotions/active' }),
  cartProgress: (cartItemIds) => request({ url: '/api/wx/promotions/cart-progress', method: 'POST', data: { cartItemIds } }),
}
