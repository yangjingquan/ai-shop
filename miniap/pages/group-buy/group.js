const groupBuyApi = require('../../api/group-buy')

Page({
  data: { groupId: 0, group: null, loading: false },
  onLoad(options) {
    const groupId = Number(options.groupId || 0)
    this.setData({ groupId })
    this.load()
  },
  load() {
    if (!this.data.groupId) return
    this.setData({ loading: true })
    groupBuyApi.group(this.data.groupId).then((res) => {
      this.setData({ group: res.data || null })
    }).finally(() => this.setData({ loading: false }))
  },
  goProduct() {
    if (!this.data.group || !this.data.group.productId) return
    wx.navigateTo({ url: `/pages/product/detail?id=${this.data.group.productId}&groupBuy=1&groupId=${this.data.groupId}` })
  },
})
