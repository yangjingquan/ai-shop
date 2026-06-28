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

  loadTree(presetTopId) {
    this.setData({ loading: true })
    categoryApi
      .tree()
      .then((res) => {
        const tree = (res && res.data) || []
        const topCats = tree
        let activeTopId = 0
        if (topCats.length > 0) {
          if (presetTopId && topCats.find((c) => c.id === presetTopId)) {
            activeTopId = presetTopId
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

  onTopTab(e) {
    const id = Number(e.currentTarget.dataset.id)
    if (id === this.data.activeTopId) return
    this.setData({ activeTopId: id })
    this.renderTop(id)
  },

  onProduct(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },
})
