const { request } = require('../utils/request')

module.exports = {
  sessions: () => request({ url: '/api/wx/seckill/sessions' }),
  session: (sessionId) => request({ url: `/api/wx/seckill/sessions/${sessionId}` }),
  product: (productId, sessionId, seckillSkuId) => request({
    url: `/api/wx/seckill/products/${productId}`,
    data: { sessionId, seckillSkuId },
  }),
  preview: (data) => request({ url: '/api/wx/seckill/orders/preview', method: 'POST', data }),
  createOrder: (data) => request({ url: '/api/wx/seckill/orders', method: 'POST', data }),
}
