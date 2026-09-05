const bundleApi = require('../../api/bundle')
const marketingCapabilities = require('../../utils/marketing-capabilities')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: { bundle: null, productId: 0, mainSkuId: 0, selectedItemSkuIds: [], preview: null, loading: true, submitting: false },

  onLoad(options) {
    this.setData({ productId: Number(options.productId || 0) })
    marketingCapabilities.ensure('BUNDLE').then((enabled) => {
      if (!enabled) return wx.switchTab({ url: '/pages/home/index' })
      this.loadBundle()
    })
  },

  loadBundle() {
    bundleApi.product(this.data.productId).then((res) => {
      const bundle = res.data
      if (!bundle) { wx.switchTab({ url: '/pages/home/index' }); return }
      const mainSkus = (bundle.mainSkus || []).map(this.normalizeSku)
      const items = (bundle.items || []).map(item => ({ ...item, mainImage: resolveImageUrl(item.mainImage || ''), skus: (item.skus || []).map(this.normalizeSku), checked: true, selectedSkuId: Number((item.skus || [])[0]?.id || 0) }))
      const mainSkuId = Number((mainSkus[0] && mainSkus[0].id) || 0)
      this.setData({ bundle: { ...bundle, mainProductImage: resolveImageUrl(bundle.mainProductImage || ''), mainSkus, items }, mainSkuId, selectedItemSkuIds: items.map(item => item.selectedSkuId).filter(Boolean), loading: false }, () => this.loadPreview())
    }).catch(() => this.setData({ loading: false }))
  },

  normalizeSku(sku) { return { ...sku, image: resolveImageUrl(sku.image || '') } },

  chooseMainSku(e) { this.setData({ mainSkuId: Number(e.currentTarget.dataset.skuid) }, () => this.loadPreview()) },

  toggleItem(e) {
    const index = Number(e.currentTarget.dataset.index)
    const items = (this.data.bundle.items || []).map((item, i) => i === index ? { ...item, checked: !item.checked } : item)
    this.setData({ 'bundle.items': items, selectedItemSkuIds: items.filter(item => item.checked).map(item => Number(item.selectedSkuId)).filter(Boolean) }, () => this.loadPreview())
  },

  chooseItemSku(e) {
    const index = Number(e.currentTarget.dataset.index)
    const skuId = Number(e.currentTarget.dataset.skuid)
    const items = (this.data.bundle.items || []).map((item, i) => i === index ? { ...item, selectedSkuId: skuId, checked: true } : item)
    this.setData({ 'bundle.items': items, selectedItemSkuIds: items.filter(item => item.checked).map(item => Number(item.selectedSkuId)).filter(Boolean) }, () => this.loadPreview())
  },

  loadPreview() {
    if (!this.data.bundle || !this.data.mainSkuId) return
    bundleApi.preview({ bundleId: this.data.bundle.id, mainSkuId: this.data.mainSkuId, itemSkuIds: this.data.selectedItemSkuIds }).then(res => { if (res.code === 0) this.setData({ preview: res.data }) }).catch(() => {})
  },

  submit(e) {
    if (this.data.submitting || !this.data.bundle || !this.data.mainSkuId) return wx.showToast({ title: '请选择主商品规格', icon: 'none' })
    this.setData({ submitting: true })
    bundleApi.addToCart({ bundleId: this.data.bundle.id, mainSkuId: this.data.mainSkuId, itemSkuIds: this.data.selectedItemSkuIds }).then(res => {
      if (res.code !== 0) throw new Error(res.msg || '加入失败')
      const result = res.data || {}
      if (e.currentTarget.dataset.action === 'buy') {
        wx.navigateTo({ url: `/pages/order/confirm?mode=bundle&bundleGroupId=${result.bundleGroupId}&cartItemIds=${(result.cartItemIds || []).join(',')}` })
      } else {
        wx.showToast({ title: '已加入购物车', icon: 'success' })
      }
    }).catch(err => wx.showToast({ title: err.message || '套餐暂不可用', icon: 'none' })).finally(() => this.setData({ submitting: false }))
  },

  goProduct() { wx.navigateBack() },
})
