const categoryApi = require('../../api/category')
const productApi = require('../../api/product')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    topCats: [],
    activeTopId: 0,
    currentTop: null,
    currentTopName: '',
    subCats: [],
    selectedSubId: 0,
    selectedSubName: '全部商品',
    products: [],
    productViewMode: 'list',
    categoryExpanded: false,
    loading: false,
    productLoading: false,
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

  loadTree(presetCategoryId) {
    this.setData({ loading: true })
    categoryApi
      .tree()
      .then((res) => {
        const topCats = (res && res.data) || []
        const pendingCategoryId = this.pendingCategoryId
        this.pendingCategoryId = null
        const targetCategoryId = presetCategoryId || pendingCategoryId
        const activeTopId = this.resolveTopId(topCats, targetCategoryId) || (topCats[0] && topCats[0].id) || 0
        this.setData({ topCats, activeTopId })
        if (activeTopId) {
          this.renderTop(activeTopId, targetCategoryId)
        }
      })
      .catch(() => {})
      .then(() => this.setData({ loading: false }))
  },

  resolveTopId(topCats, categoryId) {
    if (!categoryId) return 0
    const direct = topCats.find((c) => c.id === categoryId)
    if (direct) return direct.id
    const parent = topCats.find((c) => (c.children || []).some((sub) => sub.id === categoryId))
    return parent ? parent.id : 0
  },

  renderTop(topId, presetCategoryId) {
    const top = this.data.topCats.find((c) => c.id === topId)
    if (!top) {
      this.setData({
        currentTop: null,
        currentTopName: '',
        subCats: [],
        selectedSubId: 0,
        selectedSubName: '全部商品',
        products: [],
      })
      return
    }
    const subCats = (top.children || []).map((sub) => ({
      ...sub,
      isHot: false,
    }))
    const selectedSub = subCats.find((sub) => sub.id === presetCategoryId)
    const selectedSubId = selectedSub ? selectedSub.id : 0
    const selectedSubName = selectedSub ? selectedSub.name : '全部商品'
    this.setData({
      currentTop: top,
      currentTopName: top.name || '',
      subCats,
      selectedSubId,
      selectedSubName,
      categoryExpanded: false,
    })
    this.loadProducts(selectedSubId || top.id)
  },

  loadProducts(categoryId) {
    if (!categoryId) return
    this.setData({ productLoading: true })
    productApi
      .page({ page: 1, size: 20, categoryId })
      .then((res) => {
        const products = ((res && res.data && res.data.list) || []).map((p) => ({
          id: p.id,
          name: p.name,
          shortName: (p.name || '').slice(0, 1),
          subtitle: p.subtitle || p.categoryName || '精选好物 · 品质优选',
          mainImage: resolveImageUrl(p.mainImage || ''),
          minPrice: this.fmtPrice(p.minPrice),
        }))
        this.setData({ products })
      })
      .catch(() => this.setData({ products: [] }))
      .then(() => this.setData({ productLoading: false }))
  },

  fmtPrice(v) {
    const n = Number(v || 0)
    return n.toFixed(2)
  },

  switchTopCategory(id) {
    const topId = this.resolveTopId(this.data.topCats, Number(id))
    if (!topId || !this.data.topCats.find((c) => c.id === topId)) return
    if (topId === this.data.activeTopId) return
    this.setData({ activeTopId: topId })
    this.renderTop(topId)
  },

  onTopTab(e) {
    this.switchTopCategory(e.currentTarget.dataset.id)
  },

  onToggleCategoryPanel() {
    this.setData({ categoryExpanded: !this.data.categoryExpanded })
  },

  onSubCategory(e) {
    const id = Number(e.currentTarget.dataset.id || 0)
    const sub = this.data.subCats.find((c) => c.id === id)
    const selectedSubId = sub ? sub.id : 0
    const selectedSubName = sub ? sub.name : '全部商品'
    const categoryId = selectedSubId || this.data.activeTopId
    this.setData({ selectedSubId, selectedSubName })
    this.loadProducts(categoryId)
  },

  onSearchTap() {
    wx.switchTab({ url: '/pages/home/index' })
  },

  onToggleProductView() {
    this.setData({ productViewMode: this.data.productViewMode === 'grid' ? 'list' : 'grid' })
  },

  onProduct(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },
})
