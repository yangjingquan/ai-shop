const { request } = require('../utils/request')

module.exports = {
  products: (params) => request({ url: '/api/wx/group-buy/products', data: params }),
  productDetail: (productId) => request({ url: `/api/wx/group-buy/products/${productId}` }),
  open: (data) => request({ url: '/api/wx/group-buy/groups', method: 'POST', data }),
  join: (groupId, data) => request({ url: `/api/wx/group-buy/groups/${groupId}/join`, method: 'POST', data }),
  group: (groupId) => request({ url: `/api/wx/group-buy/groups/${groupId}` }),
  subscribeConfig: (groupId) => request({ url: `/api/wx/group-buy/groups/${groupId}/subscribe-config` }),
  subscribe: (data) => request({ url: '/api/wx/group-buy/subscribe', method: 'POST', data }),
  shareEvent: (groupId, source, opened) => request({
    url: '/api/wx/group-buy/share-events',
    method: 'POST',
    data: { groupId, source, opened: !!opened },
  }),
}
