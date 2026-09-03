const productApi = require('../../api/product')
const cartApi = require('../../api/cart')
const groupBuyApi = require('../../api/group-buy')
const { resolveImageUrl } = require('../../utils/url')
const marketingCapabilities = require('../../utils/marketing-capabilities')

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
    initialSkuId: 0,
    initialAction: '',
    groupBuyMode: false,
    groupBuyGroups: [],
    selectedGroupId: 0,
  },

  onLoad(opts) {
    const id = Number(opts.id || 0)
    if (!id) {
      wx.showToast({ title: '参数错误', icon: 'none' })
      return
    }
    this.setData({
      productId: id,
      initialSkuId: Number(opts.skuId || 0),
      initialAction: opts.action || '',
      groupBuyMode: opts.groupBuy === '1',
      selectedGroupId: Number(opts.groupId || 0),
    })
    if (this.data.groupBuyMode) {
      marketingCapabilities.ensure('GROUP_BUY').then((enabled) => {
        if (enabled) this.loadDetail()
        else wx.switchTab({ url: '/pages/home/index' })
      })
    } else {
      this.loadDetail()
    }
  },

  async loadDetail() {
    this.setData({ loading: true })
    try {
      let res = this.data.groupBuyMode
        ? await groupBuyApi.productDetail(this.data.productId)
        : await productApi.get(this.data.productId)
      let rawProduct = this.data.groupBuyMode ? ((res && res.data && res.data.product) || null) : ((res && res.data) || null)
      if (!this.data.groupBuyMode && rawProduct && Number(rawProduct.isGroupBuy) === 1) {
        res = await groupBuyApi.productDetail(this.data.productId)
        rawProduct = (res && res.data && res.data.product) || null
      }
      const product = rawProduct ? { ...rawProduct } : null
      if (!product) {
        wx.showToast({ title: '商品不存在', icon: 'none' })
        return
      }
      product.mainImage = resolveImageUrl(product.mainImage || '')
      product.images = Array.isArray(product.images) ? product.images.map(resolveImageUrl).filter(Boolean) : []
      product.skus = Array.isArray(product.skus)
        ? product.skus.map((sku) => ({
            ...sku,
            image: resolveImageUrl(sku.image || ''),
            priceText: this.fmtPrice(sku.price),
            originalPriceText: this.fmtPrice(sku.originalPrice),
            hasOriginalPrice: Number(sku.originalPrice || 0) > 0,
          }))
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
      product.salePriceText = this.fmtPrice(product.minPrice)
      product.groupBuyPriceText = this.fmtPrice(product.groupBuyPrice)
      product.groupBuyRequiredText = `${product.groupBuyRequiredCount || 0} 人成团`
      product.originalPriceText = this.fmtPrice(this.minPositivePrice(product.minOriginalPrice, product.maxOriginalPrice, product.originalPrice))
      product.hasOriginalPrice = this.hasOriginalPrice(product.minOriginalPrice, product.maxOriginalPrice, product.originalPrice)
      product.minPrice = this.fmtPrice(product.minPrice)
      product.maxPrice = this.fmtPrice(product.maxPrice)
      const selected = new Array(specs.length).fill(null)
      const groups = Number(product.isGroupBuy) === 1 ? ((res && res.data && res.data.groups) || []).map((group) => ({
        ...group,
        groupBuyPriceText: this.fmtPrice(group.groupBuyPrice || product.groupBuyPrice),
        expireText: group.expireAt ? this.formatDateTime(group.expireAt) : '',
        leaderText: group.leaderNickname || '拼团发起人',
      })) : []
      this.setData({
        product,
        banners,
        groupBuyGroups: groups,
        selectedValueIds: selected,
        selectedSku: null,
        selectedSkuText: '',
        qty: 1,
      })
      this.applyInitialSku()
    } finally {
      this.setData({ loading: false })
    }
  },

  fmtPrice(v) {
    const n = Number(v || 0)
    return n.toFixed(2)
  },

  formatDateTime(ts) {
    const date = new Date(Number(ts || 0))
    if (!Number.isFinite(date.getTime())) return ''
    const pad = (n) => String(n).padStart(2, '0')
    return `${date.getMonth() + 1}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
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

  applyInitialSku() {
    const skuId = Number(this.data.initialSkuId || 0)
    if (!skuId || !this.data.product) return
    const sku = (this.data.product.skus || []).find((item) => Number(item.id) === skuId)
    if (!sku || !Array.isArray(sku.specValueIds)) {
      wx.showToast({ title: '原规格已失效，请重新选择', icon: 'none' })
      if (this.data.initialAction === 'buy') {
        this.setData({ skuOpen: true, skuAction: 'buy' })
      }
      return
    }
    this.matchSku(sku.specValueIds.slice())
    if (this.data.initialAction === 'buy') {
      this.setData({ skuOpen: true, skuAction: 'buy' })
    }
  },

  openSku(e) {
    if (!this.data.product) return
    const rawAction = e && e.currentTarget && e.currentTarget.dataset.action
    const action = rawAction === 'cart' ? 'cart' : 'buy'
    this.setData({ skuOpen: true, skuAction: action })
  },

  openGroupBuySku() {
    this.setData({ skuOpen: true, skuAction: 'group-open', selectedGroupId: 0 })
  },

  joinGroup(e) {
    const groupId = Number(e.currentTarget.dataset.groupid || 0)
    this.setData({ skuOpen: true, skuAction: 'group-join', selectedGroupId: groupId })
  },

  closeSku() {
    this.setData({ skuOpen: false })
  },
  noop() {},

  previewSkuImage(e) {
    const url = e && e.currentTarget && e.currentTarget.dataset.url
    if (!url) return
    const urls = [url]
    ;(this.data.banners || []).forEach((item) => {
      if (item && urls.indexOf(item) === -1) urls.push(item)
    })
    wx.previewImage({ current: url, urls })
  },

  onCartTab() {
    wx.switchTab({ url: '/pages/cart/index' })
  },

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

    if (this.data.skuAction === 'group-open' || this.data.skuAction === 'group-join') {
      const url = `/pages/order/confirm?mode=groupBuy&productId=${this.data.productId}&skuId=${this.data.selectedSku.id}&quantity=${this.data.qty}&groupId=${this.data.selectedGroupId || ''}`
      this.setData({ skuOpen: false })
      wx.navigateTo({ url })
      return
    }

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
