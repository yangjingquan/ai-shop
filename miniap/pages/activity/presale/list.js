const presaleApi = require('../../../api/presale')
const marketingCapabilities = require('../../../utils/marketing-capabilities')
const { resolveImageUrl } = require('../../../utils/url')

function groupProducts(skus) {
  const groups = new Map()
  ;(skus || []).forEach((sku) => {
    const key = Number(sku.productId || 0)
    if (!groups.has(key)) groups.set(key, { productId: key, productName: sku.productName, mainImage: sku.mainImage, skus: [] })
    groups.get(key).skus.push(sku)
  })
  return Array.from(groups.values())
}

Page({
  data: { activities: [], loading: false },
  onLoad() { marketingCapabilities.ensure('PRESALE').then((enabled) => { if (enabled) this.load(); else wx.switchTab({ url: '/pages/home/index' }) }) },
  onPullDownRefresh() { this.load().finally(() => wx.stopPullDownRefresh()) },
  load() { this.setData({ loading: true }); return presaleApi.active().then((res) => { const activities = ((res && res.data) || []).map((item) => { const skus = (item.skus || []).map((sku) => ({ ...sku, mainImage: resolveImageUrl(sku.mainImage || ''), depositText: this.fmt(sku.depositAmount), balanceText: this.fmt(sku.balanceAmount), finalText: this.fmt(sku.finalAmount) })); const productGroups = groupProducts(skus); return { ...item, cover: resolveImageUrl(item.bannerImage || productGroups[0]?.mainImage || ''), skus, productGroups } }); this.setData({ activities }) }).catch(() => this.setData({ activities: [] })).finally(() => this.setData({ loading: false })) },
  fmt(value) { return Number(value || 0).toFixed(2) },
  open(e) { const id = e.currentTarget.dataset.id; if (id) wx.navigateTo({ url: `/pages/activity/presale/detail?id=${id}` }) },
})
