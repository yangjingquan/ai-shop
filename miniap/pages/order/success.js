const auth = require('../../utils/auth')
const referralApi = require('../../api/referral')
const marketingCapabilities = require('../../utils/marketing-capabilities')
const pointsApi = require('../../api/points')
const orderApi = require('../../api/order')

Page({
  data: { orderNo: '', groupId: 0, campaign: null, pointsEnabled: false, pointsEarned: 0, repurchaseEnabled: false, repurchaseCoupon: null, loading: true },
  onLoad(options) {
    this.setData({ orderNo: options.orderNo || '', groupId: Number(options.groupId || 0) })
    marketingCapabilities.load(false).then(() => {
      const referralEnabled = marketingCapabilities.isEnabled('REFERRAL')
      const pointsEnabled = marketingCapabilities.isEnabled('POINTS_MEMBER_DAY')
      const repurchaseEnabled = marketingCapabilities.isEnabled('REPURCHASE_COUPON')
      this.setData({ pointsEnabled, repurchaseEnabled })
      if (!referralEnabled && !pointsEnabled && !repurchaseEnabled) return
      return auth.silentLogin().then(() => Promise.all([
        referralEnabled ? referralApi.current().then((res) => this.setData({ campaign: res && res.data })).catch(() => {}) : Promise.resolve(),
        pointsEnabled && this.data.orderNo ? pointsApi.ledger(100).then((res) => {
          const ledger = ((res && res.data) || []).find(item => item.source === 'ORDER_PAY' && item.businessNo === this.data.orderNo)
          this.setData({ pointsEarned: Math.max(0, Number(ledger && ledger.changeValue || 0)) })
        }).catch(() => {}) : Promise.resolve(),
        repurchaseEnabled && this.data.orderNo ? this.loadRepurchaseCoupon() : Promise.resolve(),
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
  formatCoupon(coupon) {
    const validTo = new Date(coupon.validTo)
    const left = validTo.getTime() - Date.now()
    const days = Math.max(0, Math.ceil(left / (24 * 60 * 60 * 1000)))
    return {
      ...coupon,
      amountText: Number(coupon.amount || 0).toFixed(0),
      thresholdText: Number(coupon.thresholdAmount || 0).toFixed(0),
      expiryText: days <= 1 ? '仅剩 1 天有效' : `还有 ${days} 天有效`,
      available: coupon.available === true,
    }
  },
  loadRepurchaseCoupon(retryCount = 0) {
    return orderApi.repurchaseCoupon(this.data.orderNo).then((res) => {
      const coupon = res && res.data
      if (coupon) {
        this.setData({ repurchaseCoupon: this.formatCoupon(coupon) })
        return coupon
      }
      if (retryCount < 3 && this.data.repurchaseEnabled) {
        setTimeout(() => this.loadRepurchaseCoupon(retryCount + 1), 800)
      }
      return null
    }).catch(() => this.setData({ repurchaseCoupon: null }))
  },
  goUseRepurchaseCoupon() {
    const coupon = this.data.repurchaseCoupon
    if (!coupon || !coupon.available) {
      wx.showToast({ title: (coupon && coupon.unavailableReason) || '优惠券暂不可使用', icon: 'none' })
      return
    }
    wx.setStorageSync('order_selected_coupon_id', coupon.id)
    wx.navigateTo({ url: '/pages/recommend/index' })
  },
})
