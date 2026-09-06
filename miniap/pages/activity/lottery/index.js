const auth = require('../../../utils/auth')
const lotteryApi = require('../../../api/lottery')
const marketingCapabilities = require('../../../utils/marketing-capabilities')

const DRAW_ANIMATION_MS = 1700
const REVEAL_ANIMATION_MS = 450

Page({
  data: { activity: null, activityId: 0, loading: true, drawing: false, drawPhase: 'idle', drawMessage: '', ruleVisible: false },

  onLoad(options) {
    this.setData({ activityId: Number(options && options.id || 0) })
    marketingCapabilities.load(false).then(() => {
      if (marketingCapabilities.isEnabled('LOTTERY_BLIND_BOX')) this.loadActivity()
      else this.goHome('活动已结束/暂未开启')
    }).catch(() => this.goHome('活动已结束/暂未开启'))
  },

  loadActivity() {
    this.setData({ loading: true })
    const request = this.data.activityId ? lotteryApi.activity(this.data.activityId) : lotteryApi.current()
    request.then((res) => {
      const activity = res && res.data
      if (!activity || activity.active === false) {
        this.goHome('活动已结束/暂未开启')
        return
      }
      activity.prizes = (activity.prizes || []).map((prize) => ({
        ...prize,
        probabilityText: `${(Number(prize.probability || 0) * 100).toFixed(2)}%`,
      }))
      this.setData({ activity })
    }).catch(() => this.goHome('活动已结束/暂未开启')).finally(() => this.setData({ loading: false }))
  },

  draw() {
    const activity = this.data.activity
    if (!activity || this.data.drawing) return
    if (Number(activity.remainingChances || 0) <= 0) {
      wx.showToast({ title: '今日抽奖次数已用完', icon: 'none' })
      return
    }
    auth.silentLogin().then((loginData) => {
      if (!loginData || !loginData.token) {
        wx.showToast({ title: '请登录后参与抽奖', icon: 'none' })
        return
      }
      this.setData({ drawing: true, drawPhase: 'drawing', drawMessage: '抽奖中…' })
      const startedAt = Date.now()
      const key = `lottery_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
      return lotteryApi.draw(activity.id, key).then((res) => {
        const waitMs = Math.max(0, DRAW_ANIMATION_MS - (Date.now() - startedAt))
        return new Promise((resolve) => setTimeout(resolve, waitMs)).then(() => {
          this.setData({ drawPhase: 'revealing', drawMessage: '揭晓中…' })
          return new Promise((resolve) => setTimeout(resolve, REVEAL_ANIMATION_MS))
        }).then(() => {
          wx.setStorageSync('lottery_last_result', res && res.data)
          wx.redirectTo({ url: `/pages/activity/lottery/result?id=${activity.id}` })
        })
      }).catch(() => {}).finally(() => this.setData({ drawing: false, drawPhase: 'idle', drawMessage: '' }))
    })
  },

  showRules() { this.setData({ ruleVisible: true }) },
  hideRules() { this.setData({ ruleVisible: false }) },
  noop() {},
  goRewards() { wx.navigateTo({ url: '/pages/activity/lottery/rewards' }) },
  goRecords() { const id = this.data.activity && this.data.activity.id; if (id) wx.navigateTo({ url: `/pages/activity/lottery/records?id=${id}` }) },
  goHome(message) {
    if (message) wx.showToast({ title: message, icon: 'none' })
    setTimeout(() => wx.switchTab({ url: '/pages/home/index' }), 650)
  },
})
