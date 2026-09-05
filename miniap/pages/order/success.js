const auth = require('../../utils/auth')
const referralApi = require('../../api/referral')
const marketingCapabilities = require('../../utils/marketing-capabilities')

Page({
  data: { orderNo: '', groupId: 0, campaign: null, loading: true },
  onLoad(options) {
    this.setData({ orderNo: options.orderNo || '', groupId: Number(options.groupId || 0) })
    marketingCapabilities.load(false).then(() => {
      if (!marketingCapabilities.isEnabled('REFERRAL')) return this.setData({ loading: false })
      return auth.silentLogin().then(() => referralApi.current().then((res) => this.setData({ campaign: res && res.data, loading: false })).catch(() => this.setData({ loading: false })))
    }).catch(() => this.setData({ loading: false }))
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
})
