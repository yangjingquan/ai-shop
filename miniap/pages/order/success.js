const auth = require('../../utils/auth')
const referralApi = require('../../api/referral')
const marketingCapabilities = require('../../utils/marketing-capabilities')
const pointsApi = require('../../api/points')

Page({
  data: { orderNo: '', groupId: 0, campaign: null, pointsEnabled: false, pointsEarned: 0, loading: true },
  onLoad(options) {
    this.setData({ orderNo: options.orderNo || '', groupId: Number(options.groupId || 0) })
    marketingCapabilities.load(false).then(() => {
      const referralEnabled = marketingCapabilities.isEnabled('REFERRAL')
      const pointsEnabled = marketingCapabilities.isEnabled('POINTS_MEMBER_DAY')
      this.setData({ pointsEnabled })
      if (!referralEnabled && !pointsEnabled) return
      return auth.silentLogin().then(() => Promise.all([
        referralEnabled ? referralApi.current().then((res) => this.setData({ campaign: res && res.data })).catch(() => {}) : Promise.resolve(),
        pointsEnabled && this.data.orderNo ? pointsApi.ledger(100).then((res) => {
          const ledger = ((res && res.data) || []).find(item => item.source === 'ORDER_PAY' && item.businessNo === this.data.orderNo)
          this.setData({ pointsEarned: Math.max(0, Number(ledger && ledger.changeValue || 0)) })
        }).catch(() => {}) : Promise.resolve(),
      ]))
    }).catch(() => {}).finally(() => this.setData({ loading: false }))
  },
  goInvite() {
    if (!this.data.campaign || !this.data.campaign.id) return
    wx.navigateTo({ url: `/pages/activity/referral/index?campaignId=${this.data.campaign.id}` })
  },
  goOrder() {
    if (this.data.orderNo) return wx.redirectTo({ url: `/pages/order/detail?orderNo=${this.data.orderNo}` })
    wx.switchTab({ url: '/pages/order/list' })
  },
  goGroup() {
    if (this.data.groupId) return wx.redirectTo({ url: `/pages/group-buy/group?groupId=${this.data.groupId}` })
    this.goOrder()
  },
  goHome() { wx.switchTab({ url: '/pages/home/index' }) },
  goPointsMall() { wx.navigateTo({ url: '/pages/points/mall' }) },
})
