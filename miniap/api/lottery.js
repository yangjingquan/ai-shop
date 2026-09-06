const { request } = require('../utils/request')

module.exports = {
  current: () => request({ url: '/api/wx/lottery/current' }),
  activity: (id) => request({ url: `/api/wx/lottery/${id}` }),
  draw: (id, idempotencyKey) => request({
    url: `/api/wx/lottery/${id}/draw`,
    method: 'POST',
    data: { idempotencyKey },
  }),
  records: (id) => request({ url: `/api/wx/lottery/${id}/records` }),
  rewards: () => request({ url: '/api/wx/lottery/rewards' }),
  saveAddress: (rewardId, addressId) => request({
    url: `/api/wx/lottery/rewards/${rewardId}/address`,
    method: 'POST',
    data: { addressId },
  }),
}
