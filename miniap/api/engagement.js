const { request } = require('../utils/request')
module.exports = {
  detail: (productId) => request({ url: `/api/wx/product-engagement/${productId}` }),
  history: (productId) => request({ url: `/api/wx/product-engagement/${productId}/history`, method: 'POST' }),
  favorite: (productId, value) => request({ url: `/api/wx/product-engagement/${productId}/favorite`, method: value ? 'POST' : 'DELETE' }),
  favorites: (params) => request({ url: '/api/wx/product-engagement/favorites', data: params }),
  histories: (params) => request({ url: '/api/wx/product-engagement/history', data: params }),
  removeHistory: (productId) => request({ url: `/api/wx/product-engagement/history/${productId}`, method: 'DELETE' }),
  clearHistory: () => request({ url: '/api/wx/product-engagement/history', method: 'DELETE' }),
  review: (data) => request({ url: '/api/wx/product-engagement/reviews', method: 'POST', data }),
  question: (data) => request({ url: '/api/wx/product-engagement/questions', method: 'POST', data }),
  myReviews: (data) => request({ url: '/api/wx/product-engagement/my-reviews', data }),
}
