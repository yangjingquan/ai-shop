const auth = require('../../../utils/auth')
const referralApi = require('../../../api/referral')
const marketingCapabilities = require('../../../utils/marketing-capabilities')

Page({
  data: { campaignId: 0, campaign: null, rewards: [], loading: true },
  onLoad(options) {
    this.setData({ campaignId: Number(options.campaignId || 0) })
    marketingCapabilities.ensure('REFERRAL').then((enabled) => {
      if (!enabled) return wx.switchTab({ url: '/pages/home/index' })
      return auth.silentLogin().then(() => this.load())
    }).catch(() => this.setData({ loading: false }))
  },
  load() {
    const campaignRequest = this.data.campaignId ? referralApi.campaign(this.data.campaignId) : referralApi.current()
    return campaignRequest.then((res) => {
      const campaign = res && res.data
      if (!campaign) throw new Error('campaign missing')
      const campaignId = campaign && campaign.id ? campaign.id : this.data.campaignId
      const completed = Number(campaign.completedInviteCount || 0)
      const target = Number(campaign.nextTierInviteCount || completed || 1)
      const nextTierText = campaign.nextTierInviteCount ? `目标 ${campaign.nextTierInviteCount} 人` : '全部达成'
      campaign.progressPercent = Math.min(100, Math.round(completed / target * 100))
      campaign.nextTierText = nextTierText
      return referralApi.rewards(campaignId).then((rewardRes) => this.setData({ campaignId, campaign, rewards: ((rewardRes && rewardRes.data) || []).map((item) => ({ ...item, rewardLabel: item.role === 'INVITEE' ? '新人专享券' : `邀请满${item.tier}人奖励券` })), loading: false }))
    }).catch(() => this.setData({ loading: false }))
  },
  goUse(e) {
    const reward = e.currentTarget.dataset.reward
    if (!reward || !reward.couponId || reward.status !== 1) return
    wx.navigateTo({ url: '/pages/coupon/list' })
  },
  goBack() { wx.navigateBack({ delta: 1 }) },
})
