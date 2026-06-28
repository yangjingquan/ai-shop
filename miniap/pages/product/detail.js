const productApi = require('../../api/product')
const cartApi = require('../../api/cart')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    productId: 0,
    product: null,
    banners: [],
    skuOpen: false,
    selectedValueIds: [],
    selectedSku: null,
    selectedSkuText: '',
    qty: 1,
    loading: false,
    addingCart: false,
    skuAction: 'buy',
  },

  onLoad(opts) {
    const id = Number(opts.id || 0)
    if (!id) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      return
    }
    this.setData({ productId: id })
    this.loadDetail()
  },

  async loadDetail() {
    this.setData({ loading: true })
    try {
      const res = await productApi.get(this.data.productId)
      const rawProduct = (res && res.data) || null
      const product = rawProduct ? { ...rawProduct } : null
      if (!product) {
        wx.showToast({ title: '商品不存在', icon: 'none' })
        return
      }
      product.mainImage = resolveImageUrl(product.mainImage || '')
      product.images = Array.isArray(product.images) ? product.images.map(resolveImageUrl).filter(Boolean) : []
      product.skus = Array.isArray(product.skus)
        ? product.skus.map((sku) => ({ ...sku, image: resolveImageUrl(sku.image || '') }))
        : []
      const banners = []
      if (product.mainImage) banners.push(product.mainImage)
      product.images.forEach((u) => {
        if (u && banners.indexOf(u) === -1) banners.push(u)
      })
      const specs = (product.specs || []).map((s) => ({
        id: s.id,
        name: s.name,
        values: (s.values || []).map((v) => ({ id: v.id, value: v.value })),
      }))
      product.specs = specs
      product.minPrice = this.fmtPrice(product.minPrice)
      product.maxPrice = this.fmtPrice(product.maxPrice)
      const selected = new Array(specs.length).fill(null)
      this.setData({
        product,
        banners,
        selectedValueIds: selected,
        selectedSku: null,
        selectedSkuText: '',
        qty: 1,
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  fmtPrice(v) {
    const n = Number(v || 0)
    return n.toFixed(2)
  },

  openSku(e) {
    if (!this.data.product) return
    const action = e && e.currentTarget && e.currentTarget.dataset.action ? e.currentTarget.dataset.action : 'buy'
    this.setData({ skuOpen: true, skuAction: action })
  },
  closeSku() {
    this.setData({ skuOpen: false })
  },
  noop() {},

  onSelectVal(e) {
    const specIndex = Number(e.currentTarget.dataset.specIndex)
    const valId = Number(e.currentTarget.dataset.valId)
    const selected = this.data.selectedValueIds.slice()
    selected[specIndex] = selected[specIndex] === valId ? null : valId
    this.matchSku(selected)
  },

  matchSku(selectedValueIds) {
    const allSelected = selectedValueIds.every((v) => v != null)
    let selectedSku = null
    let selectedSkuText = ''
    const product = this.data.product
    if (allSelected && product) {
      selectedSku = (product.skus || []).find((sku) => {
        const ids = sku.specValueIds || []
        if (ids.length !== selectedValueIds.length) return false
        return ids.every((id, i) => id === selectedValueIds[i])
      }) || null
      if (selectedSku) {
        const parts = []
        ;(product.specs || []).forEach((spec, i) => {
          const valId = selectedValueIds[i]
          const v = (spec.values || []).find((x) => x.id === valId)
          if (v) parts.push(v.value)
        })
        selectedSkuText = parts.join(' / ')
      }
    } else {
      const parts = []
      ;(product.specs || []).forEach((spec, i) => {
        const valId = selectedValueIds[i]
        if (valId == null) return
        const v = (spec.values || []).find((x) => x.id === valId)
        if (v) parts.push(v.value)
      })
      selectedSkuText = parts.length ? `已选 ${parts.join(' / ')}` : ''
    }
    this.setData({ selectedValueIds, selectedSku, selectedSkuText })
  },

  onQtyMinus() {
    const next = Math.max(1, this.data.qty - 1)
    this.setData({ qty: next })
  },
  onQtyPlus() {
    const stock = this.data.selectedSku ? this.data.selectedSku.stock : 9999
    const next = Math.min(stock, this.data.qty + 1)
    this.setData({ qty: next })
  },

  onConfirm() {
    if (!this.data.selectedSku) {
      wx.showToast({ title: '请先选择规格', icon: 'none' })
      return
    }
    if (this.data.addingCart) return

    this.setData({ addingCart: true })
    cartApi
      .add({ skuId: this.data.selectedSku.id, quantity: this.data.qty })
      .then((res) => {
        const cartItemId = res && res.data && res.data.id
        this.setData({ skuOpen: false })
        if (this.data.skuAction === 'buy' && cartItemId) {
          wx.navigateTo({ url: `/pages/order/confirm?cartItemIds=${cartItemId}` })
          return
        }
        wx.showToast({ title: '已加入购物车', icon: 'success' })
      })
      .finally(() => {
        this.setData({ addingCart: false })
      })
  },
})
