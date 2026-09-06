const presaleApi = require('../../../api/presale')
const marketingCapabilities = require('../../../utils/marketing-capabilities')
const { resolveImageUrl } = require('../../../utils/url')

Page({
  data: { activity: null, selectedSku: null, loading: false },
  onLoad(options) { this.activityId = Number(options.id || 0); marketingCapabilities.ensure('PRESALE').then((enabled) => { if (enabled && this.activityId) this.load(); else wx.switchTab({ url: '/pages/home/index' }) }) },
  load() { this.setData({ loading: true }); return presaleApi.detail(this.activityId).then((res) => { const activity = res && res.data; if (!activity) throw new Error('missing activity'); activity.skus = (activity.skus || []).map((sku) => ({ ...sku, mainImage: resolveImageUrl(sku.mainImage || ''), depositText: this.fmt(sku.depositAmount), deductionText: this.fmt(sku.depositDeductionAmount), balanceText: this.fmt(sku.balanceAmount), finalText: this.fmt(sku.finalAmount) })); this.setData({ activity, selectedSku: activity.skus[0] || null }) }).catch(() => { wx.showToast({ title:'预售活动加载失败', icon:'none' }) }).finally(() => this.setData({ loading: false })) },
  fmt(value) { return Number(value || 0).toFixed(2) },
  selectSku(e) { const index = Number(e.currentTarget.dataset.index); const sku = (this.data.activity && this.data.activity.skus || [])[index]; if (sku) this.setData({ selectedSku: sku }) },
  startDeposit() { const { activity, selectedSku } = this.data; if (!activity || !selectedSku) return; if (activity.phase === 0) return wx.showToast({ title:`${this.formatDate(activity.depositStartAt)} 开始`, icon:'none' }); if (activity.phase !== 1) return wx.showToast({ title: activity.phaseText || '当前阶段不可支付定金', icon:'none' }); wx.navigateTo({ url:`/pages/activity/presale/confirm?activityId=${activity.id}&presaleSkuId=${selectedSku.id}` }) },
  formatDate(value) { const d = new Date(String(value || '').replace(' ', 'T')); if (!Number.isFinite(d.getTime())) return ''; return `${d.getMonth()+1}月${d.getDate()}日 ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}` },
})
