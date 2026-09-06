const auth = require('../../../utils/auth')
const lotteryApi = require('../../../api/lottery')
const marketingCapabilities = require('../../../utils/marketing-capabilities')

Page({
  data: { activityId: 0, activityName: '', records: [], loading: true },
  onLoad(options) {
    this.setData({ activityId: Number(options && options.id || 0) })
    marketingCapabilities.ensure('LOTTERY_BLIND_BOX').then((enabled) => {
      if (!enabled) return wx.navigateBack()
      return auth.silentLogin().then((loginData) => {
        if (!loginData || !loginData.token || !this.data.activityId) return
        return lotteryApi.records(this.data.activityId).then((res) => {
          const records = (res && res.data || []).map((item) => ({
            ...item,
            typeText: item.prize && item.prize.prizeType === 'CONSOLATION' ? '谢谢参与' : item.prize && item.prize.prizeType === 'POINTS' ? '积分奖励' : item.prize && item.prize.prizeType === 'COUPON' ? '优惠券奖励' : item.prize && item.prize.prizeType === 'PHYSICAL' ? '实物奖励' : '抽奖结果',
          }))
          this.setData({ records, activityName: records[0] && records[0].activity && records[0].activity.name || '' })
        })
      })
    }).catch(() => {}).finally(() => this.setData({ loading: false }))
  },
  goRewards() { wx.navigateTo({ url: '/pages/activity/lottery/rewards' }) },
  goHome() { wx.switchTab({ url: '/pages/home/index' }) },
})
