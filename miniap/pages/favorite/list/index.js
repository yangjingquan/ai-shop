const engagementApi = require('../../../api/engagement')
const { resolveImageUrl } = require('../../../utils/url')

Page({
  data: { items: [], page: 1, size: 10, total: 0, loading: false, loadingMore: false, hasMore: true, removingId: null, loadError: false },

  onShow() { this.load(true) },
  onPullDownRefresh() { this.load(true).finally(() => wx.stopPullDownRefresh()) },
  onReachBottom() { this.load(false) },

  async load(reset) {
    if (this.data.loading || (!reset && (!this.data.hasMore || this.data.loadingMore))) return
    const page = reset ? 1 : this.data.page + 1
    this.setData(reset ? { loading: true, loadError: false } : { loadingMore: true })
    try {
      const res = await engagementApi.favorites({ page, size: this.data.size })
      const result = (res && res.data) || {}
      const incoming = (result.list || []).map((item) => this.formatItem(item))
      const items = reset ? incoming : this.data.items.concat(incoming)
      const total = Number(result.total || 0)
      this.setData({ items, total, page: Number(result.pageNum || page), hasMore: items.length < total })
    } catch (_) {
      if (reset) this.setData({ items: [], total: 0, hasMore: false, loadError: true })
      else wx.showToast({ title: '加载更多失败，请重试', icon: 'none' })
    } finally {
      this.setData({ loading: false, loadingMore: false })
    }
  },

  formatItem(item) {
    return { ...item, mainImage: resolveImageUrl(item.mainImage || ''), priceText: Number(item.minPrice || 0).toFixed(2), savedAtText: this.formatTime(item.createdAt), initial: (item.name || '商').slice(0, 1) }
  },

  formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '' },

  openProduct(e) {
    const { id, available } = e.currentTarget.dataset
    if (!available) { wx.showToast({ title: '该商品已下架', icon: 'none' }); return }
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },

  removeFavorite(e) {
    const id = Number(e.currentTarget.dataset.id || 0)
    if (!id || this.data.removingId) return
    wx.showModal({ title: '取消收藏', content: '取消后可在商品详情再次收藏。', success: async (result) => {
      if (!result.confirm) return
      this.setData({ removingId: id })
      try {
        await engagementApi.favorite(id, false)
        wx.showToast({ title: '已取消收藏', icon: 'success' })
        await this.load(true)
      } catch (_) {
        wx.showToast({ title: '取消收藏失败，请重试', icon: 'none' })
      } finally {
        this.setData({ removingId: null })
      }
    } })
  },

  goShopping() { wx.switchTab({ url: '/pages/home/index' }) },
  retryLoad() { return this.load(true) },
})
