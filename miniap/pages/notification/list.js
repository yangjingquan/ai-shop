const notificationApi = require('../../api/notification')

Page({
  data: {
    list: [],
    page: 1,
    size: 20,
    total: 0,
    loading: false,
    finished: false,
    unreadCount: 0,
  },

  onShow() {
    this.reload()
  },

  onPullDownRefresh() {
    this.reload().finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (!this.data.loading && !this.data.finished) this.loadPage(this.data.page + 1)
  },

  reload() {
    this.setData({ list: [], page: 1, total: 0, finished: false })
    return Promise.all([this.loadPage(1), this.loadUnreadCount()])
  },

  loadPage(page) {
    if (this.data.loading) return Promise.resolve()
    this.setData({ loading: true })
    return notificationApi.page({ page, size: this.data.size }).then((res) => {
      const result = (res && res.data) || {}
      const rows = (result.list || []).map((item) => ({
        ...item,
        timeText: this.formatTime(item.createdAt),
      }))
      const list = page === 1 ? rows : this.data.list.concat(rows)
      const total = Number(result.total || 0)
      this.setData({ list, page, total, finished: list.length >= total })
    }).finally(() => this.setData({ loading: false }))
  },

  loadUnreadCount() {
    return notificationApi.unreadCount().then((res) => {
      this.setData({ unreadCount: Number(res && res.data && res.data.count || 0) })
    })
  },

  readMessage(e) {
    const id = Number(e.currentTarget.dataset.id || 0)
    const link = e.currentTarget.dataset.link || ''
    const item = this.data.list.find((row) => row.id === id)
    const read = item && item.isRead === 1
    const request = read ? Promise.resolve() : notificationApi.markRead(id)
    request.then(() => {
      const list = this.data.list.map((row) => row.id === id ? { ...row, isRead: 1 } : row)
      this.setData({ list, unreadCount: Math.max(0, this.data.unreadCount - (read ? 0 : 1)) })
      if (link) wx.navigateTo({ url: link })
    })
  },

  readAll() {
    if (!this.data.unreadCount) return
    notificationApi.markAllRead().then(() => {
      this.setData({
        list: this.data.list.map((item) => ({ ...item, isRead: 1 })),
        unreadCount: 0,
      })
      wx.showToast({ title: '已全部标为已读' })
    })
  },

  formatTime(value) {
    if (!value) return ''
    return String(value).replace('T', ' ').slice(0, 16)
  },
})
