const app = getApp()
const addressApi = require('../../../api/address')
const presaleApi = require('../../../api/presale')
const marketingCapabilities = require('../../../utils/marketing-capabilities')
const { resolveImageUrl } = require('../../../utils/url')

Page({
  data: {
    activity: null,
    selectedSku: null,
    activityId: 0,
    presaleSkuId: 0,
    quantity: 1,
    addresses: [],
    address: null,
    quote: null,
    agreed: false,
    submitting: false,
  },

  onLoad(options) {
    const activityId = Number(options.activityId || 0)
    const presaleSkuId = Number(options.presaleSkuId || 0)
    this.setData({ activityId, presaleSkuId })
    marketingCapabilities.ensure('PRESALE').then((enabled) => {
      if (!enabled) return
      return Promise.all([presaleApi.detail(activityId), addressApi.list()]).then(([activityRes, addressRes]) => {
        const activity = activityRes.data || {}
        const selectedSku = (activity.skus || []).find((item) => Number(item.id) === presaleSkuId) || (activity.skus || [])[0]
        const addresses = addressRes.data || []
        const defaultAddress = addresses.find((item) => item.isDefault) || addresses[0] || null
        this.setData({
          activity,
          addresses,
          address: defaultAddress,
          presaleSkuId: selectedSku ? selectedSku.id : presaleSkuId,
          selectedSku: selectedSku ? this.formatSku(selectedSku) : null,
        }, () => this.loadQuote())
      })
    }).catch(() => {})
  },

  onShow() {
    const addressId = wx.getStorageSync('presale_selected_address_id')
    if (!addressId) return
    wx.removeStorageSync('presale_selected_address_id')
    const address = (this.data.addresses || []).find((item) => Number(item.id) === Number(addressId))
    if (address) this.setData({ address }, () => this.loadQuote())
  },

  formatSku(sku) {
    const quantity = Number(this.data.quantity || 1)
    const deposit = Number(sku.depositAmount || 0)
    const deduction = Number(sku.depositDeductionAmount || 0)
    const balance = Number(sku.balanceAmount || 0)
    return {
      ...sku,
      depositText: deposit.toFixed(2),
      deductionText: deduction.toFixed(2),
      balanceText: balance.toFixed(2),
      finalText: (deposit + balance).toFixed(2),
      quantity,
      totalDepositText: (deposit * quantity).toFixed(2),
      totalDeductionText: (deduction * quantity).toFixed(2),
      totalBalanceText: (balance * quantity).toFixed(2),
      totalFinalText: ((deposit + balance) * quantity).toFixed(2),
      mainImage: resolveImageUrl(sku.mainImage || ''),
    }
  },

  chooseAddress() {
    wx.navigateTo({ url: '/pages/address/list?select=presale' })
  },

  changeQuantity(e) {
    const delta = Number(e.currentTarget.dataset.delta || 0)
    const sku = this.data.selectedSku
    if (!sku) return
    const quantity = Math.max(1, Math.min(Number(sku.userLimit || 1), Number(this.data.quantity || 1) + delta))
    this.setData({ quantity, selectedSku: this.formatSku({ ...sku, quantity }) }, () => this.loadQuote())
  },

  loadQuote() {
    const { activityId, presaleSkuId, quantity, address } = this.data
    if (!activityId || !presaleSkuId || !address) return Promise.resolve()
    return presaleApi.quoteDeposit({ activityId, presaleSkuId, addressId: address.id, quantity }).then((res) => {
      if (res.code !== 0) throw new Error(res.msg || '报价失败')
      this.setData({ quote: res.data })
    }).catch(() => {
      this.setData({ quote: null })
      wx.showToast({ title: '价格已更新，请刷新后重试', icon: 'none' })
    })
  },

  toggleAgreement() {
    this.setData({ agreed: !this.data.agreed })
  },

  submit() {
    const { activityId, presaleSkuId, quantity, address, agreed, quote, submitting } = this.data
    if (submitting) return
    if (!address) {
      wx.showToast({ title: '请选择收货地址', icon: 'none' })
      return
    }
    if (!agreed) {
      wx.showToast({ title: '请先同意预售规则', icon: 'none' })
      return
    }
    if (!quote || !quote.quoteId) {
      this.loadQuote()
      wx.showToast({ title: '正在刷新价格，请稍后重试', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    presaleApi.createDeposit({ activityId, presaleSkuId, addressId: address.id, quantity, quoteId: quote.quoteId, ruleVersion: quote.ruleVersion }).then((res) => {
      const data = res.data || {}
      if (!data.payParams) throw new Error('missing pay params')
      return this.requestPayment(data.payParams).then(() => data)
    }).then((data) => {
      wx.showToast({ title: '定金支付成功', icon: 'success' })
      setTimeout(() => wx.redirectTo({ url: `/pages/order/detail?orderNo=${data.orderNo}` }), 700)
    }).catch((err) => {
      if (err && err.message === 'missing pay params') wx.showToast({ title: '支付参数错误', icon: 'none' })
      else wx.showModal({
        title: '支付未完成',
        content: '预售订单已创建，可在订单列表中继续支付定金。',
        confirmText: '去订单',
        cancelText: '留在这里',
        success: (modalRes) => {
          if (modalRes.confirm) wx.switchTab({ url: '/pages/order/list' })
        },
      })
    }).finally(() => this.setData({ submitting: false }))
  },

  requestPayment(payParams) {
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
    })
  },
})
