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
  data: { activity: null, productGroups: [], selectedSku: null, loading: false },
  onLoad(options) { this.activityId = Number(options.id || 0); this.productId = Number(options.productId || 0); marketingCapabilities.ensure('PRESALE').then((enabled) => { if (enabled && this.activityId) this.load(); else wx.switchTab({ url: '/pages/home/index' }) }) },
  load() { this.setData({ loading: true }); return presaleApi.detail(this.activityId).then((res) => { const activity = res && res.data; if (!activity) throw new Error('missing activity'); const skus = (activity.skus || []).map((sku) => ({ ...sku, mainImage: resolveImageUrl(sku.mainImage || ''), depositText: this.fmt(sku.depositAmount), deductionText: this.fmt(sku.depositDeductionAmount), balanceText: this.fmt(sku.balanceAmount), finalText: this.fmt(sku.finalAmount) })); const allProductGroups = groupProducts(skus); const productGroup = allProductGroups.find((group) => Number(group.productId) === this.productId) || allProductGroups[0]; if (!productGroup) throw new Error('missing product'); activity.skus = productGroup.skus; activity.productId = productGroup.productId; activity.cover = resolveImageUrl(activity.bannerImage || productGroup.mainImage || ''); this.setData({ activity, productGroups: [productGroup], selectedSku: productGroup.skus[0] || null }) }).catch(() => { wx.showToast({ title:'预售活动加载失败', icon:'none' }) }).finally(() => this.setData({ loading: false })) },
  fmt(value) { return Number(value || 0).toFixed(2) },
  selectSku(e) { const skuId = Number(e.currentTarget.dataset.skuId); const sku = (this.data.activity && this.data.activity.skus || []).find((item) => Number(item.id) === skuId); if (sku) this.setData({ selectedSku: sku }) },
  startDeposit() { const { activity, selectedSku } = this.data; if (!activity || !selectedSku) return; if (activity.phase === 0) return wx.showToast({ title:`${this.formatDate(activity.depositStartAt)} 开始`, icon:'none' }); if (activity.phase !== 1) return wx.showToast({ title: activity.phaseText || '当前阶段不可支付定金', icon:'none' }); wx.navigateTo({ url:`/pages/activity/presale/confirm?activityId=${activity.id}&presaleSkuId=${selectedSku.id}` }) },
  formatDate(value) { const d = new Date(String(value || '').replace(' ', 'T')); if (!Number.isFinite(d.getTime())) return ''; return `${d.getMonth()+1}月${d.getDate()}日 ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}` },
})
