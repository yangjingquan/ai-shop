const pointsApi = require('../../../api/points')
const marketingCapabilities = require('../../../utils/marketing-capabilities')
const { resolveImageUrl } = require('../../../utils/url')

Page({
  data: {
    records: [],
    page: 1,
    size: 10,
    total: 0,
    hasMore: true,
    loading: false,
  },

  onShow() {
    marketingCapabilities.ensure('POINTS_MEMBER_DAY').then(ok => {
      if (ok) this.refresh()
    })
  },

  onPullDownRefresh() {
    this.refresh().finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) this.load()
  },

  refresh() {
    this.setData({ page: 1, records: [], total: 0, hasMore: true })
    return this.load()
  },

  load() {
    if (this.data.loading || !this.data.hasMore) return Promise.resolve()
    this.setData({ loading: true })
    return pointsApi.redeemRecords(this.data.page, this.data.size).then(res => {
      const payload = (res && res.data) || {}
      const list = (payload.list || []).map(item => ({
        ...item,
        image: resolveImageUrl(item.image || ''),
        typeText: item.redeemType === 'COUPON' ? '优惠券' : '实物',
        pointsText: `-${Number(item.pointsCost || 0)} 积分`,
        quantityText: `兑换 x${Number(item.quantity || 0)}`,
        timeText: this.time(item.createdAt),
        actionText: item.orderNo ? '查看订单' : item.couponId ? '查看优惠券' : '',
      }))
      const records = this.data.page === 1 ? list : this.data.records.concat(list)
      const total = Number(payload.total || 0)
      this.setData({
        records,
        total,
        hasMore: records.length < total,
        page: this.data.page + 1,
      })
    }).catch(() => {
      if (this.data.page === 1) this.setData({ records: [], total: 0 })
    }).finally(() => this.setData({ loading: false }))
  },

  time(value) {
    const d = new Date(String(value || '').replace(' ', 'T'))
    if (isNaN(d)) return value || ''
    const p = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
  },

  viewRecord(e) {
    const item = e.currentTarget.dataset.item
    if (!item) return
    if (item.orderNo) {
      wx.navigateTo({ url: `/pages/order/detail?orderNo=${item.orderNo}` })
      return
    }
    if (item.couponId) wx.navigateTo({ url: '/pages/coupon/list' })
  },
})
