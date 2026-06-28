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
        productApi.page({ page: 1, size: 20 }).catch(() => ({ data: { list: [] } })),
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
      const list = ((pageRes && pageRes.data && pageRes.data.list) || []).map((p) => ({
        id: p.id,
        name: p.name,
        mainImage: resolveImageUrl(p.mainImage || ''),
        minPrice: this.fmtPrice(p.minPrice),
      }))
      this.setData({ banners, topCategories: top, products: list })
    } finally {
      this.setData({ loading: false })
    }
  },

  fmtPrice(v) {
    const n = Number(v || 0)
    return n.toFixed(2)
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value })
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
    const id = e.currentTarget.dataset.id
    wx.switchTab({
      url: '/pages/category/index',
      success: () => {
        // 把选中的一级分类带过去（通过全局 storage）
        wx.setStorageSync('home_jump_category_id', id)
      },
    })
  },

  onProduct(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },

  onPullDownRefresh() {
    this.loadAll().finally(() => wx.stopPullDownRefresh())
  },
})
