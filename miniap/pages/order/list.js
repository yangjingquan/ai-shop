const orderApi = require('../../api/order')
const marketingCapabilities = require('../../utils/marketing-capabilities')
const { resolveImageUrl } = require('../../utils/url')

const SWIPE_DELETE_WIDTH = 140

Page({
  data: {
    tabs: [{ label: '全部', value: null, key: 'all' }],
    orders: [],
    currentStatus: null,
    currentStatusKey: 'all',
    groupBuyEnabled: false,
    seckillEnabled: false,
    page: 1,
    hasMore: true,
    loading: false,
  },

  onShow() {
    Promise.all([marketingCapabilities.load(false), orderApi.dictionary()]).then(([, dictionary]) => {
      const groupBuyEnabled = marketingCapabilities.isEnabled('GROUP_BUY')
      const seckillEnabled = marketingCapabilities.isEnabled('SECKILL')
      const states = (dictionary.data && dictionary.data.states) || []
      const visibleStates = [0, 1, 5, 6, 2, 3, 4, 7, 8, 9]
      const tabs = [{ label: '全部', value: null, key: 'all' }].concat(states
        .filter((state) => visibleStates.includes(Number(state.code)))
        .map((state) => ({ label: state.text, value: state.code, key: String(state.code), groupOnly: [5, 6, 7].includes(Number(state.code)) })))
      const nextData = { groupBuyEnabled, seckillEnabled, tabs }
      if (!groupBuyEnabled && [5, 6, 7].includes(this.data.currentStatus)) {
        nextData.currentStatus = null
        nextData.currentStatusKey = 'all'
      }
      this.setData(nextData)
      return this.refreshList()
    })
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
          statusText: item.stateText || item.statusText,
          displayStatusCode: item.state === undefined ? item.status : item.state,
          seckillLabel: this.data.seckillEnabled && item.orderType === 2 ? item.orderTypeText : '',
          bundleLabel: Number(item.orderType) === 4 ? item.orderTypeText : '',
          lotteryLabel: Number(item.orderType) === 5 ? item.orderTypeText : '',
          presaleLabel: Number(item.orderType) === 6 ? item.orderTypeText : '',
          isPresale: Number(item.orderType) === 6,
          firstItemImage: resolveImageUrl(item.firstItemImage || ''),
          canDelete: item.status === 3 || item.status === 4,
          swipeOffset: 0,
          swipeStyle: 'transform: translate3d(0, 0, 0);',
          groupBuyProgress: this.data.groupBuyEnabled && item.orderType === 1 && item.groupBuyRequiredCount
            ? `${item.groupBuyPaidCount || 0}/${item.groupBuyRequiredCount} 人`
            : '',
          refundStatusText: item.refundStatus === 0 ? '退款申请处理中' : item.refundStatus === 1 ? '退款处理中' : item.refundStatus === 2 ? '退款申请已拒绝' : item.refundStatus === 3 ? '退款成功' : item.refundStatus === 4 ? '退款失败，可重新申请' : item.refundStatus === 5 ? '请填写退货物流' : item.refundStatus === 6 ? '商家正在验货' : '',
          totalLabel: this.totalLabel(item),
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
          hasMore: newOrders.length < Number((res.data && res.data.total) || 0),
        })
      })
      .finally(() => {
        this.setData({ loading: false })
      })
  },

  fmtPrice(value) {
    return Number(value || 0).toFixed(2)
  },


  totalLabel(item) {
    if (Number(item.orderType) === 6 && item.presaleBalancePaid) return '已付尾款'
    if (item.status === 0) return '需支付'
    if (item.status === 8 || item.status === 9) return '已付定金'
    return '实付'
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
    if (this._suppressCardTap) return
    const rawIndex = e.currentTarget.dataset.index
    const index = rawIndex === undefined ? undefined : Number(rawIndex)
    const item = index === undefined ? null : this.data.orders[index]
    if (item && item.swipeOffset > 0) {
      this.closeSwipeRows()
      return
    }
    const orderNo = e.currentTarget.dataset.orderno
    if (!orderNo) return
    wx.navigateTo({ url: '/pages/order/detail?orderNo=' + orderNo })
  },

  closeSwipeRows(exceptIndex = -1) {
    const orders = this.data.orders || []
    const nextOrders = orders.map((item, index) => {
      if (index === exceptIndex || !item.swipeOffset) return item
      return {
        ...item,
        swipeOffset: 0,
        swipeStyle: 'transform: translate3d(0, 0, 0);',
      }
    })
    if (nextOrders.some((item, index) => item !== orders[index])) {
      this.setData({ orders: nextOrders })
    }
  },

  onSwipeStart(e) {
    const rawIndex = e.currentTarget.dataset.index
    const index = rawIndex === undefined ? -1 : Number(rawIndex)
    const item = this.data.orders[index]
    const touch = e.touches && e.touches[0]
    if (!item || !item.canDelete || !touch) {
      this.closeSwipeRows()
      return
    }
    this.closeSwipeRows(index)
    this._swipeState = {
      index,
      startX: touch.pageX,
      startY: touch.pageY,
      currentOffset: item.swipeOffset || 0,
      horizontal: false,
      moved: false,
    }
  },

  onSwipeMove(e) {
    const state = this._swipeState
    const touch = e.touches && e.touches[0]
    if (!state || !touch) return
    const dx = touch.pageX - state.startX
    const dy = touch.pageY - state.startY
    if (!state.horizontal && Math.abs(dx) < 8 && Math.abs(dy) < 8) return
    if (!state.horizontal && Math.abs(dy) > Math.abs(dx)) {
      state.vertical = true
      return
    }
    if (state.vertical) return
    state.horizontal = true
    if (Math.abs(dx) > 8) {
      state.moved = true
      this._suppressCardTap = true
    }
    const offset = Math.max(0, Math.min(SWIPE_DELETE_WIDTH, state.currentOffset - dx))
    state.currentOffset = offset
    this.setData({
      [`orders[${state.index}].swipeOffset`]: offset,
      [`orders[${state.index}].swipeStyle`]: `transform: translate3d(-${offset}rpx, 0, 0);`,
    })
  },

  onSwipeEnd() {
    const state = this._swipeState
    if (!state) return
    const offset = state.currentOffset > SWIPE_DELETE_WIDTH / 2 ? SWIPE_DELETE_WIDTH : 0
    this.setData({
      [`orders[${state.index}].swipeOffset`]: offset,
      [`orders[${state.index}].swipeStyle`]: `transform: translate3d(-${offset}rpx, 0, 0);`,
    })
    this._swipeState = null
    if (state.moved) {
      setTimeout(() => {
        this._suppressCardTap = false
      }, 220)
    }
  },

  deleteOrder(e) {
    const orderNo = e.currentTarget.dataset.orderno
    if (!orderNo) return
    this.closeSwipeRows()
    wx.showModal({
      title: '删除订单',
      content: '确定要删除这笔订单吗？删除后将无法在订单列表中查看。',
      confirmColor: '#ff4b43',
      success: (modalRes) => {
        if (!modalRes.confirm) return
        orderApi.remove(orderNo).then(() => {
          wx.showToast({ title: '已删除', icon: 'success' })
          this.refreshList()
        }).catch(() => this.refreshList())
      },
    })
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

  refundApply(e) {
    this.goDetail(e)
  },

  viewLogistics(e) {
    this.goDetail(e)
  },

  remindShip(e) {
    const orderNo = e.currentTarget.dataset.orderno
    if (!orderNo) return
    orderApi.remindShip(orderNo).then(() => {
      wx.showToast({ title: '已提醒商家发货', icon: 'success' })
      this.refreshList()
    }).catch(() => this.refreshList())
  },

  goGroup(e) {
    const groupId = e.currentTarget.dataset.groupid
    if (!groupId) return
    wx.navigateTo({ url: `/pages/group-buy/group?groupId=${groupId}` })
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
