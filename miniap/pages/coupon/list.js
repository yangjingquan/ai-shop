const couponApi = require('../../api/coupon')

Page({
  data: {
    mode: 'view',
    tabs: [{ key: 0, label: '可使用' }, { key: 1, label: '已使用' }, { key: 2, label: '已失效/过期' }],
    activeTab: 0,
    coupons: [],
    loading: false,
  },

  onLoad(options) {
    this.setData({ mode: options.mode === 'select' ? 'select' : 'view' })
    this.load()
  },

  onPullDownRefresh() { this.load().finally(() => wx.stopPullDownRefresh()) },

  async load() {
    this.setData({ loading: true })
    try {
      const res = await couponApi.list(this.data.activeTab)
      const list = (res && res.data) || []
      this.setData({ coupons: list.map(item => ({ ...item, amountText: Number(item.amount || 0).toFixed(0), thresholdText: Number(item.thresholdAmount || 0).toFixed(0), expiryText: this.expiryText(item.validTo) })) })
    } catch (_) { this.setData({ coupons: [] }) } finally { this.setData({ loading: false }) }
  },

  changeTab(e) {
    const key = Number(e.currentTarget.dataset.key)
    this.setData({ activeTab: key })
    this.load()
  },

  selectCoupon(e) {
    const coupon = e.currentTarget.dataset.coupon
    if (!coupon || !coupon.available) {
      if (coupon && coupon.unavailableReason) wx.showToast({ title: coupon.unavailableReason, icon: 'none' })
      return
    }
    if (this.data.mode === 'select') {
      wx.setStorageSync('order_selected_coupon_id', coupon.id)
      wx.navigateBack({ delta: 1 })
      return
    }
    wx.setStorageSync('order_selected_coupon_id', coupon.id)
    wx.navigateTo({ url: '/pages/recommend/index' })
  },
  expiryText(validTo) {
    const ms = new Date(validTo).getTime() - Date.now()
    const days = Math.ceil(ms / (24 * 60 * 60 * 1000))
    return days > 0 && days <= 1 ? '仅剩 1 天有效' : ''
  },
})
