const homeApi = require('../../api/home')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: { loading: true, page: null, blocks: [], unavailable: false },

  onLoad(options) {
    this.slug = String((options && options.slug) || '').trim()
    this.loadPage()
  },

  async loadPage() {
    if (!this.slug) return this.setData({ loading: false, unavailable: true })
    this.setData({ loading: true, unavailable: false })
    try {
      const response = await homeApi.topic(this.slug)
      const page = response && response.data
      if (!page || page.status !== 'PUBLISHED' || !page.published) throw new Error('专题不可用')
      const blocks = (page.published.blocks || []).map((block) => ({
        ...block,
        imageUrl: resolveImageUrl(block.imageUrl || ''),
        products: (block.products || []).map((product, index) => ({
          ...product,
          mainImage: resolveImageUrl(product.mainImage || ''),
          visualType: ['phone', 'watch', 'audio', 'bag'][index % 4],
        })),
      }))
      this.setData({ page: { ...page, coverImage: resolveImageUrl(page.coverImage || '') }, blocks })
    } catch (_) {
      this.setData({ page: null, blocks: [], unavailable: true })
    } finally {
      this.setData({ loading: false })
    }
  },

  onProduct(event) {
    const productId = Number(event.currentTarget.dataset.id || 0)
    if (productId > 0) wx.navigateTo({ url: `/pages/product/detail?id=${productId}` })
  },

  onBlockButton(event) {
    const type = String(event.currentTarget.dataset.type || '')
    if (type === 'CATEGORY') return wx.switchTab({ url: '/pages/category/index' })
    if (type === 'COUPON') return wx.navigateTo({ url: '/pages/coupon/list' })
    if (type === 'GROUP_BUY') return wx.navigateTo({ url: '/pages/group-buy/list' })
    wx.switchTab({ url: '/pages/category/index' })
  },

  onPullDownRefresh() {
    this.loadPage().finally(() => wx.stopPullDownRefresh())
  },

  backHome() {
    wx.switchTab({ url: '/pages/home/index' })
  },
})
