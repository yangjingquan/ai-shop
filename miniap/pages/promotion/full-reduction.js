const promotionApi = require('../../api/promotion')
const marketingCapabilities = require('../../utils/marketing-capabilities')

Page({
  data: { activities: [], loading: true },
  onShow() {
    marketingCapabilities.ensure('FULL_REDUCTION').then((enabled) => {
      if (!enabled) return wx.navigateBack()
      promotionApi.active().then((res) => {
        const activities = (res.data || []).map((item) => ({
          ...item,
          rules: (item.thresholds || []).map((tier) => item.activityType === 'FULL_DISCOUNT'
            ? `满${Number(tier.thresholdAmount).toFixed(0)}享${tier.discountRate}折${tier.discountCap ? `，最多优惠¥${tier.discountCap}` : ''}`
            : `满${Number(tier.thresholdAmount).toFixed(0)}减${Number(tier.reductionAmount).toFixed(0)}`),
        }))
        this.setData({ activities, loading: false })
      }).catch(() => this.setData({ loading: false }))
    })
  },
  goCart() { wx.switchTab({ url: '/pages/cart/index' }) },
})
