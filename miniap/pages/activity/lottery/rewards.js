const auth = require('../../../utils/auth')
const lotteryApi = require('../../../api/lottery')
const marketingCapabilities = require('../../../utils/marketing-capabilities')

Page({
  data: { rewards: [], loading: true },
  onShow() {
    marketingCapabilities.ensure('LOTTERY_BLIND_BOX').then((enabled) => {
      if (!enabled) return wx.navigateBack()
      return auth.silentLogin().then((loginData) => {
        if (!loginData || !loginData.token) return
        return lotteryApi.rewards().then((res) => this.setData({ rewards: res && res.data || [] }))
      })
    }).catch(() => {}).finally(() => this.setData({ loading: false }))
    const addressId = wx.getStorageSync('lottery_selected_address_id')
    if (addressId) {
      wx.removeStorageSync('lottery_selected_address_id')
      this.saveAddress(this._pendingRewardId, addressId)
    }
  },
  fillAddress(e) {
    const rewardId = Number(e.currentTarget.dataset.id)
    this._pendingRewardId = rewardId
    wx.navigateTo({ url: '/pages/address/list?select=lottery' })
  },
  saveAddress(rewardId, addressId) {
    if (!rewardId || !addressId) return
    lotteryApi.saveAddress(rewardId, addressId).then(() => {
      wx.showToast({ title: '收货地址已提交' })
      this.loadRewards()
    }).catch(() => {})
  },
  loadRewards() {
    lotteryApi.rewards().then((res) => this.setData({ rewards: res && res.data || [] })).catch(() => {})
  },
  goOrder(e) {
    const orderNo = e.currentTarget.dataset.orderNo
    if (orderNo) wx.navigateTo({ url: `/pages/order/detail?orderNo=${orderNo}` })
  },
  useCoupon() { wx.navigateTo({ url: '/pages/coupon/list' }) },
  usePoints() { wx.navigateTo({ url: '/pages/points/index' }) },
  goHome() { wx.switchTab({ url: '/pages/home/index' }) },
})
