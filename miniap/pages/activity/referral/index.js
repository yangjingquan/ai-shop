const auth = require('../../../utils/auth')
const referralApi = require('../../../api/referral')
const marketingCapabilities = require('../../../utils/marketing-capabilities')
const { resolveImageUrl } = require('../../../utils/url')

Page({
  data: {
    campaignId: 0,
    token: '',
    campaign: null,
    loading: true,
    binding: false,
    claiming: false,
    isInvitee: false,
    oldUser: false,
  },

  onLoad(options) {
    this.setData({ campaignId: Number(options.campaignId || 0), token: options.token || '' })
    marketingCapabilities.ensure('REFERRAL').then((enabled) => {
      if (!enabled) return wx.switchTab({ url: '/pages/home/index' })
      return auth.silentLogin().then(() => this.load())
    }).catch(() => this.setData({ loading: false }))
  },

  load() {
    const request = this.data.campaignId
      ? referralApi.campaign(this.data.campaignId, this.data.token)
      : referralApi.current(this.data.token)
    return request.then((res) => {
      const campaign = this.normalize(res && res.data)
      this.setData({ campaign, campaignId: campaign.id || this.data.campaignId, isInvitee: !!campaign.invitee, oldUser: !!campaign.oldUser, loading: false })
      if (this.data.token && campaign.invitee && !campaign.oldUser && !campaign.inviteeCouponId) return this.bindInvitee()
      if (!this.data.token && !campaign.invitee && !campaign.shareToken && campaign.id) {
        return referralApi.share(campaign.id).then((shareRes) => {
          if (shareRes && shareRes.data && shareRes.data.token) this.setData({ 'campaign.shareToken': shareRes.data.token })
        }).catch(() => {})
      }
      return null
    }).catch(() => this.setData({ loading: false, campaign: null }))
  },

  bindInvitee() {
    if (this.data.binding || !this.data.token || !this.data.campaignId) return Promise.resolve()
    this.setData({ binding: true })
    return referralApi.bind(this.data.campaignId, this.data.token).then((res) => {
      const campaign = this.normalize(res && res.data)
      this.setData({ campaign, isInvitee: true, oldUser: !!campaign.oldUser })
    }).catch(() => {}).finally(() => this.setData({ binding: false }))
  },

  normalize(campaign) {
    if (!campaign) return null
    return { ...campaign, landingProductImage: resolveImageUrl(campaign.landingProductImage || ''), tiers: (campaign.tiers || []).map((item) => ({ ...item, couponAmountText: Number(item.couponAmount || 0).toFixed(2) })) }
  },

  recordShare() {
    if (this.data.campaignId) referralApi.share(this.data.campaignId).then((res) => {
      if (res && res.data && res.data.token) this.setData({ 'campaign.shareToken': res.data.token })
    }).catch(() => {})
  },

  onShareAppMessage() {
    const campaign = this.data.campaign || {}
    const token = campaign.shareToken || this.data.token || ''
    return {
      title: campaign.shareTitle || '邀请好友，双方都能得券',
      path: `/pages/activity/referral/index?campaignId=${this.data.campaignId}&token=${token}`,
    }
  },

  claimCoupon() {
    if (this.data.claiming || !this.data.campaignId) return
    this.setData({ claiming: true })
    referralApi.claim(this.data.campaignId).then((res) => {
      const couponId = res && res.data
      wx.showToast({ title: '新人券已领取', icon: 'success' })
      this.setData({ 'campaign.canClaimInviteeCoupon': false, 'campaign.inviteeCouponId': couponId })
      setTimeout(() => this.goLanding(), 450)
    }).catch(() => {}).finally(() => this.setData({ claiming: false }))
  },

  goLanding() {
    const campaign = this.data.campaign || {}
    if (campaign.landingProductId) return wx.navigateTo({ url: `/pages/product/detail?id=${campaign.landingProductId}` })
    wx.switchTab({ url: '/pages/home/index' })
  },

  goRewards() {
    wx.navigateTo({ url: `/pages/activity/referral/rewards?campaignId=${this.data.campaignId}` })
  },

  goHome() { wx.switchTab({ url: '/pages/home/index' }) },
})
