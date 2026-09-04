const categoryApi = require('../../api/category')
const productApi = require('../../api/product')
const bannerApi = require('../../api/banner')
const homeApi = require('../../api/home')
const marketingCapabilities = require('../../utils/marketing-capabilities')
const couponApi = require('../../api/coupon')
const seckillApi = require('../../api/seckill')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    banners: [],
    keyword: '',
    topCategories: [],
    products: [],
    loading: false,
    marketingEnabled: {},
    seckillSummary: null,
    now: Date.now(),
    newUserCoupon: null,
  },

  onLoad() {
    this.loadAll()
  },

  async loadAll() {
    this.setData({ loading: true })
    try {
      const [homeRes, treeRes] = await Promise.all([
        homeApi.get().catch(() => null),
        categoryApi.tree().catch(() => ({ data: [] })),
      ])
      const homeData = (homeRes && homeRes.data) || {}
      const featureMap = Array.isArray(homeData.marketingFeatures)
        ? marketingCapabilities.seed(homeData.marketingFeatures)
        : {}
      const marketingEnabled = Object.keys(featureMap).reduce((result, code) => {
        result[code] = featureMap[code].enabled === true || Number(featureMap[code].enabled) === 1
        return result
      }, {})
      const bannerData = homeData.banners || await bannerApi.list().then((res) => res.data || []).catch(() => [])
      let productData = homeData.recommends || []
      if (!productData.length) {
        const fallback = await this.fetchProducts()
        productData = (fallback && fallback.data && fallback.data.list) || []
      }
      const banners = bannerData.map((b) => ({
        ...b,
        imageUrl: resolveImageUrl(b.imageUrl || ''),
      }))
      const tree = (treeRes && treeRes.data) || []
      const top = tree.slice(0, 5).map((c, idx) => ({
        id: c.id,
        name: c.name,
        icon: resolveImageUrl(c.icon || ''),
        symbol: this.categorySymbol(idx),
        tone: `tone-${(idx % 5) + 1}`,
      }))
      const list = this.normalizeProducts({ data: { list: productData } })
      this.setData({ banners, topCategories: top, products: list, marketingEnabled })
      this.loadSeckillSummary(marketingEnabled.SECKILL)
      this.loadNewUserCoupon(marketingEnabled)
    } finally {
      this.setData({ loading: false })
    }
  },

  loadSeckillSummary(enabled) {
    if (!enabled) {
      this.setData({ seckillSummary: null })
      if (this._seckillTimer) clearInterval(this._seckillTimer)
      return
    }
    seckillApi.sessions().then((res) => {
      const session = res && res.data && res.data[0]
      const product = session && session.products && session.products[0]
      if (!session || !product) {
        this.setData({ seckillSummary: null })
        return
      }
      const summary = {
        status: session.status,
        statusText: session.status === 0 ? '即将开始' : session.status === 1 ? '限量抢购' : '本场已结束',
        activityPriceText: this.fmtPrice(product.activityPrice),
        productName: product.productName,
        startText: this.formatHour(session.startAt),
        startAt: session.startAt,
        endAt: session.endAt,
      }
      this.setData({ seckillSummary: this.withSeckillCountdown(summary) })
      if (this._seckillTimer) clearInterval(this._seckillTimer)
      this._seckillTimer = setInterval(() => {
        if (this.data.seckillSummary) this.setData({ now: Date.now(), seckillSummary: this.withSeckillCountdown(this.data.seckillSummary) })
      }, 1000)
    }).catch(() => this.setData({ seckillSummary: null }))
  },

  formatHour(value) {
    const date = new Date(String(value || '').replace(' ', 'T'))
    if (!Number.isFinite(date.getTime())) return ''
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  },

  withSeckillCountdown(summary) {
    const value = summary.status === 0 ? summary.startAt : summary.endAt
    const date = new Date(String(value || '').replace(' ', 'T'))
    let seconds = Math.max(0, Math.floor((date.getTime() - Date.now()) / 1000))
    const hours = Math.floor(seconds / 3600)
    seconds -= hours * 3600
    const minutes = Math.floor(seconds / 60)
    const remain = seconds % 60
    return { ...summary, countdownText: `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(remain).padStart(2, '0')}` }
  },

  onUnload() {
    if (this._seckillTimer) clearInterval(this._seckillTimer)
  },

  async loadNewUserCoupon(marketingEnabled) {
    const app = getApp()
    if (!marketingEnabled.NEW_USER_COUPON || app.globalData.newUserCouponPopupShown) return
    try {
      const res = await couponApi.eligibility()
      const eligibility = (res && res.data) || {}
      if (eligibility.canReceive && eligibility.coupon) this.setData({ newUserCoupon: eligibility.coupon })
    } catch (_) {
      // 登录态或资格接口失败时不打断首页浏览。
    }
  },

  closeNewUserCoupon() {
    const app = getApp()
    app.globalData.newUserCouponPopupShown = true
    this.setData({ newUserCoupon: null })
  },

  claimNewUserCoupon() {
    const coupon = this.data.newUserCoupon
    if (!coupon || !coupon.templateId) return
    couponApi.receive(coupon.templateId).then(() => {
      const app = getApp()
      app.globalData.newUserCouponPopupShown = true
      this.setData({ newUserCoupon: null })
      wx.showToast({ title: '新人券已领取', icon: 'success' })
      setTimeout(() => wx.navigateTo({ url: '/pages/coupon/list' }), 500)
    }).catch(() => {})
  },

  async fetchProducts(keyword) {
    const params = { page: 1, size: 12 }
    const query = (keyword || '').trim()
    if (query) {
      params.keyword = query
      return productApi.page(params).catch(() => ({ data: { list: [] } }))
    }
    return productApi.page({ ...params, isRecommend: 1 }).catch(() => ({ data: { list: [] } }))
  },

  normalizeProducts(pageRes) {
    return ((pageRes && pageRes.data && pageRes.data.list) || []).map((p, idx) => ({
      id: p.id,
      name: p.name,
      subtitle: p.subtitle || p.categoryName || '精选好物',
      mainImage: resolveImageUrl(p.mainImage || ''),
      hasImage: !!p.mainImage,
      visualType: ['phone', 'watch', 'audio', 'bag'][idx % 4],
      cardTone: `rec-tone-${(idx % 4) + 1}`,
      minPrice: this.fmtPrice(p.minPrice),
      salePriceText: this.fmtPrice(p.minPrice),
      originalPriceText: this.fmtPrice(this.minPositivePrice(p.minOriginalPrice, p.maxOriginalPrice, p.originalPrice)),
      hasOriginalPrice: this.hasOriginalPrice(p.minOriginalPrice, p.maxOriginalPrice, p.originalPrice),
    }))
  },

  categorySymbol(idx) {
    return ['◒', '◍', '◈', '◎', '✦'][idx % 5]
  },

  fmtPrice(v) {
    const n = Number(v || 0)
    return n.toFixed(2)
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

  async onClearSearch() {
    await this.loadProducts('')
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
    const linkValue = String(banner.linkValue).trim()
    if (linkType === 1) {
      // 兼容旧版本将 linkValue 保存为小程序页面路径的 Banner。
      if (linkValue.indexOf('/pages/') === 0) {
        this.navigateMiniProgramPath(linkValue)
        return
      }
      if (!/^[1-9][0-9]*$/.test(linkValue)) {
        wx.showToast({ title: '商品链接无效', icon: 'none' })
        return
      }
      wx.navigateTo({ url: `/pages/product/detail?id=${linkValue}` })
      return
    }
    if (linkType === 2) {
      if (!/^[1-9][0-9]*$/.test(linkValue)) {
        wx.showToast({ title: '分类链接无效', icon: 'none' })
        return
      }
      wx.setStorageSync('home_jump_category_id', Number(linkValue))
      wx.switchTab({ url: '/pages/category/index' })
      return
    }
    if (linkType === 3) {
      if (!/^https:\/\/[^\s]+$/i.test(linkValue)) {
        wx.showToast({ title: '外部链接无效', icon: 'none' })
        return
      }
      wx.navigateTo({ url: `/pages/webview/index?url=${encodeURIComponent(linkValue)}` })
    }
  },

  navigateMiniProgramPath(linkValue) {
    const tabPages = ['/pages/home/index', '/pages/category/index', '/pages/cart/index', '/pages/order/list', '/pages/my/index']
    const targetPath = linkValue.split('?')[0]
    if (tabPages.includes(targetPath)) {
      wx.switchTab({ url: targetPath })
    } else {
      wx.navigateTo({ url: linkValue })
    }
  },

  onCategory(e) {
    const id = Number(e.currentTarget.dataset.id)
    wx.setStorageSync('home_jump_category_id', id)
    wx.switchTab({ url: '/pages/category/index' })
  },

  onMoreRecommend() {
    wx.navigateTo({ url: '/pages/recommend/index' })
  },

  onGroupBuy() {
    marketingCapabilities.ensure('GROUP_BUY').then((enabled) => {
      if (enabled) wx.navigateTo({ url: '/pages/group-buy/list' })
    })
  },

  onSeckill() {
    if (!this.data.marketingEnabled.SECKILL) return
    wx.navigateTo({ url: '/pages/activity/seckill/list' })
  },

  onProduct(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/product/detail?id=${id}` })
  },

  onPullDownRefresh() {
    this.loadAll().finally(() => wx.stopPullDownRefresh())
  },
})
