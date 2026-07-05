const productApi = require('../../api/product')
const categoryApi = require('../../api/category')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    products: [],
    groups: [],
    filters: [{ id: 0, name: '全部', active: true }],
    activeCategoryId: 0,
    page: 1,
    size: 8,
    hasMore: true,
    loading: false,
  },

  onLoad() {
    this.loadFilters()
    this.loadProducts(true)
  },

  async loadFilters() {
    const res = await categoryApi.tree().catch(() => ({ data: [] }))
    const cats = ((res && res.data) || []).slice(0, 6).map((c) => ({ id: c.id, name: c.name, active: false }))
    this.setData({ filters: [{ id: 0, name: '全部', active: true }, ...cats] })
  },

  async loadProducts(reset) {
    if (this.data.loading) return
    const page = reset ? 1 : this.data.page
    this.setData({ loading: true })
    try {
      const params = { page, size: this.data.size, isRecommend: 1 }
      if (this.data.activeCategoryId) params.categoryId = this.data.activeCategoryId
      const res = await productApi.page(params).catch(() => ({ data: { list: [], total: 0 } }))
      const data = (res && res.data) || { list: [], total: 0 }
      const next = this.normalizeProducts(data.list || [], reset ? 0 : this.data.products.length)
      const products = reset ? next : this.data.products.concat(next)
      this.setData({
        products,
        groups: this.groupProducts(products),
        page: page + 1,
        hasMore: products.length < Number(data.total || 0),
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  normalizeProducts(list, offset) {
    return list.map((p, idx) => ({
      id: p.id,
      name: p.name,
      subtitle: p.subtitle || p.categoryName || '精选好物 · 品质优选',
      mainImage: resolveImageUrl(p.mainImage || ''),
      hasImage: !!p.mainImage,
      visualType: ['phone', 'watch', 'audio', 'bag'][(offset + idx) % 4],
      minPrice: this.fmtPrice(p.minPrice),
      salePriceText: this.fmtPrice(p.minPrice),
      rank: String(offset + idx + 1).padStart(2, '0'),
    }))
  },

  groupProducts(products) {
    const groups = []
    for (let i = 0; i < products.length; i += 4) {
      const items = products.slice(i, i + 4)
      groups.push({
        id: `g-${i}`,
        feature: items[0] || null,
        picks: items.length >= 3 ? items.slice(1, 3) : [],
        rank: items.length === 2 ? items[1] : items[3] || null,
      })
    }
    return groups
  },

  fmtPrice(v) {
    const n = Number(v || 0)
    return n.toFixed(2)
  },

  onFilter(e) {
    const id = Number(e.currentTarget.dataset.id || 0)
    const filters = this.data.filters.map((f) => ({ ...f, active: f.id === id }))
    this.setData({ activeCategoryId: id, filters, products: [], groups: [], page: 1, hasMore: true })
    this.loadProducts(true)
  },

  onProduct(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },

  onBack() {
    wx.navigateBack({ delta: 1 })
  },

  onReachBottom() {
    if (this.data.hasMore) this.loadProducts(false)
  },

  onPullDownRefresh() {
    this.loadProducts(true).finally(() => wx.stopPullDownRefresh())
  },
})
