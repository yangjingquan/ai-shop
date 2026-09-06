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
  load() { this.setData({ loading: true }); return presaleApi.active().then((res) => { const activities = ((res && res.data) || []).map((item) => { const skus = (item.skus || []).map((sku) => ({ ...sku, mainImage: resolveImageUrl(sku.mainImage || '') })); const productGroups = groupProducts(skus).map((productGroup, index) => ({ ...productGroup, activityId: item.id, cardKey: `${item.id}-${productGroup.productId || index}`, cover: resolveImageUrl(productGroup.mainImage || item.bannerImage || '') })); return { ...item, productGroups }; }).filter((item) => item.productGroups.length); this.setData({ activities }) }).catch(() => this.setData({ activities: [] })).finally(() => this.setData({ loading: false })) },
  open(e) { const { id, productId } = e.currentTarget.dataset; if (id) wx.navigateTo({ url: `/pages/activity/presale/detail?id=${id}&productId=${productId || ''}` }) },
})
