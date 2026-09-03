const groupBuyApi = require('../../api/group-buy')
const { resolveImageUrl } = require('../../utils/url')
const marketingCapabilities = require('../../utils/marketing-capabilities')

Page({
  data: {
    keyword: '',
    products: [],
    page: 1,
    hasMore: true,
    loading: false,
  },

  onLoad() {
    marketingCapabilities.ensure('GROUP_BUY').then((enabled) => {
      if (enabled) this.refresh()
      else wx.switchTab({ url: '/pages/home/index' })
    })
  },

  onPullDownRefresh() {
    this.refresh().finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return
    this.setData({ page: this.data.page + 1 })
    this.load()
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearch() {
    this.refresh()
  },

  onClearSearch() {
    this.setData({ keyword: '' })
    this.refresh()
  },

  refresh() {
    this.setData({ page: 1, products: [], hasMore: true })
    return this.load()
  },

  load() {
    if (this.data.loading) return Promise.resolve()
    this.setData({ loading: true })
    const params = { page: this.data.page, size: 10 }
    if (this.data.keyword.trim()) params.keyword = this.data.keyword.trim()
    return groupBuyApi.products(params).then((res) => {
      const list = ((res.data && res.data.list) || []).map((item) => ({
        ...item,
        mainImage: resolveImageUrl(item.mainImage || ''),
        groupBuyPriceText: Number(item.groupBuyPrice || 0).toFixed(2),
      }))
      this.setData({
        products: this.data.page === 1 ? list : this.data.products.concat(list),
        hasMore: list.length === 10,
      })
    }).finally(() => this.setData({ loading: false }))
  },

  goProduct(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: `/pages/product/detail?id=${id}&groupBuy=1` })
  },
})
