const orderApi = require('../../api/order')
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
          refundStatusText: raw.refundStatus === 0 ? '退款申请处理中' : raw.refundStatus === 1 ? '退款处理中' : raw.refundStatus === 2 ? '退款申请已拒绝' : raw.refundStatus === 3 ? '退款成功' : raw.refundStatus === 4 ? '退款失败，可重新申请' : '',
          canRefund: [1, 2, 3].includes(raw.status) || (raw.orderType === 1 && raw.status === 6),
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
    const order = this.data.order || {}
    if (!order.shipNo) {
      wx.showToast({ title: '商家尚未发货', icon: 'none' })
      return
    }
    wx.showModal({
      title: '物流信息',
      content: `${order.shipCompany || '物流'}\n运单号：${order.shipNo}\n发货时间：${order.shipTime || '-'}`,
      showCancel: false,
    })
  },

  goGroup() {
    const groupId = this.data.order && this.data.order.groupBuyGroupId
    if (!groupId) return
    wx.navigateTo({ url: `/pages/group-buy/group?groupId=${groupId}` })
  },

  refundApply() {
    const orderNo = this.data.order && this.data.order.orderNo
    if (!orderNo) return
    wx.showModal({
      title: '申请退款',
      editable: true,
      placeholderText: '请输入退款原因（可选）',
      confirmColor: '#ff4b43',
      success: (modalRes) => {
        if (!modalRes.confirm) return
        orderApi.refund(orderNo, modalRes.content || '').then(() => {
          wx.showToast({ title: '已提交退款申请', icon: 'success' })
          this.reloadDetail()
        }).catch(() => this.reloadDetail())
      },
    })
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

  onShareAppMessage() {
    const order = this.data.order || {}
    if (order.orderType === 1 && order.groupBuyGroupId) {
      return {
        title: '快来参加我的拼团',
        path: `/pages/group-buy/group?groupId=${order.groupBuyGroupId}`,
      }
    }
    return { title: '潮选商城' }
  },

  goProductDetail(e) {
    const productId = e.currentTarget.dataset.productid
    if (!productId) return
    wx.navigateTo({ url: `/pages/product/detail?id=${productId}` })
  },
})
