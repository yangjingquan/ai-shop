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
      const top = tree.slice(0, 8).map((c) => ({
        id: c.id,
        name: c.name,
        icon: resolveImageUrl(c.icon || ''),
      }))
      const list = this.normalizeProducts(pageRes)
      this.setData({ banners, topCategories: top, products: list })
    } finally {
      this.setData({ loading: false })
    }
  },

  async fetchProducts(keyword) {
    const params = { page: 1, size: 20 }
    const query = (keyword || '').trim()
    if (query) {
      params.keyword = query
      return productApi.page(params).catch(() => ({ data: { list: [] } }))
    }

    const recommendRes = await productApi.page({ ...params, isRecommend: 1 }).catch(() => ({ data: { list: [] } }))
    const recommendList = (recommendRes && recommendRes.data && recommendRes.data.list) || []
    if (recommendList.length) return recommendRes
    return productApi.page(params).catch(() => ({ data: { list: [] } }))
  },

  normalizeProducts(pageRes) {
    return ((pageRes && pageRes.data && pageRes.data.list) || []).map((p) => ({
      id: p.id,
      name: p.name,
      mainImage: resolveImageUrl(p.mainImage || ''),
      minPrice: this.fmtPrice(p.minPrice),
      salePriceText: this.fmtPrice(p.minPrice),
      originalPriceText: this.fmtPrice(this.minPositivePrice(p.minOriginalPrice, p.maxOriginalPrice, p.originalPrice)),
      hasOriginalPrice: this.hasOriginalPrice(p.minOriginalPrice, p.maxOriginalPrice, p.originalPrice),
    }))
  },

  fmtPrice(v) {
    const n = Number(v || 0)
    return n.toFixed(2)
  },

  fmtRange(min, max) {
    const a = Number(min || 0)
    const b = Number(max || 0)
    if (!a && !b) return ''
    if (!b || a === b) return a.toFixed(2)
    if (!a) return b.toFixed(2)
    return `${a.toFixed(2)} - ${b.toFixed(2)}`
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

  onProduct(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },

  onPullDownRefresh() {
    this.loadAll().finally(() => wx.stopPullDownRefresh())
  },
})
