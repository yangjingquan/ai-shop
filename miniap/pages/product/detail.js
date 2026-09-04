const productApi = require('../../api/product')
const cartApi = require('../../api/cart')
const groupBuyApi = require('../../api/group-buy')
const seckillApi = require('../../api/seckill')
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
    seckillMode: false,
    seckillSessionId: 0,
    seckillSkuId: 0,
    seckillHasSkuSelector: false,
    seckillPriceText: '',
    seckillRemainingText: '',
    seckillLimitText: '',
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
      seckillMode: opts.activity === 'seckill',
      seckillSessionId: Number(opts.sessionId || 0),
      seckillSkuId: Number(opts.seckillSkuId || 0),
      selectedGroupId: Number(opts.groupId || 0),
    })
    if (this.data.groupBuyMode) {
      marketingCapabilities.ensure('GROUP_BUY').then((enabled) => {
        if (enabled) this.loadDetail()
        else wx.switchTab({ url: '/pages/home/index' })
      })
    } else if (this.data.seckillMode) {
      marketingCapabilities.ensure('SECKILL').then((enabled) => {
        if (enabled && this.data.seckillSessionId && this.data.seckillSkuId) this.loadDetail()
        else wx.switchTab({ url: '/pages/home/index' })
      })
    } else {
      this.loadDetail()
    }
  },

  async loadDetail() {
    this.setData({ loading: true })
    try {
      let res
      let rawProduct
      if (this.data.groupBuyMode) {
        res = await groupBuyApi.productDetail(this.data.productId)
        rawProduct = (res && res.data && res.data.product) || null
      } else if (this.data.seckillMode) {
        res = await seckillApi.product(this.data.productId, this.data.seckillSessionId, this.data.seckillSkuId)
        rawProduct = (res && res.data) || null
      } else {
        res = await productApi.get(this.data.productId)
        rawProduct = (res && res.data) || null
      }
      if (!this.data.groupBuyMode && !this.data.seckillMode && rawProduct && Number(rawProduct.isGroupBuy) === 1) {
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
            id: this.data.seckillMode ? sku.skuId : sku.id,
            seckillSkuId: this.data.seckillMode ? Number(sku.seckillSkuId || 0) : 0,
            price: this.data.seckillMode ? sku.activityPrice : sku.price,
            stock: this.data.seckillMode ? sku.remainingStock : sku.stock,
            image: resolveImageUrl(sku.image || ''),
            priceText: this.fmtPrice(this.data.seckillMode ? sku.activityPrice : sku.price),
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
      const configuredSpecValueIds = new Set(
        product.skus.flatMap((sku) => Array.isArray(sku.specValueIds) ? sku.specValueIds.map(Number) : []),
      )
      const displaySpecs = this.data.seckillMode
        ? specs
            .map((spec) => ({
              ...spec,
              values: spec.values.filter((value) => configuredSpecValueIds.has(Number(value.id))),
            }))
            .filter((spec) => spec.values.length > 0)
        : specs
      product.specs = displaySpecs
      if (this.data.seckillMode) {
        product.minPrice = product.activityPrice
        product.maxPrice = product.activityPrice
        product.salePriceText = this.fmtPrice(product.activityPrice)
        product.originalPriceText = this.fmtPrice(product.originalPrice)
        product.hasOriginalPrice = Number(product.originalPrice || 0) > 0
        product.isSeckill = 1
        product.seckillStatus = product.sessionStatus
        product.seckillStartText = this.formatDateTime(product.startAt)
        product.seckillEndText = this.formatDateTime(product.endAt)
        product.seckillRemaining = product.remainingStock
        product.seckillUserLimit = product.userLimit
        product.seckillSoldText = `已抢 ${product.soldCount || 0} 件`
      } else {
        product.salePriceText = this.fmtPrice(product.minPrice)
      }
      product.groupBuyPriceText = this.fmtPrice(product.groupBuyPrice)
      product.groupBuyRequiredText = `${product.groupBuyRequiredCount || 0} 人成团`
      if (!this.data.seckillMode) {
        product.originalPriceText = this.fmtPrice(this.minPositivePrice(product.minOriginalPrice, product.maxOriginalPrice, product.originalPrice))
        product.hasOriginalPrice = this.hasOriginalPrice(product.minOriginalPrice, product.maxOriginalPrice, product.originalPrice)
      }
      product.minPrice = this.fmtPrice(product.minPrice)
      product.maxPrice = this.fmtPrice(product.maxPrice)
      const selected = new Array(displaySpecs.length).fill(null)
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
        seckillHasSkuSelector: this.data.seckillMode && product.skus.length > 1,
        initialSkuId: this.data.seckillMode
          ? Number(this.data.initialSkuId || product.skuId || 0)
          : this.data.initialSkuId,
        selectedValueIds: selected,
        selectedSku: null,
        selectedSkuText: '',
        qty: 1,
        seckillPriceText: this.fmtPrice(product.activityPrice),
        seckillRemainingText: `仅剩 ${product.remainingStock || 0} 件`,
        seckillLimitText: `每人限购${product.userLimit || 1}件`,
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
    const date = typeof ts === 'number' || /^\d+$/.test(String(ts || ''))
      ? new Date(Number(ts || 0))
      : new Date(String(ts || '').replace(' ', 'T'))
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
    if (this.data.seckillMode && !this.data.seckillHasSkuSelector) {
      this.buySingleSeckill()
      return
    }
    const rawAction = e && e.currentTarget && e.currentTarget.dataset.action
    const action = rawAction === 'cart' ? 'cart' : 'buy'
    this.setData({ skuOpen: true, skuAction: action })
  },

  buySingleSeckill() {
    const sku = (this.data.product.skus || [])[0]
    if (!sku) {
      wx.showToast({ title: '暂无可售秒杀规格', icon: 'none' })
      return
    }
    this.setData({
      selectedSku: sku,
      selectedValueIds: Array.isArray(sku.specValueIds) ? sku.specValueIds.slice() : [],
      selectedSkuText: sku.specText || '默认规格',
      seckillSkuId: Number(sku.seckillSkuId || this.data.seckillSkuId),
      seckillPriceText: sku.priceText,
      seckillRemainingText: `仅剩 ${sku.stock || 0} 件`,
      seckillLimitText: `每人限购${sku.userLimit || 1}件`,
    }, () => this.onConfirm())
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
    const nextData = { selectedValueIds, selectedSku, selectedSkuText }
    if (this.data.seckillMode && selectedSku) {
      nextData.seckillSkuId = Number(selectedSku.seckillSkuId || this.data.seckillSkuId)
      nextData.seckillPriceText = selectedSku.priceText
      nextData.seckillRemainingText = `仅剩 ${selectedSku.stock || 0} 件`
      nextData.seckillLimitText = `每人限购${selectedSku.userLimit || 1}件`
      const limit = Math.min(selectedSku.stock || 1, selectedSku.userLimit || 1)
      if (this.data.qty > limit) nextData.qty = limit
    }
    this.setData(nextData)
  },

  onQtyMinus() {
    const next = Math.max(1, this.data.qty - 1)
    this.setData({ qty: next })
  },
  onQtyPlus() {
    const stock = this.data.selectedSku ? this.data.selectedSku.stock : 9999
    const limit = this.data.seckillMode && this.data.selectedSku ? (this.data.selectedSku.userLimit || stock) : stock
    const next = Math.min(stock, limit, this.data.qty + 1)
    this.setData({ qty: next })
  },

  onConfirm() {
    if (!this.data.selectedSku) {
      wx.showToast({ title: '请先选择规格', icon: 'none' })
      return
    }
    if (this.data.addingCart) return

    if (this.data.seckillMode && this.data.product && this.data.product.seckillStatus !== 1) {
      wx.showToast({ title: this.data.product.seckillStatus === 0 ? '秒杀尚未开始' : '秒杀已结束', icon: 'none' })
      return
    }
    if (this.data.seckillMode && Number(this.data.selectedSku.stock || 0) < 1) {
      wx.showToast({ title: '该规格已售罄', icon: 'none' })
      return
    }

    if (this.data.skuAction === 'group-open' || this.data.skuAction === 'group-join') {
      const url = `/pages/order/confirm?mode=groupBuy&productId=${this.data.productId}&skuId=${this.data.selectedSku.id}&quantity=${this.data.qty}&groupId=${this.data.selectedGroupId || ''}`
      this.setData({ skuOpen: false })
      wx.navigateTo({ url })
      return
    }

    if (this.data.seckillMode) {
      const url = `/pages/order/confirm?mode=seckill&productId=${this.data.productId}&skuId=${this.data.selectedSku.id}&sessionId=${this.data.seckillSessionId}&seckillSkuId=${this.data.seckillSkuId}&quantity=${this.data.qty}`
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
