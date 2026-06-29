const categoryApi = require('../../api/category')
const productApi = require('../../api/product')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    topCats: [],
    activeTopId: 0,
    subCats: [],
    loading: false,
  },

  onLoad(opts) {
    this.loadTree(opts && opts.categoryId ? Number(opts.categoryId) : null)
  },

  onShow() {
    const jumpCategoryId = Number(wx.getStorageSync('home_jump_category_id') || 0)
    if (!jumpCategoryId) return
    wx.removeStorageSync('home_jump_category_id')
    if (!this.data.topCats.length) {
      this.pendingCategoryId = jumpCategoryId
      return
    }
    this.switchTopCategory(jumpCategoryId)
  },

  loadTree(presetTopId) {
    this.setData({ loading: true })
    categoryApi
      .tree()
      .then((res) => {
        const tree = (res && res.data) || []
        const topCats = tree
        const pendingCategoryId = this.pendingCategoryId
        this.pendingCategoryId = null
        const targetTopId = presetTopId || pendingCategoryId
        let activeTopId = 0
        if (topCats.length > 0) {
          if (targetTopId && topCats.find((c) => c.id === targetTopId)) {
            activeTopId = targetTopId
          } else {
            activeTopId = topCats[0].id
          }
        }
        this.setData({ topCats, activeTopId })
        if (activeTopId) this.renderTop(activeTopId)
      })
      .catch(() => {})
      .then(() => this.setData({ loading: false }))
  },

  renderTop(topId) {
    const top = this.data.topCats.find((c) => c.id === topId)
    if (!top) {
      this.setData({ subCats: [] })
      return
    }
    const subs = (top.children || []).map((c) => ({
      id: c.id,
      name: c.name,
      products: [],
    }))
    this.setData({ subCats: subs })
    subs.forEach((sub, idx) => {
      productApi
        .page({ page: 1, size: 6, categoryId: sub.id })
        .then((res) => {
          const list = ((res && res.data && res.data.list) || []).map((p) => ({
            ...p,
            mainImage: resolveImageUrl(p.mainImage || ''),
          }))
          const key = `subCats[${idx}].products`
          this.setData({ [key]: list })
        })
        .catch(() => {})
    })
  },

  switchTopCategory(id) {
    const topId = Number(id)
    if (!topId || !this.data.topCats.find((c) => c.id === topId)) return
    if (topId === this.data.activeTopId) return
    this.setData({ activeTopId: topId })
    this.renderTop(topId)
  },

  onTopTab(e) {
    this.switchTopCategory(e.currentTarget.dataset.id)
  },

  onProduct(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },
})
