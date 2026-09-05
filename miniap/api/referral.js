const { request } = require('../utils/request')

module.exports = {
  current: (token) => request({ url: '/api/wx/referrals/current', data: token ? { token } : {} }),
  campaign: (campaignId, token) => request({ url: `/api/wx/referrals/${campaignId}`, data: token ? { token } : {} }),
  share: (campaignId) => request({ url: `/api/wx/referrals/${campaignId}/share`, method: 'POST' }),
  bind: (campaignId, token) => request({ url: `/api/wx/referrals/${campaignId}/bind`, method: 'POST', data: { token } }),
  claim: (campaignId) => request({ url: `/api/wx/referrals/${campaignId}/claim`, method: 'POST' }),
  rewards: (campaignId) => request({ url: `/api/wx/referrals/${campaignId}/rewards` }),
}
