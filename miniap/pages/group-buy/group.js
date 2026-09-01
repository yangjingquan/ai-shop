const groupBuyApi = require('../../api/group-buy')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    groupId: 0,
    group: null,
    loading: false,
    remainingText: '',
  },
  onLoad(options) {
    const groupId = Number(options.groupId || 0)
    this.setData({ groupId })
    this.load()
  },
  onUnload() {
    this.clearTimer()
  },
  onHide() {
    this.clearTimer()
  },
  load() {
    if (!this.data.groupId) return
    this.setData({ loading: true })
    groupBuyApi.group(this.data.groupId).then((res) => {
      const group = res.data || null
      if (group && Array.isArray(group.members)) {
        group.members = group.members.map((member) => ({
          ...member,
          avatar: resolveImageUrl(member.avatar || ''),
        }))
      }
      this.setData({ group })
      this.startTimer()
    }).finally(() => this.setData({ loading: false }))
  },
  clearTimer() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },
  startTimer() {
    this.clearTimer()
    this.updateRemaining()
    if (!this.data.group || this.data.group.status !== 0) return
    this.timer = setInterval(() => this.updateRemaining(), 1000)
  },
  updateRemaining() {
    const expireAt = Number(this.data.group && this.data.group.expireAt || 0)
    const seconds = Math.max(0, Math.floor((expireAt - Date.now()) / 1000))
    const hours = String(Math.floor(seconds / 3600)).padStart(2, '0')
    const minutes = String(Math.floor((seconds % 3600) / 60)).padStart(2, '0')
    const secs = String(seconds % 60).padStart(2, '0')
    this.setData({ remainingText: `${hours}:${minutes}:${secs}` })
    if (seconds === 0) this.clearTimer()
  },
  goProduct() {
    if (!this.data.group || !this.data.group.productId) return
    wx.navigateTo({ url: `/pages/product/detail?id=${this.data.group.productId}&groupBuy=1&groupId=${this.data.groupId}` })
  },
  goOrders() {
    wx.switchTab({ url: '/pages/order/list' })
  },
  onShareAppMessage() {
    const group = this.data.group || {}
    return {
      title: group.status === 0 ? `还差${group.remainingCount || 0}人，快来一起拼团` : '拼团详情',
      path: `/pages/group-buy/group?groupId=${this.data.groupId}`,
    }
  },
})
