const orderApi = require('../../api/order')
const groupBuyApi = require('../../api/group-buy')
const productApi = require('../../api/product')
const marketingCapabilities = require('../../utils/marketing-capabilities')
const { resolveImageUrl } = require('../../utils/url')

Page({
  data: {
    order: null,
    orderNo: '',
    groupBuyEnabled: false,
    seckillEnabled: false,
    repurchaseEnabled: false,
    repurchaseCoupon: null,
    repurchaseRecommendations: [],
    loading: false,
    logisticsLoading: false,
  },

  onLoad(options) {
    const orderNo = options.orderNo || ''
    this.setData({ orderNo })
    this.refreshMarketingState().then(() => this.loadDetail(orderNo).then(() => this.loadRepurchaseCoupon(orderNo)))
  },

  onShow() {
    if (this.data.orderNo && this.data.order) this.refreshMarketingState().then(() => this.loadRepurchaseCoupon(this.data.orderNo))
  },

  refreshMarketingState() {
    return marketingCapabilities.load(false).then(() => {
      const groupBuyEnabled = marketingCapabilities.isEnabled('GROUP_BUY')
      const seckillEnabled = marketingCapabilities.isEnabled('SECKILL')
      const repurchaseEnabled = marketingCapabilities.isEnabled('REPURCHASE_COUPON')
      const nextData = { groupBuyEnabled, seckillEnabled, repurchaseEnabled }
      if (this.data.order) nextData['order.statusText'] = this.displayStatusText(this.data.order, groupBuyEnabled)
      this.setData(nextData)
      return nextData
    })
  },

  loadRepurchaseCoupon(orderNo) {
    if (!orderNo || !this.data.repurchaseEnabled) {
      this.setData({ repurchaseCoupon: null, repurchaseRecommendations: [] })
      return Promise.resolve()
    }
    return orderApi.repurchaseCoupon(orderNo).then((res) => {
      const coupon = res && res.data
      this.setData({ repurchaseCoupon: coupon ? this.formatRepurchaseCoupon(coupon) : null })
      if (coupon) return this.loadRepurchaseRecommendations()
    }).catch(() => this.setData({ repurchaseCoupon: null, repurchaseRecommendations: [] }))
  },

  formatRepurchaseCoupon(coupon) {
    const days = Math.max(0, Math.ceil((new Date(coupon.validTo).getTime() - Date.now()) / (24 * 60 * 60 * 1000)))
    return {
      ...coupon,
      amountText: this.fmtPrice(coupon.amount),
      thresholdText: this.fmtPrice(coupon.thresholdAmount),
      expiryText: days <= 1 ? '仅剩 1 天有效' : `还有 ${days} 天有效`,
      available: coupon.available === true,
    }
  },

  goUseRepurchaseCoupon() {
    const coupon = this.data.repurchaseCoupon
    if (!coupon || !coupon.available) {
      wx.showToast({ title: (coupon && coupon.unavailableReason) || '优惠券暂不可使用', icon: 'none' })
      return
    }
    wx.setStorageSync('order_selected_coupon_id', coupon.id)
    wx.navigateTo({ url: '/pages/recommend/index' })
  },

  loadRepurchaseRecommendations() {
    const firstItem = (this.data.order && this.data.order.items || [])[0]
    if (!firstItem || !firstItem.productId) return Promise.resolve()
    return productApi.get(firstItem.productId).then((detailRes) => {
      const categoryId = detailRes && detailRes.data && detailRes.data.categoryId
      return productApi.page({ page: 1, size: 4, categoryId, isRecommend: 1 })
    }).then((res) => {
      const list = ((res && res.data && res.data.list) || []).filter((item) => Number(item.id) !== Number(firstItem.productId)).slice(0, 2)
      this.setData({ repurchaseRecommendations: list.map((item) => ({ ...item, minPriceText: this.fmtPrice(item.minPrice), mainImage: resolveImageUrl(item.mainImage || '') })) })
    }).catch(() => this.setData({ repurchaseRecommendations: [] }))
  },

  goRepurchaseProduct(e) {
    const productId = e.currentTarget.dataset.productid
    const coupon = this.data.repurchaseCoupon
    if (!productId || !coupon || !coupon.available) return this.goUseRepurchaseCoupon()
    wx.setStorageSync('order_selected_coupon_id', coupon.id)
    wx.navigateTo({ url: `/pages/product/detail?id=${productId}` })
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
          sourceStatusText: raw.statusText,
          logistics: null,
          totalAmountText: this.fmtPrice(raw.totalAmount),
          freightAmountText: this.fmtPrice(raw.freightAmount),
          discountAmountText: this.fmtPrice(raw.discountAmount),
          couponDiscountAmountText: this.fmtPrice(raw.couponDiscountAmount),
          promotionDiscountAmountText: this.fmtPrice(raw.promotionDiscountAmount),
          payAmountText: this.fmtPrice(raw.payAmount),
          groupBuyProgress: raw.groupBuyRequiredCount ? `${raw.groupBuyPaidCount || 0}/${raw.groupBuyRequiredCount} 人` : '',
          groupBuyExpireText: raw.groupBuyExpireAt ? this.formatTime(raw.groupBuyExpireAt) : '',
          statusText: this.displayStatusText(raw),
          groupBuyStatusText: raw.groupBuyStatusText || '',
          refundStatusText: raw.refundStatus === 0 ? '退款申请处理中' : raw.refundStatus === 1 ? '退款处理中' : raw.refundStatus === 2 ? '退款申请已拒绝' : raw.refundStatus === 3 ? '退款成功' : raw.refundStatus === 4 ? '退款失败，可重新申请' : raw.refundStatus === 5 ? '请填写退货物流' : raw.refundStatus === 6 ? '商家正在验货' : '',
          canRefund: ![0, 1, 5, 6].includes(raw.refundStatus)
            && ([1, 2, 3, 6, 7].includes(raw.status)
              || (raw.status === 4 && raw.cancelReason === 'REFUNDED' && raw.refundStatus === 4)),
          refundEvidenceUrls: Array.isArray(raw.refundEvidenceUrls)
            ? raw.refundEvidenceUrls.map((url) => resolveImageUrl(url)).filter(Boolean)
            : [],
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

  displayStatusText(raw, enabled = this.data.groupBuyEnabled) {
    const sourceStatusText = raw.sourceStatusText || raw.statusText
    if (enabled || raw.orderType !== 1) return sourceStatusText
    if (raw.status === 5) return '订单处理中'
    if (raw.status === 6) return '待发货'
    if (raw.status === 7) return raw.refundStatusText || '退款处理中'
    return sourceStatusText
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

  deleteOrder() {
    const order = this.data.order || {}
    if (!order.orderNo || (order.status !== 3 && order.status !== 4)) return
    wx.showModal({
      title: '删除订单',
      content: '确定要删除这笔订单吗？删除后将无法在订单列表中查看。',
      confirmColor: '#ff4b43',
      success: (modalRes) => {
        if (!modalRes.confirm) return
        orderApi.remove(order.orderNo).then(() => {
          wx.showToast({ title: '已删除', icon: 'success' })
          wx.navigateBack({ delta: 1 })
        })
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
    this.loadLogistics(false)
  },

  loadLogistics(forceRefresh) {
    const order = this.data.order || {}
    if (!order.shipNo || this.data.logisticsLoading) return Promise.resolve()
    this.setData({ logisticsLoading: true })
    const query = forceRefresh ? orderApi.refreshLogistics(order.orderNo) : orderApi.logistics(order.orderNo, false)
    return query.then((res) => {
      const raw = res.data || {}
      const logistics = {
        ...raw,
        traces: (raw.traces || []).map((trace) => ({
          ...trace,
          acceptTime: trace.acceptTime || '-',
        })),
      }
      this.setData({ 'order.logistics': logistics })
    }).finally(() => {
      this.setData({ logisticsLoading: false })
    })
  },

  refreshLogistics() {
    this.loadLogistics(true)
  },

  copyOrderNo() {
    const orderNo = this.data.order && this.data.order.orderNo
    if (!orderNo) return
    wx.setClipboardData({ data: String(orderNo) })
  },

  contactMerchant() {
    const phone = this.data.order && this.data.order.merchantContactPhone
    if (!phone) {
      wx.showToast({ title: '商家暂未提供联系电话', icon: 'none' })
      return
    }
    wx.makePhoneCall({ phoneNumber: String(phone) })
  },

  remindShip() {
    const orderNo = this.data.order && this.data.order.orderNo
    if (!orderNo) return
    orderApi.remindShip(orderNo).then(() => {
      wx.showToast({ title: '已提醒商家发货', icon: 'success' })
      this.reloadDetail()
    }).catch(() => this.reloadDetail())
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
        this.chooseRefundItems(modalRes.content || '')
      },
    })
  },

  chooseRefundItems(reason) {
    const items = (this.data.order && this.data.order.items) || []
    wx.showActionSheet({
      itemList: ['整单退款', ...items.map(item => `${item.productName} ×${item.quantity}`)],
      success: (sheetRes) => {
        if (sheetRes.tapIndex === 0) return this.promptRefundAmount(reason)
        const item = items[sheetRes.tapIndex - 1]
        this.promptRefundItemQuantity(reason, item)
      },
    })
  },

  promptRefundItemQuantity(reason, item) {
    wx.showModal({
      title: '退款商品数量',
      editable: true,
      placeholderText: `请输入 1-${item.quantity}`,
      confirmColor: '#ff4b43',
      success: (modalRes) => {
        if (!modalRes.confirm) return
        const quantity = Number(String(modalRes.content || '').trim())
        if (!Number.isInteger(quantity) || quantity < 1 || quantity > Number(item.quantity || 0)) {
          wx.showToast({ title: '退款数量不正确', icon: 'none' })
          return
        }
        this.submitRefund(reason, { items: [{ orderItemId: item.id, quantity }] })
      },
    })
  },

  promptRefundAmount(reason) {
    wx.showModal({
      title: '退款金额',
      editable: true,
      placeholderText: '请输入金额，留空表示全额退款',
      confirmColor: '#ff4b43',
      success: (modalRes) => {
        if (!modalRes.confirm) return
        const amountText = String(modalRes.content || '').trim()
        const amount = Number(amountText)
        if (amountText && (!Number.isFinite(amount) || amount <= 0 || Math.round(amount * 100) !== amount * 100)) {
          wx.showToast({ title: '退款金额格式不正确', icon: 'none' })
          return
        }
        const payload = {}
        if (amountText) payload.refundAmount = amount.toFixed(2)
        this.submitRefund(reason, payload)
      },
    })
  },

  submitRefund(reason, payload) {
    const orderNo = this.data.order && this.data.order.orderNo
    this.chooseRefundEvidence().then((filePaths) => {
          if (!filePaths.length) return []
          wx.showLoading({ title: '上传凭证中', mask: true })
          return filePaths.reduce((promise, filePath) => promise.then((urls) =>
            orderApi.uploadRefundEvidence(filePath).then((url) => urls.concat(url))), Promise.resolve([]))
            .finally(() => wx.hideLoading())
        }).then((evidenceUrls) => {
          return orderApi.refund(orderNo, { ...payload, reason, evidenceUrls })
        }).then(() => {
          wx.showToast({ title: '已提交退款申请', icon: 'success' })
          this.reloadDetail()
        }).catch(() => this.reloadDetail())
  },

  chooseRefundEvidence() {
    return new Promise((resolve, reject) => {
      wx.chooseImage({
        count: 6,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => resolve((res.tempFilePaths || []).slice(0, 6)),
        fail: (err) => {
          if (String(err && err.errMsg || '').includes('cancel')) resolve([])
          else reject(err)
        },
      })
    })
  },

  previewRefundEvidence(e) {
    const current = e.currentTarget.dataset.url
    const urls = (this.data.order && this.data.order.refundEvidenceUrls) || []
    if (current && urls.length) wx.previewImage({ current, urls })
  },

  submitReturnShipment() {
    const order = this.data.order || {}
    if (!order.refundId) return
    wx.showModal({
      title: '填写退货物流',
      editable: true,
      placeholderText: '承运商,单号，例如 顺丰,SF12345678',
      success: (modalRes) => {
        if (!modalRes.confirm) return
        const parts = String(modalRes.content || '').split(',').map((v) => v.trim())
        if (parts.length !== 2 || !parts[0] || !/^[A-Za-z0-9]{5,30}$/.test(parts[1])) {
          wx.showToast({ title: '请按“承运商,单号”填写', icon: 'none' })
          return
        }
        orderApi.submitReturnShipment(order.refundId, { shipCompany: parts[0], shipNo: parts[1] })
          .then(() => {
            wx.showToast({ title: '退货物流已提交', icon: 'success' })
            this.reloadDetail()
          })
          .catch(() => this.reloadDetail())
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
    if (this.data.groupBuyEnabled && order.orderType === 1 && order.groupBuyGroupId) {
      groupBuyApi.shareEvent(order.groupBuyGroupId, 'order_share', false).catch(() => {})
      return {
        title: '快来参加我的拼团',
        path: `/pages/group-buy/group?groupId=${order.groupBuyGroupId}&shareSource=order_share`,
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
