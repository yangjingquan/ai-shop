const engagementApi = require('../../../api/engagement')
const { resolveImageUrl } = require('../../../utils/url')

Page({
  data: { items: [], page: 1, size: 10, total: 0, loading: false, loadingMore: false, hasMore: true },

  onShow() { this.load(true) },
  onPullDownRefresh() { this.load(true).finally(() => wx.stopPullDownRefresh()) },
  onReachBottom() { this.load(false) },

  async load(reset) {
    if (this.data.loading || (!reset && (!this.data.hasMore || this.data.loadingMore))) return
    const page = reset ? 1 : this.data.page + 1
    this.setData(reset ? { loading: true } : { loadingMore: true })
    try {
      const res = await engagementApi.histories({ page, size: this.data.size })
      const result = (res && res.data) || {}
      const incoming = (result.list || []).map((item) => this.formatItem(item))
      const items = reset ? incoming : this.data.items.concat(incoming)
      const total = Number(result.total || 0)
      this.setData({ items, total, page: Number(result.pageNum || page), hasMore: items.length < total })
    } catch (_) {
      if (reset) this.setData({ items: [], total: 0, hasMore: false })
    } finally {
      this.setData({ loading: false, loadingMore: false })
    }
  },

  formatItem(item) {
    return { ...item, mainImage: resolveImageUrl(item.mainImage || ''), priceText: Number(item.minPrice || 0).toFixed(2), viewedAtText: this.formatTime(item.lastViewedAt), initial: (item.name || '商').slice(0, 1), viewCountText: Math.max(1, Number(item.viewCount || 1)) }
  },

  formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '' },

  openProduct(e) {
    const { id, available } = e.currentTarget.dataset
    if (!available) { wx.showToast({ title: '该商品已下架', icon: 'none' }); return }
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },

  removeHistory(e) {
    const id = Number(e.currentTarget.dataset.id || 0)
    if (!id) return
    wx.showModal({ title: '删除记录', content: '删除后无法恢复。', success: async (result) => {
      if (!result.confirm) return
      try { await engagementApi.removeHistory(id); wx.showToast({ title: '已删除' }); this.load(true) } catch (_) {}
    } })
  },

  clearHistory() {
    if (!this.data.items.length) return
    wx.showModal({ title: '清空浏览记录', content: '将清空全部浏览记录，且无法恢复。', success: async (result) => {
      if (!result.confirm) return
      try { await engagementApi.clearHistory(); this.setData({ items: [], total: 0, page: 1, hasMore: false }); wx.showToast({ title: '已清空' }) } catch (_) {}
    } })
  },

  goShopping() { wx.switchTab({ url: '/pages/home/index' }) },
})
