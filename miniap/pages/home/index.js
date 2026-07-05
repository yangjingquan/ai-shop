const categoryApi = require('../../api/category')
const productApi = require('../../api/product')
const bannerApi = require('../../api/banner')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    banners: [],
    keyword: '',
    topCategories: [],
    products: [],
    loading: false,
  },

  onLoad() {
    this.loadAll()
  },

  async loadAll() {
    this.setData({ loading: true })
    try {
      const [bannerRes, treeRes, pageRes] = await Promise.all([
        bannerApi.list().catch(() => ({ data: [] })),
        categoryApi.tree().catch(() => ({ data: [] })),
        this.fetchProducts(),
      ])
      const banners = ((bannerRes && bannerRes.data) || []).map((b) => ({
        ...b,
        imageUrl: resolveImageUrl(b.imageUrl || ''),
      }))
      const tree = (treeRes && treeRes.data) || []
      const top = tree.slice(0, 5).map((c, idx) => ({
        id: c.id,
        name: c.name,
        icon: resolveImageUrl(c.icon || ''),
        symbol: this.categorySymbol(idx),
        tone: `tone-${(idx % 5) + 1}`,
      }))
      const list = this.normalizeProducts(pageRes)
      this.setData({ banners, topCategories: top, products: list })
    } finally {
      this.setData({ loading: false })
    }
  },

  async fetchProducts(keyword) {
    const params = { page: 1, size: 12 }
    const query = (keyword || '').trim()
    if (query) {
      params.keyword = query
      return productApi.page(params).catch(() => ({ data: { list: [] } }))
    }
    return productApi.page({ ...params, isRecommend: 1 }).catch(() => ({ data: { list: [] } }))
  },

  normalizeProducts(pageRes) {
    return ((pageRes && pageRes.data && pageRes.data.list) || []).map((p, idx) => ({
      id: p.id,
      name: p.name,
      subtitle: p.subtitle || p.categoryName || '精选好物',
      mainImage: resolveImageUrl(p.mainImage || ''),
      hasImage: !!p.mainImage,
      visualType: ['phone', 'watch', 'audio', 'bag'][idx % 4],
      cardTone: `rec-tone-${(idx % 4) + 1}`,
      minPrice: this.fmtPrice(p.minPrice),
      salePriceText: this.fmtPrice(p.minPrice),
      originalPriceText: this.fmtPrice(this.minPositivePrice(p.minOriginalPrice, p.maxOriginalPrice, p.originalPrice)),
      hasOriginalPrice: this.hasOriginalPrice(p.minOriginalPrice, p.maxOriginalPrice, p.originalPrice),
    }))
  },

  categorySymbol(idx) {
    return ['◒', '◍', '◈', '◎', '✦'][idx % 5]
  },

  fmtPrice(v) {
    const n = Number(v || 0)
    return n.toFixed(2)
  },

  minPositivePrice(...values) {
    const nums = values.map((v) => Number(v || 0)).filter((n) => n > 0)
    return nums.length ? Math.min(...nums) : 0
  },

  hasOriginalPrice(...values) {
    return this.minPositivePrice(...values) > 0
  },

  onSearchInput(e) {
    const keyword = e.detail.value
    this.setData({ keyword })
    if (!keyword.trim()) {
      this.loadProducts('')
    }
  },

  async onSearch() {
    await this.loadProducts(this.data.keyword)
  },

  async loadProducts(keyword) {
    this.setData({ loading: true })
    try {
      const pageRes = await this.fetchProducts(keyword)
      this.setData({
        keyword: (keyword || '').trim(),
        products: this.normalizeProducts(pageRes),
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  onBannerTap(e) {
    const banner = e.currentTarget.dataset.banner
    if (!banner || Number(banner.linkType) === 0 || !banner.linkValue) return
    const linkType = Number(banner.linkType)
    const linkValue = banner.linkValue
    if (linkType === 1) {
      const tabPages = ['/pages/home/index', '/pages/category/index', '/pages/cart/index', '/pages/order/list', '/pages/my/index']
      const targetPath = linkValue.split('?')[0]
      if (tabPages.includes(targetPath)) {
        wx.switchTab({ url: targetPath })
      } else {
        wx.navigateTo({ url: linkValue })
      }
      return
    }
    if (linkType === 3) {
      wx.navigateTo({ url: `/pages/webview/index?url=${encodeURIComponent(linkValue)}` })
    }
  },

  onCategory(e) {
    const id = Number(e.currentTarget.dataset.id)
    wx.setStorageSync('home_jump_category_id', id)
    wx.switchTab({ url: '/pages/category/index' })
  },

  onMoreRecommend() {
    wx.navigateTo({ url: '/pages/recommend/index' })
  },

  onProduct(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },

  onPullDownRefresh() {
    this.loadAll().finally(() => wx.stopPullDownRefresh())
  },
})
