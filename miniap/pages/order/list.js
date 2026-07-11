const orderApi = require('../../api/order')
const config = require('../../utils/config')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    tabs: [
      { label: '全部', value: null, key: 'all' },
      { label: '待支付', value: 0, key: '0' },
      { label: '待发货', value: 1, key: '1' },
      { label: '待收货', value: 2, key: '2' },
      { label: '已完成', value: 3, key: '3' },
    ],
    orders: [],
    currentStatus: null,
    currentStatusKey: 'all',
    page: 1,
    hasMore: true,
    loading: false,
  },

  onShow() {
    this.refreshList()
  },

  onPullDownRefresh() {
    this.refreshList().finally(() => wx.stopPullDownRefresh())
  },

  refreshList() {
    this.setData({ page: 1, orders: [], hasMore: true })
    return this.loadOrders()
  },

  loadOrders() {
    if (this.data.loading) return Promise.resolve()
    const params = {
      page: this.data.page,
      size: 10,
    }
    if (this.data.currentStatus !== null && this.data.currentStatus !== undefined) {
      params.status = this.data.currentStatus
    }

    this.setData({ loading: true })
    return orderApi
      .page(params)
      .then((res) => {
        const list = ((res.data && res.data.list) || []).map((item) => ({
          ...item,
          firstItemImage: resolveImageUrl(item.firstItemImage || ''),
          totalLabel: item.status === 0 ? '需支付' : '实付',
          items: (item.items || []).map((goods) => ({
            ...goods,
            mainImage: resolveImageUrl(goods.mainImage || ''),
            unitPriceText: this.fmtPrice(goods.unitPrice),
            subtotalText: this.fmtPrice(goods.subtotal),
          })),
        }))
        const newOrders = this.data.page === 1 ? list : this.data.orders.concat(list)
        this.setData({
          orders: newOrders,
          hasMore: list.length === 10,
        })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  fmtPrice(value) {
    return Number(value || 0).toFixed(2)
  },

  switchTab(e) {
    const key = e.currentTarget.dataset.key
    const tab = this.data.tabs.find((item) => item.key === key)
    if (!tab || tab.key === this.data.currentStatusKey) return
    this.setData({
      currentStatus: tab.value,
      currentStatusKey: tab.key,
      page: 1,
      orders: [],
      hasMore: true,
    })
    this.loadOrders()
  },

  goDetail(e) {
    const orderNo = e.currentTarget.dataset.orderno
    if (!orderNo) return
    wx.navigateTo({ url: '/pages/order/detail?orderNo=' + orderNo })
  },

  goHome() {
    wx.switchTab({ url: '/pages/home/index' })
  },

  cancelOrder(e) {
    const orderNo = e.currentTarget.dataset.orderno
    if (!orderNo) return
    wx.showModal({
      title: '取消订单',
      content: '确定要取消这笔订单吗？',
      confirmColor: '#ff4b43',
      success: (modalRes) => {
        if (!modalRes.confirm) return
        orderApi.cancel(orderNo).then(() => {
          wx.showToast({ title: '已取消', icon: 'success' })
          this.refreshList()
        }).catch(() => this.refreshList())
      },
    })
  },

  payOrder(e) {
    const orderNo = e.currentTarget.dataset.orderno
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
            this.refreshList()
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
          this.refreshList()
        }).catch(() => {
          wx.showToast({ title: '支付未完成', icon: 'none' })
        })
      })
      .catch((err) => {
        if (err && err.message === 'missing payParams') return
        this.refreshList()
      })
  },

  confirmReceive(e) {
    const orderNo = e.currentTarget.dataset.orderno
    if (!orderNo) return
    wx.showModal({
      title: '确认收货',
      content: '确认已经收到商品了吗？',
      confirmColor: '#ff4b43',
      success: (modalRes) => {
        if (!modalRes.confirm) return
        orderApi.confirmReceive(orderNo).then(() => {
          wx.showToast({ title: '已确认', icon: 'success' })
          this.refreshList()
        }).catch(() => this.refreshList())
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

  buyAgain(e) {
    const productId = e.currentTarget.dataset.productid
    const skuId = e.currentTarget.dataset.skuid
    if (!productId || !skuId) {
      wx.showToast({ title: '商品信息缺失', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/product/detail?id=${productId}&skuId=${skuId}&action=buy` })
  },

  onReachBottom() {
    if (!this.data.hasMore || this.data.loading) return
    this.setData({ page: this.data.page + 1 })
    this.loadOrders()
  },
})
