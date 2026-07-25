const orderApi = require('../../api/order')
const config = require('../../utils/config')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    order: null,
    orderNo: '',
    loading: false,
  },

  onLoad(options) {
    const orderNo = options.orderNo || ''
    this.setData({ orderNo })
    this.loadDetail(orderNo)
  },

  loadDetail(orderNo) {
    if (!orderNo || this.data.loading) return Promise.resolve()
    this.setData({ loading: true })
    return orderApi
      .detail(orderNo)
      .then((res) => {
        const raw = res.data || {}
        const order = {
          ...raw,
          totalAmountText: this.fmtPrice(raw.totalAmount),
          freightAmountText: this.fmtPrice(raw.freightAmount),
          discountAmountText: this.fmtPrice(raw.discountAmount),
          payAmountText: this.fmtPrice(raw.payAmount),
          groupBuyProgress: raw.groupBuyRequiredCount ? `${raw.groupBuyPaidCount || 0}/${raw.groupBuyRequiredCount} 人` : '',
          groupBuyExpireText: raw.groupBuyExpireAt ? this.formatTime(raw.groupBuyExpireAt) : '',
          totalLabel: raw.status === 0 ? '需支付' : '实付款',
          items: (raw.items || []).map((item) => ({
            ...item,
            mainImage: resolveImageUrl(item.mainImage || ''),
            unitPriceText: this.fmtPrice(item.unitPrice),
            subtotalText: this.fmtPrice(item.subtotal),
          })),
        }
        this.setData({ order })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  fmtPrice(value) {
    return Number(value || 0).toFixed(2)
  },

  formatTime(ts) {
    const d = new Date(Number(ts || 0))
    if (!Number.isFinite(d.getTime())) return ''
    const pad = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  },

  reloadDetail() {
    return this.loadDetail(this.data.orderNo || (this.data.order && this.data.order.orderNo))
  },

  cancelOrder() {
    const orderNo = this.data.order && this.data.order.orderNo
    if (!orderNo) return
    wx.showModal({
      title: '取消订单',
      content: '确定要取消这笔订单吗？',
      confirmColor: '#ff4b43',
      success: (modalRes) => {
        if (!modalRes.confirm) return
        orderApi.cancel(orderNo).then(() => {
          wx.showToast({ title: '已取消', icon: 'success' })
          this.reloadDetail()
        }).catch(() => this.reloadDetail())
      },
    })
  },

  payOrder() {
    const orderNo = this.data.order && this.data.order.orderNo
    if (!orderNo) return
    orderApi
      .repay(orderNo)
      .then((res) => {
        const payParams = res && res.data && res.data.payParams
        if (!payParams) {
          wx.showToast({ title: '支付参数错误', icon: 'none' })
          return Promise.reject(new Error('missing payParams'))
        }
        if (config.ENV !== 'prod' || payParams.appId === 'wx_mock') {
          return orderApi.mockPay(orderNo).then(() => {
            wx.showToast({ title: '支付成功', icon: 'success' })
            this.reloadDetail()
          })
        }
        return new Promise((resolve, reject) => {
          wx.requestPayment({
            timeStamp: payParams.timeStamp,
            nonceStr: payParams.nonceStr,
            package: payParams.packageStr,
            signType: payParams.signType || 'RSA',
            paySign: payParams.paySign,
            success: resolve,
            fail: reject,
          })
        }).then(() => {
          wx.showToast({ title: '支付成功', icon: 'success' })
          this.reloadDetail()
        }).catch(() => {
          wx.showToast({ title: '支付未完成', icon: 'none' })
        })
      })
      .catch((err) => {
        if (err && err.message === 'missing payParams') return
        this.reloadDetail()
      })
  },

  confirmReceive() {
    const orderNo = this.data.order && this.data.order.orderNo
    if (!orderNo) return
    wx.showModal({
      title: '确认收货',
      content: '确认已经收到商品了吗？',
      confirmColor: '#ff4b43',
      success: (modalRes) => {
        if (!modalRes.confirm) return
        orderApi.confirmReceive(orderNo).then(() => {
          wx.showToast({ title: '已确认', icon: 'success' })
          this.reloadDetail()
        }).catch(() => this.reloadDetail())
      },
    })
  },

  viewLogistics() {
    wx.showToast({ title: '物流功能开发中', icon: 'none' })
  },

  showTodo(e) {
    const title = e.currentTarget.dataset.title || '功能开发中'
    wx.showToast({ title, icon: 'none' })
  },

  buyAgain() {
    const firstItem = this.data.order && this.data.order.items && this.data.order.items[0]
    if (!firstItem || !firstItem.productId || !firstItem.skuId) {
      wx.showToast({ title: '商品信息缺失', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/product/detail?id=${firstItem.productId}&skuId=${firstItem.skuId}&action=buy` })
  },

  goProductDetail(e) {
    const productId = e.currentTarget.dataset.productid
    if (!productId) return
    wx.navigateTo({ url: `/pages/product/detail?id=${productId}` })
  },
})
