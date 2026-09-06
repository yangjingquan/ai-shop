Page({
  data: { result: null },
  onLoad() {
    const result = wx.getStorageSync('lottery_last_result')
    if (!result || !result.prize) {
      wx.showToast({ title: '抽奖结果已失效', icon: 'none' })
      setTimeout(() => wx.switchTab({ url: '/pages/home/index' }), 650)
      return
    }
    const reward = result.reward || {}
    result.resultText = result.prize.prizeType === 'POINTS'
      ? `积分已发放：+${result.prize.pointsAmount || reward.pointsAmount || 0}`
      : result.prize.prizeType === 'COUPON'
        ? '优惠券已到账，可前往卡券包查看'
        : result.prize.prizeType === 'PHYSICAL'
          ? '请填写收货地址，商家将为你安排发货'
          : reward.pointsAmount
            ? `安慰积分已到账：+${reward.pointsAmount}`
            : '感谢参与，下次好运'
    this.setData({ result })
    wx.removeStorageSync('lottery_last_result')
  },
  useReward() {
    const result = this.data.result || {}
    const prize = result.prize || {}
    if (prize.prizeType === 'COUPON') return wx.navigateTo({ url: '/pages/coupon/list' })
    if (prize.prizeType === 'POINTS') return wx.navigateTo({ url: '/pages/points/index' })
    if (prize.prizeType === 'PHYSICAL') return wx.navigateTo({ url: '/pages/activity/lottery/rewards' })
    wx.switchTab({ url: '/pages/home/index' })
  },
  again() { const id = this.data.result && this.data.result.activity && this.data.result.activity.id; if (id) wx.redirectTo({ url: `/pages/activity/lottery/index?id=${id}` }) },
  goRecords() { const id = this.data.result && this.data.result.activity && this.data.result.activity.id; if (id) wx.navigateTo({ url: `/pages/activity/lottery/records?id=${id}` }) },
  goHome() { wx.switchTab({ url: '/pages/home/index' }) },
})
