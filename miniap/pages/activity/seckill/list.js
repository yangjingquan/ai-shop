const seckillApi = require('../../../api/seckill')
const { resolveImageUrl } = require('../../../utils/url')
const marketingCapabilities = require('../../../utils/marketing-capabilities')

Page({
  data: {
    sessions: [],
    activeIndex: 0,
    activeSession: null,
    loading: false,
    now: Date.now(),
    groupBuyEnabled: false,
  },

  onLoad() {
    marketingCapabilities.ensure('SECKILL').then((enabled) => {
      if (enabled) this.loadSessions()
      else wx.switchTab({ url: '/pages/home/index' })
    })
  },

  onUnload() {
    if (this._timer) clearInterval(this._timer)
  },

  loadSessions() {
    this.setData({ loading: true })
    return seckillApi.sessions().then((res) => {
      const sessions = ((res && res.data) || []).map((session) => this.normalizeSession(session))
      this.setData({
        sessions,
        activeIndex: 0,
        activeSession: sessions[0] ? this.withCountdown(sessions[0]) : null,
        loading: false,
      })
      if (this._timer) clearInterval(this._timer)
      if (sessions.length) this._timer = setInterval(() => this.updateCountdown(), 1000)
    }).catch(() => {
      this.setData({ sessions: [], activeSession: null, loading: false })
    })
  },

  normalizeSession(session) {
    const products = (session.products || []).map((product) => ({
      ...product,
      mainImage: resolveImageUrl(product.mainImage || ''),
      activityPriceText: this.fmtPrice(product.activityPrice),
      originalPriceText: this.fmtPrice(product.originalPrice),
      remainingText: `仅剩 ${product.remainingStock || 0} 件`,
      stockPercent: product.activityStock > 0
        ? Math.max(0, Math.min(100, Math.round((product.remainingStock || 0) / product.activityStock * 100)))
        : 0,
    }))
    return {
      ...session,
      startText: this.formatTime(session.startAt),
      endText: this.formatTime(session.endAt),
      products,
    }
  },

  fmtPrice(value) {
    return Number(value || 0).toFixed(2)
  },

  formatTime(value) {
    const date = typeof value === 'number' || /^\d+$/.test(String(value || ''))
      ? new Date(Number(value || 0))
      : new Date(String(value || '').replace(' ', 'T'))
    if (!Number.isFinite(date.getTime())) return ''
    const pad = (n) => String(n).padStart(2, '0')
    return `${pad(date.getHours())}:${pad(date.getMinutes())}`
  },

  countdown(session) {
    if (!session) return ''
    const parseTime = (value) => typeof value === 'number' || /^\d+$/.test(String(value || ''))
      ? Number(value || 0)
      : new Date(String(value || '').replace(' ', 'T')).getTime()
    const target = session.status === 0 ? parseTime(session.startAt) : parseTime(session.endAt)
    let remain = Math.max(0, Math.floor((target - this.data.now) / 1000))
    const pad = (n) => String(n).padStart(2, '0')
    const days = Math.floor(remain / 86400)
    remain -= days * 86400
    const h = Math.floor(remain / 3600)
    remain -= h * 3600
    const m = Math.floor(remain / 60)
    const s = remain % 60
    const totalHours = Math.floor(Math.max(0, target - this.data.now) / 1000 / 3600)
    if (totalHours > 99) {
      return `${days}天 ${pad(h)}:${pad(m)}:${pad(s)}`
    }
    return `${pad(h)}:${pad(m)}:${pad(s)}`
  },

  withCountdown(session) {
    return { ...session, countdownText: this.countdown(session) }
  },

  updateCountdown() {
    const session = this.data.activeSession
    if (session) this.setData({ now: Date.now(), activeSession: this.withCountdown(session) })
  },

  selectSession(e) {
    const index = Number(e.currentTarget.dataset.index)
    const session = this.data.sessions[index]
    if (!session) return
    this.setData({ activeIndex: index, activeSession: this.withCountdown(session) })
  },

  openProduct(e) {
    const item = e.currentTarget.dataset.item
    const session = this.data.activeSession
    if (!item || !session) return
    if (item.status === 0) {
      wx.showToast({ title: `将于${session.startText}开始`, icon: 'none' })
      return
    }
    if (item.status === 2 || item.status === 3) {
      wx.showToast({ title: item.statusText || '活动已结束', icon: 'none' })
      return
    }
    wx.navigateTo({
      url: `/pages/product/detail?id=${item.productId}&activity=seckill&sessionId=${session.id}&seckillSkuId=${item.seckillSkuId}&skuId=${item.skuId}`,
    })
  },
})
